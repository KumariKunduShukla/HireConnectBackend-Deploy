package com.hireconnect.interview.controller;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class InterviewResourceTest {

    @Mock
    private InterviewService service;

    @InjectMocks
    private InterviewResource resource;

    @Test
    void schedule() {
        Interview i = new Interview();
        when(service.scheduleInterview(any())).thenReturn(i);
        ResponseEntity<Interview> r = resource.schedule(i);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void confirm_Success() {
        Interview i = new Interview();
        when(service.confirmInterview(1)).thenReturn(i);
        ResponseEntity<Interview> r = resource.confirm(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void confirm_Exception() {
        when(service.confirmInterview(1)).thenThrow(new RuntimeException("not found"));
        ResponseEntity<Interview> r = resource.confirm(1);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void confirmLegacy() {
        Interview i = new Interview();
        when(service.confirmInterview(1)).thenReturn(i);
        ResponseEntity<Interview> r = resource.confirmLegacy(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void reschedule_Success() {
        Interview i = new Interview();
        LocalDateTime time = LocalDateTime.now();
        when(service.rescheduleInterview(eq(1), any())).thenReturn(i);
        ResponseEntity<Interview> r = resource.reschedule(1, time);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void reschedule_Exception() {
        LocalDateTime time = LocalDateTime.now();
        when(service.rescheduleInterview(eq(1), any())).thenThrow(new RuntimeException("not found"));
        ResponseEntity<Interview> r = resource.reschedule(1, time);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void rescheduleLegacy() {
        Interview i = new Interview();
        LocalDateTime time = LocalDateTime.now();
        when(service.rescheduleInterview(eq(1), any())).thenReturn(i);
        ResponseEntity<Interview> r = resource.rescheduleLegacy(1, time);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void cancel() {
        doNothing().when(service).cancelInterview(1);
        ResponseEntity<String> r = resource.cancel(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void cancelLegacy() {
        doNothing().when(service).cancelInterview(1);
        ResponseEntity<String> r = resource.cancelLegacy(1);
        assertEquals(200, r.getStatusCode().value());
    }

    @Test
    void getByApp() {
        when(service.getByApplication(1)).thenReturn(List.of(new Interview()));
        ResponseEntity<List<Interview>> r = resource.getByApp(1);
        assertEquals(200, r.getStatusCode().value());
    }
}
