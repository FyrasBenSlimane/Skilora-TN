package com.skilora.support.service;

import com.skilora.config.EnvConfig;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Service d'intégration avec l'API Deepgram pour la transcription vocale (Speech-to-Text).
 * Utilise le modèle nova-2 pour convertir des données audio (WAV) en texte.
 *
 * When Deepgram key is not configured, automatically falls back to OpenAI Whisper.
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

    // ── OpenAI Whisper Fallback ──
    private static final String OPENAI_WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";
    private final String openaiKey;
    private final boolean useOpenAI;

    // ── Singleton ──

    private DeepgramSTTService() {
        this.apiKey = EnvConfig.get("deepgram.api.key", "PLACEHOLDER");
        this.model = EnvConfig.get("deepgram.model", "nova-2");

        boolean deepgramConfigured = !"PLACEHOLDER".equals(this.apiKey) && !this.apiKey.isBlank();

        // Check OpenAI fallback
        String oaiKey = EnvConfig.get("openai.api.key", "");
        boolean oaiConfigured = oaiKey != null && !oaiKey.isBlank() && oaiKey.startsWith("sk-");

        if (deepgramConfigured) {
            this.useOpenAI = false;
            this.openaiKey = null;
            logger.info("DeepgramSTTService: using Deepgram API (model={})", model);
        } else if (oaiConfigured) {
            this.useOpenAI = true;
            this.openaiKey = oaiKey;
            logger.info("DeepgramSTTService: Deepgram not configured, using OpenAI Whisper fallback");
        } else {
            this.useOpenAI = false;
            this.openaiKey = null;
            logger.warn("DeepgramSTTService: No STT key configured — set DEEPGRAM_API_KEY or OPENAI_API_KEY in .env");
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
     * Sends an audio byte array for transcription (Deepgram or OpenAI Whisper fallback).
     *
     * @param audioData Byte array of the audio (WAV format expected)
     * @return The transcribed text, or an empty string if transcription fails
     */
    public String transcribe(byte[] audioData) {
        if (useOpenAI) {
            return transcribeWithWhisper(audioData);
        }
        return transcribeWithDeepgram(audioData);
    }

    /** Transcribe using the Deepgram API. */
    private String transcribeWithDeepgram(byte[] audioData) {
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

    /**
     * Transcribe using OpenAI Whisper API as fallback.
     * Uses multipart/form-data to upload the audio file.
     */
    private String transcribeWithWhisper(byte[] audioData) {
        try {
            String boundary = "----AudioBoundary" + UUID.randomUUID().toString().replace("-", "");

            // Build multipart body
            byte[] prefix = buildMultipartPrefix(boundary);
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

            // Combine: prefix + audioData + suffix
            byte[] body = new byte[prefix.length + audioData.length + suffix.length];
            System.arraycopy(prefix, 0, body, 0, prefix.length);
            System.arraycopy(audioData, 0, body, prefix.length, audioData.length);
            System.arraycopy(suffix, 0, body, prefix.length + audioData.length, suffix.length);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_WHISPER_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + openaiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String text = json.getString("text");
                logger.info("Whisper transcription successful ({} chars)", text.length());
                return text.trim();
            } else {
                logger.error("OpenAI Whisper Error: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("OpenAI Whisper transcription failed", e);
        }
        return "";
    }

    /** Build the multipart/form-data prefix with model field and file header. */
    private byte[] buildMultipartPrefix(String boundary) {
        StringBuilder sb = new StringBuilder();
        // model field
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        sb.append("whisper-1\r\n");
        // file field header
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n");
        sb.append("Content-Type: audio/wav\r\n\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Helpers ──
}
