package com.skilora.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Centralised SVG icon constants and factory helpers.
 * All paths are based on a 24 × 24 viewBox (Lucide-style).
 * <p>
 * Usage:
 * <pre>
 *   Label l = new Label("Saved");
 *   l.setGraphic(SvgIcons.icon(SvgIcons.HEART, 16));
 * </pre>
 */
public final class SvgIcons {

    private SvgIcons() {}

    // ── Navigation & Actions ──────────────────────────────────────────
    public static final String ARROW_LEFT     = "M19 12H5 M12 19l-7-7 7-7";
    public static final String ARROW_RIGHT    = "M5 12h14 M12 5l7 7-7 7";
    public static final String CHECK          = "M20 6L9 17l-5-5";
    public static final String CHECK_CIRCLE   = "M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3";
    public static final String X_CIRCLE       = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M15 9l-6 6 M9 9l6 6";
    public static final String REFRESH        = "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10 M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
    public static final String SEND           = "M22 2L11 13 M22 2l-7 20-4-9-9-4z";
    public static final String TRASH          = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2 M10 11v6 M14 11v6";
    public static final String SAVE           = "M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z M17 21v-8H7v8 M7 3v5h8";

    // ── Search & Filter ───────────────────────────────────────────────
    public static final String SEARCH         = "M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16z M21 21l-4.35-4.35";
    public static final String FILTER         = "M22 3H2l8 9.46V19l4 2v-8.54L22 3z";

    // ── Communication & Social ────────────────────────────────────────
    public static final String MESSAGE_CIRCLE = "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z";
    public static final String BELL           = "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0";
    public static final String MAIL           = "M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z M22 6l-10 7L2 6";
    public static final String PHONE          = "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.362 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.338 1.85.573 2.81.7A2 2 0 0 1 22 16.92z";
    public static final String GLOBE          = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M2 12h20 M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z";

    // ── Objects ───────────────────────────────────────────────────────
    public static final String BRIEFCASE      = "M20 7H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2";
    public static final String MAP_PIN        = "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M12 7a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String CLOCK          = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 6v6l4 2";
    public static final String CALENDAR       = "M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M16 2v4 M8 2v4 M3 10h18";
    public static final String DOLLAR_SIGN    = "M12 1v22 M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6";
    public static final String BOOKMARK       = "M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z";
    public static final String HEART          = "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z";

    // ── Status & Feedback ─────────────────────────────────────────────
    public static final String ALERT_TRIANGLE = "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01";
    public static final String SHIELD         = "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z";
    public static final String LOCK           = "M19 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z M7 11V7a5 5 0 0 1 10 0v4";

    // ── Content & Media ───────────────────────────────────────────────
    public static final String FILE_TEXT      = "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8";
    public static final String EYE           = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String BOOK_OPEN      = "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z";
    public static final String VIDEO          = "M23 7l-7 5 7 5V7z M14 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z";
    public static final String LIGHTBULB      = "M9 18h6 M10 22h4 M15.09 14c.18-.98.65-1.74 1.41-2.5A7 7 0 1 0 7.5 11.5c.76.76 1.23 1.52 1.41 2.5";

    // ── Learning & Education ──────────────────────────────────────────
    public static final String GRADUATION_CAP = "M22 10l-10-5-10 5 10 5 10-5z M6 12v5c3 3 9 3 12 0v-5";
    public static final String TIMER          = "M12 5v7l4 4 M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M10 2h4";

    // ── Trends & Analytics ───────────────────────────────────────────
    public static final String TRENDING_UP    = "M23 6l-9.5 9.5-5-5L1 18 M17 6h6v6";
    public static final String TRENDING_DOWN  = "M23 18l-9.5-9.5-5 5L1 6 M17 18h6v-6";
    public static final String BAR_CHART      = "M18 20V10 M12 20V4 M6 20v-6";
    public static final String ACTIVITY       = "M22 12h-4l-3 9L9 3l-3 9H2";
    public static final String PIE_CHART      = "M21.21 15.89A10 10 0 1 1 8 2.83 M22 12A10 10 0 0 0 12 2v10z";

    // ── People ────────────────────────────────────────────────────────
    public static final String USER           = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
    public static final String USER_PLUS      = "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M8.5 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M20 8v6 M23 11h-6";
    public static final String USERS          = "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";

    // ── System & Security ─────────────────────────────────────────────
    public static final String KEY            = "M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.78 7.78 5.5 5.5 0 0 1 7.78-7.78zM15.5 7.5l3 3L22 7l-3-3";
    public static final String DATABASE       = "M12 2C6.48 2 2 4.02 2 6.5v11C2 19.98 6.48 22 12 22s10-2.02 10-4.5v-11C22 4.02 17.52 2 12 2z M2 6.5C2 8.98 6.48 11 12 11s10-2.02 10-4.5 M2 12c0 2.48 4.48 4.5 10 4.5s10-2.02 10-4.5";
    public static final String CLIPBOARD      = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M15 2H9a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1z";
    public static final String TOOL           = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z";
    public static final String LAYERS         = "M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5";
    public static final String LOG_IN         = "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4 M10 17l5-5-5-5 M15 12H3";
    public static final String SHIELD_CHECK   = "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4";
    public static final String SERVER         = "M2 4h20v6H2z M2 14h20v6H2z M6 7h.01 M6 17h.01";

    // ── Misc ──────────────────────────────────────────────────────────
    public static final String STAR           = "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
    public static final String SPARKLES       = "M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3z M19 13l.75 2.25L22 16l-2.25.75L19 19l-.75-2.25L16 16l2.25-.75L19 13z";
    public static final String SUN            = "M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 M18.36 18.36l1.42 1.42 M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42";
    public static final String WIFI           = "M5 12.55a11 11 0 0 1 14.08 0 M1.42 9a16 16 0 0 1 21.16 0 M8.53 16.11a6 6 0 0 1 6.95 0 M12 20h.01";
    public static final String SATELLITE      = "M13 7l2 2 M10.1 13.9l-6.6 6.6 M7.5 10.5l-4.21 4.21a1 1 0 0 0 0 1.41l2.59 2.59a1 1 0 0 0 1.41 0l4.21-4.21 M13.5 7.5l4.21-4.21a1 1 0 0 1 1.41 0l2.59 2.59a1 1 0 0 1 0 1.41L17.5 11.5";

    // ── Eye toggle (used in TLPasswordField) ──────────────────────────
    public static final String EYE_OFF        = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94 M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 M1 1l22 22";

    // ─────────────────────────────────────────────────────────────────
    //  Factory helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates a stroke-based SVG icon (Lucide style) at 16 px,
     * coloured with the current foreground token.
     */
    public static SVGPath icon(String pathData) {
        return icon(pathData, 16, "-fx-foreground");
    }

    /** Creates a stroke-based SVG icon at the given logical size. */
    public static SVGPath icon(String pathData, double size) {
        return icon(pathData, size, "-fx-foreground");
    }

    /**
     * Creates a stroke-based SVG icon.
     *
     * @param pathData SVG path d-attribute content
     * @param size     desired icon size in px (icons are 24×24 native)
     * @param cssColor a CSS colour value or token name (e.g. "-fx-foreground", "#ff0000", "-fx-muted-foreground")
     */
    public static SVGPath icon(String pathData, double size, String cssColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);

        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);

        svg.setStyle(
            "-fx-fill: transparent; " +
            "-fx-stroke: " + cssColor + "; " +
            "-fx-stroke-width: 2;"
        );
        return svg;
    }

    /**
     * Creates a <b>filled</b> SVG icon (no stroke).
     * Useful for solid icons like hearts, stars, checkmarks.
     */
    public static SVGPath filledIcon(String pathData, double size, String cssColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        svg.setStyle("-fx-fill: " + cssColor + "; -fx-stroke: transparent;");
        return svg;
    }

    /**
     * Returns an HBox containing [icon | text] aligned CENTER_LEFT with 6 px gap.
     * Drop-in replacement for patterns like  "📍 Tunis"  →  [pin icon] Tunis.
     */
    public static HBox withText(String pathData, String text) {
        return withText(pathData, text, 14, "-fx-foreground");
    }

    public static HBox withText(String pathData, String text, double iconSize, String cssColor) {
        SVGPath svg = icon(pathData, iconSize, cssColor);
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + cssColor + ";");
        HBox box = new HBox(6, svg, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
