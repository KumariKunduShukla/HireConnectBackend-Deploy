package com.hireconnect.profile.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED) // Creates separate tables linked by ID
public abstract class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int profileId;
    
    private String fullName;
    
    @Column(unique = true)
    private String email;

    // Common profile fields shared across candidate/recruiter profiles.
    private String phone;
    private String location;
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private List<Address> addresses;
}