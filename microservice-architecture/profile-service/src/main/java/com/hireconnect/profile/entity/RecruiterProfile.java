package com.hireconnect.profile.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class RecruiterProfile extends UserProfile {
    private String companyName;
    private String companySize;
    private String industry;
    private String website;

    @ElementCollection
    private List<String> skills;
}