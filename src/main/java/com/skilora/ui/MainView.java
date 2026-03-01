package com.skilora.ui;

import com.skilora.framework.layouts.TLAppLayout;
import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import com.skilora.service.usermanagement.UserService;
import com.skilora.service.formation.TrainingService;
import com.skilora.service.usermanagement.PreferencesService;
import com.skilora.controller.usermanagement.LoginController;
import com.skilora.controller.usermanagement.UserFormController;
import com.skilora.controller.usermanagement.SettingsController;
import com.skilora.ui.usermanagement.ProfileWizardView;
import com.skilora.utils.I18n;
import com.skilora.utils.DialogUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
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
    private final TrainingService trainingService;

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
    private Node cachedAdminTrainingsView;
    private Node cachedReportsView;
    private Node cachedInterviewsView;
    private Node cachedTrainingProgressView;
    private com.skilora.controller.formation.TrainingProgressController cachedTrainingProgressController;
    private Node cachedCertificationsView;
    private com.skilora.controller.formation.CertificationsController cachedCertificationsController;
    private Node cachedTrainingDetailView;

    public MainView(User user) {
        super();
        this.currentUser = user;
        this.userService = UserService.getInstance();
        this.trainingService = TrainingService.getInstance();

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
                        createNavButton(I18n.get("nav.formations"),
                                "M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z",
                                this::showAdminTrainingsView),
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
                                this::showInterviewsView));
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
                        createNavButton(I18n.get("nav.my_trainings"),
                                "M9 11.24V7.5a2.5 2.5 0 0 1 5 0v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63l-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.43-.72c-.08-.01-.15-.03-.24-.03-.31 0-.59.13-.79.33l-.79.8 4.94 4.94c.27.27.65.44 1.06.44h6.79c.75 0 1.33-.55 1.44-1.28l.75-5.27c.01-.07.02-.14.02-.2 0-.62-.38-1.16-.91-1.38z",
                                this::showTrainingProgressView),
                        createNavButton(I18n.get("nav.certificates"),
                                "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z",
                                this::showCertificationsView));
                break;
        }

        // User Profile Section (Bottom of Sidebar) – wrapped so avatar is centered when
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
     * "Admin User" → "AU").
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
        
        // Always reload dashboard to refresh statistics (especially for admin)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/DashboardView.fxml"));
            VBox dashboardContent = loader.load();
            
            // Link controller
            com.skilora.controller.usermanagement.DashboardController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }
            
            TLScrollArea scrollArea = new TLScrollArea(dashboardContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");
            
            cachedDashboardView = scrollArea; // Update cache
            centerStack.getChildren().add(scrollArea);
            animateEntry(dashboardContent, 0);
            
        } catch (Exception e) {
            logger.error("Failed to load DashboardView", e);
            centerStack.getChildren().add(new Label("Error loading dashboard: " + e.getMessage()));
        }
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
            com.skilora.controller.usermanagement.UsersController controller = loader.getController();
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
            centerStack.getChildren().add(cachedUsersView);
            animateEntry(usersContent, 0);
            
        } catch (Exception e) {
            logger.error("Failed to load UsersView", e);
            centerStack.getChildren().add(new Label(I18n.get("error.loading.users")));
        }
    }

    private void showAdminTrainingsView() {
        centerStack.getChildren().clear();
        
        if (cachedAdminTrainingsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/AdminTrainingsView.fxml"));
                VBox trainingsContent = loader.load();
                
                // Link controller
                com.skilora.controller.formation.AdminTrainingController controller = loader.getController();
                if (controller != null) {
                    controller.initializeContext(
                        trainingService,
                        this::showTrainingForm,
                        () -> {
                            // Refresh callback: clear cache and reload
                            cachedAdminTrainingsView = null;
                            showAdminTrainingsView();
                        }
                    );
                }
                
                cachedAdminTrainingsView = trainingsContent;
                animateEntry(trainingsContent, 0);
                
            } catch (Exception e) {
                logger.error("Failed to load AdminTrainingsView", e);
                centerStack.getChildren().add(new Label("Erreur lors du chargement des formations"));
                return;
            }
        }
        centerStack.getChildren().add(cachedAdminTrainingsView);
    }

    private void showTrainingForm(com.skilora.model.entity.formation.Training existingTraining) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/com/skilora/view/TrainingFormDialog.fxml");
            if (fxmlUrl == null) {
                logger.error("TrainingFormDialog.fxml not found in classpath!");
                DialogUtils.showError("Erreur", "Le fichier formulaire n'a pas été trouvé. Vérifiez que TrainingFormDialog.fxml existe dans src/main/resources/com/skilora/view/");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox formContent = loader.load();
            
            com.skilora.controller.formation.TrainingFormController controller = loader.getController();
            if (controller == null) {
                logger.error("TrainingFormController is null after loading FXML!");
                DialogUtils.showError("Erreur", "Le contrôleur du formulaire n'a pas été initialisé.");
                return;
            }
            controller.setTraining(existingTraining);
            
            TLDialog<com.skilora.model.entity.formation.Training> dialog = new TLDialog<>();
            
            if (getScene() != null && getScene().getWindow() != null) {
                dialog.initOwner(getScene().getWindow());
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            }

            if (!isDarkMode()) {
                dialog.getDialogPane().getStyleClass().add("light");
            }
            
            dialog.setDialogTitle(existingTraining == null ? "Nouvelle Formation" : "Modifier Formation");
            dialog.setDescription(existingTraining == null ? "Créez une nouvelle formation" : "Modifiez les informations de la formation");
            dialog.setContent(formContent);

            // Remove default buttons - we use custom buttons in the form steps
            ButtonType cancelBtnType = new ButtonType(I18n.get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(cancelBtnType);
            
            // Setup final save callback (called from step 2's "Enregistrer la formation" button)
            controller.setOnFinalSave(() -> {
                // Validate form first
                if (!controller.validate()) {
                    DialogUtils.showError("Erreur de validation", 
                        "Veuillez corriger les erreurs dans le formulaire avant de sauvegarder.");
                    return;
                }
                
                // Get training object (signature is auto-captured in getTraining())
                try {
                    com.skilora.model.entity.formation.Training training = controller.getTraining();
                    if (training != null) {
                        // Close dialog first
                        dialog.close();
                        
                        // Save training in background thread
                        Thread saveThread = new Thread(() -> {
                            try {
                                trainingService.saveTraining(training);
                                javafx.application.Platform.runLater(() -> {
                                    DialogUtils.showSuccess("Succès", 
                                        existingTraining == null 
                                            ? "Formation créée avec succès!" 
                                            : "Formation modifiée avec succès!");
                                    // Clear cache to refresh the list
                                    cachedAdminTrainingsView = null;
                                    showAdminTrainingsView();
                                });
                            } catch (Exception ex) {
                                logger.error("Error saving training", ex);
                                javafx.application.Platform.runLater(() -> {
                                    DialogUtils.showError("Erreur", 
                                        "Impossible de sauvegarder la formation: " + ex.getMessage());
                                });
                            }
                        }, "SaveTrainingThread");
                        saveThread.setDaemon(true);
                        saveThread.start();
                    }
                } catch (Exception ex) {
                    logger.error("Error getting training from form", ex);
                    DialogUtils.showError("Erreur", 
                        "Une erreur s'est produite lors de la préparation des données: " + ex.getMessage());
                }
            });

            // Show dialog (no result converter needed - we handle save via callback)
            dialog.showAndWait();
            
        } catch (Exception e) {
            logger.error("Failed to load TrainingFormDialog", e);
            DialogUtils.showError("Erreur", "Impossible de charger le formulaire: " + e.getMessage());
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
                // FeedController removed - job-related functionality disabled
                Object controller = loader.getController();
                // if (controller != null) {
                //     controller.setOnJobClick(this::showJobDetails);
                // }
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
                    cachedAdminTrainingsView = null;
                    cachedReportsView = null;
                    cachedInterviewsView = null;
                    cachedTrainingProgressView = null;

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
                    this::showDashboard
                );
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
                
                // ApplicationsController removed - job-related functionality disabled
                Object controller = loader.getController();
                // if (controller != null) {
                //     controller.setCurrentUser(currentUser);
                // }
                
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ApplicationInboxView.fxml"));
                VBox inboxContent = loader.load();
                
                // ApplicationInboxController removed - job-related functionality disabled
                Object controller = loader.getController();
                // if (controller != null) {
                //     controller.setCurrentUser(currentUser);
                // }
                
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

    public void showJobDetails(Object job) { // JobOpportunity removed - job-related functionality disabled
        centerStack.getChildren().clear();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/JobDetailsView.fxml"));
            VBox jobDetailsContent = loader.load();
            
            // JobDetailsController removed - job-related functionality disabled
            Object controller = loader.getController();
            // if (controller != null) {
            //     controller.setJob(job);
            //     controller.setCallbacks(
            //         this::showFeedView,
            //         () -> {
            //             // Handle application submission
            //             logger.info("Application submitted");
            //         }
            //     );
            // }
            
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
        centerStack.getChildren().clear();
        
        if (cachedPostJobView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/PostJobView.fxml"));
                VBox postJobContent = loader.load();
                
                // PostJobController removed - job-related functionality disabled
                Object controller = loader.getController();
                // if (controller != null) {
                //     controller.setOnCancel(this::showDashboard);
                //     controller.setCurrentUser(currentUser);
                // }
                
                TLScrollArea scrollArea = new TLScrollArea(postJobContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                
                cachedPostJobView = scrollArea;
                animateEntry(postJobContent, 0);
                
            } catch (Exception e) {
                logger.error("Failed to load PostJobView", e);
                return;
            }
        }
        centerStack.getChildren().add(cachedPostJobView);
    }

    public void showForgotPasswordView() {
        centerStack.getChildren().clear();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ForgotPasswordView.fxml"));
            VBox forgotPasswordContent = loader.load();
            
            com.skilora.controller.usermanagement.ForgotPasswordController controller = loader.getController();
            if (controller != null) {
                controller.setOnBack(() -> {
                    // This would navigate back to login, but since we're in MainView, go to dashboard
                    showDashboard();
                });
            }
            
            centerStack.getChildren().add(forgotPasswordContent);
            animateEntry(forgotPasswordContent, 0);
            
        } catch (Exception e) {
            logger.error("Failed to load ForgotPasswordView", e);
        }
    }

    public void showError(Object type, String message, String details) { // ErrorController removed
        centerStack.getChildren().clear();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/ErrorView.fxml"));
            VBox errorContent = loader.load();
            
            // ErrorController removed - using simple error display
            Object controller = loader.getController();
            // if (controller != null) {
            //     controller.setError(type, message, details);
            //     controller.setCallbacks(
            //         this::showDashboard,  // Go home
            //         () -> {               // Go back
            //             // TODO: Implement navigation history
            //             showDashboard();
            //         },
            //         () -> {               // Support
            //             logger.debug("Opening support dialog");
            //             HelpDialog.show(getScene().getWindow());
            //         }
            //     );
            // }
            
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

            // MyOffersController removed - job-related functionality disabled
            Object controller = loader.getController();
            // if (controller != null) {
            //     controller.setCurrentUser(currentUser);
            //     controller.setOnNewOffer(this::showPostJobView);
            // }

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
                StackPane formationsContent = loader.load();

                com.skilora.controller.formation.FormationsController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                    controller.setOnShowTrainingDetail(training -> showTrainingDetailView(training));
                    // Refresh "Mes Formations" view after enrollment
                    controller.setOnEnrollmentSuccess(() -> {
                        if (cachedTrainingProgressController != null) {
                            cachedTrainingProgressController.refresh();
                        }
                    });
                }

                // Extract chatbot container from the StackPane before wrapping in ScrollArea
                // The chatbot needs to be outside the scroll area to remain visible as an overlay
                javafx.scene.layout.StackPane chatbotContainer = null;
                if (controller != null) {
                    try {
                        // Use reflection or direct access to get chatbotContainer
                        // Since it's @FXML injected, we can access it via controller
                        java.lang.reflect.Field field = controller.getClass().getDeclaredField("chatbotContainer");
                        field.setAccessible(true);
                        chatbotContainer = (javafx.scene.layout.StackPane) field.get(controller);
                    } catch (Exception e) {
                        logger.warn("Could not extract chatbot container via reflection: {}", e.getMessage());
                    }
                }
                
                // Find chatbotContainer in the scene graph if reflection failed
                if (chatbotContainer == null) {
                    chatbotContainer = (javafx.scene.layout.StackPane) formationsContent.lookup("#chatbotContainer");
                }
                
                // Remove chatbot from the StackPane temporarily
                if (chatbotContainer != null && chatbotContainer.getParent() == formationsContent) {
                    formationsContent.getChildren().remove(chatbotContainer);
                }

                // Wrap only the scrollable content (VBox) in ScrollArea
                // Find the VBox content (first child of StackPane)
                Node scrollableContent = null;
                for (Node child : formationsContent.getChildren()) {
                    if (child instanceof javafx.scene.layout.VBox) {
                        scrollableContent = child;
                        break;
                    }
                }
                
                // If no VBox found, use the entire StackPane (fallback)
                if (scrollableContent == null) {
                    scrollableContent = formationsContent;
                } else {
                    // Remove VBox from StackPane
                    formationsContent.getChildren().remove(scrollableContent);
                }

                TLScrollArea scrollArea = new TLScrollArea(scrollableContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");

                // Create a new StackPane to hold both scroll area and chatbot overlay
                StackPane containerWithChatbot = new StackPane();
                containerWithChatbot.getChildren().add(scrollArea);
                
                // Add chatbot back as overlay on top (outside scroll area)
                if (chatbotContainer != null) {
                    // Store references as final for lambda
                    final javafx.scene.layout.StackPane finalChatbotContainer = chatbotContainer;
                    final com.skilora.controller.formation.FormationsController finalController = controller;
                    
                    // Ensure chatbot container fills the entire parent and is positioned correctly
                    finalChatbotContainer.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
                    finalChatbotContainer.setPickOnBounds(false);
                    finalChatbotContainer.setMouseTransparent(false);
                    
                    // Make container fill parent StackPane (required for proper positioning)
                    finalChatbotContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    finalChatbotContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                    
                    // Set alignment in parent StackPane
                    javafx.scene.layout.StackPane.setAlignment(finalChatbotContainer, javafx.geometry.Pos.BOTTOM_RIGHT);
                    
                    containerWithChatbot.getChildren().add(finalChatbotContainer);
                    
                    // Ensure chatbot container is properly configured
                    finalChatbotContainer.setVisible(true);
                    finalChatbotContainer.setManaged(true);
                    
                    // Bring to front and ensure visibility
                    Platform.runLater(() -> {
                        finalChatbotContainer.toFront();
                        // Force controller to setup chatbot if container is empty
                        // This handles the case where setupChatbot() hasn't been called yet
                        if (finalController != null && finalChatbotContainer.getChildren().isEmpty()) {
                            try {
                                java.lang.reflect.Method setupMethod = finalController.getClass().getDeclaredMethod("setupChatbot");
                                setupMethod.setAccessible(true);
                                setupMethod.invoke(finalController);
                                logger.info("Chatbot setup triggered after container restoration");
                            } catch (Exception e) {
                                logger.debug("Could not trigger setupChatbot via reflection: {}", e.getMessage());
                            }
                        }
                        logger.info("Chatbot container restored as overlay - visible: {}, managed: {}, children count: {}, parent: {}, size: {}x{}", 
                            finalChatbotContainer.isVisible(), finalChatbotContainer.isManaged(), 
                            finalChatbotContainer.getChildren().size(), finalChatbotContainer.getParent() != null,
                            finalChatbotContainer.getWidth(), finalChatbotContainer.getHeight());
                    });
                } else {
                    logger.warn("Chatbot container not found - chatbot will not be visible");
                }

                cachedFormationsView = containerWithChatbot;
                animateEntry(scrollableContent, 0);

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

            // InterviewsController removed - job-related functionality disabled
            Object controller = loader.getController();
            // if (controller != null) {
            //     controller.setCurrentUser(currentUser);
            // }

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

    private void showTrainingProgressView() {
        centerStack.getChildren().clear();
        if (cachedTrainingProgressView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/TrainingProgressView.fxml"));
                VBox progressContent = loader.load();
                com.skilora.controller.formation.TrainingProgressController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                    controller.setOnTrainingClick(training -> showTrainingDetailView(training));
                    cachedTrainingProgressController = controller;
                }
                TLScrollArea scrollArea = new TLScrollArea(progressContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                cachedTrainingProgressView = scrollArea;
                animateEntry(progressContent, 0);
            } catch (Exception e) {
                logger.error("Failed to load TrainingProgressView", e);
                return;
            }
        } else {
            // Refresh data when view is shown again
            if (cachedTrainingProgressController != null) {
                cachedTrainingProgressController.refresh();
            }
        }
        centerStack.getChildren().add(cachedTrainingProgressView);
    }

    private void showCertificationsView() {
        centerStack.getChildren().clear();
        if (cachedCertificationsView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/CertificationsView.fxml"));
                VBox certsContent = loader.load();
                com.skilora.controller.formation.CertificationsController controller = loader.getController();
                if (controller != null) {
                    controller.setCurrentUser(currentUser);
                    // Set primary stage for FileChooser dialogs
                    if (certsContent.getScene() != null && certsContent.getScene().getWindow() instanceof Stage) {
                        controller.setPrimaryStage((Stage) certsContent.getScene().getWindow());
                    }
                    cachedCertificationsController = controller;
                }
                TLScrollArea scrollArea = new TLScrollArea(certsContent);
                scrollArea.setFitToWidth(true);
                scrollArea.setFitToHeight(true);
                scrollArea.getStyleClass().add("transparent-bg");
                cachedCertificationsView = scrollArea;
                animateEntry(certsContent, 0);
            } catch (Exception e) {
                logger.error("Failed to load CertificationsView", e);
                return;
            }
        } else {
            // Refresh data when view is shown again
            if (cachedCertificationsController != null) {
                cachedCertificationsController.refresh();
            }
        }
        centerStack.getChildren().add(cachedCertificationsView);
    }

    public void showTrainingDetailView(com.skilora.model.entity.formation.Training training) {
        if (training == null) {
            logger.error("showTrainingDetailView called with null training");
            DialogUtils.showError(I18n.get("common.error"), "Training data is missing");
            return;
        }
        
        centerStack.getChildren().clear();
        try {
            // Check if FXML resource exists
            java.net.URL fxmlUrl = getClass().getResource("/com/skilora/view/TrainingDetailView.fxml");
            if (fxmlUrl == null) {
                logger.error("TrainingDetailView.fxml not found in classpath!");
                DialogUtils.showError(I18n.get("common.error"), 
                    "Training detail view file not found. Please check that TrainingDetailView.fxml exists in src/main/resources/com/skilora/view/");
                return;
            }
            
            logger.debug("Loading TrainingDetailView.fxml from: {}", fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox detailContent = loader.load();
            
            com.skilora.controller.formation.TrainingDetailController controller = loader.getController();
            if (controller == null) {
                logger.error("TrainingDetailController is null after loading FXML!");
                DialogUtils.showError(I18n.get("common.error"), 
                    "Failed to initialize training detail controller. Check that fx:controller is set correctly in TrainingDetailView.fxml");
                return;
            }
            
            logger.debug("Controller loaded successfully, setting training: {}", training.getId());
            
            // Get enrollment if user is logged in
            com.skilora.service.formation.TrainingEnrollmentService enrollmentService = 
                com.skilora.service.formation.TrainingEnrollmentService.getInstance();
            com.skilora.model.entity.formation.TrainingEnrollment enrollment = null;
            if (currentUser != null) {
                try {
                    enrollment = enrollmentService.getEnrollment(currentUser.getId(), training.getId()).orElse(null);
                    logger.debug("Enrollment status for user {} and training {}: {}", 
                        currentUser.getId(), training.getId(), enrollment != null ? "enrolled" : "not enrolled");
                } catch (Exception e) {
                    logger.error("Error getting enrollment status", e);
                    // Continue without enrollment - user can still view training details
                }
            }
            
            // Set up controller
                controller.setTraining(training, enrollment);
                controller.setCurrentUser(currentUser);
                controller.setOnBack(() -> {
                    showFormationsView();
                    // Refresh formations view to show updated progress
                    if (cachedFormationsView != null) {
                        cachedFormationsView = null; // Force reload
                        showFormationsView();
                    }
                });
                controller.setOnViewProgress(() -> {
                    showTrainingProgressView();
                    // Refresh progress view
                    if (cachedTrainingProgressView != null) {
                        cachedTrainingProgressView = null; // Force reload
                        showTrainingProgressView();
                    }
                });
            
            // Wrap in scroll area
            TLScrollArea scrollArea = new TLScrollArea(detailContent);
            scrollArea.setFitToWidth(true);
            scrollArea.setFitToHeight(true);
            scrollArea.getStyleClass().add("transparent-bg");
            cachedTrainingDetailView = scrollArea;
            
            animateEntry(detailContent, 0);
            centerStack.getChildren().add(cachedTrainingDetailView);
            
            logger.debug("TrainingDetailView loaded successfully");
        } catch (javafx.fxml.LoadException e) {
            logger.error("FXML LoadException when loading TrainingDetailView", e);
            logger.error("FXML error details: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("FXML cause: {}", e.getCause().getMessage(), e.getCause());
            }
            DialogUtils.showError(I18n.get("common.error"), 
                "Failed to load training detail view: " + e.getMessage() + 
                (e.getCause() != null ? " (" + e.getCause().getMessage() + ")" : ""));
        } catch (Exception e) {
            logger.error("Failed to load TrainingDetailView", e);
            logger.error("Exception type: {}, message: {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
            DialogUtils.showError(I18n.get("common.error"), 
                "Failed to load training details: " + e.getMessage());
        }
    }
}
