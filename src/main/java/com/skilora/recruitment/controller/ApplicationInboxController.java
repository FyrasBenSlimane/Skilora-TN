package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLTable;
import com.skilora.framework.components.TLTextField;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.RecruitmentIntelligenceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

/**
 * ApplicationInboxController - Manage incoming job applications (Employer view)
 * Content from src 2 recruitment.
 */
public class ApplicationInboxController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInboxController.class);

    @FXML
    private Label statsLabel;
    @FXML
    private TLButton refreshBtn;

    @FXML
    private TLTextField searchField;

    @FXML
    private TLButton resetBtn;

    @FXML
    private TLTable<ApplicationRow> applicationsTable;
    @FXML
    private TableColumn<ApplicationRow, String> candidateCol;
    @FXML
    private TableColumn<ApplicationRow, String> jobCol;
    @FXML
    private TableColumn<ApplicationRow, String> dateCol;
    @FXML
    private TableColumn<ApplicationRow, String> matchCol;
    @FXML
    private TableColumn<ApplicationRow, String> scoreCol;
    @FXML
    private TableColumn<ApplicationRow, String> statusCol;
    @FXML
    private TableColumn<ApplicationRow, Void> actionsCol;

    @FXML
    private Label paginationLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private TLButton prevBtn;
    @FXML
    private TLButton nextBtn;

    @FXML
    private ToggleGroup statusGroup;
    @FXML
    private ToggleButton allStatusBtn;
    @FXML
    private HBox statusFilterBox;

    private User currentUser;
    private ObservableList<ApplicationRow> applications;
    private ObservableList<ApplicationRow> allApplications;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final JobService jobService = JobService.getInstance();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean isLoading = false;
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
    }

    private void setupFilters() {
        resetBtn.setText(I18n.get("inbox.reset"));
        searchField.setPromptText(I18n.get("inbox.search_placeholder"));
        searchField.getControl().textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        if (statusFilterBox != null && statusGroup != null) {
            statusFilterBox.getChildren().clear();
            String[][] filters = {
                {I18n.get("inbox.filter.all",       "Toutes"),      null},
                {I18n.get("inbox.status.pending",   "En attente"),  "PENDING"},
                {I18n.get("inbox.status.review",    "En révision"), "REVIEW"},
                {I18n.get("inbox.status.interview", "Entretien"),   "INTERVIEW"},
                {I18n.get("inbox.status.accepted",  "Accepté"),     "ACCEPTED"},
                {I18n.get("inbox.status.rejected",  "Refusé"),      "REJECTED"},
            };
            for (String[] f : filters) {
                ToggleButton btn = new ToggleButton(f[0]);
                btn.setToggleGroup(statusGroup);
                btn.setUserData(f[1]);
                btn.getStyleClass().add("filter-chip");
                statusFilterBox.getChildren().add(btn);
                if (f[1] == null) {
                    btn.setSelected(true);
                    this.allStatusBtn = btn;
                }
            }
        }
        if (statusGroup != null) {
            statusGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null && oldVal != null) {
                    statusGroup.selectToggle(oldVal);
                    return;
                }
                applyFilters();
            });
        }
    }

    public void setCurrentUser(User user) {
        if (this.currentUser != null && user != null && this.currentUser.getId() == user.getId()) {
            loadApplications();
            return;
        }
        this.currentUser = user;
        if (user != null) {
            logger.info("Loading applications for employer user ID: {}", user.getId());
            debugDatabaseState();
            int appCount = com.skilora.utils.DataFixer.verifyApplicationsForEmployer(user.getId());
            if (appCount == 0) {
                logger.info("No applications found for employer. Attempting to link applications by job title...");
                int fixed = com.skilora.utils.DataFixer.fixApplicationsForEmployer(user.getId());
                if (fixed > 0) logger.info("Linked {} application(s) to your offers. Reloading...", fixed);
            }
            loadApplications();
        } else {
            logger.warn("setCurrentUser called with null user");
            if (applications != null) applications.clear();
            if (applicationsTable != null && applicationsTable.getItems() != null) applicationsTable.getItems().clear();
        }
    }

    private void debugDatabaseState() {
        if (currentUser == null) return;
        logger.info("DIAGNOSTIC: Debugging database state for employer user ID: {}", currentUser.getId());
        try (java.sql.Connection conn = com.skilora.config.DatabaseConfig.getInstance().getConnection()) {
            try (java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as total FROM applications");
                    java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) logger.info("DIAGNOSTIC: Total applications in DB: {}", rs.getInt("total"));
            }
            int companyId = jobService.getOrCreateEmployerCompanyId(currentUser.getId(), "Entreprise");
            logger.info("DIAGNOSTIC: Resolved Company ID: {} for owner {}", companyId, currentUser.getId());
            if (companyId > 0) {
                try (java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as cnt FROM job_offers WHERE company_id = ?")) {
                    stmt.setInt(1, companyId);
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) logger.info("DIAGNOSTIC: Job offers for this company: {}", rs.getInt("cnt"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("DIAGNOSTIC error", e);
        }
    }

    private void setupTable() {
        candidateCol.setCellValueFactory(new PropertyValueFactory<>("candidateName"));
        candidateCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                if (empty || value == null) { setText(null); return; }
                setText(value);
                setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                if (value.length() > 18) Tooltip.install(this, new Tooltip(value));
            }
        });

        jobCol.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        jobCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                if (empty || value == null) { setText(null); return; }
                setText(value);
                setWrapText(true);
                setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                Tooltip tip = new Tooltip(value);
                tip.setWrapText(true);
                tip.setMaxWidth(340);
                Tooltip.install(this, tip);
            }
        });

        dateCol.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        dateCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                if (empty || value == null) { setText(null); return; }
                setText(value);
                setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                setAlignment(Pos.CENTER);
            }
        });

        scoreCol.setCellValueFactory(new PropertyValueFactory<>("candidateScore"));
        Label scoreHeader = new Label("Score profil");
        scoreHeader.setTooltip(new Tooltip("Qualité globale du profil du candidat (completude, compétences, expérience)."));
        scoreCol.setGraphic(scoreHeader);
        scoreCol.setText(null);

        matchCol.setCellValueFactory(new PropertyValueFactory<>("matchScore"));
        matchCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                setText(null);
                if (empty || value == null || "-".equals(value)) return;
                int score = 0;
                try { score = Integer.parseInt(value.replace("%", "").trim()); } catch (NumberFormatException ignored) {}
                String colour = score >= 85 ? "#16a34a" : score >= 70 ? "#2563eb" : score >= 50 ? "#d97706" : score >= 30 ? "#ea580c" : "#dc2626";
                Label badge = new Label(value);
                badge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                badge.setStyle("-fx-background-color:" + colour + "; -fx-text-fill:white; -fx-padding:3 10; -fx-background-radius:6; -fx-font-weight:bold; -fx-font-size:12;");
                HBox wrapper = new HBox(badge);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setAlignment(Pos.CENTER);
            }
        });

        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setGraphic(null);
                setText(null);
                if (empty || status == null) return;
                String label; String bg; String fg;
                switch (status) {
                    case "INTERVIEW": label = "Entretien";  bg = "#1e3a5f"; fg = "#bae6fd"; break;
                    case "REVIEW": case "REVIEWING": label = "En révision"; bg = "#312e81"; fg = "#c7d2fe"; break;
                    case "OFFER": label = "Offre faite"; bg = "#064e3b"; fg = "#a7f3d0"; break;
                    case "ACCEPTED": label = "Accepté";    bg = "#14532d"; fg = "#bbf7d0"; break;
                    case "REJECTED": label = "Refusé";     bg = "#7f1d1d"; fg = "#fca5a5"; break;
                    case "PENDING": default: label = "En attente"; bg = "#27272a"; fg = "#a1a1aa"; break;
                }
                Label badge = new Label(label);
                badge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-padding:3 10; -fx-background-radius:6; -fx-font-weight:bold; -fx-font-size:11;");
                HBox wrapper = new HBox(badge);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setAlignment(Pos.CENTER);
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<ApplicationRow, Void>() {
            private final TLButton moreBtn = new TLButton("⋮");
            { moreBtn.setVariant(TLButton.ButtonVariant.GHOST); }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    ApplicationRow app = getTableView().getItems().get(getIndex());
                    moreBtn.setOnAction(e -> {
                        TLDropdownMenu menu = new TLDropdownMenu();
                        menu.addItem(I18n.get("inbox.view_profile"), ev -> handleViewProfile(app));
                        menu.addItem(I18n.get("inbox.review"), ev -> handleReview(app));
                        menu.addItem(I18n.get("inbox.accept"), ev -> handleAccept(app));
                        menu.addItem(I18n.get("inbox.reject"), ev -> handleReject(app));
                        menu.show(moreBtn, javafx.geometry.Side.BOTTOM, 0, 4);
                    });
                    setGraphic(moreBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void loadApplications() {
        if (isLoading) return;
        isLoading = true;
        if (applications != null) applications.clear();
        if (applicationsTable != null && applicationsTable.getItems() != null) applicationsTable.getItems().clear();
        if (statsLabel != null) statsLabel.setText(I18n.get("common.loading"));

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                int companyId = jobService.getOrCreateEmployerCompanyId(currentUser.getId(), "Entreprise");
                if (companyId <= 0) return java.util.Collections.emptyList();
                com.skilora.utils.DataFixer.fixApplicationsForEmployer(currentUser.getId());
                List<Application> apps = applicationService.getApplicationsByCompanyId(companyId);
                for (Application app : apps) {
                    if (app.getMatchPercentage() <= 0) {
                        int match = intelligenceService.calculateCompatibility(app.getCandidateProfileId(), app.getJobOfferId()).join();
                        app.setMatchPercentage(match);
                    }
                    if (app.getCandidateScore() <= 0) {
                        int score = intelligenceService.scoreCandidate(app.getCandidateProfileId()).join();
                        app.setCandidateScore(score);
                    }
                }
                apps.sort((a, b) -> Integer.compare((b.getMatchPercentage() + b.getCandidateScore()), (a.getMatchPercentage() + a.getCandidateScore())));
                return apps;
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;
            List<Application> dbApps = task.getValue();
            if (applications != null) applications.clear();
            applications = FXCollections.observableArrayList();
            if (dbApps == null || dbApps.isEmpty()) {
                isLoading = false;
                logger.warn("No applications found for employer ID: {}", currentUser.getId());
                if (applications != null) applications.clear();
                applications = FXCollections.observableArrayList();
                allApplications = FXCollections.observableArrayList(applications);
                applyFilters();
                updateStats();
                updatePagination();
                if (statsLabel != null) statsLabel.setText(I18n.get("inbox.pending", 0) + " — " + I18n.get("inbox.empty.hint", "Les candidatures pour vos offres apparaîtront ici."));
                return;
            } else {
                for (Application app : dbApps) {
                    String displayStatus = mapStatusToI18nKey(app.getStatus());
                    applications.add(new ApplicationRow(
                            app.getId(),
                            app.getCandidateName() != null ? app.getCandidateName() : I18n.get("inbox.candidate_num", app.getCandidateProfileId()),
                            app.getJobTitle() != null ? app.getJobTitle() : I18n.get("inbox.offer_num", app.getJobOfferId()),
                            app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "",
                            app.getMatchPercentage() > 0 ? app.getMatchPercentage() + "%" : "-",
                            app.getCandidateScore() > 0 ? String.valueOf(app.getCandidateScore()) : "-",
                            displayStatus));
                }
            }
            allApplications = FXCollections.observableArrayList(applications);
            applyFilters();
            updateStats();
            updatePagination();
        });

        task.setOnFailed(e -> {
            isLoading = false;
            if (statsLabel != null) statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
        });

        Thread t = new Thread(task, "AppLoadThread");
        t.setDaemon(true);
        t.start();
    }

    private String mapStatusToI18nKey(Status status) {
        if (status == null) return "PENDING";
        switch (status) {
            case PENDING: return "PENDING";
            case REVIEWING: return "REVIEW";
            case INTERVIEW: return "INTERVIEW";
            case OFFER: return "OFFER";
            case ACCEPTED: return "ACCEPTED";
            case REJECTED: return "REJECTED";
            default: return "PENDING";
        }
    }

    private void updateStats() {
        if (applications == null || statsLabel == null) return;
        long pending = applications.stream().filter(a -> "PENDING".equals(a.getStatus()) || "REVIEW".equals(a.getStatus())).count();
        statsLabel.setText(I18n.get("inbox.pending", pending));
    }

    private void updatePagination() {
        int total = applications == null ? 0 : applications.size();
        if (total == 0) {
            if (paginationLabel != null) paginationLabel.setText("0 sur 0");
            if (pageLabel != null) pageLabel.setText("Page 1");
            if (prevBtn != null) prevBtn.setDisable(true);
            if (nextBtn != null) nextBtn.setDisable(true);
            return;
        }
        int start = currentPage * itemsPerPage + 1;
        int end = Math.min((currentPage + 1) * itemsPerPage, total);
        if (paginationLabel != null) paginationLabel.setText(start + "-" + end + " " + I18n.get("inbox.of") + " " + total);
        if (pageLabel != null) pageLabel.setText("Page " + (currentPage + 1));
        if (prevBtn != null) prevBtn.setDisable(currentPage == 0);
        if (nextBtn != null) nextBtn.setDisable(end >= total);
    }

    private void handleViewProfile(ApplicationRow app) {
        try {
            Application fullApp = null;
            try {
                fullApp = applicationService.getApplicationById(app.getApplicationId());
            } catch (Exception ex) {
                logger.error("Error loading application by ID: {}", app.getApplicationId(), ex);
            }
            if (fullApp == null) {
                List<Application> allApps = applicationService.getApplicationsByCompanyOwner(currentUser.getId());
                fullApp = allApps.stream().filter(a -> a.getId() == app.getApplicationId()).findFirst().orElse(null);
            }
            if (fullApp == null) {
                com.skilora.utils.DialogUtils.showError("Erreur", "Candidature introuvable.");
                return;
            }
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Détails de la candidature");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            if (applicationsTable.getScene() != null && applicationsTable.getScene().getWindow() != null)
                dialogStage.initOwner(applicationsTable.getScene().getWindow());
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/ApplicationDetailsView.fxml"));
            javafx.scene.layout.VBox root = loader.load();
            ApplicationDetailsController controller = loader.getController();
            if (controller != null) {
                controller.setup(fullApp, dialogStage, newStatus -> {
                    applications.stream().filter(r -> r.getApplicationId() == app.getApplicationId()).findFirst()
                            .ifPresent(r -> {
                                r.setStatus(mapStatusToI18nKey(newStatus));
                                javafx.application.Platform.runLater(() -> applicationsTable.refresh());
                            });
                });
            }
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 960, 720);
            scene.getStylesheets().addAll(applicationsTable.getScene().getStylesheets());
            dialogStage.setMinWidth(860);
            dialogStage.setMinHeight(600);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            loadApplications();
        } catch (Exception e) {
            logger.error("Failed to show application details", e);
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible d'afficher les détails: " + e.getMessage());
        }
    }

    private void handleAccept(ApplicationRow app) {
        new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.ACCEPTED);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("ACCEPTED");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) { logger.error("Failed to accept application", ex); }
        }, "AcceptAppThread").start();
    }

    private void handleReview(ApplicationRow app) {
        new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.REVIEWING);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("REVIEW");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) { logger.error("Failed to set to reviewing", ex); }
        }, "ReviewAppThread").start();
    }

    private void handleReject(ApplicationRow app) {
        new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.REJECTED);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("REJECTED");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) { logger.error("Failed to reject application", ex); }
        }, "RejectAppThread").start();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) searchField.setText("");
        if (allStatusBtn != null) allStatusBtn.setSelected(true);
        applyFilters();
    }

    @FXML
    private void handleRefresh() {
        if (currentUser != null) loadApplications();
    }

    @FXML
    private void applyFilters() {
        if (allApplications == null) return;
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedStatus = null;
        if (statusGroup != null && statusGroup.getSelectedToggle() != null) {
            Object userData = statusGroup.getSelectedToggle().getUserData();
            if (userData != null) selectedStatus = userData.toString();
        }
        final String finalSelectedStatus = selectedStatus;
        List<ApplicationRow> filtered = allApplications.stream().filter(app -> {
            if (!searchText.isEmpty() && !app.getCandidateName().toLowerCase().contains(searchText)) return false;
            if (finalSelectedStatus != null && !finalSelectedStatus.isEmpty()) {
                String appStatus = app.getStatus();
                if (finalSelectedStatus.equals("REVIEW"))
                    return "REVIEW".equals(appStatus) || "REVIEWING".equals(appStatus) || "INTERVIEW".equals(appStatus) || "OFFER".equals(appStatus);
                if (!appStatus.equals(finalSelectedStatus)) return false;
            }
            return true;
        }).collect(Collectors.toList());
        applications = FXCollections.observableArrayList(filtered);
        applicationsTable.setItems(applications);
        currentPage = 0;
        updateStats();
        updatePagination();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; updatePagination(); }
    }

    @FXML
    private void handleNextPage() {
        int maxPage = (applications == null || applications.isEmpty()) ? 0 : (applications.size() - 1) / itemsPerPage;
        if (currentPage < maxPage) { currentPage++; updatePagination(); }
    }

    public static class ApplicationRow {
        private int applicationId;
        private String candidateName;
        private String jobTitle;
        private String applicationDate;
        private String matchScore;
        private String candidateScore;
        private String status;

        public ApplicationRow(int applicationId, String candidateName, String jobTitle, String applicationDate,
                String matchScore, String candidateScore, String status) {
            this.applicationId = applicationId;
            this.candidateName = candidateName;
            this.jobTitle = jobTitle;
            this.applicationDate = applicationDate;
            this.matchScore = matchScore;
            this.candidateScore = candidateScore;
            this.status = status;
        }

        public int getApplicationId() { return applicationId; }
        public String getCandidateName() { return candidateName; }
        public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public String getApplicationDate() { return applicationDate; }
        public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
        public String getMatchScore() { return matchScore; }
        public void setMatchScore(String matchScore) { this.matchScore = matchScore; }
        public String getCandidateScore() { return candidateScore; }
        public void setCandidateScore(String candidateScore) { this.candidateScore = candidateScore; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
