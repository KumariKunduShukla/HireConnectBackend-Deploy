package com.hireconnect.subscription.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testSubscription() {
        Subscription s = new Subscription();
        s.setSubscriptionId(1);
        s.setRecruiterId(2);
        s.setPlan("PRO");
        s.setAmountPaid(100.0);
        s.setStatus("ACTIVE");
        s.setRazorpayOrderId("O1");
        s.setStartDate(LocalDateTime.now());
        s.setEndDate(LocalDateTime.now());
        s.setBillingEmail("e@e.com");

        assertEquals(1, s.getSubscriptionId());
        assertEquals(2, s.getRecruiterId());
        assertEquals("PRO", s.getPlan());
        assertEquals(100.0, s.getAmountPaid());
        assertEquals("ACTIVE", s.getStatus());
        assertEquals("O1", s.getRazorpayOrderId());
        assertNotNull(s.getStartDate());
        assertNotNull(s.getEndDate());
        assertEquals("e@e.com", s.getBillingEmail());
    }

    @Test
    void testInvoice() {
        Invoice i = new Invoice();
        i.setInvoiceId(1);
        i.setSubscriptionId(2);
        i.setRecruiterId(3);
        i.setAmount(100.0);
        i.setPaymentMode("UPI");
        i.setTransactionId("T1");
        i.setPaymentDate(LocalDateTime.now());
        i.setPlanName("PRO");
        i.setStatus("PAID");

        assertEquals(1, i.getInvoiceId());
        assertEquals(2, i.getSubscriptionId());
        assertEquals(3, i.getRecruiterId());
        assertEquals(100.0, i.getAmount());
        assertEquals("UPI", i.getPaymentMode());
        assertEquals("T1", i.getTransactionId());
        assertNotNull(i.getPaymentDate());
        assertEquals("PRO", i.getPlanName());
        assertEquals("PAID", i.getStatus());
    }
}
