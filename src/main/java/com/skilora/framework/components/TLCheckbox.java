package com.skilora.framework.components;

import javafx.scene.control.CheckBox;

public class TLCheckbox extends CheckBox {
    public TLCheckbox() { super(); getStyleClass().add("tl-checkbox"); }
    public TLCheckbox(String text) { super(text); getStyleClass().add("tl-checkbox"); }
}
