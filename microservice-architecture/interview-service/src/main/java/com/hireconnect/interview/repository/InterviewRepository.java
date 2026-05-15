package com.hireconnect.interview.repository;

import com.hireconnect.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Integer> {
    List<Interview> findByApplicationId(int applicationId);
    List<Interview> findByStatus(String status);
    List<Interview> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end);
    void deleteByInterviewId(int interviewId);
}