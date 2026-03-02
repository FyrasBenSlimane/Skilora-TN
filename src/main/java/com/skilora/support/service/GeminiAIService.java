package com.skilora.support.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.config.EnvConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'intégration avec l'API Gemini de Google.
 * Fournit des fonctionnalités d'intelligence artificielle pour le système de support :
 * - Analyse de sentiment des feedbacks
 * - Prédiction automatique de catégorie de ticket
 * - Suggestion de réponses professionnelles
 * - Correction grammaticale et orthographique
 *
 * Modèle utilisé : gemini-2.5-flash (Google Generative Language API v1beta)
 *
 * Singleton — obtain via {@link #getInstance()}.
 */
public class GeminiAIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);
    private static volatile GeminiAIService instance;

    /** API key loaded from config/application.properties */
    private final String apiKey;

    /** Model name loaded from config or default */
    private final String model;

    /** Base URL template for the Gemini generateContent endpoint */
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /** Shared HTTP client */
    private final HttpClient client = HttpClient.newHttpClient();

    // ── Singleton ──

    private GeminiAIService() {
        this.apiKey = EnvConfig.get("gemini.api.key", "PLACEHOLDER");
        this.model = EnvConfig.get("gemini.model", "gemini-2.5-flash");
        if ("PLACEHOLDER".equals(this.apiKey) || this.apiKey.isBlank()) {
            logger.warn("Gemini API key not configured — set GEMINI_API_KEY in .env");
        }
    }

    public static GeminiAIService getInstance() {
        if (instance == null) {
            synchronized (GeminiAIService.class) {
                if (instance == null) {
                    instance = new GeminiAIService();
                }
            }
        }
        return instance;
    }

    // ── Public API ──

    /**
     * Envoie un prompt à l'API Gemini et retourne la réponse textuelle.
     *
     * @param prompt Le texte à envoyer au modèle IA
     * @return La réponse textuelle du modèle, ou un message d'erreur si la requête échoue
     */
    public String askGemini(String prompt) {
        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            JSONArray parts = new JSONArray();
            parts.put(part);
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            String url = String.format(API_URL_TEMPLATE, model, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text").trim();
            } else {
                logger.error("Gemini API Error: {}", response.body());
                return "Error: " + response.statusCode();
            }
        } catch (Exception e) {
            logger.error("Gemini API call failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Analyse le sentiment d'un texte de feedback utilisateur.
     *
     * @param text Le commentaire ou feedback à analyser
     * @return Une chaîne parmi : "Happy", "Neutral" ou "Angry"
     */
    public String analyzeSentiment(String text) {
        String prompt = "Analyze the sentiment of the following feedback text. "
                + "Answer with ONLY one word: 'Happy', 'Neutral', or 'Angry'.\n\nText: " + text;
        return askGemini(prompt);
    }

    /**
     * Prédit la catégorie la plus appropriée pour un ticket de support.
     *
     * @param subject     Le sujet du ticket
     * @param description La description détaillée du problème
     * @return La catégorie prédite en majuscules : "TECHNICAL", "BILLING" ou "GENERAL"
     */
    public String predictCategory(String subject, String description) {
        String prompt = "Based on the subject and description of this support ticket, "
                + "categorize it into ONLY one of these categories: 'TECHNICAL', 'BILLING', or 'GENERAL'. "
                + "Answer with ONLY the category name.\n\nSubject: " + subject
                + "\nDescription: " + description;
        return askGemini(prompt).toUpperCase();
    }

    /**
     * Génère une suggestion de réponse professionnelle pour un agent de support.
     *
     * @param subject     Le sujet du ticket
     * @param description La description détaillée du problème
     * @return Une suggestion de réponse professionnelle (max 3 phrases)
     */
    public String suggestReply(String subject, String description) {
        String prompt = "As a professional support agent, suggest a polite and helpful reply "
                + "for this ticket. Keep it concise (max 3 sentences).\n\nSubject: " + subject
                + "\nDescription: " + description;
        return askGemini(prompt);
    }

    /**
     * Corrige automatiquement l'orthographe et la grammaire d'un texte.
     *
     * @param text Le texte brut à corriger
     * @return Le texte corrigé et professionalisé
     */
    public String correctText(String text) {
        String prompt = "Correct the spelling and grammar of the following text to make it "
                + "professional and clear. Answer with ONLY the corrected text.\n\nText: " + text;
        return askGemini(prompt);
    }

    // ── Helpers ──
}
