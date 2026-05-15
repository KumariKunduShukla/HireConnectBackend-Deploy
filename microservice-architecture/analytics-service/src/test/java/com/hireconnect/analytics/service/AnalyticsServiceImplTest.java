package com.hireconnect.analytics.service;

import com.hireconnect.analytics.entity.AnalyticsSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AnalyticsServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getJobViewCount() {
        assertEquals(1500, analyticsService.getJobViewCount(1));
    }

    @Test
    void getAppCountByJob_Success() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(List.of(1, 2, 3));
        assertEquals(3, analyticsService.getAppCountByJob(1));
    }

    @Test
    void getAppCountByJob_Null() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(null);
        assertEquals(0, analyticsService.getAppCountByJob(1));
    }

    @Test
    void getAppCountByJob_Exception() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenThrow(new RuntimeException("Error"));
        assertEquals(0, analyticsService.getAppCountByJob(1));
    }

    @Test
    void getViewToApplyRatio() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(List.of(1, 2, 3));
        // View count is hardcoded to 1500, apps is 3
        // Ratio is (3.0 / 1500) * 100 = 0.2
        assertEquals(0.2, analyticsService.getViewToApplyRatio(1));
    }

    @Test
    void getTimeToHire() {
        assertEquals(14.5, analyticsService.getTimeToHire(1));
    }

    @Test
    void getPipelineStats_Success() {
        Map<String, Object> job1 = Map.of("jobId", 1);
        Map<String, Object> job2 = Map.of("jobId", 2);
        
        when(restTemplate.getForObject("http://job-service/api/jobs/recruiter/1", List.class))
                .thenReturn(List.of(job1, job2, Map.of())); // One invalid job without ID

        Map<String, Object> app1 = Map.of("status", "Shortlisted");
        Map<String, Object> app2 = Map.of("status", "Offered");
        Map<String, Object> app3 = Map.of("status", "Rejected");
        Map<String, Object> app4 = Map.of("status", "INTERVIEW_SCHEDULED");
        Map<String, Object> app5 = Map.of(); // empty status

        when(restTemplate.getForObject("http://application-service/api/applications/job/1", List.class))
                .thenReturn(List.of(app1, app2));
        when(restTemplate.getForObject("http://application-service/api/applications/job/2", List.class))
                .thenReturn(List.of(app3, app4, app5));

        AnalyticsSummary summary = analyticsService.getPipelineStats(1);
        
        assertEquals(3, summary.getTotalJobs()); // Only counts entries, 3 items in list
        assertEquals(5, summary.getTotalApplications());
        assertEquals(1, summary.getShortlistedCount());
        assertEquals(1, summary.getInterviewsScheduledCount());
        assertEquals(1, summary.getOfferedCount());
        assertEquals(1, summary.getRejectedCount());
    }

    @Test
    void getPipelineStats_Exception() {
        when(restTemplate.getForObject("http://job-service/api/jobs/recruiter/1", List.class))
                .thenThrow(new RuntimeException("Error"));

        AnalyticsSummary summary = analyticsService.getPipelineStats(1);
        assertEquals(0, summary.getTotalJobs());
    }

    @Test
    void getPlatformStats_Success() {
        Map<String, Object> job1 = Map.of("jobId", 1);
        
        when(restTemplate.getForObject("http://job-service/api/jobs", List.class))
                .thenReturn(List.of(job1));

        Map<String, Object> app1 = Map.of("status", "Shortlisted");

        when(restTemplate.getForObject("http://application-service/api/applications/job/1", List.class))
                .thenReturn(List.of(app1));

        AnalyticsSummary summary = analyticsService.getPlatformStats();
        
        assertEquals(1, summary.getTotalJobs());
        assertEquals(1, summary.getTotalApplications());
        assertEquals(1, summary.getShortlistedCount());
    }

    @Test
    void getPlatformStats_Exception() {
        when(restTemplate.getForObject("http://job-service/api/jobs", List.class))
                .thenThrow(new RuntimeException("Error"));

        AnalyticsSummary summary = analyticsService.getPlatformStats();
        assertEquals(0, summary.getTotalJobs());
    }

    @Test
    void getTopJobCategories_Success() {
        Map<String, Object> job1 = Map.of("category", "IT");
        Map<String, Object> job2 = Map.of("category", "IT");
        Map<String, Object> job3 = Map.of("category", "HR");
        Map<String, Object> job4 = Map.of(); // missing category -> "Other"
        
        when(restTemplate.getForObject("http://job-service/api/jobs", List.class))
                .thenReturn(List.of(job1, job2, job3, job4));

        Map<String, Long> categories = analyticsService.getTopJobCategories();
        
        assertEquals(2L, categories.get("IT"));
        assertEquals(1L, categories.get("HR"));
        assertEquals(1L, categories.get("Other"));
    }

    @Test
    void getTopJobCategories_Exception() {
        when(restTemplate.getForObject("http://job-service/api/jobs", List.class))
                .thenThrow(new RuntimeException("Error"));

        Map<String, Long> categories = analyticsService.getTopJobCategories();
        assertEquals(0, categories.size());
    }
}
