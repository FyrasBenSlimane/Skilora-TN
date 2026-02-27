package com.skilora.controller.recruitment;

import com.skilora.framework.components.*;
import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.JobStatus;
import com.skilora.service.recruitment.JobService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.skilora.utils.I18n;
import com.skilora.utils.Validators;

/**
 * PostJobController - 3-step wizard for posting job opportunities
 */
public class PostJobController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PostJobController.class);

    @FXML
    private Label stepLabel;
    @FXML
    private TLButton saveBtn;
    @FXML
    private VBox step1Indicator;
    @FXML
    private VBox step2Indicator;
    @FXML
    private VBox step3Indicator;
    @FXML
    private ProgressBar progress1;
    @FXML
    private ProgressBar progress2;
    @FXML
    private ProgressBar progress3;
    @FXML
    private StackPane contentStack;
    @FXML
    private TLButton cancelBtn;
    @FXML
    private TLButton prevBtn;
    @FXML
    private TLButton nextBtn;
    @FXML
    private TLButton publishBtn;

    private int currentStep = 1;
    private VBox step1View;
    private VBox step2View;
    private VBox step3View;

    // Form fields (Step 1)
    private TLTextField jobTitleField;
    private TLTextarea jobDescriptionField;
    private TLSelect<String> jobTypeSelect;
    private TLTextField locationField;
    private TLTextField salaryField;

    // Form fields (Step 2)
    private VBox skillsList;
    private TLTextField skillInput;

    private Runnable onCancel;
    private User currentUser;
    private volatile int companyId = -1;
    private JobOffer offerToEdit; // For edit mode

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createStepViews();
        showStep(1);
    }

    /**
     * Set the current employer user and resolve their company.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Resolve company in background
        if (user != null) {
            Thread t = new Thread(() -> {
                this.companyId = JobService.getInstance().getOrCreateEmployerCompanyId(user.getId(),
                        user.getFullName());
            }, "CompanyResolverThread");
            t.setDaemon(true);
            t.start();
        }
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    /**
     * Load an existing job offer for editing.
     * Populates all form fields with the offer's data.
     */
    public void loadJobOfferForEdit(JobOffer offer) {
        if (offer == null) {
            return;
        }

        this.offerToEdit = offer;

        // Update button text to indicate edit mode
        if (publishBtn != null) {
            publishBtn.setText(I18n.get("postjob.update", "Mettre à jour"));
        }

        // Populate Step 1 fields
        if (offer.getTitle() != null) {
            jobTitleField.setText(offer.getTitle());
        }
        if (offer.getDescription() != null) {
            jobDescriptionField.setText(offer.getDescription());
        }
        if (offer.getLocation() != null) {
            locationField.setText(offer.getLocation());
        }

        // Set work type
        if (offer.getWorkType() != null) {
            String workType = offer.getWorkType().toUpperCase();
            // Convert database format (FULL_TIME, PART_TIME, etc.) to display format
            String displayType;
            if (workType.equals("FULL_TIME") || workType.equals("FULL-TIME")) {
                displayType = "Full-time";
            } else if (workType.equals("PART_TIME") || workType.equals("PART-TIME")) {
                displayType = "Part-time";
            } else if (workType.equals("CONTRACT")) {
                displayType = "Contract";
            } else if (workType.equals("FREELANCE")) {
                displayType = "Freelance";
            } else if (workType.equals("INTERNSHIP")) {
                displayType = "Internship";
            } else {
                // Try to format it nicely
                displayType = workType.replace("_", "-");
                // Capitalize first letter of each word
                String[] words = displayType.toLowerCase().split("-");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < words.length; i++) {
                    if (i > 0)
                        formatted.append("-");
                    if (!words[i].isEmpty()) {
                        formatted.append(words[i].substring(0, 1).toUpperCase());
                        if (words[i].length() > 1) {
                            formatted.append(words[i].substring(1));
                        }
                    }
                }
                displayType = formatted.toString();
            }
            jobTypeSelect.setValue(displayType);
        }

        // Set salary range
        if (offer.getSalaryMin() > 0 || offer.getSalaryMax() > 0) {
            if (offer.getSalaryMin() > 0 && offer.getSalaryMax() > 0) {
                salaryField.setText(String.format("%.0f-%.0f", offer.getSalaryMin(), offer.getSalaryMax()));
            } else if (offer.getSalaryMin() > 0) {
                salaryField.setText(String.format("%.0f", offer.getSalaryMin()));
            } else if (offer.getSalaryMax() > 0) {
                salaryField.setText(String.format("%.0f", offer.getSalaryMax()));
            }
        }

        // Populate Step 2 - Skills
        skillsList.getChildren().clear();
        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            for (String skill : offer.getRequiredSkills()) {
                addSkillBadge(skill);
            }
        }

        // Set company ID
        if (offer.getEmployerId() > 0) {
            this.companyId = offer.getEmployerId();
        }
    }

    private void createStepViews() {
        // Step 1: Basic Info
        step1View = new VBox(16);
        step1View.setPadding(new Insets(24));

        jobTitleField = new TLTextField();
        jobTitleField.setPromptText("ex: Senior Java Developer");

        jobDescriptionField = new TLTextarea();
        jobDescriptionField.setPromptText(I18n.get("postjob.description.placeholder"));
        jobDescriptionField.setPrefHeight(200);

        jobTypeSelect = new TLSelect<>();
        jobTypeSelect.getItems().addAll("Full-time", "Part-time", "Contract", "Freelance", "Internship");
        jobTypeSelect.setPromptText(I18n.get("postjob.contract_type.prompt"));

        locationField = new TLTextField();
        locationField.setPromptText(I18n.get("postjob.location.placeholder"));

        salaryField = new TLTextField();
        salaryField.setPromptText(I18n.get("postjob.salary.placeholder"));

        step1View.getChildren().addAll(
                createFormField(I18n.get("postjob.job_title"), jobTitleField),
                createFormField(I18n.get("postjob.description"), jobDescriptionField),
                createFormField(I18n.get("postjob.contract_type.label"), jobTypeSelect),
                createFormField(I18n.get("postjob.location"), locationField),
                createFormField(I18n.get("postjob.salary"), salaryField));

        // Step 2: Skills
        step2View = new VBox(16);
        step2View.setPadding(new Insets(24));

        skillsList = new VBox(8);
        skillInput = new TLTextField();
        skillInput.setPromptText(I18n.get("postjob.add_skill.placeholder"));

        TLButton addSkillBtn = new TLButton(I18n.get("postjob.add_skill.btn"));
        addSkillBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
        addSkillBtn.setOnAction(e -> addSkill());

        HBox skillInputRow = new HBox(8);
        HBox.setHgrow(skillInput, Priority.ALWAYS);
        skillInputRow.getChildren().addAll(skillInput, addSkillBtn);

        Label skillsTitle = new Label(I18n.get("postjob.skills_required"));
        skillsTitle.getStyleClass().add("h4");

        Label skillsSubtitle = new Label(I18n.get("postjob.skills_instruction"));
        skillsSubtitle.getStyleClass().add("text-muted");

        step2View.getChildren().addAll(
                skillsTitle,
                skillsSubtitle,
                skillInputRow,
                skillsList);

        // Don't add default skills - let user add their own or load from existing offer

        // Step 3: Preview
        step3View = new VBox(16);
        step3View.setPadding(new Insets(24));

        Label previewTitle = new Label(I18n.get("postjob.preview"));
        previewTitle.getStyleClass().add("h3");

        step3View.getChildren().add(previewTitle);
    }

    private VBox createFormField(String labelText, javafx.scene.Node field) {
        VBox fieldBox = new VBox(8);
        Label label = new Label(labelText);
        label.getStyleClass().add("label");
        fieldBox.getChildren().addAll(label, field);
        return fieldBox;
    }

    private void addSkill() {
        String skill = skillInput.getText().trim();
        if (!skill.isEmpty()) {
            addSkillBadge(skill);
            skillInput.setText("");
        }
    }

    private void addSkillBadge(String skill) {
        HBox skillRow = new HBox(8);
        skillRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TLBadge badge = new TLBadge(skill, TLBadge.Variant.SECONDARY);

        TLButton removeBtn = new TLButton("×");
        removeBtn.setVariant(TLButton.ButtonVariant.GHOST);
        removeBtn.setStyle("-fx-min-width: 24; -fx-min-height: 24; -fx-padding: 0;");
        removeBtn.setOnAction(e -> skillsList.getChildren().remove(skillRow));

        skillRow.getChildren().addAll(badge, removeBtn);
        skillsList.getChildren().add(skillRow);
    }

    private void showStep(int step) {
        currentStep = step;

        // Update step label
        stepLabel.setText(I18n.get("postjob.step_indicator", step, getStepTitle(step)));

        // Update progress bars
        progress1.setProgress(step >= 1 ? 1.0 : 0.0);
        progress2.setProgress(step >= 2 ? 1.0 : 0.0);
        progress3.setProgress(step >= 3 ? 1.0 : 0.0);

        // Update content
        contentStack.getChildren().clear();
        switch (step) {
            case 1 -> contentStack.getChildren().add(step1View);
            case 2 -> contentStack.getChildren().add(step2View);
            case 3 -> {
                updatePreview();
                contentStack.getChildren().add(step3View);
            }
        }

        // Update navigation buttons
        prevBtn.setVisible(step > 1);
        prevBtn.setManaged(step > 1);
        nextBtn.setVisible(step < 3);
        nextBtn.setManaged(step < 3);
        publishBtn.setVisible(step == 3);
        publishBtn.setManaged(step == 3);
    }

    private String getStepTitle(int step) {
        return switch (step) {
            case 1 -> I18n.get("postjob.step1");
            case 2 -> I18n.get("postjob.step2");
            case 3 -> I18n.get("postjob.step3");
            default -> "";
        };
    }

    private void updatePreview() {
        step3View.getChildren().clear();

        Label previewTitle = new Label(I18n.get("postjob.preview"));
        previewTitle.getStyleClass().add("h3");

        TLCard previewCard = new TLCard();
        VBox previewContent = new VBox(16);
        previewContent.setPadding(new Insets(24));

        Label title = new Label(
                jobTitleField.getText().isEmpty() ? I18n.get("postjob.no_title") : jobTitleField.getText());
        title.getStyleClass().add("h2");

        Label type = new Label(
                jobTypeSelect.getValue() != null ? jobTypeSelect.getValue() : I18n.get("postjob.no_type"));
        type.getStyleClass().add("text-muted");

        Label location = new Label("📍 "
                + (locationField.getText().isEmpty() ? I18n.get("postjob.no_location") : locationField.getText()));

        Label salary = new Label(
                "💰 " + (salaryField.getText().isEmpty() ? I18n.get("postjob.no_salary") : salaryField.getText()));

        Label descTitle = new Label(I18n.get("postjob.description").replace(" *", ""));
        descTitle.getStyleClass().add("h4");

        Label desc = new Label(jobDescriptionField.getText().isEmpty() ? I18n.get("postjob.no_description")
                : jobDescriptionField.getText());
        desc.setWrapText(true);

        previewContent.getChildren().addAll(title, type, location, salary, new Separator(), descTitle, desc);

        if (!skillsList.getChildren().isEmpty()) {
            Label skillsTitle = new Label(I18n.get("postjob.skills_required"));
            skillsTitle.getStyleClass().add("h4");
            previewContent.getChildren().addAll(new Separator(), skillsTitle, skillsList);
        }

        previewCard.getChildren().add(previewContent);

        step3View.getChildren().addAll(previewTitle, previewCard);
    }

    @FXML
    private void handleNext() {
        if (currentStep == 1) {
            if (!validateStep1())
                return;
        } else if (currentStep == 2) {
            if (!validateStep2())
                return;
        }

        if (currentStep < 3) {
            showStep(currentStep + 1);
        }
    }

    private boolean validateStep1() {
        String title = jobTitleField.getText().trim();
        String description = jobDescriptionField.getText() != null ? jobDescriptionField.getText().trim() : "";
        String location = locationField.getText() != null ? locationField.getText().trim() : "";
        String contractType = jobTypeSelect.getValue();

        boolean isValid = true;

        // Title
        if (!Validators.required(title)) {
            showFieldError(jobTitleField, I18n.get("postjob.title_required"));
            isValid = false;
        } else if (!Validators.minLength(title, 3)) {
            showFieldError(jobTitleField,
                    I18n.get("postjob.title_min_length", "Le titre doit contenir au moins 3 caractères"));
            isValid = false;
        } else {
            clearFieldError(jobTitleField);
        }

        // Description
        if (!Validators.required(description)) {
            showFieldError(jobDescriptionField,
                    I18n.get("postjob.description_required", "La description est obligatoire"));
            isValid = false;
        } else if (!Validators.minLength(description, 50)) {
            showFieldError(jobDescriptionField,
                    I18n.get("postjob.description_min_length", "La description doit contenir au moins 50 caractères"));
            isValid = false;
        } else {
            clearFieldError(jobDescriptionField);
        }

        // Location
        if (!Validators.required(location)) {
            showFieldError(locationField, I18n.get("postjob.location_required", "La localisation est obligatoire"));
            isValid = false;
        } else {
            clearFieldError(locationField);
        }

        // Contract Type
        if (!Validators.required(contractType)) {
            showFieldError(jobTypeSelect,
                    I18n.get("postjob.contract_type_required", "Le type de contrat est obligatoire"));
            isValid = false;
        } else {
            clearFieldError(jobTypeSelect);
        }

        return isValid;
    }

    private boolean validateStep2() {
        List<String> skills = getSkillsFromList();
        if (skills.isEmpty()) {
            // Show error on input if no skills added
            showFieldError(skillInput, I18n.get("postjob.skills_required", "Au moins une compétence est requise"));
            return false;
        }
        clearFieldError(skillInput);
        return true;
    }

    private List<String> getSkillsFromList() {
        List<String> skills = new ArrayList<>();
        for (javafx.scene.Node node : skillsList.getChildren()) {
            if (node instanceof HBox row) {
                for (javafx.scene.Node child : row.getChildren()) {
                    if (child instanceof TLBadge badge) {
                        String skillText = badge.getText();
                        if (skillText != null && !skillText.trim().isEmpty()) {
                            skills.add(skillText.trim());
                        }
                    }
                }
            }
        }
        return skills;
    }

    @FXML
    private void handlePrevious() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handlePublish() {
        if (!validateStep1() || !validateStep2()) {
            currentStep = !validateStep1() ? 1 : 2;
            showStep(currentStep);
            return;
        }

        String title = jobTitleField.getText().trim();
        String description = jobDescriptionField.getText().trim();
        String location = locationField.getText().trim();
        String contractType = jobTypeSelect.getValue();
        List<String> skills = getSkillsFromList();

        // Toutes les validations sont passées
        publishBtn.setDisable(true);
        final boolean isEditMode = offerToEdit != null && offerToEdit.getId() > 0;
        publishBtn
                .setText(isEditMode ? I18n.get("postjob.updating", "Mise à jour...") : I18n.get("postjob.publishing"));

        new Thread(() -> {
            try {

                // Ensure companyId is resolved before publishing
                int resolvedCompanyId = companyId;
                if (resolvedCompanyId <= 0 && currentUser != null) {
                    resolvedCompanyId = JobService.getInstance().getOrCreateEmployerCompanyId(
                            currentUser.getId(), currentUser.getFullName());
                    companyId = resolvedCompanyId;
                }
                if (resolvedCompanyId <= 0) {
                    resolvedCompanyId = JobService.getInstance().getOrCreateSystemCompanyId();
                }

                // Build JobOffer from form data
                JobOffer offer;
                if (isEditMode) {
                    // Use existing offer and update its fields (preserve status and dates)
                    offer = offerToEdit;
                } else {
                    // Create new offer - ALWAYS set status to OPEN for new offers
                    offer = new JobOffer();
                    offer.setStatus(JobStatus.OPEN);
                }

                offer.setEmployerId(resolvedCompanyId);
                offer.setTitle(title);
                offer.setDescription(description);
                offer.setLocation(location);
                // Ensure new offers are always OPEN (not DRAFT)
                if (!isEditMode) {
                    offer.setStatus(JobStatus.OPEN);
                }

                // Parse salary range (already validated above)
                String salaryTextLocal = salaryField.getText() != null ? salaryField.getText().trim() : "";
                if (!salaryTextLocal.isEmpty()) {
                    String cleanSalary = salaryTextLocal.replaceAll("[^0-9\\-]", "");
                    if (cleanSalary.contains("-")) {
                        String[] parts = cleanSalary.split("-");
                        offer.setSalaryMin(Double.parseDouble(parts[0].trim()));
                        offer.setSalaryMax(Double.parseDouble(parts[1].trim()));
                    } else {
                        double singleSalary = Double.parseDouble(cleanSalary.trim());
                        offer.setSalaryMin(singleSalary);
                        offer.setSalaryMax(singleSalary);
                    }
                }

                // Map contract type to work type (already validated above)
                if (contractType != null) {
                    offer.setWorkType(contractType.toUpperCase().replace("-", "_"));
                }

                if (offer.getCurrency() == null || offer.getCurrency().isEmpty()) {
                    offer.setCurrency("TND");
                }

                // Collect skills from the skills list (already validated above)
                offer.setRequiredSkills(skills);

                boolean success;
                if (isEditMode) {
                    // Update existing offer
                    success = JobService.getInstance().updateJobOffer(offer);
                } else {
                    // Create new offer
                    int id = JobService.getInstance().createJobOffer(offer);
                    success = id > 0;
                }

                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        publishBtn.setText("✓ " + (isEditMode ? I18n.get("postjob.updated", "Mis à jour")
                                : I18n.get("postjob.published")));
                        // Return to dashboard after delay
                        Thread delayThread = new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            javafx.application.Platform.runLater(() -> {
                                if (onCancel != null)
                                    onCancel.run();
                            });
                        }, "PublishDelayThread");
                        delayThread.setDaemon(true);
                        delayThread.start();
                    } else {
                        publishBtn.setText(isEditMode ? I18n.get("postjob.update_error", "Erreur de mise à jour")
                                : I18n.get("postjob.publish_error"));
                        publishBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to " + (offerToEdit != null ? "update" : "publish") + " job", e);
                javafx.application.Platform.runLater(() -> {
                    publishBtn.setText("Erreur: " + e.getMessage());
                    publishBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleSaveDraft() {
        logger.debug("Saving draft...");
        saveBtn.setText("✓ " + I18n.get("postjob.saved"));
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private void showFieldError(javafx.scene.Node field, String message) {
        if (field instanceof TLTextField) {
            ((TLTextField) field).getStyleClass().add("input-error");
        } else if (field instanceof TLTextarea) {
            ((TLTextarea) field).getStyleClass().add("textarea-error");
        } else if (field instanceof TLSelect) {
            ((TLSelect<?>) field).getStyleClass().add("input-error");
        }
        // Optionally show tooltip with error message
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(message);
        tooltip.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;");
        javafx.scene.control.Tooltip.install(field, tooltip);
    }

    private void clearFieldError(javafx.scene.Node field) {
        if (field instanceof TLTextField) {
            ((TLTextField) field).getStyleClass().remove("input-error");
        } else if (field instanceof TLTextarea) {
            ((TLTextarea) field).getStyleClass().remove("textarea-error");
        } else if (field instanceof TLSelect) {
            ((TLSelect<?>) field).getStyleClass().remove("input-error");
        }
        javafx.scene.control.Tooltip.uninstall(field, null);
    }
}
