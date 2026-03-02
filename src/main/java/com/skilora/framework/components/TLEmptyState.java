package com.skilora.framework.components;

import com.skilora.utils.SvgIcons;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Standardized empty state component (G-11).
 * Icon + title + subtitle + optional action button.
 *
 * Usage:
 *   TLEmptyState empty = new TLEmptyState(SvgIcons.SEARCH, "No results", "Try a different search.");
 *   TLEmptyState empty = new TLEmptyState(SvgIcons.BELL, "No notifications", "You're all caught up.", actionBtn);
 */
public class TLEmptyState extends VBox {

    private static final String STYLESHEET =
            TLEmptyState.class.getResource("/com/skilora/framework/styles/tl-empty-state.css").toExternalForm();

    public TLEmptyState(String iconPath, String title, String description) {
        this(iconPath, title, description, null);
    }

    public TLEmptyState(String iconPath, String title, String description, TLButton actionButton) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("empty");
        setAlignment(Pos.CENTER);

        SVGPath icon = SvgIcons.icon(iconPath, 48, "-fx-muted-foreground");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(320);

        getChildren().addAll(icon, titleLabel, descLabel);

        if (actionButton != null) {
            getChildren().add(actionButton);
        }
    }
}
