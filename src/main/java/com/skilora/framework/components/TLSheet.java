package com.skilora.framework.components;

import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * TLSheet - shadcn/ui Sheet (panel sliding from edge, like Drawer).
 * Typically bottom or side; show/hide with optional animation.
 */
public class TLSheet extends VBox {

    public enum Side { LEFT, RIGHT, TOP, BOTTOM }

    private final Side side;
    private final VBox contentBox;
    private TranslateTransition transition;

    public TLSheet(Side side) {
        this.side = side;
        getStyleClass().addAll("sheet", "sheet-content");
        getStyleClass().add("sheet-" + side.name().toLowerCase());
        contentBox = new VBox();
        contentBox.getStyleClass().add("sheet-content");
        contentBox.setSpacing(16);
        getChildren().add(contentBox);
        setVisible(false);
        setManaged(false);
    }

    public TLSheet() {
        this(Side.BOTTOM);
    }

    public void setContent(Node... nodes) {
        contentBox.getChildren().setAll(nodes);
    }

    public VBox getContentBox() {
        return contentBox;
    }

    public void show() {
        setVisible(true);
        setManaged(true);
        if (transition != null) transition.stop();
        transition = createTransition(1.0);
        if (transition != null) transition.play();
    }

    public void hide() {
        if (transition != null) transition.stop();
        transition = createTransition(0.0);
        if (transition != null) {
            transition.setOnFinished(e -> {
                setVisible(false);
                setManaged(false);
            });
            transition.play();
        } else {
            setVisible(false);
            setManaged(false);
        }
    }

    private TranslateTransition createTransition(double toValue) {
        TranslateTransition t = new TranslateTransition(Duration.millis(200), this);
        boolean show = toValue >= 1.0;
        switch (side) {
            case LEFT -> { t.setFromX(show ? -getWidth() : 0); t.setToX(show ? 0 : -getWidth()); }
            case RIGHT -> { t.setFromX(show ? getWidth() : 0); t.setToX(show ? 0 : getWidth()); }
            case TOP -> { t.setFromY(show ? -getHeight() : 0); t.setToY(show ? 0 : -getHeight()); }
            case BOTTOM -> { t.setFromY(show ? getHeight() : 0); t.setToY(show ? 0 : getHeight()); }
        }
        return t;
    }
}
