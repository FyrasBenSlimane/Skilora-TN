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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StripePaymentService — Stripe payment integration (TEST mode).
 *
 * Maps known Stripe test card numbers to predefined Payment Method IDs
 * (pm_card_visa, pm_card_mastercard, etc.) to avoid Radar issues in test mode.
 *
 * API key loaded from config/application.properties: stripe.secret.key
 * Singleton with getInstance().
 */
public class StripePaymentService {

    private static final String STRIPE_BASE = "https://api.stripe.com/v1";

    // Mapping test card numbers → predefined Stripe Payment Method IDs (TEST mode only)
    private static final Map<String, String> TEST_CARD_PM_MAP;
    static {
        TEST_CARD_PM_MAP = new HashMap<>();
        TEST_CARD_PM_MAP.put("4242424242424242", "pm_card_visa");
        TEST_CARD_PM_MAP.put("4000056655665556", "pm_card_visa_debit");
        TEST_CARD_PM_MAP.put("5555555555554444", "pm_card_mastercard");
        TEST_CARD_PM_MAP.put("5200828282828210", "pm_card_mastercard_debit");
        TEST_CARD_PM_MAP.put("378282246310005", "pm_card_amex");
        TEST_CARD_PM_MAP.put("6011111111111117", "pm_card_discover");
        TEST_CARD_PM_MAP.put("3056930009020004", "pm_card_diners");
        TEST_CARD_PM_MAP.put("4000002500003155", "pm_card_threeDSecure2Required");
    }

    private static volatile StripePaymentService instance;

    private final HttpClient httpClient;
    private final String secretKey;

    private StripePaymentService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        String key = EnvConfig.get("stripe.secret.key", "");
        if (key == null || key.isBlank() || key.equals("PLACEHOLDER")) {
            System.err.println("[Stripe] stripe.secret.key not configured — set STRIPE_SECRET_KEY in .env");
            key = "";
        }
        this.secretKey = key;
    }

    public static StripePaymentService getInstance() {
        if (instance == null) {
            synchronized (StripePaymentService.class) {
                if (instance == null) {
                    instance = new StripePaymentService();
                }
            }
        }
        return instance;
    }

    /**
     * Resolves the Payment Method ID to use.
     * If card number matches a known test card → returns predefined pm_card_xxx.
     * Otherwise → attempts to create a PM via the API.
     */
    public String resolvePaymentMethodId(String cardNumber, String expMonth, String expYear, String cvc)
            throws IOException, InterruptedException {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");

        String predefinedPm = TEST_CARD_PM_MAP.get(digits);
        if (predefinedPm != null) {
            System.out.println("[Stripe] Test card detected → using predefined PM: " + predefinedPm);
            return predefinedPm;
        }

        System.out.println("[Stripe] Attempting PaymentMethod creation via API...");
        return createPaymentMethodViaApi(digits,
                expMonth.replaceAll("[^0-9]", ""),
                expYear.replaceAll("[^0-9]", ""),
                cvc.replaceAll("[^0-9]", ""));
    }

    private String createPaymentMethodViaApi(String cardNumber, String expMonth, String expYear, String cvc)
            throws IOException, InterruptedException {
        String body = "type=card"
                + "&card[number]=" + encode(cardNumber)
                + "&card[exp_month]=" + encode(expMonth)
                + "&card[exp_year]=" + encode(expYear)
                + "&card[cvc]=" + encode(cvc);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_BASE + "/payment_methods"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = sendRequest(request);
        String json = response.body() != null ? response.body() : "";

        String pmId = extractJsonValue(json, "id", "pm_");
        if (pmId == null || pmId.isBlank()) {
            throw new IOException("Stripe: unable to create PaymentMethod. Response: " + safeShort(json));
        }
        return pmId;
    }

    /**
     * Creates a Stripe PaymentIntent.
     *
     * @param amountCents amount in cents (e.g., 10000 = 100.00 USD)
     * @return the payment_intent id (e.g., pi_XXXX)
     */
    public String createPaymentIntent(long amountCents) throws IOException, InterruptedException {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Amount must be > 0.");
        }

        String body = "amount=" + encode(String.valueOf(amountCents))
                + "&currency=" + encode("usd")
                + "&payment_method_types[]=" + encode("card")
                + "&confirm=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_BASE + "/payment_intents"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = sendRequest(request);
        String json = response.body() != null ? response.body() : "";

        String intentId = extractJsonValue(json, "id", "pi_");
        if (intentId == null || intentId.isBlank()) {
            throw new IOException("Stripe: unable to extract PaymentIntent id. Response: " + safeShort(json));
        }
        return intentId;
    }

    /**
     * Confirms a PaymentIntent with a payment_method_id (pm_card_visa, pm_XXXX, etc.).
     *
     * @return true if status == "succeeded" or "requires_action" (3DS in test)
     */
    public boolean confirmPayment(String paymentIntentId, String paymentMethodId)
            throws IOException, InterruptedException {
        if (paymentIntentId == null || paymentIntentId.isBlank())
            throw new IllegalArgumentException("paymentIntentId is empty.");
        if (paymentMethodId == null || paymentMethodId.isBlank())
            throw new IllegalArgumentException("paymentMethodId is empty.");

        URI confirmUri = URI.create(STRIPE_BASE + "/payment_intents/" + paymentIntentId + "/confirm");
        String body = "payment_method=" + encode(paymentMethodId)
                + "&return_url=" + encode("https://example.com/return");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(confirmUri)
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = sendRequest(request);
        String json = response.body() != null ? response.body() : "";

        String status = extractJsonValue(json, "status", null);
        System.out.println("[Stripe] Payment status: " + status);
        return "succeeded".equalsIgnoreCase(status) || "requires_action".equalsIgnoreCase(status);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                String body = response.body() != null ? response.body() : "";
                throw new IOException("Stripe HTTP " + code + " : " + safeShort(body));
            }
            return response;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IOException("Stripe timeout: " + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String extractJsonValue(String json, String key, String prefixFilter) {
        if (json == null || json.isBlank()) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String v = m.group(1);
            if (prefixFilter == null || (v != null && v.startsWith(prefixFilter))) {
                return v;
            }
        }
        return null;
    }

    private static String safeShort(String s) {
        if (s == null) return "";
        String oneLine = s.replace("\n", " ").replace("\r", " ").trim();
        return oneLine.length() > 240 ? oneLine.substring(0, 240) + "..." : oneLine;
    }
}
