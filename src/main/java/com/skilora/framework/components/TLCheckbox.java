package com.skilora.framework.components;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLCheckbox - shadcn/ui Checkbox for JavaFX.
 *
 * Wraps JavaFX CheckBox with optional description text, matching shadcn styling.
 * The underlying CheckBox is already styled by components.css (.check-box).
 *
 * Usage:
 *   TLCheckbox cb = new TLCheckbox("Accept terms");
 *   TLCheckbox cb = new TLCheckbox("Enable notifications", "You'll receive email alerts");
 *   cb.isChecked(); cb.setChecked(true);
 */
public class TLCheckbox extends VBox {

    private final CheckBox checkBox;
    private Label helperLabel;

    public TLCheckbox(String text) {
        this(text, null);
    }

    public TLCheckbox(String text, String helperText) {
        setSpacing(2);

        checkBox = new CheckBox(text);
        getChildren().add(checkBox);

        if (helperText != null && !helperText.isEmpty()) {
            helperLabel = new Label(helperText);
            helperLabel.getStyleClass().add("checkbox-helper");
            helperLabel.setWrapText(true);
            // Indent to align with text after the box
            helperLabel.setPadding(new javafx.geometry.Insets(0, 0, 0, 24));
            getChildren().add(helperLabel);
        }
    }

    public boolean isChecked() {
        return checkBox.isSelected();
    }

    public void setChecked(boolean checked) {
        checkBox.setSelected(checked);
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public javafx.beans.property.BooleanProperty checkedProperty() {
        return checkBox.selectedProperty();
    }

    /**
     * Mark the checkbox as errored (destructive border).
     */
    public void setError(boolean error) {
        if (error) {
            if (!getStyleClass().contains("checkbox-error")) {
                getStyleClass().add("checkbox-error");
            }
        } else {
            getStyleClass().remove("checkbox-error");
        }
    }
}
