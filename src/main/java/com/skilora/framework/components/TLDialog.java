package com.skilora.framework.components;

import com.skilora.framework.layouts.TLAppLayout;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

/**
 * TLDialog - Modern styled dialog matching shadcn/ui design.
 * Features: close button, ESC handling, scrollable content, dynamic sizing,
 * backdrop overlay, theme-aware via CSS.
 */
public class TLDialog<R> extends Dialog<R> {

    private static final String STYLESHEET =
            TLDialog.class.getResource("/com/skilora/framework/styles/tl-dialog.css").toExternalForm();
    private static final String STYLESHEET_DEP_0 =
            TLDialog.class.getResource("/com/skilora/framework/styles/tl-button.css").toExternalForm();

    private final VBox contentBox;
    private final VBox headerBox;
    private final VBox mainContainer;
    private final ScrollPane scrollWrapper;
    private final Button closeBtn;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean closable = true;

    public TLDialog() {
        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(STYLESHEET);
        pane.getStylesheets().add(STYLESHEET_DEP_0);
        initStyle(javafx.stage.StageStyle.TRANSPARENT);

        // Ensure theme tokens from theme.css apply (they're defined under `.root`).
        if (!pane.getStyleClass().contains("root")) {
            pane.getStyleClass().add("root");
        }
        pane.getStyleClass().add("tl-dialog-pane");
        pane.setHeaderText(null);
        pane.setGraphic(null);

        TLAppLayout.applyStylesheets(pane);

        // Header row with title area + close button
        headerBox = new VBox(6);
        headerBox.getStyleClass().add("dialog-header");
        headerBox.setPickOnBounds(false);

        closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().addAll("dialog-close-btn", "btn", "btn-ghost");
        closeBtn.setOnAction(e -> closeDialog());

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.TOP_RIGHT);
        headerRow.getStyleClass().add("dialog-header-row");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox.setHgrow(headerBox, Priority.ALWAYS);
        headerRow.getChildren().addAll(headerBox, headerSpacer, closeBtn);

        // Scrollable content area
        contentBox = new VBox(16);
        contentBox.getStyleClass().add("dialog-content");
        contentBox.setPadding(new Insets(0, 24, 16, 24));

        scrollWrapper = new ScrollPane(contentBox);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.getStyleClass().add("dialog-scroll");

        // Main container
        mainContainer = new VBox(0);
        mainContainer.getStyleClass().add("dialog");
        mainContainer.getChildren().addAll(headerRow, scrollWrapper);

        pane.setContent(mainContainer);

        // JavaFX may add 'content' class to the content node, which can conflict
        // with .dialog-pane > .content transparent rule. Remove it.
        mainContainer.getStyleClass().remove("content");

        // ESC key handling
        pane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE && closable) {
                closeDialog();
                event.consume();
            }
        });

        // Dragging via header
        headerRow.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        headerRow.setOnMouseDragged(event -> {
            if (pane.getScene() != null && pane.getScene().getWindow() != null) {
                pane.getScene().getWindow().setX(event.getScreenX() - xOffset);
                pane.getScene().getWindow().setY(event.getScreenY() - yOffset);
            }
        });

        // Dynamic sizing: limit to 80% of screen height
        setOnShowing(e -> {
            styleButtons();

            // Strip 'content' class again — JavaFX may re-add it during layout
            mainContainer.getStyleClass().remove("content");

            if (getDialogPane().getScene() != null) {
                javafx.scene.Scene dialogScene = getDialogPane().getScene();
                dialogScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

                // Apply stylesheets at SCENE level (not just DialogPane)
                // so CSS variables resolve everywhere in the scene graph
                for (String ss : getDialogPane().getStylesheets()) {
                    if (!dialogScene.getStylesheets().contains(ss)) {
                        dialogScene.getStylesheets().add(ss);
                    }
                }

                javafx.scene.Parent dialogRoot = dialogScene.getRoot();
                if (dialogRoot != null && !dialogRoot.getStyleClass().contains("root")) {
                    dialogRoot.getStyleClass().add("root");
                }
                applyTheme();

                javafx.application.Platform.runLater(() -> {
                    // Force CSS reapply to resolve theme tokens
                    getDialogPane().applyCss();

                    double screenH = Screen.getPrimary().getVisualBounds().getHeight();
                    double maxH = screenH * 0.8;
                    scrollWrapper.setMaxHeight(maxH - 150);
                    scrollWrapper.setPrefHeight(Region.USE_COMPUTED_SIZE);

                    if (dialogScene.getWindow() != null) {
                        dialogScene.getWindow().sizeToScene();
                    }
                });
            }
        });
    }

    private void closeDialog() {
        if (getDialogPane().getButtonTypes().isEmpty()) {
            getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }

        if (getDialogPane().getButtonTypes().contains(ButtonType.CANCEL)) {
            setResult(null);
            close();
        } else if (getDialogPane().getButtonTypes().contains(ButtonType.CLOSE)) {
            setResult(null);
            close();
        } else if (getDialogPane().getButtonTypes().contains(ButtonType.OK)) {
            setResult(null);
            close();
        } else {
            setResult(null);
            close();
        }
    }

    private void applyTheme() {
        if (getOwner() != null && getOwner().getScene() != null) {
            javafx.scene.Parent ownerRoot = getOwner().getScene().getRoot();
            javafx.scene.Parent dialogRoot = getDialogPane().getScene().getRoot();
            boolean isLight = ownerRoot.getStyleClass().contains("light");

            if (isLight) {
                if (!dialogRoot.getStyleClass().contains("light"))
                    dialogRoot.getStyleClass().add("light");
                if (!getDialogPane().getStyleClass().contains("light"))
                    getDialogPane().getStyleClass().add("light");
                if (!mainContainer.getStyleClass().contains("light"))
                    mainContainer.getStyleClass().add("light");
            } else {
                dialogRoot.getStyleClass().remove("light");
                getDialogPane().getStyleClass().remove("light");
                mainContainer.getStyleClass().remove("light");
            }
        }
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

    /**
     * Set content nodes inside the scrollable area.
     * Use this instead of getDialogPane().setContent() to maintain the dialog layout.
     */
    public void setContent(Node... nodes) {
        contentBox.getChildren().setAll(nodes);
    }

    /**
     * Set a single content node (convenience for form-based dialogs).
     * Replaces scrollable content area.
     */
    public void setDialogContent(Node content) {
        contentBox.getChildren().setAll(content);
    }

    public VBox getContentBox() {
        return contentBox;
    }

    public void addButton(ButtonType type) {
        getDialogPane().getButtonTypes().add(type);
    }

    public void setClosable(boolean closable) {
        this.closable = closable;
        closeBtn.setVisible(closable);
        closeBtn.setManaged(closable);
    }

    /** Style buttons after they've been added. */
    public void styleButtons() {
        DialogPane pane = getDialogPane();
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(buttonType);
            if (button != null) {
                button.setStyle(null);
                button.setGraphic(null);
                if (!button.getStyleClass().contains("btn"))
                    button.getStyleClass().add("btn");

                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                    buttonType.getButtonData() == ButtonBar.ButtonData.APPLY ||
                    buttonType.getButtonData() == ButtonBar.ButtonData.YES) {
                    if (!button.getStyleClass().contains("btn-primary"))
                        button.getStyleClass().add("btn-primary");
                } else {
                    if (!button.getStyleClass().contains("btn-outline"))
                        button.getStyleClass().add("btn-outline");
                }
            }
        }
    }

    /** Creates a styled form section with a title and fields. */
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
