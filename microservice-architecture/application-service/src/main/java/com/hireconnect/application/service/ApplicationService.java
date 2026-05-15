package com.hireconnect.application.service;

import com.hireconnect.application.entity.Application;
import java.util.List;
import java.util.Optional;

public interface ApplicationService {
    Application submitApplication(Application application);
    List<Application> getByCandidate(int candidateId);
    List<Application> getByJob(int jobId);
    Application updateStatus(int applicationId, String status);
    void withdrawApplication(int applicationId);
    Optional<Application> getById(int applicationId);
    int countByJobId(int jobId);
    List<Application> getAllApplications();
    void reTriggerNotification(int applicationId);
}