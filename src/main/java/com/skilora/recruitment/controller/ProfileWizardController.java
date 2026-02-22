package com.skilora.recruitment.controller;

import com.skilora.controller.BiometricAuthController;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.model.entity.Profile;
import com.skilora.model.entity.User;
import com.skilora.model.service.BiometricService;
import com.skilora.model.service.ProfileService;
import com.skilora.model.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class ProfileWizardController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileWizardController.class);

    @FXML
    private TLTextField firstNameField;
    @FXML
    private TLTextField lastNameField;
    @FXML
    private TLTextField headlineField;
    @FXML
    private TextArea bioArea;

    @FXML
    private TLTextField emailField;
    @FXML
    private TLTextField phoneField;
    @FXML
    private TLTextField locationField;
    @FXML
    private TLTextField websiteField;

    @FXML
    private TLButton saveBtn;
    @FXML
    private Label statusLabel;
    @FXML
    private Label biometricStatusLabel;
    @FXML
    private TLButton biometricBtn;

    private User currentUser;
    private Profile userProfile;
    private UserService userService;
    private ProfileService profileService;
    private Runnable onProfileUpdated;

    public void initializeContext(User user, Runnable onProfileUpdated) {
        this.currentUser = user;
        this.onProfileUpdated = onProfileUpdated;
        this.userService = UserService.getInstance();
        this.profileService = ProfileService.getInstance();

        loadProfileAndPopulate();
    }

    private void loadProfileAndPopulate() {
        if (currentUser == null)
            return;

        Task<Profile> loadTask = new Task<>() {
            @Override
            protected Profile call() throws Exception {
                return profileService.findProfileByUserId(currentUser.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            this.userProfile = loadTask.getValue();
            if (this.userProfile == null) {
                this.userProfile = new Profile();
                this.userProfile.setUserId(currentUser.getId());
                // Fallback from user full name if exists
                if (currentUser.getFullName() != null) {
                    String[] parts = currentUser.getFullName().split(" ", 2);
                    this.userProfile.setFirstName(parts[0]);
                    if (parts.length > 1)
                        this.userProfile.setLastName(parts[1]);
                }
            }
            populateFields();
        });

        new Thread(loadTask).start();
    }

    private void populateFields() {
        if (currentUser == null || userProfile == null)
            return;

        firstNameField.setText(userProfile.getFirstName() != null ? userProfile.getFirstName() : "");
        lastNameField.setText(userProfile.getLastName() != null ? userProfile.getLastName() : "");
        phoneField.setText(userProfile.getPhone() != null ? userProfile.getPhone() : "");
        locationField.setText(userProfile.getLocation() != null ? userProfile.getLocation() : "");

        if (emailField != null && currentUser.getEmail() != null) {
            emailField.setText(currentUser.getEmail());
        }

        // Check biometric status
        updateBiometricStatus();
    }

    private void updateBiometricStatus() {
        Thread t = new Thread(() -> {
            boolean hasBiometric = BiometricService.getInstance().hasBiometricData(currentUser.getUsername());
            javafx.application.Platform.runLater(() -> {
                if (hasBiometric) {
                    biometricStatusLabel.setText(I18n.get("profile.biometric.status.configured"));
                    biometricStatusLabel.setStyle("-fx-text-fill: -fx-success; -fx-font-weight: bold;");
                    biometricBtn.setText(I18n.get("profile.biometric.reconfigure"));
                } else {
                    biometricStatusLabel.setText(I18n.get("profile.biometric.status.not_configured"));
                    biometricStatusLabel.setStyle("-fx-text-fill: -fx-destructive; -fx-font-weight: bold;");
                    biometricBtn.setText(I18n.get("profile.biometric.configure"));
                }
            });
        }, "BiometricStatusThread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleBiometricSetup() {
        if (currentUser == null)
            return;

        Stage ownerStage = (Stage) biometricBtn.getScene().getWindow();
        BiometricAuthController.showRegistrationDialog(ownerStage, currentUser.getUsername(), (registeredUser) -> {
            updateBiometricStatus();
            com.skilora.utils.DialogUtils.showInfo(
                    I18n.get("profile.biometric.success"),
                    I18n.get("profile.biometric.success_msg"));
        });
    }

    @FXML
    private void handleSave() {
        if (currentUser == null)
            return;

        saveBtn.setDisable(true);
        saveBtn.setText(I18n.get("profile.saving"));
        statusLabel.setText(I18n.get("profile.saving"));

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Update User Record
                currentUser.setFullName(firstNameField.getText() + " " + lastNameField.getText());
                String email = emailField != null ? emailField.getText() : null;
                if (email != null && !email.trim().isEmpty()) {
                    currentUser.setEmail(email.trim());
                }
                userService.saveUser(currentUser);

                // 2. Update Profile Record
                if (userProfile == null) {
                    userProfile = new Profile();
                    userProfile.setUserId(currentUser.getId());
                }
                userProfile.setFirstName(firstNameField.getText());
                userProfile.setLastName(lastNameField.getText());
                userProfile.setPhone(phoneField.getText());
                userProfile.setLocation(locationField.getText());

                profileService.saveProfile(userProfile);

                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.updated"));
            statusLabel.setStyle("-fx-text-fill: -fx-success;");

            if (onProfileUpdated != null) {
                onProfileUpdated.run();
            }
        });

        saveTask.setOnFailed(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.error"));
            statusLabel.setStyle("-fx-text-fill: -fx-destructive;");
            logger.error("Failed to save profile", e.getSource().getException());
        });

        new Thread(saveTask).start();
    }
}
