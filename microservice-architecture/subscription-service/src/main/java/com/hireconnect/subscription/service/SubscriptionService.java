package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.PlanOfferDto;
import com.hireconnect.subscription.dto.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.RazorpayVerifyRequest;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;

import java.util.List;
import java.util.Map;

public interface SubscriptionService {

    // Step 1: Create Razorpay order — returns order details to frontend
    Map<String, Object> createOrder(RazorpayOrderRequest request);

    // Step 2: Verify payment signature — activates subscription + generates invoice
    Invoice verifyPayment(RazorpayVerifyRequest verifyRequest);

    // Cancel subscription
    void cancelSubscription(int subscriptionId);

    // Renew subscription — creates new Razorpay order for renewal
    Map<String, Object> renewSubscription(int subscriptionId);

    // Get all invoices for a subscription
    List<Invoice> getInvoices(int subscriptionId);

    // Get all invoices for a recruiter
    List<Invoice> getInvoicesByRecruiter(int recruiterId);

    // Get subscription by recruiter
    List<Subscription> getSubscriptionsByRecruiter(int recruiterId);

    // Get all subscriptions
    List<Subscription> getAllSubscriptions();

    // Generate invoice manually
    Invoice generateInvoice(Subscription sub, String paymentMode, String transactionId);

    /** Published catalog (amounts match Razorpay order creation). */
    List<PlanOfferDto> listPlans();

    /** PDF invoice for download / email attachment; recruiter must own the invoice. */
    byte[] getInvoicePdf(int invoiceId, int recruiterId);
}