package com.skilora.framework.components;

import javafx.scene.control.Label;

/**
 * TLKbd - shadcn/ui Kbd (keyboard key chip).
 * Label styled as a key cap; theme-adaptive.
 */
public class TLKbd extends Label {

    public TLKbd(String key) {
        super(key != null ? key : "");
        getStyleClass().add("kbd");
    }

    public TLKbd() {
        this("");
    }

    public void setKey(String key) {
        setText(key != null ? key : "");
    }
}
