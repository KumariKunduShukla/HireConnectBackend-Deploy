package com.hireconnect.analytics.controller;

import com.hireconnect.analytics.entity.AnalyticsSummary;
import com.hireconnect.analytics.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsResource {

    @Autowired
    private AnalyticsService analyticsService;

    // For Recruiter Dashboard
    @GetMapping("/recruiter/{id}")
    public ResponseEntity<AnalyticsSummary> getRecruiterStats(@PathVariable("id") int recruiterId) {
        return ResponseEntity.ok(analyticsService.getPipelineStats(recruiterId));
    }

    // For Admin Dashboard
    @GetMapping("/admin/platform-stats")
    public ResponseEntity<AnalyticsSummary> getPlatformStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    // Single Job metric
    @GetMapping("/job/{jobId}/views")
    public ResponseEntity<Integer> getJobViewCount(@PathVariable("jobId") int jobId) {
        return ResponseEntity.ok(analyticsService.getJobViewCount(jobId));
    }
}