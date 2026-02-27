package com.skilora.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application Configuration
 * 
 * Stores application-wide settings and constants.
 * Loads additional properties from config file if available.
 * 
 * CONSTANTS:
 * - Application info (name, version)
 * - File upload settings
 * - Date formats
 * - Default values
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    // Application Info
    public static final String APP_NAME = "Skilora Tunisia";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DESCRIPTION = "Professional Profile & Matching Platform";
    
    // File Upload Settings
    public static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024; // 5 MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png", "gif"};
    public static final String[] ALLOWED_DOCUMENT_TYPES = {"pdf", "doc", "docx"};
    
    // Date Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";
    
    // Matching Algorithm Settings
    public static final double SKILLS_WEIGHT = 0.40;  // 40%
    public static final double EXPERIENCE_WEIGHT = 0.30;  // 30%
    public static final double LANGUAGE_WEIGHT = 0.20;  // 20%
    public static final double LOCATION_WEIGHT = 0.10;  // 10%
    
    // Profile Completion Thresholds
    public static final int MIN_PROFILE_COMPLETION = 60;  // Minimum for job matching
    public static final int RECOMMENDED_SKILLS = 5;
    public static final int RECOMMENDED_EXPERIENCES = 2;
    
    // UI Settings
    public static final int ITEMS_PER_PAGE = 10;
    public static final int MAX_SEARCH_RESULTS = 50;
    
    // Properties from config file (optional)
    private static Properties properties;
    
    // Static initialization block
    static {
        loadProperties();
    }
    
    /**
     * Loads properties from config file if available.
     * File should be located at: /config/application.properties
     */
    private static void loadProperties() {
        properties = new Properties();
        
        try (InputStream input = AppConfig.class.getResourceAsStream("/config/application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.debug("Loaded application properties");
            } else {
                logger.debug("No application.properties found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Error loading properties: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets a property value from config file.
     * 
     * @param key Property key
     * @return Property value or null if not found
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets a property value with default fallback.
     * 
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Gets application name and version string.
     * 
     * @return Formatted app info
     */
    public static String getAppInfo() {
        return APP_NAME + " v" + APP_VERSION;
    }
    
    /**
     * Checks if file size is within upload limit.
     * 
     * @param fileSize File size in bytes
     * @return true if size is acceptable
     */
    public static boolean isFileSizeAcceptable(long fileSize) {
        return fileSize <= MAX_UPLOAD_SIZE;
    }
    
    /**
     * Checks if file type is allowed for images.
     * 
     * @param fileExtension File extension (without dot)
     * @return true if type is allowed
     */
    public static boolean isImageTypeAllowed(String fileExtension) {
        if (fileExtension == null) return false;
        String ext = fileExtension.toLowerCase();
        for (String allowed : ALLOWED_IMAGE_TYPES) {
            if (allowed.equals(ext)) return true;
        }
        return false;
    }
    
    /**
     * Checks if file type is allowed for documents.
     * 
     * @param fileExtension File extension (without dot)
     * @return true if type is allowed
     */
    public static boolean isDocumentTypeAllowed(String fileExtension) {
        if (fileExtension == null) return false;
        String ext = fileExtension.toLowerCase();
        for (String allowed : ALLOWED_DOCUMENT_TYPES) {
            if (allowed.equals(ext)) return true;
        }
        return false;
    }
}
