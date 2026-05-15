package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.repository.InterviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.hireconnect.interview.config.RabbitMQConfig;
import com.hireconnect.interview.dto.NotificationEvent;

@Slf4j
@Service
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private void sendInterviewNotification(Interview interview, String type, String candidatePrefix, String recruiterPrefix) {
        try {
            log.info("Preparing interview notification for Application: {}", interview.getApplicationId());
            Map<?, ?> application = null;
            try {
                application = restTemplate.getForObject("http://application-service/api/applications/" + interview.getApplicationId(), Map.class);
            } catch (Exception e) {
                log.error("CRITICAL: Failed to fetch application {} from application-service: {}", interview.getApplicationId(), e.getMessage());
                return;
            }

            if (application == null || application.get("candidateId") == null || application.get("jobId") == null) {
                log.warn("Cannot send notification: Application data is incomplete for ID {}: {}", interview.getApplicationId(), application);
                return;
            }
            
            int candidateId = Integer.parseInt(application.get("candidateId").toString());
            int jobId = Integer.parseInt(application.get("jobId").toString());

            String jobTitle = "a job";
            int recruiterId = 0;
            try {
                // FIXED: job-service maps to /api/jobs, not /api/v1/jobs
                Map<?, ?> job = restTemplate.getForObject("http://job-service/api/jobs/" + jobId, Map.class);
                if (job != null) {
                    if (job.get("title") != null) jobTitle = job.get("title").toString();
                    if (job.get("postedBy") != null) recruiterId = Integer.parseInt(job.get("postedBy").toString());
                    else if (job.get("recruiterId") != null) recruiterId = Integer.parseInt(job.get("recruiterId").toString());
                }
            } catch (Exception e) {
                log.warn("Could not fetch job info for job {}: {}", jobId, e.getMessage());
            }

            String messageBody = String.format("Job Title: %s|Scheduled At: %s|Mode: %s|Location: %s|Meet Link: %s|Notes: %s",
                    jobTitle, 
                    interview.getScheduledAt() != null ? interview.getScheduledAt().toString().replace("T", " ") : "TBD",
                    interview.getMode() != null ? interview.getMode() : "ONLINE",
                    interview.getLocation() != null ? interview.getLocation() : "N/A",
                    interview.getMeetLink() != null ? interview.getMeetLink() : "N/A",
                    interview.getNotes() != null && !interview.getNotes().isBlank() ? interview.getNotes() : "None");

            String candidateEmail = null;
            // Strategy: First try Profile Service (Source of Truth for Candidate contact info)
            try {
                Map<?, ?> profile = restTemplate.getForObject("http://profile-service/api/profiles/" + candidateId, Map.class);
                if (profile != null && profile.get("email") != null) {
                    candidateEmail = profile.get("email").toString();
                }
            } catch (Exception e) {
                log.warn("Could not fetch candidate profile for ID {}: {}", candidateId, e.getMessage());
            }

            // Fallback: Try Auth Service
            if (candidateEmail == null || candidateEmail.isBlank()) {
                try {
                    candidateEmail = restTemplate.getForObject("http://auth-service/auth/users/" + candidateId + "/email", String.class);
                } catch (Exception e) {
                    log.warn("Could not fetch candidate email from auth-service for ID {}: {}", candidateId, e.getMessage());
                }
            }

            // MANDATORY DELIVERY: Send in-app notification even if email is missing
            String effectiveEmail = (candidateEmail == null || candidateEmail.isBlank()) ? "no-email-found@hireconnect.com" : candidateEmail;
            
            NotificationEvent event = new NotificationEvent(
                    candidateId,
                    effectiveEmail,
                    type,
                    candidatePrefix + "|" + messageBody
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Published {} notification event to RabbitMQ for candidate {} (Effective Email: {})", type, candidateId, effectiveEmail);

            // Removed recruiter notification logic as per requirements:
            // Recruiter should not receive interview schedule/update/cancel emails.

        } catch (Exception e) {
            log.error("Failed to send interview notification: {}", e.getMessage());
        }
    }

    @Override
    public Interview scheduleInterview(Interview interview) {
        log.info("Scheduling interview for Application: {}", interview.getApplicationId());
        interview.setStatus("SCHEDULED");
        Interview saved = repository.save(interview);
        sendInterviewNotification(saved, "INTERVIEW_INVITE", "You have been invited for an interview!", "An interview has been scheduled with a candidate");
        return saved;
    }

    @Override
    public Interview confirmInterview(int id) {
        log.info("Confirming interview ID: {}", id);
        return repository.findById(id).map(i -> {
            i.setStatus("CONFIRMED");
            Interview saved = repository.save(i);
            sendInterviewNotification(saved, "INTERVIEW_UPDATE", "Your interview has been confirmed.", "An interview has been confirmed");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    @Override
    public Interview rescheduleInterview(int id, LocalDateTime newTime) {
        log.info("Rescheduling interview ID: {} to {}", id, newTime);
        return repository.findById(id).map(i -> {
            i.setScheduledAt(newTime);
            i.setStatus("RESCHEDULED");
            Interview saved = repository.save(i);
            sendInterviewNotification(saved, "INTERVIEW_UPDATE", "Your interview has been rescheduled.", "An interview has been rescheduled");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    @Override
    public void cancelInterview(int id) {
        log.info("Cancelling interview ID: {}", id);
        repository.findById(id).ifPresent(i -> {
            i.setStatus("CANCELLED");
            Interview saved = repository.save(i);
            sendInterviewNotification(saved, "INTERVIEW_UPDATE", "Your interview has been cancelled.", "An interview has been cancelled");
        });
    }

    @Override
    public List<Interview> getByApplication(int appId) {
        log.info("Fetching interviews for Application: {}", appId);
        return repository.findByApplicationId(appId);
    }
}