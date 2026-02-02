package com.skilora.framework.components;

import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLButton - Skilora Button Component
 *
 * Uses CSS classes (theme.css + components.css) so colors follow dark/light
 * theme.
 * Usage: TLButton btn = new TLButton("Click Me", ButtonVariant.PRIMARY);
 */
public class TLButton extends Button {

    private static final Logger logger = LoggerFactory.getLogger(TLButton.class);

    public enum ButtonVariant {
        PRIMARY, SECONDARY, OUTLINE, GHOST, SUCCESS, DANGER
    }

    public enum ButtonSize {
        SM, MD, LG
    }

    private ButtonVariant variant = ButtonVariant.PRIMARY;
    private ButtonSize size = ButtonSize.MD;

    public TLButton() {
        super();
        applyStyleClasses();
    }

    public TLButton(String text) {
        super(text);
        applyStyleClasses();
    }

    public TLButton(String text, ButtonVariant variant) {
        super(text);
        this.variant = variant;
        applyStyleClasses();
    }

    public TLButton(String text, ButtonVariant variant, ButtonSize size) {
        super(text);
        this.variant = variant;
        this.size = size;
        applyStyleClasses();
    }

    private static String variantToClass(ButtonVariant v) {
        return switch (v) {
            case PRIMARY -> "btn-primary";
            case SECONDARY -> "btn-secondary";
            case OUTLINE -> "btn-outline";
            case GHOST -> "btn-ghost";
            case SUCCESS -> "btn-success";
            case DANGER -> "btn-destructive";
        };
    }

    private static String sizeToClass(ButtonSize s) {
        return switch (s) {
            case SM -> "btn-sm";
            case MD -> "btn";
            case LG -> "btn-lg";
        };
    }

    private void applyStyleClasses() {
        getStyleClass().removeIf(s -> s.startsWith("btn-") || s.equals("btn"));
        getStyleClass().add("btn");
        getStyleClass().add(variantToClass(variant));
        String sizeClass = sizeToClass(size);
        if (!sizeClass.equals("btn"))
            getStyleClass().add(sizeClass);
        setCursor(javafx.scene.Cursor.HAND);
    }

    /** Get current variant */
    public ButtonVariant getVariant() {
        return variant;
    }

    /** Set variant using enum */
    public void setVariant(ButtonVariant variant) {
        this.variant = variant;
        applyStyleClasses();
    }

    /** Set variant from string (for FXML compatibility) */
    public void setVariant(String variant) {
        try {
            this.variant = ButtonVariant.valueOf(variant.toUpperCase());
            applyStyleClasses();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid button variant: " + variant, e);
        }
    }

    /** Get current size */
    public ButtonSize getSize() {
        return size;
    }

    /** Set size using enum */
    public void setSize(ButtonSize size) {
        this.size = size;
        applyStyleClasses();
    }

    /** Set size from string (for FXML compatibility) */
    public void setSize(String size) {
        try {
            this.size = ButtonSize.valueOf(size.toUpperCase());
            applyStyleClasses();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid button size: " + size, e);
        }
    }
}
