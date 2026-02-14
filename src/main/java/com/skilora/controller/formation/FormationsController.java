package com.skilora.controller.formation;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.formation.TrainingEnrollment;
import com.skilora.model.entity.formation.TrainingLesson;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.TrainingCategory;
import com.skilora.model.enums.TrainingLevel;
import com.skilora.service.formation.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

/**
 * FormationsController - Training/Courses view for job seekers.
 * Displays available formations and learning resources.
 */
public class FormationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FormationsController.class);

    @FXML private Label statsLabel;
    @FXML private HBox categoryBox;
    @FXML private TLTextField searchField;
    @FXML private FlowPane formationsGrid;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private TrainingService trainingService;
    private TrainingEnrollmentService enrollmentService;
    private LessonProgressService progressService;
    private TrainingLessonService lessonService;
    private User currentUser;
    private List<Training> allTrainings;
    private ToggleGroup categoryGroup;
    private TrainingCategory currentCategory = null; // null means "ALL"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.trainingService = TrainingService.getInstance();
        this.enrollmentService = TrainingEnrollmentService.getInstance();
        this.progressService = LessonProgressService.getInstance();
        this.lessonService = TrainingLessonService.getInstance();
        setupCategories();
        setupSearch();
        loadFormations();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void setupCategories() {
        categoryGroup = new ToggleGroup();
        
        // Add "All" button
        ToggleButton allBtn = new ToggleButton(I18n.get("formations.filter.all"));
        allBtn.setUserData(null); // null = all categories
        allBtn.getStyleClass().add("chip-filter");
        allBtn.setToggleGroup(categoryGroup);
        allBtn.setSelected(true);
        allBtn.setOnAction(e -> {
            if (allBtn.isSelected()) {
                currentCategory = null;
                applyFilters();
            } else if (categoryGroup.getSelectedToggle() == null) {
                allBtn.setSelected(true);
            }
        });
        categoryBox.getChildren().add(allBtn);

        // Add category buttons
        for (TrainingCategory category : TrainingCategory.values()) {
            ToggleButton btn = new ToggleButton(category.getDisplayName());
            btn.setUserData(category);
            btn.getStyleClass().add("chip-filter");
            btn.setToggleGroup(categoryGroup);
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    currentCategory = category;
                    applyFilters();
                } else if (categoryGroup.getSelectedToggle() == null) {
                    allBtn.setSelected(true);
                }
            });
            categoryBox.getChildren().add(btn);
        }
    }

    private void setupSearch() {
        if (searchField != null && searchField.getControl() != null) {
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> applyFilters());
            searchField.getControl().setOnKeyReleased(e -> pause.playFromStart());
        }
    }

    private void loadFormations() {
        statsLabel.setText("Chargement...");
        formationsGrid.getChildren().clear();

        Task<List<Training>> loadTask = new Task<>() {
            @Override
            protected List<Training> call() throws Exception {
                return trainingService.getAllTrainings();
            }
        };

        loadTask.setOnSucceeded(e -> {
            allTrainings = loadTask.getValue();
            applyFilters();
        });

        loadTask.setOnFailed(e -> {
            logger.error("Error loading trainings", loadTask.getException());
            statsLabel.setText("Erreur lors du chargement");
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        });

        new Thread(loadTask).start();
    }

    private void applyFilters() {
        if (allTrainings == null) return;

        String query = searchField != null && searchField.getControl() != null 
            ? searchField.getText() : "";
        
        List<Training> filtered;
        
        if (currentCategory == null) {
            // Search all categories
            if (query == null || query.trim().isEmpty()) {
                filtered = allTrainings;
            } else {
                filtered = trainingService.searchTrainings(query);
            }
        } else {
            // Filter by category and optionally search
            filtered = trainingService.searchTrainingsByCategoryAndKeyword(currentCategory, query);
        }

        renderFormations(filtered);
    }

    private void renderFormations(List<Training> trainings) {
        formationsGrid.getChildren().clear();

        if (trainings.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("formations.not_found"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        statsLabel.setText(I18n.get("formations.count", trainings.size()));

        for (Training training : trainings) {
            formationsGrid.getChildren().add(createFormationCard(training));
        }
    }

    private TLCard createFormationCard(Training training) {
        TLCard card = new TLCard();
        card.setPrefWidth(320);
        card.setMinWidth(280);
        card.setMaxWidth(360);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        // Category badge + Level
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        TLBadge catBadge = new TLBadge(
            training.getCategory() != null ? training.getCategory().getDisplayName() : "", 
            TLBadge.Variant.DEFAULT);
        TLBadge levelBadge = new TLBadge(
            training.getLevel() != null ? training.getLevel().getDisplayName() : "", 
            TLBadge.Variant.SECONDARY);
        if (training.getLevel() == TrainingLevel.BEGINNER) {
            levelBadge.getStyleClass().add("badge-success");
        } else if (training.getLevel() == TrainingLevel.ADVANCED) {
            levelBadge.getStyleClass().add("badge-destructive");
        }
        badgeRow.getChildren().addAll(catBadge, levelBadge);

        // Title - make it clickable if enrolled
        Label titleLabel = new Label(training.getTitle() != null ? training.getTitle() : "");
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);
        boolean isEnrolled = currentUser != null && enrollmentService.isEnrolled(currentUser.getId(), training.getId());
        if (isEnrolled) {
            titleLabel.setStyle("-fx-cursor: hand; -fx-text-fill: -fx-primary;");
            titleLabel.setOnMouseClicked(e -> {
                if (onShowTrainingDetail != null) {
                    onShowTrainingDetail.accept(training);
                }
            });
        }

        // Description
        Label descLabel = new Label(training.getDescription() != null ? training.getDescription() : "");
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);

        // Duration and Lesson Count
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label durationLabel = new Label("⏱ " + training.getFormattedDuration());
        durationLabel.getStyleClass().add("text-muted");
        Label lessonCountLabel = new Label("📚 " + training.getFormattedLessonCount());
        lessonCountLabel.getStyleClass().add("text-muted");
        metaRow.getChildren().addAll(durationLabel, lessonCountLabel);

        // Add all elements to content VBox in correct order
        content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow);

        // Progress indicator (if enrolled) - Show lesson-based progress
        if (isEnrolled && currentUser != null) {
            try {
                java.util.Optional<TrainingEnrollment> enrollmentOpt = 
                    enrollmentService.getEnrollment(currentUser.getId(), training.getId());
                if (enrollmentOpt.isPresent()) {
                    TrainingEnrollment enrollment = enrollmentOpt.get();
                    int totalLessons = training.getLessonCount();
                    int completedLessons = 0;
                    int progressPercentage = 0;
                    
                    if (totalLessons > 0) {
                        completedLessons = progressService.getCompletedLessonsCount(enrollment.getId());
                        progressPercentage = (completedLessons * 100) / totalLessons;
                    }
                    
                    // Show completion checkmark if 100%
                    if (enrollment.isCompleted() || progressPercentage >= 100) {
                        HBox completionRow = new HBox(8);
                        completionRow.setAlignment(Pos.CENTER_LEFT);
                        Label checkmark = new Label("✓");
                        checkmark.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 16px; -fx-font-weight: bold;");
                        Label completedLabel = new Label("Formation complétée");
                        completedLabel.getStyleClass().addAll("text-sm", "text-success");
                        completionRow.getChildren().addAll(checkmark, completedLabel);
                        content.getChildren().add(completionRow);
                    } else {
                        // Show progress with lesson count
                        VBox progressBox = new VBox(6);
                        HBox progressRow = new HBox(8);
                        progressRow.setAlignment(Pos.CENTER_LEFT);
                        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(progressPercentage / 100.0);
                        progressBar.setPrefWidth(120);
                        progressBar.setPrefHeight(8);
                        progressBar.getStyleClass().add("progress-bar");
                        Label progressLabel = new Label(progressPercentage + "%");
                        progressLabel.getStyleClass().add("text-xs");
                        progressRow.getChildren().addAll(progressBar, progressLabel);
                        
                        Label lessonProgressLabel = new Label(completedLessons + "/" + totalLessons + " leçons complétées");
                        lessonProgressLabel.getStyleClass().addAll("text-xs", "text-muted");
                        progressBox.getChildren().addAll(progressRow, lessonProgressLabel);
                        content.getChildren().add(progressBox);
                    }
                }
            } catch (Exception e) {
                logger.error("Error loading progress for training card", e);
            }
        }
        
        // Enroll button
        Separator sep = new Separator();
        isEnrolled = currentUser != null && enrollmentService.isEnrolled(currentUser.getId(), training.getId());
        TLButton enrollBtn = new TLButton(
            isEnrolled ? I18n.get("formations.enrolled") : 
            (training.isFree() ? I18n.get("formations.enroll.free") : 
            I18n.get("formations.enroll.paid", training.getFormattedCost())), 
            isEnrolled ? TLButton.ButtonVariant.SECONDARY : TLButton.ButtonVariant.PRIMARY);
        enrollBtn.setMaxWidth(Double.MAX_VALUE);
        enrollBtn.setDisable(isEnrolled);
        enrollBtn.setOnAction(e -> handleEnroll(training, enrollBtn));
        content.getChildren().addAll(sep, enrollBtn);

        card.getContent().add(content);
        return card;
    }

    @FXML
    private void handleRefresh() {
        loadFormations();
    }

    private void handleEnroll(Training training, TLButton enrollBtn) {
        if (currentUser == null) {
            DialogUtils.showError(I18n.get("common.error"), I18n.get("formations.enroll.login_required"));
            return;
        }

        enrollBtn.setDisable(true);
        enrollBtn.setText(I18n.get("formations.enrolling"));

                Thread enrollThread = new Thread(() -> {
            try {
                enrollmentService.enrollUser(currentUser.getId(), training.getId());
                logger.info("User {} successfully enrolled in training {}", currentUser.getId(), training.getId());
                Platform.runLater(() -> {
                    DialogUtils.showSuccess(I18n.get("common.success"), 
                        I18n.get("formations.enroll.success", training.getTitle()));
                    enrollBtn.setText(I18n.get("formations.enrolled"));
                    enrollBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
                    enrollBtn.setDisable(true);
                    
                    // Refresh the formations view to update card state (show progress indicator)
                    applyFilters();
                    
                    // Refresh "Mes Formations" view if callback is set
                    if (onEnrollmentSuccess != null) {
                        onEnrollmentSuccess.run();
                    }
                    
                    // Open training detail view
                    if (onShowTrainingDetail != null) {
                        onShowTrainingDetail.accept(training);
                    }
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), e.getMessage());
                    enrollBtn.setDisable(false);
                    enrollBtn.setText(training.isFree() ? I18n.get("formations.enroll.free") : 
                        I18n.get("formations.enroll.paid", training.getFormattedCost()));
                });
            } catch (Exception e) {
                logger.error("Error enrolling in training", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        I18n.get("formations.enroll.error", e.getMessage()));
                    enrollBtn.setDisable(false);
                    enrollBtn.setText(training.isFree() ? I18n.get("formations.enroll.free") : 
                        I18n.get("formations.enroll.paid", training.getFormattedCost()));
                });
            }
        }, "EnrollThread");
        enrollThread.setDaemon(true);
        enrollThread.start();
    }

    private java.util.function.Consumer<Training> onShowTrainingDetail;
    private Runnable onEnrollmentSuccess;

    public void setOnShowTrainingDetail(java.util.function.Consumer<Training> callback) {
        this.onShowTrainingDetail = callback;
    }

    public void setOnEnrollmentSuccess(Runnable callback) {
        this.onEnrollmentSuccess = callback;
    }

    private void showTrainingDetail(Training training) {
        if (onShowTrainingDetail != null) {
            onShowTrainingDetail.accept(training);
        }
    }
}
