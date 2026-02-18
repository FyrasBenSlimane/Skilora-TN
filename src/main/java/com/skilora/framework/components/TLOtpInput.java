package com.skilora.framework.components;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

/**
 * TLOtpInput - A 6-digit OTP input with individual digit boxes.
 * Features:
 * - Auto-advance to next box on digit entry
 * - Backspace navigates to previous box
 * - Paste support: pastes 6-digit code across all boxes
 * - Numeric-only input filtering
 * - Completion callback when all 6 digits are entered
 */
public class TLOtpInput extends HBox {

    private static final int DIGIT_COUNT = 6;
    private final TextField[] digitFields = new TextField[DIGIT_COUNT];
    private Runnable onComplete;

    public TLOtpInput() {
        setSpacing(8);
        setAlignment(Pos.CENTER);

        for (int i = 0; i < DIGIT_COUNT; i++) {
            final int index = i;
            TextField tf = new TextField();
            tf.setPrefWidth(48);
            tf.setPrefHeight(48);
            tf.setMinWidth(48);
            tf.setMaxWidth(48);
            tf.setMinHeight(48);
            tf.setMaxHeight(48);
            tf.setAlignment(Pos.CENTER);
            tf.getStyleClass().add("otp-digit");
            tf.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: 700; -fx-background-color: transparent; " +
                "-fx-border-color: -fx-input; -fx-border-width: 2; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-text-fill: -fx-foreground; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 1, 0, 0, 1);"
            );

            // Focus styling
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    tf.setStyle(
                        "-fx-font-size: 20px; -fx-font-weight: 700; -fx-background-color: transparent; " +
                        "-fx-border-color: -fx-ring; -fx-border-width: 2; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-text-fill: -fx-foreground; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 3, 0, 0, 0);"
                    );
                    tf.selectAll();
                } else {
                    tf.setStyle(
                        "-fx-font-size: 20px; -fx-font-weight: 700; -fx-background-color: transparent; " +
                        "-fx-border-color: -fx-input; -fx-border-width: 2; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-text-fill: -fx-foreground; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 1, 0, 0, 1);"
                    );
                }
            });

            // Filter: only allow single digit
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isEmpty()) return;

                // Filter non-digits
                String filtered = newVal.replaceAll("[^0-9]", "");
                if (filtered.isEmpty()) {
                    tf.setText("");
                    return;
                }

                // Keep only last digit typed (in case of overwrite)
                if (filtered.length() > 1) {
                    filtered = filtered.substring(filtered.length() - 1);
                }

                if (!filtered.equals(newVal)) {
                    tf.setText(filtered);
                    return;
                }

                // Auto-advance to next field
                if (!filtered.isEmpty() && index < DIGIT_COUNT - 1) {
                    digitFields[index + 1].requestFocus();
                }

                // Check completion
                checkCompletion();
            });

            // Key events for navigation
            tf.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.BACK_SPACE) {
                    if (tf.getText().isEmpty() && index > 0) {
                        digitFields[index - 1].requestFocus();
                        digitFields[index - 1].clear();
                        event.consume();
                    }
                } else if (event.getCode() == KeyCode.LEFT && index > 0) {
                    digitFields[index - 1].requestFocus();
                    event.consume();
                } else if (event.getCode() == KeyCode.RIGHT && index < DIGIT_COUNT - 1) {
                    digitFields[index + 1].requestFocus();
                    event.consume();
                } else if (event.getCode() == KeyCode.V && event.isShortcutDown()) {
                    // Handle paste
                    handlePaste();
                    event.consume();
                }
            });

            digitFields[i] = tf;
            getChildren().add(tf);
        }
    }

    private void handlePaste() {
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            String content = clipboard.getString();
            if (content == null) return;

            // Extract digits only
            String digits = content.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return;

            // Fill as many boxes as we have digits (up to 6)
            int count = Math.min(digits.length(), DIGIT_COUNT);
            for (int i = 0; i < count; i++) {
                digitFields[i].setText(String.valueOf(digits.charAt(i)));
            }

            // Focus appropriate field
            if (count >= DIGIT_COUNT) {
                digitFields[DIGIT_COUNT - 1].requestFocus();
            } else {
                digitFields[count].requestFocus();
            }

            checkCompletion();
        } catch (Exception e) {
            // Ignore clipboard errors
        }
    }

    private void checkCompletion() {
        String code = getText();
        if (code.length() == DIGIT_COUNT && onComplete != null) {
            onComplete.run();
        }
    }

    /**
     * Returns the concatenated OTP code from all digit boxes.
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (TextField tf : digitFields) {
            sb.append(tf.getText());
        }
        return sb.toString();
    }

    /**
     * Sets the OTP value programmatically across all boxes.
     */
    public void setText(String code) {
        clear();
        if (code == null) return;
        String digits = code.replaceAll("[^0-9]", "");
        int count = Math.min(digits.length(), DIGIT_COUNT);
        for (int i = 0; i < count; i++) {
            digitFields[i].setText(String.valueOf(digits.charAt(i)));
        }
    }

    /**
     * Clears all digit boxes.
     */
    public void clear() {
        for (TextField tf : digitFields) {
            tf.clear();
        }
    }

    /**
     * Focus the first digit box.
     */
    public void focusFirst() {
        if (digitFields[0] != null) {
            digitFields[0].requestFocus();
        }
    }

    /**
     * Set a callback to run when all 6 digits have been entered.
     */
    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }

    /**
     * Enable or disable all digit fields.
     */
    public void setFieldsDisabled(boolean disable) {
        for (TextField tf : digitFields) {
            tf.setDisable(disable);
        }
    }

    /**
     * Set an Enter key handler on all digit fields.
     */
    public void setOnEnter(Runnable action) {
        for (TextField tf : digitFields) {
            tf.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER && action != null) {
                    action.run();
                }
            });
        }
    }
}
