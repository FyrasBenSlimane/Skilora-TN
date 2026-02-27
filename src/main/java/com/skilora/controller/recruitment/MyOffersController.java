package com.skilora.controller.recruitment;

import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.JobStatus;
import com.skilora.service.recruitment.JobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;

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
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

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
    private java.util.function.Consumer<JobOffer> onEditOffer;
    private java.util.function.Consumer<JobOffer> onViewOffer;
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

    public void setOnEditOffer(java.util.function.Consumer<JobOffer> onEditOffer) {
        this.onEditOffer = onEditOffer;
    }

    public void setOnViewOffer(java.util.function.Consumer<JobOffer> onViewOffer) {
        this.onViewOffer = onViewOffer;
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
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> applyFilters());
            searchField.getControl().setOnKeyReleased(e -> pause.playFromStart());
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

        Thread thread = new Thread(task, "MyOffersLoader");
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
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

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
            Label locLabel = new Label("📍 " + offer.getLocation());
            locLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(locLabel);
        }

        if (offer.getWorkType() != null) {
            Label typeLabel = new Label("💼 " + offer.getWorkType());
            typeLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(typeLabel);
        }

        Label salaryLabel = new Label("💰 " + offer.getSalaryRange() + " " +
            (offer.getCurrency() != null ? offer.getCurrency() : "TND"));
        salaryLabel.getStyleClass().add("text-muted");
        detailsRow.getChildren().add(salaryLabel);

        if (offer.getPostedDate() != null) {
            Label dateLabel = new Label("📅 " + offer.getPostedDate().format(DATE_FMT));
            dateLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(dateLabel);
        }

        // Applications count
        try {
            int appCount = jobService.countApplicationsForJob(offer.getId());
            if (appCount > 0) {
                Label appCountLabel = new Label("📋 " + appCount + " " + (appCount == 1 ? I18n.get("myoffers.application") : I18n.get("myoffers.applications")));
                appCountLabel.getStyleClass().add("text-muted");
                detailsRow.getChildren().add(appCountLabel);
            }
        } catch (Exception e) {
            logger.debug("Could not load application count for offer {}", offer.getId(), e);
        }

        // Actions row
        Separator sep = new Separator();

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        TLButton viewBtn = new TLButton("Voir détails", TLButton.ButtonVariant.GHOST);
        viewBtn.setOnAction(e -> {
            if (onViewOffer != null) onViewOffer.accept(offer);
        });

        TLButton editBtn = new TLButton(I18n.get("myoffers.edit"), TLButton.ButtonVariant.OUTLINE);
        editBtn.setOnAction(e -> {
            if (onEditOffer != null) {
                onEditOffer.accept(offer);
            }
        });

        TLButton deleteBtn = new TLButton(I18n.get("myoffers.delete"), TLButton.ButtonVariant.GHOST);
        deleteBtn.getStyleClass().add("text-destructive");
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            TLButton closeBtn = new TLButton(I18n.get("myoffers.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> handleCloseOffer(offer));
            actionsRow.getChildren().add(closeBtn);
        } else if (offer.getStatus() == JobStatus.CLOSED) {
            TLButton openBtn = new TLButton(I18n.get("myoffers.open"), TLButton.ButtonVariant.OUTLINE);
            openBtn.setOnAction(e -> handleOpenOffer(offer));
            actionsRow.getChildren().add(openBtn);
        }

        actionsRow.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        card.getChildren().add(content);
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
        String title = offer.getTitle() != null ? offer.getTitle() : I18n.get("myoffers.no_title");
        java.util.Optional<javafx.scene.control.ButtonType> result =
                com.skilora.utils.DialogUtils.showConfirmation(
                        I18n.get("myoffers.delete_confirm_title", "Supprimer l'offre"),
                        I18n.get("myoffers.delete_confirm_message",
                                "Voulez-vous vraiment supprimer l'offre « " + title + " » ? Cette action est irréversible."));
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
            return;
        }

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

        Thread thread = new Thread(task, "DeleteOffer");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleCloseOffer(JobOffer offer) {
        offer.setStatus(JobStatus.CLOSED);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return jobService.updateJobOffer(offer);
            }
        };
        task.setOnSucceeded(e -> applyFilters());
        task.setOnFailed(e -> logger.error("Failed to close offer", task.getException()));

        Thread thread = new Thread(task, "CloseOffer");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleOpenOffer(JobOffer offer) {
        offer.setStatus(JobStatus.OPEN);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return jobService.updateJobOffer(offer);
            }
        };
        task.setOnSucceeded(e -> applyFilters());
        task.setOnFailed(e -> logger.error("Failed to open offer", task.getException()));

        Thread thread = new Thread(task, "OpenOffer");
        thread.setDaemon(true);
        thread.start();
    }
}
