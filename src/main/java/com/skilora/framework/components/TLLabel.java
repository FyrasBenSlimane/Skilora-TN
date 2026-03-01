package com.skilora.framework.components;

import javafx.scene.control.Label;

/**
 * TLLabel - shadcn/ui Label (CSS-only, theme-adaptive)
 *
 * Uses .label; optional .label-sm, .label-lg, .text-muted.
 * Reference: https://ui.shadcn.com/docs/components/label
 */
public class TLLabel extends Label {

    public TLLabel() {
        this("");
    }

    public TLLabel(String text) {
        super(text);
        getStyleClass().add("label");
    }

    public TLLabel(String text, Size size) {
        super(text);
        getStyleClass().add("label");
        if (size == Size.SM) getStyleClass().add("label-sm");
        if (size == Size.LG) getStyleClass().add("label-lg");
    }

    /** Add .text-muted for secondary text. */
    public void setMuted(boolean muted) {
        if (muted) {
            if (!getStyleClass().contains("text-muted")) getStyleClass().add("text-muted");
        } else {
            getStyleClass().remove("text-muted");
        }
    }

    public enum Size { SM, DEFAULT, LG }
}
