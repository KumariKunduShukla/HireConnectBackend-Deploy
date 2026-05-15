package com.hireconnect.profile.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class CandidateProfile extends UserProfile {
    private Long mobile;
    private LocalDate dob;
    private String gender;
    
    @ElementCollection // Creates a separate mapping table for a list of strings
    private List<String> skills;
    
    private int experience;
    private String resumePath;
    private String resumeUrl;
}