package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class TLCard extends VBox {
    public TLCard() {
        super();
        getStyleClass().add("tl-card");
        setSpacing(12);
    }

    /** Set the card body content (replaces existing children). */
    public void setBody(Node content) {
        getChildren().clear();
        if (content != null) getChildren().add(content);
    }
}
