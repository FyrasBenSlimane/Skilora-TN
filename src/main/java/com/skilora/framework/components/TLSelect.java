package com.skilora.framework.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLSelect - shadcn/ui Select / Native Select (CSS-only, theme-adaptive)
 *
 * Optional label + ComboBox. Uses .select, .select-label, .select-trigger.
 * Reference: https://ui.shadcn.com/docs/components/select
 */
public final class TLSelect<T> extends VBox {

    private final Label labelNode;
    private final ComboBox<T> combo;

    public TLSelect() {
        this("", FXCollections.<T>observableArrayList());
    }

    public TLSelect(String label) {
        this(label, FXCollections.<T>observableArrayList());
    }

    public TLSelect(String label, ObservableList<T> items) {
        getStyleClass().add("select");
        setSpacing(8);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("select-label");
        getChildren().add(labelNode);

        combo = new ComboBox<>(items);
        combo.getStyleClass().add("select-trigger");
        combo.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(combo);
    }

    @SafeVarargs
    public TLSelect(String label, T... items) {
        this(label, FXCollections.observableArrayList(items));
    }

    public void setLabel(String label) {
        labelNode.setText(label);
    }

    public String getLabel() {
        return labelNode.getText();
    }

    public ComboBox<T> getComboBox() {
        return combo;
    }

    public T getValue() {
        return combo.getValue();
    }

    public void setValue(T value) {
        combo.setValue(value);
    }

    public javafx.beans.property.ObjectProperty<T> valueProperty() {
        return combo.valueProperty();
    }

    public ObservableList<T> getItems() {
        return combo.getItems();
    }

    public String getPromptText() {
        return combo.getPromptText();
    }

    public void setPromptText(String text) {
        combo.setPromptText(text);
    }
}
