package com.skilora.framework.components;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;

/**
 * TLSeparator - shadcn/ui Separator for JavaFX.
 *
 * Thin wrapper around JavaFX Separator that applies theme-consistent styling.
 * Supports horizontal (default) and vertical orientations.
 *
 * CSS classes: .separator, .separator > .line
 *
 * Usage:
 *   content.getChildren().add(new TLSeparator());                    // horizontal
 *   toolbar.getChildren().add(new TLSeparator(Orientation.VERTICAL)); // vertical
 */
public class TLSeparator extends Separator {

    public TLSeparator() {
        this(Orientation.HORIZONTAL);
    }

    public TLSeparator(Orientation orientation) {
        super(orientation);
        getStyleClass().add("separator");
    }
}
