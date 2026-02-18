package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.user.entity.User;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLToast;
import com.skilora.utils.UiUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.scene.control.ButtonType;

/**
 * MyOffersController - Employer's own job offers management.
 * Displays CRUD operations for the employer's posted job offers.
 */
public class MyOffersController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MyOffersController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private TLTextField searchField;
    @FXML private VBox offersContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;
    @FXML private TLButton newOfferBtn;

    private final JobService jobService = JobService.getInstance();
    private User currentUser;
    private Runnable onNewOffer;
    private List<JobOffer> allOffers;
    private ToggleGroup filterGroup;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        setupSearch();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    public void setOnNewOffer(Runnable onNewOffer) {
        this.onNewOffer = onNewOffer;
    }

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        String[][] filters = {
            {I18n.get("myoffers.filter.all"), "ALL"},
            {I18n.get("myoffers.filter.open"), "OPEN"},
            {I18n.get("myoffers.filter.draft"), "DRAFT"},
            {I18n.get("myoffers.filter.closed"), "CLOSED"}
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
            UiUtils.debounce(searchField.getControl(), 300, this::applyFilters);
        }
    }

    private void loadData() {
        if (currentUser == null) return;

        Task<List<JobOffer>> task = new Task<>() {
            @Override
            protected List<JobOffer> call() throws Exception {
                return jobService.findJobOffersByCompanyOwner(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            allOffers = task.getValue();
            applyFilters();
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load offers", task.getException());
            statsLabel.setText(I18n.get("myoffers.error"));
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
                    return title.contains(lowerQuery) || loc.contains(lowerQuery);
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
            statsLabel.setText(I18n.get("myoffers.count.zero"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        statsLabel.setText(I18n.get("myoffers.count", offers.size()));

        for (JobOffer offer : offers) {
            offersContainer.getChildren().add(createOfferCard(offer));
        }
    }

    private TLCard createOfferCard(JobOffer offer) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Top row: Title + Status badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(offer.getTitle() != null ? offer.getTitle() : I18n.get("myoffers.no_title"));
        titleLabel.getStyleClass().add("h4");

        TLBadge statusBadge = new TLBadge(
            offer.getStatus() != null ? offer.getStatus().getDisplayName() : I18n.get("myoffers.draft"),
            TLBadge.Variant.DEFAULT
        );
        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            statusBadge.getStyleClass().add("badge-success");
        } else if (offer.getStatus() == JobStatus.CLOSED) {
            statusBadge.getStyleClass().add("badge-destructive");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(titleLabel, statusBadge, spacer);

        // Details row
        HBox detailsRow = new HBox(16);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        if (offer.getLocation() != null) {
            Label locLabel = new Label(offer.getLocation());
            locLabel.setGraphic(SvgIcons.icon(SvgIcons.MAP_PIN, 14, "-fx-muted-foreground"));
            locLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(locLabel);
        }

        if (offer.getWorkType() != null) {
            Label typeLabel = new Label(offer.getWorkType());
            typeLabel.setGraphic(SvgIcons.icon(SvgIcons.BRIEFCASE, 14, "-fx-muted-foreground"));
            typeLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(typeLabel);
        }

        Label salaryLabel = new Label(offer.getSalaryRange() + " " +
            (offer.getCurrency() != null ? offer.getCurrency() : "TND"));
        salaryLabel.setGraphic(SvgIcons.icon(SvgIcons.DOLLAR_SIGN, 14, "-fx-muted-foreground"));
        salaryLabel.getStyleClass().add("text-muted");
        detailsRow.getChildren().add(salaryLabel);

        if (offer.getPostedDate() != null) {
            Label dateLabel = new Label(offer.getPostedDate().format(DATE_FMT));
            dateLabel.setGraphic(SvgIcons.icon(SvgIcons.CALENDAR, 14, "-fx-muted-foreground"));
            dateLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(dateLabel);
        }

        // Actions row
        TLSeparator sep = new TLSeparator();

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        TLButton editBtn = new TLButton(I18n.get("myoffers.edit"), TLButton.ButtonVariant.OUTLINE);
        editBtn.setOnAction(e -> logger.info("Edit offer: {}", offer.getId()));

        TLButton deleteBtn = new TLButton(I18n.get("myoffers.delete"), TLButton.ButtonVariant.GHOST);
        deleteBtn.getStyleClass().add("text-destructive");
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            TLButton closeBtn = new TLButton(I18n.get("myoffers.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> handleCloseOffer(offer));
            actionsRow.getChildren().add(closeBtn);
        }

        actionsRow.getChildren().addAll(editBtn, deleteBtn);

        content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    @FXML
    private void handleNewOffer() {
        if (onNewOffer != null) {
            onNewOffer.run();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void handleDeleteOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("myoffers.delete.confirm.title"),
            I18n.get("myoffers.delete.confirm.message", offer.getTitle())
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
                task.setOnFailed(e -> {
                    logger.error("Failed to delete offer", task.getException());
                    TLToast.error(offersContainer.getScene(), I18n.get("common.error"), I18n.get("error.failed_delete_offer"));
                });

                AppThreadPool.execute(task);
            }
        });
    }

    private void handleCloseOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("myoffers.close.confirm.title"),
            I18n.get("myoffers.close.confirm.message", offer.getTitle())
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
                task.setOnFailed(e -> {
                    logger.error("Failed to close offer", task.getException());
                    TLToast.error(offersContainer.getScene(), I18n.get("common.error"), I18n.get("error.failed_close_offer"));
                });

                AppThreadPool.execute(task);
            }
        });
    }
}
