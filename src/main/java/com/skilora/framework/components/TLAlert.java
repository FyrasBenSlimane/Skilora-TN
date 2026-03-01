package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLAlert - shadcn/ui Alert (theme-adaptive message box).
 * Variants: default, destructive, success, warning, info.
 */
public class TLAlert extends VBox {

    public enum Variant {
        DEFAULT, DESTRUCTIVE, SUCCESS, WARNING, INFO
    }

    private final Label titleLabel;
    private final Label descriptionLabel;

    public TLAlert(String title, String description) {
        this(title, description, Variant.DEFAULT);
    }

    public TLAlert(String title, String description, Variant variant) {
        getStyleClass().add("alert");
        if (variant != Variant.DEFAULT)
            getStyleClass().add(variantToClass(variant));
        setSpacing(8);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("alert-title");
        descriptionLabel = new Label(description != null ? description : "");
        descriptionLabel.getStyleClass().add("alert-description");
        descriptionLabel.setWrapText(true);

        getChildren().addAll(titleLabel, descriptionLabel);
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

    public void setIcon(Node icon) {
        if (icon != null) {
            icon.getStyleClass().add("alert-icon");
            getChildren().removeIf(n -> n.getStyleClass().contains("alert-icon"));
            getChildren().add(0, icon);
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description != null ? description : "");
    }
}
