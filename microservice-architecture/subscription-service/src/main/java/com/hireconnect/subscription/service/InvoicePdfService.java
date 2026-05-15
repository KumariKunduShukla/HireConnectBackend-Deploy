package com.hireconnect.subscription.service;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generatePdf(Invoice invoice, Subscription subscription) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(new Paragraph("HireConnect", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            doc.add(new Paragraph("Invoice / receipt", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Invoice number: " + invoice.getInvoiceId(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            if (subscription != null) {
                doc.add(new Paragraph("Subscription number: " + subscription.getSubscriptionId(), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            }
            doc.add(new Paragraph("Plan: " + nz(invoice.getPlanName()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            doc.add(new Paragraph("Amount (INR): " + String.format("%,.2f", invoice.getAmount()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            doc.add(new Paragraph("Payment mode: " + nz(invoice.getPaymentMode()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            doc.add(new Paragraph("Transaction reference: " + nz(invoice.getTransactionId()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            if (invoice.getPaymentDate() != null) {
                doc.add(new Paragraph("Paid at: " + invoice.getPaymentDate().format(FMT), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            }
            doc.add(new Paragraph("Status: " + nz(invoice.getStatus()), FontFactory.getFont(FontFactory.HELVETICA, 11)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Thank you for your business.", FontFactory.getFont(FontFactory.HELVETICA, 11)));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to build invoice PDF", e);
        }
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
