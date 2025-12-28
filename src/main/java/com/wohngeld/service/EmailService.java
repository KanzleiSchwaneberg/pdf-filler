package com.wohngeld.service;

import com.wohngeld.model.WohngeldAntragRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

//@Service
//@Slf4j
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//    private final boolean mailEnabled;
//
//    @Value("${spring.mail.username:}")
//    private String senderEmail;
//
//    @Autowired(required = false)
//    public EmailService(JavaMailSender mailSender) {
//        this.mailSender = mailSender;
//        this.mailEnabled = mailSender != null;
//        if (!mailEnabled) {
//            log.warn("Email-Service ist DEAKTIVIERT (kein MailSender konfiguriert)");
//        }
//    }
//
//    public boolean isEnabled() {
//        return mailEnabled;
//    }
//
//    @Async
//    public CompletableFuture<Boolean> sendAntragEmail(
//            String recipientEmail,
//            String pdfPath,
//            WohngeldAntragRequest antragData
//    ) {
//        if (!mailEnabled) {
//            log.warn("Email-Versand übersprungen (deaktiviert). Empfänger wäre: {}", recipientEmail);
//            return CompletableFuture.completedFuture(false);
//        }
//
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(senderEmail);
//            helper.setTo(recipientEmail);
//            helper.setSubject(createSubject(antragData));
//            helper.setText(createTextBody(antragData), createHtmlBody(antragData));
//
//            // PDF-Anhang
//            File pdfFile = new File(pdfPath);
//            if (pdfFile.exists()) {
//                FileSystemResource resource = new FileSystemResource(pdfFile);
//                helper.addAttachment(pdfFile.getName(), resource);
//            }
//
//            mailSender.send(message);
//            log.info("Email erfolgreich gesendet an: {}", recipientEmail);
//
//            return CompletableFuture.completedFuture(true);
//
//        } catch (MessagingException e) {
//            log.error("Fehler beim Email-Versand an {}: {}", recipientEmail, e.getMessage());
//            return CompletableFuture.completedFuture(false);
//        }
//    }
//
//    private String createSubject(WohngeldAntragRequest data) {
//        String name = data.getAntragsteller().getFullName();
//        return "Wohngeldantrag - " + name;
//    }
//
//    private String createTextBody(WohngeldAntragRequest data) {
//        String name = data.getAntragsteller().getFullName();
//        String adresse = data.getAdresse().getFullAddress();
//        String telefon = data.getAntragsteller().getTelefon() != null
//                ? data.getAntragsteller().getTelefon() : "Nicht angegeben";
//        String email = data.getAntragsteller().getEmail() != null
//                ? data.getAntragsteller().getEmail() : "Nicht angegeben";
//
//        return String.format("""
//                Sehr geehrte Damen und Herren,
//
//                anbei übersende ich Ihnen meinen Wohngeldantrag (Mietzuschuss).
//
//                Antragsteller: %s
//                Anschrift: %s
//                Telefon: %s
//                Email: %s
//
//                Bei Rückfragen stehe ich Ihnen gerne zur Verfügung.
//
//                Mit freundlichen Grüßen
//                %s
//                """, name, adresse, telefon, email, name);
//    }
//
//    private String createHtmlBody(WohngeldAntragRequest data) {
//        String name = data.getAntragsteller().getFullName();
//        String adresse = data.getAdresse().getFullAddress();
//        String telefon = data.getAntragsteller().getTelefon() != null
//                ? data.getAntragsteller().getTelefon() : "Nicht angegeben";
//        String email = data.getAntragsteller().getEmail() != null
//                ? data.getAntragsteller().getEmail() : "Nicht angegeben";
//
//        return String.format("""
//                <!DOCTYPE html>
//                <html>
//                <head>
//                    <meta charset="utf-8">
//                    <style>
//                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
//                        .header { background-color: #f5f5f5; padding: 20px; border-bottom: 2px solid #0066cc; }
//                        .content { padding: 20px; }
//                        .footer { font-size: 12px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
//                        .info-box { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }
//                    </style>
//                </head>
//                <body>
//                    <div class="header">
//                        <h2>Wohngeldantrag (Mietzuschuss)</h2>
//                    </div>
//                    <div class="content">
//                        <p>Sehr geehrte Damen und Herren,</p>
//                        <p>anbei übersende ich Ihnen meinen Wohngeldantrag (Mietzuschuss).</p>
//
//                        <div class="info-box">
//                            <strong>Antragstellerdaten:</strong><br>
//                            Name: %s<br>
//                            Anschrift: %s<br>
//                            Telefon: %s<br>
//                            Email: %s
//                        </div>
//
//                        <p>Bei Rückfragen stehe ich Ihnen gerne zur Verfügung.</p>
//                        <p>Mit freundlichen Grüßen<br><strong>%s</strong></p>
//                    </div>
//                    <div class="footer">
//                        <p>Diese Email wurde automatisch generiert.</p>
//                    </div>
//                </body>
//                </html>
//                """, name, adresse, telefon, email, name);
//    }
//}
