package com.skilora.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skilora.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for the Python recruitment API (FastAPI): CV analysis, matching, recommendations, WhatsApp.
 */
public class PythonApiService {
    private static final Logger logger = LoggerFactory.getLogger(PythonApiService.class);
    private static volatile PythonApiService instance;
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "PythonApiClient");
        t.setDaemon(true);
        return t;
    });

    private final String baseUrl;

    private PythonApiService() {
        this.baseUrl = AppConfig.getProperty("python.api.url", "http://localhost:8000").replaceAll("/$", "");
    }

    public static PythonApiService getInstance() {
        if (instance == null) {
            synchronized (PythonApiService.class) {
                if (instance == null) {
                    instance = new PythonApiService();
                }
            }
        }
        return instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * POSTs a PDF file to /analyze-cv and returns the analysis as JsonObject (skills_detected, experience_level, etc.).
     */
    public CompletableFuture<JsonObject> analyzeCv(File file) {
        return CompletableFuture.supplyAsync(() -> {
            if (file == null || !file.isFile() || !file.getName().toLowerCase().endsWith(".pdf")) {
                logger.warn("analyzeCv: invalid or non-PDF file");
                return null;
            }
            String endpoint = baseUrl + "/analyze-cv";
            String boundary = "----SkiloraBoundary" + System.currentTimeMillis();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                try (OutputStream out = conn.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
                    writer.append("Content-Type: application/pdf\r\n\r\n").flush();
                    Files.copy(file.toPath(), out);
                    out.flush();
                    writer.append("\r\n--").append(boundary).append("--\r\n").flush();
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    try (Reader r = new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8)) {
                        String err = new BufferedReader(r).readLine();
                        logger.warn("analyze-cv failed {}: {}", code, err);
                    }
                    return null;
                }
                try (Reader r = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                    logger.info("analyze-cv success for {}", file.getName());
                    return obj;
                }
            } catch (Exception e) {
                logger.error("analyzeCv failed for " + file.getAbsolutePath(), e);
                return null;
            }
        }, executor);
    }
}
