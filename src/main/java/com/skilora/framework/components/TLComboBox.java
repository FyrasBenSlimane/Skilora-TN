package com.skilora.framework.components;

import javafx.scene.control.ComboBox;

public class TLComboBox<T> extends ComboBox<T> {
    public TLComboBox() { super(); getStyleClass().add("tl-combo-box"); }
}
