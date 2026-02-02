package com.skilora.community.controller;

import com.skilora.community.entity.Report;
import com.skilora.community.service.ReportService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLSeparator;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Loads reports from the database `reports` table.
 */
public class ReportsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ReportService reportService = ReportService.getInstance();

    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private VBox reportsContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private List<Report> allReports = new ArrayList<>();
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
        statsLabel.setText(I18n.get("common.loading"));

        Task<List<Report>> task = new Task<>() {
            @Override
            protected List<Report> call() {
                return reportService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            allReports = task.getValue();
            applyFilters();
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load reports", task.getException());
            statsLabel.setText(I18n.get("common.error"));
        });

        new Thread(task, "ReportsLoader") {{ setDaemon(true); }}.start();
    }

    private void applyFilters() {
        if (allReports == null) return;

        List<Report> filtered = allReports.stream()
            .filter(r -> "ALL".equals(currentFilter) || r.getStatus().equals(currentFilter))
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

        long pending = reports.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
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

        TLBadge typeBadge = new TLBadge(report.getType(), TLBadge.Variant.DEFAULT);
        String type = report.getType() != null ? report.getType().toLowerCase() : "";
        if (type.contains("fraud") || type.contains("fraude")) {
            typeBadge.getStyleClass().add("badge-destructive");
        } else if (type.contains("spam")) {
            typeBadge.getStyleClass().add("badge-warning");
        } else if (type.contains("inappropriate") || type.contains("inapproprié")) {
            typeBadge.getStyleClass().add("badge-destructive");
        }

        TLBadge statusBadge = new TLBadge(getStatusDisplayName(report.getStatus()), TLBadge.Variant.DEFAULT);
        if ("PENDING".equals(report.getStatus())) {
            statusBadge.getStyleClass().add("badge-warning");
        } else if ("RESOLVED".equals(report.getStatus())) {
            statusBadge.getStyleClass().add("badge-success");
        } else if ("DISMISSED".equals(report.getStatus())) {
            statusBadge.getStyleClass().add("badge-secondary");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(formatRelativeTime(report.getCreatedAt()));
        timeLabel.getStyleClass().add("text-muted");

        topRow.getChildren().addAll(typeBadge, statusBadge, spacer, timeLabel);

        // Subject
        Label subjectLabel = new Label(report.getSubject());
        subjectLabel.getStyleClass().add("h4");
        subjectLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(report.getDescription());
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Reporter info
        Label reporterLabel = new Label(I18n.get("reports.reported_by") + ": " +
            (report.getReporterName() != null ? report.getReporterName() : I18n.get("common.unknown")));
        reporterLabel.getStyleClass().add("text-muted");

        // Actions
        TLSeparator sep = new TLSeparator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if ("PENDING".equals(report.getStatus())) {
            TLButton investigateBtn = new TLButton(I18n.get("reports.examine"), TLButton.ButtonVariant.OUTLINE);
            investigateBtn.setOnAction(e -> updateReportStatus(report, "INVESTIGATING"));

            TLButton resolveBtn = new TLButton(I18n.get("reports.resolve"), TLButton.ButtonVariant.PRIMARY);
            resolveBtn.setOnAction(e -> updateReportStatus(report, "RESOLVED"));

            TLButton dismissBtn = new TLButton(I18n.get("reports.reject"), TLButton.ButtonVariant.GHOST);
            dismissBtn.setOnAction(e -> updateReportStatus(report, "DISMISSED"));

            actionsRow.getChildren().addAll(investigateBtn, resolveBtn, dismissBtn);
        } else if ("INVESTIGATING".equals(report.getStatus())) {
            TLButton resolveBtn = new TLButton(I18n.get("reports.resolve"), TLButton.ButtonVariant.PRIMARY);
            resolveBtn.setOnAction(e -> updateReportStatus(report, "RESOLVED"));
            actionsRow.getChildren().add(resolveBtn);
        }

        content.getChildren().addAll(topRow, subjectLabel, descLabel, reporterLabel, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    private void updateReportStatus(Report report, String newStatus) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return reportService.updateStatus(report.getId(), newStatus, null);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                report.setStatus(newStatus);
                Platform.runLater(this::applyFilters);
            }
        });

        task.setOnFailed(e -> logger.error("Failed to update report status", task.getException()));

        new Thread(task, "UpdateReportStatus") {{ setDaemon(true); }}.start();
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
        if (days < 7) return days + "d";
        return dateTime.format(DATE_FMT);
    }

    @FXML
    private void handleRefresh() {
        loadReports();
    }
}
