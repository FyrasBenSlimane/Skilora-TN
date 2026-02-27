package com.skilora.framework.components;

import javafx.scene.control.Label;

/**
 * TLTypography - shadcn/ui Typography (theme-adaptive text variants).
 * Label with .h1, .h2, .h3, .p, .text-sm; optional .text-muted, .font-bold.
 */
public class TLTypography extends Label {

    public enum Variant {
        H1("h1"), H2("h2"), H3("h3"), P("p"), SM("text-sm"), XS("text-xs");

        private final String cssClass;

        Variant(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public TLTypography() {
        this("", Variant.P);
    }

    public TLTypography(String text) {
        this(text, Variant.P);
    }

    public TLTypography(String text, Variant variant) {
        super(text);
        getStyleClass().add(variant.getCssClass());
    }

    private static String variantToClass(Variant v) {
        return v.getCssClass();
    }

    public void setVariant(Variant variant) {
        getStyleClass().removeAll("h1", "h2", "h3", "p", "text-sm", "text-xs");
        getStyleClass().add(variantToClass(variant));
    }

    /** Add or remove .text-muted (secondary text). */
    public void setMuted(boolean muted) {
        if (muted) {
            if (!getStyleClass().contains("text-muted"))
                getStyleClass().add("text-muted");
        } else {
            getStyleClass().remove("text-muted");
        }
    }

    /** Add or remove .font-bold. */
    public void setBold(boolean bold) {
        if (bold) {
            if (!getStyleClass().contains("font-bold"))
                getStyleClass().add("font-bold");
        } else {
            getStyleClass().remove("font-bold");
        }
    }
}
