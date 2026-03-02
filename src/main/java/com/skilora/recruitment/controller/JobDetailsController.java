package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.entity.SavedJob;
import com.skilora.recruitment.service.RecruitmentIntelligenceService;
import com.skilora.recruitment.service.SavedJobService;
import com.skilora.user.entity.User;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
    @FXML private Label descriptionHeader;
    @FXML private Label skillsHeader;
    @FXML private Label salaryHeader;
    @FXML private Label benefitsHeader;
    @FXML private Label statsHeader;
    @FXML private Label applicantCountLabel;
    @FXML private Label viewCountLabel;
    
    private JobOpportunity currentJob;
    private Runnable onBack;
    private Runnable onApply;
    private boolean jobSaved = false;
    private Integer currentUserId;
    private User currentUserObj;
    private final SavedJobService savedJobService = SavedJobService.getInstance();
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();

    public void setCurrentUser(User user) {
        this.currentUserId = user != null ? user.getId() : null;
        this.currentUserObj = user;
        syncSavedState();
    }
    
    public void setJob(JobOpportunity job) {
        this.currentJob = job;
        backBtn.setText(I18n.get("jobdetails.back"));
        backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        applyBtn.setText(I18n.get("feed.open_listing"));
        syncSavedState();
        if (descriptionHeader != null) descriptionHeader.setText(I18n.get("jobdetails.description"));
        if (skillsHeader != null) skillsHeader.setText(I18n.get("jobdetails.skills"));
        if (salaryHeader != null) salaryHeader.setText(I18n.get("jobdetails.salary_section"));
        if (benefitsHeader != null) benefitsHeader.setText(I18n.get("jobdetails.benefits"));
        if (statsHeader != null) statsHeader.setText(I18n.get("jobdetails.statistics"));
        if (applicantCountLabel != null) applicantCountLabel.setText(I18n.get("jobdetails.applications"));
        if (viewCountLabel != null) viewCountLabel.setText(I18n.get("jobdetails.views"));
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
        jobLocation.setGraphic(SvgIcons.icon(SvgIcons.MAP_PIN, 14, "-fx-muted-foreground"));
        jobLocation.setText((currentJob.getLocation() != null && !currentJob.getLocation().isEmpty() 
            ? currentJob.getLocation() 
            : I18n.get("jobdetails.remote") + " / " + I18n.get("jobdetails.not_specified")));
        jobType.setGraphic(SvgIcons.icon(SvgIcons.BRIEFCASE, 14, "-fx-muted-foreground"));
        jobType.setText(I18n.get("jobdetails.type.fulltime")); // Default when no type data available
        
        // Posted date
        if (currentJob.getPostedDate() != null) {
            try {
                LocalDate posted = LocalDate.parse(currentJob.getPostedDate());
                long daysAgo = ChronoUnit.DAYS.between(posted, LocalDate.now());
                String dateText = daysAgo == 0 ? I18n.get("jobdetails.today") 
                    : daysAgo == 1 ? I18n.get("jobdetails.yesterday") 
                    : I18n.get("jobdetails.days_ago", daysAgo);
                postedDate.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 14, "-fx-muted-foreground"));
                postedDate.setText(dateText);
            } catch (Exception e) {
                postedDate.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 14, "-fx-muted-foreground"));
                postedDate.setText(currentJob.getPostedDate());
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

        // Stats - show as unavailable since external jobs don't have real tracking
        applicantCount.setText("—");
        viewCount.setText("—");

        // Check if job is closed
        boolean isOfferClosed = currentJob.getStatus() != null && "CLOSED".equals(currentJob.getStatus());
        if (isOfferClosed && applyBtn != null) {
            applyBtn.setDisable(true);
            applyBtn.setText("Ferm\u00e9e");
            applyBtn.setVariant(TLButton.ButtonVariant.DANGER);
            applyBtn.setOpacity(1.0);
        }

        // Load AI insights asynchronously
        loadAiInsights();
    }

    /**
     * Load AI-based matching insights for the current user against this job.
     */
    private void loadAiInsights() {
        if (matchScoreLabel == null || candidateScoreLabel == null || recommendationLabel == null) return;
        matchScoreLabel.setText("Match: -");
        candidateScoreLabel.setText("Score candidat: -");
        recommendationLabel.setText("Recommandations: -");

        if (currentJob == null || currentJob.getId() <= 0 || currentUserObj == null) return;

        Thread t = new Thread(() -> {
            try {
                var profile = com.skilora.user.service.ProfileService.getInstance()
                        .findProfileByUserId(currentUserObj.getId());
                if (profile == null) return;

                int match = intelligenceService.calculateCompatibility(profile.getId(), currentJob.getId()).join();
                int candidateScore = intelligenceService.scoreCandidate(profile.getId()).join();

                Platform.runLater(() -> {
                    matchScoreLabel.setText("Match: " + match + "%");
                    candidateScoreLabel.setText("Score candidat: " + candidateScore + "/100");
                });
            } catch (Exception e) {
                logger.debug("AI insights unavailable: {}", e.getMessage());
                Platform.runLater(() -> recommendationLabel.setText("Recommandations: service IA indisponible."));
            }
        }, "JobDetailsAiInsights");
        t.setDaemon(true);
        t.start();
    }
    
    private void extractAndDisplaySkills() {
        String[] commonSkills = {"Java", "Spring", "JavaFX", "SQL", "Git", "Docker", "REST API",
            "Python", "JavaScript", "TypeScript", "React", "Angular", "Node.js", "AWS",
            "Azure", "Kubernetes", "CI/CD", "Agile", "Scrum", "Linux", "C#", ".NET",
            "PHP", "Laravel", "DevOps", "MongoDB", "PostgreSQL", "Redis", "Kafka"};
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
        
        Label checkIcon = new Label();
        checkIcon.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, "-fx-green"));
        
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

        // Only show benefits actually found in description
        if (count == 0) {
            Label noBenefits = new Label(I18n.get("jobdetails.benefit.not_listed"));
            noBenefits.getStyleClass().add("text-muted");
            benefitsList.getChildren().add(noBenefits);
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
        applyBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
        applyBtn.setText(I18n.get("feed.opened"));
        applyBtn.setDisable(true);
        
        // Open the job URL in the default browser
        String targetUrl = currentJob != null && currentJob.getApplyUrl() != null && !currentJob.getApplyUrl().isBlank()
                ? currentJob.getApplyUrl()
                : (currentJob != null ? currentJob.getUrl() : null);
        if (targetUrl != null && !targetUrl.isBlank()) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(targetUrl));
            } catch (Exception ex) {
                // Fallback: just log
                System.err.println("Failed to open URL: " + targetUrl);
            }
        }
        
        if (onApply != null) {
            onApply.run();
        }
    }
    
    @FXML
    private void handleSave() {
        jobSaved = !jobSaved;
        persistSavedState();
        updateSaveButtonUi();
    }

    private void syncSavedState() {
        if (saveBtn == null) return;
        String url = currentJob != null ? currentJob.getUrl() : null;
        if (currentUserId != null && currentUserId > 0 && url != null && !url.isBlank()) {
            jobSaved = savedJobService.isSaved(currentUserId, url);
        } else {
            jobSaved = false;
        }
        updateSaveButtonUi();
    }

    private void persistSavedState() {
        String url = currentJob != null ? currentJob.getUrl() : null;
        if (currentUserId == null || currentUserId <= 0 || url == null || url.isBlank()) return;

        if (jobSaved) {
            SavedJob s = new SavedJob();
            s.setUserId(currentUserId);
            s.setJobUrl(url);
            s.setJobTitle(currentJob.getTitle());
            s.setCompanyName(currentJob.getSource());
            s.setLocation(currentJob.getLocation());
            s.setSource(currentJob.getSource());
            savedJobService.save(s);
        } else {
            savedJobService.unsave(currentUserId, url);
        }
    }

    private void updateSaveButtonUi() {
        if (saveBtn == null) return;
        if (jobSaved) {
            saveBtn.setGraphic(SvgIcons.filledIcon(SvgIcons.BOOKMARK, 14, "-fx-primary"));
            saveBtn.setText(I18n.get("jobdetails.saved"));
        } else {
            saveBtn.setGraphic(SvgIcons.icon(SvgIcons.BOOKMARK, 14));
            saveBtn.setText(I18n.get("jobdetails.save"));
        }
    }
}
