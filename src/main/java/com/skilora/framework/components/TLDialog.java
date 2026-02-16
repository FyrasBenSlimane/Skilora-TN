package com.skilora.framework.components;

import com.skilora.framework.layouts.TLAppLayout;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * TLDialog - Modern styled dialog matching shadcn/ui design.
 * Features clean header, styled content area, and proper button styling.
 * Fully theme-aware via CSS.
 */
public class TLDialog<R> extends Dialog<R> {

    private final VBox contentBox;
    private final VBox headerBox;
    private final VBox mainContainer;

    // Drag offsets
    private double xOffset = 0;
    private double yOffset = 0;

    public TLDialog() {
        initStyle(javafx.stage.StageStyle.TRANSPARENT);

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("tl-dialog-pane");
        pane.setHeaderText(null);
        pane.setGraphic(null);

        // Ensure Stylesheets are applied
        TLAppLayout.applyStylesheets(pane);

        // Header with close button
        headerBox = new VBox(6);
        headerBox.getStyleClass().add("dialog-header");
        headerBox.setPickOnBounds(false); // Only capture clicks on actual header content

        // Content area
        contentBox = new VBox(20);
        contentBox.getStyleClass().add("dialog-content");
        contentBox.setPickOnBounds(false); // Don't block clicks through empty space

        // Main container
        mainContainer = new VBox(0);
        mainContainer.getStyleClass().add("dialog");
        mainContainer.getChildren().addAll(headerBox, contentBox);
        mainContainer.setPickOnBounds(false); // Don't block clicks through empty space

        pane.setContent(mainContainer);

        // Add a hidden CLOSE button type so dialog.close() works with custom buttons
        pane.getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = pane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setManaged(false);
            closeButton.setVisible(false);
        }

        // Window Dragging Logic
        headerBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        headerBox.setOnMouseDragged(event -> {
            if (pane.getScene() != null && pane.getScene().getWindow() != null) {
                pane.getScene().getWindow().setX(event.getScreenX() - xOffset);
                pane.getScene().getWindow().setY(event.getScreenY() - yOffset);
            }
        });

        // Style buttons when dialog is about to be shown
        setOnShowing(e -> {
            styleButtons();

            // Ensure scene fill is transparent for the undecorated look
            if (getDialogPane().getScene() != null) {
                getDialogPane().getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

                // Inherit theme from owner window → apply to scene root for CSS var resolution
                if (getOwner() != null && getOwner().getScene() != null) {
                    javafx.scene.Parent ownerRoot = getOwner().getScene().getRoot();
                    javafx.scene.Parent dialogRoot = getDialogPane().getScene().getRoot();
                    boolean isLight = ownerRoot.getStyleClass().contains("light");

                    // Apply/remove light class on scene root (where .root vars are resolved)
                    if (isLight) {
                        if (!dialogRoot.getStyleClass().contains("light")) {
                            dialogRoot.getStyleClass().add("light");
                        }
                    } else {
                        dialogRoot.getStyleClass().remove("light");
                    }

                    // Also apply to our containers for specificity
                    if (isLight) {
                        if (!pane.getStyleClass().contains("light")) {
                            pane.getStyleClass().add("light");
                        }
                        if (!mainContainer.getStyleClass().contains("light")) {
                            mainContainer.getStyleClass().add("light");
                        }
                    } else {
                        pane.getStyleClass().remove("light");
                        mainContainer.getStyleClass().remove("light");
                    }
                }
            }
        });
    }

    /** Set dialog header title. */
    public void setDialogTitle(String title) {
        headerBox.getChildren().removeIf(n -> n.getStyleClass().contains("dialog-title"));

        Label t = new Label(title);
        t.getStyleClass().add("dialog-title");
        t.setWrapText(true);

        if (headerBox.getChildren().isEmpty()) {
            headerBox.getChildren().add(t);
        } else {
            headerBox.getChildren().add(0, t);
        }
    }

    public void setDescription(String description) {
        if (description != null && !description.isEmpty()) {
            headerBox.getChildren().removeIf(n -> n.getStyleClass().contains("dialog-description"));

            Label d = new Label(description);
            d.getStyleClass().add("dialog-description");
            d.setWrapText(true);
            headerBox.getChildren().add(d);
        }
    }

    public void setContent(Node... nodes) {
        contentBox.getChildren().setAll(nodes);
    }

    public VBox getContentBox() {
        return contentBox;
    }

    public void addButton(ButtonType type) {
        getDialogPane().getButtonTypes().add(type);
    }

    /**
     * Style the buttons after they've been added to the dialog.
     */
    public void styleButtons() {
        DialogPane pane = getDialogPane();

        // Style individual buttons
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(buttonType);
            if (button != null) {
                // Remove default styling artifacts if any
                button.setStyle(null); // Clear inline styles
                button.setGraphic(null);

                // Add base class
                if (!button.getStyleClass().contains("btn")) {
                    button.getStyleClass().add("btn");
                }

                // Add variant class
                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                        buttonType.getButtonData() == ButtonBar.ButtonData.APPLY ||
                        buttonType.getButtonData() == ButtonBar.ButtonData.YES) {
                    if (!button.getStyleClass().contains("btn-primary")) {
                        button.getStyleClass().add("btn-primary");
                    }
                } else {
                    if (!button.getStyleClass().contains("btn-outline")) {
                        button.getStyleClass().add("btn-outline");
                    }
                }
            }
        }
    }

    /**
     * Creates a styled form section with a title and fields.
     */
    public VBox createFormSection(String sectionTitle, Node... fields) {
        VBox section = new VBox(14);
        section.getStyleClass().add("dialog-form-section");
        section.setPadding(new Insets(0, 0, 16, 0));

        Label title = new Label(sectionTitle);
        title.getStyleClass().add("dialog-section-title");

        VBox fieldsBox = new VBox(14);
        fieldsBox.getChildren().addAll(fields);

        section.getChildren().addAll(title, fieldsBox);
        return section;
    }
}
