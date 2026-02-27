package com.skilora.controller.usermanagement;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLAvatar;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.service.usermanagement.UserService;
import com.skilora.service.usermanagement.ProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
    private TLAvatar profileAvatar;
    @FXML
    private TLButton changePhotoBtn;
    @FXML
    private TLButton saveBtn;
    @FXML
    private Label statusLabel;

    private User currentUser;
    private Profile currentProfile;
    private UserService userService;
    private ProfileService profileService;
    private Runnable onProfileUpdated;
    private File selectedPhotoFile;
    private String photoFilePath;

    public void initializeContext(User user, Runnable onProfileUpdated) {
        this.currentUser = user;
        this.onProfileUpdated = onProfileUpdated;
        this.userService = UserService.getInstance();
        this.profileService = ProfileService.getInstance();

        loadProfile();
        populateFields();
    }

    private void loadProfile() {
        try {
            currentProfile = profileService.findProfileByUserId(currentUser.getId());
            if (currentProfile == null) {
                // Profile doesn't exist yet, will be created on save
                currentProfile = null;
            }
        } catch (Exception e) {
            logger.error("Error loading profile", e);
            currentProfile = null;
        }
    }

    private void populateFields() {
        if (currentUser == null)
            return;

        // Split name for demo purposes if separate fields not available in entity
        if (currentUser.getFullName() != null) {
            String[] parts = currentUser.getFullName().split(" ", 2);
            firstNameField.setText(parts[0]);
            if (parts.length > 1)
                lastNameField.setText(parts[1]);
        }

        // Populate email from user entity
        if (emailField != null && currentUser.getEmail() != null) {
            emailField.setText(currentUser.getEmail());
        }

        // Populate profile fields if profile exists
        if (currentProfile != null) {
            if (phoneField != null && currentProfile.getPhone() != null) {
                phoneField.setText(currentProfile.getPhone());
            }
            if (locationField != null && currentProfile.getLocation() != null) {
                locationField.setText(currentProfile.getLocation());
            }
        }

        // Load profile photo
        loadProfilePhoto();

    }

    private void loadProfilePhoto() {
        if (profileAvatar == null)
            return;

        String photoUrl = null;
        if (currentProfile != null && currentProfile.getPhotoUrl() != null
                && !currentProfile.getPhotoUrl().trim().isEmpty()) {
            photoUrl = currentProfile.getPhotoUrl();
        } else if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().trim().isEmpty()) {
            photoUrl = currentUser.getPhotoUrl();
        }

        // Set initials as fallback
        String fullName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
        String initials = getInitialsFromName(fullName);
        profileAvatar.setFallback(initials);
        profileAvatar.setSize(TLAvatar.Size.LG);

        // Load photo if available
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            try {
                Path photoPath = Paths.get(photoUrl);
                File photoFile = photoPath.toFile();
                if (photoFile.exists()) {
                    Image img = new Image(photoFile.toURI().toString(), 96, 96, true, true, true);
                    if (!img.isError()) {
                        profileAvatar.setImage(img);
                    } else {
                        profileAvatar.setImage(null);
                    }
                } else {
                    profileAvatar.setImage(null);
                }
            } catch (Exception e) {
                logger.debug("Could not load profile photo: {}", e.getMessage());
                profileAvatar.setImage(null);
            }
        } else {
            profileAvatar.setImage(null);
        }
    }

    private String getInitialsFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0];
            String last = parts[parts.length - 1];
            return ((first.isEmpty() ? "" : first.substring(0, 1)) +
                    (last.isEmpty() ? "" : last.substring(0, 1))).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    @FXML
    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        Stage stage = null;
        if (changePhotoBtn != null && changePhotoBtn.getScene() != null) {
            stage = (Stage) changePhotoBtn.getScene().getWindow();
        }

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            // Validate file size (max 2MB)
            long fileSize = file.length();
            if (fileSize > 2 * 1024 * 1024) {
                com.skilora.utils.DialogUtils.showError("Erreur", "La taille de l'image ne doit pas dépasser 2MB.");
                return;
            }

            // Validate file type
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") &&
                    !fileName.endsWith(".png") && !fileName.endsWith(".gif")) {
                com.skilora.utils.DialogUtils.showError("Erreur", "Veuillez sélectionner une image (JPG, PNG ou GIF).");
                return;
            }

            selectedPhotoFile = file;
            savePhotoFile();
            updatePhotoDisplay();
        }
    }

    private void savePhotoFile() {
        if (selectedPhotoFile == null)
            return;

        try {
            // Create uploads/photos directory if it doesn't exist
            Path uploadsDir = Paths.get("uploads", "photos");
            Files.createDirectories(uploadsDir);

            // Generate unique filename
            String extension = getFileExtension(selectedPhotoFile.getName());
            String fileName = currentUser.getId() + "_" + System.currentTimeMillis() + extension;
            Path targetPath = uploadsDir.resolve(fileName);

            // Copy file to uploads directory
            Files.copy(selectedPhotoFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Store relative path for database
            photoFilePath = "uploads/photos/" + fileName;

            // Update photo display immediately
            Image img = new Image(targetPath.toUri().toString(), 96, 96, true, true, true);
            if (!img.isError()) {
                profileAvatar.setImage(img);
            }

        } catch (Exception e) {
            logger.error("Error saving photo file", e);
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible de sauvegarder la photo.");
            selectedPhotoFile = null;
            photoFilePath = null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private void updatePhotoDisplay() {
        if (selectedPhotoFile != null && profileAvatar != null) {
            changePhotoBtn.setText("✓ Photo sélectionnée");
            changePhotoBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        } else {
            changePhotoBtn.setText("Changer la photo");
            changePhotoBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        }
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
                // Update user model
                currentUser.setFullName(firstNameField.getText() + " " + lastNameField.getText());

                // Save email if provided
                String email = emailField != null ? emailField.getText() : null;
                if (email != null && !email.trim().isEmpty()) {
                    currentUser.setEmail(email.trim());
                }

                userService.saveUser(currentUser);

                // Get or create profile
                Profile profile = profileService.findProfileByUserId(currentUser.getId());
                if (profile == null) {
                    // Create new profile
                    profile = new Profile();
                    profile.setUserId(currentUser.getId());
                    String[] nameParts = currentUser.getFullName().split(" ", 2);
                    profile.setFirstName(nameParts.length > 0 ? nameParts[0] : currentUser.getUsername());
                    profile.setLastName(nameParts.length > 1 ? nameParts[1] : "");

                    // Save photo if selected
                    if (photoFilePath != null && !photoFilePath.trim().isEmpty()) {
                        profile.setPhotoUrl(photoFilePath);
                    }

                    // Save other profile fields if available
                    if (phoneField != null && !phoneField.getText().trim().isEmpty()) {
                        profile.setPhone(phoneField.getText().trim());
                    }
                    if (locationField != null && !locationField.getText().trim().isEmpty()) {
                        profile.setLocation(locationField.getText().trim());
                    }

                    profileService.createProfile(profile);
                } else {
                    // Update existing profile
                    String[] nameParts = currentUser.getFullName().split(" ", 2);
                    profile.setFirstName(nameParts.length > 0 ? nameParts[0] : currentUser.getUsername());
                    profile.setLastName(nameParts.length > 1 ? nameParts[1] : "");

                    // Update photo if selected
                    if (photoFilePath != null && !photoFilePath.trim().isEmpty()) {
                        profile.setPhotoUrl(photoFilePath);
                    }

                    // Update other profile fields
                    if (phoneField != null) {
                        profile.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : null);
                    }
                    if (locationField != null) {
                        profile.setLocation(locationField.getText() != null ? locationField.getText().trim() : null);
                    }

                    profileService.updateProfile(profile);
                }

                // Update user's photo URL from profile for sidebar display
                Profile updatedProfile = profileService.findProfileByUserId(currentUser.getId());
                if (updatedProfile != null && updatedProfile.getPhotoUrl() != null) {
                    currentUser.setPhotoUrl(updatedProfile.getPhotoUrl());
                    userService.saveUser(currentUser);
                }

                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.updated"));
            statusLabel.setStyle("-fx-text-fill: -fx-success;");

            // Reload profile to get updated data
            loadProfile();
            loadProfilePhoto();

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
