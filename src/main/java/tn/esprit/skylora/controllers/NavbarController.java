package tn.esprit.skylora.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

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

    // Méthode utilitaire pour charger et afficher une nouvelle vue FXML dans la
    // zone de contenu
    private void switchScene(ActionEvent event, String fxmlPath) {
        MainShellController mainShell = MainShellController.getInstance();
        if (mainShell != null) {
            mainShell.loadView(fxmlPath);
        } else {
            System.err.println("MainShellController instance is null! Cannot navigate within content area.");
        }
    }
}
