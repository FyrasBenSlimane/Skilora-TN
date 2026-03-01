package com.skilora.utils;

import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * PDF Generator for Certificates
 * 
 * Generates PDF certificates for completed trainings.
 * Note: This generates HTML files that can be printed as PDFs or converted using external tools.
 */
public class PDFGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PDFGenerator.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final String CERTIFICATES_DIR = "certificates";

    /**
     * Generate a PDF certificate for a user's completed training
     */
    public static String generateCertificate(Certificate certificate, Training training, User user) {
        try {
            // Ensure certificates directory exists
            File certDir = new File(CERTIFICATES_DIR);
            if (!certDir.exists()) {
                certDir.mkdirs();
            }

            // Generate HTML content for the certificate
            String htmlContent = generateCertificateHTML(certificate, training, user);
            
            // Generate file name: Certificate_<UserName>_<TrainingTitle>.html
            String userName = sanitizeFileName(user.getFullName() != null && !user.getFullName().trim().isEmpty() 
                ? user.getFullName() : user.getUsername());
            String trainingTitle = sanitizeFileName(training.getTitle());
            String fileName = String.format("Certificate_%s_%s.html", userName, trainingTitle);
            String filePath = CERTIFICATES_DIR + File.separator + fileName;
            
            // Write HTML file with UTF-8 encoding to preserve French characters
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new java.io.FileOutputStream(filePath), StandardCharsets.UTF_8)) {
                writer.write(htmlContent);
            }
            
            logger.info("Certificate generated: {}", filePath);
            return filePath;
            
        } catch (IOException e) {
            logger.error("Error generating certificate PDF", e);
            throw new RuntimeException("Failed to generate certificate: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sanitize file name by removing invalid characters
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "Unknown";
        // Replace invalid file name characters with underscores
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
    
    /**
     * Format director signature base64 string for use in HTML img tag
     * 
     * @param directorSignatureBase64 Raw base64 string from database (may include data URL prefix or whitespace)
     * @return Formatted img tag with proper data URL, or empty string if signature is invalid/missing
     */
    private static String formatDirectorSignature(String directorSignatureBase64) {
        // Return empty string if signature is null or empty
        if (directorSignatureBase64 == null || directorSignatureBase64.isBlank()) {
            logger.debug("Director signature is null or empty - not displaying signature image");
            return "";
        }
        
        try {
            // Clean the base64 string
            String cleaned = directorSignatureBase64.trim();
            
            // Remove any existing data URL prefix if present
            if (cleaned.startsWith("data:image/png;base64,")) {
                cleaned = cleaned.substring("data:image/png;base64,".length());
            } else if (cleaned.startsWith("data:image/")) {
                // Handle other image formats or malformed prefixes
                int base64Index = cleaned.indexOf("base64,");
                if (base64Index >= 0) {
                    cleaned = cleaned.substring(base64Index + "base64,".length());
                }
            }
            
            // Remove all whitespace and newlines (base64 should be continuous)
            cleaned = cleaned.replaceAll("\\s+", "");
            
            // Validate that it's not empty after cleaning
            if (cleaned.isEmpty()) {
                logger.warn("Director signature base64 is empty after cleaning");
                return "";
            }
            
            // Validate base64 format (basic check - should only contain base64 characters)
            if (!cleaned.matches("^[A-Za-z0-9+/=]+$")) {
                logger.warn("Director signature contains invalid base64 characters");
                return "";
            }
            
            // Format as proper data URL in img tag
            // Remove alt text as requested, and ensure proper styling
            String imgTag = "<img src=\"data:image/png;base64,%s\" " +
                    "style=\"max-width:160px; max-height:80px; display:block; margin:0 auto 8px auto; object-fit:contain;\"/>";
            
            return String.format(imgTag, cleaned);
            
        } catch (Exception e) {
            logger.error("Error formatting director signature: {}", e.getMessage(), e);
            return ""; // Return empty string on error - no broken image will be shown
        }
    }
    
    /**
     * Load Skilora logo from resources and convert to base64 for embedding in HTML
     * 
     * @return Base64-encoded PNG image string, or empty string if logo not found
     */
    private static String loadSkiloraLogoBase64() {
        try {
            // Try to load logo from resources
            // First try: /com/skilora/assets/images/skilora-logo.png
            // Second try: /com/skilora/assets/logo.png
            // Third try: /assets/skilora-logo.png
            String[] logoPaths = {
                "/com/skilora/assets/images/skilora-logo.png",
                "/com/skilora/assets/logo.png",
                "/assets/skilora-logo.png",
                "/skilora-logo.png"
            };
            
            InputStream logoStream = null;
            for (String path : logoPaths) {
                logoStream = PDFGenerator.class.getResourceAsStream(path);
                if (logoStream != null) {
                    logger.debug("Found Skilora logo at: {}", path);
                    break;
                }
            }
            
            if (logoStream == null) {
                logger.warn("Skilora logo not found in resources. Creating placeholder badge.");
                // Create a simple text-based badge as fallback
                return createTextBadgeBase64();
            }
            
            // Read image and convert to base64
            BufferedImage image = ImageIO.read(logoStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            logoStream.close();
            baos.close();
            
            return Base64.getEncoder().encodeToString(imageBytes);
            
        } catch (Exception e) {
            logger.warn("Error loading Skilora logo: {}. Using text badge fallback.", e.getMessage());
            return createTextBadgeBase64();
        }
    }
    
    /**
     * Create a simple text-based badge as fallback if logo image is not available
     */
    private static String createTextBadgeBase64() {
        try {
            // Create a simple badge image programmatically
            BufferedImage badge = new BufferedImage(160, 80, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = badge.createGraphics();
            
            // Set rendering hints for better quality
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                             java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background (white with border)
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 160, 80);
            g.setColor(new java.awt.Color(102, 126, 234)); // #667eea
            g.setStroke(new java.awt.BasicStroke(2));
            g.drawRect(1, 1, 157, 77);
            
            // Draw text "SKILORA"
            g.setColor(new java.awt.Color(102, 126, 234));
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
            java.awt.FontMetrics fm = g.getFontMetrics();
            String text = "SKILORA";
            int textWidth = fm.stringWidth(text);
            int x = (160 - textWidth) / 2;
            int y = 35;
            g.drawString(text, x, y);
            
            // Draw "TUNISIA" below
            g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
            fm = g.getFontMetrics();
            text = "TUNISIA";
            textWidth = fm.stringWidth(text);
            x = (160 - textWidth) / 2;
            y = 55;
            g.drawString(text, x, y);
            
            g.dispose();
            
            // Convert to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(badge, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            baos.close();
            
            return Base64.getEncoder().encodeToString(imageBytes);
            
        } catch (Exception e) {
            logger.error("Error creating text badge: {}", e.getMessage());
            return ""; // Return empty if even fallback fails
        }
    }
    
    /**
     * Format formation creation date in French
     * 
     * @param createdAt LocalDateTime of formation creation
     * @return Formatted date string like "Formation créée le : 27 février 2026"
     */
    private static String formatFormationCreationDate(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }
        
        try {
            // Use French locale for month names
            DateTimeFormatter frenchFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
            String dateStr = createdAt.format(frenchFormatter);
            return "Formation créée le : " + dateStr;
        } catch (Exception e) {
            logger.warn("Error formatting creation date: {}", e.getMessage());
            // Fallback to default format
            String dateStr = createdAt.format(DATE_FORMAT);
            return "Formation créée le : " + dateStr;
        }
    }

    private static String generateCertificateHTML(Certificate cert, Training training, User user) {
        LocalDateTime completionDate = cert.getCompletedAt() != null ? cert.getCompletedAt() : cert.getIssuedAt();
        String dateStr = completionDate.format(DATE_FORMAT);
        
        // Get user full name or fallback to username
        String userFullName = user.getFullName() != null && !user.getFullName().trim().isEmpty() 
            ? user.getFullName() : user.getUsername();
        
        // Get training details
        String category = training.getCategory() != null ? training.getCategory().getDisplayName() : "Formation";
        String level = training.getLevel() != null ? training.getLevel().getDisplayName() : "";
        int duration = training.getDuration();
        
        // Certificate ID (using certificate number)
        String certId = cert.getCertificateNumber();
        
        // Platform name
        String platformName = "Skilora Tunisia";

        // Director signature (per-formation, base64 PNG stored in Training)
        String rawSignature = training.getDirectorSignature();
        logger.debug("Certificate generation for training ID {}: signature present = {}", 
            training.getId(), rawSignature != null && !rawSignature.isBlank());
        String directorSignatureImgTag = formatDirectorSignature(rawSignature);
        
        // Skilora logo (loaded from resources or generated as fallback)
        String skiloraLogoBase64 = loadSkiloraLogoBase64();
        String skiloraLogoImgTag = "";
        if (skiloraLogoBase64 != null && !skiloraLogoBase64.isBlank()) {
            skiloraLogoImgTag = "<img src=\"data:image/png;base64,%s\" " +
                    "style=\"max-width:160px; max-height:80px; display:block; margin:0 auto 15px auto; object-fit:contain;\"/>"
                    .formatted(skiloraLogoBase64);
        }
        
        // Formation creation date (in French)
        String formationCreationDate = formatFormationCreationDate(training.getCreatedAt());
        
        // Generate simple QR code with verification URL
        String qrCodeImgTag = "";
        try {
            // Get certificate ID (reuse existing certId variable from method parameter context)
            String qrCertId = cert.getCertificateNumber();
            if (qrCertId == null || qrCertId.trim().isEmpty()) {
                qrCertId = cert.getVerificationToken();
                if (qrCertId == null || qrCertId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Certificate has no ID or token");
                }
            }
            qrCertId = qrCertId.trim();
            
            // Get server port
            int port = com.skilora.service.formation.VerificationServer.getPort();
            if (port == -1) {
                port = 8080;
            }
            
            // Get local IP
            String localIP = com.skilora.utils.NetworkUtils.getLocalIPAddress();
            if (localIP == null || localIP.isEmpty()) {
                localIP = "localhost";
            }
            
            // Build URL
            String qrUrl = String.format("http://%s:%d/verify/%s", localIP, port, qrCertId);
            
            // Generate QR code
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeImage(qrUrl, 150, 150);
            
            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            byte[] qrBytes = baos.toByteArray();
            String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);
            
            // Embed in HTML
            qrCodeImgTag = "<img src=\"data:image/png;base64,%s\" alt=\"Code QR de vérification\" class=\"qr-code\" " +
                    "style=\"width:150px; height:150px; min-width:150px; min-height:150px; background:#ffffff; border:3px solid #000000; padding:8px; display:block; margin:0 auto;\">"
                    .formatted(qrBase64);
            
            logger.info("QR code generated for certificate {}: {}", cert.getId(), qrUrl);
        } catch (Exception ex) {
            logger.error("Error generating QR code for certificate {}: {}", cert.getId(), ex.getMessage(), ex);
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Certificat de Complétion - %s</title>
                <style>
                    @page {
                        size: A4 landscape;
                        margin: 0;
                    }
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: 'Georgia', 'Times New Roman', serif;
                        margin: 0;
                        padding: 0;
                        background: #f5f5f5;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                    }
                    .certificate-wrapper {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 40px;
                        width: 100%%;
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                    }
                    .certificate {
                        background: white;
                        padding: 100px 80px;
                        max-width: 1100px;
                        width: 100%%;
                        text-align: center;
                        position: relative;
                        box-shadow: 0 20px 80px rgba(0,0,0,0.3);
                    }
                    /* Decorative border */
                    .certificate::before {
                        content: '';
                        position: absolute;
                        top: 30px;
                        left: 30px;
                        right: 30px;
                        bottom: 30px;
                        border: 8px solid #667eea;
                        border-radius: 4px;
                        pointer-events: none;
                    }
                    .certificate::after {
                        content: '';
                        position: absolute;
                        top: 50px;
                        left: 50px;
                        right: 50px;
                        bottom: 50px;
                        border: 2px solid #764ba2;
                        border-radius: 2px;
                        pointer-events: none;
                    }
                    /* Header */
                    .header {
                        margin-bottom: 60px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        color: #667eea;
                        font-size: 48px;
                        font-weight: bold;
                        letter-spacing: 4px;
                        margin-bottom: 10px;
                        text-transform: uppercase;
                    }
                    .header .subtitle {
                        color: #888;
                        font-size: 16px;
                        letter-spacing: 2px;
                        margin-top: 10px;
                    }
                    /* Content */
                    .content {
                        margin: 60px 0;
                        position: relative;
                        z-index: 1;
                    }
                    .content .intro-text {
                        font-size: 22px;
                        line-height: 1.8;
                        color: #333;
                        margin-bottom: 40px;
                    }
                    .name {
                        font-size: 44px;
                        font-weight: bold;
                        color: #667eea;
                        margin: 40px 0;
                        padding: 20px 0;
                        border-bottom: 3px solid #764ba2;
                        border-top: 3px solid #764ba2;
                        display: inline-block;
                        min-width: 60%%;
                    }
                    .training-title {
                        font-size: 32px;
                        color: #764ba2;
                        margin: 40px 0 20px 0;
                        font-weight: bold;
                    }
                    .training-details {
                        font-size: 18px;
                        color: #666;
                        margin: 20px 0;
                        line-height: 1.8;
                    }
                    .training-details span {
                        display: inline-block;
                        margin: 0 15px;
                        padding: 8px 20px;
                        background: #f0f0f0;
                        border-radius: 20px;
                    }
                    .completion-date {
                        font-size: 20px;
                        color: #333;
                        margin: 40px 0;
                        font-style: italic;
                    }
                    /* Footer */
                    .footer {
                        margin-top: 80px;
                        padding-top: 40px;
                        border-top: 3px solid #eee;
                        position: relative;
                        z-index: 1;
                    }
                    .signature-section {
                        display: flex;
                        justify-content: space-around;
                        margin-top: 60px;
                        margin-bottom: 40px;
                    }
                    .signature-box {
                        flex: 1;
                        max-width: 300px;
                    }
                    .signature-line {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        margin-top: 40px;
                        padding-top: 20px;
                    }
                    .signature-line img {
                        margin-bottom: 15px;
                        max-width: 160px;
                        max-height: 80px;
                        width: auto;
                        height: auto;
                        object-fit: contain;
                        display: block;
                    }
                    .signature-name {
                        font-weight: bold;
                        color: #667eea;
                        font-size: 16px;
                        margin-top: 8px;
                    }
                    .signature-title {
                        color: #888;
                        font-size: 14px;
                        margin-top: 5px;
                    }
                    .formation-date {
                        color: #666;
                        font-size: 12px;
                        margin-top: 8px;
                        font-style: italic;
                    }
                    .platform-name {
                        font-size: 24px;
                        font-weight: bold;
                        color: #667eea;
                        margin-bottom: 20px;
                    }
                    .cert-id {
                        font-size: 14px;
                        color: #999;
                        margin-top: 20px;
                        font-family: 'Courier New', monospace;
                        letter-spacing: 1px;
                    }
                    .cert-id-label {
                        font-weight: bold;
                        color: #666;
                    }
                    .qr-container {
                        margin-top: 30px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                    }
                    .qr-code {
                        width: 150px;
                        height: 150px;
                        min-width: 150px;
                        min-height: 150px;
                        border: 3px solid #000000;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                        border-radius: 8px;
                        background: #ffffff;
                        padding: 8px;
                        display: block;
                        margin: 0 auto;
                    }
                    .qr-label {
                        font-size: 10px;
                        color: #999;
                        margin-top: 8px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    /* Decorative elements */
                    .decorative-corner {
                        position: absolute;
                        width: 80px;
                        height: 80px;
                        border: 4px solid #667eea;
                        z-index: 0;
                    }
                    .corner-top-left {
                        top: 20px;
                        left: 20px;
                        border-right: none;
                        border-bottom: none;
                    }
                    .corner-top-right {
                        top: 20px;
                        right: 20px;
                        border-left: none;
                        border-bottom: none;
                    }
                    .corner-bottom-left {
                        bottom: 20px;
                        left: 20px;
                        border-right: none;
                        border-top: none;
                    }
                    .corner-bottom-right {
                        bottom: 20px;
                        right: 20px;
                        border-left: none;
                        border-top: none;
                    }
                    @media print {
                        body {
                            background: white;
                        }
                        .certificate-wrapper {
                            background: white;
                            padding: 0;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="certificate-wrapper">
                    <div class="certificate">
                        <!-- Decorative corners -->
                        <div class="decorative-corner corner-top-left"></div>
                        <div class="decorative-corner corner-top-right"></div>
                        <div class="decorative-corner corner-bottom-left"></div>
                        <div class="decorative-corner corner-bottom-right"></div>
                        
                        <!-- Header -->
                        <div class="header">
                            <h1>Certificat de Complétion</h1>
                            <div class="subtitle">Ce certificat atteste que</div>
                        </div>
                        
                        <!-- Content -->
                        <div class="content">
                            <div class="intro-text">a complété avec succès la formation</div>
                            <div class="name">%s</div>
                            <div class="training-title">%s</div>
                            <div class="training-details">
                                <span>%s</span>
                                <span>%s</span>
                                <span>%d heures</span>
                            </div>
                            <div class="completion-date">Terminé le %s</div>
                        </div>
                        
                        <!-- Footer -->
                        <div class="footer">
                            <div class="platform-name">%s</div>
                            <div class="signature-section">
                                <div class="signature-box">
                                    <div class="signature-line">
                                        %s
                                        <div class="signature-name">Directeur</div>
                                        <div class="signature-title">Skilora Tunisia</div>
                                    </div>
                                </div>
                                <div class="signature-box">
                                    <div class="signature-line">
                                        %s
                                        <div class="signature-name">Gestionnaire de Plateforme</div>
                                        <div class="signature-title">Skilora Tunisia</div>
                                        <div class="formation-date">%s</div>
                                    </div>
                                </div>
                            </div>
                            <div class="cert-id">
                                <span class="cert-id-label">ID Certificat :</span> %s
                            </div>
                            
                            <!-- QR Code for Verification -->
                            <div class="qr-container">
                                %s
                                <div class="qr-label">Scanner pour vérifier</div>
                            </div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                training.getTitle(),
                userFullName,
                training.getTitle(),
                category,
                level,
                duration,
                dateStr,
                platformName,
                directorSignatureImgTag,
                skiloraLogoImgTag,
                formationCreationDate,
                certId,
                qrCodeImgTag
            );
    }

    /**
     * Download certificate file
     */
    public static void downloadCertificate(String filePath, String targetPath) throws IOException {
        Files.copy(Paths.get(filePath), Paths.get(targetPath));
    }
}
