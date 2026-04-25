package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLTextarea;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;
import com.skilora.service.recruitment.RecruitmentIntelligenceService;
import com.skilora.service.usermanagement.ProfileService;
import com.skilora.utils.I18n;
import com.skilora.utils.Validators;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

/**
 * ApplicationDialogController - Handles job application with mandatory CV upload
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
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();

    @FXML
    public void initialize() {
        // FXML initialization - will be called after FXML loading
    }
    
    public void setup(int jobOfferId, User user, Runnable onSuccess, Stage stage) {
        this.jobOfferId = jobOfferId;
        this.currentUser = user;
        this.onSuccess = onSuccess;
        this.dialogStage = stage;
        
        setupUI();
    }
    
    private void setupUI() {
        // Check if FXML components are loaded
        if (cvLabel == null || selectCvBtn == null || submitBtn == null || coverLetterField == null) {
            System.err.println("ERROR: ApplicationDialogController FXML components not loaded!");
            return;
        }
        
        // Check if job offer is still open (not CLOSED)
        checkJobOfferStatus();
        
        cvLabel.setText(I18n.get("application.cv.label") + " *");
        selectCvBtn.setText(I18n.get("application.select_cv"));
        selectCvBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        selectCvBtn.setOnAction(e -> selectCvFile());
        
        coverLetterField.setPromptText(I18n.get("application.cover_letter.placeholder"));
        // Set height for the TextArea - make it taller for better UX
        coverLetterField.setTextAreaHeight(200);
        coverLetterField.setTextAreaRowCount(8);
        
        if (cancelBtn != null) {
            cancelBtn.setText(I18n.get("common.cancel"));
            cancelBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        }

        submitBtn.setText(I18n.get("application.submit"));
        submitBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        if (cvAnalysisLabel != null) {
            cvAnalysisLabel.setText("");
        }
        
        updateCvButtonState();
    }
    
    /**
     * Check if the job offer is still open (not CLOSED) and if candidate has already applied.
     * If closed or already applied, disable the form and show a message.
     */
    private void checkJobOfferStatus() {
        try {
            com.skilora.service.recruitment.JobService jobService = com.skilora.service.recruitment.JobService.getInstance();
            com.skilora.service.recruitment.ApplicationService appService = com.skilora.service.recruitment.ApplicationService.getInstance();
            com.skilora.service.usermanagement.ProfileService profileService = com.skilora.service.usermanagement.ProfileService.getInstance();
            
            com.skilora.model.entity.recruitment.JobOffer offer = jobService.findJobOfferById(jobOfferId);
            
            if (offer != null && offer.getStatus() == com.skilora.model.enums.JobStatus.CLOSED) {
                // Offer is closed - disable form
                if (selectCvBtn != null) {
                    selectCvBtn.setDisable(true);
                }
                if (submitBtn != null) {
                    submitBtn.setDisable(true);
                    submitBtn.setText(I18n.get("application.offer_closed", "Cette offre est fermée"));
                }
                if (coverLetterField != null) {
                    coverLetterField.setEditable(false);
                    coverLetterField.setPromptText(I18n.get("application.offer_closed_message", "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler."));
                }
                showError(I18n.get("application.offer_closed", "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler."));
                return;
            }
            
            // Check if candidate has already applied
            if (currentUser != null) {
                com.skilora.model.entity.usermanagement.Profile profile = profileService.findProfileByUserId(currentUser.getId());
                if (profile != null && profile.getId() > 0) {
                    boolean alreadyApplied = appService.hasApplied(jobOfferId, profile.getId());
                    if (alreadyApplied) {
                        // Already applied - disable form
                        if (selectCvBtn != null) {
                            selectCvBtn.setDisable(true);
                        }
                        if (submitBtn != null) {
                            submitBtn.setDisable(true);
                            submitBtn.setText(I18n.get("application.already_applied", "Déjà postulé"));
                        }
                        if (coverLetterField != null) {
                            coverLetterField.setEditable(false);
                            coverLetterField.setPromptText(I18n.get("application.already_applied_message", "Vous avez déjà postulé à cette offre d'emploi."));
                        }
                        showError(I18n.get("application.already_applied_message", "Vous avez déjà postulé à cette offre d'emploi."));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error checking job offer status", e);
        }
    }
    
    private void selectCvFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("application.select_cv"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Ensure we have a valid stage
        Stage stageToUse = dialogStage;
        if (stageToUse == null && selectCvBtn != null && selectCvBtn.getScene() != null) {
            stageToUse = (Stage) selectCvBtn.getScene().getWindow();
        }
        
        File file = fileChooser.showOpenDialog(stageToUse);
        if (file != null) {
            // Validate file size (max 5MB)
            long fileSize = file.length();
            if (fileSize == 0) {
                showError(I18n.get("application.cv.empty_error", "Le fichier est vide"));
                return;
            }
            if (fileSize > 5 * 1024 * 1024) {
                showError(I18n.get("application.cv.size_error", "Le fichier est trop volumineux. Taille maximale: 5MB"));
                return;
            }
            
            // Validate file type
            String fileName = file.getName().toLowerCase();
            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".doc") && !fileName.endsWith(".docx")) {
                showError(I18n.get("application.cv.type_error", "Format de fichier non supporté. Formats acceptés: PDF, DOC, DOCX"));
                return;
            }
            
            // Validate file name length
            if (file.getName().length() > 255) {
                showError(I18n.get("application.cv.name_too_long", "Le nom du fichier est trop long (max 255 caractères)"));
                return;
            }
            
            selectedCvFile = file;
            saveCvFile();
            updateCvButtonState();
            analyzeSelectedCv();
        }
    }

    private void analyzeSelectedCv() {
        if (selectedCvFile == null || cvAnalysisLabel == null) {
            return;
        }
        cvAnalysisLabel.setText("Analyse CV en cours...");
        intelligenceService.analyzeCvAndStore(resolveProfileIdForAnalysis(), selectedCvFile)
                .thenAccept(analysis -> Platform.runLater(() -> {
                    if (analysis == null) {
                        cvAnalysisLabel.setText("Analyse IA indisponible (service Python hors ligne).");
                        return;
                    }
                    int years = analysis.has("years_of_experience") ? analysis.get("years_of_experience").getAsInt() : 0;
                    int skills = analysis.has("skills_detected") ? analysis.getAsJsonArray("skills_detected").size() : 0;
                    String level = analysis.has("experience_level") ? analysis.get("experience_level").getAsString() : "N/A";
                    cvAnalysisLabel.setText("IA CV: " + skills + " competences detectees | " + years + " ans | niveau " + level);
                }));
    }

    private int resolveProfileIdForAnalysis() {
        try {
            if (currentUser == null) {
                return 0;
            }
            var profile = ProfileService.getInstance().findProfileByUserId(currentUser.getId());
            return profile != null ? profile.getId() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
    
    private void saveCvFile() {
        if (selectedCvFile == null) return;
        
        try {
            // Create uploads directory if it doesn't exist
            Path uploadsDir = Paths.get("uploads", "cvs");
            Files.createDirectories(uploadsDir);
            
            // Generate unique filename
            String fileName = currentUser.getId() + "_" + jobOfferId + "_" + System.currentTimeMillis() + 
                             getFileExtension(selectedCvFile.getName());
            Path targetPath = uploadsDir.resolve(fileName);
            
            // Copy file to uploads directory
            Files.copy(selectedCvFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Store relative path for database
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
    
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        } else if (cancelBtn != null && cancelBtn.getScene() != null && cancelBtn.getScene().getWindow() != null) {
            ((Stage) cancelBtn.getScene().getWindow()).close();
        }
    }

    /**
     * Runs on the JavaFX thread. DB work runs on a background thread so the UI
     * stays responsive and feedback dialogs stack correctly on this stage.
     */
    @FXML
    private void submitApplication() {
        if (submitBtn != null && submitBtn.isDisable()) {
            return;
        }

        if (currentUser == null) {
            showError(I18n.get("application.error", "Session invalide. Veuillez vous reconnecter."));
            return;
        }

        if (selectedCvFile == null || cvFilePath == null) {
            showError(I18n.get("application.cv.required", "Le CV est obligatoire"));
            if (selectCvBtn != null) {
                selectCvBtn.getStyleClass().add("input-error");
            }
            return;
        }
        if (selectCvBtn != null) {
            selectCvBtn.getStyleClass().remove("input-error");
        }

        String coverLetter = coverLetterField != null && coverLetterField.getText() != null
                ? coverLetterField.getText().trim()
                : "";
        if (!coverLetter.isEmpty()) {
            if (!Validators.maxLength(coverLetter, 2000)) {
                showError(I18n.get("application.cover_letter_max_length",
                        "La lettre de motivation ne peut pas dépasser 2000 caractères"));
                if (coverLetterField != null) {
                    coverLetterField.getStyleClass().add("textarea-error");
                }
                return;
            }
        }
        if (coverLetterField != null) {
            coverLetterField.getStyleClass().remove("textarea-error");
        }

        final String coverLetterFinal = coverLetter;
        final String cvPathFinal = cvFilePath;
        final int offerIdFinal = jobOfferId;
        final User userFinal = currentUser;

        if (submitBtn != null) {
            submitBtn.setDisable(true);
            submitBtn.setText(I18n.get("application.sending", "Envoi en cours…"));
        }

        Thread worker = new Thread(() -> {
            try {
                com.skilora.service.recruitment.JobService jobService =
                        com.skilora.service.recruitment.JobService.getInstance();
                com.skilora.model.entity.recruitment.JobOffer offer = jobService.findJobOfferById(offerIdFinal);

                if (offer == null) {
                    Platform.runLater(() -> {
                        showError(I18n.get("application.offer_not_found", "Cette offre d'emploi n'existe plus."));
                        if (dialogStage != null) {
                            dialogStage.close();
                        }
                    });
                    return;
                }
                if (offer.getStatus() == com.skilora.model.enums.JobStatus.CLOSED) {
                    Platform.runLater(() -> {
                        showError(I18n.get("application.offer_closed",
                                "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler."));
                        if (dialogStage != null) {
                            dialogStage.close();
                        }
                    });
                    return;
                }

                ProfileService profileService = ProfileService.getInstance();
                com.skilora.model.entity.usermanagement.Profile profile =
                        profileService.findProfileByUserId(userFinal.getId());

                if (profile == null) {
                    profile = new com.skilora.model.entity.usermanagement.Profile();
                    profile.setUserId(userFinal.getId());
                    String fullName = userFinal.getFullName();
                    if (fullName == null || fullName.trim().isEmpty()) {
                        fullName = userFinal.getUsername() != null ? userFinal.getUsername() : "User";
                    }
                    String trimmed = fullName.trim();
                    String[] nameParts = trimmed.split("\\s+", 2);
                    String firstName = !nameParts[0].isEmpty()
                            ? nameParts[0]
                            : (userFinal.getUsername() != null ? userFinal.getUsername() : "User");
                    // ProfileService.validateProfile requires non-blank last name
                    String lastName = nameParts.length > 1 && !nameParts[1].trim().isEmpty()
                            ? nameParts[1].trim()
                            : firstName;
                    profile.setFirstName(firstName);
                    profile.setLastName(lastName);
                    profile.setCvUrl(cvPathFinal);
                    int profileId = profileService.createProfile(profile);
                    profile.setId(profileId);
                } else {
                    profile.setCvUrl(cvPathFinal);
                    profileService.updateProfile(profile);
                }

                ApplicationService appService = ApplicationService.getInstance();
                if (appService.hasApplied(offerIdFinal, profile.getId())) {
                    Platform.runLater(() -> {
                        showError(I18n.get("application.already_applied_message",
                                "Vous avez déjà postulé à cette offre d'emploi."));
                        if (dialogStage != null) {
                            dialogStage.close();
                        }
                    });
                    return;
                }

                int applicationId = appService.apply(offerIdFinal, profile.getId(), coverLetterFinal, cvPathFinal);
                final int profileIdForUi = profile.getId();

                Platform.runLater(() -> {
                    if (applicationId > 0) {
                        showSuccess(I18n.get("application.success"));
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        if (dialogStage != null) {
                            dialogStage.close();
                        }
                    } else {
                        try {
                            if (appService.hasApplied(offerIdFinal, profileIdForUi)) {
                                showError(I18n.get("application.already_applied_message",
                                        "Vous avez déjà postulé à cette offre d'emploi."));
                            } else {
                                showError(I18n.get("application.error"));
                            }
                        } catch (SQLException e) {
                            logger.error("Error re-checking application state", e);
                            showError(I18n.get("application.error"));
                        }
                        restoreSubmitButton();
                    }
                });
            } catch (SQLException e) {
                logger.error("Error submitting application", e);
                Platform.runLater(() -> {
                    String base = I18n.get("application.database_error");
                    String detail = e.getMessage();
                    if (detail != null && !detail.isBlank()) {
                        base = base + "\n" + detail;
                    }
                    showError(base);
                    restoreSubmitButton();
                });
            } catch (Throwable t) {
                logger.error("Unexpected error submitting application", t);
                Platform.runLater(() -> {
                    String extra = t.getMessage() != null ? ("\n" + t.getMessage()) : "";
                    showError(I18n.get("application.error") + extra);
                    restoreSubmitButton();
                });
            }
        }, "application-submit");
        worker.setDaemon(true);
        worker.start();
    }

    private void restoreSubmitButton() {
        if (submitBtn != null) {
            submitBtn.setDisable(false);
            submitBtn.setText(I18n.get("application.submit"));
        }
    }
    
    /**
     * Owner for blocking alerts: must be the application modal stage so dialogs appear
     * on top (otherwise JavaFX may stack them behind the modal and the UI looks "dead").
     */
    private Window resolveAlertOwner() {
        if (dialogStage != null) {
            return dialogStage;
        }
        if (submitBtn != null && submitBtn.getScene() != null && submitBtn.getScene().getWindow() != null) {
            return submitBtn.getScene().getWindow();
        }
        return null;
    }

    private void showError(String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        Window owner = resolveAlertOwner();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        dialog.setTitle(I18n.get("error.title"));
        dialog.setDialogTitle(I18n.get("error.title"));
        dialog.setDescription(message);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        dialog.showAndWait();
    }

    private void showSuccess(String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        Window owner = resolveAlertOwner();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        dialog.setTitle(I18n.get("success.title"));
        dialog.setDialogTitle(I18n.get("success.title"));
        dialog.setDescription(message);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        dialog.showAndWait();
    }
}

