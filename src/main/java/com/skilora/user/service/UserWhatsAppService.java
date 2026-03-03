package com.skilora.user.service;

import com.skilora.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * UserWhatsAppService - Sends WhatsApp messages for User module (2FA OTPs).
 * Uses Twilio (Finance account) for WhatsApp delivery.
 */
public class UserWhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(UserWhatsAppService.class);
    private static UserWhatsAppService instance;

    private final HttpClient httpClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    private UserWhatsAppService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        // Reuse Finance Twilio credentials for User 2FA
        this.accountSid = EnvConfig.get("TWILIO_FINANCE_ACCOUNT_SID", "");
        this.authToken = EnvConfig.get("TWILIO_FINANCE_AUTH_TOKEN", "");
        this.fromNumber = EnvConfig.get("TWILIO_FINANCE_FROM_NUMBER", "whatsapp:+14155238886");
    }

    public static synchronized UserWhatsAppService getInstance() {
        if (instance == null) {
            instance = new UserWhatsAppService();
        }
        return instance;
    }

    /**
     * Check if WhatsApp service is configured.
     */
    public boolean isConfigured() {
        return !accountSid.isBlank() && !authToken.isBlank();
    }

    /**
     * Send 2FA OTP via WhatsApp.
     * 
     * @param toPhone Phone number in international format (e.g., +21654072071)
     * @param otp     The 6-digit OTP code
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> send2FAOTP(String toPhone, String otp) {
        if (!isConfigured()) {
            logger.warn("WhatsApp service not configured");
            return CompletableFuture.completedFuture(false);
        }

        if (toPhone == null || toPhone.isBlank()) {
            logger.warn("Cannot send 2FA OTP: phone number is empty");
            return CompletableFuture.completedFuture(false);
        }

        // Format phone for WhatsApp
        String formattedTo = formatWhatsAppNumber(toPhone);
        
        String message = String.format(
            "🔐 *Skilora 2FA Code*\n\n" +
            "Your verification code is: *%s*\n\n" +
            "This code expires in 5 minutes.\n" +
            "Do not share this code with anyone.\n\n" +
            "_If you didn't request this, please ignore this message._",
            otp
        );

        return sendWhatsAppMessage(formattedTo, message);
    }

    /**
     * Send a generic WhatsApp message.
     */
    public CompletableFuture<Boolean> sendWhatsAppMessage(String toNumber, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(
                    "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                    accountSid
                );

                String formData = String.format(
                    "From=%s&To=%s&Body=%s",
                    URLEncoder.encode(fromNumber, StandardCharsets.UTF_8),
                    URLEncoder.encode(toNumber, StandardCharsets.UTF_8),
                    URLEncoder.encode(message, StandardCharsets.UTF_8)
                );

                String auth = Base64.getEncoder().encodeToString(
                    (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)
                );

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .timeout(Duration.ofSeconds(30))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("WhatsApp 2FA OTP sent successfully to {}", toNumber);
                    return true;
                } else {
                    logger.warn("WhatsApp send failed: {} - {}", response.statusCode(), response.body());
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to send WhatsApp message", e);
                return false;
            }
        });
    }

    /**
     * Format phone number for WhatsApp.
     * Ensures the number has the whatsapp: prefix and international format.
     */
    private String formatWhatsAppNumber(String phone) {
        // Remove any existing whatsapp: prefix
        String cleaned = phone.replace("whatsapp:", "").trim();
        
        // Ensure it starts with +
        if (!cleaned.startsWith("+")) {
            // Assume Tunisia (+216) if no country code
            if (cleaned.startsWith("0")) {
                cleaned = "+216" + cleaned.substring(1);
            } else if (cleaned.length() == 8) {
                cleaned = "+216" + cleaned;
            } else {
                cleaned = "+" + cleaned;
            }
        }
        
        return "whatsapp:" + cleaned;
    }
}
