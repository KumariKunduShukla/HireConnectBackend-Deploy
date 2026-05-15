package com.hireconnect.job.controller;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // <-- Add logger here too
@RestController
@RequestMapping("/api/jobs")
public class JobResource {

    @Autowired
    private JobService jobService;

    @PostMapping
    public ResponseEntity<Job> addJob(@RequestBody Job job) {
        log.info("REST Request received to POST a new job");
        return ResponseEntity.ok(jobService.addJob(job));
    }

    // Backward-compatible alias used by existing frontend.
    @PostMapping("/add")
    public ResponseEntity<Job> addJobLegacy(@RequestBody Job job) {
        return addJob(job);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        log.info("REST Request received to GET all jobs");
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    // Backward-compatible alias used by existing frontend.
    @GetMapping("/all")
    public ResponseEntity<List<Job>> getAllJobsLegacy() {
        return getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable int id) {
        log.info("REST Request received to GET job by ID: {}", id);
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Backward-compatible alias used by existing frontend.
    @GetMapping("/id/{id}")
    public ResponseEntity<Job> getJobByIdLegacy(@PathVariable int id) {
        return getJobById(id);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Job>> getJobsByCategory(@PathVariable String category) {
        log.info("REST Request received to GET jobs by category: {}", category);
        return ResponseEntity.ok(jobService.getJobsByCategory(category));
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<Job>> getJobsByLocation(@PathVariable String location) {
        log.info("REST Request received to GET jobs by location: {}", location);
        return ResponseEntity.ok(jobService.getJobsByLocation(location));
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Job>> getJobsByRecruiter(@PathVariable int recruiterId) {
        log.info("REST Request received to GET jobs by recruiter ID: {}", recruiterId);
        return ResponseEntity.ok(jobService.getJobsByRecruiter(recruiterId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") double minSalary,
            @RequestParam(defaultValue = "0") double maxSalary) {
        log.info("REST Request received to GET /search with custom parameters");
        return ResponseEntity.ok(jobService.searchJobs(title, category, minSalary, maxSalary));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable int id, @RequestBody Job job) {
        log.info("REST Request received to PUT/update job ID: {}", id);
        try {
            return ResponseEntity.ok(jobService.updateJob(id, job));
        } catch (RuntimeException e) {
            log.error("Error processing update request for ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Backward-compatible alias used by existing frontend.
    @PutMapping("/update")
    public ResponseEntity<Job> updateJobLegacy(@RequestBody Job job) {
        if (job.getJobId() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return updateJob(job.getJobId(), job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable int id) {
        log.info("REST Request received to DELETE job ID: {}", id);
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }

    // Backward-compatible alias used by existing frontend.
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteJobLegacy(@PathVariable int id) {
        return deleteJob(id);
    }
}