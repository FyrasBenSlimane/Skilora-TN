package com.skilora.framework.components;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLDatePicker - shadcn/ui Date Picker (theme-adaptive).
 */
public class TLDatePicker extends VBox {

    private final DatePicker picker;
    private final Label labelNode;

    public TLDatePicker() {
        this("");
    }

    public TLDatePicker(String label) {
        getStyleClass().add("form-field");
        setSpacing(8);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        if (label == null || label.isEmpty()) {
            labelNode.setVisible(false);
            labelNode.setManaged(false);
        }
        getChildren().add(labelNode);

        picker = new DatePicker();
        picker.getStyleClass().add("date-picker-input");
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setStyle("-fx-font-size: 14px; -fx-background-color: #2a2a2a; -fx-text-fill: #ffffff;");
        getChildren().add(picker);
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

    public DatePicker getDatePicker() {
        return picker;
    }

    public javafx.beans.property.ObjectProperty<java.time.LocalDate> valueProperty() {
        return picker.valueProperty();
    }

    public java.time.LocalDate getValue() {
        return picker.getValue();
    }

    public void setValue(java.time.LocalDate date) {
        picker.setValue(date);
    }
}
