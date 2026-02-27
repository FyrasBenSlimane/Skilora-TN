package com.skilora.framework.components;

import javafx.scene.control.ProgressBar;

public class TLProgress extends ProgressBar {
    public TLProgress() { super(); getStyleClass().add("tl-progress"); }
    public TLProgress(double progress) { super(progress); getStyleClass().add("tl-progress"); }
}
