package com.skilora.service.formation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.skilora.config.AppConfig;
import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.usermanagement.UserService;
import com.skilora.utils.NetworkUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for certificate verification.
 *
 * Binds to 0.0.0.0 (all network interfaces) so phones on the
 * same WiFi network can reach it.  Port is read from application.properties
 * (app.server.port, default 8080).
 *
 * Routes:
 *   /verify/certificate/{token}   → serves the verification HTML page
 *   /api/verify/{token}           → returns JSON with certificate details
 */
public class VerificationServer {

    private static final Logger logger = LoggerFactory.getLogger(VerificationServer.class);
    private static HttpServer server;
    private static final Gson gson = new Gson();

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public static synchronized void start() {
        if (server != null) return;

        int preferredPort;
        try {
            preferredPort = Integer.parseInt(AppConfig.getProperty("app.server.port", "8080"));
        } catch (NumberFormatException e) {
            preferredPort = 8080;
        }

        int port = preferredPort;
        int maxAttempts = 50; // Try up to 50 ports (8080-8129) to find an available one
        boolean started = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // Bind to 0.0.0.0 → reachable from any network interface (WiFi, Ethernet)
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 50);

                // Route: HTML page using certificate ID (public format: /verify/{certificateId})
                server.createContext("/verify/", new PageHandler());

                // Route: HTML page using token (backward compatibility: /verify/certificate/{token})
                server.createContext("/verify/certificate/", new PageHandler());

                // Route: JSON API using certificate ID (public format: /api/verify/{certificateId})
                server.createContext("/api/verify/", new ApiHandler());

                // Route: JSON API using token (backward compatibility: /api/verify/token/{token})
                server.createContext("/api/verify/token/", new TokenApiHandler());

                // Use a real thread pool – the default single-thread executor can deadlock
                server.setExecutor(Executors.newFixedThreadPool(4));
                server.start();

                String localIP = NetworkUtils.getLocalIPAddress();
                logger.info("======================================================");
                logger.info("  Verification Server started");
                if (port != preferredPort) {
                    logger.warn("  Port {} was in use, using port {} instead", preferredPort, port);
                }
                logger.info("  Local:   http://localhost:{}/verify/certificate/", port);
                logger.info("  Network: http://{}:{}/verify/certificate/", localIP, port);
                logger.info("  Scan QR code from phone on the same WiFi network.");
                logger.info("======================================================");
                
                started = true;
                break;

            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                    // Port is in use, try next port
                    port = preferredPort + attempt + 1;
                    logger.debug("Port {} is in use, trying port {}...", preferredPort + attempt, port);
                    server = null; // Reset before next attempt
                } else {
                    // Other error, log and stop trying
                    logger.error("Failed to start Verification Server on port {}: {}", port, e.getMessage());
                    break;
                }
            }
        }

        if (!started) {
            int lastPort = preferredPort + maxAttempts - 1;
            logger.error("======================================================");
            logger.error("  FAILED TO START VERIFICATION SERVER");
            logger.error("  Could not find an available port between {} and {}", preferredPort, lastPort);
            logger.error("  All {} ports are currently in use", maxAttempts);
            logger.error("======================================================");
            logger.error("SOLUTIONS:");
            logger.error("  1. Close applications using ports {}-{}", preferredPort, lastPort);
            logger.error("  2. Change app.server.port in application.properties to a different port");
            logger.error("  3. Restart your computer to free up ports");
            logger.error("======================================================");
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("Verification Server stopped.");
        }
    }
    
    /**
     * Checks if the verification server is currently running.
     * 
     * @return true if server is running, false otherwise
     */
    public static synchronized boolean isRunning() {
        return server != null;
    }
    
    /**
     * Gets the port the server is running on, or -1 if not running.
     * 
     * @return port number or -1 if server is not running
     */
    public static synchronized int getPort() {
        if (server == null) {
            return -1;
        }
        return server.getAddress().getPort();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Adds CORS headers so the browser's fetch() call from the HTML page
     * (which may be served from the same origin) doesn't get blocked.
     */
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * Handles HTTP OPTIONS preflight requests sent by browsers.
     * Returns 204 No Content immediately.
     */
    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /**
     * Extracts the certificate ID or token from a path like:
     *   /verify/SKL-ABC123              → "SKL-ABC123" (certificate ID)
     *   /verify/certificate/ABC123DEF   → "ABC123DEF" (token)
     *   /api/verify/SKL-ABC123         → "SKL-ABC123" (certificate ID)
     *   /api/verify/token/ABC123DEF    → "ABC123DEF" (token)
     * Returns an empty string if the path has no trailing segment.
     */
    private static String extractIdentifier(String path) {
        if (path == null || path.isEmpty()) return "";
        // Remove trailing slash if present
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
    }

    // ─── Page Handler ────────────────────────────────────────────────────────

    /**
     * Serves the verification HTML page for every sub-path under
     * /verify/certificate/.  The token is embedded in the URL and read
     * client-side by JavaScript, which then calls /api/verify/{token}.
     */
    static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Page Request: {} {}", exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            if (handlePreflight(exchange)) return;

            InputStream is = getClass().getResourceAsStream("/verification_page.html");
            if (is == null) {
                logger.error("verification_page.html NOT FOUND in classpath!");
                sendError(exchange, 404, "Verification page not found.");
                return;
            }

            String content;
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
                content = s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }

            byte[] response = content.getBytes(StandardCharsets.UTF_8);
            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }

            logger.info("Served verification page for path: {}", exchange.getRequestURI().getPath());
        }
    }

    // ─── API Handler ─────────────────────────────────────────────────────────

    /**
     * Returns certificate JSON for a given certificate ID (certificate number).
     * Publicly accessible, no auth required.
     * This is the primary handler for public QR code verification.
     */
    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String certificateId = extractIdentifier(exchange.getRequestURI().getPath());
            logger.info("API verify request → certificateId='{}'", certificateId);

            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

            String responseBody;
            int statusCode;

            if (certificateId.isEmpty()) {
                statusCode = 400;
                JsonObject err = new JsonObject();
                err.addProperty("status", "invalid");
                err.addProperty("message", "No certificate ID provided.");
                responseBody = gson.toJson(err);
            } else {
                // Look up by certificate number (public ID like "SKL-ABC123")
                Certificate cert = CertificateService.getInstance().getCertificateByNumber(certificateId);

                if (cert == null) {
                    statusCode = 404;
                    JsonObject err = new JsonObject();
                    err.addProperty("status", "invalid");
                    err.addProperty("message", "Certificate not found for ID: " + certificateId);
                    responseBody = gson.toJson(err);
                    logger.warn("Certificate not found for ID: {}", certificateId);
                } else {
                    statusCode = 200;
                    JsonObject json = new JsonObject();
                    json.addProperty("status", "valid");
                    json.addProperty("certId", cert.getCertificateNumber());

                    // Holder name
                    User user = UserService.getInstance().getUserById(cert.getUserId()).orElse(null);
                    String holderName = "Unknown";
                    if (user != null) {
                        holderName = (user.getFullName() != null && !user.getFullName().isBlank())
                                ? user.getFullName()
                                : user.getUsername();
                    }
                    json.addProperty("holderName", holderName);

                    // Training title
                    Optional<Training> training = TrainingService.getInstance().getTrainingById(cert.getTrainingId());
                    json.addProperty("trainingTitle", training.map(Training::getTitle).orElse("Unknown Training"));

                    // Completion date
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                    String date = cert.getCompletedAt() != null
                            ? cert.getCompletedAt().format(fmt)
                            : cert.getIssuedAt().format(fmt);
                    json.addProperty("completionDate", date);

                    responseBody = gson.toJson(json);
                    logger.info("Certificate verified successfully: {}", cert.getCertificateNumber());
                }
            }

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /**
     * Returns certificate JSON for a given verification token (backward compatibility).
     * Publicly accessible, no auth required.
     */
    static class TokenApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String token = extractIdentifier(exchange.getRequestURI().getPath());
            logger.info("API verify request (token) → token='{}'", token);

            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

            String responseBody;
            int statusCode;

            if (token.isEmpty()) {
                statusCode = 400;
                JsonObject err = new JsonObject();
                err.addProperty("status", "invalid");
                err.addProperty("message", "No certificate token provided.");
                responseBody = gson.toJson(err);
            } else {
                Certificate cert = CertificateService.getInstance().getCertificateByToken(token);

                if (cert == null) {
                    statusCode = 404;
                    JsonObject err = new JsonObject();
                    err.addProperty("status", "invalid");
                    err.addProperty("message", "Certificate not found for token: " + token);
                    responseBody = gson.toJson(err);
                    logger.warn("Certificate not found for token: {}", token);
                } else {
                    statusCode = 200;
                    JsonObject json = new JsonObject();
                    json.addProperty("status", "valid");
                    json.addProperty("certId", cert.getCertificateNumber());

                    // Holder name
                    User user = UserService.getInstance().getUserById(cert.getUserId()).orElse(null);
                    String holderName = "Unknown";
                    if (user != null) {
                        holderName = (user.getFullName() != null && !user.getFullName().isBlank())
                                ? user.getFullName()
                                : user.getUsername();
                    }
                    json.addProperty("holderName", holderName);

                    // Training title
                    Optional<Training> training = TrainingService.getInstance().getTrainingById(cert.getTrainingId());
                    json.addProperty("trainingTitle", training.map(Training::getTitle).orElse("Unknown Training"));

                    // Completion date
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                    String date = cert.getCompletedAt() != null
                            ? cert.getCompletedAt().format(fmt)
                            : cert.getIssuedAt().format(fmt);
                    json.addProperty("completionDate", date);

                    responseBody = gson.toJson(json);
                    logger.info("Certificate verified successfully (token): {}", cert.getCertificateNumber());
                }
            }

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
