package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * TLPasswordField - shadcn/ui Password Input
 */
public class TLPasswordField extends VBox {

    private final PasswordField input;
    private final Label labelNode;
    private final Label helperNode;

    public TLPasswordField() {
        this("", "", "");
    }

    public TLPasswordField(String label, String placeholder) {
        this(label, placeholder, "");
    }

    public TLPasswordField(String label, String placeholder, String helperText) {
        getStyleClass().add("form-field");
        setSpacing(8);

        labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        getChildren().add(labelNode);

        input = new PasswordField();
        input.setPromptText(placeholder);
        input.getStyleClass().add("text-input");
        getChildren().add(input);

        helperNode = new Label(helperText);
        helperNode.getStyleClass().add("form-helper");
        helperNode.setVisible(!helperText.isEmpty());
        helperNode.setManaged(!helperText.isEmpty());
        getChildren().add(helperNode);
    }

    public String getText() {
        return input.getText();
    }

    // FXML Property Getters and Setters
    public String getLabel() {
        return labelNode.getText();
    }

    public void setLabel(String label) {
        labelNode.setText(label);
        boolean hasLabel = label != null && !label.isEmpty();
        labelNode.setVisible(hasLabel);
        labelNode.setManaged(hasLabel);
    }

    public String getPromptText() {
        return input.getPromptText();
    }

    public void setPromptText(String promptText) {
        input.setPromptText(promptText);
    }

    public String getHelperText() {
        return helperNode.getText();
    }

    public void setHelperText(String text) {
        helperNode.setText(text);
        boolean hasText = text != null && !text.isEmpty();
        helperNode.setVisible(hasText);
        helperNode.setManaged(hasText);
    }

    public PasswordField getControl() {
        return input;
    }
}
