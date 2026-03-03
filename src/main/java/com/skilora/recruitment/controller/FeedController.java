package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.ui.JobCard;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.user.entity.User;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * FeedController - FXML Controller for FeedView.fxml
 *
 * Uses a virtualized ListView so only visible job cards are created,
 * avoiding lag on scroll. Filtered list is kept in an ObservableList.
 */
public class FeedController implements Initializable {

    @FXML
    private FlowPane tabsBox;
    @FXML
    private TLButton refreshBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private ListView<JobOpportunity> jobList;

    private ToggleGroup tagGroup = new ToggleGroup();
    private final JobService jobService;

    private List<JobOpportunity> allJobs;
    private List<JobOpportunity> currentFilteredJobs;
    private final ObservableList<JobOpportunity> jobItems = FXCollections.observableArrayList();

    private java.util.function.Consumer<JobOpportunity> onJobClick;
    private java.util.function.Consumer<JobOpportunity> onApplyClick;
    private User currentUser;

    /** Fixed cell height for smooth virtualized scrolling (one card per row). */
    private static final int CARD_CELL_HEIGHT = 300;

    // Old implementation tried to cache lowercased fields in a Map<JobOpportunity, String[]>.
    // Because JobOpportunity.equals/hashCode are based only on URL (often null/identical),
    // this collapsed many entries to one and broke filtering. We now compute lowercase fields on the fly.
    private Map<JobOpportunity, String[]> lowerCaseCache;

    // Predefined keywords for tags
    private static final String[] KEYWORDS = { "Remote", "Java", "Python", "Marketing", "Design", "DevOps", "Full-time",
            "Intern" };

    public FeedController() {
        this.jobService = JobService.getInstance();
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

    /** Reload feed when returning to view. */
    public void reloadData() {
        allJobs = null;
        currentFilteredJobs = null;
        lowerCaseCache = null;
        jobItems.clear();
        loadDataAsync();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        allJobs = null;
        currentFilteredJobs = null;
        lowerCaseCache = null;
        jobItems.clear();

        if (jobList != null) {
            jobList.setPlaceholder(new TLLoadingState());
            jobList.setFixedCellSize(CARD_CELL_HEIGHT);
            jobList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(JobOpportunity job, boolean empty) {
                    super.updateItem(job, empty);
                    if (empty || job == null) {
                        setGraphic(null);
                    } else {
                        JobCard card = new JobCard(job, onJobClick,
                                currentUser != null ? currentUser.getId() : null,
                                onApplyClick);
                        HBox.setHgrow(card, Priority.ALWAYS);
                        card.setMaxWidth(Double.MAX_VALUE);
                        setGraphic(card);
                    }
                }
            });
        }

        setupEventHandlers();
        loadDataAsync();
    }

    private void applyI18n() {
        if (refreshBtn != null) refreshBtn.setText(I18n.get("feed.refresh"));
        if (searchField != null) searchField.setPromptText(I18n.get("feed.search.prompt"));
    }

    private void setupEventHandlers() {
        // Filter on every keystroke using the new value from the listener (avoids runLater reading stale/empty)
        if (searchField != null && searchField.getControl() != null) {
            searchField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                String query = (newVal != null ? newVal : "").trim();
                filterJobsByTag(getCurrentRawTag(), query);
            });
        }
    }

    /** Current search text from the control so filtering always uses the latest typed value. */
    private String getSearchQuery() {
        if (searchField == null) return "";
        var c = searchField.getControl();
        if (c != null) {
            String t = c.getText();
            return t != null ? t.trim() : "";
        }
        String t = searchField.getText();
        return t != null ? t.trim() : "";
    }

    @FXML
    private void handleRefresh() {
        if (refreshBtn == null) return;
        refreshBtn.setDisable(true);
        refreshBtn.setText(I18n.get("feed.updating"));
        jobService.refreshFeed(() -> {
            Platform.runLater(() -> {
                loadDataAsync();
                refreshBtn.setText(I18n.get("feed.refresh"));
                refreshBtn.setDisable(false);
            });
        });
    }

    private void loadDataAsync() {
        if (jobList != null) {
            jobList.setItems(FXCollections.emptyObservableList());
            jobList.setPlaceholder(new TLLoadingState());
        }

        Task<List<JobOpportunity>> loadTask = new Task<>() {
            @Override
            protected List<JobOpportunity> call() {
                return loadJobsFromDbOrCache();
            }
        };

        loadTask.setOnSucceeded(event -> {
            allJobs = loadTask.getValue();
            if (allJobs == null || allJobs.isEmpty()) {
                Platform.runLater(() -> {
                    if (jobList != null) {
                        jobList.setItems(FXCollections.emptyObservableList());
                        jobList.setPlaceholder(new TLEmptyState(
                                SvgIcons.BRIEFCASE,
                                I18n.get("common.error"),
                                I18n.get("feed.load_error")));
                    }
                });
                return;
            }
            buildLowerCaseCache();
            refreshTags();
            // Apply filter with current search text (user may have typed before load finished)
            Platform.runLater(() -> filterJobsByTag(getCurrentRawTag().isEmpty() ? "All" : getCurrentRawTag(), getSearchQuery()));
        });

        loadTask.setOnFailed(event -> {
            allJobs = List.of();
            if (jobList != null) {
                jobList.setItems(FXCollections.emptyObservableList());
                jobList.setPlaceholder(new TLEmptyState(
                        SvgIcons.BRIEFCASE,
                        I18n.get("common.error"),
                        I18n.get("feed.load_error")));
            }
        });

        AppThreadPool.execute(loadTask);
    }

    /** Load job offers from DB for candidates; if none, fall back to JSON cache. */
    private List<JobOpportunity> loadJobsFromDbOrCache() {
        try {
            List<JobOffer> offers = jobService.findAllJobOffersForCandidates();
            if (offers != null && !offers.isEmpty()) {
                List<JobOpportunity> list = new java.util.ArrayList<>(offers.size());
                for (JobOffer o : offers) {
                    JobOpportunity opp = new JobOpportunity();
                    opp.setId(o.getId());
                    opp.setTitle(o.getTitle() != null ? o.getTitle() : "");
                    opp.setDescription(o.getDescription() != null ? o.getDescription() : "");
                    opp.setLocation(o.getLocation() != null ? o.getLocation() : "");
                    opp.setCompany(o.getCompanyName() != null ? o.getCompanyName() : "");
                    opp.setSource(o.getCompanyName() != null && !o.getCompanyName().isBlank() ? o.getCompanyName() : "Skilora");
                    opp.setStatus(o.getStatus() != null ? o.getStatus().name() : "OPEN");
                    if (o.getPostedDate() != null) opp.setPostedDate(o.getPostedDate().toString());
                    if (o.getWorkType() != null && !o.getWorkType().isEmpty()) opp.setType(o.getWorkType());
                    if (o.getSalaryMin() > 0 || o.getSalaryMax() > 0) {
                        opp.setSalaryInfo(String.format("%.0f - %.0f TND", o.getSalaryMin(), o.getSalaryMax()));
                    }
                    list.add(opp);
                }
                return list;
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(FeedController.class).warn("Could not load job offers from DB for feed", e);
        }
        return new java.util.ArrayList<>(jobService.getJobsFromCache());
    }

    /**
     * Legacy hook – we used to pre-build a lowercase cache here.
     * It is now a no-op because JobOpportunity.equals/hashCode are URL-based,
     * which made the Map unreliable when many jobs shared the same URL.
     * Filtering and tag counts now compute lowercase fields on the fly.
     */
    private void buildLowerCaseCache() {
        lowerCaseCache = null;
    }

    private void refreshTags() {
        if (allJobs == null || tabsBox == null) return;

        Map<String, Integer> counts = new HashMap<>();
        counts.put("All", allJobs.size());
        for (String key : KEYWORDS) {
            int c = 0;
            String lowerKey = key.toLowerCase();
            for (JobOpportunity job : allJobs) {
                String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                String location = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
                String desc = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
                if (title.contains(lowerKey) || location.contains(lowerKey) || desc.contains(lowerKey)) {
                    c++;
                }
            }
            if (c > 0) counts.put(key, c);
        }

        tabsBox.getChildren().clear();
        tagGroup = new ToggleGroup();

        addTagButton("All", "All", true, () -> filterJobsByTag("All", getSearchQuery()));
        for (String key : KEYWORDS) {
            if (counts.containsKey(key)) {
                String label = key + " (" + counts.get(key) + ")";
                addTagButton(label, key, false, () -> filterJobsByTag(key, getSearchQuery()));
            }
        }
    }

    private void addTagButton(String label, String rawKey, boolean selected, Runnable onSelect) {
        ToggleButton btn = new ToggleButton(label);
        btn.setUserData(rawKey);
        btn.getStyleClass().add("chip-filter");
        btn.setSelected(selected);
        btn.setToggleGroup(tagGroup);
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setMaxWidth(Region.USE_PREF_SIZE);
        btn.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        btn.setOnAction(e -> {
            if (btn.isSelected()) onSelect.run();
            else if (tagGroup.getSelectedToggle() == null) btn.setSelected(true);
        });
        tabsBox.getChildren().add(btn);
    }

    /** Returns the raw filter key stored in the selected toggle's userData (e.g. "Remote", "Java", "All"). */
    private String getCurrentRawTag() {
        if (tagGroup.getSelectedToggle() != null) {
            Object data = tagGroup.getSelectedToggle().getUserData();
            return data != null ? data.toString() : "All";
        }
        return "All";
    }

    private void filterJobsByTag(String tagRaw, String query) {
        // Data not loaded yet (user typed before load finished): leave loading state, do not show "no results"
        if (allJobs == null) return;
        if (allJobs.isEmpty()) {
            if (jobList != null) {
                jobList.setItems(FXCollections.emptyObservableList());
                jobList.setPlaceholder(new TLEmptyState(
                        SvgIcons.BRIEFCASE,
                        I18n.get("feed.no_results"),
                        I18n.get("feed.no_results.desc")));
            }
            return;
        }

        // tagRaw is the raw userData key (never contains " (n)" count suffix now)
        String tag = (tagRaw == null || tagRaw.isBlank()) ? "All" : tagRaw;

        String lowerQuery = query == null ? "" : query.trim().toLowerCase();
        String finalTag = tag.toLowerCase();
        boolean isAll = "all".equalsIgnoreCase(tag);

        currentFilteredJobs = allJobs.stream()
                .filter(j -> {
                    String title = j.getTitle() != null ? j.getTitle().toLowerCase() : "";
                    String location = j.getLocation() != null ? j.getLocation().toLowerCase() : "";
                    String desc = j.getDescription() != null ? j.getDescription().toLowerCase() : "";
                    String source = j.getSource() != null ? j.getSource().toLowerCase() : "";
                    String company = j.getCompany() != null ? j.getCompany().toLowerCase() : "";

                    boolean matchesSearch = lowerQuery.isEmpty() ||
                            title.contains(lowerQuery) || location.contains(lowerQuery)
                            || source.contains(lowerQuery) || desc.contains(lowerQuery) || company.contains(lowerQuery);
                    boolean matchesTag = isAll ||
                            title.contains(finalTag) || desc.contains(finalTag) || location.contains(finalTag);
                    return matchesSearch && matchesTag;
                })
                .collect(Collectors.toList());

        jobItems.clear();
        jobItems.addAll(currentFilteredJobs);
        if (jobList != null) {
            jobList.setItems(jobItems);
            if (currentFilteredJobs.isEmpty()) {
                jobList.setPlaceholder(new TLEmptyState(
                        SvgIcons.BRIEFCASE,
                        I18n.get("feed.no_results"),
                        I18n.get("feed.no_results.desc")));
            } else {
                jobList.setPlaceholder(null);
            }
        }
    }
}
