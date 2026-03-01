package com.skilora.framework.components;

import javafx.scene.control.Slider;

/**
 * TLSlider - shadcn/ui Slider (theme-adaptive).
 */
public class TLSlider extends Slider {

    public TLSlider() {
        this(0, 100, 50);
    }

    public TLSlider(double min, double max, double value) {
        super(min, max, value);
        getStyleClass().add("slider");
    }
}
