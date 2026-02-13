package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class TicketModalController implements Initializable {

    @FXML
    private Label modalTitle;
    @FXML
    private TextField subjectField;
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private ComboBox<String> priorityBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private ServiceTicket serviceTicket = new ServiceTicket();
    private UserDashboardController parentController;
    private Ticket existingTicket;

    @Override
    // Initialise les listes déroulantes (catégorie, priorité)
    public void initialize(URL url, ResourceBundle rb) {
        categoryBox.getItems().addAll("TECHNICAL", "PAYMENT", "ACCOUNT", "FORMATION", "RECRUITMENT", "OTHER");
        priorityBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT");
    }

    // Garde une référence vers le tableau de bord pour pouvoir le rafraîchir
    public void setParentController(UserDashboardController parentController) {
        this.parentController = parentController;
    }

    // Remplit le formulaire avec les données d'un ticket existant (si modification)
    public void setTicketData(Ticket ticket) {
        this.existingTicket = ticket;
        modalTitle.setText("Modifier le Ticket #" + ticket.getId());
        subjectField.setText(ticket.getSubject());
        categoryBox.setValue(ticket.getCategorie());
        priorityBox.setValue(ticket.getPriorite());
        descriptionArea.setText(ticket.getDescription());
    }

    @FXML
    // Sauvegarde le ticket (création ou modification) après vérification des champs
    private void handleSave() {
        if (subjectField.getText().isEmpty() || categoryBox.getValue() == null ||
                priorityBox.getValue() == null || descriptionArea.getText().isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs obligatoires.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        try {
            if (existingTicket == null) {
                // Nouveau ticket
                Ticket newTicket = new Ticket(1, // Simulation user 1
                        subjectField.getText(),
                        categoryBox.getValue(),
                        priorityBox.getValue(),
                        "OUVERT",
                        descriptionArea.getText());
                serviceTicket.ajouter(newTicket);
            } else {
                // Modification
                existingTicket.setSubject(subjectField.getText());
                existingTicket.setCategorie(categoryBox.getValue());
                existingTicket.setPriorite(priorityBox.getValue());
                existingTicket.setDescription(descriptionArea.getText());
                serviceTicket.modifier(existingTicket);
            }

            parentController.loadTickets();
            close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    // Ferme la fenêtre sans enregistrer
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
