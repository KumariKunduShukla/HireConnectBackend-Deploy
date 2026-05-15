package com.hireconnect.profile.repository;

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<UserProfile, Integer> {

    // Inherited automatically from base class
    Optional<UserProfile> findByEmail(String email);
    Optional<UserProfile> findByProfileId(int profileId);
    void deleteByProfileId(int profileId);

    // Custom Query: Targets the Candidate subclass for the mobile number
    @Query("SELECT c FROM CandidateProfile c WHERE c.mobile = :mobile")
    Optional<CandidateProfile> findByMobile(@Param("mobile") Long mobile);

    // Custom Query: Filters by class type to separate Candidates and Recruiters
    @Query("SELECT u FROM UserProfile u WHERE " +
           "(UPPER(:role) = 'CANDIDATE' AND TYPE(u) = CandidateProfile) OR " +
           "(UPPER(:role) = 'RECRUITER' AND TYPE(u) = RecruiterProfile)")
    List<UserProfile> findAllByRole(@Param("role") String role);
}