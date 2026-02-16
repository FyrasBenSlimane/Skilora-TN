package com.skilora;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.skilora.user.controller.LoginController;
import com.skilora.ui.AppFontLoader;
import com.skilora.config.DatabaseInitializer;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Application Entry Point
 *
 * Skilora Tunisia - Launches Login View.
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        // Initialize Database in Background (Non-blocking startup)
        new Thread(() -> {
            DatabaseInitializer.initialize();
        }).start();

        // Register shutdown hook to close database connections
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                com.skilora.config.DatabaseConfig.getInstance().closeConnection();
            } catch (Exception ignored) {}
        }));

        // Prewarm heavy assets (Video)
        com.skilora.user.service.MediaCache.getInstance().prewarm();

        // Load custom fonts
        AppFontLoader.load();

        // Configure Stage (Transparent for custom chrome)
        WindowConfig.configureStage(primaryStage);

        try {
            // Load LoginView from FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/LoginView.fxml"));
            HBox loginRoot = loader.load();

            // Pass stage to controller
            LoginController controller = loader.getController();
            if (controller != null) {
                controller.setStage(primaryStage);
            }

            // Wrap in TLWindow for consistent chrome
            TLWindow root = new TLWindow(primaryStage, "Skilora", loginRoot);

            Scene scene = new Scene(root, 1200, 800);
            WindowConfig.configureScene(scene);

            primaryStage.setTitle("Skilora - Tunisians Top Talent Platform");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            logger.error("Failed to load LoginView: " + e.getMessage(), e);
        }
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
