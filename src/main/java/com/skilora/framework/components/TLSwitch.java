package com.skilora.framework.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;

/**
 * TLSwitch - shadcn/ui Switch (toggle-style CheckBox).
 * Uses .switch, .switch-track-off/on; theme-adaptive.
 */
public class TLSwitch extends CheckBox {

    private final BooleanProperty on = new SimpleBooleanProperty(false);

    public TLSwitch() {
        getStyleClass().addAll("switch", "switch-track", "switch-track-off");
        setSelected(false);
        selectedProperty().addListener((o, oldVal, newVal) -> {
            on.set(newVal);
            getStyleClass().removeAll("switch-track-off", "switch-track-on");
            getStyleClass().add(newVal ? "switch-track-on" : "switch-track-off");
        });
        getStyleClass().add("switch-track-off");
    }

    public boolean isOn() {
        return on.get();
    }

    public void setOn(boolean on) {
        setSelected(on);
        this.on.set(on);
    }

    public BooleanProperty onProperty() {
        return on;
    }
}
