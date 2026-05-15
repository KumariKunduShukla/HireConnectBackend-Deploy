package com.hireconnect.application.service;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ApplicationServiceImpl service;

    @Test
    void submitApplication_Success() {
        Application app = new Application();
        app.setJobId(1);
        app.setCandidateId(10);
        
        when(repository.findFirstByJobIdAndCandidateId(1, 10)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(app);

        // Mock email fetching
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "candidate@test.com"));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Dev", "postedBy", 20));
        
        // Mock role check for notification-filtering
        when(restTemplate.getForObject(contains("auth-service/auth/users/"), eq(String.class)))
                .thenReturn("CANDIDATE");

        Application res = service.submitApplication(app);
        assertEquals(1, res.getJobId());
        
        // 2 notifications should be sent: candidate and recruiter via RabbitMQ
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void submitApplication_Duplicate() {
        Application app = new Application();
        app.setJobId(1);
        app.setCandidateId(10);
        
        when(repository.findFirstByJobIdAndCandidateId(1, 10)).thenReturn(Optional.of(app));

        assertThrows(RuntimeException.class, () -> service.submitApplication(app));
    }

    @Test
    void getByCandidate() {
        when(repository.findByCandidateId(1)).thenReturn(List.of(new Application()));
        assertEquals(1, service.getByCandidate(1).size());
    }

    @Test
    void getByJob() {
        when(repository.findByJobId(1)).thenReturn(List.of(new Application()));
        assertEquals(1, service.getByJob(1).size());
    }

    @Test
    void updateStatus_Success() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        // Fallback auth service email mock
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class))).thenThrow(new RuntimeException());
        when(restTemplate.getForObject(contains("auth-service/auth/users/1/email"), eq(String.class))).thenReturn("test@test.com");
        when(restTemplate.getForObject(contains("auth-service/auth/users/1/role"), eq(String.class))).thenReturn("CANDIDATE");

        Application res = service.updateStatus(1, "Accepted");
        assertEquals("Accepted", res.getStatus());
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateStatus_NotFound() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateStatus(1, "Accepted"));
    }

    @Test
    void updateStatus_Offered() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "candidate@test.com"));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Software Engineer"));

        Application res = service.updateStatus(1, "Offered");
        assertEquals("Offered", res.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateStatus_Joined() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "test@test.com"));
        
        // RecruiterId fallback
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Dev", "recruiterId", 50));

        Application res = service.updateStatus(1, "Joined");
        assertEquals("Joined", res.getStatus());
        
        // 2 for candidate (OFFER_ACCEPTED, JOINING_INSTRUCTIONS), 1 for recruiter (CANDIDATE_JOINED)
        when(restTemplate.getForObject(contains("auth-service/auth/users/1/role"), eq(String.class))).thenReturn("CANDIDATE");
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateStatus_InvalidRecruiterId() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "test@test.com"));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Dev", "postedBy", "not-a-number"));

        Application res = service.updateStatus(1, "Joined");
        assertEquals("Joined", res.getStatus());
        // Should not crash, just skip recruiter notification
    }

    @Test
    void withdrawApplication() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        service.withdrawApplication(1);
        assertEquals("Withdrawn", app.getStatus());
    }

    @Test
    void getById() {
        when(repository.findById(1)).thenReturn(Optional.of(new Application()));
        assertTrue(service.getById(1).isPresent());
    }

    @Test
    void countByJobId() {
        when(repository.countByJobId(1)).thenReturn(5);
        assertEquals(5, service.countByJobId(1));
    }

    @Test
    void submitApplication_JobService_Failure() {
        Application app = new Application();
        app.setJobId(1);
        app.setCandidateId(10);
        
        when(repository.findFirstByJobIdAndCandidateId(1, 10)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(app);
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class))).thenThrow(new RuntimeException("Job service down"));

        Application res = service.submitApplication(app);
        assertNotNull(res);
        // Should log warning but not fail
    }

    @Test
    void updateStatus_AuthService_Failure() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        when(restTemplate.getForObject(contains("auth-service/auth/users/1/role"), eq(String.class))).thenThrow(new RuntimeException("Auth service down"));

        assertDoesNotThrow(() -> service.updateStatus(1, "Accepted"));
    }

    @Test
    void sendNotification_CatchBlock() {
        // This is private, but called via public methods.
        // We'll trigger it by making rabbitTemplate throw.
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        doThrow(new RuntimeException("Rabbit down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> service.updateStatus(1, "Accepted"));
    }

    @Test
    void getAllApplications() {
        when(repository.findAll()).thenReturn(List.of(new Application()));
        assertEquals(1, service.getAllApplications().size());
    }

    @Test
    void getEmailFromProfile_CriticalFailure() {
        // Both fail
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class))).thenThrow(new RuntimeException());
        when(restTemplate.getForObject(contains("auth-service"), eq(String.class))).thenThrow(new RuntimeException());
        
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        // Should not crash, just log error
        assertDoesNotThrow(() -> service.updateStatus(1, "Accepted"));
    }

    @Test
    void submitApplication_NonCandidateRole() {
        Application app = new Application();
        app.setJobId(1);
        app.setCandidateId(10);
        
        when(repository.findFirstByJobIdAndCandidateId(1, 10)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(app);

        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class)))
                .thenReturn(Map.of("email", "admin@test.com"));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class)))
                .thenReturn(Map.of("title", "Dev", "postedBy", 20));
        when(restTemplate.getForObject(contains("auth-service/auth/users/"), eq(String.class)))
                .thenReturn("ADMIN");

        service.submitApplication(app);
        
        // Candidate notification should NOT be sent, but recruiter notification should (if recruiter exists)
        // Recruiter is 20. Let's mock recruiter profile.
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendNotification_EmptyEmail() {
        Application app = new Application();
        app.setCandidateId(1);
        app.setJobId(2);
        when(repository.findById(1)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);

        // Mock empty email
        when(restTemplate.getForObject(contains("profile-service"), eq(Map.class))).thenReturn(Map.of("email", ""));
        when(restTemplate.getForObject(contains("job-service"), eq(Map.class))).thenReturn(Map.of("title", "Dev"));
        
        service.updateStatus(1, "Accepted");
        // Now it SHOULD send even if email is empty (using placeholder)
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
