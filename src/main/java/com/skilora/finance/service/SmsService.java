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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

/**
 * SmsService — Envoi de WhatsApp personnalisés via Twilio REST API.
 *
 * Configuration requise dans resources/config.properties :
 * twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * twilio.auth.token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * twilio.from.number=whatsapp:+1XXXXXXXXXX
 * twilio.to.number=whatsapp:+216XXXXXXXX
 */
public class SmsService {

    private static SmsService instance;

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

        Properties props = loadConfig();
        this.accountSid = props.getProperty("twilio.account.sid", "").trim();
        this.authToken = props.getProperty("twilio.auth.token", "").trim();
        this.fromNumber = props.getProperty("twilio.from.number", "").trim();
        this.toNumber = props.getProperty("twilio.to.number", "").trim();

        this.configured = !accountSid.isEmpty() && !authToken.isEmpty()
                && !fromNumber.isEmpty() && !toNumber.isEmpty()
                && accountSid.startsWith("AC");

        if (!configured) {
            System.out.println("[WhatsApp] Twilio non configuré — Messages désactivés. " +
                    "Ajoutez twilio.* dans config.properties.");
        } else {
            System.out.println("[WhatsApp] Twilio configuré ✅ → Envois vers " + maskNumber(toNumber));
        }
    }

    public static synchronized SmsService getInstance() {
        if (instance == null)
            instance = new SmsService();
        return instance;
    }

    /**
     * Envoie un WA de confirmation de paiement personnalisé.
     * ATTENTION : WhatsApp n'autorise les messages libres (sans template) que
     * dans une fenêtre de 24h après le dernier message de l'utilisateur vers votre
     * sandbox Twilio.
     *
     * @param montant         montant payé (ex: 1000.00)
     * @param beneficiaire    nom du bénéficiaire
     * @param referenceProjet référence du projet
     * @param transactionId   ID Stripe (pi_XXXX)
     */
    public void sendPaymentSuccess(double montant, String beneficiaire,
            String referenceProjet, String transactionId) {
        if (!configured) {
            System.out.println("[WhatsApp] Twilio non configuré — Message ignoré.");
            return;
        }

        String dateStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));

        // Voici le message 100% personnalisé
        String customMessage = String.format(
                "✅ *PAIEMENT ATTRIBUÉ AVEC SUCCÈS*\n\n" +
                        "👤 Bénéficiaire : %s\n" +
                        "💰 Montant : %.2f USD\n" +
                        "📄 Projet : %s\n" +
                        "📅 Date : %s\n" +
                        "🆔 Réf. TX : %s\n\n" +
                        "— *Support Skilora Finance*",
                beneficiaire, montant, referenceProjet, dateStr,
                (transactionId != null ? transactionId : "N/A"));

        try {
            sendWhatsAppMessage(toNumber, customMessage);
            System.out.println("[WhatsApp] Message personnalisé envoyé avec succès → " + maskNumber(toNumber));
        } catch (Exception e) {
            System.err.println("[WhatsApp] Erreur envoi: " + e.getMessage());
            throw new RuntimeException("Erreur WhatsApp: " + e.getMessage(), e);
        }
    }

    /**
     * Envoie un message texte libre WhatsApp via l'API Twilio.
     */
    private void sendWhatsAppMessage(String to, String bodyMessage)
            throws IOException, InterruptedException {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        // Pour un message libre, on utilise simplement paramètre Body
        String requestBody = "To=" + encode(to)
                + "&From=" + encode(fromNumber)
                + "&Body=" + encode(bodyMessage);

        // Twilio utilise Basic Auth : AccountSid:AuthToken en Base64
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

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = SmsService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null)
                props.load(in);
        } catch (IOException e) {
            System.err.println("[WhatsApp] Erreur lecture config.properties: " + e.getMessage());
        }
        return props;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String maskNumber(String number) {
        if (number == null || number.length() < 4)
            return "****";
        return number.substring(0, number.length() - 4) + "****";
    }

    private static String safeShort(String s) {
        if (s == null)
            return "";
        String oneLine = s.replace("\n", " ").replace("\r", " ").trim();
        return oneLine.length() > 200 ? oneLine.substring(0, 200) + "..." : oneLine;
    }
}
