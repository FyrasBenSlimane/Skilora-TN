package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLEmpty - shadcn/ui Empty state (theme-adaptive).
 * Optional icon, title, description, and action button.
 */
public class TLEmpty extends VBox {

    private final Label titleLabel;
    private final Label descriptionLabel;

    public TLEmpty() {
        this("No results", "Nothing to show here.");
    }

    public TLEmpty(String title, String description) {
        getStyleClass().add("empty");
        setSpacing(16);
        setAlignment(javafx.geometry.Pos.CENTER);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");
        descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("empty-description");
        descriptionLabel.setWrapText(true);

        getChildren().addAll(titleLabel, descriptionLabel);
    }

    public void setIcon(Node icon) {
        if (icon != null) {
            icon.getStyleClass().add("empty-icon");
            if (!getChildren().isEmpty() && getChildren().get(0) != icon) {
                getChildren().removeIf(n -> n.getStyleClass().contains("empty-icon"));
                getChildren().add(0, icon);
            } else if (getChildren().isEmpty()) {
                getChildren().add(0, icon);
            }
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    public void setAction(Node button) {
        getChildren().removeIf(n -> n instanceof javafx.scene.control.Button);
        if (button != null) getChildren().add(button);
    }
}
