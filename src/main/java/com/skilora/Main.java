package com.skilora;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.skilora.user.controller.LoginController;
import com.skilora.ui.AppFontLoader;
import com.skilora.ui.SplashScreen;
import com.skilora.config.DatabaseInitializer;
import com.skilora.recruitment.service.JobService;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main Application Entry Point
 *
 * Skilora Tunisia - Launches Login View.
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        // Load custom fonts first (needed by splash)
        AppFontLoader.load();

        // Configure Stage (Transparent for custom chrome)
        WindowConfig.configureStage(primaryStage);

        // Register shutdown hook to close database connections
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AppThreadPool.shutdown();
            try {
                com.skilora.config.DatabaseConfig.getInstance().closeConnection();
            } catch (Exception e) {
                System.err.println("Shutdown cleanup: " + e.getMessage());
            }
        }));

        // Prewarm heavy assets (Video)
        com.skilora.user.service.MediaCache.getInstance().prewarm();

        // Create splash screen overlay
        SplashScreen splash = new SplashScreen();

        try {
            // Load LoginView from FXML (hidden behind splash)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/LoginView.fxml"));
            HBox loginRoot = loader.load();

            LoginController controller = loader.getController();
            if (controller != null) {
                controller.setStage(primaryStage);
            }

            TLWindow root = new TLWindow(primaryStage, "Skilora", loginRoot);

            // Stack splash on top of login view
            StackPane appRoot = new StackPane(root, splash);

            Scene scene = new Scene(appRoot, 1200, 800);
            WindowConfig.configureScene(scene);

            primaryStage.setTitle(I18n.get("app.title"));
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            logger.error("Failed to load LoginView: " + e.getMessage(), e);
            return;
        }

        // Initialize Database + refresh job feed in parallel (non-blocking startup).
        splash.setProgress(0.1, "Connecting to database...");

        Future<?> dbInitFuture = AppThreadPool.submit(() -> {
            DatabaseInitializer.initialize();
            Platform.runLater(() -> splash.setProgress(0.5, "Database ready."));
            return null;
        });

        AppThreadPool.execute(() -> {
            try {
                logger.info("Startup: refreshing job feed in background...");
                Platform.runLater(() -> splash.setProgress(0.3, "Loading job feed..."));
                JobService.getInstance().refreshFeed(false, () -> {
                    logger.info("Startup: job feed refresh complete.");
                    Platform.runLater(() -> splash.setProgress(0.7, "Job feed loaded."));
                });
            } catch (Exception e) {
                logger.debug("Startup: job feed refresh failed: {}", e.getMessage());
            }
        });

        AppThreadPool.execute(() -> {
            try {
                dbInitFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug("Startup: DB init wait failed: {}", e.getMessage());
            }
            try {
                JobService.getInstance().reloadCacheFromJson();
            } catch (Exception e) {
                logger.debug("Startup: post-init cache reload failed: {}", e.getMessage());
            }

            // Camera is NOT pre-warmed at startup — it only activates when user clicks Face ID
            // This prevents the camera LED from turning on at the login screen

            Platform.runLater(() -> {
                splash.setProgress(1.0, "Ready!");
                // Small delay so user sees 100% before fade
                PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(500));
                pause.setOnFinished(ev -> splash.fadeOut(null));
                pause.play();
            });
        });
    }

    public static void main(String[] args) {
        // PERFORMANCE: Enable hardware acceleration and rendering optimizations
        System.setProperty("prism.lcdtext", "false"); // Disable LCD text for faster rendering
        System.setProperty("prism.text", "t2k"); // Use faster text renderer
        System.setProperty("prism.order", "d3d,sw"); // Prefer DirectX on Windows
        System.setProperty("prism.forceGPU", "true"); // Force GPU usage

        launch(args);
    }
}
