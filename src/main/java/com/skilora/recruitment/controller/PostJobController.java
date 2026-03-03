package com.skilora.recruitment.controller;

import com.skilora.framework.components.*;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.user.entity.User;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

/**
 * PostJobController - 3-step wizard for posting job opportunities
 */
public class PostJobController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PostJobController.class);

    @FXML private Label stepLabel;
    @FXML private TLButton saveBtn;
    @FXML private VBox step1Indicator;
    @FXML private VBox step2Indicator;
    @FXML private VBox step3Indicator;
    @FXML private ProgressBar progress1;
    @FXML private ProgressBar progress2;
    @FXML private ProgressBar progress3;
    @FXML private StackPane contentStack;
    @FXML private TLButton cancelBtn;
    @FXML private TLButton prevBtn;
    @FXML private TLButton nextBtn;
    @FXML private TLButton publishBtn;
    
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
    private JobOffer offerToEdit; // non-null when editing an existing offer
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        saveBtn.setGraphic(SvgIcons.icon(SvgIcons.SAVE, 14));
        prevBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        nextBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_RIGHT, 14));
        nextBtn.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        publishBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 14));
        createStepViews();
        showStep(1);
    }

    private void applyI18n() {
        saveBtn.setText(I18n.get("postjob.save_draft"));
        cancelBtn.setText(I18n.get("common.cancel"));
        prevBtn.setText(I18n.get("postjob.previous"));
        nextBtn.setText(I18n.get("common.next"));
        publishBtn.setText(I18n.get("postjob.publish"));
    }
    
    /**
     * Set the current employer user and resolve their company.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Resolve company in background
        if (user != null) {
            AppThreadPool.execute(() -> {
                try {
                    this.companyId = JobService.getInstance().getOrCreateEmployerCompanyId(user.getId(), user.getFullName());
                } catch (Exception e) {
                    logger.error("Failed to resolve employer company ID", e);
                }
            });
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
        if (offer == null) return;
        this.offerToEdit = offer;

        if (publishBtn != null) publishBtn.setText(I18n.get("postjob.update", "Mettre à jour"));

        // Step 1 fields
        if (offer.getTitle() != null) jobTitleField.setText(offer.getTitle());
        if (offer.getDescription() != null) jobDescriptionField.setText(offer.getDescription());
        if (offer.getLocation() != null) locationField.setText(offer.getLocation());

        if (offer.getWorkType() != null) {
            String wt = offer.getWorkType().toUpperCase();
            String display = switch (wt) {
                case "FULL_TIME", "FULL-TIME" -> "Full-time";
                case "PART_TIME", "PART-TIME" -> "Part-time";
                case "CONTRACT" -> "Contract";
                case "FREELANCE" -> "Freelance";
                case "INTERNSHIP" -> "Internship";
                default -> {
                    String[] words = wt.toLowerCase().replace("_", "-").split("-");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < words.length; i++) {
                        if (i > 0) sb.append("-");
                        if (!words[i].isEmpty()) sb.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
                    }
                    yield sb.toString();
                }
            };
            jobTypeSelect.setValue(display);
        }

        if (offer.getSalaryMin() > 0 || offer.getSalaryMax() > 0) {
            if (offer.getSalaryMin() > 0 && offer.getSalaryMax() > 0)
                salaryField.setText(String.format("%.0f-%.0f", offer.getSalaryMin(), offer.getSalaryMax()));
            else if (offer.getSalaryMin() > 0)
                salaryField.setText(String.format("%.0f", offer.getSalaryMin()));
            else
                salaryField.setText(String.format("%.0f", offer.getSalaryMax()));
        }

        // Step 2 - Skills
        skillsList.getChildren().clear();
        if (offer.getRequiredSkills() != null) {
            for (String skill : offer.getRequiredSkills()) addSkillBadge(skill);
        }

        if (offer.getEmployerId() > 0) this.companyId = offer.getEmployerId();
    }
    
    private void createStepViews() {
        // Step 1: Basic Info
        step1View = new VBox(16);
        step1View.setPadding(new Insets(24));
        
        jobTitleField = new TLTextField();
        jobTitleField.setPromptText(I18n.get("postjob.title.placeholder"));
        
        jobDescriptionField = new TLTextarea();
        jobDescriptionField.setPromptText(I18n.get("postjob.description.placeholder"));
        jobDescriptionField.setPrefHeight(200);
        
        jobTypeSelect = new TLSelect<String>(I18n.get("postjob.contract_type"));
        jobTypeSelect.getItems().addAll(
            I18n.get("postjob.type.fulltime"),
            I18n.get("postjob.type.parttime"),
            I18n.get("postjob.type.contract"),
            I18n.get("postjob.type.freelance"),
            I18n.get("postjob.type.internship")
        );
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
            createFormField(I18n.get("postjob.salary"), salaryField)
        );
        
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
            skillsList
        );
        
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
        removeBtn.getStyleClass().add("icon-btn-sm");
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
        
        Label title = new Label(jobTitleField.getText().isEmpty() ? I18n.get("postjob.no_title") : jobTitleField.getText());
        title.getStyleClass().add("h2");
        
        Label type = new Label(jobTypeSelect.getValue() != null ? jobTypeSelect.getValue() : I18n.get("postjob.no_type"));
        type.getStyleClass().add("text-muted");
        
        Label location = new Label(locationField.getText().isEmpty() ? I18n.get("postjob.no_location") : locationField.getText());
        location.setGraphic(SvgIcons.icon(SvgIcons.MAP_PIN, 14, "-fx-muted-foreground"));
        
        Label salary = new Label(salaryField.getText().isEmpty() ? I18n.get("postjob.no_salary") : salaryField.getText());
        salary.setGraphic(SvgIcons.icon(SvgIcons.DOLLAR_SIGN, 14, "-fx-muted-foreground"));
        
        Label descTitle = new Label(I18n.get("postjob.description").replace(" *", ""));
        descTitle.getStyleClass().add("h4");
        
        Label desc = new Label(jobDescriptionField.getText().isEmpty() ? I18n.get("postjob.no_description") : jobDescriptionField.getText());
        desc.setWrapText(true);
        
        previewContent.getChildren().addAll(title, type, location, salary, new TLSeparator(), descTitle, desc);
        
        if (!skillsList.getChildren().isEmpty()) {
            Label skillsTitle = new Label(I18n.get("postjob.skills_required"));
            skillsTitle.getStyleClass().add("h4");
            previewContent.getChildren().addAll(new TLSeparator(), skillsTitle, skillsList);
        }
        
        previewCard.getContent().add(previewContent);
        
        step3View.getChildren().addAll(previewTitle, previewCard);
    }
    
    @FXML
    private void handleNext() {
        if (currentStep == 1 && !validateStep1()) return;
        if (currentStep == 2 && !validateStep2()) return;
        if (currentStep < 3) {
            showStep(currentStep + 1);
        }
    }

    private boolean validateStep2() {
        List<String> skills = getSkillsFromList();
        if (skills.isEmpty()) {
            if (skillInput != null) skillInput.setError(I18n.get("postjob.skills_required", "Au moins une compétence est requise"));
            return false;
        }
        if (skillInput != null) skillInput.clearValidation();
        return true;
    }

    private List<String> getSkillsFromList() {
        List<String> skills = new ArrayList<>();
        for (javafx.scene.Node node : skillsList.getChildren()) {
            if (node instanceof HBox row) {
                for (javafx.scene.Node child : row.getChildren()) {
                    if (child instanceof TLBadge badge) {
                        // TLBadge stores text in its internal Label
                        String text = badge.toString();
                        try { text = ((javafx.scene.control.Label) badge.getChildren().get(0)).getText(); } catch (Exception ignored) {}
                        if (text != null && !text.trim().isEmpty()) skills.add(text.trim());
                    }
                }
            }
        }
        return skills;
    }

    private boolean validateStep1() {
        boolean valid = true;
        if (jobTitleField != null) {
            if (jobTitleField.getText() == null || jobTitleField.getText().trim().isEmpty()) {
                jobTitleField.setError(I18n.get("postjob.error.title_required"));
                valid = false;
            } else {
                jobTitleField.clearValidation();
            }
        }
        if (jobDescriptionField != null) {
            String desc = jobDescriptionField.getText();
            if (desc == null || desc.trim().length() < 20) {
                jobDescriptionField.setError(I18n.get("postjob.error.description_short"));
                valid = false;
            } else {
                jobDescriptionField.clearValidation();
            }
        }
        if (locationField != null) {
            if (locationField.getText() == null || locationField.getText().trim().isEmpty()) {
                locationField.setError(I18n.get("postjob.error.location_required"));
                valid = false;
            } else {
                locationField.clearValidation();
            }
        }
        return valid;
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
            showStep(!validateStep1() ? 1 : 2);
            return;
        }

        String title = jobTitleField.getText().trim();
        final boolean isEditMode = offerToEdit != null && offerToEdit.getId() > 0;

        publishBtn.setDisable(true);
        publishBtn.setText(isEditMode ? I18n.get("postjob.updating", "Mise à jour...") : I18n.get("postjob.publishing"));

        AppThreadPool.execute(() -> {
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
                    offer = offerToEdit;
                } else {
                    offer = new JobOffer();
                    offer.setStatus(JobStatus.OPEN);
                }
                offer.setEmployerId(resolvedCompanyId);
                offer.setTitle(title);
                offer.setDescription(jobDescriptionField.getText());
                offer.setLocation(locationField.getText());
                if (!isEditMode) offer.setStatus(JobStatus.OPEN);

                // Parse salary range (e.g. "3000-5000")
                String salaryText = salaryField.getText().replaceAll("[^0-9\\-]", "");
                if (salaryText.contains("-")) {
                    String[] parts = salaryText.split("-");
                    try {
                        offer.setSalaryMin(Double.parseDouble(parts[0].trim()));
                        offer.setSalaryMax(Double.parseDouble(parts[1].trim()));
                    } catch (NumberFormatException e) {
                        // Expected: user typed non-numeric salary range, leave min/max unset
                        logger.debug("Could not parse salary range: {}", salaryText);
                    }
                }

                // Map contract type to work type
                String contractType = jobTypeSelect.getValue();
                if (contractType != null) {
                    offer.setWorkType(contractType.toUpperCase().replace("-", "_"));
                }

                offer.setCurrency("TND");

                // Collect skills from the skills list
                List<String> skills = new ArrayList<>();
                for (javafx.scene.Node node : skillsList.getChildren()) {
                    if (node instanceof HBox row) {
                        for (javafx.scene.Node child : row.getChildren()) {
                            if (child instanceof TLBadge badge) {
                                // Get text from badge's label child
                                if (!badge.getChildren().isEmpty() && badge.getChildren().get(0) instanceof javafx.scene.control.Label lbl) {
                                    skills.add(lbl.getText());
                                }
                            }
                        }
                    }
                }
                offer.setRequiredSkills(skills);

                boolean success;
                if (isEditMode) {
                    success = JobService.getInstance().updateJobOffer(offer);
                } else {
                    int id = JobService.getInstance().createJobOffer(offer);
                    success = id > 0;
                }

                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        publishBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
                        publishBtn.setText(isEditMode
                                ? I18n.get("postjob.updated", "Mis à jour")
                                : I18n.get("postjob.published"));
                        // Return to dashboard after delay
                        AppThreadPool.execute(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            javafx.application.Platform.runLater(() -> {
                                if (onCancel != null) onCancel.run();
                            });
                        });
                    } else {
                        publishBtn.setText(isEditMode
                                ? I18n.get("postjob.update_error", "Erreur de mise à jour")
                                : I18n.get("postjob.publish_error"));
                        publishBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to " + (isEditMode ? "update" : "publish") + " job", e);
                javafx.application.Platform.runLater(() -> {
                    publishBtn.setText(I18n.get("postjob.publish_failed"));
                    publishBtn.setDisable(false);
                });
            }
        });
    }
    
    @FXML
    private void handleSaveDraft() {
        logger.debug("Saving draft...");

        saveBtn.setDisable(true);
        saveBtn.setText(I18n.get("postjob.saving"));

        AppThreadPool.execute(() -> {
            try {
                // Ensure companyId is resolved
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
                JobOffer draft = new JobOffer();
                draft.setEmployerId(resolvedCompanyId);
                draft.setTitle(jobTitleField.getText().trim().isEmpty()
                        ? I18n.get("postjob.untitled_draft") : jobTitleField.getText().trim());
                draft.setDescription(jobDescriptionField.getText());
                draft.setLocation(locationField.getText());
                draft.setStatus(JobStatus.DRAFT);

                // Parse salary range
                String salaryText = salaryField.getText().replaceAll("[^0-9\\-]", "");
                if (salaryText.contains("-")) {
                    String[] parts = salaryText.split("-");
                    try {
                        draft.setSalaryMin(Double.parseDouble(parts[0].trim()));
                        draft.setSalaryMax(Double.parseDouble(parts[1].trim()));
                    } catch (NumberFormatException e) {
                        logger.debug("Could not parse salary range: {}", salaryText);
                    }
                }

                // Map contract type
                String contractType = jobTypeSelect.getValue();
                if (contractType != null) {
                    draft.setWorkType(contractType.toUpperCase().replace("-", "_"));
                }

                draft.setCurrency("TND");

                // Collect skills
                List<String> skills = new ArrayList<>();
                for (javafx.scene.Node node : skillsList.getChildren()) {
                    if (node instanceof javafx.scene.layout.HBox row) {
                        for (javafx.scene.Node child : row.getChildren()) {
                            if (child instanceof TLBadge badge) {
                                if (!badge.getChildren().isEmpty()
                                        && badge.getChildren().get(0) instanceof javafx.scene.control.Label lbl) {
                                    skills.add(lbl.getText());
                                }
                            }
                        }
                    }
                }
                draft.setRequiredSkills(skills);

                int id = JobService.getInstance().createJobOffer(draft);

                javafx.application.Platform.runLater(() -> {
                    if (id > 0) {
                        saveBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
                        saveBtn.setText(I18n.get("postjob.saved"));
                    } else {
                        saveBtn.setGraphic(SvgIcons.icon(SvgIcons.ALERT_TRIANGLE, 14, "-fx-destructive"));
                        saveBtn.setText(I18n.get("postjob.save_error"));
                    }
                    saveBtn.setDisable(false);
                });
            } catch (Exception e) {
                logger.error("Failed to save draft", e);
                javafx.application.Platform.runLater(() -> {
                    saveBtn.setGraphic(SvgIcons.icon(SvgIcons.ALERT_TRIANGLE, 14, "-fx-destructive"));
                    saveBtn.setText(I18n.get("postjob.save_error"));
                    saveBtn.setDisable(false);
                });
            }
        });
    }
    
    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }
}
