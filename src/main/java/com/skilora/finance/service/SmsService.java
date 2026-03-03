package com.skilora.finance.service;

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
 * SmsService — WhatsApp messaging via Twilio REST API for Finance module.
 *
 * Configuration in .env:
 * TWILIO_FINANCE_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * TWILIO_FINANCE_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * TWILIO_FINANCE_FROM_NUMBER=whatsapp:+1XXXXXXXXXX
 * TWILIO_FINANCE_TO_NUMBER=whatsapp:+216XXXXXXXX
 *
 * Singleton with getInstance().
 */
public class SmsService {

    private static volatile SmsService instance;

    private final HttpClient httpClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String toNumber;
    private final boolean configured;

    private SmsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.accountSid = EnvConfig.get("twilio.finance.account.sid", "").trim();
        this.authToken = EnvConfig.get("twilio.finance.auth.token", "").trim();
        this.fromNumber = EnvConfig.get("twilio.finance.from.number", "").trim();
        this.toNumber = EnvConfig.get("twilio.finance.to.number", "").trim();

        this.configured = !accountSid.isEmpty() && !authToken.isEmpty()
                && !fromNumber.isEmpty() && !toNumber.isEmpty()
                && accountSid.startsWith("AC");

        if (!configured) {
            System.out.println("[WhatsApp] Twilio not configured — Messages disabled. "
                    + "Add TWILIO_* variables in .env.");
        } else {
            System.out.println("[WhatsApp] Twilio configured — Sending to " + maskNumber(toNumber));
        }
    }

    public static SmsService getInstance() {
        if (instance == null) {
            synchronized (SmsService.class) {
                if (instance == null) {
                    instance = new SmsService();
                }
            }
        }
        return instance;
    }

    /**
     * Sends a WhatsApp payment confirmation message.
     *
     * @param montant         payment amount (e.g., 1000.00)
     * @param beneficiaire    beneficiary name
     * @param referenceProjet project reference
     * @param transactionId   Stripe ID (pi_XXXX)
     */
    public void sendPaymentSuccess(double montant, String beneficiaire,
                                   String referenceProjet, String transactionId) {
        if (!configured) {
            System.out.println("[WhatsApp] Twilio not configured — Message skipped.");
            return;
        }

        String dateStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String customMessage = String.format(
                "PAYMENT SUCCESSFUL\n\n" +
                        "Beneficiary: %s\n" +
                        "Amount: %.2f USD\n" +
                        "Project: %s\n" +
                        "Date: %s\n" +
                        "TX Ref: %s\n\n" +
                        "— Skilora Finance Support",
                beneficiaire, montant, referenceProjet, dateStr,
                (transactionId != null ? transactionId : "N/A"));

        try {
            sendWhatsAppMessage(toNumber, customMessage);
            System.out.println("[WhatsApp] Message sent successfully to " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[WhatsApp] Send error: " + e.getMessage());
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
