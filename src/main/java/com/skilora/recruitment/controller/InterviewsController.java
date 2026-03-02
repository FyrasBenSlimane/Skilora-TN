package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.entity.HireOffer;
import com.skilora.recruitment.ui.InterviewCalendar;
import com.skilora.user.entity.User;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
import com.skilora.recruitment.service.HireOfferService;
import com.skilora.framework.components.*;
import com.skilora.framework.components.TLLoadingState;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

/**
 * InterviewsController - Employer interview management.
 * Shows applications in INTERVIEW+ status with actual Interview records,
 * scheduling, completion with feedback, and hire offer creation.
 */
public class InterviewsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InterviewsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statsLabel;
    @FXML private HBox filterBox;
    @FXML private VBox interviewsContainer;
    @FXML private VBox emptyState;
    @FXML private VBox calendarContainer;
    @FXML private TLButton refreshBtn;

    private InterviewCalendar calendar;

    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final InterviewService interviewService = InterviewService.getInstance();
    private final HireOfferService hireOfferService = HireOfferService.getInstance();

    private User currentUser;
    private List<Application> allInterviews;
    private Map<Integer, List<Interview>> interviewsByAppId = new HashMap<>();
    private String currentFilter = "ALL";
    private volatile boolean isLoading = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (refreshBtn != null) refreshBtn.setText(I18n.get("common.refresh"));
        setupFilters();
        calendar = new InterviewCalendar();
        if (calendarContainer != null) {
            calendarContainer.getChildren().add(calendar);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void setupFilters() {
        String[][] filters = {
            {I18n.get("interviews.filter.all"), "ALL"},
            {I18n.get("interviews.filter.scheduled"), "SCHEDULED"},
            {I18n.get("interviews.filter.to_schedule"), "TO_SCHEDULE"}
        };

        TLTabs tabs = new TLTabs();
        for (String[] f : filters) {
            tabs.addTab(f[1], f[0], (Node) null);
        }
        tabs.setOnTabChanged(tabId -> {
            currentFilter = tabId;
            applyFilters();
        });
        filterBox.getChildren().add(tabs);
    }

    private void loadData() {
        if (currentUser == null || isLoading) return;
        isLoading = true;

        interviewsContainer.getChildren().clear();
        interviewsContainer.getChildren().add(new TLLoadingState());
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        Task<Void> task = new Task<>() {
            private List<Application> apps;
            private List<Interview> interviews;

            @Override
            protected Void call() throws Exception {
                List<Application> all = applicationService.getApplicationsByCompanyOwner(currentUser.getId());
                apps = all.stream()
                    .filter(a -> a.getStatus() == Application.Status.INTERVIEW ||
                                 a.getStatus() == Application.Status.OFFER ||
                                 a.getStatus() == Application.Status.ACCEPTED ||
                                 a.getStatus() == Application.Status.REJECTED)
                    .collect(Collectors.toList());

                interviews = interviewService.findByEmployer(currentUser.getId());
                return null;
            }

            @Override
            protected void succeeded() {
                isLoading = false;
                allInterviews = apps;
                interviewsByAppId.clear();
                for (Interview iv : interviews) {
                    interviewsByAppId
                        .computeIfAbsent(iv.getApplicationId(), k -> new ArrayList<>())
                        .add(iv);
                }
                if (calendar != null) {
                    calendar.setInterviews(interviews);
                }
                applyFilters();
            }

            @Override
            protected void failed() {
                isLoading = false;
                logger.error("Failed to load interviews", getException());
                statsLabel.setText(I18n.get("interviews.error"));
            }
        };

        AppThreadPool.execute(task);
    }

    private void applyFilters() {
        if (allInterviews == null) return;

        List<Application> filtered;
        if ("SCHEDULED".equals(currentFilter)) {
            // Applications that have at least one SCHEDULED interview
            filtered = allInterviews.stream()
                .filter(a -> {
                    List<Interview> ivs = interviewsByAppId.getOrDefault(a.getId(), List.of());
                    return ivs.stream().anyMatch(iv -> "SCHEDULED".equals(iv.getStatus()));
                })
                .collect(Collectors.toList());
        } else if ("TO_SCHEDULE".equals(currentFilter)) {
            // Applications in INTERVIEW status with NO scheduled interview yet
            filtered = allInterviews.stream()
                .filter(a -> a.getStatus() == Application.Status.INTERVIEW)
                .filter(a -> {
                    List<Interview> ivs = interviewsByAppId.getOrDefault(a.getId(), List.of());
                    return ivs.stream().noneMatch(iv -> "SCHEDULED".equals(iv.getStatus()));
                })
                .collect(Collectors.toList());
        } else {
            filtered = allInterviews;
        }

        renderInterviews(filtered);
    }

    private void renderInterviews(List<Application> interviews) {
        interviewsContainer.getChildren().clear();

        if (interviews.isEmpty()) {
            emptyState.getChildren().clear();
            emptyState.getChildren().add(new TLEmptyState(
                SvgIcons.CALENDAR,
                I18n.get("interviews.empty.title"),
                I18n.get("interviews.empty.subtitle")));
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("interviews.count.zero"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        long interviewCount = interviews.stream()
            .filter(a -> a.getStatus() == Application.Status.INTERVIEW).count();
        statsLabel.setText(I18n.get("interviews.count", interviews.size(), interviewCount));

        for (Application app : interviews) {
            List<Interview> appInterviews = interviewsByAppId.getOrDefault(app.getId(), List.of());
            interviewsContainer.getChildren().add(createInterviewCard(app, appInterviews));
        }
    }

    private TLCard createInterviewCard(Application app, List<Interview> interviews) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);

        // Top row: Candidate name + Status badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(app.getCandidateName() != null
            ? app.getCandidateName()
            : I18n.get("interviews.candidate_num", app.getCandidateProfileId()));
        nameLabel.getStyleClass().add("h4");

        TLBadge statusBadge = createStatusBadge(app.getStatus());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(nameLabel, spacer, statusBadge);

        // Job details
        HBox detailsRow = new HBox(16);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        if (app.getJobTitle() != null) {
            Label jobLabel = new Label(app.getJobTitle());
            jobLabel.setGraphic(SvgIcons.icon(SvgIcons.BRIEFCASE, 14, "-fx-muted-foreground"));
            jobLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(jobLabel);
        }

        if (app.getJobLocation() != null) {
            Label locLabel = new Label(app.getJobLocation());
            locLabel.setGraphic(SvgIcons.icon(SvgIcons.MAP_PIN, 14, "-fx-muted-foreground"));
            locLabel.getStyleClass().add("text-muted");
            detailsRow.getChildren().add(locLabel);
        }

        content.getChildren().addAll(topRow, detailsRow);

        // Interview details section
        if (!interviews.isEmpty()) {
            TLSeparator sep1 = new TLSeparator();
            content.getChildren().add(sep1);

            for (Interview iv : interviews) {
                HBox ivRow = new HBox(12);
                ivRow.setAlignment(Pos.CENTER_LEFT);
                ivRow.setPadding(new Insets(4, 0, 4, 0));

                Label dateLabel = new Label(iv.getScheduledDate() != null
                    ? iv.getScheduledDate().format(DATE_FMT) : "—");
                dateLabel.setGraphic(SvgIcons.icon(SvgIcons.CALENDAR, 14, "-fx-muted-foreground"));
                dateLabel.getStyleClass().add("text-muted");

                TLBadge typeBadge = new TLBadge(iv.getType(), TLBadge.Variant.OUTLINE);

                Label durationLabel = new Label(iv.getDurationMinutes() + " min");
                durationLabel.getStyleClass().add("text-muted");

                TLBadge ivStatusBadge = new TLBadge(iv.getStatus(), TLBadge.Variant.SECONDARY);
                if ("COMPLETED".equals(iv.getStatus())) {
                    ivStatusBadge = new TLBadge(iv.getStatus(), TLBadge.Variant.SUCCESS);
                    if (iv.getRating() > 0) {
                        ivStatusBadge.setText(iv.getStatus() + " (" + iv.getRating() + "/5)");
                    }
                } else if ("CANCELLED".equals(iv.getStatus())) {
                    ivStatusBadge = new TLBadge(iv.getStatus(), TLBadge.Variant.DESTRUCTIVE);
                }

                ivRow.getChildren().addAll(dateLabel, typeBadge, durationLabel, ivStatusBadge);

                if (iv.getLocation() != null && !iv.getLocation().isBlank()) {
                    Label locLabel = new Label(iv.getLocation());
                    locLabel.getStyleClass().add("text-muted");
                    ivRow.getChildren().add(locLabel);
                }

                // Countdown widget for scheduled interviews
                if ("SCHEDULED".equals(iv.getStatus()) && iv.getScheduledDate() != null) {
                    InterviewCountdownWidget countdown = InterviewCountdownWidget.of(iv.getScheduledDate());
                    ivRow.getChildren().add(countdown);
                }

                content.getChildren().add(ivRow);
            }
        }

        // Actions
        TLSeparator sep = new TLSeparator();
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (app.getStatus() == Application.Status.INTERVIEW) {
            boolean hasScheduled = interviews.stream().anyMatch(iv -> "SCHEDULED".equals(iv.getStatus()));
            boolean allCompleted = !interviews.isEmpty() &&
                interviews.stream().allMatch(iv -> "COMPLETED".equals(iv.getStatus()) || "CANCELLED".equals(iv.getStatus()));

            TLButton scheduleBtn = new TLButton(
                I18n.get("interviews.schedule"), TLButton.ButtonVariant.OUTLINE);
            scheduleBtn.setGraphic(SvgIcons.icon(SvgIcons.CALENDAR, 14));
            scheduleBtn.setOnAction(e -> showScheduleDialog(app));
            actionsRow.getChildren().add(scheduleBtn);

            if (hasScheduled) {
                TLButton completeBtn = new TLButton(
                    I18n.get("interviews.complete"), TLButton.ButtonVariant.SECONDARY);
                completeBtn.setOnAction(e -> showCompleteDialog(app, interviews));
                actionsRow.getChildren().add(completeBtn);
            }

            if (allCompleted) {
                TLButton offerBtn = new TLButton(
                    I18n.get("interviews.send_offer"), TLButton.ButtonVariant.PRIMARY);
                offerBtn.setOnAction(e -> showHireOfferDialog(app));
                actionsRow.getChildren().add(offerBtn);
            }

            TLButton rejectBtn = new TLButton(
                I18n.get("interviews.reject"), TLButton.ButtonVariant.GHOST);
            rejectBtn.getStyleClass().add("text-destructive");
            rejectBtn.setOnAction(e -> handleReject(app));
            actionsRow.getChildren().add(rejectBtn);

        } else if (app.getStatus() == Application.Status.OFFER) {
            Label waitingLabel = new Label(I18n.get("interviews.waiting_response"));
            waitingLabel.getStyleClass().add("text-muted");
            actionsRow.getChildren().add(waitingLabel);
        } else if (app.getStatus() == Application.Status.ACCEPTED) {
            Label acceptedLabel = new Label(I18n.get("interviews.offer_accepted"));
            acceptedLabel.getStyleClass().addAll("text-muted");
            TLBadge acceptBadge = new TLBadge(I18n.get("interviews.hired"), TLBadge.Variant.SUCCESS);
            actionsRow.getChildren().addAll(acceptedLabel, acceptBadge);
        }

        content.getChildren().addAll(sep, actionsRow);
        card.getContent().add(content);
        return card;
    }

    private TLBadge createStatusBadge(Application.Status status) {
        return switch (status) {
            case INTERVIEW -> new TLBadge(status.getDisplayName(), TLBadge.Variant.OUTLINE);
            case OFFER -> new TLBadge(status.getDisplayName(), TLBadge.Variant.SUCCESS);
            case ACCEPTED -> new TLBadge(status.getDisplayName(), TLBadge.Variant.SUCCESS);
            case REJECTED -> new TLBadge(status.getDisplayName(), TLBadge.Variant.DESTRUCTIVE);
            default -> new TLBadge(status.getDisplayName(), TLBadge.Variant.DEFAULT);
        };
    }

    // ── Schedule Interview Dialog ──

    private void showScheduleDialog(Application app) {
        TLDialog<Interview> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("interviews.schedule_title"));
        dialog.setDescription(I18n.get("interviews.schedule_desc", app.getCandidateName()));

        VBox form = new VBox(12);

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(3));
        datePicker.setPromptText(I18n.get("interviews.pick_date"));
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TLSelect<String> hourSelect = new TLSelect<>();
        for (int h = 8; h <= 18; h++) {
            hourSelect.getItems().add(String.format("%02d:00", h));
            hourSelect.getItems().add(String.format("%02d:30", h));
        }
        hourSelect.setValue("10:00");

        TLSelect<String> typeSelect = new TLSelect<>();
        typeSelect.getItems().addAll("VIDEO", "IN_PERSON", "PHONE");
        typeSelect.setValue("VIDEO");

        TLSelect<String> durationSelect = new TLSelect<>();
        durationSelect.getItems().addAll("30 min", "45 min", "60 min", "90 min");
        durationSelect.setValue("60 min");

        TLTextField locationField = new TLTextField();
        locationField.setPromptText(I18n.get("interviews.location_or_link"));

        TLTextarea notesField = new TLTextarea();
        notesField.setPromptText(I18n.get("interviews.notes_placeholder"));
        notesField.getControl().setPrefRowCount(3);

        form.getChildren().addAll(
            createFormField(I18n.get("interviews.date"), datePicker),
            createFormField(I18n.get("interviews.time"), hourSelect),
            createFormField(I18n.get("interviews.type"), typeSelect),
            createFormField(I18n.get("interviews.duration"), durationSelect),
            createFormField(I18n.get("interviews.location"), locationField),
            createFormField(I18n.get("interviews.notes"), notesField)
        );

        dialog.setDialogContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                if (datePicker.getValue() == null) {
                    datePicker.setStyle("-fx-border-color: -fx-destructive;");
                    event.consume();
                } else {
                    datePicker.setStyle("");
                }
            }
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                LocalDate date = datePicker.getValue();

                String timeStr = hourSelect.getValue();
                String[] parts = timeStr.split(":");
                LocalTime time = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

                String durStr = durationSelect.getValue();
                int duration = Integer.parseInt(durStr.replaceAll("[^0-9]", ""));

                Interview iv = new Interview();
                iv.setApplicationId(app.getId());
                iv.setScheduledDate(LocalDateTime.of(date, time));
                iv.setDurationMinutes(duration);
                iv.setType(typeSelect.getValue());
                iv.setLocation(locationField.getText());
                iv.setNotes(notesField.getText());
                return iv;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(interview -> {
            AppThreadPool.execute(() -> {
                int id = interviewService.create(interview);
                javafx.application.Platform.runLater(() -> {
                    if (id > 0) {
                        TLToast.success(interviewsContainer.getScene(),
                            I18n.get("interviews.scheduled_success"),
                            app.getCandidateName());
                        loadData();
                    } else {
                        TLToast.error(interviewsContainer.getScene(),
                            I18n.get("common.error"),
                            I18n.get("interviews.schedule_failed"));
                    }
                });
            });
        });
    }

    // ── Complete Interview Dialog ──

    private void showCompleteDialog(Application app, List<Interview> interviews) {
        Interview scheduled = interviews.stream()
            .filter(iv -> "SCHEDULED".equals(iv.getStatus()))
            .findFirst().orElse(null);

        if (scheduled == null) {
            TLToast.error(interviewsContainer.getScene(),
                I18n.get("common.error"),
                I18n.get("interviews.no_scheduled"));
            return;
        }

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("interviews.complete_title"));
        dialog.setDescription(I18n.get("interviews.complete_desc",
            app.getCandidateName(),
            scheduled.getScheduledDate() != null ? scheduled.getScheduledDate().format(DATE_FMT) : ""));

        VBox form = new VBox(12);

        TLSelect<String> ratingSelect = new TLSelect<>();
        ratingSelect.getItems().addAll("1 - Poor", "2 - Below Average", "3 - Average", "4 - Good", "5 - Excellent");
        ratingSelect.setValue("3 - Average");

        TLTextarea feedbackField = new TLTextarea();
        feedbackField.setPromptText(I18n.get("interviews.feedback_placeholder"));
        feedbackField.getControl().setPrefRowCount(4);

        form.getChildren().addAll(
            createFormField(I18n.get("interviews.rating"), ratingSelect),
            createFormField(I18n.get("interviews.feedback"), feedbackField)
        );

        dialog.setDialogContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                int rating = Integer.parseInt(ratingSelect.getValue().substring(0, 1));
                String feedback = feedbackField.getText();

                AppThreadPool.execute(() -> {
                    boolean success = interviewService.complete(scheduled.getId(), feedback, rating);
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            TLToast.success(interviewsContainer.getScene(),
                                I18n.get("interviews.completed_success"),
                                app.getCandidateName());
                            loadData();
                        }
                    });
                });
            }
        });
    }

    // ── Hire Offer Dialog ──

    private void showHireOfferDialog(Application app) {
        TLDialog<HireOffer> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("interviews.offer_title"));
        dialog.setDescription(I18n.get("interviews.offer_desc", app.getCandidateName()));

        VBox form = new VBox(12);

        TLTextField salaryField = new TLTextField();
        salaryField.setPromptText("e.g. 3500.00");

        TLSelect<String> currencySelect = new TLSelect<>();
        currencySelect.getItems().addAll("TND", "EUR", "USD", "GBP");
        currencySelect.setValue("TND");

        TLSelect<String> contractSelect = new TLSelect<>();
        contractSelect.getItems().addAll("CDI", "CDD", "FREELANCE", "STAGE");
        contractSelect.setValue("CDI");

        DatePicker startDatePicker = new DatePicker(LocalDate.now().plusMonths(1));

        TLTextarea benefitsField = new TLTextarea();
        benefitsField.setPromptText(I18n.get("interviews.benefits_placeholder"));
        benefitsField.getControl().setPrefRowCount(3);

        form.getChildren().addAll(
            createFormField(I18n.get("interviews.salary"), salaryField),
            createFormField(I18n.get("interviews.currency"), currencySelect),
            createFormField(I18n.get("interviews.contract_type"), contractSelect),
            createFormField(I18n.get("interviews.start_date"), startDatePicker),
            createFormField(I18n.get("interviews.benefits"), benefitsField)
        );

        dialog.setDialogContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String salaryText = salaryField.getText() != null ? salaryField.getText().trim() : "";
                if (salaryText.isEmpty()) {
                    salaryField.setError(I18n.get("error.validation.required", I18n.get("interviews.salary")));
                    event.consume();
                } else {
                    try {
                        Double.parseDouble(salaryText);
                        salaryField.clearValidation();
                    } catch (NumberFormatException e) {
                        salaryField.setError(I18n.get("error.validation.number"));
                        event.consume();
                    }
                }
            }
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                double salary = Double.parseDouble(salaryField.getText().trim());
                HireOffer offer = new HireOffer();
                offer.setApplicationId(app.getId());
                offer.setSalaryOffered(salary);
                offer.setCurrency(currencySelect.getValue());
                offer.setContractType(contractSelect.getValue());
                offer.setStartDate(startDatePicker.getValue());
                offer.setBenefits(benefitsField.getText());
                return offer;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(offer -> {
            // Disable the card area to prevent double hire offers
            interviewsContainer.setDisable(true);
            AppThreadPool.execute(() -> {
                try {
                    int offerId = hireOfferService.create(offer);
                    if (offerId > 0) {
                        applicationService.updateStatus(app.getId(), Application.Status.OFFER);
                    }
                    javafx.application.Platform.runLater(() -> {
                        interviewsContainer.setDisable(false);
                        if (offerId > 0) {
                            TLToast.success(interviewsContainer.getScene(),
                                I18n.get("interviews.offer_sent_success"),
                                app.getCandidateName());
                            loadData();
                        } else {
                            TLToast.error(interviewsContainer.getScene(),
                                I18n.get("common.error"),
                                I18n.get("interviews.offer_send_failed"));
                        }
                    });
                } catch (Exception ex) {
                    logger.error("Failed to send hire offer", ex);
                    javafx.application.Platform.runLater(() -> {
                        interviewsContainer.setDisable(false);
                        TLToast.error(interviewsContainer.getScene(),
                            I18n.get("common.error"), ex.getMessage());
                    });
                }
            });
        });
    }

    // ── Reject Handler ──

    private void handleReject(Application app) {
        DialogUtils.showConfirmation(
            I18n.get("interviews.reject.confirm.title"),
            I18n.get("interviews.reject.confirm.message",
                app.getCandidateName() != null ? app.getCandidateName() : "")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                AppThreadPool.execute(() -> {
                    try {
                        applicationService.updateStatus(app.getId(), Application.Status.REJECTED);
                        javafx.application.Platform.runLater(() -> {
                            TLToast.success(interviewsContainer.getScene(),
                                I18n.get("interviews.rejected_success"), "");
                            loadData();
                        });
                    } catch (Exception ex) {
                        logger.error("Failed to reject", ex);
                    }
                });
            }
        });
    }

    // ── Helpers ──

    private VBox createFormField(String label, Node input) {
        VBox field = new VBox(4);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        field.getChildren().addAll(lbl, input);
        return field;
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
