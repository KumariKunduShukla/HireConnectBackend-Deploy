package com.hireconnect.job.service;

import com.hireconnect.job.entity.Job;
import com.hireconnect.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j // Lombok annotation that automatically creates the 'log' object
@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobRepository jobRepository;

    @Override
    public Job addJob(Job job) {
        log.info("Attempting to save a new job: {}", job.getTitle());
        Job savedJob = jobRepository.save(job);
        log.info("Successfully saved job with ID: {}", savedJob.getJobId());
        return savedJob;
    }

    @Override
    public List<Job> getAllJobs() {
        log.info("Fetching all jobs from the database");
        List<Job> jobs = jobRepository.findAll();
        log.info("Found {} jobs in the database", jobs.size());
        return jobs;
    }

    @Override
    public Optional<Job> getJobById(int jobId) {
        log.info("Fetching job details for ID: {}", jobId);
        Optional<Job> job = jobRepository.findById(jobId);
        if (job.isEmpty()) {
            log.warn("Job not found for ID: {}", jobId);
        }
        return job;
    }

    @Override
    public Job updateJob(int jobId, Job updatedJob) {
        log.info("Attempting to update job with ID: {}", jobId);
        
        return jobRepository.findById(jobId).map(existingJob -> {
            if(updatedJob.getTitle() != null) existingJob.setTitle(updatedJob.getTitle());
            if(updatedJob.getCategory() != null) existingJob.setCategory(updatedJob.getCategory());
            if(updatedJob.getLocation() != null) existingJob.setLocation(updatedJob.getLocation());
            if(updatedJob.getSalaryMin() > 0) existingJob.setSalaryMin(updatedJob.getSalaryMin());
            if(updatedJob.getSalaryMax() > 0) existingJob.setSalaryMax(updatedJob.getSalaryMax());
            if(updatedJob.getDescription() != null) existingJob.setDescription(updatedJob.getDescription());
            if(updatedJob.getStatus() != null) existingJob.setStatus(updatedJob.getStatus());
            
            Job savedJob = jobRepository.save(existingJob);
            log.info("Successfully updated job with ID: {}", jobId);
            return savedJob;
        }).orElseThrow(() -> {
            log.error("Failed to update: Job not found with ID: {}", jobId);
            return new RuntimeException("Job not found with ID: " + jobId);
        });
    }

    @Override
    public void deleteJob(int jobId) {
        log.info("Attempting to delete job with ID: {}", jobId);
        jobRepository.deleteById(jobId);
        log.info("Successfully deleted job with ID: {}", jobId);
    }

    @Override
    public List<Job> getJobsByCategory(String category) {
        log.info("Fetching jobs for category: {}", category);
        return jobRepository.findByCategory(category);
    }

    @Override
    public List<Job> getJobsByLocation(String location) {
        log.info("Fetching jobs for location: {}", location);
        return jobRepository.findByLocation(location);
    }

    @Override
    public List<Job> getJobsByRecruiter(int recruiterId) {
        log.info("Fetching jobs posted by recruiter ID: {}", recruiterId);
        return jobRepository.findByPostedBy(recruiterId);
    }

    @Override
    public List<Job> searchJobs(String title, String category, double minSalary, double maxSalary) {
        log.info("Executing complex search - Title: {}, Category: {}, MinSalary: {}, MaxSalary: {}", 
                 title, category, minSalary, maxSalary);
        List<Job> results = jobRepository.searchJobs(title, category, minSalary, maxSalary);
        log.info("Search completed. Found {} matching jobs.", results.size());
        return results;
    }
}