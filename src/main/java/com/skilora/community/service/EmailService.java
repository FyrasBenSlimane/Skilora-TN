package com.skilora.community.service;

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
        // TODO: Move back to env vars after testing
        String envEmail = System.getenv("SKILORA_MAIL_EMAIL");
        String envPassword = System.getenv("SKILORA_MAIL_PASSWORD");

        this.fromEmail = (envEmail != null && !envEmail.isBlank()) ? envEmail : "firaszx232@gmail.com";
        this.fromPassword = (envPassword != null && !envPassword.isBlank()) ? envPassword : "lgqu ninc tzec lnvp";

        logger.info("EmailService initialized with: {}", this.fromEmail);
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
                String subject = "\uD83D\uDD10 Skilora — Your verification code";
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
        // Build individual digit cells — premium glassmorphism style
        StringBuilder digitCells = new StringBuilder();
        for (int i = 0; i < otpCode.length(); i++) {
            char c = otpCode.charAt(i);
            digitCells.append(String.format(
                "<td align=\"center\" style=\"padding: 0 5px;\">" +
                "<div style=\"width: 54px; height: 68px; line-height: 68px; " +
                "background: linear-gradient(145deg, #ffffff, #f0f4ff); " +
                "border: 2px solid #c7d2fe; border-bottom: 3px solid #818cf8; " +
                "border-radius: 14px; " +
                "font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'Consolas', monospace; " +
                "font-size: 30px; font-weight: 800; color: #312e81; text-align: center; " +
                "letter-spacing: 0;\">%c</div></td>", c));
        }

        return """
                <!DOCTYPE html>
                <html lang="en" xmlns:v="urn:schemas-microsoft-com:vml">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light dark">
                    <meta name="supported-color-schemes" content="light dark">
                    <title>Skilora — Verification Code</title>
                    <!--[if mso]>
                    <style>
                        table { border-collapse: collapse; }
                        td { font-family: Arial, sans-serif; }
                    </style>
                    <![endif]-->
                    <style>
                        @media (prefers-color-scheme: dark) {
                            .email-bg { background-color: #0f172a !important; }
                            .card-bg { background-color: #1e293b !important; }
                            .title-text { color: #f1f5f9 !important; }
                            .body-text { color: #94a3b8 !important; }
                            .footer-text { color: #64748b !important; }
                            .digit-box { background: linear-gradient(145deg, #1e293b, #0f172a) !important; border-color: #4338ca !important; color: #a5b4fc !important; }
                        }
                        @media only screen and (max-width: 480px) {
                            .main-table { width: 100%% !important; }
                            .content-pad { padding-left: 24px !important; padding-right: 24px !important; }
                            .digit-box { width: 42px !important; height: 56px !important; line-height: 56px !important; font-size: 24px !important; }
                            .digit-gap { padding: 0 3px !important; }
                        }
                    </style>
                </head>
                <body style="margin: 0; padding: 0; background-color: #eef2f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" class="email-bg" style="background-color: #eef2f7;">
                        <tr>
                            <td align="center" style="padding: 40px 16px 48px 16px;">

                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" class="main-table" style="max-width: 560px;">

                                    <!-- ====== BRAND HEADER ====== -->
                                    <tr>
                                        <td align="center" style="padding: 0 0 28px 0;">
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="vertical-align: middle; padding-right: 12px;">
                                                        <div style="width: 40px; height: 40px; background: linear-gradient(135deg, #4f46e5, #7c3aed, #a855f7); border-radius: 12px; box-shadow: 0 2px 8px rgba(79,70,229,0.3);"></div>
                                                    </td>
                                                    <td style="vertical-align: middle;">
                                                        <span style="font-size: 24px; font-weight: 800; color: #1e1b4b; letter-spacing: -0.5px;">Skilora</span>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- ====== MAIN CARD ====== -->
                                    <tr>
                                        <td>
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" class="card-bg" style="background-color: #ffffff; border-radius: 20px; box-shadow: 0 1px 2px rgba(0,0,0,0.04), 0 4px 16px rgba(0,0,0,0.06), 0 12px 40px rgba(79,70,229,0.06); overflow: hidden;">

                                                <!-- Gradient accent bar -->
                                                <tr>
                                                    <td style="height: 5px; background: linear-gradient(90deg, #4f46e5, #7c3aed, #a855f7, #ec4899);"></td>
                                                </tr>

                                                <!-- Hero Section -->
                                                <tr>
                                                    <td class="content-pad" style="padding: 44px 48px 0 48px;">

                                                        <!-- Icon -->
                                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                                            <tr>
                                                                <td align="center" style="padding-bottom: 20px;">
                                                                    <div style="width: 72px; height: 72px; background: linear-gradient(135deg, #eef2ff, #e0e7ff); border-radius: 50%%; line-height: 72px; text-align: center; font-size: 34px; box-shadow: 0 2px 12px rgba(99,102,241,0.12);">\uD83D\uDD10</div>
                                                                </td>
                                                            </tr>
                                                        </table>

                                                        <!-- Title -->
                                                        <h1 class="title-text" style="margin: 0 0 6px 0; font-size: 24px; font-weight: 800; color: #1e1b4b; text-align: center; letter-spacing: -0.4px;">Verification Code</h1>
                                                        <p class="body-text" style="margin: 0 0 4px 0; font-size: 15px; line-height: 24px; color: #6b7280; text-align: center;">Use the code below to verify your identity</p>
                                                        <p class="body-text" style="margin: 0; font-size: 15px; line-height: 24px; color: #6b7280; text-align: center;">and reset your password.</p>
                                                    </td>
                                                </tr>

                                                <!-- ====== OTP Digits ====== -->
                                                <tr>
                                                    <td align="center" class="content-pad" style="padding: 32px 48px 28px 48px;">
                                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                %s
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>

                                                <!-- Timer Pill -->
                                                <tr>
                                                    <td align="center" style="padding: 0 48px 8px 48px;">
                                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td style="background: linear-gradient(135deg, #fef3c7, #fef9c3); border: 1px solid #fcd34d; border-radius: 24px; padding: 8px 20px;">
                                                                    <p style="margin: 0; font-size: 13px; font-weight: 600; color: #92400e; text-align: center;">
                                                                        \u23F1\uFE0F Expires in <span style="color: #b45309; font-weight: 700;">2:00</span> minutes
                                                                    </p>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>

                                                <!-- Security Tips Box -->
                                                <tr>
                                                    <td class="content-pad" style="padding: 24px 48px 0 48px;">
                                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0;">
                                                            <tr>
                                                                <td style="padding: 20px 24px;">
                                                                    <p style="margin: 0 0 12px 0; font-size: 12px; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.8px;">\uD83D\uDEE1\uFE0F Security Tips</p>
                                                                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                                                        <tr>
                                                                            <td style="padding: 3px 0; font-size: 13px; line-height: 20px; color: #64748b;">
                                                                                <span style="color: #22c55e; font-size: 11px;">\u2713</span>&nbsp;&nbsp;Never share this code with anyone
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding: 3px 0; font-size: 13px; line-height: 20px; color: #64748b;">
                                                                                <span style="color: #22c55e; font-size: 11px;">\u2713</span>&nbsp;&nbsp;Skilora will never ask for your password
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td style="padding: 3px 0; font-size: 13px; line-height: 20px; color: #64748b;">
                                                                                <span style="color: #22c55e; font-size: 11px;">\u2713</span>&nbsp;&nbsp;This code is valid for one use only
                                                                            </td>
                                                                        </tr>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>

                                                <!-- Divider -->
                                                <tr>
                                                    <td style="padding: 28px 48px 0 48px;"><div style="height: 1px; background: linear-gradient(90deg, transparent, #e2e8f0, transparent);"></div></td>
                                                </tr>

                                                <!-- Didn't request section -->
                                                <tr>
                                                    <td class="content-pad" style="padding: 20px 48px 36px 48px;">
                                                        <p class="body-text" style="margin: 0; font-size: 13px; line-height: 20px; color: #94a3b8; text-align: center;">
                                                            Didn't request this code? No worries — just ignore this email.<br>Your account is safe and no changes have been made.
                                                        </p>
                                                    </td>
                                                </tr>

                                            </table>
                                        </td>
                                    </tr>

                                    <!-- ====== FOOTER ====== -->
                                    <tr>
                                        <td align="center" style="padding: 32px 0 0 0;">
                                            <!-- Divider dots -->
                                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="padding: 0 3px;"><div style="width: 4px; height: 4px; background-color: #c7d2fe; border-radius: 50%%;"></div></td>
                                                    <td style="padding: 0 3px;"><div style="width: 4px; height: 4px; background-color: #a5b4fc; border-radius: 50%%;"></div></td>
                                                    <td style="padding: 0 3px;"><div style="width: 4px; height: 4px; background-color: #c7d2fe; border-radius: 50%%;"></div></td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 16px 0 0 0;">
                                            <p class="footer-text" style="margin: 0 0 2px 0; font-size: 12px; font-weight: 500; color: #94a3b8;">&copy; 2026 Skilora Tunisia</p>
                                            <p class="footer-text" style="margin: 0 0 8px 0; font-size: 11px; color: #cbd5e1;">Empowering careers through technology</p>
                                            <p class="footer-text" style="margin: 0; font-size: 11px; color: #cbd5e1;">This is an automated message — please do not reply.</p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(digitCells.toString());
    }
}
