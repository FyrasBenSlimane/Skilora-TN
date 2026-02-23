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
import javafx.application.Platform;
import tn.esprit.skylora.utils.GeminiService;

/**
 * Contrôleur de la fenêtre modale de création et modification de tickets.
 * Gère la saisie des informations d'un ticket (sujet, catégorie, priorité,
 * description)
 * et offre des fonctionnalités IA via Gemini :
 * - Auto-catégorisation du ticket selon sa description
 * - Correction automatique de la description
 */
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

    /**
     * Initialise les listes déroulantes des catégories et priorités au chargement
     * de la fenêtre.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryBox.getItems().addAll("TECHNICAL", "PAYMENT", "ACCOUNT", "FORMATION", "RECRUITMENT", "OTHER");
        priorityBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT");
    }

    /**
     * Garde une référence vers le tableau de bord parent pour pouvoir le rafraîchir
     * après la création ou la modification d'un ticket.
     *
     * @param parentController Le contrôleur du tableau de bord utilisateur
     */
    public void setParentController(UserDashboardController parentController) {
        this.parentController = parentController;
    }

    /**
     * Pré-remplit le formulaire avec les données d'un ticket existant.
     * Appeler cette méthode bascule le mode de la fenêtre de "Création" à
     * "Modification".
     *
     * @param ticket Le ticket dont les données doivent pré-remplir le formulaire
     */
    public void setTicketData(Ticket ticket) {
        this.existingTicket = ticket;
        modalTitle.setText("Modifier le Ticket #" + ticket.getId());
        subjectField.setText(ticket.getSubject());
        categoryBox.setValue(ticket.getCategorie());
        priorityBox.setValue(ticket.getPriorite());
        descriptionArea.setText(ticket.getDescription());
    }

    /**
     * Envoie le sujet et la description à l'IA Gemini pour déterminer
     * automatiquement
     * la catégorie la plus appropriée et la sélectionne dans la liste déroulante.
     * L'opération s'effectue en arrière-plan pour ne pas bloquer l'interface.
     */
    @FXML
    public void handleAICategorize() {
        String subject = subjectField.getText();
        String description = descriptionArea.getText();

        if (subject.isEmpty() && description.isEmpty()) {
            errorLabel.setText("Veuillez remplir au moins le sujet ou la description.");
            errorLabel.setVisible(true);
            return;
        }

        new Thread(() -> {
            String prediction = GeminiService.predictCategory(subject, description);
            Platform.runLater(() -> {
                if (prediction != null && !prediction.startsWith("Error")) {
                    categoryBox.setValue(prediction);
                }
            });
        }).start();
    }

    /**
     * Envoie la description saisie à l'IA Gemini pour corriger automatiquement
     * les fautes d'orthographe et de grammaire, puis met à jour le champ de
     * description.
     * L'opération s'effectue en arrière-plan pour ne pas bloquer l'interface.
     */
    @FXML
    public void handleAICorrect() {
        String description = descriptionArea.getText();
        if (description.isEmpty()) {
            return;
        }

        new Thread(() -> {
            String corrected = GeminiService.correctText(description);
            Platform.runLater(() -> {
                if (corrected != null && !corrected.startsWith("Error")) {
                    descriptionArea.setText(corrected);
                }
            });
        }).start();
    }

    /**
     * Valide le formulaire et enregistre le ticket en base de données.
     * En mode création, génère un nouveau ticket avec le statut "OUVERT".
     * En mode modification, met à jour le ticket existant.
     * Affiche un message d'erreur si des champs obligatoires sont manquants.
     */
    @FXML
    public void handleSave() {
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

    /**
     * Ferme la fenêtre modale sans enregistrer les modifications.
     */
    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
