package com.skilora.support.controller;

import com.skilora.support.entity.TicketMessage;
import com.skilora.support.service.TicketMessageService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller for the ticket message management view (table-based CRUD).
 * Ported from branch MessageTicketController with full package refactor.
 */
public class TicketMessageController {

    @FXML private VBox rootVBox;
    @FXML private TextField tfTicketId;
    @FXML private TextField tfUtilisateurId;
    @FXML private TextArea taContenu;
    @FXML private TableView<TicketMessage> tableMessages;
    @FXML private TableColumn<TicketMessage, Integer> colId;
    @FXML private TableColumn<TicketMessage, Integer> colTicketId;
    @FXML private TableColumn<TicketMessage, Integer> colUtilisateurId;
    @FXML private TableColumn<TicketMessage, String> colContenu;
    @FXML private TableColumn<TicketMessage, String> colDateEnvoi;

    private final TicketMessageService service = TicketMessageService.getInstance();
    private ObservableList<TicketMessage> messageList;

    public void initialize() {
        colId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTicketId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getTicketId()).asObject());
        colUtilisateurId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSenderId()).asObject());
        colContenu.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMessage()));
        colDateEnvoi.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getCreatedDate() != null
                                ? data.getValue().getCreatedDate().toString()
                                : ""));

        messageList = FXCollections.observableArrayList();
        tableMessages.setItems(messageList);

        tfUtilisateurId.setText("1");
        loadMessages();
    }

    private void loadMessages() {
        List<TicketMessage> messages = service.findAll();
        messageList.setAll(messages);
    }

    @FXML
    private void ajouterMessage() {
        if (tfTicketId.getText().isEmpty() || tfUtilisateurId.getText().isEmpty()
                || taContenu.getText().isEmpty()) {
            showAlert("Error", "All fields must be filled.");
            return;
        }

        try {
            TicketMessage m = new TicketMessage(
                    Integer.parseInt(tfTicketId.getText()),
                    Integer.parseInt(tfUtilisateurId.getText()),
                    taContenu.getText(),
                    false);
            if (service.addMessage(m) > 0) {
                loadMessages();
                clearFields();
            } else {
                showAlert("Error", "Failed to add message.");
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Ticket ID and User ID must be numbers.");
        }
    }

    @FXML
    private void modifierMessage() {
        TicketMessage selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Select a message to edit.");
            return;
        }

        selected.setMessage(taContenu.getText());
        if (service.updateMessage(selected)) {
            loadMessages();
            clearFields();
        } else {
            showAlert("Error", "Failed to update message.");
        }
    }

    @FXML
    private void supprimerMessage() {
        TicketMessage selected = tableMessages.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Select a message to delete.");
            return;
        }

        if (service.delete(selected.getId())) {
            loadMessages();
            clearFields();
        } else {
            showAlert("Error", "Failed to delete message.");
        }
    }

    private void clearFields() {
        tfTicketId.clear();
        tfUtilisateurId.clear();
        taContenu.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
