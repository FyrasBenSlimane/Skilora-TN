package com.skilora.utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.binding.StringBinding;

import java.text.MessageFormat;
import java.util.*;

/**
 * I18n - Internationalization manager for Skilora.
 *
 * Supports French (fr), English (en), Arabic (ar).
 * Uses ResourceBundle for message resolution.
 *
 * Usage:
 *   I18n.get("nav.dashboard")             → "Tableau de Bord" / "Dashboard" / "لوحة التحكم"
 *   I18n.get("greeting.hello", "Ahmed")   → "Bonjour, Ahmed"
 *   I18n.setLocale(Locale.ENGLISH)        → switches all bindings automatically
 */
public final class I18n {

    private static final String BUNDLE_BASE = "com.skilora.i18n.messages";

    /** Supported locales */
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_AR = new Locale("ar");

    /** Observable locale property – listeners react to language changes */
    private static final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>(LOCALE_FR);

    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, LOCALE_FR);

    private I18n() {}

    // ── Locale Management ───────────────────────────────────────────

    public static Locale getLocale() {
        return currentLocale.get();
    }

    public static void setLocale(Locale locale) {
        currentLocale.set(locale);
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
    }

    public static ObjectProperty<Locale> localeProperty() {
        return currentLocale;
    }

    /**
     * Set locale from display name (Français / English / العربية).
     */
    public static void setLocaleFromDisplayName(String displayName) {
        if (displayName == null) return;
        switch (displayName) {
            case "Français":
                setLocale(LOCALE_FR);
                break;
            case "English":
                setLocale(LOCALE_EN);
                break;
            case "العربية":
                setLocale(LOCALE_AR);
                break;
            default:
                setLocale(LOCALE_FR);
                break;
        }
    }

    /**
     * Get the display name for the current locale.
     */
    public static String getDisplayName() {
        Locale loc = getLocale();
        if (LOCALE_AR.getLanguage().equals(loc.getLanguage())) return "العربية";
        if (LOCALE_EN.getLanguage().equals(loc.getLanguage())) return "English";
        return "Français";
    }

    // ── Message Resolution ──────────────────────────────────────────

    /**
     * Get a translated string for the given key.
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    /**
     * Get a translated string with MessageFormat arguments.
     */
    public static String get(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    // ── JavaFX Bindings (auto-update on locale change) ──────────────

    /**
     * Create a StringBinding that auto-updates when locale changes.
     */
    public static StringBinding binding(String key) {
        return new StringBinding() {
            { bind(currentLocale); }
            @Override
            protected String computeValue() {
                return I18n.get(key);
            }
        };
    }

    /**
     * Create a StringBinding with arguments that auto-updates when locale changes.
     */
    public static StringBinding binding(String key, Object... args) {
        return new StringBinding() {
            { bind(currentLocale); }
            @Override
            protected String computeValue() {
                return I18n.get(key, args);
            }
        };
    }

    // ── Utilities ───────────────────────────────────────────────────

    /**
     * Get all supported locale display names.
     */
    public static List<String> getSupportedLanguages() {
        return List.of("Français", "English", "العربية");
    }

    /**
     * Check if current locale is RTL (Arabic).
     */
    public static boolean isRTL() {
        return LOCALE_AR.getLanguage().equals(getLocale().getLanguage());
    }
}
