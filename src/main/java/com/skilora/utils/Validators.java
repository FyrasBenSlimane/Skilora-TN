package com.skilora.utils;

/**
 * Static validation helpers for forms (required, length, email, password).
 */
public final class Validators {

    private Validators() {}

    /** Returns true if the value is not null and not blank. */
    public static boolean required(String value) {
        return value != null && !value.isBlank();
    }

    /** Returns true if value is not null and length >= min. */
    public static boolean minLength(String value, int min) {
        return value != null && value.length() >= min;
    }

    /** Returns true if value is null or length <= max. */
    public static boolean maxLength(String value, int max) {
        return value == null || value.length() <= max;
    }

    /** Simple email format check. */
    public static boolean email(String value) {
        if (value == null || value.isBlank()) return false;
        int at = value.indexOf('@');
        int dot = value.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < value.length() - 1;
    }

    /**
     * Validates password strength. Returns null if valid, or an error message if invalid.
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Le mot de passe est requis.";
        }
        if (password.length() < 8) {
            return "Le mot de passe doit contenir au moins 8 caractères.";
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter) {
            return "Le mot de passe doit contenir au moins une lettre.";
        }
        if (!hasDigit) {
            return "Le mot de passe doit contenir au moins un chiffre.";
        }
        return null;
    }
}
