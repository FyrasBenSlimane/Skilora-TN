package com.skilora.recruitment.controller;

import com.skilora.framework.components.*;
import com.skilora.model.entity.Application;
import com.skilora.model.entity.User;
import com.skilora.model.service.ApplicationService;
import com.skilora.model.service.ProfileService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.skilora.utils.I18n;

/**
 * ApplicationsController - Kanban board for tracking job applications (Job Seeker view)
 */
public class ApplicationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsController.class);

    @FXML private Label statsLabel;
    @FXML private TLButton refreshBtn;
    
    @FXML private Label appliedCount;
    @FXML private Label reviewingCount;
    @FXML private Label interviewingCount;
    @FXML private Label offerCount;
    
    @FXML private VBox appliedColumn;
    @FXML private VBox reviewingColumn;
    @FXML private VBox interviewingColumn;
    @FXML private VBox offerColumn;

    private User currentUser;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data loads after setCurrentUser is called
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadApplications();
    }
    
    private void loadApplications() {
        appliedColumn.getChildren().clear();
        reviewingColumn.getChildren().clear();
        interviewingColumn.getChildren().clear();
        offerColumn.getChildren().clear();
        statsLabel.setText(I18n.get("common.loading"));

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                // Get profile ID for this user
                var profile = ProfileService.getInstance()
                        .findProfileByUserId(currentUser.getId());
                if (profile == null) return List.of();
                return applicationService.getApplicationsByProfile(profile.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Application> apps = task.getValue();
            for (Application app : apps) {
                TLCard card = createApplicationCard(
                        app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()),
                        app.getCompanyName() != null ? app.getCompanyName() : "",
                        app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "");

                switch (app.getStatus()) {
                    case PENDING -> appliedColumn.getChildren().add(card);
                    case REVIEWING -> reviewingColumn.getChildren().add(card);
                    case INTERVIEW -> interviewingColumn.getChildren().add(card);
                    case OFFER, ACCEPTED -> offerColumn.getChildren().add(card);
                    case REJECTED -> {} // Could add a rejected column if desired
                }
            }

            updateCounts();
        });

        task.setOnFailed(e -> {
            statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
        });

        new Thread(task).start();
    }

    private void updateCounts() {
        appliedCount.setText(String.valueOf(appliedColumn.getChildren().size()));
        reviewingCount.setText(String.valueOf(reviewingColumn.getChildren().size()));
        interviewingCount.setText(String.valueOf(interviewingColumn.getChildren().size()));
        offerCount.setText(String.valueOf(offerColumn.getChildren().size()));

        int total = appliedColumn.getChildren().size() + reviewingColumn.getChildren().size()
                + interviewingColumn.getChildren().size() + offerColumn.getChildren().size();
        statsLabel.setText(I18n.get("applications.count", total));
    }
    
    private TLCard createApplicationCard(String title, String company, String date) {
        TLCard card = new TLCard();
        
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);
        
        Label companyLabel = new Label(company);
        companyLabel.getStyleClass().add("text-muted");
        
        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-font-size: 11px;");
        dateLabel.getStyleClass().add("text-muted");
        
        content.getChildren().addAll(titleLabel, companyLabel, dateLabel);
        card.getContent().add(content);
        
        return card;
    }
    
    @FXML
    private void handleRefresh() {
        if (currentUser != null) {
            loadApplications();
        }
    }
}

