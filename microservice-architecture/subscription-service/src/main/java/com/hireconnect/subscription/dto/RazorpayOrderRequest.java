package com.hireconnect.subscription.dto;

import lombok.Data;

@Data
public class RazorpayOrderRequest {
    /** Nullable so a bad/missing id returns a clear 400 from the service instead of Jackson bind errors. */
    private Integer recruiterId;
    private String plan; // FREE, PROFESSIONAL, ENTERPRISE
    /** Invoice / receipt email (recruiter login email). */
    private String billingEmail;
}