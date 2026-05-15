package com.hireconnect.subscription.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int subscriptionId;

    @Column(nullable = false)
    private int recruiterId;

    // FREE, PROFESSIONAL, ENTERPRISE
    @Column(nullable = false)
    private String plan;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // PENDING, ACTIVE, CANCELLED, EXPIRED
    @Column(nullable = false)
    private String status;

    private double amountPaid;

    // Razorpay order ID stored here before payment
    private String razorpayOrderId;

    /** Where to send invoice emails (usually recruiter login email). */
    @Column(length = 255)
    private String billingEmail;

    @PrePersist
    protected void onCreate() {
        this.startDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING"; // PENDING until payment verified
        }
    }
}