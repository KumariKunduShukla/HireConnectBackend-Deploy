package com.hireconnect.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.entity.UserProfile;
import com.hireconnect.profile.repository.CandidateRepository;
import com.hireconnect.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProfileResourceTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private ProfileResource profileResource;

    @Test
    void createCandidateProfile() throws IOException {
        CandidateProfile profile = new CandidateProfile();
        when(objectMapper.readValue(anyString(), eq(CandidateProfile.class))).thenReturn(profile);
        when(profileService.addCandidate(any(), any())).thenReturn(profile);

        MultipartFile file = mock(MultipartFile.class);
        ResponseEntity<CandidateProfile> response = profileResource.createCandidateProfile("{}", file);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void addRecruiter() {
        RecruiterProfile profile = new RecruiterProfile();
        when(profileService.addRecruiterProfile(any())).thenReturn(profile);

        ResponseEntity<RecruiterProfile> response = profileResource.addRecruiter(profile);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getAll() {
        when(profileService.getAllProfiles()).thenReturn(List.of(new CandidateProfile()));
        ResponseEntity<List<UserProfile>> response = profileResource.getAll();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getByEmailQuery() {
        when(profileService.getByEmail("test@test.com")).thenReturn(new CandidateProfile());
        ResponseEntity<UserProfile> response = profileResource.getByEmailQuery("test@test.com");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getByEmail() {
        when(profileService.getByEmail("test@test.com")).thenReturn(new CandidateProfile());
        ResponseEntity<UserProfile> response = profileResource.getByEmail("test@test.com");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getById() {
        when(profileService.getProfileById(1)).thenReturn(new CandidateProfile());
        ResponseEntity<UserProfile> response = profileResource.getById(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateProfile() {
        when(profileService.updateProfile(eq(1), anyMap())).thenReturn(new CandidateProfile());
        ResponseEntity<UserProfile> response = profileResource.updateProfile(1, Map.of("key", "val"));
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteProfile() {
        doNothing().when(profileService).deleteProfile(1);
        ResponseEntity<String> response = profileResource.deleteProfile(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void downloadResume_NotFound() {
        when(candidateRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> profileResource.downloadResume(1));
    }

    @Test
    void downloadResume_NoPath() {
        CandidateProfile p = new CandidateProfile();
        p.setResumePath(null);
        when(candidateRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class, () -> profileResource.downloadResume(1));
    }

    @Test
    void downloadResume_BlankPath() {
        CandidateProfile p = new CandidateProfile();
        p.setResumePath("   ");
        when(candidateRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class, () -> profileResource.downloadResume(1));
    }

    @Test
    void downloadResume_FileNotExists() {
        CandidateProfile p = new CandidateProfile();
        p.setResumePath("invalid_path_12345");
        when(candidateRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ResponseStatusException.class, () -> profileResource.downloadResume(1));
    }

    @Test
    void downloadResume_Success() throws IOException {
        CandidateProfile p = new CandidateProfile();
        Path tempFile = Files.createTempFile("test_resume", ".pdf");
        Files.write(tempFile, "dummy content".getBytes());
        p.setResumePath(tempFile.toAbsolutePath().toString());
        
        when(candidateRepository.findById(1)).thenReturn(Optional.of(p));
        
        ResponseEntity<Resource> response = profileResource.downloadResume(1);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        
        Files.deleteIfExists(tempFile);
    }
}
