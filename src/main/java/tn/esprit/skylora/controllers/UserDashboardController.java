package tn.esprit.skylora.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserDashboardController implements Initializable {

    @FXML
    private FlowPane ticketContainer;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;

    private ServiceTicket serviceTicket = new ServiceTicket();
    private List<Ticket> allTickets = new java.util.ArrayList<>();
    private int currentUserId = 1; // Simulation: utilisateur connecté

    @Override
    // Initialise la page et charge les tickets de l'utilisateur
    public void initialize(URL url, ResourceBundle rb) {
        statusFilter.getItems().addAll("TOUS", "OUVERT", "EN_COURS", "RESOLU", "CLOTURE");
        statusFilter.setValue("TOUS");

        loadTickets();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTickets());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTickets());
    }

    // Récupère les tickets de l'utilisateur connecté depuis la base de données
    public void loadTickets() {
        try {
            allTickets = serviceTicket.getTicketsByUserId(currentUserId);
            displayTickets(allTickets);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Crée et affiche une carte pour chaque ticket dans la liste
    private void displayTickets(List<Ticket> tickets) {
        ticketContainer.getChildren().clear();
        if (tickets == null)
            return;
        for (Ticket ticket : tickets) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/TicketCard.fxml"));
                Parent card = loader.load();
                TicketCardController cardController = loader.getController();
                cardController.setData(ticket, this);
                ticketContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Filtre les tickets affichés selon la recherche et le statut choisi
    private void filterTickets() {
        String search = searchField.getText().toLowerCase();
        String status = statusFilter.getValue();

        List<Ticket> filtered = allTickets.stream()
                .filter(t -> t.getSubject().toLowerCase().contains(search)
                        || t.getCategorie().toLowerCase().contains(search))
                .filter(t -> status.equals("TOUS") || t.getStatut().equals(status))
                .collect(Collectors.toList());

        displayTickets(filtered);
    }

    @FXML
    // Ouvre la fenêtre pour créer un nouveau ticket
    private void handleNewTicket() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/TicketModal.fxml"));
            Parent root = loader.load();
            TicketModalController controller = loader.getController();
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouveau Ticket");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
