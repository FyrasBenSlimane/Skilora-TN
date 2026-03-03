package com.skilora.support.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.user.service.UserService;
import com.skilora.support.entity.*;
import com.skilora.support.service.*;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * SupportAdminController — Admin view for managing support tickets,
 * FAQ articles, auto-responses, user feedback, and statistics.
 * Admin does NOT create tickets — they manage tickets from users/employers.
 */
public class SupportAdminController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SupportAdminController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;

    private User currentUser;

    private final SupportTicketService ticketService = SupportTicketService.getInstance();
    private final TicketMessageService messageService = TicketMessageService.getInstance();
    private final TicketAttachmentService attachmentService = TicketAttachmentService.getInstance();
    private final FAQService faqService = FAQService.getInstance();
    private final AutoResponseService autoResponseService = AutoResponseService.getInstance();
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();
    private final UserService userService = UserService.getInstance();
    private final GeminiAIService geminiService = GeminiAIService.getInstance();
    private final AudioRecorderService audioRecorder = AudioRecorderService.getInstance();
    private final DeepgramSTTService deepgramService = DeepgramSTTService.getInstance();
    private String adminTicketFilter = null; // null=ALL, "MY"=assigned to me, status string

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("support.admin.title"));
        subtitleLabel.setText(I18n.get("support.admin.subtitle"));
        createTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null || user.getRole() != com.skilora.user.enums.Role.ADMIN) {
            if (contentPane != null && contentPane.getScene() != null) {
                TLToast.error(contentPane.getScene(), I18n.get("errorpage.access_denied"),
                        I18n.get("errorpage.access_denied.desc"));
            }
            return;
        }
        showTicketsTab();
    }

    // ==================== Tab Navigation ====================

    private void createTabs() {
        tabBox.getChildren().clear();
        TLTabs tabs = new TLTabs();
        tabs.addTab("tickets", I18n.get("support.admin.tab.tickets"), (javafx.scene.Node) null);
        tabs.addTab("faq", I18n.get("support.admin.tab.faq"), (javafx.scene.Node) null);
        tabs.addTab("auto", I18n.get("support.admin.tab.auto_responses"), (javafx.scene.Node) null);
        tabs.addTab("feedback", I18n.get("support.admin.tab.feedback"), (javafx.scene.Node) null);
        tabs.addTab("stats", I18n.get("support.admin.tab.stats"), (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
            switch (tabId) {
                case "tickets" -> showTicketsTab();
                case "faq" -> showFAQTab();
                case "auto" -> showAutoResponsesTab();
                case "feedback" -> showFeedbackTab();
                case "stats" -> showStatsTab();
            }
        });
        tabBox.getChildren().add(tabs);
    }

    private void enterTicketDetailMode() {
        if (tabBox != null) {
            tabBox.setVisible(false);
            tabBox.setManaged(false);
        }
    }

    private void exitTicketDetailMode() {
        if (tabBox != null) {
            tabBox.setVisible(true);
            tabBox.setManaged(true);
        }
    }

    // ==================== Tickets Tab ====================

    private void showTicketsTab() {
        exitTicketDetailMode();
        contentPane.getChildren().clear();

        // Trigger auto-escalation in background
        AppThreadPool.execute(new Task<Integer>() {
            @Override protected Integer call() { return ticketService.autoEscalateOverdueTickets(); }
        });

        // Quick filter chips row (My Tickets / All / by status)
        HBox chipRow = new HBox(8);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        chipRow.setPadding(new Insets(0, 0, 4, 0));

        String[] quickFilters = {"ALL", "MY", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};
        String[] chipLabels = {I18n.get("support.admin.filter.all"), I18n.get("support.admin.filter.my_tickets"),
                "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};
        for (int i = 0; i < quickFilters.length; i++) {
            final String filterVal = quickFilters[i];
            TLButton chip = new TLButton(chipLabels[i],
                    isActiveChip(filterVal) ? TLButton.ButtonVariant.PRIMARY : TLButton.ButtonVariant.OUTLINE);
            chip.setOnAction(e -> {
                adminTicketFilter = "ALL".equals(filterVal) ? null : filterVal;
                showTicketsTab();
            });
            chipRow.getChildren().add(chip);
        }
        contentPane.getChildren().add(chipRow);

        // Filter bar (status + priority dropdowns)
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 0, 8, 0));

        TLSelect<String> statusFilter = new TLSelect<>(I18n.get("support.admin.filter.status"));
        statusFilter.getItems().addAll(
                I18n.get("support.admin.filter.all"),
                "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
        statusFilter.setValue(I18n.get("support.admin.filter.all"));

        TLSelect<String> priorityFilter = new TLSelect<>(I18n.get("support.admin.filter.priority"));
        priorityFilter.getItems().addAll(
                I18n.get("support.admin.filter.all"),
                "LOW", "MEDIUM", "HIGH", "URGENT");
        priorityFilter.setValue(I18n.get("support.admin.filter.all"));

        TLButton filterBtn = new TLButton(I18n.get("support.admin.filter.apply"), TLButton.ButtonVariant.OUTLINE);
        filterBtn.setOnAction(e -> applyTicketFilters(statusFilter.getValue(), priorityFilter.getValue()));

        filterBar.getChildren().addAll(statusFilter, priorityFilter, filterBtn);
        contentPane.getChildren().add(filterBar);

        loadFilteredTickets();
    }

    private boolean isActiveChip(String filterVal) {
        if (adminTicketFilter == null) return "ALL".equals(filterVal);
        return adminTicketFilter.equals(filterVal);
    }

    private void loadFilteredTickets() {
        if ("MY".equals(adminTicketFilter) && currentUser != null) {
            loadMyTickets();
        } else if (adminTicketFilter != null && !"ALL".equals(adminTicketFilter)) {
            loadTicketsByStatus(adminTicketFilter);
        } else {
            loadAllTickets();
        }
    }

    private void loadMyTickets() {
        VBox ticketArea = getOrCreateTicketArea();
        ticketArea.getChildren().clear();
        ticketArea.getChildren().add(createLoadingIndicator());

        int adminId = currentUser.getId();
        Task<List<SupportTicket>> task = new Task<>() {
            @Override
            protected List<SupportTicket> call() {
                return ticketService.findByAssignedTo(adminId);
            }
        };
        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            Platform.runLater(() -> displayTickets(tickets, ticketArea));
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load my tickets", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_tickets"));
        });
        AppThreadPool.execute(task);
    }

    private void applyTicketFilters(String status, String priority) {
        String allLabel = I18n.get("support.admin.filter.all");
        boolean hasStatus = !allLabel.equals(status);
        boolean hasPriority = !allLabel.equals(priority);

        if (hasStatus && hasPriority) {
            // Both filters active — load by status, then filter by priority client-side
            VBox ticketArea = getOrCreateTicketArea();
            ticketArea.getChildren().clear();
            ticketArea.getChildren().add(createLoadingIndicator());

            Task<List<SupportTicket>> task = new Task<>() {
                @Override
                protected List<SupportTicket> call() {
                    return ticketService.findByStatus(status);
                }
            };
            task.setOnSucceeded(e -> {
                List<SupportTicket> result = task.getValue();
                List<SupportTicket> filtered = (result != null ? result : List.<SupportTicket>of()).stream()
                        .filter(t -> priority.equalsIgnoreCase(t.getPriority()))
                        .toList();
                Platform.runLater(() -> displayTickets(filtered, ticketArea));
            });
            task.setOnFailed(e -> {
                logger.error("Failed to load filtered tickets", task.getException());
                TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_tickets"));
            });
            AppThreadPool.execute(task);
        } else if (hasStatus) {
            loadTicketsByStatus(status);
        } else if (hasPriority) {
            loadTicketsByPriority(priority);
        } else {
            loadAllTickets();
        }
    }

    private void loadAllTickets() {
        VBox ticketArea = getOrCreateTicketArea();
        ticketArea.getChildren().clear();
        ticketArea.getChildren().add(createLoadingIndicator());

        Task<List<SupportTicket>> task = new Task<>() {
            @Override
            protected List<SupportTicket> call() {
                return ticketService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            Platform.runLater(() -> displayTickets(tickets, ticketArea));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load all tickets", task.getException());
            Platform.runLater(() -> {
                ticketArea.getChildren().clear();
                ticketArea.getChildren().add(createEmptyState(I18n.get("support.admin.tickets.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void loadTicketsByStatus(String status) {
        VBox ticketArea = getOrCreateTicketArea();
        ticketArea.getChildren().clear();
        ticketArea.getChildren().add(createLoadingIndicator());

        Task<List<SupportTicket>> task = new Task<>() {
            @Override
            protected List<SupportTicket> call() {
                return ticketService.findByStatus(status);
            }
        };

        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            Platform.runLater(() -> displayTickets(tickets, ticketArea));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load tickets by status", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_tickets"));
        });

        AppThreadPool.execute(task);
    }

    private void loadTicketsByPriority(String priority) {
        VBox ticketArea = getOrCreateTicketArea();
        ticketArea.getChildren().clear();
        ticketArea.getChildren().add(createLoadingIndicator());

        Task<List<SupportTicket>> task = new Task<>() {
            @Override
            protected List<SupportTicket> call() {
                return ticketService.findByPriority(priority);
            }
        };

        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            Platform.runLater(() -> displayTickets(tickets, ticketArea));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load tickets by priority", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_tickets"));
        });

        AppThreadPool.execute(task);
    }

    private VBox getOrCreateTicketArea() {
        if (contentPane.getChildren().size() > 1 && contentPane.getChildren().get(1) instanceof VBox vbox) {
            return vbox;
        }
        VBox ticketArea = new VBox(12);
        VBox.setVgrow(ticketArea, Priority.ALWAYS);
        contentPane.getChildren().add(ticketArea);
        return ticketArea;
    }

    private void displayTickets(List<SupportTicket> tickets, VBox container) {
        container.getChildren().clear();

        if (tickets == null || tickets.isEmpty()) {
            container.getChildren().add(createEmptyState(I18n.get("support.admin.tickets.empty")));
            return;
        }

        VBox ticketList = new VBox(10);
        ticketList.setPadding(new Insets(4, 0, 4, 0));

        for (SupportTicket ticket : tickets) {
            TLCard card = new TLCard();
            card.getStyleClass().addAll("ticket-card", "card-interactive");

            VBox content = new VBox(10);
            content.setPadding(new Insets(4, 0, 4, 0));

            // ===== HEADER: Badges + Subject =====
            HBox headerRow = new HBox(10);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            // Status badge with icon
            TLBadge statusBadge = createTicketStatusBadge(ticket.getStatus());

            // Priority badge
            TLBadge priorityBadge = createTicketPriorityBadge(ticket.getPriority());

            // Subject label (grows)
            Label subjectLabel = new Label(ticket.getSubject() != null ? ticket.getSubject() : "-");
            subjectLabel.getStyleClass().addAll("h5", "ticket-subject");
            subjectLabel.setWrapText(true);
            HBox.setHgrow(subjectLabel, Priority.ALWAYS);

            // Quick actions (visible on hover)
            HBox quickActions = new HBox(4);
            quickActions.setAlignment(Pos.CENTER_RIGHT);
            quickActions.setOpacity(0.7);

            TLButton assignBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
            assignBtn.setGraphic(SvgIcons.icon(SvgIcons.USER_PLUS, 14, "-fx-muted-foreground"));
            assignBtn.setTooltip(new Tooltip(I18n.get("support.admin.ticket.assign")));
            assignBtn.setOnAction(e -> {
                e.consume();
                showAssignTicketDialog(ticket);
            });

            TLButton viewBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
            viewBtn.setGraphic(SvgIcons.icon(SvgIcons.EYE, 14, "-fx-muted-foreground"));
            viewBtn.setTooltip(new Tooltip(I18n.get("support.admin.ticket.view")));
            viewBtn.setOnAction(e -> {
                e.consume();
                openTicketDetail(ticket.getId());
            });

            quickActions.getChildren().addAll(assignBtn, viewBtn);

            headerRow.getChildren().addAll(statusBadge, priorityBadge, subjectLabel, quickActions);

            // ===== DESCRIPTION PREVIEW =====
            VBox descBox = null;
            String desc = ticket.getDescription() != null ? ticket.getDescription() : "";
            if (!desc.isEmpty()) {
                String preview = desc.length() > 140 ? desc.substring(0, 140) + "..." : desc;
                Label descLabel = new Label(preview);
                descLabel.setWrapText(true);
                descLabel.getStyleClass().addAll("text-sm", "text-muted", "ticket-description");
                descBox = new VBox(descLabel);
            }

            // ===== METADATA ROW =====
            javafx.scene.layout.FlowPane metadataPane = new javafx.scene.layout.FlowPane(12, 6);
            metadataPane.setPrefWrapLength(800);

            // User info
            metadataPane.getChildren().add(createTicketMetadataItem(SvgIcons.USER,
                ticket.getUserName() != null ? ticket.getUserName() : "#" + ticket.getUserId()));

            // Assignee info
            String assignee = ticket.getAssignedToName() != null
                ? ticket.getAssignedToName()
                : I18n.get("support.admin.ticket.unassigned");
            metadataPane.getChildren().add(createTicketMetadataItem(SvgIcons.SHIELD,
                I18n.get("support.admin.ticket.assigned_short") + ": " + assignee));

            // Created date
            if (ticket.getCreatedDate() != null) {
                String dateStr = ticket.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                metadataPane.getChildren().add(createTicketMetadataItem(SvgIcons.CLOCK, dateStr));
            }

            // Category if available
            if (ticket.getCategory() != null && !ticket.getCategory().isEmpty()) {
                metadataPane.getChildren().add(createTicketMetadataItem(SvgIcons.BOOKMARK,
                    ticket.getCategory()));
            }

            // SLA indicator
            if (ticket.getSlaDueDate() != null) {
                long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), ticket.getSlaDueDate());
                HBox slaItem;
                if (minutesLeft < 0) {
                    long overdueHours = Math.abs(minutesLeft) / 60;
                    slaItem = createSLAMetadataItem(SvgIcons.ALERT_TRIANGLE,
                        "SLA: " + overdueHours + "h " + I18n.get("support.admin.sla.overdue"),
                        "-fx-destructive", true);
                } else if (minutesLeft < 60) {
                    slaItem = createSLAMetadataItem(SvgIcons.CLOCK,
                        "SLA: " + minutesLeft + "min " + I18n.get("support.admin.sla.remaining"),
                        "-fx-warning", false);
                } else if (minutesLeft < 240) {
                    slaItem = createSLAMetadataItem(SvgIcons.CLOCK,
                        "SLA: " + (minutesLeft / 60) + "h " + I18n.get("support.admin.sla.remaining"),
                        "-fx-warning", false);
                } else {
                    slaItem = createSLAMetadataItem(SvgIcons.CHECK_CIRCLE,
                        "SLA: " + (minutesLeft / 60) + "h " + I18n.get("support.admin.sla.remaining"),
                        "-fx-success", false);
                }
                metadataPane.getChildren().add(slaItem);
            }

            // Assemble content
            content.getChildren().add(headerRow);
            if (descBox != null) {
                content.getChildren().add(descBox);
            }
            content.getChildren().add(metadataPane);

            card.setContent(content);
            ticketList.getChildren().add(card);

            // Click to open ticket detail
            card.setOnMouseClicked(e -> openTicketDetail(ticket.getId()));
        }

        ScrollPane scrollPane = new ScrollPane(ticketList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        container.getChildren().add(scrollPane);
    }

    // ==================== Ticket Detail View ====================

    private void openTicketDetail(int ticketId) {
        Task<SupportTicket> ticketTask = new Task<>() {
            @Override
            protected SupportTicket call() {
                return ticketService.findById(ticketId);
            }
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
                    List<TicketMessage> messages = messageService.findByTicketId(ticketId);
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
        enterTicketDetailMode();
        contentPane.getChildren().clear();

        VBox detailView = new VBox(16);
        detailView.setPadding(new Insets(0));

        // Back button
        TLButton backBtn = new TLButton("", TLButton.ButtonVariant.GHOST);
        backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 16));
        backBtn.setOnAction(e -> showTicketsTab());

        // Header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectLabel = new Label(ticket.getSubject());
        subjectLabel.getStyleClass().addAll("text-xl", "font-bold");

        TLBadge statusBadge = new TLBadge(
                ticket.getStatus() != null ? ticket.getStatus() : "OPEN",
                getTicketBadgeVariant(ticket.getStatus()));
        TLBadge priorityBadge = new TLBadge(
                ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
                getPriorityBadgeVariant(ticket.getPriority()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(subjectLabel, statusBadge, priorityBadge, spacer);

        // Admin action buttons
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // Assign to me (quick)
        if (ticket.getAssignedTo() == null || 
            (currentUser != null && ticket.getAssignedTo() != currentUser.getId())) {
            TLButton assignBtn = new TLButton(I18n.get("support.admin.ticket.assign_me"), TLButton.ButtonVariant.OUTLINE);
            assignBtn.setOnAction(e -> assignTicket(ticket.getId(), currentUser != null ? currentUser.getId() : 0));
            actionBox.getChildren().add(assignBtn);
        }

        // Assign to another admin dropdown
        TLSelect<String> assignSelect = new TLSelect<>(I18n.get("support.admin.ticket.assign_to"));
        assignSelect.setPrefWidth(200);
        List<User> admins = userService.findByRole("ADMIN");
        Map<String, Integer> adminNameToId = new java.util.LinkedHashMap<>();
        for (User admin : admins) {
            String display = admin.getFullName();
            adminNameToId.put(display, admin.getId());
            assignSelect.getItems().add(display);
        }
        if (ticket.getAssignedToName() != null) {
            assignSelect.setValue(ticket.getAssignedToName());
        }
        TLButton assignOtherBtn = new TLButton(I18n.get("support.admin.ticket.assign_action"), TLButton.ButtonVariant.OUTLINE);
        assignOtherBtn.setOnAction(e -> {
            String selected = assignSelect.getValue();
            if (selected != null && adminNameToId.containsKey(selected)) {
                assignTicket(ticket.getId(), adminNameToId.get(selected));
            }
        });
        actionBox.getChildren().addAll(assignSelect, assignOtherBtn);

        // Status transitions
        String status = ticket.getStatus() != null ? ticket.getStatus() : "OPEN";
        switch (status) {
            case "OPEN" -> {
                TLButton inProgressBtn = new TLButton(I18n.get("support.admin.action.in_progress"), TLButton.ButtonVariant.OUTLINE);
                inProgressBtn.setOnAction(e -> updateTicketStatus(ticket.getId(), "IN_PROGRESS"));
                actionBox.getChildren().add(inProgressBtn);
            }
            case "IN_PROGRESS" -> {
                TLButton resolveBtn = new TLButton(I18n.get("support.admin.ticket.resolve"), TLButton.ButtonVariant.SUCCESS);
                resolveBtn.setOnAction(e -> updateTicketStatus(ticket.getId(), "RESOLVED"));
                actionBox.getChildren().add(resolveBtn);
            }
            case "RESOLVED" -> {
                TLButton closeBtn = new TLButton(I18n.get("support.admin.action.close"), TLButton.ButtonVariant.OUTLINE);
                closeBtn.setOnAction(e -> updateTicketStatus(ticket.getId(), "CLOSED"));
                actionBox.getChildren().add(closeBtn);
                
                TLButton reopenBtn = new TLButton(I18n.get("support.admin.action.reopen"), TLButton.ButtonVariant.OUTLINE);
                reopenBtn.setOnAction(e -> updateTicketStatus(ticket.getId(), "OPEN"));
                actionBox.getChildren().add(reopenBtn);
            }
            case "CLOSED" -> {
                TLButton reopenBtn = new TLButton(I18n.get("support.admin.action.reopen"), TLButton.ButtonVariant.OUTLINE);
                reopenBtn.setOnAction(e -> updateTicketStatus(ticket.getId(), "OPEN"));
                actionBox.getChildren().add(reopenBtn);
            }
        }

        // Delete button (always available)
        TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
        deleteBtn.setOnAction(e -> deleteTicket(ticket.getId()));
        actionBox.getChildren().add(deleteBtn);

        // PDF export button (branch integration)
        TLButton pdfBtn = new TLButton("📄 PDF", TLButton.ButtonVariant.OUTLINE);
        pdfBtn.setTooltip(new Tooltip("Export ticket to PDF"));
        pdfBtn.setOnAction(e -> exportTicketToPdf(ticket, messages));
        actionBox.getChildren().add(pdfBtn);

        headerRow.getChildren().add(actionBox);

        // Ticket info card
        TLCard infoCard = new TLCard();
        VBox infoContent = new VBox(8);

        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.user"),
                ticket.getUserName() != null ? ticket.getUserName() : "#" + ticket.getUserId()));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.category"),
                ticket.getCategory() != null ? ticket.getCategory() : "-"));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.assigned"),
                ticket.getAssignedToName() != null ? ticket.getAssignedToName() : I18n.get("support.admin.ticket.unassigned")));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.date"),
                ticket.getCreatedDate() != null
                        ? ticket.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"));
        if (ticket.getSlaDueDate() != null) {
            infoContent.getChildren().add(createDetailRow(I18n.get("ticket.sla_due"),
                    ticket.getSlaDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        }
        if (ticket.getFirstResponseDate() != null) {
            infoContent.getChildren().add(createDetailRow(I18n.get("ticket.first_response"),
                    ticket.getFirstResponseDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        }

        // Full description
        Label descTitle = new Label(I18n.get("ticket.description"));
        descTitle.getStyleClass().addAll("font-bold", "pt-8");
        Label descBody = new Label(ticket.getDescription() != null ? ticket.getDescription() : "-");
        descBody.setWrapText(true);
        descBody.getStyleClass().add("text-muted");

        infoContent.getChildren().addAll(descTitle, descBody);
        infoCard.setContent(infoContent);

        // Conversation section
        Label convTitle = new Label(I18n.get("ticket.conversation"));
        convTitle.getStyleClass().addAll("text-base", "font-bold", "pt-8");

        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(8, 0, 8, 0));

        if (messages.isEmpty()) {
            Label noMsg = new Label(I18n.get("ticket.no_messages"));
            noMsg.getStyleClass().add("text-muted");
            messagesBox.getChildren().add(noMsg);
        } else {
            for (TicketMessage msg : messages) {
                messagesBox.getChildren().add(createMessageBubble(msg,
                        attachmentsByMessage != null ? attachmentsByMessage.get(msg.getId()) : null));
            }
        }

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.setPrefHeight(250);
        msgScroll.getStyleClass().add("bg-transparent");

        // Reply box (admin can always reply unless CLOSED)
        VBox replySection = new VBox(8);
        if (!"CLOSED".equals(ticket.getStatus())) {
            TLTextarea replyArea = new TLTextarea(I18n.get("support.admin.reply"), I18n.get("support.admin.reply.placeholder"));
            replyArea.getControl().setPrefRowCount(6);
            replyArea.getControl().setPrefHeight(140);
            replyArea.getControl().setMinHeight(120);
            replyArea.getControl().getStyleClass().add("reply-textarea");

            // Internal note toggle
            TLSwitch internalSwitch = new TLSwitch();
            Label internalLabel = new Label(I18n.get("support.admin.reply.internal"));
            internalLabel.getStyleClass().addAll("text-muted", "text-sm");
            HBox internalToggle = new HBox(8, internalSwitch, internalLabel);
            internalToggle.setAlignment(Pos.CENTER_LEFT);

            List<File> pendingAttachments = new ArrayList<>();
            HBox chipsRow = new HBox(6);
            chipsRow.setAlignment(Pos.CENTER_LEFT);
            chipsRow.setVisible(false);
            chipsRow.setManaged(false);

            TLButton attachBtn = new TLButton("", TLButton.ButtonVariant.OUTLINE);
            attachBtn.setGraphic(com.skilora.utils.SvgIcons.icon(com.skilora.utils.SvgIcons.FILE_TEXT, 16));
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

            TLButton sendBtn = new TLButton(I18n.get("ticket.send_reply"), TLButton.ButtonVariant.PRIMARY);
            sendBtn.setOnAction(e -> {
                String text = replyArea.getText() != null ? replyArea.getText().trim() : "";
                if (!text.isEmpty() || !pendingAttachments.isEmpty()) {
                    sendAdminReply(ticket.getId(), text, internalSwitch.isSelected(), pendingAttachments);
                }
            });

            // AI Suggestion button — context-aware reply suggestion (subject, description, category, full conversation)
            TLButton aiSuggestBtn = new TLButton("✨ AI Suggest", TLButton.ButtonVariant.OUTLINE);
            aiSuggestBtn.setTooltip(new Tooltip(I18n.get("support.admin.ai_suggest_tooltip", "Suggest a reply using ticket and conversation context")));
            aiSuggestBtn.setOnAction(e -> {
                aiSuggestBtn.setDisable(true);
                String convThread = buildConversationThreadForAI(messages, ticket.getUserId());
                Task<String> aiTask = new Task<>() {
                    @Override protected String call() {
                        return geminiService.suggestReplyWithContext(
                            ticket.getSubject() != null ? ticket.getSubject() : "",
                            ticket.getDescription() != null ? ticket.getDescription() : "",
                            ticket.getCategory(),
                            convThread);
                    }
                };
                aiTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                    String suggestion = aiTask.getValue();
                    if (suggestion != null && !suggestion.isBlank() && !suggestion.startsWith("Error:")) {
                        replyArea.setText(suggestion);
                    } else if (suggestion != null && suggestion.startsWith("Error:")) {
                        if (contentPane != null && contentPane.getScene() != null) {
                            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("support.admin.ai_suggest_error", "AI suggestion failed"));
                        }
                    }
                    aiSuggestBtn.setDisable(false);
                }));
                aiTask.setOnFailed(ev -> Platform.runLater(() -> {
                    logger.error("AI suggestion failed", aiTask.getException());
                    if (contentPane != null && contentPane.getScene() != null) {
                        TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("support.admin.ai_suggest_error", "AI suggestion failed"));
                    }
                    aiSuggestBtn.setDisable(false);
                }));
                AppThreadPool.execute(aiTask);
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
                                    String current = replyArea.getText() != null ? replyArea.getText() : "";
                                    replyArea.setText(current.isEmpty() ? transcript : current + " " + transcript);
                                }
                                voiceBtn.setDisable(false);
                            }));
                            sttTask.setOnFailed(ev -> Platform.runLater(() -> voiceBtn.setDisable(false)));
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

            HBox replyActions = new HBox(12, internalToggle, aiSuggestBtn, voiceBtn, attachBtn, sendBtn);
            replyActions.setAlignment(Pos.CENTER_RIGHT);

            replySection.getChildren().addAll(replyArea, chipsRow, replyActions);
        } else {
            Label closedLabel = new Label(I18n.get("ticket.closed_notice"));
            closedLabel.getStyleClass().addAll("text-muted", "italic");
            replySection.getChildren().add(closedLabel);
        }

        detailView.getChildren().addAll(backBtn, headerRow, infoCard, convTitle, msgScroll, replySection);

        ScrollPane outerScroll = new ScrollPane(detailView);
        outerScroll.setFitToWidth(true);
        outerScroll.getStyleClass().add("bg-transparent");
        VBox.setVgrow(outerScroll, Priority.ALWAYS);
        contentPane.getChildren().add(outerScroll);
    }

    /**
     * Builds a plain-text conversation thread for the AI (subject + description + messages).
     * Excludes internal notes so the suggestion is appropriate for a user-visible reply.
     */
    private String buildConversationThreadForAI(List<TicketMessage> messages, int ticketUserId) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (TicketMessage m : messages) {
            if (m.isInternal()) continue;
            String who = (ticketUserId > 0 && m.getSenderId() == ticketUserId)
                    ? "User"
                    : (m.getSenderName() != null ? "Support (" + m.getSenderName() + ")" : "Support");
            String text = m.getMessage() != null ? m.getMessage().trim() : "";
            if (text.isEmpty()) continue;
            sb.append(who).append(": ").append(text).append("\n");
        }
        return sb.toString().trim();
    }

    private VBox createMessageBubble(TicketMessage msg, List<TicketAttachment> attachments) {
        VBox bubble = new VBox(4);
        boolean isAdmin = currentUser != null && msg.getSenderId() == currentUser.getId();

        bubble.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.setPadding(new Insets(0, isAdmin ? 0 : 40, 0, isAdmin ? 40 : 0));

        String senderInfo = (msg.getSenderName() != null ? msg.getSenderName() : "#" + msg.getSenderId());
        if (msg.getSenderRole() != null && !msg.getSenderRole().isEmpty()) {
            senderInfo += " (" + msg.getSenderRole() + ")";
        }
        if (msg.isInternal()) {
            senderInfo += " [" + I18n.get("support.admin.reply.internal_label") + "]";
        }
        Label senderLabel = new Label(senderInfo);
        senderLabel.getStyleClass().addAll("text-2xs", "text-muted");

        Label msgLabel = new Label(msg.getMessage());
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(500);

        String bubbleClass;
        if (msg.isInternal()) {
            bubbleClass = "chat-bubble-internal";
        } else if (isAdmin) {
            bubbleClass = "chat-bubble-mine";
        } else {
            bubbleClass = "chat-bubble-other";
        }
        msgLabel.getStyleClass().add(bubbleClass);

        Label timeLabel = new Label(msg.getCreatedDate() != null
                ? msg.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        timeLabel.getStyleClass().addAll("text-3xs", "text-muted");

        VBox attachmentsBox = createAttachmentsBox(attachments);
        if (attachmentsBox != null) {
            bubble.getChildren().addAll(senderLabel, msgLabel, attachmentsBox, timeLabel);
        } else {
            bubble.getChildren().addAll(senderLabel, msgLabel, timeLabel);
        }
        return bubble;
    }

    private VBox createAttachmentsBox(List<TicketAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        VBox box = new VBox(4);
        for (TicketAttachment a : attachments) {
            Hyperlink link = new Hyperlink(a.getFileName() != null ? a.getFileName() : "");
            link.getStyleClass().add("attachment-link");
            link.setGraphic(com.skilora.utils.SvgIcons.icon(com.skilora.utils.SvgIcons.FILE_TEXT, 14));
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

    private void sendAdminReply(int ticketId, String text, boolean isInternal, List<File> attachments) {
        if (currentUser == null) return;

        List<File> files = attachments != null ? new ArrayList<>(attachments) : List.of();
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                TicketMessage msg = new TicketMessage(ticketId, currentUser.getId(), text, isInternal);
                int messageId = messageService.addMessage(msg);
                if (messageId > 0 && !isInternal) {
                    ticketService.markFirstResponseIfMissing(ticketId);
                }
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
            logger.error("Failed to send admin reply", task.getException());
            Platform.runLater(() -> DialogUtils.showError(I18n.get("message.error"), I18n.get("error.db.operation")));
        });

        AppThreadPool.execute(task);
    }

    private void assignTicket(int ticketId, int adminId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return ticketService.assign(ticketId, adminId);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> openTicketDetail(ticketId)));
        task.setOnFailed(e -> {
            logger.error("Failed to assign ticket {}", ticketId, task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_assign_ticket"));
        });

        AppThreadPool.execute(task);
    }

    private void updateTicketStatus(int ticketId, String status) {
        Runnable doUpdate = () -> {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return ticketService.updateStatus(ticketId, status);
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(() -> openTicketDetail(ticketId)));
            task.setOnFailed(e -> {
                logger.error("Failed to update ticket {} status", ticketId, task.getException());
                TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_update_ticket"));
            });

            AppThreadPool.execute(task);
        };

        // Confirmation for destructive actions
        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            String titleKey = "RESOLVED".equals(status) ? "support.admin.resolve.confirm.title" : "support.admin.close.confirm.title";
            String msgKey = "RESOLVED".equals(status) ? "support.admin.resolve.confirm.message" : "support.admin.close.confirm.message";
            DialogUtils.showConfirmation(I18n.get(titleKey), I18n.get(msgKey)).ifPresent(result -> {
                if (result == ButtonType.OK) doUpdate.run();
            });
        } else {
            doUpdate.run();
        }
    }

    private void deleteTicket(int ticketId) {
        DialogUtils.showConfirmation(
                I18n.get("support.admin.delete.confirm.title"),
                I18n.get("support.admin.delete.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                contentPane.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ticketService.delete(ticketId);
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    contentPane.setDisable(false);
                    TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.deleted"));
                    showTicketsTab();
                }));
                task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== FAQ Management ====================

    private void showFAQTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<FAQArticle>> task = new Task<>() {
            @Override
            protected List<FAQArticle> call() {
                return faqService.findAllIncludingDrafts();
            }
        };

        task.setOnSucceeded(e -> {
            List<FAQArticle> articles = task.getValue();
            Platform.runLater(() -> displayFAQAdmin(articles));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load FAQ articles", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_faq"));
        });

        AppThreadPool.execute(task);
    }

    private void displayFAQAdmin(List<FAQArticle> articles) {
        contentPane.getChildren().clear();

        TLButton addBtn = new TLButton(I18n.get("support.admin.faq.add"), TLButton.ButtonVariant.PRIMARY);
        addBtn.setOnAction(e -> showFAQDialog(null));
        HBox btnBox = new HBox(addBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        contentPane.getChildren().add(btnBox);

        if (articles == null || articles.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("support.admin.faq.empty")));
            return;
        }

        VBox faqList = new VBox(12);

        for (FAQArticle article : articles) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            Label questionLabel = new Label(article.getQuestion() != null ? article.getQuestion() : "-");
            questionLabel.getStyleClass().addAll("text-sm", "font-bold");
            questionLabel.setWrapText(true);

            Label answerLabel = new Label(article.getAnswer() != null ? article.getAnswer() : "-");
            answerLabel.setWrapText(true);
            answerLabel.getStyleClass().add("text-muted");

            content.getChildren().addAll(questionLabel, answerLabel);

            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.category"),
                    article.getCategory() != null ? article.getCategory() : "-"));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.views"),
                    String.valueOf(article.getViewCount())));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.helpful"),
                    String.valueOf(article.getHelpfulCount())));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.not_helpful"),
                    String.valueOf(article.getNotHelpfulCount())));

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));

            TLButton editBtn = new TLButton(I18n.get("common.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> showFAQDialog(article));

            TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setOnAction(e -> deleteFAQ(article.getId()));

            actions.getChildren().addAll(editBtn, deleteBtn);
            content.getChildren().add(actions);

            card.setContent(content);
            faqList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(faqList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void showFAQDialog(FAQArticle existing) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(existing == null
                ? I18n.get("support.admin.faq.add")
                : I18n.get("support.admin.faq.edit"));

        VBox form = new VBox(12);
        form.setPrefWidth(500);

        TLTextField categoryField = new TLTextField(I18n.get("support.admin.faq.category"), "");
        TLTextField questionField = new TLTextField(I18n.get("support.admin.faq.question"), "");
        TLTextarea answerField = new TLTextarea(I18n.get("support.admin.faq.answer"));
        answerField.getControl().setPrefRowCount(8);
        answerField.getControl().setPrefHeight(160);

        TLSelect<String> langSelect = new TLSelect<>(I18n.get("support.admin.faq.language"));
        langSelect.getItems().addAll("fr", "en", "ar");
        langSelect.setValue("fr");

        if (existing != null) {
            categoryField.setText(existing.getCategory() != null ? existing.getCategory() : "");
            questionField.setText(existing.getQuestion() != null ? existing.getQuestion() : "");
            answerField.setText(existing.getAnswer() != null ? existing.getAnswer() : "");
            if (existing.getLanguage() != null) langSelect.setValue(existing.getLanguage());
        }

        form.getChildren().addAll(categoryField, questionField, answerField, langSelect);
        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.getDialogPane().setPrefWidth(560);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String q = questionField.getText() != null ? questionField.getText().trim() : "";
                String a = answerField.getText() != null ? answerField.getText().trim() : "";
                if (q.isEmpty()) {
                    questionField.setError(I18n.get("error.validation.required", I18n.get("support.admin.faq.question")));
                    valid = false;
                } else { questionField.clearValidation(); }
                if (a.isEmpty()) {
                    answerField.setError(I18n.get("error.validation.required", I18n.get("support.admin.faq.answer")));
                    valid = false;
                } else { answerField.clearValidation(); }
                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                saveFAQ(existing, categoryField.getText(), questionField.getText(),
                        answerField.getText(), langSelect.getValue());
            }
        });
    }

    private void saveFAQ(FAQArticle existing, String category, String question, String answer, String language) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                if (existing == null) {
                    FAQArticle article = new FAQArticle(category, question, answer, language);
                    return faqService.create(article) > 0;
                } else {
                    existing.setCategory(category);
                    existing.setQuestion(question);
                    existing.setAnswer(answer);
                    existing.setLanguage(language);
                    return faqService.update(existing);
                }
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::showFAQTab));
        task.setOnFailed(e -> logger.error("Failed to save FAQ article", task.getException()));
        AppThreadPool.execute(task);
    }

    private void deleteFAQ(int id) {
        DialogUtils.showConfirmation(I18n.get("common.confirm"), I18n.get("support.admin.faq.delete_confirm"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        Task<Boolean> task = new Task<>() {
                            @Override
                            protected Boolean call() { return faqService.delete(id); }
                        };
                        task.setOnSucceeded(e -> Platform.runLater(this::showFAQTab));
                        task.setOnFailed(e -> logger.error("Failed to delete FAQ article {}", id, task.getException()));
                        AppThreadPool.execute(task);
                    }
                });
    }

    // ==================== Auto-Responses ====================

    private void showAutoResponsesTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<AutoResponse>> task = new Task<>() {
            @Override
            protected List<AutoResponse> call() { return autoResponseService.findAll(); }
        };

        task.setOnSucceeded(e -> {
            List<AutoResponse> responses = task.getValue();
            Platform.runLater(() -> displayAutoResponses(responses));
        });

        task.setOnFailed(e -> logger.error("Failed to load auto responses", task.getException()));
        AppThreadPool.execute(task);
    }

    private void displayAutoResponses(List<AutoResponse> responses) {
        contentPane.getChildren().clear();

        TLButton addBtn = new TLButton(I18n.get("support.admin.auto.add"), TLButton.ButtonVariant.PRIMARY);
        addBtn.setOnAction(e -> showAutoResponseDialog(null));
        HBox btnBox = new HBox(addBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        contentPane.getChildren().add(btnBox);

        if (responses == null || responses.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("support.admin.auto.empty")));
            return;
        }

        VBox responseList = new VBox(12);

        for (AutoResponse ar : responses) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            Label keywordLabel = new Label(ar.getTriggerKeyword() != null ? ar.getTriggerKeyword() : "-");
            keywordLabel.getStyleClass().addAll("text-sm", "font-bold");

            TLBadge activeBadge = new TLBadge(
                    ar.isActive() ? I18n.get("common.active") : I18n.get("common.inactive"),
                    ar.isActive() ? TLBadge.Variant.SUCCESS : TLBadge.Variant.SECONDARY);

            HBox header = new HBox(12, keywordLabel, activeBadge);
            header.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(header);

            Label responseLabel = new Label(ar.getResponseText() != null ? ar.getResponseText() : "-");
            responseLabel.setWrapText(true);
            responseLabel.getStyleClass().add("text-muted");
            content.getChildren().add(responseLabel);

            content.getChildren().add(createDetailRow(I18n.get("support.admin.auto.category"),
                    ar.getCategory() != null ? ar.getCategory() : "-"));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.auto.usage"),
                    String.valueOf(ar.getUsageCount())));

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));

            TLButton toggleBtn = new TLButton(
                    ar.isActive() ? I18n.get("common.deactivate") : I18n.get("common.activate"),
                    TLButton.ButtonVariant.OUTLINE);
            toggleBtn.setOnAction(e -> toggleAutoResponse(ar.getId()));

            TLButton editBtn = new TLButton(I18n.get("common.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> showAutoResponseDialog(ar));

            TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setOnAction(e -> deleteAutoResponse(ar.getId()));

            actions.getChildren().addAll(toggleBtn, editBtn, deleteBtn);
            content.getChildren().add(actions);

            card.setContent(content);
            responseList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(responseList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void showAutoResponseDialog(AutoResponse existing) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(existing == null
                ? I18n.get("support.admin.auto.add")
                : I18n.get("support.admin.auto.edit"));

        VBox form = new VBox(12);
        form.setPrefWidth(480);

        TLTextField keywordField = new TLTextField(I18n.get("support.admin.auto.keyword"), "");
        TLTextarea responseField = new TLTextarea(I18n.get("support.admin.auto.response"));
        responseField.getControl().setPrefRowCount(6);
        responseField.getControl().setPrefHeight(140);
        TLTextField categoryField = new TLTextField(I18n.get("support.admin.auto.category"), "");

        TLSelect<String> langSelect = new TLSelect<>(I18n.get("support.admin.auto.language"));
        langSelect.getItems().addAll("fr", "en", "ar");
        langSelect.setValue("fr");

        if (existing != null) {
            keywordField.setText(existing.getTriggerKeyword() != null ? existing.getTriggerKeyword() : "");
            responseField.setText(existing.getResponseText() != null ? existing.getResponseText() : "");
            categoryField.setText(existing.getCategory() != null ? existing.getCategory() : "");
            if (existing.getLanguage() != null) langSelect.setValue(existing.getLanguage());
        }

        form.getChildren().addAll(keywordField, responseField, categoryField, langSelect);
        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.getDialogPane().setPrefWidth(540);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String kw = keywordField.getText() != null ? keywordField.getText().trim() : "";
                String resp = responseField.getText() != null ? responseField.getText().trim() : "";
                if (kw.isEmpty()) {
                    keywordField.setError(I18n.get("error.validation.required", I18n.get("support.admin.auto.keyword")));
                    valid = false;
                } else { keywordField.clearValidation(); }
                if (resp.isEmpty()) {
                    responseField.setError(I18n.get("error.validation.required", I18n.get("support.admin.auto.response")));
                    valid = false;
                } else { responseField.clearValidation(); }
                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                saveAutoResponse(existing, keywordField.getText(), responseField.getText(),
                        categoryField.getText(), langSelect.getValue());
            }
        });
    }

    private void saveAutoResponse(AutoResponse existing, String keyword, String response,
                                  String category, String language) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                if (existing == null) {
                    AutoResponse ar = new AutoResponse(keyword, response, category, language);
                    return autoResponseService.create(ar) > 0;
                } else {
                    existing.setTriggerKeyword(keyword);
                    existing.setResponseText(response);
                    existing.setCategory(category);
                    existing.setLanguage(language);
                    return autoResponseService.update(existing);
                }
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::showAutoResponsesTab));
        task.setOnFailed(e -> logger.error("Failed to save auto response", task.getException()));
        AppThreadPool.execute(task);
    }

    private void toggleAutoResponse(int id) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() { return autoResponseService.toggleActive(id); }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::showAutoResponsesTab));
        task.setOnFailed(e -> logger.error("Failed to toggle auto response {}", id, task.getException()));
        AppThreadPool.execute(task);
    }

    private void deleteAutoResponse(int id) {
        DialogUtils.showConfirmation(I18n.get("common.confirm"), I18n.get("support.admin.auto.delete_confirm"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        Task<Boolean> task = new Task<>() {
                            @Override
                            protected Boolean call() { return autoResponseService.delete(id); }
                        };
                        task.setOnSucceeded(e -> Platform.runLater(this::showAutoResponsesTab));
                        task.setOnFailed(e -> logger.error("Failed to delete auto response {}", id, task.getException()));
                        AppThreadPool.execute(task);
                    }
                });
    }

    // ==================== Feedback ====================

    private void showFeedbackTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<UserFeedback>> task = new Task<>() {
            @Override
            protected List<UserFeedback> call() { return feedbackService.findAll(); }
        };

        task.setOnSucceeded(e -> {
            List<UserFeedback> feedbackList = task.getValue();
            Platform.runLater(() -> displayFeedback(feedbackList));
        });

        task.setOnFailed(e -> logger.error("Failed to load feedback", task.getException()));
        AppThreadPool.execute(task);
    }

    private void displayFeedback(List<UserFeedback> feedbackList) {
        contentPane.getChildren().clear();

        if (feedbackList == null || feedbackList.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("support.admin.feedback.empty")));
            return;
        }

        VBox feedbackContainer = new VBox(12);

        for (UserFeedback fb : feedbackList) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            Label userLabel = new Label(fb.getUserName() != null
                    ? fb.getUserName() : I18n.get("support.admin.feedback.user") + " #" + fb.getUserId());
            userLabel.getStyleClass().addAll("text-sm", "font-bold");

            TLBadge typeBadge = new TLBadge(
                    fb.getFeedbackType() != null ? fb.getFeedbackType() : "-",
                    TLBadge.Variant.SECONDARY);

            HBox header = new HBox(12, userLabel, typeBadge);
            header.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(header);

            String stars = "\u2605".repeat(Math.max(0, fb.getRating()))
                    + "\u2606".repeat(Math.max(0, 5 - fb.getRating()));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.feedback.rating"), stars));

            if (fb.getComment() != null && !fb.getComment().isEmpty()) {
                Label commentLabel = new Label(fb.getComment());
                commentLabel.setWrapText(true);
                commentLabel.getStyleClass().add("text-muted");
                content.getChildren().add(commentLabel);
            }

            content.getChildren().add(createDetailRow(I18n.get("support.admin.feedback.date"),
                    fb.getCreatedDate() != null
                            ? fb.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"));

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setPadding(new Insets(8, 0, 0, 0));

            if (!fb.isResolved()) {
                TLButton resolveBtn = new TLButton(I18n.get("support.admin.feedback.resolve"), TLButton.ButtonVariant.SUCCESS);
                resolveBtn.setOnAction(e -> resolveFeedback(fb.getId()));
                actions.getChildren().add(resolveBtn);
            } else {
                TLBadge resolvedBadge = new TLBadge(I18n.get("support.admin.feedback.resolved_label"), TLBadge.Variant.SUCCESS);
                content.getChildren().add(resolvedBadge);
            }

            TLButton deleteFbBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
            deleteFbBtn.setOnAction(e -> deleteFeedback(fb.getId()));
            actions.getChildren().add(deleteFbBtn);

            content.getChildren().add(actions);
            card.setContent(content);
            feedbackContainer.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(feedbackContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void resolveFeedback(int id) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() { return feedbackService.resolve(id); }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::showFeedbackTab));
        task.setOnFailed(e -> logger.error("Failed to resolve feedback {}", id, task.getException()));
        AppThreadPool.execute(task);
    }

    private void deleteFeedback(int id) {
        DialogUtils.showConfirmation(I18n.get("common.confirm"), I18n.get("support.admin.feedback.delete_confirm"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        Task<Boolean> task = new Task<>() {
                            @Override
                            protected Boolean call() { return feedbackService.delete(id); }
                        };
                        task.setOnSucceeded(e -> Platform.runLater(this::showFeedbackTab));
                        task.setOnFailed(e -> logger.error("Failed to delete feedback {}", id, task.getException()));
                        AppThreadPool.execute(task);
                    }
                });
    }

    // ==================== Stats ====================

    private void showStatsTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<StatsData> task = new Task<>() {
            @Override
            protected StatsData call() {
                long openCount = ticketService.countOpen();
                long resolvedCount = ticketService.countByStatus("RESOLVED");
                long closedCount = ticketService.countByStatus("CLOSED");
                long slaBreached = ticketService.countSlaBreachedOpen();
                double avgFirstResponseMins = ticketService.getAvgFirstResponseMinutes();
                double avgResolutionMins = ticketService.getAvgResolutionMinutes();
                double avgRating = feedbackService.getAverageRating();
                return new StatsData(openCount, resolvedCount, closedCount, slaBreached, avgFirstResponseMins, avgResolutionMins, avgRating);
            }
        };

        task.setOnSucceeded(e -> {
            StatsData stats = task.getValue();
            Platform.runLater(() -> displayStats(stats));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load stats", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("support.admin.stats.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void displayStats(StatsData stats) {
        contentPane.getChildren().clear();

        Label headerLabel = new Label(I18n.get("support.admin.stats.title"));
        headerLabel.getStyleClass().addAll("text-lg", "font-bold");
        contentPane.getChildren().add(headerLabel);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.setPadding(new Insets(16, 0, 0, 0));

        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.open_tickets"), String.valueOf(stats.openCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.resolved_tickets"), String.valueOf(stats.resolvedCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.closed_tickets"), String.valueOf(stats.closedCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.sla_breached"), String.valueOf(stats.slaBreached)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.avg_first_response"), formatMinutes(stats.avgFirstResponseMins)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.avg_resolution"), formatMinutes(stats.avgResolutionMins)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.avg_rating"), String.format("%.1f / 5", stats.avgRating)));

        contentPane.getChildren().add(statsGrid);
    }

    private String formatMinutes(double minutes) {
        if (minutes <= 0) return "-";
        if (minutes < 60) return String.format("%.0f min", minutes);
        double hours = minutes / 60.0;
        return String.format("%.1f h", hours);
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
            remove.setGraphic(com.skilora.utils.SvgIcons.icon(com.skilora.utils.SvgIcons.X_CIRCLE, 14));
            remove.setTooltip(new Tooltip("Remove"));
            remove.setOnAction(e -> {
                attachments.remove(f);
                rebuildAttachmentChips(chipsRow, attachments);
            });

            chip.getChildren().addAll(name, remove);
            chipsRow.getChildren().add(chip);
        }
    }

    // ==================== PDF Export (Branch Integration) ====================

    private void exportTicketToPdf(SupportTicket ticket, List<TicketMessage> messages) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Ticket as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("ticket_" + ticket.getId() + ".pdf");
        File file = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws DocumentException, IOException {
                Document document = new Document();
                PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                document.open();

                document.add(new Paragraph("Ticket #" + ticket.getId()));
                document.add(new Paragraph("Subject: " + (ticket.getSubject() != null ? ticket.getSubject() : "-")));
                document.add(new Paragraph("Status: " + (ticket.getStatus() != null ? ticket.getStatus() : "-")));
                document.add(new Paragraph("Priority: " + (ticket.getPriority() != null ? ticket.getPriority() : "-")));
                document.add(new Paragraph("Category: " + (ticket.getCategory() != null ? ticket.getCategory() : "-")));
                document.add(new Paragraph("User: " + (ticket.getUserName() != null ? ticket.getUserName() : "#" + ticket.getUserId())));
                document.add(new Paragraph("Assigned To: " + (ticket.getAssignedToName() != null ? ticket.getAssignedToName() : "Unassigned")));
                document.add(new Paragraph("Created: " + (ticket.getCreatedDate() != null
                        ? ticket.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-")));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Description:"));
                document.add(new Paragraph(ticket.getDescription() != null ? ticket.getDescription() : "-"));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("--- Conversation ---"));
                document.add(new Paragraph(" "));

                if (messages != null) {
                    for (TicketMessage msg : messages) {
                        String sender = msg.getSenderName() != null ? msg.getSenderName() : "#" + msg.getSenderId();
                        String time = msg.getCreatedDate() != null
                                ? msg.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                        String prefix = msg.isInternal() ? "[INTERNAL] " : "";
                        document.add(new Paragraph(prefix + sender + " (" + time + "):"));
                        document.add(new Paragraph(msg.getMessage() != null ? msg.getMessage() : ""));
                        document.add(new Paragraph(" "));
                    }
                }

                document.close();
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() ->
            TLToast.success(contentPane.getScene(), I18n.get("message.success"), "PDF exported: " + file.getName())));
        task.setOnFailed(e -> {
            logger.error("Failed to export PDF", task.getException());
            Platform.runLater(() -> DialogUtils.showError(I18n.get("message.error"), "PDF export failed"));
        });
        AppThreadPool.execute(task);
    }

    // ==================== Helper Methods ====================

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().addAll("font-bold", "text-muted");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);
        Label valueNode = new Label(value != null ? value : "-");
        valueNode.setWrapText(true);
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private TLEmptyState createEmptyState(String message) {
        return new TLEmptyState(SvgIcons.SEARCH, message, "");
    }

    private StackPane createLoadingIndicator() {
        return TLSpinner.createCentered(TLSpinner.Size.LG);
    }

    private TLCard createStatCard(String title, String value) {
        TLCard card = new TLCard();
        card.setPrefWidth(220);
        VBox content = new VBox(8);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().addAll("text-xs", "text-muted");
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().addAll("text-2xl", "font-bold");
        content.getChildren().addAll(titleLbl, valueLbl);
        card.setContent(content);
        return card;
    }

    private TLBadge.Variant getTicketBadgeVariant(String status) {
        if (status == null) return TLBadge.Variant.DEFAULT;
        return switch (status) {
            case "OPEN" -> TLBadge.Variant.DEFAULT;
            case "IN_PROGRESS" -> TLBadge.Variant.SECONDARY;
            case "RESOLVED" -> TLBadge.Variant.SUCCESS;
            case "CLOSED" -> TLBadge.Variant.OUTLINE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }

    private TLBadge.Variant getPriorityBadgeVariant(String priority) {
        if (priority == null) return TLBadge.Variant.DEFAULT;
        return switch (priority) {
            case "LOW" -> TLBadge.Variant.SECONDARY;
            case "MEDIUM" -> TLBadge.Variant.DEFAULT;
            case "HIGH" -> TLBadge.Variant.DESTRUCTIVE;
            case "URGENT" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }

    // ===== Ticket Card Helper Methods =====

    private TLBadge createTicketStatusBadge(String status) {
        String label;
        TLBadge.Variant variant;

        if (status == null) {
            label = "OPEN";
            variant = TLBadge.Variant.DEFAULT;
        } else {
            label = status;
            variant = getTicketBadgeVariant(status);
        }

        TLBadge badge = new TLBadge(label, variant);
        badge.getStyleClass().add("ticket-status-badge");
        return badge;
    }

    private TLBadge createTicketPriorityBadge(String priority) {
        String label = priority != null ? priority : "MEDIUM";
        TLBadge.Variant variant = getPriorityBadgeVariant(priority);

        TLBadge badge = new TLBadge(label, variant);
        badge.getStyleClass().add("ticket-priority-badge");
        return badge;
    }

    private HBox createTicketMetadataItem(String iconSvg, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("ticket-metadata-item");

        javafx.scene.Node icon = SvgIcons.icon(iconSvg, 12, "-fx-muted-foreground");
        Label label = new Label(text);
        label.getStyleClass().addAll("text-xs", "text-muted");

        box.getChildren().addAll(icon, label);
        return box;
    }

    private HBox createSLAMetadataItem(String iconSvg, String text, String colorStyle, boolean bold) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("ticket-sla-item");

        javafx.scene.Node icon = SvgIcons.icon(iconSvg, 12, colorStyle);
        Label label = new Label(text);
        label.getStyleClass().addAll("text-xs");
        if (bold) {
            label.setStyle("-fx-text-fill: " + colorStyle.replace("-fx-", "") + "; -fx-font-weight: bold;");
        } else {
            label.setStyle("-fx-text-fill: " + colorStyle.replace("-fx-", "") + ";");
        }

        box.getChildren().addAll(icon, label);
        return box;
    }

    private void showAssignTicketDialog(SupportTicket ticket) {
        // TODO: Implement assign ticket dialog
        TLToast.info(contentPane.getScene(),
            I18n.get("support.admin.ticket.assign"),
            I18n.get("support.admin.ticket.assign.message", ticket.getId()));
    }

    private record TicketThreadData(List<TicketMessage> messages, Map<Integer, List<TicketAttachment>> attachmentsByMessage) {}

    private record StatsData(
            long openCount,
            long resolvedCount,
            long closedCount,
            long slaBreached,
            double avgFirstResponseMins,
            double avgResolutionMins,
            double avgRating) {}
}
