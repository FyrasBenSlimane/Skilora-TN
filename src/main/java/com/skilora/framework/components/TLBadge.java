package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * TLBadge - shadcn/ui Badge (CSS-only, theme-adaptive)
 *
 * Uses .badge + variant class; colors from theme (dark/light).
 */
public class TLBadge extends StackPane {

    private static final String STYLESHEET =
            TLBadge.class.getResource("/com/skilora/framework/styles/tl-badge.css").toExternalForm();

    public enum Variant {
        DEFAULT, SECONDARY, OUTLINE, DESTRUCTIVE, SUCCESS, INFO
    }

    private final Label label;
    private Variant currentVariant;

    /** No-arg constructor for FXML loading (e.g. PublicProfileView). Set text/variant via setters. */
    public TLBadge() {
        this("", Variant.DEFAULT);
    }

    public TLBadge(String text, Variant variant) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("badge");
        this.currentVariant = variant;
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        label = new Label(text);
        getChildren().add(label);
    }

    /** For FXML: readable property so attribute variant="SUCCESS" works. */
    public Variant getVariant() {
        return currentVariant;
    }

    /** For FXML: set variant by name (e.g. variant="SUCCESS"). */
    public void setVariant(String variantName) {
        if (variantName == null || variantName.isBlank()) return;
        try {
            setVariant(Variant.valueOf(variantName.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            setVariant(Variant.DEFAULT);
        }
    }

    public String getText() {
        return label != null ? label.getText() : "";
    }

    public void setText(String text) {
        if (label != null) label.setText(text != null ? text : "");
    }

    public void setVariant(Variant variant) {
        if (currentVariant == variant) return;
        // Remove old variant class
        if (currentVariant != Variant.DEFAULT) {
            getStyleClass().remove(variantToClass(currentVariant));
        }
        // Add new variant class
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        currentVariant = variant;
    }

    private static String variantToClass(Variant v) {
        return switch (v) {
            case SECONDARY -> "badge-secondary";
            case OUTLINE -> "badge-outline";
            case DESTRUCTIVE -> "badge-destructive";
            case SUCCESS -> "badge-success";
            case INFO -> "badge-info";
            default -> "";
        };
    }
}
