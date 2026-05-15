package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.LoginRequestDTO;
import com.hireconnect.auth.dto.OAuthRequestDTO;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping({"", "/auth"})
public class AuthResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.hireconnect.auth.repository.AuthRepository authRepository;

    @GetMapping("/users/{id}/email")
    public ResponseEntity<String> getUserEmailById(@PathVariable int id) {
        return authRepository.findByUserId(id)
                .map(u -> ResponseEntity.ok(u.getEmail()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/debug/all-users")
    public ResponseEntity<java.util.List<UserCredential>> getAllUsers() {
        return ResponseEntity.ok(authRepository.findAll());
    }

    @GetMapping("/users/{id}/role")
    public ResponseEntity<String> getUserRoleById(@PathVariable int id) {
        return authRepository.findByUserId(id)
                .map(u -> ResponseEntity.ok(u.getRole()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/oauth2/github")
    public ResponseEntity<String> githubLogin(@RequestBody OAuthRequestDTO request) {
        try {
            String jwtToken = authService.loginWithGithub(request.getCode());
            return ResponseEntity.ok(jwtToken);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("GitHub Login Failed: " + e.getMessage());
        }
    }

    @GetMapping("/oauth2/github")
    public ResponseEntity<Void> githubLoginCallback(@RequestParam("code") String code) {
        try {
            String jwtToken = authService.loginWithGithub(code);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .header(org.springframework.http.HttpHeaders.LOCATION, "http://localhost:3000/login?token=" + jwtToken)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .header(org.springframework.http.HttpHeaders.LOCATION, "http://localhost:3000/login?error=" + e.getMessage())
                    .build();
        }
    }

    /**
     * Candidate registration — sends OTP to email for verification.
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserCredential user){
        try {
            String result = authService.registerAndSendOtp(user);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * OTP verification — completes candidate registration.
     * POST /auth/verify-otp?email=...&otp=...
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestBody UserCredential user) {
        try {
            String result = authService.verifyAndSaveUser(email, otp, user);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Forgot Password - Step 1: Send OTP
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            String result = authService.forgotPasswordSendOtp(email);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Forgot Password - Step 2: Reset Password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        try {
            String result = authService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Change Email - Step 1: Request Email Change
     */
    @PostMapping("/request-email-change")
    public ResponseEntity<String> requestEmailChange(
            @RequestParam String oldEmail,
            @RequestParam String newEmail) {
        try {
            String result = authService.requestEmailChange(oldEmail, newEmail);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Change Email - Step 2: Verify Email Change
     */
    @PostMapping("/verify-email-change")
    public ResponseEntity<String> verifyEmailChange(
            @RequestParam String oldEmail,
            @RequestParam String otp) {
        try {
            String result = authService.verifyEmailChange(oldEmail, otp);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Login endpoint — accepts { email, password }.
     * Returns JWT or error message if PENDING/REJECTED.
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
        try {
            String token = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            // Surface PENDING / REJECTED messages directly to frontend
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Recruiter self-registration — status set to PENDING_APPROVAL.
     * POST /auth/recruiter/apply
     */
    @PostMapping("/recruiter/apply")
    public ResponseEntity<String> recruiterApply(@RequestBody UserCredential recruiter) {
        try {
            String result = authService.recruiterApplyForRegistration(recruiter);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Recruiter sets password after receiving OTP via approval email.
     * POST /auth/recruiter/set-password?email=...&otp=...&newPassword=...
     */
    @PostMapping("/recruiter/set-password")
    public ResponseEntity<String> recruiterSetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        try {
            String result = authService.recruiterSetPassword(email, otp, newPassword);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}