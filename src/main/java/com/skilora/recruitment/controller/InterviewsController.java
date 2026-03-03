package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Interview;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

/**
 * InterviewsController - Employer interview management (Réunions).
 * Same behaviour and layout as src 2: stats, filter chips (Tous / Planifiés / À planifier),
 * list of TLCards with candidate name, status badge, job/location, date, interview details, countdown, Planifier/Modifier.
 */
public class InterviewsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InterviewsController.class);
    private static final java.time.format.DateTimeFormatter DATE_FMT = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        String[][] filters = {
            { I18n.get("interviews.filter.all"), "ALL" },
            { I18n.get("interviews.tab.scheduled"), "SCHEDULED" },
            { I18n.get("interviews.tab.to_schedule"), "UNSCHEDULED" }
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
        if (isLoading) return;
        isLoading = true;
        if (interviewsContainer != null) interviewsContainer.getChildren().clear();
        if (statsLabel != null) statsLabel.setText(I18n.get("common.loading"));

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                List<Application> allCandidates = applicationService.getApplicationsByCompanyOwner(currentUser.getId())
                    .stream()
                    .filter(a -> a.getStatus() != Application.Status.REJECTED)
                    .collect(Collectors.toList());
                List<Interview> scheduledInterviews = interviewService.findByEmployer(currentUser.getId());
                Set<Integer> existingIds = allCandidates.stream().map(Application::getId).collect(Collectors.toSet());
                for (Interview interview : scheduledInterviews) {
                    if (existingIds.contains(interview.getApplicationId())) continue;
                    try {
                        Application app = applicationService.getApplicationById(interview.getApplicationId());
                        if (app != null) {
                            app.setCandidateName(interview.getCandidateName());
                            app.setJobTitle(interview.getJobTitle());
                            app.setCompanyName(interview.getCompanyName());
                            allCandidates.add(app);
                            existingIds.add(app.getId());
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
            if (statsLabel != null) statsLabel.setText(I18n.get("interviews.error"));
        });
        Thread t = new Thread(task, "InterviewsLoader");
        t.setDaemon(true);
        t.start();
    }

    private boolean hasScheduledInterview(int applicationId) {
        try {
            List<Interview> list = interviewService.findByApplication(applicationId);
            return list != null && !list.isEmpty();
        } catch (Exception e) {
            logger.error("Error checking interview for application {}", applicationId, e);
            return false;
        }
    }

    private Interview getScheduledInterview(int applicationId) {
        try {
            List<Interview> list = interviewService.findByApplication(applicationId);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void applyFilters() {
        if (allInterviews == null) return;
        List<Application> filtered;
        if ("ALL".equals(currentFilter)) {
            filtered = allInterviews;
        } else if ("SCHEDULED".equals(currentFilter)) {
            filtered = allInterviews.stream()
                .filter(app -> hasScheduledInterview(app.getId()))
                .collect(Collectors.toList());
        } else if ("UNSCHEDULED".equals(currentFilter)) {
            filtered = allInterviews.stream()
                .filter(app -> !hasScheduledInterview(app.getId()))
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
            if (statsLabel != null) statsLabel.setText(I18n.get("interviews.count.zero"));
            return;
        }
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        long scheduledCount = interviews.stream().filter(app -> hasScheduledInterview(app.getId())).count();
        if (statsLabel != null) statsLabel.setText(I18n.get("interviews.count", interviews.size(), scheduledCount));
        for (Application app : interviews) {
            interviewsContainer.getChildren().add(createInterviewCard(app));
        }
    }

    private TLCard createInterviewCard(Application app) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(app.getCandidateName() != null ? app.getCandidateName() : I18n.get("interviews.candidate_num", app.getCandidateProfileId()));
        nameLabel.getStyleClass().add("h4");
        TLBadge statusBadge = new TLBadge(app.getStatus().getDisplayName(), TLBadge.Variant.DEFAULT);
        switch (app.getStatus()) {
            case PENDING:
            case REVIEWING:
                statusBadge.getStyleClass().add("badge-default");
                break;
            case INTERVIEW:
                statusBadge.getStyleClass().add("badge-warning");
                break;
            case OFFER:
            case ACCEPTED:
                statusBadge.getStyleClass().add("badge-success");
                break;
            case REJECTED:
                statusBadge.getStyleClass().add("badge-destructive");
                break;
            default:
                break;
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(nameLabel, spacer, statusBadge);

        HBox jobRow = new HBox(16);
        jobRow.setAlignment(Pos.CENTER_LEFT);
        if (app.getJobTitle() != null) {
            Label jobLabel = new Label("💼 " + app.getJobTitle());
            jobLabel.getStyleClass().add("text-muted");
            jobLabel.setWrapText(true);
            jobLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(jobLabel, Priority.ALWAYS);
            jobRow.getChildren().add(jobLabel);
        }
        if (app.getJobLocation() != null) {
            Label locLabel = new Label("📍 " + app.getJobLocation());
            locLabel.getStyleClass().add("text-muted");
            locLabel.setWrapText(true);
            jobRow.getChildren().add(locLabel);
        }

        Label dateLabel = null;
        if (app.getAppliedDate() != null) {
            dateLabel = new Label("📋 " + I18n.get("interviews.application_date") + " : " + app.getAppliedDate().format(DATE_FMT));
            dateLabel.getStyleClass().add("text-muted");
            dateLabel.setWrapText(true);
            dateLabel.setMaxWidth(Double.MAX_VALUE);
        }

        Interview existingInterview = getScheduledInterview(app.getId());
        VBox interviewDetailsBox = null;
        InterviewCountdownWidget countdown = null;
        if (existingInterview != null) {
            interviewDetailsBox = new VBox(4);
            if (existingInterview.getScheduledDate() != null) {
                Label planLabel = new Label("📅 Planifié : " + existingInterview.getScheduledDate().format(DATE_FMT));
                planLabel.getStyleClass().add("text-muted");
                planLabel.setWrapText(true);
                planLabel.setMaxWidth(Double.MAX_VALUE);
                interviewDetailsBox.getChildren().add(planLabel);
            }
            if (existingInterview.getLocation() != null && !existingInterview.getLocation().isEmpty()) {
                Label ivLocLabel = new Label("📍 " + existingInterview.getLocation());
                ivLocLabel.getStyleClass().add("text-muted");
                ivLocLabel.setWrapText(true);
                ivLocLabel.setMaxWidth(Double.MAX_VALUE);
                interviewDetailsBox.getChildren().add(ivLocLabel);
            }
            if (existingInterview.getType() != null && !existingInterview.getType().isEmpty()) {
                String typeDisplay = formatInterviewType(existingInterview.getType());
                Label typeLabel = new Label("🎯 " + typeDisplay);
                typeLabel.getStyleClass().add("text-muted");
                interviewDetailsBox.getChildren().add(typeLabel);
            }
            if (existingInterview.getScheduledDate() != null) {
                countdown = InterviewCountdownWidget.of(existingInterview.getScheduledDate());
            }
        }

        Separator sep = new Separator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        if (existingInterview != null) {
            TLButton editBtn = new TLButton(I18n.get("interviews.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> openScheduleDialog(app, existingInterview));
            actionsRow.getChildren().add(editBtn);
        } else {
            TLButton scheduleBtn = new TLButton(I18n.get("interviews.schedule"), TLButton.ButtonVariant.PRIMARY);
            scheduleBtn.setOnAction(e -> openScheduleDialog(app, null));
            actionsRow.getChildren().add(scheduleBtn);
        }

        content.getChildren().add(topRow);
        if (!jobRow.getChildren().isEmpty()) content.getChildren().add(jobRow);
        if (dateLabel != null) content.getChildren().add(dateLabel);
        if (interviewDetailsBox != null) content.getChildren().add(interviewDetailsBox);
        if (countdown != null) content.getChildren().add(countdown);
        content.getChildren().addAll(sep, actionsRow);
        card.getChildren().add(content);
        return card;
    }

    private static String formatInterviewType(String type) {
        if (type == null) return "";
        switch (type.toUpperCase()) {
            case "VIDEO": return "Vidéo";
            case "IN_PERSON": return "En personne";
            case "PHONE": return "Téléphone";
            case "ONLINE": return "En ligne";
            default: return type;
        }
    }

    private void openScheduleDialog(Application app, Interview existingInterview) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/skilora/view/recruitment/ScheduleInterviewView.fxml"));
            javafx.scene.layout.VBox dialogContent = loader.load();
            ScheduleInterviewController controller = loader.getController();
            if (controller != null) {
                controller.setup(app, existingInterview, this::loadData);
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
                if (btnType == saveBtnType && controller != null && controller.validateAndSave()) return Boolean.TRUE;
                return null;
            });
            dialog.showAndWait().ifPresent(result -> { if (Boolean.TRUE.equals(result)) loadData(); });
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
