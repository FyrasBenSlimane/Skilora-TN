package com.skilora.utils;

import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Set;

/**
 * Utility class for Base64 image encoding/decoding.
 * Profile photos are stored as Base64 data URIs in the database
 * to ensure they persist regardless of file system changes.
 */
public final class ImageUtils {

    /** Maximum allowed image file size: 5 MB */
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    /** Maximum allowed image file size in MB (for display) */
    public static final int MAX_IMAGE_SIZE_MB = 5;

    /** Supported file extensions */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp"
    );

    private ImageUtils() {
        // Utility class
    }

    /**
     * Validates an image file for upload.
     *
     * @param file the image file to validate
     * @return null if valid, or an error message key if invalid
     */
    public static String validateImageFile(File file) {
        if (file == null || !file.exists()) {
            return "profile.photo.error.not_found";
        }

        if (file.length() > MAX_IMAGE_SIZE_BYTES) {
            return "profile.photo.error.too_large";
        }

        String name = file.getName().toLowerCase();
        boolean validExtension = SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
        if (!validExtension) {
            return "profile.photo.error.invalid_format";
        }

        return null;
    }

    /**
     * Encodes an image file to a Base64 data URI string.
     * Format: data:image/png;base64,iVBORw0KGgo...
     *
     * @param file the image file to encode
     * @return the Base64 data URI string
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the file exceeds the size limit or has an invalid format
     */
    public static String encodeToBase64DataUri(File file) throws IOException {
        String validationError = validateImageFile(file);
        if (validationError != null) {
            throw new IllegalArgumentException(I18n.get(validationError));
        }

        String mimeType = detectMimeType(file);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Decodes a Base64 data URI string to a JavaFX Image.
     *
     * @param dataUri the Base64 data URI (e.g., "data:image/png;base64,...")
     * @return the JavaFX Image, or null if decoding fails
     */
    public static Image decodeBase64ToImage(String dataUri) {
        return decodeBase64ToImage(dataUri, 0, 0);
    }

    /**
     * Decodes a Base64 data URI string to a JavaFX Image with specified dimensions.
     *
     * @param dataUri the Base64 data URI
     * @param width   requested width (0 for original)
     * @param height  requested height (0 for original)
     * @return the JavaFX Image, or null if decoding fails
     */
    public static Image decodeBase64ToImage(String dataUri, double width, double height) {
        if (dataUri == null || !dataUri.startsWith("data:image")) {
            return null;
        }

        try {
            // Extract the Base64 part after "base64,"
            int base64Start = dataUri.indexOf("base64,");
            if (base64Start == -1) return null;
            String base64Data = dataUri.substring(base64Start + 7);

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

            if (width > 0 && height > 0) {
                return new Image(bais, width, height, true, true);
            }
            return new Image(bais);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks whether a string is a Base64 data URI.
     *
     * @param url the string to check
     * @return true if it starts with "data:image"
     */
    public static boolean isBase64DataUri(String url) {
        return url != null && url.startsWith("data:image");
    }

    /**
     * Creates a JavaFX Image from a photo URL string, handling both
     * Base64 data URIs and regular URLs/file paths.
     *
     * @param photoUrl the photo URL (Base64 data URI, http://, or file://)
     * @param width    requested width
     * @param height   requested height
     * @return the JavaFX Image, or null if loading fails
     */
    public static Image loadProfileImage(String photoUrl, double width, double height) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }

        try {
            if (isBase64DataUri(photoUrl)) {
                return decodeBase64ToImage(photoUrl, width, height);
            } else {
                // Legacy: URL-based image (file:///... or http://...)
                Image img = new Image(photoUrl, width, height, true, true, true);
                return img.isError() ? null : img;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Detects the MIME type from a file extension.
     */
    private static String detectMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".bmp")) return "image/bmp";
        return "image/png"; // default
    }

    /**
     * Returns the file size in a human-readable format.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
