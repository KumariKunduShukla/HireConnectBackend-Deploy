package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.RecruiterResponseDTO;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only API endpoints (JWT with role ADMIN), except email action links which use
 */
@RestController
@RequestMapping("/auth/admin")
public class AdminResource {

    @Autowired
    private AuthService authService;

    /**
     * View all recruiters pending admin approval.
     * GET /auth/admin/pending-recruiters — requires Authorization: Bearer (admin JWT).
     */
    @GetMapping("/pending-recruiters")
    public ResponseEntity<Object> getPendingRecruiters(Authentication authentication) {
        try {
            int adminId = authService.resolveAdminUserIdFromEmail(authentication.getName());
            List<UserCredential> pending = authService.getPendingRecruiters(adminId);
            // Return safe DTO — no password hash exposed
            List<RecruiterResponseDTO> dtos = pending.stream()
                    .map(r -> new RecruiterResponseDTO(r.getUserId(), r.getEmail(), r.getStatus(), r.getCreatedAt()))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * View all users by role (e.g. CANDIDATE or RECRUITER).
     * GET /auth/admin/users?role={role} — requires Authorization: Bearer (admin JWT).
     */
    @GetMapping("/users")
    public ResponseEntity<Object> getUsersByRole(Authentication authentication, @RequestParam String role) {
        try {
            int adminId = authService.resolveAdminUserIdFromEmail(authentication.getName());
            List<UserCredential> users = authService.getAllUsersByRole(adminId, role.toUpperCase());
            // Return safe DTO
            List<RecruiterResponseDTO> dtos = users.stream()
                    .map(u -> new RecruiterResponseDTO(u.getUserId(), u.getEmail(), u.getStatus(), u.getCreatedAt()))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Approve a recruiter application. Sends approval email with OTP to recruiter.
     * POST /auth/admin/approve-recruiter?recruiterId={id} — requires admin JWT.
     */
    @PostMapping("/approve-recruiter")
    public ResponseEntity<String> approveRecruiter(
            Authentication authentication,
            @RequestParam int recruiterId) {
        try {
            int adminId = authService.resolveAdminUserIdFromEmail(authentication.getName());
            return ResponseEntity.ok(authService.approveRecruiter(adminId, recruiterId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Reject a recruiter application. Sends rejection email with reason.
     * POST /auth/admin/reject-recruiter?recruiterId={id}&reason={reason} — requires admin JWT.
     */
    @PostMapping("/reject-recruiter")
    public ResponseEntity<String> rejectRecruiter(
            Authentication authentication,
            @RequestParam int recruiterId,
            @RequestParam(required = false, defaultValue = "No reason provided") String reason) {
        try {
            int adminId = authService.resolveAdminUserIdFromEmail(authentication.getName());
            return ResponseEntity.ok(authService.rejectRecruiterWithReason(adminId, recruiterId, reason));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /**
     * Email-friendly approval link. Uses a one-time token (not guessable admin/recruiter IDs).
     * GET /auth/admin/approve-recruiter-link?token=...
     */
    @GetMapping("/approve-recruiter-link")
    public ResponseEntity<String> approveRecruiterFromEmail(@RequestParam String token) {
        try {
            String message = authService.approveRecruiterFromEmailToken(token);
            return htmlPage("Recruiter Approved", message);
        } catch (RuntimeException e) {
            return htmlPage("Approval Failed", e.getMessage());
        }
    }

    /**
     * Email-friendly rejection link.
     * GET /auth/admin/reject-recruiter-link?token=...&reason=...
     */
    @GetMapping("/reject-recruiter-link")
    public ResponseEntity<String> rejectRecruiterFromEmail(
            @RequestParam String token,
            @RequestParam(required = false, defaultValue = "Rejected from email") String reason) {
        try {
            String message = authService.rejectRecruiterFromEmailToken(token, reason);
            return htmlPage("Recruiter Rejected", message);
        } catch (RuntimeException e) {
            return htmlPage("Rejection Failed", e.getMessage());
        }
    }

    private ResponseEntity<String> htmlPage(String title, String message) {
        String html = """
                <!doctype html>
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <title>%s</title>
                    <style>
                      body { font-family: Arial, sans-serif; background: #f6f8fb; padding: 40px; color: #222; }
                      .box { max-width: 560px; margin: 0 auto; background: #fff; padding: 28px; border-radius: 10px; box-shadow: 0 8px 30px rgba(0,0,0,.08); }
                      h1 { margin: 0 0 12px; }
                      p { line-height: 1.5; }
                      a { color: #2563eb; }
                    </style>
                  </head>
                  <body>
                    <div class="box">
                      <h1>%s</h1>
                      <p>%s</p>
                      <p><a href="http://localhost:3000/dashboard">Back to Admin Dashboard</a></p>
                    </div>
                  </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(title), escapeHtml(message));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
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

    /**
     * Add a recruiter directly (without approval flow).
     * POST /auth/admin/add-recruiter — requires admin JWT.
     */
    @PostMapping("/add-recruiter")
    public ResponseEntity<Object> addRecruiter(
            Authentication authentication,
            @RequestBody UserCredential newRecruiter) {
        try {
            int adminId = authService.resolveAdminUserIdFromEmail(authentication.getName());
            return ResponseEntity.ok(authService.addRecruiter(adminId, newRecruiter));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}