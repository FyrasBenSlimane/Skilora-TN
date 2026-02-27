package com.skilora.framework.components;

import javafx.scene.control.Spinner;

public class TLSpinner<T> extends Spinner<T> {
    public TLSpinner() { super(); getStyleClass().add("tl-spinner"); }
}
