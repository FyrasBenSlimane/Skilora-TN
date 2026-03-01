package tn.esprit.skylora.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent; // Added import for Parent

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Charger le FXML depuis le classpath
        Parent root = FXMLLoader.load(getClass().getResource("/tn/esprit/skylora/gui/MainShell.fxml"));

        Scene scene = new Scene(root, 1100, 750);
        stage.setTitle("Skylora Support System"); // Updated title
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
