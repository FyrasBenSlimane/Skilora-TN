package com.skilora.community.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AISummaryService — Service d'intégration de l'API Google Gemini pour résumer les discussions.
 *
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║  API UTILISÉE : Google Gemini (generativelanguage.googleapis.com)║
 * ║  Modèle       : gemini-2.0-flash (gratuit)                     ║
 * ║  Format        : JSON (REST)                                    ║
 * ║  Limite        : 15 requêtes/min (tier gratuit)                 ║
 * ╠═══════════════════════════════════════════════════════════════════╣
 * ║  STRATÉGIE :                                                     ║
 * ║  1. Appel Gemini API avec retry (3 tentatives, backoff)         ║
 * ║  2. Si API indisponible → résumé local algorithmique            ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Pattern : Singleton thread-safe (cohérent avec TranslationService, etc.)
 */
@SuppressWarnings("unused")
public class AISummaryService {

    private static final Logger logger = LoggerFactory.getLogger(AISummaryService.class);

    // ── Clé API Google Gemini (chargée depuis .env ou application.properties) ──
    private static final String GEMINI_API_KEY = com.skilora.config.EnvConfig.get("gemini.api.key", "");

    // ── URL de l'API Gemini ──
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    // ── OpenAI Fallback ──
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4.1-mini";
    private static final String OPENAI_API_KEY = com.skilora.config.EnvConfig.get("openai.api.key", "");

    // ── Retry config ──
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000; // 2 seconds

    // ── Singleton ──
    private static volatile AISummaryService instance;

    private AISummaryService() {}

    public static AISummaryService getInstance() {
        if (instance == null) {
            synchronized (AISummaryService.class) {
                if (instance == null) {
                    instance = new AISummaryService();
                }
            }
        }
        return instance;
    }

    /**
     * Résume une liste de messages de discussion.
     * Tente l'API Gemini avec retry, puis bascule sur un résumé local si échec.
     *
     * @param messages Liste de chaînes au format "Nom: message"
     * @return Le résumé généré
     */
    public String summarize(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "Aucun message à résumer.";
        }

        // 1. Tenter l'API Gemini avec retry (if key is configured)
        if (GEMINI_API_KEY != null && !GEMINI_API_KEY.isBlank() && !"PLACEHOLDER".equals(GEMINI_API_KEY)) {
            String aiResult = callGeminiWithRetry(messages);
            if (aiResult != null) {
                return aiResult;
            }
        }

        // 2. Fallback : OpenAI (if key is configured)
        if (OPENAI_API_KEY != null && !OPENAI_API_KEY.isBlank() && OPENAI_API_KEY.startsWith("sk-")) {
            String openaiResult = callOpenAIFallback(messages);
            if (openaiResult != null) {
                return openaiResult;
            }
        }

        // 3. Fallback : résumé local algorithmique
        logger.info("All AI APIs unavailable, using local summary fallback");
        return generateLocalSummary(messages);
    }

    /**
     * Appelle l'API Gemini avec retry et backoff exponentiel.
     * @return Le résumé ou null si toutes les tentatives échouent
     */
    private String callGeminiWithRetry(List<String> messages) {
        StringBuilder conversationText = new StringBuilder();
        for (String msg : messages) {
            conversationText.append(msg).append("\n");
        }

        String prompt = "Tu es un assistant intelligent. Résume la conversation suivante de manière concise et structurée en français. "
                + "Identifie les points clés, les décisions prises et les sujets abordés. "
                + "Format le résumé avec des puces (•) pour chaque point important.\n\n"
                + "=== CONVERSATION ===\n"
                + conversationText
                + "\n=== FIN DE LA CONVERSATION ===\n\n"
                + "Résumé :";

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        String body = requestBody.toString();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Gemini API attempt {}/{}", attempt, MAX_RETRIES);

                URL url = new URL(GEMINI_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                logger.info("Gemini API response code: {}", responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (!candidates.isEmpty()) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentResp = firstCandidate.getJSONObject("content");
                        JSONArray partsResp = contentResp.getJSONArray("parts");
                        if (!partsResp.isEmpty()) {
                            String summary = partsResp.getJSONObject(0).getString("text");
                            logger.info("Summary generated successfully ({} chars)", summary.length());
                            return summary.trim();
                        }
                    }
                    return null; // Empty response, fall to local

                } else if (responseCode == 429) {
                    // Rate limited — wait and retry
                    long waitMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    logger.warn("Rate limited (429), waiting {}ms before retry...", waitMs);
                    Thread.sleep(waitMs);
                    // Continue to next attempt

                } else {
                    // Other error — read and log, don't retry
                    StringBuilder errorResponse = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorResponse.append(line);
                        }
                    }
                    logger.error("Gemini API error {}: {}", responseCode, errorResponse);
                    return null; // Fall to local summary
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.error("Error calling Gemini API (attempt {})", attempt, e);
                if (attempt == MAX_RETRIES) return null;
            }
        }
        return null; // All retries exhausted
    }

    /**
     * Call OpenAI chat completions as a fallback when Gemini is unavailable.
     */
    private String callOpenAIFallback(List<String> messages) {
        try {
            StringBuilder conversationText = new StringBuilder();
            for (String msg : messages) {
                conversationText.append(msg).append("\n");
            }

            String prompt = "Tu es un assistant intelligent. Résume la conversation suivante de manière concise et structurée en français. "
                    + "Identifie les points clés, les décisions prises et les sujets abordés. "
                    + "Format le résumé avec des puces (•) pour chaque point important.\n\n"
                    + "=== CONVERSATION ===\n"
                    + conversationText
                    + "\n=== FIN DE LA CONVERSATION ===\n\n"
                    + "Résumé :";

            JSONObject body = new JSONObject();
            body.put("model", OPENAI_MODEL);
            body.put("max_tokens", 800);
            body.put("temperature", 0.4);

            JSONArray msgArray = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            msgArray.put(userMsg);
            body.put("messages", msgArray);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String summary = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();
                logger.info("OpenAI summary generated ({} chars)", summary.length());
                return summary;
            } else {
                logger.error("OpenAI fallback error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("OpenAI fallback call failed", e);
        }
        return null;
    }

    /**
     * Génère un résumé local algorithmique (sans API).
     * Analyse les messages pour extraire : participants, nombre de messages,
     * sujets principaux, et les messages les plus longs (souvent les plus importants).
     */
    private String generateLocalSummary(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Résumé automatique (mode local)\n\n");

        // Participants
        Map<String, Integer> participantCount = new LinkedHashMap<>();
        List<String> textMessages = new ArrayList<>();

        for (String msg : messages) {
            int colonIdx = msg.indexOf(": ");
            if (colonIdx > 0) {
                String sender = msg.substring(0, colonIdx).trim();
                String text = msg.substring(colonIdx + 2).trim();
                participantCount.merge(sender, 1, Integer::sum);
                if (!text.startsWith("[") && text.length() > 2) {
                    textMessages.add(text);
                }
            }
        }

        // Stats
        sb.append("• Messages : ").append(messages.size()).append(" au total\n");
        sb.append("• Participants : ").append(participantCount.size()).append("\n");
        for (Map.Entry<String, Integer> entry : participantCount.entrySet()) {
            sb.append("   - ").append(entry.getKey()).append(" (").append(entry.getValue()).append(" messages)\n");
        }

        // Messages les plus longs = souvent les plus importants
        if (!textMessages.isEmpty()) {
            sb.append("\n• Points clés (messages les plus détaillés) :\n");
            textMessages.stream()
                    .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                    .limit(5)
                    .forEach(t -> {
                        String excerpt = t.length() > 120 ? t.substring(0, 120) + "..." : t;
                        sb.append("   - \"").append(excerpt).append("\"\n");
                    });
        }

        // Mots fréquents (sujets)
        if (!textMessages.isEmpty()) {
            Map<String, Integer> wordFreq = new HashMap<>();
            Set<String> stopWords = Set.of("le", "la", "les", "de", "du", "des", "un", "une", "et", "en",
                    "est", "que", "qui", "dans", "pour", "pas", "sur", "ce", "il", "je", "tu", "nous",
                    "vous", "se", "ne", "on", "au", "avec", "son", "sa", "ses", "the", "is", "are",
                    "and", "to", "of", "in", "it", "a", "i", "you", "we", "this", "that", "was", "be",
                    "have", "has", "had", "but", "or", "my", "me", "he", "she", "do", "can", "will",
                    "just", "so", "no", "not", "more", "very", "also", "than", "too", "all", "been");
            for (String text : textMessages) {
                String[] words = text.toLowerCase().replaceAll("[^a-zàâäéèêëïîôùûüç\\s]", "").split("\\s+");
                for (String w : words) {
                    if (w.length() > 3 && !stopWords.contains(w)) {
                        wordFreq.merge(w, 1, Integer::sum);
                    }
                }
            }
            List<Map.Entry<String, Integer>> topWords = wordFreq.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(8)
                    .toList();
            if (!topWords.isEmpty()) {
                sb.append("\n• Sujets fréquents : ");
                sb.append(topWords.stream()
                        .map(e -> e.getKey() + " (" + e.getValue() + ")")
                        .collect(Collectors.joining(", ")));
                sb.append("\n");
            }
        }

        sb.append("\n⚠️ Résumé généré localement (quota API IA épuisé). Réessayez plus tard pour un résumé IA.");
        return sb.toString();
    }
}
