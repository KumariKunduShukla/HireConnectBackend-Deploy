package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.exception.NotificationNotFoundException;
import com.hireconnect.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notifRepo;
    private final JavaMailSender emailSender;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public NotificationServiceImpl(NotificationRepository notifRepo, JavaMailSender emailSender, org.springframework.web.client.RestTemplate restTemplate) {
        this.notifRepo = notifRepo;
        this.emailSender = emailSender;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public void sendNotification(Notification notification) {
        sendNotification(notification, null, null);
    }

    @Override
    @Transactional
    public void sendNotification(Notification notification, byte[] attachment, String fileName) {
        if (notification.getUserId() <= 0) {
            log.error("Cannot save notification: Invalid userId {}", notification.getUserId());
            return;
        }

        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification saved = notifRepo.save(notification);
        log.info("In-app notification saved for user {} (ID: {})", notification.getUserId(), saved.getNotificationId());

        if (notification.getRecipientEmail() != null && !notification.getRecipientEmail().isBlank()) {
            sendEmailAlert(notification.getRecipientEmail(), notification.getType(), notification.getMessage(), attachment, fileName);
        }
    }

    @Override
    @Transactional
    public void markAsRead(int id) {
        Notification n = notifRepo.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        n.setRead(true);
        notifRepo.save(n);
        log.debug("Notification ID {} marked as read", id);
    }

    @Override
    @Transactional
    public void markAllRead(int userId) {
        notifRepo.markAllAsReadByUserId(userId);
        log.info("All notifications marked as read for user ID: {}", userId);
    }

    @Override
    public List<Notification> getByUser(int userId) {
        // IDENTITY LINKING FOR DEMO: If user is 1 or 2, fetch for both to bridge Candidate/Recruiter accounts
        java.util.Set<Integer> linkedIds = new java.util.HashSet<>();
        linkedIds.add(userId);
        if (userId == 1 || userId == 2) {
            linkedIds.add(1);
            linkedIds.add(2);
        }
        
        List<Notification> allNotifs = new java.util.ArrayList<>();
        for (Integer id : linkedIds) {
            allNotifs.addAll(notifRepo.findByUserId(id));
        }
        
        // Sort by ID descending (newest first)
        allNotifs.sort((a, b) -> b.getNotificationId() - a.getNotificationId());
        
        log.info("Fetched {} merged notifications for user ID {} (Linked IDs: {})", allNotifs.size(), userId, linkedIds);
        return allNotifs;
    }

    @Override
    @Transactional
    public void deleteNotification(int id) {
        if (!notifRepo.existsById(id)) {
            throw new NotificationNotFoundException(id);
        }
        notifRepo.deleteById(id);
        log.info("Notification ID {} deleted", id);
    }

    @Override
    public void sendEmailAlert(String to, String subject, String body) {
        sendEmailAlert(to, subject, body, null, null);
    }

    @Override
    public void sendEmailAlert(String to, String subject, String body, byte[] attachment, String fileName) {
        if (to == null || to.contains("no-email-found")) {
            log.info("Skipping email dispatch for placeholder/null address: {}", to);
            return;
        }
        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("HireConnect: " + subject);
            
            String htmlBody;
            if ("INTERVIEW_INVITE".equals(subject) || "INTERVIEW_UPDATE".equals(subject)) {
                String[] parts = body.split("\\|");
                String jobTitle = "Interview";
                String scheduledAt = "TBD";
                String mode = "ONLINE";
                String location = "N/A";
                String meetLink = "N/A";
                String notes = "None";
                String prefixMsg = "INTERVIEW_INVITE".equals(subject) ? "You have been invited to an interview" : "You have an update regarding your interview";

                for (String part : parts) {
                    if (part.startsWith("Job Title: ")) jobTitle = part.substring("Job Title: ".length());
                    else if (part.startsWith("Scheduled At: ")) scheduledAt = part.substring("Scheduled At: ".length());
                    else if (part.startsWith("Mode: ")) mode = part.substring("Mode: ".length());
                    else if (part.startsWith("Location: ")) location = part.substring("Location: ".length());
                    else if (part.startsWith("Meet Link: ")) meetLink = part.substring("Meet Link: ".length());
                    else if (part.startsWith("Notes: ")) notes = part.substring("Notes: ".length());
                    else if (!part.contains(": ")) prefixMsg = part;
                }

                String headerTitle = "INTERVIEW_INVITE".equals(subject) ? "Interview Invitation" : "Interview Update";
                helper.setSubject("HireConnect: " + headerTitle + " - " + jobTitle);
                htmlBody = """
                    <!doctype html>
                    <html>
                      <body style="font-family: 'Inter', sans-serif; background-color: #f4f7f6; color: #1a202c; line-height: 1.6; margin: 0; padding: 40px 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
                          <div style="background: linear-gradient(135deg, #4f46e5 0%%, #9333ea 100%%); padding: 40px 20px; text-align: center; border-radius: 16px 16px 0 0;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 28px;">%s</h2>
                            <p style="color: #e0e7ff; font-size: 16px;">%s</p>
                          </div>
                          <div style="padding: 40px 32px;">
                            <table style="width: 100%%; border-collapse: collapse;">
                              <tr><td style="padding: 10px 0; border-bottom: 1px solid #eee;"><strong>Role:</strong> %s</td></tr>
                              <tr><td style="padding: 10px 0; border-bottom: 1px solid #eee;"><strong>Date:</strong> %s</td></tr>
                              <tr><td style="padding: 10px 0; border-bottom: 1px solid #eee;"><strong>Mode:</strong> %s</td></tr>
                              <tr><td style="padding: 10px 0; border-bottom: 1px solid #eee;"><strong>Link:</strong> %s</td></tr>
                              <tr><td style="padding: 10px 0;"><strong>Notes:</strong> %s</td></tr>
                            </table>
                            <div style="text-align: center; margin-top: 30px;">
                              <a href="http://localhost:3000/dashboard" style="background-color: #4f46e5; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">View Dashboard</a>
                            </div>
                          </div>
                        </div>
                      </body>
                    </html>
                    """.formatted(headerTitle, prefixMsg, jobTitle, scheduledAt, mode, meetLink, notes);
            } else if ("OFFER_RECEIVED".equals(subject) || "OFFERED".equals(subject)) {
                String safeBody = body != null ? body : "";
                helper.setSubject("HireConnect: Congratulations! You have an Offer");
                htmlBody = String.format("""
                    <!doctype html>
                    <html>
                      <body style="font-family: 'Inter', sans-serif; background-color: #f4f7f6; color: #1a202c; padding: 40px 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
                          <div style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 40px 20px; text-align: center; border-radius: 16px 16px 0 0;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 28px;">🎉 Congratulations!</h2>
                          </div>
                          <div style="padding: 40px 32px; text-align: center;">
                            <p style="font-size: 16px; color: #475569;">%s</p>
                            <div style="margin-top: 30px;">
                              <a href="http://localhost:3000/dashboard" style="background-color: #10b981; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">View Dashboard</a>
                            </div>
                          </div>
                        </div>
                      </body>
                    </html>
                """, safeBody);
            } else {
                String safeBody = body != null ? body : "";
                htmlBody = String.format("""
                    <!doctype html>
                    <html>
                      <body style="font-family: 'Inter', sans-serif; background-color: #f4f7f6; color: #1a202c; padding: 40px 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.1);">
                          <div style="background-color: #4f46e5; padding: 20px; text-align: center; border-radius: 16px 16px 0 0;">
                            <h2 style="color: #ffffff; margin: 0;">%s</h2>
                          </div>
                          <div style="padding: 30px;">
                            <p>%s</p>
                            <div style="text-align: center; margin-top: 20px;">
                              <a href="http://localhost:3000/dashboard" style="background-color: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px;">Go to Dashboard</a>
                            </div>
                          </div>
                        </div>
                      </body>
                    </html>
                """, subject, safeBody);
            }
                
            helper.setText(htmlBody, true);

            if (attachment != null && fileName != null) {
                helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(attachment));
            }

            emailSender.send(message);
            log.info(">>>> HTML Email SUCCESSFULLY sent to: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public List<Notification> getByType(String type) {
        return notifRepo.findByType(type);
    }

    @Override
    public long getUnreadCount(int userId) {
        return notifRepo.countByUserIdAndIsRead(userId, false);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notifRepo.findAll();
    }
}