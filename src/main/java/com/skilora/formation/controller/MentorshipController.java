package com.skilora.formation.controller;

import com.skilora.framework.components.*;
import com.skilora.formation.entity.Mentorship;
import com.skilora.user.entity.User;
import com.skilora.formation.service.MentorshipService;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void initializeContext(User user) {
        this.currentUser = user;
        titleLabel.setText(I18n.get("mentorship.title"));
        loadMentorships();
    }

    @FXML
    private void handleRequest() {
        if (currentUser == null) return;

        // Load available mentors in background, then show dialog
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

        loadMentorsTask.setOnFailed(ev -> logger.error("Failed to load mentors", loadMentorsTask.getException()));
        Thread t = new Thread(loadMentorsTask);
        t.setDaemon(true);
        t.start();
    }

    private void showRequestDialog(LinkedHashMap<Integer, String> mentors) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("mentorship.request"));
        dialog.setDescription(I18n.get("mentorship.request.description"));
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        // Mentor selection dropdown
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
                loadMentorships();
            }
        });
        task.setOnFailed(e -> logger.error("Mentorship request failed", task.getException()));
        new Thread(task).start();
    }

    private void loadMentorships() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;
        
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
                    contentPane.getChildren().add(createMentorshipCard(mentorship));
                }
            }
        });
        new Thread(task).start();
    }

    private TLCard createMentorshipCard(Mentorship mentorship) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        Label topic = new Label(mentorship.getTopic());
        topic.getStyleClass().add("h4");
        
        Label mentor = new Label(I18n.get("mentorship.as_mentor") + ": " + mentorship.getMentorName());
        mentor.getStyleClass().add("text-muted");
        
        TLBadge badge = new TLBadge(mentorship.getStatus().name(), TLBadge.Variant.DEFAULT);
        
        content.getChildren().addAll(topic, mentor, badge);
        card.setContent(content);
        return card;
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
