package com.hireconnect.notification.service;

import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void consume_Success() {
        NotificationEvent event = new NotificationEvent();
        event.setUserId(1);
        event.setRecipientEmail("test@test.com");
        event.setType("WELCOME");
        event.setMessage("Welcome to HireConnect!");

        notificationConsumer.consume(event);

        verify(notificationService, times(1)).sendNotification(any(Notification.class));
    }
}
