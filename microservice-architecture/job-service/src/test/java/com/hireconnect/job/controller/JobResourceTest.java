package com.hireconnect.job.controller;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class JobResourceTest {

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobResource jobResource;

    @Test
    void addJob() {
        Job j = new Job();
        when(jobService.addJob(any())).thenReturn(j);
        ResponseEntity<Job> r = jobResource.addJob(j);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void addJobLegacy() {
        Job j = new Job();
        when(jobService.addJob(any())).thenReturn(j);
        ResponseEntity<Job> r = jobResource.addJobLegacy(j);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getAllJobs() {
        when(jobService.getAllJobs()).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.getAllJobs();
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getAllJobsLegacy() {
        when(jobService.getAllJobs()).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.getAllJobsLegacy();
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getJobById_Success() {
        when(jobService.getJobById(1)).thenReturn(Optional.of(new Job()));
        ResponseEntity<Job> r = jobResource.getJobById(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getJobById_NotFound() {
        when(jobService.getJobById(1)).thenReturn(Optional.empty());
        ResponseEntity<Job> r = jobResource.getJobById(1);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void getJobByIdLegacy() {
        when(jobService.getJobById(1)).thenReturn(Optional.of(new Job()));
        ResponseEntity<Job> r = jobResource.getJobByIdLegacy(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getJobsByCategory() {
        when(jobService.getJobsByCategory(anyString())).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.getJobsByCategory("IT");
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getJobsByLocation() {
        when(jobService.getJobsByLocation(anyString())).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.getJobsByLocation("NY");
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getJobsByRecruiter() {
        when(jobService.getJobsByRecruiter(1)).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.getJobsByRecruiter(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void searchJobs() {
        when(jobService.searchJobs(anyString(), anyString(), anyDouble(), anyDouble())).thenReturn(List.of(new Job()));
        ResponseEntity<List<Job>> r = jobResource.searchJobs("dev", "IT", 1000, 2000);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void updateJob_Success() {
        Job j = new Job();
        when(jobService.updateJob(eq(1), any())).thenReturn(j);
        ResponseEntity<Job> r = jobResource.updateJob(1, j);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void updateJob_NotFound() {
        when(jobService.updateJob(eq(1), any())).thenThrow(new RuntimeException("not found"));
        ResponseEntity<Job> r = jobResource.updateJob(1, new Job());
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void updateJobLegacy_Success() {
        Job j = new Job();
        j.setJobId(1);
        when(jobService.updateJob(eq(1), any())).thenReturn(j);
        ResponseEntity<Job> r = jobResource.updateJobLegacy(j);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void updateJobLegacy_BadRequest() {
        Job j = new Job();
        j.setJobId(0);
        ResponseEntity<Job> r = jobResource.updateJobLegacy(j);
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void deleteJob() {
        doNothing().when(jobService).deleteJob(1);
        ResponseEntity<String> r = jobResource.deleteJob(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void deleteJobLegacy() {
        doNothing().when(jobService).deleteJob(1);
        ResponseEntity<String> r = jobResource.deleteJobLegacy(1);
        assertEquals(200, r.getStatusCode().value());
    }
}
