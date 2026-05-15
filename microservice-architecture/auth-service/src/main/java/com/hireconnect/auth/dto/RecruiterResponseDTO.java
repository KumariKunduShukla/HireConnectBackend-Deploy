package com.hireconnect.auth.dto;

import java.time.LocalDateTime;

/**
 * Safe DTO for returning recruiter info to admin.
 * Never exposes the password hash.
 */
public class RecruiterResponseDTO {
    private int userId;
    private String email;
    private String status;
    private LocalDateTime createdAt;

    public RecruiterResponseDTO() {}

    public RecruiterResponseDTO(int userId, String email, String status, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}