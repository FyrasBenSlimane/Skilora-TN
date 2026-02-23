package tn.esprit.skylora.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileOutputStream;
import javafx.animation.FadeTransition;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;

/**
 * Contrôleur principal du tableau de bord administrateur.
 * Affiche et gère tous les tickets du système avec les fonctionnalités
 * suivantes :
 * - Tableau de tous les tickets avec colonnes de statut, priorité, note...
 * - Filtres de recherche en temps réel par sujet, statut et priorité
 * - Cartes de statistiques (total, ouverts, résolus, en cours, note moyenne)
 * - Actions rapides : voir détails, changer statut (avec notification e-mail)
 * - Export du tableau en rapport PDF
 * - Accès à la vue statistiques graphiques
 */
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
    private Label avgRatingLabel;

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
    private TableColumn<Ticket, String> colAdminRating;
    @FXML
    private TableColumn<Ticket, Void> colAdminActions;

    private ServiceTicket serviceTicket = new ServiceTicket();
    private tn.esprit.skylora.services.ServiceFeedback serviceFeedback = new tn.esprit.skylora.services.ServiceFeedback();
    private Map<Integer, String> ticketRatingMap = new HashMap<>();
    private List<Ticket> allTickets = new java.util.ArrayList<>();
    private ObservableList<Ticket> observableTickets = FXCollections.observableArrayList();

    /**
     * Point d'entrée principal du contrôleur.
     * Configure le tableau, les filtres, puis charge les données depuis la base.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();
        refreshData();
    }

    /**
     * Configure les liaisons entre les colonnes du tableau et les attributs de
     * l'entité Ticket.
     * Définit également les rendus personnalisés pour la date, la note et les
     * boutons d'actions.
     */
    private void setupTable() {
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colAdminUser.setCellValueFactory(new PropertyValueFactory<>("utilisateurId"));
        colAdminCategory.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colAdminPriority.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colAdminRating.setCellValueFactory(cellData -> {
            String rating = ticketRatingMap.getOrDefault(cellData.getValue().getId(), "N/A");
            return new javafx.beans.property.SimpleStringProperty(rating);
        });

        // Add some style to rating
        colAdminRating.setCellFactory(column -> new TableCell<Ticket, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.equals("N/A") ? item : "★ " + item);
                    if (!item.equals("N/A")) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #9CA3AF;");
                    }
                }
            }
        });

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

    /**
     * Initialise les listes déroulantes de filtrage (statut et priorité)
     * et attache des écouteurs pour déclencher le filtrage en temps réel.
     */
    private void setupFilters() {
        adminStatusFilter.getItems().addAll("TOUS", "OUVERT", "EN_COURS", "RESOLU", "CLOTURE");
        adminPriorityFilter.getItems().addAll("TOUS", "LOW", "MEDIUM", "HIGH", "URGENT");

        adminSearchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        adminStatusFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
        adminPriorityFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
    }

    /**
     * Recharge tous les tickets depuis la base de données.
     * Met également à jour la carte des notes (ticketRatingMap) et recalcule les
     * statistiques.
     */
    @FXML
    public void refreshData() {
        try {
            allTickets = serviceTicket.afficher();

            // Fetch all feedback to populate the rating column efficiently
            List<tn.esprit.skylora.entities.Feedback> feedbacks = serviceFeedback.afficher();
            ticketRatingMap.clear();
            for (tn.esprit.skylora.entities.Feedback f : feedbacks) {
                ticketRatingMap.put(f.getTicketId(), String.valueOf(f.getRating()));
            }

            calculateStats();
            filterData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcule les statistiques globales des tickets (total, ouverts, résolus, en
     * cours)
     * et la note moyenne de tous les feedbacks, puis affiche les valeurs avec une
     * animation.
     */
    private void calculateStats() {
        try {
            long total = serviceTicket.getTotalTickets();
            long open = serviceTicket.getCountByStatus("OUVERT");
            long resolved = serviceTicket.getCountByStatus("RESOLU");
            long pending = serviceTicket.getCountByStatus("EN_COURS");

            tn.esprit.skylora.services.ServiceFeedback serviceFeedback = new tn.esprit.skylora.services.ServiceFeedback();
            double avgRating = serviceFeedback.getAverageRating();

            totalTicketsLabel.setText(String.valueOf(total));
            openTicketsLabel.setText(String.valueOf(open));
            resolvedTicketsLabel.setText(String.valueOf(resolved));
            pendingTicketsLabel.setText(String.valueOf(pending));
            avgRatingLabel.setText(String.format("%.1f", avgRating));

            // Animation des labels de stats
            animateLabel(totalTicketsLabel);
            animateLabel(openTicketsLabel);
            animateLabel(resolvedTicketsLabel);
            animateLabel(pendingTicketsLabel);
            animateLabel(avgRatingLabel);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Applique une animation de fondu (fade-in) sur un label de statistique.
     *
     * @param label Le label à animer
     */
    private void animateLabel(Label label) {
        FadeTransition ft = new FadeTransition(Duration.millis(800), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Filtre la liste des tickets affichés en fonction du texte de recherche,
     * du statut sélectionné et de la priorité sélectionnée.
     * Appelé automatiquement lors de toute modification d'un filtre.
     */
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

    /**
     * Ouvre la fenêtre de détails d'un ticket en mode lecture seule (mode
     * administrateur).
     * L'administrateur peut voir les feedbacks mais ne peut pas les modifier.
     *
     * @param ticket Le ticket à afficher
     */
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

    /**
     * Affiche un dialogue de sélection pour modifier rapidement le statut d'un
     * ticket.
     * Après la mise à jour, envoie automatiquement un e-mail de notification à
     * l'utilisateur
     * via le service EmailService, puis rafraîchit le tableau.
     *
     * @param ticket Le ticket dont le statut doit être modifié
     */
    private void handleQuickStatusUpdate(Ticket ticket) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(ticket.getStatut(), "OUVERT", "EN_COURS", "RESOLU", "CLOTURE");
        dialog.setTitle("Changer Statut");
        dialog.setHeaderText("Mettre à jour le statut du ticket #" + ticket.getId());
        dialog.setContentText("Choisir un statut:");

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                serviceTicket.updateStatus(ticket.getId(), newStatus);

                // Email Notification
                String emailSubject = "Mise à jour du Ticket #" + ticket.getId();
                String emailContent = "Bonjour,\n\nVotre ticket #" + ticket.getId() + " (" + ticket.getSubject()
                        + ") a été mis à jour.\n"
                        + "Nouveau statut : " + newStatus + ".\n\nMerci d'avoir contacté le support Skylora.";

                new Thread(() -> {
                    tn.esprit.skylora.utils.EmailService.sendEmail(emailSubject, emailContent);
                }).start();

                refreshData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Ouvre la fenêtre des statistiques graphiques (camembert et histogramme)
     * présentant une vue visuelle de la répartition des tickets.
     */
    @FXML
    private void handleViewStats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/skylora/gui/StatisticsView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Statistiques Support");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Déclenche l'export du tableau des tickets en fichier PDF.
     * Affiche une boîte de dialogue pour choisir l'emplacement de sauvegarde,
     * génère le PDF en arrière-plan et notifie l'utilisateur du résultat.
     */
    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("rapport_tickets_" + java.time.LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Stage stage = (Stage) adminTicketTable.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file == null)
            return;

        new Thread(() -> {
            try {
                generatePDF(file, observableTickets);
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Exportation réussie");
                    alert.setHeaderText(null);
                    alert.setContentText("Le PDF a été exporté avec succès !\n" + file.getAbsolutePath());
                    alert.showAndWait();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setContentText("Erreur lors de l'exportation : " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void generatePDF(File file, List<Ticket> tickets) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // ── Title ──
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.decode("#1E293B"));
        Paragraph title = new Paragraph("Rapport des Tickets de Support - Skylora", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.decode("#6B7280"));
        Paragraph sub = new Paragraph("Généré le : " + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        document.add(sub);

        // ── Table ──
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.5f, 4f, 2f, 3f, 2.5f, 2.5f, 3f, 2f });

        // Header style
        Color headerBg = Color.decode("#1E293B");
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        String[] headers = { "ID", "Sujet", "Utilisateur", "Catégorie", "Priorité", "Statut", "Date Création", "Note" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(Color.decode("#334155"));
            table.addCell(cell);
        }

        // Row data
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        boolean alternate = false;
        for (Ticket t : tickets) {
            Color rowColor = alternate ? Color.decode("#F8FAFC") : Color.WHITE;
            alternate = !alternate;

            String[] values = {
                    String.valueOf(t.getId()),
                    t.getSubject(),
                    String.valueOf(t.getUtilisateurId()),
                    t.getCategorie(),
                    t.getPriorite(),
                    t.getStatut(),
                    t.getDateCreation() != null ? t.getDateCreation().format(fmt) : "-",
                    ticketRatingMap.getOrDefault(t.getId(), "N/A")
            };

            for (String val : values) {
                PdfPCell cell = new PdfPCell(new Phrase(val, rowFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(Color.decode("#E2E8F0"));
                table.addCell(cell);
            }
        }

        document.add(table);

        // ── Footer ──
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.decode("#9CA3AF"));
        Paragraph footer = new Paragraph("Total Tickets : " + tickets.size() + " | Skylora Support System", footerFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(15);
        document.add(footer);

        document.close();
    }
}
