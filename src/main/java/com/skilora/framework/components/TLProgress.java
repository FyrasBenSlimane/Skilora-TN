package com.skilora.framework.components;

import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * TLProgress - shadcn/ui Progress (CSS-only, theme-adaptive)
 *
 * Uses .progress; optional .progress-sm, .progress-lg. Value 0–1 or indeterminate.
 * Reference: https://ui.shadcn.com/docs/components/progress
 */
public class TLProgress extends VBox {

    private final ProgressBar bar;

    public TLProgress() {
        this(0);
    }

    public TLProgress(double progress) {
        getStyleClass().add("progress");
        setMaxWidth(Double.MAX_VALUE);

        bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(bar);
    }

    public ProgressBar getProgressBar() {
        return bar;
    }

    public double getProgress() {
        return bar.getProgress();
    }

    public void setProgress(double value) {
        bar.setProgress(value);
    }

    public javafx.beans.property.DoubleProperty progressProperty() {
        return bar.progressProperty();
    }

    public void setIndeterminate(boolean indeterminate) {
        bar.setProgress(indeterminate ? -1 : (bar.getProgress() < 0 ? 0 : bar.getProgress()));
    }

    /** Add .progress-sm for smaller height. */
    public void setSmall(boolean small) {
        if (small) {
            if (!getStyleClass().contains("progress-sm")) getStyleClass().add("progress-sm");
        } else {
            getStyleClass().remove("progress-sm");
        }
    }

    /** Add .progress-lg for larger height. */
    public void setLarge(boolean large) {
        if (large) {
            if (!getStyleClass().contains("progress-lg")) getStyleClass().add("progress-lg");
        } else {
            getStyleClass().remove("progress-lg");
        }
    }
}
