package com.skilora;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.skilora.controller.LoginController;
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
            try {
                DatabaseInitializer.initialize();
            } catch (Exception e) {
                // Database not available - continue in offline mode
                System.err.println("Database initialization skipped: " + e.getMessage());
            }
        }).start();

        // Register shutdown hook to close database connections
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                com.skilora.config.DatabaseConfig.getInstance().closeConnection();
            } catch (Exception ignored) {
            }
        }));

        // Prewarm heavy assets (Video)
        com.skilora.model.service.MediaCache.getInstance().prewarm();

        // Load custom fonts
        AppFontLoader.load();

        // Configure Stage (Transparent for custom chrome)
        WindowConfig.configureStage(primaryStage);

        try {
            // Load LoginView from FXML
            java.net.URL fxmlUrl = getClass().getResource("/com/skilora/view/LoginView.fxml");
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML introuvable: /com/skilora/view/LoginView.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
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
            // AFFICHER L'ERREUR COMPLETE (ne pas avaler silencieusement !)
            logger.error("========================================");
            logger.error("ERREUR CRITIQUE AU DEMARRAGE !");
            logger.error("Message: " + e.getMessage());
            logger.error("Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "aucune"));
            logger.error("========================================", e);
            e.printStackTrace(); // Visible meme sans SLF4J
            // Fermer proprement au lieu de tourner en arriere-plan sans fenetre
            javafx.application.Platform.exit();
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
