package com.hireconnect.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Catalog entry returned to recruiters when choosing a subscription tier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanOfferDto {
    /** FREE, PROFESSIONAL, ENTERPRISE — matches {@code RazorpayOrderRequest.plan}. */
    private String code;
    private String displayName;
    /** Amount in INR (not paise). */
    private double amountInr;
    /** Razorpay amount in paise. */
    private int amountPaise;
    private String currency;
    private String billingPeriod;
    private String summary;
}
