package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TLEmpty extends VBox {
    public TLEmpty() { super(); getChildren().add(new Label("Aucun élément")); getStyleClass().add("tl-empty"); }
    public TLEmpty(String message) { super(); getChildren().add(new Label(message)); getStyleClass().add("tl-empty"); }
}
