package com.skilora.formation.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLProgress;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.entity.Enrollment;
import com.skilora.user.entity.User;
import com.skilora.formation.service.FormationService;
import com.skilora.formation.service.EnrollmentService;
import com.skilora.formation.enums.FormationLevel;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

/**
 * FormationsController - Training/Courses view for job seekers.
 * Displays available formations, certifications, and learning resources.
 * Database-backed with full CRUD operations.
 */
public class FormationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FormationsController.class);

    private final FormationService formationService = FormationService.getInstance();
    private final EnrollmentService enrollmentService = EnrollmentService.getInstance();
    
    private User currentUser;

    @FXML private Label statsLabel;
    @FXML private HBox categoryBox;
    @FXML private TLTextField searchField;
    @FXML private FlowPane formationsGrid;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private List<Formation> allFormations;
    private Map<Integer, Enrollment> userEnrollments = new HashMap<>();
    private ToggleGroup categoryGroup;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategories();
        setupSearch();
        loadFormations();
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

        task.setOnFailed(e -> logger.error("Failed to load user enrollments", task.getException()));
        new Thread(task).start();
    }

    private void setupCategories() {
        categoryGroup = new ToggleGroup();
        String[][] categories = {
            {I18n.get("formations.filter.all"), "ALL"},
            {I18n.get("formations.filter.development"), "Développement"},
            {I18n.get("formations.filter.design"), "Design"},
            {I18n.get("formations.filter.marketing"), "Marketing"},
            {I18n.get("formations.filter.data_science"), "Data Science"},
            {I18n.get("formations.filter.languages"), "Langues"},
            {I18n.get("formations.filter.soft_skills"), "Soft Skills"}
        };

        for (String[] cat : categories) {
            ToggleButton btn = new ToggleButton(cat[0]);
            btn.setUserData(cat[1]);
            btn.getStyleClass().add("chip-filter");
            btn.setToggleGroup(categoryGroup);
            if ("ALL".equals(cat[1])) btn.setSelected(true);

            final String catKey = cat[1];
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    currentCategory = catKey;
                    applyFilters();
                } else if (categoryGroup.getSelectedToggle() == null) {
                    btn.setSelected(true);
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
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load formations", task.getException());
            Platform.runLater(() -> {
                allFormations = new ArrayList<>();
                applyFilters();
            });
        });

        new Thread(task).start();
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
        Label providerLabel = new Label("🏫 " + (formation.getProvider() != null ? formation.getProvider() : ""));
        providerLabel.getStyleClass().add("text-muted");
        Label durationLabel = new Label("⏱ " + I18n.get("formations.duration", formation.getDurationHours()));
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
            continueBtn.setOnAction(e -> {
                logger.info("Continue formation: {}", formation.getTitle());
                // TODO: Navigate to formation details/modules
            });
            
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, new TLSeparator(), progressBox, continueBtn);
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
            enrollBtn.setOnAction(e -> handleEnroll(formation));
            
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, sep, enrollBtn);
        }

        card.getContent().add(content);
        return card;
    }

    private void handleEnroll(Formation formation) {
        if (currentUser == null) {
            logger.warn("Cannot enroll: user not logged in");
            return;
        }

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

        new Thread(task).start();
    }

    @FXML
    private void handleRefresh() {
        loadFormations();
        if (currentUser != null) {
            loadUserEnrollments();
        }
    }
}
