package com.hireconnect.application.service;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.repository.ApplicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.hireconnect.application.config.RabbitMQConfig;
import com.hireconnect.application.dto.NotificationEvent;

@Slf4j
@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OfferLetterPdfService offerLetterPdfService;

    private void sendNotification(int userId, String email, String type, String message) {
        sendNotificationWithAttachment(userId, email, type, message, null, null);
    }

    private void sendNotificationWithAttachment(int userId, String email, String type, String message, byte[] attachment, String fileName) {
        // We now allow email to be null/blank for in-app-only notifications
        String effectiveEmail = (email == null || email.isBlank()) ? "no-email-found@hireconnect.com" : email;
        
        try {
            // AUDIT LOG: Confirm message was published to RabbitMQ
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("application_dispatch.log"),
                String.format("[%s] Dispatched: %s for User %d (Email: %s)\n", 
                    java.time.LocalDateTime.now(), type, userId, email),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND
            );

            NotificationEvent event = new NotificationEvent(userId, email, type, message);
            event.setAttachmentBytes(attachment);
            event.setAttachmentName(fileName);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Published {} notification event to RabbitMQ for user {} (with attachment: {})", type, userId, fileName != null);
        } catch (Exception e) {
            log.error("Failed to publish notification event for user {}: {}", userId, e.getMessage());
        }
    }

    private String getEmailFromProfile(int userId) {
        // Try Profile Service FIRST as it's the source of truth for Candidate contact info
        try {
            Map<?, ?> profile = restTemplate.getForObject("http://profile-service/api/profiles/" + userId, Map.class);
            if (profile != null && profile.get("email") != null) {
                return profile.get("email").toString();
            }
        } catch (Exception e) {
            log.warn("Profile service lookup failed for user {}: {}", userId, e.getMessage());
        }

        // Fallback: Try Auth Service
        try {
            return restTemplate.getForObject("http://auth-service/auth/users/" + userId + "/email", String.class);
        } catch (Exception e) {
            log.warn("Auth service lookup failed for user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private String getRoleFromAuth(int userId) {
        try {
            return restTemplate.getForObject("http://auth-service/auth/users/" + userId + "/role", String.class);
        } catch (Exception e) {
            log.warn("Could not fetch role for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private Map<?, ?> getJobDetails(int jobId) {
        try {
            return restTemplate.getForObject("http://job-service/api/jobs/" + jobId, Map.class);
        } catch (Exception e) {
            log.error(" CRITICAL: Failed to fetch job details for job ID {} from job-service! Reason: {}", jobId, e.getMessage());
            return null;
        }
    }

    @Override
    public Application submitApplication(Application application) {
        log.info("Attempting to submit application for Candidate {} to Job {}", 
                 application.getCandidateId(), application.getJobId());
        
        // Business Logic: Prevent duplicate applications
        Optional<Application> existing = repository.findFirstByJobIdAndCandidateId(
                application.getJobId(), application.getCandidateId());
        
        if (existing.isPresent()) {
            log.warn("Candidate {} already applied for Job {}", 
                     application.getCandidateId(), application.getJobId());
            throw new RuntimeException("You have already applied for this job.");
        }

        Application saved = repository.save(application);
        log.info("Successfully submitted application with ID: {}", saved.getApplicationId());

        // Notification Logic
        String candidateEmail = getEmailFromProfile(application.getCandidateId());
        Map<?, ?> job = getJobDetails(application.getJobId());
        String jobTitle = job != null && job.get("title") != null ? job.get("title").toString() : "Job #" + application.getJobId();
        
        if (candidateEmail != null) {
            String role = getRoleFromAuth(application.getCandidateId());
            // Only send application submission notification if the user is a CANDIDATE
            if ("CANDIDATE".equalsIgnoreCase(role)) {
                sendNotification(application.getCandidateId(), candidateEmail, "APPLICATION_SUBMITTED",
                        "Your application for " + jobTitle + " has been successfully submitted. You will be notified of any updates.");
            } else {
                log.info("Skipping APPLICATION_SUBMITTED notification for user {} as they have role {}", 
                        application.getCandidateId(), role);
            }
        }

        if (job != null) {
            Object recruiterObj = job.get("postedBy");
            if (recruiterObj == null) {
                recruiterObj = job.get("recruiterId");
            }
            if (recruiterObj != null) {
                try {
                    int recruiterId = Integer.parseInt(recruiterObj.toString());
                    String recruiterEmail = getEmailFromProfile(recruiterId);
                    if (recruiterEmail != null) {
                        sendNotification(recruiterId, recruiterEmail, "NEW_APPLICATION",
                                "A new candidate has applied for your job posting: " + jobTitle + ". Please log in to review their application.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid recruiter ID format: {}", recruiterObj);
                }
            }
        }

        return saved;
    }

    @Override
    public List<Application> getByCandidate(int candidateId) {
        log.info("Fetching all applications for Candidate ID: {}", candidateId);
        return repository.findByCandidateId(candidateId);
    }

    @Override
    public List<Application> getByJob(int jobId) {
        log.info("Fetching all applications for Job ID: {}", jobId);
        return repository.findByJobId(jobId);
    }

    @Override
    public Application updateStatus(int applicationId, String status) {
        log.info("Updating status of application {} to '{}'", applicationId, status);
        
        return repository.findById(applicationId).map(app -> {
            app.setStatus(status);
            Application updated = repository.save(app);
            log.info("Successfully updated status for application {}", applicationId);

            // Notify candidate of status change
            String candidateEmail = getEmailFromProfile(updated.getCandidateId());
            Map<?, ?> job = getJobDetails(updated.getJobId());
            String jobTitle = job != null && job.get("title") != null ? job.get("title").toString() : "Job #" + updated.getJobId();
            log.info("Candidate email found: {}. Proceeding with notification logic for status: {}", candidateEmail, status);

            if ("Offered".equalsIgnoreCase(status) || "Offer".equalsIgnoreCase(status) || "OFFERED".equalsIgnoreCase(status)) {
                if (candidateEmail != null) {
                    byte[] offerLetter = null;
                    String fileName = null;
                    try {
                        String candidateName = "Candidate #" + updated.getCandidateId();
                        try {
                            Map<?, ?> profile = restTemplate.getForObject("http://profile-service/api/profiles/" + updated.getCandidateId(), Map.class);
                            if (profile != null && profile.get("fullName") != null) {
                                candidateName = profile.get("fullName").toString();
                            } else {
                                // Fallback: If profile is missing, use email prefix as a temporary name
                                candidateName = candidateEmail != null ? candidateEmail.split("@")[0] : "Candidate";
                            }
                        } catch (Exception e) {
                            log.warn("Profile service unavailable, using email prefix for candidate name: {}", candidateEmail);
                            candidateName = candidateEmail.split("@")[0];
                        }
                        
                        offerLetter = offerLetterPdfService.generateOfferLetter(candidateName, jobTitle, 
                                job != null && job.get("companyName") != null ? job.get("companyName").toString() : "Your Company");
                        fileName = "Offer_Letter_" + updated.getApplicationId() + ".pdf";
                    } catch (Exception e) {
                        log.error("Failed to generate offer letter for application {}: {}", updated.getApplicationId(), e.getMessage());
                    }

                    log.info("Dispatching OFFER_RECEIVED notification for Candidate {} with attachment: {}", updated.getCandidateId(), fileName);
                    sendNotificationWithAttachment(updated.getCandidateId(), candidateEmail, "OFFER_RECEIVED",
                            "Congratulations! You have received an offer for the " + jobTitle + " role. Please log into your dashboard to view the details and find your Offer Letter attached to this email.",
                            offerLetter, fileName);
                } else {
                    log.warn("Candidate email is null, sending in-app ONLY notification for OFFER_RECEIVED");
                    sendNotificationWithAttachment(updated.getCandidateId(), null, "OFFER_RECEIVED",
                            "Congratulations! You have received an offer for the " + jobTitle + " role. Please log into your dashboard to view the details.",
                            null, null);
                }
                // Removed recruiter notification for offer letter as requested.
            } else if ("Accepted".equalsIgnoreCase(status) || "Joined".equalsIgnoreCase(status)) {
                if (candidateEmail != null) {
                    sendNotification(updated.getCandidateId(), candidateEmail, "OFFER_ACCEPTED",
                            "Thank you for accepting the offer for the " + jobTitle + " role! We are thrilled to have you join us.");
                    sendNotification(updated.getCandidateId(), candidateEmail, "JOINING_INSTRUCTIONS",
                            "Welcome aboard! Your joining instructions for the " + jobTitle + " role are now available. Please check your dashboard for onboarding tasks.");
                }
                
                // Notify the recruiter that the candidate joined
                if (job != null) {
                    Object recruiterObj = job.get("postedBy") != null ? job.get("postedBy") : job.get("recruiterId");
                    if (recruiterObj != null) {
                        try {
                            int recruiterId = Integer.parseInt(recruiterObj.toString());
                            String recruiterEmail = getEmailFromProfile(recruiterId);
                            if (recruiterEmail != null) {
                                sendNotification(recruiterId, recruiterEmail, "CANDIDATE_JOINED",
                                        "Great news! Candidate ID " + updated.getCandidateId() + " has accepted the offer and joined for the " + jobTitle + " role.");
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } else {
                sendNotification(updated.getCandidateId(), candidateEmail, "STATUS_UPDATE",
                        "The status of your application for " + jobTitle + " has been updated to: " + status + ".");
            }

            return updated;
        }).orElseThrow(() -> {
            log.error("Failed to update: Application {} not found", applicationId);
            return new RuntimeException("Application not found");
        });
    }

    @Override
    public void withdrawApplication(int applicationId) {
        log.info("Attempting to withdraw application ID: {}", applicationId);
        // Instead of deleting, we change status to "Withdrawn" to keep a historical record
        updateStatus(applicationId, "Withdrawn");
        log.info("Application {} successfully withdrawn", applicationId);
    }

    @Override
    public Optional<Application> getById(int applicationId) {
        log.info("Fetching application details for ID: {}", applicationId);
        return repository.findById(applicationId);
    }

    @Override
    public int countByJobId(int jobId) {
        log.info("Counting total applications for Job ID: {}", jobId);
        return repository.countByJobId(jobId);
    }

    @Override
    public List<Application> getAllApplications() {
        log.info("Fetching all applications");
        return repository.findAll();
    }

    @Override
    public void reTriggerNotification(int applicationId) {
        log.info("Manually re-triggering notification for application ID: {}", applicationId);
        repository.findById(applicationId).ifPresent(app -> {
            updateStatus(applicationId, app.getStatus());
        });
    }
}