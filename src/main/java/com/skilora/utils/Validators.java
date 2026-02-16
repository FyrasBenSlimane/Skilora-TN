package com.skilora.utils;

import java.util.regex.Pattern;

/**
 * Validators Utility
 * Static utility methods for input validation.
 */
public class Validators {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASSWORD_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public static boolean required(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean email(String email) {
        if (!required(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates password strength.
     * Requirements: min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char.
     *
     * @return null if valid, or an error message describing what's missing
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!PASSWORD_UPPERCASE.matcher(password).find()) {
            return "Password must contain at least one uppercase letter";
        }
        if (!PASSWORD_LOWERCASE.matcher(password).find()) {
            return "Password must contain at least one lowercase letter";
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            return "Password must contain at least one digit";
        }
        if (!PASSWORD_SPECIAL.matcher(password).find()) {
            return "Password must contain at least one special character";
        }
        return null; // Valid
    }
}
