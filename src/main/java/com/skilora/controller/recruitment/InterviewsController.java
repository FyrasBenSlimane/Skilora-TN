package com.skilora.controller.recruitment;

import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;
import com.skilora.service.recruitment.InterviewService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.InterviewCountdownWidget;

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

    private final InterviewService interviewService = InterviewService.getInstance();
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private User currentUser;
    private List<Application> allInterviews;
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";
    private boolean isLoading = false;

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
        // Simplified filters: only show scheduled/unscheduled interviews
        String[][] filters = {
            {I18n.get("interviews.filter.all"), "ALL"},
            {I18n.get("interviews.filter.scheduled"), "SCHEDULED"},
            {I18n.get("interviews.filter.unscheduled"), "UNSCHEDULED"}
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
        
        // Prevent multiple simultaneous loads
        if (isLoading) {
            logger.debug("Load already in progress, skipping duplicate call");
            return;
        }
        
        isLoading = true;
        
        // Clear existing data first
        if (interviewsContainer != null) {
            interviewsContainer.getChildren().clear();
        }
        
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("common.loading"));
        }

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                // Get both eligible candidates AND scheduled interviews
                List<Application> allCandidates = interviewService.getEligibleInterviewCandidatesForEmployer(currentUser.getId());
                
                // Get scheduled interviews and convert them to Application objects
                List<Interview> scheduledInterviews = interviewService.getInterviewsForEmployer(currentUser.getId());
                
                for (Interview interview : scheduledInterviews) {
                    try {
                        Application app = applicationService.getApplicationById(interview.getApplicationId());
                        if (app != null) {
                            // Set transient fields from interview
                            app.setCandidateName(interview.getCandidateName());
                            app.setJobTitle(interview.getJobTitle());
                            app.setCompanyName(interview.getCompanyName());
                            allCandidates.add(app);
                        }
                    } catch (Exception e) {
                        logger.error("Error loading application for interview {}", interview.getId(), e);
                    }
                }
                
                return allCandidates;
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;
            allInterviews = task.getValue();
            applyFilters();
        });

        task.setOnFailed(e -> {
            isLoading = false;
            logger.error("Failed to load interviews", task.getException());
            if (statsLabel != null) {
                statsLabel.setText(I18n.get("interviews.error"));
            }
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
        } else if ("SCHEDULED".equals(currentFilter)) {
            // Filter to applications that have interviews scheduled
            filtered = allInterviews.stream()
                .filter(app -> {
                    try {
                        return interviewService.getInterviewByApplicationId(app.getId()).isPresent();
                    } catch (Exception e) {
                        logger.error("Error checking interview for application {}", app.getId(), e);
                        return false;
                    }
                })
                .collect(Collectors.toList());
        } else if ("UNSCHEDULED".equals(currentFilter)) {
            // Filter to applications without interviews
            filtered = allInterviews.stream()
                .filter(app -> {
                    try {
                        return interviewService.getInterviewByApplicationId(app.getId()).isEmpty();
                    } catch (Exception e) {
                        logger.error("Error checking interview for application {}", app.getId(), e);
                        return true;
                    }
                })
                .collect(Collectors.toList());
        } else {
            filtered = allInterviews;
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

        // Count scheduled vs unscheduled
        long scheduledCount = interviews.stream()
            .filter(app -> {
                try {
                    return interviewService.getInterviewByApplicationId(app.getId()).isPresent();
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
        
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("interviews.count", interviews.size(), scheduledCount));
        }

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
            case PENDING:
            case REVIEWING:
                // These shouldn't appear (only ACCEPTED), but handle them anyway
                statusBadge.getStyleClass().add("badge-default");
                break;
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

        // Check if interview is already scheduled
        final Interview[] existingInterviewRef = new Interview[1];
        boolean hasInterview = false;
        try {
            var interviewOpt = interviewService.getInterviewByApplicationId(app.getId());
            if (interviewOpt.isPresent()) {
                hasInterview = true;
                existingInterviewRef[0] = interviewOpt.get();
            }
        } catch (Exception e) {
            logger.error("Error checking interview for application {}", app.getId(), e);
        }
        
        final boolean hasInterviewFinal = hasInterview;
        final Interview existingInterviewFinal = existingInterviewRef[0];
        
        // Show interview details + countdown if scheduled
        InterviewCountdownWidget countdown = null;
        if (hasInterviewFinal && existingInterviewFinal != null) {
            Label interviewInfo = new Label();
            interviewInfo.getStyleClass().add("text-muted");
            String interviewText = "📅 " + I18n.get("interviews.scheduled") + ": " +
                existingInterviewFinal.getInterviewDate().format(DATE_FMT);
            if (existingInterviewFinal.getLocation() != null && !existingInterviewFinal.getLocation().isEmpty()) {
                interviewText += " | 📍 " + existingInterviewFinal.getLocation();
            }
            if (existingInterviewFinal.getInterviewType() != null) {
                interviewText += " | " + existingInterviewFinal.getInterviewType().getDisplayName();
            }
            interviewInfo.setText(interviewText);
            detailsRow.getChildren().add(interviewInfo);

            // Live countdown badge
            countdown = InterviewCountdownWidget.of(existingInterviewFinal.getInterviewDate());
        }

        // Actions
        Separator sep = new Separator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (hasInterviewFinal) {
            // Show edit/reschedule button
            TLButton editBtn = new TLButton(I18n.get("interviews.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> openScheduleDialog(app, existingInterviewFinal));
            actionsRow.getChildren().add(editBtn);
        } else {
            // Show schedule interview button
            TLButton scheduleBtn = new TLButton(I18n.get("interviews.schedule"), TLButton.ButtonVariant.PRIMARY);
            scheduleBtn.setOnAction(e -> openScheduleDialog(app, null));
            actionsRow.getChildren().add(scheduleBtn);
        }

        if (countdown != null) {
            content.getChildren().addAll(topRow, detailsRow, countdown, sep, actionsRow);
        } else {
            content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        }
        card.getChildren().add(content);
        return card;
    }

    private void openScheduleDialog(Application app, Interview existingInterview) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/skilora/view/recruitment/ScheduleInterviewView.fxml"));
            javafx.scene.layout.VBox dialogContent = loader.load();
            
            ScheduleInterviewController controller = loader.getController();
            if (controller != null) {
                controller.setup(app, existingInterview, () -> {
                    // Refresh after scheduling
                    loadData();
                });
            }
            
            TLDialog<Boolean> dialog = new TLDialog<>();
            if (interviewsContainer.getScene() != null && interviewsContainer.getScene().getWindow() != null) {
                dialog.initOwner(interviewsContainer.getScene().getWindow());
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            }
            
            dialog.setDialogTitle(existingInterview != null ? I18n.get("interviews.edit_interview") : I18n.get("interviews.schedule_interview"));
            dialog.setDescription(I18n.get("interviews.schedule_description"));
            dialog.setContent(dialogContent);
            
            javafx.scene.control.ButtonType saveBtnType = new javafx.scene.control.ButtonType(I18n.get("common.save"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancelBtnType = new javafx.scene.control.ButtonType(I18n.get("common.cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, cancelBtnType);
            
            dialog.setResultConverter(btnType -> {
                if (btnType == saveBtnType && controller != null) {
                    if (controller.validateAndSave()) {
                        return true; // Return true to indicate success
                    }
                }
                return null;
            });
            
            dialog.showAndWait().ifPresent(result -> {
                // Data already saved by controller, just refresh
                loadData();
            });
            
        } catch (Exception e) {
            logger.error("Failed to open schedule interview dialog", e);
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible d'ouvrir le formulaire de planification: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
