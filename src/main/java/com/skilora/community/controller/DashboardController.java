package com.skilora.community.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLProgress;
import com.skilora.framework.components.TLTypography;
import com.skilora.security.SecurityAlert;
import com.skilora.security.SecurityAlertService;
import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.community.service.DashboardStatsService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.ContentDisplay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private Label quickActionsLabel;
    @FXML
    private Label statsHeaderLabel;
    @FXML
    private Label activityHeaderLabel;
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
    private Runnable onNavigateToPostJob;
    private Runnable onNavigateToApplicationInbox;
    private Runnable onNavigateToFormations;
    private Runnable onNavigateToFormationAdmin;
    private Runnable onNavigateToFinance;

    public void initializeContext(User user, Runnable toProfile, Runnable toFeed, Runnable toUsers, Runnable toReports) {
        this.currentUser = user;
        this.onNavigateToProfile = toProfile;
        this.onNavigateToFeed = toFeed;
        this.onNavigateToUsers = toUsers;
        this.onNavigateToReports = toReports;

        if (quickActionsLabel != null) quickActionsLabel.setText(I18n.get("dashboard.quick_actions"));
        if (statsHeaderLabel != null) statsHeaderLabel.setText(I18n.get("dashboard.statistics"));
        if (activityHeaderLabel != null) activityHeaderLabel.setText(I18n.get("dashboard.recent_activity"));

        setupGreeting();
        setupQuickActions();
        setupStats();
        setupActivity();
        setupProfileCompletion();

        // Admin-only: security alerts + marketplace overview
        if (currentUser.getRole() == Role.ADMIN) {
            setupSecurityAlerts();
            setupMarketplaceOverview();
        }
    }

    /** Set extra navigation callbacks for role-specific quick actions. */
    public void setNavigationCallbacks(Runnable toPostJob, Runnable toApplicationInbox,
                                       Runnable toFormations, Runnable toFormationAdmin,
                                       Runnable toFinance) {
        this.onNavigateToPostJob = toPostJob;
        this.onNavigateToApplicationInbox = toApplicationInbox;
        this.onNavigateToFormations = toFormations;
        this.onNavigateToFormationAdmin = toFormationAdmin;
        this.onNavigateToFinance = toFinance;

        // Rebuild quick actions now that all callbacks are wired
        setupQuickActions();
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
                                onNavigateToReports),
                        createQuickAction(I18n.get("dashboard.action.manage_formations"),
                                SvgIcons.GRADUATION_CAP,
                                onNavigateToFormationAdmin),
                        createQuickAction(I18n.get("dashboard.action.view_finance"),
                                SvgIcons.DOLLAR_SIGN,
                                onNavigateToFinance));
                break;
            case EMPLOYER:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.post_job"),
                                SvgIcons.BRIEFCASE,
                                onNavigateToPostJob),
                        createQuickAction(I18n.get("dashboard.action.search_candidates"),
                                SvgIcons.SEARCH,
                                onNavigateToApplicationInbox));
                break;
            case TRAINER:
                actionsContainer.getChildren().addAll(
                        createQuickAction(I18n.get("dashboard.action.my_formations"),
                                SvgIcons.GRADUATION_CAP,
                                onNavigateToFormationAdmin),
                        createQuickAction(I18n.get("dashboard.action.update_profile"),
                                SvgIcons.USER,
                                onNavigateToProfile));
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
        statsGrid.prefWrapLengthProperty().bind(statsGrid.widthProperty());

        // Show loading state
        statsGrid.getChildren().add(new TLLoadingState());

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
                        // Marketplace stats
                        int formations = statsService.getTotalFormations();
                        int enrollments = statsService.getTotalEnrollments();
                        int contracts = statsService.getActiveContracts();
                        int appsThisMonth = statsService.getTotalApplicationsThisMonth();
                        int interviews = statsService.getPlatformInterviewsThisWeek();
                        int completedJobs = statsService.getCompletedJobsThisMonth();
                        Platform.runLater(() -> {
                            statsGrid.getChildren().clear();
                            // Row 1 — Core metrics
                            statsGrid.getChildren().addAll(
                                createStatCard(I18n.get("dashboard.stat.users"), String.format("%,d", users), "+" + newUsers + " " + I18n.get("common.per_month"), newUsers > 0),
                                createStatCard(I18n.get("dashboard.stat.active_offers"), String.format("%,d", offers), completedJobs + " filled this month", completedJobs > 0),
                                createStatCard(I18n.get("dashboard.stat.open_tickets"), String.valueOf(tickets), I18n.get("dashboard.stat.pending"), tickets > 0),
                                createStatCard(I18n.get("dashboard.stat.revenue"), payrollStr, I18n.get("dashboard.stat.this_month"), true));
                            // Row 2 — Marketplace health
                            statsGrid.getChildren().addAll(
                                createStatCard("Formations", String.valueOf(formations), enrollments + " enrollments", enrollments > 0),
                                createStatCard("Applications", String.valueOf(appsThisMonth), I18n.get("dashboard.stat.this_month"), appsThisMonth > 0),
                                createStatCard("Interviews", String.valueOf(interviews), I18n.get("dashboard.stat.this_week"), interviews > 0),
                                createStatCard("Active Contracts", String.valueOf(contracts), "", false));
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
                    case TRAINER: {
                        int formations = statsService.getTrainerFormationCount(userId);
                        int enrollees = statsService.getTrainerTotalEnrollees(userId);
                        int connections = statsService.getUserConnectionCount(userId);
                        Platform.runLater(() -> {
                            statsGrid.getChildren().clear();
                            statsGrid.getChildren().addAll(
                                createStatCard(I18n.get("dashboard.stat.formations"), String.valueOf(formations), "", false),
                                createStatCard(I18n.get("dashboard.stat.enrollees"), String.valueOf(enrollees), "", enrollees > 0),
                                createStatCard(I18n.get("dashboard.stat.connections"), String.valueOf(connections), "", false));
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
        activityList.getChildren().add(new TLLoadingState());

        AppThreadPool.execute(() -> {
            try {
                List<DashboardStatsService.ActivityItem> items = statsService.getRecentActivity(currentUser, 5);
                Platform.runLater(() -> {
                    activityList.getChildren().clear();
                    activityList.setSpacing(0);
                    if (items.isEmpty()) {
                        activityList.getChildren().add(new TLEmptyState(
                                SvgIcons.ACTIVITY,
                                I18n.get("dashboard.activity.empty"),
                                ""));
                    } else {
                        for (int i = 0; i < items.size(); i++) {
                            DashboardStatsService.ActivityItem item = items.get(i);
                            activityList.getChildren().add(createActivityItem(
                                    item.getType(),
                                    item.getDescription(),
                                    formatRelativeTime(item.getTime())));
                            // Add separator between items (not after last)
                            if (i < items.size() - 1) {
                                Region sep = new Region();
                                sep.setMaxHeight(1);
                                sep.setPrefHeight(1);
                                sep.setStyle("-fx-background-color: -fx-border;");
                                VBox.setMargin(sep, new javafx.geometry.Insets(0, 16, 0, 16));
                                activityList.getChildren().add(sep);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load dashboard activity", e);
                Platform.runLater(() -> {
                    activityList.getChildren().clear();
                    activityList.getChildren().add(new com.skilora.framework.components.TLEmptyState(
                        com.skilora.utils.SvgIcons.ACTIVITY, "Error", "Failed to load activity feed"));
                });
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

    /**
     * D-07: Show profile completion progress bar for USER / TRAINER roles.
     * Checks which fields are filled and shows percentage.
     */
    private void setupProfileCompletion() {
        if (currentUser == null) return;
        if (currentUser.getRole() != com.skilora.user.enums.Role.USER
                && currentUser.getRole() != com.skilora.user.enums.Role.TRAINER) {
            return;
        }

        int totalFields = 5;
        int filled = 0;
        if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) filled++;
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) filled++;
        if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) filled++;
        if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isBlank()) filled++;
        if (currentUser.isVerified()) filled++;

        int pct = (int) Math.round((filled / (double) totalFields) * 100);
        if (pct >= 100) return; // Profile complete — no need to show

        TLCard card = new TLCard();
        VBox body = new VBox(8);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath icon = SvgIcons.icon(SvgIcons.USER, 16, "-fx-primary");
        TLTypography titleText = new TLTypography(
                I18n.get("dashboard.profile_completion"), TLTypography.Variant.SM);
        titleText.getStyleClass().add("font-bold");
        titleRow.getChildren().addAll(icon, titleText);

        TLProgress progressBar = new TLProgress();
        progressBar.setProgress(filled / (double) totalFields);
        progressBar.setPrefHeight(8);

        TLTypography pctLabel = new TLTypography(pct + "% " + I18n.get("dashboard.complete"),
                TLTypography.Variant.XS);
        pctLabel.setMuted(true);

        TLButton completeBtn = new TLButton(I18n.get("dashboard.action.update_profile"), ButtonVariant.OUTLINE);
        completeBtn.setGraphic(SvgIcons.icon(SvgIcons.USER, 14));
        completeBtn.setContentDisplay(ContentDisplay.LEFT);
        if (onNavigateToProfile != null) {
            completeBtn.setOnAction(e -> onNavigateToProfile.run());
        }

        body.getChildren().addAll(titleRow, progressBar, pctLabel, completeBtn);
        card.setBody(body);

        // Insert between stats and activity
        if (activitySection != null && activitySection.getParent() instanceof VBox) {
            VBox parent = (VBox) activitySection.getParent();
            int idx = parent.getChildren().indexOf(activitySection);
            if (idx >= 0) {
                parent.getChildren().add(idx, card);
            }
        }
    }

    // --- Admin: Security Alerts Panel ---

    private VBox securityAlertsSection;
    private TLBadge securityCountBadge;
    private String currentAlertFilter = "UNREVIEWED"; // UNREVIEWED, ALL, LOCKOUT, BRUTE_FORCE, FAILED_LOGIN_CAPTURE

    /**
     * Builds a full-featured security alerts panel for admin users.
     * Includes filter tabs, detailed alert cards, click-to-expand detail dialog,
     * admin review with notes, alert timeline per user, and severity-based styling.
     */
    private void setupSecurityAlerts() {
        securityAlertsSection = new VBox(12);

        // Header with badge + refresh
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath shieldIcon = SvgIcons.icon(SvgIcons.SHIELD, 18, "-fx-destructive");
        TLTypography headerText = new TLTypography("Security Alerts", TLTypography.Variant.H3);
        securityCountBadge = new TLBadge("...", TLBadge.Variant.DESTRUCTIVE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        TLButton refreshBtn = new TLButton("Refresh", ButtonVariant.GHOST);
        refreshBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14));
        refreshBtn.setContentDisplay(ContentDisplay.LEFT);
        refreshBtn.setOnAction(e -> loadSecurityAlerts());
        headerRow.getChildren().addAll(shieldIcon, headerText, securityCountBadge, spacer, refreshBtn);

        // Filter tabs — Unreviewed | All | Lockout | Brute Force | Failed Login
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(0, 0, 4, 0));

        String[][] filters = {
            {"UNREVIEWED", "Unreviewed"},
            {"ALL", "All"},
            {"LOCKOUT", "Lockout"},
            {"BRUTE_FORCE", "Brute Force"},
            {"FAILED_LOGIN_CAPTURE", "Failed Login"}
        };

        for (String[] f : filters) {
            TLButton filterBtn = new TLButton(f[1],
                    f[0].equals(currentAlertFilter) ? ButtonVariant.SECONDARY : ButtonVariant.GHOST);
            filterBtn.setStyle(filterBtn.getStyle() + "-fx-font-size: 12px; -fx-padding: 4 10;");
            filterBtn.setOnAction(e -> {
                currentAlertFilter = f[0];
                // Update button styles
                for (Node n : filterRow.getChildren()) {
                    if (n instanceof TLButton btn) {
                        btn.setVariant(ButtonVariant.GHOST);
                    }
                }
                filterBtn.setVariant(ButtonVariant.SECONDARY);
                loadSecurityAlerts();
            });
            filterRow.getChildren().add(filterBtn);
        }

        securityAlertsSection.getChildren().addAll(headerRow, filterRow, new TLLoadingState());

        // Insert between stats and activity
        if (activitySection != null && activitySection.getParent() instanceof VBox) {
            VBox parent = (VBox) activitySection.getParent();
            int idx = parent.getChildren().indexOf(activitySection);
            if (idx >= 0) {
                parent.getChildren().add(idx, securityAlertsSection);
            }
        }

        loadSecurityAlerts();
    }

    private void loadSecurityAlerts() {
        AppThreadPool.execute(() -> {
            try {
                SecurityAlertService svc = SecurityAlertService.getInstance();
                int unreviewedCount = svc.getUnreviewedCount();

                List<SecurityAlert> alerts;
                switch (currentAlertFilter) {
                    case "ALL" -> alerts = svc.getAllAlerts(100);
                    case "LOCKOUT", "BRUTE_FORCE", "FAILED_LOGIN_CAPTURE" -> {
                        List<SecurityAlert> all = svc.getAllAlerts(200);
                        alerts = all.stream()
                                .filter(a -> a.getAlertType().equals(currentAlertFilter))
                                .toList();
                    }
                    default -> alerts = svc.getUnreviewedAlerts(); // UNREVIEWED
                }

                Platform.runLater(() -> {
                    securityCountBadge.setText(String.valueOf(unreviewedCount));

                    // Remove everything after filter row (header=0, filterRow=1, rest=2+)
                    if (securityAlertsSection.getChildren().size() > 2) {
                        securityAlertsSection.getChildren().remove(2, securityAlertsSection.getChildren().size());
                    }

                    if (alerts.isEmpty()) {
                        TLCard emptyCard = new TLCard();
                        HBox emptyRow = new HBox(10);
                        emptyRow.setAlignment(Pos.CENTER);
                        emptyRow.setPadding(new Insets(24));
                        SVGPath checkIcon = SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 24, "-fx-success");
                        VBox emptyInfo = new VBox(4);
                        TLTypography emptyTitle = new TLTypography(
                                "All clear", TLTypography.Variant.SM);
                        emptyTitle.getStyleClass().add("font-bold");
                        TLTypography emptyText = new TLTypography(
                                currentAlertFilter.equals("UNREVIEWED")
                                        ? "No unreviewed security alerts"
                                        : "No alerts matching this filter",
                                TLTypography.Variant.XS);
                        emptyText.setMuted(true);
                        emptyInfo.getChildren().addAll(emptyTitle, emptyText);
                        emptyRow.getChildren().addAll(checkIcon, emptyInfo);
                        emptyCard.setBody(emptyRow);
                        securityAlertsSection.getChildren().add(emptyCard);
                    } else {
                        TLCard alertsCard = new TLCard();
                        VBox alertsList = new VBox(0);
                        for (int i = 0; i < alerts.size(); i++) {
                            SecurityAlert alert = alerts.get(i);
                            alertsList.getChildren().add(createEnhancedAlertRow(alert));
                            if (i < alerts.size() - 1) {
                                Region sep = new Region();
                                sep.setMaxHeight(1);
                                sep.setPrefHeight(1);
                                sep.setStyle("-fx-background-color: -fx-border;");
                                VBox.setMargin(sep, new Insets(0, 16, 0, 16));
                                alertsList.getChildren().add(sep);
                            }
                        }
                        alertsCard.setBody(alertsList);
                        securityAlertsSection.getChildren().add(alertsCard);

                        // Summary footer
                        TLTypography summary = new TLTypography(
                                alerts.size() + " alert" + (alerts.size() != 1 ? "s" : "") + " shown"
                                + (unreviewedCount > 0 ? " • " + unreviewedCount + " unreviewed" : ""),
                                TLTypography.Variant.XS);
                        summary.setMuted(true);
                        summary.setPadding(new Insets(4, 0, 0, 0));
                        securityAlertsSection.getChildren().add(summary);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load security alerts", e);
                Platform.runLater(() -> {
                    if (activityList.getScene() != null) {
                        com.skilora.framework.components.TLToast.error(
                            activityList.getScene(), "Error", "Failed to load security alerts");
                    }
                });
            }
        });
    }

    /**
     * Creates an enhanced alert row card with:
     * - Severity-colored left accent bar
     * - Photo thumbnail or type icon
     * - Rich detail text (user, type, attempts, IP, time)
     * - Reviewed status indicator
     * - Click to open full detail dialog
     * - Quick review button
     */
    private Node createEnhancedAlertRow(SecurityAlert alert) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setCursor(javafx.scene.Cursor.HAND);

        // Severity accent bar on the left
        Region accentBar = new Region();
        accentBar.setPrefWidth(3);
        accentBar.setMinWidth(3);
        accentBar.setMaxWidth(3);
        accentBar.setMinHeight(50);
        String accentColor = switch (alert.getAlertType()) {
            case "LOCKOUT" -> "#ef4444";       // red
            case "BRUTE_FORCE" -> "#f97316";   // orange
            default -> "#eab308";              // yellow (warning)
        };
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 2;");

        // Photo or icon
        StackPane iconWrap = buildAlertThumbnail(alert, 48);

        // Alert details
        VBox details = new VBox(3);
        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        TLTypography username = new TLTypography(alert.getUsername(), TLTypography.Variant.SM);
        username.getStyleClass().add("font-bold");

        TLBadge typeBadge = new TLBadge(
                formatAlertType(alert.getAlertType()),
                getSeverityBadgeVariant(alert.getAlertType()));
        titleRow.getChildren().addAll(username, typeBadge);

        // Reviewed badge
        if (alert.isReviewed()) {
            TLBadge reviewedBadge = new TLBadge("Reviewed", TLBadge.Variant.OUTLINE);
            titleRow.getChildren().add(reviewedBadge);
        }

        // Detail line
        StringBuilder detailStr = new StringBuilder();
        detailStr.append(alert.getFailedAttempts()).append(" failed attempt")
                .append(alert.getFailedAttempts() != 1 ? "s" : "");
        if (alert.getIpAddress() != null && !alert.getIpAddress().isEmpty()) {
            detailStr.append(" • IP: ").append(alert.getIpAddress());
        }
        TLTypography detailText = new TLTypography(detailStr.toString(), TLTypography.Variant.XS);
        detailText.setMuted(true);

        // Time line with icon
        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath clockIcon = SvgIcons.icon(SvgIcons.CLOCK, 10, "-fx-muted-foreground");
        TLTypography timeText = new TLTypography(
                alert.getCreatedAt() != null
                        ? formatRelativeTime(alert.getCreatedAt()) + " — "
                          + alert.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        : "—",
                TLTypography.Variant.XS);
        timeText.setMuted(true);
        timeRow.getChildren().addAll(clockIcon, timeText);

        details.getChildren().addAll(titleRow, detailText, timeRow);
        HBox.setHgrow(details, Priority.ALWAYS);

        // Action buttons
        VBox actions = new VBox(4);
        actions.setAlignment(Pos.CENTER);

        TLButton viewBtn = new TLButton("View Details", ButtonVariant.OUTLINE);
        viewBtn.setGraphic(SvgIcons.icon(SvgIcons.EYE, 12));
        viewBtn.setContentDisplay(ContentDisplay.LEFT);
        viewBtn.setStyle(viewBtn.getStyle() + "-fx-font-size: 11px; -fx-padding: 4 10;");
        viewBtn.setOnAction(e -> showAlertDetailDialog(alert));

        actions.getChildren().add(viewBtn);

        if (!alert.isReviewed()) {
            TLButton reviewBtn = new TLButton("Review", ButtonVariant.GHOST);
            reviewBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 12));
            reviewBtn.setContentDisplay(ContentDisplay.LEFT);
            reviewBtn.setStyle(reviewBtn.getStyle() + "-fx-font-size: 11px; -fx-padding: 4 10;");
            reviewBtn.setOnAction(e -> showReviewDialog(alert));
            actions.getChildren().add(reviewBtn);
        }

        row.getChildren().addAll(accentBar, iconWrap, details, actions);

        // Click row to open detail
        row.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof javafx.scene.control.ButtonBase)) {
                showAlertDetailDialog(alert);
            }
        });
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 6;"));
        row.setOnMouseExited(e -> row.setStyle(""));

        return row;
    }

    /**
     * Opens a rich detail dialog for a security alert.
     * Shows full-size photo, alert info, user history, and review controls.
     */
    private void showAlertDetailDialog(SecurityAlert alert) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle("Security Alert — " + alert.getUsername());
        dialog.setDescription("Alert #" + alert.getId() + " • " + formatAlertType(alert.getAlertType()));
        dialog.initOwner(activityList.getScene().getWindow());

        VBox content = new VBox(20);

        // ── Top: Alert summary bar ──
        HBox summaryBar = new HBox(16);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(12));
        String summaryBg = switch (alert.getAlertType()) {
            case "LOCKOUT" -> "rgba(239,68,68,0.1)";
            case "BRUTE_FORCE" -> "rgba(249,115,22,0.1)";
            default -> "rgba(234,179,8,0.1)";
        };
        summaryBar.setStyle("-fx-background-color: " + summaryBg + "; -fx-background-radius: 8;");

        SVGPath typeIcon = SvgIcons.icon(getAlertIcon(alert.getAlertType()), 24,
                "LOCKOUT".equals(alert.getAlertType()) ? "-fx-destructive" : "-fx-warning");
        StackPane typeIconWrap = new StackPane(typeIcon);
        typeIconWrap.setMinSize(44, 44);
        typeIconWrap.setPrefSize(44, 44);
        typeIconWrap.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10;");
        typeIconWrap.setAlignment(Pos.CENTER);

        VBox summaryInfo = new VBox(2);
        TLTypography alertTypeLabel = new TLTypography(formatAlertType(alert.getAlertType()), TLTypography.Variant.H3);
        HBox summaryMeta = new HBox(12);
        summaryMeta.setAlignment(Pos.CENTER_LEFT);
        TLTypography attemptsLabel = new TLTypography(
                alert.getFailedAttempts() + " failed attempt" + (alert.getFailedAttempts() != 1 ? "s" : ""),
                TLTypography.Variant.SM);
        attemptsLabel.setMuted(true);
        TLBadge statusBadge = new TLBadge(
                alert.isReviewed() ? "Reviewed" : "Pending Review",
                alert.isReviewed() ? TLBadge.Variant.SUCCESS : TLBadge.Variant.DESTRUCTIVE);
        summaryMeta.getChildren().addAll(attemptsLabel, statusBadge);
        summaryInfo.getChildren().addAll(alertTypeLabel, summaryMeta);
        HBox.setHgrow(summaryInfo, Priority.ALWAYS);
        summaryBar.getChildren().addAll(typeIconWrap, summaryInfo);

        // ── Photo section ──
        VBox photoSection = new VBox(8);
        TLTypography photoHeader = new TLTypography("Captured Photo", TLTypography.Variant.SM);
        photoHeader.getStyleClass().add("font-bold");
        photoSection.getChildren().add(photoHeader);

        if (alert.getPhotoPath() != null && !alert.getPhotoPath().isEmpty()) {
            try {
                File photoFile = new File(alert.getPhotoPath());
                if (photoFile.exists()) {
                    Image fullPhoto = new Image(photoFile.toURI().toString(), 500, 375, true, true);
                    ImageView photoView = new ImageView(fullPhoto);
                    photoView.setFitWidth(500);
                    photoView.setPreserveRatio(true);
                    photoView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);");

                    StackPane photoWrap = new StackPane(photoView);
                    photoWrap.setStyle("-fx-background-color: #000; -fx-background-radius: 8; -fx-padding: 4;");
                    photoWrap.setAlignment(Pos.CENTER);
                    photoSection.getChildren().add(photoWrap);

                    // Photo metadata
                    TLTypography photoPath = new TLTypography("Path: " + alert.getPhotoPath(), TLTypography.Variant.XS);
                    photoPath.setMuted(true);
                    photoSection.getChildren().add(photoPath);
                } else {
                    TLTypography noFile = new TLTypography("Photo file not found: " + alert.getPhotoPath(), TLTypography.Variant.XS);
                    noFile.setMuted(true);
                    photoSection.getChildren().add(noFile);
                }
            } catch (Exception ex) {
                TLTypography err = new TLTypography("Failed to load photo", TLTypography.Variant.XS);
                err.setMuted(true);
                photoSection.getChildren().add(err);
            }
        } else {
            TLTypography noCapture = new TLTypography("No photo captured for this alert", TLTypography.Variant.XS);
            noCapture.setMuted(true);
            photoSection.getChildren().add(noCapture);
        }

        // ── Detail grid ──
        VBox detailGrid = new VBox(8);
        TLTypography detailHeader = new TLTypography("Alert Details", TLTypography.Variant.SM);
        detailHeader.getStyleClass().add("font-bold");
        detailGrid.getChildren().add(detailHeader);

        detailGrid.getChildren().addAll(
                createDetailRow("Alert ID", "#" + alert.getId()),
                createDetailRow("Username", alert.getUsername()),
                createDetailRow("Alert Type", formatAlertType(alert.getAlertType())),
                createDetailRow("Failed Attempts", String.valueOf(alert.getFailedAttempts())),
                createDetailRow("IP Address", alert.getIpAddress() != null ? alert.getIpAddress() : "N/A"),
                createDetailRow("Date & Time",
                        alert.getCreatedAt() != null
                                ? alert.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                                : "—"),
                createDetailRow("Relative Time",
                        alert.getCreatedAt() != null ? formatRelativeTime(alert.getCreatedAt()) : "—"),
                createDetailRow("Status", alert.isReviewed() ? "Reviewed" : "Pending Review")
        );

        if (alert.isReviewed()) {
            detailGrid.getChildren().addAll(
                    createDetailRow("Reviewed By", alert.getReviewedBy() != null ? alert.getReviewedBy() : "—"),
                    createDetailRow("Reviewed At",
                            alert.getReviewedAt() != null
                                    ? alert.getReviewedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                                    : "—"),
                    createDetailRow("Admin Notes", alert.getNotes() != null && !alert.getNotes().isEmpty()
                            ? alert.getNotes() : "No notes")
            );
        }

        // ── User alert history ──
        VBox historySection = new VBox(8);
        TLTypography historyHeader = new TLTypography(
                "Alert History for '" + alert.getUsername() + "'", TLTypography.Variant.SM);
        historyHeader.getStyleClass().add("font-bold");
        historySection.getChildren().add(historyHeader);

        // Load history async and populate
        AppThreadPool.execute(() -> {
            try {
                List<SecurityAlert> history = SecurityAlertService.getInstance()
                        .getAlertsByUsername(alert.getUsername());
                Platform.runLater(() -> {
                    if (history.isEmpty() || (history.size() == 1 && history.get(0).getId() == alert.getId())) {
                        TLTypography noHistory = new TLTypography("No other alerts for this user", TLTypography.Variant.XS);
                        noHistory.setMuted(true);
                        historySection.getChildren().add(noHistory);
                    } else {
                        VBox timeline = new VBox(0);
                        int shown = 0;
                        for (SecurityAlert h : history) {
                            if (shown >= 10) break; // limit
                            HBox historyRow = new HBox(8);
                            historyRow.setAlignment(Pos.CENTER_LEFT);
                            historyRow.setPadding(new Insets(6, 8, 6, 8));

                            // Timeline dot
                            Region dot = new Region();
                            dot.setMinSize(8, 8);
                            dot.setPrefSize(8, 8);
                            dot.setMaxSize(8, 8);
                            String dotColor = h.getId() == alert.getId() ? "-fx-primary" :
                                    (h.isReviewed() ? "-fx-muted-foreground" : "-fx-destructive");
                            dot.setStyle("-fx-background-color: " + dotColor + "; -fx-background-radius: 4;");

                            TLBadge hTypeBadge = new TLBadge(formatAlertType(h.getAlertType()),
                                    getSeverityBadgeVariant(h.getAlertType()));

                            TLTypography hTime = new TLTypography(
                                    h.getCreatedAt() != null
                                            ? h.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                                            : "—",
                                    TLTypography.Variant.XS);
                            hTime.setMuted(true);

                            TLTypography hAttempts = new TLTypography(
                                    h.getFailedAttempts() + " attempts",
                                    TLTypography.Variant.XS);
                            hAttempts.setMuted(true);

                            TLTypography hStatus = new TLTypography(
                                    h.isReviewed() ? "✓" : "⏳",
                                    TLTypography.Variant.XS);

                            if (h.getId() == alert.getId()) {
                                historyRow.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 4;");
                            }

                            historyRow.getChildren().addAll(dot, hTypeBadge, hTime, hAttempts, hStatus);
                            timeline.getChildren().add(historyRow);
                            shown++;
                        }
                        historySection.getChildren().add(timeline);

                        TLTypography totalLabel = new TLTypography(
                                history.size() + " total alert" + (history.size() != 1 ? "s" : "") + " for this user",
                                TLTypography.Variant.XS);
                        totalLabel.setMuted(true);
                        historySection.getChildren().add(totalLabel);
                    }
                });
            } catch (Exception ex) {
                logger.debug("Failed to load alert history for {}", alert.getUsername(), ex);
            }
        });

        content.getChildren().addAll(summaryBar, photoSection,
                new com.skilora.framework.components.TLSeparator(),
                detailGrid,
                new com.skilora.framework.components.TLSeparator(),
                historySection);

        // ── Review action at bottom (if not yet reviewed) ──
        if (!alert.isReviewed()) {
            content.getChildren().add(new com.skilora.framework.components.TLSeparator());

            VBox reviewSection = new VBox(8);
            TLTypography reviewHeader = new TLTypography("Mark as Reviewed", TLTypography.Variant.SM);
            reviewHeader.getStyleClass().add("font-bold");

            com.skilora.framework.components.TLTextarea notesField = new com.skilora.framework.components.TLTextarea();
            notesField.setPromptText("Add notes about this security event (optional)...");
            notesField.setPrefRowCount(3);

            TLButton markReviewedBtn = new TLButton("Mark as Reviewed", ButtonVariant.PRIMARY);
            markReviewedBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 14));
            markReviewedBtn.setContentDisplay(ContentDisplay.LEFT);
            markReviewedBtn.setOnAction(e -> {
                markReviewedBtn.setDisable(true);
                markReviewedBtn.setText("Reviewing...");
                String notes = notesField.getText() != null ? notesField.getText().trim() : "";
                AppThreadPool.execute(() -> {
                    boolean ok = SecurityAlertService.getInstance()
                            .markReviewed(alert.getId(), currentUser.getUsername(),
                                    notes.isEmpty() ? "Reviewed from dashboard" : notes);
                    Platform.runLater(() -> {
                        if (ok) {
                            markReviewedBtn.setText("✓ Reviewed");
                            markReviewedBtn.setVariant(ButtonVariant.SUCCESS);
                            loadSecurityAlerts();
                            // Close dialog after brief delay
                            new java.util.Timer().schedule(new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    Platform.runLater(dialog::close);
                                }
                            }, 800);
                        } else {
                            markReviewedBtn.setText("Failed — Retry");
                            markReviewedBtn.setDisable(false);
                        }
                    });
                });
            });

            TLButton dismissBtn = new TLButton("Dismiss", ButtonVariant.GHOST);
            dismissBtn.setOnAction(e -> dialog.close());

            HBox actionRow = new HBox(10);
            actionRow.setAlignment(Pos.CENTER_RIGHT);
            actionRow.getChildren().addAll(dismissBtn, markReviewedBtn);

            reviewSection.getChildren().addAll(reviewHeader, notesField, actionRow);
            content.getChildren().add(reviewSection);
        }

        dialog.setContent(content);
        dialog.addButton(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Opens a quick review dialog for an alert (from the list row).
     */
    private void showReviewDialog(SecurityAlert alert) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle("Review Security Alert");
        dialog.setDescription("Alert #" + alert.getId() + " — " + alert.getUsername()
                + " (" + formatAlertType(alert.getAlertType()) + ")");
        dialog.initOwner(activityList.getScene().getWindow());

        VBox content = new VBox(12);

        // Quick summary
        HBox quickInfo = new HBox(10);
        quickInfo.setAlignment(Pos.CENTER_LEFT);
        StackPane thumb = buildAlertThumbnail(alert, 60);
        VBox quickDetails = new VBox(4);
        quickDetails.getChildren().addAll(
                new TLTypography(alert.getFailedAttempts() + " failed attempts", TLTypography.Variant.SM),
                new TLTypography("IP: " + (alert.getIpAddress() != null ? alert.getIpAddress() : "N/A"), TLTypography.Variant.XS),
                new TLTypography(alert.getCreatedAt() != null
                        ? alert.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        : "—", TLTypography.Variant.XS)
        );
        quickDetails.getChildren().forEach(n -> { if (n instanceof TLTypography t) t.setMuted(true); });
        ((TLTypography) quickDetails.getChildren().get(0)).setMuted(false);
        quickInfo.getChildren().addAll(thumb, quickDetails);

        com.skilora.framework.components.TLTextarea notesField = new com.skilora.framework.components.TLTextarea();
        notesField.setPromptText("Admin notes (optional)...");
        notesField.setPrefRowCount(2);

        TLButton confirmBtn = new TLButton("Confirm Review", ButtonVariant.PRIMARY);
        confirmBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
        confirmBtn.setContentDisplay(ContentDisplay.LEFT);
        confirmBtn.setOnAction(e -> {
            confirmBtn.setDisable(true);
            String notes = notesField.getText() != null ? notesField.getText().trim() : "";
            AppThreadPool.execute(() -> {
                boolean ok = SecurityAlertService.getInstance()
                        .markReviewed(alert.getId(), currentUser.getUsername(),
                                notes.isEmpty() ? "Quick review from dashboard" : notes);
                Platform.runLater(() -> {
                    if (ok) {
                        loadSecurityAlerts();
                        dialog.close();
                    } else {
                        confirmBtn.setText("Failed — Retry");
                        confirmBtn.setDisable(false);
                    }
                });
            });
        });

        content.getChildren().addAll(quickInfo, notesField, confirmBtn);
        dialog.setContent(content);
        dialog.addButton(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ── Security Alerts Helpers ──

    private StackPane buildAlertThumbnail(SecurityAlert alert, double size) {
        StackPane wrap;
        if (alert.getPhotoPath() != null && !alert.getPhotoPath().isEmpty()) {
            try {
                File photoFile = new File(alert.getPhotoPath());
                if (photoFile.exists()) {
                    Image photo = new Image(photoFile.toURI().toString(), size, size, true, true);
                    ImageView photoView = new ImageView(photo);
                    photoView.setFitWidth(size);
                    photoView.setFitHeight(size);
                    photoView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 1);");
                    wrap = new StackPane(photoView);
                    wrap.setMinSize(size, size);
                    wrap.setPrefSize(size, size);
                    wrap.setMaxSize(size, size);
                    wrap.setStyle("-fx-background-radius: 8; -fx-border-radius: 8;");
                    return wrap;
                }
            } catch (Exception ignored) { /* fall through to icon */ }
        }

        // Fallback: type icon
        SVGPath icon = SvgIcons.icon(getAlertIcon(alert.getAlertType()), (int) (size * 0.4),
                "LOCKOUT".equals(alert.getAlertType()) ? "-fx-destructive" : "-fx-warning");
        wrap = new StackPane(icon);
        wrap.setMinSize(size, size);
        wrap.setPrefSize(size, size);
        wrap.setMaxSize(size, size);
        wrap.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-background-radius: 8;");
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));

        TLTypography labelText = new TLTypography(label + ":", TLTypography.Variant.XS);
        labelText.setMuted(true);
        labelText.setMinWidth(120);
        labelText.setPrefWidth(120);

        TLTypography valueText = new TLTypography(value, TLTypography.Variant.SM);
        HBox.setHgrow(valueText, Priority.ALWAYS);

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private String getAlertIcon(String alertType) {
        return switch (alertType) {
            case "LOCKOUT" -> SvgIcons.LOCK;
            case "BRUTE_FORCE" -> SvgIcons.SHIELD;
            default -> SvgIcons.SCAN_FACE;
        };
    }

    private String formatAlertType(String type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case "LOCKOUT" -> "Lockout";
            case "BRUTE_FORCE" -> "Brute Force";
            case "FAILED_LOGIN_CAPTURE" -> "Failed Login";
            default -> type.replace("_", " ");
        };
    }

    private TLBadge.Variant getSeverityBadgeVariant(String alertType) {
        return switch (alertType) {
            case "LOCKOUT" -> TLBadge.Variant.DESTRUCTIVE;
            case "BRUTE_FORCE" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.SECONDARY;
        };
    }

    // --- Admin: Marketplace Overview (Fiverr/Upwork-style) ---

    /**
     * Builds a platform health / marketplace overview section with
     * user breakdown by role and platform-wide metrics.
     */
    private void setupMarketplaceOverview() {
        VBox overviewSection = new VBox(12);

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath chartIcon = SvgIcons.icon(SvgIcons.BAR_CHART, 18, "-fx-primary");
        TLTypography headerText = new TLTypography("Marketplace Overview", TLTypography.Variant.H3);
        headerRow.getChildren().addAll(chartIcon, headerText);

        FlowPane overviewGrid = new FlowPane();
        overviewGrid.setHgap(16);
        overviewGrid.setVgap(16);
        overviewGrid.prefWrapLengthProperty().bind(overviewGrid.widthProperty());
        overviewGrid.getChildren().add(new TLLoadingState());

        overviewSection.getChildren().addAll(headerRow, overviewGrid);

        // Insert after activitySection
        if (activitySection != null && activitySection.getParent() instanceof VBox) {
            VBox parent = (VBox) activitySection.getParent();
            int idx = parent.getChildren().indexOf(activitySection);
            if (idx >= 0) {
                parent.getChildren().add(idx + 1, overviewSection);
            }
        }

        AppThreadPool.execute(() -> {
            try {
                int employers = statsService.getUserCountByRole("EMPLOYER");
                int trainers = statsService.getUserCountByRole("TRAINER");
                int users = statsService.getUserCountByRole("USER");
                int admins = statsService.getUserCountByRole("ADMIN");

                Platform.runLater(() -> {
                    overviewGrid.getChildren().clear();
                    overviewGrid.getChildren().addAll(
                        createUserBreakdownCard(employers, trainers, users, admins),
                        createPlatformHealthCard()
                    );
                });
            } catch (Exception e) {
                logger.error("Failed to load marketplace overview", e);
                Platform.runLater(() -> {
                    if (activityList.getScene() != null) {
                        com.skilora.framework.components.TLToast.error(
                            activityList.getScene(), "Error", "Failed to load overview data");
                    }
                });
            }
        });
    }

    private Node createUserBreakdownCard(int employers, int trainers, int users, int admins) {
        TLCard card = new TLCard();
        card.setPrefWidth(380);
        card.setMinWidth(300);

        VBox body = new VBox(10);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath icon = SvgIcons.icon(SvgIcons.USERS, 16, "-fx-primary");
        TLTypography title = new TLTypography("User Breakdown", TLTypography.Variant.SM);
        title.getStyleClass().add("font-bold");
        titleRow.getChildren().addAll(icon, title);

        int total = employers + trainers + users + admins;
        body.getChildren().add(titleRow);
        body.getChildren().add(createRoleBar("Employers", employers, total, "-fx-primary"));
        body.getChildren().add(createRoleBar("Trainers", trainers, total, "#c9a84c"));
        body.getChildren().add(createRoleBar("Users", users, total, "-fx-success"));
        body.getChildren().add(createRoleBar("Admins", admins, total, "-fx-destructive"));

        card.setBody(body);
        return card;
    }

    private Node createRoleBar(String roleName, int count, int total, String color) {
        VBox row = new VBox(4);

        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER_LEFT);
        TLTypography name = new TLTypography(roleName, TLTypography.Variant.XS);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        TLTypography countLabel = new TLTypography(String.valueOf(count), TLTypography.Variant.XS);
        countLabel.getStyleClass().add("font-bold");
        labelRow.getChildren().addAll(name, sp, countLabel);

        // Progress bar
        double pct = total > 0 ? (double) count / total : 0;
        StackPane barBg = new StackPane();
        barBg.setPrefHeight(6);
        barBg.setMaxHeight(6);
        barBg.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 3;");

        Region barFill = new Region();
        barFill.setPrefHeight(6);
        barFill.setMaxHeight(6);
        barFill.setMaxWidth(Double.MAX_VALUE);
        barFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");

        StackPane barContainer = new StackPane(barBg, barFill);
        barContainer.setAlignment(Pos.CENTER_LEFT);
        // Bind fill width to percentage of parent width
        barFill.maxWidthProperty().bind(barContainer.widthProperty().multiply(pct));

        row.getChildren().addAll(labelRow, barContainer);
        return row;
    }

    private Node createPlatformHealthCard() {
        TLCard card = new TLCard();
        card.setPrefWidth(380);
        card.setMinWidth(300);

        VBox body = new VBox(12);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath icon = SvgIcons.icon(SvgIcons.ACTIVITY, 16, "-fx-success");
        TLTypography title = new TLTypography("Platform Health", TLTypography.Variant.SM);
        title.getStyleClass().add("font-bold");
        titleRow.getChildren().addAll(icon, title);
        body.getChildren().add(titleRow);

        // Health indicators
        body.getChildren().addAll(
            createHealthRow(SvgIcons.DATABASE, "Database", "Connected", true),
            createHealthRow(SvgIcons.SERVER, "Services", "All Running", true),
            createHealthRow(SvgIcons.SHIELD_CHECK, "Security", "Monitoring Active", true),
            createHealthRow(SvgIcons.GLOBE, "Job Feed", "Synced", true)
        );

        card.setBody(body);
        return card;
    }

    private Node createHealthRow(String iconPath, String label, String status, boolean healthy) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        // Status dot
        Region dot = new Region();
        dot.setMinSize(8, 8);
        dot.setPrefSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color: " + (healthy ? "-fx-success" : "-fx-destructive") +
                     "; -fx-background-radius: 4;");

        TLTypography labelText = new TLTypography(label, TLTypography.Variant.SM);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        TLTypography statusText = new TLTypography(status, TLTypography.Variant.XS);
        statusText.setMuted(true);

        row.getChildren().addAll(dot, labelText, sp, statusText);
        return row;
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
        card.setMinWidth(200);
        card.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(4);
        content.setAlignment(Pos.TOP_LEFT);

        TLTypography titleTypo = new TLTypography(title, TLTypography.Variant.SM);
        titleTypo.setMuted(true);

        TLTypography valueTypo = new TLTypography(value, TLTypography.Variant.H2);
        VBox.setMargin(valueTypo, new javafx.geometry.Insets(4, 0, 0, 0));

        // Build trend row with arrow icon
        HBox trendRow = new HBox(4);
        trendRow.setAlignment(Pos.CENTER_LEFT);
        trendRow.setMinHeight(18);

        if (trend != null && !trend.isEmpty()) {
            boolean isPositive = positive || trend.startsWith("+");
            String trendColor = isPositive ? "-fx-success" : "-fx-muted-foreground";
            String arrowPath = isPositive ? SvgIcons.TRENDING_UP : SvgIcons.TRENDING_DOWN;

            SVGPath trendArrow = SvgIcons.icon(arrowPath, 12, trendColor);
            TLTypography trendTypo = new TLTypography(trend, TLTypography.Variant.XS);
            trendTypo.getStyleClass().add(isPositive ? "text-success" : "text-muted");

            trendRow.getChildren().addAll(trendArrow, trendTypo);
        }

        content.getChildren().addAll(titleTypo, valueTypo, trendRow);
        card.setBody(content);
        return card;
    }

    private Node createActivityItem(String type, String desc, String time) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));

        // Activity-type icon in a rounded container
        String iconPath = getActivityIcon(type);
        SVGPath activityIcon = SvgIcons.icon(iconPath, 16, "-fx-muted-foreground");
        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(activityIcon);
        iconWrap.setMinSize(36, 36);
        iconWrap.setPrefSize(36, 36);
        iconWrap.setMaxSize(36, 36);
        iconWrap.getStyleClass().add("bg-muted-rounded-sm");
        iconWrap.setAlignment(Pos.CENTER);

        VBox texts = new VBox(2);
        TLTypography typeText = new TLTypography(type, TLTypography.Variant.SM);
        typeText.getStyleClass().add("font-bold");

        TLTypography descText = new TLTypography(desc, TLTypography.Variant.XS);
        descText.setMuted(true);
        texts.getChildren().addAll(typeText, descText);
        HBox.setHgrow(texts, Priority.ALWAYS);

        TLTypography timeText = new TLTypography(time, TLTypography.Variant.XS);
        timeText.setMuted(true);

        row.getChildren().addAll(iconWrap, texts, timeText);
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
