package com.skilora.user.controller;

import com.skilora.user.entity.User;
import com.skilora.user.service.AuthService;
import com.skilora.ui.MainView;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.utils.WindowConfig;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLButton;
import com.skilora.user.service.MediaCache;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * LoginController - Handles interaction for LoginView.fxml
 */
public class LoginController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private StackPane heroContainer;
    @FXML
    private Label errorLabel;
    @FXML
    private TLTextField usernameField;
    @FXML
    private TLPasswordField passwordField;
    @FXML
    private TLButton loginBtn;
    @FXML
    private TLButton biometricBtn;
    @FXML
    private TLButton forgotPasswordLink;
    @FXML
    private TLButton registerLink; // Optional if only used for action

    private MediaPlayer activeMediaPlayer; // Track for disposal

    private Stage stage;
    private final AuthService authService;

    public LoginController() {
        this.authService = AuthService.getInstance();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        initializeVideo();
    }

    private void setupEventHandlers() {
        // "Enter" key shortcuts
        if (usernameField != null && usernameField.getControl() != null) {
            usernameField.getControl().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER)
                    handleLogin();
            });
        }
        if (passwordField != null && passwordField.getControl() != null) {
            passwordField.getControl().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER)
                    handleLogin();
            });
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Null/empty validation
        if (username == null || username.trim().isEmpty()) {
            showError(I18n.get("login.error.username_required"));
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError(I18n.get("login.error.password_required"));
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText(I18n.get("login.signing_in"));

        AppThreadPool.execute(() -> {
            try {
                // All DB calls run on background thread
                boolean lockedOut = authService.isLockedOut(username);
                if (lockedOut) {
                    int minutes = authService.getRemainingLockoutMinutes(username);
                    Platform.runLater(() -> {
                        showError(I18n.get("login.error.locked").replace("{0}", String.valueOf(minutes)));
                        loginBtn.setDisable(false);
                        loginBtn.setText(I18n.get("login.sign_in"));
                    });
                    return;
                }

                Optional<User> user = authService.login(username, password);

                // Check lockout state on background thread (not UI thread)
                boolean lockedAfterAttempt = !user.isPresent() && authService.isLockedOut(username);
                int lockMinutes = lockedAfterAttempt ? authService.getRemainingLockoutMinutes(username) : 0;

                Platform.runLater(() -> {
                    if (user.isPresent()) {
                        openDashboard(user.get());
                    } else {
                        if (lockedAfterAttempt) {
                            showError(I18n.get("login.error.too_many_attempts").replace("{0}", String.valueOf(lockMinutes)));
                        } else {
                            showError(I18n.get("login.error.invalid_credentials"));
                        }
                        loginBtn.setDisable(false);
                        loginBtn.setText(I18n.get("login.sign_in"));
                    }
                });
            } catch (Exception e) {
                logger.error("Login failed unexpectedly", e);
                Platform.runLater(() -> {
                    showError(I18n.get("login.error.unexpected"));
                    loginBtn.setDisable(false);
                    loginBtn.setText(I18n.get("login.sign_in"));
                });
            }
        });
    }

    @FXML
    private void handleBiometricLogin() {
        if (stage == null && usernameField.getScene() != null) {
            stage = (Stage) usernameField.getScene().getWindow();
        }

        String username = usernameField.getText();
        String targetUser = username.isEmpty() ? null : username;

        BiometricAuthController.showDialog(stage, targetUser, (detectedUsername) -> {
            AppThreadPool.execute(() -> {
                try {
                    Optional<User> user = authService.getUser(detectedUsername);
                    Platform.runLater(() -> {
                        if (user.isPresent()) {
                            openDashboard(user.get());
                        } else {
                            showError(I18n.get("login.error.user_not_found").replace("{0}", detectedUsername));
                        }
                    });
                } catch (Exception e) {
                    logger.error("Biometric login failed unexpectedly", e);
                    Platform.runLater(() -> {
                        showError(I18n.get("login.error.unexpected"));
                    });
                }
            });
        });
    }

    @FXML
    private void openForgotPassword() {
        if (stage == null && usernameField.getScene() != null) {
            stage = (Stage) usernameField.getScene().getWindow();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/skilora/view/user/ForgotPasswordView.fxml"));
            javafx.scene.layout.VBox forgotRoot = loader.load();

            ForgotPasswordController controller = loader.getController();
            if (controller != null) {
                controller.setOnBack(() -> {
                    // Navigate back to login
                    try {
                        javafx.fxml.FXMLLoader loginLoader = new javafx.fxml.FXMLLoader(
                                getClass().getResource("/com/skilora/view/user/LoginView.fxml"));
                        javafx.scene.layout.HBox loginRoot = loginLoader.load();

                        LoginController loginController = loginLoader.getController();
                        if (loginController != null) {
                            loginController.setStage(stage);
                        }

                        TLWindow root = new TLWindow(stage, I18n.get("window.title.login"), loginRoot);
                        stage.getScene().setRoot(root);
                    } catch (Exception ex) {
                        logger.error("Failed to load LoginView: " + ex.getMessage(), ex);
                    }
                });
            }

            TLWindow root = new TLWindow(stage, I18n.get("window.title.forgot_password"), forgotRoot);
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to load ForgotPasswordView: " + e.getMessage(), e);
        }
    }

    @FXML
    private void openRegister() {
        if (stage == null && registerLink.getScene() != null) {
            stage = (Stage) registerLink.getScene().getWindow();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/skilora/view/user/RegisterView.fxml"));
            javafx.scene.layout.HBox registerRoot = loader.load();

            RegisterController controller = loader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }

            TLWindow root = new TLWindow(stage, I18n.get("window.title.create_account"), registerRoot);
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to load RegisterView: " + e.getMessage(), e);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void openDashboard(User user) {
        if (stage == null) {
            stage = (Stage) loginBtn.getScene().getWindow();
        }

        // Dispose media player to free native resources
        if (activeMediaPlayer != null) {
            activeMediaPlayer.stop();
            activeMediaPlayer.dispose();
            activeMediaPlayer = null;
        }
        MediaCache.getInstance().dispose();

        MainView dashboardView = new MainView(user);
        dashboardView.setupWindowControls(stage);

        Scene scene = new Scene(dashboardView, 1200, 800);
        WindowConfig.configureScene(scene);

        stage.setScene(scene);
        stage.centerOnScreen();

        // Check biometric data after dashboard loads — show reminder if not enrolled
        checkBiometricReminder(user);
    }

    /**
     * Checks if the user has registered biometric data.
     * If not, shows a styled reminder dialog offering to set up face recognition.
     */
    private void checkBiometricReminder(User user) {
        AppThreadPool.execute(() -> {
            try {
                boolean hasBiometric = com.skilora.user.service.BiometricService.getInstance()
                        .hasBiometricData(user.getUsername());
                if (!hasBiometric) {
                    // Small delay so the dashboard fully renders first
                    Thread.sleep(800);
                    Platform.runLater(() -> showBiometricReminderDialog(user));
                }
            } catch (Exception e) {
                // Silently ignore — not critical
            }
        });
    }

    /**
     * Shows a styled reminder dialog encouraging the user to register biometric data.
     */
    private void showBiometricReminderDialog(User user) {
        com.skilora.framework.components.TLDialog<ButtonType> dialog = new com.skilora.framework.components.TLDialog<>();
        dialog.initOwner(stage);

        // Header
        dialog.setDialogTitle(com.skilora.utils.I18n.get("login.biometric.title"));
        dialog.setDescription(com.skilora.utils.I18n.get("login.biometric.description"));

        // Content — icon + benefits list
        VBox contentBox = new VBox(16);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // Face scan icon
        SVGPath faceIcon = new SVGPath();
        faceIcon.setContent("M9 11.75c-.69 0-1.25.56-1.25 1.25s.56 1.25 1.25 1.25 1.25-.56 1.25-1.25-.56-1.25-1.25-1.25zm6 0c-.69 0-1.25.56-1.25 1.25s.56 1.25 1.25 1.25 1.25-.56 1.25-1.25-.56-1.25-1.25-1.25zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8 0-.29.02-.58.05-.86 2.36-1.05 4.23-2.98 5.21-5.37C11.07 8.33 14.05 10 17.42 10c.78 0 1.53-.09 2.25-.26.21.71.33 1.47.33 2.26 0 4.41-3.59 8-8 8z");
        faceIcon.getStyleClass().add("svg-path");
        faceIcon.setScaleX(2.0);
        faceIcon.setScaleY(2.0);

        HBox iconRow = new HBox(faceIcon);
        iconRow.setAlignment(Pos.CENTER);
        iconRow.setPadding(new Insets(8, 0, 8, 0));

        // Benefits
        Label benefit1 = new Label(com.skilora.utils.I18n.get("login.biometric.feature.instant"));
        benefit1.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, "-fx-green"));
        benefit1.getStyleClass().add("text-sm");
        Label benefit2 = new Label(com.skilora.utils.I18n.get("login.biometric.feature.security"));
        benefit2.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, "-fx-green"));
        benefit2.getStyleClass().add("text-sm");
        Label benefit3 = new Label(com.skilora.utils.I18n.get("login.biometric.feature.easy"));
        benefit3.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, "-fx-green"));
        benefit3.getStyleClass().add("text-sm");

        VBox benefitsBox = new VBox(8, benefit1, benefit2, benefit3);
        benefitsBox.setPadding(new Insets(0, 0, 0, 8));

        contentBox.getChildren().addAll(iconRow, benefitsBox);
        dialog.setContent(contentBox);

        // Buttons
        ButtonType setupNow = new ButtonType(com.skilora.utils.I18n.get("login.biometric.setup_now"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType remindLater = new ButtonType(com.skilora.utils.I18n.get("login.biometric.later"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(remindLater, setupNow);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == setupNow) {
            // Open biometric registration
            BiometricAuthController.showRegistrationDialog(stage, user.getUsername(), (registeredUser) -> {
                com.skilora.utils.DialogUtils.showInfo(
                        com.skilora.utils.I18n.get("login.biometric.success_title"),
                        com.skilora.utils.I18n.get("login.biometric.success_msg"));
            });
        }
    }

    private void initializeVideo() {
        try {
            MediaCache cache = com.skilora.user.service.MediaCache.getInstance();
            Media media = cache.getHeroMedia();

            if (media == null || heroContainer == null) {
                logger.error("Video or container not available");
                return;
            }

            // Use the optimized player from cache
            MediaPlayer mediaPlayer = cache.createReadyPlayer();
            if (mediaPlayer == null) {
                logger.error("Failed to create media player");
                return;
            }
            activeMediaPlayer = mediaPlayer;

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(false);
            mediaView.setSmooth(true);

            // Enable hardware acceleration
            mediaView.setCache(true);
            mediaView.setCacheHint(javafx.scene.CacheHint.SPEED);

            // Custom resizing pane for cover-fit behavior
            Pane videoWrapper = new Pane() {
                @Override
                protected void layoutChildren() {
                    double w = getWidth();
                    double h = getHeight();
                    if (w <= 0 || h <= 0)
                        return;

                    double videoW = media.getWidth();
                    double videoH = media.getHeight();

                    if (videoW <= 0 || videoH <= 0) {
                        mediaView.setFitWidth(w);
                        mediaView.setFitHeight(h);
                        return;
                    }

                    // Cover-fit calculation
                    double scaleX = w / videoW;
                    double scaleY = h / videoH;
                    double scale = Math.max(scaleX, scaleY);

                    double finalW = videoW * scale;
                    double finalH = videoH * scale;

                    mediaView.setFitWidth(finalW);
                    mediaView.setFitHeight(finalH);
                    mediaView.setX((w - finalW) / 2);
                    mediaView.setY((h - finalH) / 2);
                }
            };

            videoWrapper.getChildren().add(mediaView);
            videoWrapper.setCache(true);
            videoWrapper.setCacheHint(javafx.scene.CacheHint.SPEED);

            // Clip to bounds
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(videoWrapper.widthProperty());
            clip.heightProperty().bind(videoWrapper.heightProperty());
            videoWrapper.setClip(clip);

            // Trigger layout when media is ready
            mediaPlayer.setOnReady(videoWrapper::requestLayout);
            videoWrapper.setOpacity(0.6);

            // Add video elements to container
            Platform.runLater(() -> {
                // Insert video at bottom layer
                heroContainer.getChildren().add(0, videoWrapper);

                // Add gradient overlay
                Rectangle overlay = new Rectangle();
                overlay.widthProperty().bind(heroContainer.widthProperty());
                overlay.heightProperty().bind(heroContainer.heightProperty());

                // Derive overlay color from the current theme background
                // Auth hero overlay always uses dark tint for text readability over video
                javafx.scene.paint.Color themeBg = javafx.scene.paint.Color.web("#000000");
                javafx.scene.paint.Stop[] stops = new javafx.scene.paint.Stop[] {
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.color(themeBg.getRed(), themeBg.getGreen(), themeBg.getBlue(), 0.9)),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.color(themeBg.getRed(), themeBg.getGreen(), themeBg.getBlue(), 0.4)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.color(themeBg.getRed(), themeBg.getGreen(), themeBg.getBlue(), 0.1))
                };
                javafx.scene.paint.LinearGradient lg = new javafx.scene.paint.LinearGradient(
                        0, 1, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
                overlay.setFill(lg);
                overlay.setCache(true);

                heroContainer.getChildren().add(1, overlay);

                // Ensure text content stays on top
                javafx.scene.Node content = heroContainer.getChildren().get(heroContainer.getChildren().size() - 1);
                if (content instanceof javafx.scene.layout.VBox) {
                    content.toFront();
                }
            });

        } catch (Exception e) {
            logger.error("Video Init Failed: " + e.getMessage(), e);
        }
    }
}
