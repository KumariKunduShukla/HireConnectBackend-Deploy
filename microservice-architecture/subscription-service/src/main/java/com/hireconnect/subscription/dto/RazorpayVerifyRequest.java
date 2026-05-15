package com.hireconnect.subscription.dto;

import lombok.Data;

@Data
public class RazorpayVerifyRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    /** Invoice email (usually same as signed-in recruiter). */
    private String billingEmail;
}