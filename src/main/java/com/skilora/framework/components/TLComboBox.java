package com.skilora.framework.components;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLComboBox - Editable ComboBox with optional label (theme-adaptive).
 * Used in finance and other forms; supports FXML label and promptText.
 */
public class TLComboBox<T> extends VBox {

    private final ComboBox<T> comboBox;
    private final Label labelNode;

    public TLComboBox() {
        this("");
    }

    public TLComboBox(String label) {
        getStyleClass().add("form-field");
        setSpacing(8);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        if (label == null || label.isEmpty()) {
            labelNode.setVisible(false);
            labelNode.setManaged(false);
        }
        getChildren().add(labelNode);

        comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.getStyleClass().addAll("combobox", "combobox-trigger");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(comboBox);
    }

    public ComboBox<T> getComboBox() {
        return comboBox;
    }

    public ObservableList<T> getItems() {
        return comboBox.getItems();
    }

    public void setItems(ObservableList<T> items) {
        comboBox.setItems(items);
    }

    public String getLabel() {
        return labelNode.getText();
    }

    public void setLabel(String label) {
        labelNode.setText(label);
        boolean hasLabel = label != null && !label.isEmpty();
        labelNode.setVisible(hasLabel);
        labelNode.setManaged(hasLabel);
    }

    public T getValue() {
        return comboBox.getValue();
    }

    public void setValue(T value) {
        comboBox.setValue(value);
    }

    public javafx.beans.property.ObjectProperty<T> valueProperty() {
        return comboBox.valueProperty();
    }

    public String getPromptText() {
        return comboBox.getPromptText();
    }

    public void setPromptText(String promptText) {
        comboBox.setPromptText(promptText);
    }
}
