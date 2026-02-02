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
        DEFAULT, SECONDARY, OUTLINE, DESTRUCTIVE, SUCCESS
    }

    private final Label label;
    private Variant currentVariant;

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
        label.setText(text);
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
            default -> "";
        };
    }
}
