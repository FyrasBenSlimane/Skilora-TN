package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLTabs;
import com.skilora.framework.components.TLToast;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import com.skilora.utils.UiUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.scene.control.ButtonType;

/**
 * ActiveOffersController - Admin view for all job offers across the platform.
 * Provides oversight and moderation of all active job listings.
 */
public class ActiveOffersController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ActiveOffersController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label titleLabel;
    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private TLTextField searchField;
    @FXML private VBox offersContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;
    @FXML private Label emptyTitleLabel;
    @FXML private Label emptySubtitleLabel;

    private final JobService jobService = JobService.getInstance();
    private List<JobOffer> allOffers;
    private List<JobOffer> currentFiltered;
    private String currentFilter = "ALL";

    // Pagination — render in batches to avoid lag with 900+ cards
    private static final int PAGE_SIZE = 30;
    private int currentPage = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        setupFilters();
        setupSearch();
        loadData();
    }

    private void applyI18n() {
        if (titleLabel != null) titleLabel.setText(I18n.get("offers.title"));
        if (refreshBtn != null) refreshBtn.setText(I18n.get("common.refresh"));
        if (searchField != null) searchField.setPromptText(I18n.get("offers.search"));
        if (emptyTitleLabel != null) emptyTitleLabel.setText(I18n.get("offers.empty.title"));
        if (emptySubtitleLabel != null) emptySubtitleLabel.setText(I18n.get("offers.empty.subtitle"));
        if (statsLabel != null) statsLabel.setText(I18n.get("common.loading"));
    }

    private void setupFilters() {
        String[][] filters = {
            {I18n.get("offers.filter.all"), "ALL"},
            {I18n.get("offers.filter.open"), "OPEN"},
            {I18n.get("offers.filter.active"), "ACTIVE"},
            {I18n.get("offers.filter.draft"), "DRAFT"},
            {I18n.get("offers.filter.closed"), "CLOSED"}
        };

        TLTabs tabs = new TLTabs();
        for (String[] f : filters) {
            tabs.addTab(f[1], f[0], (javafx.scene.Node) null);
        }
        tabs.setOnTabChanged(tabId -> {
            currentFilter = tabId;
            applyFilters();
        });
        filterBox.getChildren().add(tabs);
    }

    private void setupSearch() {
        if (searchField != null && searchField.getControl() != null) {
            UiUtils.debounce(searchField.getControl(), 300, this::applyFilters);
        }
    }

    private void loadData() {
        offersContainer.getChildren().clear();
        offersContainer.getChildren().add(new TLLoadingState());
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        Task<List<JobOffer>> task = new Task<>() {
            @Override
            protected List<JobOffer> call() throws Exception {
                return jobService.findAllJobOffersWithCompany();
            }
        };

        task.setOnSucceeded(e -> {
            allOffers = task.getValue();
            applyFilters();
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load all offers", task.getException());
            statsLabel.setText(I18n.get("common.error"));
        });

        AppThreadPool.execute(task);
    }

    private void applyFilters() {
        if (allOffers == null) return;

        String query = searchField != null ? searchField.getText() : "";
        String lowerQuery = query == null ? "" : query.toLowerCase();

        List<JobOffer> filtered = allOffers.stream()
            .filter(o -> {
                if (!"ALL".equals(currentFilter)) {
                    try {
                        if (o.getStatus() != JobStatus.valueOf(currentFilter)) return false;
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                }
                if (!lowerQuery.isEmpty()) {
                    String title = o.getTitle() != null ? o.getTitle().toLowerCase() : "";
                    String loc = o.getLocation() != null ? o.getLocation().toLowerCase() : "";
                    String company = o.getCompanyName() != null ? o.getCompanyName().toLowerCase() : "";
                    return title.contains(lowerQuery) || loc.contains(lowerQuery) || company.contains(lowerQuery);
                }
                return true;
            })
            .collect(Collectors.toList());

        renderOffers(filtered);
    }

    private void renderOffers(List<JobOffer> offers) {
        offersContainer.getChildren().clear();
        currentFiltered = offers;
        currentPage = 0;

        if (offers.isEmpty()) {
            emptyState.getChildren().clear();
            emptyState.getChildren().add(new TLEmptyState(
                SvgIcons.SEARCH,
                I18n.get("offers.empty.title"),
                I18n.get("offers.empty.subtitle")));
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("offers.total", 0));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        statsLabel.setText(I18n.get("offers.total", offers.size()));

        // Render first page only
        appendPage();
    }

    /** Appends the next page of cards to the container. */
    private void appendPage() {
        if (currentFiltered == null) return;

        // Remove existing "Load More" button if present
        if (!offersContainer.getChildren().isEmpty()) {
            javafx.scene.Node last = offersContainer.getChildren().get(offersContainer.getChildren().size() - 1);
            if (last.getUserData() != null && "load-more".equals(last.getUserData())) {
                offersContainer.getChildren().remove(last);
            }
        }

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentFiltered.size());

        for (int i = start; i < end; i++) {
            offersContainer.getChildren().add(createOfferCard(currentFiltered.get(i)));
        }

        currentPage++;

        // Add "Load More" button if there are remaining offers
        int remaining = currentFiltered.size() - end;
        if (remaining > 0) {
            TLButton loadMoreBtn = new TLButton(
                    I18n.get("feed.load_more").replace("{0}", String.valueOf(remaining)),
                    TLButton.ButtonVariant.OUTLINE);
            loadMoreBtn.setMaxWidth(Double.MAX_VALUE);
            loadMoreBtn.setUserData("load-more");
            loadMoreBtn.setOnAction(e -> appendPage());

            VBox loadMoreWrap = new VBox(loadMoreBtn);
            loadMoreWrap.setPadding(new Insets(12, 0, 12, 0));
            loadMoreWrap.setUserData("load-more");
            offersContainer.getChildren().add(loadMoreWrap);
        }
    }

    private TLCard createOfferCard(JobOffer offer) {
        TLCard card = new TLCard();
        card.getStyleClass().add("offer-card");

        VBox content = new VBox(12);

        // ── Top row: Title + Status Badge ──
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(2);
        Label titleLabel = new Label(offer.getTitle() != null ? offer.getTitle() : "—");
        titleLabel.getStyleClass().add("h4");

        if (offer.getCompanyName() != null) {
            Label companyLabel = new Label(offer.getCompanyName());
            companyLabel.getStyleClass().addAll("text-muted", "text-13");
            titleBlock.getChildren().addAll(titleLabel, companyLabel);
        } else {
            titleBlock.getChildren().add(titleLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String statusKey;
        if (offer.getStatus() != null) {
            switch (offer.getStatus()) {
                case OPEN: statusKey = "offers.status.open"; break;
                case ACTIVE: statusKey = "offers.status.active"; break;
                case CLOSED: statusKey = "offers.status.closed"; break;
                default: statusKey = "offers.status.draft"; break;
            }
        } else {
            statusKey = "offers.status.draft";
        }

        TLBadge statusBadge = new TLBadge(I18n.get(statusKey), TLBadge.Variant.DEFAULT);
        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            statusBadge.getStyleClass().add("badge-success");
        } else if (offer.getStatus() == JobStatus.CLOSED) {
            statusBadge.getStyleClass().add("badge-destructive");
        }

        topRow.getChildren().addAll(titleBlock, spacer, statusBadge);

        // ── Details row: Location | Type | Salary | Date ──
        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        detailsRow.setPadding(new Insets(4, 0, 4, 0));

        if (offer.getLocation() != null) {
            detailsRow.getChildren().add(createDetailChip(
                SvgIcons.MAP_PIN,
                offer.getLocation()));
        }

        if (offer.getWorkType() != null) {
            detailsRow.getChildren().add(createDetailChip(
                SvgIcons.BRIEFCASE,
                offer.getWorkType()));
        }

        detailsRow.getChildren().add(createDetailChip(
            SvgIcons.DOLLAR_SIGN,
            offer.getSalaryRange() + " " + (offer.getCurrency() != null ? offer.getCurrency() : "TND")));

        if (offer.getPostedDate() != null) {
            detailsRow.getChildren().add(createDetailChip(
                SvgIcons.CALENDAR,
                offer.getPostedDate().format(DATE_FMT)));
        }

        // ── Separator + Actions ──
        TLSeparator sep = new TLSeparator();

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            TLButton closeBtn = new TLButton(I18n.get("offers.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> handleCloseOffer(offer));
            actionsRow.getChildren().add(closeBtn);
        }

        TLButton deleteBtn = new TLButton(I18n.get("offers.delete"), TLButton.ButtonVariant.GHOST);
        deleteBtn.getStyleClass().add("text-destructive");
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));
        actionsRow.getChildren().add(deleteBtn);

        content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    /**
     * Creates a compact detail chip with an SVG icon + text, used inside offer cards.
     */
    private HBox createDetailChip(String svgContent, String text) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.getStyleClass().add("svg-path");
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);

        Label label = new Label(text);
        label.getStyleClass().addAll("text-muted", "text-13");

        chip.getChildren().addAll(icon, label);
        return chip;
    }

    /** Called by MainView infinite-scroll listener to auto-load the next page. */
    public void loadNextPageIfAvailable() {
        if (currentFiltered == null) return;
        int loaded = currentPage * PAGE_SIZE;
        if (loaded < currentFiltered.size()) {
            javafx.application.Platform.runLater(this::appendPage);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void handleDeleteOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("offers.delete.confirm.title"),
            I18n.get("offers.delete.confirm.message", offer.getTitle())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                offersContainer.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.deleteJobOffer(offer.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    offersContainer.setDisable(false);
                    if (task.getValue()) {
                        allOffers.remove(offer);
                        applyFilters();
                    }
                });
                task.setOnFailed(e -> {
                    offersContainer.setDisable(false);
                    logger.error("Failed to delete offer", task.getException());
                    TLToast.error(offersContainer.getScene(), I18n.get("common.error"), I18n.get("error.failed_delete_offer"));
                });

                AppThreadPool.execute(task);
            }
        });
    }

    private void handleCloseOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("offers.close.confirm.title"),
            I18n.get("offers.close.confirm.message", offer.getTitle())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                offersContainer.setDisable(true);
                offer.setStatus(JobStatus.CLOSED);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.updateJobOffer(offer);
                    }
                };
                task.setOnSucceeded(e -> {
                    offersContainer.setDisable(false);
                    applyFilters();
                });
                task.setOnFailed(e -> {
                    offersContainer.setDisable(false);
                    logger.error("Failed to close offer", task.getException());
                    TLToast.error(offersContainer.getScene(), I18n.get("common.error"), I18n.get("error.failed_close_offer"));
                });

                AppThreadPool.execute(task);
            }
        });
    }
}
