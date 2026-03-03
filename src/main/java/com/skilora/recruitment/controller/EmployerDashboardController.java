package com.skilora.recruitment.controller;

import com.skilora.framework.components.InterviewCountdownWidget;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLTypography;
import com.skilora.utils.I18n;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
import com.skilora.recruitment.service.JobService;
import com.skilora.user.entity.User;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * EmployerDashboardController – Shows stats, upcoming interviews, and quick actions.
 */
public class EmployerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(EmployerDashboardController.class);
    private static final DateTimeFormatter INTERVIEW_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    @FXML private Label greetingLabel;
    @FXML private Label subGreetingLabel;
    @FXML private FlowPane statsGrid;
    @FXML private VBox activityList;
    @FXML private FlowPane quickActionsContainer;

    private User currentUser;
    private final JobService jobService = JobService.getInstance();
    private final ApplicationService appService = ApplicationService.getInstance();
    private final InterviewService interviewService = InterviewService.getInstance();

    private Runnable onNavigateToPostJob;
    private Runnable onNavigateToInbox;

    public void initializeContext(User user, Runnable toProfile, Runnable toPostJob, Runnable toInbox) {
        this.currentUser = user;
        this.onNavigateToPostJob = toPostJob;
        this.onNavigateToInbox = toInbox;

        setupGreeting();
        setupQuickActions();
        loadDashboardData();
    }

    private void setupGreeting() {
        String greeting = getTimeBasedGreeting();
        String name = (currentUser.getFullName() != null)
                ? currentUser.getFullName().split(" ")[0]
                : currentUser.getUsername();

        greetingLabel.setText(greeting + ", " + name + " \uD83D\uDC4B");
        subGreetingLabel.setText("Voici un aperçu de vos activités de recrutement.");
    }

    private void setupQuickActions() {
        quickActionsContainer.getChildren().clear();
        quickActionsContainer.getChildren().addAll(
                createQuickAction("Publier une offre", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", onNavigateToPostJob),
                createQuickAction("Gérer les candidatures",
                        "M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                        onNavigateToInbox));
    }

    private void loadDashboardData() {
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                DashboardData data = new DashboardData();

                // 1. Job Offers
                List<JobOffer> offers = jobService.findJobOffersByCompanyOwner(currentUser.getId());
                data.totalOffers = offers.size();
                data.activeOffers = (int) offers.stream()
                        .filter(o -> o.getStatus() == JobStatus.ACTIVE || o.getStatus() == JobStatus.OPEN)
                        .count();
                data.closedOffers = (int) offers.stream()
                        .filter(o -> o.getStatus() == JobStatus.CLOSED).count();

                // 2. Applications
                List<Application> apps = appService.getApplicationsByCompanyOwner(currentUser.getId());
                data.totalApplications = apps.size();
                data.pendingApps = (int) apps.stream().filter(a -> a.getStatus() == Application.Status.PENDING).count();
                data.acceptedApps = (int) apps.stream().filter(a -> a.getStatus() == Application.Status.ACCEPTED).count();
                data.rejectedApps = (int) apps.stream().filter(a -> a.getStatus() == Application.Status.REJECTED).count();

                // 3. Interviews
                List<Interview> interviews = interviewService.findByEmployer(currentUser.getId());
                data.scheduledInterviews = interviews.size();

                // 4. Upcoming interviews (first 3)
                try {
                    List<Interview> upcoming = interviewService.findUpcomingForUser(currentUser.getId());
                    data.upcomingInterviews = upcoming.size() > 3 ? upcoming.subList(0, 3) : upcoming;
                } catch (Exception ex) {
                    logger.warn("Could not load upcoming interviews: {}", ex.getMessage());
                }

                return data;
            }
        };

        task.setOnSucceeded(e -> updateUI(task.getValue()));
        task.setOnFailed(e -> logger.error("Failed to load dashboard data", task.getException()));

        Thread thread = new Thread(task, "Dashboard-Loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateUI(DashboardData data) {
        statsGrid.getChildren().clear();

        statsGrid.getChildren().addAll(
                createStatCard("Offres Postées", String.valueOf(data.totalOffers),
                        data.activeOffers + " actives, " + data.closedOffers + " closes", true),
                createStatCard("Candidatures", String.valueOf(data.totalApplications),
                        data.pendingApps + " en attente, " + data.rejectedApps + " refusées", false),
                createStatCard("Entretiens", String.valueOf(data.scheduledInterviews),
                        "Prévus cette semaine", true),
                createStatCard("Taux d'Acceptation",
                        data.totalApplications > 0 ? (data.acceptedApps * 100 / data.totalApplications) + "%" : "0%",
                        data.acceptedApps + " candidats acceptés", true));

        // Upcoming interviews section
        activityList.getChildren().clear();

        if (data.upcomingInterviews != null && !data.upcomingInterviews.isEmpty()) {
            Label sectionTitle = new Label("\uD83D\uDCC5 Prochains entretiens");
            sectionTitle.getStyleClass().add("form-title");
            sectionTitle.setPadding(new Insets(0, 0, 4, 0));
            activityList.getChildren().add(sectionTitle);

            for (Interview iv : data.upcomingInterviews) {
                activityList.getChildren().add(buildEmployerInterviewRow(iv));
            }

            Separator sep = new Separator();
            sep.setPadding(new Insets(8, 0, 8, 0));
            activityList.getChildren().add(sep);
        }

        // General activity items
        activityList.getChildren().add(createActivityItem("Système", "Bienvenue sur votre nouveau tableau de bord", "Maintenant"));
        if (data.totalApplications > 0) {
            activityList.getChildren().add(createActivityItem("Candidature",
                    "Vous avez " + data.pendingApps + " candidatures à examiner", "Aujourd'hui"));
        }
    }

    private Node buildEmployerInterviewRow(Interview iv) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 10; "
                + "-fx-border-color: -fx-border; -fx-border-radius: 10; -fx-border-width: 1;");

        // Left: candidate + job info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        String candidate = iv.getCandidateName() != null ? iv.getCandidateName() : "Candidat";
        Label nameLabel = new Label("\uD83D\uDC64 " + candidate);
        nameLabel.setStyle("-fx-font-weight:bold;");

        String jobTitle = iv.getJobTitle() != null ? "\uD83D\uDCBC " + iv.getJobTitle() : "";
        String dateStr = iv.getScheduledDate() != null
                ? "  \uD83D\uDCC5 " + iv.getScheduledDate().format(INTERVIEW_FMT) : "";
        Label metaLabel = new Label(jobTitle + dateStr);
        metaLabel.getStyleClass().add("text-muted");

        info.getChildren().addAll(nameLabel, metaLabel);

        // Right: auto-updating countdown or "passed" label
        row.getChildren().add(info);
        if (iv.getScheduledDate() != null) {
            if (iv.getScheduledDate().isAfter(LocalDateTime.now())) {
                row.getChildren().add(new InterviewCountdownWidget(iv.getScheduledDate()));
            } else {
                Label passed = new Label(I18n.get("interviews.passed"));
                passed.getStyleClass().add("text-muted");
                row.getChildren().add(passed);
            }
        }
        return row;
    }

    private TLButton createQuickAction(String text, String svgPath, Runnable action) {
        TLButton btn = new TLButton(text, TLButton.ButtonVariant.OUTLINE);
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("svg-path");
        btn.setGraphic(icon);
        btn.setGraphicTextGap(10);
        if (action != null) btn.setOnAction(e -> action.run());
        return btn;
    }

    private Node createStatCard(String title, String value, String subText, boolean positive) {
        TLCard card = new TLCard();
        card.setPrefWidth(240);
        card.getStyleClass().add("stat-card");

        VBox content = new VBox(8);
        TLTypography titleTypo = new TLTypography(title, TLTypography.Variant.SM);
        titleTypo.setMuted(true);

        TLTypography valueTypo = new TLTypography(value, TLTypography.Variant.H2);
        valueTypo.setStyle("-fx-font-weight: 800;");

        TLTypography subTypo = new TLTypography(subText, TLTypography.Variant.XS);
        subTypo.setMuted(true);

        content.getChildren().addAll(titleTypo, valueTypo, subTypo);
        card.setBody(content);
        return card;
    }

    private Node createActivityItem(String type, String desc, String time) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("activity-item");
        row.setStyle("-fx-border-color: transparent transparent -fx-border transparent; -fx-border-width: 0 0 1 0;");

        VBox texts = new VBox(4);
        TLTypography typeText = new TLTypography(type.toUpperCase(), TLTypography.Variant.XS);
        typeText.setStyle("-fx-font-weight: 800; -fx-text-fill: -fx-primary;");

        TLTypography descText = new TLTypography(desc, TLTypography.Variant.P);
        texts.getChildren().addAll(typeText, descText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLTypography timeText = new TLTypography(time, TLTypography.Variant.XS);
        timeText.setMuted(true);

        row.getChildren().addAll(texts, spacer, timeText);
        return row;
    }

    private String getTimeBasedGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Bonjour";
        if (hour >= 12 && hour < 18) return "Bon après-midi";
        return "Bonsoir";
    }

    private static class DashboardData {
        int totalOffers;
        int activeOffers;
        int closedOffers;
        int totalApplications;
        int pendingApps;
        int acceptedApps;
        int rejectedApps;
        int scheduledInterviews;
        List<Interview> upcomingInterviews = new ArrayList<>();
    }
}
