package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.RecruiterResponseDTO;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminResource adminResource;

    @Test
    void addRecruiter() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenReturn(1);
        UserCredential recruiter = new UserCredential();
        when(authService.addRecruiter(1, recruiter)).thenReturn(recruiter);

        ResponseEntity<?> response = adminResource.addRecruiter(authentication, recruiter);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getPendingRecruiters() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenReturn(1);
        when(authService.getPendingRecruiters(1)).thenReturn(List.of());

        ResponseEntity<?> response = adminResource.getPendingRecruiters(authentication);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void approveRecruiter() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenReturn(1);
        when(authService.approveRecruiter(1, 2)).thenReturn("Approved");

        ResponseEntity<String> response = adminResource.approveRecruiter(authentication, 2);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void rejectRecruiter() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenReturn(1);
        when(authService.rejectRecruiterWithReason(1, 2, "No reason provided")).thenReturn("Rejected");

        ResponseEntity<String> response = adminResource.rejectRecruiter(authentication, 2, "No reason provided");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getUsersByRole() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenReturn(1);
        when(authService.getAllUsersByRole(1, "RECRUITER")).thenReturn(List.of());

        ResponseEntity<?> response = adminResource.getUsersByRole(authentication, "RECRUITER");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void approveRecruiterFromEmail() {
        when(authService.approveRecruiterFromEmailToken("token")).thenReturn("Success");
        ResponseEntity<String> response = adminResource.approveRecruiterFromEmail("token");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void rejectRecruiterFromEmail() {
        when(authService.rejectRecruiterFromEmailToken("token", "reason")).thenReturn("Rejected");
        ResponseEntity<String> response = adminResource.rejectRecruiterFromEmail("token", "reason");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void addRecruiter_Exception() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> response = adminResource.addRecruiter(authentication, new UserCredential());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getPendingRecruiters_Exception() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> response = adminResource.getPendingRecruiters(authentication);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getUsersByRole_Exception() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> response = adminResource.getUsersByRole(authentication, "RECRUITER");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void approveRecruiter_Exception() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<String> response = adminResource.approveRecruiter(authentication, 2);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void rejectRecruiter_Exception() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(authService.resolveAdminUserIdFromEmail("admin@test.com")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<String> response = adminResource.rejectRecruiter(authentication, 2, "reason");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void approveRecruiterFromEmail_Exception() {
        when(authService.approveRecruiterFromEmailToken("token")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = adminResource.approveRecruiterFromEmail("token");
        assertEquals(200, response.getStatusCode().value()); // Note: HTML page is returned with 200 OK
    }

    @Test
    void rejectRecruiterFromEmail_Exception() {
        when(authService.rejectRecruiterFromEmailToken("token", "reason")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = adminResource.rejectRecruiterFromEmail("token", "reason");
        assertEquals(200, response.getStatusCode().value()); // Note: HTML page is returned with 200 OK
    }
}
