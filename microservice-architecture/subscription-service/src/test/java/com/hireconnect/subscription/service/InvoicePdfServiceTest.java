package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @InjectMocks
    private InvoicePdfService service;

    @Test
    void generatePdf_Success() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1);
        invoice.setPlanName("PROFESSIONAL");
        invoice.setAmount(999.0);
        invoice.setPaymentMode("ONLINE");
        invoice.setTransactionId("txn_123");
        invoice.setPaymentDate(LocalDateTime.now());
        invoice.setStatus("PAID");

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10);
        subscription.setRecruiterId(5);

        byte[] pdf = service.generatePdf(invoice, subscription);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
