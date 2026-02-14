package com.skilora.framework.components;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * TLRadioGroup - shadcn/ui Radio Group (theme-adaptive).
 * VBox with ToggleGroup + RadioButtons; each item gets .radio-group-item, .radio-button, .radio-label.
 */
public class TLRadioGroup extends VBox {

    private final ToggleGroup group;

    public TLRadioGroup() {
        getStyleClass().add("radio-group");
        setSpacing(12);
        group = new ToggleGroup();
    }

    public RadioButton addOption(String label) {
        RadioButton rb = new RadioButton(label);
        rb.setToggleGroup(group);
        rb.getStyleClass().addAll("radio-group-item", "radio-button");
        rb.getStyleClass().add("radio-label"); // label is the text, styled via .radio-button .label
        getChildren().add(rb);
        return rb;
    }

    public ToggleGroup getToggleGroup() {
        return group;
    }

    public RadioButton getSelected() {
        return (RadioButton) group.getSelectedToggle();
    }

    public void setSelected(int index) {
        if (index >= 0 && index < getChildren().size() && getChildren().get(index) instanceof RadioButton rb)
            rb.setSelected(true);
    }
}
