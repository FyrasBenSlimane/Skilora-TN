package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.Application;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLSeparator;

import javafx.concurrent.Task;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.scene.control.ButtonType;

import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

/**
 * InterviewsController - Employer interview management.
 * Shows applications in INTERVIEW status for the current employer.
 */
public class InterviewsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InterviewsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private VBox interviewsContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private final ApplicationService applicationService = ApplicationService.getInstance();
    private User currentUser;
    private List<Application> allInterviews;
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        String[][] filters = {
            {I18n.get("interviews.filter.all"), "ALL"},
            {I18n.get("interviews.filter.interview"), "INTERVIEW"},
            {I18n.get("interviews.filter.offer_sent"), "OFFER"},
            {I18n.get("interviews.filter.accepted"), "ACCEPTED"},
            {I18n.get("interviews.filter.rejected"), "REJECTED"}
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

    private void loadData() {
        if (currentUser == null) return;

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                // Get all applications for this employer's companies
                List<Application> all = applicationService.getApplicationsByCompanyOwner(currentUser.getId());
                // Filter to interview-stage and beyond
                return all.stream()
                    .filter(a -> a.getStatus() == Application.Status.INTERVIEW ||
                                 a.getStatus() == Application.Status.OFFER ||
                                 a.getStatus() == Application.Status.ACCEPTED ||
                                 a.getStatus() == Application.Status.REJECTED)
                    .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> {
            allInterviews = task.getValue();
            applyFilters();
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load interviews", task.getException());
            statsLabel.setText(I18n.get("interviews.error"));
        });

        Thread thread = new Thread(task, "InterviewsLoader");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyFilters() {
        if (allInterviews == null) return;

        List<Application> filtered;
        if ("ALL".equals(currentFilter)) {
            filtered = allInterviews;
        } else {
            try {
                Application.Status targetStatus = Application.Status.valueOf(currentFilter);
                filtered = allInterviews.stream()
                    .filter(a -> a.getStatus() == targetStatus)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException ex) {
                filtered = allInterviews;
            }
        }

        renderInterviews(filtered);
    }

    private void renderInterviews(List<Application> interviews) {
        interviewsContainer.getChildren().clear();

        if (interviews.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("interviews.count.zero"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        long interviewCount = interviews.stream()
            .filter(a -> a.getStatus() == Application.Status.INTERVIEW).count();
        statsLabel.setText(I18n.get("interviews.count", interviews.size(), interviewCount));

        for (Application app : interviews) {
            interviewsContainer.getChildren().add(createInterviewCard(app));
        }
    }

    private TLCard createInterviewCard(Application app) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Top row: Candidate name + Status badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(app.getCandidateName() != null ? app.getCandidateName() : I18n.get("interviews.candidate_num", app.getCandidateProfileId()));
        nameLabel.getStyleClass().add("h4");

        TLBadge statusBadge = new TLBadge(app.getStatus().getDisplayName(), TLBadge.Variant.DEFAULT);
        switch (app.getStatus()) {
            case INTERVIEW:
                statusBadge.getStyleClass().add("badge-warning");
                break;
            case OFFER:
                statusBadge.getStyleClass().add("badge-success");
                break;
            case ACCEPTED:
                statusBadge.getStyleClass().add("badge-success");
                break;
            case REJECTED:
                statusBadge.getStyleClass().add("badge-destructive");
                break;
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(nameLabel, spacer, statusBadge);

        // Job details
        HBox detailsRow = new HBox(16);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        if (app.getJobTitle() != null) {
            Label jobLabel = new Label("💼 " + app.getJobTitle());
            jobLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(jobLabel);
        }

        if (app.getJobLocation() != null) {
            Label locLabel = new Label("📍 " + app.getJobLocation());
            locLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(locLabel);
        }

        if (app.getAppliedDate() != null) {
            Label dateLabel = new Label("📅 " + I18n.get("interviews.application_date") + ": " + app.getAppliedDate().format(DATE_FMT));
            dateLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(dateLabel);
        }

        // Actions
        TLSeparator sep = new TLSeparator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (app.getStatus() == Application.Status.INTERVIEW) {
            TLButton offerBtn = new TLButton(I18n.get("interviews.send_offer"), TLButton.ButtonVariant.PRIMARY);
            offerBtn.setOnAction(e -> updateStatus(app, Application.Status.OFFER));

            TLButton rejectBtn = new TLButton(I18n.get("interviews.reject"), TLButton.ButtonVariant.GHOST);
            rejectBtn.getStyleClass().add("text-destructive");
            rejectBtn.setOnAction(e -> {
                DialogUtils.showConfirmation(
                    I18n.get("interviews.reject.confirm.title"),
                    I18n.get("interviews.reject.confirm.message",
                        app.getCandidateName() != null ? app.getCandidateName() : "")
                ).ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        updateStatus(app, Application.Status.REJECTED);
                    }
                });
            });

            actionsRow.getChildren().addAll(offerBtn, rejectBtn);
        } else if (app.getStatus() == Application.Status.OFFER) {
            Label waitingLabel = new Label(I18n.get("interviews.waiting"));
            waitingLabel.getStyleClass().add("text-muted");
            actionsRow.getChildren().add(waitingLabel);
        }

        content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    private void updateStatus(Application app, Application.Status newStatus) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return applicationService.updateStatus(app.getId(), newStatus);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                app.setStatus(newStatus);
                applyFilters();
            }
        });

        task.setOnFailed(e -> logger.error("Failed to update status", task.getException()));

        Thread thread = new Thread(task, "UpdateStatus");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
