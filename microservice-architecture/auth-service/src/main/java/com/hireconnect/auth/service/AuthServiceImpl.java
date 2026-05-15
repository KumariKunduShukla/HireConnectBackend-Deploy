package com.hireconnect.auth.service;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.util.JwtUtil;
import com.hireconnect.auth.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hireconnect.auth.exception.*;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String ERR_USER_NOT_FOUND = "User not found";
    private static final String ERR_ADMIN_NOT_FOUND = "Admin not found";
    private static final String ERR_RECRUITER_NOT_FOUND = "Recruiter not found";
    private static final String ERR_EMAIL_EXISTS = "Email already registered!";
    private static final String ERR_INVALID_OTP = "Invalid OTP.";
    
    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_GITHUB = "GITHUB";

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.backend-base-url:http://localhost:8080/api/v1}")
    private String backendBaseUrl;

    private static final String OTP_PREFIX = "otp:";
    private static final String PENDING_PASS = "pending_pass:";
    private static final String FORGOT_PREFIX = "forgot_otp:";
    private static final String EMAIL_CHANGE_PREFIX = "email_change:";
    private static final long OTP_TTL_MIN = 10L;
    private static final long LINK_TTL_DAYS = 7L;
    private static final String LINK_APPROVE = "admin_link:approve:";
    private static final String LINK_REJECT = "admin_link:reject:";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_RECRUITER = "RECRUITER";
    private static final String ROLE_CANDIDATE = "CANDIDATE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    // ================= BOOTSTRAP ADMIN =================
    @PostConstruct
    public void bootstrapAdmin() {
        if (authRepository.findByEmail(adminEmail).isEmpty()) {
            UserCredential admin = new UserCredential();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(ROLE_ADMIN);
            admin.setProvider(PROVIDER_LOCAL);
            admin.setStatus(STATUS_ACTIVE);
            authRepository.save(admin);
            log.info("[HireConnect] Admin account initialised for: {}", adminEmail);
        }
    }

    // ================= EMAIL =================
   
   
     
    private String mailSetupHint() {
        return "Use a Gmail address for MAIL_USERNAME and a 16-character App Password (Google Account → Security → "
                + "2-Step Verification → App passwords) as MAIL_PASSWORD — not your normal Gmail password. "
                + "MAIL_USERNAME must be the same Google account that owns that app password. "
                + "For admin alerts, set ADMIN_EMAIL to the inbox you check (often the same Gmail).";
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[HireConnect] Email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[HireConnect] Email send FAILED to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email to " + toEmail + ". " + mailSetupHint()
                    + " SMTP error: " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[HireConnect] HTML email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[HireConnect] HTML email send FAILED to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email to " + toEmail + ". " + mailSetupHint()
                    + " SMTP error: " + e.getMessage());
        }
    }

    private void sendRecruiterApplicationEmail(UserCredential admin, UserCredential recruiter) {
        String approveToken = UUID.randomUUID().toString();
        String rejectToken = UUID.randomUUID().toString();
        String payload = admin.getUserId() + ":" + recruiter.getUserId();
        redisTemplate.opsForValue().set(LINK_APPROVE + approveToken, payload, LINK_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(LINK_REJECT + rejectToken, payload, LINK_TTL_DAYS, TimeUnit.DAYS);

        String approveUrl = backendBaseUrl + "/auth/admin/approve-recruiter-link?token=" + approveToken;
        String rejectUrl = backendBaseUrl + "/auth/admin/reject-recruiter-link?token=" + rejectToken;

        String html = """
                <!doctype html>
                <html>
                  <body style=\"font-family: Arial, sans-serif; color: #222; line-height: 1.5;\">
                    <h2 style=\"margin-bottom: 8px;\">New Recruiter Application</h2>
                    <p>A new recruiter has applied for registration.</p>

                    <table cellpadding=\"6\" cellspacing=\"0\" style=\"margin: 12px 0 20px; border-collapse: collapse;\">
                      <tr><td><strong>Recruiter ID</strong></td><td>%s</td></tr>
                      <tr><td><strong>Email</strong></td><td>%s</td></tr>
                      <tr><td><strong>Status</strong></td><td>%s</td></tr>
                    </table>

                    <p style=\"margin-bottom: 18px;\">Choose an action:</p>
                    <p>
                      <a href=\"%s\" style=\"display:inline-block;background:#16a34a;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:bold;margin-right:10px;\">Approve</a>
                      <a href=\"%s\" style=\"display:inline-block;background:#dc2626;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:bold;\">Reject</a>
                    </p>

                    <p style=\"font-size: 13px; color: #666; margin-top: 24px;\">
                      If the buttons do not work, open these links:<br/>
                      Approve: <a href=\"%s\">%s</a><br/>
                      Reject: <a href=\"%s\">%s</a>
                    </p>
                  </body>
                </html>
                """.formatted(
                recruiter.getUserId(),
                escapeHtml(recruiter.getEmail()),
                escapeHtml(recruiter.getStatus()),
                approveUrl,
                rejectUrl,
                approveUrl,
                approveUrl,
                rejectUrl,
                rejectUrl
        );

        sendHtmlEmail(admin.getEmail(), "HireConnect - New Recruiter Application", html);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
            throw new RuntimeException("Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character");
        }
    }

    // ================= REGISTER (CANDIDATE) =================
    @Override
    public UserCredential register(UserCredential user) {
        if (authRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(ERR_EMAIL_EXISTS);
        }
        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(ROLE_CANDIDATE);
        user.setProvider(PROVIDER_LOCAL);
        user.setStatus(STATUS_ACTIVE);
        return authRepository.save(user);
    }

    // ================= REGISTER + OTP (CANDIDATE) =================
    @Override
    public String registerAndSendOtp(UserCredential user) {
        if (authRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(ERR_EMAIL_EXISTS);
        }
        validatePassword(user.getPassword());
        String otp = otpUtil.generateOtp();

        // Store OTP and raw password in Redis with 10-minute TTL
        redisTemplate.opsForValue().set(OTP_PREFIX + user.getEmail(), otp, OTP_TTL_MIN, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(PENDING_PASS + user.getEmail(), user.getPassword(), OTP_TTL_MIN, TimeUnit.MINUTES);

        String html = """
                <!doctype html>
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1a202c; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                      <h2 style="color: #3b82f6; margin-top: 0;">Welcome to HireConnect!</h2>
                      <p style="font-size: 16px; color: #475569;">Your verification code is:</p>
                      <div style="background-color: #ffffff; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 16px; margin: 24px auto; width: fit-content;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #0f172a;">%s</span>
                      </div>
                      <p style="font-size: 14px; color: #64748b;">This code is valid for <strong>10 minutes</strong>.</p>
                      <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">If you did not request this, please ignore this email.<br/>&copy; The HireConnect Team</p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);

        // Send email — throws RuntimeException if it fails so frontend gets a real error
        sendHtmlEmail(
                user.getEmail(),
                "HireConnect - Email Verification OTP",
                html
        );

        return "OTP sent successfully";
    }

    // ================= VERIFY OTP =================
    @Override
    public String verifyAndSaveUser(String email, String otp, UserCredential user) {
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);

        // FIXED: throw RuntimeException instead of returning plain String.
        // Original code returned "OTP expired or not found" as HTTP 200 OK — frontend
        // saw a successful response and navigated to login even though nothing was saved.
        // Now returns HTTP 400 Bad Request so frontend shows an actual error toast.
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP expired or not found. Please register again.");
        }
        if (!savedOtp.equals(otp)) {
            throw new InvalidOtpException(ERR_INVALID_OTP + " Please check the code sent to your email.");
        }

        String rawPassword = redisTemplate.opsForValue().get(PENDING_PASS + email);
        if (rawPassword == null) {
            throw new RuntimeException("Session expired. Please register again.");
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(ROLE_CANDIDATE);
        user.setProvider(PROVIDER_LOCAL);
        user.setStatus(STATUS_ACTIVE);
        user.setOtp(savedOtp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MIN));
        authRepository.save(user);

        redisTemplate.delete(OTP_PREFIX + email);
        redisTemplate.delete(PENDING_PASS + email);

        log.info("[HireConnect] User registered successfully: {}", email);
        return "User registered successfully!";
    }

    // ================= FORGOT PASSWORD =================
    @Override
    public String forgotPasswordSendOtp(String email) {
        if (authRepository.findByEmail(email).isEmpty()) {
            throw new RuntimeException("Email not registered!");
        }
        String otp = otpUtil.generateOtp();
        redisTemplate.opsForValue().set(FORGOT_PREFIX + email, otp, OTP_TTL_MIN, TimeUnit.MINUTES);

        String html = """
                <!doctype html>
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1a202c; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                      <h2 style="color: #3b82f6; margin-top: 0;">Password Reset</h2>
                      <p style="font-size: 16px; color: #475569;">Your password reset code is:</p>
                      <div style="background-color: #ffffff; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 16px; margin: 24px auto; width: fit-content;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #0f172a;">%s</span>
                      </div>
                      <p style="font-size: 14px; color: #64748b;">This code is valid for <strong>10 minutes</strong>.</p>
                      <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #ef4444; font-weight: 500;">If you did not request a password reset, please secure your account immediately.</p>
                      <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">&copy; The HireConnect Team</p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);

        sendHtmlEmail(
                email,
                "HireConnect - Password Reset OTP",
                html
        );
        return "OTP sent to your email";
    }

    @Override
    public String resetPassword(String email, String otp, String newPassword) {
        String savedOtp = redisTemplate.opsForValue().get(FORGOT_PREFIX + email);
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP expired or not found. Please request a new one.");
        }
        if (!savedOtp.equals(otp)) {
            throw new InvalidOtpException(ERR_INVALID_OTP);
        }

        UserCredential user = authRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ERR_USER_NOT_FOUND));

        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        authRepository.save(user);

        redisTemplate.delete(FORGOT_PREFIX + email);
        return "Password successfully reset";
    }

    // ================= CHANGE EMAIL =================
    @Override
    public String requestEmailChange(String oldEmail, String newEmail) {
        if (authRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("New email is already in use by another account.");
        }

        String otp = otpUtil.generateOtp();
        String payload = newEmail + ":" + otp;
        redisTemplate.opsForValue().set(EMAIL_CHANGE_PREFIX + oldEmail, payload, OTP_TTL_MIN, TimeUnit.MINUTES);

        String html = """
                <!doctype html>
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1a202c; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                      <h2 style="color: #3b82f6; margin-top: 0;">Email Change Request</h2>
                      <p style="font-size: 16px; color: #475569;">You requested to change your HireConnect account email to this address.<br>Your verification code is:</p>
                      <div style="background-color: #ffffff; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 16px; margin: 24px auto; width: fit-content;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #0f172a;">%s</span>
                      </div>
                      <p style="font-size: 14px; color: #64748b;">This code is valid for <strong>10 minutes</strong>.</p>
                      <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">If you did not request this, please ignore this email.<br/>&copy; The HireConnect Team</p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);

        sendHtmlEmail(
                newEmail,
                "HireConnect - Email Change Verification",
                html
        );
        return "OTP sent to your new email address";
    }

    @Override
    public String verifyEmailChange(String oldEmail, String otp) {
        String payload = redisTemplate.opsForValue().get(EMAIL_CHANGE_PREFIX + oldEmail);
        if (payload == null) {
            throw new RuntimeException("Session expired. Please request the email change again.");
        }
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            redisTemplate.delete(EMAIL_CHANGE_PREFIX + oldEmail);
            throw new RuntimeException("Invalid session payload.");
        }
        String newEmail = parts[0];
        String savedOtp = parts[1];

        if (!savedOtp.equals(otp)) {
            throw new InvalidOtpException(ERR_INVALID_OTP);
        }

        UserCredential user = authRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new UserNotFoundException(ERR_USER_NOT_FOUND));

        user.setEmail(newEmail);
        authRepository.save(user);

        redisTemplate.delete(EMAIL_CHANGE_PREFIX + oldEmail);
        return "Email successfully updated";
    }

    // ================= LOGIN =================
    @Override
    public String login(String email, String rawPassword) {
        UserCredential user = authRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ERR_USER_NOT_FOUND));

        if (STATUS_PENDING.equals(user.getStatus())) {
            throw new RuntimeException("PENDING: Your recruiter account is awaiting admin approval.");
        }
        if (STATUS_REJECTED.equals(user.getStatus())) {
            throw new RuntimeException("REJECTED: Your recruiter application was rejected by admin.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId());
    }

    // ================= LOGOUT =================
    @Override
    public void logout(String token) {
        // Stateless JWT — client simply discards the token
    }

    // ================= VALIDATE TOKEN =================
    @Override
    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String refreshToken(String token) {
        return token;
    }

    // ================= ADMIN: Create admin manually =================
    @Override
    public UserCredential createInitialAdmin() {
        if (authRepository.findByEmail(adminEmail).isEmpty()) {
            UserCredential admin = new UserCredential();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(ROLE_ADMIN);
            admin.setProvider(PROVIDER_LOCAL);
            admin.setStatus(STATUS_ACTIVE);
            return authRepository.save(admin);
        }
        throw new RuntimeException("Admin already exists!");
    }

    // ================= ADMIN: Add recruiter directly =================
    @Override
    public UserCredential addRecruiter(int adminId, UserCredential newRecruiter) {
        UserCredential admin = authRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin allowed");
        }
        validatePassword(newRecruiter.getPassword());
        newRecruiter.setPassword(passwordEncoder.encode(newRecruiter.getPassword()));
        newRecruiter.setRole(ROLE_RECRUITER);
        newRecruiter.setProvider(PROVIDER_LOCAL);
        newRecruiter.setStatus(STATUS_ACTIVE);
        return authRepository.save(newRecruiter);
    }

    // ================= RECRUITER SELF-APPLY =================
    @Override
    public String recruiterApplyForRegistration(UserCredential recruiter) {
        if (authRepository.existsByEmail(recruiter.getEmail())) {
            throw new EmailAlreadyExistsException(ERR_EMAIL_EXISTS);
        }
        recruiter.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        recruiter.setRole(ROLE_RECRUITER);
        recruiter.setProvider(PROVIDER_LOCAL);
        recruiter.setStatus(STATUS_PENDING);
        UserCredential savedRecruiter = authRepository.save(recruiter);

        List<UserCredential> admins = authRepository.findByRole(ROLE_ADMIN);
        if (admins.isEmpty()) {
            authRepository.delete(savedRecruiter);
            throw new RuntimeException(
                    "No ADMIN user exists yet. Set ADMIN_EMAIL and ADMIN_PASSWORD, restart auth-service, then try again.");
        }

        int notified = 0;
        StringBuilder failures = new StringBuilder();
        for (UserCredential admin : admins) {
            try {
                sendRecruiterApplicationEmail(admin, savedRecruiter);
                notified++;
            } catch (Exception e) {
                log.error("[HireConnect] Could not notify admin {}: {}", admin.getEmail(), e.getMessage());
                failures.append(" [").append(admin.getEmail()).append(": ").append(e.getMessage()).append("]");
            }
        }

        if (notified == 0) {
            authRepository.delete(savedRecruiter);
            throw new RuntimeException(
                    "Could not email any admin about your application (SMTP misconfigured). " + mailSetupHint()
                            + failures);
        }

        return "Application submitted. Awaiting admin approval.";
    }

    // ================= ADMIN: Get pending recruiters =================
    @Override
    public List<UserCredential> getPendingRecruiters(int adminId) {
        UserCredential admin = authRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin can view pending recruiters");
        }
        return authRepository.findByRoleAndStatus(ROLE_RECRUITER, STATUS_PENDING);
    }

    // ================= ADMIN: Approve recruiter =================
    @Override
    public String approveRecruiter(int adminId, int recruiterId) {
        UserCredential admin = authRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin can approve recruiters");
        }

        UserCredential recruiter = authRepository.findById(recruiterId)
                .orElseThrow(() -> new UserNotFoundException(ERR_RECRUITER_NOT_FOUND));
        if (!recruiter.getRole().equalsIgnoreCase(ROLE_RECRUITER)) {
            throw new RuntimeException("User is not a recruiter");
        }

        String otp = otpUtil.generateOtp();
        redisTemplate.opsForValue().set(OTP_PREFIX + "approve:" + recruiter.getEmail(), otp, 60L, TimeUnit.MINUTES);

        recruiter.setStatus(STATUS_APPROVED);
        recruiter.setOtp(otp);
        recruiter.setOtpExpiresAt(LocalDateTime.now().plusMinutes(60));
        authRepository.save(recruiter);

        String html = """
                <!doctype html>
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1a202c; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                      <h2 style="color: #10b981; margin-top: 0;">Application Approved! 🎉</h2>
                      <p style="font-size: 16px; color: #475569;">Congratulations! Your recruiter application has been approved.</p>
                      <p style="font-size: 15px; color: #475569;">To activate your account and set your password, use this OTP:</p>
                      <div style="background-color: #ffffff; border: 2px dashed #cbd5e1; border-radius: 8px; padding: 16px; margin: 24px auto; width: fit-content;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #0f172a;">%s</span>
                      </div>
                      <p style="font-size: 14px; color: #64748b;">This code is valid for <strong>60 minutes</strong>.</p>
                      
                      <div style="text-align: left; background: #ffffff; padding: 16px; border-radius: 8px; margin: 24px 0; border: 1px solid #e2e8f0;">
                        <h4 style="margin: 0 0 10px 0; color: #0f172a;">Next Steps:</h4>
                        <ol style="margin: 0; padding-left: 20px; color: #475569; font-size: 14px;">
                          <li style="margin-bottom: 8px;">Go to <strong>HireConnect &rarr; Recruiter Apply &rarr; "Already approved? Set your password"</strong></li>
                          <li>Enter your email, OTP, and choose a new password.</li>
                        </ol>
                      </div>
                      
                      <h3 style="color: #3b82f6;">Welcome to HireConnect!</h3>
                      <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">&copy; The HireConnect Team</p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);

        sendHtmlEmail(recruiter.getEmail(),
                "HireConnect - Application Approved! 🎉",
                html);

        return "Recruiter approved and notified via email.";
    }

    // ================= ADMIN: Reject recruiter =================
    @Override
    public String rejectRecruiter(int adminId, int recruiterId) {
        return rejectRecruiterWithReason(adminId, recruiterId, "No reason provided");
    }

    // ================= ADMIN: Get all users by role =================
    @Override
    public List<UserCredential> getAllUsersByRole(int adminId, String role) {
        UserCredential admin = authRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin can view all users");
        }
        return authRepository.findByRole(role);
    }

    public String rejectRecruiterWithReason(int adminId, int recruiterId, String reason) {
        UserCredential admin = authRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin can reject recruiters");
        }

        UserCredential recruiter = authRepository.findById(recruiterId)
                .orElseThrow(() -> new UserNotFoundException(ERR_RECRUITER_NOT_FOUND));

        recruiter.setStatus(STATUS_REJECTED);
        recruiter.setOtp(null);
        recruiter.setOtpExpiresAt(null);
        authRepository.save(recruiter);

        String html = """
                <!doctype html>
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1a202c; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #fcfcfc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center;">
                      <h2 style="color: #0f172a; margin-top: 0;">Application Decision</h2>
                      <p style="font-size: 16px; color: #475569;">Thank you for your interest in HireConnect.</p>
                      <p style="font-size: 15px; color: #475569;">Unfortunately, your recruiter application has not been approved at this time.</p>
                      
                      <div style="text-align: left; background: #fee2e2; padding: 16px; border-radius: 8px; margin: 24px 0; border: 1px solid #fca5a5;">
                        <h4 style="margin: 0 0 10px 0; color: #991b1b;">Reason:</h4>
                        <p style="margin: 0; color: #7f1d1d; font-size: 14px;">%s</p>
                      </div>
                      
                      <p style="font-size: 14px; color: #64748b;">If you believe this is an error or wish to appeal, please contact support.</p>
                      <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                      <p style="font-size: 12px; color: #94a3b8; margin-bottom: 0;">&copy; The HireConnect Team</p>
                    </div>
                  </body>
                </html>
                """.formatted(escapeHtml(reason));

        try {
            sendHtmlEmail(recruiter.getEmail(),
                    "HireConnect - Application Decision",
                    html);
        } catch (Exception e) {
            log.error("[HireConnect] Could not send rejection email: {}", e.getMessage());
        }

        return "Recruiter rejected and notified via email.";
    }

    // ================= RECRUITER: Set password after approval =================
    @Override
    public String recruiterSetPassword(String email, String otp, String newPassword) {
        String savedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + "approve:" + email);
        if (savedOtp == null) {
            throw new RuntimeException("OTP expired or not found. Please ask admin to re-approve.");
        }
        if (!savedOtp.equals(otp)) {
            throw new InvalidOtpException(ERR_INVALID_OTP);
        }

        UserCredential recruiter = authRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ERR_RECRUITER_NOT_FOUND));

        if (!STATUS_APPROVED.equals(recruiter.getStatus())) {
            throw new RuntimeException("Account is not approved yet");
        }

        validatePassword(newPassword);
        recruiter.setPassword(passwordEncoder.encode(newPassword));
        recruiter.setStatus(STATUS_ACTIVE);
        recruiter.setOtp(null);
        recruiter.setOtpExpiresAt(null);
        authRepository.save(recruiter);

        redisTemplate.delete(OTP_PREFIX + "approve:" + email);

        return "Password set successfully! You can now sign in.";
    }

    // ================= GITHUB OAuth    @Override
    public String loginWithGithub(String code) {
        if (githubClientId == null || githubClientId.isBlank() || githubClientSecret == null || githubClientSecret.isBlank()) {
            throw new RuntimeException("GitHub OAuth is not configured.");
        }
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Missing OAuth authorization code");
        }

        String accessToken = exchangeGithubToken(code);
        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setBearerAuth(accessToken);
        apiHeaders.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
        HttpEntity<Void> userEntity = new HttpEntity<>(apiHeaders);

        Map<?, ?> ghUser = fetchGithubUserProfile(userEntity);
        String email = resolveGithubUserEmail(ghUser, userEntity);

        UserCredential user = authRepository.findByEmail(email).orElseGet(() -> {
            UserCredential u = new UserCredential();
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            u.setRole(ROLE_CANDIDATE);
            u.setProvider(PROVIDER_GITHUB);
            u.setStatus(STATUS_ACTIVE);
            return authRepository.save(u);
        });

        if (STATUS_PENDING.equals(user.getStatus())) {
            throw new RuntimeException("PENDING: Your recruiter account is awaiting admin approval.");
        }
        if (STATUS_REJECTED.equals(user.getStatus())) {
            throw new RuntimeException("REJECTED: Your recruiter application was rejected by admin.");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId());
    }

    private String exchangeGithubToken(String code) {
        String tokenUrl = "https://github.com/login/oauth/access_token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubClientId);
        form.add("client_secret", githubClientSecret);
        form.add("code", code.trim());

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(form, tokenHeaders),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> tokenBody = tokenResponse.getBody();
        if (tokenBody == null || !tokenBody.containsKey("access_token")) {
            throw new RuntimeException("GitHub token exchange failed: " + (tokenBody != null ? tokenBody.get("error_description") : "Empty response"));
        }
        return (String) tokenBody.get("access_token");
    }

    private Map<?, ?> fetchGithubUserProfile(HttpEntity<Void> userEntity) {
        ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, userEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<?, ?> ghUser = userResponse.getBody();
        if (ghUser == null) {
            throw new RuntimeException("GitHub user profile is empty");
        }
        return ghUser;
    }

    private String resolveGithubUserEmail(Map<?, ?> ghUser, HttpEntity<Void> userEntity) {
        String email = ghUser.get("email") != null ? String.valueOf(ghUser.get("email")) : null;
        if (email == null || email.isBlank() || "null".equalsIgnoreCase(email)) {
            email = fetchEmailFromGithubApi(userEntity);
        }
        if (email == null || email.isBlank()) {
            Object loginStr = ghUser.get("login");
            Object idStr = ghUser.get("id");
            if (loginStr != null && idStr != null) {
                email = loginStr + "_" + idStr + "@github.user.local";
            } else {
                throw new RuntimeException("GitHub did not return a verified email.");
            }
        }
        return email;
    }

    private String fetchEmailFromGithubApi(HttpEntity<Void> userEntity) {
        try {
            ResponseEntity<List<Map<String, Object>>> emailsResponse = restTemplate.exchange(
                    "https://api.github.com/user/emails", HttpMethod.GET, userEntity,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> emails = emailsResponse.getBody();
            if (emails != null) {
                for (Map<String, Object> m : emails) {
                    if (Boolean.TRUE.equals(m.get("verified")) && m.get("email") != null) {
                        String addr = String.valueOf(m.get("email"));
                        if (Boolean.TRUE.equals(m.get("primary"))) return addr;
                    }
                }
            }
        } catch (Exception e) {
            log.error("[HireConnect] Could not fetch GitHub emails: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public int resolveAdminUserIdFromEmail(String email) {
        UserCredential admin = authRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ERR_ADMIN_NOT_FOUND));
        if (!admin.getRole().equalsIgnoreCase(ROLE_ADMIN)) {
            throw new RuntimeException("Only admin allowed");
        }
        return admin.getUserId();
    }

    @Override
    public String approveRecruiterFromEmailToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing token");
        }
        String key = LINK_APPROVE + token.trim();
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            throw new RuntimeException("Invalid or expired approval link.");
        }
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            redisTemplate.delete(key);
            throw new RuntimeException("Invalid approval link.");
        }
        int adminId = Integer.parseInt(parts[0]);
        int recruiterId = Integer.parseInt(parts[1]);
        String result = approveRecruiter(adminId, recruiterId);
        redisTemplate.delete(key);
        return result;
    }

    @Override
    public String rejectRecruiterFromEmailToken(String token, String reason) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing token");
        }
        String key = LINK_REJECT + token.trim();
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            throw new RuntimeException("Invalid or expired rejection link.");
        }
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            redisTemplate.delete(key);
            throw new RuntimeException("Invalid rejection link.");
        }
        int adminId = Integer.parseInt(parts[0]);
        int recruiterId = Integer.parseInt(parts[1]);
        String result = rejectRecruiterWithReason(adminId, recruiterId, reason);
        redisTemplate.delete(key);
        return result;
    }
}