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

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.DialogEvent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
    /** Application ids that have a row in {@code interviews} (from last successful load). */
    private Set<Integer> scheduledApplicationIds = Set.of();
    /** Interview rows for this employer keyed by application id (from last load). */
    private Map<Integer, Interview> interviewByApplicationId = Map.of();
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";
    /** Only the latest load may update the UI (avoids stale data when a refresh is requested during an in-flight load). */
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    private record InterviewsPageData(
            List<Application> applications,
            Set<Integer> scheduledIds,
            Map<Integer, Interview> interviewsByAppId) {}

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

        final int generation = loadGeneration.incrementAndGet();

        // Clear existing data first
        if (interviewsContainer != null) {
            interviewsContainer.getChildren().clear();
        }
        
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("common.loading"));
        }

        Task<InterviewsPageData> task = new Task<>() {
            @Override
            protected InterviewsPageData call() throws Exception {
                LinkedHashMap<Integer, Application> byId = new LinkedHashMap<>();

                try {
                    List<Application> eligible = interviewService.getEligibleInterviewCandidatesForEmployer(
                            currentUser.getId());
                    for (Application a : eligible) {
                        byId.put(a.getId(), a);
                    }
                } catch (Exception ex) {
                    logger.error("Interviews page: eligible candidates query failed", ex);
                }

                Map<Integer, Interview> ivByApp = new HashMap<>();
                try {
                    List<Interview> scheduledInterviews = interviewService.getInterviewsForEmployer(currentUser.getId());
                    for (Interview interview : scheduledInterviews) {
                        ivByApp.put(interview.getApplicationId(), interview);
                        try {
                            Application app = applicationService.getApplicationById(interview.getApplicationId());
                            if (app != null && !byId.containsKey(app.getId())) {
                                if (interview.getCandidateName() != null && !interview.getCandidateName().isBlank()) {
                                    app.setCandidateName(interview.getCandidateName());
                                }
                                if (interview.getJobTitle() != null) {
                                    app.setJobTitle(interview.getJobTitle());
                                }
                                if (interview.getCompanyName() != null) {
                                    app.setCompanyName(interview.getCompanyName());
                                }
                                byId.put(app.getId(), app);
                            }
                        } catch (Exception e) {
                            logger.error("Error loading application for interview {}", interview.getId(), e);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Interviews page: scheduled interviews query failed", ex);
                }

                Set<Integer> scheduledIds;
                try {
                    scheduledIds = interviewService.findScheduledApplicationIds(byId.keySet());
                } catch (Exception ex) {
                    logger.warn("Batch scheduled interview lookup failed, falling back to employer list: {}", ex.getMessage());
                    scheduledIds = new HashSet<>(ivByApp.keySet());
                }

                return new InterviewsPageData(new ArrayList<>(byId.values()), scheduledIds, Collections.unmodifiableMap(new HashMap<>(ivByApp)));
            }
        };

        task.setOnSucceeded(e -> {
            if (generation != loadGeneration.get()) {
                return;
            }
            InterviewsPageData data = task.getValue();
            allInterviews = data.applications();
            scheduledApplicationIds = data.scheduledIds();
            interviewByApplicationId = data.interviewsByAppId();
            applyFilters();
        });

        task.setOnFailed(e -> {
            if (generation != loadGeneration.get()) {
                return;
            }
            logger.error("Failed to load interviews", task.getException());
            allInterviews = new ArrayList<>();
            scheduledApplicationIds = Set.of();
            interviewByApplicationId = Map.of();
            applyFilters();
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
            filtered = allInterviews.stream()
                .filter(app -> scheduledApplicationIds.contains(app.getId()))
                .collect(Collectors.toList());
        } else if ("UNSCHEDULED".equals(currentFilter)) {
            filtered = allInterviews.stream()
                .filter(app -> !scheduledApplicationIds.contains(app.getId()))
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

        long scheduledCount = interviews.stream()
            .filter(app -> scheduledApplicationIds.contains(app.getId()))
            .count();
        
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("interviews.count", interviews.size(), scheduledCount));
        }

        for (Application app : interviews) {
            interviewsContainer.getChildren().add(createInterviewCard(app));
        }
    }

    private static String resolveCandidateDisplayName(Application app) {
        String n = app.getCandidateName();
        if (n != null && !n.isBlank()) {
            return n;
        }
        if (app.getCandidateProfileId() > 0) {
            return I18n.get("interviews.candidate_num", app.getCandidateProfileId());
        }
        return I18n.get("interviews.candidate_application_ref", app.getId());
    }

    private TLCard createInterviewCard(Application app) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Top row: Candidate name + Status badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(resolveCandidateDisplayName(app));
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

        // ── Job info row (poste + lieu) ─────────────────────────
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

        // ── Application date row ─────────────────────────────────
        Label dateLabel = null;
        if (app.getAppliedDate() != null) {
            dateLabel = new Label("📋 " + I18n.get("interviews.application_date") + " : "
                    + app.getAppliedDate().format(DATE_FMT));
            dateLabel.getStyleClass().add("text-muted");
            dateLabel.setWrapText(true);
            dateLabel.setMaxWidth(Double.MAX_VALUE);
        }

        final boolean booked = scheduledApplicationIds.contains(app.getId());
        final Interview[] interviewRef = new Interview[1];
        interviewRef[0] = interviewByApplicationId.get(app.getId());
        if (interviewRef[0] == null && booked) {
            try {
                interviewRef[0] = interviewService.getInterviewByApplicationId(app.getId()).orElse(null);
            } catch (Exception ex) {
                logger.error("Error loading interview row for application {}", app.getId(), ex);
            }
        }

        // ── Interview details row (each piece on its own label) ───
        VBox interviewDetailsBox = null;
        InterviewCountdownWidget countdown = null;

        if (interviewRef[0] != null) {
            interviewDetailsBox = new VBox(4);

            // Scheduled date/time
            if (interviewRef[0].getInterviewDate() != null) {
                Label planLabel = new Label("📅 Planifié : "
                        + interviewRef[0].getInterviewDate().format(DATE_FMT));
                planLabel.getStyleClass().add("text-muted");
                planLabel.setWrapText(true);
                planLabel.setMaxWidth(Double.MAX_VALUE);
                interviewDetailsBox.getChildren().add(planLabel);
            }

            // Location
            if (interviewRef[0].getLocation() != null
                    && !interviewRef[0].getLocation().isEmpty()) {
                Label ivLocLabel = new Label("📍 " + interviewRef[0].getLocation());
                ivLocLabel.getStyleClass().add("text-muted");
                ivLocLabel.setWrapText(true);
                ivLocLabel.setMaxWidth(Double.MAX_VALUE);
                interviewDetailsBox.getChildren().add(ivLocLabel);
            }

            // Type
            if (interviewRef[0].getInterviewType() != null) {
                Label typeLabel = new Label("🎯 " + interviewRef[0].getInterviewType().getDisplayName());
                typeLabel.getStyleClass().add("text-muted");
                interviewDetailsBox.getChildren().add(typeLabel);
            }

            // Live countdown badge
            countdown = InterviewCountdownWidget.of(interviewRef[0].getInterviewDate());
        }

        // ── Actions ──────────────────────────────────────────────
        Separator sep = new Separator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (booked) {
            TLButton editBtn = new TLButton(I18n.get("interviews.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> openScheduleDialog(app, interviewRef[0]));
            actionsRow.getChildren().add(editBtn);
        } else {
            TLButton scheduleBtn = new TLButton(I18n.get("interviews.schedule"), TLButton.ButtonVariant.PRIMARY);
            scheduleBtn.setOnAction(e -> openScheduleDialog(app, null));
            actionsRow.getChildren().add(scheduleBtn);
        }

        // ── Assemble card ────────────────────────────────────────
        content.getChildren().add(topRow);
        if (!jobRow.getChildren().isEmpty())   content.getChildren().add(jobRow);
        if (dateLabel != null)                 content.getChildren().add(dateLabel);
        if (interviewDetailsBox != null)       content.getChildren().add(interviewDetailsBox);
        if (countdown != null)                 content.getChildren().add(countdown);
        content.getChildren().addAll(sep, actionsRow);
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
                    // Align with fiche candidature: statut ENTRETIEN + rechargement après commit SQL
                    Thread refresh = new Thread(() -> {
                        try {
                            applicationService.updateStatus(app.getId(), Application.Status.INTERVIEW);
                            app.setStatus(Application.Status.INTERVIEW);
                        } catch (Exception ex) {
                            logger.warn("Could not set application status to INTERVIEW after scheduling: {}", ex.getMessage());
                        }
                        Platform.runLater(InterviewsController.this::loadData);
                    }, "InterviewScheduled-Refresh");
                    refresh.setDaemon(true);
                    refresh.start();
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

            // Sauvegarde explicite sur le bouton : le resultConverter seul est parfois peu fiable avec TLDialog.
            EventHandler<DialogEvent> previousOnShowing = dialog.getOnShowing();
            dialog.setOnShowing(ev -> {
                if (previousOnShowing != null) {
                    previousOnShowing.handle(ev);
                }
                Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
                if (saveBtn != null && controller != null) {
                    saveBtn.addEventFilter(ActionEvent.ACTION, evt -> {
                        evt.consume();
                        if (controller.validateAndSave()) {
                            dialog.setResult(Boolean.TRUE);
                            dialog.close();
                        }
                    });
                }
            });

            dialog.setResultConverter(btnType -> {
                if (btnType == cancelBtnType) {
                    return Boolean.FALSE;
                }
                return null;
            });

            dialog.showAndWait();
            
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
