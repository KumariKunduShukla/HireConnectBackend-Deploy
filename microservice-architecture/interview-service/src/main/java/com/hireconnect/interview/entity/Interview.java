package com.hireconnect.interview.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int interviewId;

    private int applicationId;
    private LocalDateTime scheduledAt;
    private String mode;
    private String meetLink;
    private String location;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // FIX: Added @PrePersist so status is never null when saved.
    // Previously the entity had no default, meaning any save outside
    // scheduleInterview() would write a null status to the DB.
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = "SCHEDULED";
        }
    }
}