package com.skilora.framework.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * TLToast - shadcn/ui Toast notification popup for JavaFX.
 *
 * Shows a transient notification at the bottom-right of the scene.
 * Auto-dismisses after a configurable duration.
 *
 * CSS classes: .toast
 *
 * Usage:
 *   TLToast.success(scene, "Saved", "Your changes have been saved.");
 *   TLToast.error(scene, "Error", "Something went wrong.");
 *   TLToast.info(scene, "Info", "New notification received.");
 *   TLToast.show(scene, "Title", "Description", Duration.seconds(5));
 */
public class TLToast extends VBox {

    private static final Duration DEFAULT_DURATION = Duration.seconds(4);
    private static final double TOAST_WIDTH = 360;

    public enum Variant {
        DEFAULT, SUCCESS, DESTRUCTIVE, WARNING, INFO
    }

    private TLToast(String title, String description, Variant variant) {
        getStyleClass().add("toast");
        if (variant != Variant.DEFAULT) {
            getStyleClass().add("toast-" + variant.name().toLowerCase());
        }
        setPrefWidth(TOAST_WIDTH);
        setMaxWidth(TOAST_WIDTH);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("alert-title");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("alert-description");
        descLabel.setWrapText(true);

        getChildren().addAll(titleLabel, descLabel);
    }

    // ---- Static factory methods ----

    public static void show(Scene scene, String title, String description) {
        show(scene, title, description, Variant.DEFAULT, DEFAULT_DURATION);
    }

    public static void show(Scene scene, String title, String description, Duration duration) {
        show(scene, title, description, Variant.DEFAULT, duration);
    }

    public static void success(Scene scene, String title, String description) {
        show(scene, title, description, Variant.SUCCESS, DEFAULT_DURATION);
    }

    public static void error(Scene scene, String title, String description) {
        show(scene, title, description, Variant.DESTRUCTIVE, DEFAULT_DURATION);
    }

    public static void info(Scene scene, String title, String description) {
        show(scene, title, description, Variant.INFO, DEFAULT_DURATION);
    }

    public static void warning(Scene scene, String title, String description) {
        show(scene, title, description, Variant.WARNING, DEFAULT_DURATION);
    }

    public static void show(Scene scene, String title, String description, Variant variant, Duration duration) {
        if (scene == null || scene.getRoot() == null) return;

        TLToast toast = new TLToast(title, description, variant);

        // Wrap in an alignment container
        StackPane wrapper = new StackPane(toast);
        wrapper.setPickOnBounds(false);
        wrapper.setAlignment(Pos.BOTTOM_RIGHT);
        wrapper.setPadding(new javafx.geometry.Insets(0, 24, 24, 0));
        wrapper.setMouseTransparent(false);

        // Insert into scene root
        Node root = scene.getRoot();
        if (root instanceof StackPane sp) {
            sp.getChildren().add(wrapper);
            animateInOut(toast, wrapper, sp, duration);
        } else if (root instanceof javafx.scene.layout.BorderPane bp) {
            // Wrap existing content in StackPane temporarily
            StackPane overlay = new StackPane(wrapper);
            overlay.setPickOnBounds(false);
            overlay.setAlignment(Pos.BOTTOM_RIGHT);
            bp.getChildren().add(overlay);
            animateInOut(toast, overlay, bp, duration);
        } else {
            // Fallback: try to add to any Pane
            if (root instanceof javafx.scene.layout.Pane pane) {
                pane.getChildren().add(wrapper);
                animateInOut(toast, wrapper, pane, duration);
            }
        }
    }

    private static void animateInOut(TLToast toast, Node container, javafx.scene.layout.Pane parent, Duration duration) {
        // Slide in from right
        toast.setTranslateX(TOAST_WIDTH);
        toast.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), toast);
        slideIn.setFromX(TOAST_WIDTH);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        slideIn.play();
        fadeIn.play();

        // Auto-dismiss
        PauseTransition pause = new PauseTransition(duration);
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> parent.getChildren().remove(container));
            fadeOut.play();
        });
        pause.play();

        // Click to dismiss early
        toast.setOnMouseClicked(e -> {
            pause.stop();
            parent.getChildren().remove(container);
        });
    }
}
