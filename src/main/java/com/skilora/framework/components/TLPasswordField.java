package com.skilora.framework.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import com.skilora.utils.SvgIcons;

/**
 * TLPasswordField - shadcn/ui Password Input with show/hide toggle
 * and optional password strength indicator bar.
 * Contains a PasswordField (masked) and a TextField (visible) that swap
 * on toggle, sharing the same text value.
 */
public class TLPasswordField extends VBox {

    private static final String STYLESHEET =
            TLPasswordField.class.getResource("/com/skilora/framework/styles/tl-password-field.css").toExternalForm();

    private final PasswordField passwordInput;
    private final TextField textInput;
    private final Label labelNode;
    private final Label helperNode;
    private final SVGPath eyeIcon;
    private final StackPane inputContainer;
    private boolean revealed = false;

    // Strength indicator components
    private HBox strengthBar;
    private Label strengthLabel;
    private VBox strengthContainer;
    private final Region[] segments = new Region[4];
    private boolean strengthIndicatorEnabled = false;

    public TLPasswordField() {
        this("", "", "");
    }

    public TLPasswordField(String label, String placeholder) {
        this(label, placeholder, "");
    }

    public TLPasswordField(String label, String placeholder, String helperText) {
        getStylesheets().add(STYLESHEET);
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
        eyeIcon.setContent(SvgIcons.EYE);
        eyeIcon.getStyleClass().add("password-eye-icon");
        eyeIcon.setScaleX(0.7);
        eyeIcon.setScaleY(0.7);

        Button toggleBtn = new Button();
        toggleBtn.setGraphic(eyeIcon);
        toggleBtn.getStyleClass().addAll("password-toggle-btn");
        toggleBtn.setFocusTraversable(false);
        toggleBtn.setOnAction(e -> toggleVisibility());

        // Stack the two inputs on top of each other + toggle aligned right
        inputContainer = new StackPane(passwordInput, textInput);
        HBox.setHgrow(inputContainer, Priority.ALWAYS);

        HBox inputRow = new HBox(0, inputContainer, toggleBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("password-input-row");

        // Remove individual input borders since the row provides the border
        passwordInput.getStyleClass().add("password-inner-input");
        textInput.getStyleClass().add("password-inner-input");

        // Focus forwarding: highlight the row border when either input is focused
        passwordInput.focusedProperty().addListener((obs, o, focused) -> updateRowFocus(inputRow, focused || textInput.isFocused()));
        textInput.focusedProperty().addListener((obs, o, focused) -> updateRowFocus(inputRow, focused || passwordInput.isFocused()));

        getChildren().add(inputRow);

        // ── Strength indicator (hidden by default, call setShowStrengthIndicator(true) to enable) ──
        strengthBar = new HBox(3);
        strengthBar.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 4; i++) {
            segments[i] = new Region();
            segments[i].setPrefHeight(4);
            segments[i].setMinHeight(4);
            segments[i].setMaxHeight(4);
            segments[i].getStyleClass().add("strength-segment");
            HBox.setHgrow(segments[i], Priority.ALWAYS);
            strengthBar.getChildren().add(segments[i]);
        }
        strengthLabel = new Label("");
        strengthLabel.getStyleClass().add("strength-label");

        strengthContainer = new VBox(4, strengthBar, strengthLabel);
        strengthContainer.setVisible(false);
        strengthContainer.setManaged(false);
        getChildren().add(strengthContainer);

        // Listen for text changes to update strength
        passwordInput.textProperty().addListener((obs, o, n) -> updateStrengthIndicator(n));
        textInput.textProperty().addListener((obs, o, n) -> updateStrengthIndicator(n));

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
            textInput.setOnKeyPressed(passwordInput.getOnKeyPressed());
            passwordInput.setVisible(false);
            passwordInput.setManaged(false);
            textInput.setVisible(true);
            textInput.setManaged(true);
            eyeIcon.setContent(SvgIcons.EYE_OFF);
            textInput.requestFocus();
            textInput.positionCaret(textInput.getText().length());
        } else {
            passwordInput.setOnKeyPressed(textInput.getOnKeyPressed());
            textInput.setVisible(false);
            textInput.setManaged(false);
            passwordInput.setVisible(true);
            passwordInput.setManaged(true);
            eyeIcon.setContent(SvgIcons.EYE);
            passwordInput.requestFocus();
            passwordInput.positionCaret(passwordInput.getText().length());
        }
    }

    private void updateRowFocus(HBox row, boolean focused) {
        if (focused) {
            row.getStyleClass().add("password-input-row-focused");
        } else {
            row.getStyleClass().remove("password-input-row-focused");
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

    // ── Strength Indicator API ──

    /**
     * Enable or disable the password strength indicator bar.
     * When enabled, a 4-segment bar + label appears below the input
     * showing: Weak / Fair / Good / Strong based on password complexity.
     */
    public void setShowStrengthIndicator(boolean show) {
        this.strengthIndicatorEnabled = show;
        if (!show) {
            strengthContainer.setVisible(false);
            strengthContainer.setManaged(false);
        } else if (!passwordInput.getText().isEmpty()) {
            updateStrengthIndicator(passwordInput.getText());
        }
    }

    public boolean isShowStrengthIndicator() {
        return strengthIndicatorEnabled;
    }

    /**
     * Compute password strength score (0-4) based on:
     * - Length >= 8
     * - Has uppercase letter
     * - Has lowercase letter
     * - Has digit
     * - Has special character
     * Score mapping: 0-1 = Weak, 2 = Fair, 3 = Good, 4-5 = Strong
     */
    private int computeStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        return score;
    }

    private void updateStrengthIndicator(String password) {
        if (!strengthIndicatorEnabled) return;

        if (password == null || password.isEmpty()) {
            strengthContainer.setVisible(false);
            strengthContainer.setManaged(false);
            return;
        }

        strengthContainer.setVisible(true);
        strengthContainer.setManaged(true);

        int score = computeStrength(password);
        // Map score 0-5 to level 0-3
        int level; // 0=weak, 1=fair, 2=good, 3=strong
        String label;

        if (score <= 1) {
            level = 0; label = "Weak";
        } else if (score == 2) {
            level = 1; label = "Fair";
        } else if (score == 3) {
            level = 2; label = "Good";
        } else {
            level = 3; label = "Strong";
        }

        // Fill segments: segments 0..level filled, rest empty
        for (int i = 0; i < 4; i++) {
            segments[i].getStyleClass().removeAll("strength-weak", "strength-fair", "strength-good", "strength-strong");
            if (i <= level) {
                String levelClass;
                if (level == 0) levelClass = "strength-weak";
                else if (level == 1) levelClass = "strength-fair";
                else if (level == 2) levelClass = "strength-good";
                else levelClass = "strength-strong";
                segments[i].getStyleClass().add(levelClass);
            }
        }

        strengthLabel.setText(label);
        strengthLabel.getStyleClass().removeAll("strength-weak", "strength-fair", "strength-good", "strength-strong");
        String labelClass;
        if (level == 0) labelClass = "strength-weak";
        else if (level == 1) labelClass = "strength-fair";
        else if (level == 2) labelClass = "strength-good";
        else labelClass = "strength-strong";
        strengthLabel.getStyleClass().add(labelClass);
    }

    // ── Inline Validation API ──

    /**
     * Set inline validation error state: red border and error text below the field.
     * Pass null or empty to clear.
     */
    public void setError(String errorMessage) {
        getStyleClass().removeAll("field-valid", "field-invalid");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            getStyleClass().add("field-invalid");
            helperNode.setText(errorMessage);
            helperNode.getStyleClass().removeAll("field-success-text", "field-error-text");
            helperNode.getStyleClass().add("field-error-text");
            helperNode.setVisible(true);
            helperNode.setManaged(true);
        } else {
            helperNode.setVisible(false);
            helperNode.setManaged(false);
        }
    }

    /**
     * Set inline validation success state: green border and success text below the field.
     * Pass null or empty to clear.
     */
    public void setSuccess(String successMessage) {
        getStyleClass().removeAll("field-valid", "field-invalid");
        if (successMessage != null && !successMessage.isEmpty()) {
            getStyleClass().add("field-valid");
            helperNode.setText(successMessage);
            helperNode.getStyleClass().removeAll("field-success-text", "field-error-text");
            helperNode.getStyleClass().add("field-success-text");
            helperNode.setVisible(true);
            helperNode.setManaged(true);
        } else {
            helperNode.setVisible(false);
            helperNode.setManaged(false);
        }
    }

    /** Clear validation state back to neutral. */
    public void clearValidation() {
        getStyleClass().removeAll("field-valid", "field-invalid");
        helperNode.getStyleClass().removeAll("field-success-text", "field-error-text");
        helperNode.setVisible(false);
        helperNode.setManaged(false);
    }
}
