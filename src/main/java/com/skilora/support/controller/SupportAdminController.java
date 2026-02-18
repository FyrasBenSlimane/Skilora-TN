package com.skilora.support.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.support.entity.*;
import com.skilora.support.service.*;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

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
    private final FAQService faqService = FAQService.getInstance();
    private final AutoResponseService autoResponseService = AutoResponseService.getInstance();
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("support.admin.title"));
        subtitleLabel.setText(I18n.get("support.admin.subtitle"));
        createTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
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

    // ==================== Tickets Tab ====================

    private void showTicketsTab() {
        contentPane.getChildren().clear();

        // Filter bar
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

        loadAllTickets();
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

        VBox ticketList = new VBox(12);

        for (SupportTicket ticket : tickets) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));
            content.setStyle("-fx-cursor: hand;");

            // Subject header with status + priority badges
            Label subjectLabel = new Label(ticket.getSubject() != null ? ticket.getSubject() : "-");
            subjectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            TLBadge statusBadge = new TLBadge(
                    ticket.getStatus() != null ? ticket.getStatus() : "OPEN",
                    getTicketBadgeVariant(ticket.getStatus()));

            TLBadge priorityBadge = new TLBadge(
                    ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
                    getPriorityBadgeVariant(ticket.getPriority()));

            HBox header = new HBox(12, subjectLabel, statusBadge, priorityBadge);
            header.setAlignment(Pos.CENTER_LEFT);

            content.getChildren().add(header);

            // Description preview
            String desc = ticket.getDescription() != null ? ticket.getDescription() : "";
            if (!desc.isEmpty()) {
                Label descLabel = new Label(desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
                descLabel.setWrapText(true);
                descLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
                content.getChildren().add(descLabel);
            }

            // Info row
            HBox infoRow = new HBox(16);
            infoRow.setAlignment(Pos.CENTER_LEFT);
            
            Label userLabel = new Label(I18n.get("support.admin.ticket.user") + ": " + 
                (ticket.getUserName() != null ? ticket.getUserName() : "#" + ticket.getUserId()));
            userLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
            
            Label assignLabel = new Label(I18n.get("support.admin.ticket.assigned") + ": " +
                (ticket.getAssignedToName() != null ? ticket.getAssignedToName() : I18n.get("support.admin.ticket.unassigned")));
            assignLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
            
            Label dateLabel = new Label(ticket.getCreatedDate() != null
                    ? ticket.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-");
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

            infoRow.getChildren().addAll(userLabel, assignLabel, dateLabel);
            content.getChildren().add(infoRow);

            card.setContent(content);
            ticketList.getChildren().add(card);

            // Click to open ticket detail 
            content.setOnMouseClicked(e -> openTicketDetail(ticket.getId()));
        }

        ScrollPane scrollPane = new ScrollPane(ticketList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
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

            Task<List<TicketMessage>> msgTask = new Task<>() {
                @Override
                protected List<TicketMessage> call() {
                    return messageService.findByTicketId(ticketId);
                }
            };

            msgTask.setOnSucceeded(ev -> {
                List<TicketMessage> messages = msgTask.getValue();
                Platform.runLater(() -> showTicketDetailView(ticket, messages));
            });

            AppThreadPool.execute(msgTask);
        });

        AppThreadPool.execute(ticketTask);
    }

    private void showTicketDetailView(SupportTicket ticket, List<TicketMessage> messages) {
        contentPane.getChildren().clear();

        VBox detailView = new VBox(16);
        detailView.setPadding(new Insets(0));

        // Back button
        TLButton backBtn = new TLButton("\u2190 " + I18n.get("common.back"));
        backBtn.setVariant("SECONDARY");
        backBtn.setOnAction(e -> showTicketsTab());

        // Header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectLabel = new Label(ticket.getSubject());
        subjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

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

        // Assign to me
        if (ticket.getAssignedTo() == null || 
            (currentUser != null && ticket.getAssignedTo() != currentUser.getId())) {
            TLButton assignBtn = new TLButton(I18n.get("support.admin.ticket.assign_me"), TLButton.ButtonVariant.OUTLINE);
            assignBtn.setOnAction(e -> assignTicket(ticket.getId(), currentUser != null ? currentUser.getId() : 0));
            actionBox.getChildren().add(assignBtn);
        }

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

        headerRow.getChildren().add(actionBox);

        // Ticket info card
        TLCard infoCard = new TLCard();
        VBox infoContent = new VBox(8);
        infoContent.setPadding(new Insets(16));

        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.user"),
                ticket.getUserName() != null ? ticket.getUserName() : "#" + ticket.getUserId()));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.category"),
                ticket.getCategory() != null ? ticket.getCategory() : "-"));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.assigned"),
                ticket.getAssignedToName() != null ? ticket.getAssignedToName() : I18n.get("support.admin.ticket.unassigned")));
        infoContent.getChildren().add(createDetailRow(I18n.get("support.admin.ticket.date"),
                ticket.getCreatedDate() != null
                        ? ticket.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"));

        // Full description
        Label descTitle = new Label(I18n.get("ticket.description"));
        descTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        Label descBody = new Label(ticket.getDescription() != null ? ticket.getDescription() : "-");
        descBody.setWrapText(true);
        descBody.setStyle("-fx-text-fill: -fx-muted-foreground;");

        infoContent.getChildren().addAll(descTitle, descBody);
        infoCard.setContent(infoContent);

        // Conversation section
        Label convTitle = new Label(I18n.get("ticket.conversation"));
        convTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(8, 0, 8, 0));

        if (messages.isEmpty()) {
            Label noMsg = new Label(I18n.get("ticket.no_messages"));
            noMsg.setStyle("-fx-text-fill: -fx-muted-foreground;");
            messagesBox.getChildren().add(noMsg);
        } else {
            for (TicketMessage msg : messages) {
                messagesBox.getChildren().add(createMessageBubble(msg));
            }
        }

        ScrollPane msgScroll = new ScrollPane(messagesBox);
        msgScroll.setFitToWidth(true);
        msgScroll.setPrefHeight(250);
        msgScroll.setStyle("-fx-background-color: transparent;");

        // Reply box (admin can always reply unless CLOSED)
        VBox replySection = new VBox(8);
        if (!"CLOSED".equals(ticket.getStatus())) {
            TLTextarea replyArea = new TLTextarea(I18n.get("support.admin.reply"), I18n.get("support.admin.reply.placeholder"));
            replyArea.getControl().setPrefRowCount(6);
            replyArea.getControl().setPrefHeight(140);
            replyArea.getControl().setMinHeight(120);
            replyArea.getControl().setStyle("-fx-background-color: -fx-background; -fx-text-fill: -fx-foreground; -fx-border-color: -fx-input; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");

            // Internal note toggle
            TLSwitch internalSwitch = new TLSwitch();
            Label internalLabel = new Label(I18n.get("support.admin.reply.internal"));
            internalLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
            HBox internalToggle = new HBox(8, internalSwitch, internalLabel);
            internalToggle.setAlignment(Pos.CENTER_LEFT);

            TLButton sendBtn = new TLButton(I18n.get("ticket.send_reply"), TLButton.ButtonVariant.PRIMARY);
            sendBtn.setOnAction(e -> {
                String text = replyArea.getText() != null ? replyArea.getText().trim() : "";
                if (!text.isEmpty()) {
                    sendAdminReply(ticket.getId(), text, internalSwitch.isSelected());
                }
            });

            HBox replyActions = new HBox(12, internalToggle, sendBtn);
            replyActions.setAlignment(Pos.CENTER_RIGHT);

            replySection.getChildren().addAll(replyArea, replyActions);
        } else {
            Label closedLabel = new Label(I18n.get("ticket.closed_notice"));
            closedLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-style: italic;");
            replySection.getChildren().add(closedLabel);
        }

        detailView.getChildren().addAll(backBtn, headerRow, infoCard, convTitle, msgScroll, replySection);

        ScrollPane outerScroll = new ScrollPane(detailView);
        outerScroll.setFitToWidth(true);
        outerScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(outerScroll, Priority.ALWAYS);
        contentPane.getChildren().add(outerScroll);
    }

    private VBox createMessageBubble(TicketMessage msg) {
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
        senderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        Label msgLabel = new Label(msg.getMessage());
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(500);

        String bgColor;
        if (msg.isInternal()) {
            bgColor = "-fx-background-color: -fx-secondary; -fx-text-fill: -fx-muted-foreground; -fx-padding: 10 14; -fx-background-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-font-style: italic;";
        } else if (isAdmin) {
            bgColor = "-fx-background-color: -fx-primary; -fx-text-fill: -fx-primary-foreground; -fx-padding: 10 14; -fx-background-radius: 12;";
        } else {
            bgColor = "-fx-background-color: -fx-muted; -fx-text-fill: -fx-foreground; -fx-padding: 10 14; -fx-background-radius: 12;";
        }
        msgLabel.setStyle(bgColor);

        Label timeLabel = new Label(msg.getCreatedDate() != null
                ? msg.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-muted-foreground;");

        bubble.getChildren().addAll(senderLabel, msgLabel, timeLabel);
        return bubble;
    }

    private void sendAdminReply(int ticketId, String text, boolean isInternal) {
        if (currentUser == null) return;

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                TicketMessage msg = new TicketMessage(ticketId, currentUser.getId(), text, isInternal);
                return messageService.addMessage(msg);
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
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ticketService.delete(ticketId);
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.deleted"));
                    showTicketsTab();
                }));
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
            content.setPadding(new Insets(16));

            Label questionLabel = new Label(article.getQuestion() != null ? article.getQuestion() : "-");
            questionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            questionLabel.setWrapText(true);

            Label answerLabel = new Label(article.getAnswer() != null ? article.getAnswer() : "-");
            answerLabel.setWrapText(true);
            answerLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");

            content.getChildren().addAll(questionLabel, answerLabel);

            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.category"),
                    article.getCategory() != null ? article.getCategory() : "-"));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.views"),
                    String.valueOf(article.getViewCount())));
            content.getChildren().add(createDetailRow(I18n.get("support.admin.faq.helpful"),
                    String.valueOf(article.getHelpfulCount())));

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
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void showFAQDialog(FAQArticle existing) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(existing == null
                ? I18n.get("support.admin.faq.add")
                : I18n.get("support.admin.faq.edit"));

        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
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
            content.setPadding(new Insets(16));

            Label keywordLabel = new Label(ar.getTriggerKeyword() != null ? ar.getTriggerKeyword() : "-");
            keywordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            TLBadge activeBadge = new TLBadge(
                    ar.isActive() ? I18n.get("common.active") : I18n.get("common.inactive"),
                    ar.isActive() ? TLBadge.Variant.SUCCESS : TLBadge.Variant.SECONDARY);

            HBox header = new HBox(12, keywordLabel, activeBadge);
            header.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(header);

            Label responseLabel = new Label(ar.getResponseText() != null ? ar.getResponseText() : "-");
            responseLabel.setWrapText(true);
            responseLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
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
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void showAutoResponseDialog(AutoResponse existing) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(existing == null
                ? I18n.get("support.admin.auto.add")
                : I18n.get("support.admin.auto.edit"));

        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
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
            content.setPadding(new Insets(16));

            Label userLabel = new Label(fb.getUserName() != null
                    ? fb.getUserName() : I18n.get("support.admin.feedback.user") + " #" + fb.getUserId());
            userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

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
                commentLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
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
        scrollPane.setStyle("-fx-background-color: transparent;");
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
                double avgRating = feedbackService.getAverageRating();
                return new StatsData(openCount, resolvedCount, closedCount, avgRating);
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
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        contentPane.getChildren().add(headerLabel);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.setPadding(new Insets(16, 0, 0, 0));

        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.open_tickets"), String.valueOf(stats.openCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.resolved_tickets"), String.valueOf(stats.resolvedCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.closed_tickets"), String.valueOf(stats.closedCount)));
        statsGrid.getChildren().add(createStatCard(I18n.get("support.admin.stats.avg_rating"), String.format("%.1f / 5", stats.avgRating)));

        contentPane.getChildren().add(statsGrid);
    }

    // ==================== Helper Methods ====================

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-muted-foreground;");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);
        Label valueNode = new Label(value != null ? value : "-");
        valueNode.setWrapText(true);
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-muted-foreground;");
        emptyState.getChildren().add(label);
        return emptyState;
    }

    private StackPane createLoadingIndicator() {
        return TLSpinner.createCentered(TLSpinner.Size.LG);
    }

    private TLCard createStatCard(String title, String value) {
        TLCard card = new TLCard();
        card.setPrefWidth(220);
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
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

    private record StatsData(long openCount, long resolvedCount, long closedCount, double avgRating) {}
}
