package com.skilora.ui;

import com.skilora.model.entity.recruitment.JobOpportunity;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class JobCard extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(JobCard.class);

    private final JobOpportunity job;

    public JobCard(JobOpportunity job) {
        this(job, null, null);
    }

    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick) {
        this(job, onCardClick, null);
    }

    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick, Consumer<JobOpportunity> onJobClick) {
        this.job = job;
        getStyleClass().add("job-card");
        setPadding(new Insets(20));
        setSpacing(16);
        setPrefWidth(300);
        setMinWidth(300);
        setMaxWidth(300);
        setVisible(true);
        setOpacity(1.0);
        setDisable(false);
        
        // Make card clickable - use CSS :hover instead of inline setStyle()
        if (onCardClick != null) {
            getStyleClass().add("job-card-clickable");
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    onCardClick.accept(job);
                }
            });
        }

        // Header
        VBox header = new VBox(4);
        
        // Title row with closed badge if applicable
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-card-title");
        title.setWrapText(true);
        title.setVisible(true);
        title.setOpacity(1.0);
        title.setStyle("-fx-text-fill: -fx-foreground; -fx-opacity: 1.0;");
        titleRow.getChildren().add(title);

        if (job.isRecommended()) {
            Label recoBadge = new Label("⭐ Recommandée");
            recoBadge.getStyleClass().add("job-card-badge");
            recoBadge.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 6; -fx-font-size: 12px;");
            titleRow.getChildren().add(recoBadge);
        }
        
        // Show "Fermée" badge next to title if closed (more visible)
        String jobStatus = job.getStatus();
        // Debug: Always log the status to see what we're getting
        if (jobStatus != null) {
            System.out.println("DEBUG JobCard - Job ID: " + job.getId() + ", Title: " + job.getTitle() + ", Status: '" + jobStatus + "'");
        } else {
            System.out.println("DEBUG JobCard - Job ID: " + job.getId() + ", Title: " + job.getTitle() + ", Status: NULL");
        }
        // Check for CLOSED status (case-insensitive for safety)
        boolean isClosed = jobStatus != null && ("CLOSED".equalsIgnoreCase(jobStatus));
        if (isClosed) {
            System.out.println("DEBUG JobCard - Job is CLOSED! Adding badge for ID: " + job.getId());
            Label closedBadge = new Label("🔒 FERMÉE");
            closedBadge.getStyleClass().add("job-card-badge");
            // Use explicit red color to ensure visibility
            closedBadge.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 6; -fx-font-size: 12px;");
            titleRow.getChildren().add(closedBadge);
            logger.info("JobCard - Added 'FERMÉE' badge for closed job ID: {}, Status: {}", job.getId(), jobStatus);
        } else {
            System.out.println("DEBUG JobCard - Job is NOT closed. Status: '" + jobStatus + "'");
        }
        
        header.getChildren().add(titleRow);

        Label source = new Label(job.getSource());
        source.getStyleClass().add("job-card-source");
        source.setVisible(true);
        source.setOpacity(1.0);
        source.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-opacity: 1.0;");
        header.getChildren().add(source);

        // Tags / Metadata (FlowPane so badges wrap and are not truncated)
        FlowPane meta = new FlowPane();
        meta.setHgap(8);
        meta.setVgap(6);
        meta.setAlignment(Pos.CENTER_LEFT);

        // Location Badge
        Label locLabel = createBadge(job.getLocation() != null && !job.getLocation().isEmpty() ? job.getLocation() : "Remote");
        meta.getChildren().add(locLabel);
        
        // Work Type Badge (if available)
        if (job.getType() != null && !job.getType().isEmpty()) {
            meta.getChildren().add(createBadge(job.getType()));
        }
        
        // Posted Date Badge
        if (job.getPostedDate() != null && !job.getPostedDate().isEmpty()) {
            meta.getChildren().add(createBadge(job.getPostedDate()));
        }

        // Description (Truncated for card view, full in details). Sanitize garbage/placeholder text.
        String rawDesc = job.getDescription();
        String descText = "";
        if (rawDesc != null && !rawDesc.isEmpty() && !isGarbageDescription(rawDesc)) {
            String clean = rawDesc.replace("\n", " ").trim();
            descText = clean.length() > 120 ? clean.substring(0, 120).trim() + "..." : clean;
        }
        if (descText.isEmpty()) {
            descText = "Aucune description fournie.";
        }
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.getStyleClass().add("job-card-desc");
        desc.setMinHeight(40);
        desc.setVisible(true);
        desc.setOpacity(1.0);
        desc.setStyle("-fx-text-fill: -fx-foreground; -fx-opacity: 1.0;");
        
        // Add salary info if available (for database offers)
        if (job.getSalaryInfo() != null && !job.getSalaryInfo().isEmpty()) {
            Label salaryLabel = new Label("💰 " + job.getSalaryInfo());
            salaryLabel.getStyleClass().add("job-card-badge");
            salaryLabel.setStyle("-fx-background-color: -fx-success; -fx-text-fill: -fx-success-foreground;");
            meta.getChildren().add(salaryLabel);
        }

        if (job.getMatchPercentage() > 0) {
            Label matchLabel = new Label("Match: " + job.getMatchPercentage() + "%");
            matchLabel.getStyleClass().add("job-card-badge");
            matchLabel.setStyle("-fx-background-color: #0f766e; -fx-text-fill: white;");
            meta.getChildren().add(matchLabel);
        }

        // Footer (View Details / Apply Button)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER);
        
        if (onCardClick != null) {
            TLButton viewBtn = new TLButton("View Details", ButtonVariant.OUTLINE);
            viewBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(viewBtn, Priority.ALWAYS);
            viewBtn.setOnAction(e -> {
                e.consume(); // Prevent card click
                onCardClick.accept(job);
            });
            buttonRow.getChildren().add(viewBtn);
        }

        // Only show Apply button if onJobClick callback is provided (for opening application dialog)
        // But disable it if the offer is closed
        if (onJobClick != null) {
            String status = job.getStatus();
            System.out.println("DEBUG JobCard Button - Job ID: " + job.getId() + ", Status for button: '" + status + "'");
            boolean isClosedStatus = status != null && "CLOSED".equalsIgnoreCase(status);
            TLButton applyBtn;
            if (isClosedStatus) {
                System.out.println("DEBUG JobCard Button - Creating FERMÉE button for closed job ID: " + job.getId());
                // Show "Fermée" in red for closed offers
                applyBtn = new TLButton("FERMÉE", ButtonVariant.DANGER);
                applyBtn.setDisable(true);
                applyBtn.setOpacity(1.0); // Keep it visible even when disabled
                // Force red color even when disabled
                applyBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-opacity: 1.0;");
                logger.info("JobCard - Created disabled 'FERMÉE' button for closed job ID: {}, Status: {}", job.getId(), status);
            } else {
                applyBtn = new TLButton("Apply", ButtonVariant.PRIMARY);
                applyBtn.setOnAction(e -> {
                    e.consume(); // Prevent card click
                    onJobClick.accept(job);
                });
            }
            applyBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(applyBtn, Priority.ALWAYS);
            buttonRow.getChildren().add(applyBtn);
        }

        getChildren().addAll(header, meta, desc, spacer, buttonRow);
    }

    /** Treats as garbage: null, empty, or strings that look like placeholders (e.g. random consonants). */
    private static boolean isGarbageDescription(String s) {
        if (s == null || s.length() < 3) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        // No spaces and very few vowels => likely placeholder/garbage
        int vowels = 0;
        int letters = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = Character.toLowerCase(t.charAt(i));
            if (Character.isLetter(c)) {
                letters++;
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y') vowels++;
            }
        }
        if (letters < 10) return false; // short strings might be valid
        return vowels < letters / 4; // e.g. "kjgzlrkgjzklgjrzlkgjzrlkgrzgzg" has almost no vowels
    }

    private Label createBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("job-card-badge");
        badge.setVisible(true);
        badge.setOpacity(1.0);
        badge.setStyle("-fx-text-fill: -fx-foreground; -fx-opacity: 1.0;");
        return badge;
    }

    public JobOpportunity getJob() {
        return job;
    }
}
