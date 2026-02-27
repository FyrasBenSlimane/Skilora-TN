package com.skilora.framework.components;

import javafx.scene.control.ComboBox;

public class TLSelect<T> extends ComboBox<T> {
    public TLSelect() {
        super();
        getStyleClass().add("tl-select");
    }
}
