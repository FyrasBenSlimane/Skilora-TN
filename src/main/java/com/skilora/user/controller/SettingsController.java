package com.skilora.user.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.skilora.framework.components.TLSwitch;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLTextarea;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLButton;
import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.user.service.PreferencesService;
import com.skilora.user.service.RoleUpgradeService;
import com.skilora.user.service.TwoFactorAuthService;
import com.skilora.user.service.UserService;
import com.skilora.finance.service.UserCurrencyService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private final PreferencesService prefService = PreferencesService.getInstance();
    private final TwoFactorAuthService twoFactorAuthService;

    public SettingsController() {
        this.twoFactorAuthService = TwoFactorAuthService.getInstance();
    }

    @FXML
    private TLSwitch darkModeCheckbox;
    @FXML
    private TLSwitch animationsCheckbox;
    @FXML
    private TLSwitch notificationsCheckbox;
    @FXML
    private TLSwitch soundNotificationsCheckbox;
    @FXML
    private TLSwitch emailNotificationsCheckbox;
    @FXML
    private TLSelect<String> languageComboBox;
    @FXML
    private TLButton saveBtn;
    @FXML
    private TLButton cancelBtn;

    // i18n labels
    @FXML private Label settingsTitleLabel;
    @FXML private Label settingsSubtitleLabel;
    @FXML private Label appearanceLabel;
    @FXML private Label darkModeLabel;
    @FXML private Label animationsLabel;
    @FXML private Label notificationsLabel;
    @FXML private Label enableNotificationsLabel;
    @FXML private Label soundNotificationsLabel;
    @FXML private Label emailNotificationsLabel;
    @FXML private Label languageSectionLabel;
    @FXML private Label languageLabel;
    @FXML private Label securitySectionLabel;
    @FXML private Label changePasswordLabel;
    @FXML private TLButton changePasswordBtn;
    @FXML private Label advancedPrefsLabel;
    @FXML private Label currencyPrefLabel;
    @FXML private TLButton currencyBtn;
    @FXML private Label trainerUpgradeLabel;
    @FXML private TLButton trainerUpgradeBtn;

    @FXML private Label dangerZoneLabel;
    @FXML private Label deleteAccountLabel;
    @FXML private TLButton deleteAccountBtn;

    // 2FA fields
    @FXML private Label twoFactorAuthLabel;
    @FXML private Label twoFactorAuthStatusLabel;
    @FXML private TLSwitch twoFactorAuthSwitch;
    @FXML private Label twoFactorAuthDescription;

    private Runnable onSave;
    private Runnable onCancel;
    private Runnable onLanguageChanged;
    private User currentUser;

    @FXML
    public void initialize() {
        languageComboBox.getItems().setAll(I18n.getSupportedLanguages());
        languageComboBox.setValue(I18n.getDisplayName());
        
        saveBtn.setOnAction(e -> saveSettings());
        cancelBtn.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });

        applyI18n();
    }

    private void applyI18n() {
        if (settingsTitleLabel != null) settingsTitleLabel.setText(I18n.get("settings.title"));
        if (settingsSubtitleLabel != null) settingsSubtitleLabel.setText(I18n.get("settings.subtitle"));
        if (appearanceLabel != null) appearanceLabel.setText(I18n.get("settings.appearance"));
        if (darkModeLabel != null) darkModeLabel.setText(I18n.get("settings.dark_mode"));
        if (animationsLabel != null) animationsLabel.setText(I18n.get("settings.animations"));
        if (notificationsLabel != null) notificationsLabel.setText(I18n.get("settings.notifications"));
        if (enableNotificationsLabel != null) enableNotificationsLabel.setText(I18n.get("settings.enable_notifications"));
        if (soundNotificationsLabel != null) soundNotificationsLabel.setText(I18n.get("settings.sound_notifications"));
        if (emailNotificationsLabel != null) emailNotificationsLabel.setText(I18n.get("settings.email_notifications"));
        if (languageSectionLabel != null) languageSectionLabel.setText(I18n.get("settings.language"));
        if (languageLabel != null) languageLabel.setText(I18n.get("settings.language_label"));
        if (securitySectionLabel != null) securitySectionLabel.setText(I18n.get("settings.security"));
        if (changePasswordLabel != null) changePasswordLabel.setText(I18n.get("settings.change_password_label"));
        if (changePasswordBtn != null) changePasswordBtn.setText(I18n.get("settings.change_password"));
        if (saveBtn != null) saveBtn.setText(I18n.get("settings.save"));
        if (cancelBtn != null) cancelBtn.setText(I18n.get("settings.cancel"));
        if (advancedPrefsLabel != null) advancedPrefsLabel.setText(I18n.get("settings.advanced_prefs"));
        if (currencyPrefLabel != null) currencyPrefLabel.setText(I18n.get("settings.currency_pref"));
        if (currencyBtn != null) currencyBtn.setText(I18n.get("settings.change"));
        if (trainerUpgradeLabel != null) trainerUpgradeLabel.setText(I18n.get("settings.trainer_upgrade"));
        if (trainerUpgradeBtn != null) trainerUpgradeBtn.setText(I18n.get("settings.trainer_request"));
        if (dangerZoneLabel != null) dangerZoneLabel.setText(I18n.get("settings.danger_zone"));
        if (deleteAccountLabel != null) deleteAccountLabel.setText(I18n.get("settings.delete_account_label"));
        if (deleteAccountBtn != null) deleteAccountBtn.setText(I18n.get("settings.delete_account"));
    }

    @FXML
    private void handleDeleteAccount() {
        if (currentUser == null || deleteAccountBtn == null || deleteAccountBtn.getScene() == null) return;

        // Admin accounts cannot be self-deleted
        if (currentUser.getRole() == Role.ADMIN) {
            TLToast.error(deleteAccountBtn.getScene(), I18n.get("common.error"), I18n.get("admin.self_action.delete"));
            return;
        }

        boolean ok = DialogUtils.showConfirmation(
                I18n.get("settings.delete_account_confirm_title"),
                I18n.get("settings.delete_account_confirm_msg"))
                .orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (!ok) return;

        deleteAccountBtn.setDisable(true);
        AppThreadPool.execute(() -> {
            try {
                UserService.getInstance().deleteUser(currentUser.getId());
                Platform.runLater(() -> {
                    TLToast.success(deleteAccountBtn.getScene(), I18n.get("settings.delete_account_done"), "");
                    Platform.exit();
                });
            } catch (Exception e) {
                logger.error("Failed to delete account", e);
                Platform.runLater(() -> {
                    deleteAccountBtn.setDisable(false);
                    TLToast.error(deleteAccountBtn.getScene(), I18n.get("common.error"), I18n.get("settings.delete_account_failed"));
                });
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserPreferences();

        // Only freelancers (Role.USER) may request a trainer upgrade
        boolean showTrainerUpgrade = (user != null && user.getRole() == Role.USER);
        if (trainerUpgradeBtn != null) {
            trainerUpgradeBtn.setVisible(showTrainerUpgrade);
            trainerUpgradeBtn.setManaged(showTrainerUpgrade);
        }
        if (trainerUpgradeLabel != null) {
            trainerUpgradeLabel.setVisible(showTrainerUpgrade);
            trainerUpgradeLabel.setManaged(showTrainerUpgrade);
        }

        // If user already has a pending request, disable button and show pending text
        if (showTrainerUpgrade) {
            updateTrainerButtonState();
        }

        // Hide delete account UI for Admin
        boolean canDelete = (user != null && user.getRole() != Role.ADMIN);
        if (deleteAccountBtn != null) {
            deleteAccountBtn.setVisible(canDelete);
            deleteAccountBtn.setManaged(canDelete);
        }
        if (deleteAccountLabel != null) {
            deleteAccountLabel.setVisible(canDelete);
            deleteAccountLabel.setManaged(canDelete);
        }
    }

    public void setCallbacks(Runnable onSave, Runnable onCancel) {
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    public void setOnLanguageChanged(Runnable callback) {
        this.onLanguageChanged = callback;
    }

    public void setDarkMode(boolean darkMode) {
        darkModeCheckbox.setSelected(darkMode);
    }

    public void setAnimations(boolean enabled) {
        animationsCheckbox.setSelected(enabled);
    }

    public void setNotifications(boolean enabled) {
        notificationsCheckbox.setSelected(enabled);
    }

    public void setSoundNotifications(boolean enabled) {
        soundNotificationsCheckbox.setSelected(enabled);
    }

    public void setLanguage(String language) {
        languageComboBox.setValue(language);
    }

    /**
     * Load saved preferences from DB and populate the UI.
     */
    private void loadUserPreferences() {
        if (currentUser == null) return;

        try {
            Map<String, String> prefs = prefService.getAll(currentUser.getId());

            if (prefs.containsKey(PreferencesService.KEY_DARK_MODE)) {
                darkModeCheckbox.setSelected(Boolean.parseBoolean(prefs.get(PreferencesService.KEY_DARK_MODE)));
            }
            if (prefs.containsKey(PreferencesService.KEY_ANIMATIONS)) {
                animationsCheckbox.setSelected(Boolean.parseBoolean(prefs.get(PreferencesService.KEY_ANIMATIONS)));
            }
            if (prefs.containsKey(PreferencesService.KEY_NOTIFICATIONS)) {
                notificationsCheckbox.setSelected(Boolean.parseBoolean(prefs.get(PreferencesService.KEY_NOTIFICATIONS)));
            }
            if (prefs.containsKey(PreferencesService.KEY_SOUND_NOTIFICATIONS)) {
                soundNotificationsCheckbox.setSelected(Boolean.parseBoolean(prefs.get(PreferencesService.KEY_SOUND_NOTIFICATIONS)));
            }
            if (prefs.containsKey(PreferencesService.KEY_EMAIL_NOTIFICATIONS)) {
                emailNotificationsCheckbox.setSelected(Boolean.parseBoolean(prefs.get(PreferencesService.KEY_EMAIL_NOTIFICATIONS)));
            }
            if (prefs.containsKey(PreferencesService.KEY_LANGUAGE)) {
                languageComboBox.setValue(prefs.get(PreferencesService.KEY_LANGUAGE));
            }
        } catch (Exception e) {
            logger.warn("Could not load user preferences: {}", e.getMessage());
        }
    }

    private void saveSettings() {
        // Apply language change
        String selectedLang = languageComboBox.getValue();
        I18n.setLocaleFromDisplayName(selectedLang);

        // Persist to DB if we have a user
        if (currentUser != null) {
            Map<String, String> prefs = new LinkedHashMap<>();
            prefs.put(PreferencesService.KEY_DARK_MODE, String.valueOf(darkModeCheckbox.isSelected()));
            prefs.put(PreferencesService.KEY_ANIMATIONS, String.valueOf(animationsCheckbox.isSelected()));
            prefs.put(PreferencesService.KEY_NOTIFICATIONS, String.valueOf(notificationsCheckbox.isSelected()));
            prefs.put(PreferencesService.KEY_SOUND_NOTIFICATIONS, String.valueOf(soundNotificationsCheckbox.isSelected()));
            prefs.put(PreferencesService.KEY_EMAIL_NOTIFICATIONS, String.valueOf(emailNotificationsCheckbox.isSelected()));
            prefs.put(PreferencesService.KEY_LANGUAGE, selectedLang);
            prefService.saveAll(currentUser.getId(), prefs);
        }

        logger.info("Settings saved: darkMode={}, animations={}, notifications={}, sound={}, email={}, language={}",
            darkModeCheckbox.isSelected(), animationsCheckbox.isSelected(),
            notificationsCheckbox.isSelected(), soundNotificationsCheckbox.isSelected(),
            emailNotificationsCheckbox.isSelected(), selectedLang);
        
        if (onLanguageChanged != null) onLanguageChanged.run();
        if (onSave != null) onSave.run();
    }

    public boolean isDarkModeEnabled() {
        return darkModeCheckbox.isSelected();
    }

    public boolean isAnimationsEnabled() {
        return animationsCheckbox.isSelected();
    }

    public boolean isNotificationsEnabled() {
        return notificationsCheckbox.isSelected();
    }

    public boolean isSoundNotificationsEnabled() {
        return soundNotificationsCheckbox.isSelected();
    }

    public String getSelectedLanguage() {
        return languageComboBox.getValue();
    }

    @FXML
    private void handleChangePassword() {
        if (currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("settings.change_password"));
        dialog.setDescription(I18n.get("settings.change_password_label"));

        ButtonType confirmType = new ButtonType(I18n.get("settings.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.addButton(confirmType);
        dialog.addButton(ButtonType.CANCEL);

        VBox form = new VBox(14);

        TLPasswordField currentPwd = new TLPasswordField();
        currentPwd.setPromptText(I18n.get("settings.current_password"));
        TLPasswordField newPwd = new TLPasswordField();
        newPwd.setPromptText(I18n.get("settings.new_password"));
        TLPasswordField confirmPwd = new TLPasswordField();
        confirmPwd.setPromptText(I18n.get("settings.confirm_password"));

        form.getChildren().addAll(
            createFormField(I18n.get("settings.current_password"), currentPwd),
            createFormField(I18n.get("settings.new_password"), newPwd),
            createFormField(I18n.get("settings.confirm_password"), confirmPwd)
        );

        dialog.setDialogContent(form);

        dialog.getDialogPane().lookupButton(confirmType).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String current = currentPwd.getText();
                String newPass = newPwd.getText();
                String confirm = confirmPwd.getText();

                if (current == null || current.isEmpty()) {
                    currentPwd.setError(I18n.get("error.validation.required", I18n.get("settings.current_password")));
                    valid = false;
                } else if (!UserService.verifyPassword(current, currentUser.getPassword())) {
                    currentPwd.setError(I18n.get("settings.password.wrong_current"));
                    valid = false;
                } else { currentPwd.clearValidation(); }

                if (newPass == null || newPass.length() < 6) {
                    newPwd.setError(I18n.get("settings.password.too_short"));
                    valid = false;
                } else { newPwd.clearValidation(); }

                if (confirm == null || !confirm.equals(newPass)) {
                    confirmPwd.setError(I18n.get("settings.password.mismatch"));
                    valid = false;
                } else { confirmPwd.clearValidation(); }

                if (!valid) event.consume();
            }
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == confirmType) {
            String newPass = newPwd.getText();
            Scene scene = changePasswordBtn.getScene();
            changePasswordBtn.setDisable(true);
            changePasswordBtn.setText(I18n.get("common.saving"));

            AppThreadPool.execute(() -> {
                boolean updated = UserService.getInstance().updatePassword(currentUser.getId(), newPass);
                Platform.runLater(() -> {
                    changePasswordBtn.setDisable(false);
                    changePasswordBtn.setText(I18n.get("settings.change_password"));
                    if (updated) {
                        currentUser.setPassword(UserService.hashPassword(newPass));
                        TLToast.success(scene, I18n.get("settings.password.success"), I18n.get("settings.password.success_desc"));
                        logger.info("Password changed for user {}", currentUser.getUsername());
                    } else {
                        TLToast.error(scene, I18n.get("settings.password.error"), I18n.get("settings.password.failed"));
                    }
                });
            });
        }
    }

    /**
     * Check if user has a pending trainer request and update the button accordingly.
     */
    private void updateTrainerButtonState() {
        if (currentUser == null || trainerUpgradeBtn == null) return;

        AppThreadPool.execute(() -> {
            boolean pending = RoleUpgradeService.getInstance().hasPendingRequest(currentUser.getId());
            Platform.runLater(() -> {
                if (pending) {
                    trainerUpgradeBtn.setText(I18n.get("settings.trainer_request_pending"));
                    trainerUpgradeBtn.setDisable(true);
                    trainerUpgradeBtn.setOpacity(0.7);
                } else {
                    trainerUpgradeBtn.setText(I18n.get("settings.trainer_request"));
                    trainerUpgradeBtn.setDisable(false);
                    trainerUpgradeBtn.setOpacity(1.0);
                }
            });
        });
    }

    @FXML
    private void handleRequestTrainerUpgrade() {
        if (currentUser == null || currentUser.getRole() != Role.USER) return;

        RoleUpgradeService upgradeService = RoleUpgradeService.getInstance();
        if (upgradeService.hasPendingRequest(currentUser.getId())) {
            updateTrainerButtonState();
            return;
        }

        TLDialog<String> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("role.upgrade.request"));
        dialog.setDescription(I18n.get("role.upgrade.justification"));

        VBox form = new VBox(12);
        TLTextarea justification = new TLTextarea();
        justification.setPromptText(I18n.get("role.upgrade.justification"));
        justification.getControl().setPrefRowCount(4);
        form.getChildren().add(justification);

        dialog.setDialogContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String text = justification.getText() != null ? justification.getText().trim() : "";
                if (text.isEmpty()) {
                    justification.setError(I18n.get("error.validation.required", I18n.get("role.upgrade.justification")));
                    event.consume();
                } else {
                    justification.clearValidation();
                }
            }
        );

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? justification.getText() : null);

        dialog.showAndWait().ifPresent(text -> {
            if (text != null && !text.isBlank()) {
                trainerUpgradeBtn.setDisable(true);
                trainerUpgradeBtn.setText(I18n.get("common.submitting"));
                AppThreadPool.execute(() -> {
                    int id = upgradeService.requestUpgrade(currentUser.getId(), Role.TRAINER.name(), text);
                    Platform.runLater(() -> {
                        if (id > 0) {
                            TLToast.success(saveBtn.getScene(), I18n.get("role.upgrade.success"), "");
                            updateTrainerButtonState();
                        } else {
                            trainerUpgradeBtn.setDisable(false);
                            trainerUpgradeBtn.setText(I18n.get("settings.trainer_request"));
                        }
                    });
                });
            }
        });
    }

    @FXML
    private void handleCurrencyPreference() {
        if (currentUser == null) return;

        TLDialog<String> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("currency.preferred"));

        TLSelect<String> currencySelect = new TLSelect<>();
        currencySelect.getItems().addAll("TND", "EUR", "USD", "GBP");

        String current = UserCurrencyService.getInstance().getPreferredCurrency(currentUser.getId());
        currencySelect.setValue(current);

        VBox form = new VBox(12);
        form.getChildren().add(currencySelect);

        dialog.setDialogContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? currencySelect.getValue() : null);

        dialog.showAndWait().ifPresent(currency -> {
            if (currency != null) {
                UserCurrencyService.getInstance().setPreferredCurrency(currentUser.getId(), currency);
                TLToast.success(saveBtn.getScene(), I18n.get("currency.updated"), currency);
            }
        });
    }

    private VBox createFormField(String labelText, Node field) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("text-sm");
        box.getChildren().addAll(label, field);
        return box;
    }

    /**
     * Initialize 2FA settings UI.
     */
    private void initialize2FASettings() {
        if (twoFactorAuthSwitch != null) {
            twoFactorAuthSwitch.selectedProperty().addListener((obs, old, newVal) -> {
                if (newVal) {
                    handleEnable2FA();
                } else {
                    handleDisable2FA();
                }
            });
        }
        update2FAStatus();
    }

    /**
     * Update 2FA status label.
     */
    private void update2FAStatus() {
        if (currentUser == null || twoFactorAuthStatusLabel == null) return;

        AppThreadPool.execute(() -> {
            boolean enabled = twoFactorAuthService.is2FAEnabled(currentUser.getId());
            Platform.runLater(() -> {
                twoFactorAuthStatusLabel.setText(enabled ?
                    I18n.get("settings.2fa.enabled") : I18n.get("settings.2fa.disabled"));
                twoFactorAuthStatusLabel.getStyleClass().removeAll("text-success", "text-muted");
                twoFactorAuthStatusLabel.getStyleClass().add(enabled ? "text-success" : "text-muted");
                if (twoFactorAuthSwitch != null) {
                    twoFactorAuthSwitch.setSelected(enabled);
                }
            });
        });
    }

    /**
     * Handle enabling 2FA - show confirmation dialog.
     */
    @FXML
    private void handleEnable2FA() {
        if (currentUser == null) return;

        boolean confirmed = DialogUtils.showConfirmation(
            I18n.get("settings.2fa.enable_title"),
            I18n.get("settings.2fa.enable_message"))
            .orElse(ButtonType.CANCEL) == ButtonType.OK;

        if (confirmed) {
            AppThreadPool.execute(() -> {
                twoFactorAuthService.enable2FA(currentUser.getId());
                Platform.runLater(() -> {
                    update2FAStatus();
                    TLToast.success(saveBtn.getScene(),
                        I18n.get("settings.2fa.enabled_title"),
                        I18n.get("settings.2fa.enabled_message"));
                });
            });
        } else {
            // Revert switch if cancelled
            twoFactorAuthSwitch.setSelected(false);
        }
    }

    /**
     * Handle disabling 2FA - show confirmation dialog.
     */
    @FXML
    private void handleDisable2FA() {
        if (currentUser == null) return;

        boolean confirmed = DialogUtils.showConfirmation(
            I18n.get("settings.2fa.disable_title"),
            I18n.get("settings.2fa.disable_message"))
            .orElse(ButtonType.CANCEL) == ButtonType.OK;

        if (confirmed) {
            AppThreadPool.execute(() -> {
                twoFactorAuthService.disable2FA(currentUser.getId());
                Platform.runLater(() -> {
                    update2FAStatus();
                    TLToast.success(saveBtn.getScene(),
                        I18n.get("settings.2fa.disabled_title"),
                        I18n.get("settings.2fa.disabled_message"));
                });
            });
        } else {
            // Revert switch if cancelled
            twoFactorAuthSwitch.setSelected(true);
        }
    }
}