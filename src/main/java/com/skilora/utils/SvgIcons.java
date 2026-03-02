package com.skilora.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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

    // ── Navigation & Actions ─────────────────────────────── (Lucide v0.574.0)
    public static final String ARROW_LEFT     = "M12 19l-7-7 7-7 M19 12H5";
    public static final String ARROW_RIGHT    = "M5 12h14 M12 5l7 7-7 7";
    public static final String CHECK          = "M20 6 9 17l-5-5";
    public static final String CHECK_DOUBLE   = "M18 6 7 17l-5-5 M22 10l-7.5 7.5L13 16";
    public static final String CHECK_CIRCLE   = "M9 12l2 2 4-4 M2,12a10,10 0 1,0 20,0a10,10 0 1,0 -20,0";
    public static final String X_CIRCLE       = "M15 9l-6 6 M9 9l6 6 M2,12a10,10 0 1,0 20,0a10,10 0 1,0 -20,0";
    public static final String REFRESH        = "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8 M21 3v5h-5 M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16 M8 16H3v5";
    public static final String SEND           = "M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z M21.854 2.147l-10.94 10.939";
    public static final String EDIT           = "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
    public static final String TRASH          = "M10 11v6 M14 11v6 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
    public static final String SAVE           = "M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7 M7 3v4a1 1 0 0 0 1 1h7";

    // ── Search & Filter ───────────────────────────────────────────────
    public static final String SEARCH         = "M21 21l-4.34-4.34 M3,11a8,8 0 1,0 16,0a8,8 0 1,0 -16,0";
    public static final String FILTER         = "M10 20a1 1 0 0 0 .553.895l2 1A1 1 0 0 0 14 21v-7a2 2 0 0 1 .517-1.341L21.74 4.67A1 1 0 0 0 21 3H3a1 1 0 0 0-.742 1.67l7.225 7.989A2 2 0 0 1 10 14z";

    // ── Communication & Social ────────────────────────────────────────
    public static final String MESSAGE_CIRCLE = "M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719";
    public static final String BELL           = "M10.268 21a2 2 0 0 0 3.464 0 M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326";
    public static final String MAIL           = "M22 7l-8.991 5.727a2 2 0 0 1-2.009 0L2 7 M4,4h16a2,2 0 0 1 2,2v12a2,2 0 0 1 -2,2h-16a2,2 0 0 1 -2,-2v-12a2,2 0 0 1 2,-2z";
    public static final String PHONE          = "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384";
    public static final String GLOBE          = "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20 M2 12h20 M2,12a10,10 0 1,0 20,0a10,10 0 1,0 -20,0";

    // ── Objects ───────────────────────────────────────────────────────
    public static final String BRIEFCASE      = "M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16 M4,6h16a2,2 0 0 1 2,2v10a2,2 0 0 1 -2,2h-16a2,2 0 0 1 -2,-2v-10a2,2 0 0 1 2,-2z";
    public static final String MAP_PIN        = "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0 M9,10a3,3 0 1,0 6,0a3,3 0 1,0 -6,0";
    public static final String CLOCK          = "M12 6v6l4 2 M2,12a10,10 0 1,0 20,0a10,10 0 1,0 -20,0";
    public static final String CALENDAR       = "M8 2v4 M16 2v4 M3 10h18 M5,4h14a2,2 0 0 1 2,2v14a2,2 0 0 1 -2,2h-14a2,2 0 0 1 -2,-2v-14a2,2 0 0 1 2,-2z";
    public static final String DOLLAR_SIGN    = "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6 M12 2L12 22";
    public static final String BOOKMARK       = "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z";
    public static final String HEART          = "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5";

    // ── Status & Feedback ─────────────────────────────────────────────
    public static final String ALERT_TRIANGLE = "M21.73 18l-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3 M12 9v4 M12 17h.01";
    public static final String FLAG           = "M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2v13 M4 22v-7";
    public static final String SHIELD         = "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z";
    public static final String LOCK           = "M7 11V7a5 5 0 0 1 10 0v4 M5,11h14a2,2 0 0 1 2,2v7a2,2 0 0 1 -2,2h-14a2,2 0 0 1 -2,-2v-7a2,2 0 0 1 2,-2z";

    // ── Content & Media ───────────────────────────────────────────────
    public static final String FILE_TEXT      = "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z M14 2v5a1 1 0 0 0 1 1h5 M10 9H8 M16 13H8 M16 17H8";
    public static final String EYE           = "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0 M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0";
    public static final String BOOK_OPEN      = "M12 7v14 M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z";
    public static final String VIDEO          = "M16 13l5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5 M4,6h10a2,2 0 0 1 2,2v8a2,2 0 0 1 -2,2h-10a2,2 0 0 1 -2,-2v-8a2,2 0 0 1 2,-2z";
    public static final String LIGHTBULB      = "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5 M9 18h6 M10 22h4";

    // ── Learning & Education ──────────────────────────────────────────
    public static final String GRADUATION_CAP = "M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z M22 10v6 M6 12.5V16a6 3 0 0 0 12 0v-3.5";
    public static final String TIMER          = "M4,14a8,8 0 1,0 16,0a8,8 0 1,0 -16,0 M10 2L14 2 M12 14L15 11";

    // ── Trends & Analytics ───────────────────────────────────────────
    public static final String TRENDING_UP    = "M16 7h6v6 M22 7l-8.5 8.5-5-5L2 17";
    public static final String TRENDING_DOWN  = "M16 17h6v-6 M22 17l-8.5-8.5-5 5L2 7";
    public static final String BAR_CHART      = "M3 3v16a2 2 0 0 0 2 2h16 M7 16h8 M7 11h12 M7 6h3";
    public static final String ACTIVITY       = "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2";
    public static final String PIE_CHART      = "M21 12c.552 0 1.005-.449.95-.998a10 10 0 0 0-8.953-8.951c-.55-.055-.998.398-.998.95v8a1 1 0 0 0 1 1z M21.21 15.89A10 10 0 1 1 8 2.83";

    // ── People ────────────────────────────────────────────────────────
    public static final String USER           = "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M8,7a4,4 0 1,0 8,0a4,4 0 1,0 -8,0";
    public static final String USER_PLUS      = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M5,7a4,4 0 1,0 8,0a4,4 0 1,0 -8,0 M19 8L19 14 M22 11L16 11";
    public static final String USERS          = "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3.128a4 4 0 0 1 0 7.744 M22 21v-2a4 4 0 0 0-3-3.87 M5,7a4,4 0 1,0 8,0a4,4 0 1,0 -8,0";

    // ── System & Security ─────────────────────────────────────────────
    public static final String KEY            = "M15.5 7.5l2.3 2.3a1 1 0 0 0 1.4 0l2.1-2.1a1 1 0 0 0 0-1.4L19 4 M21 2l-9.6 9.6 M2.0,15.5a5.5,5.5 0 1,0 11.0,0a5.5,5.5 0 1,0 -11.0,0";
    public static final String DATABASE       = "M3 5V19A9 3 0 0 0 21 19V5 M3 12A9 3 0 0 0 21 12 M3,5a9,3 0 1,0 18,0a9,3 0 1,0 -18,0";
    public static final String CLIPBOARD      = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M9,2h6a1,1 0 0 1 1,1v2a1,1 0 0 1 -1,1h-6a1,1 0 0 1 -1,-1v-2a1,1 0 0 1 1,-1z";
    public static final String TOOL           = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.106-3.105c.32-.322.863-.22.983.218a6 6 0 0 1-8.259 7.057l-7.91 7.91a1 1 0 0 1-2.999-3l7.91-7.91a6 6 0 0 1 7.057-8.259c.438.12.54.662.219.984z";
    public static final String LAYERS         = "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12 M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17";
    public static final String LOG_IN         = "M10 17l5-5-5-5 M15 12H3 M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4";
    public static final String SHIELD_CHECK   = "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z M9 12l2 2 4-4";
    public static final String SERVER         = "M4,2h16a2,2 0 0 1 2,2v4a2,2 0 0 1 -2,2h-16a2,2 0 0 1 -2,-2v-4a2,2 0 0 1 2,-2z M4,14h16a2,2 0 0 1 2,2v4a2,2 0 0 1 -2,2h-16a2,2 0 0 1 -2,-2v-4a2,2 0 0 1 2,-2z M6 6L6.01 6 M6 18L6.01 18";

    // ── Misc ──────────────────────────────────────────────────────────
    public static final String STAR           = "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z";
    public static final String SPARKLES       = "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z M20 2v4 M22 4h-4 M2,20a2,2 0 1,0 4,0a2,2 0 1,0 -4,0";
    public static final String SUN            = "M12 2v2 M12 20v2 M4.93 4.93l1.41 1.41 M17.66 17.66l1.41 1.41 M2 12h2 M20 12h2 M6.34 17.66l-1.41 1.41 M19.07 4.93l-1.41 1.41 M8,12a4,4 0 1,0 8,0a4,4 0 1,0 -8,0";
    public static final String WIFI           = "M12 20h.01 M2 8.82a15 15 0 0 1 20 0 M5 12.859a10 10 0 0 1 14 0 M8.5 16.429a5 5 0 0 1 7 0";
    public static final String SATELLITE      = "M13.5 6.5l-3.148-3.148a1.205 1.205 0 0 0-1.704 0L6.352 5.648a1.205 1.205 0 0 0 0 1.704L9.5 10.5 M16.5 7.5 19 5 M17.5 10.5l3.148 3.148a1.205 1.205 0 0 1 0 1.704l-2.296 2.296a1.205 1.205 0 0 1-1.704 0L13.5 14.5 M9 21a6 6 0 0 0-6-6 M9.352 10.648a1.205 1.205 0 0 0 0 1.704l2.296 2.296a1.205 1.205 0 0 0 1.704 0l4.296-4.296a1.205 1.205 0 0 0 0-1.704l-2.296-2.296a1.205 1.205 0 0 0-1.704 0z";

    // ── Eye toggle (used in TLPasswordField) ──────────────────────────
    public static final String EYE_OFF        = "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49 M14.084 14.158a3 3 0 0 1-4.242-4.242 M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143 M2 2l20 20";

    // ── UI Chrome & Layout ────────────────────────────────────────────
    public static final String MENU              = "M4 5h16 M4 12h16 M4 19h16";
    public static final String LAYOUT_DASHBOARD  = "M4,3h5a1,1 0 0 1 1,1v7a1,1 0 0 1 -1,1h-5a1,1 0 0 1 -1,-1v-7a1,1 0 0 1 1,-1z M15,3h5a1,1 0 0 1 1,1v3a1,1 0 0 1 -1,1h-5a1,1 0 0 1 -1,-1v-3a1,1 0 0 1 1,-1z M15,12h5a1,1 0 0 1 1,1v7a1,1 0 0 1 -1,1h-5a1,1 0 0 1 -1,-1v-7a1,1 0 0 1 1,-1z M4,16h5a1,1 0 0 1 1,1v3a1,1 0 0 1 -1,1h-5a1,1 0 0 1 -1,-1v-3a1,1 0 0 1 1,-1z";
    public static final String PLUS              = "M5 12h14 M12 5v14";
    public static final String MOON              = "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401";
    public static final String NEWSPAPER         = "M15 18h-5 M18 14h-8 M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-4 0v-9a2 2 0 0 1 2-2h2 M11,6h6a1,1 0 0 1 1,1v2a1,1 0 0 1 -1,1h-6a1,1 0 0 1 -1,-1v-2a1,1 0 0 1 1,-1z";
    public static final String ELLIPSIS          = "M11,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0 M18,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0 M4,12a1,1 0 1,0 2,0a1,1 0 1,0 -2,0";
    public static final String SHARE             = "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8 M16 6l-4-4-4 4 M12 2v13";
    public static final String COLUMNS_3         = "M9 3v18 M15 3v18 M5,3h14a2,2 0 0 1 2,2v14a2,2 0 0 1 -2,2h-14a2,2 0 0 1 -2,-2v-14a2,2 0 0 1 2,-2z";
    public static final String SCAN_FACE         = "M3 7V5a2 2 0 0 1 2-2h2 M17 3h2a2 2 0 0 1 2 2v2 M21 17v2a2 2 0 0 1-2 2h-2 M7 21H5a2 2 0 0 1-2-2v-2 M8 14s1.5 2 4 2 4-2 4-2 M9 9h.01 M15 9h.01";
    public static final String MESSAGE_SQUARE    = "M22 17a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 21.286V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2z";

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
