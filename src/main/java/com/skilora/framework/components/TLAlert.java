package com.skilora.framework.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * TLAlert - shadcn/ui Alert callout component for JavaFX.
 *
 * Inline notification callout with icon, title, and description.
 * Variants: DEFAULT, DESTRUCTIVE, SUCCESS, WARNING, INFO.
 *
 * CSS classes: .alert, .alert-destructive, .alert-success, .alert-warning, .alert-info,
 *   .alert-icon, .alert-title, .alert-description
 *
 * Usage:
 *   TLAlert alert = new TLAlert("Heads up!", "You can add components using the CLI.");
 *   TLAlert alert = new TLAlert(Variant.DESTRUCTIVE, "Error", "Session expired.");
 *   TLAlert alert = TLAlert.withIcon(iconNode, "Success!", "Changes saved.");
 */
public class TLAlert extends HBox {

    public enum Variant {
        DEFAULT, DESTRUCTIVE, SUCCESS, WARNING, INFO
    }

    private final Label titleLabel;
    private final Label descriptionLabel;
    private Variant currentVariant;

    public TLAlert(String title, String description) {
        this(Variant.DEFAULT, title, description);
    }

    public TLAlert(Variant variant, String title, String description) {
        this(variant, null, title, description);
    }

    public TLAlert(Variant variant, Node icon, String title, String description) {
        getStyleClass().add("alert");
        currentVariant = variant;
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        setAlignment(Pos.TOP_LEFT);
        setSpacing(12);

        // Icon (optional)
        if (icon != null) {
            icon.getStyleClass().add("alert-icon");
            getChildren().add(icon);
        }

        // Text content
        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("alert-title");
        titleLabel.setWrapText(true);

        descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("alert-description");
        descriptionLabel.setWrapText(true);

        textBox.getChildren().addAll(titleLabel, descriptionLabel);
        getChildren().add(textBox);
    }

    /**
     * Convenience factory with icon.
     */
    public static TLAlert withIcon(Node icon, String title, String description) {
        return new TLAlert(Variant.DEFAULT, icon, title, description);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    public void setVariant(Variant variant) {
        if (currentVariant != Variant.DEFAULT) {
            getStyleClass().remove(variantToClass(currentVariant));
        }
        if (variant != Variant.DEFAULT) {
            getStyleClass().add(variantToClass(variant));
        }
        currentVariant = variant;
    }

    private static String variantToClass(Variant v) {
        return switch (v) {
            case DESTRUCTIVE -> "alert-destructive";
            case SUCCESS -> "alert-success";
            case WARNING -> "alert-warning";
            case INFO -> "alert-info";
            default -> "";
        };
    }
}
