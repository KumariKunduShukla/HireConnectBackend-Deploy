package com.hireconnect.auth.repository;

import com.hireconnect.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface AuthRepository extends JpaRepository<UserCredential, Integer> {

    Optional<UserCredential> findByEmail(String email);

    Optional<UserCredential> findByUserId(int userId);

    boolean existsByEmail(String email);

    @Transactional
    void deleteByUserId(int userId);

    // Fetch all recruiters waiting for admin approval
    List<UserCredential> findByRoleAndStatus(String role, String status);

    // Fetch ALL users by role (used to notify all admins)
    List<UserCredential> findByRole(String role);
}	