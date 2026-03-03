package com.skilora.recruitment.ui;

import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.entity.SavedJob;
import com.skilora.recruitment.service.SavedJobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class JobCard extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(JobCard.class);

    /** In-memory bookmarked job URLs for this session (fallback when userId is null). */
    private static final Set<String> bookmarkedJobs = new HashSet<>();

    // Common tech skills for badge extraction
    private static final String[] SKILL_KEYWORDS = {
        "java", "python", "javascript", "react", "angular", "vue", "node",
        "spring", "docker", "kubernetes", "aws", "azure", "sql", "mongodb",
        "typescript", "go", "rust", "c++", "swift", "kotlin", "flutter",
        "machine learning", "ai", "devops", "linux", "git", "agile", "scrum"
    };

    private final JobOpportunity job;
    private final Integer userId;
    private final Consumer<JobOpportunity> onCardClick;
    private final Consumer<JobOpportunity> onApplyClick;
    private final SavedJobService savedJobService;
    private ToggleButton bookmarkBtn;

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public JobCard(JobOpportunity job) {
        this(job, null, null, null);
    }

    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick) {
        this(job, onCardClick, null, null);
    }

    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick, Integer userId) {
        this(job, onCardClick, userId, null);
    }

    /** Same as src 2: optional apply callback to open application dialog instead of external URL. */
    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick, Integer userId, Consumer<JobOpportunity> onApplyClick) {
        this.job = job;
        this.userId = userId;
        this.onCardClick = onCardClick;
        this.onApplyClick = onApplyClick;
        this.savedJobService = SavedJobService.getInstance();
        getStyleClass().add("job-card");
        setPadding(new Insets(18));
        setSpacing(10);
        setPrefWidth(340);
        setMinWidth(260);
        setMaxWidth(Double.MAX_VALUE);
        
        // Make card clickable. Filter MOUSE_PRESSED (not CLICKED) so we run before ListView selection;
        // consuming press prevents the list from selecting the cell (which can hide/obscure text on first click).
        if (onCardClick != null) {
            getStyleClass().add("job-card-clickable");
            setCursor(Cursor.HAND);
            addEventFilter(MOUSE_PRESSED, this::handleCardClick);
        }

        // ── Company avatar + title row ──
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.TOP_LEFT);

        // Company initial — prefer company field, fallback to source
        String companyForAvatar = (job != null && job.getCompany() != null && !job.getCompany().isBlank())
                ? job.getCompany() : (job != null ? job.getSource() : null);
        String companyInitial = extractInitial(companyForAvatar);
        Label avatarLabel = new Label(companyInitial);
        avatarLabel.setMinSize(40, 40);
        avatarLabel.setMaxSize(40, 40);
        avatarLabel.setPrefSize(40, 40);
        avatarLabel.setAlignment(Pos.CENTER);
        avatarLabel.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 20; " +
                "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 16; -fx-font-weight: bold;");

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        String safeTitle = job != null && job.getTitle() != null ? job.getTitle() : I18n.get("jobdetails.not_specified");
        Label title = new Label(safeTitle);
        title.getStyleClass().add("job-card-title");
        title.setWrapText(true);

        // Company name label — prefer company field, fallback to source
        String safeSource = job != null
                ? (job.getCompany() != null && !job.getCompany().isBlank() ? job.getCompany()
                   : (job.getSource() != null ? job.getSource() : ""))
                : "";
        Label source = new Label(safeSource);
        source.getStyleClass().add("job-card-source");

        titleBox.getChildren().addAll(title, source);

        // Bookmark toggle
        bookmarkBtn = new ToggleButton();
        bookmarkBtn.getStyleClass().add("bookmark-toggle");
        bookmarkBtn.setSelected(isSavedNow());
        updateBookmarkIcon();
        bookmarkBtn.setOnAction(e -> {
            e.consume();
            toggleSaved(bookmarkBtn.isSelected());
            updateBookmarkIcon();
        });

        headerRow.getChildren().addAll(avatarLabel, titleBox, bookmarkBtn);

        // ── "New" badge row (if recent) ──
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        if (job != null && isRecentlyPosted(job.getPostedDate(), 3)) {
            Label newBadge = new Label("New");
            newBadge.getStyleClass().add("badge-new");
            badgeRow.getChildren().add(newBadge);
        }

        // Work type indicator
        String workType = inferWorkType(job);
        if (!workType.isEmpty()) {
            Label workTypeBadge = new Label(workType);
            workTypeBadge.getStyleClass().add("job-card-badge");
            if ("Remote".equalsIgnoreCase(workType)) {
                workTypeBadge.setStyle(workTypeBadge.getStyle() + "-fx-text-fill: #22c55e;");
            }
            badgeRow.getChildren().add(workTypeBadge);
        }

        // Tags / Metadata — location + posted date
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);

        String safeLocation = job != null && job.getLocation() != null ? job.getLocation().trim() : "";
        String safePosted = job != null && job.getPostedDate() != null ? job.getPostedDate().trim() : "";

        if (!safeLocation.isEmpty()) {
            Label locLabel = createBadge("📍 " + safeLocation);
            meta.getChildren().add(locLabel);
        }
        String formattedDate = formatPostedDate(safePosted);
        if (!formattedDate.isEmpty()) {
            Label dateLabel = createBadge(formattedDate);
            meta.getChildren().add(dateLabel);
        }

        // Skills preview (max 3 tags)
        HBox skillsRow = new HBox(6);
        skillsRow.setAlignment(Pos.CENTER_LEFT);
        extractSkills(job, skillsRow, 3);

        // Description (Truncated to 120 chars)
        String safeDesc = job != null && job.getDescription() != null ? job.getDescription() : "";
        String normalizedDesc = safeDesc.replace("\n", " ").trim();
        String descText = normalizedDesc.length() > 120
                ? normalizedDesc.substring(0, 120) + "..."
                : normalizedDesc;
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.getStyleClass().add("job-card-desc");
        desc.setMinHeight(40);

        // Footer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER);
        
        if (onCardClick != null) {
            TLButton viewBtn = new TLButton(I18n.get("jobcard.view_details"), ButtonVariant.OUTLINE);
            viewBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(viewBtn, Priority.ALWAYS);
            viewBtn.setOnAction(e -> {
                e.consume();
                onCardClick.accept(job);
            });
            buttonRow.getChildren().add(viewBtn);
        }

        boolean isClosed = job != null && "CLOSED".equalsIgnoreCase(job.getStatus());
        TLButton applyBtn;
        if (onApplyClick != null && !isClosed) {
            applyBtn = new TLButton(I18n.get("feed.apply", "Postuler"), ButtonVariant.PRIMARY);
            applyBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(applyBtn, Priority.ALWAYS);
            applyBtn.setOnAction(e -> {
                e.consume();
                onApplyClick.accept(job);
            });
        } else if (onApplyClick != null && isClosed) {
            applyBtn = new TLButton(I18n.get("feed.closed", "Fermée"), ButtonVariant.DANGER);
            applyBtn.setMaxWidth(Double.MAX_VALUE);
            applyBtn.setDisable(true);
            HBox.setHgrow(applyBtn, Priority.ALWAYS);
        } else {
            applyBtn = new TLButton(I18n.get("feed.open_listing"), ButtonVariant.PRIMARY);
            applyBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(applyBtn, Priority.ALWAYS);
            applyBtn.setOnAction(e -> {
                e.consume();
                try {
                    if (Desktop.isDesktopSupported()) {
                        String targetUrl = job != null && job.getApplyUrl() != null && !job.getApplyUrl().isBlank()
                                ? job.getApplyUrl()
                                : (job != null ? job.getUrl() : null);
                        if (targetUrl != null && !targetUrl.isBlank()) {
                            Desktop.getDesktop().browse(new URI(targetUrl));
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Failed to open job URL", ex);
                }
            });
        }
        buttonRow.getChildren().add(applyBtn);

        getChildren().add(headerRow);
        if (!badgeRow.getChildren().isEmpty()) getChildren().add(badgeRow);
        getChildren().addAll(meta);
        if (!skillsRow.getChildren().isEmpty()) getChildren().add(skillsRow);
        getChildren().addAll(desc, spacer, buttonRow);
    }

    /** Capture-phase handler on press so we run before ListCell/ListView selection. */
    private void handleCardClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || e.isConsumed()) return;
        javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
        // Don't open details when pressing on buttons/links
        if (target instanceof javafx.scene.control.ButtonBase
                || target instanceof javafx.scene.control.TextInputControl
                || target instanceof javafx.scene.control.Hyperlink) return;
        e.consume();
        if (onCardClick != null) onCardClick.accept(job);
    }

    private String extractInitial(String source) {
        if (source == null || source.isBlank()) return "?";
        return source.substring(0, 1).toUpperCase();
    }

    private String inferWorkType(JobOpportunity job) {
        if (job == null) return "";
        String combined = ((job.getTitle() != null ? job.getTitle() : "") + " " +
                (job.getLocation() != null ? job.getLocation() : "") + " " +
                (job.getDescription() != null ? job.getDescription() : "")).toLowerCase();
        if (combined.contains("remote") || combined.contains("télétravail") || combined.contains("\u0639\u0646 \u0628\u0639\u062f")) return "Remote";
        if (combined.contains("hybrid") || combined.contains("hybride")) return "Hybrid";
        if (combined.contains("on-site") || combined.contains("on site") || combined.contains("présentiel")) return "On-site";
        return "";
    }

    private void extractSkills(JobOpportunity job, HBox container, int max) {
        if (job == null) return;
        String combined = ((job.getTitle() != null ? job.getTitle() : "") + " " +
                (job.getDescription() != null ? job.getDescription() : "")).toLowerCase();
        int count = 0;
        for (String skill : SKILL_KEYWORDS) {
            if (count >= max) break;
            if (combined.contains(skill)) {
                Label skillBadge = new Label(skill.substring(0, 1).toUpperCase() + skill.substring(1));
                skillBadge.getStyleClass().add("job-card-badge");
                skillBadge.setStyle("-fx-text-fill: -fx-primary; -fx-border-color: -fx-primary; -fx-border-radius: 8; -fx-background-radius: 8;");
                container.getChildren().add(skillBadge);
                count++;
            }
        }
    }

    private Label createBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("job-card-badge");
        return badge;
    }

    public JobOpportunity getJob() {
        return job;
    }

    /** Checks if the posted date string (ISO format) is within the given number of days. */
    private boolean isRecentlyPosted(String dateStr, int withinDays) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            LocalDate posted = LocalDate.parse(dateStr);
            return !posted.isBefore(LocalDate.now().minusDays(withinDays));
        } catch (DateTimeParseException e) {
            return false; // unparsable date — not "new"
        }
    }

    /** Formats an ISO date string to a human-friendly label. */
    private String formatPostedDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            LocalDate posted = LocalDate.parse(dateStr);
            long days = ChronoUnit.DAYS.between(posted, LocalDate.now());
            if (days == 0) return "Aujourd'hui";
            if (days == 1) return "Hier";
            if (days < 7)  return "Il y a " + days + "j";
            if (days < 30) return "Il y a " + (days / 7) + " sem.";
            if (days < 365) return "Il y a " + (days / 30) + " mois";
            return posted.format(DATE_DISPLAY);
        } catch (DateTimeParseException e) {
            return dateStr; // fallback: show raw
        }
    }

    /** Updates the bookmark button icon based on selected state. */
    private void updateBookmarkIcon() {
        SVGPath icon = bookmarkBtn.isSelected()
                ? SvgIcons.filledIcon(SvgIcons.BOOKMARK, 14, "-fx-primary")
                : SvgIcons.icon(SvgIcons.BOOKMARK, 14, "-fx-muted-foreground");
        bookmarkBtn.setGraphic(icon);
    }

    private boolean isSavedNow() {
        String url = job != null ? job.getUrl() : null;
        if (url == null || url.isBlank()) return false;
        if (userId != null && userId > 0) {
            return savedJobService.isSaved(userId, url);
        }
        return bookmarkedJobs.contains(url);
    }

    private void toggleSaved(boolean shouldSave) {
        String url = job != null ? job.getUrl() : null;
        if (url == null || url.isBlank()) return;

        if (userId != null && userId > 0) {
            if (shouldSave) {
                SavedJob s = new SavedJob();
                s.setUserId(userId);
                s.setJobUrl(url);
                s.setJobTitle(job != null ? job.getTitle() : null);
                s.setCompanyName(job != null ? job.getSource() : null);
                s.setLocation(job != null ? job.getLocation() : null);
                s.setSource(job != null ? job.getSource() : null);
                savedJobService.save(s);
            } else {
                savedJobService.unsave(userId, url);
            }
            return;
        }

        if (shouldSave) bookmarkedJobs.add(url);
        else bookmarkedJobs.remove(url);
    }
}
