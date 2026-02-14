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
        DEFAULT, PRIMARY, SECONDARY, OUTLINE, DESTRUCTIVE, SUCCESS
    }

    private final Label label;
    private Variant variant = Variant.DEFAULT;

    public TLBadge() {
        this("", Variant.DEFAULT);
    }

    public TLBadge(String text) {
        this(text, Variant.DEFAULT);
    }

    public TLBadge(String text, Variant variant) {
        getStyleClass().add("badge");
        this.label = new Label(text);
        getChildren().add(label);
        setVariant(variant);
    }

    public String getText() {
        return label.getText();
    }

    public void setText(String text) {
        label.setText(text);
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        // Remove old variant class
        if (this.variant != Variant.DEFAULT) {
            getStyleClass().remove(variantToClass(this.variant));
        }
        // Add new variant class
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        this.variant = variant;
    }

    /** For FXML compatibility */
    public void setVariant(String variantStr) {
        try {
            setVariant(Variant.valueOf(variantStr.toUpperCase()));
        } catch (Exception e) {
            setVariant(Variant.DEFAULT);
        }
    }

    private static String variantToClass(Variant v) {
        return switch (v) {
            case PRIMARY -> "badge-primary";
            case SECONDARY -> "badge-secondary";
            case OUTLINE -> "badge-outline";
            case DESTRUCTIVE -> "badge-destructive";
            case SUCCESS -> "badge-success";
            default -> "";
        };
    }
}
