package com.skilora.community.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLTypography;
import com.skilora.user.entity.User;
import com.skilora.community.service.DashboardStatsService;
import com.skilora.utils.I18n;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.ContentDisplay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardStatsService statsService = DashboardStatsService.getInstance();

    @FXML
    private Label greetingLabel;
    @FXML
    private Label subGreetingLabel;
    @FXML
    private FlowPane actionsContainer;
    @FXML
    private FlowPane statsGrid;
    @FXML
    private VBox activityList;

    @FXML
    private VBox headerSection;
    @FXML
    private VBox quickActionsSection;
    @FXML
    private VBox statsSection;
    @FXML
    private VBox activitySection;

    private User currentUser;
    private Runnable onNavigateToProfile;
    private Runnable onNavigateToFeed;
    private Runnable onNavigateToUsers;

    public void initializeContext(User user, Runnable toProfile, Runnable toFeed, Runnable toUsers) {
        this.currentUser = user;
        this.onNavigateToProfile = toProfile;
        this.onNavigateToFeed = toFeed;
        this.onNavigateToUsers = toUsers;

        setupGreeting();
        setupQuickActions();
        setupStats();
        setupActivity();
    }

    private void setupGreeting() {
        String greeting = getTimeBasedGreeting();
        String name = (currentUser.getFullName() != null)
                ? currentUser.getFullName().split(" ")[0]
                : currentUser.getUsername();

        greetingLabel.setText(greeting + ", " + name + " \uD83D\uDC4B");
        subGreetingLabel.setText(I18n.get("dashboard.sub_greeting",
                currentUser.getRole().getDisplayName().toLowerCase()));
    }

    private void setupQuickActions() {
        actionsContainer.getChildren().clear();

        switch (currentUser.getRole()) {
            case ADMIN:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.add_user"),
                                "M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                                onNavigateToUsers),
                        createQuickAction(I18n.get("dashboard.action.view_reports"), "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z", null));
                break;
            case EMPLOYER:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.post_job"), "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", null),
                        createQuickAction(I18n.get("dashboard.action.search_candidates"),
                                "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
                                null));
                break;
            case USER:
            default:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.update_profile"),
                                "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                                onNavigateToProfile),
                        createQuickAction(I18n.get("dashboard.action.explore_offers"),
                                "M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z",
                                onNavigateToFeed));
                break;
        }

        // Hide entire section if no actions
        if (actionsContainer.getChildren().isEmpty()) {
            quickActionsSection.setVisible(false);
            quickActionsSection.setManaged(false);
        }
    }

    private void setupStats() {
        statsGrid.getChildren().clear();

        // Show loading placeholders
        statsGrid.getChildren().add(createStatCard(I18n.get("common.loading"), "...", "", false));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                return null; // Stats fetched on FX thread via Platform.runLater
            }
        };

        new Thread(() -> {
            try {
                int userId = currentUser.getId();
                switch (currentUser.getRole()) {
                    case ADMIN: {
                        int users = statsService.getTotalUsers();
                        int newUsers = statsService.getNewUsersThisMonth();
                        int offers = statsService.getTotalActiveOffers();
                        int tickets = statsService.getOpenTickets();
                        double payroll = statsService.getTotalPayrollThisMonth();
                        String payrollStr = String.format("%,.0f TND", payroll);
                        Platform.runLater(() -> {
                            statsGrid.getChildren().clear();
                            statsGrid.getChildren().addAll(
                                createStatCard(I18n.get("dashboard.stat.users"), String.format("%,d", users), "+" + newUsers + " " + I18n.get("common.per_month"), newUsers > 0),
                                createStatCard(I18n.get("dashboard.stat.active_offers"), String.format("%,d", offers), "", false),
                                createStatCard(I18n.get("dashboard.stat.reports"), String.valueOf(tickets), I18n.get("dashboard.stat.open_tickets"), tickets > 0),
                                createStatCard(I18n.get("dashboard.stat.revenue"), payrollStr, I18n.get("dashboard.stat.this_month"), true));
                        });
                        break;
                    }
                    case EMPLOYER: {
                        int views = statsService.getOfferViewsForEmployer(userId);
                        int apps = statsService.getApplicationsForEmployer(userId);
                        int interviews = statsService.getInterviewsThisWeek(userId);
                        Platform.runLater(() -> {
                            statsGrid.getChildren().clear();
                            statsGrid.getChildren().addAll(
                                createStatCard(I18n.get("dashboard.stat.offer_views"), String.format("%,d", views), "", false),
                                createStatCard(I18n.get("dashboard.stat.applications"), String.valueOf(apps), I18n.get("dashboard.stat.pending"), apps > 0),
                                createStatCard(I18n.get("dashboard.stat.interviews"), String.valueOf(interviews), I18n.get("dashboard.stat.this_week"), false));
                        });
                        break;
                    }
                    case USER:
                    default: {
                        int totalApps = statsService.getUserApplicationCount(userId);
                        int activeApps = statsService.getUserActiveApplications(userId);
                        int enrollments = statsService.getUserEnrollmentCount(userId);
                        int connections = statsService.getUserConnectionCount(userId);
                        Platform.runLater(() -> {
                            statsGrid.getChildren().clear();
                            statsGrid.getChildren().addAll(
                                createStatCard(I18n.get("dashboard.stat.applications"), String.valueOf(totalApps), activeApps + " " + I18n.get("dashboard.stat.in_progress"), activeApps > 0),
                                createStatCard(I18n.get("dashboard.stat.formations"), String.valueOf(enrollments), "", false),
                                createStatCard(I18n.get("dashboard.stat.connections"), String.valueOf(connections), "", false));
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load dashboard stats", e);
                Platform.runLater(() -> {
                    statsGrid.getChildren().clear();
                    statsGrid.getChildren().add(createStatCard(I18n.get("common.error"), "—", "", false));
                });
            }
        }, "DashboardStatsThread").start();
    }

    private void setupActivity() {
        activityList.getChildren().clear();

        new Thread(() -> {
            try {
                List<DashboardStatsService.ActivityItem> items = statsService.getRecentActivity(currentUser, 5);
                Platform.runLater(() -> {
                    activityList.getChildren().clear();
                    if (items.isEmpty()) {
                        TLTypography emptyText = new TLTypography(I18n.get("dashboard.activity.empty"), TLTypography.Variant.SM);
                        emptyText.setMuted(true);
                        activityList.getChildren().add(emptyText);
                    } else {
                        for (DashboardStatsService.ActivityItem item : items) {
                            activityList.getChildren().add(createActivityItem(
                                    item.getType(),
                                    item.getDescription(),
                                    formatRelativeTime(item.getTime())));
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load dashboard activity", e);
            }
        }, "DashboardActivityThread").start();
    }

    private String formatRelativeTime(LocalDateTime time) {
        long minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
        if (minutes < 1) return I18n.get("dashboard.activity.time.now");
        if (minutes < 60) return I18n.get("dashboard.activity.time.minutes", minutes);
        long hours = minutes / 60;
        if (hours < 24) return I18n.get("dashboard.activity.time.hours", hours);
        long days = hours / 24;
        if (days < 7) return days + "d";
        return time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // --- Helpers (extracted from MainView) ---

    private TLButton createQuickAction(String text, String svgPath, Runnable action) {
        TLButton btn = new TLButton(text, ButtonVariant.OUTLINE);
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("svg-path");
        icon.setScaleX(0.9);
        icon.setScaleY(0.9);

        btn.setGraphic(icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);
        if (action != null)
            btn.setOnAction(e -> action.run());
        return btn;
    }

    private Node createStatCard(String title, String value, String trend, boolean positive) {
        TLCard card = new TLCard();
        card.setPrefWidth(240);

        VBox content = new VBox(8);
        TLTypography titleTypo = new TLTypography(title, TLTypography.Variant.SM);
        titleTypo.setMuted(true);

        TLTypography valueTypo = new TLTypography(value, TLTypography.Variant.H2);

        TLTypography trendTypo = new TLTypography(trend, TLTypography.Variant.XS);
        trendTypo.setStyle(positive || trend.startsWith("+")
                ? "-fx-text-fill: -fx-success;"
                : "-fx-text-fill: -fx-muted-foreground;");

        content.getChildren().addAll(titleTypo, valueTypo, trendTypo);
        card.setBody(content);
        return card;
    }

    private Node createActivityItem(String type, String desc, String time) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(8, 0, 8, 0));

        VBox texts = new VBox(4);
        TLTypography typeText = new TLTypography(type, TLTypography.Variant.SM);
        typeText.setStyle("-fx-font-weight: bold;");

        TLTypography descText = new TLTypography(desc, TLTypography.Variant.P);
        texts.getChildren().addAll(typeText, descText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLTypography timeText = new TLTypography(time, TLTypography.Variant.XS);
        timeText.setMuted(true);

        row.getChildren().addAll(texts, spacer, timeText);
        return row;
    }

    private String getTimeBasedGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour >= 5 && hour < 12)
            return I18n.get("dashboard.greeting.morning");
        if (hour >= 12 && hour < 18)
            return I18n.get("dashboard.greeting.afternoon");
        return I18n.get("dashboard.greeting.evening");
    }
}
