package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.model.entity.JobOpportunity;
import com.skilora.utils.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * JobDetailsController - Displays detailed information about a job opportunity
 */
public class JobDetailsController {

    @FXML private TLButton backBtn;
    @FXML private TLButton applyBtn;
    @FXML private TLButton saveBtn;
    
    @FXML private Label companyInitials;
    @FXML private Label jobTitle;
    @FXML private Label companyName;
    @FXML private Label jobLocation;
    @FXML private Label jobType;
    @FXML private Label postedDate;
    @FXML private Label jobDescription;
    @FXML private Label salaryRange;
    @FXML private Label applicantCount;
    @FXML private Label viewCount;
    
    @FXML private FlowPane skillsContainer;
    @FXML private VBox benefitsList;
    
    private JobOpportunity currentJob;
    private Runnable onBack;
    private Runnable onApply;
    
    public void setJob(JobOpportunity job) {
        this.currentJob = job;
        populateJobDetails();
    }
    
    public void setCallbacks(Runnable onBack, Runnable onApply) {
        this.onBack = onBack;
        this.onApply = onApply;
    }
    
    private void populateJobDetails() {
        if (currentJob == null) return;
        
        // Header
        jobTitle.setText(currentJob.getTitle());
        companyName.setText(currentJob.getSource() != null ? currentJob.getSource() : I18n.get("jobdetails.not_specified"));
        
        // Company initials
        String company = currentJob.getSource();
        if (company != null && !company.isEmpty()) {
            String[] words = company.split("\\s+");
            String initials = words.length >= 2 
                ? (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase()
                : company.substring(0, Math.min(2, company.length())).toUpperCase();
            companyInitials.setText(initials);
        }
        
        // Location & Type
        jobLocation.setText("ðŸ“ " + (currentJob.getLocation() != null && !currentJob.getLocation().isEmpty() 
            ? currentJob.getLocation() 
            : "Remote / " + I18n.get("jobdetails.not_specified")));
        jobType.setText("ðŸ’¼ Full-time"); // Default, could be from job data
        
        // Posted date
        if (currentJob.getPostedDate() != null) {
            try {
                LocalDate posted = LocalDate.parse(currentJob.getPostedDate());
                long daysAgo = ChronoUnit.DAYS.between(posted, LocalDate.now());
                String dateText = daysAgo == 0 ? I18n.get("jobdetails.today") 
                    : daysAgo == 1 ? I18n.get("jobdetails.yesterday") 
                    : I18n.get("jobdetails.days_ago", daysAgo);
                postedDate.setText("ðŸ• " + dateText);
            } catch (Exception e) {
                postedDate.setText("ðŸ• " + currentJob.getPostedDate());
            }
        }
        
        // Description
        String desc = currentJob.getDescription();
        if (desc != null && desc.length() > 500) {
            desc = desc.substring(0, 500) + "...";
        }
        jobDescription.setText(desc != null ? desc : I18n.get("jobdetails.no_description"));
        
        // Skills (from title/description keywords)
        skillsContainer.getChildren().clear();
        extractAndDisplaySkills();

        // Salary - extract from description if available
        salaryRange.setText(extractSalaryFromDescription(desc));

        // Benefits - extract from description if mentioned
        benefitsList.getChildren().clear();
        extractAndDisplayBenefits(currentJob.getDescription());

        // Stats - deterministic based on job hash to avoid random flicker
        int hash = Math.abs((currentJob.getTitle() + currentJob.getSource()).hashCode());
        applicantCount.setText(String.valueOf(10 + (hash % 40)));
        viewCount.setText(String.valueOf(50 + (hash % 200)));
    }
    
    private void extractAndDisplaySkills() {
        String[] commonSkills = {"Java", "Spring", "JavaFX", "SQL", "Git", "Docker", "REST API"};
        String searchText = (currentJob.getTitle() + " " + currentJob.getDescription()).toLowerCase();
        
        for (String skill : commonSkills) {
            if (searchText.contains(skill.toLowerCase())) {
                TLBadge skillBadge = new TLBadge(skill, TLBadge.Variant.SECONDARY);
                skillsContainer.getChildren().add(skillBadge);
            }
        }
        
        if (skillsContainer.getChildren().isEmpty()) {
            TLBadge placeholder = new TLBadge(I18n.get("jobdetails.view_description"), TLBadge.Variant.OUTLINE);
            skillsContainer.getChildren().add(placeholder);
        }
    }
    
    private void addBenefit(String benefit) {
        HBox benefitRow = new HBox(8);
        benefitRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label checkIcon = new Label("âœ“");
        checkIcon.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        
        Label benefitLabel = new Label(benefit);
        benefitLabel.getStyleClass().add("text-muted");
        
        benefitRow.getChildren().addAll(checkIcon, benefitLabel);
        benefitsList.getChildren().add(benefitRow);
    }
    
    private String extractSalaryFromDescription(String desc) {
        if (desc == null) return I18n.get("jobdetails.salary.negotiable");
        String lower = desc.toLowerCase();

        // Look for common salary patterns
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(\\d{1,3}[.,]?\\d{0,3})\\s*(tnd|dt|eur|â‚¬|\\$|usd)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(desc);
        if (m.find()) {
            return m.group(0);
        }

        if (lower.contains("compÃ©titi") || lower.contains("attracti")) {
            return I18n.get("jobdetails.salary.competitive");
        }
        return I18n.get("jobdetails.salary.negotiable");
    }

    private void extractAndDisplayBenefits(String desc) {
        String lower = desc != null ? desc.toLowerCase() : "";

        String[][] benefitKeywords = {
            {"assurance", I18n.get("jobdetails.benefit.health")},
            {"remote", I18n.get("jobdetails.benefit.remote")},
            {"tÃ©lÃ©travail", I18n.get("jobdetails.benefit.telework")},
            {"formation", I18n.get("jobdetails.benefit.training")},
            {"transport", I18n.get("jobdetails.benefit.transport")},
            {"restaurant", I18n.get("jobdetails.benefit.meal")},
            {"bonus", I18n.get("jobdetails.benefit.bonus")},
            {"congÃ©", I18n.get("jobdetails.benefit.leave")},
            {"flexible", I18n.get("jobdetails.benefit.hours")}
        };

        int count = 0;
        for (String[] pair : benefitKeywords) {
            if (lower.contains(pair[0])) {
                addBenefit(pair[1]);
                count++;
            }
        }

        // Always show at least some default benefits
        if (count == 0) {
            addBenefit(I18n.get("jobdetails.benefit.health"));
            addBenefit(I18n.get("jobdetails.benefit.training"));
            addBenefit(I18n.get("jobdetails.benefit.dynamic"));
        }
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }
    
    @FXML
    private void handleApply() {
        applyBtn.setText("âœ“ " + I18n.get("jobdetails.applied"));
        applyBtn.setDisable(true);
        
        if (onApply != null) {
            onApply.run();
        }
    }
    
    @FXML
    private void handleSave() {
        if (saveBtn.getText().contains("ðŸ’¾")) {
            saveBtn.setText("â¤ï¸ " + I18n.get("jobdetails.saved"));
        } else {
            saveBtn.setText("ðŸ’¾ " + I18n.get("jobdetails.save"));
        }
    }
}

