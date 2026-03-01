package com.skilora.framework.components;

import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;

/**
 * TLDatePicker - shadcn/ui Date Picker (theme-adaptive).
 */
public class TLDatePicker extends VBox {

    private final DatePicker picker;

    public TLDatePicker() {
        getStyleClass().add("date-picker");
        picker = new DatePicker();
        picker.getStyleClass().add("date-picker-input");
        getChildren().add(picker);
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
