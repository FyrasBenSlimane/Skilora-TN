package com.skilora.controller;

import com.skilora.framework.components.*;
import com.skilora.model.entity.JobOffer;
import com.skilora.model.entity.User;
import com.skilora.model.enums.JobStatus;
import com.skilora.model.service.JobService;

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
                this.companyId = JobService.getInstance().getOrCreateEmployerCompanyId(user.getId(), user.getFullName());
            }, "CompanyResolverThread");
            t.setDaemon(true);
            t.start();
        }
    }
    
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
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
        
        jobTypeSelect = new TLSelect<String>(I18n.get("postjob.contract_type"));
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
        
        // Add some default skills as examples
        addSkillBadge("Java");
        addSkillBadge("Spring Boot");
        addSkillBadge("SQL");
        
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
        
        Label title = new Label(jobTitleField.getText().isEmpty() ? I18n.get("postjob.no_title") : jobTitleField.getText());
        title.getStyleClass().add("h2");
        
        Label type = new Label(jobTypeSelect.getValue() != null ? jobTypeSelect.getValue() : I18n.get("postjob.no_type"));
        type.getStyleClass().add("text-muted");
        
        Label location = new Label("📍 " + (locationField.getText().isEmpty() ? I18n.get("postjob.no_location") : locationField.getText()));
        
        Label salary = new Label("💰 " + (salaryField.getText().isEmpty() ? I18n.get("postjob.no_salary") : salaryField.getText()));
        
        Label descTitle = new Label(I18n.get("postjob.description").replace(" *", ""));
        descTitle.getStyleClass().add("h4");
        
        Label desc = new Label(jobDescriptionField.getText().isEmpty() ? I18n.get("postjob.no_description") : jobDescriptionField.getText());
        desc.setWrapText(true);
        
        previewContent.getChildren().addAll(title, type, location, salary, new Separator(), descTitle, desc);
        
        if (!skillsList.getChildren().isEmpty()) {
            Label skillsTitle = new Label(I18n.get("postjob.skills_required"));
            skillsTitle.getStyleClass().add("h4");
            previewContent.getChildren().addAll(new Separator(), skillsTitle, skillsList);
        }
        
        previewCard.getContent().add(previewContent);
        
        step3View.getChildren().addAll(previewTitle, previewCard);
    }
    
    @FXML
    private void handleNext() {
        if (currentStep < 3) {
            showStep(currentStep + 1);
        }
    }
    
    @FXML
    private void handlePrevious() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }
    
    @FXML
    private void handlePublish() {
        String title = jobTitleField.getText().trim();
        if (title.isEmpty()) {
            publishBtn.setText("⚠ " + I18n.get("postjob.title_required"));
            return;
        }

        publishBtn.setDisable(true);
        publishBtn.setText(I18n.get("postjob.publishing"));

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
                JobOffer offer = new JobOffer();
                offer.setEmployerId(resolvedCompanyId);
                offer.setTitle(title);
                offer.setDescription(jobDescriptionField.getText());
                offer.setLocation(locationField.getText());
                offer.setStatus(JobStatus.OPEN);

                // Parse salary range (e.g. "3000-5000")
                String salaryText = salaryField.getText().replaceAll("[^0-9\\-]", "");
                if (salaryText.contains("-")) {
                    String[] parts = salaryText.split("-");
                    try {
                        offer.setSalaryMin(Double.parseDouble(parts[0].trim()));
                        offer.setSalaryMax(Double.parseDouble(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
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

                int id = JobService.getInstance().createJobOffer(offer);

                javafx.application.Platform.runLater(() -> {
                    if (id > 0) {
                        publishBtn.setText("✓ " + I18n.get("postjob.published"));
                        // Return to dashboard after delay
                        Thread delayThread = new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            javafx.application.Platform.runLater(() -> {
                                if (onCancel != null) onCancel.run();
                            });
                        }, "PublishDelayThread");
                        delayThread.setDaemon(true);
                        delayThread.start();
                    } else {
                        publishBtn.setText(I18n.get("postjob.publish_error"));
                        publishBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to publish job", e);
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
}
