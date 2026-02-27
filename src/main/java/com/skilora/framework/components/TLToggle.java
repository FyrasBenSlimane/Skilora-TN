package com.skilora.framework.components;

import javafx.scene.control.ToggleButton;

public class TLToggle extends ToggleButton {
    public TLToggle() { super(); getStyleClass().add("tl-toggle"); }
    public TLToggle(String text) { super(text); getStyleClass().add("tl-toggle"); }
}
