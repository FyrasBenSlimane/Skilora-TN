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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Service d'intégration avec l'API Gemini de Google.
 * Fournit des fonctionnalités d'intelligence artificielle pour le système de support :
 * - Analyse de sentiment des feedbacks
 * - Prédiction automatique de catégorie de ticket
 * - Suggestion de réponses professionnelles
 * - Correction grammaticale et orthographique
 *
 * When Gemini key is not configured, automatically falls back to OpenAI.
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

    // ── OpenAI Fallback ──
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4.1-mini";
    private final String openaiKey;
    private final boolean useOpenAI;

    /** Shared HTTP client */
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * System prompt for the Help Center (Centre d'aide) Assistant.
     * Restricts the AI to only answer questions about the Skilora Talent application.
     */
    private static final String HELP_CENTER_SYSTEM_PROMPT =
            "You are the official in-app assistant for Skilora Talent, a professional platform for "
            + "recruitment, training (formations), community, finance, and support. Your role is strictly limited to helping users "
            + "with questions and issues related to this application only.\n\n"
            + "You MAY answer questions about: how to use the app (Tableau de bord, Trouver une mission, Mes propositions, "
            + "Communauté, Formations, Finances, Support), creating or managing support tickets, account and profile, "
            + "job applications and offers, trainings and certificates, community and messaging, payments and contracts, "
            + "and any feature or content within Skilora Talent.\n\n"
            + "You must NOT answer: general knowledge, other products or websites, coding or technical topics unrelated to the app, "
            + "personal advice, or any subject outside Skilora Talent. If the user asks something off-topic, respond politely "
            + "in one short sentence that you can only help with questions about the Skilora Talent application, and suggest "
            + "they create a support ticket (Nouveau Ticket) for other requests or ask something about the app. "
            + "Keep responses concise (2–4 sentences). Reply in the same language as the user (French, English, or Arabic).";

    // ── Singleton ──

    private GeminiAIService() {
        this.apiKey = EnvConfig.get("gemini.api.key", "PLACEHOLDER");
        this.model = EnvConfig.get("gemini.model", "gemini-2.5-flash");

        boolean geminiConfigured = !"PLACEHOLDER".equals(this.apiKey) && !this.apiKey.isBlank();

        // Check OpenAI fallback
        String oaiKey = EnvConfig.get("openai.api.key", "");
        boolean oaiConfigured = oaiKey != null && !oaiKey.isBlank() && oaiKey.startsWith("sk-");

        if (geminiConfigured) {
            this.useOpenAI = false;
            this.openaiKey = null;
            logger.info("GeminiAIService: using Gemini API (model={})", model);
        } else if (oaiConfigured) {
            this.useOpenAI = true;
            this.openaiKey = oaiKey;
            logger.info("GeminiAIService: Gemini not configured, using OpenAI fallback (model={})", OPENAI_MODEL);
        } else {
            this.useOpenAI = false;
            this.openaiKey = null;
            logger.warn("GeminiAIService: No AI key configured — set GEMINI_API_KEY or OPENAI_API_KEY in .env");
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
     * Envoie un prompt à l'API AI (Gemini ou OpenAI fallback) et retourne la réponse textuelle.
     *
     * @param prompt Le texte à envoyer au modèle IA
     * @return La réponse textuelle du modèle, ou un message d'erreur si la requête échoue
     */
    /**
     * Returns true if an AI backend (Gemini or OpenAI) is configured and can be used for the Help Center chat.
     */
    public boolean isAiAvailable() {
        if (useOpenAI) {
            return openaiKey != null && !openaiKey.isBlank();
        }
        return apiKey != null && !apiKey.isBlank() && !"PLACEHOLDER".equals(apiKey);
    }

    /**
     * Generates a reply for the Help Center (Centre d'aide) Assistant tab.
     * Uses a strict system prompt so the AI only answers questions about the Skilora Talent application;
     * off-topic questions are politely declined.
     *
     * @param userMessage         The latest user message
     * @param conversationHistory Optional list of recent messages (e.g. "User: ..." or "AI: ..."); can be null or empty
     * @return The AI reply text, or null if AI is not configured or the request fails
     */
    public String replyHelpCenter(String userMessage, List<String> conversationHistory) {
        if (!isAiAvailable() || userMessage == null || userMessage.isBlank()) {
            return null;
        }
        if (useOpenAI) {
            return callOpenAIHelpCenter(userMessage, conversationHistory);
        }
        return callGeminiHelpCenter(userMessage, conversationHistory);
    }

    /** Call Gemini with system instruction and optional conversation context. */
    private String callGeminiHelpCenter(String userMessage, List<String> conversationHistory) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("systemInstruction", new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", HELP_CENTER_SYSTEM_PROMPT))));

            StringBuilder userContent = new StringBuilder();
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int start = Math.max(0, conversationHistory.size() - 10);
                for (int i = start; i < conversationHistory.size(); i++) {
                    userContent.append(conversationHistory.get(i)).append("\n");
                }
            }
            userContent.append("User: ").append(userMessage.trim());

            JSONArray contents = new JSONArray();
            contents.put(new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", userContent.toString()))));
            requestBody.put("contents", contents);

            requestBody.put("generationConfig", new JSONObject()
                    .put("maxOutputTokens", 512)
                    .put("temperature", 0.4));

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
                logger.warn("Gemini Help Center API error: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Gemini Help Center call failed", e);
            return null;
        }
    }

    /** Call OpenAI with system prompt for Help Center (app-only). */
    private String callOpenAIHelpCenter(String userMessage, List<String> conversationHistory) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", OPENAI_MODEL);
            body.put("max_tokens", 512);
            body.put("temperature", 0.4);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", HELP_CENTER_SYSTEM_PROMPT));
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int start = Math.max(0, conversationHistory.size() - 10);
                for (int i = start; i < conversationHistory.size(); i++) {
                    String line = conversationHistory.get(i);
                    if (line.startsWith("User:")) {
                        messages.put(new JSONObject().put("role", "user").put("content", line.substring(5).trim()));
                    } else if (line.startsWith("AI:") || line.startsWith("Assistant:")) {
                        messages.put(new JSONObject().put("role", "assistant").put("content", line.replaceFirst("^(AI|Assistant):", "").trim()));
                    }
                }
            }
            messages.put(new JSONObject().put("role", "user").put("content", userMessage));
            body.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + openaiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();
            } else {
                logger.warn("OpenAI Help Center API error: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.warn("OpenAI Help Center call failed", e);
            return null;
        }
    }

    public String askGemini(String prompt) {
        if (useOpenAI) {
            return callOpenAI(prompt);
        }
        return callGemini(prompt);
    }

    /** Call the Gemini API directly. */
    private String callGemini(String prompt) {
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

    /** Call OpenAI chat completions as a fallback for Gemini. */
    private String callOpenAI(String prompt) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", OPENAI_MODEL);
            body.put("max_tokens", 500);
            body.put("temperature", 0.4);

            JSONArray messages = new JSONArray();
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful AI assistant for a support system. Be concise and professional.");
            messages.put(systemMsg);
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);
            body.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + openaiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();
            } else {
                logger.error("OpenAI fallback API Error {}: {}", response.statusCode(), response.body());
                return "Error: " + response.statusCode();
            }
        } catch (Exception e) {
            logger.error("OpenAI fallback call failed", e);
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
        return suggestReplyWithContext(subject, description, null, null);
    }

    /**
     * Suggests a professional support reply using full ticket and conversation context.
     * Uses subject, description, category, and the conversation thread so the AI can
     * acknowledge the latest user message and previous replies.
     *
     * @param subject       Ticket subject
     * @param description   Ticket description
     * @param category      Optional category (e.g. ACCOUNT, TECHNICAL, BILLING)
     * @param conversation  Optional thread: one line per message, e.g. "User (Name): message" or "Admin: message"
     * @return Suggested reply text, or error message if the request fails
     */
    public String suggestReplyWithContext(String subject, String description, String category, String conversation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional support agent. Suggest a single, polite and helpful reply for this ticket. ");
        prompt.append("Keep it concise (2–4 sentences). ");
        prompt.append("Acknowledge the user's concern and, if relevant, mention next steps or that you are looking into it. ");
        prompt.append("Do not invent information (e.g. do not say documents are approved unless you know they are). ");
        prompt.append("Reply in the same language as the last user message if possible, otherwise use a neutral professional tone.\n\n");
        prompt.append("Ticket subject: ").append(subject != null ? subject : "(none)").append("\n");
        prompt.append("Ticket description: ").append(description != null ? description : "(none)").append("\n");
        if (category != null && !category.isBlank()) {
            prompt.append("Category: ").append(category).append("\n");
        }
        if (conversation != null && !conversation.isBlank()) {
            prompt.append("\nConversation so far:\n").append(conversation).append("\n");
            prompt.append("Suggest a reply from the support agent to the user based on the conversation above.\n");
        }
        return askGemini(prompt.toString());
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
