package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTable;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Application.Status;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;

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
import javafx.scene.control.cell.PropertyValueFactory;
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

    @FXML private Label statsLabel;
    @FXML private TLButton filterBtn;
    @FXML private TLButton refreshBtn;
    @FXML private HBox filterBar;
    
    @FXML private TLSelect<String> jobSelect;
    @FXML private TLSelect<String> statusSelect;
    
    @FXML private TLTable<ApplicationRow> applicationsTable;
    @FXML private TableColumn<ApplicationRow, String> candidateCol;
    @FXML private TableColumn<ApplicationRow, String> jobCol;
    @FXML private TableColumn<ApplicationRow, String> dateCol;
    @FXML private TableColumn<ApplicationRow, String> statusCol;
    @FXML private TableColumn<ApplicationRow, Void> actionsCol;
    
    @FXML private Label paginationLabel;
    @FXML private Label pageLabel;
    @FXML private TLButton prevBtn;
    @FXML private TLButton nextBtn;
    
    private User currentUser;
    private ObservableList<ApplicationRow> applications;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private boolean isLoading = false; // Prevent multiple simultaneous loads
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        // Data loads after setCurrentUser is called
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
        try {
            java.sql.Connection conn = com.skilora.config.DatabaseConfig.getInstance().getConnection();
            try (java.sql.Statement stmt = conn.createStatement()) {
                // 1. Check total applications
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM applications")) {
                    if (rs.next()) {
                        logger.info("DEBUG: Total applications in database: {}", rs.getInt("total"));
                    }
                }
                
                // 2. Check employer's company
                try (java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT c.id, c.owner_id, c.name FROM companies c WHERE c.owner_id = " + currentUser.getId())) {
                    if (rs.next()) {
                        int companyId = rs.getInt("id");
                        logger.info("DEBUG: Employer company ID: {}, Name: {}", companyId, rs.getString("name"));
                        
                        // 3. Check job offers for this company
                        try (java.sql.ResultSet rs2 = stmt.executeQuery(
                            "SELECT jo.id, jo.title, jo.status FROM job_offers jo WHERE jo.company_id = " + companyId)) {
                            int offerCount = 0;
                            while (rs2.next()) {
                                offerCount++;
                                logger.info("DEBUG: Job Offer ID: {}, Title: {}, Status: {}", 
                                    rs2.getInt("id"), rs2.getString("title"), rs2.getString("status"));
                            }
                            logger.info("DEBUG: Total job offers for company ID {}: {}", companyId, offerCount);
                            
                            // 4. Check applications for these job offers
                            if (offerCount > 0) {
                                try (java.sql.ResultSet rs3 = stmt.executeQuery(
                                    "SELECT a.id, a.job_offer_id, a.candidate_profile_id, a.status " +
                                    "FROM applications a " +
                                    "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                                    "WHERE jo.company_id = " + companyId)) {
                                    int appCount = 0;
                                    while (rs3.next()) {
                                        appCount++;
                                        logger.info("DEBUG: Application ID: {}, Job Offer ID: {}, Candidate Profile ID: {}, Status: {}", 
                                            rs3.getInt("id"), rs3.getInt("job_offer_id"), 
                                            rs3.getInt("candidate_profile_id"), rs3.getString("status"));
                                    }
                                    logger.info("DEBUG: Total applications for company ID {}: {}", companyId, appCount);
                                }
                            }
                        }
                    } else {
                        logger.warn("DEBUG: No company found for employer user ID: {}", currentUser.getId());
                    }
                }
                
                // 5. Check all applications with their job offers
                try (java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT a.id, a.job_offer_id, jo.company_id, c.owner_id " +
                    "FROM applications a " +
                    "LEFT JOIN job_offers jo ON a.job_offer_id = jo.id " +
                    "LEFT JOIN companies c ON jo.company_id = c.id")) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        logger.info("DEBUG: Application ID: {}, Job Offer ID: {}, Company ID: {}, Owner ID: {}", 
                            rs.getInt("id"), rs.getInt("job_offer_id"), 
                            rs.getObject("company_id"), rs.getObject("owner_id"));
                    }
                    if (count > 0) {
                        logger.info("DEBUG: Found {} applications total (some may not be linked to employer's company)", count);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in debugDatabaseState", e);
        }
    }
    
    private void setupTable() {
        candidateCol.setCellValueFactory(new PropertyValueFactory<>("candidateName"));
        jobCol.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        
        // Status column with badges
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    TLBadge badge;
                    switch (status) {
                        case "REVIEW":
                            // En cours = vert
                            badge = new TLBadge(I18n.get("inbox.status.review"), TLBadge.Variant.SUCCESS);
                            break;
                        case "INTERVIEW":
                            badge = new TLBadge(I18n.get("inbox.status.interview"), TLBadge.Variant.OUTLINE);
                            break;
                        case "OFFER":
                            badge = new TLBadge(I18n.get("inbox.status.offer"), TLBadge.Variant.OUTLINE);
                            break;
                        case "ACCEPTED":
                            // Accepté = bleu
                            badge = new TLBadge(I18n.get("inbox.status.accepted"), TLBadge.Variant.INFO);
                            break;
                        case "REJECTED":
                            // Refusé = rouge
                            badge = new TLBadge(I18n.get("inbox.status.rejected"), TLBadge.Variant.DESTRUCTIVE);
                            break;
                        default:
                            badge = new TLBadge(I18n.get("inbox.status.pending"), TLBadge.Variant.DEFAULT);
                            break;
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
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
                        menu.addItem("👁️ " + I18n.get("inbox.view_profile"), ev -> handleViewProfile(app));
                        menu.addItem("📋 " + I18n.get("inbox.review"), ev -> handleReview(app));
                        menu.addItem("✅ " + I18n.get("inbox.accept"), ev -> handleAccept(app));
                        menu.addItem("❌ " + I18n.get("inbox.reject"), ev -> handleReject(app));
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
                return applicationService.getApplicationsByCompanyOwner(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;
            List<Application> dbApps = task.getValue();
            logger.info("Loaded {} applications for employer user ID: {}", dbApps.size(), currentUser != null ? currentUser.getId() : "null");
            
            // Ensure we still have a clean list (clear again in case of race conditions)
            if (applications != null) {
                applications.clear();
            }
            applications = FXCollections.observableArrayList();

            for (Application app : dbApps) {
                String displayStatus = mapStatusToI18nKey(app.getStatus());
                applications.add(new ApplicationRow(
                        app.getId(),
                        app.getCandidateName() != null ? app.getCandidateName() : I18n.get("inbox.candidate_num", app.getCandidateProfileId()),
                        app.getJobTitle() != null ? app.getJobTitle() : I18n.get("inbox.offer_num", app.getJobOfferId()),
                        app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "",
                        displayStatus
                ));
            }

            applicationsTable.setItems(applications);
            updateStats();
            updatePagination();
            
            if (dbApps.isEmpty()) {
                logger.info("No applications found. Checking if employer has a company and job offers...");
                // Log debug info
                try {
                    int companyId = com.skilora.service.recruitment.JobService.getInstance()
                        .getOrCreateEmployerCompanyId(currentUser.getId(), "Entreprise");
                    logger.info("Employer company ID: {}", companyId);
                    
                    var offers = com.skilora.service.recruitment.JobService.getInstance()
                        .findJobOffersByCompanyOwner(currentUser.getId());
                    logger.info("Employer has {} job offers", offers.size());
                    
                    if (offers.isEmpty()) {
                        logger.warn("Employer has no job offers. They need to create job offers first.");
                    } else {
                        logger.info("Employer has job offers but no applications yet. Waiting for candidates to apply.");
                    }
                } catch (Exception ex) {
                    logger.error("Error checking employer data", ex);
                }
            } else {
                logger.info("Successfully loaded {} applications for employer", dbApps.size());
            }
        });

        task.setOnFailed(e -> {
            isLoading = false;
            if (statsLabel != null) {
                statsLabel.setText(I18n.get("applications.error"));
            }
            logger.error("Failed to load applications for employer user ID: {}", currentUser != null ? currentUser.getId() : "null", task.getException());
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        new Thread(task, "AppLoadThread") {{ setDaemon(true); }}.start();
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
        long pending = applications.stream().filter(a ->
                a.getStatus().equals("PENDING") || a.getStatus().equals("REVIEW")).count();
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
                controller.setup(fullApp, dialogStage);
            }
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 700, 600);
            // Apply application stylesheets
            scene.getStylesheets().addAll(applicationsTable.getScene().getStylesheets());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
            // Refresh applications after dialog closes (in case status was changed)
            loadApplications();
        } catch (Exception e) {
            logger.error("Failed to show application details", e);
            e.printStackTrace();
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible d'afficher les détails de la candidature: " + e.getMessage());
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
    private void handleFilter() {
        filterBar.setManaged(!filterBar.isManaged());
        filterBar.setVisible(!filterBar.isVisible());
    }
    
    @FXML
    private void handleResetFilters() {
        jobSelect.setValue(null);
        statusSelect.setValue(null);
        if (currentUser != null) loadApplications();
    }
    
    @FXML
    private void handleRefresh() {
        if (currentUser != null) loadApplications();
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
        private String status;
        
        public ApplicationRow(int applicationId, String candidateName, String jobTitle, String applicationDate, String status) {
            this.applicationId = applicationId;
            this.candidateName = candidateName;
            this.jobTitle = jobTitle;
            this.applicationDate = applicationDate;
            this.status = status;
        }

        public int getApplicationId() { return applicationId; }
        
        public String getCandidateName() { return candidateName; }
        public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
        
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        
        public String getApplicationDate() { return applicationDate; }
        public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
