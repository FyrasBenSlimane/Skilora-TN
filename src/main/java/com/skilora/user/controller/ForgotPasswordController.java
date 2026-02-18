package com.skilora.user.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLOtpInput;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLTextField;
import com.skilora.user.entity.User;
import com.skilora.user.service.OtpService;
import com.skilora.user.service.UserService;
import com.skilora.community.service.EmailService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.SvgIcons;
import com.skilora.utils.Validators;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.utils.I18n;


import java.util.Optional;

/**
 * ForgotPasswordController - Handle password reset flow with DB-backed OTP.
 * 3-step flow: Email → OTP Verification → New Password
 * Features: step indicator, countdown timer, loading states, animations.
 */
public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    // ===== FXML - Header & Steps =====
    @FXML private HBox stepIndicator;
    @FXML private StackPane step1Circle, step2Circle, step3Circle;
    @FXML private Label step1Label, step2Label, step3Label;
    @FXML private Label step1Text, step2Text, step3Text;
    @FXML private Region connector1, connector2;
    @FXML private Label iconLabel, titleLabel, subtitleLabel;

    // ===== FXML - Content =====
    @FXML private VBox contentArea;
    @FXML private VBox emailContainer;
    @FXML private Label fieldLabel;
    @FXML private TLTextField emailField;
    @FXML private VBox otpContainer;
    @FXML private Label otpFieldLabel;
    @FXML private TLOtpInput otpInput;
    @FXML private VBox passwordContainer;
    @FXML private TLPasswordField newPasswordField;
    @FXML private TLPasswordField confirmPasswordField;
    @FXML private Label errorLabel, successLabel;
    @FXML private HBox loadingBox;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label loadingLabel;

    // ===== FXML - Actions =====
    @FXML private TLButton sendBtn, backBtn, resendBtn;
    @FXML private VBox resendSection;
    @FXML private Label notReceivedLabel;

    // ===== State =====
    private Runnable onBack;
    private String targetEmail;
    private int targetUserId;

    private enum Step { EMAIL, OTP, NEW_PASSWORD }
    private Step currentStep = Step.EMAIL;

    private static final int OTP_EXPIRY_SECONDS = OtpService.getInstance().getExpirySeconds();
    private Timeline countdownTimeline;
    private int remainingSeconds;

    // Style constants for step indicator (use theme tokens)
    private static final String STEP_ACTIVE_BG = "-fx-background-color: -fx-primary; -fx-background-radius: 16;";
    private static final String STEP_ACTIVE_TEXT = "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -fx-primary-foreground;";
    private static final String STEP_ACTIVE_LABEL = "-fx-font-size: 11px; -fx-text-fill: -fx-primary;";
    private static final String STEP_DONE_BG = "-fx-background-color: -fx-green; -fx-background-radius: 16;";
    private static final String STEP_DONE_TEXT = "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -fx-green-foreground;";
    private static final String STEP_DONE_LABEL = "-fx-font-size: 11px; -fx-text-fill: -fx-green;";
    private static final String STEP_INACTIVE_BG = "-fx-background-color: -fx-muted; -fx-background-radius: 16;";
    private static final String STEP_INACTIVE_TEXT = "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -fx-muted-foreground;";
    private static final String STEP_INACTIVE_LABEL = "-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;";
    private static final String CONNECTOR_DONE = "-fx-background-color: -fx-green; -fx-background-radius: 1;";
    private static final String CONNECTOR_PENDING = "-fx-background-color: -fx-muted; -fx-background-radius: 1;";

    private final OtpService otpService = OtpService.getInstance();

    @FXML
    public void initialize() {
        // Apply i18n to labels
        if (iconLabel != null) iconLabel.setGraphic(SvgIcons.icon(SvgIcons.LOCK, 24));
        if (titleLabel != null) titleLabel.setText(I18n.get("forgot.ui.title"));
        if (subtitleLabel != null) subtitleLabel.setText(I18n.get("forgot.ui.subtitle"));
        if (fieldLabel != null) fieldLabel.setText(I18n.get("forgot.ui.email_label"));
        if (notReceivedLabel != null) notReceivedLabel.setText(I18n.get("forgot.ui.not_received"));
        if (sendBtn != null) sendBtn.setText(I18n.get("forgot.send_code"));
        if (backBtn != null) {
            backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
            backBtn.setText(I18n.get("forgot.back_to_login"));
        }
        if (resendBtn != null) resendBtn.setText(I18n.get("forgot.resend"));
        if (emailField != null) emailField.setPromptText(I18n.get("forgot.ui.email_placeholder"));

        // Step indicator labels
        if (step1Text != null) step1Text.setText(I18n.get("forgot.step.email"));
        if (step2Text != null) step2Text.setText(I18n.get("forgot.step.verify"));
        if (step3Text != null) step3Text.setText(I18n.get("forgot.step.reset"));

        // Enable password strength indicator on new password field
        if (newPasswordField != null) {
            newPasswordField.setShowStrengthIndicator(true);
        }

        // Enter key support
        if (emailField != null && emailField.getControl() != null) {
            emailField.getControl().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) handleSendReset();
            });
        }
        // OTP input: auto-submit when all 6 digits entered + enter key support
        if (otpInput != null) {
            otpInput.setOnComplete(this::handleSendReset);
            otpInput.setOnEnter(this::handleSendReset);
        }
        if (newPasswordField != null && newPasswordField.getControl() != null) {
            newPasswordField.getControl().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) handleSendReset();
            });
        }
        if (confirmPasswordField != null && confirmPasswordField.getControl() != null) {
            confirmPasswordField.getControl().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) handleSendReset();
            });
        }

        updateStepIndicator(Step.EMAIL);
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    // ==================== STEP INDICATOR ====================

    private void updateStepIndicator(Step step) {
        switch (step) {
            case EMAIL -> {
                setStepStyle(step1Circle, step1Label, step1Text, "1", STEP_ACTIVE_BG, STEP_ACTIVE_TEXT, STEP_ACTIVE_LABEL);
                setStepStyle(step2Circle, step2Label, step2Text, "2", STEP_INACTIVE_BG, STEP_INACTIVE_TEXT, STEP_INACTIVE_LABEL);
                setStepStyle(step3Circle, step3Label, step3Text, "3", STEP_INACTIVE_BG, STEP_INACTIVE_TEXT, STEP_INACTIVE_LABEL);
                if (connector1 != null) connector1.setStyle(CONNECTOR_PENDING);
                if (connector2 != null) connector2.setStyle(CONNECTOR_PENDING);
            }
            case OTP -> {
                setStepStyle(step1Circle, step1Label, step1Text, "\u2713", STEP_DONE_BG, STEP_DONE_TEXT, STEP_DONE_LABEL);
                setStepStyle(step2Circle, step2Label, step2Text, "2", STEP_ACTIVE_BG, STEP_ACTIVE_TEXT, STEP_ACTIVE_LABEL);
                setStepStyle(step3Circle, step3Label, step3Text, "3", STEP_INACTIVE_BG, STEP_INACTIVE_TEXT, STEP_INACTIVE_LABEL);
                if (connector1 != null) connector1.setStyle(CONNECTOR_DONE);
                if (connector2 != null) connector2.setStyle(CONNECTOR_PENDING);
            }
            case NEW_PASSWORD -> {
                setStepStyle(step1Circle, step1Label, step1Text, "\u2713", STEP_DONE_BG, STEP_DONE_TEXT, STEP_DONE_LABEL);
                setStepStyle(step2Circle, step2Label, step2Text, "\u2713", STEP_DONE_BG, STEP_DONE_TEXT, STEP_DONE_LABEL);
                setStepStyle(step3Circle, step3Label, step3Text, "3", STEP_ACTIVE_BG, STEP_ACTIVE_TEXT, STEP_ACTIVE_LABEL);
                if (connector1 != null) connector1.setStyle(CONNECTOR_DONE);
                if (connector2 != null) connector2.setStyle(CONNECTOR_DONE);
            }
        }
    }

    private void setStepStyle(StackPane circle, Label numLabel, Label textLabel,
                               String text, String circleBg, String numStyle, String textStyle) {
        if (circle != null) circle.setStyle(circleBg);
        if (numLabel != null) { numLabel.setText(text); numLabel.setStyle(numStyle); }
        if (textLabel != null) textLabel.setStyle(textStyle);
    }

    // ==================== MAIN ACTION HANDLER ====================

    @FXML
    private void handleSendReset() {
        hideMessages();
        switch (currentStep) {
            case EMAIL -> handleSendEmail();
            case OTP -> handleVerifyOtp();
            case NEW_PASSWORD -> handleNewPassword();
        }
    }

    // ==================== STEP 1: SEND EMAIL ====================

    private void handleSendEmail() {
        String email = emailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showError(I18n.get("forgot.email_required"));
            shakeNode(emailField);
            return;
        }
        if (!Validators.email(email)) {
            showError(I18n.get("forgot.email_invalid"));
            shakeNode(emailField);
            return;
        }

        showLoading(I18n.get("forgot.sending"));
        sendBtn.setDisable(true);

        AppThreadPool.execute(() -> {
            try {
                Optional<User> userOpt = UserService.getInstance().findByEmail(email);
                if (userOpt.isEmpty()) {
                    Platform.runLater(() -> {
                        hideLoading();
                        showSuccess(I18n.get("forgot.sent_if_exists"));
                        sendBtn.setDisable(false);
                    });
                    return;
                }

                targetEmail = email;
                targetUserId = userOpt.get().getId();

                String otp = otpService.generate();
                otpService.store(targetUserId, otp);

                EmailService.getInstance().sendOtpEmail(email, otp).thenAccept(success -> {
                    Platform.runLater(() -> {
                        hideLoading();
                        if (success) {
                            showSuccess(I18n.get("forgot.code_sent"));
                            // Brief delay before transitioning to let user see the success message
                            PauseTransition pause = new PauseTransition(Duration.millis(600));
                            pause.setOnFinished(e -> transitionToStep(Step.OTP));
                            pause.play();
                        } else {
                            showError(I18n.get("forgot.send_error"));
                            sendBtn.setDisable(false);
                        }
                    });
                });
            } catch (Exception e) {
                logger.error("Error during password reset", e);
                Platform.runLater(() -> {
                    hideLoading();
                    showError(I18n.get("forgot.unexpected_error"));
                    sendBtn.setDisable(false);
                });
            }
        });
    }

    // ==================== STEP 2: VERIFY OTP ====================

    private void handleVerifyOtp() {
        String inputOtp = otpInput != null ? otpInput.getText() : "";

        if (inputOtp.isEmpty() || inputOtp.length() < 6) {
            showError(I18n.get("forgot.code_required"));
            if (otpInput != null) shakeNode(otpInput);
            return;
        }

        showLoading(I18n.get("forgot.verify_code") + "...");
        sendBtn.setDisable(true);
        String trimmedOtp = inputOtp.trim();

        AppThreadPool.execute(() -> {
            try {
                boolean valid = otpService.verify(targetUserId, trimmedOtp);
                if (valid) {
                    otpService.markUsed(targetUserId, trimmedOtp);
                    Platform.runLater(() -> {
                        hideLoading();
                        showSuccess(I18n.get("forgot.code_valid"));
                        PauseTransition pause = new PauseTransition(Duration.millis(600));
                        pause.setOnFinished(e -> transitionToStep(Step.NEW_PASSWORD));
                        pause.play();
                    });
                } else {
                    Platform.runLater(() -> {
                        hideLoading();
                        showError(I18n.get("forgot.code_invalid"));
                        if (otpInput != null) {
                            shakeNode(otpInput);
                            otpInput.clear();
                            otpInput.focusFirst();
                        }
                        sendBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                logger.error("Error verifying OTP", e);
                Platform.runLater(() -> {
                    hideLoading();
                    showError(I18n.get("forgot.verify_error"));
                    sendBtn.setDisable(false);
                });
            }
        });
    }

    // ==================== STEP 3: NEW PASSWORD ====================

    private void handleNewPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword == null || newPassword.trim().isEmpty()) {
            showError(I18n.get("forgot.password_required"));
            shakeNode(newPasswordField);
            return;
        }

        String passwordError = Validators.validatePasswordStrength(newPassword);
        if (passwordError != null) {
            showError(passwordError);
            shakeNode(newPasswordField);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(I18n.get("forgot.password_mismatch"));
            shakeNode(confirmPasswordField);
            return;
        }

        showLoading(I18n.get("forgot.updating"));
        sendBtn.setDisable(true);

        AppThreadPool.execute(() -> {
            try {
                boolean updated = UserService.getInstance().updatePassword(targetUserId, newPassword);
                Platform.runLater(() -> {
                    hideLoading();
                    if (updated) {
                        showPasswordUpdatedSuccess();
                    } else {
                        showError(I18n.get("forgot.update_error"));
                        sendBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                logger.error("Error updating password", e);
                Platform.runLater(() -> {
                    hideLoading();
                    showError(I18n.get("forgot.unexpected_error"));
                    sendBtn.setDisable(false);
                });
            }
        });
    }

    private void showPasswordUpdatedSuccess() {
        // Mark all steps done
        setStepStyle(step3Circle, step3Label, step3Text, "\u2713", STEP_DONE_BG, STEP_DONE_TEXT, STEP_DONE_LABEL);

        // Hide form, show success state
        if (contentArea != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), contentArea);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                contentArea.setVisible(false);
                contentArea.setManaged(false);
            });
            fadeOut.play();
        }

        iconLabel.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 32, "-fx-green"));
        iconLabel.setText("");
        titleLabel.setText(I18n.get("forgot.password_updated"));
        subtitleLabel.setText(I18n.get("forgot.redirect_login"));

        sendBtn.setVisible(false);
        sendBtn.setManaged(false);
        if (resendSection != null) {
            resendSection.setVisible(false);
            resendSection.setManaged(false);
        }

        backBtn.setText(I18n.get("forgot.back_to_login"));

        // Auto-redirect after 3 seconds
        PauseTransition redirect = new PauseTransition(Duration.seconds(3));
        redirect.setOnFinished(e -> {
            if (onBack != null) onBack.run();
        });
        redirect.play();
    }

    // ==================== STEP TRANSITIONS ====================

    private void transitionToStep(Step newStep) {
        hideMessages();

        // Fade out content
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            // Configure new step content
            configureStepContent(newStep);
            updateStepIndicator(newStep);
            currentStep = newStep;

            // Fade in content
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), contentArea);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void configureStepContent(Step step) {
        switch (step) {
            case EMAIL -> {
                stopOtpCountdown();
                // Show email container, hide OTP and password containers
                emailContainer.setVisible(true);
                emailContainer.setManaged(true);
                emailField.setText(targetEmail != null ? targetEmail : "");
                emailField.setPromptText(I18n.get("forgot.ui.email_placeholder"));
                emailField.setDisable(false);
                fieldLabel.setText(I18n.get("forgot.ui.email_label"));
                fieldLabel.setStyle("");

                otpContainer.setVisible(false);
                otpContainer.setManaged(false);
                passwordContainer.setVisible(false);
                passwordContainer.setManaged(false);

                iconLabel.setGraphic(SvgIcons.icon(SvgIcons.LOCK, 24));
                iconLabel.setText("");
                titleLabel.setText(I18n.get("forgot.ui.title"));
                subtitleLabel.setText(I18n.get("forgot.ui.subtitle"));

                sendBtn.setText(I18n.get("forgot.send_code"));
                sendBtn.setDisable(false);
                sendBtn.setVisible(true);
                sendBtn.setManaged(true);
                backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
                backBtn.setText(I18n.get("forgot.back_to_login"));

                resendSection.setVisible(false);
                resendSection.setManaged(false);
            }
            case OTP -> {
                // Show OTP digit input, hide email and password containers
                emailContainer.setVisible(false);
                emailContainer.setManaged(false);

                otpContainer.setVisible(true);
                otpContainer.setManaged(true);
                otpInput.clear();
                otpInput.focusFirst();

                passwordContainer.setVisible(false);
                passwordContainer.setManaged(false);

                iconLabel.setGraphic(SvgIcons.icon(SvgIcons.MAIL, 24));
                iconLabel.setText("");
                titleLabel.setText(I18n.get("forgot.verify_code"));
                subtitleLabel.setText(I18n.get("forgot.otp_instruction"));

                // Timer label replaces the OTP field label
                if (otpFieldLabel != null) {
                    otpFieldLabel.setText(I18n.get("forgot.verify_code"));
                }

                sendBtn.setText(I18n.get("forgot.verify_code"));
                sendBtn.setDisable(false);
                sendBtn.setVisible(true);
                sendBtn.setManaged(true);
                backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
                backBtn.setText(I18n.get("forgot.step.email"));

                // Show resend section
                resendSection.setVisible(true);
                resendSection.setManaged(true);
                if (resendBtn != null) resendBtn.setDisable(true);

                // Start countdown
                startOtpCountdown();
            }
            case NEW_PASSWORD -> {
                stopOtpCountdown();
                // Hide email and OTP, show password fields
                emailContainer.setVisible(false);
                emailContainer.setManaged(false);
                otpContainer.setVisible(false);
                otpContainer.setManaged(false);

                passwordContainer.setVisible(true);
                passwordContainer.setManaged(true);
                newPasswordField.getControl().clear();
                confirmPasswordField.getControl().clear();

                iconLabel.setGraphic(SvgIcons.icon(SvgIcons.SHIELD, 24));
                iconLabel.setText("");
                titleLabel.setText(I18n.get("forgot.new_password_label"));
                subtitleLabel.setText(I18n.get("forgot.new_password_subtitle"));

                sendBtn.setText(I18n.get("forgot.change_password"));
                sendBtn.setDisable(false);
                sendBtn.setVisible(true);
                sendBtn.setManaged(true);
                backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
                backBtn.setText(I18n.get("forgot.step.verify"));

                resendSection.setVisible(false);
                resendSection.setManaged(false);
            }
        }
    }

    // ==================== OTP COUNTDOWN TIMER ====================

    private void startOtpCountdown() {
        stopOtpCountdown();
        remainingSeconds = OTP_EXPIRY_SECONDS;
        updateTimerLabel();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel();

            if (remainingSeconds <= 0) {
                stopOtpCountdown();
                Platform.runLater(() -> {
                    showError(I18n.get("forgot.code_expired"));
                    sendBtn.setDisable(true);
                    if (resendBtn != null) {
                        resendBtn.setDisable(false);
                        resendBtn.setText(I18n.get("forgot.resend"));
                    }
                });
            }
        }));
        countdownTimeline.setCycleCount(OTP_EXPIRY_SECONDS);
        countdownTimeline.play();

        // Start resend cooldown simultaneously
        startResendCooldown();
    }

    private void stopOtpCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void updateTimerLabel() {
        int min = remainingSeconds / 60;
        int sec = remainingSeconds % 60;
        String timeStr = String.format("%d:%02d", min, sec);
        Platform.runLater(() -> {
            if (otpFieldLabel != null && currentStep == Step.OTP) {
                String timerColor;
                if (remainingSeconds > 60) timerColor = "-fx-green";
                else if (remainingSeconds > 30) timerColor = "-fx-amber";
                else timerColor = "-fx-red";
                otpFieldLabel.setText(I18n.get("forgot.code_timer").replace("{0}", timeStr));
                otpFieldLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + timerColor + "; -fx-alignment: center;");
            }
        });
    }

    // ==================== RESEND ====================

    @FXML
    private void handleResend() {
        if (targetEmail == null) return;

        if (resendBtn != null) {
            resendBtn.setText(I18n.get("forgot.resending"));
            resendBtn.setDisable(true);
        }
        hideMessages();
        showLoading(I18n.get("forgot.resending"));

        String otp = otpService.generate();
        otpService.store(targetUserId, otp);

        EmailService.getInstance().sendOtpEmail(targetEmail, otp).thenAccept(success -> {
            Platform.runLater(() -> {
                hideLoading();
                if (success) {
                    showSuccess(I18n.get("forgot.code_resent"));
                    sendBtn.setDisable(false);
                    if (otpInput != null) {
                        otpInput.clear();
                        otpInput.focusFirst();
                    }
                    startOtpCountdown();
                } else {
                    showError(I18n.get("forgot.resend_error"));
                    if (resendBtn != null) {
                        resendBtn.setText(I18n.get("forgot.resend"));
                        resendBtn.setDisable(false);
                    }
                }
            });
        });
    }

    private void startResendCooldown() {
        if (resendBtn == null) return;
        resendBtn.setDisable(true);
        final int cooldown = 30;
        final int[] remaining = {cooldown};

        Timeline cooldownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            resendBtn.setText(I18n.get("forgot.resend") + " (" + remaining[0] + "s)");
            if (remaining[0] <= 0) {
                resendBtn.setText(I18n.get("forgot.resend"));
                resendBtn.setDisable(false);
            }
        }));
        cooldownTimeline.setCycleCount(cooldown);
        cooldownTimeline.play();
    }

    // ==================== BACK NAVIGATION ====================

    @FXML
    private void handleBack() {
        hideMessages();
        switch (currentStep) {
            case NEW_PASSWORD -> transitionToStep(Step.OTP);
            case OTP -> {
                stopOtpCountdown();
                transitionToStep(Step.EMAIL);
            }
            case EMAIL -> {
                if (onBack != null) onBack.run();
            }
        }
    }

    // ==================== UI Helpers ====================

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);

        // Quick fade-in for error
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        FadeTransition ft = new FadeTransition(Duration.millis(200), successLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showLoading(String text) {
        if (loadingLabel != null) loadingLabel.setText(text);
        if (loadingBox != null) {
            loadingBox.setVisible(true);
            loadingBox.setManaged(true);
        }
        sendBtn.setDisable(true);
    }

    private void hideLoading() {
        if (loadingBox != null) {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
        }
    }

    /** Shake animation for invalid input feedback */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }
}
