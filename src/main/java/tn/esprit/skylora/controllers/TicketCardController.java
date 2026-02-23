package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.skylora.entities.Ticket;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class TicketCardController {

    @FXML
    private VBox cardRoot;
    @FXML
    private Label priorityBadge;
    @FXML
    private Label statusBadge;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private javafx.scene.control.Button rateButton;

    private Ticket ticket;
    private UserDashboardController parentController;

    // Remplit la carte avec les informations du ticket (sujet, date, statut...)
    public void setData(Ticket ticket, UserDashboardController parentController) {
        this.ticket = ticket;
        this.parentController = parentController;

        subjectLabel.setText(ticket.getSubject());
        categoryLabel.setText("Catégorie: " + ticket.getCategorie());
        if (ticket.getDateCreation() != null) {
            dateLabel.setText("Créé le: " + ticket.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        priorityBadge.setText(ticket.getPriorite());
        priorityBadge.getStyleClass().add("priority-" + ticket.getPriorite().toLowerCase());

        statusBadge.setText(ticket.getStatut());
        statusBadge.getStyleClass().add("status-" + ticket.getStatut().toLowerCase().replace("_", "-"));

        // Show "Noter" button only for RESOLVED tickets
        if ("RESOLU".equalsIgnoreCase(ticket.getStatut())) {
            checkIfAlreadyRated();
        }
    }

    private void checkIfAlreadyRated() {
        // Always show the "Noter" button for RESOLU tickets.
        // The modal will inform the user if they have already rated or if the DB table
        // is missing.
        rateButton.setVisible(true);
        rateButton.setManaged(true);
    }

    @FXML
    private void handleRate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/FeedbackModal.fxml"));
            Parent root = loader.load();
            FeedbackModalController controller = loader.getController();
            controller.setTicketId(ticket.getId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Noter le service");
            stage.setScene(new Scene(root));

            // Refresh dashboard after closing modal
            stage.setOnHidden(event -> {
                if (parentController != null)
                    parentController.loadTickets();
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    // Ouvre la fenêtre pour modifier ce ticket
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/TicketModal.fxml"));
            Parent root = loader.load();
            TicketModalController controller = loader.getController();
            controller.setParentController(parentController);
            controller.setTicketData(ticket);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Ticket");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    // Supprime ce ticket après confirmation de l'utilisateur
    private void handleDelete() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le ticket");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer ce ticket ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            tn.esprit.skylora.services.ServiceTicket service = new tn.esprit.skylora.services.ServiceTicket();
            try {
                service.supprimer(ticket.getId());
                parentController.loadTickets();
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    // Ouvre les détails complets du ticket
    private void handleView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/TicketDetail.fxml"));
            Parent root = loader.load();
            TicketDetailController controller = loader.getController();
            controller.setAdminMode(false); // Ensure user mode (ID 1)
            controller.setTicket(ticket);

            Stage stage = new Stage();
            stage.setTitle("Détails du Ticket");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
