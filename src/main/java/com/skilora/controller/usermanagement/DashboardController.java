package com.skilora.controller.usermanagement;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLProgress;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.formation.DashboardService;
import com.skilora.service.formation.DashboardService.DashboardStats;
import com.skilora.service.formation.UserDashboardService;
import com.skilora.service.formation.UserDashboardService.UserDashboardStats;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * DashboardController
 * 
 * Handles both admin and user dashboard views.
 * Displays role-appropriate statistics and quick actions.
 */
public class DashboardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private VBox headerSection;
    @FXML
    private Label greetingLabel;
    @FXML
    private Label subGreetingLabel;
    @FXML
    private FlowPane statsGrid;
    @FXML
    private FlowPane actionsContainer;
    @FXML
    private VBox activitySection;

    private User currentUser;
    private DashboardService dashboardService;
    private UserDashboardService userDashboardService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardService = DashboardService.getInstance();
        userDashboardService = UserDashboardService.getInstance();
        
        // Initialize UI with loading state
        greetingLabel.setText("Loading...");
        subGreetingLabel.setText("Please wait while we fetch your statistics");
        
        // Stats will be loaded when setCurrentUser is called
    }

    /**
     * Set the current user and load dashboard data.
     * This should be called after FXML loading.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        if (user == null) {
            greetingLabel.setText("Error");
            subGreetingLabel.setText("User information not available.");
            return;
        }

        // Update greeting
        String fullName = user.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = user.getUsername();
        }
        greetingLabel.setText("Welcome back, " + fullName + "!");
        
        // Load role-appropriate dashboard
        if (user.getRole() == Role.ADMIN) {
            subGreetingLabel.setText("Here's what's happening with your platform today.");
            loadAdminStatistics();
            setupAdminQuickActions();
        } else if (user.getRole() == Role.USER || user.getRole() == Role.JOB_SEEKER) {
            subGreetingLabel.setText("Track your learning progress and achievements.");
            loadUserStatistics();
            setupUserQuickActions();
        } else {
            greetingLabel.setText("Access Denied");
            subGreetingLabel.setText("Dashboard is only available for administrators and regular users.");
        }
    }

    /**
     * Load admin dashboard statistics from the database.
     */
    private void loadAdminStatistics() {
        Task<DashboardStats> statsTask = new Task<DashboardStats>() {
            @Override
            protected DashboardStats call() throws Exception {
                return dashboardService.getDashboardStats();
            }

            @Override
            protected void succeeded() {
                DashboardStats stats = getValue();
                Platform.runLater(() -> displayAdminStatistics(stats));
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load admin dashboard statistics", exception);
                Platform.runLater(() -> {
                    // Display error state
                    displayAdminStatistics(new DashboardStats(0, 0, 0));
                    subGreetingLabel.setText("Error loading statistics. Please try refreshing.");
                });
            }
        };

        Thread statsThread = new Thread(statsTask, "AdminDashboardStatsThread");
        statsThread.setDaemon(true);
        statsThread.start();
    }

    /**
     * Load user dashboard statistics from the database.
     */
    private void loadUserStatistics() {
        Task<UserDashboardStats> statsTask = new Task<UserDashboardStats>() {
            @Override
            protected UserDashboardStats call() throws Exception {
                return userDashboardService.getUserDashboardStats(currentUser.getId());
            }

            @Override
            protected void succeeded() {
                UserDashboardStats stats = getValue();
                Platform.runLater(() -> displayUserStatistics(stats));
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load user dashboard statistics", exception);
                Platform.runLater(() -> {
                    // Display error state
                    displayUserStatistics(new UserDashboardStats(0, 0, 0, 0, 0));
                    subGreetingLabel.setText("Error loading statistics. Please try refreshing.");
                });
            }
        };

        Thread statsThread = new Thread(statsTask, "UserDashboardStatsThread");
        statsThread.setDaemon(true);
        statsThread.start();
    }

    /**
     * Display admin statistics in dashboard cards.
     */
    private void displayAdminStatistics(DashboardStats stats) {
        statsGrid.getChildren().clear();

        // Card 1: Total Formations
        TLCard totalCard = createStatCardWithIcon(
            "Total Formations",
            String.valueOf(stats.getTotalFormations()),
            "All formations in the system",
            "#3b82f6",
            "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"
        );
        statsGrid.getChildren().add(totalCard);

        // Card 2: Processed Formations
        TLCard processedCard = createStatCardWithIcon(
            "Processed Formations",
            String.valueOf(stats.getProcessedFormations()),
            "Formations that have been reviewed",
            "#10b981",
            "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
        );
        statsGrid.getChildren().add(processedCard);

        // Card 3: Certificates Generated
        TLCard certificatesCard = createStatCardWithIcon(
            "Certificates Generated",
            String.valueOf(stats.getCertificatesGenerated()),
            "Certificates ready for distribution",
            "#f59e0b",
            "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"
        );
        statsGrid.getChildren().add(certificatesCard);
    }

    /**
     * Display user statistics in dashboard cards with progress indicators.
     */
    private void displayUserStatistics(UserDashboardStats stats) {
        statsGrid.getChildren().clear();

        // Card 1: Available Formations
        TLCard availableCard = createStatCardWithIcon(
            "Available Formations",
            String.valueOf(stats.getTotalAvailableFormations()),
            "Formations you can enroll in",
            "#3b82f6",
            "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"
        );
        statsGrid.getChildren().add(availableCard);

        // Card 2: Enrolled Formations
        TLCard enrolledCard = createStatCardWithIcon(
            "My Enrollments",
            String.valueOf(stats.getEnrolledFormations()),
            "Formations you're currently taking",
            "#8b5cf6",
            "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
        );
        statsGrid.getChildren().add(enrolledCard);

        // Card 3: Completed Formations
        TLCard completedCard = createStatCardWithIcon(
            "Completed",
            String.valueOf(stats.getCompletedFormations()),
            "Formations you've finished",
            "#10b981",
            "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
        );
        statsGrid.getChildren().add(completedCard);

        // Card 4: Certificates Earned
        TLCard certificatesCard = createStatCardWithIcon(
            "Certificates",
            String.valueOf(stats.getCertificatesEarned()),
            "Certificates you've earned",
            "#f59e0b",
            "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"
        );
        statsGrid.getChildren().add(certificatesCard);

        // Progress Card: Overall Progress
        if (stats.getEnrolledFormations() > 0) {
            TLCard progressCard = createProgressCard(stats);
            statsGrid.getChildren().add(progressCard);
        }
    }

    /**
     * Create a statistics card with modern UI and icon.
     */
    private TLCard createStatCardWithIcon(String title, String value, String description, String color, String iconPath) {
        TLCard card = new TLCard();
        card.setPrefWidth(280);
        card.setPrefHeight(160);
        card.setStyle("-fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);

        // Icon and Title Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.setStyle("-fx-fill: " + color + ";");
        icon.setScaleX(1.2);
        icon.setScaleY(1.2);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-sm");
        titleLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.6); -fx-font-weight: 500;");
        
        headerRow.getChildren().addAll(icon, titleLabel);
        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

        // Value
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        // Description
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-xs");
        descLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.5);");
        descLabel.setWrapText(true);

        content.getChildren().addAll(headerRow, valueLabel, descLabel);
        card.getChildren().add(content);

        return card;
    }

    /**
     * Create a progress card showing overall learning progress.
     */
    private TLCard createProgressCard(UserDashboardStats stats) {
        TLCard card = new TLCard();
        card.setPrefWidth(580);
        card.setPrefHeight(180);
        card.setStyle("-fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        SVGPath icon = new SVGPath();
        icon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        icon.setStyle("-fx-fill: #3b82f6;");
        icon.setScaleX(1.2);
        icon.setScaleY(1.2);
        
        Label titleLabel = new Label("Overall Progress");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: rgba(0,0,0,0.8);");
        
        headerRow.getChildren().addAll(icon, titleLabel);

        // Progress Bar
        VBox progressBox = new VBox(8);
        HBox progressRow = new HBox(16);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        
        TLProgress progressBar = new TLProgress(stats.getProgressPercentage() / 100.0);
        progressBar.setPrefWidth(400);
        progressBar.getStyleClass().add("progress-lg");
        
        Label progressLabel = new Label(stats.getProgressPercentage() + "%");
        progressLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
        
        progressRow.getChildren().addAll(progressBar, progressLabel);
        HBox.setHgrow(progressBar, javafx.scene.layout.Priority.ALWAYS);

        // Progress Details
        Label detailsLabel = new Label(
            stats.getCompletedFormations() + " out of " + stats.getEnrolledFormations() + 
            " formations completed"
        );
        detailsLabel.getStyleClass().add("text-sm");
        detailsLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.6);");

        progressBox.getChildren().addAll(progressRow, detailsLabel);
        content.getChildren().addAll(headerRow, progressBox);
        card.getChildren().add(content);

        return card;
    }

    /**
     * Setup quick action buttons for admin.
     */
    private void setupAdminQuickActions() {
        actionsContainer.getChildren().clear();

        // Quick Action: Manage Formations
        TLButton manageFormationsBtn = new TLButton("Manage Formations", TLButton.ButtonVariant.PRIMARY);
        manageFormationsBtn.setOnAction(e -> {
            logger.info("Navigate to formations management");
        });
        actionsContainer.getChildren().add(manageFormationsBtn);

        // Quick Action: View Users
        TLButton viewUsersBtn = new TLButton("View Users", TLButton.ButtonVariant.OUTLINE);
        viewUsersBtn.setOnAction(e -> {
            logger.info("Navigate to users management");
        });
        actionsContainer.getChildren().add(viewUsersBtn);

        // Quick Action: Generate Reports
        TLButton reportsBtn = new TLButton("Generate Reports", TLButton.ButtonVariant.OUTLINE);
        reportsBtn.setOnAction(e -> {
            logger.info("Navigate to reports");
        });
        actionsContainer.getChildren().add(reportsBtn);
    }

    /**
     * Setup quick action buttons for regular users.
     */
    private void setupUserQuickActions() {
        actionsContainer.getChildren().clear();

        // Quick Action: Browse Formations
        TLButton browseFormationsBtn = new TLButton("Browse Formations", TLButton.ButtonVariant.PRIMARY);
        browseFormationsBtn.setOnAction(e -> {
            logger.info("Navigate to formations");
        });
        actionsContainer.getChildren().add(browseFormationsBtn);

        // Quick Action: My Progress
        TLButton myProgressBtn = new TLButton("My Progress", TLButton.ButtonVariant.OUTLINE);
        myProgressBtn.setOnAction(e -> {
            logger.info("Navigate to training progress");
        });
        actionsContainer.getChildren().add(myProgressBtn);

        // Quick Action: My Certificates
        TLButton certificatesBtn = new TLButton("My Certificates", TLButton.ButtonVariant.OUTLINE);
        certificatesBtn.setOnAction(e -> {
            logger.info("Navigate to certificates");
        });
        actionsContainer.getChildren().add(certificatesBtn);
    }

    /**
     * Refresh dashboard statistics.
     * Can be called manually or via refresh button.
     */
    public void refresh() {
        if (currentUser == null) {
            return;
        }
        
        if (currentUser.getRole() == Role.ADMIN) {
            loadAdminStatistics();
        } else if (currentUser.getRole() == Role.USER || currentUser.getRole() == Role.JOB_SEEKER) {
            loadUserStatistics();
        }
    }
}
