package com.hireconnect.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.entity.UserProfile;
import com.hireconnect.profile.service.ProfileService;
import com.hireconnect.profile.repository.CandidateRepository;

// IMPORTANT: Use the Spring MediaType, not Jakarta
import org.springframework.http.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
public class ProfileResource {

    private static final String ERR_RESUME_NOT_FOUND = "Resume not found";

    @Autowired
    private ProfileService profileService;

    // A raw `new ObjectMapper()` has no JavaTimeModule, so LocalDate fields
    // like `dob` in CandidateProfile would throw JsonMappingException (HTTP 500).
    // Spring's ObjectMapper bean is already configured with JavaTimeModule and all
    // necessary modules via JacksonAutoConfiguration.
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @PostMapping(value = "/candidate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateProfile> createCandidateProfile(
            @RequestPart("profile") String profileJson,
            @RequestPart("resume") MultipartFile file) throws IOException {

        CandidateProfile candidateProfile = objectMapper.readValue(profileJson, CandidateProfile.class);

        return ResponseEntity.ok(profileService.addCandidate(candidateProfile, file));
    }

    @PostMapping("/recruiter")
    public ResponseEntity<RecruiterProfile> addRecruiter(@RequestBody RecruiterProfile profile) {
        return ResponseEntity.ok(profileService.addRecruiterProfile(profile));
    }

    @GetMapping
    public ResponseEntity<List<UserProfile>> getAll() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    /**
     * Preferred lookup for login / subscription flows — avoids path-encoding and {@code /{id}} clashes
     * with the literal segment {@code "email"} when emails appear in the path.
     */
    @GetMapping(value = "/by-email", params = "email")
    public ResponseEntity<UserProfile> getByEmailQuery(@RequestParam("email") String email) {
        return ResponseEntity.ok(profileService.getByEmail(email));
    }

    @GetMapping("/email/{email:.+}")
    public ResponseEntity<UserProfile> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(profileService.getByEmail(email));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserProfile> getById(@PathVariable int id) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @PatchMapping("/{id:\\d+}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable int id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(profileService.updateProfile(id, updates));
    }

    @GetMapping("/{id:\\d+}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable int id) throws IOException {
        CandidateProfile profile = candidateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, ERR_RESUME_NOT_FOUND));

        if (profile.getResumePath() == null || profile.getResumePath().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, ERR_RESUME_NOT_FOUND);
        }

        Path path = Paths.get(profile.getResumePath());
        if (!Files.exists(path)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, ERR_RESUME_NOT_FOUND);
        }

        byte[] fileBytes = Files.readAllBytes(path);
        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "inline; filename=resume")
                .body(resource);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<String> deleteProfile(@PathVariable int id) {
        profileService.deleteProfile(id);
        return ResponseEntity.ok("Profile deleted successfully");
    }
}