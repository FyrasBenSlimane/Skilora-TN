package com.skilora.framework.components;

import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/**
 * TLToggleGroup - shadcn/ui Toggle Group (group of ToggleButtons).
 * HBox of TLToggle with shared ToggleGroup; theme-adaptive.
 */
public class TLToggleGroup extends HBox {

    private final ToggleGroup group;

    public TLToggleGroup() {
        getStyleClass().add("toggle-group");
        setSpacing(0);
        group = new ToggleGroup();
    }

    public TLToggle addToggle(String text) {
        TLToggle toggle = new TLToggle(text);
        toggle.setToggleGroup(group);
        getChildren().add(toggle);
        return toggle;
    }

    public ToggleGroup getToggleGroup() {
        return group;
    }

    public TLToggle getSelected() {
        return (TLToggle) group.getSelectedToggle();
    }
}
