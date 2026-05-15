package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.repository.InterviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InterviewServiceImpl service;

    @Test
    void scheduleInterview() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Dev"));
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "test@test.com"));
        Interview res = service.scheduleInterview(i);
        assertEquals("SCHEDULED", res.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void confirmInterview_Success() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.findById(1)).thenReturn(Optional.of(i));
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenThrow(new RuntimeException());
        when(restTemplate.getForObject(contains("auth-service"), eq(String.class)))
                .thenReturn("test@test.com");

        Interview res = service.confirmInterview(1);
        assertEquals("CONFIRMED", res.getStatus());
    }

    @Test
    void confirmInterview_NotFound() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.confirmInterview(1));
    }

    @Test
    void rescheduleInterview_Success() {
        Interview i = new Interview();
        when(repository.findById(1)).thenReturn(Optional.of(i));
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "test@test.com"));

        LocalDateTime time = LocalDateTime.now();
        Interview res = service.rescheduleInterview(1, time);
        assertEquals("RESCHEDULED", res.getStatus());
        assertEquals(time, res.getScheduledAt());
    }

    @Test
    void rescheduleInterview_NotFound() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.rescheduleInterview(1, LocalDateTime.now()));
    }

    @Test
    void cancelInterview() {
        Interview i = new Interview();
        when(repository.findById(1)).thenReturn(Optional.of(i));
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "test@test.com"));

        service.cancelInterview(1);
        assertEquals("CANCELLED", i.getStatus());
    }

    @Test
    void scheduleInterview_NotificationFailure() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);

        // Application exists but profile fetch fails
        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenThrow(new RuntimeException("Profile Error"));
        when(restTemplate.getForObject(contains("auth-service"), eq(String.class)))
                .thenReturn(null); // Auth service also fails to return email

        Interview res = service.scheduleInterview(i);
        assertEquals("SCHEDULED", res.getStatus());
        // NOW IT SHOULD STILL SEND (Mandatory Delivery)
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void scheduleInterview_NullApplication() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);
        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(null);

        Interview res = service.scheduleInterview(i);
        assertEquals("SCHEDULED", res.getStatus());
        verify(repository, times(1)).save(any());
        // Should not send notification if application is null
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void scheduleInterview_ApplicationServiceFailure() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);
        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenThrow(new RuntimeException("Application Service Down"));

        Interview res = service.scheduleInterview(i);
        assertEquals("SCHEDULED", res.getStatus());
        // Should catch exception and not crash
    }

    @Test
    void scheduleInterview_JobFetchFailAndAuthFallback() {
        Interview i = new Interview();
        i.setApplicationId(1);
        i.setScheduledAt(LocalDateTime.now());
        i.setNotes("Notes");
        i.setMode("HYBRID");
        i.setLocation("Office");
        i.setMeetLink("zoom.com");
        
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        
        // Job service fails
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenThrow(new RuntimeException("Job Service Down"));
        
        // Profile service returns no email
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("name", "Test"));
        
        // Fallback to auth-service
        when(restTemplate.getForObject(contains("auth-service"), eq(String.class)))
                .thenReturn("fallback@test.com");

        Interview res = service.scheduleInterview(i);
        assertEquals("SCHEDULED", res.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void scheduleInterview_ProfileSuccess() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        
        // Profile service SUCCEEDS
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "profile@test.com"));
        
        Interview res = service.scheduleInterview(i);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void scheduleInterview_FallbackToPlaceholder() {
        Interview i = new Interview();
        i.setApplicationId(1);
        when(repository.save(any())).thenReturn(i);

        when(restTemplate.getForObject(contains("application-service"), eq(Map.class)))
                .thenReturn(Map.of("candidateId", 10, "jobId", 20));
        
        // ALL services fail to return email
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class))).thenThrow(new RuntimeException());
        when(restTemplate.getForObject(contains("auth-service"), eq(String.class))).thenReturn(null);
        
        Interview res = service.scheduleInterview(i);
        // It should still send using the placeholder email
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getByApplication() {
        when(repository.findByApplicationId(1)).thenReturn(List.of(new Interview()));
        assertEquals(1, service.getByApplication(1).size());
    }
}
