package com.skilora.framework.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * TLComboBox - shadcn/ui Combobox (editable/search-style ComboBox, theme-adaptive).
 */
public class TLComboBox<T> extends ComboBox<T> {

    public TLComboBox() {
        this(FXCollections.observableArrayList());
    }

    public TLComboBox(ObservableList<T> items) {
        super(items);
        getStyleClass().addAll("combobox", "combobox-trigger");
        setMaxWidth(Double.MAX_VALUE);
    }

    @SafeVarargs
    public TLComboBox(T... items) {
        this(FXCollections.observableArrayList(items));
    }
}
