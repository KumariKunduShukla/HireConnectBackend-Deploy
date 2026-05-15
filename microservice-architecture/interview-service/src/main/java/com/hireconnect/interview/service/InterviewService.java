package com.hireconnect.interview.service;

import com.hireconnect.interview.entity.Interview;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {
    Interview scheduleInterview(Interview interview);
    Interview confirmInterview(int id);
    Interview rescheduleInterview(int id, LocalDateTime newTime);
    void cancelInterview(int id);
    List<Interview> getByApplication(int appId);
}