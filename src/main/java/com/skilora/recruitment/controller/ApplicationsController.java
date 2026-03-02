package com.skilora.recruitment.controller;

import com.skilora.framework.components.*;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.HireOffer;
import com.skilora.formation.service.FormationMatchingService;
import com.skilora.formation.service.FormationMatchingService.ScoredFormation;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.HireOfferService;
import com.skilora.recruitment.service.RecruitmentFinanceBridge;
import com.skilora.user.service.ProfileService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
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

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

/**
 * ApplicationsController - Kanban board for tracking job applications (Job Seeker view)
 */
public class ApplicationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsController.class);

    @FXML private Label statsLabel;
    @FXML private TLButton refreshBtn;
    
    @FXML private Label appliedCount;
    @FXML private Label reviewingCount;
    @FXML private Label interviewingCount;
    @FXML private Label offerCount;
    @FXML private Label appliedHeader;
    @FXML private Label reviewingHeader;
    @FXML private Label interviewingHeader;
    @FXML private Label offerHeader;
    
    @FXML private VBox appliedColumn;
    @FXML private VBox reviewingColumn;
    @FXML private VBox interviewingColumn;
    @FXML private VBox offerColumn;

    private User currentUser;
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final HireOfferService hireOfferService = HireOfferService.getInstance();
    private final RecruitmentFinanceBridge recruitmentFinanceBridge = RecruitmentFinanceBridge.getInstance();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        refreshBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14));
        if (appliedHeader != null) appliedHeader.setGraphic(SvgIcons.icon(SvgIcons.FILE_TEXT, 14));
        if (reviewingHeader != null) reviewingHeader.setGraphic(SvgIcons.icon(SvgIcons.EYE, 14));
        if (interviewingHeader != null) interviewingHeader.setGraphic(SvgIcons.icon(SvgIcons.MESSAGE_CIRCLE, 14));
        if (offerHeader != null) offerHeader.setGraphic(SvgIcons.icon(SvgIcons.SPARKLES, 14));
    }

    private void applyI18n() {
        refreshBtn.setText(I18n.get("common.refresh"));
        if (appliedHeader != null) appliedHeader.setText(I18n.get("applications.applied"));
        if (reviewingHeader != null) reviewingHeader.setText(I18n.get("applications.in_progress"));
        if (interviewingHeader != null) interviewingHeader.setText(I18n.get("applications.interview"));
        if (offerHeader != null) offerHeader.setText(I18n.get("applications.offer"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadApplications();
    }
    
    private void loadApplications() {
        appliedColumn.getChildren().clear();
        reviewingColumn.getChildren().clear();
        interviewingColumn.getChildren().clear();
        offerColumn.getChildren().clear();
        appliedColumn.getChildren().add(new TLLoadingState());
        statsLabel.setText(I18n.get("common.loading"));

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                // Get profile ID for this user
                var profile = ProfileService.getInstance()
                        .findProfileByUserId(currentUser.getId());
                if (profile == null) return List.of();
                return applicationService.getApplicationsByProfile(profile.getId());
            }
        };

        task.setOnSucceeded(e -> {
            appliedColumn.getChildren().clear();
            List<Application> apps = task.getValue();
            for (Application app : apps) {
                TLCard card = createApplicationCard(
                        app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()),
                        app.getCompanyName() != null ? app.getCompanyName() : "",
                        app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "");

                switch (app.getStatus()) {
                    case PENDING -> appliedColumn.getChildren().add(card);
                    case REVIEWING -> reviewingColumn.getChildren().add(card);
                    case INTERVIEW -> interviewingColumn.getChildren().add(card);
                    case OFFER -> offerColumn.getChildren().add(createOfferCard(app));
                    case ACCEPTED -> offerColumn.getChildren().add(createAcceptedCard(app));
                    case REJECTED -> appliedColumn.getChildren().add(createRejectedCard(app));
                }
            }

            updateCounts();
        });

        task.setOnFailed(e -> {
            statsLabel.setText(I18n.get("applications.error"));
            logger.error("Failed to load applications", task.getException());
        });

        AppThreadPool.execute(task);
    }

    private void updateCounts() {
        appliedCount.setText(String.valueOf(appliedColumn.getChildren().size()));
        reviewingCount.setText(String.valueOf(reviewingColumn.getChildren().size()));
        interviewingCount.setText(String.valueOf(interviewingColumn.getChildren().size()));
        offerCount.setText(String.valueOf(offerColumn.getChildren().size()));

        int total = appliedColumn.getChildren().size() + reviewingColumn.getChildren().size()
                + interviewingColumn.getChildren().size() + offerColumn.getChildren().size();
        statsLabel.setText(I18n.get("applications.count", total));
    }
    
    private TLCard createApplicationCard(String title, String company, String date) {
        TLCard card = new TLCard();
        
        VBox content = new VBox(8);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);
        
        Label companyLabel = new Label(company);
        companyLabel.getStyleClass().add("text-muted");
        
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().addAll("text-2xs", "text-muted");
        
        content.getChildren().addAll(titleLabel, companyLabel, dateLabel);
        card.getContent().add(content);
        
        return card;
    }
    
    private TLCard createRejectedCard(Application app) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);

        Label titleLabel = new Label(app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()));
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        TLBadge rejectedBadge = new TLBadge(I18n.get("applications.rejected"), TLBadge.Variant.DESTRUCTIVE);

        Label dateLabel = new Label(app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "");
        dateLabel.getStyleClass().addAll("text-2xs", "text-muted");

        TLButton suggestBtn = new TLButton(I18n.get("applications.get_training"), TLButton.ButtonVariant.OUTLINE);
        suggestBtn.setMaxWidth(Double.MAX_VALUE);
        suggestBtn.setGraphic(SvgIcons.icon(SvgIcons.GRADUATION_CAP, 14));
        suggestBtn.setOnAction(e -> showTrainingSuggestions(app));

        content.getChildren().addAll(titleLabel, rejectedBadge, dateLabel, new TLSeparator(), suggestBtn);
        card.getContent().add(content);
        return card;
    }

    private TLCard createAcceptedCard(Application app) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);

        Label titleLabel = new Label(app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()));
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        TLBadge acceptedBadge = new TLBadge(I18n.get("applications.accepted"), TLBadge.Variant.SUCCESS);

        Label companyLabel = new Label(app.getCompanyName() != null ? app.getCompanyName() : "");
        companyLabel.getStyleClass().add("text-muted");

        Label dateLabel = new Label(app.getAppliedDate() != null ? app.getAppliedDate().format(DATE_FMT) : "");
        dateLabel.getStyleClass().addAll("text-2xs", "text-muted");

        content.getChildren().addAll(titleLabel, acceptedBadge, companyLabel, dateLabel);
        card.getContent().add(content);
        return card;
    }

    private TLCard createOfferCard(Application app) {
        TLCard card = new TLCard();
        VBox content = new VBox(10);

        Label titleLabel = new Label(app.getJobTitle() != null ? app.getJobTitle() : I18n.get("applications.offer_num", app.getJobOfferId()));
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        TLBadge offerBadge = new TLBadge(I18n.get("applications.offer_received"), TLBadge.Variant.SECONDARY);

        Label companyLabel = new Label(app.getCompanyName() != null ? app.getCompanyName() : "");
        companyLabel.getStyleClass().add("text-muted");

        VBox offerDetailsBox = new VBox(6);
        offerDetailsBox.getChildren().add(new TLLoadingState(I18n.get("common.loading")));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        TLButton rejectBtn = new TLButton(I18n.get("common.reject"), TLButton.ButtonVariant.OUTLINE);
        rejectBtn.setGraphic(SvgIcons.icon(SvgIcons.X_CIRCLE, 14));
        TLButton acceptBtn = new TLButton(I18n.get("common.accept"), TLButton.ButtonVariant.PRIMARY);
        acceptBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
        rejectBtn.setDisable(true);
        acceptBtn.setDisable(true);

        actions.getChildren().addAll(rejectBtn, acceptBtn);

        content.getChildren().addAll(titleLabel, offerBadge, companyLabel, new TLSeparator(), offerDetailsBox, actions);
        card.getContent().add(content);

        AppThreadPool.execute(() -> {
            HireOffer offer = null;
            try {
                List<HireOffer> offers = hireOfferService.findByApplication(app.getId());
                offer = offers.stream().filter(HireOffer::isPending).findFirst().orElse(offers.isEmpty() ? null : offers.get(0));
            } catch (Exception ex) {
                logger.error("Failed loading hire offer for application {}", app.getId(), ex);
            }

            HireOffer finalOffer = offer;
            Platform.runLater(() -> {
                offerDetailsBox.getChildren().clear();
                if (finalOffer == null) {
                    offerDetailsBox.getChildren().add(new TLEmptyState(
                            SvgIcons.SPARKLES,
                            I18n.get("applications.offer_missing"),
                            I18n.get("applications.offer_missing_desc")));
                    return;
                }

                Label salary = new Label(I18n.get("applications.salary", finalOffer.getFormattedSalary()));
                salary.getStyleClass().add("text-sm");

                Label contractType = new Label(I18n.get("applications.contract_type", finalOffer.getContractType()));
                contractType.getStyleClass().add("text-muted");

                Label start = new Label(finalOffer.getStartDate() != null
                        ? I18n.get("applications.start_date", finalOffer.getStartDate())
                        : I18n.get("applications.start_date_na"));
                start.getStyleClass().addAll("text-2xs", "text-muted");

                offerDetailsBox.getChildren().addAll(salary, contractType, start);

                rejectBtn.setDisable(false);
                acceptBtn.setDisable(false);

                rejectBtn.setOnAction(e -> handleOfferReject(finalOffer, app));
                acceptBtn.setOnAction(e -> handleOfferAccept(finalOffer, app));
            });
        });

        return card;
    }

    private void handleOfferAccept(HireOffer offer, Application app) {
        if (offer == null) return;
        boolean confirmed = DialogUtils.showConfirmation(
                I18n.get("applications.offer_accept_title"),
                I18n.get("applications.offer_accept_desc", offer.getFormattedSalary())
        ).orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (!confirmed) return;

        // Disable the entire offer column to prevent double-accept
        offerColumn.setDisable(true);

        AppThreadPool.execute(() -> {
            int contractId = recruitmentFinanceBridge.acceptOfferAndGenerateContract(offer.getId());
            Platform.runLater(() -> {
                offerColumn.setDisable(false);
                if (contractId > 0) {
                    TLToast.success(offerColumn.getScene(),
                            I18n.get("applications.offer_accepted"),
                            I18n.get("applications.contract_generated", contractId));
                } else {
                    TLToast.error(offerColumn.getScene(),
                            I18n.get("common.error"),
                            I18n.get("applications.offer_accept_failed"));
                }
                loadApplications();
            });
        });
    }

    private void handleOfferReject(HireOffer offer, Application app) {
        if (offer == null) return;
        boolean confirmed = DialogUtils.showConfirmation(
                I18n.get("applications.offer_reject_title"),
                I18n.get("applications.offer_reject_desc")
        ).orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (!confirmed) return;

        // Disable to prevent double-reject
        offerColumn.setDisable(true);

        AppThreadPool.execute(() -> {
            boolean rejected = hireOfferService.reject(offer.getId());
            try {
                if (rejected) {
                    applicationService.updateStatus(app.getId(), Application.Status.REJECTED);
                }
            } catch (Exception ex) {
                logger.error("Failed updating application status after offer reject", ex);
            }
            Platform.runLater(() -> {
                offerColumn.setDisable(false);
                if (rejected) {
                    TLToast.success(offerColumn.getScene(),
                            I18n.get("applications.offer_rejected"),
                            I18n.get("applications.offer_rejected_desc"));
                } else {
                    TLToast.error(offerColumn.getScene(),
                            I18n.get("common.error"),
                            I18n.get("applications.offer_reject_failed"));
                }
                loadApplications();
            });
        });
    }

    private void showTrainingSuggestions(Application app) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("applications.training_suggestions"));
        dialog.setDescription(I18n.get("applications.training_suggestions_desc", app.getJobTitle()));

        VBox suggestionsBox = new VBox(12);

        Label loadingLabel = new Label(I18n.get("common.loading"));
        loadingLabel.getStyleClass().add("text-muted");
        suggestionsBox.getChildren().add(loadingLabel);

        dialog.setDialogContent(suggestionsBox);
        dialog.addButton(javafx.scene.control.ButtonType.CLOSE);

        AppThreadPool.execute(() -> {
            try {
                List<ScoredFormation> suggestions = FormationMatchingService.getInstance()
                    .getSuggestionsForRejected(currentUser.getId(), app.getJobOfferId());

                Platform.runLater(() -> {
                    suggestionsBox.getChildren().clear();
                    if (suggestions.isEmpty()) {
                        Label emptyLabel = new Label(I18n.get("applications.no_suggestions"));
                        emptyLabel.getStyleClass().add("text-muted");
                        suggestionsBox.getChildren().add(emptyLabel);
                        return;
                    }

                    for (ScoredFormation sf : suggestions) {
                        TLCard suggCard = new TLCard();
                        VBox cardContent = new VBox(6);

                        HBox topRow = new HBox(8);
                        topRow.setAlignment(Pos.CENTER_LEFT);
                        Label name = new Label(sf.getFormation().getTitle());
                        name.getStyleClass().add("h4");
                        name.setWrapText(true);
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        TLBadge scoreBadge = new TLBadge(
                            String.format("%.0f%%", sf.getScore()), TLBadge.Variant.SUCCESS);
                        topRow.getChildren().addAll(name, spacer, scoreBadge);

                        Label reason = new Label(sf.getReason());
                        reason.getStyleClass().add("text-muted");
                        reason.setWrapText(true);

                        HBox metaRow = new HBox(12);
                        Label catLabel = new Label(sf.getFormation().getCategory());
                        catLabel.getStyleClass().add("text-muted");
                        Label durLabel = new Label(sf.getFormation().getDurationHours() + "h");
                        durLabel.getStyleClass().add("text-muted");
                        metaRow.getChildren().addAll(catLabel, durLabel);

                        cardContent.getChildren().addAll(topRow, reason, metaRow);
                        suggCard.getContent().add(cardContent);
                        suggestionsBox.getChildren().add(suggCard);
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to load training suggestions", ex);
                Platform.runLater(() -> {
                    suggestionsBox.getChildren().clear();
                    Label errorLabel = new Label(I18n.get("common.error"));
                    errorLabel.getStyleClass().add("text-destructive");
                    suggestionsBox.getChildren().add(errorLabel);
                });
            }
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        if (currentUser != null) {
            loadApplications();
        }
    }
}
