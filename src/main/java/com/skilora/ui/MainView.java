package com.skilora.ui;

import com.skilora.framework.layouts.TLAppLayout;
import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.model.entity.JobOffer;
import com.skilora.model.entity.User;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import com.skilora.model.service.UserService;
import com.skilora.model.service.PreferencesService;
import com.skilora.controller.LoginController;
import com.skilora.controller.UserFormController;
import com.skilora.controller.SettingsController;
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
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.geometry.Side;

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
    private Node cachedFinanceView;
    private Node cachedUserFinanceView;
    private Node cachedEmployeurFinanceView;
    private Node cachedPaiementView;
    private Node cachedSearchView;

    public MainView(User user) {
        super();
        this.currentUser = user;
        this.userService = UserService.getInstance();

        // Load saved user preferences on login
        loadUserPreferences();

        setupChrome();

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

    private void setupChrome() {
        Label appTitle = new Label();
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
        appTitle.setText(roleTitle);

        appTitle.getStyleClass().add("h4"); // Make it look like a title
        getTopBar().getChildren().add(appTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getTopBar().getChildren().add(spacer);

        Button themeToggle = new Button();
        themeToggle.getStyleClass().add("theme-toggle");
        updateThemeToggleText(themeToggle, isDarkMode());
        themeToggle.setOnAction(e -> {
            toggleTheme();
            updateThemeToggleText(themeToggle, isDarkMode());
        });
        getTopBar().getChildren().add(themeToggle);

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
                        createNavButton(I18n.get("nav.search"),
                                "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
                                this::showSearchView),
                        createNavButton("Finance",
                                "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
                                this::showFinanceView),
                        createNavButton("Paiement Projet",
                                "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4H4V6h16v2zm0 10H4v-6h16v6z",
                                this::showPaiementView),
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
                        createNavButton(I18n.get("nav.interviews"),
                                "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z",
                                this::showInterviewsView),
                        createNavButton("Ma Paie",
                                "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
                                this::showEmployeurFinanceView),
                        createNavButton("Paiement Projet",
                                "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4H4V6h16v2zm0 10H4v-6h16v6z",
                                this::showPaiementView));
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
                        createNavButton(I18n.get("nav.profile"),
                                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                                this::showProfileView),
                        createNavButton(I18n.get("nav.formations"),
                                "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z",
                                this::showFormationsView),
                        createNavButton("Consultation Finance",
                                "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
                                this::showUserFinanceView),
                        createNavButton("Paiement Projet",
                                "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4H4V6h16v2zm0 10H4v-6h16v6z",
                                this::showPaiementView));
                break;
        }

        // User Profile Section (Bottom of Sidebar) â€“ wrapped so avatar is centered
        // when
        // collapsed
        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        String userName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName()
                : "Admin User";
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

    private static void updateThemeToggleText(Button btn, boolean darkMode) {
        btn.setText(darkMode ? "Light" : "Dark");
    }

    /**
     * Default initials from display name when no avatar image is available (e.g.
     * "Admin User" â†’ "AU").
     */
    private void updateSidebarAvatar() {
        if (sidebarAvatar == null)
            return;

        String initials = getInitialsFromDisplayName(
                currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
        sidebarAvatar.setFallback(initials);

        if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isBlank()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(currentUser.getPhotoUrl(), 36, 36, true,
                        true, true);
                if (!img.isError()) {
                    sidebarAvatar.setImage(img);
                }
            } catch (Exception e) {
                sidebarAvatar.setImage(null);
            }
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

    private TLButton createNavButton(String text, String svgPath) {
        return createNavButton(text, svgPath, null);
    }

    private void showDashboard() {
        centerStack.getChildren().clear();

        if (cachedDashboardView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/DashboardView.fxml"));
                VBox dashboardContent = loader.load();

                // Link controller
                com.skilora.controller.DashboardController controller = loader.getController();
                if (controller != null) {
                    controller.initializeContext(
                            currentUser,
                            this::showProfileView,
                            this::showFeedView,
                            this::showUsersView);
                }

                TLScrollArea scrollArea = new TLScrollArea(dashboardContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedDashboardView = scrollArea;
                animateEntry(dashboardContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load DashboardView", e);
                centerStack.getChildren().add(new Label(I18n.get("error.loading.dashboard")));
                return;
            }
        }
        centerStack.getChildren().add(cachedDashboardView);
    }

    private void handleLogout() {
        Stage stage = (Stage) this.getScene().getWindow();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/LoginView.fxml"));
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

        centerStack.getChildren().clear();
        centerStack.getChildren().add(cachedProfileScroll);
    }

    private void showUsersView() {
        centerStack.getChildren().clear();

        // Always recreate users view to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/UsersView.fxml"));
            VBox usersContent = loader.load();

            // Link controller
            com.skilora.controller.UsersController controller = loader.getController();
            if (controller != null) {
                controller.initializeContext(
                        userService,
                        this::showUserForm,
                        this::showUsersView,
                        user -> getInitialsFromDisplayName(
                                user.getFullName() != null ? user.getFullName() : user.getUsername()));
            }

            cachedUsersView = usersContent;
            centerStack.getChildren().add(cachedUsersView);
            animateEntry(usersContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load UsersView", e);
            centerStack.getChildren().add(new Label(I18n.get("error.loading.users")));
        }
    }

    private void showUserForm(User existingUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/UserFormDialog.fxml"));
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
        centerStack.getChildren().clear();
        if (cachedFeedView == null) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/skilora/view/FeedView.fxml"));
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
        centerStack.getChildren().add(cachedFeedView);

        // Ensure it's visible if it was hidden or detached
        cachedFeedView.setOpacity(1);
        cachedFeedView.setTranslateY(0);

        // Only animate if it's the first time
        if (cachedFeedView.getParent() == null) {
            animateEntry(cachedFeedView, 0);
        }
    }

    private void showSettingsView() {
        centerStack.getChildren().clear();

        // Always recreate to pick up current i18n state
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/SettingsView.fxml"));
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

                    // Rebuild the sidebar with new locale
                    getSidebar().getChildren().clear();
                    setupChrome();
                });
                controller.setCallbacks(
                        () -> {
                            if (controller.isDarkModeEnabled() != isDarkMode()) {
                                toggleTheme();
                            }
                            showDashboard();
                        },
                        this::showDashboard);
            }

            cachedSettingsView = settingsContent;
            animateEntry(settingsContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load SettingsView", e);
            return;
        }
        centerStack.getChildren().add(cachedSettingsView);
    }

    private void showApplicationsView() {
        centerStack.getChildren().clear();

        if (cachedApplicationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ApplicationsView.fxml"));
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
                animateEntry(applicationsContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load ApplicationsView", e);
                return;
            }
        }
        centerStack.getChildren().add(cachedApplicationsView);
    }

    private void showApplicationInboxView() {
        centerStack.getChildren().clear();

        if (cachedApplicationInboxView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/skilora/view/ApplicationInboxView.fxml"));
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
                animateEntry(inboxContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load ApplicationInboxView", e);
                return;
            }
        }
        centerStack.getChildren().add(cachedApplicationInboxView);
    }

    public void showJobDetails(com.skilora.model.entity.JobOpportunity job) {
        centerStack.getChildren().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/JobDetailsView.fxml"));
            VBox jobDetailsContent = loader.load();

            com.skilora.recruitment.controller.JobDetailsController controller = loader.getController();
            if (controller != null) {
                controller.setJob(job);
                controller.setCallbacks(
                        this::showFeedView,
                        () -> {
                            // Handle application submission
                            logger.info("Application submitted for: {}", job.getTitle());
                        });
            }

            TLScrollArea scrollArea = new TLScrollArea(jobDetailsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.setStyle("-fx-background-color: transparent;");

            centerStack.getChildren().add(scrollArea);
            animateEntry(jobDetailsContent, 0);

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

    private void showNotificationsView() {
        centerStack.getChildren().clear();

        if (cachedNotificationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/NotificationsView.fxml"));
                VBox notificationsContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(notificationsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedNotificationsView = scrollArea;
                animateEntry(notificationsContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load NotificationsView", e);
                return;
            }
        }
        centerStack.getChildren().add(cachedNotificationsView);
    }

    private void showPostJobView() {
        showPostJobView(null);
    }

    public void showPostJobView(JobOffer existingOffer) {
        centerStack.getChildren().clear();

        // Recreate to ensure the controller state is fresh when editing
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/PostJobView.fxml"));
            VBox postJobContent = loader.load();

            com.skilora.recruitment.controller.PostJobController controller = loader.getController();
            if (controller != null) {
                controller.setOnCancel(this::showDashboard);
                controller.setCurrentUser(currentUser);
                if (existingOffer != null) {
                    controller.setEditingOffer(existingOffer);
                }
            }

            TLScrollArea scrollArea = new TLScrollArea(postJobContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            // We don't cache the editing view as it depends on parameters
            centerStack.getChildren().add(scrollArea);
            animateEntry(postJobContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load PostJobView", e);
        }
    }

    public void showForgotPasswordView() {
        centerStack.getChildren().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ForgotPasswordView.fxml"));
            VBox forgotPasswordContent = loader.load();

            com.skilora.controller.ForgotPasswordController controller = loader.getController();
            if (controller != null) {
                controller.setOnBack(() -> {
                    // This would navigate back to login, but since we're in MainView, go to
                    // dashboard
                    showDashboard();
                });
            }

            centerStack.getChildren().add(forgotPasswordContent);
            animateEntry(forgotPasswordContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load ForgotPasswordView", e);
        }
    }

    private void showFinanceView() {
        centerStack.getChildren().clear();

        if (cachedFinanceView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FinanceView.fxml"));
                VBox financeContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(financeContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFinanceView = scrollArea;
                animateEntry(financeContent, 0);
            } catch (Exception e) {
                logger.error("Failed to load FinanceView", e);
                centerStack.getChildren().add(new Label("Error loading Finance Module: " + e.getMessage()));
                return;
            }
        }
        centerStack.getChildren().add(cachedFinanceView);
    }

    /**
     * USER role: Read-only consultation of ALL finance data (admin view).
     */
    private void showUserFinanceView() {
        centerStack.getChildren().clear();

        if (cachedUserFinanceView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserFinanceView.fxml"));
                VBox financeContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(financeContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedUserFinanceView = scrollArea;
                animateEntry(financeContent, 0);
            } catch (Exception e) {
                logger.error("Failed to load UserFinanceView", e);
                centerStack.getChildren().add(new Label("Error loading Finance: " + e.getMessage()));
                return;
            }
        }
        centerStack.getChildren().add(cachedUserFinanceView);
    }

    /**
     * EMPLOYER role: Rich personal payroll dashboard.
     * Uses the same TLScrollArea pattern as showUserFinanceView.
     */
    private void showEmployeurFinanceView() {
        centerStack.getChildren().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EmployeurFinanceView.fxml"));
            javafx.scene.Node root = loader.load();

            // Inject the current user ID — controller fetches real name from DB
            com.skilora.finance.controller.EmployeurFinanceController controller = loader.getController();
            if (controller != null) {
                String nameHint = currentUser.getFullName() != null
                        ? currentUser.getFullName()
                        : currentUser.getUsername();
                controller.setEmployeeId(currentUser.getId(), nameHint);
            }

            TLScrollArea scrollArea = new TLScrollArea(root);
            scrollArea.setFitToWidth(true);
            // scrollArea.setFitToHeight(true); // Don't fit to height to allow vertical
            // scrolling
            scrollArea.getStyleClass().add("transparent-bg");

            cachedEmployeurFinanceView = scrollArea;
            centerStack.getChildren().add(scrollArea);
            animateEntry(root, 0);

        } catch (Exception e) {
            logger.error("Failed to load EmployeurFinanceView", e);
            centerStack.getChildren().add(new Label(
                    "Erreur chargement Ma Paie: " + e.getMessage()));
        }
    }

    /**
     * Paiement Projet : Stripe (mode TEST).
     * Charge la vue /finance/views/paiement.fxml.
     */
    private void showPaiementView() {
        centerStack.getChildren().clear();

        if (cachedPaiementView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/finance/views/paiement.fxml"));
                VBox paiementContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(paiementContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedPaiementView = scrollArea;
                animateEntry(paiementContent, 0);
            } catch (Exception e) {
                logger.error("Failed to load paiement.fxml", e);
                centerStack.getChildren().add(new Label("Erreur chargement Paiement: " + e.getMessage()));
                return;
            }
        }

        centerStack.getChildren().add(cachedPaiementView);
    }

    public void showError(com.skilora.controller.ErrorController.ErrorType type, String message, String details) {
        centerStack.getChildren().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ErrorView.fxml"));
            VBox errorContent = loader.load();

            com.skilora.controller.ErrorController controller = loader.getController();
            if (controller != null) {
                controller.setError(type, message, details);
                controller.setCallbacks(
                        this::showDashboard, // Go home
                        () -> { // Go back
                            // TODO: Implement navigation history
                            showDashboard();
                        },
                        () -> { // Support
                            logger.debug("Opening support dialog");
                            HelpDialog.show(getScene().getWindow());
                        });
            }

            centerStack.getChildren().add(errorContent);
            animateEntry(errorContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load ErrorView", e);
        }
    }

    private void showMyOffersView() {
        centerStack.getChildren().clear();

        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/MyOffersView.fxml"));
            VBox myOffersContent = loader.load();

            com.skilora.recruitment.controller.MyOffersController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
                controller.setOnNewOffer(this::showPostJobView);
                controller.setOnEditOffer(this::showPostJobView);
            }

            TLScrollArea scrollArea = new TLScrollArea(myOffersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedMyOffersView = scrollArea;
            centerStack.getChildren().add(cachedMyOffersView);
            animateEntry(myOffersContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load MyOffersView", e);
        }
    }

    private void showActiveOffersView() {
        centerStack.getChildren().clear();

        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ActiveOffersView.fxml"));
            VBox activeOffersContent = loader.load();

            TLScrollArea scrollArea = new TLScrollArea(activeOffersContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedActiveOffersView = scrollArea;
            centerStack.getChildren().add(cachedActiveOffersView);
            animateEntry(activeOffersContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load ActiveOffersView", e);
        }
    }

    private void showFormationsView() {
        centerStack.getChildren().clear();

        if (cachedFormationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/FormationsView.fxml"));
                VBox formationsContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(formationsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedFormationsView = scrollArea;
                animateEntry(formationsContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load FormationsView", e);
                return;
            }
        }
        centerStack.getChildren().add(cachedFormationsView);
    }

    private void showReportsView() {
        centerStack.getChildren().clear();

        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ReportsView.fxml"));
            VBox reportsContent = loader.load();

            TLScrollArea scrollArea = new TLScrollArea(reportsContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");

            cachedReportsView = scrollArea;
            centerStack.getChildren().add(cachedReportsView);
            animateEntry(reportsContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load ReportsView", e);
        }
    }

    private void showInterviewsView() {
        centerStack.getChildren().clear();

        // Always recreate to refresh data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/InterviewsView.fxml"));
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
            centerStack.getChildren().add(cachedInterviewsView);
            animateEntry(interviewsContent, 0);

        } catch (Exception e) {
            logger.error("Failed to load InterviewsView", e);
        }
    }

    private void showSearchView() {
        centerStack.getChildren().clear();

        if (cachedSearchView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/SearchView.fxml"));
                VBox searchContent = loader.load();

                TLScrollArea scrollArea = new TLScrollArea(searchContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                cachedSearchView = scrollArea;
                animateEntry(searchContent, 0);

            } catch (Exception e) {
                logger.error("Failed to load SearchView", e);
                centerStack.getChildren().add(new Label(I18n.get("error.loading.search")));
                return;
            }
        }
        centerStack.getChildren().add(cachedSearchView);
    }
}
