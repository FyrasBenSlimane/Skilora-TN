package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextarea;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.JobService;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.User;
import com.skilora.user.service.ProfileService;
import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

/**
 * ApplicationDialogController – Handles job application with mandatory CV upload.
 */
public class ApplicationDialogController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationDialogController.class);

    @FXML private Label cvLabel;
    @FXML private TLButton selectCvBtn;
    @FXML private TLButton submitBtn;
    @FXML private TLButton cancelBtn;
    @FXML private TLTextarea coverLetterField;
    @FXML private Label cvAnalysisLabel;

    private int jobOfferId;
    private User currentUser;
    private File selectedCvFile;
    private String cvFilePath;
    private Runnable onSuccess;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Called after FXML loading
    }

    public void setup(int jobOfferId, User user, Runnable onSuccess, Stage stage) {
        this.jobOfferId = jobOfferId;
        this.currentUser = user;
        this.onSuccess = onSuccess;
        this.dialogStage = stage;
        setupUI();
    }

    private void setupUI() {
        if (cvLabel == null || selectCvBtn == null || submitBtn == null || coverLetterField == null) {
            logger.error("FXML components not loaded for ApplicationDialogController");
            return;
        }

        checkJobOfferStatus();

        cvLabel.setText(I18n.get("application.cv.label") + " *");
        selectCvBtn.setText(I18n.get("application.select_cv"));
        selectCvBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        selectCvBtn.setOnAction(e -> selectCvFile());

        coverLetterField.setPromptText(I18n.get("application.cover_letter.placeholder"));
        coverLetterField.getControl().setPrefHeight(200);
        coverLetterField.setPrefRowCount(8);

        if (cancelBtn != null) {
            cancelBtn.setText(I18n.get("common.cancel"));
            cancelBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
            cancelBtn.setOnAction(e -> handleCancel());
        }

        submitBtn.setText(I18n.get("application.submit"));
        submitBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        submitBtn.setOnAction(e -> submitApplication());

        if (cvAnalysisLabel != null) cvAnalysisLabel.setText("");

        updateCvButtonState();
    }

    /**
     * Check if the job offer is still open and if the candidate has already applied.
     */
    private void checkJobOfferStatus() {
        try {
            JobService jobService = JobService.getInstance();
            ApplicationService appService = ApplicationService.getInstance();
            ProfileService profileService = ProfileService.getInstance();

            JobOffer offer = jobService.findJobOfferById(jobOfferId);

            if (offer != null && offer.getStatus() == JobStatus.CLOSED) {
                disableForm(I18n.get("application.offer_closed", "Cette offre est fermée"));
                showError(I18n.get("application.offer_closed", "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler."));
                return;
            }

            if (currentUser != null) {
                Profile profile = profileService.findProfileByUserId(currentUser.getId());
                if (profile != null && profile.getId() > 0) {
                    boolean alreadyApplied = appService.hasApplied(jobOfferId, profile.getId());
                    if (alreadyApplied) {
                        disableForm(I18n.get("application.already_applied", "Déjà postulé"));
                        showError(I18n.get("application.already_applied_message", "Vous avez déjà postulé à cette offre d'emploi."));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error checking job offer status", e);
        }
    }

    private void disableForm(String submitLabel) {
        if (selectCvBtn != null) selectCvBtn.setDisable(true);
        if (submitBtn != null) {
            submitBtn.setDisable(true);
            submitBtn.setText(submitLabel);
        }
        if (coverLetterField != null) {
            coverLetterField.getControl().setEditable(false);
            coverLetterField.setPromptText(submitLabel);
        }
    }

    private void selectCvFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("application.select_cv"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        Stage stageToUse = dialogStage;
        if (stageToUse == null && selectCvBtn != null && selectCvBtn.getScene() != null)
            stageToUse = (Stage) selectCvBtn.getScene().getWindow();

        File file = fileChooser.showOpenDialog(stageToUse);
        if (file != null) {
            if (file.length() == 0) { showError(I18n.get("application.cv.empty_error", "Le fichier est vide")); return; }
            if (file.length() > 5 * 1024 * 1024) { showError(I18n.get("application.cv.size_error", "Le fichier est trop volumineux. Taille maximale: 5MB")); return; }

            String name = file.getName().toLowerCase();
            if (!name.endsWith(".pdf") && !name.endsWith(".doc") && !name.endsWith(".docx")) {
                showError(I18n.get("application.cv.type_error", "Format de fichier non supporté. Formats acceptés: PDF, DOC, DOCX"));
                return;
            }
            if (file.getName().length() > 255) {
                showError(I18n.get("application.cv.name_too_long", "Le nom du fichier est trop long (max 255 caractères)"));
                return;
            }

            selectedCvFile = file;
            saveCvFile();
            updateCvButtonState();
        }
    }

    private void saveCvFile() {
        if (selectedCvFile == null) return;
        try {
            Path uploadsDir = Paths.get("uploads", "cvs");
            Files.createDirectories(uploadsDir);

            String fileName = currentUser.getId() + "_" + jobOfferId + "_" + System.currentTimeMillis()
                    + getFileExtension(selectedCvFile.getName());
            Path targetPath = uploadsDir.resolve(fileName);
            Files.copy(selectedCvFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            cvFilePath = "uploads/cvs/" + fileName;
        } catch (Exception e) {
            showError(I18n.get("application.cv.save_error"));
            selectedCvFile = null;
            cvFilePath = null;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    private void updateCvButtonState() {
        if (selectedCvFile != null) {
            selectCvBtn.setText("✓ " + selectedCvFile.getName());
            selectCvBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        } else {
            selectCvBtn.setText(I18n.get("application.select_cv"));
            selectCvBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        }
    }

    private void handleCancel() {
        if (dialogStage != null) dialogStage.close();
        else if (cancelBtn != null && cancelBtn.getScene() != null)
            ((Stage) cancelBtn.getScene().getWindow()).close();
    }

    private void submitApplication() {
        // Disable submit button immediately to prevent double-submit
        if (submitBtn != null) {
            submitBtn.setDisable(true);
            submitBtn.setText(I18n.get("common.submitting"));
        }

        // Re-validate offer status
        try {
            JobOffer offer = JobService.getInstance().findJobOfferById(jobOfferId);
            if (offer == null) {
                showError(I18n.get("application.offer_not_found", "Cette offre d'emploi n'existe plus."));
                if (dialogStage != null) dialogStage.close();
                return;
            }
            if (offer.getStatus() == JobStatus.CLOSED) {
                showError(I18n.get("application.offer_closed", "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler."));
                if (dialogStage != null) dialogStage.close();
                return;
            }
        } catch (Exception e) {
            logger.error("Error checking job offer status before submission", e);
            showError(I18n.get("application.error", "Erreur lors de la vérification de l'offre."));
            return;
        }

        // Validate CV
        if (selectedCvFile == null || cvFilePath == null) {
            showError(I18n.get("application.cv.required", "Le CV est obligatoire"));
            if (selectCvBtn != null) selectCvBtn.getStyleClass().add("input-error");
            return;
        }
        if (selectCvBtn != null) selectCvBtn.getStyleClass().remove("input-error");

        // Validate cover letter
        String coverLetter = coverLetterField.getText() != null ? coverLetterField.getText().trim() : "";
        if (!coverLetter.isEmpty() && coverLetter.length() > 2000) {
            showError(I18n.get("application.cover_letter_max_length", "La lettre de motivation ne peut pas dépasser 2000 caractères"));
            if (coverLetterField != null) coverLetterField.getStyleClass().add("textarea-error");
            return;
        }
        if (coverLetterField != null) coverLetterField.getStyleClass().remove("textarea-error");

        // Submit
        ProfileService profileService = ProfileService.getInstance();
        try {
            Profile profile = profileService.findProfileByUserId(currentUser.getId());

            if (profile == null) {
                profile = new Profile();
                profile.setUserId(currentUser.getId());
                String fullName = currentUser.getFullName();
                if (fullName == null || fullName.trim().isEmpty()) fullName = currentUser.getUsername();
                String[] nameParts = fullName.split(" ", 2);
                profile.setFirstName(nameParts.length > 0 ? nameParts[0] : currentUser.getUsername());
                profile.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                profile.setCvUrl(cvFilePath);
                int profileId = profileService.createProfile(profile);
                profile.setId(profileId);
            } else {
                profile.setCvUrl(cvFilePath);
                profileService.updateProfile(profile);
            }

            ApplicationService appService = ApplicationService.getInstance();
            if (appService.hasApplied(jobOfferId, profile.getId())) {
                showError(I18n.get("application.already_applied_message", "Vous avez déjà postulé à cette offre d'emploi."));
                if (dialogStage != null) dialogStage.close();
                return;
            }

            int applicationId = appService.apply(jobOfferId, profile.getId(), coverLetter, cvFilePath);

            if (applicationId > 0) {
                showSuccess(I18n.get("application.success"));
                if (onSuccess != null) onSuccess.run();
                if (dialogStage != null) dialogStage.close();
            } else {
                if (appService.hasApplied(jobOfferId, profile.getId()))
                    showError(I18n.get("application.already_applied_message", "Vous avez déjà postulé à cette offre d'emploi."));
                else
                    showError(I18n.get("application.error"));
            }
        } catch (SQLException e) {
            logger.error("Error submitting application", e);
            showError(I18n.get("application.database_error"));
        }
    }

    private void showError(String message) {
        // Re-enable submit button on error (unless dialog is closing)
        if (submitBtn != null) {
            submitBtn.setDisable(false);
            submitBtn.setText(I18n.get("application.submit"));
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18n.get("error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("success.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
