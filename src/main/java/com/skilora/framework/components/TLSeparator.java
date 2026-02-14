package com.skilora.framework.components;

import javafx.scene.control.Separator;

/**
 * TLSeparator - shadcn/ui Separator (CSS-only, theme-adaptive)
 *
 * Uses .separator, .separator-horizontal (default) or .separator-vertical.
 * Reference: https://ui.shadcn.com/docs/components/separator
 */
public class TLSeparator extends Separator {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public TLSeparator() {
        this(Orientation.HORIZONTAL);
    }

    public TLSeparator(Orientation orientation) {
        getStyleClass().add("separator");

        // Listen for orientation changes to update style classes
        orientationProperty().addListener((obs, oldVal, newVal) -> {
            getStyleClass().removeAll("separator-horizontal", "separator-vertical");
            if (newVal == javafx.geometry.Orientation.VERTICAL) {
                getStyleClass().add("separator-vertical");
            } else {
                getStyleClass().add("separator-horizontal");
            }
        });

        setOrientation(orientation == Orientation.VERTICAL ? javafx.geometry.Orientation.VERTICAL
                : javafx.geometry.Orientation.HORIZONTAL);
    }
}
