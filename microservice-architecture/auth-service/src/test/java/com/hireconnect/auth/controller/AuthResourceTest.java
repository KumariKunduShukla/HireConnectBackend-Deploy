package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.LoginRequestDTO;
import com.hireconnect.auth.dto.OAuthRequestDTO;
import com.hireconnect.auth.entity.TokenRequest;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private com.hireconnect.auth.repository.AuthRepository authRepository;

    @InjectMocks
    private AuthResource authResource;

    @Test
    void register() {
        UserCredential user = new UserCredential();
        when(authService.registerAndSendOtp(any(UserCredential.class))).thenReturn("OTP Sent");

        ResponseEntity<?> response = authResource.register(user);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void recruiterApply() {
        UserCredential user = new UserCredential();
        when(authService.recruiterApplyForRegistration(any(UserCredential.class))).thenReturn("Applied");

        ResponseEntity<String> response = authResource.recruiterApply(user);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Applied", response.getBody());
    }

    @Test
    void verifyOtp() {
        UserCredential user = new UserCredential();
        when(authService.verifyAndSaveUser("test@test.com", "123456", user)).thenReturn("Verified");

        ResponseEntity<?> response = authResource.verifyOtp("test@test.com", "123456", user);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void login() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("test@test.com");
        req.setPassword("pass");
        when(authService.login("test@test.com", "pass")).thenReturn("token");

        ResponseEntity<?> response = authResource.login(req);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void githubLogin() {
        OAuthRequestDTO req = new OAuthRequestDTO();
        req.setCode("code123");
        when(authService.loginWithGithub("code123")).thenReturn("token");

        ResponseEntity<?> response = authResource.githubLogin(req);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void githubLoginCallback() {
        when(authService.loginWithGithub("code123")).thenReturn("token");
        ResponseEntity<Void> response = authResource.githubLoginCallback("code123");
        assertEquals(302, response.getStatusCode().value());
    }

    @Test
    void requestEmailChange() {
        when(authService.requestEmailChange("old", "new")).thenReturn("Success");
        ResponseEntity<String> response = authResource.requestEmailChange("old", "new");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void verifyEmailChange() {
        when(authService.verifyEmailChange("old", "otp")).thenReturn("Success");
        ResponseEntity<String> response = authResource.verifyEmailChange("old", "otp");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void recruiterSetPassword() {
        when(authService.recruiterSetPassword("email", "otp", "pass")).thenReturn("Success");
        ResponseEntity<String> response = authResource.recruiterSetPassword("email", "otp", "pass");
        assertEquals(200, response.getStatusCode().value());
    }

    // --- Exception Tests ---

    @Test
    void githubLogin_Exception() {
        OAuthRequestDTO req = new OAuthRequestDTO();
        req.setCode("code123");
        when(authService.loginWithGithub("code123")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = authResource.githubLogin(req);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void githubLoginCallback_Exception() {
        when(authService.loginWithGithub("code123")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<Void> response = authResource.githubLoginCallback("code123");
        assertEquals(302, response.getStatusCode().value());
    }

    @Test
    void register_Exception() {
        when(authService.registerAndSendOtp(any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.register(new UserCredential());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void verifyOtp_Exception() {
        when(authService.verifyAndSaveUser(anyString(), anyString(), any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.verifyOtp("email", "otp", new UserCredential());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void forgotPassword_Exception() {
        when(authService.forgotPasswordSendOtp(anyString())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = authResource.forgotPassword("email");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void resetPassword_Exception() {
        when(authService.resetPassword(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = authResource.resetPassword("email", "otp", "pass");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void requestEmailChange_Exception() {
        when(authService.requestEmailChange("old", "new")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.requestEmailChange("old", "new");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void verifyEmailChange_Exception() {
        when(authService.verifyEmailChange("old", "otp")).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.verifyEmailChange("old", "otp");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void login_Exception() {
        LoginRequestDTO req = new LoginRequestDTO();
        when(authService.login(any(), any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = authResource.login(req);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void recruiterApply_Exception() {
        when(authService.recruiterApplyForRegistration(any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.recruiterApply(new UserCredential());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void recruiterSetPassword_Exception() {
        when(authService.recruiterSetPassword(any(), any(), any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<String> response = authResource.recruiterSetPassword("email", "otp", "pass");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void forgotPassword() {
        when(authService.forgotPasswordSendOtp("test@test.com")).thenReturn("OTP Sent");

        ResponseEntity<?> response = authResource.forgotPassword("test@test.com");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void resetPassword() {
        when(authService.resetPassword("test@test.com", "123456", "newpass")).thenReturn("Reset");

        ResponseEntity<?> response = authResource.resetPassword("test@test.com", "123456", "newpass");
        assertEquals(200, response.getStatusCode().value());
    }
    @Test
    void getUserEmailById_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        when(authRepository.findByUserId(1)).thenReturn(java.util.Optional.of(user));

        ResponseEntity<String> response = authResource.getUserEmailById(1);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("test@test.com", response.getBody());
    }

    @Test
    void getUserEmailById_NotFound() {
        when(authRepository.findByUserId(1)).thenReturn(java.util.Optional.empty());

        ResponseEntity<String> response = authResource.getUserEmailById(1);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getUserRoleById_Success() {
        UserCredential user = new UserCredential();
        user.setRole("CANDIDATE");
        when(authRepository.findByUserId(1)).thenReturn(java.util.Optional.of(user));

        ResponseEntity<String> response = authResource.getUserRoleById(1);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("CANDIDATE", response.getBody());
    }

    @Test
    void getUserRoleById_NotFound() {
        when(authRepository.findByUserId(1)).thenReturn(java.util.Optional.empty());

        ResponseEntity<String> response = authResource.getUserRoleById(1);
        assertEquals(404, response.getStatusCode().value());
    }
}
