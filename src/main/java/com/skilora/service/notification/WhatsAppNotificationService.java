package com.skilora.service.notification;

import com.skilora.config.AppConfig;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * WhatsAppNotificationService
 *
 * Sends automated WhatsApp messages to candidates when an interview is scheduled.
 * Uses the Twilio WhatsApp API (no extra Maven dependency – relies on Java 17's
 * built-in java.net.http.HttpClient).
 *
 * Configuration (src/main/resources/config/application.properties):
 *   twilio.account.sid   – Twilio Account SID
 *   twilio.auth.token    – Twilio Auth Token
 *   twilio.whatsapp.from – Sender number (default Sandbox: +14155238886)
 *   whatsapp.notifications.enabled – true/false feature flag
 */
public class WhatsAppNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private static final String TWILIO_API_BASE = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static volatile WhatsAppNotificationService instance;

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean enabled;

    private WhatsAppNotificationService() {
        this.accountSid  = AppConfig.getProperty("twilio.account.sid", "");
        this.authToken   = AppConfig.getProperty("twilio.auth.token", "");
        this.fromNumber  = AppConfig.getProperty("twilio.whatsapp.from", "+14155238886");
        this.enabled     = Boolean.parseBoolean(AppConfig.getProperty("whatsapp.notifications.enabled", "true"));
    }

    public static WhatsAppNotificationService getInstance() {
        if (instance == null) {
            synchronized (WhatsAppNotificationService.class) {
                if (instance == null) {
                    instance = new WhatsAppNotificationService();
                }
            }
        }
        return instance;
    }

    /**
     * Sends an interview-scheduled WhatsApp notification to the candidate.
     * The call is non-blocking: it runs on a daemon thread so the UI is never frozen.
     *
     * @param interview   The saved interview
     * @param application The related job application (provides jobTitle, candidateName)
     * @param phoneNumber Candidate's phone number (e.g. "+21698765432")
     */
    public void sendInterviewScheduledNotification(Interview interview, Application application, String phoneNumber) {
        if (!enabled) {
            logger.info("WhatsApp notifications are disabled – skipping.");
            return;
        }

        if (!isConfigured()) {
            logger.warn("Twilio credentials not configured in application.properties – WhatsApp notification skipped.");
            return;
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            logger.warn("Candidate has no phone number on file – WhatsApp notification skipped for application {}",
                    application != null ? application.getId() : "?");
            return;
        }

        String normalizedPhone = normalizePhone(phoneNumber);
        String message         = buildMessage(interview, application);

        Thread sender = new Thread(() -> {
            try {
                sendMessage(normalizedPhone, message);
            } catch (Exception e) {
                logger.error("Failed to send WhatsApp notification to {}", normalizedPhone, e);
            }
        }, "WhatsApp-Notifier");
        sender.setDaemon(true);
        sender.start();
    }

    // ------------------------------------------------------------------
    //  Private helpers
    // ------------------------------------------------------------------

    private boolean isConfigured() {
        return accountSid  != null && !accountSid.isBlank()  && !accountSid.startsWith("YOUR_")
            && authToken   != null && !authToken.isBlank()   && !authToken.startsWith("YOUR_");
    }

    /**
     * Normalises a phone number to E.164 format accepted by Twilio.
     * Handles common Tunisian formats: 0X, +216X, 216X → +216X.
     */
    private String normalizePhone(String raw) {
        String cleaned = raw.replaceAll("[\\s\\-().]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        // Tunisia local format (starts with 0)
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }
        if (cleaned.startsWith("216")) {
            return "+" + cleaned;
        }
        if (cleaned.length() == 8) {
            // 8-digit Tunisian number without country code
            return "+216" + cleaned;
        }
        // Fallback: add + and trust the number is already in E.164 without the plus
        return "+" + cleaned;
    }

    /** Builds the WhatsApp message body from interview & application data. */
    private String buildMessage(Interview interview, Application application) {
        String candidateName = (application != null && application.getCandidateName() != null)
                ? application.getCandidateName() : "Candidat";
        String jobTitle      = (application != null && application.getJobTitle() != null)
                ? application.getJobTitle() : "N/A";
        String companyName   = "Skilora Tunisia";

        String date     = interview.getInterviewDate() != null ? interview.getInterviewDate().format(DATE_FMT) : "—";
        String time     = interview.getInterviewDate() != null ? interview.getInterviewDate().format(TIME_FMT) : "—";
        String type     = interview.getInterviewType() != null ? interview.getInterviewType().getDisplayName() : "—";
        String location = (interview.getLocation() != null && !interview.getLocation().isBlank())
                ? interview.getLocation() : "À préciser";

        return String.format(
            "Bonjour %s! 👋\n\n" +
            "🎉 *%s* vous a planifié un entretien.\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n" +
            "💼 *Poste :* %s\n" +
            "📅 *Date :* %s\n" +
            "⏰ *Heure :* %s\n" +
            "🎯 *Type :* %s\n" +
            "📍 *Lieu :* %s\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Bonne chance ! 🍀\n" +
            "_L'équipe Skilora Tunisia_",
            candidateName, companyName, jobTitle, date, time, type, location
        );
    }

    /**
     * Performs the actual HTTP POST to the Twilio Messages API.
     * Uses Java 17's built-in HttpClient – no extra dependency required.
     */
    private void sendMessage(String toPhone, String body) throws Exception {
        String url = String.format(TWILIO_API_BASE, accountSid);

        String formBody = "To="   + encode("whatsapp:" + toPhone)
                + "&From=" + encode("whatsapp:" + fromNumber)
                + "&Body=" + encode(body);

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpClient client   = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            logger.info("WhatsApp notification sent successfully to {} (status {})", toPhone, response.statusCode());
        } else {
            logger.error("WhatsApp notification failed for {} – HTTP {} – {}", toPhone, response.statusCode(), response.body());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
