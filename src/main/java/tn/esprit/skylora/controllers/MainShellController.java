package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainShellController implements Initializable {

    @FXML
    private StackPane contentArea;

    private static MainShellController instance;

    public MainShellController() {
        instance = this;
    }

    public static MainShellController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load default view (User Dashboard)
        loadView("/tn/esprit/skylora/gui/UserDashboard.fxml");
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void loadViewCustom(Parent root) {
        contentArea.getChildren().setAll(root);
    }
}
