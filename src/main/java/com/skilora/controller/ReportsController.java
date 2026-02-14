package com.skilora.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

/**
 * ReportsController - Admin signalements/reports management.
 * Displays reports and flags submitted by users or system.
 */
public class ReportsController implements Initializable {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private VBox reportsContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private List<Report> allReports;
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        loadReports();
    }

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        String[][] filters = {
            {I18n.get("reports.filter.all"), "ALL"},
            {I18n.get("reports.filter.pending"), "PENDING"},
            {I18n.get("reports.filter.in_progress"), "INVESTIGATING"},
            {I18n.get("reports.filter.resolved"), "RESOLVED"},
            {I18n.get("reports.filter.rejected"), "DISMISSED"}
        };

        for (String[] f : filters) {
            ToggleButton btn = new ToggleButton(f[0]);
            btn.setUserData(f[1]);
            btn.getStyleClass().add("chip-filter");
            btn.setToggleGroup(filterGroup);
            if ("ALL".equals(f[1])) btn.setSelected(true);

            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    currentFilter = (String) btn.getUserData();
                    applyFilters();
                } else if (filterGroup.getSelectedToggle() == null) {
                    btn.setSelected(true);
                }
            });
            filterBox.getChildren().add(btn);
        }
    }

    private void loadReports() {
        allReports = getSampleReports();
        applyFilters();
    }

    private void applyFilters() {
        if (allReports == null) return;

        List<Report> filtered = allReports.stream()
            .filter(r -> "ALL".equals(currentFilter) || r.status.equals(currentFilter))
            .collect(Collectors.toList());

        renderReports(filtered);
    }

    private void renderReports(List<Report> reports) {
        reportsContainer.getChildren().clear();

        if (reports.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("reports.count.zero"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        long pending = reports.stream().filter(r -> "PENDING".equals(r.status)).count();
        statsLabel.setText(I18n.get("reports.count", reports.size()) +
            (pending > 0 ? " (" + I18n.get("reports.pending_count", pending) + ")" : ""));

        for (Report report : reports) {
            reportsContainer.getChildren().add(createReportCard(report));
        }
    }

    private TLCard createReportCard(Report report) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Top row: Type badge + Status + Time
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TLBadge typeBadge = new TLBadge(report.type, TLBadge.Variant.DEFAULT);
        switch (report.type) {
            case "Fraude":
                typeBadge.getStyleClass().add("badge-destructive");
                break;
            case "Spam":
                typeBadge.getStyleClass().add("badge-warning");
                break;
            case "Contenu inapproprié":
                typeBadge.getStyleClass().add("badge-destructive");
                break;
        }

        TLBadge statusBadge = new TLBadge(getStatusDisplayName(report.status), TLBadge.Variant.DEFAULT);
        if ("PENDING".equals(report.status)) {
            statusBadge.getStyleClass().add("badge-warning");
        } else if ("RESOLVED".equals(report.status)) {
            statusBadge.getStyleClass().add("badge-success");
        } else if ("DISMISSED".equals(report.status)) {
            statusBadge.getStyleClass().add("badge-secondary");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(formatRelativeTime(report.reportedAt));
        timeLabel.getStyleClass().add("text-muted");

        topRow.getChildren().addAll(typeBadge, statusBadge, spacer, timeLabel);

        // Subject
        Label subjectLabel = new Label(report.subject);
        subjectLabel.getStyleClass().add("h4");
        subjectLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(report.description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Reporter info
        Label reporterLabel = new Label(I18n.get("reports.reported_by") + ": " + report.reporterName);
        reporterLabel.getStyleClass().add("text-muted");

        // Actions
        Separator sep = new Separator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if ("PENDING".equals(report.status)) {
            TLButton investigateBtn = new TLButton(I18n.get("reports.examine"), TLButton.ButtonVariant.OUTLINE);
            investigateBtn.setOnAction(e -> {
                report.status = "INVESTIGATING";
                applyFilters();
            });

            TLButton resolveBtn = new TLButton(I18n.get("reports.resolve"), TLButton.ButtonVariant.PRIMARY);
            resolveBtn.setOnAction(e -> {
                report.status = "RESOLVED";
                applyFilters();
            });

            TLButton dismissBtn = new TLButton(I18n.get("reports.reject"), TLButton.ButtonVariant.GHOST);
            dismissBtn.setOnAction(e -> {
                report.status = "DISMISSED";
                applyFilters();
            });

            actionsRow.getChildren().addAll(investigateBtn, resolveBtn, dismissBtn);
        } else if ("INVESTIGATING".equals(report.status)) {
            TLButton resolveBtn = new TLButton(I18n.get("reports.resolve"), TLButton.ButtonVariant.PRIMARY);
            resolveBtn.setOnAction(e -> {
                report.status = "RESOLVED";
                applyFilters();
            });
            actionsRow.getChildren().add(resolveBtn);
        }

        content.getChildren().addAll(topRow, subjectLabel, descLabel, reporterLabel, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    private String getStatusDisplayName(String status) {
        switch (status) {
            case "PENDING": return I18n.get("reports.status.pending");
            case "INVESTIGATING": return I18n.get("reports.status.in_progress");
            case "RESOLVED": return I18n.get("reports.status.resolved");
            case "DISMISSED": return I18n.get("reports.status.rejected");
            default: return status;
        }
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return I18n.get("reports.time.now");
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        if (days < 7) return days + "j";
        return dateTime.format(DATE_FMT);
    }

    @FXML
    private void handleRefresh() {
        loadReports();
    }

    /**
     * Sample reports data.
     * In production, these would come from a database table.
     */
    private List<Report> getSampleReports() {
        List<Report> reports = new ArrayList<>();

        reports.add(new Report("Offre d'emploi suspecte", "Fraude",
            "L'offre 'Gagnez 5000 TND par jour' semble être une arnaque. Demande un paiement initial.",
            "Ahmed Ben Ali", LocalDateTime.now().minusHours(2), "PENDING"));

        reports.add(new Report("Profil avec contenu inapproprié", "Contenu inapproprié",
            "Le profil utilisateur contient des images et descriptions non professionnelles.",
            "Sarah Mansour", LocalDateTime.now().minusHours(5), "PENDING"));

        reports.add(new Report("Messages spam répétitifs", "Spam",
            "Cet utilisateur envoie des messages promotionnels non sollicités aux candidats.",
            "Mohamed Trabelsi", LocalDateTime.now().minusDays(1), "INVESTIGATING"));

        reports.add(new Report("Entreprise non vérifiée", "Fraude",
            "L'entreprise 'TechFake Corp' n'existe pas selon le registre national des entreprises.",
            "Fatma Gharbi", LocalDateTime.now().minusDays(2), "RESOLVED"));

        reports.add(new Report("Discrimination dans l'offre", "Contenu inapproprié",
            "L'offre d'emploi contient des critères de sélection discriminatoires basés sur le genre.",
            "Leila Hamdi", LocalDateTime.now().minusDays(3), "RESOLVED"));

        reports.add(new Report("Copie d'offre existante", "Spam",
            "Cette offre est une copie exacte d'une autre offre publiée par une entreprise différente.",
            "Karim Bouzid", LocalDateTime.now().minusDays(5), "DISMISSED"));

        return reports;
    }

    /**
     * Report data model.
     * In production, this would be a proper entity with DB persistence.
     */
    static class Report {
        String subject;
        String type;
        String description;
        String reporterName;
        LocalDateTime reportedAt;
        String status;

        Report(String subject, String type, String description, String reporterName,
               LocalDateTime reportedAt, String status) {
            this.subject = subject;
            this.type = type;
            this.description = description;
            this.reporterName = reporterName;
            this.reportedAt = reportedAt;
            this.status = status;
        }
    }
}
