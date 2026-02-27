package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * TLBadge - shadcn/ui Badge (CSS-only, theme-adaptive)
 *
 * Uses .badge + variant class; colors from theme (dark/light).
 */
public class TLBadge extends StackPane {

    public enum Variant {
        DEFAULT(""),
        SECONDARY("badge-secondary"),
        OUTLINE("badge-outline"),
        DESTRUCTIVE("badge-destructive"),
        SUCCESS("badge-success"),
        INFO("badge-info");

        private final String cssClass;

        Variant(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private Label label;
    private Variant currentVariant;

    // Default constructor for FXML
    public TLBadge() {
        this("", Variant.DEFAULT);
    }

    public TLBadge(String text, Variant variant) {
        getStyleClass().add("badge");
        this.currentVariant = variant;
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        label = new Label(text);
        getChildren().add(label);
    }

    public void setText(String text) {
        if (label == null) {
            label = new Label(text);
            getChildren().add(label);
        } else {
            label.setText(text);
        }
    }

    public void setVariant(Variant variant) {
        if (currentVariant == variant)
            return;

        // Ensure badge style class is present
        if (!getStyleClass().contains("badge")) {
            getStyleClass().add("badge");
        }

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

    // Setter for FXML that accepts String and converts to enum
    public void setVariant(String variantName) {
        try {
            Variant variant = Variant.valueOf(variantName.toUpperCase());
            setVariant(variant);
        } catch (IllegalArgumentException e) {
            // Default to DEFAULT if invalid variant name
            setVariant(Variant.DEFAULT);
        }
    }

    public String getText() {
        return label != null ? label.getText() : "";
    }

    public Variant getVariant() {
        return currentVariant;
    }

    private static String variantToClass(Variant v) {
        return v.getCssClass();
    }
}
