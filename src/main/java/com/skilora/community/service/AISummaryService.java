package com.skilora.community.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class AISummaryService {

    private static final Logger logger = LoggerFactory.getLogger(AISummaryService.class);

    // ── Clé API Google Gemini (chargée depuis .env) ──
    private static final String GEMINI_API_KEY = loadApiKey();

    // ── URL de l'API Gemini ──
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    /**
     * Charge la clé API depuis le fichier .env à la racine du projet.
     */
    private static String loadApiKey() {
        try {
            // Chercher .env dans le répertoire courant (racine du projet)
            Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                // Chercher aussi relativement au classpath
                envPath = Paths.get(System.getProperty("user.dir"), ".env");
            }
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=")) {
                        String key = line.substring("GEMINI_API_KEY=".length()).trim();
                        LoggerFactory.getLogger(AISummaryService.class).info("Gemini API key loaded from .env");
                        return key;
                    }
                }
            }
            LoggerFactory.getLogger(AISummaryService.class).warn(".env file not found or GEMINI_API_KEY missing");
        } catch (Exception e) {
            LoggerFactory.getLogger(AISummaryService.class).error("Error loading .env file", e);
        }
        return "";
    }

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

        // 1. Tenter l'API Gemini avec retry
        String aiResult = callGeminiWithRetry(messages);
        if (aiResult != null) {
            return aiResult;
        }

        // 2. Fallback : résumé local algorithmique
        logger.info("Gemini API unavailable, using local summary fallback");
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
                + conversationText.toString()
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
                    if (candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject contentResp = firstCandidate.getJSONObject("content");
                        JSONArray partsResp = contentResp.getJSONArray("parts");
                        if (partsResp.length() > 0) {
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
                    logger.error("Gemini API error {}: {}", responseCode, errorResponse.toString());
                    return null; // Fall to local summary
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.error("Error calling Gemini API (attempt " + attempt + ")", e);
                if (attempt == MAX_RETRIES) return null;
            }
        }
        return null; // All retries exhausted
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
                    .collect(Collectors.toList());
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
