package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByRecruiterId(int recruiterId);

    List<Subscription> findByStatus(String status);

    Optional<Subscription> findBySubscriptionId(int subscriptionId);

    // Find active subscription for a recruiter
    Optional<Subscription> findByRecruiterIdAndStatus(int recruiterId, String status);

    // Find subscription by Razorpay order ID (used during payment verification)
    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);

    long countByPlan(String plan);
}