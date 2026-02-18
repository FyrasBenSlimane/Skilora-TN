package com.skilora.community.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLTypography;
import com.skilora.user.entity.User;
import com.skilora.community.service.DashboardStatsService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.application.Platform;
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
    private Runnable onNavigateToReports;

    public void initializeContext(User user, Runnable toProfile, Runnable toFeed, Runnable toUsers, Runnable toReports) {
        this.currentUser = user;
        this.onNavigateToProfile = toProfile;
        this.onNavigateToFeed = toFeed;
        this.onNavigateToUsers = toUsers;
        this.onNavigateToReports = toReports;

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

        greetingLabel.setText(greeting + ", " + name);
        subGreetingLabel.setText(I18n.get("dashboard.sub_greeting",
                currentUser.getRole().getDisplayName().toLowerCase()));
    }

    private void setupQuickActions() {
        actionsContainer.getChildren().clear();

        switch (currentUser.getRole()) {
            case ADMIN:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.add_user"),
                                SvgIcons.USER_PLUS,
                                onNavigateToUsers),
                        createQuickAction(I18n.get("dashboard.action.view_reports"),
                                SvgIcons.ALERT_TRIANGLE,
                                onNavigateToReports));
                break;
            case EMPLOYER:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.post_job"),
                                SvgIcons.BRIEFCASE,
                                () -> TLToast.success(greetingLabel.getScene(), I18n.get("common.info"), I18n.get("dashboard.coming_soon"))),
                        createQuickAction(I18n.get("dashboard.action.search_candidates"),
                                SvgIcons.SEARCH,
                                () -> TLToast.success(greetingLabel.getScene(), I18n.get("common.info"), I18n.get("dashboard.coming_soon"))));
                break;
            case USER:
            default:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.update_profile"),
                                SvgIcons.USER,
                                onNavigateToProfile),
                        createQuickAction(I18n.get("dashboard.action.explore_offers"),
                                SvgIcons.GLOBE,
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

        AppThreadPool.execute(() -> {
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
        });
    }

    private void setupActivity() {
        activityList.getChildren().clear();

        AppThreadPool.execute(() -> {
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
        });
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
        SVGPath icon = SvgIcons.icon(svgPath, 16);
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

        // Build trend row with arrow icon
        HBox trendRow = new HBox(4);
        trendRow.setAlignment(Pos.CENTER_LEFT);

        if (trend != null && !trend.isEmpty()) {
            boolean isPositive = positive || trend.startsWith("+");
            String trendColor = isPositive ? "-fx-success" : "-fx-muted-foreground";
            String arrowPath = isPositive ? SvgIcons.TRENDING_UP : SvgIcons.TRENDING_DOWN;

            SVGPath trendArrow = SvgIcons.icon(arrowPath, 12, trendColor);
            TLTypography trendTypo = new TLTypography(trend, TLTypography.Variant.XS);
            trendTypo.setStyle("-fx-text-fill: " + trendColor + ";");

            trendRow.getChildren().addAll(trendArrow, trendTypo);
        }

        content.getChildren().addAll(titleTypo, valueTypo, trendRow);
        card.setBody(content);
        return card;
    }

    private Node createActivityItem(String type, String desc, String time) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(8, 0, 8, 0));

        // Activity-type icon
        String iconPath = getActivityIcon(type);
        SVGPath activityIcon = SvgIcons.icon(iconPath, 16, "-fx-muted-foreground");
        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(activityIcon);
        iconWrap.setMinSize(32, 32);
        iconWrap.setPrefSize(32, 32);
        iconWrap.setMaxSize(32, 32);
        iconWrap.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 6;");
        iconWrap.setAlignment(Pos.CENTER);

        VBox texts = new VBox(4);
        TLTypography typeText = new TLTypography(type, TLTypography.Variant.SM);
        typeText.setStyle("-fx-font-weight: bold;");

        TLTypography descText = new TLTypography(desc, TLTypography.Variant.P);
        texts.getChildren().addAll(typeText, descText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLTypography timeText = new TLTypography(time, TLTypography.Variant.XS);
        timeText.setMuted(true);

        row.getChildren().addAll(iconWrap, texts, spacer, timeText);
        return row;
    }

    /** Map activity type strings to relevant icons. */
    private String getActivityIcon(String type) {
        if (type == null) return SvgIcons.ACTIVITY;
        String lower = type.toLowerCase();
        if (lower.contains("login") || lower.contains("connexion")) return SvgIcons.LOG_IN;
        if (lower.contains("ticket") || lower.contains("support")) return SvgIcons.MESSAGE_CIRCLE;
        if (lower.contains("user") || lower.contains("utilisateur")) return SvgIcons.USER;
        if (lower.contains("offer") || lower.contains("offre")) return SvgIcons.BRIEFCASE;
        if (lower.contains("formation") || lower.contains("course")) return SvgIcons.GRADUATION_CAP;
        if (lower.contains("payment") || lower.contains("finance")) return SvgIcons.DOLLAR_SIGN;
        if (lower.contains("report") || lower.contains("signal")) return SvgIcons.ALERT_TRIANGLE;
        if (lower.contains("interview") || lower.contains("entretien")) return SvgIcons.VIDEO;
        return SvgIcons.ACTIVITY;
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
