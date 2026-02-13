package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import tn.esprit.skylora.entities.MessageTicket;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.MessageTicketService;
import tn.esprit.skylora.utils.MyConnection;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketDetailController {

    @FXML
    private Label subjectLabel;
    @FXML
    private Label statusBadge;
    @FXML
    private Label metaLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private VBox messageContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendButton;

    private Ticket ticket;
    private MessageTicketService messageService = new MessageTicketService(MyConnection.getInstance().getConnection());
    private int currentUserId = 1; // Simulation: utilisateur connecté
    private boolean isAdminMode = false;

    // Active ou désactive le mode administrateur (permet de modérer les messages)
    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        this.currentUserId = isAdmin ? 2 : 1; // Scenario: 1=User, 2=Admin
    }

    // Affiche les informations du ticket et charge la conversation
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
        subjectLabel.setText(ticket.getSubject());
        statusBadge.setText(ticket.getStatut());
        statusBadge.getStyleClass().add("status-" + ticket.getStatut().toLowerCase().replace("_", "-"));
        metaLabel.setText("Catégorie: " + ticket.getCategorie() + " | Priorité: " + ticket.getPriorite());
        descriptionLabel.setText(ticket.getDescription());

        loadMessages();
    }

    // Charge tous les messages de ce ticket depuis la base de données
    private void loadMessages() {
        messageContainer.getChildren().clear();
        List<MessageTicket> messages = messageService.getMessagesByTicketId(ticket.getId());
        for (MessageTicket msg : messages) {
            addMessageToUI(msg);
        }
        scrollPane.setVvalue(1.0);
    }

    // Crée une bulle de message (bleue pour moi, grise pour l'autre) et l'ajoute à
    // l'écran
    private void addMessageToUI(MessageTicket msg) {
        VBox msgBox = new VBox(5);
        msgBox.setMaxWidth(350);
        msgBox.setPadding(new Insets(10));

        Label content = new Label(msg.getContenu());
        content.setWrapText(true);

        Label date = new Label(msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        HBox wrapper = new HBox();
        if (msg.getUtilisateurId() == currentUserId) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            msgBox.setStyle("-fx-background-color: #1E40AF; -fx-background-radius: 15 15 0 15;");
            content.setStyle("-fx-text-fill: white;");
            msgBox.getChildren().addAll(content, date);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            msgBox.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 15 15 15 0;");
            content.setStyle("-fx-text-fill: #1F2937;");
            msgBox.getChildren().addAll(content, date);
        }

        if (isAdminMode) {
            msgBox.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem editItem = new MenuItem("Modifier");
                    MenuItem deleteItem = new MenuItem("Supprimer");

                    editItem.setOnAction(e -> handleEditMessage(msg));
                    deleteItem.setOnAction(e -> handleDeleteMessage(msg));

                    contextMenu.getItems().addAll(editItem, deleteItem);
                    contextMenu.show(msgBox, event.getScreenX(), event.getScreenY());
                }
            });
            msgBox.setStyle(msgBox.getStyle() + "-fx-cursor: hand;");
        }

        wrapper.getChildren().add(msgBox);
        messageContainer.getChildren().add(wrapper);
    }

    // Permet de modifier un message existant (clic droit admin)
    private void handleEditMessage(MessageTicket msg) {
        TextInputDialog dialog = new TextInputDialog(msg.getContenu());
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("Modifier le contenu du message");
        dialog.setContentText("Contenu:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newContent -> {
            msg.setContenu(newContent);
            if (messageService.modifierMessage(msg)) {
                loadMessages();
            }
        });
    }

    // Supprime un message (clic droit admin)
    private void handleDeleteMessage(MessageTicket msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le message");
        alert.setHeaderText("Supprimer ce message ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (messageService.supprimerMessage(msg.getId())) {
                loadMessages();
            }
        }
    }

    @FXML
    // Envoie le message écrit dans la zone de texte
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty())
            return;

        MessageTicket msg = new MessageTicket(ticket.getId(), currentUserId, text);
        if (messageService.ajouterMessage(msg)) {
            messageInput.clear();
            loadMessages();
        }
    }
}
