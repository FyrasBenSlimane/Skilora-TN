package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLAvatar;
import com.skilora.framework.components.TLTextarea;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.service.usermanagement.ProfileService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ApplicationDetailsController - Displays full details of a job application for employers
 */
public class ApplicationDetailsController {

    @FXML private TLAvatar candidateAvatar;
    @FXML private Label candidateNameLabel;
    @FXML private Label aiScoresLabel;
    @FXML private TLTextarea coverLetterArea;
    @FXML private TLButton viewCvBtn;
    @FXML private Label cvFileNameLabel;
    @FXML private TLButton closeBtn;

    private Application application;
    private Profile candidateProfile;
    private Stage dialogStage;

    @FXML
    public void initialize() {
        // Setup close button
        if (closeBtn != null) {
            closeBtn.setOnAction(e -> {
                if (dialogStage != null) {
                    dialogStage.close();
                } else if (closeBtn.getScene() != null && closeBtn.getScene().getWindow() != null) {
                    ((Stage) closeBtn.getScene().getWindow()).close();
                }
            });
        }

        // Setup CV view button
        if (viewCvBtn != null) {
            viewCvBtn.setOnAction(e -> handleViewCv());
        }
        
        // Setup cover letter textarea
        if (coverLetterArea != null) {
            coverLetterArea.setEditable(false);
            coverLetterArea.setTextAreaHeight(200);
            coverLetterArea.setTextAreaRowCount(8);
        }
        
        // Setup avatar size
        if (candidateAvatar != null) {
            candidateAvatar.setSize(com.skilora.framework.components.TLAvatar.Size.LG);
        }
    }

    public void setup(Application app, Stage stage) {
        this.application = app;
        this.dialogStage = stage;
        
        // Load candidate profile
        loadCandidateProfile();
        
        // Populate UI
        populateDetails();
    }

    private void loadCandidateProfile() {
        try {
            ProfileService profileService = ProfileService.getInstance();
            candidateProfile = profileService.findProfileById(application.getCandidateProfileId());
        } catch (Exception e) {
            // Profile not found or error loading
            candidateProfile = null;
        }
    }

    private void populateDetails() {
        // Candidate name and photo
        String fullName = "Candidat inconnu";
        if (candidateProfile != null) {
            fullName = candidateProfile.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = application.getCandidateName() != null ? application.getCandidateName() : "Candidat inconnu";
            }
            
            // Load profile photo
            if (candidateAvatar != null) {
                String initials = getInitialsFromName(fullName);
                candidateAvatar.setFallback(initials);
                
                if (candidateProfile.getPhotoUrl() != null && !candidateProfile.getPhotoUrl().trim().isEmpty()) {
                    try {
                        String photoUrl = candidateProfile.getPhotoUrl();
                        // Handle both relative and absolute paths
                        File photoFile = new File(photoUrl);
                        if (photoFile.exists()) {
                            Image img = new Image(photoFile.toURI().toString(), 96, 96, true, true, true);
                            if (!img.isError()) {
                                candidateAvatar.setImage(img);
                            } else {
                                candidateAvatar.setImage(null);
                            }
                        } else {
                            // Try as URL
                            Image img = new Image(photoUrl, 96, 96, true, true, true);
                            if (!img.isError()) {
                                candidateAvatar.setImage(img);
                            } else {
                                candidateAvatar.setImage(null);
                            }
                        }
                    } catch (Exception e) {
                        candidateAvatar.setImage(null);
                    }
                } else {
                    candidateAvatar.setImage(null);
                }
            }
        } else {
            fullName = application.getCandidateName() != null ? application.getCandidateName() : "Candidat inconnu";
            if (candidateAvatar != null) {
                String initials = getInitialsFromName(fullName);
                candidateAvatar.setFallback(initials);
                candidateAvatar.setSize(com.skilora.framework.components.TLAvatar.Size.LG);
                candidateAvatar.setImage(null);
            }
        }
        candidateNameLabel.setText(fullName);
        if (aiScoresLabel != null) {
            String match = application.getMatchPercentage() > 0 ? application.getMatchPercentage() + "%" : "-";
            String score = application.getCandidateScore() > 0 ? String.valueOf(application.getCandidateScore()) : "-";
            aiScoresLabel.setText("AI Match: " + match + " | Score: " + score);
        }
        
        // Cover letter
        if (coverLetterArea != null) {
            String coverLetter = application.getCoverLetter();
            if (coverLetter != null && !coverLetter.trim().isEmpty()) {
                coverLetterArea.setText(coverLetter);
                coverLetterArea.getStyleClass().remove("text-muted");
            } else {
                coverLetterArea.setText("Aucune lettre de motivation fournie.");
                // Add a specific style class for empty state instead of text-muted on the textarea
                coverLetterArea.getStyleClass().add("cover-letter-empty");
            }
        }

        // CV - Priority: custom CV from application, then profile CV
        String cvUrl = application.getCustomCvUrl();
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            // Try to get CV from profile
            if (candidateProfile != null && candidateProfile.getCvUrl() != null && !candidateProfile.getCvUrl().trim().isEmpty()) {
                cvUrl = candidateProfile.getCvUrl();
            }
        }
        
        if (cvUrl != null && !cvUrl.trim().isEmpty()) {
            Path cvPath = Paths.get(cvUrl);
            String fileName = cvPath.getFileName().toString();
            cvFileNameLabel.setText("Fichier: " + fileName);
            viewCvBtn.setDisable(false);
        } else {
            cvFileNameLabel.setText("Aucun CV disponible");
            viewCvBtn.setDisable(true);
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

    private void handleViewCv() {
        String cvUrl = application.getCustomCvUrl();
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            // Try profile CV
            if (candidateProfile != null && candidateProfile.getCvUrl() != null) {
                cvUrl = candidateProfile.getCvUrl();
            }
        }

        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            com.skilora.utils.DialogUtils.showError("Erreur", "Aucun CV disponible pour ce candidat.");
            return;
        }

        try {
            Path cvPath = Paths.get(cvUrl);
            File cvFile = cvPath.toFile();

            if (!cvFile.exists()) {
                com.skilora.utils.DialogUtils.showError("Erreur", "Le fichier CV n'existe plus sur le serveur.");
                return;
            }

            // Open file with system default application
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(cvFile);
            } else {
                // Fallback: show file chooser to save
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Enregistrer le CV");
                fileChooser.setInitialFileName(cvFile.getName());
                File saveFile = fileChooser.showSaveDialog(dialogStage);
                
                if (saveFile != null) {
                    Files.copy(cvFile.toPath(), saveFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    com.skilora.utils.DialogUtils.showInfo("Succès", "CV téléchargé avec succès.");
                }
            }
        } catch (IOException e) {
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible d'ouvrir le CV: " + e.getMessage());
        } catch (Exception e) {
            com.skilora.utils.DialogUtils.showError("Erreur", "Erreur lors de l'ouverture du CV.");
        }
    }
    
}

