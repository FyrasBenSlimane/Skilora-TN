package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLTable;
import com.skilora.framework.components.TLTextField;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Application.Status;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;
import com.skilora.service.recruitment.JobService;
import com.skilora.service.recruitment.RecruitmentIntelligenceService;

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

import com.skilora.utils.I18n;

/**
 * ApplicationInboxController - Manage incoming job applications (Employer view)
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
    private ObservableList<ApplicationRow> allApplications; // Full unfiltered list
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean isLoading = false; // Prevent multiple simultaneous loads
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        // Data loads after setCurrentUser is called
    }

    private void setupFilters() {
        // Set i18n texts
        resetBtn.setText(I18n.get("inbox.reset"));
        searchField.setPromptText(I18n.get("inbox.search_placeholder"));

        // Search field real-time filtering
        searchField.getControl().textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Build all status filter toggle buttons dynamically
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
                btn.setUserData(f[1]); // null = all
                btn.getStyleClass().add("filter-chip");
                statusFilterBox.getChildren().add(btn);
                if (f[1] == null) {
                    btn.setSelected(true); // "Toutes" is selected by default
                    this.allStatusBtn = btn;
                }
            }
        }

        // Status chip filtering
        if (statusGroup != null) {
            statusGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                // Prevent deselecting all buttons
                if (newVal == null && oldVal != null) {
                    statusGroup.selectToggle(oldVal);
                    return;
                }
                applyFilters();
            });
        }
    }

    public void setCurrentUser(User user) {
        // Only reload if user changed or is null (to prevent duplicate loads)
        if (this.currentUser != null && user != null && this.currentUser.getId() == user.getId()) {
            // Same user, just refresh the data
            loadApplications();
            return;
        }

        this.currentUser = user;
        if (user != null) {
            logger.info("Loading applications for employer user ID: {}", user.getId());
            // Debug: Check database state first
            debugDatabaseState();

            // Try to fix data if no applications found
            int appCount = com.skilora.utils.DataFixer.verifyApplicationsForEmployer(user.getId());
            if (appCount == 0) {
                logger.info("No applications found. Attempting to fix data relationships...");
                int fixed = com.skilora.utils.DataFixer.fixApplicationsForEmployer(user.getId());
                if (fixed > 0) {
                    logger.info("Fixed {} job offers. Reloading applications...", fixed);
                }
            }

            loadApplications();
        } else {
            logger.warn("setCurrentUser called with null user");
            // Clear data when user is null
            if (applications != null) {
                applications.clear();
            }
            if (applicationsTable != null && applicationsTable.getItems() != null) {
                applicationsTable.getItems().clear();
            }
        }
    }

    private void debugDatabaseState() {
        if (currentUser == null)
            return;
        logger.info("DIAGNOSTIC: Debugging database state for employer user ID: {}", currentUser.getId());
        try (java.sql.Connection conn = com.skilora.config.DatabaseConfig.getInstance().getConnection()) {
            // 1. Total applications check
            try (java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as total FROM applications");
                    java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    logger.info("DIAGNOSTIC: Total applications in DB: {}", rs.getInt("total"));
            }

            // 2. Resolve company
            int companyId = JobService.getInstance().getOrCreateEmployerCompanyId(currentUser.getId(), "Entreprise");
            logger.info("DIAGNOSTIC: Resolved Company ID: {} for owner {}", companyId, currentUser.getId());

            if (companyId > 0) {
                // 3. Check jobs
                try (java.sql.PreparedStatement stmt = conn
                        .prepareStatement("SELECT COUNT(*) as cnt FROM job_offers WHERE company_id = ?")) {
                    stmt.setInt(1, companyId);
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            logger.info("DIAGNOSTIC: Job offers for this company: {}", rs.getInt("cnt"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("DIAGNOSTIC error", e);
        }
    }

    private void setupTable() {
        // ── Candidat column – wrap + tooltip ────────────────────────────────
        candidateCol.setCellValueFactory(new PropertyValueFactory<>("candidateName"));
        candidateCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                if (empty || value == null) { setText(null); return; }
                setText(value);
                setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                if (value.length() > 18) {
                    Tooltip.install(this, new Tooltip(value));
                }
            }
        });

        // ── Offre column – wrap text, full title visible via tooltip ─────────
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

        // ── Date column – show full date, no truncation ──────────────────────
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
        // TableColumn is not a Node, so set tooltip on header via a Label
        Label scoreHeader = new Label("Score profil");
        scoreHeader.setTooltip(new Tooltip("Qualité globale du profil du candidat (completude, compétences, expérience). Identique pour toutes les offres du même candidat."));
        scoreCol.setGraphic(scoreHeader);
        scoreCol.setText(null);

        // Match-score column with colored badge (no text truncation)
        matchCol.setCellValueFactory(new PropertyValueFactory<>("matchScore"));
        matchCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(null);
                setText(null);
                if (empty || value == null || "-".equals(value)) return;

                int score = 0;
                try { score = Integer.parseInt(value.replace("%", "").trim()); }
                catch (NumberFormatException ignored) {}

                String colour;
                if      (score >= 85) colour = "#16a34a";
                else if (score >= 70) colour = "#2563eb";
                else if (score >= 50) colour = "#d97706";
                else if (score >= 30) colour = "#ea580c";
                else                  colour = "#dc2626";

                Label badge = new Label(value);
                // Prevent any internal text truncation inside the badge
                badge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                badge.setMaxWidth(Double.MAX_VALUE);
                badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                badge.setStyle(
                    "-fx-background-color:" + colour + "; -fx-text-fill:white; " +
                    "-fx-padding:3 10; -fx-background-radius:6; " +
                    "-fx-font-weight:bold; -fx-font-size:12;");

                HBox wrapper = new HBox(badge);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setAlignment(Pos.CENTER);
            }
        });

        // Status column – colored label (no text truncation)
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
                    case "INTERVIEW":
                        label = "Entretien";  bg = "#1e3a5f"; fg = "#bae6fd"; break;
                    case "REVIEW": case "REVIEWING":
                        label = "En révision"; bg = "#312e81"; fg = "#c7d2fe"; break;
                    case "OFFER":
                        label = "Offre faite"; bg = "#064e3b"; fg = "#a7f3d0"; break;
                    case "ACCEPTED":
                        label = "Accepté";    bg = "#14532d"; fg = "#bbf7d0"; break;
                    case "REJECTED":
                        label = "Refusé";     bg = "#7f1d1d"; fg = "#fca5a5"; break;
                    case "PENDING": default:
                        label = "En attente"; bg = "#27272a"; fg = "#a1a1aa"; break;
                }
                Label badge = new Label(label);
                badge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                badge.setStyle(
                    "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; " +
                    "-fx-padding:3 10; -fx-background-radius:6; " +
                    "-fx-font-weight:bold; -fx-font-size:11;");
                HBox wrapper = new HBox(badge);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setAlignment(Pos.CENTER);
            }
        });

        // Actions column
        actionsCol.setCellFactory(col -> new TableCell<ApplicationRow, Void>() {
            private final TLButton moreBtn = new TLButton("⋮");

            {
                moreBtn.setVariant(TLButton.ButtonVariant.GHOST);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
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
        // Prevent multiple simultaneous loads
        if (isLoading) {
            logger.debug("Load already in progress, skipping duplicate call");
            return;
        }

        isLoading = true;

        // Clear existing data first to prevent duplicates
        if (applications != null) {
            applications.clear();
        }
        if (applicationsTable != null && applicationsTable.getItems() != null) {
            applicationsTable.getItems().clear();
        }

        if (statsLabel != null) {
            statsLabel.setText(I18n.get("common.loading"));
        }

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                List<Application> apps = applicationService.getApplicationsByCompanyOwner(currentUser.getId());
                for (Application app : apps) {
                    if (app.getMatchPercentage() <= 0) {
                        int match = intelligenceService
                                .calculateCompatibility(app.getCandidateProfileId(), app.getJobOfferId()).join();
                        app.setMatchPercentage(match);
                    }
                    if (app.getCandidateScore() <= 0) {
                        int score = intelligenceService.scoreCandidate(app.getCandidateProfileId()).join();
                        app.setCandidateScore(score);
                    }
                }
                apps.sort((a, b) -> Integer.compare(
                        (b.getMatchPercentage() + b.getCandidateScore()),
                        (a.getMatchPercentage() + a.getCandidateScore())));
                return apps;
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;
            List<Application> dbApps = task.getValue();

            if (applications != null)
                applications.clear();
            applications = FXCollections.observableArrayList();

            if (dbApps == null || dbApps.isEmpty()) {
                logger.warn("No applications found for employer ID: {}. Checking DB...", currentUser.getId());
                debugDatabaseState();
                if (statsLabel != null) {
                    statsLabel.setText("Aucune candidature trouvée. Utilisez 'Répare & Demo' si besoin.");
                }
            } else {
                for (Application app : dbApps) {
                    String displayStatus = mapStatusToI18nKey(app.getStatus());
                    applications.add(new ApplicationRow(
                            app.getId(),
                            app.getCandidateName() != null ? app.getCandidateName()
                                    : I18n.get("inbox.candidate_num", app.getCandidateProfileId()),
                            app.getJobTitle() != null ? app.getJobTitle()
                                    : I18n.get("inbox.offer_num", app.getJobOfferId()),
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
            if (statsLabel != null) {
                statsLabel.setText(I18n.get("applications.error"));
            }
            logger.error("Failed to load applications for employer user ID: {}",
                    currentUser != null ? currentUser.getId() : "null", task.getException());
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        new Thread(task, "AppLoadThread") {
            {
                setDaemon(true);
            }
        }.start();
    }

    private String mapStatusToI18nKey(Status status) {
        return switch (status) {
            case PENDING -> "PENDING";
            case REVIEWING -> "REVIEW";
            case INTERVIEW -> "INTERVIEW";
            case OFFER -> "OFFER";
            case ACCEPTED -> "ACCEPTED";
            case REJECTED -> "REJECTED";
        };
    }

    private void updateStats() {
        long pending = applications.stream()
                .filter(a -> a.getStatus().equals("PENDING") || a.getStatus().equals("REVIEW")).count();
        statsLabel.setText(I18n.get("inbox.pending", pending));
    }

    private void updatePagination() {
        int total = applications.size();
        if (total == 0) {
            paginationLabel.setText("0 sur 0");
            pageLabel.setText("Page 1");
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            return;
        }
        int start = currentPage * itemsPerPage + 1;
        int end = Math.min((currentPage + 1) * itemsPerPage, total);

        paginationLabel.setText(start + "-" + end + " " + I18n.get("inbox.of") + " " + total);
        pageLabel.setText("Page " + (currentPage + 1));

        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(end >= total);
    }

    private void handleViewProfile(ApplicationRow app) {
        try {
            // Load full application details with all necessary data
            Application fullApp = null;
            try {
                fullApp = applicationService.getApplicationById(app.getApplicationId());
            } catch (Exception ex) {
                logger.error("Error loading application by ID: {}", app.getApplicationId(), ex);
            }

            // If getApplicationById fails, try to get it from the list we already have
            if (fullApp == null) {
                // Try to find it in the current list
                List<Application> allApps = applicationService.getApplicationsByCompanyOwner(currentUser.getId());
                fullApp = allApps.stream()
                        .filter(a -> a.getId() == app.getApplicationId())
                        .findFirst()
                        .orElse(null);
            }

            if (fullApp == null) {
                logger.error("Application not found: {}", app.getApplicationId());
                com.skilora.utils.DialogUtils.showError("Erreur", "Candidature introuvable.");
                return;
            }

            // Create dialog stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Détails de la candidature");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            if (applicationsTable.getScene() != null && applicationsTable.getScene().getWindow() != null) {
                dialogStage.initOwner(applicationsTable.getScene().getWindow());
            }

            // Load FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/skilora/view/recruitment/ApplicationDetailsView.fxml"));
            javafx.scene.layout.VBox root = loader.load();

            ApplicationDetailsController controller = loader.getController();
            if (controller != null) {
                controller.setup(fullApp, dialogStage, newStatus -> {
                    // Refresh the matching row in the table when status changes
                    applications.stream()
                            .filter(r -> r.getApplicationId() == app.getApplicationId())
                            .findFirst()
                            .ifPresent(r -> {
                                r.setStatus(mapStatusToI18nKey(newStatus));
                                javafx.application.Platform.runLater(() -> applicationsTable.refresh());
                            });
                });
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(root, 960, 720);
            // Apply application stylesheets
            scene.getStylesheets().addAll(applicationsTable.getScene().getStylesheets());
            dialogStage.setMinWidth(860);
            dialogStage.setMinHeight(600);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            // Full refresh after the dialog closes
            loadApplications();
        } catch (Exception e) {
            logger.error("Failed to show application details", e);
            e.printStackTrace();
            com.skilora.utils.DialogUtils.showError("Erreur",
                    "Impossible d'afficher les détails de la candidature: " + e.getMessage());
        }
    }

    private void handleAccept(ApplicationRow app) {
        Thread t = new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.ACCEPTED);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("ACCEPTED");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) {
                logger.error("Failed to accept application", ex);
            }
        }, "AcceptAppThread");
        t.setDaemon(true);
        t.start();
    }

    private void handleReview(ApplicationRow app) {
        Thread t = new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.REVIEWING);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("REVIEW");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) {
                logger.error("Failed to set application to reviewing", ex);
            }
        }, "ReviewAppThread");
        t.setDaemon(true);
        t.start();
    }

    private void handleReject(ApplicationRow app) {
        Thread t = new Thread(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.REJECTED);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("REJECTED");
                    applicationsTable.refresh();
                    updateStats();
                });
            } catch (Exception ex) {
                logger.error("Failed to reject application", ex);
            }
        }, "RejectAppThread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null)
            searchField.clear();
        if (allStatusBtn != null) {
            allStatusBtn.setSelected(true);
        }
        applyFilters();
    }

    @FXML
    private void handleRefresh() {
        if (currentUser != null)
            loadApplications();
    }

    @FXML
    private void handleFixAndSeed() {
        if (currentUser == null)
            return;
        if (statsLabel != null)
            statsLabel.setText("Réparation et génération de données en cours...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                com.skilora.utils.DataFixer.fixApplicationsForEmployer(currentUser.getId());
                com.skilora.utils.TestDataGenerator.generateTestData();
                return null;
            }
        };

        task.setOnSucceeded(e -> loadApplications());
        task.setOnFailed(e -> {
            if (statsLabel != null)
                statsLabel.setText("❌ Échec de la réparation");
            logger.error("Fix and Seed failed", task.getException());
        });

        Thread thread = new Thread(task, "FixSeedThread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void applyFilters() {
        if (allApplications == null) {
            return;
        }

        String searchText = searchField.getText().toLowerCase().trim();

        // Get status from ToggleGroup
        String selectedStatus = null;
        if (statusGroup != null && statusGroup.getSelectedToggle() != null) {
            Object userData = statusGroup.getSelectedToggle().getUserData();
            if (userData != null) {
                selectedStatus = userData.toString();
            }
        }

        // Filter applications
        final String finalSelectedStatus = selectedStatus;
        List<ApplicationRow> filtered = allApplications.stream()
                .filter(app -> {
                    // Filter by Search Text (Candidate Name)
                    if (!searchText.isEmpty() && !app.getCandidateName().toLowerCase().contains(searchText)) {
                        return false;
                    }

                    // Filter by status
                    if (finalSelectedStatus != null && !finalSelectedStatus.isEmpty()) {
                        String appStatus = app.getStatus();
                        // 'En cours' (REVIEW) filter matches multiples active statuses
                        if (finalSelectedStatus.equals("REVIEW")) {
                            return appStatus.equals("REVIEW") ||
                                    appStatus.equals("REVIEWING") ||
                                    appStatus.equals("INTERVIEW") ||
                                    appStatus.equals("OFFER");
                        } else if (!appStatus.equals(finalSelectedStatus)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Update displayed list
        applications = FXCollections.observableArrayList(filtered);
        applicationsTable.setItems(applications);

        // Reset to first page when filtering
        currentPage = 0;
        updateStats();
        updatePagination();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void handleNextPage() {
        int maxPage = (applications.size() - 1) / itemsPerPage;
        if (currentPage < maxPage) {
            currentPage++;
            updatePagination();
        }
    }

    // Inner class for table data
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

        public int getApplicationId() {
            return applicationId;
        }

        public String getCandidateName() {
            return candidateName;
        }

        public void setCandidateName(String candidateName) {
            this.candidateName = candidateName;
        }

        public String getJobTitle() {
            return jobTitle;
        }

        public void setJobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
        }

        public String getApplicationDate() {
            return applicationDate;
        }

        public void setApplicationDate(String applicationDate) {
            this.applicationDate = applicationDate;
        }

        public String getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(String matchScore) {
            this.matchScore = matchScore;
        }

        public String getCandidateScore() {
            return candidateScore;
        }

        public void setCandidateScore(String candidateScore) {
            this.candidateScore = candidateScore;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
