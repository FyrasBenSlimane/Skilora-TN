package com.skilora.formation.controller;

import com.skilora.framework.components.*;
import com.skilora.formation.entity.Mentorship;
import com.skilora.formation.enums.MentorshipStatus;
import com.skilora.user.entity.User;
import com.skilora.formation.service.MentorshipService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MentorshipController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MentorshipController.class);

    @FXML private Label titleLabel;
    @FXML private TLButton requestBtn;
    @FXML private VBox contentPane;

    private User currentUser;
    private boolean showingMentorView = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void initializeContext(User user) {
        this.currentUser = user;
        titleLabel.setText(I18n.get("mentorship.title"));
        showingMentorView = false;
        loadMenteeView();
    }

    @FXML
    private void handleRequest() {
        if (currentUser == null) return;

        Task<LinkedHashMap<Integer, String>> loadMentorsTask = new Task<>() {
            @Override
            protected LinkedHashMap<Integer, String> call() {
                return MentorshipService.getInstance().findAvailableMentors(currentUser.getId());
            }
        };

        loadMentorsTask.setOnSucceeded(ev -> {
            LinkedHashMap<Integer, String> mentors = loadMentorsTask.getValue();
            if (mentors.isEmpty()) {
                DialogUtils.showInfo(I18n.get("mentorship.title"), I18n.get("mentorship.no_mentors_available"));
                return;
            }
            showRequestDialog(mentors);
        });

        loadMentorsTask.setOnFailed(ev -> {
            logger.error("Failed to load mentors", loadMentorsTask.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_mentors"));
        });
        AppThreadPool.execute(loadMentorsTask);
    }

    private void showRequestDialog(LinkedHashMap<Integer, String> mentors) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("mentorship.request"));
        dialog.setDescription(I18n.get("mentorship.request.description"));
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLSelect<String> mentorSelect = new TLSelect<>(I18n.get("mentorship.select_mentor"));
        Map<String, Integer> nameToIdMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : mentors.entrySet()) {
            String displayName = entry.getValue();
            mentorSelect.getItems().add(displayName);
            nameToIdMap.put(displayName, entry.getKey());
        }
        mentorSelect.setPromptText(I18n.get("mentorship.select_mentor.prompt"));
        
        TLTextField topicField = new TLTextField();
        topicField.setPromptText(I18n.get("mentorship.topic.placeholder"));
        
        TLTextarea goalsArea = new TLTextarea();
        goalsArea.setPromptText(I18n.get("mentorship.goals.placeholder"));
        goalsArea.getControl().setPrefRowCount(5);
        
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        TLButton cancelBtn = new TLButton();
        cancelBtn.setText(I18n.get("common.cancel"));
        cancelBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());
        
        TLButton submitBtn = new TLButton();
        submitBtn.setText(I18n.get("common.submit"));
        submitBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        submitBtn.setOnAction(e -> {
            String selectedMentor = mentorSelect.getValue();
            if (selectedMentor == null || selectedMentor.isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"),
                    I18n.get("error.validation.required", I18n.get("mentorship.mentor")));
                return;
            }
            String topic = topicField.getText();
            String goals = goalsArea.getText();
            if (topic == null || topic.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"),
                    I18n.get("error.validation.required", I18n.get("mentorship.topic")));
                return;
            }
            if (goals == null || goals.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"),
                    I18n.get("error.validation.required", I18n.get("mentorship.goals")));
                return;
            }
            int mentorId = nameToIdMap.get(selectedMentor);
            requestMentorship(mentorId, topic.trim(), goals.trim());
            dialog.close();
        });
        
        buttons.getChildren().addAll(cancelBtn, submitBtn);
        content.getChildren().addAll(mentorSelect, topicField, goalsArea, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void requestMentorship(int mentorId, String topic, String goals) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return MentorshipService.getInstance().requestMentorship(currentUser.getId(), mentorId, topic, goals);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue() > 0) {
                TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("mentorship.success.requested"));
                loadMenteeView();
            }
        });
        task.setOnFailed(e -> {
            logger.error("Mentorship request failed", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_mentorship_request"));
        });
        AppThreadPool.execute(task);
    }

    // ──────────── View Toggle ────────────

    private void loadMenteeView() {
        showingMentorView = false;
        contentPane.getChildren().clear();
        if (currentUser == null) return;

        // Toggle button to switch to mentor view
        HBox toggleBar = new HBox(8);
        toggleBar.setAlignment(Pos.CENTER_LEFT);
        toggleBar.setPadding(new Insets(0, 0, 8, 0));
        TLButton toggleBtn = new TLButton(I18n.get("mentorship.view_as_mentor"));
        toggleBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        toggleBtn.setOnAction(e -> loadMentorView());
        Label currentViewLabel = new Label(I18n.get("mentorship.as_mentee"));
        currentViewLabel.getStyleClass().add("text-bold");
        toggleBar.getChildren().addAll(currentViewLabel, toggleBtn);
        contentPane.getChildren().add(toggleBar);

        Task<List<Mentorship>> task = new Task<>() {
            @Override
            protected List<Mentorship> call() {
                return MentorshipService.getInstance().findByMentee(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Mentorship> mentorships = task.getValue();
            if (mentorships.isEmpty()) {
                Label empty = new Label(I18n.get("mentorship.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                for (Mentorship mentorship : mentorships) {
                    contentPane.getChildren().add(createMenteeCard(mentorship));
                }
            }
        });
        AppThreadPool.execute(task);
    }

    private void loadMentorView() {
        showingMentorView = true;
        contentPane.getChildren().clear();
        if (currentUser == null) return;

        // Toggle button to switch back to mentee view
        HBox toggleBar = new HBox(8);
        toggleBar.setAlignment(Pos.CENTER_LEFT);
        toggleBar.setPadding(new Insets(0, 0, 8, 0));
        TLButton toggleBtn = new TLButton(I18n.get("mentorship.view_as_mentee"));
        toggleBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        toggleBtn.setOnAction(e -> loadMenteeView());
        Label currentViewLabel = new Label(I18n.get("mentorship.as_mentor"));
        currentViewLabel.getStyleClass().add("text-bold");
        toggleBar.getChildren().addAll(currentViewLabel, toggleBtn);
        contentPane.getChildren().add(toggleBar);

        // Load pending requests first, then all mentorships
        Task<List<Mentorship>> pendingTask = new Task<>() {
            @Override
            protected List<Mentorship> call() {
                return MentorshipService.getInstance().findPendingForMentor(currentUser.getId());
            }
        };

        Task<List<Mentorship>> allTask = new Task<>() {
            @Override
            protected List<Mentorship> call() {
                return MentorshipService.getInstance().findByMentor(currentUser.getId());
            }
        };

        pendingTask.setOnSucceeded(e -> {
            List<Mentorship> pending = pendingTask.getValue();
            if (!pending.isEmpty()) {
                Label pendingTitle = new Label(I18n.get("mentorship.pending_requests") + " (" + pending.size() + ")");
                pendingTitle.getStyleClass().add("h4");
                contentPane.getChildren().add(pendingTitle);
                for (Mentorship m : pending) {
                    contentPane.getChildren().add(createMentorPendingCard(m));
                }
            }
            // Now load all
            allTask.setOnSucceeded(ev -> {
                List<Mentorship> all = allTask.getValue();
                // Filter out pending (already shown above)
                List<Mentorship> active = all.stream()
                        .filter(m -> m.getStatus() != MentorshipStatus.PENDING)
                        .toList();
                if (!active.isEmpty()) {
                    Label activeTitle = new Label(I18n.get("mentorship.active_mentorships"));
                    activeTitle.getStyleClass().add("h4");
                    activeTitle.setPadding(new Insets(16, 0, 0, 0));
                    contentPane.getChildren().add(activeTitle);
                    for (Mentorship m : active) {
                        contentPane.getChildren().add(createMentorCard(m));
                    }
                }
                if (pending.isEmpty() && active.isEmpty()) {
                    Label empty = new Label(I18n.get("mentorship.mentor.empty"));
                    empty.getStyleClass().add("text-muted");
                    contentPane.getChildren().add(empty);
                }
            });
            AppThreadPool.execute(allTask);
        });
        AppThreadPool.execute(pendingTask);
    }

    // ──────────── Card Renderers ────────────

    private TLCard createMenteeCard(Mentorship mentorship) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        Label topic = new Label(mentorship.getTopic());
        topic.getStyleClass().add("h4");
        
        Label mentor = new Label(I18n.get("mentorship.mentor") + ": " + mentorship.getMentorName());
        mentor.getStyleClass().add("text-muted");
        
        TLBadge badge = new TLBadge(
                mentorship.getStatus().name().charAt(0) + mentorship.getStatus().name().substring(1).toLowerCase(),
                getBadgeVariant(mentorship.getStatus()));
        
        content.getChildren().addAll(topic, mentor, badge);
        card.setContent(content);
        return card;
    }

    private TLCard createMentorPendingCard(Mentorship mentorship) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        Label topic = new Label(mentorship.getTopic());
        topic.getStyleClass().add("h4");

        Label mentee = new Label(I18n.get("mentorship.mentee") + ": " + mentorship.getMenteeName());
        mentee.getStyleClass().add("text-muted");

        Label goals = new Label(mentorship.getGoals());
        goals.setWrapText(true);

        TLBadge badge = new TLBadge(
                MentorshipStatus.PENDING.name().charAt(0) + MentorshipStatus.PENDING.name().substring(1).toLowerCase(),
                TLBadge.Variant.DEFAULT);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        TLButton acceptBtn = new TLButton(I18n.get("mentorship.accept"));
        acceptBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        acceptBtn.setOnAction(e -> acceptMentorship(mentorship.getId()));

        TLButton cancelBtn = new TLButton(I18n.get("mentorship.decline"));
        cancelBtn.setVariant(TLButton.ButtonVariant.DANGER);
        cancelBtn.setOnAction(e -> cancelMentorship(mentorship.getId()));

        actions.getChildren().addAll(acceptBtn, cancelBtn);
        content.getChildren().addAll(topic, mentee, goals, badge, actions);
        card.setContent(content);
        return card;
    }

    private TLCard createMentorCard(Mentorship mentorship) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        Label topic = new Label(mentorship.getTopic());
        topic.getStyleClass().add("h4");

        Label mentee = new Label(I18n.get("mentorship.mentee") + ": " + mentorship.getMenteeName());
        mentee.getStyleClass().add("text-muted");

        TLBadge badge = new TLBadge(
                mentorship.getStatus().name().charAt(0) + mentorship.getStatus().name().substring(1).toLowerCase(),
                getBadgeVariant(mentorship.getStatus()));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        if (mentorship.getStatus() == MentorshipStatus.ACTIVE) {
            TLButton completeBtn = new TLButton(I18n.get("mentorship.complete"));
            completeBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            completeBtn.setOnAction(e -> showCompleteDialog(mentorship.getId()));

            TLButton cancelBtn = new TLButton(I18n.get("common.cancel"));
            cancelBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
            cancelBtn.setOnAction(e -> cancelMentorship(mentorship.getId()));

            actions.getChildren().addAll(completeBtn, cancelBtn);
        }

        content.getChildren().addAll(topic, mentee, badge, actions);
        card.setContent(content);
        return card;
    }

    // ──────────── Mentor Actions ────────────

    private void acceptMentorship(int id) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return MentorshipService.getInstance().accept(id);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("mentorship.success.accepted"));
                loadMentorView();
            }
        });
        AppThreadPool.execute(task);
    }

    private void cancelMentorship(int id) {
        DialogUtils.showConfirmation(
            I18n.get("mentorship.cancel.confirm.title"),
            I18n.get("mentorship.cancel.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return MentorshipService.getInstance().cancel(id);
                    }
                };
                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        if (showingMentorView) loadMentorView(); else loadMenteeView();
                    }
                });
                AppThreadPool.execute(task);
            }
        });
    }

    private void showCompleteDialog(int mentorshipId) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("mentorship.complete"));
        dialog.setDescription(I18n.get("mentorship.complete.description"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label ratingLabel = new Label(I18n.get("mentorship.rating"));
        TLSelect<String> ratingSelect = new TLSelect<>(I18n.get("mentorship.rating"));
        ratingSelect.getItems().addAll("1", "2", "3", "4", "5");
        ratingSelect.setValue("5");

        TLTextarea feedbackArea = new TLTextarea();
        feedbackArea.setPromptText(I18n.get("mentorship.feedback.placeholder"));
        feedbackArea.getControl().setPrefRowCount(4);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"));
        cancelBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton submitBtn = new TLButton(I18n.get("common.submit"));
        submitBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        submitBtn.setOnAction(e -> {
            int rating = Integer.parseInt(ratingSelect.getValue());
            String feedback = feedbackArea.getText() != null ? feedbackArea.getText().trim() : "";
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return MentorshipService.getInstance().complete(mentorshipId, rating, feedback);
                }
            };
            task.setOnSucceeded(ev -> {
                if (task.getValue()) {
                    TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("mentorship.success.completed"));
                    loadMentorView();
                }
            });
            AppThreadPool.execute(task);
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, submitBtn);
        content.getChildren().addAll(ratingLabel, ratingSelect, feedbackArea, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private TLBadge.Variant getBadgeVariant(MentorshipStatus status) {
        return switch (status) {
            case PENDING -> TLBadge.Variant.DEFAULT;
            case ACTIVE -> TLBadge.Variant.SUCCESS;
            case COMPLETED -> TLBadge.Variant.SECONDARY;
            case CANCELLED -> TLBadge.Variant.DESTRUCTIVE;
        };
    }
}
