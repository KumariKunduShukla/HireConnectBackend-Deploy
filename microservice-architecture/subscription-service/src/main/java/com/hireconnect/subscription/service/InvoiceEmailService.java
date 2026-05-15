package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Emails invoice details to the recruiter after a successful payment (or free plan activation).
 */
@Slf4j
@Service
public class InvoiceEmailService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private InvoicePdfService invoicePdfService;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void sendInvoiceEmail(Invoice invoice, Subscription subscription, String toEmail) {
        if (mailSender == null) {
            log.warn("Mail not configured — skipping invoice email for invoice {}", invoice.getInvoiceId());
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No billing email — skipping invoice email for invoice {}", invoice.getInvoiceId());
            return;
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            log.warn("spring.mail.username not set — skipping invoice email");
            return;
        }

        byte[] pdf = invoicePdfService.generatePdf(invoice, subscription);
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(mailFrom.trim());
            helper.setTo(toEmail.trim());
            helper.setSubject("HireConnect — Invoice #" + invoice.getInvoiceId() + " (" + invoice.getPlanName() + ")");
            helper.setText(buildBody(invoice, subscription), false);
            String fname = "hireconnect-invoice-" + invoice.getInvoiceId() + ".pdf";
            helper.addAttachment(fname, new ByteArrayResource(pdf) {
                @Override
                public String getFilename() {
                    return fname;
                }
            });
            mailSender.send(mime);
            log.info("Invoice {} emailed with PDF to {}", invoice.getInvoiceId(), toEmail);
        } catch (org.springframework.mail.MailException e) {
            log.warn("Mail send failed ({}), trying fallback if possible", e.getMessage());
            sendPlainInvoiceEmail(invoice, subscription, toEmail);
        } catch (Exception e) {
            log.error("Failed to send invoice email: {}", e.getMessage());
        }
    }

    private void sendPlainInvoiceEmail(Invoice invoice, Subscription subscription, String toEmail) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom.trim());
        msg.setTo(toEmail.trim());
        msg.setSubject("HireConnect — Invoice #" + invoice.getInvoiceId() + " (" + invoice.getPlanName() + ")");
        msg.setText(buildBody(invoice, subscription));
        try {
            mailSender.send(msg);
            log.info("Invoice {} emailed (plain text) to {}", invoice.getInvoiceId(), toEmail);
        } catch (Exception e) {
            log.error("Failed to send plain invoice email: {}", e.getMessage());
        }
    }

    private String buildBody(Invoice invoice, Subscription subscription) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello,\n\n");
        sb.append("Thank you for your HireConnect subscription payment.\n\n");
        sb.append("Invoice number: ").append(invoice.getInvoiceId()).append('\n');
        sb.append("Subscription ID: ").append(subscription.getSubscriptionId()).append('\n');
        sb.append("Plan: ").append(invoice.getPlanName()).append('\n');
        sb.append("Amount (INR): ").append(String.format("%.2f", invoice.getAmount())).append('\n');
        sb.append("Payment mode: ").append(invoice.getPaymentMode()).append('\n');
        sb.append("Transaction reference: ").append(invoice.getTransactionId()).append('\n');
        if (invoice.getPaymentDate() != null) {
            sb.append("Paid at: ").append(invoice.getPaymentDate().format(FMT)).append('\n');
        }
        sb.append("Status: ").append(invoice.getStatus()).append('\n');
        sb.append("\nA PDF copy is attached when your mail client supports attachments.\n");
        sb.append("\n— HireConnect Billing\n");
        return sb.toString();
    }
}
