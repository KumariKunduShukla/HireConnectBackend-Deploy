package com.hireconnect.application.controller;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ApplicationResourceTest {

    @Mock
    private ApplicationService service;

    @InjectMocks
    private ApplicationResource resource;

    @Test
    void submitApplication_Success() {
        Application app = new Application();
        when(service.submitApplication(any())).thenReturn(app);
        ResponseEntity<?> r = resource.submitApplication(app);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void submitApplication_Exception() {
        when(service.submitApplication(any())).thenThrow(new RuntimeException("error"));
        ResponseEntity<?> r = resource.submitApplication(new Application());
        assertEquals(400, r.getStatusCode().value());
    }

    @Test
    void getById_Success() {
        when(service.getById(1)).thenReturn(Optional.of(new Application()));
        ResponseEntity<Application> r = resource.getById(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getById_NotFound() {
        when(service.getById(1)).thenReturn(Optional.empty());
        ResponseEntity<Application> r = resource.getById(1);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void getByCandidate() {
        when(service.getByCandidate(1)).thenReturn(List.of(new Application()));
        ResponseEntity<List<Application>> r = resource.getByCandidate(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getByJob() {
        when(service.getByJob(1)).thenReturn(List.of(new Application()));
        ResponseEntity<List<Application>> r = resource.getByJob(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void updateStatus_Success() {
        when(service.updateStatus(eq(1), anyString())).thenReturn(new Application());
        ResponseEntity<?> r = resource.updateStatus(1, "status");
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void updateStatus_Exception() {
        when(service.updateStatus(eq(1), anyString())).thenThrow(new RuntimeException("not found"));
        ResponseEntity<?> r = resource.updateStatus(1, "status");
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void updateStatusLegacy() {
        when(service.updateStatus(eq(1), anyString())).thenReturn(new Application());
        ResponseEntity<?> r = resource.updateStatusLegacy(1, "status");
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void withdrawApplication_Success() {
        doNothing().when(service).withdrawApplication(1);
        ResponseEntity<String> r = resource.withdrawApplication(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void withdrawApplication_Exception() {
        doThrow(new RuntimeException("not found")).when(service).withdrawApplication(1);
        ResponseEntity<String> r = resource.withdrawApplication(1);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void withdrawApplicationLegacy() {
        doNothing().when(service).withdrawApplication(1);
        ResponseEntity<String> r = resource.withdrawApplicationLegacy(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getApplicationCountForJob() {
        when(service.countByJobId(1)).thenReturn(5);
        ResponseEntity<Integer> r = resource.getApplicationCountForJob(1);
        assertEquals(200, r.getStatusCode().value());
        assertEquals(5, r.getBody());
    }

    @Test
    void getAllApplications() {
        when(service.getAllApplications()).thenReturn(List.of(new Application()));
        ResponseEntity<List<Application>> r = resource.getAllApplications();
        assertEquals(200, r.getStatusCode().value());
    }
}
