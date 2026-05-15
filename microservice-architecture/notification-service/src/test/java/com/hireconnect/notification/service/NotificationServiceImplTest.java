package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.exception.NotificationNotFoundException;
import com.hireconnect.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private org.springframework.mail.javamail.JavaMailSender emailSender;

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    @Spy
    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    void sendNotification_NoEmail() {
        Notification n = new Notification();
        n.setUserId(1);
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("CANDIDATE");
        when(repository.save(any())).thenReturn(n);

        service.sendNotification(n);
        verify(repository, times(1)).save(n);
        // Email is never sent in the new implementation
    }

    @Test
    void sendNotification_Success() {
        Notification n = new Notification();
        n.setUserId(1);
        n.setRecipientEmail("test@test.com");
        n.setType("INTERVIEW_INVITE");
        n.setMessage("Job Title: Dev|Scheduled At: Tomorrow|Mode: ONLINE|Location: NY|Meet Link: http://link|Notes: None");
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("CANDIDATE");
        when(repository.save(any())).thenReturn(n);
        
        service.sendNotification(n);
        verify(repository, times(1)).save(n);
        // Verify email alert is skipped (In-app ONLY)
        verify(service, never()).sendEmailAlert(anyString(), anyString(), anyString());
    }

    @Test
    void sendNotification_Admin_Skipped() {
        Notification n = new Notification();
        n.setUserId(99);
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("ADMIN");
        
        service.sendNotification(n);
        
        verify(repository, never()).save(any());
    }

    @Test
    void sendNotification_MailException_Ignored_Since_Email_Disabled() {
        Notification n = new Notification();
        n.setUserId(1);
        n.setRecipientEmail("test@test.com");
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("CANDIDATE");
        when(repository.save(any())).thenReturn(n);
        
        // This won't even be called now
        assertDoesNotThrow(() -> service.sendNotification(n));
        verify(repository, times(1)).save(n);
    }

    @Test
    void markAsRead_Success() {
        Notification n = new Notification();
        when(repository.findById(1)).thenReturn(Optional.of(n));
        
        service.markAsRead(1);
        assertTrue(n.isRead());
        verify(repository, times(1)).save(n);
    }

    @Test
    void markAsRead_NotFound() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotificationNotFoundException.class, () -> service.markAsRead(1));
    }

    @Test
    void markAllRead() {
        doNothing().when(repository).markAllAsReadByUserId(1);
        service.markAllRead(1);
        verify(repository, times(1)).markAllAsReadByUserId(1);
    }

    @Test
    void getByUser() {
        when(repository.findByUserId(1)).thenReturn(List.of(new Notification()));
        assertEquals(1, service.getByUser(1).size());
    }

    @Test
    void deleteNotification_Success() {
        when(repository.existsById(1)).thenReturn(true);
        doNothing().when(repository).deleteById(1);
        service.deleteNotification(1);
        verify(repository, times(1)).deleteById(1);
    }

    @Test
    void deleteNotification_NotFound() {
        when(repository.existsById(1)).thenReturn(false);
        assertThrows(NotificationNotFoundException.class, () -> service.deleteNotification(1));
    }

    @Test
    void getByType() {
        when(repository.findByType("Alert")).thenReturn(List.of(new Notification()));
        assertEquals(1, service.getByType("Alert").size());
    }

    @Test
    void getUnreadCount() {
        when(repository.countByUserIdAndIsRead(1, false)).thenReturn(5L);
        assertEquals(5L, service.getUnreadCount(1));
    }

    @Test
    void sendEmailAlert_Interview() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "INTERVIEW_INVITE", "Job Title: Dev|Scheduled At: Tomorrow|Mode: ONLINE|Location: NY|Meet Link: http://link|Notes: None");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_Interview_MissingParts() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "INTERVIEW_UPDATE", "Some message without parts");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_Other() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "OTHER", "Normal body\nWith newline");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_OfferReceived() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "OFFER_RECEIVED", "You got an offer!");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_JoiningInstructions() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "JOINING_INSTRUCTIONS", "Join here");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_NewApplication() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "NEW_APPLICATION", "New app");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_CandidateJoined() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "CANDIDATE_JOINED", "Candidate joined");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailAlert_DefaultTemplate() {
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        when(emailSender.createMimeMessage()).thenReturn(msg);

        service.sendEmailAlert("test@test.com", "GENERAL", "Hello\nWorld");
        verify(emailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendNotification_AuthService_Failure() {
        Notification n = new Notification();
        n.setUserId(1);
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("Auth service down"));
        when(repository.save(any())).thenReturn(n);

        assertDoesNotThrow(() -> service.sendNotification(n));
        verify(repository, times(1)).save(n);
    }

    @Test
    void sendEmailAlert_Exception() {
        when(emailSender.createMimeMessage()).thenThrow(new RuntimeException("error"));
        assertDoesNotThrow(() -> service.sendEmailAlert("test@test.com", "OTHER", "body"));
    }
}
