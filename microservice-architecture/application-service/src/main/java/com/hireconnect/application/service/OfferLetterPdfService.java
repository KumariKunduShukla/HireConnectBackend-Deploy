package com.hireconnect.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class OfferLetterPdfService {

    public byte[] generateOfferLetter(String candidateName, String jobTitle, String companyName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(new Paragraph("HireConnect Official Offer Letter", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Dear " + candidateName + ","));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("We are pleased to offer you the position of " + jobTitle + " at " + companyName + ". We were very impressed with your background and skills, and we believe you will be a valuable addition to our team."));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Position: " + jobTitle));
            doc.add(new Paragraph("Company: " + companyName));
            doc.add(new Paragraph("Location: Remote / As per agreement"));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Your hard work and dedication have led to this opportunity, and we look forward to having you join us. Please log in to the HireConnect platform to accept this offer and complete your onboarding."));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Congratulations!"));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Sincerely,"));
            doc.add(new Paragraph(companyName + " HR Team"));
            doc.add(new Paragraph("Verified by HireConnect Platform"));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Offer Letter PDF", e);
        }
    }
}
