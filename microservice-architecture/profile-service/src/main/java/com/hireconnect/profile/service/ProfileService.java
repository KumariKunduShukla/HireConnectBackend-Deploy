package com.hireconnect.profile.service;

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.entity.UserProfile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    CandidateProfile addCandidateProfile(CandidateProfile profile);
    RecruiterProfile addRecruiterProfile(RecruiterProfile profile);
    UserProfile updateProfile(int profileId, Map<String, Object> updates);
    void deleteProfile(int profileId);
    UserProfile getProfileById(int profileId);
    UserProfile getByEmail(String email);
    List<UserProfile> getAllProfiles();
    CandidateProfile addCandidate(CandidateProfile profile, MultipartFile file) throws IOException;
    
}