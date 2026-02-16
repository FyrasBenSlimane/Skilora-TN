package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTable;
import com.skilora.model.entity.Application;
import com.skilora.model.entity.Application.Status;
import com.skilora.model.entity.User;
import com.skilora.model.service.ApplicationService;

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
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        // Data loads after setCurrentUser is called
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadApplications();
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
                            badge = new TLBadge(I18n.get("inbox.status.review"), TLBadge.Variant.SECONDARY);
                            break;
                        case "INTERVIEW":
                            badge = new TLBadge(I18n.get("inbox.status.interview"), TLBadge.Variant.OUTLINE);
                            break;
                        case "OFFER":
                        case "ACCEPTED":
                            badge = new TLBadge(status.equals("OFFER") ? I18n.get("inbox.status.offer") : I18n.get("inbox.status.accepted"), TLBadge.Variant.SUCCESS);
                            break;
                        case "REJECTED":
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
            private final TLButton moreBtn = new TLButton("â‹®");
            
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
                        menu.addItem("ðŸ‘ï¸ " + I18n.get("inbox.view_profile"), ev -> handleViewProfile(app));
                        menu.addItem("âœ… " + I18n.get("inbox.accept"), ev -> handleAccept(app));
                        menu.addItem("âŒ " + I18n.get("inbox.reject"), ev -> handleReject(app));
                        menu.addItem("ðŸ“§ " + I18n.get("inbox.contact"), ev -> handleContact(app));
                        menu.show(moreBtn, javafx.geometry.Side.BOTTOM, 0, 4);
                    });
                    
                    setGraphic(moreBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }
    
    private void loadApplications() {
        statsLabel.setText(I18n.get("common.loading"));

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                return applicationService.getApplicationsByCompanyOwner(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Application> dbApps = task.getValue();
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
        });

        task.setOnFailed(e -> {
            statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
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
        logger.debug("Viewing profile: {}", app.getCandidateName());
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
    
    private void handleContact(ApplicationRow app) {
        logger.debug("Contacting: {}", app.getCandidateName());
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

