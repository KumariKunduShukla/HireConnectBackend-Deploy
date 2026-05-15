package com.hireconnect.subscription.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void testPlanOfferDto() {
        PlanOfferDto d = new PlanOfferDto("P", "N", 100.0, 10000, "INR", "year", "Desc");
        assertEquals("P", d.getCode());
        assertEquals("N", d.getDisplayName());
        assertEquals(100.0, d.getAmountInr());
        assertEquals(10000, d.getAmountPaise());
        assertEquals("INR", d.getCurrency());
        assertEquals("year", d.getBillingPeriod());
        assertEquals("Desc", d.getSummary());
    }

    @Test
    void testRazorpayOrderRequest() {
        RazorpayOrderRequest r = new RazorpayOrderRequest();
        r.setRecruiterId(1);
        r.setPlan("PRO");
        r.setBillingEmail("e@e.com");
        assertEquals(1, r.getRecruiterId());
        assertEquals("PRO", r.getPlan());
        assertEquals("e@e.com", r.getBillingEmail());
    }

    @Test
    void testRazorpayVerifyRequest() {
        RazorpayVerifyRequest r = new RazorpayVerifyRequest();
        r.setRazorpayOrderId("o1");
        r.setRazorpayPaymentId("p1");
        r.setRazorpaySignature("s1");
        r.setBillingEmail("e@e.com");
        assertEquals("o1", r.getRazorpayOrderId());
        assertEquals("p1", r.getRazorpayPaymentId());
        assertEquals("s1", r.getRazorpaySignature());
        assertEquals("e@e.com", r.getBillingEmail());
    }
}
