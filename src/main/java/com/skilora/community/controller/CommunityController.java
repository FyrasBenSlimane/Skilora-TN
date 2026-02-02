package com.skilora.community.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.community.entity.*;
import com.skilora.community.service.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
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
        task.setOnFailed(e -> logger.error("Failed to create post", task.getException()));
        new Thread(task).start();
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
        new Thread(task).start();
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
        new Thread(task).start();
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
        new Thread(task).start();
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
                new Thread(task).start();
            }
        });
    }

    private void loadMessagesTab() {
        contentPane.getChildren().clear();
        Label placeholder = new Label(I18n.get("message.title"));
        contentPane.getChildren().add(placeholder);
    }

    private void loadEventsTab() {
        contentPane.getChildren().clear();
        
        Task<List<Event>> task = new Task<>() {
            @Override
            protected List<Event> call() {
                return EventService.getInstance().findUpcoming();
            }
        };

        task.setOnSucceeded(e -> {
            List<Event> events = task.getValue();
            for (Event event : events) {
                contentPane.getChildren().add(createEventCard(event));
            }
        });
        new Thread(task).start();
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
        
        content.getChildren().addAll(title, date, attendees);
        card.setContent(content);
        return card;
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
            for (CommunityGroup group : groups) {
                contentPane.getChildren().add(createGroupCard(group));
            }
        });
        new Thread(task).start();
    }

    private TLCard createGroupCard(CommunityGroup group) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        
        Label name = new Label(group.getName());
        name.getStyleClass().add("h4");
        
        Label members = new Label(I18n.get("group.members", group.getMemberCount()));
        members.getStyleClass().add("text-muted");
        
        content.getChildren().addAll(name, members);
        card.setContent(content);
        return card;
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
