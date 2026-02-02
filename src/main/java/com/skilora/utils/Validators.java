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

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern FULLNAME_PATTERN = Pattern.compile("^[\\p{L} .'-]+$");
    private static final Pattern PHONE_DIGITS_PATTERN = Pattern.compile("^[0-9]+$");

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
            return I18n.get("validation.password.required");
        }
        if (password.length() < 8) {
            return I18n.get("validation.password.min_length");
        }
        if (!PASSWORD_UPPERCASE.matcher(password).find()) {
            return I18n.get("validation.password.uppercase");
        }
        if (!PASSWORD_LOWERCASE.matcher(password).find()) {
            return I18n.get("validation.password.lowercase");
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            return I18n.get("validation.password.digit");
        }
        if (!PASSWORD_SPECIAL.matcher(password).find()) {
            return I18n.get("validation.password.special");
        }
        return null; // Valid
    }

    /**
     * Validates username format.
     * Requirements: 3-30 chars, letters/digits/underscores only.
     *
     * @return null if valid, or an error message describing what's wrong
     */
    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return I18n.get("validation.username.required");
        }
        if (username.length() < 3) {
            return I18n.get("validation.username.min_length");
        }
        if (username.length() > 30) {
            return I18n.get("validation.username.max_length");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return I18n.get("validation.username.format");
        }
        return null;
    }

    /**
     * Validates email and returns error message.
     *
     * @return null if valid, or an error message
     */
    public static String validateEmail(String emailAddr) {
        if (emailAddr == null || emailAddr.trim().isEmpty()) {
            return I18n.get("validation.email.required");
        }
        if (!EMAIL_PATTERN.matcher(emailAddr.trim()).matches()) {
            return I18n.get("validation.email.invalid");
        }
        return null;
    }

    /**
     * Validates full name.
     * Requirements: 2-100 chars, letters/spaces/hyphens/apostrophes only.
     *
     * @return null if valid, or an error message
     */
    public static String validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return I18n.get("validation.fullname.required");
        }
        if (fullName.trim().length() < 2) {
            return I18n.get("validation.fullname.min_length");
        }
        if (fullName.trim().length() > 100) {
            return I18n.get("validation.fullname.max_length");
        }
        if (!FULLNAME_PATTERN.matcher(fullName.trim()).matches()) {
            return I18n.get("validation.fullname.format");
        }
        return null;
    }

    /**
     * Validates phone number.
     * Requirements: digits only (after stripping spaces/parens/dashes/plus), 8-15 digits.
     * Phone is optional — returns null for empty input.
     *
     * @return null if valid or empty, or an error message
     */
    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null; // Phone is optional
        }
        String digits = phone.replaceAll("[\\s()+-]", "");
        if (!PHONE_DIGITS_PATTERN.matcher(digits).matches()) {
            return I18n.get("validation.phone.format");
        }
        if (digits.length() < 8 || digits.length() > 15) {
            return I18n.get("validation.phone.length");
        }
        return null;
    }
}
