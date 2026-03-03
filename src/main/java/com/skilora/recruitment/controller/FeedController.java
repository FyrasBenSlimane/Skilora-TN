package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.RecruitmentIntelligenceService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.recruitment.ui.JobCard;
import com.skilora.user.entity.User;

/**
 * FeedController - FXML Controller for FeedView.fxml
 *
 * Handles all user interactions and job feed loading logic.
 * Uses background Task for data loading to keep UI responsive.
 */
public class FeedController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);

    @FXML
    private FlowPane grid;
    @FXML
    private FlowPane tabsBox;
    @FXML
    private TLButton refreshBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private ScrollPane scrollPane;

    private ToggleGroup tagGroup = new ToggleGroup();
    private final JobService jobService = JobService.getInstance();

    private List<JobOpportunity> allJobs;
    private List<JobOpportunity> currentFilteredJobs;
    private boolean isLoadingMore = false;
    private Task<List<JobOpportunity>> currentLoadTask = null;
    private long lastLoadMoreTime = 0;
    private static final long LOAD_MORE_THROTTLE_MS = 400;

    private java.util.function.Consumer<JobOpportunity> onJobClick;
    private java.util.function.Consumer<JobOpportunity> onApplyClick;
    private User currentUser;

    // Pagination (smaller first page for snappier open and smooth scroll)
    private static final int CARDS_PER_PAGE = 12;
    private int currentPage = 0;

    // Predefined keywords for tags
    private static final String[] KEYWORDS = { "Remote", "Java", "Python", "Marketing", "Design", "DevOps", "Full-time",
            "Intern" };

    // Cache for lowercase job fields to avoid repeated toLowerCase() in filtering
    private Map<JobOpportunity, String[]> lowerCaseCache;

    public FeedController() {
    }

    public void setOnJobClick(java.util.function.Consumer<JobOpportunity> onJobClick) {
        this.onJobClick = onJobClick;
    }

    public void setOnApplyClick(java.util.function.Consumer<JobOpportunity> onApplyClick) {
        this.onApplyClick = onApplyClick;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Public method to reload data when returning to the feed view.
     * This ensures fresh data and proper UI state after navigating away.
     */
    public void reloadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) currentLoadTask.cancel();
        allJobs = null;
        currentFilteredJobs = null;
        lowerCaseCache = null;
        currentPage = 0;
        if (grid != null) grid.getChildren().clear();
        loadDataAsync();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        allJobs = null;
        currentFilteredJobs = null;
        lowerCaseCache = null;
        currentPage = 0;
        isLoadingMore = false;
        if (grid != null) grid.getChildren().clear();
        // Bind grid width so cards fill available width properly
        if (scrollPane != null && grid != null) {
            grid.prefWidthProperty().bind(scrollPane.widthProperty());
            grid.minWidthProperty().bind(scrollPane.widthProperty());
        }
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Debounce Search
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> {
            String currentTag = getCurrentTagText();
            filterJobsByTag(currentTag, searchField.getText());
        });

        if (searchField != null && searchField.getControl() != null) {
            searchField.getControl().setOnKeyReleased(e -> pause.playFromStart());
        }

        // Infinite scroll (throttled to avoid lag)
        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() < 0.8 || isLoadingMore || !hasMoreCards()) return;
                long now = System.currentTimeMillis();
                if (now - lastLoadMoreTime < LOAD_MORE_THROTTLE_MS) return;
                lastLoadMoreTime = now;
                loadMoreCardsQuietly();
            });
        }
    }

    @FXML
    private void handleRefresh() {
        if (refreshBtn == null)
            return;

        refreshBtn.setDisable(true);
        refreshBtn.setText("Updating...");

        jobService.refreshFeed(() -> {
            Platform.runLater(() -> {
                loadDataAsync();
                refreshBtn.setText("Refresh");
                refreshBtn.setDisable(false);
            });
        });
    }

    /**
     * Load data asynchronously using javafx.concurrent.Task.
     * Keeps the UI thread free during data fetching.
     * Now loads from database (ACTIVE/OPEN job offers) instead of cache.
     */
    private void loadDataAsync() {
        // Cancel previous task if still running
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            logger.info("FeedController - Cancelling previous load task before starting new one");
            currentLoadTask.cancel();
        }

        Task<List<JobOpportunity>> loadTask = new Task<>() {
            @Override
            protected List<JobOpportunity> call() {
                try {
                    // Load all job offers including CLOSED (candidates can see closed offers but cannot apply)
                    List<com.skilora.recruitment.entity.JobOffer> offers = jobService.findAllJobOffersForCandidates();

                    logger.info("FeedController - Loaded {} job offers from database", offers.size());

                    // Convert JobOffer to JobOpportunity for display with ALL information
                    List<JobOpportunity> opportunities = new java.util.ArrayList<>();
                    for (com.skilora.recruitment.entity.JobOffer offer : offers) {
                        JobOpportunity opp = new JobOpportunity();
                        opp.setId(offer.getId());
                        opp.setTitle(offer.getTitle() != null ? offer.getTitle() : "");
                        opp.setDescription(offer.getDescription() != null ? offer.getDescription() : "");
                        opp.setLocation(offer.getLocation() != null ? offer.getLocation() : "");
                        opp.setCompany(offer.getCompanyName() != null ? offer.getCompanyName() : "Entreprise");
                        // Store status to check if offer is closed
                        String status = offer.getStatus() != null ? offer.getStatus().name() : "OPEN";
                        opp.setStatus(status);

                        // Format work type properly
                        if (offer.getWorkType() != null && !offer.getWorkType().isEmpty()) {
                            try {
                                com.skilora.recruitment.enums.WorkType workTypeEnum = com.skilora.recruitment.enums.WorkType.valueOf(offer.getWorkType());
                                opp.setType(workTypeEnum.getDisplayName());
                            } catch (IllegalArgumentException e) {
                                opp.setType(offer.getWorkType());
                            }
                        } else {
                            opp.setType("Temps plein");
                        }

                        // Format posted date
                        if (offer.getPostedDate() != null) {
                            opp.setPostedDate(offer.getPostedDate().toString());
                        } else {
                            opp.setPostedDate(java.time.LocalDate.now().toString());
                        }

                        // Store salary information separately for proper display
                        if (offer.getSalaryMin() > 0 || offer.getSalaryMax() > 0) {
                            String salaryInfo = "";
                            if (offer.getSalaryMin() > 0 && offer.getSalaryMax() > 0) {
                                salaryInfo = String.format("%.0f - %.0f %s",
                                    offer.getSalaryMin(),
                                    offer.getSalaryMax(),
                                    offer.getCurrency() != null ? offer.getCurrency() : "TND");
                            } else if (offer.getSalaryMin() > 0) {
                                salaryInfo = String.format("À partir de %.0f %s",
                                    offer.getSalaryMin(),
                                    offer.getCurrency() != null ? offer.getCurrency() : "TND");
                            } else if (offer.getSalaryMax() > 0) {
                                salaryInfo = String.format("Jusqu'à %.0f %s",
                                    offer.getSalaryMax(),
                                    offer.getCurrency() != null ? offer.getCurrency() : "TND");
                            }
                            opp.setSalaryInfo(salaryInfo);
                        }

                        // Company logo or source indicator
                        opp.setSource(offer.getCompanyName() != null ? offer.getCompanyName() : "Skilora");

                        opportunities.add(opp);
                    }

                    logger.info("FeedController - Converted {} offers to opportunities", opportunities.size());
                    return opportunities;
                } catch (Exception e) {
                    logger.error("FeedController - Error loading job offers", e);
                    throw new RuntimeException("Failed to load job offers: " + e.getMessage(), e);
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            currentLoadTask = null;
            allJobs = loadTask.getValue();
            logger.info("FeedController - Loaded {} total jobs", allJobs != null ? allJobs.size() : 0);

            // Enrich with match scores if user is logged in
            if (currentUser != null) {
                enrichWithMatchScores(allJobs);
            }

            buildLowerCaseCache();
            refreshTags();
            // Show all DB jobs first; do not apply search box until user interacts
            filterJobsByTag("All", "");
        });

        loadTask.setOnFailed(e -> {
            currentLoadTask = null;
            logger.error("FeedController - Failed to load jobs", loadTask.getException());
        });

        currentLoadTask = loadTask;
        new Thread(loadTask, "JobFeedLoad").start();
    }

    private void enrichWithMatchScores(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.isEmpty()) return;
        try {
            com.skilora.user.entity.Profile profile = com.skilora.user.service.ProfileService.getInstance()
                    .findProfileByUserId(currentUser.getId());
            if (profile == null) {
                logger.debug("FeedController - no profile found for user {}, skipping match scores", currentUser.getId());
                return;
            }
            RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();
            for (JobOpportunity job : jobs) {
                try {
                    int score = intelligenceService.calculateCompatibility(profile.getId(), job.getId()).join();
                    job.setMatchPercentage(score);
                } catch (Exception e) {
                    logger.debug("FeedController - could not calculate match for job {}: {}", job.getId(), e.getMessage());
                }
            }
            logger.debug("FeedController - match scores computed for {} jobs (profile {})",
                    jobs.size(), profile.getId());
        } catch (Exception e) {
            logger.warn("FeedController - could not enrich with match scores: {}", e.getMessage());
        }
    }

    /**
     * Pre-compute lowercase versions of searchable fields to avoid
     * repeated String allocation during filtering.
     */
    private void buildLowerCaseCache() {
        if (allJobs == null) {
            logger.warn("FeedController - buildLowerCaseCache called but allJobs is null");
            return;
        }
        if (allJobs.isEmpty()) {
            logger.warn("FeedController - buildLowerCaseCache called but allJobs is empty");
            lowerCaseCache = new HashMap<>();
            return;
        }
        lowerCaseCache = new HashMap<>(allJobs.size());
        for (JobOpportunity job : allJobs) {
            String[] fields = new String[] {
                job.getTitle() != null ? job.getTitle().toLowerCase() : "",
                job.getLocation() != null ? job.getLocation().toLowerCase() : "",
                job.getDescription() != null ? job.getDescription().toLowerCase() : "",
                job.getSource() != null ? job.getSource().toLowerCase() : "",
                job.getType() != null ? job.getType().toLowerCase() : ""
            };
            lowerCaseCache.put(job, fields);
        }
        logger.debug("FeedController - Built lowercase cache for {} jobs", allJobs.size());
    }

    private void refreshTags() {
        if (allJobs == null || tabsBox == null)
            return;

        // Calculate counts using cached lowercase fields
        Map<String, Integer> counts = new HashMap<>();
        counts.put("All", allJobs.size());

        for (String key : KEYWORDS) {
            int c = 0;
            String lowerKey = key.toLowerCase();
            for (JobOpportunity job : allJobs) {
                String[] fields = lowerCaseCache.get(job);
                if (fields != null) {
                    boolean matches = fields[0].contains(lowerKey) ||
                            fields[1].contains(lowerKey) ||
                            fields[2].contains(lowerKey) ||
                            (fields.length > 4 && fields[4].contains(lowerKey)) ||
                            (lowerKey.equals("remote") && fields.length > 4 && fields[4].contains("télétravail"));
                    if (matches) c++;
                }
            }
            if (c > 0)
                counts.put(key, c);
        }

        // Clear existing tabs
        tabsBox.getChildren().clear();
        tagGroup = new ToggleGroup();

        // Add 'All' first
        addTagButton("All", "All", true,
                () -> filterJobsByTag("All", searchField != null ? searchField.getText() : ""));

        // Add others
        for (String key : KEYWORDS) {
            if (counts.containsKey(key)) {
                String label = key + " (" + counts.get(key) + ")";
                addTagButton(label, key, false,
                        () -> filterJobsByTag(label, searchField != null ? searchField.getText() : ""));
            }
        }
    }

    private void addTagButton(String label, String rawKey, boolean selected, Runnable onSelect) {
        ToggleButton btn = new ToggleButton(label);
        btn.setUserData(rawKey);
        btn.getStyleClass().add("chip-filter");
        btn.setSelected(selected);
        btn.setToggleGroup(tagGroup);
        // Never truncate the button text – let it use its natural width
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setMaxWidth(Region.USE_PREF_SIZE);
        btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);

        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                onSelect.run();
            } else {
                if (tagGroup.getSelectedToggle() == null) {
                    btn.setSelected(true);
                }
            }
        });

        tabsBox.getChildren().add(btn);
    }

    private String getCurrentTagText() {
        if (tagGroup.getSelectedToggle() != null) {
            return ((ToggleButton) tagGroup.getSelectedToggle()).getText();
        }
        return "All";
    }

    private void filterJobsByTag(String tagRaw, String query) {
        if (allJobs == null) {
            logger.warn("FeedController - filterJobsByTag called but allJobs is null");
            return;
        }

        if (allJobs.isEmpty()) {
            logger.warn("FeedController - filterJobsByTag called but allJobs is empty");
            if (grid != null) {
                grid.getChildren().clear();
            }
            return;
        }

        String tag = tagRaw;
        if (tagRaw != null && tagRaw.contains(" (") && tagRaw.endsWith(")")) {
            tag = tagRaw.substring(0, tagRaw.lastIndexOf(" ("));
        }
        if (tag == null)
            tag = "All";

        String lowerQuery = query == null ? "" : query.toLowerCase();
        String finalTag = tag.toLowerCase();
        boolean isAll = tag.equals("All");

        currentFilteredJobs = allJobs.stream()
                .filter(j -> {
                    String[] fields = lowerCaseCache != null ? lowerCaseCache.get(j) : null;
                    String title = fields != null && fields.length > 0 ? fields[0] : (j.getTitle() != null ? j.getTitle().toLowerCase() : "");
                    String location = fields != null && fields.length > 1 ? fields[1] : (j.getLocation() != null ? j.getLocation().toLowerCase() : "");
                    String desc = fields != null && fields.length > 2 ? fields[2] : (j.getDescription() != null ? j.getDescription().toLowerCase() : "");
                    String source = fields != null && fields.length > 3 ? fields[3] : (j.getSource() != null ? j.getSource().toLowerCase() : "");
                    String type = fields != null && fields.length > 4 ? fields[4] : (j.getType() != null ? j.getType().toLowerCase() : "");

                    boolean matchesSearch = lowerQuery.isEmpty() ||
                            title.contains(lowerQuery) ||
                            source.contains(lowerQuery) ||
                            desc.contains(lowerQuery) ||
                            type.contains(lowerQuery);

                    boolean matchesTag = isAll ||
                            title.contains(finalTag) ||
                            desc.contains(finalTag) ||
                            location.contains(finalTag) ||
                            type.contains(finalTag) ||
                            (finalTag.equals("remote") && type.contains("télétravail"));

                    return matchesSearch && matchesTag;
                })
                .collect(Collectors.toList());

        populateGrid(currentFilteredJobs);
    }

    private void populateGrid(List<JobOpportunity> jobs) {
        if (grid == null) {
            logger.error("FeedController - grid FlowPane is null! Cannot display jobs.");
            return;
        }

        logger.info("FeedController - populateGrid() called with {} jobs", jobs != null ? jobs.size() : 0);

        if (jobs == null || jobs.isEmpty()) {
            logger.warn("FeedController - No jobs to display in grid - clearing grid");
            if (Platform.isFxApplicationThread()) {
                grid.getChildren().clear();
            } else {
                Platform.runLater(() -> {
                    if (grid != null) {
                        grid.getChildren().clear();
                    }
                });
            }
            return;
        }

        // Ensure we're on JavaFX thread - if not, schedule it
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> populateGrid(jobs));
            return;
        }

        grid.getChildren().clear();
        currentPage = 0;

        int endIdx = Math.min(CARDS_PER_PAGE, jobs.size());
        for (int i = 0; i < endIdx; i++) {
            grid.getChildren().add(new JobCard(jobs.get(i), onJobClick, onApplyClick));
        }

        // Add "Load More" button if there are more jobs
        if (jobs.size() > CARDS_PER_PAGE) {
            int remaining = jobs.size() - CARDS_PER_PAGE;
            addLoadMoreButton("Load More (" + remaining + " more)", this::loadMoreCards);
        }
    }

    private void addLoadMoreButton(String text, Runnable onAction) {
        TLButton loadMoreBtn = new TLButton(text, TLButton.ButtonVariant.OUTLINE);
        loadMoreBtn.setMaxWidth(Double.MAX_VALUE);
        loadMoreBtn.setOnAction(e -> onAction.run());

        VBox loadMoreContainer = new VBox(loadMoreBtn);
        loadMoreContainer.setPrefWidth(Double.MAX_VALUE);
        loadMoreContainer.setMaxWidth(Double.MAX_VALUE);
        loadMoreContainer.setPadding(new Insets(16, 0, 0, 0));
        grid.getChildren().add(loadMoreContainer);
    }

    private void removeLoadMoreButton() {
        if (grid != null && !grid.getChildren().isEmpty()) {
            javafx.scene.Node lastNode = grid.getChildren().get(grid.getChildren().size() - 1);
            if (lastNode instanceof VBox) {
                grid.getChildren().remove(lastNode);
            }
        }
    }

    private void loadMoreCards() {
        if (currentFilteredJobs == null)
            return;

        removeLoadMoreButton();

        currentPage++;
        int startIdx = currentPage * CARDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, currentFilteredJobs.size());

        for (int i = startIdx; i < endIdx; i++) {
            grid.getChildren().add(new JobCard(currentFilteredJobs.get(i), onJobClick, onApplyClick));
        }

        int remaining = currentFilteredJobs.size() - endIdx;
        if (remaining > 0) {
            addLoadMoreButton("Load More (" + remaining + " more)", this::loadMoreCards);
        }
    }

    private boolean hasMoreCards() {
        if (currentFilteredJobs == null)
            return false;
        int loadedCount = (currentPage + 1) * CARDS_PER_PAGE;
        return loadedCount < currentFilteredJobs.size();
    }

    private void loadMoreCardsQuietly() {
        if (currentFilteredJobs == null || isLoadingMore)
            return;

        isLoadingMore = true;
        removeLoadMoreButton();

        currentPage++;
        int startIdx = currentPage * CARDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CARDS_PER_PAGE, currentFilteredJobs.size());

        for (int i = startIdx; i < endIdx; i++) {
            grid.getChildren().add(new JobCard(currentFilteredJobs.get(i), onJobClick, onApplyClick));
        }

        isLoadingMore = false;
    }
}
