package com.skilora.finance.utils;

import java.util.regex.Pattern;

/**
 * Validation Helper - Strict rules for all fields
 */
public class ValidationHelper {

    // REGEX PATTERNS
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ\\s'-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$");

    // Relaxed IBAN Pattern for testing: Min 10 chars (originally 15)
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{6,30}$");

    /**
     * Validate name (letters, spaces, hyphens, apostrophes only)
     */
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Name is required!";
        }
        if (name.trim().length() < 2) {
            return "Name must be at least 2 characters!";
        }
        if (!NAME_PATTERN.matcher(name.trim()).matches()) {
            return "Name must contain only letters!";
        }
        return null; // Valid
    }

    /**
     * Validate email format
     */
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required!";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format!";
        }
        return null; // Valid
    }

    /**
     * Validate phone number (digits, spaces, +, - only)
     */
    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null; // Phone is optional
        }
        String cleaned = phone.replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+"))
            cleaned = cleaned.substring(1);

        if (!cleaned.matches("^[0-9]+$")) {
            return "Phone must contain only digits!";
        }
        if (cleaned.length() < 8 || cleaned.length() > 15) {
            return "Phone must be 8-15 digits!";
        }
        return null; // Valid
    }

    /**
     * Validate IBAN (10-34 alphanumeric characters)
     */
    public static String validateIBAN(String iban) {
        if (iban == null || iban.trim().isEmpty()) {
            return "IBAN is required!";
        }
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();

        if (cleaned.length() < 10 || cleaned.length() > 34) {
            return "IBAN must be 10-34 characters!";
        }
        if (!IBAN_PATTERN.matcher(cleaned).matches()) {
            return "Invalid IBAN format! (e.g. TN5914207207100707129648)";
        }
        return null; // Valid
    }

    /**
     * Validate SWIFT/BIC code (8 or 11 characters)
     */
    /**
     * Validate SWIFT/BIC code (Relaxed: 8-20 characters)
     */
    public static String validateSWIFT(String swift) {
        if (swift == null || swift.trim().isEmpty()) {
            return null; // SWIFT is optional
        }
        String cleaned = swift.trim().toUpperCase();

        if (cleaned.length() < 8 || cleaned.length() > 20) {
            return "SWIFT must be 8-20 characters!";
        }
        if (!cleaned.matches("^[A-Z0-9]+$")) {
            return "SWIFT must contain only letters and numbers!";
        }
        return null; // Valid
    }

    /**
     * Validate positive number
     */
    public static String validatePositiveNumber(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required!";
        }
        try {
            double num = Double.parseDouble(value.trim());
            if (num < 0) {
                return fieldName + " must be positive!";
            }
            if (num == 0) {
                return fieldName + " must be greater than 0!";
            }
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid number!";
        }
        return null; // Valid
    }

    /**
     * Validate non-negative number (can be 0)
     */
    public static String validateNonNegativeNumber(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return null; // Optional
        }
        try {
            double num = Double.parseDouble(value.trim());
            if (num < 0) {
                return fieldName + " cannot be negative!";
            }
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid number!";
        }
        return null; // Valid
    }

    /**
     * Validate integer
     */
    public static String validateInteger(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required!";
        }
        try {
            Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid integer!";
        }
        return null; // Valid
    }

    /**
     * Validate positive integer
     */
    public static String validatePositiveInteger(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required!";
        }
        try {
            int num = Integer.parseInt(value.trim());
            if (num <= 0) {
                return fieldName + " must be greater than 0!";
            }
        } catch (NumberFormatException e) {
            return fieldName + " must be a valid integer!";
        }
        return null; // Valid
    }

    /**
     * Validate text (not empty)
     */
    public static String validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return fieldName + " is required!";
        }
        return null; // Valid
    }

    /**
     * Validate minimum length
     */
    public static String validateMinLength(String value, int minLength, String fieldName) {
        if (value == null || value.trim().length() < minLength) {
            return fieldName + " must be at least " + minLength + " characters!";
        }
        return null; // Valid
    }

    /**
     * Clean and format IBAN
     */
    public static String formatIBAN(String iban) {
        if (iban == null)
            return "";
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();
        // Add spaces every 4 characters for readability
        return cleaned.replaceAll("(.{4})", "$1 ").trim();
    }

    /**
     * Clean phone number
     */
    public static String formatPhone(String phone) {
        if (phone == null)
            return "";
        return phone.trim();
    }
}
