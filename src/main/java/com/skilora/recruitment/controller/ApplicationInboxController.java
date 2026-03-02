package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTable;
import com.skilora.framework.components.TLToast;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.entity.MatchingScore;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.MatchingService;

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
import java.util.stream.Collectors;

import javafx.scene.control.ButtonType;

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

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
    @FXML private TableColumn<ApplicationRow, String> matchCol;
    @FXML private TableColumn<ApplicationRow, String> scoreCol;
    @FXML private TableColumn<ApplicationRow, String> statusCol;
    @FXML private TableColumn<ApplicationRow, Void> actionsCol;
    
    @FXML private Label paginationLabel;
    @FXML private Label pageLabel;
    @FXML private TLButton prevBtn;
    @FXML private TLButton nextBtn;
    
    private User currentUser;
    private ObservableList<ApplicationRow> allApplications;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final JobService jobService = JobService.getInstance();
    private final MatchingService matchingService = MatchingService.getInstance();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        setupTable();
        filterBtn.setGraphic(SvgIcons.icon(SvgIcons.SEARCH, 14));
        refreshBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14));
        prevBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        nextBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_RIGHT, 14));

        statusSelect.getItems().setAll(
            I18n.get("inbox.filter.all_statuses"),
            "PENDING", "REVIEW", "INTERVIEW", "OFFER", "ACCEPTED", "REJECTED"
        );
        statusSelect.setValue(I18n.get("inbox.filter.all_statuses"));

        jobSelect.valueProperty().addListener((obs, o, n) -> applyFiltersAndPaginate());
        statusSelect.valueProperty().addListener((obs, o, n) -> applyFiltersAndPaginate());
    }

    private void applyI18n() {
        filterBtn.setText(I18n.get("inbox.filter"));
        refreshBtn.setText(I18n.get("common.refresh"));
        candidateCol.setText(I18n.get("inbox.col.candidate"));
        jobCol.setText(I18n.get("inbox.col.position"));
        dateCol.setText(I18n.get("inbox.col.date"));
        matchCol.setText(I18n.get("inbox.col.match"));
        scoreCol.setText(I18n.get("inbox.col.score_profil"));
        statusCol.setText(I18n.get("inbox.col.status"));
        actionsCol.setText(I18n.get("inbox.col.actions"));
        jobSelect.setPromptText(I18n.get("inbox.filter.all_jobs"));
        statusSelect.setPromptText(I18n.get("inbox.filter.all_statuses"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadApplications();
    }
    
    private void setupTable() {
        candidateCol.setCellValueFactory(new PropertyValueFactory<>("candidateName"));
        candidateCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String candidateName, boolean empty) {
                super.updateItem(candidateName, empty);
                if (empty || candidateName == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label nameLabel = new Label(candidateName);
                nameLabel.getStyleClass().add("text-sm");
                setGraphic(nameLabel);
                setText(null);
            }
        });
        jobCol.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));

        // Match % column with coloured badges
        matchCol.setCellValueFactory(new PropertyValueFactory<>("matchPercent"));
        matchCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String pct, boolean empty) {
                super.updateItem(pct, empty);
                if (empty || pct == null || pct.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                TLBadge.Variant variant = TLBadge.Variant.SECONDARY;
                try {
                    double val = Double.parseDouble(pct.replace("%", ""));
                    if (val >= 80) variant = TLBadge.Variant.SUCCESS;
                    else if (val >= 60) variant = TLBadge.Variant.SECONDARY;
                    else variant = TLBadge.Variant.OUTLINE;
                } catch (NumberFormatException ignored) {}
                TLBadge badge = new TLBadge(pct, variant);
                setGraphic(badge);
                setAlignment(Pos.CENTER);
                setText(null);
            }
        });

        // Score profil column
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("candidateScore"));
        scoreCol.setCellFactory(col -> new TableCell<ApplicationRow, String>() {
            @Override
            protected void updateItem(String score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null || score.isBlank() || "0".equals(score)) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label lbl = new Label(score);
                lbl.getStyleClass().add("text-sm");
                setGraphic(lbl);
                setAlignment(Pos.CENTER);
                setText(null);
            }
        });

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
                        if ("PENDING".equals(app.getStatus())) {
                            menu.addItem(I18n.get("inbox.move_to_review"), ev -> handleMoveToReview(app));
                        }
                        if ("PENDING".equals(app.getStatus()) || "REVIEW".equals(app.getStatus())) {
                            menu.addItem(I18n.get("inbox.accept_for_interview"), ev -> handleAcceptForInterview(app));
                        }
                        menu.addItem(I18n.get("inbox.reject"), ev -> handleReject(app));
                        menu.addItem(I18n.get("inbox.contact"), ev -> handleContact(app));
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
        applicationsTable.setPlaceholder(new TLLoadingState());

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                return applicationService.getApplicationsByCompanyOwner(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Application> dbApps = task.getValue();
            allApplications = FXCollections.observableArrayList();

            for (Application app : dbApps) {
                String displayStatus = mapStatusToI18nKey(app.getStatus());
                allApplications.add(new ApplicationRow(
                        app.getId(),
                        app.getCandidateProfileId(),
                        app.getJobOfferId(),
                        app.getCandidateName() != null ? app.getCandidateName() : I18n.get("inbox.candidate_num", app.getCandidateProfileId()),
                        app.getJobTitle() != null ? app.getJobTitle() : I18n.get("inbox.offer_num", app.getJobOfferId()),
                        app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "",
                        displayStatus,
                        app.getCandidateScore() > 0 ? String.valueOf(app.getCandidateScore()) : ""
                ));
            }

            currentPage = 0;
            populateJobFilter();
            updateStats();
            applyPagination();
            computeMatchScoresAsync();
        });

        task.setOnFailed(e -> {
            statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
        });

        AppThreadPool.execute(task);
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
        long pending = getFilteredApplications().stream().filter(a ->
                a.getStatus().equals("PENDING") || a.getStatus().equals("REVIEW")).count();
        statsLabel.setText(I18n.get("inbox.pending", pending));
    }

    /** Populate the job dropdown with unique job titles from loaded data. */
    private void populateJobFilter() {
        String allLabel = I18n.get("inbox.filter.all_jobs");
        List<String> jobTitles = allApplications.stream()
            .map(ApplicationRow::getJobTitle)
            .filter(t -> t != null && !t.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        jobTitles.add(0, allLabel);
        jobSelect.getItems().setAll(jobTitles);
        jobSelect.setValue(allLabel);
    }

    /** Returns applications filtered by current jobSelect and statusSelect values. */
    private List<ApplicationRow> getFilteredApplications() {
        if (allApplications == null) return List.of();
        String selectedJob = jobSelect.getValue();
        String selectedStatus = statusSelect.getValue();
        boolean filterJob = selectedJob != null && !selectedJob.equals(I18n.get("inbox.filter.all_jobs"));
        boolean filterStatus = selectedStatus != null && !selectedStatus.equals(I18n.get("inbox.filter.all_statuses"));

        return allApplications.stream()
            .filter(a -> !filterJob || selectedJob.equals(a.getJobTitle()))
            .filter(a -> !filterStatus || selectedStatus.equals(a.getStatus()))
            .toList();
    }

    /** Re-apply filters and reset to page 0. */
    private void applyFiltersAndPaginate() {
        currentPage = 0;
        updateStats();
        applyPagination();
    }

    private void applyPagination() {
        List<ApplicationRow> filtered = getFilteredApplications();
        int total = filtered.size();
        if (total == 0) {
            applicationsTable.setItems(FXCollections.observableArrayList());
            paginationLabel.setText("0 " + I18n.get("inbox.of") + " 0");
            pageLabel.setText(I18n.get("inbox.page", 1));
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
            return;
        }
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, total);

        ObservableList<ApplicationRow> pageItems = FXCollections.observableArrayList(
                filtered.subList(start, end));
        applicationsTable.setItems(pageItems);

        paginationLabel.setText((start + 1) + "-" + end + " " + I18n.get("inbox.of") + " " + total);
        pageLabel.setText(I18n.get("inbox.page", currentPage + 1));

        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(end >= total);
    }

    private void computeMatchScoresAsync() {
        if (allApplications == null || allApplications.isEmpty()) return;
        AppThreadPool.execute(() -> {
            boolean changed = false;
            for (ApplicationRow r : allApplications) {
                if (r.getMatchPercent() != null) continue;
                try {
                    JobOffer offer = jobService.findJobOfferById(r.getJobOfferId());
                    if (offer == null) continue;
                    MatchingScore score = matchingService.calculateMatch(r.getCandidateProfileId(), offer);
                    String pct = String.format("%.0f%%", score.getTotalScore());
                    r.setMatchPercent(pct);
                    changed = true;
                } catch (Exception ex) {
                    // If match calculation fails, leave it empty (no badge)
                }
            }
            if (changed) {
                javafx.application.Platform.runLater(() -> applicationsTable.refresh());
            }
        });
    }
    
    private void handleViewProfile(ApplicationRow app) {
        DialogUtils.showInfo(I18n.get("inbox.view_profile"),
            I18n.get("inbox.candidate") + ": " + app.getCandidateName()
            + "\n" + I18n.get("inbox.job") + ": " + app.getJobTitle()
            + "\n" + I18n.get("inbox.date") + ": " + app.getApplicationDate()
            + "\n" + I18n.get("inbox.status") + ": " + app.getStatus());
    }
    
    private void handleMoveToReview(ApplicationRow app) {
        AppThreadPool.execute(() -> {
            try {
                applicationService.updateStatus(app.getApplicationId(), Status.REVIEWING);
                javafx.application.Platform.runLater(() -> {
                    app.setStatus("REVIEW");
                    applicationsTable.refresh();
                    updateStats();
                    TLToast.success(applicationsTable.getScene(),
                        I18n.get("inbox.moved_to_review"),
                        app.getCandidateName());
                });
            } catch (Exception ex) {
                logger.error("Failed to move to review", ex);
            }
        });
    }

    private void handleAcceptForInterview(ApplicationRow app) {
        DialogUtils.showConfirmation(
            I18n.get("inbox.accept_interview.title"),
            I18n.get("inbox.accept_interview.message", app.getCandidateName())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                AppThreadPool.execute(() -> {
                    try {
                        applicationService.updateStatus(app.getApplicationId(), Status.INTERVIEW);
                        javafx.application.Platform.runLater(() -> {
                            app.setStatus("INTERVIEW");
                            applicationsTable.refresh();
                            updateStats();
                            TLToast.success(applicationsTable.getScene(),
                                I18n.get("inbox.accepted_for_interview"),
                                app.getCandidateName());
                        });
                    } catch (Exception ex) {
                        logger.error("Failed to accept for interview", ex);
                    }
                });
            }
        });
    }
    
    private void handleReject(ApplicationRow app) {
        DialogUtils.showConfirmation(
            I18n.get("inbox.reject.confirm.title"),
            I18n.get("inbox.reject.confirm.message", app.getCandidateName())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                AppThreadPool.execute(() -> {
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
                });
            }
        });
    }
    
    private void handleContact(ApplicationRow app) {
        DialogUtils.showInfo(I18n.get("inbox.contact"),
            I18n.get("inbox.contact.info", app.getCandidateName()));
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
            applyPagination();
        }
    }
    
    @FXML
    private void handleNextPage() {
        int maxPage = (getFilteredApplications().size() - 1) / itemsPerPage;
        if (currentPage < maxPage) {
            currentPage++;
            applyPagination();
        }
    }
    
    // Inner class for table data
    public static class ApplicationRow {
        private int applicationId;
        private int candidateProfileId;
        private int jobOfferId;
        private String candidateName;
        private String jobTitle;
        private String applicationDate;
        private String status;
        private String matchPercent;
        private String candidateScore;
        
        public ApplicationRow(int applicationId, int candidateProfileId, int jobOfferId, String candidateName, String jobTitle, String applicationDate, String status, String candidateScore) {
            this.applicationId = applicationId;
            this.candidateProfileId = candidateProfileId;
            this.jobOfferId = jobOfferId;
            this.candidateName = candidateName;
            this.jobTitle = jobTitle;
            this.applicationDate = applicationDate;
            this.status = status;
            this.candidateScore = candidateScore;
        }

        public int getApplicationId() { return applicationId; }
        public int getCandidateProfileId() { return candidateProfileId; }
        public int getJobOfferId() { return jobOfferId; }
        
        public String getCandidateName() { return candidateName; }
        public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
        
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        
        public String getApplicationDate() { return applicationDate; }
        public void setApplicationDate(String applicationDate) { this.applicationDate = applicationDate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMatchPercent() { return matchPercent; }
        public void setMatchPercent(String matchPercent) { this.matchPercent = matchPercent; }

        public String getCandidateScore() { return candidateScore; }
        public void setCandidateScore(String candidateScore) { this.candidateScore = candidateScore; }
    }
}
