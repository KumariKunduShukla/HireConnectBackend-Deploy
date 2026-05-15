package com.hireconnect.application.repository;

import com.hireconnect.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    
    List<Application> findByCandidateId(int candidateId);
    List<Application> findByJobId(int jobId);
    List<Application> findByStatus(String status);
    
    // Prevents a candidate from applying to the exact same job twice
    Optional<Application> findFirstByJobIdAndCandidateId(int jobId, int candidateId);
    
    int countByJobId(int jobId);
}