package com.skilora.controller.usermanagement;

import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.framework.components.InterviewCountdownWidget;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.recruitment.ApplicationService;
import com.skilora.service.recruitment.InterviewService;
import com.skilora.service.recruitment.RecruitmentIntelligenceService;
import com.skilora.service.usermanagement.ProfileService;
import com.skilora.utils.I18n;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * UserDashboardController
 * 
 * Manages the dashboard for Job Seekers (USER role).
 * Displays profile progress, stats, job recommendations, and recent activity.
 */
public class UserDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(UserDashboardController.class);

    @FXML
    private Label greetingLabel;
    @FXML
    private Label subGreetingLabel;

    @FXML
    private VBox profileCompletionCard;
    @FXML
    private Label completionPercentageLabel;
    @FXML
    private ProgressBar profileProgressBar;
    @FXML
    private TLButton completeProfileBtn;

    @FXML
    private FlowPane statsGrid;

    @FXML
    private VBox recommendedJobsContainer;
    @FXML
    private VBox recentApplicationsContainer;
    @FXML
    private FlowPane quickActionsContainer;

    @FXML
    private Label profileCompletionTitle;
    @FXML
    private Label quickActionsTitle;
    @FXML
    private Label recommendedJobsTitle;
    @FXML
    private Label recentApplicationsTitle;
    @FXML
    private TLButton viewAllJobsBtn;

    private static final DateTimeFormatter INTERVIEW_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private User currentUser;
    private Profile currentProfile;

    private Runnable onNavigateToProfile;
    private Runnable onNavigateToFeed;
    private Runnable onNavigateToApplications;
    private Runnable onNavigateToSettings;

    private final ProfileService profileService = ProfileService.getInstance();
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();
    private final InterviewService interviewService = InterviewService.getInstance();

    public void initializeContext(User user, Runnable toProfile, Runnable toFeed, Runnable toApplications,
            Runnable toSettings) {
        this.currentUser = user;
        this.onNavigateToProfile = toProfile;
        this.onNavigateToFeed = toFeed;
        this.onNavigateToApplications = toApplications;
        this.onNavigateToSettings = toSettings;

        setupGreeting();
        setupQuickActions();
        setupLabels();
        loadDashboardData();
    }

    private void setupGreeting() {
        String greeting = getTimeBasedGreeting();
        String name = (currentUser.getFullName() != null)
                ? currentUser.getFullName().split(" ")[0]
                : currentUser.getUsername();

        greetingLabel.setText(greeting + ", " + name + " \uD83D\uDE80");
        subGreetingLabel.setText(I18n.get("dashboard.user.sub_greeting"));
    }

    private void setupLabels() {
        profileCompletionTitle.setText(I18n.get("dashboard.user.profile_completion"));
        completeProfileBtn.setText(I18n.get("dashboard.user.complete_now"));
        quickActionsTitle.setText(I18n.get("dashboard.title.quick_actions"));
        recommendedJobsTitle.setText(I18n.get("dashboard.user.recommended_for_you"));
        viewAllJobsBtn.setText(I18n.get("common.view_all"));
        recentApplicationsTitle.setText(I18n.get("dashboard.user.recent_applications"));
    }

    private String getTimeBasedGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour >= 5 && hour < 12)
            return I18n.get("dashboard.greeting.morning");
        if (hour >= 12 && hour < 18)
            return I18n.get("dashboard.greeting.afternoon");
        return I18n.get("dashboard.greeting.evening");
    }

    private void setupQuickActions() {
        quickActionsContainer.getChildren().clear();

        quickActionsContainer.getChildren().addAll(
                createQuickAction(I18n.get("dashboard.action.update_profile"),
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                        onNavigateToProfile),
                createQuickAction(I18n.get("dashboard.action.explore_offers"),
                        "M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z",
                        onNavigateToFeed),
                createQuickAction(I18n.get("nav.applications"),
                        "M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z",
                        onNavigateToApplications),
                createQuickAction(I18n.get("dashboard.action.settings"),
                        "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
                        onNavigateToSettings),
                createQuickAction("Créer mon CV",
                        "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.89 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
                        this::openCvGenerator));
    }

    private void loadDashboardData() {
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                // 1. Fetch Profile
                currentProfile = profileService.findProfileByUserId(currentUser.getId());

                int completion = 0;
                int appCount = 0;
                int interviewCount = 0;
                int score = 0;
                List<JobOffer> recommendations = null;
                List<Application> recentApps = null;

                if (currentProfile != null) {
                    // 2. Fetch Completion
                    Map<String, Object> details = profileService.getProfileWithDetails(currentProfile.getId());
                    completion = (int) details.get("completionPercentage");

                    // 3. Fetch Job Recommendations
                    recommendations = intelligenceService.recommendJobs(currentProfile.getId(), null, 3).join();

                    // 4. Fetch Applications
                    recentApps = applicationService.getApplicationsByProfile(currentProfile.getId());
                    appCount = recentApps.size();

                    for (Application app : recentApps) {
                        if (app.getStatus() == Application.Status.INTERVIEW) {
                            interviewCount++;
                        }
                    }

                    // 5. Fetch Candidate Score
                    score = intelligenceService.scoreCandidate(currentProfile.getId()).join();
                }

                // 6. Fetch upcoming interviews for countdown display
                List<Interview> upcoming = new java.util.ArrayList<>();
                if (currentProfile != null) {
                    try {
                        upcoming = interviewService.getUpcomingInterviewsForCandidate(currentProfile.getId());
                    } catch (Exception ex) {
                        logger.warn("Could not load upcoming interviews: {}", ex.getMessage());
                    }
                }
                return new DashboardData(completion, appCount, interviewCount, score, recommendations, recentApps, upcoming);
            }
        };

        task.setOnSucceeded(e -> updateUI(task.getValue()));
        task.setOnFailed(e -> logger.error("Failed to load user dashboard data", task.getException()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateUI(DashboardData data) {
        if (completionPercentageLabel == null || profileProgressBar == null || statsGrid == null ||
                recommendedJobsContainer == null || recentApplicationsContainer == null) {
            logger.error("Dashboard components not fully injected. Possible FXML load issues.");
            return;
        }

        // Update Profile Card
        completionPercentageLabel.setText(data.completion + "%");
        profileProgressBar.setProgress(data.completion / 100.0);

        if (data.completion == 100) {
            completeProfileBtn.setVisible(false);
            completeProfileBtn.setManaged(false);
        }

        // Update Stats
        statsGrid.getChildren().clear();
        statsGrid.getChildren().addAll(
                createStatCard(I18n.get("dashboard.stat.applications"), String.valueOf(data.appCount),
                        I18n.get("common.total"), true),
                createStatCard(I18n.get("dashboard.stat.interviews"), String.valueOf(data.interviewCount),
                        I18n.get("dashboard.stat.scheduled"), data.interviewCount > 0),
                createStatCard(I18n.get("dashboard.stat.match_score"), data.score + "%",
                        I18n.get("dashboard.stat.talent_score"), true),
                createStatCard(I18n.get("dashboard.stat.profile_views"), "24", "+2 " + I18n.get("common.this_week"),
                        true));

        // ── Upcoming interviews countdown section ─────────────────────────────
        if (statsGrid != null && statsGrid.getParent() instanceof VBox) {
            VBox page = (VBox) statsGrid.getParent();
            page.getChildren().removeIf(n -> "upcoming-interviews-section".equals(n.getId()));

            if (!data.upcomingInterviews.isEmpty()) {
                VBox section = buildCandidateInterviewsSection(data.upcomingInterviews);
                section.setId("upcoming-interviews-section");
                int idx = page.getChildren().indexOf(statsGrid);
                page.getChildren().add(idx + 1, section);
            }
        }

        // Update Recommendations
        recommendedJobsContainer.getChildren().clear();
        if (data.recommendations == null || data.recommendations.isEmpty()) {
            Label noJobs = new Label("Aucune recommandation pour le moment. Complétez votre profil !");
            noJobs.getStyleClass().add("text-muted");
            recommendedJobsContainer.getChildren().add(noJobs);
        } else {
            for (JobOffer job : data.recommendations) {
                recommendedJobsContainer.getChildren().add(createJobRecommendationCard(job));
            }
        }

        // Update Recent Applications
        recentApplicationsContainer.getChildren().clear();
        if (data.recentApps == null || data.recentApps.isEmpty()) {
            Label noApps = new Label("Vous n'avez pas encore postulé à des offres.");
            noApps.getStyleClass().add("text-muted");
            recentApplicationsContainer.getChildren().add(noApps);
        } else {
            int count = 0;
            for (Application app : data.recentApps) {
                recentApplicationsContainer.getChildren().add(createApplicationItem(app));
                if (++count >= 5)
                    break;
            }
        }
    }

    /** Opens the CV Generator as a modal window. */
    private void openCvGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/skilora/view/usermanagement/CvGeneratorView.fxml"));
            Parent root = loader.load();

            CvGeneratorController controller = loader.getController();
            if (controller != null) {
                controller.setup(currentUser);
            }

            Stage cvStage = new Stage();
            cvStage.setTitle("Créer mon CV — Skilora");
            cvStage.initModality(Modality.WINDOW_MODAL);

            // Try to get the owner window
            if (quickActionsContainer.getScene() != null) {
                cvStage.initOwner(quickActionsContainer.getScene().getWindow());
            }

            Scene scene = new Scene(root, 820, 680);
            com.skilora.framework.utils.WindowConfig.configureScene(scene);
            cvStage.setScene(scene);
            cvStage.setMinWidth(700);
            cvStage.setMinHeight(520);
            cvStage.show();

        } catch (Exception e) {
            logger.error("Failed to open CV Generator", e);
            com.skilora.utils.DialogUtils.showError("Erreur", "Impossible d'ouvrir le générateur de CV : " + e.getMessage());
        }
    }

    /**
     * Builds a styled "Prochains entretiens" section with live countdown badges
     * for each scheduled interview.  Shows at most 3 interviews.
     */
    private VBox buildCandidateInterviewsSection(List<Interview> interviews) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 0, 8, 0));

        // Section title
        Label title = new Label("📅 Vos prochains entretiens");
        title.getStyleClass().add("form-title");
        section.getChildren().add(title);

        int limit = Math.min(interviews.size(), 3);
        for (int i = 0; i < limit; i++) {
            Interview iv = interviews.get(i);
            section.getChildren().add(buildInterviewRow(iv));
        }

        if (interviews.size() > 3) {
            Label more = new Label("+ " + (interviews.size() - 3) + " autre(s) entretien(s)");
            more.getStyleClass().add("text-muted");
            section.getChildren().add(more);
        }

        return section;
    }

    private HBox buildInterviewRow(Interview iv) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle(
            "-fx-background-color: -fx-muted; -fx-background-radius: 10; " +
            "-fx-border-color: -fx-border; -fx-border-radius: 10; -fx-border-width: 1;");

        // Left: job info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        String jobTitle = iv.getJobTitle() != null ? iv.getJobTitle() : "Poste";
        String company  = iv.getCompanyName() != null ? " · " + iv.getCompanyName() : "";
        Label jobLabel = new Label("💼 " + jobTitle + company);
        jobLabel.getStyleClass().addAll("text-sm");
        jobLabel.setStyle("-fx-font-weight: bold;");

        String dateText = iv.getInterviewDate() != null
                ? "📅 " + iv.getInterviewDate().format(INTERVIEW_DATE_FMT) : "";
        String typeText = iv.getInterviewType() != null
                ? "  🎯 " + iv.getInterviewType().getDisplayName() : "";
        Label metaLabel = new Label(dateText + typeText);
        metaLabel.getStyleClass().add("text-muted");

        info.getChildren().addAll(jobLabel, metaLabel);

        // Right: live countdown
        InterviewCountdownWidget countdown =
                InterviewCountdownWidget.of(iv.getInterviewDate());

        row.getChildren().add(info);
        if (countdown != null) row.getChildren().add(countdown);
        return row;
    }

    private TLButton createQuickAction(String text, String svgPath, Runnable action) {
        TLButton btn = new TLButton(text, ButtonVariant.OUTLINE);
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("svg-path");
        icon.setScaleX(0.9);
        icon.setScaleY(0.9);

        btn.setGraphic(icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);
        if (action != null)
            btn.setOnAction(e -> action.run());
        return btn;
    }

    private Node createStatCard(String title, String value, String trend, boolean positive) {
        TLCard card = new TLCard();
        card.setPrefWidth(220);

        VBox content = new VBox(8);
        TLTypography titleTypo = new TLTypography(title, TLTypography.Variant.SM);
        titleTypo.setMuted(true);

        TLTypography valueTypo = new TLTypography(value, TLTypography.Variant.H2);

        TLTypography trendTypo = new TLTypography(trend, TLTypography.Variant.XS);
        trendTypo.setStyle(positive ? "-fx-text-fill: -fx-success;" : "-fx-text-fill: -fx-muted-foreground;");

        content.getChildren().addAll(titleTypo, valueTypo, trendTypo);
        card.setBody(content);
        return card;
    }

    private Node createJobRecommendationCard(JobOffer job) {
        TLCard card = new TLCard();
        card.getStyleClass().add("job-card");

        VBox content = new VBox(12);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        TLAvatar companyAvatar = new TLAvatar();
        String initials = job.getCompanyName() != null && !job.getCompanyName().isEmpty()
                ? job.getCompanyName().substring(0, 1).toUpperCase()
                : "C";
        companyAvatar.setFallback(initials);
        companyAvatar.setSize(TLAvatar.Size.SM);

        VBox titleBox = new VBox(2);
        TLTypography title = new TLTypography(job.getTitle(), TLTypography.Variant.P);
        title.setBold(true);
        TLTypography company = new TLTypography(job.getCompanyName(), TLTypography.Variant.SM);
        company.setMuted(true);
        titleBox.getChildren().addAll(title, company);

        header.getChildren().addAll(companyAvatar, titleBox);

        HBox meta = new HBox(16);
        Label loc = new Label("\uD83D\uDCCD " + job.getLocation());
        Label sal = new Label("\uD83D\uDCB5 " + job.getSalaryRange());
        loc.getStyleClass().add("text-xs");
        sal.getStyleClass().add("text-xs");
        meta.getChildren().addAll(loc, sal);

        TLButton applyBtn = new TLButton("Voir l'offre", ButtonVariant.GHOST);
        applyBtn.setPrefWidth(Double.MAX_VALUE);
        applyBtn.setOnAction(e -> onNavigateToFeed.run());

        content.getChildren().addAll(header, meta, applyBtn);
        card.setBody(content);
        return card;
    }

    private Node createApplicationItem(Application app) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8;");

        VBox texts = new VBox(4);
        TLTypography jobTitle = new TLTypography(app.getJobTitle(), TLTypography.Variant.SM);
        jobTitle.setBold(true);
        TLTypography companyName = new TLTypography(app.getCompanyName(), TLTypography.Variant.XS);
        companyName.setMuted(true);
        texts.getChildren().addAll(jobTitle, companyName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLBadge statusBadge = new TLBadge(app.getStatus().getDisplayName(), TLBadge.Variant.SECONDARY);
        switch (app.getStatus()) {
            case ACCEPTED:
                statusBadge.setVariant(TLBadge.Variant.SUCCESS);
                break;
            case REJECTED:
                statusBadge.setVariant(TLBadge.Variant.DESTRUCTIVE);
                break;
            case INTERVIEW:
                statusBadge.setVariant(TLBadge.Variant.INFO);
                break;
            default:
                statusBadge.setVariant(TLBadge.Variant.SECONDARY);
                break;
        }

        row.getChildren().addAll(texts, spacer, statusBadge);
        return row;
    }

    // Moved getTimeBasedGreeting to top for better accessibility

    private static class DashboardData {
        final int completion;
        final int appCount;
        final int interviewCount;
        final int score;
        final List<JobOffer> recommendations;
        final List<Application> recentApps;
        final List<Interview> upcomingInterviews;

        DashboardData(int completion, int appCount, int interviewCount, int score,
                List<JobOffer> recommendations, List<Application> recentApps,
                List<Interview> upcomingInterviews) {
            this.completion = completion;
            this.appCount = appCount;
            this.interviewCount = interviewCount;
            this.score = score;
            this.recommendations = recommendations;
            this.recentApps = recentApps;
            this.upcomingInterviews = upcomingInterviews != null ? upcomingInterviews : new java.util.ArrayList<>();
        }
    }
}
