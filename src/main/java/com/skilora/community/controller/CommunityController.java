package com.skilora.community.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.community.entity.*;
import com.skilora.community.enums.*;
import com.skilora.community.service.*;
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
import com.skilora.utils.AppThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CommunityController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TLButton newPostBtn;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;

    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTabs();
    }

    public void initializeContext(User user) {
        this.currentUser = user;
        titleLabel.setText(I18n.get("community.title"));
        subtitleLabel.setText(I18n.get("community.subtitle"));
        loadFeedTab();
    }

    private void setupTabs() {
        TLTabs tabs = new TLTabs();
        tabs.addTab("feed", I18n.get("community.tab.feed"), (javafx.scene.Node) null);
        tabs.addTab("connections", I18n.get("community.tab.connections"), (javafx.scene.Node) null);
        tabs.addTab("messages", I18n.get("community.tab.messages"), (javafx.scene.Node) null);
        tabs.addTab("events", I18n.get("community.tab.events"), (javafx.scene.Node) null);
        tabs.addTab("groups", I18n.get("community.tab.groups"), (javafx.scene.Node) null);
        tabs.setOnTabChanged(this::onTabChanged);
        tabBox.getChildren().add(tabs);
        HBox.setHgrow(tabs, Priority.ALWAYS);
    }

    private void onTabChanged(String tabId) {
        switch (tabId) {
            case "feed" -> loadFeedTab();
            case "connections" -> loadConnectionsTab();
            case "messages" -> loadMessagesTab();
            case "events" -> loadEventsTab();
            case "groups" -> loadGroupsTab();
        }
    }

    @FXML
    private void handleNewPost() {
        if (currentUser == null) return;
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("post.new"));
        dialog.setDescription(I18n.get("post.new.description"));
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        
        TLTextarea textArea = new TLTextarea("", I18n.get("post.placeholder"));
        textArea.getControl().setPrefRowCount(5);
        
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        TLButton cancelBtn = new TLButton();
        cancelBtn.setText(I18n.get("common.cancel"));
        cancelBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());
        
        TLButton postBtn = new TLButton();
        postBtn.setText(I18n.get("post.publish"));
        postBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        postBtn.setOnAction(e -> {
            String text = textArea.getText();
            if (text == null || text.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"),
                    I18n.get("error.validation.required", I18n.get("post.content")));
                return;
            }
            createPost(text.trim());
            dialog.close();
        });
        
        buttons.getChildren().addAll(cancelBtn, postBtn);
        content.getChildren().addAll(textArea, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void createPost(String content) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                Post post = new Post();
                post.setAuthorId(currentUser.getId());
                post.setContent(content);
                return PostService.getInstance().create(post);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue() > 0) {
                logger.info("Post created successfully");
                loadFeedTab();
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to create post", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_create_post"));
        });
        AppThreadPool.execute(task);
    }

    private void loadFeedTab() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;
        
        Task<List<Post>> task = new Task<>() {
            @Override
            protected List<Post> call() {
                return PostService.getInstance().getFeed(currentUser.getId(), 1, 20);
            }
        };

        task.setOnSucceeded(e -> {
            List<Post> posts = task.getValue();
            if (posts.isEmpty()) {
                Label empty = new Label(I18n.get("post.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                for (Post post : posts) {
                    contentPane.getChildren().add(createPostCard(post));
                }
            }
        });
        AppThreadPool.execute(task);
    }

    private TLCard createPostCard(Post post) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        // Author
        HBox header = new HBox(8);
        Label author = new Label(post.getAuthorName());
        author.getStyleClass().add("text-bold");
        Label date = new Label(" • " + formatDate(post.getCreatedDate()));
        date.getStyleClass().add("text-muted");
        header.getChildren().addAll(author, date);
        
        // Content
        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        
        // Actions
        HBox actions = new HBox(16);
        TLButton likeBtn = new TLButton();
        likeBtn.setText(I18n.get("post.like") + " (" + post.getLikesCount() + ")");
        likeBtn.setVariant(TLButton.ButtonVariant.GHOST);
        likeBtn.setOnAction(e -> toggleLike(post));
        
        TLButton commentBtn = new TLButton();
        commentBtn.setText(I18n.get("post.comment") + " (" + post.getCommentsCount() + ")");
        commentBtn.setVariant(TLButton.ButtonVariant.GHOST);
        commentBtn.setOnAction(e -> showComments(post));
        
        actions.getChildren().addAll(likeBtn, commentBtn);
        
        content.getChildren().addAll(header, contentLabel, actions);
        card.setContent(content);
        return card;
    }

    private void toggleLike(Post post) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return PostService.getInstance().toggleLike(post.getId(), currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> loadFeedTab());
        AppThreadPool.execute(task);
    }

    private void loadConnectionsTab() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;
        
        Task<List<Connection>> task = new Task<>() {
            @Override
            protected List<Connection> call() {
                return ConnectionService.getInstance().getConnections(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Connection> connections = task.getValue();
            Label count = new Label(I18n.get("connection.count", connections.size()));
            count.getStyleClass().add("text-muted");
            contentPane.getChildren().add(count);
            
            for (Connection conn : connections) {
                contentPane.getChildren().add(createConnectionCard(conn));
            }
        });
        AppThreadPool.execute(task);
    }

    private TLCard createConnectionCard(Connection conn) {
        TLCard card = new TLCard();
        HBox content = new HBox(12);
        content.setPadding(new Insets(12));
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label name = new Label(conn.getOtherUserName());
        name.getStyleClass().add("text-bold");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        TLButton removeBtn = new TLButton();
        removeBtn.setText(I18n.get("connection.remove"));
        removeBtn.setVariant(TLButton.ButtonVariant.DANGER);
        removeBtn.setOnAction(e -> removeConnection(conn.getId()));
        
        content.getChildren().addAll(name, spacer, removeBtn);
        card.setContent(content);
        return card;
    }

    private void removeConnection(int connectionId) {
        DialogUtils.showConfirmation(
            I18n.get("community.remove.confirm.title"),
            I18n.get("community.remove.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ConnectionService.getInstance().removeConnection(connectionId);
                    }
                };
                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        logger.info("Connection removed successfully");
                        loadConnectionsTab();
                    }
                });
                AppThreadPool.execute(task);
            }
        });
    }

    private void loadMessagesTab() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;

        Task<List<Conversation>> task = new Task<>() {
            @Override
            protected List<Conversation> call() {
                return MessagingService.getInstance().getConversations(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Conversation> conversations = task.getValue();
            if (conversations.isEmpty()) {
                Label empty = new Label(I18n.get("message.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                for (Conversation conv : conversations) {
                    contentPane.getChildren().add(createConversationCard(conv));
                }
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load conversations", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_conversations"));
        });
        AppThreadPool.execute(task);
    }

    private TLCard createConversationCard(Conversation conv) {
        TLCard card = new TLCard();
        HBox content = new HBox(12);
        content.setPadding(new Insets(12));
        content.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(4);
        Label name = new Label(conv.getOtherUserName());
        name.getStyleClass().add("text-bold");

        Label preview = new Label(conv.getLastMessagePreview() != null ? conv.getLastMessagePreview() : "");
        preview.getStyleClass().add("text-muted");
        preview.setMaxWidth(300);
        textBox.getChildren().addAll(name, preview);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        if (conv.getUnreadCount() > 0) {
            TLBadge unread = new TLBadge(String.valueOf(conv.getUnreadCount()), TLBadge.Variant.DESTRUCTIVE);
            rightBox.getChildren().add(unread);
        }

        content.getChildren().addAll(textBox, spacer, rightBox);
        card.setContent(content);

        // Click to open conversation
        card.getStyleClass().add("card-interactive");
        card.setOnMouseClicked(e -> openConversation(conv));
        return card;
    }

    private void openConversation(Conversation conv) {
        contentPane.getChildren().clear();

        VBox chatView = new VBox(8);
        chatView.setPadding(new Insets(8));

        // Back button
        TLButton backBtn = new TLButton("\u2190 " + I18n.get("common.back"));
        backBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        backBtn.setOnAction(e -> loadMessagesTab());

        Label headerLabel = new Label(conv.getOtherUserName());
        headerLabel.getStyleClass().add("h4");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(8));
        scrollPane.setContent(messagesBox);

        // Load messages
        Task<List<Message>> msgTask = new Task<>() {
            @Override
            protected List<Message> call() {
                MessagingService.getInstance().markAsRead(conv.getId(), currentUser.getId());
                return MessagingService.getInstance().getMessages(conv.getId(), 1, 50);
            }
        };
        msgTask.setOnSucceeded(e -> {
            for (Message msg : msgTask.getValue()) {
                boolean isMe = msg.getSenderId() == currentUser.getId();
                HBox row = new HBox();
                row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Label msgLabel = new Label(msg.getContent());
                msgLabel.setWrapText(true);
                msgLabel.setMaxWidth(350);
                msgLabel.setStyle(isMe
                        ? "-fx-background-color: -fx-primary; -fx-text-fill: -fx-primary-foreground; -fx-padding: 8 12; -fx-background-radius: 12;"
                        : "-fx-background-color: -fx-muted; -fx-text-fill: -fx-foreground; -fx-padding: 8 12; -fx-background-radius: 12;");
                row.getChildren().add(msgLabel);
                messagesBox.getChildren().add(row);
            }
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
        AppThreadPool.execute(msgTask);

        // Send box
        HBox sendBox = new HBox(8);
        sendBox.setAlignment(Pos.CENTER_LEFT);
        TLTextField msgField = new TLTextField("", I18n.get("message.placeholder"));
        HBox.setHgrow(msgField, Priority.ALWAYS);
        TLButton sendBtn = new TLButton(I18n.get("message.send"));
        sendBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        sendBtn.setOnAction(e -> {
            String text = msgField.getText();
            if (text != null && !text.trim().isEmpty()) {
                String finalText = text.trim();
                msgField.setText("");
                Task<Integer> sendTask = new Task<>() {
                    @Override
                    protected Integer call() {
                        return MessagingService.getInstance().sendMessage(conv.getId(), currentUser.getId(), finalText);
                    }
                };
                sendTask.setOnSucceeded(ev -> {
                    HBox row = new HBox();
                    row.setAlignment(Pos.CENTER_RIGHT);
                    Label msgLabel = new Label(finalText);
                    msgLabel.setWrapText(true);
                    msgLabel.setMaxWidth(350);
                    msgLabel.setStyle("-fx-background-color: -fx-primary; -fx-text-fill: -fx-primary-foreground; -fx-padding: 8 12; -fx-background-radius: 12;");
                    row.getChildren().add(msgLabel);
                    messagesBox.getChildren().add(row);
                    Platform.runLater(() -> scrollPane.setVvalue(1.0));
                });
                AppThreadPool.execute(sendTask);
            }
        });
        sendBox.getChildren().addAll(msgField, sendBtn);

        chatView.getChildren().addAll(backBtn, headerLabel, scrollPane, sendBox);
        contentPane.getChildren().add(chatView);
    }

    private void loadEventsTab() {
        contentPane.getChildren().clear();

        // Create event button at top
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 0, 8, 0));
        TLButton createEventBtn = new TLButton(I18n.get("event.create"));
        createEventBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        createEventBtn.setOnAction(e -> showCreateEventDialog());
        topBar.getChildren().add(createEventBtn);
        contentPane.getChildren().add(topBar);

        Task<List<Event>> task = new Task<>() {
            @Override
            protected List<Event> call() {
                return EventService.getInstance().findUpcoming();
            }
        };

        task.setOnSucceeded(e -> {
            List<Event> events = task.getValue();
            if (events.isEmpty()) {
                Label empty = new Label(I18n.get("event.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                for (Event event : events) {
                    contentPane.getChildren().add(createEventCard(event));
                }
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load events", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_events"));
        });
        AppThreadPool.execute(task);
    }

    private void showCreateEventDialog() {
        if (currentUser == null) return;
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("event.create"));
        dialog.setDescription(I18n.get("event.create.description"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField titleField = new TLTextField("", I18n.get("event.title.placeholder"));
        TLTextarea descArea = new TLTextarea("", I18n.get("event.description.placeholder"));
        descArea.getControl().setPrefRowCount(3);
        TLTextField locationField = new TLTextField("", I18n.get("event.location.placeholder"));

        TLSelect<String> typeSelect = new TLSelect<>(I18n.get("event.type"));
        for (EventType et : EventType.values()) {
            typeSelect.getItems().add(et.name().charAt(0) + et.name().substring(1).toLowerCase());
        }
        typeSelect.setValue(EventType.MEETUP.name().charAt(0) + EventType.MEETUP.name().substring(1).toLowerCase());

        TLTextField maxAttendeesField = new TLTextField("", I18n.get("event.max_attendees"));

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"));
        cancelBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton submitBtn = new TLButton(I18n.get("common.submit"));
        submitBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        submitBtn.setOnAction(e -> {
            String title = titleField.getText();
            if (title == null || title.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"), I18n.get("error.validation.required", I18n.get("event.title")));
                return;
            }
            Event event = new Event();
            event.setOrganizerId(currentUser.getId());
            event.setTitle(title.trim());
            event.setDescription(descArea.getText() != null ? descArea.getText().trim() : "");
            event.setLocation(locationField.getText() != null ? locationField.getText().trim() : "");
            event.setEventType(EventType.valueOf(typeSelect.getValue().toUpperCase()));
            event.setStartDate(LocalDateTime.now().plusDays(7));
            event.setStatus(EventStatus.UPCOMING);
            try {
                event.setMaxAttendees(Integer.parseInt(maxAttendeesField.getText()));
            } catch (NumberFormatException ex) {
                event.setMaxAttendees(50);
            }
            Task<Integer> createTask = new Task<>() {
                @Override
                protected Integer call() {
                    return EventService.getInstance().create(event);
                }
            };
            createTask.setOnSucceeded(ev -> {
                if (createTask.getValue() > 0) {
                    TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("event.success.created"));
                    loadEventsTab();
                }
            });
            createTask.setOnFailed(ev -> {
                logger.error("Failed to create event", createTask.getException());
                TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_create_event"));
            });
            AppThreadPool.execute(createTask);
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, submitBtn);
        content.getChildren().addAll(titleField, descArea, typeSelect, locationField, maxAttendeesField, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private TLCard createEventCard(Event event) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        
        Label title = new Label(event.getTitle());
        title.getStyleClass().add("h4");
        
        Label date = new Label(formatDate(event.getStartDate()));
        date.getStyleClass().add("text-muted");
        
        Label attendees = new Label(I18n.get("event.attendees", event.getCurrentAttendees()));

        // RSVP / Cancel RSVP button
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        if (currentUser != null) {
            TLButton rsvpBtn = new TLButton();
            // Check attendance in background
            Task<Boolean> checkTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return EventService.getInstance().isAttending(event.getId(), currentUser.getId());
                }
            };
            checkTask.setOnSucceeded(e -> {
                boolean attending = checkTask.getValue();
                if (attending) {
                    rsvpBtn.setText(I18n.get("event.rsvp.cancel"));
                    rsvpBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
                    rsvpBtn.setOnAction(ev -> cancelRsvp(event.getId()));
                } else {
                    rsvpBtn.setText(I18n.get("event.rsvp"));
                    rsvpBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
                    rsvpBtn.setOnAction(ev -> rsvpEvent(event.getId()));
                }
            });
            AppThreadPool.execute(checkTask);

            actions.getChildren().add(rsvpBtn);
        }

        content.getChildren().addAll(title, date, attendees, actions);
        card.setContent(content);
        return card;
    }

    private void rsvpEvent(int eventId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return EventService.getInstance().rsvp(eventId, currentUser.getId(), "GOING");
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) loadEventsTab();
        });
        AppThreadPool.execute(task);
    }

    private void cancelRsvp(int eventId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return EventService.getInstance().cancelRsvp(eventId, currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) loadEventsTab();
        });
        AppThreadPool.execute(task);
    }

    private void loadGroupsTab() {
        contentPane.getChildren().clear();

        Task<List<CommunityGroup>> task = new Task<>() {
            @Override
            protected List<CommunityGroup> call() {
                return GroupService.getInstance().findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<CommunityGroup> groups = task.getValue();
            if (groups.isEmpty()) {
                Label empty = new Label(I18n.get("group.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                for (CommunityGroup group : groups) {
                    contentPane.getChildren().add(createGroupCard(group));
                }
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load groups", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_groups"));
        });
        AppThreadPool.execute(task);
    }

    private TLCard createGroupCard(CommunityGroup group) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        
        Label name = new Label(group.getName());
        name.getStyleClass().add("h4");
        
        Label members = new Label(I18n.get("group.members", group.getMemberCount()));
        members.getStyleClass().add("text-muted");

        // Join / Leave button
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        if (currentUser != null) {
            TLButton actionBtn = new TLButton();
            Task<Boolean> checkTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return GroupService.getInstance().isMember(group.getId(), currentUser.getId());
                }
            };
            checkTask.setOnSucceeded(e -> {
                boolean isMember = checkTask.getValue();
                if (isMember) {
                    actionBtn.setText(I18n.get("group.leave"));
                    actionBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
                    actionBtn.setOnAction(ev -> leaveGroup(group.getId()));
                } else {
                    actionBtn.setText(I18n.get("group.join"));
                    actionBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
                    actionBtn.setOnAction(ev -> joinGroup(group.getId()));
                }
            });
            AppThreadPool.execute(checkTask);

            actions.getChildren().add(actionBtn);
        }

        content.getChildren().addAll(name, members, actions);
        card.setContent(content);
        return card;
    }

    private void joinGroup(int groupId) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return GroupService.getInstance().join(groupId, currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) loadGroupsTab();
        });
        AppThreadPool.execute(task);
    }

    private void leaveGroup(int groupId) {
        DialogUtils.showConfirmation(
            I18n.get("group.leave.confirm.title"),
            I18n.get("group.leave.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return GroupService.getInstance().leave(groupId, currentUser.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    if (task.getValue()) loadGroupsTab();
                });
                AppThreadPool.execute(task);
            }
        });
    }

    private void showComments(Post post) {
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("post.comments.title"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(450);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox commentsBox = new VBox(8);
        commentsBox.setPadding(new Insets(8));
        scrollPane.setContent(commentsBox);

        // Load comments
        Task<List<PostComment>> loadTask = new Task<>() {
            @Override
            protected List<PostComment> call() {
                return PostService.getInstance().getComments(post.getId());
            }
        };
        loadTask.setOnSucceeded(e -> {
            List<PostComment> comments = loadTask.getValue();
            if (comments.isEmpty()) {
                Label empty = new Label(I18n.get("post.no_comments"));
                empty.getStyleClass().add("text-muted");
                commentsBox.getChildren().add(empty);
            } else {
                for (PostComment comment : comments) {
                    VBox commentCard = new VBox(4);
                    commentCard.setPadding(new Insets(8));
                    commentCard.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8;");

                    Label author = new Label(comment.getAuthorName());
                    author.getStyleClass().add("text-bold");
                    Label text = new Label(comment.getContent());
                    text.setWrapText(true);
                    Label time = new Label(formatDate(comment.getCreatedDate()));
                    time.getStyleClass().add("text-muted");

                    commentCard.getChildren().addAll(author, text, time);
                    commentsBox.getChildren().add(commentCard);
                }
            }
        });
        AppThreadPool.execute(loadTask);

        // Add comment input
        HBox addBox = new HBox(8);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TLTextField commentField = new TLTextField("", I18n.get("post.comment.placeholder"));
        HBox.setHgrow(commentField, Priority.ALWAYS);
        TLButton addBtn = new TLButton(I18n.get("message.send"));
        addBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        addBtn.setOnAction(e -> {
            String text = commentField.getText();
            if (text != null && !text.trim().isEmpty()) {
                String finalText = text.trim();
                commentField.setText("");
                PostComment comment = new PostComment();
                comment.setPostId(post.getId());
                comment.setAuthorId(currentUser.getId());
                comment.setContent(finalText);

                Task<Integer> addTask = new Task<>() {
                    @Override
                    protected Integer call() {
                        return PostService.getInstance().addComment(comment);
                    }
                };
                addTask.setOnSucceeded(ev -> {
                    // Refresh comments
                    commentsBox.getChildren().clear();
                    Task<List<PostComment>> refreshTask = new Task<>() {
                        @Override
                        protected List<PostComment> call() {
                            return PostService.getInstance().getComments(post.getId());
                        }
                    };
                    refreshTask.setOnSucceeded(ev2 -> {
                        for (PostComment c : refreshTask.getValue()) {
                            VBox commentCard = new VBox(4);
                            commentCard.setPadding(new Insets(8));
                            commentCard.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8;");
                            Label author = new Label(c.getAuthorName());
                            author.getStyleClass().add("text-bold");
                            Label txt = new Label(c.getContent());
                            txt.setWrapText(true);
                            Label time = new Label(formatDate(c.getCreatedDate()));
                            time.getStyleClass().add("text-muted");
                            commentCard.getChildren().addAll(author, txt, time);
                            commentsBox.getChildren().add(commentCard);
                        }
                    });
                    AppThreadPool.execute(refreshTask);
                });
                AppThreadPool.execute(addTask);
            }
        });
        addBox.getChildren().addAll(commentField, addBtn);

        content.getChildren().addAll(scrollPane, addBox);
        dialog.setContent(content);
        dialog.show();
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
