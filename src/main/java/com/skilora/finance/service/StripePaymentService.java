package com.skilora.finance.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service Stripe (MODE TEST) — sans SDK externe.
 *
 * Stratégie pour mode TEST avec Radar for Fraud Teams activé :
 * Stripe interdit d'envoyer des numéros de carte bruts depuis le serveur.
 * Solution : mapper les numéros test connus vers les PM IDs prédéfinis Stripe
 * (pm_card_visa, pm_card_mastercard, etc.) et les utiliser directement.
 *
 * Clés lues depuis resources/config.properties
 */
public class StripePaymentService {

    private static final String STRIPE_BASE = "https://api.stripe.com/v1";

    // Mapping numéros test Stripe → Payment Method IDs prédéfinis (mode TEST
    // uniquement)
    // Source : https://stripe.com/docs/testing#cards
    private static final java.util.Map<String, String> TEST_CARD_PM_MAP;
    static {
        TEST_CARD_PM_MAP = new java.util.HashMap<>();
        TEST_CARD_PM_MAP.put("4242424242424242", "pm_card_visa"); // Visa
        TEST_CARD_PM_MAP.put("4000056655665556", "pm_card_visa_debit"); // Visa Debit
        TEST_CARD_PM_MAP.put("5555555555554444", "pm_card_mastercard"); // Mastercard
        TEST_CARD_PM_MAP.put("5200828282828210", "pm_card_mastercard_debit"); // MC Debit
        TEST_CARD_PM_MAP.put("378282246310005", "pm_card_amex"); // Amex
        TEST_CARD_PM_MAP.put("6011111111111117", "pm_card_discover"); // Discover
        TEST_CARD_PM_MAP.put("3056930009020004", "pm_card_diners"); // Diners
        TEST_CARD_PM_MAP.put("4000002500003155", "pm_card_threeDSecure2Required"); // 3DS
    }

    private final HttpClient httpClient;
    private final String secretKey;

    public StripePaymentService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.secretKey = loadSecretKeyFromConfig();
    }

    /**
     * Résout le Payment Method ID à utiliser.
     * Si le numéro saisi correspond à une carte test connue → retourne le
     * pm_card_xxx prédéfini.
     * Sinon → tente de créer un PM via l'API (mode live ou cartes test non
     * listées).
     */
    public String resolvePaymentMethodId(String cardNumber, String expMonth, String expYear, String cvc)
            throws IOException, InterruptedException {

        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");

        // Vérifier si c'est une carte test connue
        String predefinedPm = TEST_CARD_PM_MAP.get(digits);
        if (predefinedPm != null) {
            System.out.println("[Stripe] Carte test détectée → utilisation PM prédéfini: " + predefinedPm);
            return predefinedPm;
        }

        // Sinon, tentative via API (peut échouer avec Radar activé)
        System.out.println("[Stripe] Tentative création PaymentMethod via API...");
        return createPaymentMethodViaApi(digits,
                expMonth.replaceAll("[^0-9]", ""),
                expYear.replaceAll("[^0-9]", ""),
                cvc.replaceAll("[^0-9]", ""));
    }

    /**
     * Crée un PaymentMethod via l'API Stripe /v1/payment_methods.
     * Peut être refusé si Radar for Fraud Teams est actif.
     */
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
            throw new IOException("Stripe: impossible de créer le PaymentMethod. Réponse: " + safeShort(json));
        }
        return pmId;
    }

    /**
     * Crée un PaymentIntent Stripe.
     *
     * @param montantCentimes montant en centimes (ex: 10000 = 100.00 USD)
     * @return l'id du payment_intent (ex: pi_XXXX)
     */
    public String createPaymentIntent(long montantCentimes) throws IOException, InterruptedException {
        if (montantCentimes <= 0) {
            throw new IllegalArgumentException("Le montant doit être > 0.");
        }

        String body = "amount=" + encode(String.valueOf(montantCentimes))
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
            throw new IOException("Stripe: impossible d'extraire l'id du PaymentIntent. Réponse: " + safeShort(json));
        }
        return intentId;
    }

    /**
     * Confirme un PaymentIntent avec un payment_method_id (pm_card_visa, pm_XXXX,
     * etc.).
     * Aucun numéro de carte brut → aucune erreur 402.
     *
     * @return true si status == "succeeded" ou "requires_action" (3DS en test)
     */
    public boolean confirmPayment(String paymentIntentId, String paymentMethodId)
            throws IOException, InterruptedException {

        if (paymentIntentId == null || paymentIntentId.isBlank())
            throw new IllegalArgumentException("paymentIntentId vide.");
        if (paymentMethodId == null || paymentMethodId.isBlank())
            throw new IllegalArgumentException("paymentMethodId vide.");

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
        System.out.println("[Stripe] Statut paiement: " + status);
        // "succeeded" = immédiat OK
        // "requires_action" = 3DS (OK en mode test)
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
            throw new IOException("Timeout Stripe: " + e.getMessage(), e);
        }
    }

    private String loadSecretKeyFromConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null)
                throw new IllegalStateException("config.properties introuvable dans resources/");
            props.load(in);
            String key = props.getProperty("stripe.secret.key");
            if (key == null || key.isBlank())
                throw new IllegalStateException("stripe.secret.key manquante dans config.properties");
            return key.trim();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lecture config.properties: " + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Extrait une valeur simple d'un JSON via regex.
     * Si prefixFilter est non-null, la valeur doit commencer par ce préfixe.
     */
    private static String extractJsonValue(String json, String key, String prefixFilter) {
        if (json == null || json.isBlank())
            return null;
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
        if (s == null)
            return "";
        String oneLine = s.replace("\n", " ").replace("\r", " ").trim();
        return oneLine.length() > 240 ? oneLine.substring(0, 240) + "..." : oneLine;
    }
}
