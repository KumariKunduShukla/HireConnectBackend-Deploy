package com.hireconnect.job.service;

import com.hireconnect.job.entity.Job;
import java.util.List;
import java.util.Optional;

public interface JobService {
    Job addJob(Job job); // Returning Job instead of void is better for REST APIs
    List<Job> getAllJobs();
    Optional<Job> getJobById(int jobId);
    Job updateJob(int jobId, Job job);
    void deleteJob(int jobId);
    List<Job> getJobsByCategory(String category);
    List<Job> getJobsByLocation(String location);
    List<Job> getJobsByRecruiter(int recruiterId);
    List<Job> searchJobs(String title, String category, double minSalary, double maxSalary);
}