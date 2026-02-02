package com.skilora.framework.components;

import javafx.scene.control.ProgressBar;

/**
 * TLProgress - shadcn/ui Progress bar for JavaFX.
 *
 * Wraps JavaFX ProgressBar with shadcn-consistent styling and size/color variants.
 *
 * CSS classes: .progress, .progress-sm, .progress-lg,
 *   .progress-success, .progress-warning, .progress-destructive
 *
 * Usage:
 *   TLProgress progress = new TLProgress(0.5);                      // 50%, default size
 *   TLProgress progress = new TLProgress(0.7, Size.SM);              // small
 *   TLProgress progress = new TLProgress(0.3, Size.DEFAULT, Variant.SUCCESS);
 */
public class TLProgress extends ProgressBar {

    public enum Size {
        SM, DEFAULT, LG
    }

    public enum Variant {
        DEFAULT, SUCCESS, WARNING, DESTRUCTIVE
    }

    public TLProgress() {
        this(0.0, Size.DEFAULT, Variant.DEFAULT);
    }

    public TLProgress(double progress) {
        this(progress, Size.DEFAULT, Variant.DEFAULT);
    }

    public TLProgress(double progress, Size size) {
        this(progress, size, Variant.DEFAULT);
    }

    public TLProgress(double progress, Size size, Variant variant) {
        super(progress);
        getStyleClass().add("progress");
        setMaxWidth(Double.MAX_VALUE);

        if (size != Size.DEFAULT) {
            getStyleClass().add(sizeToClass(size));
        }
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
    }

    public void setSize(Size size) {
        getStyleClass().removeIf(s -> s.startsWith("progress-s") || s.startsWith("progress-l"));
        if (size != Size.DEFAULT) {
            getStyleClass().add(sizeToClass(size));
        }
    }

    public void setVariant(Variant variant) {
        getStyleClass().removeIf(s ->
                s.equals("progress-success") || s.equals("progress-warning") || s.equals("progress-destructive"));
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
    }

    private static String sizeToClass(Size s) {
        return switch (s) {
            case SM -> "progress-sm";
            case LG -> "progress-lg";
            default -> "";
        };
    }

    private static String variantToClass(Variant v) {
        return switch (v) {
            case SUCCESS -> "progress-success";
            case WARNING -> "progress-warning";
            case DESTRUCTIVE -> "progress-destructive";
            default -> "";
        };
    }
}
