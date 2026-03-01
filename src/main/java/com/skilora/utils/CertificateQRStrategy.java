package com.skilora.utils;

import com.skilora.config.AppConfig;
import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Strategy for generating QR code content for certificates.
 * 
 * OPTION 3 (PRIMARY): Uses localhost server URL (http://localhost:PORT/verify/CERT_ID)
 * OPTION 1 (FALLBACK): Encodes certificate data as JSON if server is unavailable
 */
public class CertificateQRStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CertificateQRStrategy.class);
    
    /**
     * Generates QR code content using the best available method.
     * 
     * Priority:
     * 1. OPTION 3: localhost server URL (if server is running)
     * 2. OPTION 1: JSON data (if server is not available)
     * 
     * @param cert Certificate entity
     * @param training Training entity
     * @param user User entity
     * @return QR code content (URL or JSON string)
     */
    public static String generateQRContent(Certificate cert, Training training, User user) {
        // First check if server is running (fast check)
        boolean serverRunning = com.skilora.service.formation.VerificationServer.isRunning();
        int serverPort = com.skilora.service.formation.VerificationServer.getPort();
        
        logger.info("QR Code Generation - Server status: running={}, port={}", serverRunning, serverPort);
        
        if (serverRunning && serverPort > 0) {
            // Server is running, ALWAYS use OPTION 3: local server URL
            // This ensures the phone can scan and display the certificate
            try {
                String localhostUrl = buildLocalhostUrl(cert);
                
                logger.info("✓ Using OPTION 3 (local server URL) for certificate {} QR code", cert.getId());
                logger.info("✓ QR Code URL: {}", localhostUrl);
                logger.info("✓ Phone can scan this QR code to view certificate");
                logger.info("✓ Make sure phone is on the same WiFi network as this computer");
                
                // Verify the URL is valid
                if (localhostUrl == null || localhostUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("Generated URL is empty");
                }
                if (!localhostUrl.startsWith("http://") && !localhostUrl.startsWith("https://")) {
                    throw new IllegalArgumentException("Generated URL is not a valid HTTP URL: " + localhostUrl);
                }
                
                return localhostUrl;
            } catch (Exception e) {
                logger.error("Failed to build localhost URL for certificate {}: {}", cert.getId(), e.getMessage(), e);
                logger.warn("Falling back to JSON data due to URL build error");
                return CertificateQRData.generateQRData(cert, training, user);
            }
        }
        
        // Fallback to OPTION 1: JSON data only if server is NOT running
        logger.warn("⚠ Server not running (running={}, port={}), using OPTION 1 (JSON data) as fallback for certificate {}", 
                serverRunning, serverPort, cert.getId());
        logger.warn("⚠ NOTE: JSON data will show raw certificate info, not a formatted certificate page");
        logger.warn("⚠ To fix: Ensure VerificationServer.start() is called during application startup");
        return CertificateQRData.generateQRData(cert, training, user);
    }
    
    /**
     * Builds local server verification URL for a certificate.
     * Uses local IP address so phones on the same WiFi can access it.
     * Format: http://LOCAL_IP:PORT/verify/CERT_ID
     * 
     * @param cert Certificate entity
     * @return local server URL string
     */
    private static String buildLocalhostUrl(Certificate cert) {
        // Get the actual port the server is running on (may differ from config if port was in use)
        int port = com.skilora.service.formation.VerificationServer.getPort();
        if (port == -1) {
            // Server not running, fallback to configured port
            try {
                port = Integer.parseInt(AppConfig.getProperty("app.server.port", "8080"));
                logger.warn("Server port not available, using configured port: {}", port);
            } catch (NumberFormatException e) {
                port = 8080;
                logger.warn("Invalid port configuration, using default: {}", port);
            }
        }
        
        logger.debug("Using port {} for QR code URL", port);
        
        // ALWAYS use local IP address (never localhost) so phones on same WiFi can access
        String localIP = NetworkUtils.getLocalIPAddress();
        if (localIP == null || localIP.isEmpty()) {
            // If IP detection fails, we cannot serve to phones - this is a problem
            logger.error("CRITICAL: Could not detect local IP address. QR code will not work from phones!");
            logger.error("NetworkUtils.getLocalIPAddress() returned null or empty");
            // Still try to use a fallback, but log the issue
            localIP = "localhost";
            logger.warn("Using localhost as fallback (will NOT work from phones on WiFi)");
        } else {
            logger.debug("Detected local IP address: {}", localIP);
        }
        
        String certId = cert.getCertificateNumber();
        if (certId == null || certId.trim().isEmpty()) {
            // Fallback to token if certificate number is not available
            logger.warn("Certificate {} has no certificate number, using verification token", cert.getId());
            certId = cert.getVerificationToken();
            if (certId == null || certId.trim().isEmpty()) {
                throw new IllegalArgumentException("Certificate has no certificate number or verification token");
            }
        }
        
        // Clean certificate ID (remove any whitespace)
        certId = certId.trim();
        
        // Build URL: http://IP:PORT/verify/CERT_ID
        String url = String.format("http://%s:%d/verify/%s", localIP, port, certId);
        
        logger.info("Built local server URL for certificate {} (ID: {}): {}", cert.getId(), certId, url);
        
        // Final validation
        if (url.contains("localhost") && !localIP.equals("localhost")) {
            logger.warn("URL contains 'localhost' but IP is {}, this may cause issues", localIP);
        }
        
        return url;
    }
    
    /**
     * Checks if the verification server is available by attempting a connection.
     * Uses a shorter timeout and accepts various response codes that indicate the server is running.
     * 
     * @param urlString The URL to check
     * @return true if server responds, false otherwise
     */
    private static boolean isServerAvailable(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(500); // 500ms timeout (faster check)
            connection.setReadTimeout(500);
            connection.setInstanceFollowRedirects(false);
            
            int responseCode = connection.getResponseCode();
            // Accept any response code that indicates the server is running:
            // 200 (OK), 404 (not found but server is running), 405 (method not allowed but server is running),
            // 400 (bad request but server is running), 500 (server error but server is running)
            boolean available = responseCode >= 200 && responseCode < 600; // Any HTTP response means server is running
            
            connection.disconnect();
            
            if (available) {
                logger.debug("Verification server is available at {} (response code: {})", urlString, responseCode);
            }
            
            return available;
            
        } catch (java.net.SocketTimeoutException e) {
            logger.debug("Verification server connection timeout at {}: {}", urlString, e.getMessage());
            return false;
        } catch (java.net.ConnectException e) {
            logger.debug("Verification server connection refused at {}: {}", urlString, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.debug("Verification server not available at {}: {}", urlString, e.getMessage());
            return false;
        }
    }
    
    /**
     * Test if the generated URL is accessible from the network.
     * This can help debug connectivity issues.
     * 
     * @param urlString The URL to test
     * @return true if accessible, false otherwise
     */
    public static boolean testUrlAccessibility(String urlString) {
        return isServerAvailable(urlString);
    }
    
    /**
     * Alternative method: Always use localhost URL (assumes server is running).
     * Use this if you want to force localhost without checking availability.
     * 
     * @param cert Certificate entity
     * @return localhost URL string
     */
    public static String buildLocalhostUrlForced(Certificate cert) {
        return buildLocalhostUrl(cert);
    }
    
    /**
     * Alternative method: Always use JSON data (offline mode).
     * Use this if you want to force JSON encoding without checking server.
     * 
     * @param cert Certificate entity
     * @param training Training entity
     * @param user User entity
     * @return JSON string
     */
    public static String buildJSONDataForced(Certificate cert, Training training, User user) {
        return CertificateQRData.generateQRData(cert, training, user);
    }
}
