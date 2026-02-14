package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLToast - shadcn/ui Toast (notification chip).
 * Title + optional description; theme-adaptive. Shown in overlay; caller manages placement/dismiss.
 */
public class TLToast extends VBox {

    private final Label titleLabel;
    private final Label descriptionLabel;

    public TLToast(String title) {
        this(title, null);
    }

    public TLToast(String title, String description) {
        getStyleClass().add("toast");
        setSpacing(8);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toast-title");
        getChildren().add(titleLabel);

        descriptionLabel = new Label(description != null ? description : "");
        descriptionLabel.getStyleClass().add("toast-description");
        descriptionLabel.setWrapText(true);
        if (description != null && !description.isEmpty())
            getChildren().add(descriptionLabel);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description != null ? description : "");
        descriptionLabel.setVisible(description != null && !description.isEmpty());
        descriptionLabel.setManaged(description != null && !description.isEmpty());
    }
}
