package com.skilora.controller;

import com.skilora.config.DatabaseConfig;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.model.entity.User;
import com.skilora.model.service.UserService;
import com.skilora.model.service.EmailService;
import com.skilora.utils.Validators;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.utils.I18n;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ForgotPasswordController - Handle password reset flow with DB-backed OTP.
 */
public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML
    private TLTextField emailField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private TLButton sendBtn;
    @FXML
    private TLButton backBtn;
    @FXML
    private TLButton resendBtn;

    private Runnable onBack;
    private String targetEmail;
    private int targetUserId;
    private boolean isOtpMode = false;
    private boolean isNewPasswordMode = false;

    private final SecureRandom secureRandom = new SecureRandom();

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void handleSendReset() {
        hideMessages();

        if (isNewPasswordMode) {
            handleNewPassword();
            return;
        }

        if (isOtpMode) {
            handleVerifyOtp();
            return;
        }

        String email = emailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showError(I18n.get("forgot.email_required"));
            return;
        }

        if (!Validators.email(email)) {
            showError(I18n.get("forgot.email_invalid"));
            return;
        }

        sendBtn.setText(I18n.get("forgot.sending"));
        sendBtn.setDisable(true);

        Thread sendThread = new Thread(() -> {
            try {
                // Check if user with this email exists (DB call - off FX thread)
                Optional<User> userOpt = UserService.getInstance().findByEmail(email);
                if (userOpt.isEmpty()) {
                    Platform.runLater(() -> {
                        showSuccess(I18n.get("forgot.sent_if_exists"));
                        sendBtn.setText(I18n.get("forgot.send_code"));
                        sendBtn.setDisable(false);
                    });
                    return;
                }

                targetEmail = email;
                targetUserId = userOpt.get().getId();

                // Generate and store OTP in database
                String otp = generateOtp();
                storeOtpInDatabase(targetUserId, otp);

                EmailService.getInstance().sendOtpEmail(email, otp).thenAccept(success -> {
                    Platform.runLater(() -> {
                        if (success) {
                            showSuccess(I18n.get("forgot.code_sent"));
                            switchToOtpMode();
                        } else {
                            showError(I18n.get("forgot.send_error"));
                            sendBtn.setText(I18n.get("forgot.send_code"));
                            sendBtn.setDisable(false);
                        }
                    });
                });
            } catch (Exception e) {
                logger.error("Error during password reset", e);
                Platform.runLater(() -> {
                    showError(I18n.get("forgot.unexpected_error"));
                    sendBtn.setText(I18n.get("forgot.send_code"));
                    sendBtn.setDisable(false);
                });
            }
        }, "PasswordResetThread");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void switchToOtpMode() {
        isOtpMode = true;
        emailField.setText("");
        emailField.setPromptText(I18n.get("forgot.enter_code"));
        emailField.setDisable(false);

        sendBtn.setText(I18n.get("forgot.verify_code"));
        sendBtn.setDisable(false);

        if (resendBtn != null) {
            resendBtn.setDisable(false);
        }
    }

    private void handleVerifyOtp() {
        String inputOtp = emailField.getText();

        if (inputOtp == null || inputOtp.trim().isEmpty()) {
            showError(I18n.get("forgot.code_required"));
            return;
        }

        sendBtn.setDisable(true);
        String trimmedOtp = inputOtp.trim();

        Thread verifyThread = new Thread(() -> {
            try {
                boolean valid = verifyOtpFromDatabase(targetUserId, trimmedOtp);
                if (valid) {
                    markOtpUsed(targetUserId, trimmedOtp);
                    Platform.runLater(() -> {
                        showSuccess(I18n.get("forgot.code_valid"));
                        switchToNewPasswordMode();
                    });
                } else {
                    Platform.runLater(() -> {
                        showError(I18n.get("forgot.code_invalid"));
                        sendBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                logger.error("Error verifying OTP", e);
                Platform.runLater(() -> {
                    showError(I18n.get("forgot.verify_error"));
                    sendBtn.setDisable(false);
                });
            }
        }, "OtpVerifyThread");
        verifyThread.setDaemon(true);
        verifyThread.start();
    }

    private void switchToNewPasswordMode() {
        isOtpMode = false;
        isNewPasswordMode = true;
        emailField.setText("");
        emailField.setPromptText(I18n.get("forgot.new_password"));
        emailField.setDisable(false);

        sendBtn.setText(I18n.get("forgot.change_password"));
        sendBtn.setDisable(false);

        if (resendBtn != null) {
            resendBtn.setDisable(true);
        }
    }

    private void handleNewPassword() {
        String newPassword = emailField.getText();

        if (newPassword == null || newPassword.trim().isEmpty()) {
            showError(I18n.get("forgot.password_required"));
            return;
        }

        String passwordError = Validators.validatePasswordStrength(newPassword);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }

        sendBtn.setDisable(true);
        sendBtn.setText(I18n.get("forgot.updating"));

        Thread updateThread = new Thread(() -> {
            try {
                boolean updated = UserService.getInstance().updatePassword(targetUserId, newPassword);
                Platform.runLater(() -> {
                    if (updated) {
                        showSuccess(I18n.get("forgot.password_updated"));
                        emailField.setDisable(true);

                        // Go back to login after a delay
                        Thread delayThread = new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    if (onBack != null) {
                                        onBack.run();
                                    }
                                });
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }, "PasswordResetDelayThread");
                        delayThread.setDaemon(true);
                        delayThread.start();
                    } else {
                        showError(I18n.get("forgot.update_error"));
                        sendBtn.setDisable(false);
                        sendBtn.setText(I18n.get("forgot.change_password"));
                    }
                });
            } catch (Exception e) {
                logger.error("Error updating password", e);
                Platform.runLater(() -> {
                    showError(I18n.get("forgot.unexpected_error"));
                    sendBtn.setDisable(false);
                    sendBtn.setText(I18n.get("forgot.change_password"));
                });
            }
        }, "PasswordUpdateThread");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @FXML
    private void handleResend() {
        if (targetEmail == null)
            return;

        if (resendBtn != null) {
            resendBtn.setText(I18n.get("forgot.resending"));
            resendBtn.setDisable(true);
        }

        String otp = generateOtp();
        storeOtpInDatabase(targetUserId, otp);

        EmailService.getInstance().sendOtpEmail(targetEmail, otp).thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    showSuccess(I18n.get("forgot.code_resent"));
                    startResendTimer();
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

    private void startResendTimer() {
        if (resendBtn != null) {
            resendBtn.setText(I18n.get("forgot.resent"));
        }
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                Platform.runLater(() -> {
                    if (resendBtn != null) {
                        resendBtn.setText(I18n.get("forgot.resend"));
                        resendBtn.setDisable(false);
                    }
                });
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        if (isNewPasswordMode) {
            isNewPasswordMode = false;
            switchToOtpMode();
            hideMessages();
            return;
        }

        if (isOtpMode) {
            isOtpMode = false;
            emailField.setText(targetEmail != null ? targetEmail : "");
            emailField.setPromptText("email@example.com");
            sendBtn.setText(I18n.get("forgot.send_code"));
            hideMessages();
            return;
        }

        if (onBack != null) {
            onBack.run();
        }
    }

    // ==================== OTP Database Operations ====================

    private String generateOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private void storeOtpInDatabase(int userId, String otp) {
        // Invalidate any existing unused OTPs for this user
        String invalidateSql = "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND used = FALSE";
        String insertSql = "INSERT INTO password_reset_tokens (user_id, otp_code, expires_at) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Invalidate old tokens
            try (PreparedStatement stmt = conn.prepareStatement(invalidateSql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            // Insert new token (15 minute expiry)
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, otp);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to store OTP for user id: {}", userId, e);
        }
    }

    private boolean verifyOtpFromDatabase(int userId, String otp) {
        String sql = "SELECT 1 FROM password_reset_tokens " +
                "WHERE user_id = ? AND otp_code = ? AND used = FALSE AND expires_at > NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, otp);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to verify OTP for user id: {}", userId, e);
        }
        return false;
    }

    private void markOtpUsed(int userId, String otp) {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND otp_code = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, otp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark OTP as used for user id: {}", userId, e);
        }
    }

    // ==================== UI Helpers ====================

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
