package com.hireconnect.profile.service;

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.entity.UserProfile;
import com.hireconnect.profile.repository.CandidateRepository;
import com.hireconnect.profile.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private ProfileRepository repository;

    @Autowired
    private CandidateRepository candidateRepository;

    //  Externalized upload dir via @Value so it can be overridden in
    // Docker/Kubernetes with a persistent volume path 
    @Value("${app.upload-dir:uploads/resumes/}")
    private String uploadDir;

    @Override
    public CandidateProfile addCandidateProfile(CandidateProfile profile) {
        return repository.save(profile);
    }

    @Override
    public RecruiterProfile addRecruiterProfile(RecruiterProfile profile) {
        return repository.save(profile);
    }

    @Override
    public List<UserProfile> getAllProfiles() {
        return repository.findAll();
    }

    @Override
    public UserProfile getProfileById(int profileId) {
        return repository.findByProfileId(profileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found for ID: " + profileId
                ));
    }

    @Override
    public UserProfile getByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found for Email: " + email
                ));
    }

    @Override
    public UserProfile updateProfile(int profileId, Map<String, Object> updates) {
        UserProfile profile = getProfileById(profileId);

        updates.forEach((key, value) -> {
            Field field = ReflectionUtils.findField(profile.getClass(), key);
            if (field == null) {
                field = ReflectionUtils.findField(UserProfile.class, key);
            }
            if (field != null) {
                ReflectionUtils.makeAccessible(field);

                // FIX: Jackson deserializes JSON numbers as Integer by default.
                // Entity fields like `mobile` are Long, `salary` are Double.
                // ReflectionUtils.setField throws IllegalArgumentException when
                // you try to assign Integer into a Long field.
                // We convert here before setting to avoid that runtime crash.
                Object convertedValue = convertIfNeeded(field.getType(), value);
                ReflectionUtils.setField(field, profile, convertedValue);
            }
        });

        return repository.save(profile);
    }

    /**
     * Converts a Jackson-deserialized value (e.g. Integer) to the field's
     * declared type (e.g. Long, Double) if necessary.
     */
    private Object convertIfNeeded(Class<?> targetType, Object value) {
        if (value == null) return null;

        if ((targetType == Long.class || targetType == long.class) && value instanceof Integer integer) {
            return integer.longValue();
        }
        if ((targetType == Double.class || targetType == double.class) && value instanceof Integer integer) {
            return integer.doubleValue();
        }
        if ((targetType == Float.class || targetType == float.class) && value instanceof Integer integer) {
            return integer.floatValue();
        }
        if ((targetType == Short.class || targetType == short.class) && value instanceof Integer integer) {
            return integer.shortValue();
        }
        return value;
    }

    @Override
    @Transactional
    public void deleteProfile(int profileId) {
        repository.deleteByProfileId(profileId);
    }

    @Override
    public CandidateProfile addCandidate(CandidateProfile profile, MultipartFile file) throws IOException {
        // Create the directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Give the file a unique name to avoid collisions
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        Path filePath = Paths.get(uploadDir + fileName);

        // Copy the uploaded file to the target location
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Store the absolute path for server-side downloads
        profile.setResumePath(filePath.toString());

        // Save once to get a profileId
        CandidateProfile saved = candidateRepository.save(profile);

        // Store a public URL the frontend can open
        // Gateway exposes profiles under /api/v1/profiles/** — keep URL consistent for browsers.
        saved.setResumeUrl("/api/v1/profiles/" + saved.getProfileId() + "/resume");
        return candidateRepository.save(saved);
    }
}