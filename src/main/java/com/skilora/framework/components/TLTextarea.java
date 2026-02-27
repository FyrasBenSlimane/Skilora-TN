package com.skilora.framework.components;

import javafx.scene.control.TextArea;

public class TLTextarea extends TextArea {
    public TLTextarea() {
        super();
        getStyleClass().add("tl-textarea");
    }

    public void setTextAreaHeight(double height) {
        setPrefHeight(height);
        setMinHeight(height);
    }

    public void setTextAreaRowCount(int rows) {
        setPrefRowCount(rows);
    }
}
