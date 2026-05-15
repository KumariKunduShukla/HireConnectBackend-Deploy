package com.hireconnect.analytics.entity;

import lombok.Data;

@Data
public class AnalyticsSummary {
    private int totalJobs;
    private int totalApplications;
    private int interviewsScheduledCount;
    private int shortlistedCount;
    private int offeredCount;
    private int rejectedCount;
    private double avgTimeToHireDays;
    private double viewToApplyRatio;
}