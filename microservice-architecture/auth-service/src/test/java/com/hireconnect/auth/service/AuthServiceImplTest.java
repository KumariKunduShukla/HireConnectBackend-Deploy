package com.hireconnect.auth.service;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.util.JwtUtil;
import com.hireconnect.auth.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.mail.internet.MimeMessage;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpUtil otpUtil;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "mailFrom", "test@hireconnect.com");
        ReflectionTestUtils.setField(authService, "adminEmail", "admin@hireconnect.com");
        ReflectionTestUtils.setField(authService, "adminPassword", "adminpass");
        ReflectionTestUtils.setField(authService, "githubClientId", "client-id");
        ReflectionTestUtils.setField(authService, "githubClientSecret", "client-secret");
    }

    @Test
    void bootstrapAdmin_AdminExists() {
        when(authRepository.findByEmail("admin@hireconnect.com")).thenReturn(Optional.of(new UserCredential()));
        authService.bootstrapAdmin();
        verify(authRepository, never()).save(any(UserCredential.class));
    }

    @Test
    void bootstrapAdmin_AdminDoesNotExist() {
        when(authRepository.findByEmail("admin@hireconnect.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("adminpass")).thenReturn("encodedPass");
        authService.bootstrapAdmin();
        verify(authRepository, times(1)).save(any(UserCredential.class));
    }

    @Test
    void register_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        user.setPassword("SecurePassword123!");

        when(authRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded");
        when(authRepository.save(any(UserCredential.class))).thenReturn(user);

        UserCredential saved = authService.register(user);

        assertEquals("CANDIDATE", saved.getRole());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("encoded", saved.getPassword());
    }

    @Test
    void register_EmailExists() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        when(authRepository.existsByEmail("test@test.com")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.register(user));
    }

    @Test
    void registerAndSendOtp_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        user.setPassword("SecurePassword123!");

        when(authRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.registerAndSendOtp(user);

        assertEquals("OTP sent successfully", result);
        verify(valueOperations).set(eq("otp:test@test.com"), eq("123456"), eq(10L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq("pending_pass:test@test.com"), eq("SecurePassword123!"), eq(10L), eq(TimeUnit.MINUTES));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void verifyAndSaveUser_Success() {
        UserCredential user = new UserCredential();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@test.com")).thenReturn("123456");
        when(valueOperations.get("pending_pass:test@test.com")).thenReturn("SecurePassword123!");
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded");

        String result = authService.verifyAndSaveUser("test@test.com", "123456", user);

        assertEquals("User registered successfully!", result);
        assertEquals("encoded", user.getPassword());
        verify(authRepository).save(user);
        verify(redisTemplate).delete("otp:test@test.com");
    }

    @Test
    void verifyAndSaveUser_InvalidOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@test.com")).thenReturn("123456");

        assertThrows(RuntimeException.class, () -> authService.verifyAndSaveUser("test@test.com", "wrong", new UserCredential()));
    }

    @Test
    void login_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole("CANDIDATE");
        user.setStatus("ACTIVE");

        when(authRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken("test@test.com", "CANDIDATE", 0)).thenReturn("jwt");

        String token = authService.login("test@test.com", "raw");
        assertEquals("jwt", token);
    }

    @Test
    void login_WrongPassword() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setStatus("ACTIVE");

        when(authRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login("test@test.com", "wrong"));
    }

    @Test
    void login_PendingStatus() {
        UserCredential user = new UserCredential();
        user.setEmail("test@test.com");
        user.setStatus("PENDING_APPROVAL");

        when(authRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertThrows(RuntimeException.class, () -> authService.login("test@test.com", "pass"));
    }

    @Test
    void forgotPasswordSendOtp_Success() {
        when(authRepository.findByEmail("test@test.com")).thenReturn(Optional.of(new UserCredential()));
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.forgotPasswordSendOtp("test@test.com");
        assertEquals("OTP sent to your email", result);
    }

    @Test
    void resetPassword_Success() {
        UserCredential user = new UserCredential();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("forgot_otp:test@test.com")).thenReturn("123456");
        when(authRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded");

        String result = authService.resetPassword("test@test.com", "123456", "SecurePassword123!");
        assertEquals("Password successfully reset", result);
        verify(authRepository).save(user);
    }

    @Test
    void validateToken_Valid() {
        doNothing().when(jwtUtil).validateToken("token");
        assertTrue(authService.validateToken("token"));
    }

    @Test
    void validateToken_Invalid() {
        doThrow(new RuntimeException()).when(jwtUtil).validateToken("token");
        assertFalse(authService.validateToken("token"));
    }

    @Test
    void addRecruiter_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded");

        UserCredential newRecruiter = new UserCredential();
        newRecruiter.setPassword("SecurePassword123!");

        when(authRepository.save(any(UserCredential.class))).thenReturn(newRecruiter);

        UserCredential saved = authService.addRecruiter(1, newRecruiter);
        assertEquals("RECRUITER", saved.getRole());
    }

    @Test
    void recruiterApplyForRegistration_Success() {
        UserCredential admin = new UserCredential();
        admin.setEmail("admin@test.com");

        UserCredential recruiter = new UserCredential();
        recruiter.setEmail("recruiter@test.com");

        when(authRepository.existsByEmail("recruiter@test.com")).thenReturn(false);
        when(passwordEncoder.encode("NOT_SET_YET")).thenReturn("encoded");
        when(authRepository.save(recruiter)).thenReturn(recruiter);
        when(authRepository.findByRole("ADMIN")).thenReturn(List.of(admin));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.recruiterApplyForRegistration(recruiter);
        assertEquals("Application submitted. Awaiting admin approval.", result);
        assertEquals("PENDING_APPROVAL", recruiter.getStatus());
    }

    @Test
    void approveRecruiter_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        UserCredential recruiter = new UserCredential();
        recruiter.setRole("RECRUITER");
        recruiter.setEmail("rec@test.com");

        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findById(2)).thenReturn(Optional.of(recruiter));
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.approveRecruiter(1, 2);
        assertEquals("Recruiter approved and notified via email.", result);
        assertEquals("APPROVED", recruiter.getStatus());
    }

    @Test
    void rejectRecruiter_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        UserCredential recruiter = new UserCredential();
        recruiter.setRole("RECRUITER");

        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findById(2)).thenReturn(Optional.of(recruiter));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.rejectRecruiter(1, 2);
        assertEquals("Recruiter rejected and notified via email.", result);
        assertEquals("REJECTED", recruiter.getStatus());
    }

    @Test
    void loginWithGithub_Success() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("access_token", "gh-token"), HttpStatus.OK));
        
        when(restTemplate.exchange(eq("https://api.github.com/user"), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("email", "gh@test.com", "login", "ghuser", "id", 123), HttpStatus.OK));
        
        when(authRepository.findByEmail("gh@test.com")).thenReturn(Optional.empty());
        when(authRepository.save(any(UserCredential.class))).thenAnswer(i -> i.getArguments()[0]);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt");

        String result = authService.loginWithGithub("code");
        assertEquals("jwt", result);
    }

    @Test
    void requestEmailChange_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("old@test.com");
        when(authRepository.findByEmail("old@test.com")).thenReturn(Optional.of(user));
        when(authRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.requestEmailChange("old@test.com", "new@test.com");
        assertEquals("OTP sent to your new email address", result);
    }

    @Test
    void verifyEmailChange_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("old@test.com");
        when(authRepository.findByEmail("old@test.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email_change:old@test.com")).thenReturn("new@test.com:123456");

        String result = authService.verifyEmailChange("old@test.com", "123456");
        assertEquals("Email successfully updated", result);
        assertEquals("new@test.com", user.getEmail());
    }

    @Test
    void recruiterSetPassword_Success() {
        UserCredential user = new UserCredential();
        user.setEmail("rec@test.com");
        user.setStatus("APPROVED");
        when(authRepository.findByEmail("rec@test.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:approve:rec@test.com")).thenReturn("123456");
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded");

        String result = authService.recruiterSetPassword("rec@test.com", "123456", "SecurePassword123!");
        assertEquals("Password set successfully! You can now sign in.", result);
        assertEquals("encoded", user.getPassword());
    }

    @Test
    void resolveAdminUserIdFromEmail_Success() {
        UserCredential admin = new UserCredential();
        admin.setUserId(1);
        admin.setRole("ADMIN");
        when(authRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        
        int result = authService.resolveAdminUserIdFromEmail("admin@test.com");
        assertEquals(1, result);
    }

    @Test
    void resolveAdminUserIdFromEmail_NotAdmin() {
        UserCredential user = new UserCredential();
        user.setRole("CANDIDATE");
        when(authRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        
        assertThrows(RuntimeException.class, () -> authService.resolveAdminUserIdFromEmail("user@test.com"));
    }

    @Test
    void approveRecruiterFromEmailToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1:2");
        
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        UserCredential recruiter = new UserCredential();
        recruiter.setRole("RECRUITER");
        recruiter.setEmail("rec@test.com");
        
        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findById(2)).thenReturn(Optional.of(recruiter));
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.approveRecruiterFromEmailToken("valid-token");
        assertEquals("Recruiter approved and notified via email.", result);
    }

    @Test
    void rejectRecruiterFromEmailToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1:2");
        
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        UserCredential recruiter = new UserCredential();
        recruiter.setRole("RECRUITER");
        
        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findById(2)).thenReturn(Optional.of(recruiter));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.rejectRecruiterFromEmailToken("valid-token", "Reason");
        assertEquals("Recruiter rejected and notified via email.", result);
    }

    @Test
    void getPendingRecruiters_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findByRoleAndStatus("RECRUITER", "PENDING_APPROVAL")).thenReturn(List.of(new UserCredential()));
        
        List<UserCredential> result = authService.getPendingRecruiters(1);
        assertFalse(result.isEmpty());
    }

    @Test
    void getAllUsersByRole_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findByRole("RECRUITER")).thenReturn(List.of(new UserCredential()));
        
        List<UserCredential> result = authService.getAllUsersByRole(1, "RECRUITER");
        assertFalse(result.isEmpty());
    }

    @Test
    void createInitialAdmin_Success() {
        when(authRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(authRepository.save(any(UserCredential.class))).thenAnswer(i -> i.getArguments()[0]);
        
        UserCredential result = authService.createInitialAdmin();
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void approveRecruiterFromEmailToken_InvalidToken() {
        assertThrows(RuntimeException.class, () -> authService.approveRecruiterFromEmailToken(null));
        assertThrows(RuntimeException.class, () -> authService.approveRecruiterFromEmailToken(""));
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.approveRecruiterFromEmailToken("expired"));
    }

    @Test
    void rejectRecruiterFromEmailToken_InvalidToken() {
        assertThrows(RuntimeException.class, () -> authService.rejectRecruiterFromEmailToken(null, "reason"));
        assertThrows(RuntimeException.class, () -> authService.rejectRecruiterFromEmailToken(" ", "reason"));
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.rejectRecruiterFromEmailToken("expired", "reason"));
    }

    @Test
    void login_UserNotFound() {
        when(authRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.login("none@test.com", "pass"));
    }

    @Test
    void login_RejectedStatus() {
        UserCredential user = new UserCredential();
        user.setStatus("REJECTED");
        when(authRepository.findByEmail("rej@test.com")).thenReturn(Optional.of(user));
        assertThrows(RuntimeException.class, () -> authService.login("rej@test.com", "pass"));
    }

    @Test
    void forgotPasswordSendOtp_EmailNotFound() {
        when(authRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.forgotPasswordSendOtp("none@test.com"));
    }

    @Test
    void resetPassword_InvalidOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("forgot_otp:test@test.com")).thenReturn("123456");
        assertThrows(RuntimeException.class, () -> authService.resetPassword("test@test.com", "wrong", "Pass123!"));
    }

    @Test
    void registerAndSendOtp_EmailExists() {
        UserCredential user = new UserCredential();
        user.setEmail("exists@test.com");
        when(authRepository.existsByEmail("exists@test.com")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.registerAndSendOtp(user));
    }

    @Test
    void verifyAndSaveUser_OtpNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@test.com")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.verifyAndSaveUser("test@test.com", "123", new UserCredential()));
    }

    @Test
    void verifyAndSaveUser_PendingPassNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:test@test.com")).thenReturn("123456");
        when(valueOperations.get("pending_pass:test@test.com")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.verifyAndSaveUser("test@test.com", "123456", new UserCredential()));
    }

    @Test
    void requestEmailChange_NewEmailInUse() {
        when(authRepository.existsByEmail("new@test.com")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.requestEmailChange("old@test.com", "new@test.com"));
    }

    @Test
    void verifyEmailChange_PayloadNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.verifyEmailChange("old@test.com", "123"));
    }

    @Test
    void verifyEmailChange_InvalidPayloadFormat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("onlyOnePart");
        assertThrows(RuntimeException.class, () -> authService.verifyEmailChange("old@test.com", "123"));
    }

    @Test
    void recruiterApplyForRegistration_NoAdmins() {
        UserCredential recruiter = new UserCredential();
        recruiter.setEmail("rec@test.com");
        when(authRepository.existsByEmail(anyString())).thenReturn(false);
        when(authRepository.save(any())).thenReturn(recruiter);
        when(authRepository.findByRole("ADMIN")).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> authService.recruiterApplyForRegistration(recruiter));
    }

    @Test
    void recruiterSetPassword_OtpNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> authService.recruiterSetPassword("rec@test.com", "123", "Pass123!"));
    }

    @Test
    void recruiterSetPassword_NotApproved() {
        UserCredential recruiter = new UserCredential();
        recruiter.setStatus("PENDING");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");
        when(authRepository.findByEmail(anyString())).thenReturn(Optional.of(recruiter));
        assertThrows(RuntimeException.class, () -> authService.recruiterSetPassword("rec@test.com", "123456", "Pass123!"));
    }

    @Test
    void loginWithGithub_ExchangeFailed() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("error", "invalid_code"), HttpStatus.OK));
        assertThrows(RuntimeException.class, () -> authService.loginWithGithub("code"));
    }

    @Test
    void rejectRecruiterWithReason_Success() {
        UserCredential admin = new UserCredential();
        admin.setRole("ADMIN");
        UserCredential recruiter = new UserCredential();
        recruiter.setRole("RECRUITER");
        recruiter.setEmail("rec@test.com");

        when(authRepository.findById(1)).thenReturn(Optional.of(admin));
        when(authRepository.findById(2)).thenReturn(Optional.of(recruiter));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String result = authService.rejectRecruiterWithReason(1, 2, "Insufficient experience");
        assertEquals("Recruiter rejected and notified via email.", result);
        assertEquals("REJECTED", recruiter.getStatus());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void loginWithGithub_EmailFallback() {
        // Mock token exchange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("access_token", "gh-token"), HttpStatus.OK));
        
        // Mock user details WITHOUT email
        when(restTemplate.exchange(eq("https://api.github.com/user"), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Map.of("login", "ghuser", "id", 123), HttpStatus.OK));
        
        // Mock emails API
        List<Map<String, Object>> emailList = List.of(
            Map.of("email", "fallback@test.com", "verified", true, "primary", true)
        );
        when(restTemplate.exchange(eq("https://api.github.com/user/emails"), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(emailList, HttpStatus.OK));

        when(authRepository.findByEmail("fallback@test.com")).thenReturn(Optional.empty());
        when(authRepository.save(any(UserCredential.class))).thenAnswer(i -> i.getArguments()[0]);
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt");

        String result = authService.loginWithGithub("code");
        assertEquals("jwt", result);
    }

    @Test
    void fetchEmailFromGithubApi_Error() {
        when(restTemplate.exchange(eq("https://api.github.com/user/emails"), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("API Down"));
        
        // This is private, but called via resolveGithubUserEmail fallback
        // We'll test it indirectly or just ensure no crash
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(authService, "fetchEmailFromGithubApi", new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders())));
    }

    @Test
    void approveRecruiterFromEmailToken_InvalidPayload() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("invalid-payload"); // No colon
        
        assertThrows(RuntimeException.class, () -> authService.approveRecruiterFromEmailToken("token"));
    }

    @Test
    void rejectRecruiterFromEmailToken_InvalidPayload() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1:2:3"); // Too many parts
        
        assertThrows(RuntimeException.class, () -> authService.rejectRecruiterFromEmailToken("token", "reason"));
    }

    @Test
    void logout_NoOp() {
        authService.logout("token");
        // No exception means pass
    }

    @Test
    void refreshToken_ReturnsSame() {
        assertEquals("token", authService.refreshToken("token"));
    }

    @Test
    void createInitialAdmin_AlreadyExists() {
        when(authRepository.findByEmail(anyString())).thenReturn(Optional.of(new UserCredential()));
        assertThrows(RuntimeException.class, () -> authService.createInitialAdmin());
    }
}
