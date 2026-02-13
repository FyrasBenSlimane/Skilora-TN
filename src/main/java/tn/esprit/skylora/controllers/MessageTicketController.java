package tn.esprit.skylora.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage; // <-- IMPORTANT
import tn.esprit.skylora.entities.MessageTicket;
import tn.esprit.skylora.services.MessageTicketService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class MessageTicketController {

    @FXML
    private VBox rootVBox;
    @FXML
    private TextField tfTicketId;
    @FXML
    private TextField tfUtilisateurId;
    @FXML
    private TextArea taContenu;
    @FXML
    private TableView<MessageTicket> tableMessages;
    @FXML
    private TableColumn<MessageTicket, Integer> colId;
    @FXML
    private TableColumn<MessageTicket, Integer> colTicketId;
    @FXML
    private TableColumn<MessageTicket, Integer> colUtilisateurId;
    @FXML
    private TableColumn<MessageTicket, String> colContenu;
    @FXML
    private TableColumn<MessageTicket, String> colDateEnvoi;

    @FXML
    // Ouvre la fenêtre de gestion des messages
    private void ouvrirMessageTicket() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MessageTicket.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Gestion des Messages Ticket");
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessageTicketService service;
    private ObservableList<MessageTicket> messageList;

    // Initialise le contrôleur, configure la connexion et charge les données
    public void initialize() {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/skylora", "root", ""); // change selon ta config
            service = new MessageTicketService(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        colId.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTicketId.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTicketId()).asObject());
        colUtilisateurId.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getUtilisateurId()).asObject());
        colContenu.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getContenu()));
        colDateEnvoi.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDateEnvoi() != null ? data.getValue().getDateEnvoi().toString() : ""));

        messageList = FXCollections.observableArrayList();
        tableMessages.setItems(messageList);

        tfUtilisateurId.setText("1"); // Default user for scenario

        loadMessages();
    }

    // Charge la liste des messages depuis la base de données
    private void loadMessages() {
        List<MessageTicket> messages = service.getAllMessages();
        messageList.setAll(messages);
    }

    @FXML
    // Ajoute un nouveau message après vérification
    private void ajouterMessage() {
        if (tfTicketId.getText().isEmpty() || tfUtilisateurId.getText().isEmpty() || taContenu.getText().isEmpty()) {
            showAlert("Erreur", "Tous les champs doivent être remplis !");
            return;
        }

        try {
            MessageTicket m = new MessageTicket(
                    Integer.parseInt(tfTicketId.getText()),
                    Integer.parseInt(tfUtilisateurId.getText()),
                    taContenu.getText());
            if (service.ajouterMessage(m)) {
                loadMessages();
                clearFields();
            } else {
                showAlert("Erreur", "Impossible d'ajouter le message.");
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Ticket ID et Utilisateur ID doivent être des nombres.");
        }
    }

    @FXML
    // Modifie le contenu du message sélectionné
    private void modifierMessage() {
        MessageTicket selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Sélectionnez un message à modifier.");
            return;
        }

        selected.setContenu(taContenu.getText());
        if (service.modifierMessage(selected)) {
            loadMessages();
            clearFields();
        } else {
            showAlert("Erreur", "Impossible de modifier le message.");
        }
    }

    @FXML
    // Supprime le message sélectionné
    private void supprimerMessage() {
        MessageTicket selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Sélectionnez un message à supprimer.");
            return;
        }

        if (service.supprimerMessage(selected.getId())) {
            loadMessages();
            clearFields();
        } else {
            showAlert("Erreur", "Impossible de supprimer le message.");
        }
    }

    // Vide les champs de saisie
    private void clearFields() {
        tfTicketId.clear();
        tfUtilisateurId.clear();
        taContenu.clear();
    }

    // Affiche une alerte à l'utilisateur
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
