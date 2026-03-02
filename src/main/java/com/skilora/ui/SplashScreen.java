package com.skilora.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * SplashScreen — animated onboarding/splash overlay shown during app startup.
 *
 * <p>Features:
 * <ul>
 *   <li>Animated logo with glow pulse</li>
 *   <li>Progress bar indicating DB init + job feed loading</li>
 *   <li>Status messages updating in real-time</li>
 *   <li>Smooth fade-out transition to login</li>
 * </ul>
 *
 * <p>Usage: add as an overlay in Main.java, call {@link #setProgress(double, String)}
 * as background tasks complete, then {@link #fadeOut(Runnable)} to dismiss.
 */
public class SplashScreen extends StackPane {

    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label versionLabel;
    private final VBox contentBox;

    public SplashScreen() {
        // Full-screen dark background
        setStyle("-fx-background-color: #09090b;");
        setAlignment(Pos.CENTER);

        contentBox = new VBox(24);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(400);
        contentBox.setPadding(new Insets(40));

        // Logo circle with "S" icon
        StackPane logoContainer = createLogo();

        // App title
        Label titleLabel = new Label("SKILORA");
        titleLabel.setStyle("""
            -fx-font-size: 32px;
            -fx-font-weight: 700;
            -fx-text-fill: #fafafa;
            -fx-font-family: 'Inter', 'SF Pro Display', -apple-system, sans-serif;
            -fx-letter-spacing: 8;
            """);

        // Subtitle
        Label subtitleLabel = new Label("Tunisia Professional Platform");
        subtitleLabel.setStyle("""
            -fx-font-size: 13px;
            -fx-font-weight: 400;
            -fx-text-fill: #71717a;
            -fx-font-family: 'Inter', -apple-system, sans-serif;
            -fx-letter-spacing: 2;
            """);

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(3);
        progressBar.setStyle("""
            -fx-accent: #c9a84c;
            -fx-control-inner-background: #27272a;
            -fx-background-radius: 2;
            """);
        VBox.setMargin(progressBar, new Insets(16, 0, 0, 0));

        // Status label
        statusLabel = new Label("Initializing...");
        statusLabel.setStyle("""
            -fx-font-size: 11px;
            -fx-font-weight: 400;
            -fx-text-fill: #52525b;
            -fx-font-family: 'Inter', -apple-system, sans-serif;
            """);

        // Version label
        versionLabel = new Label("v1.0.0");
        versionLabel.setStyle("""
            -fx-font-size: 10px;
            -fx-text-fill: #3f3f46;
            -fx-font-family: 'SF Mono', 'Fira Code', monospace;
            """);

        contentBox.getChildren().addAll(
                logoContainer, titleLabel, subtitleLabel,
                progressBar, statusLabel
        );

        // Version at bottom
        StackPane.setAlignment(versionLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(versionLabel, new Insets(0, 0, 24, 0));

        getChildren().addAll(contentBox, versionLabel);

        // Entrance animations
        playEntranceAnimation();
    }

    /**
     * Create the animated logo with a glowing gold circle.
     */
    private StackPane createLogo() {
        // Outer glow circle
        Circle glowCircle = new Circle(44);
        glowCircle.setFill(Color.TRANSPARENT);
        glowCircle.setStroke(Color.web("#c9a84c", 0.3));
        glowCircle.setStrokeWidth(1.5);

        // Inner circle
        Circle innerCircle = new Circle(36);
        innerCircle.setFill(Color.web("#c9a84c", 0.08));
        innerCircle.setStroke(Color.web("#c9a84c", 0.6));
        innerCircle.setStrokeWidth(1);

        // "S" letter as logo
        Label logoLetter = new Label("S");
        logoLetter.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: 700;
            -fx-text-fill: #c9a84c;
            -fx-font-family: 'Inter', 'SF Pro Display', sans-serif;
            """);

        StackPane logoStack = new StackPane(glowCircle, innerCircle, logoLetter);
        logoStack.setAlignment(Pos.CENTER);

        // Pulse animation on glow circle
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1500), glowCircle);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        // Glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#c9a84c", 0.4));
        glow.setRadius(20);
        glow.setSpread(0.2);
        logoStack.setEffect(glow);

        return logoStack;
    }

    /**
     * Play entrance fade + slide animation.
     */
    private void playEntranceAnimation() {
        contentBox.setOpacity(0);
        contentBox.setTranslateY(20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), contentBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(800), contentBox);
        slideUp.setFromY(20);
        slideUp.setToY(0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, slideUp);
        entrance.setInterpolator(Interpolator.EASE_OUT);
        entrance.play();
    }

    /**
     * Update progress bar and status text.
     *
     * @param progress 0.0 to 1.0
     * @param message  status message to display
     */
    public void setProgress(double progress, String message) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText(message);
        });
    }

    /**
     * Smoothly fade out the splash screen, then run the callback
     * (typically to show the login view underneath).
     */
    public void fadeOut(Runnable onComplete) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), this);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
            if (onComplete != null) onComplete.run();
        });
        fade.play();
    }
}
