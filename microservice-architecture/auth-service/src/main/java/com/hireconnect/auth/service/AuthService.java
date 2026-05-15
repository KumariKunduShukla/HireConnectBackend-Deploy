package com.hireconnect.auth.service;

import com.hireconnect.auth.entity.UserCredential;
import java.util.List;

public interface AuthService {
    UserCredential register(UserCredential user);
    String login(String email, String password);
    void logout(String token);
    boolean validateToken(String token);
    String refreshToken(String token);

    // Admin management
    UserCredential addRecruiter(int adminId, UserCredential newRecruiter);
    UserCredential createInitialAdmin();

    // Candidate OTP registration
    String registerAndSendOtp(UserCredential user);
    String verifyAndSaveUser(String email, String otp, UserCredential user);

    // Password & Email management
    String forgotPasswordSendOtp(String email);
    String resetPassword(String email, String otp, String newPassword);
    String requestEmailChange(String oldEmail, String newEmail);
    String verifyEmailChange(String oldEmail, String otp);

    // Recruiter self-registration with admin approval flow
    String recruiterApplyForRegistration(UserCredential recruiter);
    List<UserCredential> getPendingRecruiters(int adminId);
    String approveRecruiter(int adminId, int recruiterId);
    String rejectRecruiter(int adminId, int recruiterId);
    String rejectRecruiterWithReason(int adminId, int recruiterId, String reason);
    String recruiterSetPassword(String email, String otp, String newPassword);
    List<UserCredential> getAllUsersByRole(int adminId, String role);

    String loginWithGithub(String code);

    int resolveAdminUserIdFromEmail(String email);

    String approveRecruiterFromEmailToken(String token);

    String rejectRecruiterFromEmailToken(String token, String reason);
}