package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * TLInputGroup - shadcn/ui Input Group (prefix + input + suffix).
 * Single row; prefix/suffix are styled labels or nodes; middle is the text field.
 */
public class TLInputGroup extends HBox {

    private final TextField input;
    private final HBox prefixBox;
    private final HBox suffixBox;

    public TLInputGroup() {
        getStyleClass().add("input-group");
        setSpacing(0);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        prefixBox = new HBox();
        prefixBox.getStyleClass().add("input-group-prefix");
        prefixBox.setVisible(false);
        prefixBox.setManaged(false);

        input = new TextField();
        input.getStyleClass().addAll("text-input", "input-group-input");

        suffixBox = new HBox();
        suffixBox.getStyleClass().add("input-group-suffix");
        suffixBox.setVisible(false);
        suffixBox.setManaged(false);

        getChildren().addAll(prefixBox, input, suffixBox);
    }

    public TextField getInput() {
        return input;
    }

    public void setPrefix(String text) {
        prefixBox.getChildren().clear();
        if (text != null && !text.isEmpty()) {
            Label l = new Label(text);
            prefixBox.getChildren().add(l);
            prefixBox.setVisible(true);
            prefixBox.setManaged(true);
        } else {
            prefixBox.setVisible(false);
            prefixBox.setManaged(false);
        }
    }

    public void setPrefix(Node node) {
        prefixBox.getChildren().clear();
        if (node != null) {
            prefixBox.getChildren().add(node);
            prefixBox.setVisible(true);
            prefixBox.setManaged(true);
        } else {
            prefixBox.setVisible(false);
            prefixBox.setManaged(false);
        }
    }

    public void setSuffix(String text) {
        suffixBox.getChildren().clear();
        if (text != null && !text.isEmpty()) {
            Label l = new Label(text);
            suffixBox.getChildren().add(l);
            suffixBox.setVisible(true);
            suffixBox.setManaged(true);
        } else {
            suffixBox.setVisible(false);
            suffixBox.setManaged(false);
        }
    }

    public void setSuffix(Node node) {
        suffixBox.getChildren().clear();
        if (node != null) {
            suffixBox.getChildren().add(node);
            suffixBox.setVisible(true);
            suffixBox.setManaged(true);
        } else {
            suffixBox.setVisible(false);
            suffixBox.setManaged(false);
        }
    }
}
