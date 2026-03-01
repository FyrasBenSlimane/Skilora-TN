package com.skilora.model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * EmailService - SMTP email sending service
 * Uses Gmail SMTP for sending emails.
 * Credentials are loaded from environment variables.
 */
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String FROM_NAME = "Skilora Support";

    private final String fromEmail;
    private final String fromPassword;

    private static volatile EmailService instance;

    private EmailService() {
        String envEmail = System.getenv("SKILORA_MAIL_EMAIL");
        String envPassword = System.getenv("SKILORA_MAIL_PASSWORD");

        this.fromEmail = (envEmail != null && !envEmail.isBlank()) ? envEmail : "noreply.skilora@gmail.com";
        this.fromPassword = (envPassword != null && !envPassword.isBlank()) ? envPassword : "";

        if (envPassword == null || envPassword.isBlank()) {
            logger.warn("SKILORA_MAIL_PASSWORD not set. Email sending will fail. " +
                    "Set SKILORA_MAIL_EMAIL and SKILORA_MAIL_PASSWORD environment variables.");
        }
    }

    public static EmailService getInstance() {
        if (instance == null) {
            synchronized (EmailService.class) {
                if (instance == null) {
                    instance = new EmailService();
                }
            }
        }
        return instance;
    }

    /**
     * Send OTP for password reset
     */
    public CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String subject = "Votre code de verification Skilora";
                String body = buildOtpEmailBody(otpCode);
                sendEmail(toEmail, subject, body);
                return true;
            } catch (Exception e) {
                logger.error("Failed to send OTP email to: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * Send application notification email
     */
    public CompletableFuture<Boolean> sendApplicationNotification(String toEmail, String candidateName,
            String jobTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String subject = "Nouvelle candidature recue - " + jobTitle;
                String body = buildApplicationNotificationBody(candidateName, jobTitle);
                sendEmail(toEmail, subject, body);
                return true;
            } catch (Exception e) {
                logger.error("Failed to send application notification to: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * Send generic email
     */
    private void sendEmail(String toEmail, String subject, String body) throws MessagingException {
        if (fromPassword.isEmpty()) {
            throw new MessagingException("Email credentials not configured. Set SKILORA_MAIL_PASSWORD environment variable.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, fromPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(body, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);
            Transport.send(message);

            logger.info("Email sent successfully to: {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new MessagingException("Failed to send email", e);
        }
    }

    private String buildOtpEmailBody(String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f3f4f6;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                        <tr>
                            <td align="center" style="padding: 40px 0;">
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
                                    <tr>
                                        <td align="center" style="padding: 30px 40px; border-bottom: 1px solid #f0f0f0;">
                                            <h1 style="margin: 0; font-size: 24px; color: #1f2937; letter-spacing: -0.5px;">Skilora</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 40px 40px;">
                                            <h2 style="margin: 0 0 20px; font-size: 20px; color: #374151; font-weight: 600;">Verification de securite</h2>
                                            <p style="margin: 0 0 24px; font-size: 16px; line-height: 24px; color: #4b5563;">
                                                Bonjour,
                                                <br><br>
                                                Utilisez le code ci-dessous pour verifier votre identite dans l'application Skilora. Ce code est valable pour les prochaines 15 minutes.
                                            </p>
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                                <tr>
                                                    <td align="center" style="padding: 24px 0;">
                                                        <span style="display: inline-block; padding: 16px 32px; background-color: #f3f4f6; border-radius: 8px; border: 1px solid #e5e7eb; font-family: monospace; font-size: 32px; font-weight: 700; color: #2563eb; letter-spacing: 4px;">
                                                            %s
                                                        </span>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 24px 0 0; font-size: 14px; line-height: 20px; color: #6b7280; text-align: center;">
                                                Si vous n'avez pas demande ce code, veuillez ignorer cet email ou contacter le support.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color: #f9fafb; padding: 24px 40px; border-radius: 0 0 12px 12px; border-top: 1px solid #f0f0f0;">
                                            <p style="margin: 0; font-size: 12px; line-height: 20px; color: #9ca3af; text-align: center;">
                                                2026 Skilora Tunisia. Tous droits reserves.
                                                <br>
                                                Cet email est genere automatiquement.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(otpCode);
    }

    private String buildApplicationNotificationBody(String candidateName, String jobTitle) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #10b981; color: white; padding: 20px; text-align: center; }
                        .content { background: #f9fafb; padding: 30px; }
                        .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Nouvelle Candidature</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour,</p>
                            <p><strong>%s</strong> a postule pour le poste : <strong>%s</strong></p>
                            <p>Connectez-vous a votre espace employeur pour consulter le profil du candidat et sa candidature.</p>
                        </div>
                        <div class="footer">
                            <p>2026 Skilora Tunisia. Tous droits reserves.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(candidateName, jobTitle);
    }
}
