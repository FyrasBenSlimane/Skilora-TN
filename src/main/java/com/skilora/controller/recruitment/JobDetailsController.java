package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.model.entity.recruitment.JobOpportunity;
import com.skilora.service.recruitment.RecruitmentIntelligenceService;
import com.skilora.utils.I18n;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * JobDetailsController - Displays detailed information about a job opportunity
 */
public class JobDetailsController {

    private static final Logger logger = LoggerFactory.getLogger(JobDetailsController.class);

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
    @FXML private Label matchScoreLabel;
    @FXML private Label candidateScoreLabel;
    @FXML private Label recommendationLabel;
    
    @FXML private FlowPane skillsContainer;
    @FXML private VBox benefitsList;
    
    private JobOpportunity currentJob;
    private Runnable onBack;
    private Runnable onApply;
    private com.skilora.model.entity.usermanagement.User currentUser;
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();
    
    public void setJob(JobOpportunity job) {
        this.currentJob = job;
        try {
            populateJobDetails();
        } catch (Exception e) {
            logger.error("Failed to render job details", e);
            if (jobTitle != null) {
                jobTitle.setText(I18n.get("jobdetails.not_specified"));
            }
            if (jobDescription != null) {
                jobDescription.setText(I18n.get("jobdetails.no_description"));
            }
        }
    }
    
    public void setCurrentUser(com.skilora.model.entity.usermanagement.User user) {
        this.currentUser = user;
        try {
            populateJobDetails();
        } catch (Exception e) {
            logger.error("Failed to refresh job details for user", e);
        }
    }
    
    public void setCallbacks(Runnable onBack, Runnable onApply) {
        this.onBack = onBack;
        this.onApply = onApply;
    }
    
    private void populateJobDetails() {
        if (currentJob == null) return;
        
        // Check if offer is closed
        boolean isOfferClosed = currentJob.getStatus() != null && "CLOSED".equals(currentJob.getStatus());
        
        // Header
        jobTitle.setText(currentJob.getTitle() != null ? currentJob.getTitle() : I18n.get("jobdetails.not_specified"));
        // Use company name from job offer, fallback to source
        String companyDisplayName = currentJob.getCompany() != null ? currentJob.getCompany() 
            : (currentJob.getSource() != null ? currentJob.getSource() : I18n.get("jobdetails.not_specified"));
        companyName.setText(companyDisplayName);
        
        // Show closed status if applicable
        if (isOfferClosed && jobTitle != null) {
            // Add a visual indicator that the offer is closed
            String titleText = jobTitle.getText();
            if (!titleText.contains("🔒")) {
                jobTitle.setText("🔒 " + titleText);
            }
        }
        
        // Company initials (split can yield empty tokens — substring(0,1) would crash)
        String company = companyDisplayName != null ? companyDisplayName.trim() : "";
        if (!company.isEmpty()) {
            List<String> words = new ArrayList<>();
            for (String w : company.split("\\s+")) {
                if (w != null && !w.isEmpty()) {
                    words.add(w);
                }
            }
            String initials;
            if (words.size() >= 2) {
                initials = (words.get(0).substring(0, 1) + words.get(1).substring(0, 1)).toUpperCase();
            } else {
                String w0 = words.get(0);
                initials = w0.substring(0, Math.min(2, w0.length())).toUpperCase();
            }
            companyInitials.setText(initials);
        } else {
            companyInitials.setText("?");
        }
        
        // Location & Type
        jobLocation.setText("📍 " + (currentJob.getLocation() != null && !currentJob.getLocation().isEmpty() 
            ? currentJob.getLocation() 
            : "Remote / " + I18n.get("jobdetails.not_specified")));
        // Use actual work type from job data
        jobType.setText("💼 " + (currentJob.getType() != null ? currentJob.getType() : "Full-Time"));
        
        // Posted date
        if (currentJob.getPostedDate() != null && !currentJob.getPostedDate().isBlank()) {
            try {
                LocalDate posted = LocalDate.parse(currentJob.getPostedDate());
                long daysAgo = ChronoUnit.DAYS.between(posted, LocalDate.now());
                String dateText = daysAgo == 0 ? I18n.get("jobdetails.today") 
                    : daysAgo == 1 ? I18n.get("jobdetails.yesterday") 
                    : I18n.get("jobdetails.days_ago", daysAgo);
                postedDate.setText("🕐 " + dateText);
            } catch (Exception e) {
                postedDate.setText("🕐 " + currentJob.getPostedDate());
            }
        } else if (postedDate != null) {
            postedDate.setText("🕐 —");
        }
        
        // Description - show FULL description without truncation (all information posted by employer)
        String desc = currentJob.getDescription();
        // Remove any salary/skills that might have been appended in old code
        if (desc != null && desc.contains("💰 Salaire:")) {
            desc = desc.substring(0, desc.indexOf("💰 Salaire:")).trim();
        }
        if (desc != null && desc.contains("🔧 Compétences requises:")) {
            desc = desc.substring(0, desc.indexOf("🔧 Compétences requises:")).trim();
        }
        jobDescription.setText(desc != null && !desc.isEmpty() ? desc : I18n.get("jobdetails.no_description"));
        
        // Skills - use actual required skills from job offer
        skillsContainer.getChildren().clear();
        if (currentJob.getSkills() != null && !currentJob.getSkills().isEmpty()) {
            // Display actual required skills from the job offer
            for (String skill : currentJob.getSkills()) {
                if (skill != null && !skill.trim().isEmpty()) {
                    TLBadge skillBadge = new TLBadge(skill.trim(), TLBadge.Variant.SECONDARY);
                    skillsContainer.getChildren().add(skillBadge);
                }
            }
        } else {
            // Fallback to extraction if no skills provided
            extractAndDisplaySkills();
        }

        // Salary - use actual salary information from job offer
        if (currentJob.getSalaryInfo() != null && !currentJob.getSalaryInfo().isEmpty()) {
            salaryRange.setText("💰 " + currentJob.getSalaryInfo());
        } else {
            // Fallback to extraction from description
            salaryRange.setText(extractSalaryFromDescription(desc));
        }

        // Benefits - extract from description if mentioned
        benefitsList.getChildren().clear();
        extractAndDisplayBenefits(currentJob.getDescription());

        // Stats - deterministic based on job hash to avoid random flicker
        String titleKey = currentJob.getTitle() != null ? currentJob.getTitle() : "";
        String sourceKey = currentJob.getSource() != null ? currentJob.getSource() : "";
        int hash = Math.abs((titleKey + sourceKey).hashCode());
        applicantCount.setText(String.valueOf(10 + (hash % 40)));
        viewCount.setText(String.valueOf(50 + (hash % 200)));

        // Disable Apply button if offer is closed and show "Fermée" in red
        if (isOfferClosed && applyBtn != null) {
            applyBtn.setDisable(true);
            applyBtn.setText("Fermée");
            applyBtn.setVariant(TLButton.ButtonVariant.DANGER);
            applyBtn.setOpacity(1.0);
        } else if (applyBtn != null) {
            applyBtn.setDisable(false);
            applyBtn.setText("Postuler");
            applyBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            applyBtn.setOpacity(1.0);
            // DB check off the FX thread so opening the page stays responsive
            scheduleApplyButtonStateRefresh();
        }

        loadAiInsights();
    }

    /**
     * hasApplied() hits the DB — running it here used to block the UI when opening details.
     */
    private void scheduleApplyButtonStateRefresh() {
        if (currentJob == null || currentUser == null || currentJob.getId() <= 0) {
            return;
        }
        final int jobId = currentJob.getId();
        final int userId = currentUser.getId();
        Thread t = new Thread(() -> {
            try {
                com.skilora.service.usermanagement.ProfileService profileService =
                        com.skilora.service.usermanagement.ProfileService.getInstance();
                com.skilora.model.entity.usermanagement.Profile profile =
                        profileService.findProfileByUserId(userId);
                if (profile == null || profile.getId() <= 0) {
                    return;
                }
                com.skilora.service.recruitment.ApplicationService appService =
                        com.skilora.service.recruitment.ApplicationService.getInstance();
                boolean applied = appService.hasApplied(jobId, profile.getId());
                Platform.runLater(() -> {
                    if (applyBtn == null || currentJob == null || currentJob.getId() != jobId) {
                        return;
                    }
                    if (applied) {
                        applyBtn.setDisable(true);
                        applyBtn.setText("✓ " + I18n.get("jobdetails.applied", "Candidature envoyée"));
                        applyBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
                    }
                });
            } catch (Exception e) {
                logger.debug("Apply button state check skipped: {}", e.getMessage());
            }
        }, "job-details-apply-check");
        t.setDaemon(true);
        t.start();
    }

    private void loadAiInsights() {
        if (matchScoreLabel == null || candidateScoreLabel == null || recommendationLabel == null) {
            return;
        }
        matchScoreLabel.setText("Match: -");
        candidateScoreLabel.setText("Score candidat: -");
        recommendationLabel.setText("Recommandations: -");

        if (currentJob == null || currentJob.getId() <= 0 || currentUser == null) {
            return;
        }
        if (currentUser.getRole() != com.skilora.model.enums.Role.USER) {
            return;
        }
        new Thread(() -> {
            try {
                var profile = com.skilora.service.usermanagement.ProfileService.getInstance().findProfileByUserId(currentUser.getId());
                if (profile == null) {
                    return;
                }
                int match = intelligenceService.calculateCompatibility(profile.getId(), currentJob.getId()).join();
                int candidateScore = intelligenceService.scoreCandidate(profile.getId()).join();
                var recos = intelligenceService.recommendJobs(profile.getId(), null, 3).join();
                String recoText = recos.isEmpty() ? "Aucune recommandation pour le moment."
                        : recos.stream().map(com.skilora.model.entity.recruitment.JobOffer::getTitle).reduce((a, b) -> a + ", " + b).orElse("-");

                Platform.runLater(() -> {
                    matchScoreLabel.setText("Match: " + match + "%");
                    candidateScoreLabel.setText("Score candidat: " + candidateScore + "/100");
                    recommendationLabel.setText("Recommandations: " + recoText);
                });
            } catch (Exception e) {
                logger.debug("AI insights unavailable: {}", e.getMessage());
                Platform.runLater(() -> recommendationLabel.setText("Recommandations: service IA indisponible."));
            }
        }, "JobDetailsAiInsights").start();
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
        
        Label checkIcon = new Label("✓");
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
            .compile("(\\d{1,3}[.,]?\\d{0,3})\\s*(tnd|dt|eur|€|\\$|usd)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(desc);
        if (m.find()) {
            return m.group(0);
        }

        if (lower.contains("compétiti") || lower.contains("attracti")) {
            return I18n.get("jobdetails.salary.competitive");
        }
        return I18n.get("jobdetails.salary.negotiable");
    }

    private void extractAndDisplayBenefits(String desc) {
        String lower = desc != null ? desc.toLowerCase() : "";

        String[][] benefitKeywords = {
            {"assurance", I18n.get("jobdetails.benefit.health")},
            {"remote", I18n.get("jobdetails.benefit.remote")},
            {"télétravail", I18n.get("jobdetails.benefit.telework")},
            {"formation", I18n.get("jobdetails.benefit.training")},
            {"transport", I18n.get("jobdetails.benefit.transport")},
            {"restaurant", I18n.get("jobdetails.benefit.meal")},
            {"bonus", I18n.get("jobdetails.benefit.bonus")},
            {"congé", I18n.get("jobdetails.benefit.leave")},
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
        // If button is already disabled (offer is closed), do nothing
        if (applyBtn != null && applyBtn.isDisable()) {
            return;
        }
        
        // Check if job offer is still open (not CLOSED) before allowing application
        boolean isClosed = false;
        if (currentJob != null && currentJob.getId() > 0) {
            try {
                com.skilora.service.recruitment.JobService jobService = com.skilora.service.recruitment.JobService.getInstance();
                com.skilora.model.entity.recruitment.JobOffer offer = jobService.findJobOfferById(currentJob.getId());
                
                if (offer != null && offer.getStatus() == com.skilora.model.enums.JobStatus.CLOSED) {
                    isClosed = true;
                }
            } catch (Exception e) {
                // Check status from currentJob if database check fails
                if (currentJob.getStatus() != null && "CLOSED".equals(currentJob.getStatus())) {
                    isClosed = true;
                }
            }
        } else if (currentJob != null && currentJob.getStatus() != null && "CLOSED".equals(currentJob.getStatus())) {
            isClosed = true;
        }
        
        // If offer is closed, show error and do NOT change button state
        if (isClosed) {
            com.skilora.utils.DialogUtils.showError("Offre fermée", 
                "Cette offre d'emploi est fermée. Vous ne pouvez plus postuler.");
            return; // Stop here - do NOT change button or call onApply
        }

        // Open the application dialog; do not mark as "applied" here — success is
        // confirmed only after submit inside the dialog (otherwise Cancel leaves a false state).
        if (onApply != null) {
            onApply.run();
        }
    }
    
    @FXML
    private void handleSave() {
        if (saveBtn.getText().contains("💾")) {
            saveBtn.setText("❤️ " + I18n.get("jobdetails.saved"));
        } else {
            saveBtn.setText("💾 " + I18n.get("jobdetails.save"));
        }
    }
}
