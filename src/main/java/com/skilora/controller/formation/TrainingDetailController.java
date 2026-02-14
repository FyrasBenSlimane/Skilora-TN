package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.formation.TrainingEnrollment;
import com.skilora.model.entity.formation.TrainingLesson;
import com.skilora.service.formation.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class TrainingDetailController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TrainingDetailController.class);

    @FXML private TLButton backBtn;
    @FXML private TLButton progressBtn;
    @FXML private TLBadge categoryBadge;
    @FXML private TLBadge levelBadge;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label durationLabel;
    @FXML private Label costLabel;
    @FXML private Label lessonsCountLabel;
    @FXML private Label progressLabel;
    @FXML private TLButton completeBtn;
    @FXML private VBox lessonsList;
    @FXML private VBox contentArea;
    @FXML private Label lessonTitleLabel;
    @FXML private Label lessonDescriptionLabel;
    @FXML private VBox lessonContentArea;

    private Training training;
    private TrainingEnrollment enrollment;
    private List<TrainingLesson> lessons;
    private TrainingLesson currentLesson;
    private Runnable onBack;
    private Runnable onViewProgress;
    private com.skilora.model.entity.usermanagement.User currentUser;

    private final TrainingService trainingService = TrainingService.getInstance();
    private final TrainingEnrollmentService enrollmentService = TrainingEnrollmentService.getInstance();
    private final TrainingLessonService lessonService = TrainingLessonService.getInstance();
    private final LessonProgressService progressService = LessonProgressService.getInstance();
    
    public void setCurrentUser(com.skilora.model.entity.usermanagement.User user) {
        this.currentUser = user;
    }

    private boolean fxmlInitialized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("TrainingDetailController.initialize() called");
        fxmlInitialized = true;
        
        // Verify all FXML elements are injected
        if (backBtn == null) logger.warn("backBtn is null");
        if (progressBtn == null) logger.warn("progressBtn is null");
        if (titleLabel == null) logger.warn("titleLabel is null");
        if (descriptionLabel == null) logger.warn("descriptionLabel is null");
        if (durationLabel == null) logger.warn("durationLabel is null");
        if (costLabel == null) logger.warn("costLabel is null");
        if (lessonsList == null) logger.warn("lessonsList is null");
        if (contentArea == null) logger.warn("contentArea is null");
        if (lessonContentArea == null) logger.warn("lessonContentArea is null");
        
        // If training was set before FXML initialization, load it now
        if (training != null) {
            logger.debug("Training already set, loading data now");
            Platform.runLater(() -> loadTrainingData());
        } else {
            logger.debug("No training set yet, waiting for setTraining() call");
        }
    }

    public void setTraining(Training training, TrainingEnrollment enrollment) {
        logger.debug("setTraining() called with training: {}, enrollment: {}", 
            training != null ? training.getId() : "null", 
            enrollment != null ? enrollment.getId() : "null");
        
        if (training == null) {
            logger.error("setTraining called with null training!");
            return;
        }
        
        this.training = training;
        this.enrollment = enrollment;
        
        // Always wait a bit for FXML to be fully initialized
        Platform.runLater(() -> {
            // Double-check FXML is initialized, then load data
            if (fxmlInitialized && titleLabel != null) {
                logger.debug("FXML initialized, loading training data");
                loadTrainingData();
            } else {
                logger.debug("Waiting for FXML initialization...");
                // Retry after a short delay
                Platform.runLater(() -> {
                    if (titleLabel != null) {
                        loadTrainingData();
                    } else {
                        logger.error("FXML elements still null after retry!");
                    }
                });
            }
        });
    }

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    public void setOnViewProgress(Runnable callback) {
        this.onViewProgress = callback;
    }

    private void loadTrainingData() {
        if (training == null) {
            logger.warn("loadTrainingData() called but training is null");
            return;
        }
        
        logger.debug("loadTrainingData() called for training ID: {}", training.getId());
        
        // Null safety checks for FXML elements
        if (titleLabel == null || descriptionLabel == null || durationLabel == null) {
            logger.warn("FXML elements not initialized yet, retrying in 100ms...");
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(100); // Small delay to allow FXML initialization
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                loadTrainingData();
            });
            return;
        }
        
        logger.debug("All FXML elements are initialized, proceeding with data loading");

        // Set header info
        titleLabel.setText(training.getTitle() != null ? training.getTitle() : "");
        descriptionLabel.setText(training.getDescription() != null ? training.getDescription() : "");
        durationLabel.setText(training.getFormattedDuration());
        
        // Set cost/price
        if (costLabel != null) {
            if (training.getCost() != null && training.getCost() > 0) {
                costLabel.setText(training.getFormattedCost());
            } else {
                costLabel.setText("Gratuit");
            }
        }
        
        if (categoryBadge != null && training.getCategory() != null) {
            categoryBadge.setText(training.getCategory().getDisplayName());
        }
        if (levelBadge != null && training.getLevel() != null) {
            levelBadge.setText(training.getLevel().getDisplayName());
        }
        
        // Setup action buttons
        setupActionButtons();

        // Check if user is enrolled
        if (enrollment == null) {
            if (lessonTitleLabel != null) {
                lessonTitleLabel.setText("Accès non autorisé");
            }
            if (lessonDescriptionLabel != null) {
                lessonDescriptionLabel.setText("Vous devez être inscrit à cette formation pour accéder au contenu.");
            }
            if (lessonContentArea != null) {
                lessonContentArea.getChildren().clear();
                Label accessDenied = new Label("Veuillez vous inscrire à cette formation pour accéder aux leçons et au matériel.");
                accessDenied.getStyleClass().add("text-muted");
                lessonContentArea.getChildren().add(accessDenied);
            }
            if (lessonsCountLabel != null) {
                lessonsCountLabel.setText("0 leçons");
            }
            if (progressLabel != null) {
                progressLabel.setText("Non inscrit");
            }
            // Still show training details, just restrict content access
            loadLessonsForNonEnrolled();
            return;
        }

        // Load lessons
        loadLessons();
    }
    
    private void loadLessonsForNonEnrolled() {
        // Load lessons count even if not enrolled (for display purposes)
        Thread loadThread = new Thread(() -> {
            try {
                List<TrainingLesson> allLessons = lessonService.getLessonsByTrainingId(training.getId());
                Platform.runLater(() -> {
                    if (lessonsCountLabel != null) {
                        lessonsCountLabel.setText(allLessons.size() + " leçons");
                    }
                });
            } catch (Exception e) {
                logger.error("Error loading lessons count", e);
            }
        }, "LoadLessonsCountThread");
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    private void loadLessons() {
        Thread loadThread = new Thread(() -> {
            try {
                logger.debug("Loading lessons for training ID: {}", training.getId());
                lessons = lessonService.getLessonsByTrainingId(training.getId());
                logger.debug("Loaded {} lessons for training {}", lessons != null ? lessons.size() : 0, training.getId());
                
                Platform.runLater(() -> {
                    try {
                        if (lessonsCountLabel != null) {
                            int lessonCount = training.getLessonCount() > 0 ? training.getLessonCount() : (lessons != null ? lessons.size() : 0);
                            lessonsCountLabel.setText(lessonCount + " leçons");
                        }
                        renderLessons();
                        if (lessons != null && !lessons.isEmpty() && enrollment != null) {
                            selectLesson(lessons.get(0));
                        }
                        updateProgress();
                    } catch (Exception e) {
                        logger.error("Error updating UI with lessons", e);
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Erreur lors de l'affichage des leçons: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Error loading lessons for training {}", training.getId(), e);
                logger.error("Exception details: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    // Don't show error if table doesn't exist - just show 0 lessons
                    if (lessonsCountLabel != null) {
                        lessonsCountLabel.setText("0 leçons");
                    }
                    if (lessonsList != null) {
                        lessonsList.getChildren().clear();
                        Label errorLabel = new Label("Aucune leçon disponible pour le moment.");
                        errorLabel.getStyleClass().add("text-muted");
                        lessonsList.getChildren().add(errorLabel);
                    }
                });
            }
        }, "LoadLessonsThread");
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    private void setupActionButtons() {
        if (completeBtn == null) return;
        
        // Complete button - show if enrolled but not completed
        if (enrollment != null && !enrollment.isCompleted()) {
            completeBtn.setVisible(true);
            completeBtn.setManaged(true);
            completeBtn.setOnAction(e -> handleMarkComplete());
        } else {
            completeBtn.setVisible(false);
            completeBtn.setManaged(false);
        }
        
    }
    
    private void handleMarkComplete() {
        if (enrollment == null || currentUser == null) return;
        
        completeBtn.setDisable(true);
        completeBtn.setText("Marquage en cours...");
        
        Thread completeThread = new Thread(() -> {
            try {
                // First, mark all lessons as 100% complete
                if (lessons != null && !lessons.isEmpty()) {
                    for (TrainingLesson lesson : lessons) {
                        progressService.updateProgress(enrollment.getId(), lesson.getId(), 100);
                        // Mark lesson as completed
                        markLessonCompleted(enrollment.getId(), lesson.getId());
                    }
                }
                
                // Mark enrollment as completed
                enrollmentService.markCompleted(currentUser.getId(), training.getId());
                enrollment.setCompleted(true);
                
                // Reload enrollment from database to get updated state
                enrollment = enrollmentService.getEnrollment(currentUser.getId(), training.getId()).orElse(enrollment);
                
                Platform.runLater(() -> {
                    DialogUtils.showSuccess(I18n.get("common.success"), 
                        "Félicitations ! Vous avez complété la formation.");
                    setupActionButtons(); // Refresh buttons
                    updateProgress(); // This will show 100%
                    // Refresh the view to show updated progress
                    loadTrainingData();
                });
            } catch (Exception e) {
                logger.error("Error marking training as complete", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        "Erreur lors du marquage comme complété: " + e.getMessage());
                    completeBtn.setDisable(false);
                    completeBtn.setText("Marquer comme complété");
                });
            }
        }, "MarkCompleteThread");
        completeThread.setDaemon(true);
        completeThread.start();
    }
    
    private void markLessonCompleted(int enrollmentId, int lessonId) {
        try {
            String sql = "UPDATE lesson_progress SET completed = TRUE, progress_percentage = 100, completed_at = ? " +
                        "WHERE enrollment_id = ? AND lesson_id = ?";
            try (java.sql.Connection conn = com.skilora.config.DatabaseConfig.getInstance().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setInt(2, enrollmentId);
                stmt.setInt(3, lessonId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Error marking lesson as completed", e);
        }
    }
    

    private void renderLessons() {
        lessonsList.getChildren().clear();
        if (lessons == null || lessons.isEmpty()) {
            Label emptyLabel = new Label("Aucune leçon disponible");
            emptyLabel.getStyleClass().add("text-muted");
            lessonsList.getChildren().add(emptyLabel);
            return;
        }

        // Get progress map for all lessons
        Map<Integer, Integer> progressMap = new HashMap<>();
        if (enrollment != null) {
            progressMap = progressService.getProgressByEnrollment(enrollment.getId());
        }

        for (TrainingLesson lesson : lessons) {
            TLCard lessonCard = new TLCard();
            VBox cardContent = new VBox(8);
            cardContent.setPadding(new Insets(12));

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            
            // Checkbox for marking lesson as completed
            javafx.scene.control.CheckBox completedCheckbox = new javafx.scene.control.CheckBox();
            completedCheckbox.setDisable(enrollment == null);
            int lessonProgress = progressMap.getOrDefault(lesson.getId(), 0);
            boolean isCompleted = lessonProgress >= 100;
            completedCheckbox.setSelected(isCompleted);
            
            completedCheckbox.setOnAction(e -> {
                if (enrollment != null && completedCheckbox.isSelected()) {
                    // Mark lesson as completed
                    progressService.markLessonCompleted(enrollment.getId(), lesson.getId());
                    updateProgress();
                    renderLessons(); // Refresh to update all checkboxes
                }
            });
            
            Label lessonNum = new Label(lesson.getOrderIndex() + ".");
            lessonNum.getStyleClass().add("text-muted");
            lessonNum.setMinWidth(30);
            
            VBox titleBox = new VBox(4);
            Label lessonTitle = new Label(lesson.getTitle());
            lessonTitle.getStyleClass().add("text-sm");
            lessonTitle.setWrapText(true);
            if (isCompleted) {
                lessonTitle.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            }
            titleBox.getChildren().add(lessonTitle);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            header.getChildren().addAll(completedCheckbox, lessonNum, titleBox, spacer);

            HBox meta = new HBox(12);
            Label duration = new Label(lesson.getFormattedDuration());
            duration.getStyleClass().addAll("text-xs", "text-muted");
            if (isCompleted) {
                Label completedLabel = new Label("✓ Complétée");
                completedLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                completedLabel.getStyleClass().add("text-xs");
                meta.getChildren().addAll(duration, completedLabel);
            } else {
                meta.getChildren().add(duration);
            }

            cardContent.getChildren().addAll(header, meta);
            lessonCard.getContent().add(cardContent);

            lessonCard.setOnMouseClicked(e -> {
                if (e.getTarget() != completedCheckbox) {
                    selectLesson(lesson);
                }
            });
            if (currentLesson != null && currentLesson.getId() == lesson.getId()) {
                lessonCard.getStyleClass().add("selected");
            }

            lessonsList.getChildren().add(lessonCard);
        }
    }

    private void selectLesson(TrainingLesson lesson) {
        if (enrollment == null) return; // Can't access lessons if not enrolled
        
        this.currentLesson = lesson;
        renderLessons(); // Re-render to update selection

        lessonTitleLabel.setText(lesson.getTitle());
        lessonDescriptionLabel.setText(lesson.getDescription() != null ? lesson.getDescription() : "");

        lessonContentArea.getChildren().clear();
        
        if (lesson.getContent() != null && !lesson.getContent().trim().isEmpty()) {
            WebView webView = new WebView();
            webView.getEngine().loadContent(lesson.getContent(), "text/html");
            webView.setPrefHeight(600);
            lessonContentArea.getChildren().add(webView);
        } else {
            Label noContent = new Label("Le contenu de cette leçon sera bientôt disponible.");
            noContent.getStyleClass().add("text-muted");
            lessonContentArea.getChildren().add(noContent);
        }

        // Update progress
        progressService.updateProgress(enrollment.getId(), lesson.getId(), 100);
        updateProgress();
    }

    private void updateProgress() {
        if (enrollment == null) {
            if (progressLabel != null) {
                progressLabel.setText("Non inscrit");
            }
            return;
        }
        
        int totalLessons = training.getLessonCount();
        int completedLessons = 0;
        int progressPercentage = 0;
        
        // If enrollment is marked as completed, show 100%
        if (enrollment.isCompleted()) {
            progressPercentage = 100;
            completedLessons = totalLessons;
        } else if (totalLessons > 0) {
            completedLessons = progressService.getCompletedLessonsCount(enrollment.getId());
            progressPercentage = (completedLessons * 100) / totalLessons;
        }
        
        if (progressLabel != null) {
            if (progressPercentage >= 100) {
                progressLabel.setText("✓ 100% complété - " + completedLessons + "/" + totalLessons + " leçons");
                progressLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            } else {
                progressLabel.setText(completedLessons + "/" + totalLessons + " leçons complétées (" + progressPercentage + "%)");
                progressLabel.setStyle("");
            }
        }
        
        // Update enrollment completion status if all lessons completed
        if (totalLessons > 0 && completedLessons >= totalLessons && !enrollment.isCompleted()) {
            enrollmentService.markCompleted(enrollment.getUserId(), enrollment.getTrainingId());
            enrollment.setCompleted(true);
        }
    }


    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void handleViewProgress() {
        if (onViewProgress != null) {
            onViewProgress.run();
        }
    }
}
