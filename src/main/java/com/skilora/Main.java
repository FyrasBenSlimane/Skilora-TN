package com.skilora;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.skilora.controller.usermanagement.LoginController;
import com.skilora.ui.AppFontLoader;
import com.skilora.config.DatabaseInitializer;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Main Application Entry Point
 *
 * Skilora Tunisia - Launches Login View.
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    static {
        // Force initialize Logback to ensure it's loaded before SLF4J tries to find it
        // This is necessary because the JavaFX Maven plugin may not include all dependencies
        try {
            // Check if logback-classic is available
            Class.forName("ch.qos.logback.classic.LoggerContext");
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            // Load logback.xml from classpath
            java.net.URL logbackConfig = Main.class.getResource("/logback.xml");
            if (logbackConfig != null) {
                configurator.doConfigure(logbackConfig);
            }
        } catch (ClassNotFoundException e) {
            // logback-classic not in classpath - this is the problem!
            System.err.println("WARNING: logback-classic not found in classpath. Logging will not work properly.");
            System.err.println("Please ensure logback-classic is included in the JavaFX Maven plugin classpath.");
        } catch (JoranException e) {
            // Ignore - will use default configuration
        } catch (Exception e) {
            // Ignore - will use default configuration
        }
    }

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
        com.skilora.service.usermanagement.MediaCache.getInstance().prewarm();

        // Load custom fonts
        AppFontLoader.load();

        // Configure Stage (Transparent for custom chrome)
        WindowConfig.configureStage(primaryStage);

        try {
            java.net.URL loginFxml = getClass().getResource("/com/skilora/view/usermanagement/LoginView.fxml");
            if (loginFxml == null) {
                throw new IllegalStateException("LoginView.fxml not found on classpath. Check that src/main/resources is included in build.");
            }
            FXMLLoader loader = new FXMLLoader(loginFxml);
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
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Throwable t) {
            logger.error("Failed to load LoginView: " + t.getMessage(), t);
            System.err.println("Skilora startup error: " + t.getMessage());
            t.printStackTrace(System.err);
            // Always show a window so the user sees something and the error message
            VBox fallback = new VBox(20);
            fallback.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 40;");
            Label msg = new Label("Could not load login screen: " + t.getMessage());
            msg.setStyle("-fx-text-fill: #fafafa; -fx-font-size: 14px; -fx-wrap-text: true;");
            msg.setWrapText(true);
            Label hint = new Label("Check the Run console for details. Ensure FXML and CSS are in src/main/resources.");
            hint.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 12px; -fx-wrap-text: true;");
            hint.setWrapText(true);
            fallback.getChildren().addAll(msg, hint);
            Scene fallbackScene = new Scene(fallback, 600, 200);
            primaryStage.setScene(fallbackScene);
            primaryStage.setTitle("Skilora - Error");
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(200);
            primaryStage.centerOnScreen();
            primaryStage.show();
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
