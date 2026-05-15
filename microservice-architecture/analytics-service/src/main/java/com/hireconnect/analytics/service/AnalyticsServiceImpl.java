package com.hireconnect.analytics.service;

import com.hireconnect.analytics.entity.AnalyticsSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public int getJobViewCount(int jobId) {
        // Keeping as mock as platform doesn't currently track job view events natively
        return 1500;
    }

    @Override
    public int getAppCountByJob(int jobId) {
        try {
            List<?> apps = restTemplate.getForObject("http://application-service/api/applications/job/" + jobId, List.class);
            return apps != null ? apps.size() : 0;
        } catch (Exception e) {
            log.warn("Could not fetch applications for job {}", jobId, e);
            return 0;
        }
    }

    @Override
    public double getViewToApplyRatio(int jobId) {
        int views = getJobViewCount(jobId);
        int apps = getAppCountByJob(jobId);
        return (views == 0) ? 0.0 : ((double) apps / views) * 100;
    }

    @Override
    public double getTimeToHire(int jobId) {
        // Keeping as mock due to missing timestamp schema in current DB tables
        return 14.5;
    }

    @Override
    public AnalyticsSummary getPipelineStats(int recruiterId) {
        log.info("Generating real pipeline stats for Recruiter ID: {}", recruiterId);
        try {
            List<Map<String, Object>> jobs = restTemplate.getForObject("http://job-service/api/jobs/recruiter/" + recruiterId, List.class);
            AnalyticsSummary summary = calculateSummaryFromJobs(jobs);
            // Mock averages for specific recruiter context
            summary.setAvgTimeToHireDays(18.2);
            summary.setViewToApplyRatio(8.5);
            return summary;
        } catch (Exception e) {
            log.error("Error fetching pipeline stats: {}", e.getMessage());
            return new AnalyticsSummary();
        }
    }

    @Override
    public AnalyticsSummary getPlatformStats() {
        log.info("Generating real platform-wide admin statistics");
        try {
            List<Map<String, Object>> jobs = restTemplate.getForObject("http://job-service/api/jobs", List.class);
            AnalyticsSummary summary = calculateSummaryFromJobs(jobs);
            // Mock averages for platform context
            summary.setAvgTimeToHireDays(22.4);
            summary.setViewToApplyRatio(12.1);
            return summary;
        } catch (Exception e) {
            log.error("Error fetching platform stats: {}", e.getMessage());
            return new AnalyticsSummary();
        }
    }

    private AnalyticsSummary calculateSummaryFromJobs(List<Map<String, Object>> jobs) {
        AnalyticsSummary summary = new AnalyticsSummary();
        int totalJobs = 0;
        int totalApps = 0;
        int shortlisted = 0;
        int interviewsScheduled = 0;
        int offered = 0;
        int rejected = 0;

        if (jobs != null) {
            totalJobs = jobs.size();
            for (Map<String, Object> job : jobs) {
                if (job.get("jobId") == null) continue;
                int jobId = Integer.parseInt(job.get("jobId").toString());
                List<Map<String, Object>> apps = restTemplate.getForObject("http://application-service/api/applications/job/" + jobId, List.class);
                if (apps != null) {
                    totalApps += apps.size();
                    for (Map<String, Object> app : apps) {
                        String status = app.get("status") != null ? app.get("status").toString() : "";
                        if ("Shortlisted".equalsIgnoreCase(status)) shortlisted++;
                        else if ("INTERVIEW_SCHEDULED".equalsIgnoreCase(status)) interviewsScheduled++;
                        else if ("Offered".equalsIgnoreCase(status)) offered++;
                        else if ("Rejected".equalsIgnoreCase(status)) rejected++;
                    }
                }
            }
        }

        summary.setTotalJobs(totalJobs);
        summary.setTotalApplications(totalApps);
        summary.setShortlistedCount(shortlisted);
        summary.setInterviewsScheduledCount(interviewsScheduled);
        summary.setOfferedCount(offered);
        summary.setRejectedCount(rejected);
        return summary;
    }

    @Override
    public Map<String, Long> getTopJobCategories() {
        Map<String, Long> categories = new HashMap<>();
        try {
            List<Map<String, Object>> jobs = restTemplate.getForObject("http://job-service/api/jobs", List.class);
            if (jobs != null) {
                for (Map<String, Object> job : jobs) {
                    String cat = job.get("category") != null ? job.get("category").toString() : "Other";
                    categories.put(cat, categories.getOrDefault(cat, 0L) + 1);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching job categories: {}", e.getMessage());
        }
        return categories;
    }
}