package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class TLToast extends HBox {
    public TLToast() { super(); getChildren().add(new Label("")); getStyleClass().add("tl-toast"); }
    public TLToast(String message) { super(); getChildren().add(new Label(message)); getStyleClass().add("tl-toast"); }
}
