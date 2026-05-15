package com.hireconnect.application.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int applicationId;
    
    private int jobId;
    private int candidateId;
    
    private LocalDate appliedAt;
    
    // Status pipeline: Applied -> Shortlisted -> Interview Scheduled -> Offered / Rejected
    private String status; 
    
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    
    private String resumeUrl;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDate.now();
        if (this.status == null) {
            this.status = "Applied";
        }
    }
}