package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private InvoicePdfService invoicePdfService;

    @InjectMocks
    private InvoiceEmailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mailFrom", "no-reply@hireconnect.com");
    }

    @Test
    void sendInvoiceEmail_Success() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1);
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10);
        
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(invoicePdfService.generatePdf(any(), any())).thenReturn(new byte[]{1, 2, 3});

        service.sendInvoiceEmail(invoice, subscription, "test@test.com");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendInvoiceEmail_MailSenderNull() {
        ReflectionTestUtils.setField(service, "mailSender", null);
        service.sendInvoiceEmail(new Invoice(), new Subscription(), "test@test.com");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendInvoiceEmail_NoEmail() {
        service.sendInvoiceEmail(new Invoice(), new Subscription(), null);
        service.sendInvoiceEmail(new Invoice(), new Subscription(), "");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendInvoiceEmail_MailError_FallbackToPlain() {
        Invoice invoice = new Invoice();
        Subscription sub = new Subscription();
        
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(invoicePdfService.generatePdf(any(), any())).thenReturn(new byte[]{1, 2, 3});
        
        doThrow(new MailSendException("Failed")).when(mailSender).send(any(MimeMessage.class));

        service.sendInvoiceEmail(invoice, sub, "test@test.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void sendInvoiceEmail_GeneralError() {
        Invoice invoice = new Invoice();
        Subscription sub = new Subscription();
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Fatal"));
        
        service.sendInvoiceEmail(invoice, sub, "test@test.com");
    }
}
