package com.skilora.framework.components;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * TLCheckbox - shadcn/ui Checkbox (CSS-only, theme-adaptive)
 *
 * Label + optional helper. Uses .checkbox, .checkbox-input, .checkbox-label, .checkbox-helper.
 * Reference: https://ui.shadcn.com/docs/components/checkbox
 */
public class TLCheckbox extends VBox {

    private final CheckBox input;
    private final Label labelNode;
    private final Label helperNode;

    public TLCheckbox(String label) {
        this(label, false, "");
    }

    public TLCheckbox(String label, boolean selected) {
        this(label, selected, "");
    }

    public TLCheckbox(String label, boolean selected, String helperText) {
        getStyleClass().add("checkbox");
        setSpacing(4);

        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        input = new CheckBox();
        input.getStyleClass().add("checkbox-input");
        input.setSelected(selected);
        input.setCursor(javafx.scene.Cursor.HAND);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("checkbox-label");
        labelNode.setCursor(javafx.scene.Cursor.HAND);
        labelNode.setOnMouseClicked(e -> input.setSelected(!input.isSelected()));

        row.getChildren().addAll(input, labelNode);
        getChildren().add(row);

        helperNode = new Label(helperText);
        helperNode.getStyleClass().add("checkbox-helper");
        helperNode.setVisible(!helperText.isEmpty());
        helperNode.setManaged(!helperText.isEmpty());
        if (!helperText.isEmpty()) {
            getChildren().add(helperNode);
        }
    }

    public boolean isSelected() {
        return input.isSelected();
    }

    public void setSelected(boolean selected) {
        input.setSelected(selected);
    }

    public javafx.beans.property.BooleanProperty selectedProperty() {
        return input.selectedProperty();
    }

    /** Add .checkbox-error to show error state. */
    public void setError(boolean error) {
        if (error) {
            if (!getStyleClass().contains("checkbox-error")) getStyleClass().add("checkbox-error");
        } else {
            getStyleClass().remove("checkbox-error");
        }
    }
}
