package com.skilora.community.service;

import com.skilora.config.EnvConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * EmailService - SMTP email sending service
 * Uses Gmail SMTP for sending emails.
 * Credentials are loaded from environment variables, falling back to .env file.
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
        String envEmail = EnvConfig.get("SKILORA_MAIL_EMAIL", "");
        String envPassword = EnvConfig.get("SKILORA_MAIL_PASSWORD", "");

        if (envEmail.isBlank() || envPassword.isBlank()) {
            logger.warn("Email credentials not set. Set SKILORA_MAIL_EMAIL and SKILORA_MAIL_PASSWORD in .env file.");
        }

        this.fromEmail = envEmail;
        this.fromPassword = envPassword;

        if (!this.fromEmail.isBlank()) {
            logger.info("EmailService initialized with: {}", this.fromEmail);
        }
    }

    /**
     * Load key=value pairs from .env file in the working directory.
     */
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
     * Send a security alert email to an admin when suspicious login activity is detected.
     */
    public CompletableFuture<Boolean> sendSecurityAlertEmail(String adminEmail, String username,
                                                              int failedAttempts, String photoPath,
                                                              boolean isLockedOut) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String subject = isLockedOut
                        ? "🔒 Skilora Security — Account Locked: " + username
                        : "⚠️ Skilora Security — Suspicious Login: " + username;
                String body = buildSecurityAlertEmailBody(username, failedAttempts, photoPath, isLockedOut);
                sendEmail(adminEmail, subject, body);
                return true;
            } catch (Exception e) {
                logger.error("Failed to send security alert email to: {}", adminEmail, e);
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
        // Build individual digit cells — dark luxury style with bgcolor for email client compat
        StringBuilder digitCells = new StringBuilder();
        for (int i = 0; i < otpCode.length(); i++) {
            char c = otpCode.charAt(i);
            digitCells.append(String.format(
                "<td align=\"center\" style=\"padding: 0 5px;\">" +
                "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td align=\"center\" width=\"50\" height=\"62\" bgcolor=\"#0a0a0a\" " +
                "style=\"width: 50px; height: 62px; background-color: #0a0a0a; " +
                "border: 1px solid #2a2a2a; border-radius: 10px; " +
                "font-family: 'SF Mono', 'Fira Code', Consolas, monospace; " +
                "font-size: 26px; font-weight: 600; color: #c9a84c; text-align: center; " +
                "letter-spacing: 1px;\">%c</td></tr></table></td>", c));
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="dark only">
                    <meta name="supported-color-schemes" content="dark only">
                    <title>Skilora — Verification Code</title>
                    <!--[if mso]>
                    <style>
                        table { border-collapse: collapse; }
                        td { font-family: Arial, sans-serif; }
                    </style>
                    <![endif]-->
                    <style>
                        body, table, td, div, p { -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }
                        u + .body .full-wrap { width: 100%% !important; }
                        @media only screen and (max-width: 480px) {
                            .main-table { width: 100%% !important; }
                            .pad { padding-left: 20px !important; padding-right: 20px !important; }
                        }
                    </style>
                </head>
                <body class="body" style="margin: 0; padding: 0; width: 100%%; background-color: #0a0a0a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; -webkit-font-smoothing: antialiased;" bgcolor="#0a0a0a">

                <!-- Full-width dark wrapper -->
                <table role="presentation" class="full-wrap" border="0" cellpadding="0" cellspacing="0" width="100%%" bgcolor="#0a0a0a" style="background-color: #0a0a0a;">
                <tr>
                <td align="center" bgcolor="#0a0a0a" style="background-color: #0a0a0a; padding: 40px 16px 48px 16px;">

                <!-- Centered container -->
                <table role="presentation" class="main-table" border="0" cellpadding="0" cellspacing="0" width="520" style="max-width: 520px; width: 100%%;">

                    <!-- ══════ BRAND ══════ -->
                    <tr>
                        <td align="center" bgcolor="#0a0a0a" style="background-color: #0a0a0a; padding: 0 0 28px 0;">
                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td style="vertical-align: middle; padding-right: 14px;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0"><tr><td width="34" height="34" bgcolor="#c9a84c" style="width: 34px; height: 34px; background-color: #c9a84c; border-radius: 9px; font-size: 0; line-height: 0;">&nbsp;</td></tr></table>
                                    </td>
                                    <td style="vertical-align: middle;">
                                        <span style="font-size: 20px; font-weight: 300; color: #ffffff; letter-spacing: 8px; text-transform: uppercase; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">SKILORA</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- ══════ CARD ══════ -->
                    <tr>
                        <td>
                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" bgcolor="#111111" style="background-color: #111111; border-radius: 12px; border-collapse: separate;">

                                <!-- Accent line -->
                                <tr>
                                    <td height="3" bgcolor="#c9a84c" style="background-color: #c9a84c; font-size: 0; line-height: 0; border-radius: 12px 12px 0 0;">&nbsp;</td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="40" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Icon -->
                                <tr>
                                    <td align="center" bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td align="center" width="60" height="60" bgcolor="#0a0a0a" style="width: 60px; height: 60px; background-color: #0a0a0a; border: 1px solid #1a1a1a; border-radius: 50%%; text-align: center; font-size: 0; line-height: 0;"><img src="https://img.icons8.com/ios-filled/40/c9a84c/shield-with-a-checkmark.png" alt="" width="28" height="28" style="display: inline-block; border: 0; width: 28px; height: 28px;" /></td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="24" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Title + Subtitle -->
                                <tr>
                                    <td align="center" bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <p style="margin: 0 0 10px 0; font-size: 15px; font-weight: 600; color: #ffffff; letter-spacing: 5px; text-transform: uppercase; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">VERIFICATION CODE</p>
                                        <p style="margin: 0; font-size: 13px; line-height: 22px; color: #888888; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">Use the code below to verify your identity and reset your password.</p>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="32" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- ══════ OTP DIGITS ══════ -->
                                <tr>
                                    <td align="center" bgcolor="#111111" style="background-color: #111111; padding: 0 32px;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                            <tr>
                                                %s
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="24" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Timer -->
                                <tr>
                                    <td align="center" bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td bgcolor="#0a0a0a" style="background-color: #0a0a0a; border: 1px solid #1a1a1a; border-radius: 20px; padding: 7px 22px;">
                                                    <p style="margin: 0; font-size: 11px; font-weight: 600; color: #666666; letter-spacing: 2px; text-transform: uppercase; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">EXPIRES IN <span style="color: #c9a84c; font-weight: 700;">2:00</span></p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="28" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Thin separator -->
                                <tr>
                                    <td bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <div style="height: 1px; background-color: #1a1a1a; font-size: 0; line-height: 0;">&nbsp;</div>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="24" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Security Tips -->
                                <tr>
                                    <td bgcolor="#111111" style="background-color: #111111; padding: 0 40px;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" bgcolor="#0a0a0a" style="background-color: #0a0a0a; border: 1px solid #1a1a1a; border-radius: 8px;">
                                            <tr>
                                                <td bgcolor="#0a0a0a" style="background-color: #0a0a0a; padding: 18px 22px;">
                                                    <p style="margin: 0 0 12px 0; font-size: 10px; font-weight: 600; color: #666666; letter-spacing: 4px; text-transform: uppercase; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">SECURITY TIPS</p>
                                                    <p style="margin: 0 0 6px 0; font-size: 13px; line-height: 20px; color: #555555; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;"><span style="color: #c9a84c;">&#9670;</span>&nbsp; Never share this code with anyone</p>
                                                    <p style="margin: 0 0 6px 0; font-size: 13px; line-height: 20px; color: #555555; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;"><span style="color: #c9a84c;">&#9670;</span>&nbsp; Skilora will never ask for your password</p>
                                                    <p style="margin: 0; font-size: 13px; line-height: 20px; color: #555555; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;"><span style="color: #c9a84c;">&#9670;</span>&nbsp; This code is valid for one use only</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="24" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Thin separator -->
                                <tr>
                                    <td bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <div style="height: 1px; background-color: #1a1a1a; font-size: 0; line-height: 0;">&nbsp;</div>
                                    </td>
                                </tr>

                                <!-- Spacer -->
                                <tr><td height="20" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                                <!-- Didn't request -->
                                <tr>
                                    <td align="center" bgcolor="#111111" style="background-color: #111111; padding: 0 48px;">
                                        <p style="margin: 0; font-size: 12px; line-height: 20px; color: #444444; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">Didn't request this code? No worries — just ignore this email. Your account is safe and no changes have been made.</p>
                                    </td>
                                </tr>

                                <!-- Bottom padding -->
                                <tr><td height="36" bgcolor="#111111" style="background-color: #111111; font-size: 0; line-height: 0;">&nbsp;</td></tr>

                            </table>
                        </td>
                    </tr>

                    <!-- ══════ FOOTER ══════ -->
                    <tr>
                        <td align="center" bgcolor="#0a0a0a" style="background-color: #0a0a0a; padding: 32px 0 0 0;">
                            <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td style="padding: 0 4px;"><div style="width: 3px; height: 3px; background-color: #333333; border-radius: 50%%; font-size: 0; line-height: 0;">&nbsp;</div></td>
                                    <td style="padding: 0 4px;"><div style="width: 3px; height: 3px; background-color: #444444; border-radius: 50%%; font-size: 0; line-height: 0;">&nbsp;</div></td>
                                    <td style="padding: 0 4px;"><div style="width: 3px; height: 3px; background-color: #333333; border-radius: 50%%; font-size: 0; line-height: 0;">&nbsp;</div></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" bgcolor="#0a0a0a" style="background-color: #0a0a0a; padding: 18px 0 0 0;">
                            <p style="margin: 0 0 4px 0; font-size: 11px; font-weight: 300; color: #444444; letter-spacing: 4px; text-transform: uppercase; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">&copy; 2026 SKILORA TUNISIA</p>
                            <p style="margin: 0; font-size: 10px; color: #333333; letter-spacing: 1px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">This is an automated message — please do not reply.</p>
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

    /**
     * Build the HTML email body for a security alert.
     */
    private String buildSecurityAlertEmailBody(String username, int failedAttempts,
                                                String photoPath, boolean isLockedOut) {
        String statusLabel = isLockedOut ? "ACCOUNT LOCKED" : "SUSPICIOUS ACTIVITY";
        String statusColor = isLockedOut ? "#e74c3c" : "#f39c12";
        String photoNote = photoPath != null
                ? "A camera capture was taken and saved on the server."
                : "Camera was not available — no photo captured.";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Skilora Security Alert</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0a0a0a;">
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                <tr><td align="center" style="padding: 40px 0;">
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="520"
                       style="background-color: #111111; border: 1px solid #1a1a1a; border-radius: 12px; overflow: hidden;">

                    <!-- Header -->
                    <tr>
                        <td align="center" bgcolor="#111111"
                            style="padding: 32px 40px 16px 40px; background-color: #111111;">
                            <p style="margin: 0 0 8px 0; font-size: 11px; font-weight: 500; letter-spacing: 6px;
                                      text-transform: uppercase; color: %s;">🛡️ %s</p>
                            <h1 style="margin: 0; font-size: 22px; font-weight: 600; color: #ffffff;
                                       font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">
                                Security Alert</h1>
                        </td>
                    </tr>

                    <!-- Details -->
                    <tr>
                        <td bgcolor="#111111" style="padding: 16px 40px 32px 40px; background-color: #111111;">
                            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%"
                                   style="background-color: #0a0a0a; border: 1px solid #1f1f1f; border-radius: 8px;">
                                <tr><td style="padding: 20px;">
                                    <p style="margin: 0 0 12px 0; font-size: 14px; color: #cccccc;
                                              font-family: -apple-system, sans-serif;">
                                        <strong style="color: #ffffff;">Username:</strong> %s</p>
                                    <p style="margin: 0 0 12px 0; font-size: 14px; color: #cccccc;
                                              font-family: -apple-system, sans-serif;">
                                        <strong style="color: #ffffff;">Failed Attempts:</strong> %d</p>
                                    <p style="margin: 0 0 12px 0; font-size: 14px; color: #cccccc;
                                              font-family: -apple-system, sans-serif;">
                                        <strong style="color: #ffffff;">Camera:</strong> %s</p>
                                    <p style="margin: 0; font-size: 12px; color: #888888;
                                              font-family: -apple-system, sans-serif;">
                                        Please review this alert in the Skilora Admin Dashboard.</p>
                                </td></tr>
                            </table>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td align="center" bgcolor="#0a0a0a" style="padding: 20px; background-color: #0a0a0a;">
                            <p style="margin: 0; font-size: 10px; color: #444444; letter-spacing: 1px;
                                      font-family: -apple-system, sans-serif;">
                                &copy; 2026 SKILORA SECURITY — Automated Alert</p>
                        </td>
                    </tr>
                </table>
                </td></tr></table>
                </body>
                </html>
                """
                .formatted(statusColor, statusLabel, username, failedAttempts, photoNote);
    }
}
