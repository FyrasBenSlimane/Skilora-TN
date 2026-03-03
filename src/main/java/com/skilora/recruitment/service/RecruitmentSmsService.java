package com.skilora.recruitment.service;

import com.skilora.config.EnvConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * RecruitmentSmsService — WhatsApp messaging via Twilio REST API for Recruitment module.
 *
 * Configuration in .env:
 * TWILIO_RECRUITMENT_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * TWILIO_RECRUITMENT_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * TWILIO_RECRUITMENT_FROM_NUMBER=whatsapp:+1XXXXXXXXXX
 * TWILIO_RECRUITMENT_TO_NUMBER=whatsapp:+216XXXXXXXX
 *
 * Singleton with getInstance().
 */
public class RecruitmentSmsService {

    private static volatile RecruitmentSmsService instance;

    private final HttpClient httpClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String defaultToNumber;
    private final boolean configured;

    private RecruitmentSmsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.accountSid = EnvConfig.get("twilio.recruitment.account.sid", "").trim();
        this.authToken = EnvConfig.get("twilio.recruitment.auth.token", "").trim();
        this.fromNumber = EnvConfig.get("twilio.recruitment.from.number", "").trim();
        this.defaultToNumber = EnvConfig.get("twilio.recruitment.to.number", "").trim();

        this.configured = !accountSid.isEmpty() && !authToken.isEmpty()
                && !fromNumber.isEmpty() && !defaultToNumber.isEmpty()
                && accountSid.startsWith("AC");

        if (!configured) {
            System.out.println("[Recruitment WhatsApp] Twilio not configured — Messages disabled. "
                    + "Add TWILIO_RECRUITMENT_* variables in .env.");
        } else {
            System.out.println("[Recruitment WhatsApp] Twilio configured — Default to " + maskNumber(defaultToNumber));
        }
    }

    public static RecruitmentSmsService getInstance() {
        if (instance == null) {
            synchronized (RecruitmentSmsService.class) {
                if (instance == null) {
                    instance = new RecruitmentSmsService();
                }
            }
        }
        return instance;
    }

    /**
     * Check if Twilio is configured for recruitment.
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Sends a WhatsApp interview notification to the default number.
     *
     * @param candidateName  candidate's full name
     * @param jobTitle       job position title
     * @param interviewDate  scheduled date/time
     * @param interviewType  type of interview (e.g., VIDEO, IN_PERSON)
     */
    public void sendInterviewScheduled(String candidateName, String jobTitle,
                                       LocalDateTime interviewDate, String interviewType) {
        sendInterviewScheduled(candidateName, jobTitle, interviewDate, interviewType, defaultToNumber);
    }

    /**
     * Sends a WhatsApp interview notification to a specific number.
     */
    public void sendInterviewScheduled(String candidateName, String jobTitle,
                                       LocalDateTime interviewDate, String interviewType, String toNumber) {
        if (!configured) {
            System.out.println("[Recruitment WhatsApp] Twilio not configured — Message skipped.");
            return;
        }

        String dateStr = interviewDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));

        String message = String.format(
                "📅 ENTRETIEN PROGRAMMÉ\n\n" +
                        "Candidat: %s\n" +
                        "Poste: %s\n" +
                        "Date: %s\n" +
                        "Type: %s\n\n" +
                        "— Skilora Recruitment",
                candidateName, jobTitle, dateStr, interviewType);

        try {
            sendWhatsAppMessage(toNumber, message);
            System.out.println("[Recruitment WhatsApp] Interview notification sent to " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[Recruitment WhatsApp] Send error: " + e.getMessage());
        }
    }

    /**
     * Sends a WhatsApp application status update.
     *
     * @param candidateName candidate's full name
     * @param jobTitle      job position title
     * @param status        new application status
     */
    public void sendApplicationStatusUpdate(String candidateName, String jobTitle, String status) {
        sendApplicationStatusUpdate(candidateName, jobTitle, status, defaultToNumber);
    }

    /**
     * Sends a WhatsApp application status update to a specific number.
     */
    public void sendApplicationStatusUpdate(String candidateName, String jobTitle,
                                            String status, String toNumber) {
        if (!configured) {
            System.out.println("[Recruitment WhatsApp] Twilio not configured — Message skipped.");
            return;
        }

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String message = String.format(
                "📋 MISE À JOUR CANDIDATURE\n\n" +
                        "Candidat: %s\n" +
                        "Poste: %s\n" +
                        "Statut: %s\n" +
                        "Date: %s\n\n" +
                        "— Skilora Recruitment",
                candidateName, jobTitle, status, dateStr);

        try {
            sendWhatsAppMessage(toNumber, message);
            System.out.println("[Recruitment WhatsApp] Status update sent to " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[Recruitment WhatsApp] Send error: " + e.getMessage());
        }
    }

    /**
     * Sends a WhatsApp hire offer notification.
     *
     * @param candidateName candidate's full name
     * @param jobTitle      job position title
     * @param companyName   company name
     * @param salary        offered salary
     */
    public void sendHireOfferNotification(String candidateName, String jobTitle,
                                          String companyName, double salary) {
        sendHireOfferNotification(candidateName, jobTitle, companyName, salary, defaultToNumber);
    }

    /**
     * Sends a WhatsApp hire offer notification to a specific number.
     */
    public void sendHireOfferNotification(String candidateName, String jobTitle,
                                          String companyName, double salary, String toNumber) {
        if (!configured) {
            System.out.println("[Recruitment WhatsApp] Twilio not configured — Message skipped.");
            return;
        }

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String message = String.format(
                "🎉 OFFRE D'EMBAUCHE\n\n" +
                        "Candidat: %s\n" +
                        "Poste: %s\n" +
                        "Entreprise: %s\n" +
                        "Salaire proposé: %.2f TND\n" +
                        "Date: %s\n\n" +
                        "Félicitations!\n" +
                        "— Skilora Recruitment",
                candidateName, jobTitle, companyName, salary, dateStr);

        try {
            sendWhatsAppMessage(toNumber, message);
            System.out.println("[Recruitment WhatsApp] Hire offer notification sent to " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[Recruitment WhatsApp] Send error: " + e.getMessage());
        }
    }

    /**
     * Send a custom WhatsApp message.
     */
    public void sendCustomMessage(String message, String toNumber) {
        if (!configured) {
            System.out.println("[Recruitment WhatsApp] Twilio not configured — Message skipped.");
            return;
        }

        try {
            sendWhatsAppMessage(toNumber, message);
            System.out.println("[Recruitment WhatsApp] Custom message sent to " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[Recruitment WhatsApp] Send error: " + e.getMessage());
            throw new RuntimeException("WhatsApp error: " + e.getMessage(), e);
        }
    }

    private void sendWhatsAppMessage(String to, String bodyMessage)
            throws IOException, InterruptedException {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        String requestBody = "To=" + encode(to)
                + "&From=" + encode(fromNumber)
                + "&Body=" + encode(bodyMessage);

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();

        if (code < 200 || code >= 300) {
            String resp = response.body() != null ? response.body() : "";
            throw new IOException("Twilio HTTP " + code + ": " + safeShort(resp));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String maskNumber(String number) {
        if (number == null || number.length() < 4)
            return "****";
        return number.substring(0, number.length() - 4) + "****";
    }

    private static String safeShort(String s) {
        if (s == null) return "";
        String oneLine = s.replace("\n", " ").replace("\r", " ").trim();
        return oneLine.length() > 200 ? oneLine.substring(0, 200) + "..." : oneLine;
    }
}
