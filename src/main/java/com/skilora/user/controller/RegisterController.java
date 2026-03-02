package com.skilora.user.controller;

import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.user.service.AuthService;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLToast;
import com.skilora.user.service.MediaCache;
import com.skilora.utils.Validators;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

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
    @FXML
    private HBox roleToggle;
    @FXML
    private TLButton freelancerBtn;
    @FXML
    private TLButton clientBtn;
    @FXML
    private Label heroBrandLabel;
    @FXML
    private Label heroHeadingLabel;
    @FXML
    private Label heroSubtitleLabel;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Label dividerLabel;
    @FXML
    private Label alreadyAccountLabel;

    private Stage stage;
    private final AuthService authService;
    private Role selectedRole = Role.USER;

    public RegisterController() {
        this.authService = AuthService.getInstance();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        setupEventHandlers();
        initializeVideo();

        if (passwordField != null) {
            passwordField.setShowStrengthIndicator(true);
        }
    }

    private void applyI18n() {
        if (heroBrandLabel != null) heroBrandLabel.setText(I18n.get("register.hero.brand"));
        if (heroHeadingLabel != null) heroHeadingLabel.setText(I18n.get("register.hero.heading"));
        if (heroSubtitleLabel != null) heroSubtitleLabel.setText(I18n.get("register.hero.subtitle"));
        if (formTitleLabel != null) formTitleLabel.setText(I18n.get("register.form.title"));
        if (formSubtitleLabel != null) formSubtitleLabel.setText(I18n.get("register.form.subtitle"));
        if (fullNameField != null) { fullNameField.setLabel(I18n.get("register.fullname")); fullNameField.setPromptText(I18n.get("register.fullname.prompt")); }
        if (usernameField != null) { usernameField.setLabel(I18n.get("register.username")); usernameField.setPromptText(I18n.get("register.username.prompt")); }
        if (emailField != null) { emailField.setLabel(I18n.get("register.email")); emailField.setPromptText(I18n.get("register.email.prompt")); }
        if (passwordField != null) { passwordField.setLabel(I18n.get("register.password")); passwordField.setPromptText(I18n.get("register.password.prompt")); }
        if (confirmPasswordField != null) { confirmPasswordField.setLabel(I18n.get("register.confirm_password")); confirmPasswordField.setPromptText(I18n.get("register.confirm_password.prompt")); }
        if (registerBtn != null) registerBtn.setText(I18n.get("register.sign_up"));
        if (dividerLabel != null) dividerLabel.setText(I18n.get("register.divider"));
        if (alreadyAccountLabel != null) alreadyAccountLabel.setText(I18n.get("register.already_account"));
        if (loginLink != null) loginLink.setText(I18n.get("register.sign_in"));
        if (freelancerBtn != null) freelancerBtn.setText(I18n.get("register.role.freelancer"));
        if (clientBtn != null) clientBtn.setText(I18n.get("register.role.client"));
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

        // ── Real-time inline validation (A-09) ──
        setupFieldValidation(fullNameField, text ->
                text.isEmpty() ? null : Validators.validateFullName(text));

        setupFieldValidation(usernameField, text ->
                text.isEmpty() ? null : Validators.validateUsername(text));

        setupFieldValidation(emailField, text ->
                text.isEmpty() ? null : Validators.validateEmail(text));

        // Password: validated via strength indicator already, but show error for weak
        if (passwordField != null) {
            addDebouncedListener(passwordField.getPasswordField().textProperty(), (obs, o, n) -> {
                if (n == null || n.isEmpty()) { passwordField.clearValidation(); return; }
                String err = Validators.validatePasswordStrength(n);
                if (err != null) { passwordField.setError(err); } else { passwordField.clearValidation(); }
                // Also re-validate confirm if filled
                validateConfirmPassword();
            });
            addDebouncedListener(passwordField.getTextField().textProperty(), (obs, o, n) -> {
                if (n == null || n.isEmpty()) { passwordField.clearValidation(); return; }
                String err = Validators.validatePasswordStrength(n);
                if (err != null) { passwordField.setError(err); } else { passwordField.clearValidation(); }
                validateConfirmPassword();
            });
        }

        // Confirm password: must match
        if (confirmPasswordField != null) {
            ChangeListener<String> confirmListener = (obs, o, n) -> validateConfirmPassword();
            addDebouncedListener(confirmPasswordField.getPasswordField().textProperty(), confirmListener);
            addDebouncedListener(confirmPasswordField.getTextField().textProperty(), confirmListener);
        }
    }

    /** Validates confirm password matches the primary password field. */
    private void validateConfirmPassword() {
        if (confirmPasswordField == null || passwordField == null) return;
        String confirm = confirmPasswordField.getText();
        if (confirm == null || confirm.isEmpty()) {
            confirmPasswordField.clearValidation();
            return;
        }
        String password = passwordField.getText();
        if (!confirm.equals(password)) {
            confirmPasswordField.setError(I18n.get("register.error.password_mismatch"));
        } else {
            confirmPasswordField.clearValidation();
        }
    }

    /**
     * Adds a debounced text change listener to a TLTextField that validates after 300ms of inactivity.
     */
    private void setupFieldValidation(TLTextField field, java.util.function.Function<String, String> validator) {
        if (field == null || field.getControl() == null) return;
        addDebouncedListener(field.getControl().textProperty(), (obs, oldVal, newVal) -> {
            String error = validator.apply(newVal != null ? newVal : "");
            if (error == null && newVal != null && !newVal.isEmpty()) {
                field.clearValidation(); // valid - just remove error, no green
            } else if (error != null) {
                field.setError(error);
            } else {
                field.clearValidation();
            }
        });
    }

    /** Debounces a string property change listener by 300ms. */
    private void addDebouncedListener(javafx.beans.property.StringProperty prop,
                                       ChangeListener<String> action) {
        final Timer[] timer = { null };
        prop.addListener((obs, oldVal, newVal) -> {
            if (timer[0] != null) timer[0].cancel();
            timer[0] = new Timer(true);
            timer[0].schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> action.changed(obs, oldVal, newVal));
                }
            }, 300);
        });
    }

    @FXML
    private void selectFreelancer() {
        selectedRole = Role.USER;
        freelancerBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        clientBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
    }

    @FXML
    private void selectClient() {
        selectedRole = Role.EMPLOYER;
        freelancerBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        clientBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
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
                User newUser = new User(username, password, selectedRole, fullName);
                newUser.setEmail(email);
                authService.register(newUser);
                Platform.runLater(() -> openLoginWithSuccess(username));
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

    /**
     * Opens login view with success toast and pre-filled username after registration.
     */
    private void openLoginWithSuccess(String username) {
        openLoginInternal(username);
    }

    @FXML
    private void openLogin() {
        openLoginInternal(null);
    }

    private void openLoginInternal(String prefilledUsername) {
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
                if (prefilledUsername != null) {
                    controller.setPrefilledUsername(prefilledUsername);
                }
            }

            TLWindow root = new TLWindow(stage, I18n.get("window.title.login"), loginRoot);
            stage.getScene().setRoot(root);

            // Show success toast after scene is set
            if (prefilledUsername != null) {
                Platform.runLater(() -> {
                    if (stage.getScene() != null) {
                        TLToast.success(stage.getScene(),
                                I18n.get("register.success"),
                                I18n.get("register.success.login_prompt"));
                    }
                });
            }
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

                // Derive overlay color from the current theme
                boolean isLight = heroContainer.getScene() != null
                        && heroContainer.getScene().getRoot().getStyleClass().contains("light");
                javafx.scene.paint.Color themeBg = isLight
                        ? javafx.scene.paint.Color.web("#ffffff")
                        : javafx.scene.paint.Color.web("#191a1c");
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
