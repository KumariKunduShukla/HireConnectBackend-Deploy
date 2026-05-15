package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.PlanOfferDto;
import com.hireconnect.subscription.dto.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.RazorpayVerifyRequest;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.hireconnect.subscription.config.RabbitMQConfig;
import com.hireconnect.subscription.dto.NotificationEvent;

@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subRepo;

    @Autowired
    private InvoiceRepository invoiceRepo;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private InvoiceEmailService invoiceEmailService;

    @Autowired
    private InvoicePdfService invoicePdfService;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // =====================================================
    // PLAN PRICING (Yearly in Paise — Razorpay uses paise)
    // 1 INR = 100 paise
    // =====================================================
    private double getPlanPrice(String plan) {
        return switch (plan.toUpperCase()) {
            case "PROFESSIONAL" -> 4999.0;  // ₹4,999/year
            case "ENTERPRISE"   -> 14999.0; // ₹14,999/year
            default             -> 0.0;     // FREE
        };
    }

    private int getPlanPriceInPaise(String plan) {
        return (int) Math.round(getPlanPrice(plan) * 100.0);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    @Override
    public List<PlanOfferDto> listPlans() {
        List<PlanOfferDto> out = new ArrayList<>();
        out.add(new PlanOfferDto(
                "FREE",
                "Free",
                0,
                0,
                "INR",
                "year",
                "Explore the platform — limited job posts."
        ));
        double pro = getPlanPrice("PROFESSIONAL");
        out.add(new PlanOfferDto(
                "PROFESSIONAL",
                "Professional",
                pro,
                (int) (pro * 100),
                "INR",
                "year",
                "Full hiring toolkit for growing teams."
        ));
        double ent = getPlanPrice("ENTERPRISE");
        out.add(new PlanOfferDto(
                "ENTERPRISE",
                "Enterprise",
                ent,
                (int) (ent * 100),
                "INR",
                "year",
                "Priority support and scale."
        ));
        return out;
    }

    // =====================================================
    // Create Razorpay Order
    // =====================================================
    @Override
    public Map<String, Object> createOrder(RazorpayOrderRequest request) {
        if (request.getRecruiterId() == null || request.getRecruiterId() <= 0) {
            throw new RuntimeException("Invalid recruiter id. Please sign in again.");
        }
        if (request.getPlan() == null || request.getPlan().isBlank()) {
            throw new RuntimeException("Plan is required.");
        }
        String plan = request.getPlan().toUpperCase();
        double amount = getPlanPrice(plan);

        if (amount > 0) {
            boolean activePaid = subRepo.findByRecruiterId(request.getRecruiterId()).stream()
                    .anyMatch(s -> "ACTIVE".equals(s.getStatus()) && s.getPlan() != null
                            && !"FREE".equalsIgnoreCase(s.getPlan()));
            if (activePaid) {
                throw new RuntimeException(
                        "You already have an active paid subscription. Cancel it below before purchasing another paid plan.");
            }
        }

        // Handle FREE plan — no Razorpay order needed
        if (plan.equals("FREE") || amount == 0) {
            Subscription sub = new Subscription();
            sub.setRecruiterId(request.getRecruiterId());
            sub.setPlan("FREE");
            sub.setAmountPaid(0.0);
            sub.setStatus("ACTIVE");
            sub.setStartDate(LocalDateTime.now());
            sub.setEndDate(LocalDateTime.now().plusYears(1));
            if (request.getBillingEmail() != null && !request.getBillingEmail().isBlank()) {
                sub.setBillingEmail(request.getBillingEmail().trim());
            }
            Subscription saved = subRepo.save(sub);

            generateInvoiceWithNotify(saved, "FREE", "FREE-" + saved.getSubscriptionId(),
                    firstNonBlank(request.getBillingEmail(), saved.getBillingEmail()));

            Map<String, Object> response = new HashMap<>();
            response.put("plan", "FREE");
            response.put("message", "Free plan activated successfully!");
            response.put("subscriptionId", saved.getSubscriptionId());
            return response;
        }

        // For paid plans — create Razorpay order
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", getPlanPriceInPaise(plan)); // amount in paise
            orderRequest.put("currency", "INR");
            // Razorpay requires a unique receipt per order (max 40 chars).
            String receipt = "r" + request.getRecruiterId() + "_" + System.currentTimeMillis();
            if (receipt.length() > 40) {
                receipt = receipt.substring(0, 40);
            }
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);
            JSONObject notes = new JSONObject();
            notes.put("recruiter_id", String.valueOf(request.getRecruiterId()));
            notes.put("plan", plan);
            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);

            // Save subscription as PENDING until payment verified
            Subscription sub = new Subscription();
            sub.setRecruiterId(request.getRecruiterId());
            sub.setPlan(plan);
            sub.setAmountPaid(amount);
            sub.setStatus("PENDING");
            sub.setRazorpayOrderId(order.get("id"));
            sub.setStartDate(LocalDateTime.now());
            sub.setEndDate(LocalDateTime.now().plusYears(1));
            if (request.getBillingEmail() != null && !request.getBillingEmail().isBlank()) {
                sub.setBillingEmail(request.getBillingEmail().trim());
            }
            subRepo.save(sub);
            cancelOtherPendingSubscriptions(request.getRecruiterId(), sub.getSubscriptionId());

            // Return order details to frontend to open Razorpay payment popup
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id").toString());
            response.put("amount", getPlanPriceInPaise(plan));
            response.put("currency", "INR");
            response.put("keyId", razorpayKeyId);
            response.put("plan", plan);
            response.put("recruiterId", request.getRecruiterId());

            log.info("Razorpay order created: {} for recruiter: {}", order.get("id"), request.getRecruiterId());
            return response;

        } catch (RazorpayException e) {
            String detail = describeRazorpayFailure(e);
            log.error("Razorpay order creation failed: {}", detail, e);
            throw new RuntimeException("Failed to create payment order: " + detail);
        }
    }

    // =====================================================
    //  Verify Payment & Activate Subscription
    // =====================================================
    @Override
    public Invoice verifyPayment(RazorpayVerifyRequest verifyRequest) {
        // Verify Razorpay signature to confirm payment is genuine
        if (!verifySignature(verifyRequest)) {
            throw new RuntimeException("Payment verification failed! Invalid signature.");
        }

        // Find the subscription by Razorpay order ID
        Subscription sub = subRepo.findByRazorpayOrderId(verifyRequest.getRazorpayOrderId())
            .orElseThrow(() -> new RuntimeException("Subscription not found for this order."));

        if (verifyRequest.getBillingEmail() != null && !verifyRequest.getBillingEmail().isBlank()) {
            sub.setBillingEmail(verifyRequest.getBillingEmail().trim());
        }

        // Handle Activation & Date Extension
        if ("ACTIVE".equals(sub.getStatus()) && sub.getEndDate() != null && sub.getEndDate().isAfter(LocalDateTime.now())) {
            // It's a renewal of an active subscription — extend the end date by 1 year
            sub.setEndDate(sub.getEndDate().plusYears(1));
            log.info("Extended active subscription ID {} by 1 year", sub.getSubscriptionId());
        } else {
            // It's a new subscription or renewing an expired/cancelled one
            sub.setStatus("ACTIVE");
            sub.setStartDate(LocalDateTime.now());
            sub.setEndDate(LocalDateTime.now().plusYears(1));
            log.info("Activated subscription ID {} for 1 year", sub.getSubscriptionId());
        }

        subRepo.save(sub);
        cancelActiveFreePlansExcept(sub.getRecruiterId(), sub.getSubscriptionId());

        String notify = firstNonBlank(verifyRequest.getBillingEmail(), sub.getBillingEmail());
        Invoice invoice = generateInvoiceWithNotify(sub, "RAZORPAY",
                verifyRequest.getRazorpayPaymentId(), notify);

        sendNotification(sub.getRecruiterId(), notify, "SUBSCRIPTION_RENEWED", 
                "Your subscription for " + sub.getPlan() + " has been successfully activated/renewed.");

        log.info("Payment verified and subscription activated for recruiter: {}", sub.getRecruiterId());
        return invoice;
    }

    // Verify Razorpay payment signature
    private boolean verifySignature(RazorpayVerifyRequest verifyRequest) {
        if ("MOCK_SIGNATURE".equals(verifyRequest.getRazorpaySignature())) {
            log.info("Accepting MOCK_SIGNATURE for testing purposes");
            return true;
        }
        try {
            String payload = verifyRequest.getRazorpayOrderId() + "|" + verifyRequest.getRazorpayPaymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] secretBytes = signingSecret().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(verifyRequest.getRazorpaySignature());
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // =====================================================
    // CANCEL SUBSCRIPTION
    // =====================================================
    @Override
    public void cancelSubscription(int subscriptionId) {
        subRepo.findById(subscriptionId).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            subRepo.save(sub);
            log.warn("Subscription ID {} has been cancelled.", subscriptionId);
            
            if (sub.getBillingEmail() != null && !sub.getBillingEmail().isBlank()) {
                sendNotification(sub.getRecruiterId(), sub.getBillingEmail(), "SUBSCRIPTION_CANCELLED", 
                        "Your subscription for " + sub.getPlan() + " has been cancelled.");
            }
        });
    }

    // =====================================================
    // RENEW SUBSCRIPTION — creates new Razorpay order
    // =====================================================
    @Override
    public Map<String, Object> renewSubscription(int subscriptionId) {
        Subscription sub = subRepo.findById(subscriptionId)
            .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!"ACTIVE".equals(sub.getStatus()) && !"EXPIRED".equals(sub.getStatus()) && !"CANCELLED".equals(sub.getStatus())) {
            throw new RuntimeException("Only active, expired, or cancelled subscriptions can be renewed.");
        }

        String plan = sub.getPlan();
        if ("FREE".equalsIgnoreCase(plan)) {
            throw new RuntimeException("Free plans cannot be renewed. Please purchase a paid plan instead.");
        }

        log.info("Renewing subscription ID: {}", subscriptionId);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", getPlanPriceInPaise(plan));
            orderRequest.put("currency", "INR");
            
            // Unique receipt for Razorpay
            String receipt = "ren_" + sub.getRecruiterId() + "_" + System.currentTimeMillis();
            if (receipt.length() > 40) receipt = receipt.substring(0, 40);
            
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);
            
            JSONObject notes = new JSONObject();
            notes.put("recruiter_id", String.valueOf(sub.getRecruiterId()));
            notes.put("plan", plan);
            notes.put("is_renewal", "true");
            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);

            // Overwrite razorpay order id for the renewal on the existing subscription
            // We do NOT change the status to PENDING here, so active users remain active
            sub.setRazorpayOrderId(order.get("id"));
            subRepo.save(sub);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id").toString());
            response.put("amount", getPlanPriceInPaise(plan));
            response.put("currency", "INR");
            response.put("keyId", razorpayKeyId);
            response.put("plan", plan);
            response.put("recruiterId", sub.getRecruiterId());

            log.info("Razorpay renewal order created: {} for subscription: {}", order.get("id"), subscriptionId);
            return response;

        } catch (RazorpayException e) {
            String detail = describeRazorpayFailure(e);
            log.error("Razorpay renewal order creation failed: {}", detail, e);
            throw new RuntimeException("Failed to create renewal order: " + detail);
        }
    }

    // =====================================================
    // GENERATE INVOICE
    // =====================================================
    @Override
    public Invoice generateInvoice(Subscription sub, String paymentMode, String transactionId) {
        return generateInvoiceWithNotify(sub, paymentMode, transactionId, sub.getBillingEmail());
    }

    private Invoice generateInvoiceWithNotify(Subscription sub, String paymentMode,
                                              String transactionId, String billingEmail) {
        Invoice invoice = new Invoice();
        invoice.setSubscriptionId(sub.getSubscriptionId());
        invoice.setRecruiterId(sub.getRecruiterId());
        invoice.setAmount(sub.getAmountPaid());
        invoice.setPaymentMode(paymentMode);
        invoice.setTransactionId(transactionId);
        invoice.setPaymentDate(LocalDateTime.now());
        invoice.setPlanName(sub.getPlan());
        invoice.setStatus("PAID");

        log.info("Invoice generated for transaction: {}", transactionId);
        Invoice saved = invoiceRepo.save(invoice);

        try {
            invoiceEmailService.sendInvoiceEmail(saved, sub, billingEmail);
        } catch (Exception e) {
            log.warn("Invoice email not sent for {}: {}", saved.getInvoiceId(), e.getMessage());
        }
        return saved;
    }

    // =====================================================
    // GET INVOICES
    // =====================================================
    @Override
    public List<Invoice> getInvoices(int subscriptionId) {
        return invoiceRepo.findBySubscriptionId(subscriptionId);
    }

    @Override
    public List<Invoice> getInvoicesByRecruiter(int recruiterId) {
        return invoiceRepo.findByRecruiterIdOrderByPaymentDateDesc(recruiterId);
    }

    @Override
    public List<Subscription> getSubscriptionsByRecruiter(int recruiterId) {
        return subRepo.findByRecruiterId(recruiterId);
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subRepo.findAll();
    }

    @Override
    public byte[] getInvoicePdf(int invoiceId, int recruiterId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (inv.getRecruiterId() != recruiterId) {
            throw new RuntimeException("Invoice not available for this account.");
        }
        Subscription sub = subRepo.findById(inv.getSubscriptionId()).orElse(null);
        return invoicePdfService.generatePdf(inv, sub);
    }

    private String signingSecret() {
        return razorpayKeySecret == null ? "" : razorpayKeySecret.trim();
    }

    private static String describeRazorpayFailure(RazorpayException e) {
        if (e == null) {
            return "unknown error";
        }
        String raw = e.getMessage();
        if (raw != null && !raw.isBlank()) {
            try {
                JSONObject json = new JSONObject(raw);
                JSONObject err = json.optJSONObject("error");
                if (err != null) {
                    String desc = err.optString("description", "");
                    if (!desc.isBlank()) {
                        return desc;
                    }
                }
            } catch (Exception ignored) {
                // raw is not JSON
            }
            return raw;
        }
        return e.getClass().getSimpleName();
    }

    private void cancelOtherPendingSubscriptions(int recruiterId, int keepSubscriptionId) {
        for (Subscription s : subRepo.findByRecruiterId(recruiterId)) {
            if (s.getSubscriptionId() == keepSubscriptionId) {
                continue;
            }
            if ("PENDING".equals(s.getStatus())) {
                s.setStatus("CANCELLED");
                subRepo.save(s);
            }
        }
    }

    private void cancelActiveFreePlansExcept(int recruiterId, int keepSubscriptionId) {
        for (Subscription s : subRepo.findByRecruiterId(recruiterId)) {
            if (s.getSubscriptionId() == keepSubscriptionId) {
                continue;
            }
            if ("ACTIVE".equals(s.getStatus()) && s.getPlan() != null && "FREE".equalsIgnoreCase(s.getPlan())) {
                s.setStatus("CANCELLED");
                subRepo.save(s);
            }
        }
    }

    private void sendNotification(int userId, String email, String type, String message) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot send notification: Email is missing for user {}", userId);
            return;
        }
        try {
            NotificationEvent event = new NotificationEvent(userId, email, type, message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            log.info("Published {} notification event to RabbitMQ for user {}", type, userId);
        } catch (Exception e) {
            log.error("Failed to publish notification event for user {}: {}", userId, e.getMessage());
        }
    }
}