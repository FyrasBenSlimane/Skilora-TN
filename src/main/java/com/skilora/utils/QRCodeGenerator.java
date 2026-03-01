package com.skilora.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR codes.
 */
public class QRCodeGenerator {
    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    /**
     * Generates a QR code image as a Base64 string.
     *
     * @param text   The content to encode in the QR code (must be non-null/non-blank).
     * @param width  The width of the QR code (pixels).
     * @param height The height of the QR code (pixels).
     * @return Base64 encoded string of the QR code image, or {@code null} if generation fails.
     */
    public static String generateQRCodeBase64(String text, int width, int height) {
        if (text == null || text.trim().isEmpty()) {
            logger.error("Refusing to generate QR code: content is null or blank.");
            return null;
        }
        if (width < 300 || height < 300) {
            logger.warn("Generating QR code with small dimensions ({}x{}). Recommended minimum is 300x300.", width, height);
        }

        logger.info("Generating QR code with content: '{}' ({}x{})", text, width, height);

        try {
            BufferedImage image = generateQRCodeImage(text, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (WriterException | IOException e) {
            logger.error("Error generating QR code Base64 for content '{}'", text, e);
            return null;
        }
    }

    /**
     * Generates a QR code image with high contrast (pure black on pure white).
     *
     * @param text   The content to encode in the QR code (must be non-null/non-blank).
     * @param width  The width of the QR code (pixels).
     * @param height The height of the QR code (pixels).
     * @return BufferedImage of the QR code with maximum contrast.
     */
    public static BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException {
        if (text == null || text.trim().isEmpty()) {
            logger.error("Refusing to generate QR code image: content is null or blank.");
            throw new WriterException("QR code content must not be null or blank");
        }

        // Validate dimensions
        if (width < 100 || height < 100) {
            logger.warn("QR code dimensions too small ({}x{}), using minimum 100x100", width, height);
            width = Math.max(100, width);
            height = Math.max(100, height);
        }

        // Optimal settings for scannable QR codes
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Highest error correction (30% recovery)
        hints.put(EncodeHintType.MARGIN, 4); // Increased margin (quiet zone) for better scanning
        
        // Generate bit matrix
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
            logger.error("Failed to encode QR code for text: '{}'", text, e);
            throw e;
        }
        
        // Create image with maximum contrast: pure black (#000000) on pure white (#FFFFFF)
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int black = 0x000000; // Pure black for maximum contrast
        int white = 0xFFFFFF; // Pure white background
        
        // Render bit matrix to image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? black : white);
            }
        }
        
        logger.info("QR code generated successfully: {}x{} pixels, content length: {} chars, error correction: HIGH", 
                width, height, text.length());
        
        return image;
    }
}
