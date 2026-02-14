package com.skilora.finance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Skilora Finance Management System
 * Main Application Entry Point
 */
public class FinanceApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("\n" + com.skilora.config.DatabaseConfig.getConnectionStatus() + "\n");
        
        try {
            // Initialize Database (if available)
            if (com.skilora.config.DatabaseConfig.isDatabaseAvailable()) {
                System.out.println("DEBUG: Calling DatabaseInitializer.initialize()");
                com.skilora.finance.util.DatabaseInitializer.initialize();
            } else {
                System.out.println("⚠️  Running in OFFLINE mode - Finance features limited");
            }

            // Load FXML
            System.out.println("DEBUG: Loading FXML from /fxml/FinanceView.fxml");
            java.net.URL fxmlUrl = getClass().getResource("/fxml/FinanceView.fxml");
            if (fxmlUrl == null) {
                System.err.println("CRITICAL ERROR: FXML file not found at /fxml/FinanceView.fxml");
                throw new java.io.FileNotFoundException("FXML file not found");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("DEBUG: FXML loaded successfully");

            // Load Modern Theme CSS (shadcn/ui inspired)
            Scene scene = new Scene(root, 1600, 1000);

            // Load theme stylesheets in order: theme -> components -> utilities
            scene.getStylesheets().addAll(
                    getClass().getResource("/com/skilora/ui/styles/theme.css").toExternalForm(),
                    getClass().getResource("/com/skilora/ui/styles/components.css").toExternalForm(),
                    getClass().getResource("/com/skilora/ui/styles/utilities.css").toExternalForm());

            // Apply dark mode by default (can be toggled in UI)
            root.getStyleClass().add("root");

            // Configure Stage
            primaryStage.setTitle("Skilora Finance & Remuneration Management");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1400);
            primaryStage.setMinHeight(900);
            primaryStage.setMaximized(true); // Start maximized for best experience

            // Add icon (optional)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            } catch (Exception e) {
                System.out.println("Icon not found, using default");
            }

            // Show
            System.out.println("DEBUG: Showing Stage");
            primaryStage.show();

            System.out.println("╔══════════════════════════════════════════════╗");
            System.out.println("║  🚀 Skilora Finance System Started!         ║");
            System.out.println("║  Version: 1.0.0                              ║");
            System.out.println("║  Module: Finance & Remuneration              ║");
            System.out.println("║  Theme: Modern shadcn/ui Design System       ║");
            System.out.println("╚══════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("❌ CRITICAL FAILURE IN START:");
            e.printStackTrace();
            // Keep JVM alive for user to see error if possible (optional, but
            // System.exit(1) is better for automation)
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
