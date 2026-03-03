package com.skilora.user.controller;

import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.user.service.AuthService;
import com.skilora.user.service.EmailVerificationService;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLSwitch;
import com.skilora.user.service.MediaCache;
import com.skilora.utils.Validators;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
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
    private TLButton verifyEmailBtn;
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
    @FXML
    private Label emailVerifiedLabel;

    private Stage stage;
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private Role selectedRole = Role.USER;
    private boolean emailVerified = false;
    private String verifiedEmail = null;

    public RegisterController() {
        this.authService = AuthService.getInstance();
        this.emailVerificationService = EmailVerificationService.getInstance();
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

        // Check if email has been verified
        if (!emailVerified || verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(email)) {
            showError(I18n.get("register.error.email_not_verified"));
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

        // Show Terms of Service dialog
        showTermsOfServiceDialog(() -> {
            // User accepted terms, proceed with registration
            completeRegistration(fullName, username, email, password);
        });
    }

    /**
     * Complete the registration after terms are accepted.
     */
    private void completeRegistration(String fullName, String username, String email, String password) {
        registerBtn.setDisable(true);
        registerBtn.setText(I18n.get("register.creating_account"));

        AppThreadPool.execute(() -> {
            try {
                User newUser = new User(username, password, selectedRole, fullName);
                newUser.setEmail(email);
                authService.register(newUser);
                Platform.runLater(() -> showAccountCreatedDialog(username));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    registerBtn.setDisable(false);
                    registerBtn.setText(I18n.get("register.sign_up"));
                });
            }
        });
    }

    /**
     * Show Terms of Service dialog.
     */
    private void showTermsOfServiceDialog(Runnable onAccept) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(stage);
        dialog.setDialogTitle(I18n.get("register.terms.title"));
        dialog.setDescription(I18n.get("register.terms.description"));

        // Terms content
        VBox contentBox = new VBox(12);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(8, 0, 8, 0));

        Label termsText = new Label(I18n.get("register.terms.content"));
        termsText.setWrapText(true);
        termsText.getStyleClass().add("text-sm");

        ScrollPane scrollPane = new ScrollPane(termsText);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8px;");

        // Agreement switch
        HBox agreementBox = new HBox(12);
        agreementBox.setAlignment(Pos.CENTER_LEFT);
        Label agreementLabel = new Label(I18n.get("register.terms.agree_label"));
        agreementLabel.getStyleClass().add("text-sm");
        TLSwitch agreementSwitch = new TLSwitch();
        agreementBox.getChildren().addAll(agreementLabel, agreementSwitch);

        contentBox.getChildren().addAll(scrollPane, agreementBox);
        dialog.setDialogContent(contentBox);

        ButtonType acceptBtn = new ButtonType(I18n.get("register.terms.accept"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType declineBtn = new ButtonType(I18n.get("register.terms.decline"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.addButton(declineBtn);
        dialog.addButton(acceptBtn);

        // Disable accept button until switch is on
        javafx.scene.Node acceptButton = dialog.getDialogPane().lookupButton(acceptBtn);
        acceptButton.setDisable(true);
        agreementSwitch.selectedProperty().addListener((obs, old, newVal) -> {
            acceptButton.setDisable(!newVal);
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == acceptBtn && agreementSwitch.isSelected()) {
                onAccept.run();
            } else {
                // User declined terms, re-enable register button
                registerBtn.setDisable(false);
            }
        });
    }

    /**
     * Show Account Created success dialog with login button.
     */
    private void showAccountCreatedDialog(String username) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(stage);
        dialog.setDialogTitle(I18n.get("register.success.title"));

        // Success content
        VBox contentBox = new VBox(16);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(16, 0, 16, 0));

        // Success icon
        SVGPath successIcon = new SVGPath();
        successIcon.setContent(SvgIcons.CHECK_CIRCLE);
        successIcon.getStyleClass().add("svg-path");
        successIcon.setScaleX(3.0);
        successIcon.setScaleY(3.0);
        successIcon.setStyle("-fx-fill: -fx-success;");

        HBox iconBox = new HBox(successIcon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPadding(new Insets(16));

        Label successMessage = new Label(I18n.get("register.success.message"));
        successMessage.setWrapText(true);
        successMessage.getStyleClass().addAll("text-base", "text-center");

        contentBox.getChildren().addAll(iconBox, successMessage);
        dialog.setDialogContent(contentBox);

        ButtonType loginBtn = new ButtonType(I18n.get("register.success.login_button"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.addButton(loginBtn);

        dialog.showAndWait().ifPresent(result -> {
            openLoginWithSuccess(username);
        });
    }

    /**
     * Handle email verification button click.
     */
    @FXML
    private void handleVerifyEmail() {
        String email = emailField.getText();

        // Validate email format
        String emailError = Validators.validateEmail(email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        // Check if already verified
        if (emailVerified && verifiedEmail != null && verifiedEmail.equalsIgnoreCase(email)) {
            TLToast.success(getScene(), I18n.get("register.email.already_verified"), "");
            return;
        }

        // Generate and send OTP
        String otp = emailVerificationService.generateOTP();
        emailVerificationService.storeVerificationOTP(email, otp);

        // Show OTP dialog
        showEmailVerificationDialog(email, otp);
    }

    /**
     * Show email verification dialog with OTP input.
     */
    private void showEmailVerificationDialog(String email, String expectedOtp) {
        TLDialog<String> dialog = new TLDialog<>();
        dialog.initOwner(stage);
        dialog.setDialogTitle(I18n.get("register.email.verify_title"));
        dialog.setDescription(I18n.get("register.email.verify_description", email));

        // OTP input
        VBox formBox = new VBox(16);
        formBox.setAlignment(Pos.CENTER);

        TLTextField otpField = new TLTextField();
        otpField.setPromptText(I18n.get("register.email.otp_prompt"));
        otpField.setMaxWidth(200);

        Label timerLabel = new Label();
        timerLabel.getStyleClass().add("text-sm, text-muted");

        Label attemptsLabel = new Label();
        attemptsLabel.getStyleClass().add("text-sm");

        formBox.getChildren().addAll(otpField, timerLabel, attemptsLabel);
        dialog.setDialogContent(formBox);

        dialog.addButton(ButtonType.CANCEL);
        ButtonType verifyBtn = new ButtonType(I18n.get("register.email.verify_button"), javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.addButton(verifyBtn);

        // Setup countdown timer
        final int[] remainingSeconds = {emailVerificationService.getExpirySeconds()};
        final int[] remainingAttempts = {emailVerificationService.getMaxAttempts()};

        Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    remainingSeconds[0]--;
                    if (remainingSeconds[0] <= 0) {
                        timer.cancel();
                        timerLabel.setText(I18n.get("register.email.otp_expired"));
                        dialog.getDialogPane().lookupButton(verifyBtn).setDisable(true);
                    } else {
                        int minutes = remainingSeconds[0] / 60;
                        int seconds = remainingSeconds[0] % 60;
                        timerLabel.setText(String.format("%s: %02d:%02d", 
                            I18n.get("register.email.time_remaining"), minutes, seconds));
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);

        // Update attempts label
        remainingAttempts[0] = emailVerificationService.getRemainingAttempts(email);
        attemptsLabel.setText(I18n.get("register.email.attempts_remaining", remainingAttempts[0]));

        // Validate OTP on verify
        dialog.getDialogPane().lookupButton(verifyBtn).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String enteredOtp = otpField.getText();
                if (enteredOtp == null || enteredOtp.isEmpty()) {
                    otpField.setError(I18n.get("register.email.otp_required"));
                    event.consume();
                    return;
                }

                boolean verified = emailVerificationService.verifyOTP(email, enteredOtp);
                if (!verified) {
                    remainingAttempts[0] = emailVerificationService.getRemainingAttempts(email);
                    attemptsLabel.setText(I18n.get("register.email.attempts_remaining", remainingAttempts[0]));

                    if (emailVerificationService.isVerificationLocked(email)) {
                        otpField.setError(I18n.get("register.email.max_attempts_reached"));
                        timer.cancel();
                        dialog.getDialogPane().lookupButton(verifyBtn).setDisable(true);
                    } else {
                        otpField.setError(I18n.get("register.email.otp_invalid"));
                    }
                    event.consume();
                }
            }
        );

        dialog.setResultConverter(bt -> {
            if (bt == verifyBtn) {
                return otpField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        timer.cancel();

        result.ifPresent(enteredOtp -> {
            boolean verified = emailVerificationService.verifyOTP(email, enteredOtp);
            if (verified) {
                emailVerified = true;
                verifiedEmail = email;
                updateEmailVerificationStatus();
                TLToast.success(getScene(), I18n.get("register.email.verified_success"), "");
            }
        });
    }

    /**
     * Update UI to show email verification status.
     */
    private void updateEmailVerificationStatus() {
        if (emailVerifiedLabel != null) {
            emailVerifiedLabel.setText(I18n.get("register.email.verified"));
            emailVerifiedLabel.getStyleClass().add("text-success");
            emailVerifiedLabel.setVisible(true);
            emailVerifiedLabel.setManaged(true);
        }
        if (verifyEmailBtn != null) {
            verifyEmailBtn.setText(I18n.get("register.email.verified_button"));
            verifyEmailBtn.setVariant(TLButton.ButtonVariant.SUCCESS);
        }
        updateRegisterButtonState();
    }

    /**
     * Update register button state based on email verification.
     */
    private void updateRegisterButtonState() {
        if (registerBtn != null) {
            registerBtn.setDisable(!emailVerified);
        }
    }

    /**
     * Get current scene from any control.
     */
    private javafx.scene.Scene getScene() {
        if (registerBtn != null && registerBtn.getScene() != null) {
            return registerBtn.getScene();
        }
        if (emailField != null && emailField.getScene() != null) {
            return emailField.getScene();
        }
        return null;
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
