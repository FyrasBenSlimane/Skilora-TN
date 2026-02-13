package tn.esprit.skylora.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {

    @FXML
    private Label totalTicketsLabel;
    @FXML
    private Label openTicketsLabel;
    @FXML
    private Label resolvedTicketsLabel;
    @FXML
    private Label pendingTicketsLabel;

    @FXML
    private TextField adminSearchField;
    @FXML
    private ComboBox<String> adminStatusFilter;
    @FXML
    private ComboBox<String> adminPriorityFilter;

    @FXML
    private TableView<Ticket> adminTicketTable;
    @FXML
    private TableColumn<Ticket, Integer> colAdminId;
    @FXML
    private TableColumn<Ticket, String> colAdminSubject;
    @FXML
    private TableColumn<Ticket, Integer> colAdminUser;
    @FXML
    private TableColumn<Ticket, String> colAdminCategory;
    @FXML
    private TableColumn<Ticket, String> colAdminPriority;
    @FXML
    private TableColumn<Ticket, String> colAdminStatus;
    @FXML
    private TableColumn<Ticket, LocalDateTime> colAdminDate;
    @FXML
    private TableColumn<Ticket, Void> colAdminActions;

    private ServiceTicket serviceTicket = new ServiceTicket();
    private List<Ticket> allTickets = new java.util.ArrayList<>();
    private ObservableList<Ticket> observableTickets = FXCollections.observableArrayList();

    @Override
    // Initialise le tableau et les filtres au chargement de la page
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
        refreshData();
    }

    // Configure les colonnes du tableau pour afficher les tickets
    private void setupTable() {
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colAdminUser.setCellValueFactory(new PropertyValueFactory<>("utilisateurId"));
        colAdminCategory.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colAdminPriority.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colAdminDate.setCellFactory(column -> new TableCell<Ticket, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        colAdminDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        setupActionsColumn();
    }

    // Ajoute les boutons d'action (voir, changer statut) dans chaque ligne
    private void setupActionsColumn() {
        Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>> cellFactory = new Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>>() {
            @Override
            public TableCell<Ticket, Void> call(final TableColumn<Ticket, Void> param) {
                return new TableCell<Ticket, Void>() {
                    private final Button viewBtn = new Button("👁");
                    private final Button statusBtn = new Button("⚙");
                    private final HBox pane = new HBox(5, viewBtn, statusBtn);

                    {
                        viewBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-cursor: hand;");
                        statusBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-cursor: hand;");

                        viewBtn.setOnAction(event -> {
                            Ticket ticket = getTableView().getItems().get(getIndex());
                            handleViewTicket(ticket);
                        });

                        statusBtn.setOnAction(event -> {
                            Ticket ticket = getTableView().getItems().get(getIndex());
                            handleQuickStatusUpdate(ticket);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty)
                            setGraphic(null);
                        else
                            setGraphic(pane);
                    }
                };
            }
        };
        colAdminActions.setCellFactory(cellFactory);
    }

    // Remplit les listes déroulantes pour filtrer par statut et priorité
    private void setupFilters() {
        adminStatusFilter.getItems().addAll("TOUS", "OUVERT", "EN_COURS", "RESOLU", "CLOTURE");
        adminPriorityFilter.getItems().addAll("TOUS", "LOW", "MEDIUM", "HIGH", "URGENT");

        adminSearchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        adminStatusFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
        adminPriorityFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
    }

    @FXML
    // Récupère tous les tickets de la base de données et met à jour l'affichage
    public void refreshData() {
        try {
            allTickets = serviceTicket.afficher();
            calculateStats();
            filterData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Calcule et affiche le nombre de tickets par statut
    private void calculateStats() {
        long total = allTickets.size();
        long open = allTickets.stream().filter(t -> t.getStatut().equals("OUVERT")).count();
        long resolved = allTickets.stream().filter(t -> t.getStatut().equals("RESOLU")).count();
        long pending = allTickets.stream().filter(t -> t.getStatut().equals("EN_COURS")).count();

        totalTicketsLabel.setText(String.valueOf(total));
        openTicketsLabel.setText(String.valueOf(open));
        resolvedTicketsLabel.setText(String.valueOf(resolved));
        pendingTicketsLabel.setText(String.valueOf(pending));
    }

    // Filtre la liste des tickets affichés selon la recherche et les sélections
    private void filterData() {
        String search = adminSearchField.getText().toLowerCase();
        String status = adminStatusFilter.getValue();
        String priority = adminPriorityFilter.getValue();

        List<Ticket> filtered = allTickets.stream()
                .filter(t -> t.getSubject().toLowerCase().contains(search)
                        || String.valueOf(t.getUtilisateurId()).contains(search))
                .filter(t -> status == null || status.equals("TOUS") || t.getStatut().equals(status))
                .filter(t -> priority == null || priority.equals("TOUS") || t.getPriorite().equals(priority))
                .collect(Collectors.toList());

        observableTickets.setAll(filtered);
        adminTicketTable.setItems(observableTickets);
    }

    // Ouvre la fenêtre de détails pour le ticket sélectionné
    private void handleViewTicket(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/TicketDetail.fxml"));
            Parent root = loader.load();
            TicketDetailController controller = loader.getController();
            controller.setAdminMode(true);
            controller.setTicket(ticket);

            Stage stage = new Stage();
            stage.setTitle("Admin - Détails du Ticket #" + ticket.getId());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Affiche une boîte de dialogue pour changer rapidement le statut d'un ticket
    private void handleQuickStatusUpdate(Ticket ticket) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(ticket.getStatut(), "OUVERT", "EN_COURS", "RESOLU", "CLOTURE");
        dialog.setTitle("Changer Statut");
        dialog.setHeaderText("Mettre à jour le statut du ticket #" + ticket.getId());
        dialog.setContentText("Choisir un statut:");

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                serviceTicket.updateStatus(ticket.getId(), newStatus);
                refreshData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
