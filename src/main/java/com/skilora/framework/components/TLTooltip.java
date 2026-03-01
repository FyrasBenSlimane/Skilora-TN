package com.skilora.framework.components;

import javafx.scene.control.Tooltip;

/**
 * TLTooltip - shadcn/ui Tooltip (theme-adaptive).
 * Extends JavaFX Tooltip with .tooltip styling; use on any node via node.setTooltip(tooltip).
 */
public class TLTooltip extends Tooltip {

    public TLTooltip() {
        this("");
    }

    public TLTooltip(String text) {
        super(text);
        getStyleClass().add("tooltip");
    }
}
