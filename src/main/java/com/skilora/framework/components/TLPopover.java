package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * TLPopover - shadcn/ui Popover (floating content panel).
 * Uses Popup; caller positions and shows/hides.
 */
public class TLPopover extends Popup {

    private final VBox contentBox;

    public TLPopover() {
        contentBox = new VBox();
        contentBox.getStyleClass().add("popover");
        contentBox.setSpacing(8);
        getContent().add(contentBox);
    }

    public void setContent(Node... nodes) {
        contentBox.getChildren().setAll(nodes);
    }

    public VBox getContentBox() {
        return contentBox;
    }

    public void show(Node anchor) {
        if (anchor.getScene() != null && anchor.getScene().getWindow() != null) {
            javafx.geometry.Bounds b = anchor.localToScene(anchor.getLayoutBounds());
            javafx.stage.Window w = anchor.getScene().getWindow();
            show(anchor, w.getX() + b.getMinX(), w.getY() + b.getMaxY());
        }
    }

    public void show(Node anchor, double x, double y) {
        if (anchor.getScene() != null && anchor.getScene().getWindow() != null) {
            setX(x);
            setY(y);
            show(anchor.getScene().getWindow());
        }
    }
}
