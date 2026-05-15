package com.hireconnect.interview.controller;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/interviews")
public class InterviewResource {

    @Autowired
    private InterviewService service;

    @PostMapping("/schedule")
    public ResponseEntity<Interview> schedule(@RequestBody Interview interview) {
        log.info("REST Request: Schedule Interview");
        return ResponseEntity.ok(service.scheduleInterview(interview));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Interview> confirm(@PathVariable int id) {
        log.info("REST Request: Confirm Interview ID {}", id);
        try {
            return ResponseEntity.ok(service.confirmInterview(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Backward-compatible alias used by existing frontend.
    @PatchMapping("/confirm/{id}")
    public ResponseEntity<Interview> confirmLegacy(@PathVariable int id) {
        return confirm(id);
    }

    // FIX: @DateTimeFormat added so Spring can convert the URL query param string
    // (e.g. "2025-06-01T10:00:00") into a LocalDateTime.
    // Without this annotation every call returned HTTP 400 Bad Request.
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<Interview> reschedule(
            @PathVariable int id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime newTime) {
        log.info("REST Request: Reschedule Interview ID {} to {}", id, newTime);
        try {
            return ResponseEntity.ok(service.rescheduleInterview(id, newTime));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Backward-compatible alias used by existing frontend.
    @PatchMapping("/reschedule/{id}")
    public ResponseEntity<Interview> rescheduleLegacy(
            @PathVariable int id,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime newTime) {
        return reschedule(id, newTime);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable int id) {
        log.info("REST Request: Cancel Interview ID {}", id);
        service.cancelInterview(id);
        return ResponseEntity.ok("Interview cancelled successfully");
    }

    // Backward-compatible alias used by existing frontend.
    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<String> cancelLegacy(@PathVariable int id) {
        return cancel(id);
    }

    @GetMapping("/application/{appId}")
    public ResponseEntity<List<Interview>> getByApp(@PathVariable int appId) {
        log.info("REST Request: Get Interviews for App {}", appId);
        return ResponseEntity.ok(service.getByApplication(appId));
    }
}