package com.skilora.support.service;

import com.skilora.config.EnvConfig;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'intégration avec l'API Deepgram pour la transcription vocale (Speech-to-Text).
 * Utilise le modèle nova-2 pour convertir des données audio (WAV) en texte.
 *
 * Singleton — obtain via {@link #getInstance()}.
 */
public class DeepgramSTTService {

    private static final Logger logger = LoggerFactory.getLogger(DeepgramSTTService.class);
    private static volatile DeepgramSTTService instance;

    /** API key loaded from config/application.properties */
    private final String apiKey;

    /** Model name loaded from config or default */
    private final String model;

    /** Base URL template for the Deepgram listen endpoint */
    private static final String API_URL_TEMPLATE =
            "https://api.deepgram.com/v1/listen?model=%s&smart_format=true";

    // ── Singleton ──

    private DeepgramSTTService() {
        this.apiKey = EnvConfig.get("deepgram.api.key", "PLACEHOLDER");
        this.model = EnvConfig.get("deepgram.model", "nova-2");
        if ("PLACEHOLDER".equals(this.apiKey) || this.apiKey.isBlank()) {
            logger.warn("Deepgram API key not configured — set DEEPGRAM_API_KEY in .env");
        }
    }

    public static DeepgramSTTService getInstance() {
        if (instance == null) {
            synchronized (DeepgramSTTService.class) {
                if (instance == null) {
                    instance = new DeepgramSTTService();
                }
            }
        }
        return instance;
    }

    // ── Public API ──

    /**
     * Sends an audio byte array to Deepgram for transcription.
     *
     * @param audioData Byte array of the audio (WAV format expected)
     * @return The transcribed text, or an empty string if transcription fails
     */
    public String transcribe(byte[] audioData) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            String url = String.format(API_URL_TEMPLATE, model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Token " + apiKey)
                    .header("Content-Type", "audio/wav")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String transcript = json.getJSONObject("results")
                        .getJSONArray("channels")
                        .getJSONObject(0)
                        .getJSONArray("alternatives")
                        .getJSONObject(0)
                        .getString("transcript");

                // Remove periods that appear after each word
                return transcript.replaceAll("\\.\\s*", " ").trim();
            } else {
                logger.error("Deepgram Error: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Deepgram transcription failed", e);
        }
        return "";
    }

    // ── Helpers ──
}
