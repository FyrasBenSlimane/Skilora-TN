package tn.esprit.skylora.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class NavbarController {

    @FXML
    // Change la scène vers le tableau de bord utilisateur
    private void goToUserDashboard(ActionEvent event) {
        switchScene(event, "/tn/esprit/skylora/gui/UserDashboard.fxml");
    }

    @FXML
    // Change la scène vers le tableau de bord administrateur
    private void goToAdminDashboard(ActionEvent event) {
        switchScene(event, "/tn/esprit/skylora/gui/AdminDashboard.fxml");
    }

    // Méthode utilitaire pour charger et afficher une nouvelle vue FXML
    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de chargement FXML: " + fxmlPath);
            e.printStackTrace();
            // Optionnel: Afficher une alerte à l'utilisateur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de Navigation");
            alert.setHeaderText("Impossible de charger la page");
            alert.setContentText("Détails: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
