package com.skilora.framework.themes;

/**
 * shadcn/ui New York - JavaFX Theme Constants
 *
 * 100% alignment with shadcn "New York" design tokens.
 * Color: Zinc/Stone | Typography: Inter, 16px headings / 13px body | Geometry: 6px radius, 1px border
 */
public final class Theme {

    /* ========== COLOR PALETTE (Zinc/Stone) ========== */
    public static final String ZINC_50 = "#fafafa";
    public static final String ZINC_100 = "#f4f4f5";
    public static final String ZINC_200 = "#e4e4e7";
    public static final String ZINC_300 = "#d4d4d8";
    public static final String ZINC_400 = "#a1a1aa";
    public static final String ZINC_500 = "#71717a";
    public static final String ZINC_600 = "#52525b";
    public static final String ZINC_700 = "#3f3f46";
    public static final String ZINC_800 = "#27272a";
    public static final String ZINC_900 = "#18181b";
    public static final String ZINC_950 = "#09090b";

    public static final String BACKGROUND = "#ffffff";
    public static final String FOREGROUND = ZINC_950;
    public static final String PRIMARY = ZINC_900;
    public static final String PRIMARY_FOREGROUND = ZINC_50;
    public static final String PRIMARY_DARK = ZINC_800;
    public static final String PRIMARY_LIGHT = ZINC_800;

    public static final String SECONDARY = ZINC_100;
    public static final String SECONDARY_FOREGROUND = ZINC_900;

    public static final String MUTED = ZINC_100;
    public static final String MUTED_FOREGROUND = ZINC_500;

    public static final String ACCENT = ZINC_100;
    public static final String ACCENT_FOREGROUND = ZINC_900;

    public static final String CARD = "#ffffff";
    public static final String CARD_FOREGROUND = ZINC_950;

    public static final String BORDER = ZINC_200;
    public static final String INPUT = ZINC_200;
    public static final String RING = ZINC_900;

    public static final String POPOVER = "#ffffff";
    public static final String POPOVER_FOREGROUND = ZINC_950;

    public static final String DESTRUCTIVE = "#ef4444";
    public static final String DESTRUCTIVE_FOREGROUND = ZINC_50;

    public static final String SUCCESS = ZINC_900;
    public static final String WARNING = ZINC_600;
    public static final String ERROR = "#ef4444";
    public static final String INFO = ZINC_500;

    public static final String TEXT_PRIMARY = ZINC_950;
    public static final String TEXT_SECONDARY = ZINC_500;
    public static final String TEXT_MUTED = ZINC_400;

    /* Backwards compatibility */
    public static final String SURFACE = CARD;
    public static final String BG_PRIMARY = CARD;
    public static final String BG_SECONDARY = MUTED;
    public static final String BG_MUTED = ZINC_100;
    public static final String DANGER = DESTRUCTIVE;

    /* ========== SPACING (space-x / space-y equivalent) ========== */
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;
    public static final int SPACING_XXL = 48;

    /* ========== GEOMETRY (6px radius, 1px border) ========== */
    public static final int RADIUS_SM = 4;
    public static final int RADIUS_MD = 6;
    public static final int RADIUS_LG = 6;
    public static final int RADIUS_XL = 8;
    public static final int RADIUS_FULL = 9999;

    /* ========== TYPOGRAPHY (Inter / System; 16px headings, 13px body) ========== */
    public static final String FONT_FAMILY = "Inter, System, Segoe UI, sans-serif";
    public static final int FONT_SIZE_XS = 12;
    public static final int FONT_SIZE_SM = 13;
    public static final int FONT_SIZE_MD = 16;
    public static final int FONT_SIZE_LG = 18;
    public static final int FONT_SIZE_XL = 20;
    public static final int FONT_SIZE_2XL = 24;
    public static final int FONT_SIZE_3XL = 30;

    /* ========== SHADOW (dropshadow three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2) ========== */
    public static final String SHADOW_SM = "dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2)";
    public static final String SHADOW_MD = "dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3)";
    public static final String SHADOW_LG = "dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4)";

    private Theme() {}
}
