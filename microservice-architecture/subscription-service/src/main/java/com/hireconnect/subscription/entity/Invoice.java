package com.hireconnect.subscription.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int invoiceId;

    @Column(nullable = false)
    private int subscriptionId;

    @Column(nullable = false)
    private int recruiterId;

    @Column(nullable = false)
    private double amount;

    private LocalDateTime paymentDate;

    // RAZORPAY, FREE
    private String paymentMode;

    // Razorpay payment_id stored here
    private String transactionId;

    // Plan name for easy reference on invoice
    private String planName;

    /** PAID for successful charges */
    @Column(nullable = false)
    private String status = "PAID";

    @PrePersist
    protected void onPayment() {
        this.paymentDate = LocalDateTime.now();
    }
}