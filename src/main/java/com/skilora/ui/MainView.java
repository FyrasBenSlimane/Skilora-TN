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
import com.skilora.user.ui.ProfileWizardView;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;

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

    // Cached Views for Performance
    private VBox cachedFeedView;
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

    // Support notification dot
    private Circle supportNotificationDot;

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

        themeToggleBtn = new Button();
        themeToggleBtn.getStyleClass().add("theme-toggle");
        updateThemeToggleIcon(themeToggleBtn, isDarkMode());
        themeToggleBtn.setOnAction(e -> {
            toggleTheme();
            updateThemeToggleIcon(themeToggleBtn, isDarkMode());
        });

        // Set initial text
        refreshTopBarLabels();

        getTopBar().getChildren().addAll(appTitleLabel, spacer, themeToggleBtn);
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
        toggleIcon.setContent("M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z");
        toggleIcon.getStyleClass().add("svg-path");
        StackPane iconWrap = new StackPane(toggleIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setStyle("-fx-alignment: center;");
        toggleSidebarBtn.setGraphic(iconWrap);
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
                                "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z", this::showDashboard),
                        createNavButton(I18n.get("nav.users"),
                                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                                this::showUsersView),
                        createNavButton(I18n.get("nav.active_offers"),
                                "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z",
                                this::showActiveOffersView),
                        createNavButton(I18n.get("nav.reports"), "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z",
                                this::showReportsView),
                        createSupportNavButton(),
                        createNavButton(I18n.get("nav.finance"),
                                "M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
                                this::showFinanceView),
                        createNavButton(I18n.get("nav.settings"),
                                "M19.43 12.98c.04-.32.07-.64.07-.98s-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.23-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98s.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.23.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z",
                                this::showSettingsView));
                break;
            case EMPLOYER:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z",
                                this::showDashboard),
                        createNavButton(I18n.get("nav.post_job"), "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
                                this::showPostJobView),
                        createNavButton(I18n.get("nav.my_offers"),
                                "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z",
                                this::showMyOffersView),
                        createNavButton(I18n.get("nav.inbox"),
                                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                                this::showApplicationInboxView),
                        createNavButton(I18n.get("nav.notifications"),
                                "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z",
                                this::showNotificationsView),
                        createSupportNavButton(),
                        createNavButton(I18n.get("nav.interviews"),
                                "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z",
                                this::showInterviewsView),
                        createNavButton(I18n.get("nav.finance"),
                                "M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
                                this::showFinanceView));
                break;
            case USER:
            default:
                navMenu.getChildren().addAll(
                        createNavButton(I18n.get("nav.dashboard"),
                                "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z",
                                this::showDashboard),
                        createNavButton(I18n.get("nav.feed"),
                                "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z",
                                this::showFeedView),
                        createNavButton(I18n.get("nav.applications"),
                                "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h12c1.1 0 2-.89 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
                                this::showApplicationsView),
                        createNavButton(I18n.get("nav.notifications"),
                                "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z",
                                this::showNotificationsView),
                        createSupportNavButton(),
                        createNavButton(I18n.get("nav.profile"),
                                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                                this::showProfileView),
                        createNavButton(I18n.get("nav.community"),
                                "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M23 21v-2a4 4 0 0 1-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
                                this::showCommunityView),
                        createNavButton(I18n.get("nav.formations"),
                                "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z",
                                this::showFormationsView),
                        createNavButton(I18n.get("nav.finance"),
                                "M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
                                this::showFinanceView));
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
        String sunPath = "M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 0 0 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z";
        String moonPath = "M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z";

        SVGPath icon = new SVGPath();
        icon.setContent(darkMode ? sunPath : moonPath);
        icon.getStyleClass().add("svg-path");
        icon.setStyle("-fx-fill: -fx-foreground;");

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
            btn.setOnAction(e -> action.run());
        }
        return btn;
    }

    private javafx.scene.Node createSupportNavButton() {
        TLButton btn = createNavButton(I18n.get("nav.support"),
                "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
                this::showSupportView);

        supportNotificationDot = new Circle(4);
        supportNotificationDot.setStyle("-fx-fill: -fx-red;");
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
                        this::showUsersView
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
        if (cachedProfileScroll == null) {
            ProfileWizardView profileView = new ProfileWizardView(currentUser);
            profileView.setOnProfileUpdated(this::updateSidebarAvatar); // Hook up callback

            // Wrap in scroll area
            cachedProfileScroll = new TLScrollArea(profileView);
            cachedProfileScroll.setFitToWidth(true);
            cachedProfileScroll.setFitToHeight(true);
            cachedProfileScroll.getStyleClass().add("transparent-bg");
        }

        switchContent(cachedProfileScroll);
    }

    private void showUsersView() {
        // Always recreate users view to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/UsersView.fxml"));
            VBox usersContent = loader.load();
            
            // Link controller
            com.skilora.user.controller.UsersController controller = loader.getController();
            if (controller != null) {
                controller.initializeContext(
                    userService,
                    this::showUserForm,
                    this::showUsersView,
                    user -> getInitialsFromDisplayName(
                        user.getFullName() != null ? user.getFullName() : user.getUsername()
                    )
                );
            }
            
            cachedUsersView = usersContent;
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
            dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, cancelBtnType);

            dialog.setResultConverter(btnType -> {
                if (btnType == saveBtnType) {
                    if (controller.validate()) {
                        return controller.getUser();
                    }
                    return null;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(user -> {
                userService.saveUser(user);
                showUsersView();
            });
            
        } catch (Exception e) {
            logger.error("Failed to load UserFormDialog", e);
        }
    }

    private void showFeedView() {
        if (cachedFeedView == null) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/skilora/view/recruitment/FeedView.fxml"));
                cachedFeedView = loader.load();
                
                // Set callback to navigate to job details
                com.skilora.recruitment.controller.FeedController controller = loader.getController();
                if (controller != null) {
                    controller.setOnJobClick(this::showJobDetails);
                }
            } catch (Exception e) {
                logger.error("Failed to load FeedView", e);
                return;
            }
        }
        switchContent(cachedFeedView);
    }

    private void showSettingsView() {
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

            cachedSettingsView = settingsContent;

        } catch (Exception e) {
            logger.error("Failed to load SettingsView", e);
            return;
        }
        switchContent(cachedSettingsView);
    }

    private void showApplicationsView() {
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
            scrollArea.setStyle("-fx-background-color: transparent;");
            
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

    private void showNotificationsView() {
        if (cachedNotificationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/NotificationsView.fxml"));
                VBox notificationsContent = loader.load();
                
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
    }

    private void showPostJobView() {
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
                    () -> {               // Go back
                        // TODO: Implement navigation history
                        showDashboard();
                    },
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
        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/recruitment/ActiveOffersView.fxml"));
            VBox activeOffersContent = loader.load();

            TLScrollArea scrollArea = new TLScrollArea(activeOffersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedActiveOffersView = scrollArea;
            switchContent(cachedActiveOffersView);

        } catch (Exception e) {
            logger.error("Failed to load ActiveOffersView", e);
        }
    }

    private void showFormationsView() {
        if (cachedFormationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/formation/FormationsView.fxml"));
                VBox formationsContent = loader.load();
                
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

    private void showSupportView() {
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
        if (cachedCommunityView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/community/CommunityView.fxml"));
                Node communityContent = loader.load();
                com.skilora.community.controller.CommunityController controller = loader.getController();
                if (controller != null && currentUser != null) {
                    controller.initializeContext(currentUser);
                }
                cachedCommunityView = communityContent;
            } catch (Exception e) {
                logger.error("Failed to load CommunityView", e);
            }
        }
        switchContent(cachedCommunityView);
    }

    private void showReportsView() {
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
