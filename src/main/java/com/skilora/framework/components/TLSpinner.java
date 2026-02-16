package com.skilora.framework.components;

import javafx.animation.RotateTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

/**
 * TLSpinner - shadcn/ui loading spinner for JavaFX.
 *
 * Animated spinning arc indicator. Replaces all duplicated createLoadingIndicator() 
 * methods across controllers.
 *
 * CSS class: .spinner
 *
 * Usage:
 *   TLSpinner spinner = new TLSpinner();           // 24px default
 *   TLSpinner spinner = new TLSpinner(Size.LG);    // 48px large
 */
public class TLSpinner extends StackPane {

    public enum Size {
        SM(16, 2), DEFAULT(24, 2.5), LG(48, 3);

        final double diameter;
        final double strokeWidth;

        Size(double diameter, double strokeWidth) {
            this.diameter = diameter;
            this.strokeWidth = strokeWidth;
        }
    }

    private final RotateTransition rotateTransition;

    public TLSpinner() {
        this(Size.DEFAULT);
    }

    public TLSpinner(Size size) {
        getStyleClass().add("spinner");

        double radius = size.diameter / 2;
        Arc arc = new Arc(0, 0, radius, radius, 0, 270);
        arc.getStyleClass().add("spinner-arc");
        arc.setFill(null);
        arc.setStroke(javafx.scene.paint.Color.web("#a1a1aa")); // muted-foreground fallback
        arc.setStrokeWidth(size.strokeWidth);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setStrokeType(StrokeType.CENTERED);

        // Apply theme color via CSS lookup once scene is available
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.scene.paint.Paint color = newScene.getRoot().lookup(".spinner-arc") != null
                        ? null : null;
                // Use CSS pseudo-class compatible approach: bind stroke to theme token
                try {
                    arc.setStroke((javafx.scene.paint.Color) newScene.getRoot().
                            getProperties().getOrDefault("-fx-muted-foreground",
                                    javafx.scene.paint.Color.web("#a1a1aa")));
                } catch (Exception ignored) { /* fallback already set */ }
            }
        });

        setMinSize(size.diameter, size.diameter);
        setPrefSize(size.diameter, size.diameter);
        setMaxSize(size.diameter, size.diameter);

        getChildren().add(arc);

        // Infinite rotation animation
        rotateTransition = new RotateTransition(Duration.millis(1000), arc);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        rotateTransition.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rotateTransition.play();

        // Stop animation when not visible (performance)
        visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            if (isVisible) rotateTransition.play();
            else rotateTransition.stop();
        });
    }

    /**
     * Create a centered spinner wrapped in a StackPane for easy placement.
     */
    public static StackPane createCentered() {
        return createCentered(Size.DEFAULT);
    }

    public static StackPane createCentered(Size size) {
        TLSpinner spinner = new TLSpinner(size);
        StackPane wrapper = new StackPane(spinner);
        wrapper.setMinHeight(100);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER);
        return wrapper;
    }
}
