package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSeparator;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

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
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";

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
        filterGroup = new ToggleGroup();
        String[][] filters = {
            {I18n.get("offers.filter.all"), "ALL"},
            {I18n.get("offers.filter.open"), "OPEN"},
            {I18n.get("offers.filter.active"), "ACTIVE"},
            {I18n.get("offers.filter.draft"), "DRAFT"},
            {I18n.get("offers.filter.closed"), "CLOSED"}
        };

        for (String[] f : filters) {
            ToggleButton btn = new ToggleButton(f[0]);
            btn.setUserData(f[1]);
            btn.getStyleClass().add("chip-filter");
            btn.setToggleGroup(filterGroup);
            if ("ALL".equals(f[1])) btn.setSelected(true);

            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    currentFilter = (String) btn.getUserData();
                    applyFilters();
                } else if (filterGroup.getSelectedToggle() == null) {
                    btn.setSelected(true);
                }
            });
            filterBox.getChildren().add(btn);
        }
    }

    private void setupSearch() {
        if (searchField != null && searchField.getControl() != null) {
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> applyFilters());
            searchField.getControl().setOnKeyReleased(e -> pause.playFromStart());
        }
    }

    private void loadData() {
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

        Thread thread = new Thread(task, "ActiveOffersLoader");
        thread.setDaemon(true);
        thread.start();
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

        if (offers.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("offers.total", 0));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        statsLabel.setText(I18n.get("offers.total", offers.size()));

        for (JobOffer offer : offers) {
            offersContainer.getChildren().add(createOfferCard(offer));
        }
    }

    private TLCard createOfferCard(JobOffer offer) {
        TLCard card = new TLCard();
        card.getStyleClass().add("offer-card");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        // ── Top row: Title + Status Badge ──
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(2);
        Label titleLabel = new Label(offer.getTitle() != null ? offer.getTitle() : "—");
        titleLabel.getStyleClass().add("h4");

        if (offer.getCompanyName() != null) {
            Label companyLabel = new Label(offer.getCompanyName());
            companyLabel.getStyleClass().add("text-muted");
            companyLabel.setStyle("-fx-font-size: 13px;");
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
                "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z",
                offer.getLocation()));
        }

        if (offer.getWorkType() != null) {
            detailsRow.getChildren().add(createDetailChip(
                "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-2 .89-2 2v11c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z",
                offer.getWorkType()));
        }

        detailsRow.getChildren().add(createDetailChip(
            "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
            offer.getSalaryRange() + " " + (offer.getCurrency() != null ? offer.getCurrency() : "TND")));

        if (offer.getPostedDate() != null) {
            detailsRow.getChildren().add(createDetailChip(
                "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM9 10H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z",
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
        label.getStyleClass().add("text-muted");
        label.setStyle("-fx-font-size: 13px;");

        chip.getChildren().addAll(icon, label);
        return chip;
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
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.deleteJobOffer(offer.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        allOffers.remove(offer);
                        applyFilters();
                    }
                });
                task.setOnFailed(e -> logger.error("Failed to delete offer", task.getException()));

                Thread thread = new Thread(task, "AdminDeleteOffer");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private void handleCloseOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("offers.close.confirm.title"),
            I18n.get("offers.close.confirm.message", offer.getTitle())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                offer.setStatus(JobStatus.CLOSED);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.updateJobOffer(offer);
                    }
                };
                task.setOnSucceeded(e -> applyFilters());
                task.setOnFailed(e -> logger.error("Failed to close offer", task.getException()));

                Thread thread = new Thread(task, "AdminCloseOffer");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }
}
