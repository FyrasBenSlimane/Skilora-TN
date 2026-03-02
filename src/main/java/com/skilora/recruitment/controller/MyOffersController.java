package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.user.entity.User;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLTextarea;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLTabs;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLDialog;
import com.skilora.utils.UiUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private User currentUser;
    private Runnable onNewOffer;
    private java.util.function.Consumer<JobOffer> onViewDetails;
    private List<JobOffer> allOffers;
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (newOfferBtn != null) newOfferBtn.setText(I18n.get("myoffers.new"));
        if (refreshBtn != null) refreshBtn.setText(I18n.get("common.refresh"));
        if (searchField != null) searchField.setPromptText(I18n.get("myoffers.search"));
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

    public void setOnViewDetails(java.util.function.Consumer<JobOffer> onViewDetails) {
        this.onViewDetails = onViewDetails;
    }

    private void setupFilters() {
        String[][] filters = {
            {I18n.get("myoffers.filter.all"), "ALL"},
            {I18n.get("myoffers.filter.open"), "OPEN"},
            {I18n.get("myoffers.filter.draft"), "DRAFT"},
            {I18n.get("myoffers.filter.closed"), "CLOSED"}
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
        if (currentUser == null) return;

        offersContainer.getChildren().clear();
        offersContainer.getChildren().add(new TLLoadingState());
        emptyState.setVisible(false);
        emptyState.setManaged(false);

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
            emptyState.getChildren().clear();
            TLButton createBtn = new TLButton(I18n.get("myoffers.new"), TLButton.ButtonVariant.PRIMARY);
            createBtn.setOnAction(e -> handleNewOffer());
            emptyState.getChildren().add(new TLEmptyState(
                SvgIcons.BRIEFCASE,
                I18n.get("myoffers.empty.title"),
                I18n.get("myoffers.empty.subtitle"),
                createBtn));
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

        // Application count
        try {
            int appCount = jobService.countApplicationsForJob(offer.getId());
            Label appCountLabel = new Label(I18n.get("myoffers.app_count", appCount));
            appCountLabel.setGraphic(SvgIcons.icon(SvgIcons.USERS, 14, "-fx-muted-foreground"));
            appCountLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(appCountLabel);
        } catch (Exception ex) {
            // skip count if DB unavailable
        }

        // Actions row
        TLSeparator sep = new TLSeparator();

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        TLButton editBtn = new TLButton(I18n.get("myoffers.edit"), TLButton.ButtonVariant.OUTLINE);
        editBtn.setOnAction(e -> handleEditOffer(offer));

        TLButton deleteBtn = new TLButton(I18n.get("myoffers.delete"), TLButton.ButtonVariant.GHOST);
        deleteBtn.getStyleClass().add("text-destructive");
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            TLButton closeBtn = new TLButton(I18n.get("myoffers.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> handleCloseOffer(offer));
            actionsRow.getChildren().add(closeBtn);
        }

        // View Details button
        TLButton viewBtn = new TLButton(I18n.get("myoffers.view_details"), TLButton.ButtonVariant.OUTLINE);
        viewBtn.setOnAction(e -> showOfferDetailsDialog(offer));
        actionsRow.getChildren().add(viewBtn);

        actionsRow.getChildren().addAll(editBtn, deleteBtn);

        // Reopen button for closed offers
        if (offer.getStatus() == JobStatus.CLOSED) {
            TLButton reopenBtn = new TLButton(I18n.get("myoffers.reopen"), TLButton.ButtonVariant.SECONDARY);
            reopenBtn.setOnAction(e -> handleReopenOffer(offer));
            actionsRow.getChildren().add(reopenBtn);
        }

        content.getChildren().addAll(topRow, detailsRow, sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    /** Show a dialog with full details of the selected job offer. */
    private void showOfferDetailsDialog(JobOffer offer) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(I18n.get("myoffers.view_details"));
        dialog.setDialogTitle(offer.getTitle());
        dialog.setDescription(offer.getCompanyName() != null ? offer.getCompanyName() : "");

        VBox body = new VBox(12);

        // Status
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label(I18n.get("inbox.col.status") + ":");
        statusLabel.setStyle("-fx-font-weight:bold;");
        TLBadge statusBadge = new TLBadge(
                offer.getStatus() != null ? offer.getStatus().name() : "—",
                offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE
                        ? TLBadge.Variant.SUCCESS : TLBadge.Variant.SECONDARY);
        statusRow.getChildren().addAll(statusLabel, statusBadge);
        body.getChildren().add(statusRow);

        // Location & work type
        if (offer.getLocation() != null && !offer.getLocation().isBlank()) {
            body.getChildren().add(detailLine("\uD83D\uDCCD " + I18n.get("postjob.location"), offer.getLocation()));
        }
        if (offer.getWorkType() != null && !offer.getWorkType().isBlank()) {
            body.getChildren().add(detailLine("\uD83C\uDFE2 " + I18n.get("myoffers.detail.worktype"), offer.getWorkType()));
        }

        // Salary
        String salaryText = String.format("%.0f - %.0f %s",
                offer.getSalaryMin(), offer.getSalaryMax(),
                offer.getCurrency() != null ? offer.getCurrency() : "TND");
        body.getChildren().add(detailLine("\uD83D\uDCB0 " + I18n.get("postjob.salary"), salaryText));

        // Dates
        if (offer.getPostedDate() != null) {
            body.getChildren().add(detailLine("\uD83D\uDCC5 " + I18n.get("myoffers.detail.posted"), offer.getPostedDate().format(DATE_FMT)));
        }
        if (offer.getDeadline() != null) {
            body.getChildren().add(detailLine("\u23F0 " + I18n.get("myoffers.detail.deadline"), offer.getDeadline().format(DATE_FMT)));
        }

        // Required skills
        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            Label skillsTitle = new Label("\uD83D\uDCDD " + I18n.get("postjob.skills_required"));
            skillsTitle.setStyle("-fx-font-weight:bold;");
            body.getChildren().add(skillsTitle);

            javafx.scene.layout.FlowPane skillsFlow = new javafx.scene.layout.FlowPane(8, 6);
            for (String skill : offer.getRequiredSkills()) {
                TLBadge skillBadge = new TLBadge(skill, TLBadge.Variant.OUTLINE);
                skillsFlow.getChildren().add(skillBadge);
            }
            body.getChildren().add(skillsFlow);
        }

        // Description
        if (offer.getDescription() != null && !offer.getDescription().isBlank()) {
            body.getChildren().add(new TLSeparator());
            Label descTitle = new Label(I18n.get("postjob.description"));
            descTitle.setStyle("-fx-font-weight:bold;");
            Label descBody = new Label(offer.getDescription());
            descBody.setWrapText(true);
            descBody.setMaxWidth(500);
            descBody.getStyleClass().add("text-muted");
            body.getChildren().addAll(descTitle, descBody);
        }

        // Application count
        try {
            int appCount = applicationService.getApplicationsByJobOffer(offer.getId()).size();
            body.getChildren().add(detailLine("\uD83D\uDC65 " + I18n.get("myoffers.detail.applications"), String.valueOf(appCount)));
        } catch (Exception ignored) {}

        dialog.setContent(body);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /** Helper: creates a single "label: value" line for the details dialog. */
    private HBox detailLine(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight:bold;");
        Label val = new Label(value);
        val.getStyleClass().add("text-muted");
        row.getChildren().addAll(lbl, val);
        return row;
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

    private void handleEditOffer(JobOffer offer) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(I18n.get("myoffers.edit.dialog.title"));
        dialog.setDialogTitle(I18n.get("myoffers.edit.dialog.header"));
        dialog.setDescription(I18n.get("myoffers.edit.dialog.desc"));

        // Form fields pre-filled with current values
        TLTextField titleField = new TLTextField();
        titleField.setText(offer.getTitle() != null ? offer.getTitle() : "");
        titleField.setPromptText(I18n.get("postjob.title.placeholder"));

        TLTextarea descField = new TLTextarea();
        descField.setText(offer.getDescription() != null ? offer.getDescription() : "");
        descField.setPromptText(I18n.get("postjob.description.placeholder"));
        descField.setPrefHeight(150);

        TLTextField locationField = new TLTextField();
        locationField.setText(offer.getLocation() != null ? offer.getLocation() : "");
        locationField.setPromptText(I18n.get("postjob.location.placeholder"));

        TLTextField salaryField = new TLTextField();
        String currentSalary = offer.getSalaryRange();
        salaryField.setText(currentSalary != null ? currentSalary : "");
        salaryField.setPromptText(I18n.get("postjob.salary.placeholder"));

        TLSelect<String> typeSelect = new TLSelect<>(I18n.get("postjob.contract_type"));
        typeSelect.getItems().addAll(
            I18n.get("postjob.type.fulltime"),
            I18n.get("postjob.type.parttime"),
            I18n.get("postjob.type.contract"),
            I18n.get("postjob.type.freelance"),
            I18n.get("postjob.type.internship")
        );
        if (offer.getWorkType() != null) {
            typeSelect.setValue(offer.getWorkType());
        }

        TLSelect<String> statusSelect = new TLSelect<>(I18n.get("myoffers.edit.status"));
        statusSelect.getItems().addAll("OPEN", "DRAFT", "CLOSED");
        if (offer.getStatus() != null) {
            statusSelect.setValue(offer.getStatus().name());
        }

        dialog.setContent(
            dialog.createFormSection(I18n.get("postjob.job_title"), titleField),
            dialog.createFormSection(I18n.get("postjob.description"), descField),
            dialog.createFormSection(I18n.get("postjob.location"), locationField),
            dialog.createFormSection(I18n.get("postjob.salary"), salaryField),
            dialog.createFormSection(I18n.get("postjob.contract_type"), typeSelect),
            dialog.createFormSection(I18n.get("myoffers.edit.status"), statusSelect)
        );

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.styleButtons();

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String titleText = titleField.getText() != null ? titleField.getText().trim() : "";
                if (titleText.isEmpty()) {
                    titleField.setError(I18n.get("error.validation.required", I18n.get("postjob.title.placeholder")));
                    event.consume();
                } else {
                    titleField.clearValidation();
                }
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Apply edits
                offer.setTitle(titleField.getText().trim());
                offer.setDescription(descField.getText());
                offer.setLocation(locationField.getText().trim());

                // Parse salary
                String salaryText = salaryField.getText().replaceAll("[^0-9\\-]", "");
                if (salaryText.contains("-")) {
                    String[] parts = salaryText.split("-");
                    try {
                        offer.setSalaryMin(Double.parseDouble(parts[0].trim()));
                        offer.setSalaryMax(Double.parseDouble(parts[1].trim()));
                    } catch (NumberFormatException ex) {
                        // keep existing values
                    }
                }

                String typeVal = typeSelect.getValue();
                if (typeVal != null) {
                    offer.setWorkType(typeVal.toUpperCase().replace("-", "_"));
                }

                String statusVal = statusSelect.getValue();
                if (statusVal != null) {
                    try {
                        offer.setStatus(JobStatus.valueOf(statusVal));
                    } catch (IllegalArgumentException ex) {
                        // keep existing
                    }
                }

                // Persist update
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.updateJobOffer(offer);
                    }
                };
                task.setOnSucceeded(e2 -> {
                    if (task.getValue()) {
                        TLToast.success(offersContainer.getScene(),
                                I18n.get("myoffers.edit.success.title"),
                                I18n.get("myoffers.edit.success.msg"));
                        loadData();
                    }
                });
                task.setOnFailed(e2 -> {
                    logger.error("Failed to update offer", task.getException());
                    TLToast.error(offersContainer.getScene(),
                            I18n.get("common.error"),
                            I18n.get("myoffers.edit.error.msg"));
                });
                AppThreadPool.execute(task);
            }
        });
    }

    private void handleDeleteOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("myoffers.delete.confirm.title"),
            I18n.get("myoffers.delete.confirm.message", offer.getTitle())
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
            I18n.get("myoffers.close.confirm.title"),
            I18n.get("myoffers.close.confirm.message", offer.getTitle())
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

    private void handleReopenOffer(JobOffer offer) {
        DialogUtils.showConfirmation(
            I18n.get("myoffers.reopen.confirm.title"),
            I18n.get("myoffers.reopen.confirm.message", offer.getTitle())
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                offersContainer.setDisable(true);
                offer.setStatus(JobStatus.OPEN);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return jobService.updateJobOffer(offer);
                    }
                };
                task.setOnSucceeded(e -> {
                    offersContainer.setDisable(false);
                    TLToast.success(offersContainer.getScene(),
                            I18n.get("myoffers.reopen.success.title"),
                            I18n.get("myoffers.reopen.success.msg"));
                    loadData();
                });
                task.setOnFailed(e -> {
                    offersContainer.setDisable(false);
                    logger.error("Failed to reopen offer", task.getException());
                    TLToast.error(offersContainer.getScene(),
                            I18n.get("common.error"),
                            I18n.get("myoffers.reopen.error.msg"));
                });

                AppThreadPool.execute(task);
            }
        });
    }
}
