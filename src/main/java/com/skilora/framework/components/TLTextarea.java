package com.skilora.framework.components;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * TLTextarea - shadcn/ui Textarea (CSS-only, theme-adaptive)
 *
 * Optional label + TextArea. Uses .form-field, .form-label, .textarea.
 * Reference: https://ui.shadcn.com/docs/components/textarea
 */
public class TLTextarea extends VBox {

    private static final String STYLESHEET =
            TLTextarea.class.getResource("/com/skilora/framework/styles/tl-textarea.css").toExternalForm();

    private final TextArea area;
    private final Label labelNode;

    public TLTextarea() {
        this("", "");
    }

    public TLTextarea(String label) {
        this(label, "");
    }

    public TLTextarea(String label, String prompt) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("form-field");
        setSpacing(8);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        if (label != null && !label.isEmpty()) {
            getChildren().add(labelNode);
        }

        area = new TextArea();
        area.getStyleClass().add("textarea");
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.setMinHeight(80);     // shadcn min-h-16 = 64px, slightly more for usability
        area.setPrefHeight(120);   // Comfortable default height
        getChildren().add(area);

        // NOTE: Bitmap caching removed — it corrupts text rendering in TextArea.
    }

    public String getText() {
        return area.getText();
    }

    public void setText(String text) {
        area.setText(text);
    }

    public TextArea getControl() {
        return area;
    }

    public StringProperty textProperty() {
        return area.textProperty();
    }

    public String getPromptText() {
        return area.getPromptText();
    }

    public void setPromptText(String text) {
        area.setPromptText(text);
    }

    public StringProperty promptTextProperty() {
        return area.promptTextProperty();
    }

    public void setWrapText(boolean wrap) {
        area.setWrapText(wrap);
    }

    public void setPrefRowCount(int rows) {
        area.setPrefRowCount(rows);
    }

    /** Add .textarea-error for error state. */
    public void setError(boolean error) {
        if (error) {
            if (!area.getStyleClass().contains("textarea-error"))
                area.getStyleClass().add("textarea-error");
        } else {
            area.getStyleClass().remove("textarea-error");
        }
    }

    public void setError(String errorMessage) {
        setError(true);
        if (errorLabel == null) {
            errorLabel = new Label();
            errorLabel.getStyleClass().add("field-error");
            getChildren().add(errorLabel);
        }
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public void clearValidation() {
        setError(false);
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private Label errorLabel;
}
