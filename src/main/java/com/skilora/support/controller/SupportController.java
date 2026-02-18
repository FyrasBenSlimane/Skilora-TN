package com.skilora.support.controller;

import com.skilora.user.entity.*;
import com.skilora.support.entity.*;
import com.skilora.support.service.*;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.DialogUtils;
import com.skilora.framework.components.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SupportController — User/Employer support view.
 * Users can create tickets, view their tickets, open ticket detail with
 * conversation, reply, close/delete own tickets, use FAQ, chatbot, and feedback.
 */
public class SupportController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TLButton newTicketBtn;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;

    private User currentUser;

    private final SupportTicketService ticketService = SupportTicketService.getInstance();
    private final TicketMessageService messageService = TicketMessageService.getInstance();
    private final FAQService faqService = FAQService.getInstance();
    private final ChatbotService chatbotService = ChatbotService.getInstance();
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();

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
        tabs.addTab("tickets", I18n.get("support.tab.tickets"), (javafx.scene.Node) null);
        tabs.addTab("faq", I18n.get("support.tab.faq"), (javafx.scene.Node) null);
        tabs.addTab("chatbot", I18n.get("support.tab.chatbot"), (javafx.scene.Node) null);
        tabs.addTab("feedback", I18n.get("support.tab.feedback"), (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
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
        form.setPadding(new Insets(20));
        form.setPrefWidth(520);
        
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
        descriptionArea.getControl().setPrefRowCount(10);
        descriptionArea.getControl().setPrefHeight(200);
        descriptionArea.getControl().setPromptText(I18n.get("ticket.description.placeholder"));
        
        form.getChildren().addAll(subjectField, categorySelect, prioritySelect, descriptionArea);
        
        dialog.setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        // Set preferred dialog size
        dialog.getDialogPane().setPrefWidth(580);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String subject = subjectField.getText() != null ? subjectField.getText().trim() : "";
                if (subject.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("ticket.subject")));
                    return null;
                }
                String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
                if (description.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("ticket.description")));
                    return null;
                }
                createTicket(subject,
                            getCategoryKey(categorySelect.getValue()),
                            getPriorityKey(prioritySelect.getValue()),
                            description);
            }
            return buttonType;
        });
        
        dialog.showAndWait();
    }

    private void createTicket(String subject, String category, String priority, String description) {
        if (currentUser == null) return;
        
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
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

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.created"));
                loadTicketsTab();
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                DialogUtils.showError(I18n.get("message.error"), I18n.get("error.db.operation"));
            });
        });

        AppThreadPool.execute(task);
    }

    // ==================== Tickets List ====================

    private void loadTicketsTab() {
        if (currentUser == null) return;
        
        Task<List<SupportTicket>> task = new Task<>() {
            @Override
            protected List<SupportTicket> call() {
                return ticketService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<SupportTicket> tickets = task.getValue();
            Platform.runLater(() -> displayTickets(tickets));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load tickets", task.getException());
        });

        AppThreadPool.execute(task);
    }

    private void displayTickets(List<SupportTicket> tickets) {
        contentPane.getChildren().clear();
        
        if (tickets.isEmpty()) {
            VBox emptyState = createEmptyState(I18n.get("ticket.empty"));
            contentPane.getChildren().add(emptyState);
            return;
        }
        
        VBox ticketList = new VBox(12);
        ticketList.setPadding(new Insets(8, 0, 0, 0));
        
        for (SupportTicket ticket : tickets) {
            TLCard card = createTicketCard(ticket);
            ticketList.getChildren().add(card);
        }
        
        ScrollPane scrollPane = new ScrollPane(ticketList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private TLCard createTicketCard(SupportTicket ticket) {
        TLCard card = new TLCard();
        card.getStyleClass().add("card-interactive");
        
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        // Header row: subject + badges
        Label cardTitle = new Label(ticket.getSubject());
        cardTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        TLBadge statusBadge = new TLBadge(
            I18n.get("ticket.status." + ticket.getStatus().toLowerCase()),
            getBadgeVariant(ticket.getStatus())
        );
        
        TLBadge priorityBadge = new TLBadge(
            ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
            getPriorityBadgeVariant(ticket.getPriority())
        );
        
        HBox header = new HBox(12, cardTitle, statusBadge, priorityBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Description preview (truncated)
        String desc = ticket.getDescription() != null ? ticket.getDescription() : "";
        Label descLabel = new Label(desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
        
        // Footer: category + date
        Label categoryLabel = new Label(ticket.getCategory() != null ? ticket.getCategory() : "-");
        Label dateLabel = new Label(formatDate(ticket.getCreatedDate()));
        dateLabel.getStyleClass().add("text-muted");
        
        HBox footer = new HBox(16, categoryLabel, dateLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        content.getChildren().addAll(header, descLabel, footer);
        card.setContent(content);
        
        // Click to open ticket detail
        card.setOnMouseClicked(e -> openTicketDetail(ticket.getId()));
        
        return card;
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
                    return messageService.findByTicketIdPublic(ticketId);
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
        detailView.setPadding(new Insets(8, 0, 0, 0));
        
        // Back button
        TLButton backBtn = new TLButton("\u2190 " + I18n.get("common.back"));
        backBtn.setVariant("SECONDARY");
        backBtn.setOnAction(e -> loadTicketsTab());
        
        // Ticket header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label subjectLabel = new Label(ticket.getSubject());
        subjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        TLBadge statusBadge = new TLBadge(
            I18n.get("ticket.status." + ticket.getStatus().toLowerCase()),
            getBadgeVariant(ticket.getStatus())
        );
        TLBadge priorityBadge = new TLBadge(
            ticket.getPriority() != null ? ticket.getPriority() : "MEDIUM",
            getPriorityBadgeVariant(ticket.getPriority())
        );
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerRow.getChildren().addAll(subjectLabel, statusBadge, priorityBadge, spacer);
        
        // Action buttons (close / delete) — only if ticket is still open
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        if (!"CLOSED".equals(ticket.getStatus()) && !"RESOLVED".equals(ticket.getStatus())) {
            TLButton closeBtn = new TLButton(I18n.get("ticket.close"), TLButton.ButtonVariant.OUTLINE);
            closeBtn.setOnAction(e -> closeTicket(ticket.getId()));
            actionBox.getChildren().add(closeBtn);
        }
        
        TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
        deleteBtn.setOnAction(e -> deleteTicket(ticket.getId()));
        actionBox.getChildren().add(deleteBtn);
        
        headerRow.getChildren().add(actionBox);
        
        // Ticket info card
        TLCard infoCard = new TLCard();
        VBox infoContent = new VBox(8);
        infoContent.setPadding(new Insets(16));
        
        infoContent.getChildren().add(createDetailRow(I18n.get("ticket.category"), 
            ticket.getCategory() != null ? ticket.getCategory() : "-"));
        infoContent.getChildren().add(createDetailRow(I18n.get("ticket.date"), 
            formatDate(ticket.getCreatedDate())));
        if (ticket.getAssignedToName() != null) {
            infoContent.getChildren().add(createDetailRow(I18n.get("ticket.assigned_to"), 
                ticket.getAssignedToName()));
        }
        
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
        
        // Reply box (only if ticket is still open/in-progress)
        VBox replySection = new VBox(8);
        if (!"CLOSED".equals(ticket.getStatus()) && !"RESOLVED".equals(ticket.getStatus())) {
            TLTextarea replyArea = new TLTextarea(I18n.get("ticket.reply"), I18n.get("ticket.reply.placeholder"));
            replyArea.getControl().setPrefRowCount(6);
            replyArea.getControl().setPrefHeight(140);
            replyArea.getControl().setMinHeight(120);
            replyArea.getControl().setStyle("-fx-background-color: -fx-background; -fx-text-fill: -fx-foreground; -fx-border-color: -fx-input; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;");
            
            TLButton sendBtn = new TLButton(I18n.get("ticket.send_reply"), TLButton.ButtonVariant.PRIMARY);
            sendBtn.setOnAction(e -> {
                String text = replyArea.getText() != null ? replyArea.getText().trim() : "";
                if (!text.isEmpty()) {
                    sendReply(ticket.getId(), text);
                }
            });
            
            HBox replyActions = new HBox(sendBtn);
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
        boolean isMe = currentUser != null && msg.getSenderId() == currentUser.getId();
        
        bubble.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.setPadding(new Insets(0, isMe ? 0 : 40, 0, isMe ? 40 : 0));
        
        // Sender name + role
        String senderInfo = (msg.getSenderName() != null ? msg.getSenderName() : "#" + msg.getSenderId());
        if (msg.getSenderRole() != null && !msg.getSenderRole().isEmpty()) {
            senderInfo += " (" + msg.getSenderRole() + ")";
        }
        Label senderLabel = new Label(senderInfo);
        senderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
        
        // Message content
        Label msgLabel = new Label(msg.getMessage());
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(500);
        msgLabel.setStyle(isMe ?
            "-fx-background-color: -fx-primary; -fx-text-fill: -fx-primary-foreground; -fx-padding: 10 14; -fx-background-radius: 12;" :
            "-fx-background-color: -fx-muted; -fx-text-fill: -fx-foreground; -fx-padding: 10 14; -fx-background-radius: 12;");
        
        // Timestamp
        Label timeLabel = new Label(formatDate(msg.getCreatedDate()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-muted-foreground;");
        
        bubble.getChildren().addAll(senderLabel, msgLabel, timeLabel);
        return bubble;
    }

    private void sendReply(int ticketId, String text) {
        if (currentUser == null) return;
        
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                TicketMessage msg = new TicketMessage(ticketId, currentUser.getId(), text, false);
                return messageService.addMessage(msg);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> openTicketDetail(ticketId)));
        task.setOnFailed(e -> {
            logger.error("Failed to send reply", task.getException());
            Platform.runLater(() -> DialogUtils.showError(I18n.get("message.error"), I18n.get("error.db.operation")));
        });

        AppThreadPool.execute(task);
    }

    private void closeTicket(int ticketId) {
        DialogUtils.showConfirmation(
            I18n.get("ticket.close.confirm.title"),
            I18n.get("ticket.close.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ticketService.updateStatus(ticketId, "CLOSED");
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> openTicketDetail(ticketId)));
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
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ticketService.delete(ticketId);
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("ticket.deleted"));
                    loadTicketsTab();
                }));
                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== FAQ Tab ====================

    private void loadFAQTab() {
        Task<List<FAQArticle>> task = new Task<>() {
            @Override
            protected List<FAQArticle> call() {
                return faqService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<FAQArticle> articles = task.getValue();
            Platform.runLater(() -> displayFAQ(articles));
        });

        AppThreadPool.execute(task);
    }

    private void displayFAQ(List<FAQArticle> articles) {
        contentPane.getChildren().clear();
        
        VBox faqContainer = new VBox(16);
        faqContainer.setPadding(new Insets(8, 0, 0, 0));
        
        TLTextField searchField = new TLTextField("", I18n.get("faq.search"));
        
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
                
                TLCard faqCard = new TLCard();
                VBox cardContent = new VBox(8);
                cardContent.setPadding(new Insets(12));
                
                Label questionLabel = new Label(article.getQuestion());
                questionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                questionLabel.setWrapText(true);
                
                Label answerLabel = new Label(article.getAnswer());
                answerLabel.setWrapText(true);
                answerLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
                
                cardContent.getChildren().addAll(questionLabel, answerLabel);
                faqCard.setContent(cardContent);
                faqList.getChildren().add(faqCard);
            }
            
            if (faqList.getChildren().isEmpty()) {
                faqList.getChildren().add(createEmptyState(I18n.get("faq.empty")));
            }
        };
        
        searchField.getControl().textProperty().addListener((obs, oldVal, newVal) -> renderFaqs.run());
        renderFaqs.run();
        
        ScrollPane scrollPane = new ScrollPane(faqList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        faqContainer.getChildren().addAll(searchField, scrollPane);
        contentPane.getChildren().add(faqContainer);
    }

    // ==================== Chatbot Tab ====================

    private VBox chatMessages;
    private HBox typingIndicator;
    private int currentConversationId = -1;

    private void loadChatbotTab() {
        contentPane.getChildren().clear();
        
        VBox chatContainer = new VBox(12);
        chatContainer.setPadding(new Insets(8, 0, 0, 0));
        
        ScrollPane messageArea = new ScrollPane();
        messageArea.setFitToWidth(true);
        messageArea.setPrefHeight(400);
        messageArea.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(messageArea, Priority.ALWAYS);
        
        chatMessages = new VBox(8);
        chatMessages.setPadding(new Insets(8));
        messageArea.setContent(chatMessages);
        
        // Welcome message
        addChatMessage(I18n.get("chatbot.welcome"), false);
        
        // Start a conversation on a background thread to avoid blocking the UI
        if (currentUser != null) {
            Task<Integer> startTask = new Task<>() {
                @Override
                protected Integer call() {
                    return chatbotService.startConversation(currentUser.getId());
                }
            };
            startTask.setOnSucceeded(ev -> currentConversationId = startTask.getValue());
            startTask.setOnFailed(ev -> {
                logger.error("Failed to start chatbot conversation", startTask.getException());
                TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_start_chatbot"));
            });
            AppThreadPool.execute(startTask);
        }
        
        HBox inputBox = new HBox(8);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        
        TLTextField messageField = new TLTextField("", I18n.get("chatbot.placeholder"));
        HBox.setHgrow(messageField, Priority.ALWAYS);
        
        TLButton sendBtn = new TLButton(I18n.get("chatbot.send"));
        sendBtn.setVariant("PRIMARY");
        sendBtn.setOnAction(e -> {
            String userMessage = messageField.getText() != null ? messageField.getText().trim() : "";
            if (!userMessage.isEmpty()) {
                addChatMessage(userMessage, true);
                messageField.setText("");
                handleChatbotResponse(userMessage);
            }
        });
        
        // Escalate to ticket button
        TLButton escalateBtn = new TLButton(I18n.get("chatbot.escalate"));
        escalateBtn.setVariant("SECONDARY");
        escalateBtn.setOnAction(e -> escalateChatToTicket());
        
        inputBox.getChildren().addAll(messageField, sendBtn, escalateBtn);
        chatContainer.getChildren().addAll(messageArea, inputBox);
        contentPane.getChildren().add(chatContainer);
    }

    private void addChatMessage(String message, boolean isUser) {
        if (chatMessages == null) return;
        
        HBox messageBox = new HBox();
        messageBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(400);
        msgLabel.setStyle(isUser ? 
            "-fx-background-color: -fx-primary; -fx-text-fill: -fx-primary-foreground; -fx-padding: 10 14; -fx-background-radius: 12;" :
            "-fx-background-color: -fx-muted; -fx-text-fill: -fx-foreground; -fx-padding: 10 14; -fx-background-radius: 12;"
        );
        
        messageBox.getChildren().add(msgLabel);
        chatMessages.getChildren().add(messageBox);
    }

    /**
     * Creates and shows an animated three-dot typing indicator in the chat area.
     * The dots pulse in sequence to indicate the bot is processing.
     */
    private void showTypingIndicator() {
        if (chatMessages == null) return;

        HBox container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        dots.setStyle("-fx-background-color: -fx-muted; -fx-padding: 10 18; -fx-background-radius: 12;");

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.setFill(Color.web("#888888"));
            dot.setOpacity(0.4);

            // Staggered pulse animation per dot
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

        container.getChildren().add(dots);
        typingIndicator = container;
        chatMessages.getChildren().add(typingIndicator);
    }

    /**
     * Removes the typing indicator from the chat area if present.
     */
    private void hideTypingIndicator() {
        if (typingIndicator != null && chatMessages != null) {
            chatMessages.getChildren().remove(typingIndicator);
            typingIndicator = null;
        }
    }

    private void handleChatbotResponse(String userMessage) {
        showTypingIndicator();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Persist user message
                if (currentConversationId > 0 && currentUser != null) {
                    ChatbotMessage userMsg = new ChatbotMessage(currentConversationId, "USER", userMessage);
                    chatbotService.addMessage(userMsg);
                }
                
                String response = chatbotService.getAutoResponse(userMessage);
                
                // Persist bot response
                if (currentConversationId > 0) {
                    String botReply = response != null ? response : I18n.get("chatbot.no_match");
                    ChatbotMessage botMsg = new ChatbotMessage(currentConversationId, "BOT", botReply);
                    chatbotService.addMessage(botMsg);
                }
                
                return response != null ? response : I18n.get("chatbot.no_match");
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                hideTypingIndicator();
                addChatMessage(task.getValue(), false);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                hideTypingIndicator();
                addChatMessage(I18n.get("chatbot.no_match"), false);
            });
        });

        AppThreadPool.execute(task);
    }

    private void escalateChatToTicket() {
        if (currentUser == null) return;
        
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("chatbot.escalate.title"));
        dialog.setDescription(I18n.get("chatbot.escalate.description"));
        
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setPrefWidth(480);
        
        TLTextField subjectField = new TLTextField(I18n.get("ticket.subject"), "");
        TLTextarea descArea = new TLTextarea(I18n.get("ticket.description"));
        descArea.getControl().setPrefRowCount(6);
        descArea.getControl().setPrefHeight(140);
        
        form.getChildren().addAll(subjectField, descArea);
        dialog.setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String subject = subjectField.getText() != null ? subjectField.getText().trim() : "";
                String desc = descArea.getText() != null ? descArea.getText().trim() : "";
                if (subject.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"), 
                        I18n.get("error.validation.required", I18n.get("ticket.subject")));
                    return;
                }
                
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
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
        contentPane.getChildren().clear();
        
        VBox feedbackContainer = new VBox(16);
        feedbackContainer.setPadding(new Insets(8, 0, 0, 0));
        
        Label feedbackTitle = new Label(I18n.get("feedback.title"));
        feedbackTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TLSelect<String> typeSelect = new TLSelect<>(I18n.get("feedback.type"));
        typeSelect.getItems().addAll(
            I18n.get("feedback.type.bug"),
            I18n.get("feedback.type.feature"),
            I18n.get("feedback.type.general"),
            I18n.get("feedback.type.complaint"),
            I18n.get("feedback.type.praise")
        );
        typeSelect.setValue(I18n.get("feedback.type.general"));
        
        // Rating selector (1-5 stars)
        HBox ratingBox = new HBox(8);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        Label ratingLabel = new Label(I18n.get("feedback.rating") + ":");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setValue(5);
        ratingBox.getChildren().addAll(ratingLabel, ratingCombo);
        
        TLTextarea commentArea = new TLTextarea(I18n.get("feedback.comment"), "");
        commentArea.getControl().setPrefRowCount(6);
        
        TLButton submitBtn = new TLButton(I18n.get("feedback.submit"));
        submitBtn.setVariant("PRIMARY");
        submitBtn.setOnAction(e -> {
            submitFeedback(getFeedbackTypeKey(typeSelect.getValue()), 
                          ratingCombo.getValue(), 
                          commentArea.getText());
        });
        
        feedbackContainer.getChildren().addAll(feedbackTitle, typeSelect, ratingBox, commentArea, submitBtn);
        contentPane.getChildren().add(feedbackContainer);
    }

    private void submitFeedback(String type, int rating, String comment) {
        if (currentUser == null) return;
        
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                UserFeedback feedback = new UserFeedback();
                feedback.setUserId(currentUser.getId());
                feedback.setFeedbackType(type);
                feedback.setRating(rating);
                feedback.setComment(comment);
                
                return feedbackService.submit(feedback);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                TLToast.success(contentPane.getScene(), I18n.get("message.success"), I18n.get("feedback.submitted"));
                loadFeedbackTab();
            });
        });

        AppThreadPool.execute(task);
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
        label.getStyleClass().add("text-muted");
        
        emptyState.getChildren().add(label);
        return emptyState;
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
            case "HIGH" -> TLBadge.Variant.DESTRUCTIVE;
            case "URGENT" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
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
}
