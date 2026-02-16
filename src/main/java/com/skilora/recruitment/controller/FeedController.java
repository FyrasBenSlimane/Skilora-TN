package com.skilora.recruitment.controller;

import com.skilora.model.entity.JobOpportunity;
import com.skilora.model.service.JobService;
import com.skilora.ui.JobCard;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * FeedController - FXML Controller for FeedView.fxml
 *
 * Handles all user interactions and job feed loading logic.
 * Uses background Task for data loading to keep UI responsive.
 */
public class FeedController implements Initializable {

    @FXML
    private FlowPane grid;
    @FXML
    private HBox tabsBox;
    @FXML
    private TLButton refreshBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private ScrollPane scrollPane;

    private ToggleGroup tagGroup = new ToggleGroup();
    private final JobService jobService;

    private List<JobOpportunity> allJobs;
    private List<JobOpportunity> currentFilteredJobs;
    private boolean isLoadingMore = false;

    private java.util.function.Consumer<JobOpportunity> onJobClick;

    // Pagination
    private static final int CARDS_PER_PAGE = 20;
    private int currentPage = 0;

    // Predefined keywords for tags
    private static final String[] KEYWORDS = { "Remote", "Java", "Python", "Marketing", "Design", "DevOps", "Full-time",
            "Intern" };

    // Cache for lowercase job fields to avoid repeated toLowerCase() in filtering
    private Map<JobOpportunity, String[]> lowerCaseCache;

    public FeedController() {
        this.jobService = JobService.getInstance();
    }

    public void setOnJobClick(java.util.function.Consumer<JobOpportunity> onJobClick) {
        this.onJobClick = onJobClick;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEventHandlers();
        loadDataAsync();
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

        // Infinite scroll
        if (scrollPane != null) {
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.8 && !isLoadingMore && hasMoreCards()) {
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
     */
    private void loadDataAsync() {
        Task<List<JobOpportunity>> loadTask = new Task<>() {
            @Override
            protected List<JobOpportunity> call() {
                return jobService.getJobsFromCache();
            }
        };

        loadTask.setOnSucceeded(event -> {
            allJobs = loadTask.getValue();
            buildLowerCaseCache();
            refreshTags();
            filterJobsByTag("All", "");
        });

        loadTask.setOnFailed(event -> {
            // Fallback to empty state
            allJobs = List.of();
        });

        Thread thread = new Thread(loadTask, "FeedLoader");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Pre-compute lowercase versions of searchable fields to avoid
     * repeated String allocation during filtering.
     */
    private void buildLowerCaseCache() {
        if (allJobs == null) return;
        lowerCaseCache = new HashMap<>(allJobs.size());
        for (JobOpportunity job : allJobs) {
            String[] fields = new String[] {
                job.getTitle() != null ? job.getTitle().toLowerCase() : "",
                job.getLocation() != null ? job.getLocation().toLowerCase() : "",
                job.getDescription() != null ? job.getDescription().toLowerCase() : "",
                job.getSource() != null ? job.getSource().toLowerCase() : ""
            };
            lowerCaseCache.put(job, fields);
        }
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
                            fields[2].contains(lowerKey);
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
        if (allJobs == null)
            return;

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

                    return matchesSearch && matchesTag;
                })
                .collect(Collectors.toList());

        populateGrid(currentFilteredJobs);
    }

    private void populateGrid(List<JobOpportunity> jobs) {
        if (grid == null)
            return;

        grid.getChildren().clear();
        currentPage = 0;

        int endIdx = Math.min(CARDS_PER_PAGE, jobs.size());
        for (int i = 0; i < endIdx; i++) {
            grid.getChildren().add(new JobCard(jobs.get(i), onJobClick));
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
            grid.getChildren().add(new JobCard(currentFilteredJobs.get(i), onJobClick));
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
            grid.getChildren().add(new JobCard(currentFilteredJobs.get(i)));
        }

        isLoadingMore = false;
    }
}

