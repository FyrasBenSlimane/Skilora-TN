package com.skilora.formation.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLProgress;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLTabs;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.FormationRatingPanel;
import com.skilora.framework.components.ChatbotWidget;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.entity.FormationModule;
import com.skilora.formation.entity.Enrollment;
import com.skilora.formation.entity.Achievement;
import com.skilora.formation.entity.Certificate;
import com.skilora.user.entity.User;
import com.skilora.formation.service.FormationService;
import com.skilora.formation.service.FormationModuleService;
import com.skilora.formation.service.EnrollmentService;
import com.skilora.formation.service.AchievementService;
import com.skilora.formation.service.CertificateGenerationService;
import com.skilora.formation.service.CertificateService;
import com.skilora.formation.service.LessonProgressService;
import com.skilora.formation.enums.FormationLevel;
import com.skilora.utils.UiUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.finance.entity.BankAccount;
import com.skilora.finance.entity.PaymentTransaction;
import com.skilora.finance.service.BankAccountService;
import com.skilora.finance.service.PaymentTransactionService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import java.math.BigDecimal;

/**
 * FormationsController - Training/Courses view for job seekers.
 * Displays available formations, certifications, and learning resources.
 * Database-backed with full CRUD operations.
 */
public class FormationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FormationsController.class);

    private final FormationService formationService = FormationService.getInstance();
    private final EnrollmentService enrollmentService = EnrollmentService.getInstance();
    private final FormationModuleService moduleService = FormationModuleService.getInstance();
    private final CertificateService certificateService = CertificateService.getInstance();
    private final AchievementService achievementService = AchievementService.getInstance();
    private final CertificateGenerationService certificateGenerationService = CertificateGenerationService.getInstance();
    private final LessonProgressService lessonProgressService = LessonProgressService.getInstance();
    
    private User currentUser;

    // Callbacks for navigation integration
    @SuppressWarnings("unused")
    private java.util.function.Consumer<Formation> onShowFormationDetail;
    @SuppressWarnings("unused")
    private Runnable onEnrollmentSuccess;

    @FXML private Label titleLabel;
    @FXML private Label statsLabel;
    @FXML private HBox categoryBox;
    @FXML private TLTextField searchField;
    @FXML private FlowPane formationsGrid;
    @FXML private VBox emptyState;
    @FXML private VBox gridContainer;
    @FXML private VBox courseDetailContainer;
    @FXML private TLButton refreshBtn;
    @FXML private TLButton certificatesBtn;
    @FXML private StackPane chatbotContainer;

    private List<Formation> allFormations;
    private Map<Integer, Enrollment> userEnrollments = new HashMap<>();
    private String currentCategory = "ALL";

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            loadUserEnrollments();
            // Re-load formations if they haven't loaded yet or to refresh data
            if (allFormations == null) {
                loadFormations();
            }
        }
    }

    /**
     * Set callback to navigate to formation detail view when a formation is clicked.
     */
    public void setOnShowFormationDetail(java.util.function.Consumer<Formation> callback) {
        this.onShowFormationDetail = callback;
    }

    /**
     * Set callback to refresh external views after successful enrollment.
     */
    public void setOnEnrollmentSuccess(Runnable callback) {
        this.onEnrollmentSuccess = callback;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        setupCategories();
        setupSearch();
        loadFormations();
    }

    private void applyI18n() {
        if (titleLabel != null) titleLabel.setText(I18n.get("formations.title"));
        if (refreshBtn != null) refreshBtn.setText(I18n.get("common.refresh"));
        if (certificatesBtn != null) certificatesBtn.setText(I18n.get("formations.certificates"));
        if (searchField != null) searchField.setPromptText(I18n.get("formations.search"));
    }

    private void loadUserEnrollments() {
        if (currentUser == null) return;

        Task<List<Enrollment>> task = new Task<>() {
            @Override
            protected List<Enrollment> call() throws Exception {
                return enrollmentService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Enrollment> enrollments = task.getValue();
            userEnrollments.clear();
            for (Enrollment enrollment : enrollments) {
                userEnrollments.put(enrollment.getFormationId(), enrollment);
            }
            applyFilters();
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load user enrollments", task.getException());
            TLToast.error(formationsGrid.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_enrollments"));
        });
        AppThreadPool.execute(task);
    }

    private void setupCategories() {
        String[][] categories = {
            {I18n.get("formations.filter.all"), "ALL"},
            {I18n.get("formations.filter.development"), "Développement"},
            {I18n.get("formations.filter.design"), "Design"},
            {I18n.get("formations.filter.marketing"), "Marketing"},
            {I18n.get("formations.filter.data_science"), "Data Science"},
            {I18n.get("formations.filter.languages"), "Langues"},
            {I18n.get("formations.filter.soft_skills"), "Soft Skills"}
        };

        TLTabs tabs = new TLTabs();
        for (String[] cat : categories) {
            tabs.addTab(cat[1], cat[0], (javafx.scene.Node) null);
        }
        tabs.setOnTabChanged(tabId -> {
            currentCategory = tabId;
            applyFilters();
        });
        categoryBox.getChildren().add(tabs);
    }

    private void setupSearch() {
        if (searchField != null && searchField.getControl() != null) {
            UiUtils.debounce(searchField.getControl(), 300, this::applyFilters);
        }
    }

    private boolean chatbotInitialized = false;

    /**
     * Initialize the floating chatbot widget once formations are loaded.
     */
    private void initChatbotWidget() {
        if (chatbotInitialized || chatbotContainer == null || allFormations == null) return;
        chatbotInitialized = true;
        try {
            ChatbotWidget chatbot = new ChatbotWidget(allFormations);
            chatbotContainer.getChildren().add(chatbot);
            StackPane.setAlignment(chatbot, Pos.BOTTOM_RIGHT);
        } catch (Exception ex) {
            logger.warn("Chatbot widget initialization skipped: {}", ex.getMessage());
        }
    }

    private void loadFormations() {
        formationsGrid.getChildren().clear();
        formationsGrid.getChildren().add(new TLLoadingState());
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        Task<List<Formation>> task = new Task<>() {
            @Override
            protected List<Formation> call() throws Exception {
                return formationService.findAll();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            allFormations = task.getValue();
            applyFilters();
            logger.info("Loaded {} formations from database", allFormations.size());
            initChatbotWidget();
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load formations", task.getException());
            Platform.runLater(() -> {
                allFormations = new ArrayList<>();
                applyFilters();
            });
        });

        AppThreadPool.execute(task);
    }

    private void applyFilters() {
        if (allFormations == null) return;

        String query = searchField != null ? searchField.getText() : "";
        String lowerQuery = query == null ? "" : query.toLowerCase();

        List<Formation> filtered = allFormations.stream()
            .filter(f -> {
                if (!"ALL".equals(currentCategory) && !f.getCategory().equals(currentCategory)) {
                    return false;
                }
                if (!lowerQuery.isEmpty()) {
                    String title = f.getTitle() != null ? f.getTitle().toLowerCase() : "";
                    String provider = f.getProvider() != null ? f.getProvider().toLowerCase() : "";
                    String desc = f.getDescription() != null ? f.getDescription().toLowerCase() : "";
                    return title.contains(lowerQuery) || provider.contains(lowerQuery) || desc.contains(lowerQuery);
                }
                return true;
            })
            .collect(Collectors.toList());

        renderFormations(filtered);
    }

    private void renderFormations(List<Formation> formations) {
        formationsGrid.getChildren().clear();

        if (formations.isEmpty()) {
            emptyState.getChildren().clear();
            emptyState.getChildren().add(new TLEmptyState(
                SvgIcons.GRADUATION_CAP,
                I18n.get("formations.empty.title"),
                I18n.get("formations.empty.subtitle")));
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("formations.not_found"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        statsLabel.setText(I18n.get("formations.count", formations.size()));

        for (Formation f : formations) {
            formationsGrid.getChildren().add(createFormationCard(f));
        }
    }

    private TLCard createFormationCard(Formation formation) {
        TLCard card = new TLCard();
        card.setPrefWidth(320);
        card.setMinWidth(280);
        card.setMaxWidth(360);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        // Category badge + Level
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        TLBadge catBadge = new TLBadge(formation.getCategory(), TLBadge.Variant.DEFAULT);
        
        String levelKey = "formations.level." + (formation.getLevel() != null ? formation.getLevel().name().toLowerCase() : "beginner");
        TLBadge levelBadge = new TLBadge(I18n.get(levelKey), TLBadge.Variant.SECONDARY);
        
        if (formation.getLevel() == FormationLevel.BEGINNER) {
            levelBadge.getStyleClass().add("badge-success");
        } else if (formation.getLevel() == FormationLevel.ADVANCED) {
            levelBadge.getStyleClass().add("badge-destructive");
        }
        badgeRow.getChildren().addAll(catBadge, levelBadge);

        // Title
        Label titleLabel = new Label(formation.getTitle());
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(formation.getDescription() != null ? formation.getDescription() : "");
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);

        // Provider + Duration
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label providerLabel = new Label(formation.getProvider() != null ? formation.getProvider() : "");
        providerLabel.setGraphic(SvgIcons.icon(SvgIcons.GRADUATION_CAP, 14, "-fx-muted-foreground"));
        providerLabel.getStyleClass().add("text-muted");
        Label durationLabel = new Label(I18n.get("formations.duration", formation.getDurationHours()));
        durationLabel.setGraphic(SvgIcons.icon(SvgIcons.TIMER, 14, "-fx-muted-foreground"));
        durationLabel.getStyleClass().add("text-muted");
        metaRow.getChildren().addAll(providerLabel, durationLabel);

        // Check if user is enrolled
        Enrollment enrollment = userEnrollments.get(formation.getId());
        
        if (enrollment != null) {
            // Progress bar (if enrolled)
            VBox progressBox = new VBox(4);
            Label progLabel = new Label(I18n.get("formations.completion", String.format("%.0f", enrollment.getProgress())));
            progLabel.getStyleClass().add("text-muted");
            TLProgress progressBar = new TLProgress(enrollment.getProgress() / 100.0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(6);
            progressBox.getChildren().addAll(progLabel, progressBar);
            
            TLButton continueBtn = new TLButton(I18n.get("formations.continue"), TLButton.ButtonVariant.SECONDARY);
            continueBtn.setMaxWidth(Double.MAX_VALUE);
            continueBtn.setOnAction(e -> showCourseDetail(formation, enrollment));
            
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, new TLSeparator(), progressBox, continueBtn);

            // View Certificate button (when completed)
            if (enrollment.getProgress() >= 100.0) {
                TLButton certBtn = new TLButton(I18n.get("certificate.view"), TLButton.ButtonVariant.PRIMARY);
                certBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 16));
                certBtn.setMaxWidth(Double.MAX_VALUE);
                certBtn.setOnAction(e -> {
                    AppThreadPool.execute(() -> {
                        Certificate cert = certificateService.findByEnrollment(enrollment.getId());
                        Platform.runLater(() -> {
                            if (cert != null && formationsGrid.getScene() != null) {
                                javafx.stage.Stage stage = (javafx.stage.Stage) formationsGrid.getScene().getWindow();
                                String userName = currentUser != null ? currentUser.getFullName() : "";
                                CertificateViewController.show(stage, cert, userName, formation.getTitle());
                            } else {
                                TLToast.info(formationsGrid.getScene(), I18n.get("certificate.title"), I18n.get("formations.certificates.not_found"));
                            }
                        });
                    });
                });
                content.getChildren().add(certBtn);
            }
        } else {
            // Enroll button
            TLSeparator sep = new TLSeparator();
            String enrollText;
            if (formation.isFree()) {
                enrollText = I18n.get("formations.enroll.free");
            } else {
                enrollText = I18n.get("formations.enroll.paid", formation.getCost() + " " + formation.getCurrency());
            }
            
            TLButton enrollBtn = new TLButton(enrollText, TLButton.ButtonVariant.PRIMARY);
            enrollBtn.setMaxWidth(Double.MAX_VALUE);
            enrollBtn.setOnAction(e -> {
                enrollBtn.setDisable(true);
                enrollBtn.setText(I18n.get("common.enrolling"));
                handleEnroll(formation);
            });

            TLButton detailsBtn = new TLButton(I18n.get("formations.view_details"), TLButton.ButtonVariant.OUTLINE);
            detailsBtn.setMaxWidth(Double.MAX_VALUE);
            detailsBtn.setOnAction(e -> showCourseDetail(formation, null));
            
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, sep, detailsBtn, enrollBtn);
        }

        card.getContent().add(content);
        return card;
    }

    /**
     * Shows an inline course detail section replacing the formations grid.
     * Displays formation info, progress (if enrolled), and all modules/chapters.
     */
    private void showCourseDetail(Formation formation, Enrollment enrollment) {
        // Hide grid, show detail
        gridContainer.setVisible(false);
        gridContainer.setManaged(false);
        categoryBox.setVisible(false);
        categoryBox.setManaged(false);
        if (searchField != null) {
            searchField.setVisible(false);
            searchField.setManaged(false);
        }
        courseDetailContainer.getChildren().clear();
        courseDetailContainer.setVisible(true);
        courseDetailContainer.setManaged(true);

        // ── Back button row ──
        HBox backRow = new HBox(12);
        backRow.setAlignment(Pos.CENTER_LEFT);
        TLButton backBtn = new TLButton(I18n.get("common.back"), TLButton.ButtonVariant.OUTLINE);
        backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        backBtn.setOnAction(e -> hideCourseDetail());
        backRow.getChildren().add(backBtn);

        // ── Header section ──
        VBox headerSection = new VBox(12);
        headerSection.setPadding(new Insets(0, 0, 8, 0));

        Label courseTitle = new Label(formation.getTitle());
        courseTitle.getStyleClass().add("h2");
        courseTitle.setWrapText(true);

        Label courseDesc = new Label(formation.getDescription() != null ? formation.getDescription() : "");
        courseDesc.getStyleClass().add("text-muted");
        courseDesc.setWrapText(true);

        // Badges
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        TLBadge catBadge = new TLBadge(formation.getCategory(), TLBadge.Variant.DEFAULT);
        String levelKey = "formations.level." + (formation.getLevel() != null ? formation.getLevel().name().toLowerCase() : "beginner");
        TLBadge levelBadge = new TLBadge(I18n.get(levelKey), TLBadge.Variant.SECONDARY);
        if (formation.getLevel() == FormationLevel.BEGINNER) levelBadge.getStyleClass().add("badge-success");
        else if (formation.getLevel() == FormationLevel.ADVANCED) levelBadge.getStyleClass().add("badge-destructive");

        if (!formation.isFree()) {
            TLBadge priceBadge = new TLBadge(formation.getCost() + " " + formation.getCurrency(), TLBadge.Variant.OUTLINE);
            badgeRow.getChildren().addAll(catBadge, levelBadge, priceBadge);
        } else {
            TLBadge freeBadge = new TLBadge(I18n.get("formations.free"), TLBadge.Variant.DEFAULT);
            freeBadge.getStyleClass().add("badge-success");
            badgeRow.getChildren().addAll(catBadge, levelBadge, freeBadge);
        }

        // Meta row
        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (formation.getProvider() != null && !formation.getProvider().isBlank()) {
            Label provLbl = new Label(formation.getProvider());
            provLbl.setGraphic(SvgIcons.icon(SvgIcons.GRADUATION_CAP, 14, "-fx-muted-foreground"));
            provLbl.getStyleClass().add("text-muted");
            metaRow.getChildren().add(provLbl);
        }
        Label durLbl = new Label(I18n.get("formations.duration", formation.getDurationHours()));
        durLbl.setGraphic(SvgIcons.icon(SvgIcons.TIMER, 14, "-fx-muted-foreground"));
        durLbl.getStyleClass().add("text-muted");
        metaRow.getChildren().add(durLbl);

        headerSection.getChildren().addAll(courseTitle, badgeRow, courseDesc, metaRow);

        courseDetailContainer.getChildren().addAll(backRow, headerSection);

        // ── Progress section (if enrolled) ──
        if (enrollment != null) {
            TLCard progressCard = new TLCard();
            VBox progressBox = new VBox(12);
            progressBox.setPadding(new Insets(20));

            Label progTitle = new Label(I18n.get("formations.detail.your_progress"));
            progTitle.getStyleClass().add("h3");

            // Overall progress bar
            double progressVal = enrollment.getProgress();
            boolean isCompleted = progressVal >= 100.0;

            Label progLabel;
            if (isCompleted) {
                progLabel = new Label("\u2713 " + I18n.get("progress.formation_completed"));
                progLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: 600;");
            } else {
                progLabel = new Label(I18n.get("formations.completion", String.format("%.0f", progressVal)));
                progLabel.getStyleClass().add("text-muted");
            }

            TLProgress progressBar = new TLProgress(progressVal / 100.0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(10);

            // Stats row — populated after modules load
            HBox statsRow = new HBox(12);
            statsRow.setAlignment(Pos.CENTER_LEFT);
            Label totalModulesLabel = new Label("...");
            totalModulesLabel.getStyleClass().add("text-muted");
            totalModulesLabel.setGraphic(SvgIcons.icon(SvgIcons.BOOK_OPEN, 14, "-fx-muted-foreground"));
            Label completedModulesLabel = new Label("...");
            completedModulesLabel.getStyleClass().add("text-muted");
            completedModulesLabel.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 14, "#22c55e"));
            Label remainingModulesLabel = new Label("...");
            remainingModulesLabel.getStyleClass().add("text-muted");
            remainingModulesLabel.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 14, "-fx-muted-foreground"));
            statsRow.getChildren().addAll(totalModulesLabel, completedModulesLabel, remainingModulesLabel);

            // Store refs for async update of stats
            final Label[] statRefs = {totalModulesLabel, completedModulesLabel, remainingModulesLabel};

            progressBox.getChildren().addAll(progTitle, progLabel, progressBar, statsRow);

            // Certificate button if 100%
            if (isCompleted) {
                HBox certActions = new HBox(8);
                certActions.setAlignment(Pos.CENTER_LEFT);
                TLButton certBtn = new TLButton(I18n.get("certificate.view"), TLButton.ButtonVariant.PRIMARY);
                certBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 16));
                certBtn.setOnAction(e -> {
                    AppThreadPool.execute(() -> {
                        Certificate cert = certificateService.findByEnrollment(enrollment.getId());
                        Platform.runLater(() -> {
                            if (cert != null && formationsGrid.getScene() != null) {
                                javafx.stage.Stage stage = (javafx.stage.Stage) formationsGrid.getScene().getWindow();
                                String userName = currentUser != null ? currentUser.getFullName() : "";
                                CertificateViewController.show(stage, cert, userName, formation.getTitle());
                            }
                        });
                    });
                });

                TLButton quizBtnProgress = new TLButton(I18n.get("quiz.title"), TLButton.ButtonVariant.OUTLINE);
                quizBtnProgress.setGraphic(SvgIcons.icon(SvgIcons.LIGHTBULB, 14));
                quizBtnProgress.setOnAction(ev -> {
                    if (currentUser != null)
                        QuizController.showQuizDialog(formation.getId(), currentUser.getId(), formationsGrid.getScene());
                });

                certActions.getChildren().addAll(certBtn, quizBtnProgress);
                progressBox.getChildren().add(certActions);
            }
            progressCard.getContent().add(progressBox);
            courseDetailContainer.getChildren().add(progressCard);

            // Resolve module stats async
            AppThreadPool.execute(() -> {
                int totalMods = moduleService.countByFormation(formation.getId());
                int completedMods = lessonProgressService.getCompletedLessonsCount(enrollment.getId());
                int remaining = Math.max(0, totalMods - completedMods);
                Platform.runLater(() -> {
                    statRefs[0].setText(I18n.get("progress.stat.total") + ": " + totalMods);
                    statRefs[1].setText(I18n.get("progress.stat.completed") + ": " + completedMods);
                    statRefs[2].setText(I18n.get("progress.stat.remaining") + ": " + remaining);
                });
            });
        } else {
            // Not enrolled — show enroll button
            TLCard enrollCard = new TLCard();
            VBox enrollBox = new VBox(8);
            enrollBox.setPadding(new Insets(16));
            Label enrollTitle = new Label(I18n.get("formations.detail.enroll_prompt"));
            enrollTitle.getStyleClass().add("h4");
            enrollTitle.setWrapText(true);

            String enrollText;
            if (formation.isFree()) {
                enrollText = I18n.get("formations.enroll.free");
            } else {
                enrollText = I18n.get("formations.enroll.paid", formation.getCost() + " " + formation.getCurrency());
            }
            TLButton enrollBtn = new TLButton(enrollText, TLButton.ButtonVariant.PRIMARY);
            enrollBtn.setMaxWidth(300);
            enrollBtn.setOnAction(e -> {
                enrollBtn.setDisable(true);
                enrollBtn.setText(I18n.get("common.enrolling"));
                handleEnroll(formation);
            });
            enrollBox.getChildren().addAll(enrollTitle, enrollBtn);
            enrollCard.getContent().add(enrollBox);
            courseDetailContainer.getChildren().add(enrollCard);
        }

        // ── Modules/Chapters section ──
        courseDetailContainer.getChildren().add(new TLSeparator());

        Label modulesHeader = new Label(I18n.get("formations.detail.course_content"));
        modulesHeader.getStyleClass().add("h3");
        courseDetailContainer.getChildren().add(modulesHeader);

        // Loading state
        Label loadingLabel = new Label(I18n.get("formations.detail.loading"));
        loadingLabel.getStyleClass().add("text-muted");
        courseDetailContainer.getChildren().add(loadingLabel);

        // Load modules in background
        Task<List<FormationModule>> task = new Task<>() {
            @Override
            protected List<FormationModule> call() throws Exception {
                return moduleService.findByFormation(formation.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            courseDetailContainer.getChildren().remove(loadingLabel);
            List<FormationModule> modules = task.getValue();

            if (modules.isEmpty()) {
                Label emptyLabel = new Label(I18n.get("formations.detail.no_modules"));
                emptyLabel.getStyleClass().add("text-muted");
                courseDetailContainer.getChildren().add(emptyLabel);
                return;
            }

            // Load per-module progress if enrolled
            Map<Integer, Integer> moduleProgress = new HashMap<>();
            if (enrollment != null) {
                moduleProgress.putAll(lessonProgressService.getProgressByEnrollment(enrollment.getId()));
            }

            Label countLabel = new Label(I18n.get("formations.detail.modules_title", modules.size()));
            countLabel.getStyleClass().add("text-muted");
            courseDetailContainer.getChildren().add(countLabel);

            for (int i = 0; i < modules.size(); i++) {
                FormationModule mod = modules.get(i);
                boolean isCompleted = moduleProgress.getOrDefault(mod.getId(), 0) >= 100;

                courseDetailContainer.getChildren().add(
                        buildModuleRow(formation, enrollment, modules, i, mod, isCompleted));
            }

            // Quiz button at the bottom
            if (enrollment != null) {
                TLSeparator quizSep = new TLSeparator();
                TLButton quizBtn = new TLButton(I18n.get("quiz.title"), TLButton.ButtonVariant.OUTLINE);
                quizBtn.setGraphic(SvgIcons.icon(SvgIcons.LIGHTBULB, 14));
                quizBtn.setOnAction(ev -> {
                    if (currentUser == null) return;
                    QuizController.showQuizDialog(formation.getId(), currentUser.getId(), formationsGrid.getScene());
                });
                courseDetailContainer.getChildren().addAll(quizSep, quizBtn);

                // Rating panel for completed formations
                if (enrollment.getProgress() >= 100.0 && currentUser != null) {
                    TLSeparator ratingSep = new TLSeparator();
                    Label ratingHeader = new Label(I18n.get("formations.detail.rate_course"));
                    ratingHeader.getStyleClass().add("h3");
                    FormationRatingPanel ratingPanel = new FormationRatingPanel(
                            currentUser.getId(), formation.getId());
                    ratingPanel.setMaxWidth(Double.MAX_VALUE);
                    courseDetailContainer.getChildren().addAll(ratingSep, ratingHeader, ratingPanel);
                }
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            courseDetailContainer.getChildren().remove(loadingLabel);
            Label errorLabel = new Label(I18n.get("formations.detail.load_error"));
            errorLabel.getStyleClass().add("text-destructive");
            courseDetailContainer.getChildren().add(errorLabel);
        }));

        AppThreadPool.execute(task);
    }

    /**
     * Builds a single module/chapter row card for the inline course detail view.
     */
    private TLCard buildModuleRow(Formation formation, Enrollment enrollment,
                                   List<FormationModule> modules, int index,
                                   FormationModule mod, boolean isCompleted) {
        TLCard moduleCard = new TLCard();
        moduleCard.setMaxWidth(Double.MAX_VALUE);
        VBox cardContent = new VBox(8);
        cardContent.setPadding(new Insets(14));

        // Top row: index + title + status
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label(String.valueOf(index + 1));
        indexLabel.getStyleClass().add("h4");
        indexLabel.setStyle("-fx-min-width: 32; -fx-alignment: center;");

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label modTitle = new Label(mod.getTitle());
        modTitle.getStyleClass().add("h4");
        modTitle.setWrapText(true);

        HBox modMeta = new HBox(12);
        modMeta.setAlignment(Pos.CENTER_LEFT);
        Label modDuration = new Label(mod.getFormattedDuration());
        modDuration.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 12, "-fx-muted-foreground"));
        modDuration.getStyleClass().add("text-muted");
        modMeta.getChildren().add(modDuration);

        if (mod.getContentUrl() != null && !mod.getContentUrl().isBlank()) {
            Label hasContent = new Label(I18n.get("formations.detail.has_content"));
            hasContent.setGraphic(SvgIcons.icon(SvgIcons.FILE_TEXT, 12, "-fx-muted-foreground"));
            hasContent.getStyleClass().add("text-muted");
            modMeta.getChildren().add(hasContent);
        }
        infoBox.getChildren().addAll(modTitle, modMeta);

        // Status badge
        Label statusIcon = new Label();
        if (isCompleted) {
            statusIcon.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 18, "-fx-primary"));
            statusIcon.setTooltip(new javafx.scene.control.Tooltip(I18n.get("formations.detail.completed")));
        } else if (enrollment != null) {
            statusIcon.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 18, "-fx-muted-foreground"));
            statusIcon.setTooltip(new javafx.scene.control.Tooltip(I18n.get("formations.detail.pending")));
        }

        topRow.getChildren().addAll(indexLabel, infoBox, statusIcon);

        // Description if available
        cardContent.getChildren().add(topRow);
        if (mod.getDescription() != null && !mod.getDescription().isBlank()) {
            Label descLabel = new Label(mod.getDescription());
            descLabel.getStyleClass().add("text-muted");
            descLabel.setWrapText(true);
            descLabel.setPadding(new Insets(0, 0, 0, 44)); // indent under title
            cardContent.getChildren().add(descLabel);
        }

        // Inline chapter content (collapsible)
        if (mod.getContent() != null && !mod.getContent().isBlank() && enrollment != null) {
            VBox contentSection = new VBox(6);
            contentSection.setPadding(new Insets(8, 0, 0, 44));
            contentSection.setVisible(false);
            contentSection.setManaged(false);

            Label contentLabel = new Label(mod.getContent());
            contentLabel.setWrapText(true);
            contentLabel.getStyleClass().add("text-sm");
            contentLabel.setStyle("-fx-padding: 12; -fx-background-color: -fx-muted; -fx-background-radius: 6;");
            contentSection.getChildren().add(contentLabel);

            TLButton toggleBtn = new TLButton(I18n.get("formations.detail.show_content"), TLButton.ButtonVariant.GHOST, TLButton.ButtonSize.SM);
            toggleBtn.setGraphic(SvgIcons.icon(SvgIcons.BOOK_OPEN, 12));
            toggleBtn.setPadding(new Insets(0, 0, 0, 44));
            toggleBtn.setOnAction(ev -> {
                boolean showing = contentSection.isVisible();
                contentSection.setVisible(!showing);
                contentSection.setManaged(!showing);
                toggleBtn.setText(showing
                        ? I18n.get("formations.detail.show_content")
                        : I18n.get("formations.detail.hide_content"));
            });

            cardContent.getChildren().addAll(toggleBtn, contentSection);
        }

        // Action buttons (only for enrolled users)
        if (enrollment != null) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPadding(new Insets(4, 0, 0, 44));

            // Open resource
            if (mod.getContentUrl() != null && !mod.getContentUrl().isBlank()) {
                TLButton openBtn = new TLButton(I18n.get("formations.module.open_resource"), TLButton.ButtonVariant.OUTLINE, TLButton.ButtonSize.SM);
                openBtn.setGraphic(SvgIcons.icon(SvgIcons.GLOBE, 12));
                openBtn.setOnAction(e -> {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(mod.getContentUrl()));
                        }
                    } catch (Exception ex) {
                        TLToast.error(formationsGrid.getScene(), I18n.get("common.error"), I18n.get("formations.module.open_failed"));
                    }
                });
                actions.getChildren().add(openBtn);
            }

            if (!isCompleted) {
                TLButton completeBtn = new TLButton(I18n.get("formations.module.mark_complete"), TLButton.ButtonVariant.PRIMARY, TLButton.ButtonSize.SM);
                completeBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 12));
                completeBtn.setOnAction(e -> {
                    completeBtn.setDisable(true);
                    completeBtn.setText(I18n.get("common.loading"));
                    markModuleCompleteInline(formation, enrollment, modules, index);
                });
                actions.getChildren().add(completeBtn);
            }

            cardContent.getChildren().add(actions);
        }

        moduleCard.getContent().add(cardContent);
        return moduleCard;
    }

    /**
     * Hides the inline course detail and shows the formations grid again.
     */
    private void hideCourseDetail() {
        courseDetailContainer.setVisible(false);
        courseDetailContainer.setManaged(false);
        courseDetailContainer.getChildren().clear();
        gridContainer.setVisible(true);
        gridContainer.setManaged(true);
        categoryBox.setVisible(true);
        categoryBox.setManaged(true);
        if (searchField != null) {
            searchField.setVisible(true);
            searchField.setManaged(true);
        }
        // Refresh to pick up any progress changes
        loadUserEnrollments();
    }

    /**
     * Marks a module complete using LessonProgressService for proper per-module tracking,
     * then refreshes the inline course detail view.
     */
    private void markModuleCompleteInline(Formation formation, Enrollment enrollment,
                                           List<FormationModule> modules, int moduleIndex) {
        if (enrollment == null || modules == null || modules.isEmpty()) return;
        FormationModule mod = modules.get(moduleIndex);

        AppThreadPool.execute(() -> {
            // Use LessonProgressService for per-module tracking
            lessonProgressService.markLessonCompleted(enrollment.getId(), mod.getId());

            // Calculate new overall progress
            int completedCount = lessonProgressService.getCompletedLessonsCount(enrollment.getId());
            double newProgress = (completedCount / (double) modules.size()) * 100.0;
            newProgress = Math.min(100.0, newProgress);

            enrollmentService.updateProgress(enrollment.getId(), newProgress);
            enrollment.setProgress(newProgress);

            final double finalProgress = newProgress;

            if (finalProgress >= 100.0) {
                int certId = certificateGenerationService.completeAndCertify(enrollment.getId());
                Platform.runLater(() -> {
                    TLToast.success(formationsGrid.getScene(), I18n.get("common.success"),
                            I18n.get("formations.module.completed_cert"));
                    if (certId > 0) {
                        Certificate cert = certificateService.findById(certId);
                        if (cert != null && formationsGrid.getScene() != null) {
                            javafx.stage.Stage stage = (javafx.stage.Stage) formationsGrid.getScene().getWindow();
                            String userName = currentUser != null ? currentUser.getFullName() : "";
                            CertificateViewController.show(stage, cert, userName, formation.getTitle());
                        }
                    }
                    // Refresh the detail view
                    showCourseDetail(formation, enrollment);
                });
            } else {
                Platform.runLater(() -> {
                    TLToast.success(formationsGrid.getScene(), I18n.get("common.success"),
                            I18n.get("formations.module.completed"));
                    // Refresh the detail view
                    showCourseDetail(formation, enrollment);
                });
            }
        });
    }

    /**
     * Opens a detail dialog for an enrolled formation, showing its modules.
     * @deprecated Use showCourseDetail() instead for inline view.
     */
    @SuppressWarnings("unused")
    private void openFormationDetail(Formation formation, Enrollment enrollment) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(formation.getTitle());
        dialog.setDialogTitle(formation.getTitle());
        dialog.setDescription(formation.getDescription() != null ? formation.getDescription() : "");

        VBox modulesBox = new VBox(12);
        modulesBox.setPadding(new Insets(8));

        // Show loading state
        Label loadingLabel = new Label(I18n.get("formations.detail.loading"));
        loadingLabel.getStyleClass().add("text-muted");
        modulesBox.getChildren().add(loadingLabel);

        // Progress summary
        VBox progressBox = new VBox(4);
        Label progLabel = new Label(I18n.get("formations.completion",
                String.format("%.0f", enrollment.getProgress())));
        progLabel.getStyleClass().add("h4");
        TLProgress progressBar = new TLProgress(enrollment.getProgress() / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBox.getChildren().addAll(progLabel, progressBar);

        // Meta info
        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (formation.getProvider() != null) {
            Label provLbl = new Label(formation.getProvider());
            provLbl.setGraphic(SvgIcons.icon(SvgIcons.GRADUATION_CAP, 14, "-fx-muted-foreground"));
            provLbl.getStyleClass().add("text-muted");
            metaRow.getChildren().add(provLbl);
        }
        Label durLbl = new Label(I18n.get("formations.duration", formation.getDurationHours()));
        durLbl.setGraphic(SvgIcons.icon(SvgIcons.TIMER, 14, "-fx-muted-foreground"));
        durLbl.getStyleClass().add("text-muted");
        metaRow.getChildren().add(durLbl);

        dialog.setContent(progressBox, metaRow, new TLSeparator(), modulesBox);
        dialog.addButton(ButtonType.CLOSE);
        dialog.styleButtons();

        // Load modules in background
        Task<List<FormationModule>> task = new Task<>() {
            @Override
            protected List<FormationModule> call() throws Exception {
                return moduleService.findByFormation(formation.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            modulesBox.getChildren().clear();
            List<FormationModule> modules = task.getValue();

            if (modules.isEmpty()) {
                Label emptyLabel = new Label(I18n.get("formations.detail.no_modules"));
                emptyLabel.getStyleClass().add("text-muted");
                modulesBox.getChildren().add(emptyLabel);
                return;
            }

            Label modulesTitle = new Label(I18n.get("formations.detail.modules_title", modules.size()));
            modulesTitle.getStyleClass().add("h4");
            modulesBox.getChildren().add(modulesTitle);

            for (int i = 0; i < modules.size(); i++) {
                FormationModule mod = modules.get(i);
                boolean isCompleted = (enrollment.getProgress() / 100.0) * modules.size() > i;

                TLCard moduleCard = new TLCard();
                moduleCard.setCursor(javafx.scene.Cursor.HAND);
                HBox row = new HBox(12);
                row.setPadding(new Insets(12));
                row.setAlignment(Pos.CENTER_LEFT);

                // Module index
                Label indexLabel = new Label(String.valueOf(i + 1));
                indexLabel.getStyleClass().addAll("h4");
                indexLabel.setStyle("-fx-min-width: 28; -fx-alignment: center;");

                // Module info
                VBox infoBox = new VBox(2);
                HBox.setHgrow(infoBox, Priority.ALWAYS);
                Label modTitle = new Label(mod.getTitle());
                modTitle.getStyleClass().add("label");
                modTitle.setWrapText(true);

                Label modDuration = new Label(mod.getDurationMinutes() + " min");
                modDuration.getStyleClass().add("text-muted");
                infoBox.getChildren().addAll(modTitle, modDuration);

                // Status icon
                Label statusIcon;
                if (isCompleted) {
                    statusIcon = new Label();
                    statusIcon.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 16, "-fx-primary"));
                } else {
                    statusIcon = new Label();
                    statusIcon.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 16, "-fx-muted-foreground"));
                }

                row.getChildren().addAll(indexLabel, infoBox, statusIcon);
                moduleCard.getContent().add(row);
                modulesBox.getChildren().add(moduleCard);

                final int moduleIndex = i;
                moduleCard.setOnMouseClicked(ev -> openModulePlayer(formation, enrollment, modules, moduleIndex));
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            modulesBox.getChildren().clear();
            Label errorLabel = new Label(I18n.get("formations.detail.load_error"));
            errorLabel.getStyleClass().add("text-destructive");
            modulesBox.getChildren().add(errorLabel);
        }));

        AppThreadPool.execute(task);
        dialog.showAndWait();
    }

    private void openModulePlayer(Formation formation, Enrollment enrollment, List<FormationModule> modules, int moduleIndex) {
        if (modules == null || modules.isEmpty() || moduleIndex < 0 || moduleIndex >= modules.size()) return;
        FormationModule module = modules.get(moduleIndex);

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(module.getTitle());
        dialog.setDialogTitle(module.getTitle());
        dialog.setDescription(module.getDescription() != null ? module.getDescription() : "");

        VBox body = new VBox(12);
        body.setPadding(new Insets(8));

        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
                SvgIcons.withText(SvgIcons.BOOK_OPEN, I18n.get("formations.module.index", moduleIndex + 1, modules.size())),
                SvgIcons.withText(SvgIcons.CLOCK, module.getDurationMinutes() + " min")
        );

        VBox contentBox = new VBox(10);
        String url = module.getContentUrl();
        if (url == null || url.isBlank()) {
            contentBox.getChildren().add(new TLEmptyState(
                    SvgIcons.FILE_TEXT,
                    I18n.get("formations.module.no_content"),
                    I18n.get("formations.module.no_content.desc")));
        } else {
            Label urlLabel = new Label(url);
            urlLabel.getStyleClass().addAll("text-2xs", "text-muted");
            urlLabel.setWrapText(true);

            TLButton openBtn = new TLButton(I18n.get("formations.module.open_resource"), TLButton.ButtonVariant.OUTLINE);
            openBtn.setGraphic(SvgIcons.icon(SvgIcons.GLOBE, 14));
            openBtn.setOnAction(e -> {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    }
                } catch (Exception ex) {
                    TLToast.error(formationsGrid.getScene(), I18n.get("common.error"), I18n.get("formations.module.open_failed"));
                }
            });

            contentBox.getChildren().addAll(openBtn, urlLabel);
        }

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        TLButton prevBtn = new TLButton(I18n.get("common.back"), TLButton.ButtonVariant.OUTLINE);
        prevBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        prevBtn.setDisable(moduleIndex == 0);

        TLButton nextBtn = new TLButton(I18n.get("common.next"), TLButton.ButtonVariant.OUTLINE);
        nextBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_RIGHT, 14));
        nextBtn.setDisable(moduleIndex >= modules.size() - 1);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton quizBtn = new TLButton(I18n.get("quiz.title"), TLButton.ButtonVariant.OUTLINE);
        quizBtn.setGraphic(SvgIcons.icon(SvgIcons.LIGHTBULB, 14));
        quizBtn.setOnAction(e -> {
            if (currentUser == null) return;
            QuizController.showQuizDialog(formation.getId(), currentUser.getId(), formationsGrid.getScene());
        });

        TLButton completeBtn = new TLButton(I18n.get("formations.module.mark_complete"), TLButton.ButtonVariant.PRIMARY);
        completeBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
        completeBtn.setOnAction(e -> markModuleComplete(formation, enrollment, modules, moduleIndex, dialog));

        prevBtn.setOnAction(e -> {
            dialog.close();
            openModulePlayer(formation, enrollment, modules, moduleIndex - 1);
        });
        nextBtn.setOnAction(e -> {
            dialog.close();
            openModulePlayer(formation, enrollment, modules, moduleIndex + 1);
        });

        actions.getChildren().addAll(prevBtn, nextBtn, spacer, quizBtn, completeBtn);

        body.getChildren().addAll(meta, new TLSeparator(), contentBox, new TLSeparator(), actions);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.setContent(scroll);
        dialog.addButton(ButtonType.CLOSE);
        dialog.styleButtons();
        dialog.showAndWait();
    }

    private void markModuleComplete(Formation formation, Enrollment enrollment, List<FormationModule> modules, int moduleIndex, TLDialog<ButtonType> parentDialog) {
        if (enrollment == null || modules == null || modules.isEmpty()) return;
        double progress = ((moduleIndex + 1) / (double) modules.size()) * 100.0;
        double newProgress = Math.max(enrollment.getProgress(), progress);

        // Disable the complete button in the parent dialog to prevent double-click
        if (parentDialog != null) {
            Node okButton = parentDialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okButton != null) okButton.setDisable(true);
        }

        AppThreadPool.execute(() -> {
            boolean ok = enrollmentService.updateProgress(enrollment.getId(), newProgress);
            if (!ok) {
                Platform.runLater(() -> TLToast.error(formationsGrid.getScene(), I18n.get("common.error"), I18n.get("formations.module.progress_failed")));
                return;
            }

            enrollment.setProgress(newProgress);
            if (newProgress >= 100.0) {
                int certId = certificateGenerationService.completeAndCertify(enrollment.getId());
                Platform.runLater(() -> {
                    if (parentDialog != null) parentDialog.close();
                    if (certId > 0) {
                        TLToast.success(formationsGrid.getScene(), I18n.get("common.success"), I18n.get("formations.module.completed_cert"));
                        // Auto-show the certificate
                        Certificate cert = certificateService.findById(certId);
                        if (cert != null && formationsGrid.getScene() != null) {
                            javafx.stage.Stage stage = (javafx.stage.Stage) formationsGrid.getScene().getWindow();
                            String userName = currentUser != null ? currentUser.getFullName() : "";
                            CertificateViewController.show(stage, cert, userName, formation.getTitle());
                        }
                    } else {
                        TLToast.success(formationsGrid.getScene(), I18n.get("common.success"), I18n.get("formations.module.completed"));
                    }
                    loadUserEnrollments();
                });
                return;
            }

            Platform.runLater(() -> {
                if (parentDialog != null) parentDialog.close();
                TLToast.success(formationsGrid.getScene(), I18n.get("common.success"), I18n.get("formations.module.completed"));
                loadUserEnrollments();
            });
        });
    }

    @FXML
    private void handleCertificates() {
        if (currentUser == null || formationsGrid == null || formationsGrid.getScene() == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(I18n.get("formations.certificates"));
        dialog.setDialogTitle(I18n.get("formations.certificates"));
        dialog.setDescription(I18n.get("formations.certificates.desc"));

        VBox body = new VBox(14);
        body.setPadding(new Insets(8));
        body.getChildren().add(new TLLoadingState(I18n.get("common.loading")));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.setContent(scroll);
        dialog.addButton(ButtonType.CLOSE);
        dialog.styleButtons();

        AppThreadPool.execute(() -> {
            List<Achievement> achievements = achievementService.findByUserId(currentUser.getId());
            int points = achievementService.getTotalPoints(currentUser.getId());
            List<Certificate> certs = certificateService.findByUser(currentUser.getId());

            Platform.runLater(() -> {
                body.getChildren().clear();

                Label badgesTitle = new Label(I18n.get("achievement.title"));
                badgesTitle.getStyleClass().add("h4");
                Label pointsLabel = new Label(I18n.get("achievement.total_points", points));
                pointsLabel.getStyleClass().add("text-muted");

                VBox badgesBox = new VBox(10);
                if (achievements.isEmpty()) {
                    badgesBox.getChildren().add(new Label(I18n.get("achievement.empty")));
                } else {
                    for (Achievement a : achievements) {
                        TLCard card = new TLCard();
                        VBox c = new VBox(6);
                        c.setPadding(new Insets(12));
                        Label t = new Label(a.getTitle());
                        t.getStyleClass().add("h4");
                        Label d = new Label(a.getDescription() != null ? a.getDescription() : "");
                        d.getStyleClass().add("text-muted");
                        d.setWrapText(true);
                        TLBadge rarity = new TLBadge(a.getRarity() != null ? a.getRarity().name() : "", TLBadge.Variant.OUTLINE);
                        HBox top = new HBox(8, t, rarity);
                        top.setAlignment(Pos.CENTER_LEFT);
                        c.getChildren().addAll(top, d);
                        card.getContent().add(c);
                        badgesBox.getChildren().add(card);
                    }
                }

                Label certTitle = new Label(I18n.get("certificate.title"));
                certTitle.getStyleClass().add("h4");

                VBox certBox = new VBox(10);
                if (certs.isEmpty()) {
                    certBox.getChildren().add(new Label(I18n.get("formations.certificates.empty")));
                } else {
                    for (Certificate cert : certs) {
                        certBox.getChildren().add(buildCertificateCard(cert));
                    }
                }

                body.getChildren().addAll(badgesTitle, pointsLabel, badgesBox, new TLSeparator(), certTitle, certBox);
            });
        });

        dialog.showAndWait();
    }

    private TLCard buildCertificateCard(Certificate cert) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));

        Label formationTitle = new Label(I18n.get("formations.certificates.loading_formation"));
        formationTitle.getStyleClass().add("h4");
        formationTitle.setWrapText(true);

        Label number = new Label(I18n.get("certificate.number") + " " + cert.getCertificateNumber());
        number.getStyleClass().add("text-muted");

        Label issued = new Label(I18n.get("certificate.issued") + " " + (cert.getIssuedDate() != null ? cert.getIssuedDate().toLocalDate() : ""));
        issued.getStyleClass().addAll("text-2xs", "text-muted");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        TLButton verifyBtn = new TLButton(I18n.get("certificate.verify"), TLButton.ButtonVariant.OUTLINE);
        verifyBtn.setGraphic(SvgIcons.icon(SvgIcons.SHIELD_CHECK, 14));
        verifyBtn.setOnAction(e -> showCertificateDetails(cert));

        TLButton dlBtn = new TLButton(I18n.get("certificate.download"), TLButton.ButtonVariant.OUTLINE);
        dlBtn.setGraphic(SvgIcons.icon(SvgIcons.FILE_TEXT, 14));
        dlBtn.setOnAction(e -> openCertificatePdf(cert));

        actions.getChildren().addAll(verifyBtn, dlBtn);

        content.getChildren().addAll(formationTitle, number, issued, actions);
        card.getContent().add(content);

        AppThreadPool.execute(() -> {
            try {
                Enrollment enrollment = enrollmentService.findById(cert.getEnrollmentId());
                Formation f = enrollment != null ? formationService.findById(enrollment.getFormationId()) : null;
                if (f != null) {
                    Platform.runLater(() -> formationTitle.setText(f.getTitle()));
                } else {
                    Platform.runLater(() -> formationTitle.setText(I18n.get("formations.certificates.unknown_formation")));
                }
            } catch (Exception ignored) {}
        });

        return card;
    }

    private void showCertificateDetails(Certificate cert) {
        if (cert == null) return;

        // Resolve formation title from enrollment
        AppThreadPool.execute(() -> {
            String formationTitle = I18n.get("formations.certificates.unknown_formation");
            String recipientName = currentUser != null ? currentUser.getFullName() : "";
            try {
                Enrollment enrollment = enrollmentService.findById(cert.getEnrollmentId());
                if (enrollment != null) {
                    Formation f = formationService.findById(enrollment.getFormationId());
                    if (f != null) formationTitle = f.getTitle();
                }
            } catch (Exception ignored) {}

            final String title = formationTitle;
            final String name = recipientName;
            Platform.runLater(() -> {
                if (formationsGrid.getScene() != null) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) formationsGrid.getScene().getWindow();
                    CertificateViewController.show(stage, cert, name, title);
                }
            });
        });
    }

    private void openCertificatePdf(Certificate cert) {
        String url = cert != null ? cert.getPdfUrl() : null;
        if (url == null || url.isBlank()) {
            TLToast.info(formationsGrid.getScene(), I18n.get("certificate.title"), I18n.get("formations.certificates.no_pdf"));
            return;
        }
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception ex) {
            TLToast.error(formationsGrid.getScene(), I18n.get("common.error"), I18n.get("formations.certificates.open_pdf_failed"));
        }
    }

    private void handleEnroll(Formation formation) {
        if (currentUser == null) {
            logger.warn("Cannot enroll: user not logged in");
            return;
        }

        if (!formation.isFree()) {
            com.skilora.utils.DialogUtils.showConfirmation(
                I18n.get("formations.enroll.confirm.title"),
                I18n.get("formations.enroll.confirm.message",
                    formation.getTitle(),
                    formation.getCost() + " " + formation.getCurrency())
            ).ifPresent(result -> {
                if (result == javafx.scene.control.ButtonType.OK) {
                    processPaymentAndEnroll(formation);
                }
            });
            return;
        }

        executeEnroll(formation);
    }

    private void processPaymentAndEnroll(Formation formation) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Verify user has a bank account on file
                List<BankAccount> accounts = BankAccountService.getInstance()
                        .findByUserId(currentUser.getId());
                if (accounts == null || accounts.isEmpty()) {
                    return false;
                }

                // Record payment transaction
                BankAccount primaryAccount = accounts.stream()
                        .filter(BankAccount::isPrimary)
                        .findFirst()
                        .orElse(accounts.get(0));

                PaymentTransaction tx = new PaymentTransaction();
                tx.setFromAccountId(primaryAccount.getId());
                tx.setAmount(BigDecimal.valueOf(formation.getCost()));
                tx.setCurrency(formation.getCurrency() != null ? formation.getCurrency() : "TND");
                tx.setTransactionType("FORMATION_FEE");
                tx.setStatus("COMPLETED");
                tx.setNotes("Formation enrollment: " + formation.getTitle()
                        + " (ID=" + formation.getId() + ")");

                PaymentTransactionService.getInstance().create(tx);
                logger.info("Payment transaction {} recorded for formation '{}'",
                        tx.getReference(), formation.getTitle());
                return true;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (task.getValue()) {
                executeEnroll(formation);
                TLToast.success(formationsGrid.getScene(),
                        I18n.get("common.success"),
                        I18n.get("formations.enroll.payment.success"));
            } else {
                TLToast.error(formationsGrid.getScene(),
                        I18n.get("common.error"),
                        I18n.get("formations.enroll.payment.no_bank"));
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Payment processing failed for formation '{}'",
                    formation.getTitle(), task.getException());
            TLToast.error(formationsGrid.getScene(),
                    I18n.get("common.error"),
                    I18n.get("formations.enroll.payment.failed"));
        }));

        AppThreadPool.execute(task);
    }

    private void executeEnroll(Formation formation) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return enrollmentService.enroll(formation.getId(), currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            int enrollmentId = task.getValue();
            if (enrollmentId > 0) {
                logger.info("Enrolled in formation: {}", formation.getTitle());
                // Refresh enrollments so the card updates to show progress
                loadUserEnrollments();
            } else {
                logger.warn("Already enrolled in formation: {}", formation.getTitle());
            }
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to enroll", task.getException());
        });

        AppThreadPool.execute(task);
    }

    @FXML
    private void handleRefresh() {
        loadFormations();
        if (currentUser != null) {
            loadUserEnrollments();
        }
    }
}
