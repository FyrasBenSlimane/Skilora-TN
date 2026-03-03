package com.skilora.recruitment.controller;

import com.skilora.framework.components.InterviewCountdownWidget;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
import com.skilora.recruitment.service.RecruitmentIntelligenceService;
import com.skilora.user.entity.User;
import com.skilora.user.service.ProfileService;
import com.skilora.utils.I18n;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

/**
 * ApplicationsController - Grid-based candidate applications view with AI scoring.
 */
public class ApplicationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsController.class);

    @FXML private Label statsLabel;
    @FXML private TLButton refreshBtn;
    @FXML private FlowPane applicationsList;
    @FXML private ScrollPane scrollPane;

    private User currentUser;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final InterviewService interviewService = InterviewService.getInstance();
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();
    private boolean isLoading = false;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_SHORT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final double CARD_GAP = 14;
    private static final double CARD_MIN_WIDTH = 340;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Dynamically resize cards when container width changes
        if (scrollPane != null) {
            scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                if (newBounds != null && applicationsList != null) {
                    resizeCards(newBounds.getWidth());
                }
            });
        }
    }

    /** Recompute card widths so they fill rows evenly. */
    private void resizeCards(double containerWidth) {
        if (containerWidth <= 0) return;
        double usable = containerWidth - 8; // padding
        int cols = Math.max(1, (int) ((usable + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP)));
        double cardWidth = (usable - (cols - 1) * CARD_GAP) / cols;
        for (var node : applicationsList.getChildren()) {
            if (node instanceof VBox) {
                ((VBox) node).setPrefWidth(cardWidth);
                ((VBox) node).setMaxWidth(cardWidth);
            }
        }
    }

    public void setCurrentUser(User user) {
        if (this.currentUser != null && user != null && this.currentUser.getId() == user.getId()) {
            loadApplications();
            return;
        }
        this.currentUser = user;
        loadApplications();
    }

    private void loadApplications() {
        if (isLoading) return;
        isLoading = true;
        if (applicationsList != null) applicationsList.getChildren().clear();
        if (currentUser == null) {
            isLoading = false;
            if (statsLabel != null) statsLabel.setText(I18n.get("applications.error"));
            return;
        }
        if (statsLabel != null) statsLabel.setText(I18n.get("common.loading"));

        // Loading spinner
        if (applicationsList != null) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(36, 36);
            HBox spinnerBox = new HBox(spinner);
            spinnerBox.setAlignment(Pos.CENTER);
            spinnerBox.setPadding(new Insets(40));
            spinnerBox.setPrefWidth(600);
            applicationsList.getChildren().add(spinnerBox);
        }

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                var profile = ProfileService.getInstance().findProfileByUserId(currentUser.getId());
                if (profile == null) return List.of();
                List<Application> apps = applicationService.getApplicationsByProfile(profile.getId());
                for (Application app : apps) {
                    try {
                        if (app.getMatchPercentage() <= 0) {
                            int match = intelligenceService.calculateCompatibility(
                                    app.getCandidateProfileId(), app.getJobOfferId()).join();
                            app.setMatchPercentage(match);
                        }
                        if (app.getCandidateScore() <= 0) {
                            int score = intelligenceService.scoreCandidate(
                                    app.getCandidateProfileId()).join();
                            app.setCandidateScore(score);
                        }
                    } catch (Exception ex) {
                        logger.warn("AI score failed for app {}: {}", app.getId(), ex.getMessage());
                    }
                }
                apps.sort((a, b) -> Integer.compare(
                        (b.getMatchPercentage() + b.getCandidateScore()),
                        (a.getMatchPercentage() + a.getCandidateScore())));
                return apps;
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;
            List<Application> apps = task.getValue();
            if (applicationsList != null) {
                applicationsList.getChildren().clear();
                if (apps != null && !apps.isEmpty()) {
                    int idx = 0;
                    for (Application app : apps) {
                        Interview interview = null;
                        if (app.getStatus() == Application.Status.ACCEPTED || app.getStatus() == Application.Status.INTERVIEW) {
                            try {
                                var list = interviewService.findByApplication(app.getId());
                                if (list != null && !list.isEmpty()) interview = list.get(0);
                            } catch (Exception ex) {
                                logger.error("Interview load error for app {}", app.getId(), ex);
                            }
                        }
                        VBox card = createCard(app,
                                app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()),
                                interview);
                        // Stagger animation
                        card.setOpacity(0);
                        card.setTranslateY(16);
                        FadeTransition ft = new FadeTransition(Duration.millis(250), card);
                        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(idx * 50));
                        TranslateTransition tt = new TranslateTransition(Duration.millis(250), card);
                        tt.setFromY(16); tt.setToY(0); tt.setDelay(Duration.millis(idx * 50));
                        applicationsList.getChildren().add(card);
                        ft.play(); tt.play();
                        idx++;
                    }
                    // Trigger initial sizing
                    if (scrollPane != null && scrollPane.getViewportBounds() != null) {
                        resizeCards(scrollPane.getViewportBounds().getWidth());
                    }
                } else {
                    applicationsList.getChildren().add(createEmptyState());
                }
            }
            updateCounts(apps != null ? apps.size() : 0);
        });

        task.setOnFailed(e -> {
            isLoading = false;
            if (statsLabel != null) statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
        });

        Thread t = new Thread(task, "LoadApplications");
        t.setDaemon(true);
        t.start();
    }

    private void updateCounts(int total) {
        if (statsLabel != null) statsLabel.setText(I18n.get("applications.count", total));
    }

    // ════════════════════════════════════════════════════════════════════
    //  CARD — Self-contained tile with border, accent, scores, info
    // ════════════════════════════════════════════════════════════════════

    private VBox createCard(Application app, String title, Interview interview) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("app-card", getStatusCardClass(app.getStatus()));
        card.setMinWidth(CARD_MIN_WIDTH);

        // ── Force inline styles — guarantees border/bg even if CSS fails ──
        String accentColor = getStatusAccentColor(app.getStatus());
        boolean light = card.getScene() != null
                && card.getScene().getRoot() != null
                && card.getScene().getRoot().getStyleClass().contains("light");
        String bg      = light ? "#ffffff"  : "#18181b";
        String border  = light ? "#e4e4e7"  : "#2a2b2e";
        String shadow  = light ? "rgba(0,0,0,0.06)" : "rgba(0,0,0,0.10)";
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + border + " " + border + " " + border + " " + accentColor + ";" +
            "-fx-border-width: 1 1 1 3;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, " + shadow + ", 8, 0, 0, 2);"
        );

        // ── Top section: accent + header ──
        VBox topSection = new VBox(10);
        topSection.getStyleClass().add("app-card-top");
        topSection.setPadding(new Insets(16, 16, 12, 16));

        // Row 1: badge + delete
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        TLBadge statusBadge = createStatusBadge(app.getStatus());
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        Label dateLabel = new Label(formatTimeAgo(app.getAppliedDate()));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #a1a1aa;");
        TLButton deleteBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
        deleteBtn.setText("✕");
        deleteBtn.getStyleClass().add("app-card-delete-btn");
        deleteBtn.setStyle("-fx-text-fill: #71717a; -fx-font-size: 13px; -fx-min-width: 28; -fx-max-width: 28; -fx-min-height: 28; -fx-max-height: 28; -fx-padding: 0; -fx-cursor: hand;");
        deleteBtn.setTooltip(new Tooltip(I18n.get("applications.delete_tooltip", "Supprimer")));
        deleteBtn.setOnAction(e -> handleDeleteApplication(app.getId()));
        topBar.getChildren().addAll(dateLabel, topSpacer, statusBadge, deleteBtn);
        topSection.getChildren().add(topBar);

        // Row 2: Job title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #fafafa;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        topSection.getChildren().add(titleLabel);

        // Row 3: company + location
        Label subtitleLabel = new Label(buildSubtitle(app));
        subtitleLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #a1a1aa;");
        topSection.getChildren().add(subtitleLabel);

        card.getChildren().add(topSection);

        // ── Divider ──
        Region divider = new Region();
        divider.setStyle("-fx-background-color: #2a2b2e;");
        divider.setMinHeight(1); divider.setMaxHeight(1);
        card.getChildren().add(divider);

        // ── Bottom section: scores + date ──
        HBox bottomSection = new HBox(12);
        bottomSection.getStyleClass().add("app-card-bottom");
        bottomSection.setPadding(new Insets(12, 16, 14, 16));
        bottomSection.setAlignment(Pos.CENTER);

        // Match score
        VBox matchBlock = createScoreBlock(app.getMatchPercentage(), "AI Match", getMatchColor(app.getMatchPercentage()));
        // Score block
        VBox scoreBlock = createScoreBlock(app.getCandidateScore(), "Score", getScoreColor(app.getCandidateScore()));

        Region midSpacer = new Region();
        HBox.setHgrow(midSpacer, Priority.ALWAYS);

        // Applied date
        VBox dateBlock = new VBox(2);
        dateBlock.setAlignment(Pos.CENTER_RIGHT);
        Label appliedLabel = new Label("Postulé le");
        appliedLabel.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #71717a;");
        Label appliedDate = new Label(formatAppliedDate(app.getAppliedDate()));
        appliedDate.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #e4e4e7;");
        dateBlock.getChildren().addAll(appliedLabel, appliedDate);

        bottomSection.getChildren().addAll(matchBlock, scoreBlock, midSpacer, dateBlock);
        card.getChildren().add(bottomSection);

        // ── Interview section ──
        if (interview != null && interview.getScheduledDate() != null) {
            Region div2 = new Region();
            div2.setStyle("-fx-background-color: #2a2b2e;");
            div2.setMinHeight(1); div2.setMaxHeight(1);
            card.getChildren().add(div2);

            HBox interviewRow = new HBox(8);
            interviewRow.setStyle("-fx-background-color: #1e1e22; -fx-background-radius: 0 0 12 12;");
            interviewRow.setPadding(new Insets(10, 16, 12, 16));
            interviewRow.setAlignment(Pos.CENTER_LEFT);

            Label interviewIcon = new Label("🎯");
            interviewIcon.setStyle("-fx-font-size: 15px;");

            VBox interviewInfo = new VBox(2);
            Label schedLabel = new Label(interview.getScheduledDate().format(DATE_TIME_FMT));
            schedLabel.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #fafafa;");
            HBox interviewTags = new HBox(6);
            if (interview.getLocation() != null && !interview.getLocation().isEmpty()) {
                Label loc = new Label("📍 " + interview.getLocation());
                loc.setStyle("-fx-font-size: 11px; -fx-text-fill: #a1a1aa;");
                interviewTags.getChildren().add(loc);
            }
            if (interview.getType() != null && !interview.getType().isEmpty()) {
                Label type = new Label(interview.getType());
                type.setStyle("-fx-font-size: 11px; -fx-text-fill: #a1a1aa;");
                interviewTags.getChildren().add(type);
            }
            interviewInfo.getChildren().add(schedLabel);
            if (!interviewTags.getChildren().isEmpty()) interviewInfo.getChildren().add(interviewTags);

            Region iSpacer = new Region();
            HBox.setHgrow(iSpacer, Priority.ALWAYS);

            interviewRow.getChildren().addAll(interviewIcon, interviewInfo, iSpacer);

            InterviewCountdownWidget countdown = InterviewCountdownWidget.of(interview.getScheduledDate());
            if (countdown != null) interviewRow.getChildren().add(countdown);

            card.getChildren().add(interviewRow);
        }

        return card;
    }

    // ──────── Score block (ring + label) ────────

    private VBox createScoreBlock(int value, String label, String color) {
        VBox block = new VBox(3);
        block.setAlignment(Pos.CENTER);
        block.setMinWidth(56);

        StackPane ring = createScoreRing(value, color);
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a1a1aa; -fx-font-weight: 600;");

        block.getChildren().addAll(ring, nameLabel);
        return block;
    }

    private StackPane createScoreRing(int value, String color) {
        double size = 48;
        double stroke = 3.5;
        double radius = (size - stroke) / 2;

        StackPane ring = new StackPane();
        ring.setMinSize(size, size);
        ring.setMaxSize(size, size);

        Circle bg = new Circle(radius);
        bg.setStyle("-fx-fill: transparent; -fx-stroke: #3f3f46; -fx-stroke-width: " + stroke + ";");

        if (value > 0) {
            double angle = (value / 100.0) * 360.0;
            Arc arc = new Arc(0, 0, radius, radius, 90, -angle);
            arc.setType(ArcType.OPEN);
            arc.setStyle("-fx-fill: transparent; -fx-stroke: " + color + "; -fx-stroke-width: " + (stroke + 0.5)
                    + "; -fx-stroke-line-cap: round;");

            Label val = new Label(value + "%");
            val.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            ring.getChildren().addAll(bg, arc, val);
        } else {
            Label dash = new Label("—");
            dash.setStyle("-fx-font-size: 13px; -fx-text-fill: #71717a;");
            ring.getChildren().addAll(bg, dash);
        }
        return ring;
    }

    // ──────── Empty state ────────

    private VBox createEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 24, 60, 24));
        box.setStyle("-fx-background-color: #18181b; -fx-background-radius: 12; -fx-border-color: #2a2b2e; -fx-border-width: 1; -fx-border-radius: 12;");
        box.setPrefWidth(600);

        Label emptyIcon = new Label("📭");
        emptyIcon.setStyle("-fx-font-size: 42px;");
        Label emptyTitle = new Label("Aucune candidature");
        emptyTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #fafafa;");
        Label emptyDesc = new Label("Explorez les offres et postulez pour commencer.");
        emptyDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #a1a1aa;");

        box.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
        return box;
    }

    // ──────── Status helpers ────────

    private TLBadge createStatusBadge(Application.Status status) {
        String text; TLBadge.Variant variant;
        switch (status) {
            case PENDING:    text = I18n.get("inbox.status.pending");   variant = TLBadge.Variant.SECONDARY;    break;
            case REVIEWING:  text = I18n.get("inbox.status.review");    variant = TLBadge.Variant.SUCCESS;      break;
            case INTERVIEW:  text = I18n.get("inbox.status.interview"); variant = TLBadge.Variant.INFO;         break;
            case OFFER:      text = I18n.get("inbox.status.offer");     variant = TLBadge.Variant.SUCCESS;      break;
            case ACCEPTED:   text = I18n.get("inbox.status.accepted");  variant = TLBadge.Variant.SUCCESS;      break;
            case REJECTED:   text = I18n.get("inbox.status.rejected");  variant = TLBadge.Variant.DESTRUCTIVE;  break;
            default:         text = I18n.get("inbox.status.pending");   variant = TLBadge.Variant.DEFAULT;      break;
        }
        return new TLBadge(text, variant);
    }

    private String getStatusCardClass(Application.Status status) {
        switch (status) {
            case PENDING:    return "app-card-pending";
            case REVIEWING:  return "app-card-reviewing";
            case INTERVIEW:  return "app-card-interview";
            case OFFER:      return "app-card-offer";
            case ACCEPTED:   return "app-card-accepted";
            case REJECTED:   return "app-card-rejected";
            default:         return "app-card-pending";
        }
    }

    /** Left-border accent color per status (hex). */
    private String getStatusAccentColor(Application.Status status) {
        switch (status) {
            case PENDING:    return "#71717a";
            case REVIEWING:  return "#3b82f6";
            case INTERVIEW:  return "#8b5cf6";
            case OFFER:      return "#f59e0b";
            case ACCEPTED:   return "#22c55e";
            case REJECTED:   return "#ef4444";
            default:         return "#71717a";
        }
    }

    // ──────── Color helpers ────────

    private String getMatchColor(int pct) {
        if (pct >= 80) return "#22c55e";
        if (pct >= 60) return "#3b82f6";
        if (pct >= 40) return "#f59e0b";
        if (pct > 0)   return "#ef4444";
        return "#71717a";
    }

    private String getScoreColor(int score) {
        if (score >= 75) return "#22c55e";
        if (score >= 50) return "#3b82f6";
        if (score >= 25) return "#f59e0b";
        if (score > 0)   return "#ef4444";
        return "#71717a";
    }

    private String buildSubtitle(Application app) {
        StringBuilder sb = new StringBuilder();
        if (app.getCompanyName() != null && !app.getCompanyName().isEmpty()) sb.append(app.getCompanyName());
        if (app.getJobLocation() != null && !app.getJobLocation().isEmpty()) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(app.getJobLocation());
        }
        if (sb.length() == 0) sb.append("Offre #" + app.getJobOfferId());
        return sb.toString();
    }

    private String formatAppliedDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_SHORT_FMT) : "";
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "";
        long days = ChronoUnit.DAYS.between(dt, LocalDateTime.now());
        if (days == 0) return "Aujourd'hui";
        if (days == 1) return "Hier";
        if (days < 7)  return "Il y a " + days + "j";
        if (days < 30) return "Il y a " + (days / 7) + " sem.";
        if (days < 365) return "Il y a " + (days / 30) + " mois";
        return "Il y a " + (days / 365) + " an(s)";
    }

    @FXML
    private void handleRefresh() {
        if (currentUser != null) loadApplications();
    }

    public void refreshIfNeeded() {
        if (currentUser != null) loadApplications();
    }

    private void handleDeleteApplication(int applicationId) {
        java.util.Optional<javafx.scene.control.ButtonType> result = com.skilora.utils.DialogUtils.showConfirmation(
            I18n.get("applications.delete_confirm_title", "Supprimer la candidature"),
            I18n.get("applications.delete_confirm_message", "Êtes-vous sûr de vouloir supprimer cette candidature ?")
        );
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) return;

        Task<Boolean> deleteTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return applicationService.delete(applicationId);
            }
        };
        deleteTask.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(deleteTask.getValue())) {
                com.skilora.utils.DialogUtils.showInfo(
                    I18n.get("applications.delete_success_title", "Candidature supprimée"),
                    I18n.get("applications.delete_success_message", "Votre candidature a été supprimée.")
                );
                loadApplications();
            } else {
                com.skilora.utils.DialogUtils.showError(
                    I18n.get("applications.delete_error_title", "Erreur"),
                    I18n.get("applications.delete_error_message", "Impossible de supprimer la candidature.")
                );
            }
        });
        deleteTask.setOnFailed(e -> {
            logger.error("Failed to delete application {}", applicationId, deleteTask.getException());
            com.skilora.utils.DialogUtils.showError(
                I18n.get("applications.delete_error_title", "Erreur"),
                I18n.get("applications.delete_error_message", "Une erreur est survenue.")
            );
        });
        Thread t = new Thread(deleteTask, "DeleteApplication");
        t.setDaemon(true);
        t.start();
    }
}
