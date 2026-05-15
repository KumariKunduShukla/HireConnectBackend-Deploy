package com.hireconnect.analytics.service;

import com.hireconnect.analytics.entity.AnalyticsSummary;
import java.util.Map;

public interface AnalyticsService {
    int getJobViewCount(int jobId);
    int getAppCountByJob(int jobId);
    double getViewToApplyRatio(int jobId);
    double getTimeToHire(int jobId);
    
    // Aggregated stats
    AnalyticsSummary getPipelineStats(int recruiterId);
    AnalyticsSummary getPlatformStats();
    Map<String, Long> getTopJobCategories();
}