package com.hireconnect.analytics.controller;

import com.hireconnect.analytics.entity.AnalyticsSummary;
import com.hireconnect.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsResourceTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsResource analyticsResource;

    @Test
    void getRecruiterStats() {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalJobs(5);
        when(analyticsService.getPipelineStats(1)).thenReturn(summary);

        ResponseEntity<AnalyticsSummary> response = analyticsResource.getRecruiterStats(1);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getTotalJobs());
    }

    @Test
    void getPlatformStats() {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalJobs(10);
        when(analyticsService.getPlatformStats()).thenReturn(summary);

        ResponseEntity<AnalyticsSummary> response = analyticsResource.getPlatformStats();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(10, response.getBody().getTotalJobs());
    }

    @Test
    void getJobViewCount() {
        when(analyticsService.getJobViewCount(1)).thenReturn(100);

        ResponseEntity<Integer> response = analyticsResource.getJobViewCount(1);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(100, response.getBody());
    }
}
