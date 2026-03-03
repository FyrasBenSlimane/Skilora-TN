package com.skilora.formation.service;

import com.skilora.formation.entity.Formation;
import com.skilora.support.service.GeminiAIService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI service for the formation chatbot (Gemini/OpenAI).
 * Provides intelligent replies with fallback to rule-based logic.
 */
public final class FormationAIService {

    private static volatile FormationAIService instance;

    public static FormationAIService getInstance() {
        if (instance == null) {
            synchronized (FormationAIService.class) {
                if (instance == null) instance = new FormationAIService();
            }
        }
        return instance;
    }

    private FormationAIService() {}

    /**
     * Answer a user question about formations using Gemini, falling back to simple replies on failure.
     */
    public String answer(String question, List<Formation> formations) {
        if (question == null || question.isBlank()) return "";
        int n = formations != null ? formations.size() : 0;

        if (n == 0) {
            return "Il n'y a aucune formation disponible pour le moment. Revenez plus tard.";
        }

        // Try AI response
        try {
            GeminiAIService aiService = GeminiAIService.getInstance();
            if (aiService != null && aiService.isAiAvailable()) {
                String formationsContext = formations.stream()
                        .map(f -> "- " + f.getTitle() + " (" + f.getLevel() + ") : " + (f.isFree() ? "Gratuit" : f.getCost() + " " + f.getCurrency()))
                        .collect(Collectors.joining("\n"));

                String prompt = "You are a helpful and concise assistant for Skilora Formations (in French). "
                        + "Answer the user's question based strictly on this list of available formations:\n"
                        + formationsContext + "\n\n"
                        + "Question: " + question + "\n"
                        + "Do not invent any formations. If the answer is not in the list, state it politely. Keep it brief (1-3 sentences) and in French.";

                String aiResponse = aiService.askGemini(prompt);
                if (aiResponse != null && !aiResponse.isBlank()) {
                    return aiResponse;
                }
            }
        } catch (Exception e) {
            // Log silently and fallback
            System.err.println("Gemini AI failed for formation chatbot: " + e.getMessage());
        }

        // Fallback rule-based logic
        String q = question.trim().toLowerCase();
        if (q.contains("disponible") || q.contains("quelle") || q.contains("liste") || q.contains("toutes")) {
            String list = formations.stream().limit(10).map(Formation::getTitle).collect(Collectors.joining(", "));
            if (n > 10) list += ", ...";
            return "Il y a " + n + " formation(s) disponible(s). Exemples : " + list + ".";
        }
        if (q.contains("gratuit") || q.contains("free")) {
            long free = formations.stream().filter(f -> f.isFree()).count();
            return "Il y a " + free + " formation(s) gratuite(s) sur " + n + ".";
        }
        if (q.contains("débutant") || q.contains("débutants") || q.contains("débutant")) {
            return "Parcourez la liste des formations et consultez le niveau indiqué (Débutant, Intermédiaire, Avancé) pour chaque formation.";
        }
        // Default
        return "Vous avez " + n + " formation(s) disponible(s). Posez une question sur une formation précise ou demandez la liste des formations.";
    }
}
