package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.enums.TrainingCategory;
import com.skilora.model.enums.TrainingLevel;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import com.skilora.utils.DialogUtils;

/**
 * TrainingFormController
 * 
 * Handles the training form dialog for create/edit operations with real-time validation.
 */
public class TrainingFormController {

    @FXML
    private TLTextField titleField;
    @FXML
    private TLTextarea descriptionField;
    @FXML
    private TLTextField costField;
    @FXML
    private TLTextField durationField;
    @FXML
    private TLTextField lessonCountField;
    @FXML
    private TLSelect<TrainingLevel> levelSelect;
    @FXML
    private TLSelect<TrainingCategory> categorySelect;
    
    // Error labels
    @FXML
    private Label titleError;
    @FXML
    private Label descriptionError;
    @FXML
    private Label descriptionCounter;
    @FXML
    private Label costError;
    @FXML
    private Label durationError;
    @FXML
    private Label lessonCountError;
    @FXML
    private Label levelError;
    @FXML
    private Label categoryError;

    // Step navigation UI
    @FXML
    private Label stepIndicator;
    @FXML
    private StackPane stepContainer;
    @FXML
    private VBox step1Content;
    @FXML
    private VBox step2Content;
    @FXML
    private TLButton nextStepBtn;
    @FXML
    private TLButton backStepBtn;
    @FXML
    private TLButton finalSaveBtn;

    // Director signature capture
    @FXML
    private Canvas signatureCanvas;
    @FXML
    private TLButton clearSignatureBtn;
    @FXML
    private TLButton saveSignatureBtn;

    private GraphicsContext signatureGc;
    private double lastX;
    private double lastY;
    private String directorSignatureBase64; // PNG base64 without data URL prefix

    private Training existingTraining;
    private Runnable onValidationChange;
    private Runnable onFinalSave; // Callback for final save button
    
    private int currentStep = 1; // Track current step (1 or 2)
    
    // Validation constants
    private static final int TITLE_MIN_LENGTH = 3;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MIN_LENGTH = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    @FXML
    public void initialize() {
        // Populate level dropdown
        if (levelSelect != null) {
            levelSelect.getItems().addAll(TrainingLevel.values());
            levelSelect.setValue(TrainingLevel.BEGINNER);
        }
        
        // Populate category dropdown
        if (categorySelect != null) {
            categorySelect.getItems().addAll(TrainingCategory.values());
            categorySelect.setValue(TrainingCategory.DEVELOPMENT);
        }
        
        // Set textarea properties (6 rows, 150px minimum height)
        if (descriptionField != null) {
            descriptionField.setPromptText("Décrivez la formation en détail (minimum 20 caractères)...");
            if (descriptionField.getControl() != null) {
                descriptionField.getControl().setPrefRowCount(6);
                descriptionField.getControl().setMinHeight(150);
                descriptionField.getControl().setPrefHeight(150);
                descriptionField.getControl().setWrapText(true);
                // Ensure it's scrollable for long text
                descriptionField.getControl().setScrollLeft(0);
                descriptionField.getControl().setScrollTop(0);
            }
            // Set height on the TextArea component itself
            descriptionField.setTextAreaMinHeight(150);
            descriptionField.setTextAreaPrefHeight(150);
        }
        
        // Setup real-time validation listeners
        setupValidationListeners();
        
        // Initialize character counter
        updateDescriptionCounter();

        // Setup signature canvas if present
        setupSignatureCanvas();
        
        // Setup step navigation
        setupStepNavigation();
    }
    
    public void setOnValidationChange(Runnable callback) {
        this.onValidationChange = callback;
    }
    
    /**
     * Set callback for final save button (called from MainView)
     */
    public void setOnFinalSave(Runnable callback) {
        this.onFinalSave = callback;
    }
    
    /**
     * Setup step navigation buttons and handlers
     */
    private void setupStepNavigation() {
        // Initialize to step 1
        showStep(1);
        
        // Set button variants
        if (nextStepBtn != null) {
            nextStepBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            // Next button: validate and move to step 2
            nextStepBtn.setOnAction(e -> {
                // Validate all required fields before moving to step 2
                if (validateStep1()) {
                    showStep(2);
                } else {
                    // Show error message
                    com.skilora.utils.DialogUtils.showError("Erreur de validation", 
                        "Veuillez corriger les erreurs dans le formulaire avant de continuer.");
                }
            });
        }
        
        // Back button: return to step 1
        if (backStepBtn != null) {
            backStepBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
            backStepBtn.setOnAction(e -> showStep(1));
        }
        
        // Final save button: save signature then trigger form submission
        if (finalSaveBtn != null) {
            finalSaveBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            finalSaveBtn.setOnAction(e -> {
                // Auto-save signature from canvas if not already saved
                captureSignatureFromCanvas();
                // Trigger the final save callback (handled by MainView)
                if (onFinalSave != null) {
                    onFinalSave.run();
                }
            });
        }
    }
    
    /**
     * Show the specified step (1 or 2)
     */
    private void showStep(int step) {
        currentStep = step;
        
        if (stepIndicator != null) {
            stepIndicator.setText("Étape " + step + "/2");
        }
        
        if (step1Content != null && step2Content != null) {
            if (step == 1) {
                step1Content.setVisible(true);
                step1Content.setManaged(true);
                step2Content.setVisible(false);
                step2Content.setManaged(false);
            } else {
                step1Content.setVisible(false);
                step1Content.setManaged(false);
                step2Content.setVisible(true);
                step2Content.setManaged(true);
                
                // When moving to step 2, load signature if it exists
                if (directorSignatureBase64 != null && !directorSignatureBase64.isBlank()) {
                    loadSignatureToCanvas(directorSignatureBase64);
                }
            }
        }
    }
    
    /**
     * Validate all fields in step 1 (all required form fields)
     */
    private boolean validateStep1() {
        validateTitle();
        validateDescription();
        validateCost();
        validateDuration();
        validateLessonCount();
        validateLevel();
        validateCategory();
        
        return isFormValid();
    }
    
    /**
     * Capture signature from canvas (auto-save when final save is clicked)
     */
    private void captureSignatureFromCanvas() {
        if (signatureCanvas == null) return;
        
        try {
            WritableImage snapshot = signatureCanvas.snapshot(null, null);
            java.awt.image.BufferedImage img = SwingFXUtils.fromFXImage(snapshot, null);
            
            if (img != null) {
                // Quick check: sample pixels instead of checking every pixel
                boolean hasContent = false;
                int sampleStep = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 20);
                
                for (int y = 0; y < img.getHeight() && !hasContent; y += sampleStep) {
                    for (int x = 0; x < img.getWidth(); x += sampleStep) {
                        int rgb = img.getRGB(x, y);
                        if ((rgb & 0xFFFFFF) != 0xFFFFFF) {
                            hasContent = true;
                            break;
                        }
                    }
                }
                
                if (hasContent) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "png", baos);
                    baos.flush();
                    byte[] pngBytes = baos.toByteArray();
                    baos.close();
                    directorSignatureBase64 = Base64.getEncoder().encodeToString(pngBytes);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error capturing signature from canvas: " + ex.getMessage());
        }
    }
    
    private void setupValidationListeners() {
        // Title validation
        if (titleField != null && titleField.getControl() != null) {
            titleField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                validateTitle();
                notifyValidationChange();
            });
            titleField.getControl().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) { // On focus lost
                    validateTitle();
                }
            });
        }
        
        // Description validation with character counter
        if (descriptionField != null && descriptionField.getControl() != null) {
            descriptionField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                updateDescriptionCounter();
                validateDescription();
                notifyValidationChange();
            });
            descriptionField.getControl().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validateDescription();
                }
            });
        }
        
        // Cost validation
        if (costField != null && costField.getControl() != null) {
            costField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                validateCost();
                notifyValidationChange();
            });
            costField.getControl().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validateCost();
                }
            });
        }
        
        // Duration validation
        if (durationField != null && durationField.getControl() != null) {
            durationField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                validateDuration();
                notifyValidationChange();
            });
            durationField.getControl().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validateDuration();
                }
            });
        }
        
        // Lesson count validation
        if (lessonCountField != null && lessonCountField.getControl() != null) {
            lessonCountField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                validateLessonCount();
                notifyValidationChange();
            });
            lessonCountField.getControl().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validateLessonCount();
                }
            });
        }
        
        // Level validation
        if (levelSelect != null) {
            levelSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
                validateLevel();
                notifyValidationChange();
            });
        }
        
        // Category validation
        if (categorySelect != null) {
            categorySelect.valueProperty().addListener((obs, oldVal, newVal) -> {
                validateCategory();
                notifyValidationChange();
            });
        }
    }
    
    private void updateDescriptionCounter() {
        if (descriptionCounter == null || descriptionField == null) return;
        
        String text = descriptionField.getText() != null ? descriptionField.getText() : "";
        int length = text.length();
        int remaining = DESCRIPTION_MAX_LENGTH - length;
        
        if (remaining < 0) {
            descriptionCounter.setText(String.format("%d / %d (dépassement)", length, DESCRIPTION_MAX_LENGTH));
            descriptionCounter.getStyleClass().removeAll("text-muted", "text-destructive");
            if (!descriptionCounter.getStyleClass().contains("text-destructive")) {
                descriptionCounter.getStyleClass().add("text-destructive");
            }
        } else if (remaining < 50) {
            descriptionCounter.setText(String.format("%d / %d", length, DESCRIPTION_MAX_LENGTH));
            descriptionCounter.getStyleClass().removeAll("text-muted", "text-destructive");
            if (!descriptionCounter.getStyleClass().contains("text-warning")) {
                descriptionCounter.getStyleClass().add("text-warning");
            }
        } else {
            descriptionCounter.setText(String.format("%d / %d", length, DESCRIPTION_MAX_LENGTH));
            descriptionCounter.getStyleClass().removeAll("text-warning", "text-destructive");
            if (!descriptionCounter.getStyleClass().contains("text-muted")) {
                descriptionCounter.getStyleClass().add("text-muted");
            }
        }
    }
    
    private void notifyValidationChange() {
        if (onValidationChange != null) {
            onValidationChange.run();
        }
    }
    
    private void validateTitle() {
        if (titleField == null || titleError == null) return;
        
        String text = titleField.getText() != null ? titleField.getText().trim() : "";
        
        if (text.isEmpty()) {
            showError(titleField, titleError, "Le titre est requis");
        } else if (text.length() < TITLE_MIN_LENGTH) {
            showError(titleField, titleError, String.format("Le titre doit contenir au moins %d caractères", TITLE_MIN_LENGTH));
        } else if (text.length() > TITLE_MAX_LENGTH) {
            showError(titleField, titleError, String.format("Le titre ne peut pas dépasser %d caractères", TITLE_MAX_LENGTH));
        } else {
            clearError(titleField, titleError);
        }
    }
    
    private void validateDescription() {
        if (descriptionField == null || descriptionError == null) return;
        
        String text = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        
        if (text.isEmpty()) {
            showError(descriptionField, descriptionError, "La description est requise");
        } else if (text.length() < DESCRIPTION_MIN_LENGTH) {
            showError(descriptionField, descriptionError, String.format("La description doit contenir au moins %d caractères", DESCRIPTION_MIN_LENGTH));
        } else if (text.length() > DESCRIPTION_MAX_LENGTH) {
            showError(descriptionField, descriptionError, String.format("La description ne peut pas dépasser %d caractères", DESCRIPTION_MAX_LENGTH));
        } else {
            clearError(descriptionField, descriptionError);
        }
    }
    
    private void validateCost() {
        if (costField == null || costError == null) return;
        
        String text = costField.getText() != null ? costField.getText().trim() : "";
        
        // Cost is optional - only validate if provided
        if (text.isEmpty()) {
            clearError(costField, costError);
        } else {
            try {
                double cost = Double.parseDouble(text);
                if (cost < 0) {
                    showError(costField, costError, "Le coût doit être un nombre positif");
                } else {
                    clearError(costField, costError);
                }
            } catch (NumberFormatException e) {
                showError(costField, costError, "Veuillez entrer un nombre valide");
            }
        }
    }
    
    private void validateDuration() {
        if (durationField == null || durationError == null) return;
        
        String text = durationField.getText() != null ? durationField.getText().trim() : "";
        
        if (text.isEmpty()) {
            showError(durationField, durationError, "La durée est requise");
        } else {
            try {
                int duration = Integer.parseInt(text);
                if (duration < 1) {
                    showError(durationField, durationError, "La durée doit être d'au moins 1 heure");
                } else {
                    clearError(durationField, durationError);
                }
            } catch (NumberFormatException e) {
                showError(durationField, durationError, "Veuillez entrer un nombre entier valide");
            }
        }
    }
    
    private void validateLessonCount() {
        if (lessonCountField == null || lessonCountError == null) return;
        
        String text = lessonCountField.getText() != null ? lessonCountField.getText().trim() : "";
        
        if (text.isEmpty()) {
            showError(lessonCountField, lessonCountError, "Le nombre de leçons est requis");
        } else {
            try {
                int lessonCount = Integer.parseInt(text);
                if (lessonCount < 1) {
                    showError(lessonCountField, lessonCountError, "Le nombre de leçons doit être d'au moins 1");
                } else {
                    clearError(lessonCountField, lessonCountError);
                }
            } catch (NumberFormatException e) {
                showError(lessonCountField, lessonCountError, "Veuillez entrer un nombre entier valide");
            }
        }
    }
    
    private void validateLevel() {
        if (levelSelect == null || levelError == null) return;
        
        if (levelSelect.getValue() == null) {
            showError(levelSelect, levelError, "Le niveau est requis");
        } else {
            clearError(levelSelect, levelError);
        }
    }
    
    private void validateCategory() {
        if (categorySelect == null || categoryError == null) return;
        
        if (categorySelect.getValue() == null) {
            showError(categorySelect, categoryError, "La catégorie est requise");
        } else {
            clearError(categorySelect, categoryError);
        }
    }
    
    private void showError(TLTextField field, Label errorLabel, String message) {
        if (field != null && field.getControl() != null) {
            field.getControl().getStyleClass().removeAll("input-error");
            if (!field.getControl().getStyleClass().contains("input-error")) {
                field.getControl().getStyleClass().add("input-error");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    
    private void showError(TLTextarea field, Label errorLabel, String message) {
        if (field != null) {
            field.setError(true);
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    
    private void showError(TLSelect<?> field, Label errorLabel, String message) {
        if (field != null) {
            field.getStyleClass().removeAll("select-error");
            if (!field.getStyleClass().contains("select-error")) {
                field.getStyleClass().add("select-error");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    
    private void clearError(TLTextField field, Label errorLabel) {
        if (field != null && field.getControl() != null) {
            field.getControl().getStyleClass().remove("input-error");
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
    
    private void clearError(TLTextarea field, Label errorLabel) {
        if (field != null) {
            field.setError(false);
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
    
    private void clearError(TLSelect<?> field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("select-error");
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    public void setTraining(Training training) {
        this.existingTraining = training;
        // Always start on step 1
        showStep(1);
        
        if (training != null) {
            // Edit mode - populate with existing data
            if (titleField != null) titleField.setText(training.getTitle() != null ? training.getTitle() : "");
            if (descriptionField != null) descriptionField.setText(training.getDescription() != null ? training.getDescription() : "");
            if (costField != null) costField.setText(training.getCost() != null ? String.valueOf(training.getCost()) : "");
            if (durationField != null) durationField.setText(String.valueOf(training.getDuration()));
            if (lessonCountField != null) lessonCountField.setText(String.valueOf(training.getLessonCount()));
            if (levelSelect != null) levelSelect.setValue(training.getLevel());
            if (categorySelect != null) categorySelect.setValue(training.getCategory());
            this.directorSignatureBase64 = training.getDirectorSignature();
            // Signature will be loaded when user navigates to step 2
        } else {
            // Create mode - reset form
            if (titleField != null) titleField.setText("");
            if (descriptionField != null) descriptionField.setText("");
            if (costField != null) costField.setText(""); // Empty for optional field
            if (durationField != null) durationField.setText("");
            if (lessonCountField != null) lessonCountField.setText("");
            if (levelSelect != null) levelSelect.setValue(TrainingLevel.BEGINNER);
            if (categorySelect != null) categorySelect.setValue(TrainingCategory.DEVELOPMENT);
            this.directorSignatureBase64 = null;
            // Clear canvas
            if (signatureCanvas != null) {
                clearSignatureCanvas();
            }
        }
    }

    public Training getTraining() {
        Training training = existingTraining != null ? existingTraining : new Training();
        
        if (titleField != null) {
            training.setTitle(titleField.getText() != null ? titleField.getText().trim() : "");
        }
        
        if (descriptionField != null) {
            training.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : "");
        }
        
        if (costField != null) {
            try {
                String costText = costField.getText();
                if (costText != null && !costText.trim().isEmpty()) {
                    training.setCost(Double.parseDouble(costText.trim()));
                } else {
                    training.setCost(null); // Null means no price specified
                }
            } catch (NumberFormatException e) {
                training.setCost(null); // Invalid input = null (optional field)
            }
        }
        
        if (durationField != null) {
            try {
                String durationText = durationField.getText();
                if (durationText != null && !durationText.trim().isEmpty()) {
                    training.setDuration(Integer.parseInt(durationText.trim()));
                } else {
                    training.setDuration(0);
                }
            } catch (NumberFormatException e) {
                training.setDuration(0);
            }
        }
        
        if (lessonCountField != null) {
            try {
                String lessonCountText = lessonCountField.getText();
                if (lessonCountText != null && !lessonCountText.trim().isEmpty()) {
                    training.setLessonCount(Integer.parseInt(lessonCountText.trim()));
                } else {
                    training.setLessonCount(0);
                }
            } catch (NumberFormatException e) {
                training.setLessonCount(0);
            }
        }
        
        if (levelSelect != null) {
            training.setLevel(levelSelect.getValue());
        }
        
        if (categorySelect != null) {
            training.setCategory(categorySelect.getValue());
        }

        // Always capture signature from canvas when saving the form
        // This ensures the signature is saved even if user didn't click "Enregistrer la signature"
        // Wrap in try-catch to ensure it never blocks form submission
        try {
            if (signatureCanvas != null && (directorSignatureBase64 == null || directorSignatureBase64.isBlank())) {
                WritableImage snapshot = signatureCanvas.snapshot(null, null);
                java.awt.image.BufferedImage img = SwingFXUtils.fromFXImage(snapshot, null);
                
                if (img != null) {
                    // Quick check: sample pixels instead of checking every pixel (performance optimization)
                    boolean hasContent = false;
                    int sampleStep = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 20); // Sample every Nth pixel
                    
                    for (int y = 0; y < img.getHeight() && !hasContent; y += sampleStep) {
                        for (int x = 0; x < img.getWidth(); x += sampleStep) {
                            int rgb = img.getRGB(x, y);
                            // Check if pixel is not white (0xFFFFFF)
                            if ((rgb & 0xFFFFFF) != 0xFFFFFF) {
                                hasContent = true;
                                break;
                            }
                        }
                    }
                    
                    // If canvas has content, save it
                    if (hasContent) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "png", baos);
                        baos.flush();
                        byte[] pngBytes = baos.toByteArray();
                        baos.close();
                        directorSignatureBase64 = Base64.getEncoder().encodeToString(pngBytes);
                    }
                }
            }
        } catch (Exception ex) {
            // If signature capture fails for any reason, log but don't block form submission
            // Keep existing signature or null
            System.err.println("Error capturing signature from canvas (non-blocking): " + ex.getMessage());
            ex.printStackTrace();
        }

        // Persist director signature (may be null)
        training.setDirectorSignature(directorSignatureBase64);
        
        return training;
    }

    // ─── Signature Canvas Logic ──────────────────────────────────────────────

    private void setupSignatureCanvas() {
        if (signatureCanvas == null) {
            return;
        }

        signatureGc = signatureCanvas.getGraphicsContext2D();
        clearSignatureCanvas();

        signatureCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastX = e.getX();
            lastY = e.getY();
        });

        signatureCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (signatureGc == null) return;
            signatureGc.setStroke(Color.BLACK);
            signatureGc.setLineWidth(2.0);
            signatureGc.strokeLine(lastX, lastY, e.getX(), e.getY());
            lastX = e.getX();
            lastY = e.getY();
        });

        if (clearSignatureBtn != null) {
            clearSignatureBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
            clearSignatureBtn.setOnAction(e -> {
                clearSignatureCanvas();
                directorSignatureBase64 = null;
            });
        }

        if (saveSignatureBtn != null) {
            saveSignatureBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            saveSignatureBtn.setOnAction(e -> saveSignatureFromCanvas());
        }
    }

    private void clearSignatureCanvas() {
        if (signatureGc != null) {
            signatureGc.setFill(Color.WHITE);
            signatureGc.fillRect(0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());
        }
    }

    private void saveSignatureFromCanvas() {
        if (signatureCanvas == null) return;

        try {
            WritableImage snapshot = signatureCanvas.snapshot(null, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", baos);
            baos.flush();
            byte[] pngBytes = baos.toByteArray();
            baos.close();

            directorSignatureBase64 = Base64.getEncoder().encodeToString(pngBytes);
            
            // Show success feedback
            DialogUtils.showSuccess("Signature enregistrée", 
                "La signature du Directeur a été enregistrée avec succès.");
        } catch (IOException ex) {
            DialogUtils.showError("Erreur", 
                "Impossible d'enregistrer la signature: " + ex.getMessage());
        }
    }
    
    /**
     * Load signature image onto canvas from base64 string
     */
    private void loadSignatureToCanvas(String base64Signature) {
        if (signatureCanvas == null || base64Signature == null || base64Signature.isBlank()) {
            return;
        }
        
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Signature);
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            
            if (signatureGc != null) {
                // Clear canvas first
                clearSignatureCanvas();
                // Draw the loaded image
                signatureGc.drawImage(image, 0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());
            }
        } catch (Exception ex) {
            System.err.println("Error loading signature to canvas: " + ex.getMessage());
        }
    }

    public boolean validate() {
        // Validate all fields
        validateTitle();
        validateDescription();
        validateCost();
        validateDuration();
        validateLessonCount();
        validateLevel();
        validateCategory();
        
        // Check if all validations pass
        return isFormValid();
    }
    
    public boolean isFormValid() {
        // Title validation
        String title = titleField != null ? (titleField.getText() != null ? titleField.getText().trim() : "") : "";
        if (title.isEmpty() || title.length() < TITLE_MIN_LENGTH || title.length() > TITLE_MAX_LENGTH) {
            return false;
        }
        
        // Description validation
        String description = descriptionField != null ? (descriptionField.getText() != null ? descriptionField.getText().trim() : "") : "";
        if (description.isEmpty() || description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            return false;
        }
        
        // Cost validation (optional - only validate if provided)
        if (costField != null) {
            String costText = costField.getText() != null ? costField.getText().trim() : "";
            if (!costText.isEmpty()) {
                try {
                    double cost = Double.parseDouble(costText);
                    if (cost < 0) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        
        // Duration validation
        if (durationField != null) {
            String durationText = durationField.getText() != null ? durationField.getText().trim() : "";
            if (durationText.isEmpty()) {
                return false;
            }
            try {
                int duration = Integer.parseInt(durationText);
                if (duration < 1) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Lesson count validation
        if (lessonCountField != null) {
            String lessonCountText = lessonCountField.getText() != null ? lessonCountField.getText().trim() : "";
            if (lessonCountText.isEmpty()) {
                return false;
            }
            try {
                int lessonCount = Integer.parseInt(lessonCountText);
                if (lessonCount < 1) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // Level validation
        if (levelSelect == null || levelSelect.getValue() == null) {
            return false;
        }
        
        // Category validation
        if (categorySelect == null || categorySelect.getValue() == null) {
            return false;
        }
        
        return true;
    }
}
