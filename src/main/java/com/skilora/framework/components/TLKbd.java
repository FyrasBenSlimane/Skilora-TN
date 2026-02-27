package com.skilora.framework.components;

import javafx.scene.control.Label;

public class TLKbd extends Label {
    public TLKbd() { super(); getStyleClass().add("tl-kbd"); }
    public TLKbd(String text) { super(text); getStyleClass().add("tl-kbd"); }
}
