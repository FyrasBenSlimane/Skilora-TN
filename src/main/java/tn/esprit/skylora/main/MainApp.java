package tn.esprit.skylora.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Charger le FXML depuis le classpath
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/UserDashboard.fxml"));

        Scene scene = new Scene(loader.load(), 1100, 750);
        stage.setTitle("Skilora Tunisia - Support Ticket System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
