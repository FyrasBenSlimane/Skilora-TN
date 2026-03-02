package com.skilora.support.controller;

import com.skilora.user.entity.*;
import com.skilora.support.entity.*;
import com.skilora.support.service.*;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.SvgIcons;
import com.skilora.framework.components.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class SupportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TLButton newTicketBtn;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;

    private User currentUser;

    private final SupportTicketService ticketService = SupportTicketService.getInstance();
    private final TicketMessageService messageService = TicketMessageService.getInstance();
    private final TicketAttachmentService attachmentService = TicketAttachmentService.getInstance();
    private final FAQService faqService = FAQService.getInstance();
    private final ChatbotService chatbotService = ChatbotService.getInstance();
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();
    private final GeminiAIService geminiService = GeminiAIService.getInstance();
    private final AudioRecorderService audioRecorder = AudioRecorderService.getInstance();
    private final DeepgramSTTService deepgramService = DeepgramSTTService.getInstance();

    private void enterTicketDetailMode() {
        setHeaderControlsVisible(false);
    }

    private void exitTicketDetailMode() {
        setHeaderControlsVisible(true);
    }

    private void setHeaderControlsVisible(boolean visible) {
        if (newTicketBtn != null) {
            newTicketBtn.setVisible(visible);
            newTicketBtn.setManaged(visible);
        }
        if (tabBox != null) {
            tabBox.setVisible(visible);
            tabBox.setManaged(visible);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("support.title"));
        subtitleLabel.setText(I18n.get("support.subtitle"));
        newTicketBtn.setText(I18n.get("ticket.new"));
        setupTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadTicketsTab();
    }

    // ==================== Tab Navigation ====================

    private void setupTabs() {
        tabBox.getChildren().clear();
        TLTabs tabs = new TLTabs();
        tabs.addTab("tickets", I18n.get("support.tab.tickets"), (Node) null);
        tabs.addTab("faq", I18n.get("support.tab.faq"), (Node) null);
        tabs.addTab("chatbot", I18n.get("support.tab.chatbot"), (Node) null);
        tabs.addTab("feedback", I18n.get("support.tab.feedback"), (Node) null);
        tabs.setOnTabChanged(tabId -> {
            exitTicketDetailMode();
            switch (tabId) {
                case "tickets" -> loadTicketsTab();
                case "faq" -> loadFAQTab();
                case "chatbot" -> loadChatbotTab();
                case "feedback" -> loadFeedbackTab();
            }
        });
        tabBox.getChildren().add(tabs);
    }

    // ==================== New Ticket Dialog ====================

    @FXML
    private void handleNewTicket() {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("ticket.new"));
        dialog.setDescription(I18n.get("ticket.new.description"));

        VBox form = new VBox(16);

        TLTextField subjectField = new TLTextField(I18n.get("ticket.subject"), I18n.get("ticket.subject.placeholder"));

        TLSelect<String> categorySelect = new TLSelect<>(I18n.get("ticket.category"));
        categorySelect.getItems().addAll(
            I18n.get("ticket.category.general"),
            I18n.get("ticket.category.technical"),
            I18n.get("ticket.category.billing"),
            I18n.get("ticket.category.account"),
            I18n.get("ticket.category.job"),
            I18n.get("ticket.category.formation")
        );
        categorySelect.setValue(I18n.get("ticket.category.general"));

        TLSelect<String> prioritySelect = new TLSelect<>(I18n.get("ticket.priority"));
        prioritySelect.getItems().addAll(
            I18n.get("ticket.priority.low"),
            I18n.get("ticket.priority.medium"),
            I18n.get("ticket.priority.high"),
            I18n.get("ticket.priority.urgent")
        );
        prioritySelect.setValue(I18n.get("ticket.priority.medium"));

        TLTextarea descriptionArea = new TLTextarea(I18n.get("ticket.description"), I18n.get("ticket.description.placeholder"));
        descriptionArea.getControl().setPrefRowCount(6);

        // AI-powered buttons from branch integration
        HBox aiButtonRow = new HBox(8);
        aiButtonRow.setAlignment(Pos.CENTER_LEFT);

        TLButton aiCorrectBtn = new TLButton("✨ AI Correct", TLButton.ButtonVariant.OUTLINE);
        aiCorrectBtn.setTooltip(new Tooltip("Let AI fix grammar and spelling"));
        aiCorrectBtn.setOnAction(e -> {
            String text = descriptionArea.getText();
            if (text == null || text.trim().isEmpty()) return;
            aiCorrectBtn.setDisable(true);
            Task<String> aiTask = new Task<>() {
                @Override protected String call() { return geminiService.correctText(text); }
            };
            aiTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                String corrected = aiTask.getValue();
                if (corrected != null && !corrected.isBlank()) {
                    descriptionArea.setText(corrected);
                }
                aiCorrectBtn.setDisable(false);
            }));
            aiTask.setOnFailed(ev -> Platform.runLater(() -> aiCorrectBtn.setDisable(false)));
            AppThreadPool.execute(aiTask);
        });

        TLButton aiCategorizeBtn = new TLButton("✨ AI Categorize", TLButton.ButtonVariant.OUTLINE);
        aiCategorizeBtn.setTooltip(new Tooltip("Let AI predict the category"));
        aiCategorizeBtn.setOnAction(e -> {
            String subject = subjectField.getText();
            String desc = descriptionArea.getText();
            if ((subject == null || subject.trim().isEmpty()) && (desc == null || desc.trim().isEmpty())) return;
            aiCategorizeBtn.setDisable(true);
            Task<String> aiTask = new Task<>() {
                @Override protected String call() {
                    return geminiService.predictCategory(
                        subject != null ? subject : "",
                        desc != null ? desc : "");
                }
            };
            aiTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                String predicted = aiTask.getValue();
                if (predicted != null && !predicted.isBlank()) {
                    String mapped = mapPredictedCategory(predicted);
                    categorySelect.setValue(mapped);
                }
                aiCategorizeBtn.setDisable(false);
            }));
            aiTask.setOnFailed(ev -> Platform.runLater(() -> aiCategorizeBtn.setDisable(false)));
            AppThreadPool.execute(aiTask);
        });

        aiButtonRow.getChildren().addAll(aiCorrectBtn, aiCategorizeBtn);

        form.getChildren().addAll(subjectField, categorySelect, prioritySelect, descriptionArea, aiButtonRow);

        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String subject = subjectField.getText() != null ? subjectField.getText().trim() : "";
                if (subject.isEmpty()) {
                    subjectField.setError(I18n.get("error.validation.required", I18n.get("ticket.subject")));
                    valid = false;
                } else {
                    subjectField.clearValidation();
                }
                String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
                if (description.isEmpty()) {
                    descriptionArea.setError(I18n.get("error.validation.required", I18n.get("ticket.description")));
                    valid = false;
                } else {
                    descriptionArea.clearValidation();
                }
                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                createTicket(
                    subjectField.getText().trim(),
                    getCategoryKey(categorySelect.getValue()),
                    getPriorityKey(prioritySelect.getValue()),
                    descriptionArea.getText().trim());
            }
        });
    }

    private void createTicket(String subject, String category, String priority, String description) {
        if (currentUser == null) return;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                SupportTicket ticket = new SupportTicket();
                ticket.setUserId(currentUser.getId());
                ticket.setSubject(subject);
                ticket.setCategory(category);
                ticket.setPriority(priority);
                ticket.setStatus("OPEN");
                ticket.setDescription(description);
                return ticketService.create(ticket);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.created"));
            loadTicketsTab();
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
            DialogUtils.showError(I18n.get("message.error"), I18n.get("error.db.operation"))));
        AppThreadPool.execute(task);
    }

    // ==================== Tickets List ====================

    private void loadTicketsTab() {
        if (currentUser == null) return;
        exitTicketDetailMode();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(new TLLoadingState());

        Task<List<SupportTicket>> task = new Task<>() {
            @Override protected List<SupportTicket> call() {
                return ticketService.findByUserId(currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> displayTickets(task.getValue())));
        task.setOnFailed(e -> logger.error("Failed to load tickets", task.getException()));
        AppThreadPool.execute(task);
    }

    private String activeFilter = null; // null = show all

    private void displayTickets(List<SupportTicket> tickets) {
        contentPane.getChildren().clear();

        if (tickets.isEmpty()) {
            TLEmptyState empty = new TLEmptyState(
                SvgIcons.MESSAGE_CIRCLE,
                I18n.get("ticket.empty"),
                I18n.get("ticket.empty.desc")
            );
            VBox.setVgrow(empty, Priority.ALWAYS);
            contentPane.getChildren().add(empty);
            return;
        }

        VBox wrapper = new VBox(12);

        // ── Filter Chips (OPEN | IN_PROGRESS | RESOLVED | CLOSED | All) ──
        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(0, 0, 4, 0));

        String[] filters = {"ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};
        for (String filter : filters) {
            String label = "ALL".equals(filter)
                    ? I18n.get("common.all")
                    : I18n.get("ticket.status." + filter.toLowerCase());
            TLButton chip = new TLButton(label,
                    (activeFilter == null && "ALL".equals(filter)) || filter.equals(activeFilter)
                            ? TLButton.ButtonVariant.PRIMARY
                            : TLButton.ButtonVariant.OUTLINE);
            chip.getStyleClass().add("text-sm");
            long count = "ALL".equals(filter) ? tickets.size()
                    : tickets.stream().filter(t -> filter.equalsIgnoreCase(t.getStatus())).count();
            if (count > 0 && !"ALL".equals(filter)) {
                chip.setText(label + " (" + count + ")");
            }
            chip.setOnAction(e -> {
                activeFilter = "ALL".equals(filter) ? null : filter;
                displayTickets(tickets);
            });
            filterRow.getChildren().add(chip);
        }
        wrapper.getChildren().add(filterRow);

        // ── Filtered Ticket List ──
        VBox ticketList = new VBox(8);
        for (SupportTicket ticket : tickets) {
            if (activeFilter != null && !activeFilter.equalsIgnoreCase(ticket.getStatus())) continue;
            ticketList.getChildren().add(createTicketCard(ticket));
        }

        if (ticketList.getChildren().isEmpty()) {
            TLEmptyState empty = new TLEmptyState(
                SvgIcons.FILTER,
                I18n.get("ticket.empty"),
                ""
            );
            ticketList.getChildren().add(empty);
        }

        ScrollPane scrollPane = new ScrollPane(ticketList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        wrapper.getChildren().add(scrollPane);
        contentPane.getChildren().add(wrapper);
    }

    private TLCard createTicketCard(SupportTicket ticket) {
        TLCard card = new TLCard();
        card.getStyleClass().add("card-interactive");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Priority strip (left color bar)
        Region strip = new Region();
        strip.getStyleClass().addAll("ticket-priority-strip", "priority-" + (ticket.getPriority() != null ? ticket.getPriority().toLowerCase() : "medium"));
        strip.setMinWidth(4);
        strip.setMaxWidth(4);
        strip.setMinHeight(48);

        // Main info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subject = new Label(ticket.getSubject());
        subject.getStyleClass().addAll("text-base", "font-bold");

        if ("OPEN".equalsIgnoreCase(ticket.getStatus())) {
            Region dot = new Region();
            dot.getStyleClass().add("unread-dot");
            dot.setMinSize(8, 8);
            dot.setMaxSize(8, 8);
            titleRow.getChildren().addAll(subject, dot);
        } else {
            titleRow.getChildren().add(subject);
        }

        // Description preview
        String desc = ticket.getDescription() != null ? ticket.getDescription() : "";
        Label preview = new Label(desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
        preview.setWrapText(true);
        preview.getStyleClass().add("text-muted");
        preview.setMaxHeight(36);

        // Footer: category + date
        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label category = new Label(ticket.getCategory() != null ? ticket.getCategory().toUpperCase() : "");
        category.getStyleClass().addAll("text-2xs", "font-bold", "text-muted");

        Label date = new Label(formatRelativeTime(ticket.getCreatedDate()));
        date.getStyleClass().addAll("text-2xs", "text-muted");

        meta.getChildren().addAll(category, date);

        // SLA countdown (only for open/in-progress tickets with SLA)
        if (ticket.getSlaDueDate() != null && 
                ("OPEN".equalsIgnoreCase(ticket.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(ticket.getStatus()))) {
            long minutesLeft = java.time.temporal.ChronoUnit.MINUTES.between(java.time.LocalDateTime.now(), ticket.getSlaDueDate());
            String slaText;
            String slaStyle;
            if (minutesLeft < 0) {
                long hoursOverdue = Math.abs(minutesLeft) / 60;
                slaText = "SLA breached (" + hoursOverdue + "h overdue)";
                slaStyle = "-fx-text-fill: -fx-destructive; -fx-font-weight: bold;";
            } else if (minutesLeft < 120) {
                slaText = "SLA: " + minutesLeft + "min left";
                slaStyle = "-fx-text-fill: -fx-destructive;";
            } else {
                long hoursLeft = minutesLeft / 60;
                slaText = "SLA: " + hoursLeft + "h left";
                slaStyle = minutesLeft < 360 ? "-fx-text-fill: #f59e0b;" : "-fx-text-fill: -fx-muted-foreground;";
            }
            Label slaLabel = new Label(slaText);
            slaLabel.getStyleClass().add("text-2xs");
            slaLabel.setStyle(slaStyle);
            meta.getChildren().add(slaLabel);
        }

        info.getChildren().addAll(titleRow, preview, meta);

        // Badges on right
        VBox badges = new VBox(6);
        badges.setAlignment(Pos.CENTER_RIGHT);

        TLBadge statusBadge = new TLBadge(
            I18n.get("ticket.status." + ticket.getStatus().toLowerCase()),
            getBadgeVariant(ticket.getStatus())
        );
        TLBadge priorityBadge = new TLBadge(
            ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
            getPriorityBadgeVariant(ticket.getPriority())
        );
        badges.getChildren().addAll(statusBadge, priorityBadge);

        // Chevron arrow
        Node chevron = SvgIcons.icon(SvgIcons.ARROW_RIGHT, 14);
        chevron.getStyleClass().add("text-muted");

        row.getChildren().addAll(strip, info, badges, chevron);
        card.setContent(row);

        card.setOnMouseClicked(e -> openTicketDetail(ticket.getId()));
        return card;
    }

    // ==================== Ticket Detail View ====================

    private void openTicketDetail(int ticketId) {
        enterTicketDetailMode();
        contentPane.getChildren().setAll(new TLLoadingState());
        Task<SupportTicket> ticketTask = new Task<>() {
            @Override protected SupportTicket call() { return ticketService.findById(ticketId); }
        };

        ticketTask.setOnSucceeded(e -> {
            SupportTicket ticket = ticketTask.getValue();
            if (ticket == null) {
                Platform.runLater(() -> DialogUtils.showError(I18n.get("message.error"), I18n.get("error.ticket.not_found")));
                return;
            }

            Task<TicketThreadData> threadTask = new Task<>() {
                @Override
                protected TicketThreadData call() {
                    List<TicketMessage> messages = messageService.findByTicketIdPublic(ticketId);
                    Map<Integer, List<TicketAttachment>> byMessage = new HashMap<>();
                    List<TicketAttachment> atts = attachmentService.findByTicketId(ticketId);
                    for (TicketAttachment a : atts) {
                        if (a.getMessageId() == null) continue;
                        byMessage.computeIfAbsent(a.getMessageId(), k -> new ArrayList<>()).add(a);
                    }
                    return new TicketThreadData(messages, byMessage);
                }
            };
            threadTask.setOnSucceeded(ev -> Platform.runLater(() ->
                    showTicketDetailView(ticket, threadTask.getValue().messages, threadTask.getValue().attachmentsByMessage)));
            AppThreadPool.execute(threadTask);
        });

        AppThreadPool.execute(ticketTask);
    }

    private void showTicketDetailView(SupportTicket ticket, List<TicketMessage> messages, Map<Integer, List<TicketAttachment>> attachmentsByMessage) {
        contentPane.getChildren().clear();

        VBox layout = new VBox(0);
        VBox.setVgrow(layout, Priority.ALWAYS);

        // ── TOP BAR: back + subject + actions ──
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("ticket-topbar");
        topBar.setPadding(new Insets(12, 0, 12, 0));

        TLButton backBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
        backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 16));
        backBtn.setTooltip(new Tooltip("Back"));
        backBtn.setOnAction(e -> loadTicketsTab());

        Label subjectLabel = new Label(ticket.getSubject());
        subjectLabel.getStyleClass().addAll("text-lg", "font-bold");

        TLBadge statusBadge = new TLBadge(
            I18n.get("ticket.status." + ticket.getStatus().toLowerCase()),
            getBadgeVariant(ticket.getStatus())
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(backBtn, subjectLabel, statusBadge, spacer);

        if (!"CLOSED".equals(ticket.getStatus()) && !"RESOLVED".equals(ticket.getStatus())) {
            TLButton closeBtn = new TLButton(I18n.get("ticket.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> closeTicket(ticket.getId()));
            topBar.getChildren().add(closeBtn);
        }

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        topBar.getChildren().add(actionSpacer);

        TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
        deleteBtn.setOnAction(e -> deleteTicket(ticket.getId()));
        topBar.getChildren().add(deleteBtn);

        // ── INFO STRIP: compact ticket metadata row ──
        HBox infoStrip = new HBox(20);
        infoStrip.setAlignment(Pos.CENTER_LEFT);
        infoStrip.setPadding(new Insets(10, 16, 10, 16));
        infoStrip.getStyleClass().add("ticket-info-strip");

        infoStrip.getChildren().add(createInfoChip(I18n.get("ticket.category"),
            ticket.getCategory() != null ? ticket.getCategory() : "-"));
        infoStrip.getChildren().add(createInfoChip(I18n.get("ticket.priority"),
            ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM"));
        infoStrip.getChildren().add(createInfoChip(I18n.get("ticket.date"),
            formatDate(ticket.getCreatedDate())));
        if (ticket.getSlaDueDate() != null) {
            infoStrip.getChildren().add(createInfoChip(I18n.get("ticket.sla_due"),
                    ticket.getSlaDueDate().format(FULL_FMT)));
        }
        if (ticket.getAssignedToName() != null) {
            infoStrip.getChildren().add(createInfoChip(I18n.get("ticket.assigned_to"),
                ticket.getAssignedToName()));
        }

        // ── DESCRIPTION ──
        if (ticket.getDescription() != null && !ticket.getDescription().isBlank()) {
            VBox descBox = new VBox(6);
            descBox.setPadding(new Insets(12, 16, 12, 16));
            descBox.getStyleClass().add("ticket-description-box");

            Label descBody = new Label(ticket.getDescription());
            descBody.setWrapText(true);
            descBody.getStyleClass().add("text-muted");

            descBox.getChildren().add(descBody);
            layout.getChildren().addAll(topBar, infoStrip, descBox);
        } else {
            layout.getChildren().addAll(topBar, infoStrip);
        }

        // ── CONVERSATION HEADER ──
        HBox convHeader = new HBox(8);
        convHeader.setAlignment(Pos.CENTER_LEFT);
        convHeader.setPadding(new Insets(12, 16, 8, 16));

        Node msgIcon = SvgIcons.icon(SvgIcons.MESSAGE_CIRCLE, 14);
        Label convTitle = new Label(I18n.get("ticket.conversation"));
        convTitle.getStyleClass().addAll("text-sm", "font-bold");
        Label msgCount = new Label("(" + messages.size() + ")");
        msgCount.getStyleClass().addAll("text-2xs", "text-muted");

        convHeader.getChildren().addAll(msgIcon, convTitle, msgCount);
        layout.getChildren().add(convHeader);

        // ── MESSAGES AREA ──
        VBox messagesBox = new VBox(2);
        messagesBox.setPadding(new Insets(4, 16, 8, 16));
        messagesBox.getStyleClass().add("ticket-messages-area");

        if (messages.isEmpty()) {
            HBox emptyMsg = new HBox();
            emptyMsg.setAlignment(Pos.CENTER);
            emptyMsg.setPadding(new Insets(32));
            Label noMsg = new Label(I18n.get("ticket.no_messages"));
            noMsg.getStyleClass().add("text-muted");
            emptyMsg.getChildren().add(noMsg);
            messagesBox.getChildren().add(emptyMsg);
        } else {
            LocalDateTime lastDate = null;
            for (TicketMessage msg : messages) {
                // Date separator
                if (msg.getCreatedDate() != null) {
                    LocalDateTime msgDate = msg.getCreatedDate().truncatedTo(ChronoUnit.DAYS);
                    if (lastDate == null || !lastDate.equals(msgDate)) {
                        lastDate = msgDate;
                        messagesBox.getChildren().add(createDateSeparator(msg.getCreatedDate()));
                    }
                }
                messagesBox.getChildren().add(createMessageBubble(msg,
                        attachmentsByMessage != null ? attachmentsByMessage.get(msg.getId()) : null));
            }
        }

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        msgScroll.getStyleClass().addAll("bg-transparent", "msg-scroll");
        VBox.setVgrow(msgScroll, Priority.ALWAYS);
        layout.getChildren().add(msgScroll);

        // Scroll to bottom
        Platform.runLater(() -> msgScroll.setVvalue(1.0));

        // ── REPLY INPUT BAR ──
        if (!"CLOSED".equals(ticket.getStatus()) && !"RESOLVED".equals(ticket.getStatus())) {
            VBox composer = new VBox(8);
            composer.setPadding(new Insets(12, 16, 12, 16));
            composer.getStyleClass().add("ticket-reply-bar");

            List<File> pendingAttachments = new ArrayList<>();
            HBox chipsRow = new HBox(6);
            chipsRow.setAlignment(Pos.CENTER_LEFT);
            chipsRow.setVisible(false);
            chipsRow.setManaged(false);

            TextField replyField = new TextField();
            replyField.setPromptText(I18n.get("ticket.reply.placeholder"));
            replyField.getStyleClass().add("reply-input");
            HBox.setHgrow(replyField, Priority.ALWAYS);

            TLButton attachBtn = new TLButton("", TLButton.ButtonVariant.OUTLINE);
            attachBtn.setGraphic(SvgIcons.icon(SvgIcons.FILE_TEXT, 16));
            attachBtn.setTooltip(new Tooltip(I18n.get("ticket.attach")));
            attachBtn.setOnAction(e -> {
                if (contentPane == null || contentPane.getScene() == null) return;
                FileChooser chooser = new FileChooser();
                chooser.setTitle(I18n.get("ticket.attach"));
                List<File> files = chooser.showOpenMultipleDialog(contentPane.getScene().getWindow());
                if (files == null || files.isEmpty()) return;
                pendingAttachments.addAll(files);
                rebuildAttachmentChips(chipsRow, pendingAttachments);
            });

            // Voice input button (branch integration: AudioRecorder + Deepgram STT)
            TLButton voiceBtn = new TLButton("🎤", TLButton.ButtonVariant.OUTLINE);
            voiceBtn.setTooltip(new Tooltip("Speech-to-text"));
            AtomicBoolean isRecording = new AtomicBoolean(false);
            voiceBtn.setOnAction(e -> {
                if (!isRecording.get()) {
                    try {
                        audioRecorder.start();
                        isRecording.set(true);
                        voiceBtn.setText("⏹");
                        voiceBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
                    } catch (Exception ex) {
                        logger.error("Failed to start recording", ex);
                        TLToast.error(contentPane.getScene(), "Error", "Could not start recording");
                    }
                } else {
                    try {
                        byte[] audioData = audioRecorder.stop();
                        isRecording.set(false);
                        voiceBtn.setText("🎤");
                        voiceBtn.setStyle("");
                        if (audioData != null && audioData.length > 0) {
                            voiceBtn.setDisable(true);
                            Task<String> sttTask = new Task<>() {
                                @Override protected String call() { return deepgramService.transcribe(audioData); }
                            };
                            sttTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                                String transcript = sttTask.getValue();
                                if (transcript != null && !transcript.isBlank()) {
                                    String current = replyField.getText() != null ? replyField.getText() : "";
                                    replyField.setText(current.isEmpty() ? transcript : current + " " + transcript);
                                }
                                voiceBtn.setDisable(false);
                            }));
                            sttTask.setOnFailed(ev -> Platform.runLater(() -> {
                                logger.error("STT failed", sttTask.getException());
                                voiceBtn.setDisable(false);
                            }));
                            AppThreadPool.execute(sttTask);
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to stop recording", ex);
                        isRecording.set(false);
                        voiceBtn.setText("🎤");
                        voiceBtn.setStyle("");
                    }
                }
            });

            TLButton sendBtn = new TLButton("", TLButton.ButtonVariant.PRIMARY);
            sendBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 16));
            sendBtn.setTooltip(new Tooltip("Send reply"));
            sendBtn.getStyleClass().add("send-btn");

            Runnable doSend = () -> {
                String text = replyField.getText() != null ? replyField.getText().trim() : "";
                if (!text.isEmpty() || !pendingAttachments.isEmpty()) {
                    replyField.setDisable(true);
                    attachBtn.setDisable(true);
                    sendBtn.setDisable(true);
                    sendReply(ticket.getId(), text, pendingAttachments);
                }
            };

            sendBtn.setOnAction(e -> doSend.run());
            replyField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) doSend.run();
            });

            HBox inputRow = new HBox(10, attachBtn, voiceBtn, replyField, sendBtn);
            inputRow.setAlignment(Pos.CENTER);

            composer.getChildren().addAll(chipsRow, inputRow);
            layout.getChildren().add(composer);
        } else {
            HBox closedBar = new HBox();
            closedBar.setAlignment(Pos.CENTER);
            closedBar.setPadding(new Insets(14, 16, 14, 16));
            closedBar.getStyleClass().add("ticket-reply-bar");

            Label closedLabel = new Label(I18n.get("ticket.closed_notice"));
            closedLabel.getStyleClass().addAll("text-sm", "text-muted");
            closedBar.getChildren().add(closedLabel);
            layout.getChildren().add(closedBar);
        }

        contentPane.getChildren().add(layout);
    }

    private Node createDateSeparator(LocalDateTime date) {
        HBox sep = new HBox();
        sep.setAlignment(Pos.CENTER);
        sep.setPadding(new Insets(12, 0, 8, 0));

        Label label = new Label(formatDate(date));
        label.getStyleClass().addAll("text-3xs", "text-muted", "date-separator-label");
        sep.getChildren().add(label);
        return sep;
    }

    private VBox createMessageBubble(TicketMessage msg, List<TicketAttachment> attachments) {
        boolean isMe = currentUser != null && msg.getSenderId() == currentUser.getId();

        VBox wrapper = new VBox(2);
        wrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 0, 2, 0));

        HBox bubbleRow = new HBox(8);
        bubbleRow.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Avatar (only for other party)
        if (!isMe) {
            String initials = getInitials(msg.getSenderName());
            TLAvatar avatar = new TLAvatar(initials);
            avatar.setSize(TLAvatar.Size.SM);
            bubbleRow.getChildren().add(avatar);
        }

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(420);

        // Sender name (only for other)
        if (!isMe) {
            HBox senderRow = new HBox(6);
            senderRow.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(msg.getSenderName() != null ? msg.getSenderName() : "Support");
            name.getStyleClass().addAll("text-2xs", "font-bold");

            if (msg.getSenderRole() != null && !msg.getSenderRole().isEmpty()) {
                TLBadge roleBadge = new TLBadge(msg.getSenderRole(), TLBadge.Variant.SECONDARY);
                senderRow.getChildren().addAll(name, roleBadge);
            } else {
                senderRow.getChildren().add(name);
            }
            bubble.getChildren().add(senderRow);
        }

        // Message content
        Label msgLabel = new Label(msg.getMessage());
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(400);
        msgLabel.getStyleClass().add(isMe ? "chat-bubble-mine" : "chat-bubble-other");

        VBox attachmentsBox = createAttachmentsBox(attachments);

        // Time
        Label time = new Label(msg.getCreatedDate() != null ? msg.getCreatedDate().format(TIME_FMT) : "");
        time.getStyleClass().addAll("text-3xs", "text-muted");
        HBox timeRow = new HBox();
        timeRow.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        timeRow.getChildren().add(time);

        if (attachmentsBox != null) {
            bubble.getChildren().addAll(msgLabel, attachmentsBox, timeRow);
        } else {
            bubble.getChildren().addAll(msgLabel, timeRow);
        }
        bubbleRow.getChildren().add(bubble);

        wrapper.getChildren().add(bubbleRow);
        return wrapper;
    }

    private VBox createAttachmentsBox(List<TicketAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        VBox box = new VBox(4);
        for (TicketAttachment a : attachments) {
            Hyperlink link = new Hyperlink(a.getFileName() != null ? a.getFileName() : "");
            link.getStyleClass().add("attachment-link");
            link.setGraphic(SvgIcons.icon(SvgIcons.FILE_TEXT, 14));
            link.setOnAction(e -> openAttachment(a));
            box.getChildren().add(link);
        }
        return box;
    }

    private void openAttachment(TicketAttachment a) {
        if (a == null || a.getFilePath() == null) return;
        try {
            if (!Desktop.isDesktopSupported()) return;
            Desktop.getDesktop().open(new File(a.getFilePath()));
        } catch (Exception ex) {
            logger.debug("Failed to open attachment {}", a.getFilePath(), ex);
        }
    }

    private record TicketThreadData(List<TicketMessage> messages, Map<Integer, List<TicketAttachment>> attachmentsByMessage) {}

    private HBox createInfoChip(String label, String value) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().addAll("text-2xs", "text-muted");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().addAll("text-2xs", "font-bold");

        chip.getChildren().addAll(labelNode, valueNode);
        return chip;
    }

    private void sendReply(int ticketId, String text, List<File> attachments) {
        if (currentUser == null) return;

        List<File> files = attachments != null ? new ArrayList<>(attachments) : List.of();
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                TicketMessage msg = new TicketMessage(ticketId, currentUser.getId(), text != null ? text : "", false);
                int messageId = messageService.addMessage(msg);
                if (messageId > 0 && !files.isEmpty()) {
                    for (File f : files) {
                        try {
                            var stored = AttachmentStorage.store(f, ticketId);
                            TicketAttachment att = new TicketAttachment(ticketId, messageId,
                                    stored.getOriginalName(), stored.getMimeType(), stored.getStoredPath(), stored.getSize());
                            attachmentService.create(att);
                        } catch (IOException ex) {
                            logger.debug("Failed to store attachment {}", f, ex);
                        }
                    }
                }
                return messageId;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> openTicketDetail(ticketId)));
        task.setOnFailed(e -> {
            logger.error("Failed to send reply", task.getException());
            Platform.runLater(() -> DialogUtils.showError(I18n.get("message.error"), I18n.get("error.db.operation")));
        });
        AppThreadPool.execute(task);
    }

    private void rebuildAttachmentChips(HBox chipsRow, List<File> attachments) {
        if (chipsRow == null) return;
        chipsRow.getChildren().clear();
        if (attachments == null || attachments.isEmpty()) {
            chipsRow.setVisible(false);
            chipsRow.setManaged(false);
            return;
        }
        chipsRow.setVisible(true);
        chipsRow.setManaged(true);

        for (File f : new ArrayList<>(attachments)) {
            HBox chip = new HBox(6);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("attachment-chip");

            Label name = new Label(f.getName());
            name.getStyleClass().addAll("text-2xs", "text-muted");

            TLButton remove = new TLButton("", TLButton.ButtonVariant.GHOST);
            remove.setGraphic(SvgIcons.icon(SvgIcons.X_CIRCLE, 14));
            remove.setTooltip(new Tooltip("Remove"));
            remove.setOnAction(e -> {
                attachments.remove(f);
                rebuildAttachmentChips(chipsRow, attachments);
            });

            chip.getChildren().addAll(name, remove);
            chipsRow.getChildren().add(chip);
        }
    }

    private void closeTicket(int ticketId) {
        DialogUtils.showConfirmation(
            I18n.get("ticket.close.confirm.title"),
            I18n.get("ticket.close.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                contentPane.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override protected Boolean call() { return ticketService.updateStatus(ticketId, "CLOSED"); }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    contentPane.setDisable(false);
                    openTicketDetail(ticketId);
                }));
                task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
                AppThreadPool.execute(task);
            }
        });
    }

    private void deleteTicket(int ticketId) {
        DialogUtils.showConfirmation(
            I18n.get("ticket.delete.confirm.title"),
            I18n.get("ticket.delete.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                contentPane.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override protected Boolean call() { return ticketService.delete(ticketId); }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    contentPane.setDisable(false);
                    TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.deleted"));
                    loadTicketsTab();
                }));
                task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== FAQ Tab ====================

    private void loadFAQTab() {
        exitTicketDetailMode();
        contentPane.getChildren().setAll(new TLLoadingState());
        Task<List<FAQArticle>> task = new Task<>() {
            @Override protected List<FAQArticle> call() { return faqService.findAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> displayFAQ(task.getValue())));
        AppThreadPool.execute(task);
    }

    private void displayFAQ(List<FAQArticle> articles) {
        contentPane.getChildren().clear();

        VBox faqContainer = new VBox(12);

        TLTextField searchField = new TLTextField("", I18n.get("faq.search"));
        searchField.getControl().getStyleClass().add("search-input");

        VBox faqList = new VBox(8);

        Runnable renderFaqs = () -> {
            String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            faqList.getChildren().clear();

            for (FAQArticle article : articles) {
                if (!query.isEmpty()) {
                    boolean matches = (article.getQuestion() != null && article.getQuestion().toLowerCase().contains(query))
                        || (article.getAnswer() != null && article.getAnswer().toLowerCase().contains(query));
                    if (!matches) continue;
                }

                VBox faqItem = new VBox(8);
                faqItem.getStyleClass().add("faq-item");
                faqItem.setPadding(new Insets(16));

                Label q = new Label(article.getQuestion());
                q.getStyleClass().addAll("text-sm", "font-bold");
                q.setWrapText(true);

                Label a = new Label(article.getAnswer());
                a.setWrapText(true);
                a.getStyleClass().addAll("text-sm", "text-muted");

                // Helpful voting
                HBox helpfulRow = new HBox(8);
                helpfulRow.setAlignment(Pos.CENTER_LEFT);

                Label helpfulLabel = new Label(I18n.get("faq.helpful"));
                helpfulLabel.getStyleClass().addAll("text-2xs", "text-muted");

                Label votesLabel = new Label(
                        I18n.get("faq.votes", article.getHelpfulCount(), article.getNotHelpfulCount()));
                votesLabel.getStyleClass().addAll("text-2xs", "text-muted");

                TLButton yesBtn = new TLButton(I18n.get("faq.yes"), TLButton.ButtonVariant.OUTLINE);
                TLButton noBtn = new TLButton(I18n.get("faq.no"), TLButton.ButtonVariant.OUTLINE);

                Runnable disable = () -> {
                    yesBtn.setDisable(true);
                    noBtn.setDisable(true);
                };
                Runnable enable = () -> {
                    yesBtn.setDisable(false);
                    noBtn.setDisable(false);
                };

                yesBtn.setOnAction(ev -> {
                    disable.run();
                    AppThreadPool.execute(() -> {
                        boolean ok = faqService.voteHelpful(article.getId(), true);
                        Platform.runLater(() -> {
                            if (ok) {
                                article.setHelpfulCount(article.getHelpfulCount() + 1);
                                votesLabel.setText(I18n.get("faq.votes", article.getHelpfulCount(), article.getNotHelpfulCount()));
                            } else {
                                enable.run();
                            }
                        });
                    });
                });
                noBtn.setOnAction(ev -> {
                    disable.run();
                    AppThreadPool.execute(() -> {
                        boolean ok = faqService.voteHelpful(article.getId(), false);
                        Platform.runLater(() -> {
                            if (ok) {
                                article.setNotHelpfulCount(article.getNotHelpfulCount() + 1);
                                votesLabel.setText(I18n.get("faq.votes", article.getHelpfulCount(), article.getNotHelpfulCount()));
                            } else {
                                enable.run();
                            }
                        });
                    });
                });

                helpfulRow.getChildren().addAll(helpfulLabel, yesBtn, noBtn, votesLabel);

                faqItem.getChildren().addAll(q, a, helpfulRow);
                faqList.getChildren().add(faqItem);
            }

            if (faqList.getChildren().isEmpty()) {
                TLEmptyState empty = new TLEmptyState(
                    SvgIcons.SEARCH,
                    I18n.get("faq.empty"), "");
                faqList.getChildren().add(empty);
            }
        };

        searchField.getControl().textProperty().addListener((obs, o, n) -> renderFaqs.run());
        renderFaqs.run();

        ScrollPane scrollPane = new ScrollPane(faqList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        faqContainer.getChildren().addAll(searchField, scrollPane);
        contentPane.getChildren().add(faqContainer);
    }

    // ==================== Chatbot Tab ====================

    private VBox chatMessages;
    private HBox typingIndicator;
    private int currentConversationId = -1;

    private void loadChatbotTab() {
        exitTicketDetailMode();
        contentPane.getChildren().clear();

        VBox chatContainer = new VBox(0);
        VBox.setVgrow(chatContainer, Priority.ALWAYS);

        // Chat messages area
        chatMessages = new VBox(2);
        chatMessages.setPadding(new Insets(16));
        chatMessages.getStyleClass().add("chatbot-messages");

        ScrollPane messageArea = new ScrollPane(chatMessages);
        messageArea.setFitToWidth(true);
        messageArea.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageArea.getStyleClass().addAll("bg-transparent", "msg-scroll");
        VBox.setVgrow(messageArea, Priority.ALWAYS);

        // Welcome
        addBotMessage(I18n.get("chatbot.welcome"));

        if (currentUser != null) {
            Task<Integer> startTask = new Task<>() {
                @Override protected Integer call() { return chatbotService.startConversation(currentUser.getId()); }
            };
            startTask.setOnSucceeded(ev -> currentConversationId = startTask.getValue());
            startTask.setOnFailed(ev -> logger.error("Failed to start chatbot", startTask.getException()));
            AppThreadPool.execute(startTask);
        }

        // Input bar
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.getStyleClass().add("chatbot-input-bar");

        TextField messageField = new TextField();
        messageField.setPromptText(I18n.get("chatbot.placeholder"));
        messageField.getStyleClass().add("reply-input");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        TLButton sendBtn = new TLButton("", TLButton.ButtonVariant.PRIMARY);
        sendBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 16));
        sendBtn.setTooltip(new Tooltip("Send message"));
        sendBtn.getStyleClass().add("send-btn");

        TLButton escalateBtn = new TLButton(I18n.get("chatbot.escalate"), TLButton.ButtonVariant.OUTLINE);

        Runnable doSend = () -> {
            String userMessage = messageField.getText() != null ? messageField.getText().trim() : "";
            if (!userMessage.isEmpty()) {
                addUserMessage(userMessage);
                messageField.setText("");
                handleChatbotResponse(userMessage);
                Platform.runLater(() -> messageArea.setVvalue(1.0));
            }
        };

        sendBtn.setOnAction(e -> doSend.run());
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doSend.run();
        });
        escalateBtn.setOnAction(e -> escalateChatToTicket());

        inputBar.getChildren().addAll(messageField, sendBtn, escalateBtn);
        chatContainer.getChildren().addAll(messageArea, inputBar);
        contentPane.getChildren().add(chatContainer);
    }

    private void addUserMessage(String message) {
        if (chatMessages == null) return;

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setMaxWidth(400);
        msg.getStyleClass().add("chat-bubble-mine");

        row.getChildren().add(msg);
        chatMessages.getChildren().add(row);
    }

    private void addBotMessage(String message) {
        if (chatMessages == null) return;

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        TLAvatar avatar = new TLAvatar("AI");
        avatar.setSize(TLAvatar.Size.SM);

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setMaxWidth(400);
        msg.getStyleClass().add("chat-bubble-other");

        row.getChildren().addAll(avatar, msg);
        chatMessages.getChildren().add(row);
    }

    private void showTypingIndicator() {
        if (chatMessages == null) return;

        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        TLAvatar avatar = new TLAvatar("AI");
        avatar.setSize(TLAvatar.Size.SM);

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        dots.getStyleClass().add("chat-typing-indicator");

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("chat-typing-dot");
            dot.setOpacity(0.4);
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(dot.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(300), new KeyValue(dot.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(dot.opacityProperty(), 0.4))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 200L));
            pulse.play();
            dots.getChildren().add(dot);
        }

        container.getChildren().addAll(avatar, dots);
        typingIndicator = container;
        chatMessages.getChildren().add(typingIndicator);
    }

    private void hideTypingIndicator() {
        if (typingIndicator != null && chatMessages != null) {
            chatMessages.getChildren().remove(typingIndicator);
            typingIndicator = null;
        }
    }

    private void handleChatbotResponse(String userMessage) {
        showTypingIndicator();

        Task<String> task = new Task<>() {
            @Override protected String call() {
                if (currentConversationId > 0 && currentUser != null) {
                    chatbotService.addMessage(new ChatbotMessage(currentConversationId, "USER", userMessage));
                }
                String response = chatbotService.getAutoResponse(userMessage);
                if (currentConversationId > 0) {
                    String botReply = response != null ? response : I18n.get("chatbot.no_match");
                    chatbotService.addMessage(new ChatbotMessage(currentConversationId, "BOT", botReply));
                }
                return response != null ? response : I18n.get("chatbot.no_match");
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            hideTypingIndicator();
            addBotMessage(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            hideTypingIndicator();
            addBotMessage(I18n.get("chatbot.no_match"));
        }));
        AppThreadPool.execute(task);
    }

    private void escalateChatToTicket() {
        if (currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("chatbot.escalate.title"));
        dialog.setDescription(I18n.get("chatbot.escalate.description"));

        VBox form = new VBox(12);
        TLTextField subjectField = new TLTextField(I18n.get("ticket.subject"), "");
        TLTextarea descArea = new TLTextarea(I18n.get("ticket.description"));
        descArea.getControl().setPrefRowCount(5);

        form.getChildren().addAll(subjectField, descArea);
        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String subject = subjectField.getText() != null ? subjectField.getText().trim() : "";
                if (subject.isEmpty()) {
                    subjectField.setError(I18n.get("error.validation.required", I18n.get("ticket.subject")));
                    event.consume();
                } else {
                    subjectField.clearValidation();
                }
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String subject = subjectField.getText() != null ? subjectField.getText().trim() : "";
                String desc = descArea.getText() != null ? descArea.getText().trim() : "";

                Task<Void> task = new Task<>() {
                    @Override protected Void call() {
                        SupportTicket ticket = new SupportTicket();
                        ticket.setUserId(currentUser.getId());
                        ticket.setSubject(subject);
                        ticket.setDescription(desc.isEmpty() ? I18n.get("support.escalated_from_chatbot") : desc);
                        ticket.setCategory("general");
                        ticket.setPriority("MEDIUM");
                        ticket.setStatus("OPEN");
                        int ticketId = ticketService.create(ticket);
                        if (ticketId > 0 && currentConversationId > 0) {
                            chatbotService.escalateToTicket(currentConversationId, ticketId);
                        }
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("chatbot.escalated"));
                    loadTicketsTab();
                }));
                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== Feedback Tab ====================

    private void loadFeedbackTab() {
        exitTicketDetailMode();
        contentPane.getChildren().clear();

        VBox feedbackContainer = new VBox(20);
        feedbackContainer.setPadding(new Insets(4, 0, 0, 0));
        feedbackContainer.setMaxWidth(700);

        TLCard card = new TLCard();
        VBox cardContent = new VBox(20);

        Label title = new Label(I18n.get("feedback.title"));
        title.getStyleClass().addAll("text-lg", "font-bold");

        Label desc = new Label(I18n.get("feedback.description"));
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);

        TLSelect<String> typeSelect = new TLSelect<>(I18n.get("feedback.type"));
        typeSelect.getItems().addAll(
            I18n.get("feedback.type.bug"),
            I18n.get("feedback.type.feature"),
            I18n.get("feedback.type.general"),
            I18n.get("feedback.type.complaint"),
            I18n.get("feedback.type.praise")
        );
        typeSelect.setValue(I18n.get("feedback.type.general"));

        // Star rating
        VBox ratingBox = new VBox(8);
        Label ratingLabel = new Label(I18n.get("feedback.rating"));
        ratingLabel.getStyleClass().addAll("text-sm", "font-bold");
        TLStarRating starRating = new TLStarRating(5);
        ratingBox.getChildren().addAll(ratingLabel, starRating);

        TLTextarea commentArea = new TLTextarea(I18n.get("feedback.comment"), "");
        commentArea.getControl().setPrefRowCount(5);

        TLButton submitBtn = new TLButton(I18n.get("feedback.submit"), TLButton.ButtonVariant.PRIMARY);
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            submitFeedback(getFeedbackTypeKey(typeSelect.getValue()),
                starRating.getRating(),
                commentArea.getText());
        });

        cardContent.getChildren().addAll(title, desc, typeSelect, ratingBox, commentArea, submitBtn);
        card.setContent(cardContent);
        feedbackContainer.getChildren().add(card);
        contentPane.getChildren().add(feedbackContainer);
    }

    private void submitFeedback(String type, int rating, String comment) {
        if (currentUser == null) return;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                UserFeedback feedback = new UserFeedback();
                feedback.setUserId(currentUser.getId());
                feedback.setFeedbackType(type);
                feedback.setRating(rating);
                feedback.setComment(comment);
                return feedbackService.submit(feedback);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("feedback.submitted"));
            loadFeedbackTab();
        }));
        AppThreadPool.execute(task);
    }

    // ==================== Helpers ====================

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return I18n.get("time.just_now");
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        if (days < 7) return days + "d";
        return dateTime.format(DATE_FMT);
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FMT);
    }

    private TLBadge.Variant getBadgeVariant(String status) {
        return switch (status) {
            case "OPEN" -> TLBadge.Variant.DEFAULT;
            case "IN_PROGRESS" -> TLBadge.Variant.SECONDARY;
            case "RESOLVED" -> TLBadge.Variant.SUCCESS;
            case "CLOSED" -> TLBadge.Variant.OUTLINE;
            default -> TLBadge.Variant.SECONDARY;
        };
    }

    private TLBadge.Variant getPriorityBadgeVariant(String priority) {
        if (priority == null) return TLBadge.Variant.DEFAULT;
        return switch (priority) {
            case "LOW" -> TLBadge.Variant.SECONDARY;
            case "MEDIUM" -> TLBadge.Variant.DEFAULT;
            case "HIGH", "URGENT" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }

    private String getCategoryKey(String displayValue) {
        if (displayValue == null) return "general";
        if (displayValue.equals(I18n.get("ticket.category.technical"))) return "technical";
        if (displayValue.equals(I18n.get("ticket.category.billing"))) return "billing";
        if (displayValue.equals(I18n.get("ticket.category.account"))) return "account";
        if (displayValue.equals(I18n.get("ticket.category.job"))) return "job";
        if (displayValue.equals(I18n.get("ticket.category.formation"))) return "formation";
        return "general";
    }

    private String getPriorityKey(String displayValue) {
        if (displayValue == null) return "MEDIUM";
        if (displayValue.equals(I18n.get("ticket.priority.low"))) return "LOW";
        if (displayValue.equals(I18n.get("ticket.priority.high"))) return "HIGH";
        if (displayValue.equals(I18n.get("ticket.priority.urgent"))) return "URGENT";
        return "MEDIUM";
    }

    private String getFeedbackTypeKey(String displayValue) {
        if (displayValue == null) return "GENERAL";
        if (displayValue.equals(I18n.get("feedback.type.bug"))) return "BUG_REPORT";
        if (displayValue.equals(I18n.get("feedback.type.feature"))) return "FEATURE_REQUEST";
        if (displayValue.equals(I18n.get("feedback.type.complaint"))) return "COMPLAINT";
        if (displayValue.equals(I18n.get("feedback.type.praise"))) return "PRAISE";
        return "GENERAL";
    }

    private String mapPredictedCategory(String predicted) {
        if (predicted == null) return I18n.get("ticket.category.general");
        String lower = predicted.toLowerCase().trim();
        if (lower.contains("tech")) return I18n.get("ticket.category.technical");
        if (lower.contains("bill") || lower.contains("pay") || lower.contains("factur")) return I18n.get("ticket.category.billing");
        if (lower.contains("account") || lower.contains("compte")) return I18n.get("ticket.category.account");
        if (lower.contains("job") || lower.contains("emploi") || lower.contains("recruit")) return I18n.get("ticket.category.job");
        if (lower.contains("form") || lower.contains("train") || lower.contains("cours")) return I18n.get("ticket.category.formation");
        return I18n.get("ticket.category.general");
    }
}
