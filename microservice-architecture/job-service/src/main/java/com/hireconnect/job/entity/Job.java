package com.hireconnect.job.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int jobId;
    
    private String title;
    private String category;
    private String type; // e.g., Full-time, Part-time, Remote
    private String location;
    private String company; // Name of the company hiring

    
    private double salaryMin;
    private double salaryMax;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ElementCollection
    private List<String> skills;
    
    private int experienceRequired;
    
    // Links back to the Recruiter's Profile ID in the Profile-Service
    private int postedBy; 
    
    private String status; //OPEN, CLOSED
    
    private LocalDate postedAt;

    @PrePersist
    protected void onCreate() {
        this.postedAt = LocalDate.now();
        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}