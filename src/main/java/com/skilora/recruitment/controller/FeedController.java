package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.MatchingService;
import com.skilora.recruitment.ui.JobCard;
import com.skilora.user.entity.Profile;
import com.skilora.user.service.ProfileService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTabs;
import com.skilora.framework.components.TLTextField;
import com.skilora.user.entity.User;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import com.skilora.utils.UiUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * FeedController - FXML Controller for FeedView.fxml
 *
 * Handles all user interactions and job feed loading logic.
 * Uses background Task for data loading to keep UI responsive.
 */
public class FeedController implements Initializable {

    @FXML
    private Label feedTitle;
    @FXML
    private Label feedSubtitle;
    @FXML
    private FlowPane grid;
    @FXML
    private VBox tabsBox;
    @FXML
    private TLButton refreshBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private ScrollPane scrollPane;

    private TLTabs tagTabs;
    private final JobService jobService;
    private final MatchingService matchingService;
    private final ProfileService profileService;

    private List<JobOpportunity> allJobs;
    private List<JobOpportunity> currentFilteredJobs;
    private boolean isLoadingMore = false;

    /** Pre-built user skill corpus for efficient batch scoring (null if unavailable). */
    private Set<String> candidateCorpus;

    private java.util.function.Consumer<JobOpportunity> onJobClick;
    private User currentUser;

    // Pagination — show more cards initially and per batch for better UX
    private static final int CARDS_PER_PAGE = 40;
    private int currentPage = 0;

    // Predefined keywords for tags
    private static final String[] KEYWORDS = { "Remote", "Java", "Python", "Marketing", "Design", "DevOps", "Full-time",
            "Intern" };

    // Cache for lowercase job fields to avoid repeated toLowerCase() in filtering
    private Map<JobOpportunity, String[]> lowerCaseCache;

    // Filter requests sequence (to drop stale async results)
    private final AtomicLong filterSeq = new AtomicLong(0);

    // Sort & additional filter state
    private String currentSort = "recent"; // "recent", "a-z", "z-a"
    private String currentLocationFilter = null; // null = all
    private Label resultCountLabel;

    public FeedController() {
        this.jobService = JobService.getInstance();
        this.matchingService = MatchingService.getInstance();
        this.profileService = ProfileService.getInstance();
    }

    public void setOnJobClick(java.util.function.Consumer<JobOpportunity> onJobClick) {
        this.onJobClick = onJobClick;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        if (grid != null) {
            // FlowPane caching can cause "blank but clickable" artifacts with dynamic content.
            grid.setCache(false);
            // Make cards responsive: listen to grid width and recalculate card sizes
            grid.widthProperty().addListener((obs, oldW, newW) -> updateCardWidths(newW.doubleValue()));
        }
        if (scrollPane != null) {
            // Smooth scroll setup: disable fit-to-width quirks, set pannable for touch
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setCache(true);
            scrollPane.setCacheHint(javafx.scene.CacheHint.SPEED);
        }
        setupEventHandlers();
        loadDataAsync();
    }

    private void applyI18n() {
        if (feedTitle != null) feedTitle.setText(I18n.get("feed.title"));
        if (feedSubtitle != null) feedSubtitle.setText(I18n.get("feed.subtitle"));
        if (refreshBtn != null) refreshBtn.setText(I18n.get("feed.refresh"));
        if (searchField != null) {
            searchField.setLabel(I18n.get("feed.search.label"));
            searchField.setPromptText(I18n.get("feed.search.prompt"));
        }
    }

    private void setupEventHandlers() {
        // Debounce Search
        if (searchField != null && searchField.getControl() != null) {
            UiUtils.debounce(searchField.getControl(), 300, () -> {
                String currentTag = getCurrentTagText();
                requestFilter(currentTag, searchField.getText());
            });
        }

        // Infinite scroll — trigger pre-fetch early for seamless loading
        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.7 && !isLoadingMore && hasMoreCards()) {
                    loadMoreCardsQuietly();
                }
            });
        }
    }

    @FXML
    private void handleRefresh() {
        if (refreshBtn == null)
            return;

        refreshBtn.setDisable(true);
        refreshBtn.setText(I18n.get("feed.updating"));

        // Manual refresh validates links for cleaner data (slower, but more correct).
        jobService.refreshFeed(true, () -> {
            Platform.runLater(() -> {
                loadDataAsync();
                refreshBtn.setText(I18n.get("feed.refresh"));
                refreshBtn.setDisable(false);
            });
        });
    }

    /**
     * Load data asynchronously using javafx.concurrent.Task.
     * Keeps the UI thread free during data fetching.
     */
    private void loadDataAsync() {
        if (grid != null) {
            grid.getChildren().clear();
            grid.getChildren().add(new TLLoadingState());
        }

        Task<FeedData> loadTask = new Task<>() {
            @Override
            protected FeedData call() {
                List<JobOpportunity> jobs = jobService.getJobsFromCache();
                Map<JobOpportunity, String[]> cache = buildLowerCaseCache(jobs);

                // Build candidate corpus + enrich match scores (no UI calls)
                Set<String> corpus = null;
                if (currentUser != null) {
                    try {
                        Profile profile = profileService.findProfileByUserId(currentUser.getId());
                        if (profile != null) {
                            corpus = matchingService.buildCandidateCorpus(profile.getId());
                            if (corpus != null && !corpus.isEmpty()) {
                                for (JobOpportunity job : jobs) {
                                    int score = matchingService.scoreFromCorpus(
                                        corpus, job.getTitle(), job.getDescription(),
                                        job.getSkills());
                                    job.setMatchPercentage(score);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // Profile not found or DB error — skip enrichment
                    }
                }
                return new FeedData(jobs, cache, corpus);
            }
        };

        loadTask.setOnSucceeded(event -> {
            FeedData data = loadTask.getValue();
            allJobs = data.jobs;
            lowerCaseCache = data.lowerCaseCache;
            candidateCorpus = data.corpus;
            refreshTags();
            setupFilterBar();
            requestFilter("All", "");
        });

        loadTask.setOnFailed(event -> {
            allJobs = List.of();
            if (grid != null) {
                grid.getChildren().clear();
                grid.getChildren().add(new TLEmptyState(
                    SvgIcons.BRIEFCASE,
                    I18n.get("common.error"),
                    I18n.get("feed.load_error")));
            }
        });

        AppThreadPool.execute(loadTask);
    }

    /**
     * Pre-compute lowercase versions of searchable fields to avoid
     * repeated String allocation during filtering.
     */
    private Map<JobOpportunity, String[]> buildLowerCaseCache(List<JobOpportunity> jobs) {
        if (jobs == null) return new HashMap<>();
        Map<JobOpportunity, String[]> cache = new HashMap<>(jobs.size());
        for (JobOpportunity job : jobs) {
            String[] fields = new String[] {
                job.getTitle() != null ? job.getTitle().toLowerCase() : "",
                job.getLocation() != null ? job.getLocation().toLowerCase() : "",
                job.getDescription() != null ? job.getDescription().toLowerCase() : "",
                job.getSource() != null ? job.getSource().toLowerCase() : ""
            };
            cache.put(job, fields);
        }
        return cache;
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
                String[] fields = lowerCacheFor(job);
                if (fields != null) {
                    boolean matches = fields[0].contains(lowerKey) ||
                            fields[1].contains(lowerKey) ||
                            fields[2].contains(lowerKey);
                    if (matches) c++;
                }
            }
            if (c > 0)
                counts.put(key, c);
        }

        // Clear existing tabs and rebuild
        tabsBox.getChildren().clear();
        tagTabs = new TLTabs();

        // Add 'All' first
        tagTabs.addTab("All", "All (" + counts.get("All") + ")", (javafx.scene.Node) null);

        // Add keyword tabs with counts
        for (String key : KEYWORDS) {
            if (counts.containsKey(key)) {
                tagTabs.addTab(key, key + " (" + counts.get(key) + ")", (javafx.scene.Node) null);
            }
        }

        tagTabs.setOnTabChanged(tabId -> {
            requestFilter(tabId, searchField != null ? searchField.getText() : "");
        });

        tabsBox.getChildren().add(tagTabs);
    }

    /**
     * Build a secondary filter bar with sort + location dropdown + result count.
     */
    private void setupFilterBar() {
        if (tabsBox == null || allJobs == null) return;

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 0, 0, 0));

        // Sort selector
        TLSelect<String> sortSelect = new TLSelect<>(I18n.get("feed.sort"));
        sortSelect.getItems().addAll(I18n.get("feed.sort.recent"), I18n.get("feed.sort.az"), I18n.get("feed.sort.za"));
        // Add "Best Match" sort only if match scores are available
        if (candidateCorpus != null && !candidateCorpus.isEmpty()) {
            sortSelect.getItems().add(I18n.get("feed.sort.match"));
        }
        sortSelect.setValue(I18n.get("feed.sort.recent"));
        sortSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (I18n.get("feed.sort.az").equals(newVal)) currentSort = "a-z";
            else if (I18n.get("feed.sort.za").equals(newVal)) currentSort = "z-a";
            else if (I18n.get("feed.sort.match").equals(newVal)) currentSort = "match";
            else currentSort = "recent";
            requestFilter(getCurrentTagText(), searchField != null ? searchField.getText() : "");
        });

        // Location filter (dynamically populated from data)
        TLSelect<String> locationSelect = new TLSelect<>(I18n.get("feed.filter.location"));
        Set<String> locations = new TreeSet<>();
        locations.add(I18n.get("support.admin.filter.all"));
        for (JobOpportunity job : allJobs) {
            if (job.getLocation() != null && !job.getLocation().isBlank()) {
                locations.add(job.getLocation().trim());
            }
        }
        locationSelect.getItems().addAll(locations);
        locationSelect.setValue(I18n.get("support.admin.filter.all"));
        locationSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (I18n.get("support.admin.filter.all").equals(newVal)) {
                currentLocationFilter = null;
            } else {
                currentLocationFilter = newVal;
            }
            requestFilter(getCurrentTagText(), searchField != null ? searchField.getText() : "");
        });

        // Result count
        resultCountLabel = new Label("");
        resultCountLabel.getStyleClass().addAll("text-sm", "text-muted");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        filterBar.getChildren().addAll(sortSelect, locationSelect, spacer, resultCountLabel);
        tabsBox.getChildren().add(filterBar);
    }

    private String[] lowerCacheFor(JobOpportunity job) {
        return lowerCaseCache != null ? lowerCaseCache.get(job) : null;
    }

    private String getCurrentTagText() {
        if (tagTabs != null && tagTabs.getActiveTabId() != null) {
            return tagTabs.getActiveTabId();
        }
        return "All";
    }

    private void requestFilter(String tagRaw, String query) {
        if (allJobs == null) return;
        final long seq = filterSeq.incrementAndGet();

        AppThreadPool.execute(() -> {
            List<JobOpportunity> filtered = computeFilteredJobs(tagRaw, query);
            Platform.runLater(() -> {
                if (seq != filterSeq.get()) return; // stale result
                currentFilteredJobs = filtered;
                populateGrid(filtered);
            });
        });
    }

    private List<JobOpportunity> computeFilteredJobs(String tagRaw, String query) {
        if (allJobs == null) return List.of();

        String tag = tagRaw;
        if (tagRaw != null && tagRaw.contains(" (") && tagRaw.endsWith(")")) {
            tag = tagRaw.substring(0, tagRaw.lastIndexOf(" ("));
        }
        if (tag == null) tag = "All";

        String lowerQuery = query == null ? "" : query.toLowerCase();
        String finalTag = tag.toLowerCase();
        boolean isAll = tag.equals("All");
        String locFilter = currentLocationFilter != null ? currentLocationFilter.toLowerCase() : null;

        List<JobOpportunity> result = allJobs.stream()
                .filter(j -> {
                    String[] fields = lowerCaseCache != null ? lowerCaseCache.get(j) : null;
                    String title = fields != null ? fields[0] : (j.getTitle() != null ? j.getTitle().toLowerCase() : "");
                    String location = fields != null ? fields[1] : (j.getLocation() != null ? j.getLocation().toLowerCase() : "");
                    String desc = fields != null ? fields[2] : (j.getDescription() != null ? j.getDescription().toLowerCase() : "");
                    String source = fields != null ? fields[3] : (j.getSource() != null ? j.getSource().toLowerCase() : "");

                    boolean matchesSearch = lowerQuery.isEmpty() ||
                            title.contains(lowerQuery) ||
                            source.contains(lowerQuery) ||
                            desc.contains(lowerQuery);

                    boolean matchesTag = isAll ||
                            title.contains(finalTag) ||
                            desc.contains(finalTag) ||
                            location.contains(finalTag);

                    boolean matchesLocation = locFilter == null || location.contains(locFilter);

                    return matchesSearch && matchesTag && matchesLocation;
                })
                .collect(Collectors.toList());

        // Apply sort
        switch (currentSort) {
            case "a-z" -> result.sort(Comparator.comparing(
                    j -> j.getTitle() != null ? j.getTitle().toLowerCase() : "", Comparator.naturalOrder()));
            case "z-a" -> result.sort(Comparator.comparing(
                    (JobOpportunity j) -> j.getTitle() != null ? j.getTitle().toLowerCase() : "").reversed());
            case "match" -> result.sort(Comparator.comparingInt(JobOpportunity::getMatchPercentage).reversed());
            default -> {} // "recent" is default order from source
        }

        return result;
    }

    private void populateGrid(List<JobOpportunity> jobs) {
        if (grid == null)
            return;

        grid.getChildren().clear();
        currentPage = 0;

        // Update result count
        if (resultCountLabel != null) {
            resultCountLabel.setText(jobs.size() + " " + I18n.get("feed.results"));
        }

        if (jobs.isEmpty()) {
            grid.getChildren().add(new TLEmptyState(
                SvgIcons.BRIEFCASE,
                I18n.get("feed.no_results"),
                I18n.get("feed.no_results.desc")));
            return;
        }

        int endIdx = Math.min(CARDS_PER_PAGE, jobs.size());
        for (int i = 0; i < endIdx; i++) {
            grid.getChildren().add(new JobCard(jobs.get(i), onJobClick, currentUser != null ? currentUser.getId() : null));
        }

        // Size cards to fill grid evenly
        updateCardWidths(grid.getWidth());

        // Add "Load More" button if there are more jobs
        if (jobs.size() > CARDS_PER_PAGE) {
            int remaining = jobs.size() - CARDS_PER_PAGE;
            addLoadMoreButton(I18n.get("feed.load_more").replace("{0}", String.valueOf(remaining)), this::loadMoreCards);
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
            grid.getChildren().add(new JobCard(currentFilteredJobs.get(i), onJobClick, currentUser != null ? currentUser.getId() : null));
        }

        int remaining = currentFilteredJobs.size() - endIdx;
        if (remaining > 0) {
            addLoadMoreButton(I18n.get("feed.load_more").replace("{0}", String.valueOf(remaining)), this::loadMoreCards);
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
            // Keep cards clickable even when loaded via infinite scroll
            grid.getChildren().add(new JobCard(
                currentFilteredJobs.get(i),
                onJobClick,
                currentUser != null ? currentUser.getId() : null
            ));
        }

        // Size newly added cards
        updateCardWidths(grid.getWidth());

        isLoadingMore = false;
    }

    private static final class FeedData {
        private final List<JobOpportunity> jobs;
        private final Map<JobOpportunity, String[]> lowerCaseCache;
        private final Set<String> corpus;

        private FeedData(List<JobOpportunity> jobs, Map<JobOpportunity, String[]> lowerCaseCache, Set<String> corpus) {
            this.jobs = jobs != null ? jobs : List.of();
            this.lowerCaseCache = lowerCaseCache != null ? lowerCaseCache : new HashMap<>();
            this.corpus = corpus;
        }
    }

    /**
     * Dynamically resize job cards to fill the grid width evenly.
     * Calculates how many columns fit and distributes remaining space.
     */
    private void updateCardWidths(double gridWidth) {
        if (gridWidth <= 0 || grid == null) return;
        double gap = grid.getHgap();
        double minCard = 280;
        double maxCard = 420;
        int cols = Math.max(1, (int) ((gridWidth + gap) / (minCard + gap)));
        double cardWidth = (gridWidth - (cols - 1) * gap) / cols;
        cardWidth = Math.min(cardWidth, maxCard);
        cardWidth = Math.max(cardWidth, minCard);
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof JobCard card) {
                card.setPrefWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
        }
    }
}
