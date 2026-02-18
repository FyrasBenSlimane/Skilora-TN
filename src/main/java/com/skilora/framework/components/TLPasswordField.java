package com.skilora.framework.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * TLPasswordField - shadcn/ui Password Input with show/hide toggle.
 * Contains a PasswordField (masked) and a TextField (visible) that swap
 * on toggle, sharing the same text value.
 */
public class TLPasswordField extends VBox {

    // SVG eye paths (Lucide icons)
    private static final String EYE_SVG = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    private static final String EYE_OFF_SVG = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94 M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 M1 1l22 22";

    private final PasswordField passwordInput;
    private final TextField textInput;
    private final Label labelNode;
    private final Label helperNode;
    private final SVGPath eyeIcon;
    private final StackPane inputContainer;
    private boolean revealed = false;

    public TLPasswordField() {
        this("", "", "");
    }

    public TLPasswordField(String label, String placeholder) {
        this(label, placeholder, "");
    }

    public TLPasswordField(String label, String placeholder, String helperText) {
        getStyleClass().add("form-field");
        setSpacing(8);

        // Label
        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        boolean hasLabel = label != null && !label.isEmpty();
        labelNode.setVisible(hasLabel);
        labelNode.setManaged(hasLabel);
        getChildren().add(labelNode);

        // Password input (masked — default visible)
        passwordInput = new PasswordField();
        passwordInput.setPromptText(placeholder);
        passwordInput.getStyleClass().add("text-input");

        // Text input (revealed — initially hidden)
        textInput = new TextField();
        textInput.setPromptText(placeholder);
        textInput.getStyleClass().add("text-input");
        textInput.setVisible(false);
        textInput.setManaged(false);

        // Sync text between the two fields
        passwordInput.textProperty().addListener((obs, o, n) -> {
            if (!textInput.getText().equals(n)) textInput.setText(n);
        });
        textInput.textProperty().addListener((obs, o, n) -> {
            if (!passwordInput.getText().equals(n)) passwordInput.setText(n);
        });

        // Eye toggle button
        eyeIcon = new SVGPath();
        eyeIcon.setContent(EYE_SVG);
        eyeIcon.setStyle("-fx-fill: -fx-muted-foreground;");
        eyeIcon.setScaleX(0.7);
        eyeIcon.setScaleY(0.7);

        Button toggleBtn = new Button();
        toggleBtn.setGraphic(eyeIcon);
        toggleBtn.getStyleClass().addAll("password-toggle-btn");
        toggleBtn.setStyle(
            "-fx-background-color: transparent; -fx-padding: 0 8 0 4; -fx-cursor: hand; " +
            "-fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28;"
        );
        toggleBtn.setFocusTraversable(false);
        toggleBtn.setOnAction(e -> toggleVisibility());

        // Stack the two inputs on top of each other + toggle aligned right
        inputContainer = new StackPane(passwordInput, textInput);
        HBox.setHgrow(inputContainer, Priority.ALWAYS);

        HBox inputRow = new HBox(0, inputContainer, toggleBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("password-input-row");
        inputRow.setStyle(
            "-fx-background-color: transparent; -fx-border-color: -fx-input; -fx-border-width: 1; " +
            "-fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 1, 0, 0, 1);"
        );

        // Remove individual input borders since the row provides the border
        passwordInput.setStyle("-fx-border-color: transparent; -fx-background-color: transparent; -fx-effect: null;");
        textInput.setStyle("-fx-border-color: transparent; -fx-background-color: transparent; -fx-effect: null;");

        // Focus forwarding: highlight the row border when either input is focused
        passwordInput.focusedProperty().addListener((obs, o, focused) -> updateRowFocus(inputRow, focused || textInput.isFocused()));
        textInput.focusedProperty().addListener((obs, o, focused) -> updateRowFocus(inputRow, focused || passwordInput.isFocused()));

        getChildren().add(inputRow);

        // Helper text
        helperNode = new Label(helperText);
        helperNode.getStyleClass().add("form-helper");
        helperNode.setVisible(!helperText.isEmpty());
        helperNode.setManaged(!helperText.isEmpty());
        getChildren().add(helperNode);
    }

    private void toggleVisibility() {
        revealed = !revealed;
        if (revealed) {
            // Transfer key handler & style from password → text
            textInput.setOnKeyPressed(passwordInput.getOnKeyPressed());
            textInput.setStyle(passwordInput.getStyle());
            passwordInput.setVisible(false);
            passwordInput.setManaged(false);
            textInput.setVisible(true);
            textInput.setManaged(true);
            eyeIcon.setContent(EYE_OFF_SVG);
            textInput.requestFocus();
            textInput.positionCaret(textInput.getText().length());
        } else {
            // Transfer key handler & style from text → password
            passwordInput.setOnKeyPressed(textInput.getOnKeyPressed());
            passwordInput.setStyle(textInput.getStyle());
            textInput.setVisible(false);
            textInput.setManaged(false);
            passwordInput.setVisible(true);
            passwordInput.setManaged(true);
            eyeIcon.setContent(EYE_SVG);
            passwordInput.requestFocus();
            passwordInput.positionCaret(passwordInput.getText().length());
        }
    }

    private void updateRowFocus(HBox row, boolean focused) {
        if (focused) {
            row.setStyle(
                "-fx-background-color: transparent; -fx-border-color: -fx-ring; -fx-border-width: 2; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 3, 0, 0, 0);"
            );
        } else {
            row.setStyle(
                "-fx-background-color: transparent; -fx-border-color: -fx-input; -fx-border-width: 1; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 1, 0, 0, 1);"
            );
        }
    }

    // ---- Public API ----

    public String getText() {
        return passwordInput.getText();
    }

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
        return passwordInput.getPromptText();
    }

    public void setPromptText(String promptText) {
        passwordInput.setPromptText(promptText);
        textInput.setPromptText(promptText);
    }

    public String getHelperText() {
        return helperNode.getText();
    }

    public void setHelperText(String text) {
        helperNode.setText(text);
        boolean hasText = text != null && !text.isEmpty();
        helperNode.setVisible(hasText);
        helperNode.setManaged(hasText);
    }

    /**
     * Returns the currently active input control (PasswordField or TextField).
     * For key event handlers that need access regardless of toggle state.
     */
    public javafx.scene.control.TextInputControl getControl() {
        return revealed ? textInput : passwordInput;
    }

    /** Returns the underlying PasswordField (always the masked one). */
    public PasswordField getPasswordField() {
        return passwordInput;
    }

    /** Returns the underlying TextField (the revealed one). */
    public TextField getTextField() {
        return textInput;
    }
}
