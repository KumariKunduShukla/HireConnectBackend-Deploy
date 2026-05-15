package com.hireconnect.subscription.controller;

import com.hireconnect.subscription.dto.PlanOfferDto;
import com.hireconnect.subscription.dto.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.RazorpayVerifyRequest;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionResource {

    @Autowired
    private SubscriptionService service;

    /** Published plan tiers and prices (same rules as {@link #createOrder}). */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanOfferDto>> listPlans() {
        return ResponseEntity.ok(service.listPlans());
    }

    /** PDF download — recruiter must match invoice owner. */
    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable int invoiceId,
            @RequestParam int recruiterId) {
        try {
            byte[] pdf = service.getInvoicePdf(invoiceId, recruiterId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"hireconnect-invoice-" + invoiceId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // STEP 1: Create Razorpay order — frontend calls this first
    // Returns orderId, amount, keyId to open Razorpay popup
    /** Accept kebab-case and snake_case paths so older clients and proxies keep working. */
    @PostMapping({"/create-order", "/create_order"})
    public ResponseEntity<?> createOrder(@RequestBody RazorpayOrderRequest request) {
        try {
            Map<String, Object> orderDetails = service.createOrder(request);
            return ResponseEntity.ok(orderDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // STEP 2: Verify payment after Razorpay popup closes
    // Activates subscription + generates invoice
    @PostMapping({"/verify-payment", "/verify_payment"})
    public ResponseEntity<?> verifyPayment(@RequestBody RazorpayVerifyRequest verifyRequest) {
        try {
            Invoice invoice = service.verifyPayment(verifyRequest);
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Cancel subscription
    @PutMapping("/cancel/{id}")
    public ResponseEntity<String> cancel(@PathVariable int id) {
        try {
            service.cancelSubscription(id);
            return ResponseEntity.ok("Subscription cancelled successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Renew subscription — creates new Razorpay order
    @PostMapping("/renew/{id}")
    public ResponseEntity<?> renew(@PathVariable int id) {
        try {
            Map<String, Object> orderDetails = service.renewSubscription(id);
            return ResponseEntity.ok(orderDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get all invoices for a subscription
    @GetMapping("/{subId}/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(@PathVariable int subId) {
        return ResponseEntity.ok(service.getInvoices(subId));
    }

    // Get all invoices for a recruiter
    @GetMapping("/recruiter/{recruiterId}/invoices")
    public ResponseEntity<List<Invoice>> getInvoicesByRecruiter(@PathVariable int recruiterId) {
        return ResponseEntity.ok(service.getInvoicesByRecruiter(recruiterId));
    }

    // Get all subscriptions for a recruiter
    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByRecruiter(@PathVariable int recruiterId) {
        return ResponseEntity.ok(service.getSubscriptionsByRecruiter(recruiterId));
    }

    // Get all subscriptions
    @GetMapping({"/all", ""})
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(service.getAllSubscriptions());
    }
}