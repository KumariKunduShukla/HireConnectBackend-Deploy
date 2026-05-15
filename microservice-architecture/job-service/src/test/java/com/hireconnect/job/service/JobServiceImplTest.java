package com.hireconnect.job.service;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void addJob() {
        Job j = new Job();
        when(jobRepository.save(any())).thenReturn(j);
        assertEquals(j, jobService.addJob(j));
    }

    @Test
    void getAllJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(new Job()));
        assertEquals(1, jobService.getAllJobs().size());
    }

    @Test
    void getJobById_Success() {
        Job j = new Job();
        when(jobRepository.findById(1)).thenReturn(Optional.of(j));
        assertTrue(jobService.getJobById(1).isPresent());
    }

    @Test
    void getJobById_NotFound() {
        when(jobRepository.findById(1)).thenReturn(Optional.empty());
        assertFalse(jobService.getJobById(1).isPresent());
    }

    @Test
    void updateJob_Success() {
        Job existing = new Job();
        existing.setJobId(1);
        when(jobRepository.findById(1)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any())).thenReturn(existing);

        Job update = new Job();
        update.setTitle("Dev");
        update.setCategory("IT");
        update.setLocation("NY");
        update.setSalaryMin(100);
        update.setSalaryMax(200);
        update.setDescription("desc");
        update.setStatus("OPEN");

        Job res = jobService.updateJob(1, update);
        assertEquals("Dev", res.getTitle());
    }

    @Test
    void updateJob_NotFound() {
        when(jobRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.updateJob(1, new Job()));
    }

    @Test
    void deleteJob() {
        doNothing().when(jobRepository).deleteById(1);
        jobService.deleteJob(1);
        verify(jobRepository, times(1)).deleteById(1);
    }

    @Test
    void getJobsByCategory() {
        when(jobRepository.findByCategory(anyString())).thenReturn(List.of(new Job()));
        assertEquals(1, jobService.getJobsByCategory("IT").size());
    }

    @Test
    void getJobsByLocation() {
        when(jobRepository.findByLocation(anyString())).thenReturn(List.of(new Job()));
        assertEquals(1, jobService.getJobsByLocation("NY").size());
    }

    @Test
    void getJobsByRecruiter() {
        when(jobRepository.findByPostedBy(1)).thenReturn(List.of(new Job()));
        assertEquals(1, jobService.getJobsByRecruiter(1).size());
    }

    @Test
    void searchJobs() {
        when(jobRepository.searchJobs(anyString(), anyString(), anyDouble(), anyDouble())).thenReturn(List.of(new Job()));
        assertEquals(1, jobService.searchJobs("dev", "IT", 100, 200).size());
    }
}
