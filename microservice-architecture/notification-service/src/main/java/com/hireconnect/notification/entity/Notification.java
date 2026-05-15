package com.hireconnect.notification.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * STEP 1 — ENTITY (Database Table Blueprint)
 *
 * This class maps directly to the 'notification' table in MySQL.
 * Every field here becomes a column in that table.
 *
 * Fixes applied:
 *  @NotBlank on required String fields — rejects empty/null values at the API layer
 *  @Email on recipientEmail — validates format before any code runs
 * @NotNull on userId — prevents saving notifications with no owner
 */
@Entity
@Data  // Lombok: auto-generates getters, setters, toString, equals, hashCode
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificationId;         // Auto-incremented PK by MySQL

    @NotNull(message = "userId is required")
    private int userId;                 // Which user receives this alert

    @NotBlank(message = "type must not be blank")
    private String type;                // e.g. "Job Alert", "Interview", "Application Update"

    @NotBlank(message = "recipientEmail must not be blank")
    @Email(message = "recipientEmail must be a valid email address")
    private String recipientEmail;      // Where the email gets delivered

    @NotBlank(message = "message must not be blank")
    private String message;             // The text the user reads

    private boolean isRead;             // false = unread (new), true = read

    private LocalDateTime createdAt;    // Set automatically in the service — not from API
}