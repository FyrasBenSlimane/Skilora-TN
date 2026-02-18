package com.skilora.user.controller;

import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.user.service.AuthService;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLButton;
import com.skilora.user.service.MediaCache;
import com.skilora.utils.Validators;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * RegisterController - FXML Controller for RegisterView.fxml
 * 
 * Handles user interaction and business logic for registration.
 */
public class RegisterController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private StackPane heroContainer;
    @FXML
    private Label errorLabel;
    @FXML
    private TLTextField fullNameField;
    @FXML
    private TLTextField usernameField;
    @FXML
    private TLTextField emailField;
    @FXML
    private TLPasswordField passwordField;
    @FXML
    private TLPasswordField confirmPasswordField;
    @FXML
    private TLButton registerBtn;
    @FXML
    private TLButton loginLink;

    private Stage stage;
    private final AuthService authService;

    public RegisterController() {
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
        // Enable "Enter" key to trigger registration
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> hitEnter = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleRegister();
            }
        };

        if (fullNameField != null && fullNameField.getControl() != null) {
            fullNameField.getControl().setOnKeyPressed(hitEnter);
        }
        if (usernameField != null && usernameField.getControl() != null) {
            usernameField.getControl().setOnKeyPressed(hitEnter);
        }
        if (emailField != null && emailField.getControl() != null) {
            emailField.getControl().setOnKeyPressed(hitEnter);
        }
        if (passwordField != null && passwordField.getControl() != null) {
            passwordField.getControl().setOnKeyPressed(hitEnter);
        }
        if (confirmPasswordField != null && confirmPasswordField.getControl() != null) {
            confirmPasswordField.getControl().setOnKeyPressed(hitEnter);
        }
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Full name validation
        String fullNameError = Validators.validateFullName(fullName);
        if (fullNameError != null) {
            showError(fullNameError);
            return;
        }
        // Username validation
        String usernameError = Validators.validateUsername(username);
        if (usernameError != null) {
            showError(usernameError);
            return;
        }
        // Email validation
        String emailError = Validators.validateEmail(email);
        if (emailError != null) {
            showError(emailError);
            return;
        }
        // Password strength validation
        String passwordError = Validators.validatePasswordStrength(password);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }
        // Confirm password
        if (!password.equals(confirmPassword)) {
            showError(I18n.get("register.error.password_mismatch"));
            return;
        }

        registerBtn.setDisable(true);
        registerBtn.setText(I18n.get("register.creating_account"));

        AppThreadPool.execute(() -> {
            try {
                User newUser = new User(username, password, Role.USER, fullName);
                newUser.setEmail(email);
                authService.register(newUser);
                Platform.runLater(this::openLogin);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    registerBtn.setDisable(false);
                    registerBtn.setText(I18n.get("register.sign_up"));
                });
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void openLogin() {
        if (stage == null && loginLink.getScene() != null) {
            stage = (Stage) loginLink.getScene().getWindow();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/skilora/view/user/LoginView.fxml"));
            javafx.scene.layout.HBox loginRoot = loader.load();

            LoginController controller = loader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }

            TLWindow root = new TLWindow(stage, I18n.get("window.title.login"), loginRoot);
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to load LoginView: " + e.getMessage(), e);
        }
    }

    private void initializeVideo() {
        try {
            MediaCache cache = MediaCache.getInstance();
            Media media = cache.getHeroMedia();

            if (media == null || heroContainer == null) {
                return;
            }

            MediaPlayer mediaPlayer = cache.createReadyPlayer();
            if (mediaPlayer == null) {
                return;
            }

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(false);
            mediaView.setSmooth(true);
            mediaView.setCache(true);
            mediaView.setCacheHint(javafx.scene.CacheHint.SPEED);

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

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(videoWrapper.widthProperty());
            clip.heightProperty().bind(videoWrapper.heightProperty());
            videoWrapper.setClip(clip);

            mediaPlayer.setOnReady(videoWrapper::requestLayout);
            videoWrapper.setOpacity(0.6);

            Platform.runLater(() -> {
                heroContainer.getChildren().add(0, videoWrapper);

                Rectangle overlay = new Rectangle();
                overlay.widthProperty().bind(heroContainer.widthProperty());
                overlay.heightProperty().bind(heroContainer.heightProperty());

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

                javafx.scene.Node content = heroContainer.getChildren().get(heroContainer.getChildren().size() - 1);
                if (content instanceof javafx.scene.layout.VBox) {
                    content.toFront();
                }
            });

        } catch (Exception e) {
            // Video is optional - don't crash registration if it fails
        }
    }
}
