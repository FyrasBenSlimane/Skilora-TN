package com.skilora.framework.layouts;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * TLWindow - Generic Window Shell
 * 
 * Provides custom title bar and window controls (Min, Max, Close).
 * Wraps any content.
 */
public class TLWindow extends BorderPane {

    private final HBox topBar;
    private double xOffset = 0;
    private double yOffset = 0;

    public TLWindow(Stage stage, String title, Node content) {
        getStyleClass().addAll("app-layout", "custom-window");
        setPadding(new Insets(0));

        topBar = new HBox(8);
        topBar.getStyleClass().add("app-topbar");
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 16, 0, 16));
        
        // Drag Logic
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        topBar.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // Add Spacer to push controls to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        topBar.getChildren().add(spacer);

        // Setup Controls
        setupWindowControls(stage);

        setTop(topBar);
        setCenter(content);
        
        // Add content padding
        if (content != null) {
            // Apply a default background to center if needed?
            // content.setStyle("-fx-background-color: -color-bg-default;");
        }
    }

    private void setupWindowControls(Stage stage) {
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Button btnMin = createWindowButton("min", "M4 11h8v1H4z"); 
        btnMin.setOnAction(e -> stage.setIconified(true));

        Button btnClose = createWindowButton("close", "M 4.7 3.3 L 3.3 4.7 L 6.6 8 L 3.3 11.3 L 4.7 12.7 L 8 9.4 L 11.3 12.7 L 12.7 11.3 L 9.4 8 L 12.7 4.7 L 11.3 3.3 L 8 6.6 L 4.7 3.3 z");
        btnClose.getStyleClass().add("window-control-close");
        btnClose.setOnAction(e -> stage.close());

        controls.getChildren().addAll(btnMin, btnClose);
        topBar.getChildren().add(controls);
    }

    private Button createWindowButton(String type, String svgContent) {
        Button btn = new Button();
        btn.getStyleClass().add("window-control-btn");
        
        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.getStyleClass().add("svg-path");
        // Ensure icon has a color (inheritance from button text fill usually works, but explicit ensures visibility)
        icon.setStyle("-fx-fill: -fx-muted-foreground;"); 
        btn.setGraphic(icon);
        
        return btn;
    }
}
