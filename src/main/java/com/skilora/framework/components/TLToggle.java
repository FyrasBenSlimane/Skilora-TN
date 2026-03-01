package com.skilora.framework.components;

import javafx.scene.control.ToggleButton;

/**
 * TLToggle - shadcn/ui Toggle (toggle button, theme-adaptive).
 */
public class TLToggle extends ToggleButton {

    public TLToggle() {
        this("");
    }

    public TLToggle(String text) {
        super(text);
        getStyleClass().add("toggle");
        selectedProperty().addListener((o, oldVal, newVal) -> {
            if (newVal) getStyleClass().add("toggle-on");
            else getStyleClass().remove("toggle-on");
        });
    }
}
