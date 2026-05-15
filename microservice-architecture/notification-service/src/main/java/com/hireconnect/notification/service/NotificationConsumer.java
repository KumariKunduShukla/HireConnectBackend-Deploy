package com.hireconnect.notification.service;

import com.hireconnect.notification.config.RabbitMQConfig;
import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(NotificationEvent event) {
        log.info(">>>> RabbitMQ: Received {} notification for user {} (Email: {})", 
                event.getType(), event.getUserId(), event.getRecipientEmail());
        
        try {
            // AUDIT LOG: Write to a file so we can verify RabbitMQ receipt even if DB fails
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("notification_audit.log"),
                String.format("[%s] Received: %s for User %d (Email: %s)\n", 
                    java.time.LocalDateTime.now(), event.getType(), event.getUserId(), event.getRecipientEmail()),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND
            );

            Notification notification = new Notification();
            notification.setUserId(event.getUserId());
            notification.setRecipientEmail(event.getRecipientEmail());
            notification.setType(event.getType());
            notification.setMessage(event.getMessage());
            
            notificationService.sendNotification(notification, event.getAttachmentBytes(), event.getAttachmentName());
            log.info("Successfully processed notification event for user {} (Type: {})", 
                    event.getUserId(), event.getType());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to process notification event from RabbitMQ for user {}. Reason: {}", 
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
