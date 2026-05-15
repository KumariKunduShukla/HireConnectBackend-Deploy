package com.hireconnect.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobResource.class)
class JobResourceCompatibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @Test
    void legacyAllEndpointReturnsOk() throws Exception {
        Mockito.when(jobService.getAllJobs()).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs/all"))
                .andExpect(status().isOk());
    }

    @Test
    void legacyIdEndpointReturnsOk() throws Exception {
        Job job = new Job();
        job.setJobId(1);
        Mockito.when(jobService.getJobById(1)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/id/1"))
                .andExpect(status().isOk());
    }

    @Test
    void legacyUpdateRequiresJobId() throws Exception {
        Job job = new Job();
        job.setTitle("Backend Engineer");

        mockMvc.perform(put("/api/jobs/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legacyDeleteEndpointReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/jobs/delete/1"))
                .andExpect(status().isOk());
    }
}

