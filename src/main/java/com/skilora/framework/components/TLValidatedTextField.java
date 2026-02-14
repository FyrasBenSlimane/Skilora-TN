package com.skilora.framework.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * TLValidatedTextField - TextField with animated real-time validation
 */
public class TLValidatedTextField extends VBox {

    private final TextField input;
    private final Label labelNode;
    private final Label errorLabel;
    private String errorMessage = "";
    private Timeline blinkTimeline;

    public TLValidatedTextField() {
        this("", "");
    }

    public TLValidatedTextField(String label, String placeholder) {
        getStyleClass().add("form-field");
        setSpacing(4);

        // Label
        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        labelNode.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        if (label == null || label.isEmpty()) {
            labelNode.setVisible(false);
            labelNode.setManaged(false);
        }
        getChildren().add(labelNode);

        // Input Field
        input = new TextField();
        input.setPromptText(placeholder);
        input.getStyleClass().add("text-input");
        input.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-background-color: #2a2a2a; -fx-border-color: #555; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        getChildren().add(input);

        // Error Label (initially hidden)
        errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-padding: 4 0 0 4; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        getChildren().add(errorLabel);
    }

    public String getText() {
        return input.getText();
    }

    public void setText(String text) {
        input.setText(text);
        clearError();
    }

    public void setError(String message) {
        this.errorMessage = message;
        if (message != null && !message.isEmpty()) {
            errorLabel.setText("⚠️ " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            input.setStyle(
                    "-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-background-color: #2a2a2a; -fx-border-color: #ff4444; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

            // Start blinking animation
            startBlinkingAnimation();
        } else {
            clearError();
        }
    }

    private void startBlinkingAnimation() {
        // Stop previous animation if exists
        if (blinkTimeline != null) {
            blinkTimeline.stop();
        }

        // Create blinking animation
        blinkTimeline = new Timeline(
                new KeyFrame(Duration.millis(0), e -> errorLabel.setTextFill(Color.web("#ff4444"))),
                new KeyFrame(Duration.millis(500), e -> errorLabel.setTextFill(Color.web("#ff8888"))),
                new KeyFrame(Duration.millis(1000), e -> errorLabel.setTextFill(Color.web("#ff4444"))));
        blinkTimeline.setCycleCount(Timeline.INDEFINITE);
        blinkTimeline.play();
    }

    public void clearError() {
        errorMessage = "";
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        input.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-background-color: #2a2a2a; -fx-border-color: #555; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

        // Stop blinking animation
        if (blinkTimeline != null) {
            blinkTimeline.stop();
        }
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    // FXML Property Getters and Setters
    public String getLabel() {
        return labelNode.getText();
    }

    public void setLabel(String label) {
        labelNode.setText(label);
        boolean hasLabel = label != null && !label.isEmpty();
        labelNode.setVisible(hasLabel);
        labelNode.setManaged(hasLabel);
    }

    public String getPromptText() {
        return input.getPromptText();
    }

    public void setPromptText(String promptText) {
        input.setPromptText(promptText);
    }

    public TextField getControl() {
        return input;
    }
}
