package com.skilora.framework.components;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class TLTextField extends VBox {
    private final TextField control = new TextField();
    private String labelText;

    public TLTextField() {
        getChildren().add(control);
        getStyleClass().add("tl-text-field");
        control.prefWidthProperty().bind(widthProperty());
    }

    /** Expose for FXML: promptText="..." */
    public final StringProperty promptTextProperty() {
        return control.promptTextProperty();
    }

    public String getPromptText() {
        return control != null ? control.getPromptText() : "";
    }

    public void setPromptText(String s) {
        if (control != null) control.setPromptText(s);
    }

    public void setLabel(String label) {
        this.labelText = label;
        if (label != null && !label.isEmpty() && (getChildren().isEmpty() || getChildren().get(0) == control)) {
            getChildren().add(0, new Label(label));
        }
    }

    public TextField getControl() {
        return control;
    }

    public String getText() {
        return control != null ? control.getText() : "";
    }

    public void setText(String text) {
        if (control != null) control.setText(text);
    }

    /** Clear the text field. */
    public void clear() {
        if (control != null) control.clear();
    }
}
