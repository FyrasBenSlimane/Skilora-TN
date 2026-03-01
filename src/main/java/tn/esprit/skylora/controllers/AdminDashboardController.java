package tn.esprit.skylora.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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
    private Label avgResolutionTimeLabel;
    @FXML
    private Label topCategoryLabel;

    @FXML
    private javafx.scene.control.Button adminMicBtn;

    private tn.esprit.skylora.utils.AudioRecorder recorder = new tn.esprit.skylora.utils.AudioRecorder();

    @FXML
    private javafx.scene.control.TextField adminSearchField;
    @FXML
    private ComboBox<String> adminStatusFilter;
    @FXML
    private ComboBox<String> adminPriorityFilter;

    @FXML
    private TableView<Ticket> adminTicketTable;
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

    // Ajoute les boutons d'action (voir, changer statut, supprimer) dans chaque
    // ligne
    private void setupActionsColumn() {
        Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>> cellFactory = new Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>>() {
            @Override
            public TableCell<Ticket, Void> call(final TableColumn<Ticket, Void> param) {
                return new TableCell<Ticket, Void>() {
                    private final Button viewBtn = new Button("👁");
                    private final Button statusBtn = new Button("⚙");
                    private final Button suppBtn = new Button("🗑");
                    private final HBox pane = new HBox(5, viewBtn, statusBtn, suppBtn);

                    {
                        viewBtn.setStyle(
                                "-fx-background-color: #10B981; -fx-text-fill: white; -fx-cursor: hand; -fx-min-width: 35px; -fx-padding: 5px 10px;");
                        statusBtn.setStyle(
                                "-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-cursor: hand; -fx-min-width: 35px; -fx-padding: 5px 10px;");
                        suppBtn.setStyle(
                                "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand; -fx-min-width: 60px; -fx-padding: 5px 10px;");

                        viewBtn.setOnAction(event -> {
                            Ticket ticket = getTableView().getItems().get(getIndex());
                            handleViewTicket(ticket);
                        });

                        statusBtn.setOnAction(event -> {
                            Ticket ticket = getTableView().getItems().get(getIndex());
                            handleQuickStatusUpdate(ticket);
                        });

                        suppBtn.setOnAction(event -> {
                            Ticket ticket = getTableView().getItems().get(getIndex());
                            handleDeleteTicket(ticket);
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
        adminStatusFilter.setValue("TOUS");
        adminPriorityFilter.setValue("TOUS");

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
            // Clear previous data first so the table never shows stale rows
            allTickets.clear();
            observableTickets.clear();
            adminTicketTable.getItems().clear();

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

            // Extra Stats Calculation
            calculateExtraStats();

            // Animation des labels de stats
            animateLabel(totalTicketsLabel);
            animateLabel(openTicketsLabel);
            animateLabel(resolvedTicketsLabel);
            animateLabel(pendingTicketsLabel);
            animateLabel(avgRatingLabel);
            animateLabel(avgResolutionTimeLabel);
            animateLabel(topCategoryLabel);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculateExtraStats() {
        if (allTickets == null || allTickets.isEmpty()) {
            avgResolutionTimeLabel.setText("0h");
            topCategoryLabel.setText("N/A");
            return;
        }

        // 1. Avg Resolution Time (for RESOLVED tickets)
        long totalHours = 0;
        int resolvedCount = 0;

        for (Ticket t : allTickets) {
            if ("RESOLU".equals(t.getStatut()) && t.getDateCreation() != null && t.getDateResolution() != null) {
                java.time.Duration duration = java.time.Duration.between(t.getDateCreation(), t.getDateResolution());
                totalHours += duration.toHours();
                resolvedCount++;
            }
        }

        if (resolvedCount > 0) {
            long avgHours = totalHours / resolvedCount;
            if (avgHours > 24) {
                avgResolutionTimeLabel.setText((avgHours / 24) + "j");
            } else {
                avgResolutionTimeLabel.setText(avgHours + "h");
            }
        } else {
            avgResolutionTimeLabel.setText("0h");
        }

        // 2. Most Active Category
        Map<String, Long> categoryCount = allTickets.stream()
                .filter(t -> t.getCategorie() != null)
                .collect(Collectors.groupingBy(Ticket::getCategorie, Collectors.counting()));

        String topCat = categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        topCategoryLabel.setText(topCat);
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
        String search = (adminSearchField.getText() != null ? adminSearchField.getText() : "").toLowerCase();
        String status = adminStatusFilter.getValue();
        String priority = adminPriorityFilter.getValue();

        List<Ticket> source = allTickets != null ? allTickets : java.util.Collections.emptyList();
        List<Ticket> filtered = source.stream()
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

            MainShellController.getInstance().loadViewCustom(root);
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
     * Supprime un ticket après confirmation. Rafraîchit le tableau après
     * suppression.
     *
     * @param ticket Le ticket à supprimer
     */
    private void handleDeleteTicket(Ticket ticket) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le ticket");
        confirm.setHeaderText("Supprimer le ticket #" + ticket.getId() + " ?");
        confirm.setContentText("Sujet : " + ticket.getSubject() + "\n\nCette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    serviceTicket.supprimer(ticket.getId());
                    refreshData();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Erreur");
                    error.setHeaderText(null);
                    error.setContentText("Impossible de supprimer le ticket : " + e.getMessage());
                    error.showAndWait();
                }
            }
        });
    }

    /**
     * Ouvre la fenêtre des statistiques graphiques (camembert et histogramme)
     * présentant une vue visuelle de la répartition des tickets.
     */
    @FXML
    private void handleViewStats() {
        MainShellController.getInstance().loadView("/tn/esprit/skylora/gui/StatisticsView.fxml");
    }

    @FXML
    private void handleSpeechToText() {
        if (!recorder.isRecording()) {
            try {
                recorder.start();
                adminMicBtn.setText("🛑");
                adminMicBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 18px;");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            byte[] audioData = recorder.stop();
            adminMicBtn.setText("🎤");
            adminMicBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: -fx-base-primary; -fx-font-size: 18px;");

            new Thread(() -> {
                String transcript = tn.esprit.skylora.utils.DeepgramService.transcribe(audioData);
                javafx.application.Platform.runLater(() -> {
                    if (transcript != null && !transcript.isEmpty()) {
                        adminSearchField.setText(transcript);
                        filterData();
                    }
                });
            }).start();
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
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(59, 130, 246));
        Paragraph title = new Paragraph("🌟 RAPPORT DES TICKETS DE SUPPORT - SKYLORA 🌟", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(251, 191, 36));
        Paragraph sub = new Paragraph("✨ Généré le : " + java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " ✨", subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        document.add(sub);

        // ── Table ──
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 4f, 2f, 3f, 2.5f, 2.5f, 3f, 2f });

        // Header style
        Color headerBg = new Color(59, 130, 246);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        String[] headers = { "Sujet", "Utilisateur", "Catégorie", "Priorité", "Statut", "Date Création", "Note" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(new Color(30, 64, 175));
            table.addCell(cell);
        }

        // Row data
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(55, 65, 81));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        boolean alternate = false;
        for (Ticket t : tickets) {
            Color rowColor = alternate ? new Color(240, 249, 255) : Color.WHITE;
            alternate = !alternate;

            String[] values = {
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
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(new Color(226, 232, 240));
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
