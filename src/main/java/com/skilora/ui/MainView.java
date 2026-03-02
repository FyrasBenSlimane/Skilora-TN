package com.skilora.ui;

import com.skilora.framework.layouts.TLAppLayout;
import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.user.entity.User;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import com.skilora.user.service.UserService;
import com.skilora.user.service.PreferencesService;
import com.skilora.user.controller.LoginController;
import com.skilora.user.controller.UserFormController;
import com.skilora.user.controller.SettingsController;
import com.skilora.formation.controller.FormationsController;
import com.skilora.formation.controller.CertificationsController;
import com.skilora.recruitment.controller.ActiveOffersController;
import com.skilora.user.ui.ProfileWizardView;
import com.skilora.user.ui.PublicProfileView;
import com.skilora.community.service.NotificationService;
import com.skilora.security.AuditLogService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import com.skilora.user.enums.Capability;
import com.skilora.user.enums.Role;
import com.skilora.user.service.PermissionService;
import com.skilora.user.service.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Circle;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.geometry.Side;
import javafx.geometry.Insets;
import javafx.concurrent.Task;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MainView - Component showcase + dashboard. All TL components in a scrollable
 * area.
 */
public class MainView extends TLAppLayout {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final User currentUser;
    private final UserService userService;

    private StackPane centerStack;
    private TLAvatar sidebarAvatar; // Promoted to field

    // TopBar items – stored as fields for in-place i18n refresh
    private Label appTitleLabel;
    private Button themeToggleBtn;
    private StackPane notifBellPane;
    private Label notifBadge;

    // Cached Views for Performance
    private Node cachedFeedView;
    private com.skilora.recruitment.controller.FeedController cachedFeedController;
    private TLScrollArea cachedProfileScroll;
    private Node cachedDashboardView;
    private Node cachedSettingsView;
    private Node cachedUsersView;
    private Node cachedApplicationsView;
    private Node cachedNotificationsView;
    private Node cachedPostJobView;
    private Node cachedApplicationInboxView;
    private Node cachedMyOffersView;
    private Node cachedActiveOffersView;
    private Node cachedFormationsView;
    private Node cachedReportsView;
    private Node cachedInterviewsView;
    private Node cachedSupportView;
    private Node cachedSupportAdminView;
    private Node cachedCommunityView;
    private Node cachedFinanceView;
    private Node cachedCertificationsView;

    // Support notification dot
    private Circle supportNotificationDot;

    // Active nav button tracking
    private TLButton activeNavButton;

    // Navigation history for "Go Back" support
    private final Deque<Runnable> navigationHistory = new ArrayDeque<>();
    private Runnable currentViewAction;

    public MainView(User user) {
        super();
        this.currentUser = user;
        this.userService = UserService.getInstance();

        // Load saved user preferences on login
        loadUserPreferences();

        setupTopBar();
        setupSidebar();

        // Initial drawer setup - kept for reference if we re-enable the separate drawer
        // logic
        // currently using integrated sidebar
        this.centerStack = new StackPane();

        // Main Dashboard Content
        showDashboard();

        setContent(centerStack);
    }

    /**
     * Load and apply saved user preferences (theme, language) on login.
     */
    private void loadUserPreferences() {
        try {
            PreferencesService prefService = PreferencesService.getInstance();
            java.util.Map<String, String> prefs = prefService.getAll(currentUser.getId());

            // Apply language
            if (prefs.containsKey(PreferencesService.KEY_LANGUAGE)) {
                I18n.setLocaleFromDisplayName(prefs.get(PreferencesService.KEY_LANGUAGE));
            }

            // Apply dark mode
            if (prefs.containsKey(PreferencesService.KEY_DARK_MODE)) {
                boolean dark = Boolean.parseBoolean(prefs.get(PreferencesService.KEY_DARK_MODE));
                setDarkMode(dark);
            }
        } catch (Exception e) {
            logger.debug("Could not load user preferences: {}", e.getMessage());
        }
    }

    /**
     * Build the topbar once: [appTitle, spacer, themeToggle].
     * Window controls (min/max/close) are added later by setupWindowControls().
     * Call refreshTopBarLabels() to update text after an i18n change.
     */
    private void setupTopBar() {
        appTitleLabel = new Label();
        appTitleLabel.getStyleClass().add("h4");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Notification bell with badge
        Button notifBtn = new Button();
        notifBtn.setGraphic(SvgIcons.icon(SvgIcons.BELL, 16));
        notifBtn.setTooltip(new Tooltip("Notifications"));
        notifBtn.getStyleClass().addAll("topbar-icon-btn");
        notifBtn.setOnAction(e -> showNotificationsView());

        notifBadge = new Label();
        notifBadge.getStyleClass().add("notif-badge");
        notifBadge.setVisible(false);
        notifBadge.setMouseTransparent(true);

        notifBellPane = new StackPane(notifBtn, notifBadge);
        notifBellPane.setMaxSize(34, 34);
        notifBellPane.setMinSize(34, 34);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(notifBadge, new Insets(-2, -2, 0, 0));

        themeToggleBtn = new Button();
        themeToggleBtn.getStyleClass().add("theme-toggle");
        updateThemeToggleIcon(themeToggleBtn, isDarkMode());
        themeToggleBtn.setOnAction(e -> {
            toggleTheme();
            updateThemeToggleIcon(themeToggleBtn, isDarkMode());
        });

        refreshTopBarLabels();
        refreshNotificationBadge();

        HBox rightGroup = new HBox(8);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);
        rightGroup.getStyleClass().add("topbar-actions");
        rightGroup.getChildren().addAll(notifBellPane, themeToggleBtn);

        getTopBar().getChildren().addAll(appTitleLabel, spacer, rightGroup);
    }

    private void refreshNotificationBadge() {
        if (currentUser == null) return;
        AppThreadPool.execute(() -> {
            int count = NotificationService.getInstance().getUnreadCount(currentUser.getId());
            javafx.application.Platform.runLater(() -> {
                if (count > 0) {
                    notifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    notifBadge.setVisible(true);
                } else {
                    notifBadge.setVisible(false);
                }
            });
        });
    }

    /**
     * Update topbar labels in-place (no add/remove) after an i18n locale change.
     */
    private void refreshTopBarLabels() {
        String roleTitle;
        switch (currentUser.getRole()) {
            case ADMIN:
                roleTitle = I18n.get("app.admin");
                break;
            case EMPLOYER:
                roleTitle = I18n.get("app.employer");
                break;
            case TRAINER:
                roleTitle = I18n.get("app.trainer");
                break;
            default:
                roleTitle = I18n.get("app.talent");
                break;
        }
        appTitleLabel.setText(roleTitle);
        updateThemeToggleIcon(themeToggleBtn, isDarkMode());
    }

    /**
     * Build (or rebuild) the sidebar: nav menu + profile section.
     * Always call getSidebar().getChildren().clear() before this when rebuilding.
     */
    private void setupSidebar() {
        // Hamburger toggle (inlined with "MENU PRINCIPAL" to save vertical space)
        Button toggleSidebarBtn = new Button();
        toggleSidebarBtn.getStyleClass().addAll("btn-ghost", "sidebar-toggle-btn");
        SVGPath toggleIcon = new SVGPath();
        toggleIcon.setContent(SvgIcons.MENU);
        toggleIcon.getStyleClass().add("svg-path");
        StackPane iconWrap = new StackPane(toggleIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.getStyleClass().add("items-center");
        toggleSidebarBtn.setGraphic(iconWrap);
        toggleSidebarBtn.setTooltip(new Tooltip("Toggle sidebar"));
        toggleSidebarBtn.setMinSize(36, 36);
        toggleSidebarBtn.setPrefSize(36, 36);

        TLTypography navTitle = new TLTypography(I18n.get("nav.menu"), TLTypography.Variant.SM);
        navTitle.setMuted(true);
        navTitle.getStyleClass().add("sidebar-title");
        navTitle.setMaxWidth(Double.MAX_VALUE);
        navTitle.setMinWidth(Region.USE_PREF_SIZE);

        HBox.setHgrow(navTitle, Priority.ALWAYS);
        HBox menuHeaderRow = new HBox(navTitle, toggleSidebarBtn);
        menuHeaderRow.setAlignment(Pos.CENTER_LEFT);
        menuHeaderRow.getStyleClass().add("sidebar-menu-header-row");
        menuHeaderRow.setSpacing(6);

        VBox navMenu = new VBox(4);
        navMenu.getStyleClass().add("nav-menu");

        // Dynamic Menu Items based on Role
        navMenu.getChildren().add(menuHeaderRow);

        switch (currentUser.getRole()) {
            case ADMIN:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                SvgIcons.LAYOUT_DASHBOARD, this::showDashboard),
                        createNavButton(I18n.get("nav.users"),
                                SvgIcons.USERS, this::showUsersView),
                        createNavButton(I18n.get("nav.active_offers"),
                                SvgIcons.BRIEFCASE, this::showActiveOffersView),
                        createNavButton(I18n.get("nav.reports"),
                                SvgIcons.ALERT_TRIANGLE, this::showReportsView),
                        createNavButton(I18n.get("nav.community"),
                                SvgIcons.USERS, this::showCommunityView),
                        createNavButton(I18n.get("nav.formations"),
                                SvgIcons.GRADUATION_CAP, this::showFormationAdminView),
                        createNavButton(I18n.get("nav.finance"),
                                SvgIcons.DOLLAR_SIGN, this::showFinanceView),
                        createSupportNavButton());
                break;
            case EMPLOYER:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                SvgIcons.LAYOUT_DASHBOARD, this::showDashboard),
                        createNavButton(I18n.get("nav.post_job"),
                                SvgIcons.PLUS, this::showPostJobView),
                        createNavButton(I18n.get("nav.my_offers"),
                                SvgIcons.BRIEFCASE, this::showMyOffersView),
                        createNavButton(I18n.get("nav.inbox"),
                                SvgIcons.USER, this::showApplicationInboxView),
                        createNavButton(I18n.get("nav.interviews"),
                                SvgIcons.VIDEO, this::showInterviewsView),
                        createNavButton(I18n.get("nav.community"),
                                SvgIcons.USERS, this::showCommunityView),
                        createNavButton(I18n.get("nav.finance"),
                                SvgIcons.DOLLAR_SIGN, this::showFinanceView),
                        createSupportNavButton());
                break;
            case TRAINER:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                SvgIcons.LAYOUT_DASHBOARD, this::showDashboard),
                        createNavButton(I18n.get("nav.formations"),
                                SvgIcons.GRADUATION_CAP, this::showFormationAdminView),
                        createNavButton(I18n.get("nav.mentorship"),
                                SvgIcons.HEART, this::showMentorshipView),
                        createNavButton(I18n.get("nav.community"),
                                SvgIcons.USERS, this::showCommunityView),
                        createNavButton(I18n.get("nav.finance"),
                                SvgIcons.DOLLAR_SIGN, this::showFinanceView),
                        createSupportNavButton());
                break;
            case USER:
            default:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                SvgIcons.LAYOUT_DASHBOARD, this::showDashboard),
                        createNavButton(I18n.get("nav.feed"),
                                SvgIcons.NEWSPAPER, this::showFeedView),
                        createNavButton(I18n.get("nav.applications"),
                                SvgIcons.FILE_TEXT, this::showApplicationsView),
                        createNavButton(I18n.get("nav.community"),
                                SvgIcons.USERS, this::showCommunityView),
                        createNavButton(I18n.get("nav.formations"),
                                SvgIcons.GRADUATION_CAP, this::showFormationsView),
                        createNavButton(I18n.get("nav.certifications"),
                                SvgIcons.CHECK_CIRCLE, this::showCertificationsView),
                        createNavButton(I18n.get("nav.finance"),
                                SvgIcons.DOLLAR_SIGN, this::showFinanceView),
                        createSupportNavButton());
                break;
        }

        // User Profile Section (Bottom of Sidebar) – wrapped so avatar is centered when
        // collapsed
        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        String userName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName()
                : I18n.get("app.default_user");
        String initials = getInitialsFromDisplayName(userName);

        sidebarAvatar = new TLAvatar(initials);
        updateSidebarAvatar();

        sidebarAvatar.setPrefSize(36, 36);
        sidebarAvatar.setMinSize(36, 36);
        sidebarAvatar.setMaxSize(36, 36);

        TLButton profileBtn = new TLButton(userName, ButtonVariant.GHOST);
        profileBtn.setGraphic(sidebarAvatar);
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        profileBtn.setAlignment(Pos.CENTER_LEFT);
        profileBtn.setFocusTraversable(false);
        profileBtn.getStyleClass().add("profile-btn");
        profileBtn.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        profileBtn.setEllipsisString("\u2026");

        Region profileSpacerLeft = new Region();
        Region profileSpacerRight = new Region();
        HBox profileWrap = new HBox(profileSpacerLeft, profileBtn, profileSpacerRight);
        profileWrap.getStyleClass().add("sidebar-profile-wrap");
        profileWrap.setAlignment(Pos.CENTER_LEFT);
        profileWrap.setMinHeight(48);
        profileWrap.setPrefHeight(48);
        profileWrap.setVisible(true);
        profileWrap.setManaged(true);
        HBox.setHgrow(profileBtn, Priority.ALWAYS);
        HBox.setHgrow(profileSpacerLeft, Priority.NEVER);
        HBox.setHgrow(profileSpacerRight, Priority.NEVER);

        TLDropdownMenu profileMenu = new TLDropdownMenu();
        profileMenu.addItem(I18n.get("menu.profile"), e -> showProfileView());
        profileMenu.addItem(I18n.get("menu.settings"), e -> showSettingsView());
        profileMenu.addItem(I18n.get("menu.help"), e -> HelpDialog.show(getScene().getWindow()));
        profileMenu.addItem(I18n.get("menu.logout"), e -> handleLogout());

        profileBtn.setOnAction(e -> profileMenu.showWithinWindow(profileBtn, Side.RIGHT, 8));

        toggleSidebarBtn.setOnAction(e -> {
            toggleSidebar();
            boolean collapsed = isSidebarCollapsed();
            navTitle.setVisible(!collapsed);
            navTitle.setManaged(!collapsed);
            menuHeaderRow.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            navMenu.setAlignment(collapsed ? Pos.CENTER : Pos.TOP_LEFT);
            profileWrap.setVisible(true);
            profileWrap.setManaged(true);
            profileWrap.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            profileWrap.setMinHeight(48);
            HBox.setHgrow(profileSpacerLeft, collapsed ? Priority.ALWAYS : Priority.NEVER);
            HBox.setHgrow(profileSpacerRight, collapsed ? Priority.ALWAYS : Priority.NEVER);
            HBox.setHgrow(profileBtn, collapsed ? Priority.NEVER : Priority.ALWAYS);
            profileBtn.setVisible(true);
            profileBtn.setManaged(true);
            profileBtn.setContentDisplay(collapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            profileBtn.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            profileBtn.setMaxWidth(collapsed ? 48 : Double.MAX_VALUE);
            profileBtn.setMinWidth(collapsed ? 48 : 0);
            for (Node n : navMenu.getChildren()) {
                if (n instanceof Button) {
                    Button b = (Button) n;
                    b.setContentDisplay(collapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
                    b.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                    b.setMaxWidth(collapsed ? 44 : Double.MAX_VALUE);
                    b.setMinWidth(collapsed ? 44 : 0);
                }
            }
        });

        getSidebar().getChildren().addAll(navMenu, sidebarSpacer, profileWrap);
        VBox.setVgrow(profileWrap, Priority.NEVER);
    }

    private static void updateThemeToggleIcon(Button btn, boolean darkMode) {
        // Sun icon for dark mode (click to go light), moon icon for light mode (click to go dark)
        SVGPath icon = new SVGPath();
        icon.setContent(darkMode ? SvgIcons.SUN : SvgIcons.MOON);
        icon.getStyleClass().add("svg-path");

        StackPane iconWrap = new StackPane(icon);
        iconWrap.setMinSize(20, 20);
        iconWrap.setPrefSize(20, 20);
        iconWrap.setMaxSize(20, 20);

        btn.setGraphic(iconWrap);
        btn.setText(null);
        btn.setTooltip(new Tooltip(darkMode ? I18n.get("theme.light") : I18n.get("theme.dark")));
    }

    /**
     * Default initials from display name when no avatar image is available (e.g.
     * "Admin User" → "AU").
     */
    private void updateSidebarAvatar() {
        if (sidebarAvatar == null)
            return;

        String initials = getInitialsFromDisplayName(
                currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
        sidebarAvatar.setFallback(initials);

        javafx.scene.image.Image img = com.skilora.utils.ImageUtils.loadProfileImage(
                currentUser.getPhotoUrl(), 36, 36);
        if (img != null) {
            sidebarAvatar.setImage(img);
        } else {
            sidebarAvatar.setImage(null);
        }
    }

    private static String getInitialsFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank())
            return "?";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length >= 2) {
            String a = parts[0];
            String b = parts[parts.length - 1];
            return ((a.isEmpty() ? "" : a.substring(0, 1)) + (b.isEmpty() ? "" : b.substring(0, 1))).toUpperCase();
        }
        return displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
    }

    private TLButton createNavButton(String text, String svgPath, Runnable action) {
        TLButton btn = new TLButton(text, ButtonVariant.GHOST);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFocusTraversable(false);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("svg-path");
        icon.setScaleX(0.85); // Scale down slightly to fit 24x24 box implicitly
        icon.setScaleY(0.85);
        btn.setGraphic(icon);

        if (action != null) {
            btn.setOnAction(e -> {
                setActiveNavButton(btn);
                action.run();
            });
        }
        return btn;
    }

    /** Highlights the given nav button and clears the previous one. */
    private void setActiveNavButton(TLButton btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-active");
        }
        activeNavButton = btn;
        if (btn != null && !btn.getStyleClass().contains("nav-active")) {
            btn.getStyleClass().add("nav-active");
        }
    }

    private javafx.scene.Node createSupportNavButton() {
        TLButton btn = createNavButton(I18n.get("nav.support"),
                SvgIcons.MESSAGE_SQUARE,
                this::showSupportView);

        supportNotificationDot = new Circle(4);
        supportNotificationDot.getStyleClass().add("icon-red");
        supportNotificationDot.setVisible(false);
        supportNotificationDot.setManaged(false);

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(btn, supportNotificationDot);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.StackPane.setAlignment(supportNotificationDot, Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(supportNotificationDot, new Insets(8, 10, 0, 0));

        checkSupportUnread();

        return wrapper;
    }

    private void checkSupportUnread() {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    var user = currentUser;
                    if (user == null) return false;
                    var service = com.skilora.support.service.SupportTicketService.getInstance();
                    if (user.getRole() == com.skilora.user.enums.Role.ADMIN) {
                        var tickets = service.findAll();
                        return tickets != null && tickets.stream()
                                .anyMatch(t -> t.getStatus() != null &&
                                        (t.getStatus().equals("OPEN") || t.getStatus().equals("IN_PROGRESS")));
                    } else {
                        var tickets = service.findByUserId(user.getId());
                        return tickets != null && !tickets.isEmpty() &&
                                tickets.stream().anyMatch(t -> t.getStatus() != null &&
                                        !t.getStatus().equals("CLOSED"));
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        };
        task.setOnSucceeded(e -> {
            boolean hasUnread = task.getValue();
            if (supportNotificationDot != null) {
                supportNotificationDot.setVisible(hasUnread);
            }
        });
        AppThreadPool.execute(task);
    }

    private void showDashboard() {
        pushNavigation(this::showDashboard);
        if (cachedDashboardView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/DashboardView.fxml"));
                VBox dashboardContent = loader.load();
                
                // Link controller
                com.skilora.community.controller.DashboardController controller = loader.getController();
                if (controller != null) {
                    controller.initializeContext(
                        currentUser,
                        this::showProfileView,
                        this::showFeedView,
                        this::showUsersView,
                        this::showReportsView
                    );
                    controller.setNavigationCallbacks(
                        this::showPostJobView,
                        this::showApplicationInboxView,
                        this::showFormationsView,
                        this::showFormationAdminView,
                        this::showFinanceView
                    );
                }
                
                TLScrollArea scrollArea = new TLScrollArea(dashboardContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedDashboardView = scrollArea;
                
            } catch (Exception e) {
                logger.error("Failed to load DashboardView", e);
                switchContent(new Label(I18n.get("error.loading.dashboard")));
                return;
            }
        }
        switchContent(cachedDashboardView);
    }



    private void handleLogout() {
        // Clear cached views to prevent memory leaks
        cachedFeedView = null;
        cachedFeedController = null;
        cachedProfileScroll = null;
        cachedDashboardView = null;
        cachedSettingsView = null;
        cachedUsersView = null;
        cachedApplicationsView = null;
        cachedNotificationsView = null;
        cachedPostJobView = null;
        cachedApplicationInboxView = null;
        cachedMyOffersView = null;
        cachedActiveOffersView = null;
        cachedFormationsView = null;
        cachedReportsView = null;
        cachedInterviewsView = null;
        cachedSupportView = null;
        cachedSupportAdminView = null;
        cachedCommunityView = null;
        cachedFinanceView = null;
        cachedCertificationsView = null;

        // Clear navigation history to prevent stale state on re-login
        navigationHistory.clear();

        AuthService.getInstance().logout();

        // Camera is NOT pre-warmed — it only activates when user clicks Face ID

        Stage stage = (Stage) this.getScene().getWindow();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/LoginView.fxml"));
            HBox loginRoot = loader.load();

            LoginController controller = loader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }

            TLWindow root = new TLWindow(stage, "Skilora", loginRoot);

            Scene scene = new Scene(root, 1200, 800);
            WindowConfig.configureScene(scene);

            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            logger.error("Failed to load LoginView", e);
        }
    }



    private void showProfileView() {
        pushNavigation(this::showProfileView);
        if (cachedProfileScroll == null) {
            ProfileWizardView profileView = new ProfileWizardView(currentUser);
            profileView.setOnProfileUpdated(this::updateSidebarAvatar); // Hook up callback
            profileView.setOnViewPublicProfile(this::showPublicProfileView);

            // Wrap in scroll area
            cachedProfileScroll = new TLScrollArea(profileView);
            cachedProfileScroll.setFitToWidth(true);
            cachedProfileScroll.setFitToHeight(true);
            cachedProfileScroll.getStyleClass().add("transparent-bg");
        }

        switchContent(cachedProfileScroll);
    }

    private void showPublicProfileView() {
        pushNavigation(this::showPublicProfileView);
        PublicProfileView publicProfile = new PublicProfileView(currentUser);
        switchContent(publicProfile);
    }

    private void showUsersView() {
        pushNavigation(this::showUsersView);
        if (!requireCapability("Users", Capability.MANAGE_USERS)) {
            return;
        }
        // Always recreate users view to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/UsersView.fxml"));
            VBox usersContent = loader.load();
            
            // Link controller
            com.skilora.user.controller.UsersController controller = loader.getController();
            if (controller != null) {
                controller.initializeContext(
                    userService,
                    currentUser,
                    this::showUserForm,
                    this::showUsersView,
                    user -> getInitialsFromDisplayName(
                        user.getFullName() != null ? user.getFullName() : user.getUsername()
                    )
                );
            }
            
            TLScrollArea scrollArea = new TLScrollArea(usersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedUsersView = scrollArea;
            switchContent(cachedUsersView);
            
        } catch (Exception e) {
            logger.error("Failed to load UsersView", e);
            switchContent(new Label(I18n.get("error.loading.users")));
        }
    }

    private void showUserForm(User existingUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/UserFormDialog.fxml"));
            VBox formContent = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setUser(existingUser);
            
            TLDialog<User> dialog = new TLDialog<>();
            
            if (getScene() != null && getScene().getWindow() != null) {
                dialog.initOwner(getScene().getWindow());
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            }

            if (!isDarkMode()) {
                dialog.getDialogPane().getStyleClass().add("light");
            }
            
            dialog.setDialogTitle(existingUser == null ? I18n.get("users.new") : I18n.get("users.edit"));
            dialog.setDescription(I18n.get("users.form.subtitle"));
            dialog.setContent(formContent);

            ButtonType saveBtnType = new ButtonType(I18n.get("common.save"), ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType(I18n.get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.addButton(saveBtnType);
            dialog.addButton(cancelBtnType);

            // Prevent dialog from closing when validation fails
            dialog.getDialogPane().lookupButton(saveBtnType).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (!controller.validate()) {
                        event.consume(); // Keep dialog open
                    }
                }
            );

            dialog.setResultConverter(btnType -> {
                if (btnType == saveBtnType) {
                    return controller.getUser();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(user -> {
                boolean isUpdate = user.getId() != 0;
                // Defense-in-depth: prevent admin from modifying their own account
                if (isUpdate && currentUser != null && user.getId() == currentUser.getId()
                        && currentUser.getRole() == com.skilora.user.enums.Role.ADMIN) {
                    logger.warn("Blocked admin self-edit attempt for user id {}", user.getId());
                    return;
                }
                userService.saveUser(user);
                AuditLogService.getInstance().log(
                        currentUser != null ? currentUser.getId() : null,
                        "user",
                        isUpdate ? "user_update" : "user_create",
                        "User",
                        user.getId() != 0 ? String.valueOf(user.getId()) : null,
                        "username=" + user.getUsername() + ", email=" + user.getEmail() + ", role=" + user.getRole()
                );
                showUsersView();
            });
            
        } catch (Exception e) {
            logger.error("Failed to load UserFormDialog", e);
        }
    }

    private void showFeedView() {
        pushNavigation(this::showFeedView);
        if (cachedFeedView == null) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/skilora/view/recruitment/FeedView.fxml"));
                VBox feedContent = loader.load();
                
                // Set callback to navigate to job details
                cachedFeedController = loader.getController();
                if (cachedFeedController != null) {
                    cachedFeedController.setOnJobClick(this::showJobDetails);
                    cachedFeedController.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(feedContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFeedView = scrollArea;
            } catch (Exception e) {
                logger.error("Failed to load FeedView", e);
                return;
            }
        }
        if (cachedFeedController != null) cachedFeedController.setCurrentUser(currentUser);
        switchContent(cachedFeedView);
    }

    private void showSettingsView() {
        pushNavigation(this::showSettingsView);
        // Always recreate to pick up current i18n state
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/SettingsView.fxml"));
            VBox settingsContent = loader.load();

            SettingsController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
                controller.setDarkMode(isDarkMode());
                controller.setOnLanguageChanged(() -> {
                    // Clear all cached views so they rebuild with new locale
                    cachedDashboardView = null;
                    cachedSettingsView = null;
                    cachedFeedView = null;
                    cachedFeedController = null;
                    cachedProfileScroll = null;
                    cachedUsersView = null;
                    cachedApplicationsView = null;
                    cachedNotificationsView = null;
                    cachedPostJobView = null;
                    cachedApplicationInboxView = null;
                    cachedMyOffersView = null;
                    cachedActiveOffersView = null;
                    cachedFormationsView = null;
                    cachedReportsView = null;
                    cachedInterviewsView = null;
                    cachedCommunityView = null;
                    cachedSupportView = null;
                    cachedSupportAdminView = null;
                    cachedFinanceView = null;
                    cachedCertificationsView = null;

                    // Update topbar labels in-place (no duplication)
                    refreshTopBarLabels();

                    // Rebuild the sidebar with new locale
                    getSidebar().getChildren().clear();
                    setupSidebar();
                });
                controller.setCallbacks(
                    () -> {
                        if (controller.isDarkModeEnabled() != isDarkMode()) {
                            toggleTheme();
                        }
                        showDashboard();
                    },
                    this::showDashboard
                );
            }

            TLScrollArea scrollArea = new TLScrollArea(settingsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(false);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedSettingsView = scrollArea;

        } catch (Exception e) {
            logger.error("Failed to load SettingsView", e);
            return;
        }
        switchContent(cachedSettingsView);
    }

    private void showApplicationsView() {
        pushNavigation(this::showApplicationsView);
        if (cachedApplicationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/ApplicationsView.fxml"));
                VBox applicationsContent = loader.load();
                
                com.skilora.recruitment.controller.ApplicationsController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                }
                
                TLScrollArea scrollArea = new TLScrollArea(applicationsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedApplicationsView = scrollArea;
                
            } catch (Exception e) {
                logger.error("Failed to load ApplicationsView", e);
                return;
            }
        }
        switchContent(cachedApplicationsView);
    }

    private void showApplicationInboxView() {
        pushNavigation(this::showApplicationInboxView);
        if (!requireCapability("ApplicationInbox", Capability.VIEW_APPLICATION_INBOX)) return;
        if (cachedApplicationInboxView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/ApplicationInboxView.fxml"));
                VBox inboxContent = loader.load();
                
                com.skilora.recruitment.controller.ApplicationInboxController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                }
                
                TLScrollArea scrollArea = new TLScrollArea(inboxContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedApplicationInboxView = scrollArea;
                
            } catch (Exception e) {
                logger.error("Failed to load ApplicationInboxView", e);
                return;
            }
        }
        switchContent(cachedApplicationInboxView);
    }

    public void showJobDetails(com.skilora.recruitment.entity.JobOpportunity job) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/JobDetailsView.fxml"));
            VBox jobDetailsContent = loader.load();
            
            com.skilora.recruitment.controller.JobDetailsController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
                controller.setJob(job);
                controller.setCallbacks(
                    this::showFeedView,
                    () -> {
                        // Handle application submission
                        logger.info("Application submitted for: {}", job.getTitle());
                    }
                );
            }
            
            TLScrollArea scrollArea = new TLScrollArea(jobDetailsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("bg-transparent");
            
            switchContent(scrollArea);
            
        } catch (Exception e) {
            logger.error("Failed to load JobDetailsView", e);
        }
    }

    private void animateEntry(Node node, int delayMillis) {
        node.setOpacity(0);
        node.setTranslateY(15);

        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), node);
        tt.setFromY(15);
        tt.setToY(0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(node, ft, tt);
        pt.setDelay(Duration.millis(delayMillis));
        pt.play();
    }

    /**
     * Switch the center content with a smooth crossfade transition.
     * Fades out the current view (120ms), swaps, and fades in the new view (200ms).
     */
    private void switchContent(Node newView) {
        if (centerStack.getChildren().isEmpty()) {
            // No previous content — just add and fade in
            centerStack.getChildren().add(newView);
            animateEntry(newView, 0);
            return;
        }

        // If the same node is already showing, skip transition
        if (centerStack.getChildren().size() == 1 && centerStack.getChildren().get(0) == newView) {
            return;
        }

        // Fade out current content, then swap
        Node currentView = centerStack.getChildren().get(centerStack.getChildren().size() - 1);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), currentView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            centerStack.getChildren().clear();
            newView.setOpacity(0);
            newView.setTranslateY(8);
            centerStack.getChildren().add(newView);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), newView);
            slideIn.setFromY(8);
            slideIn.setToY(0);

            javafx.animation.ParallelTransition enterAnim = new javafx.animation.ParallelTransition(newView, fadeIn, slideIn);
            enterAnim.play();
        });
        fadeOut.play();
    }

    /**
     * Push current view to navigation history before navigating away.
     * Call this at the start of each show*View() method.
     */
    private void pushNavigation(Runnable viewAction) {
        if (currentViewAction != null) {
            navigationHistory.push(currentViewAction);
            // Keep history bounded to last 20 entries
            while (navigationHistory.size() > 20) {
                navigationHistory.removeLast();
            }
        }
        currentViewAction = viewAction;
    }

    /**
     * Navigate back to the previous view, or dashboard if no history.
     */
    public void navigateBack() {
        if (!navigationHistory.isEmpty()) {
            Runnable prev = navigationHistory.pop();
            currentViewAction = prev;
            prev.run();
        } else {
            showDashboard();
        }
    }

    @SuppressWarnings("unused")
    private boolean requireRole(String viewName, Role... allowed) {
        if (currentUser == null) {
            showError(com.skilora.community.controller.ErrorController.ErrorType.UNAUTHORIZED,
                    I18n.get("errorpage.access_denied"),
                    I18n.get("errorpage.access_denied.desc") + (viewName != null ? ("\n" + viewName) : ""));
            return false;
        }
        if (allowed != null) {
            for (Role r : allowed) {
                if (currentUser.getRole() == r) return true;
            }
        }
        showError(com.skilora.community.controller.ErrorController.ErrorType.UNAUTHORIZED,
                I18n.get("errorpage.access_denied"),
                I18n.get("errorpage.access_denied.desc") + (viewName != null ? ("\n" + viewName) : ""));
        return false;
    }

    private boolean requireCapability(String viewName, Capability capability) {
        if (currentUser == null) {
            showError(com.skilora.community.controller.ErrorController.ErrorType.UNAUTHORIZED,
                    I18n.get("errorpage.access_denied"),
                    I18n.get("errorpage.access_denied.desc") + (viewName != null ? ("\n" + viewName) : ""));
            return false;
        }
        if (PermissionService.getInstance().can(currentUser.getRole(), capability)) {
            return true;
        }
        showError(com.skilora.community.controller.ErrorController.ErrorType.UNAUTHORIZED,
                I18n.get("errorpage.access_denied"),
                I18n.get("errorpage.access_denied.desc") + (viewName != null ? ("\n" + viewName) : ""));
        return false;
    }

    private void showNotificationsView() {
        pushNavigation(this::showNotificationsView);
        if (cachedNotificationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/NotificationsView.fxml"));
                VBox notificationsContent = loader.load();
                
                com.skilora.community.controller.NotificationsController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                }
                
                TLScrollArea scrollArea = new TLScrollArea(notificationsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedNotificationsView = scrollArea;
                
            } catch (Exception e) {
                logger.error("Failed to load NotificationsView", e);
                return;
            }
        }
        switchContent(cachedNotificationsView);
        refreshNotificationBadge();
    }

    private void showPostJobView() {
        pushNavigation(this::showPostJobView);
        if (!requireCapability("PostJob", Capability.POST_PROJECT)) return;
        if (cachedPostJobView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/PostJobView.fxml"));
                VBox postJobContent = loader.load();
                
                com.skilora.recruitment.controller.PostJobController controller = loader.getController();
                if (controller != null) {
                    controller.setOnCancel(this::showDashboard);
                    controller.setCurrentUser(currentUser);
                }
                
                TLScrollArea scrollArea = new TLScrollArea(postJobContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedPostJobView = scrollArea;
                
            } catch (Exception e) {
                logger.error("Failed to load PostJobView", e);
                return;
            }
        }
        switchContent(cachedPostJobView);
    }

    public void showError(com.skilora.community.controller.ErrorController.ErrorType type, String message, String details) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/ErrorView.fxml"));
            VBox errorContent = loader.load();
            
            com.skilora.community.controller.ErrorController controller = loader.getController();
            if (controller != null) {
                controller.setError(type, message, details);
                controller.setCallbacks(
                    this::showDashboard,  // Go home
                    this::navigateBack,   // Go back (uses navigation history)
                    () -> {               // Support
                        logger.debug("Opening support dialog");
                        HelpDialog.show(getScene().getWindow());
                    }
                );
            }
            
            switchContent(errorContent);
            
        } catch (Exception e) {
            logger.error("Failed to load ErrorView", e);
        }
    }

    private void showMyOffersView() {
        pushNavigation(this::showMyOffersView);
        if (!requireCapability("MyOffers", Capability.MANAGE_OWN_OFFERS)) return;
        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/MyOffersView.fxml"));
            VBox myOffersContent = loader.load();

            com.skilora.recruitment.controller.MyOffersController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
                controller.setOnNewOffer(this::showPostJobView);
            }

            TLScrollArea scrollArea = new TLScrollArea(myOffersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedMyOffersView = scrollArea;
            switchContent(cachedMyOffersView);

        } catch (Exception e) {
            logger.error("Failed to load MyOffersView", e);
        }
    }

    private void showActiveOffersView() {
        pushNavigation(this::showActiveOffersView);
        if (!requireCapability("ActiveOffers", Capability.VIEW_ACTIVE_OFFERS)) {
            return;
        }
        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/ActiveOffersView.fxml"));
            VBox activeOffersContent = loader.load();
            ActiveOffersController controller = loader.getController();

            TLScrollArea scrollArea = new TLScrollArea(activeOffersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");
            scrollArea.setCache(true);
            scrollArea.setCacheHint(javafx.scene.CacheHint.SPEED);

            // Infinite scroll — auto-load next page when near bottom
            scrollArea.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.75) {
                    controller.loadNextPageIfAvailable();
                }
            });

            cachedActiveOffersView = scrollArea;
            switchContent(cachedActiveOffersView);

        } catch (Exception e) {
            logger.error("Failed to load ActiveOffersView", e);
        }
    }

    private void showFormationsView() {
        pushNavigation(this::showFormationsView);
        if (cachedFormationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/formation/FormationsView.fxml"));
                StackPane formationsContent = loader.load();
                
                // Pass current user to the controller
                FormationsController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(formationsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFormationsView = scrollArea;

            } catch (Exception e) {
                logger.error("Failed to load FormationsView", e);
                return;
            }
        }
        switchContent(cachedFormationsView);
    }

    private javafx.scene.Node cachedFormationAdminView;

    private void showFormationAdminView() {
        pushNavigation(this::showFormationAdminView);
        if (!requireCapability("FormationAdmin", Capability.ADMIN_FORMATIONS)) {
            return;
        }
        if (cachedFormationAdminView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/formation/FormationAdminView.fxml"));
                VBox adminContent = loader.load();

                com.skilora.formation.controller.FormationAdminController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(adminContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFormationAdminView = scrollArea;
            } catch (Exception e) {
                logger.error("Failed to load FormationAdminView", e);
                return;
            }
        }
        switchContent(cachedFormationAdminView);
    }

    private void showCertificationsView() {
        pushNavigation(this::showCertificationsView);
        if (cachedCertificationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/formation/CertificationsView.fxml"));
                VBox certContent = loader.load();

                CertificationsController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(certContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedCertificationsView = scrollArea;
            } catch (Exception e) {
                logger.error("Failed to load CertificationsView", e);
                return;
            }
        }
        switchContent(cachedCertificationsView);
    }

    private Node cachedMentorshipView;

    private void showMentorshipView() {
        pushNavigation(this::showMentorshipView);
        if (!requireCapability("Mentorship", Capability.VIEW_MENTORSHIP)) {
            return;
        }
        if (cachedMentorshipView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/formation/MentorshipView.fxml"));
                Node mentorshipContent = loader.load();

                com.skilora.formation.controller.MentorshipController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.initializeContext(currentUser);
                }

                cachedMentorshipView = mentorshipContent;
            } catch (Exception e) {
                logger.error("Failed to load MentorshipView", e);
                return;
            }
        }
        switchContent(cachedMentorshipView);
    }

    private void showSupportView() {
        pushNavigation(this::showSupportView);
        // Clear notification dot when support is opened
        if (supportNotificationDot != null) {
            supportNotificationDot.setVisible(false);
        }

        // Admin gets the admin support view
        if (currentUser != null && currentUser.getRole() == com.skilora.user.enums.Role.ADMIN) {
            if (cachedSupportAdminView == null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/support/SupportAdminView.fxml"));
                    VBox adminContent = loader.load();
                    
                    com.skilora.support.controller.SupportAdminController controller = loader.getController();
                    if (controller != null) {
                        controller.setCurrentUser(currentUser);
                    }

                    TLScrollArea scrollArea = new TLScrollArea(adminContent);
                    scrollArea.setFitToWidth(true);
                    scrollArea.setFitToHeight(true);
                    scrollArea.getStyleClass().add("transparent-bg");

                    cachedSupportAdminView = scrollArea;

                } catch (Exception e) {
                    logger.error("Failed to load SupportAdminView", e);
                    return;
                }
            }
            switchContent(cachedSupportAdminView);
            return;
        }

        // User / Employer get the regular support view
        if (cachedSupportView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/support/SupportView.fxml"));
                VBox supportContent = loader.load();
                
                com.skilora.support.controller.SupportController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(supportContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedSupportView = scrollArea;

            } catch (Exception e) {
                logger.error("Failed to load SupportView", e);
            }
        }

        switchContent(cachedSupportView);
    }

    private void showCommunityView() {
        pushNavigation(this::showCommunityView);
        if (cachedCommunityView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/CommunityView.fxml"));
                Node communityContent = loader.load();
                com.skilora.community.controller.CommunityController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.initializeContext(currentUser);
                }

                // CommunityView.fxml root is ScrollPane — don't wrap in TLScrollArea to avoid double-scroll
                cachedCommunityView = communityContent;
            } catch (Exception e) {
                logger.error("Failed to load CommunityView", e);
            }
        }
        switchContent(cachedCommunityView);
    }

    private void showReportsView() {
        pushNavigation(this::showReportsView);
        if (!requireCapability("Reports", Capability.VIEW_REPORTS)) {
            return;
        }
        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/ReportsView.fxml"));
            VBox reportsContent = loader.load();

            TLScrollArea scrollArea = new TLScrollArea(reportsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedReportsView = scrollArea;
            switchContent(cachedReportsView);

        } catch (Exception e) {
            logger.error("Failed to load ReportsView", e);
        }
    }

    private void showFinanceView() {
        pushNavigation(this::showFinanceView);
        if (cachedFinanceView == null) {
            try {
                // Admin/Employer see FinanceAdminView, others see FinanceView
                String fxmlPath;
                if (currentUser.getRole() == com.skilora.user.enums.Role.ADMIN
                        || currentUser.getRole() == com.skilora.user.enums.Role.EMPLOYER) {
                    fxmlPath = "/com/skilora/view/finance/FinanceAdminView.fxml";
                } else {
                    fxmlPath = "/com/skilora/view/finance/FinanceView.fxml";
                }

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                VBox financeContent = loader.load();

                Object controller = loader.getController();
                if (controller instanceof com.skilora.finance.controller.FinanceController fc) {
                    fc.setCurrentUser(currentUser);
                } else if (controller instanceof com.skilora.finance.controller.FinanceAdminController fac) {
                    fac.setCurrentUser(currentUser);
                }

                TLScrollArea scrollArea = new TLScrollArea(financeContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFinanceView = scrollArea;

            } catch (Exception e) {
                logger.error("Failed to load FinanceView", e);
                return;
            }
        }
        switchContent(cachedFinanceView);
    }

    private void showInterviewsView() {
        pushNavigation(this::showInterviewsView);
        if (!requireCapability("Interviews", Capability.MANAGE_INTERVIEWS)) return;
        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/InterviewsView.fxml"));
            VBox interviewsContent = loader.load();

            com.skilora.recruitment.controller.InterviewsController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }

            TLScrollArea scrollArea = new TLScrollArea(interviewsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedInterviewsView = scrollArea;
            switchContent(cachedInterviewsView);

        } catch (Exception e) {
            logger.error("Failed to load InterviewsView", e);
        }
    }
}
