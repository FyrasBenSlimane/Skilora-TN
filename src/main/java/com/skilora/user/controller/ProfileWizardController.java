package com.skilora.user.controller;

import com.skilora.framework.components.TLAvatar;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.user.entity.User;
import com.skilora.user.entity.Profile;
import com.skilora.user.service.BiometricService;
import com.skilora.user.service.ProfileService;
import com.skilora.user.service.UserService;
import com.skilora.utils.ImageUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ProfileWizardController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileWizardController.class);

    @FXML
    private TLAvatar photoAvatar;
    @FXML
    private TLButton uploadPhotoBtn;
    @FXML
    private TLButton removePhotoBtn;
    @FXML
    private Label photoStatusLabel;

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
    private TLButton cancelBtn;
    @FXML
    private HBox actionsBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label biometricStatusLabel;
    @FXML
    private TLButton biometricBtn;

    private User currentUser;
    private Profile currentProfile;
    private UserService userService;
    private ProfileService profileService;
    private Runnable onProfileUpdated;
    private boolean dirty = false;

    /** Snapshot of original field values for cancel/reset */
    private String origFirstName, origLastName, origHeadline, origBio;
    private String origEmail, origPhone, origLocation, origWebsite;

    public void initializeContext(User user, Runnable onProfileUpdated) {
        this.currentUser = user;
        this.onProfileUpdated = onProfileUpdated;
        this.userService = UserService.getInstance();
        this.profileService = ProfileService.getInstance();

        loadProfileFromDb();
        populateFields();
    }

    /**
     * Loads (or creates) the Profile row from the database for the current user.
     */
    private void loadProfileFromDb() {
        try {
            currentProfile = profileService.findProfileByUserId(currentUser.getId());
            if (currentProfile == null) {
                // First visit — seed names from User.fullName
                String firstName = "";
                String lastName = "";
                if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
                    String[] parts = currentUser.getFullName().trim().split("\\s+", 2);
                    firstName = parts[0];
                    if (parts.length > 1) lastName = parts[1];
                }
                currentProfile = new Profile(currentUser.getId(),
                        firstName.isEmpty() ? "-" : firstName,
                        lastName.isEmpty() ? "-" : lastName);
                profileService.createProfile(currentProfile);
            }
        } catch (Exception e) {
            logger.error("Failed to load/create profile for user {}", currentUser.getId(), e);
            currentProfile = new Profile(currentUser.getId(), "-", "-");
        }
    }

    private void populateFields() {
        if (currentUser == null)
            return;

        // Populate from Profile entity (has precedence for name parts)
        if (currentProfile != null) {
            if (currentProfile.getFirstName() != null) {
                firstNameField.setText(currentProfile.getFirstName());
            }
            if (currentProfile.getLastName() != null) {
                lastNameField.setText(currentProfile.getLastName());
            }
            if (headlineField != null && currentProfile.getHeadline() != null) {
                headlineField.setText(currentProfile.getHeadline());
            }
            if (bioArea != null && currentProfile.getBio() != null) {
                bioArea.setText(currentProfile.getBio());
            }
            if (phoneField != null && currentProfile.getPhone() != null) {
                phoneField.setText(currentProfile.getPhone());
            }
            if (locationField != null && currentProfile.getLocation() != null) {
                locationField.setText(currentProfile.getLocation());
            }
            if (websiteField != null && currentProfile.getWebsite() != null) {
                websiteField.setText(currentProfile.getWebsite());
            }
        }

        // Fallback: if Profile names are empty, split from User.fullName
        if ((firstNameField.getText() == null || firstNameField.getText().isBlank())
                && currentUser.getFullName() != null) {
            String[] parts = currentUser.getFullName().split(" ", 2);
            firstNameField.setText(parts[0]);
            if (parts.length > 1)
                lastNameField.setText(parts[1]);
        }

        // Populate email from User entity
        if (emailField != null && currentUser.getEmail() != null) {
            emailField.setText(currentUser.getEmail());
        }

        // Load profile photo
        loadProfilePhoto();

        // Check biometric status
        updateBiometricStatus();

        // Snapshot original values for cancel/reset
        snapshotOriginalValues();

        // Install dirty-state listeners (must be AFTER population)
        setupDirtyTracking();
    }

    /** Stores current field values so cancel can restore them. */
    private void snapshotOriginalValues() {
        origFirstName = getText(firstNameField);
        origLastName  = getText(lastNameField);
        origHeadline  = getText(headlineField);
        origBio       = bioArea != null ? (bioArea.getText() != null ? bioArea.getText() : "") : "";
        origEmail     = getText(emailField);
        origPhone     = getText(phoneField);
        origLocation  = getText(locationField);
        origWebsite   = getText(websiteField);
    }

    private String getText(TLTextField field) {
        if (field == null) return "";
        return field.getText() != null ? field.getText() : "";
    }

    /** Installs text-change listeners on every editable field to show/hide the actions bar. */
    private void setupDirtyTracking() {
        javafx.beans.value.ChangeListener<String> dirtyListener = (obs, oldVal, newVal) -> markDirty();

        if (firstNameField != null) firstNameField.getControl().textProperty().addListener(dirtyListener);
        if (lastNameField  != null) lastNameField.getControl().textProperty().addListener(dirtyListener);
        if (headlineField  != null) headlineField.getControl().textProperty().addListener(dirtyListener);
        if (emailField     != null) emailField.getControl().textProperty().addListener(dirtyListener);
        if (phoneField     != null) phoneField.getControl().textProperty().addListener(dirtyListener);
        if (locationField  != null) locationField.getControl().textProperty().addListener(dirtyListener);
        if (websiteField   != null) websiteField.getControl().textProperty().addListener(dirtyListener);
        if (bioArea        != null) bioArea.textProperty().addListener(dirtyListener);
    }

    /** Shows the actions bar when the form becomes dirty. */
    private void markDirty() {
        if (!dirty) {
            dirty = true;
            if (actionsBar != null) {
                actionsBar.setVisible(true);
                actionsBar.setManaged(true);
            }
            statusLabel.setText("");
        }
    }

    /** Hides the actions bar and clears dirty state. */
    private void clearDirty() {
        dirty = false;
        if (actionsBar != null) {
            actionsBar.setVisible(false);
            actionsBar.setManaged(false);
        }
    }

    @FXML
    private void handleCancel() {
        // Restore original values
        if (firstNameField != null) firstNameField.setText(origFirstName);
        if (lastNameField  != null) lastNameField.setText(origLastName);
        if (headlineField  != null) headlineField.setText(origHeadline);
        if (bioArea        != null) bioArea.setText(origBio);
        if (emailField     != null) emailField.setText(origEmail);
        if (phoneField     != null) phoneField.setText(origPhone);
        if (locationField  != null) locationField.setText(origLocation);
        if (websiteField   != null) websiteField.setText(origWebsite);

        clearDirty();
    }

    private void loadProfilePhoto() {
        if (photoAvatar == null) return;

        // Set avatar size to LG for the profile wizard
        photoAvatar.setSize(TLAvatar.Size.LG);

        String initials = "?";
        if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
            String[] parts = currentUser.getFullName().trim().split("\\s+");
            if (parts.length >= 2) {
                initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
        }
        photoAvatar.setFallback(initials);

        String photoUrl = currentUser.getPhotoUrl();
        Image img = ImageUtils.loadProfileImage(photoUrl, 96, 96);
        if (img != null) {
            photoAvatar.setImage(img);
            if (removePhotoBtn != null) removePhotoBtn.setVisible(true);
        } else {
            photoAvatar.setImage(null);
            if (removePhotoBtn != null) removePhotoBtn.setVisible(false);
        }
    }

    @FXML
    private void handleUploadPhoto() {
        if (currentUser == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("profile.photo.choose_title"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        I18n.get("profile.photo.filter_images"),
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp")
        );

        Stage stage = (Stage) uploadPhotoBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) return;

        // Validate file
        String validationError = ImageUtils.validateImageFile(selectedFile);
        if (validationError != null) {
            photoStatusLabel.setText(I18n.get(validationError));
            photoStatusLabel.setStyle("-fx-text-fill: -fx-destructive;");
            return;
        }

        try {
            // Encode to Base64 data URI
            String base64DataUri = ImageUtils.encodeToBase64DataUri(selectedFile);

            // Update the user model
            currentUser.setPhotoUrl(base64DataUri);

            // Preview the image
            Image img = ImageUtils.decodeBase64ToImage(base64DataUri, 96, 96);
            if (img != null) {
                photoAvatar.setImage(img);
                removePhotoBtn.setVisible(true);
                photoStatusLabel.setText(I18n.get("profile.photo.uploaded",
                        ImageUtils.formatFileSize(selectedFile.length())));
                photoStatusLabel.setStyle("-fx-text-fill: -fx-success;");
            }
        } catch (Exception e) {
            logger.error("Failed to encode image to Base64", e);
            photoStatusLabel.setText(I18n.get("profile.photo.error.encode_failed"));
            photoStatusLabel.setStyle("-fx-text-fill: -fx-destructive;");
        }
    }

    @FXML
    private void handleRemovePhoto() {
        currentUser.setPhotoUrl(null);
        photoAvatar.setImage(null);
        removePhotoBtn.setVisible(false);
        photoStatusLabel.setText(I18n.get("profile.photo.removed"));
        photoStatusLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
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
        if (currentUser == null) return;

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
                String firstName = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
                String lastName = lastNameField.getText() != null ? lastNameField.getText().trim() : "";

                // Update User entity
                currentUser.setFullName((firstName + " " + lastName).trim());

                String email = emailField != null ? emailField.getText() : null;
                if (email != null && !email.trim().isEmpty()) {
                    currentUser.setEmail(email.trim());
                }

                userService.saveUser(currentUser);

                // Update Profile entity
                currentProfile.setFirstName(firstName.isEmpty() ? null : firstName);
                currentProfile.setLastName(lastName.isEmpty() ? null : lastName);

                if (headlineField != null) {
                    String h = headlineField.getText();
                    currentProfile.setHeadline(h != null && !h.isBlank() ? h.trim() : null);
                }
                if (bioArea != null) {
                    String b = bioArea.getText();
                    currentProfile.setBio(b != null && !b.isBlank() ? b.trim() : null);
                }
                if (phoneField != null) {
                    String p = phoneField.getText();
                    currentProfile.setPhone(p != null && !p.isBlank() ? p.trim() : null);
                }
                if (locationField != null) {
                    String l = locationField.getText();
                    currentProfile.setLocation(l != null && !l.isBlank() ? l.trim() : null);
                }
                if (websiteField != null) {
                    String w = websiteField.getText();
                    currentProfile.setWebsite(w != null && !w.isBlank() ? w.trim() : null);
                }

                // Sync photo URL to profile as well
                currentProfile.setPhotoUrl(currentUser.getPhotoUrl());

                profileService.updateProfile(currentProfile);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.updated"));
            statusLabel.setStyle("-fx-text-fill: -fx-success;");

            // Re-snapshot and clear dirty state
            snapshotOriginalValues();
            dirty = false;
            // Keep actionsBar visible briefly so user sees success message
            // It will hide on next populateFields or after a short delay
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(ev -> clearDirty());
            pause.play();

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
