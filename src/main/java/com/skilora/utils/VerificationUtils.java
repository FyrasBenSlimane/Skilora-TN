package com.skilora.utils;

import com.skilora.config.AppConfig;
import com.skilora.model.entity.formation.Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility helpers related to public certificate verification.
 *
 * Centralises construction of the verification URL so that:
 *  - The base URL normalisation logic is used consistently
 *  - Only the strong verification token is ever exposed publicly
 *  - Callers get clear exceptions instead of silently generating bad QR codes
 */
public final class VerificationUtils {

    private static final Logger logger = LoggerFactory.getLogger(VerificationUtils.class);

    private VerificationUtils() {
        // Utility class
    }

    /**
     * Builds the public verification URL for a certificate using certificate ID.
     * This is the preferred method for QR codes as it uses a human-readable certificate number.
     *
     * @param certificate Non-null certificate with a non-empty certificate number
     * @return Fully-qualified URL such as https://example.com/verify/{certificateId}
     * @throws IllegalArgumentException if the certificate or its certificate number is missing/invalid
     */
    public static String buildVerificationUrlByCertificateId(Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Cannot build verification URL: certificate is null");
        }

        String certId = certificate.getCertificateNumber();
        if (certId == null || certId.trim().isEmpty()) {
            String message = "Certificate " + certificate.getId()
                    + " has no certificate number. Refusing to build verification URL.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        // Check if a public verification URL is configured (for QR codes accessible from anywhere)
        String publicUrl = AppConfig.getPublicVerificationUrl();
        if (publicUrl != null && !publicUrl.isEmpty()) {
            // Use public URL - accessible from any network (no WiFi requirement)
            String verificationUrl = publicUrl + "verify/" + certId.trim();
            // Remove any double slashes
            verificationUrl = verificationUrl.replaceAll("([^:])//+", "$1/");
            logger.info("Built public verification URL (accessible from anywhere) for certificate {} (ID: {}): {}", 
                    certificate.getId(), certId, verificationUrl);
            return verificationUrl;
        }
        
        // Fallback to local/base URL (requires same WiFi network)
        String baseUrl = AppConfig.getVerificationBaseUrl();
        logger.info("No public URL configured (app.verification.public.url is empty). Using local/base URL: {}", baseUrl);
        logger.info("⚠ QR codes will require the phone to be on the same WiFi network. To enable public access, configure app.verification.public.url in application.properties");
        
        // Detect if this is a local/private network address (keep HTTP, don't force HTTPS)
        // Check for localhost, 127.0.0.1, or private IP ranges (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
        // Also check for any 172.x.x.x as it's commonly used in private networks
        boolean isLocal = baseUrl.contains("localhost") 
                || baseUrl.contains("127.0.0.1")
                || baseUrl.contains("192.168.")
                || baseUrl.contains("10.")
                || baseUrl.contains("172."); // All 172.x.x.x are typically private
        
        logger.debug("Is local address: {} (baseUrl: {})", isLocal, baseUrl);
        
        if (!isLocal) {
            // Force HTTPS for production URLs only
            if (baseUrl.startsWith("http://")) {
                baseUrl = "https://" + baseUrl.substring("http://".length());
                logger.info("Upgraded verification URL to HTTPS for public access: {}", baseUrl);
            } else if (!baseUrl.startsWith("https://")) {
                baseUrl = "https://" + baseUrl;
            }
        } else {
            logger.debug("Keeping HTTP for local address: {}", baseUrl);
        }
        
        // Extract domain and port from base URL, removing any path
        // Example: http://172.24.42.1:8080/verify/certificate/ -> http://172.24.42.1:8080
        String domainAndPort = baseUrl;
        if (baseUrl.contains("://")) {
            int schemeEnd = baseUrl.indexOf("://") + 3;
            int pathStart = baseUrl.indexOf('/', schemeEnd);
            if (pathStart > 0) {
                domainAndPort = baseUrl.substring(0, pathStart);
            }
        }
        
        logger.debug("Extracted domain and port: {}", domainAndPort);
        
        // Build clean verification URL: {domain}:{port}/verify/{certificateId}
        // Use /verify/ path (not /verify/certificate/) for certificate ID-based URLs
        // Ensure no double slashes or duplicate /verify/ segments
        String verificationUrl;
        if (domainAndPort.endsWith("/")) {
            verificationUrl = domainAndPort + "verify/" + certId.trim();
        } else {
            verificationUrl = domainAndPort + "/verify/" + certId.trim();
        }
        
        // Final validation: remove any duplicate /verify/verify/ patterns
        verificationUrl = verificationUrl.replaceAll("/verify/verify/", "/verify/");
        
        if (isLocal) {
            logger.info("Built LOCAL verification URL (requires same WiFi) for certificate {} (ID: {}): {}", 
                    certificate.getId(), certId, verificationUrl);
        } else {
            logger.info("Built verification URL for certificate {} (ID: {}): {}", 
                    certificate.getId(), certId, verificationUrl);
        }
        return verificationUrl;
    }

    /**
     * Builds the public verification URL for a certificate using verification token.
     * This method is kept for backward compatibility.
     *
     * @param certificate Non-null certificate with a non-empty verification token
     * @return Fully-qualified URL such as https://example.com/verify/certificate/{token}
     * @throws IllegalArgumentException if the certificate or its token is missing/invalid
     */
    public static String buildVerificationUrl(Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Cannot build verification URL: certificate is null");
        }

        String token = certificate.getVerificationToken();
        if (token == null || token.trim().isEmpty()) {
            String message = "Certificate " + certificate.getId()
                    + " has no verification token. Refusing to build verification URL.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        String baseUrl = AppConfig.getVerificationBaseUrl();
        String verificationUrl = baseUrl + token;
        logger.debug("Built verification URL for certificate {}: {}", certificate.getId(), verificationUrl);
        return verificationUrl;
    }
}

