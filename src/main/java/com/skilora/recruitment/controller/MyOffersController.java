package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.user.entity.User;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLEmptyState;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLTextarea;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTabs;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLDialog;
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
    // Callback to open the applications inbox for a given offer (wired from MainView)
    private java.util.function.Consumer<JobOffer> onViewApplications;
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

    public void setOnViewApplications(java.util.function.Consumer<JobOffer> onViewApplications) {
        this.onViewApplications = onViewApplications;
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
        card.getStyleClass().addAll("offer-card", "card-interactive");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 4, 0));

        // ===== HEADER: Title + Status + Actions =====
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Status indicator dot
        TLBadge statusBadge = createStatusBadge(offer.getStatus());

        // Title with enhanced styling
        Label titleLabel = new Label(offer.getTitle() != null ? offer.getTitle() : I18n.get("myoffers.no_title"));
        titleLabel.getStyleClass().addAll("h4", "job-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(500);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick action buttons in header
        HBox quickActions = new HBox(6);
        quickActions.setAlignment(Pos.CENTER_RIGHT);

        TLButton editBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
        editBtn.setGraphic(SvgIcons.icon(SvgIcons.EDIT, 16, "-fx-muted-foreground"));
        editBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("myoffers.edit")));
        editBtn.setOnAction(e -> handleEditOffer(offer));

        TLButton deleteBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
        deleteBtn.setGraphic(SvgIcons.icon(SvgIcons.TRASH, 16, "-fx-destructive"));
        deleteBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("myoffers.delete")));
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

        quickActions.getChildren().addAll(editBtn, deleteBtn);

        headerRow.getChildren().addAll(statusBadge, titleLabel, spacer, quickActions);

        // ===== METADATA GRID: Location | Work Type | Salary | Date =====
        javafx.scene.layout.FlowPane metadataPane = new javafx.scene.layout.FlowPane(12, 8);
        metadataPane.setPrefWrapLength(600);

        if (offer.getLocation() != null && !offer.getLocation().isEmpty()) {
            metadataPane.getChildren().add(createMetadataItem(SvgIcons.MAP_PIN, offer.getLocation()));
        }

        if (offer.getWorkType() != null && !offer.getWorkType().isEmpty()) {
            metadataPane.getChildren().add(createMetadataItem(SvgIcons.BRIEFCASE, offer.getWorkType()));
        }

        if (offer.getSalaryRange() != null && !offer.getSalaryRange().isEmpty()) {
            metadataPane.getChildren().add(createMetadataItem(SvgIcons.DOLLAR_SIGN,
                offer.getSalaryRange() + " " + (offer.getCurrency() != null ? offer.getCurrency() : "TND")));
        }

        if (offer.getPostedDate() != null) {
            String dateText = I18n.get("myoffers.posted") + " " + offer.getPostedDate().format(DATE_FMT);
            metadataPane.getChildren().add(createMetadataItem(SvgIcons.CALENDAR, dateText));
        }

        // ===== DESCRIPTION PREVIEW =====
        VBox descBox = null;
        if (offer.getDescription() != null && !offer.getDescription().isEmpty()) {
            String desc = offer.getDescription();
            if (desc.length() > 120) {
                desc = desc.substring(0, 120) + "...";
            }
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().addAll("text-muted", "text-sm");
            descLabel.setWrapText(true);
            descBox = new VBox(descLabel);
        }

        // ===== FOOTER: Primary Actions =====
        HBox footerRow = new HBox(8);
        footerRow.setAlignment(Pos.CENTER_RIGHT);
        footerRow.setPadding(new Insets(8, 0, 0, 0));

        if (offer.getStatus() == JobStatus.OPEN || offer.getStatus() == JobStatus.ACTIVE) {
            TLButton closeBtn = new TLButton(I18n.get("myoffers.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, null));
            closeBtn.setOnAction(e -> handleCloseOffer(offer));
            footerRow.getChildren().add(closeBtn);
        } else if (offer.getStatus() == JobStatus.CLOSED) {
            TLButton reopenBtn = new TLButton(I18n.get("myoffers.reopen"), TLButton.ButtonVariant.OUTLINE);
            reopenBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14, null));
            reopenBtn.setOnAction(e -> handleReopenOffer(offer));
            footerRow.getChildren().add(reopenBtn);
        }

        TLButton viewAppsBtn = new TLButton(I18n.get("myoffers.view_applications"), TLButton.ButtonVariant.SECONDARY);
        viewAppsBtn.setGraphic(SvgIcons.icon(SvgIcons.USERS, 14, null));
        viewAppsBtn.setOnAction(e -> handleViewApplications(offer));
        footerRow.getChildren().add(viewAppsBtn);

        // Assemble content
        content.getChildren().add(headerRow);
        if (!metadataPane.getChildren().isEmpty()) {
            content.getChildren().add(metadataPane);
        }
        if (descBox != null) {
            content.getChildren().add(descBox);
        }
        content.getChildren().add(footerRow);

        card.getContent().add(content);
        return card;
    }

    private TLBadge createStatusBadge(JobStatus status) {
        String label;
        TLBadge.Variant variant;

        if (status == null) {
            label = I18n.get("myoffers.draft");
            variant = TLBadge.Variant.DEFAULT;
        } else {
            switch (status) {
                case OPEN, ACTIVE -> {
                    label = I18n.get("myoffers.status.active");
                    variant = TLBadge.Variant.SUCCESS;
                }
                case CLOSED -> {
                    label = I18n.get("myoffers.status.closed");
                    variant = TLBadge.Variant.DESTRUCTIVE;
                }
                case DRAFT -> {
                    label = I18n.get("myoffers.status.draft");
                    variant = TLBadge.Variant.SECONDARY;
                }
                default -> {
                    label = status.getDisplayName();
                    variant = TLBadge.Variant.DEFAULT;
                }
            }
        }

        TLBadge badge = new TLBadge(label, variant);
        badge.getStyleClass().add("status-badge");
        return badge;
    }

    private HBox createMetadataItem(String iconSvg, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("metadata-item");

        javafx.scene.Node icon = SvgIcons.icon(iconSvg, 14, "-fx-muted-foreground");
        Label label = new Label(text);
        label.getStyleClass().addAll("text-sm", "text-muted");

        box.getChildren().addAll(icon, label);
        return box;
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
                    applyFilters();
                    TLToast.success(offersContainer.getScene(),
                        I18n.get("myoffers.reopen.success.title"),
                        I18n.get("myoffers.reopen.success.message"));
                });
                task.setOnFailed(e -> {
                    offersContainer.setDisable(false);
                    logger.error("Failed to reopen offer", task.getException());
                    TLToast.error(offersContainer.getScene(), I18n.get("common.error"), I18n.get("error.failed_reopen_offer"));
                });
                AppThreadPool.execute(task);
            }
        });
    }

    private void handleViewApplications(JobOffer offer) {
        if (onViewApplications != null) {
            onViewApplications.accept(offer);
            return;
        }
        // Fallback: simple toast if callback not wired
        if (offersContainer.getScene() != null) {
            TLToast.info(offersContainer.getScene(),
                    I18n.get("myoffers.applications.title"),
                    I18n.get("myoffers.applications.message", offer.getTitle()));
        }
    }
}
