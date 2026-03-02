package com.skilora.framework.components;

import com.skilora.utils.I18n;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Standardized loading state component (G-09).
 * Centered spinner + optional label, reusable across all views.
 *
 * Usage:
 *   TLLoadingState loading = new TLLoadingState();  // default "Loading..." text
 *   TLLoadingState loading = new TLLoadingState("Fetching data...");
 */
public class TLLoadingState extends VBox {

    private static final String STYLESHEET =
            TLLoadingState.class.getResource("/com/skilora/framework/styles/tl-loading-state.css").toExternalForm();

    private final ProgressIndicator spinner;
    private final Label messageLabel;

    public TLLoadingState() {
        this(I18n.get("common.loading"));
    }

    public TLLoadingState(String message) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("loading-state");

        spinner = new ProgressIndicator();
        spinner.getStyleClass().add("loading-spinner");
        spinner.setMaxSize(32, 32);

        messageLabel = new Label(message);
        messageLabel.getStyleClass().add("loading-label");

        getChildren().addAll(spinner, messageLabel);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}
