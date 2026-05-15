package com.hireconnect.profile.repository;

import com.hireconnect.profile.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateProfile, Integer> {
}