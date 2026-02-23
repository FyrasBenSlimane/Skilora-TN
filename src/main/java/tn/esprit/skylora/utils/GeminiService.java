package tn.esprit.skylora.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'intégration avec l'API Gemini de Google.
 * Fournit des fonctionnalités d'intelligence artificielle pour le système de
 * support :
 * - Analyse de sentiment des feedbacks
 * - Prédiction automatique de catégorie de ticket
 * - Suggestion de réponses professionnelles
 * - Correction grammaticale et orthographique
 *
 * Modèle utilisé : gemini-2.5-flash (Google Generative Language API v1beta)
 */
public class GeminiService {

    /** Clé API pour authentifier les requêtes vers Gemini */
    private static final String API_KEY = "AIzaSyCmGSjJ7uYimw8x0MhU6f_gg4W6vSbNUGU";

    /** URL de l'endpoint Gemini pour la génération de contenu */
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + API_KEY;

    /** Client HTTP partagé pour toutes les requêtes */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Envoie un prompt à l'API Gemini et retourne la réponse textuelle.
     * Cette méthode constitue le cœur du service : elle construit la requête JSON,
     * l'envoie au modèle et extrait la réponse.
     *
     * @param prompt Le texte à envoyer au modèle IA
     * @return La réponse textuelle du modèle, ou un message d'erreur si la requête
     *         échoue
     */
    public static String askGemini(String prompt) {
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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
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
                System.err.println("Gemini API Error: " + response.body());
                return "Error: " + response.statusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Analyse le sentiment d'un texte de feedback utilisateur.
     * Interroge l'IA pour déterminer si le texte exprime une émotion positive,
     * neutre ou négative.
     *
     * @param text Le commentaire ou feedback à analyser
     * @return Une chaîne parmi : "Happy", "Neutral" ou "Angry"
     */
    public static String analyzeSentiment(String text) {
        String prompt = "Analyze the sentiment of the following feedback text. Answer with ONLY one word: 'Happy', 'Neutral', or 'Angry'.\n\nText: "
                + text;
        return askGemini(prompt);
    }

    /**
     * Prédit la catégorie la plus appropriée pour un ticket de support.
     * Analyse le sujet et la description pour choisir entre les catégories
     * disponibles.
     *
     * @param subject     Le sujet du ticket
     * @param description La description détaillée du problème
     * @return La catégorie prédite en majuscules : "TECHNICAL", "BILLING" ou
     *         "GENERAL"
     */
    public static String predictCategory(String subject, String description) {
        String prompt = "Based on the subject and description of this support ticket, categorize it into ONLY one of these categories: 'TECHNICAL', 'BILLING', or 'GENERAL'. Answer with ONLY the category name.\n\nSubject: "
                + subject + "\nDescription: " + description;
        return askGemini(prompt).toUpperCase();
    }

    /**
     * Génère une suggestion de réponse professionnelle pour un agent de support.
     * Le modèle produit une réponse concise et polie basée sur le contexte du
     * ticket.
     *
     * @param subject     Le sujet du ticket
     * @param description La description détaillée du problème
     * @return Une suggestion de réponse professionnelle (max 3 phrases)
     */
    public static String suggestReply(String subject, String description) {
        String prompt = "As a professional support agent, suggest a polite and helpful reply for this ticket. Keep it concise (max 3 sentences).\n\nSubject: "
                + subject + "\nDescription: " + description;
        return askGemini(prompt);
    }

    /**
     * Corrige automatiquement l'orthographe et la grammaire d'un texte.
     * Améliore la qualité rédactionnelle tout en préservant le sens original.
     *
     * @param text Le texte brut à corriger
     * @return Le texte corrigé et professionalisé
     */
    public static String correctText(String text) {
        String prompt = "Correct the spelling and grammar of the following text to make it professional and clear. Answer with ONLY the corrected text.\n\nText: "
                + text;
        return askGemini(prompt);
    }
}
