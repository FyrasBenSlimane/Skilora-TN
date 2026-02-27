package com.skilora.framework.components;

import javafx.scene.control.Label;

public class TLLabel extends Label {
    public TLLabel() { super(); getStyleClass().add("tl-label"); }
    public TLLabel(String text) { super(text); getStyleClass().add("tl-label"); }
}
