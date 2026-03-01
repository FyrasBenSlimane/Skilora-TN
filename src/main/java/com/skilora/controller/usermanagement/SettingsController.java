package com.skilora.controller.usermanagement;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.usermanagement.PreferencesService;
import com.skilora.utils.I18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private final PreferencesService prefService = PreferencesService.getInstance();

    @FXML
    private CheckBox darkModeCheckbox;
    @FXML
    private CheckBox animationsCheckbox;
    @FXML
    private CheckBox notificationsCheckbox;
    @FXML
    private CheckBox soundNotificationsCheckbox;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;

    // i18n labels
    @FXML private Label settingsTitleLabel;
    @FXML private Label settingsSubtitleLabel;
    @FXML private Label appearanceLabel;
    @FXML private Label darkModeLabel;
    @FXML private Label animationsLabel;
    @FXML private Label notificationsLabel;
    @FXML private Label enableNotificationsLabel;
    @FXML private Label soundNotificationsLabel;
    @FXML private Label languageSectionLabel;
    @FXML private Label languageLabel;

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
        if (languageSectionLabel != null) languageSectionLabel.setText(I18n.get("settings.language"));
        if (languageLabel != null) languageLabel.setText(I18n.get("settings.language_label"));
        if (saveBtn != null) saveBtn.setText(I18n.get("settings.save"));
        if (cancelBtn != null) cancelBtn.setText(I18n.get("settings.cancel"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserPreferences();
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
            prefs.put(PreferencesService.KEY_LANGUAGE, selectedLang);
            prefService.saveAll(currentUser.getId(), prefs);
        }

        logger.info("Settings saved: darkMode={}, animations={}, notifications={}, sound={}, language={}",
            darkModeCheckbox.isSelected(), animationsCheckbox.isSelected(),
            notificationsCheckbox.isSelected(), soundNotificationsCheckbox.isSelected(),
            selectedLang);
        
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
}
