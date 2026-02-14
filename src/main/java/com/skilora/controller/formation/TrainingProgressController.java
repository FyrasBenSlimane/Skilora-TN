package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.formation.TrainingEnrollment;
import com.skilora.model.entity.formation.TrainingLesson;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.formation.*;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TrainingProgressController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TrainingProgressController.class);

    @FXML private Label totalEnrollmentsLabel;
    @FXML private Label completedLabel;
    @FXML private Label inProgressLabel;
    @FXML private VBox trainingsContainer;
    @FXML private VBox emptyState;

    private User currentUser;
    private java.util.function.Consumer<Training> onTrainingClick;

    private final TrainingEnrollmentService enrollmentService = TrainingEnrollmentService.getInstance();
    private final TrainingService trainingService = TrainingService.getInstance();
    private final TrainingLessonService lessonService = TrainingLessonService.getInstance();
    private final LessonProgressService progressService = LessonProgressService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data loaded via setCurrentUser
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadProgress();
    }

    public void setOnTrainingClick(java.util.function.Consumer<Training> callback) {
        this.onTrainingClick = callback;
    }

    /**
     * Public method to refresh the enrollments list
     * Can be called from outside to force a reload
     */
    public void refresh() {
        logger.debug("Refresh requested for TrainingProgressController");
        loadProgress();
    }

    private void loadProgress() {
        if (currentUser == null) return;

        Thread loadThread = new Thread(() -> {
            try {
                List<TrainingEnrollment> enrollments = enrollmentService.getUserEnrollments(currentUser.getId());
                Platform.runLater(() -> {
                    renderProgress(enrollments);
                });
            } catch (Exception e) {
                logger.error("Error loading progress", e);
            }
        }, "LoadProgressThread");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void renderProgress(List<TrainingEnrollment> enrollments) {
        trainingsContainer.getChildren().clear();

        if (enrollments.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            totalEnrollmentsLabel.setText("0");
            completedLabel.setText("0");
            inProgressLabel.setText("0");
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        int total = enrollments.size();
        int completed = 0;
        int inProgress = 0;

        for (TrainingEnrollment enrollment : enrollments) {
            Training training = trainingService.getTrainingById(enrollment.getTrainingId()).orElse(null);
            if (training == null) continue;

            if (enrollment.isCompleted()) {
                completed++;
            } else {
                inProgress++;
            }

            TLCard card = createProgressCard(training, enrollment);
            trainingsContainer.getChildren().add(card);
        }

        totalEnrollmentsLabel.setText(String.valueOf(total));
        completedLabel.setText(String.valueOf(completed));
        inProgressLabel.setText(String.valueOf(inProgress));
    }

    private TLCard createProgressCard(Training training, TrainingEnrollment enrollment) {
        TLCard card = new TLCard();
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(12);
        VBox titleBox = new VBox(4);
        Label title = new Label(training.getTitle());
        title.getStyleClass().add("h4");
        Label category = new Label(training.getCategory() != null ? training.getCategory().getDisplayName() : "");
        category.getStyleClass().addAll("text-muted", "text-sm");
        titleBox.getChildren().addAll(title, category);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(titleBox, spacer);

        // Progress - Use lesson-based tracking
        int totalLessons = training.getLessonCount();
        int completedLessons = 0;
        int progressPercentage = 0;
        
        if (enrollment.isCompleted()) {
            progressPercentage = 100;
            completedLessons = totalLessons;
        } else if (totalLessons > 0) {
            completedLessons = progressService.getCompletedLessonsCount(enrollment.getId());
            progressPercentage = (completedLessons * 100) / totalLessons;
        }

        VBox progressBox = new VBox(10);
        
        // Show completion checkmark if 100%
        if (enrollment.isCompleted() || progressPercentage >= 100) {
            HBox completionRow = new HBox(8);
            completionRow.setAlignment(Pos.CENTER_LEFT);
            Label checkmark = new Label("✓");
            checkmark.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 18px; -fx-font-weight: bold;");
            Label completedLabel = new Label("Formation complétée");
            completedLabel.getStyleClass().addAll("text-sm", "text-success");
            completionRow.getChildren().addAll(checkmark, completedLabel);
            progressBox.getChildren().add(completionRow);
        } else {
            // Show progress with lesson count
            HBox progressHeader = new HBox(8);
            Label progressLabel = new Label(progressPercentage + "% complété");
            progressLabel.getStyleClass().add("text-sm");
            Label lessonsProgressLabel = new Label(completedLessons + "/" + totalLessons + " leçons complétées");
            lessonsProgressLabel.getStyleClass().addAll("text-muted", "text-sm");
            Region progressSpacer = new Region();
            HBox.setHgrow(progressSpacer, javafx.scene.layout.Priority.ALWAYS);
            progressHeader.getChildren().addAll(progressLabel, progressSpacer, lessonsProgressLabel);

            ProgressBar progressBar = new ProgressBar(progressPercentage / 100.0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(10);
            progressBar.getStyleClass().add("progress-bar");

            progressBox.getChildren().addAll(progressHeader, progressBar);
        }

        // Actions
        HBox actions = new HBox(8);
        TLButton continueBtn = new TLButton("Continuer", TLButton.ButtonVariant.PRIMARY);
        continueBtn.setOnAction(e -> {
            if (onTrainingClick != null) {
                onTrainingClick.accept(training);
            }
        });
        actions.getChildren().add(continueBtn);

        content.getChildren().addAll(header, progressBox, actions);
        card.getContent().add(content);

        return card;
    }
}
