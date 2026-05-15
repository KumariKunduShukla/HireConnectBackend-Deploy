package com.hireconnect.application.controller;

import com.hireconnect.application.entity.Application;
import com.hireconnect.application.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/applications")
public class ApplicationResource {

    @Autowired
    private ApplicationService service;

    // Support both canonical POST /api/applications and legacy POST /api/applications/submit.
    @PostMapping({"", "/", "/submit"})
    public ResponseEntity<?> submitApplication(@RequestBody Application application) {
        log.info("REST Request received to submit a new application");
        try {
            return ResponseEntity.ok(service.submitApplication(application));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable int id) {
        log.info("REST Request received to GET application ID: {}", id);
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<Application>> getByCandidate(@PathVariable int candidateId) {
        log.info("REST Request received to GET applications for Candidate: {}", candidateId);
        return ResponseEntity.ok(service.getByCandidate(candidateId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> getByJob(@PathVariable int jobId) {
        log.info("REST Request received to GET applications for Job: {}", jobId);
        return ResponseEntity.ok(service.getByJob(jobId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable int id, @RequestParam String status) {
        log.info("REST Request received to PATCH status for application {} to {}", id, status);
        try {
            return ResponseEntity.ok(service.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Backward-compatible alias used by existing frontend.
    @PatchMapping("/status/{id}")
    public ResponseEntity<?> updateStatusLegacy(@PathVariable int id, @RequestParam String status) {
        return updateStatus(id, status);
    }

    // FIX: previously returned 200 even when the application ID didn't exist.
    // Now returns 404 if the application is not found.
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<String> withdrawApplication(@PathVariable int id) {
        log.info("REST Request received to withdraw application {}", id);
        try {
            service.withdrawApplication(id);
            return ResponseEntity.ok("Application withdrawn successfully");
        } catch (RuntimeException e) {
            log.warn("Withdraw failed for application ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Backward-compatible alias used by existing frontend.
    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<String> withdrawApplicationLegacy(@PathVariable int id) {
        return withdrawApplication(id);
    }

    @GetMapping("/job/{jobId}/count")
    public ResponseEntity<Integer> getApplicationCountForJob(@PathVariable int jobId) {
        log.info("REST Request received to GET application count for Job: {}", jobId);
        return ResponseEntity.ok(service.countByJobId(jobId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Application>> getAllApplications() {
        log.info("REST Request received to GET all applications");
        return ResponseEntity.ok(service.getAllApplications());
    }

    @GetMapping("/re-notify/{id}")
    public ResponseEntity<String> reTriggerNotification(@PathVariable int id) {
        log.info("REST Request received to re-trigger notification for application {}", id);
        try {
            service.reTriggerNotification(id);
            return ResponseEntity.ok("Notification re-triggered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}