package com.skilora.formation.service;

import com.skilora.formation.entity.Certificate;
import com.skilora.formation.entity.Formation;
import com.skilora.user.entity.User;
import com.skilora.user.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * CertificateVerificationServer
 * 
 * Embedded HTTP server for certificate verification.
 * Binds to 0.0.0.0 (all network interfaces) so phones on the
 * same WiFi network can reach it.
 * 
 * Routes:
 *   /verify/certificate/{token}   → serves the verification HTML page
 *   /verify/{certId}              → serves the verification HTML page
 *   /api/verify/{certId}          → returns JSON with certificate details
 *   /api/verify/token/{token}     → returns JSON with certificate details
 * 
 * Adapted from branch VerificationServer.java.
 */
public class CertificateVerificationServer {

    private static final Logger logger = LoggerFactory.getLogger(CertificateVerificationServer.class);
    private static HttpServer server;
    private static final int DEFAULT_PORT = 8443;

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public static synchronized void start() {
        if (server != null) return;

        int preferredPort = DEFAULT_PORT;
        int port = preferredPort;
        int maxAttempts = 50;
        boolean started = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 50);

                server.createContext("/verify/", new PageHandler());
                server.createContext("/verify/certificate/", new PageHandler());
                server.createContext("/api/verify/", new ApiHandler());
                server.createContext("/api/verify/token/", new TokenApiHandler());

                server.setExecutor(Executors.newFixedThreadPool(4));
                server.start();

                String localIP = getLocalIPAddress();
                logger.info("======================================================");
                logger.info("  Certificate Verification Server started");
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
                    port = preferredPort + attempt + 1;
                    logger.debug("Port {} is in use, trying port {}...", preferredPort + attempt, port);
                    server = null;
                } else {
                    logger.error("Failed to start Verification Server on port {}: {}", port, e.getMessage());
                    break;
                }
            }
        }

        if (!started) {
            logger.error("FAILED TO START VERIFICATION SERVER — no available port found between {} and {}",
                    preferredPort, preferredPort + maxAttempts - 1);
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("Verification Server stopped.");
        }
    }

    public static synchronized boolean isRunning() {
        return server != null;
    }

    public static synchronized int getPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static String extractIdentifier(String path) {
        if (path == null || path.isEmpty()) return "";
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static String toJson(String status, String message) {
        return "{\"status\":\"" + escapeJson(status) + "\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String buildCertificateJson(Certificate cert) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"status\":\"valid\"");
        json.append(",\"certId\":\"").append(escapeJson(cert.getCertificateNumber())).append("\"");

        // Holder name
        String holderName = "Unknown";
        try {
            Optional<User> user = UserService.getInstance().findById(cert.getUserId());
            if (user.isPresent()) {
                User u = user.get();
                holderName = (u.getFullName() != null && !u.getFullName().isBlank())
                        ? u.getFullName() : u.getUsername();
            }
        } catch (Exception e) {
            logger.warn("Could not resolve user for certificate: {}", e.getMessage());
        }
        json.append(",\"holderName\":\"").append(escapeJson(holderName)).append("\"");

        // Formation title
        String formationTitle = "Unknown Formation";
        try {
            Formation formation = FormationService.getInstance().findById(cert.getFormationId());
            if (formation != null) {
                formationTitle = formation.getTitle();
            }
        } catch (Exception e) {
            logger.warn("Could not resolve formation for certificate: {}", e.getMessage());
        }
        json.append(",\"trainingTitle\":\"").append(escapeJson(formationTitle)).append("\"");

        // Completion date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String date = cert.getCompletedAt() != null
                ? cert.getCompletedAt().format(fmt)
                : cert.getIssuedDate().format(fmt);
        json.append(",\"completionDate\":\"").append(escapeJson(date)).append("\"");

        json.append("}");
        return json.toString();
    }

    /**
     * Gets the server address (IP:port) for external access.
     */
    public String getServerAddress() {
        return getLocalIPAddress();
    }

    /**
     * Gets the local IP address for LAN access.
     */
    public static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.warn("Could not determine local IP address: {}", e.getMessage());
        }
        return "localhost";
    }

    // ─── Page Handler ────────────────────────────────────────────────────────

    static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Page Request: {} {}", exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            if (handlePreflight(exchange)) return;

            InputStream is = getClass().getResourceAsStream("/com/skilora/view/formation/verification_page.html");
            if (is == null) {
                // Fallback to root classpath
                is = getClass().getResourceAsStream("/verification_page.html");
            }
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
        }
    }

    // ─── API Handler (by certificate number) ────────────────────────────────

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
                responseBody = toJson("invalid", "No certificate ID provided.");
            } else {
                Certificate cert = CertificateService.getInstance().getCertificateByNumber(certificateId);
                if (cert == null) {
                    statusCode = 404;
                    responseBody = toJson("invalid", "Certificate not found for ID: " + certificateId);
                    logger.warn("Certificate not found for ID: {}", certificateId);
                } else {
                    statusCode = 200;
                    responseBody = buildCertificateJson(cert);
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

    // ─── Token API Handler ───────────────────────────────────────────────────

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
                responseBody = toJson("invalid", "No certificate token provided.");
            } else {
                Certificate cert = CertificateService.getInstance().getCertificateByToken(token);
                if (cert == null) {
                    statusCode = 404;
                    responseBody = toJson("invalid", "Certificate not found for token: " + token);
                    logger.warn("Certificate not found for token: {}", token);
                } else {
                    statusCode = 200;
                    responseBody = buildCertificateJson(cert);
                    logger.info("Certificate verified (token): {}", cert.getCertificateNumber());
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
