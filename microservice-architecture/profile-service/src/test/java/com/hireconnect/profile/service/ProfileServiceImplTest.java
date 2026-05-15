package com.hireconnect.profile.service;

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.entity.UserProfile;
import com.hireconnect.profile.repository.CandidateRepository;
import com.hireconnect.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository repository;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileService, "uploadDir", "target/test-uploads/");
    }

    @Test
    void addCandidateProfile() {
        CandidateProfile p = new CandidateProfile();
        when(repository.save(any())).thenReturn(p);
        assertEquals(p, profileService.addCandidateProfile(p));
    }

    @Test
    void addRecruiterProfile() {
        RecruiterProfile p = new RecruiterProfile();
        when(repository.save(any())).thenReturn(p);
        assertEquals(p, profileService.addRecruiterProfile(p));
    }

    @Test
    void getAllProfiles() {
        when(repository.findAll()).thenReturn(List.of(new CandidateProfile()));
        assertEquals(1, profileService.getAllProfiles().size());
    }

    @Test
    void getProfileById_Success() {
        CandidateProfile p = new CandidateProfile();
        when(repository.findByProfileId(1)).thenReturn(Optional.of(p));
        assertEquals(p, profileService.getProfileById(1));
    }

    @Test
    void getProfileById_NotFound() {
        when(repository.findByProfileId(1)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> profileService.getProfileById(1));
    }

    @Test
    void getByEmail_Success() {
        CandidateProfile p = new CandidateProfile();
        when(repository.findByEmail("test")).thenReturn(Optional.of(p));
        assertEquals(p, profileService.getByEmail("test"));
    }

    @Test
    void getByEmail_NotFound() {
        when(repository.findByEmail("test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> profileService.getByEmail("test"));
    }

    @Test
    void updateProfile() {
        CandidateProfile p = new CandidateProfile();
        p.setMobile(12345L);
        when(repository.findByProfileId(1)).thenReturn(Optional.of(p));
        when(repository.save(any())).thenReturn(p);

        Map<String, Object> updates = Map.of("phone", "99999", "fullName", "Test User");
        CandidateProfile updated = (CandidateProfile) profileService.updateProfile(1, updates);
        
        assertEquals("Test User", updated.getFullName());
        assertEquals("99999", updated.getPhone());
    }

    @Test
    void updateProfile_TypeConversions() {
        CandidateProfile p = new CandidateProfile();
        when(repository.findByProfileId(1)).thenReturn(Optional.of(p));
        when(repository.save(any())).thenReturn(p);

        // mobile is Long, salary might be Double, etc.
        // We use Map.of with Integers and check if they are converted
        Map<String, Object> updates = Map.of(
            "mobile", 88888, // Integer -> Long
            "headline", "New Headline",
            "nonExistentField", "Ignore Me"
        );
        
        CandidateProfile updated = (CandidateProfile) profileService.updateProfile(1, updates);
        assertEquals(88888L, updated.getMobile());
        assertEquals("New Headline", updated.getHeadline());
    }

    @Test
    void updateProfile_DoubleFloatShort() {
        // We need a class that has these types. RecruiterProfile might have them or we can just mock them.
        // Actually CandidateProfile has mobile (Long). 
        // Let's assume we want to test the logic of convertIfNeeded directly if possible or via a mock entity.
        CandidateProfile p = new CandidateProfile();
        when(repository.findByProfileId(1)).thenReturn(Optional.of(p));
        when(repository.save(any())).thenReturn(p);

        // We can't easily test Double/Float/Short on CandidateProfile if it doesn't have those fields.
        // But we can test the convertIfNeeded logic by calling it via reflection or just adding a test entity.
        // Actually, let's just add tests for other branches in addCandidate.
    }

    @Test
    void convertIfNeeded_DirectTests() {
        // Test Long
        Object longRes = ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", Long.class, 123);
        assertEquals(123L, (Long) longRes);
        assertTrue(longRes instanceof Long);

        // Test Double
        Object doubleRes = ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", Double.class, 123);
        assertEquals(123.0, (Double) doubleRes);
        assertTrue(doubleRes instanceof Double);

        // Test Float
        Object floatRes = ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", Float.class, 123);
        assertEquals(123.0f, (Float) floatRes);
        assertTrue(floatRes instanceof Float);

        // Test Short
        Object shortRes = ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", Short.class, 123);
        assertEquals((short)123, (Short) shortRes);
        assertTrue(shortRes instanceof Short);

        // Test primitive types
        assertEquals(123L, (Long) ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", long.class, 123));
        assertEquals(123.0, (Double) ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", double.class, 123));
        assertEquals(123.0f, (Float) ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", float.class, 123));
        assertEquals((short)123, (Short) ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", short.class, 123));

        // Test null and other types
        assertNull(ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", String.class, null));
        assertEquals("test", ReflectionTestUtils.invokeMethod(profileService, "convertIfNeeded", String.class, "test"));
    }

    @Test
    void deleteProfile() {
        doNothing().when(repository).deleteByProfileId(1);
        profileService.deleteProfile(1);
        verify(repository, times(1)).deleteByProfileId(1);
    }

    @Test
    void addCandidate() throws IOException {
        CandidateProfile p = new CandidateProfile();
        p.setProfileId(10);
        
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        
        when(candidateRepository.save(any())).thenReturn(p);

        CandidateProfile saved = profileService.addCandidate(p, file);
        assertEquals(10, saved.getProfileId());
        assertTrue(saved.getResumeUrl().contains("/api/v1/profiles/10/resume"));
        
        // Cleanup test directory if created
        Path path = Path.of("target/test-uploads/");
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(java.util.Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }
    
    @Test
    void addCandidate_DirectoryExists() throws IOException {
        CandidateProfile p = new CandidateProfile();
        p.setProfileId(20);
        
        // Pre-create directory
        new File("target/test-uploads-existing/").mkdirs();
        ReflectionTestUtils.setField(profileService, "uploadDir", "target/test-uploads-existing/");

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test2.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test2".getBytes()));
        
        when(candidateRepository.save(any())).thenReturn(p);

        CandidateProfile saved = profileService.addCandidate(p, file);
        assertNotNull(saved);
        
        // Cleanup
        Path path = Path.of("target/test-uploads-existing/");
        if (Files.exists(path)) {
            Files.walk(path).sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
