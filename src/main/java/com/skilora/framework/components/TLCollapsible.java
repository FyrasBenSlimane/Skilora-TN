package com.skilora.framework.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * TLCollapsible - shadcn/ui Collapsible (single expand/collapse section).
 * Trigger button toggles visibility of content with optional animation.
 */
public class TLCollapsible extends VBox {

    private final BooleanProperty open = new SimpleBooleanProperty(false);
    private final Button trigger;
    private final VBox contentBox;

    public TLCollapsible(String triggerText) {
        getStyleClass().add("collapsible");
        setSpacing(0);

        trigger = new Button(triggerText);
        trigger.getStyleClass().add("collapsible-trigger");
        trigger.setMaxWidth(Double.MAX_VALUE);
        trigger.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        contentBox = new VBox();
        contentBox.getStyleClass().add("collapsible-content");
        contentBox.setVisible(false);
        contentBox.setManaged(false);

        trigger.setOnAction(e -> setOpen(!isOpen()));

        open.addListener((o, oldVal, newVal) -> {
            contentBox.setVisible(newVal);
            contentBox.setManaged(newVal);
        });

        getChildren().addAll(trigger, contentBox);
    }

    public boolean isOpen() {
        return open.get();
    }

    public void setOpen(boolean open) {
        this.open.set(open);
    }

    public BooleanProperty openProperty() {
        return open;
    }

    public Button getTrigger() {
        return trigger;
    }

    public VBox getContent() {
        return contentBox;
    }

    public void setContent(javafx.scene.Node... nodes) {
        contentBox.getChildren().clear();
        contentBox.getChildren().addAll(nodes);
    }
}
