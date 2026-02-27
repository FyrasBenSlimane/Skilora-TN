package com.skilora.controller.recruitment;

import com.skilora.framework.components.*;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;
import com.skilora.service.recruitment.InterviewService;
import com.skilora.service.usermanagement.ProfileService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
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
    
    @FXML private VBox applicationsList;

    private User currentUser;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final InterviewService interviewService = InterviewService.getInstance();
    private boolean isLoading = false; // Prevent multiple simultaneous loads
    private static final java.time.format.DateTimeFormatter DATE_TIME_FMT = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        loadApplications();
    }
    
    private void loadApplications() {
        // Prevent multiple simultaneous loads
        if (isLoading) {
            logger.debug("Load already in progress, skipping duplicate call");
            return;
        }
        
        isLoading = true;
        
        // Always clear existing data first to prevent duplicates
        if (applicationsList != null) {
            applicationsList.getChildren().clear();
        }
        
        if (currentUser == null) {
            isLoading = false;
            if (statsLabel != null) {
                statsLabel.setText(I18n.get("applications.error"));
            }
            return;
        }
        
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("common.loading"));
        }

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
            isLoading = false;
            List<Application> apps = task.getValue();
            if (applicationsList != null) {
                // Ensure list is still clear (in case of multiple rapid calls)
                applicationsList.getChildren().clear();
                
                if (apps != null) {
                    for (Application app : apps) {
                        // Load interview info for this application if it exists
                        Interview interview = null;
                        if (app.getStatus() == Application.Status.ACCEPTED) {
                            try {
                                var interviewOpt = interviewService.getInterviewByApplicationId(app.getId());
                                if (interviewOpt.isPresent()) {
                                    interview = interviewOpt.get();
                                }
                            } catch (Exception ex) {
                                logger.error("Error loading interview for application {}", app.getId(), ex);
                            }
                        }
                        
                        // Create line with name, status, interview info, and delete button
                        HBox line = createApplicationLine(
                                app,
                                app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()),
                                interview);
                        applicationsList.getChildren().add(line);
                    }
                }
            }

            updateCounts(apps != null ? apps.size() : 0);
        });

        task.setOnFailed(e -> {
            isLoading = false;
            if (statsLabel != null) {
                statsLabel.setText(I18n.get("applications.error"));
            }
            logger.error("Failed to load applications", task.getException());
        });

        new Thread(task).start();
    }

    private void updateCounts(int total) {
        statsLabel.setText(I18n.get("applications.count", total));
    }
    
    private HBox createApplicationLine(Application app, String title, Interview interview) {
        VBox container = new VBox(8);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: rgba(255, 255, 255, 0.02); -fx-background-radius: 4;");
        
        // Top row: Title, Status, and Delete button
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        // Title - takes most of the space
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-base");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);
        
        // Status badge
        TLBadge statusBadge = createStatusBadge(app.getStatus());
        
        // Delete button
        TLButton deleteBtn = new TLButton("🗑️", TLButton.ButtonVariant.GHOST);
        deleteBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("applications.delete_tooltip", "Supprimer cette candidature")));
        deleteBtn.setOnAction(e -> handleDeleteApplication(app.getId()));
        
        topRow.getChildren().addAll(titleLabel, statusBadge, deleteBtn);
        
        container.getChildren().add(topRow);

        Label aiLabel = new Label("AI Match: " +
                (app.getMatchPercentage() > 0 ? app.getMatchPercentage() + "%" : "-")
                + " | Score: " +
                (app.getCandidateScore() > 0 ? app.getCandidateScore() : "-"));
        aiLabel.getStyleClass().add("text-muted");
        aiLabel.setStyle("-fx-font-size: 0.85em;");
        container.getChildren().add(aiLabel);
        
        // Interview info row (if interview is scheduled)
        if (interview != null && interview.getInterviewDate() != null) {
            HBox interviewRow = new HBox(12);
            interviewRow.setAlignment(Pos.CENTER_LEFT);
            interviewRow.setPadding(new Insets(4, 0, 0, 0));
            
            Label interviewLabel = new Label();
            interviewLabel.getStyleClass().add("text-muted");
            interviewLabel.setStyle("-fx-font-size: 0.875em;");
            
            String interviewText = "📅 " + I18n.get("interviews.interview_scheduled") + ": " + 
                interview.getInterviewDate().format(DATE_TIME_FMT);
            
            if (interview.getLocation() != null && !interview.getLocation().isEmpty()) {
                interviewText += " | 📍 " + interview.getLocation();
            }
            
            if (interview.getInterviewType() != null) {
                interviewText += " | " + interview.getInterviewType().getDisplayName();
            }
            
            interviewLabel.setText(interviewText);
            interviewRow.getChildren().add(interviewLabel);
            
            container.getChildren().add(interviewRow);
        }
        
        // Wrap in HBox for compatibility
        HBox line = new HBox();
        line.getChildren().add(container);
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);
        
        return line;
    }
    
    private TLBadge createStatusBadge(Application.Status status) {
        String text;
        TLBadge.Variant variant;
        
        switch (status) {
            case PENDING:
                text = I18n.get("inbox.status.pending");
                variant = TLBadge.Variant.DEFAULT;
                break;
            case REVIEWING:
                text = I18n.get("inbox.status.review");
                variant = TLBadge.Variant.SUCCESS; // Vert
                break;
            case INTERVIEW:
                text = I18n.get("inbox.status.interview");
                variant = TLBadge.Variant.OUTLINE;
                break;
            case OFFER:
                text = I18n.get("inbox.status.offer");
                variant = TLBadge.Variant.OUTLINE;
                break;
            case ACCEPTED:
                text = I18n.get("inbox.status.accepted");
                variant = TLBadge.Variant.INFO; // Bleu
                break;
            case REJECTED:
                text = I18n.get("inbox.status.rejected");
                variant = TLBadge.Variant.DESTRUCTIVE; // Rouge
                break;
            default:
                text = I18n.get("inbox.status.pending");
                variant = TLBadge.Variant.DEFAULT;
                break;
        }
        
        return new TLBadge(text, variant);
    }
    
    @FXML
    private void handleRefresh() {
        if (currentUser != null) {
            loadApplications();
        }
    }
    
    // Make sure applications view refreshes when status changes
    // This will be called when user navigates back to this view
    public void refreshIfNeeded() {
        if (currentUser != null) {
            loadApplications();
        }
    }
    
    /**
     * Handle deletion of an application by the candidate.
     */
    private void handleDeleteApplication(int applicationId) {
        // Confirm deletion
        java.util.Optional<javafx.scene.control.ButtonType> result = com.skilora.utils.DialogUtils.showConfirmation(
            I18n.get("applications.delete_confirm_title", "Supprimer la candidature"),
            I18n.get("applications.delete_confirm_message", "Êtes-vous sûr de vouloir supprimer cette candidature ? Cette action est irréversible.")
        );
        
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
            return;
        }
        
        // Delete in background thread
        Task<Boolean> deleteTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return applicationService.delete(applicationId);
            }
        };
        
        deleteTask.setOnSucceeded(e -> {
            boolean success = deleteTask.getValue();
            if (success) {
                com.skilora.utils.DialogUtils.showInfo(
                    I18n.get("applications.delete_success_title", "Candidature supprimée"),
                    I18n.get("applications.delete_success_message", "Votre candidature a été supprimée avec succès.")
                );
                // Reload applications list
                loadApplications();
            } else {
                com.skilora.utils.DialogUtils.showError(
                    I18n.get("applications.delete_error_title", "Erreur"),
                    I18n.get("applications.delete_error_message", "Impossible de supprimer la candidature. Veuillez réessayer.")
                );
            }
        });
        
        deleteTask.setOnFailed(e -> {
            logger.error("Failed to delete application {}", applicationId, deleteTask.getException());
            com.skilora.utils.DialogUtils.showError(
                I18n.get("applications.delete_error_title", "Erreur"),
                I18n.get("applications.delete_error_message", "Une erreur est survenue lors de la suppression de la candidature.")
            );
        });
        
        new Thread(deleteTask, "DeleteApplication").start();
    }
}
