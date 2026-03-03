package com.skilora.finance.service;

import com.skilora.config.EnvConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * FinanceChatbotAIService — OpenAI-powered AI service for the Finance chatbot
 * and PDF report summary generation.
 *
 * API key loaded from config/application.properties: openai.api.key
 * Singleton with getInstance().
 */
public class FinanceChatbotAIService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4.1-mini";
    private static final int MAX_TOKENS = 600;
    private static final int MAX_TOKENS_SUMMARY = 400;
    private static final int TIMEOUT_SECONDS = 30;

    private static volatile FinanceChatbotAIService instance;

    private final HttpClient httpClient;
    private final String apiKey;
    private final boolean configured;

    private FinanceChatbotAIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        String key = EnvConfig.get("openai.api.key", "");
        this.apiKey = (key != null) ? key.trim() : "";
        this.configured = !apiKey.isBlank() && apiKey.startsWith("sk-");
        if (!configured) {
            System.out.println("[Chatbot AI] OpenAI not configured \u2014 add OPENAI_API_KEY in .env to enable AI.");
        }
    }

    public static FinanceChatbotAIService getInstance() {
        if (instance == null) {
            synchronized (FinanceChatbotAIService.class) {
                if (instance == null) {
                    instance = new FinanceChatbotAIService();
                }
            }
        }
        return instance;
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * Sends user question to AI with employee context. Returns null on error or if not configured.
     */
    public String askAI(String userQuestion, FinanceChatbotService.FinanceChatContext context) {
        if (!configured)
            return null;
        String systemPrompt = buildSystemPrompt(context);
        return callOpenAI(systemPrompt, userQuestion != null ? userQuestion.trim() : "", MAX_TOKENS, 0.5);
    }

    /**
     * Generates a professional PDF report summary via AI from raw employee data.
     *
     * @param rawDataFacts text describing the data (contract, payslips, bonuses, accounts)
     * @return a professional paragraph in French, or null if not configured/error
     */
    public String generateReportSummary(String rawDataFacts) {
        if (!configured || rawDataFacts == null || rawDataFacts.isBlank())
            return null;
        String systemPrompt = "Tu es un assistant RH/Finance pour Skilora. À partir des données fournies ci-dessous, "
                + "rédige UN SEUL paragraphe professionnel en français pour le résumé d'un rapport financier employé. "
                + "Sois concis, formel, sans liste à puces. Utilise uniquement les informations fournies. "
                + "Ne dis pas « Voici le résumé » ou « Résumé : », écris directement le paragraphe.";
        String out = callOpenAI(systemPrompt, rawDataFacts.trim(), MAX_TOKENS_SUMMARY, 0.3);
        return (out != null && !out.isBlank()) ? out.trim() : null;
    }

    private String callOpenAI(String systemContent, String userContent, int maxTokens, double temperature) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("max_tokens", maxTokens);
            body.addProperty("temperature", temperature);

            JsonArray messages = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemContent);
            messages.add(systemMsg);
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userContent);
            messages.add(userMsg);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            String responseBody = response.body() != null ? response.body() : "";
            if (code < 200 || code >= 300) {
                System.err.println("[Chatbot AI] OpenAI HTTP " + code + ": " + safeShort(responseBody));
                return null;
            }
            return extractContentFromResponse(responseBody);
        } catch (IOException | InterruptedException e) {
            System.err.println("[Chatbot AI] API call error: " + e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt(FinanceChatbotService.FinanceChatContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es l'assistant Ma Paie de Skilora (gestion finance). ");
        sb.append("Tu réponds UNIQUEMENT aux questions sur la paie, les bulletins de paie, le salaire (net/brut), ");
        sb.append("la CNSS, l'IRPP, les contrats, les primes, les comptes bancaires et les virements. ");
        sb.append("Si la question n'est pas liée à la finance ou la paie, refuse poliment en indiquant que tu ne traites que les sujets paie/finance. ");
        sb.append("Réponds en français, de façon claire et professionnelle. Tu peux utiliser du gras avec ** pour les montants ou termes importants.\n\n");

        if (context != null) {
            sb.append("Contexte à utiliser pour personnaliser tes réponses (données réelles de l'application) :\n");
            sb.append("- Employé connecté : ").append(context.getEmployeeName() != null ? context.getEmployeeName() : "—").append("\n");
            if (context.getLastPayslipPeriod() != null || context.getLastPayslipNet() != null) {
                sb.append("- Dernier bulletin : période ").append(context.getLastPayslipPeriod() != null ? context.getLastPayslipPeriod() : "—");
                sb.append(", net à payer ").append(context.getLastPayslipNet() != null ? context.getLastPayslipNet() : "—").append(" TND.\n");
            }
            if (context.getEmployeesByNormalizedName() != null && !context.getEmployeesByNormalizedName().isEmpty()) {
                sb.append("- Données d'autres employés (pour répondre aux questions du type « salaire de X ») :\n");
                for (Map.Entry<String, FinanceChatbotService.EmployeeSnapshot> e : context.getEmployeesByNormalizedName().entrySet()) {
                    FinanceChatbotService.EmployeeSnapshot snap = e.getValue();
                    sb.append("  * ").append(snap.getFullName());
                    if (snap.getLastPayslipNet() != null && !snap.getLastPayslipNet().isEmpty())
                        sb.append(" — dernier net ").append(snap.getLastPayslipNet()).append(" TND");
                    if (snap.getLastPayslipPeriod() != null && !snap.getLastPayslipPeriod().isEmpty())
                        sb.append(" (").append(snap.getLastPayslipPeriod()).append(")");
                    if (snap.getCurrentSalary() != null && !snap.getCurrentSalary().isEmpty())
                        sb.append(" — salaire de base ").append(snap.getCurrentSalary()).append(" TND");
                    sb.append("\n");
                }
            }
            sb.append("\nUtilise ces données pour donner des réponses précises quand on te demande « mon salaire », « mon net » ou « salaire de [nom] ».");
        }
        return sb.toString();
    }

    private String extractContentFromResponse(String json) {
        try {
            JsonObject root = new Gson().fromJson(json, JsonObject.class);
            if (root == null) return null;
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            JsonObject first = choices.get(0).getAsJsonObject();
            if (first == null) return null;
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) return null;
            var content = message.get("content");
            return content != null && !content.isJsonNull() ? content.getAsString().trim() : null;
        } catch (Exception e) {
            System.err.println("[Chatbot AI] JSON parse error: " + e.getMessage());
            return null;
        }
    }

    private static String safeShort(String s) {
        if (s == null) return "";
        String one = s.replace("\n", " ").trim();
        return one.length() > 300 ? one.substring(0, 300) + "..." : one;
    }
}
