package com.skilora.framework.components;

import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;

/**
 * TLCalendar - shadcn/ui Calendar (theme-adaptive date picker / calendar view).
 * Wraps DatePicker with .calendar styling; for full month grid use DatePicker's built-in popup.
 */
public class TLCalendar extends VBox {

    private final DatePicker picker;

    public TLCalendar() {
        getStyleClass().add("calendar");
        setSpacing(8);
        picker = new DatePicker();
        picker.getStyleClass().add("calendar-header");
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
