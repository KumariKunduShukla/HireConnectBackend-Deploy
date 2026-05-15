package com.hireconnect.notification.contoller;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationResourceTest {

    @Mock
    private NotificationService service;

    @InjectMocks
    private NotificationResource resource;

    @Test
    void send() {
        doNothing().when(service).sendNotification(any());
        ResponseEntity<String> r = resource.send(new Notification());
        assertEquals(201, r.getStatusCode().value());
    }

    @Test
    void getByUser() {
        when(service.getByUser(1)).thenReturn(List.of(new Notification()));
        ResponseEntity<List<Notification>> r = resource.getByUser(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void markRead() {
        doNothing().when(service).markAsRead(1);
        ResponseEntity<String> r = resource.markRead(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void markAllRead() {
        doNothing().when(service).markAllRead(1);
        ResponseEntity<String> r = resource.markAllRead(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getByType() {
        when(service.getByType(anyString())).thenReturn(List.of(new Notification()));
        ResponseEntity<List<Notification>> r = resource.getByType("Alert");
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void delete() {
        doNothing().when(service).deleteNotification(1);
        ResponseEntity<String> r = resource.delete(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getUnreadCount() {
        when(service.getUnreadCount(1)).thenReturn(5L);
        ResponseEntity<Long> r = resource.getUnreadCount(1);
        assertEquals(200, r.getStatusCode().value());
        assertEquals(5L, r.getBody());
    }
}
