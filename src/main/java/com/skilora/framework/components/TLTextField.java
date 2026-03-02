package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * TLTextField - shadcn/ui Input (CSS-only, theme-adaptive)
 *
 * Label + TextField + optional helper. All colors from theme (dark/light).
 */
public class TLTextField extends VBox {

    private final TextField input;
    private final Label labelNode;
    private final Label helperNode;

    public TLTextField() {
        this("", "", "");
    }

    public TLTextField(String label, String placeholder) {
        this(label, placeholder, "");
    }

    public TLTextField(String label, String placeholder, String helperText) {
        getStyleClass().add("form-field");
        // Only add spacing if we have a label or helper
        setSpacing(label != null && !label.isEmpty() ? 8 : 0);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        if (label == null || label.isEmpty()) {
            labelNode.setVisible(false);
            labelNode.setManaged(false);
        }
        getChildren().add(labelNode);

        input = new TextField();
        input.setPromptText(placeholder);
        input.getStyleClass().add("text-input");
        getChildren().add(input);

        helperNode = new Label(helperText);
        helperNode.getStyleClass().add("form-helper");
        helperNode.setVisible(!helperText.isEmpty());
        helperNode.setManaged(!helperText.isEmpty());
        getChildren().add(helperNode);
    }

    public String getText() {
        return input.getText();
    }

    public void setText(String text) {
        input.setText(text);
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
        setSpacing(hasLabel ? 8 : 0);
    }

    public String getPromptText() {
        return input.getPromptText();
    }

    public void setPromptText(String promptText) {
        input.setPromptText(promptText);
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

    public TextField getControl() {
        return input;
    }

    /**
     * Set inline validation state: shows error text and red border.
     * Pass null or empty to clear the error.
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
     * Set inline success state: shows success text and green border.
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
