package com.skilora.framework.components;

import javafx.scene.control.ProgressIndicator;

/**
 * TLSpinner - shadcn/ui Spinner (indeterminate loading indicator).
 */
public class TLSpinner extends ProgressIndicator {

    public TLSpinner() {
        setProgress(-1);
        getStyleClass().add("spinner");
    }
}
