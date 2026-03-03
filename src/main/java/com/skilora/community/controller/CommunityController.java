package com.skilora.community.controller;

import com.skilora.framework.components.*;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.user.entity.*;
import com.skilora.community.entity.*;
import com.skilora.community.enums.*;
import com.skilora.community.service.*;
import com.skilora.user.enums.Role;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import com.skilora.utils.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Animation;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Popup;
import javafx.util.Duration;
import com.skilora.utils.AppThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
public class CommunityController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TLButton newPostBtn;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;
    @FXML private ScrollPane mainScrollPane;

    private User currentUser;
    private int currentPage = 1;
    private boolean isLoading = false;
    private String activeTab = "feed";
    private TLTabs communityTabs;
    private CommunityNotificationService communityNotificationService;
    private Timeline messagePollingTimeline;
    private Timeline typingPollTimeline;
    private Timeline conversationDotAnimation;
    private Timeline conversationRecordTimerTimeline;
    private int openConversationId = -1;

    /** Feed tab: current posts (for client-side sort/filter), container and controls refs */
    private List<Post> currentFeedPosts = new ArrayList<>();
    private VBox feedPostsContainer;
    private TLSelect<String> feedSortSelect;
    private TLTextField feedSearchField;

    // New feature services
    private final TranslationService translationService = TranslationService.getInstance();
    private final CloudinaryUploadService cloudinaryService = CloudinaryUploadService.getInstance();
    private final MentionService mentionService = MentionService.getInstance();
    private final SearchService searchService = SearchService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        newPostBtn.setText(I18n.get("post.new"));
        setupTabs();
        
        if (mainScrollPane != null) {
            mainScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.9 && !isLoading) {
                    loadNextPage();
                }
            });
        }
    }

    public void initializeContext(User user) {
        this.currentUser = user;
        titleLabel.setText(I18n.get("community.title"));
        subtitleLabel.setText(I18n.get("community.subtitle"));
        // Start real-time notifications (poller for messages/invitations)
        startRealTimeNotifications();
        // Heartbeat for online presence
        OnlineStatusService.getInstance().startHeartbeat(currentUser.getId());
        loadFeedTab();
    }

    private void setupTabs() {
        communityTabs = new TLTabs();
        communityTabs.addTab("feed", I18n.get("community.tab.feed"), (javafx.scene.Node) null);
        communityTabs.addTab("connections", I18n.get("community.tab.connections"), (javafx.scene.Node) null);
        communityTabs.addTab("messages", I18n.get("community.tab.messages"), (javafx.scene.Node) null);
        communityTabs.addTab("events", I18n.get("community.tab.events"), (javafx.scene.Node) null);
        communityTabs.addTab("groups", I18n.get("community.tab.groups"), (javafx.scene.Node) null);
        communityTabs.addTab("blog", I18n.get("community.tab.blog"), (javafx.scene.Node) null);
        communityTabs.setOnTabChanged(this::onTabChanged);
        tabBox.getChildren().add(communityTabs);
        HBox.setHgrow(communityTabs, Priority.ALWAYS);
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    private boolean canEditOrDelete(int authorId) {
        if (currentUser == null) return false;
        return isAdmin() || currentUser.getId() == authorId;
    }

    private void onTabChanged(String tabId) {
        activeTab = tabId;
        if (!"messages".equals(tabId)) {
            if (messagePollingTimeline != null) messagePollingTimeline.stop();
            if (typingPollTimeline != null) typingPollTimeline.stop();
        }
        configureHeaderForTab(tabId);
        switch (tabId) {
            case "feed" -> loadFeedTab();
            case "connections" -> loadConnectionsTab();
            case "messages" -> loadMessagesTab();
            case "events" -> loadEventsTab();
            case "groups" -> loadGroupsTab();
            case "blog" -> loadBlogTab();
        }
    }

    private void configureHeaderForTab(String tabId) {
        if (newPostBtn == null) return;
        if ("blog".equals(tabId)) {
            newPostBtn.setText(I18n.get("blog.new"));
            newPostBtn.setOnAction(e -> handleNewArticle());
            return;
        }
        newPostBtn.setText(I18n.get("post.new"));
        newPostBtn.setOnAction(e -> handleNewPost());
    }

    @FXML
    private void handleNewPost() {
        if (currentUser == null) return;
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("post.new"));
        dialog.setDescription(I18n.get("post.new.description"));
        
        VBox content = new VBox(12);
        
        TLTextarea textArea = new TLTextarea("", I18n.get("post.placeholder"));
        textArea.getControl().setPrefRowCount(5);
        
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Label imageLabel = new Label();
        imageLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
        
        final String[] selectedImageUrl = {null};
        
        TLButton attachImageBtn = new TLButton();
        attachImageBtn.setText("Attach Image");
        attachImageBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        attachImageBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
            if (selectedFile != null) {
                // Validate file size (max 5 MB for images)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    TLToast.error(contentPane.getScene(), "Error", "Image must be under 5 MB");
                    return;
                }
                try {
                    String home = System.getProperty("user.home");
                    java.io.File uploadsDir = new java.io.File(home, ".skilora/uploads");
                    if (!uploadsDir.exists()) {
                        uploadsDir.mkdirs();
                    }
                    // Sanitize filename to prevent path traversal
                    String safeName = selectedFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
                    String fileName = System.currentTimeMillis() + "_" + safeName;
                    java.io.File destFile = new java.io.File(uploadsDir, fileName);
                    java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    selectedImageUrl[0] = destFile.toURI().toString();
                    imageLabel.setText(selectedFile.getName());
                } catch (java.io.IOException ex) {
                    logger.error("Failed to copy image", ex);
                    TLToast.error(contentPane.getScene(), "Error", "Failed to attach image");
                }
            }
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
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
            createPost(text.trim(), selectedImageUrl[0]);
            dialog.close();
        });
        
        buttons.getChildren().addAll(attachImageBtn, imageLabel, spacer, cancelBtn, postBtn);
        content.getChildren().addAll(textArea, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void showPostDialog(Post existingPost) {
        if (currentUser == null || existingPost == null) return;
        TLDialog<Void> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("post.edit"));
        dialog.setDescription(I18n.get("post.new.description"));

        VBox content = new VBox(12);
        TLTextarea textArea = new TLTextarea("", I18n.get("post.placeholder"));
        textArea.getControl().setPrefRowCount(5);
        textArea.setText(existingPost.getContent() != null ? existingPost.getContent() : "");

        Label imageLabel = new Label();
        imageLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
        final String[] selectedImageUrl = {existingPost.getImageUrl()};
        if (existingPost.getImageUrl() != null && !existingPost.getImageUrl().isBlank()) {
            imageLabel.setText("Image attachée");
        }

        TLButton attachImageBtn = new TLButton();
        attachImageBtn.setText("Attach Image");
        attachImageBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        attachImageBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
            if (selectedFile != null) {
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    TLToast.error(contentPane.getScene(), "Error", "Image must be under 5 MB");
                    return;
                }
                try {
                    String home = System.getProperty("user.home");
                    java.io.File uploadsDir = new java.io.File(home, ".skilora/uploads");
                    if (!uploadsDir.exists()) uploadsDir.mkdirs();
                    String safeName = selectedFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
                    String fileName = System.currentTimeMillis() + "_" + safeName;
                    java.io.File destFile = new java.io.File(uploadsDir, fileName);
                    java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    selectedImageUrl[0] = destFile.toURI().toString();
                    imageLabel.setText(selectedFile.getName());
                } catch (java.io.IOException ex) {
                    logger.error("Failed to copy image", ex);
                    TLToast.error(contentPane.getScene(), "Error", "Failed to attach image");
                }
            }
        });

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        TLButton cancelBtn = new TLButton();
        cancelBtn.setText(I18n.get("common.cancel"));
        cancelBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());
        TLButton saveBtn = new TLButton();
        saveBtn.setText(I18n.get("common.save"));
        saveBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        saveBtn.setOnAction(e -> {
            String text = textArea.getText();
            if (text == null || text.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"), I18n.get("error.validation.required", I18n.get("post.content")));
                return;
            }
            existingPost.setContent(text.trim());
            existingPost.setImageUrl(selectedImageUrl[0]);
            updatePost(existingPost);
            dialog.close();
        });
        buttons.getChildren().addAll(attachImageBtn, imageLabel, spacer, cancelBtn, saveBtn);
        content.getChildren().addAll(textArea, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void createPost(String content, String imageUrl) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                Post post = new Post();
                post.setAuthorId(currentUser.getId());
                post.setContent(content);
                post.setImageUrl(imageUrl);
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

        // Search bar
        feedSearchField = new TLTextField("", "Rechercher");
        HBox.setHgrow(feedSearchField, Priority.ALWAYS);
        feedSearchField.getControl().setOnAction(e -> applyFeedFilterAndSort());
        feedSearchField.getControl().textProperty().addListener((o, oldVal, newVal) -> applyFeedFilterAndSort());

        // Sort bar
        HBox sortBar = new HBox(10);
        sortBar.setAlignment(Pos.CENTER_LEFT);
        sortBar.setPadding(new Insets(0, 0, 8, 0));
        Label sortLabel = new Label("Trier par :");
        sortLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");
        feedSortSelect = new TLSelect<>("", "Plus récent", "Plus ancien");
        feedSortSelect.setValue("Plus récent");
        feedSortSelect.valueProperty().addListener((obs, oldVal, newVal) -> applyFeedFilterAndSort());
        sortBar.getChildren().addAll(sortLabel, feedSortSelect);

        // Role badge
        String roleDisplay = currentUser.getRole() != null ? currentUser.getRole().getDisplayName() : "";
        Label roleBadge = new Label(roleDisplay);
        roleBadge.getStyleClass().add("badge");
        roleBadge.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-radius: 4; -fx-background-color: -fx-primary; -fx-text-fill: white;");

        contentPane.getChildren().add(feedSearchField);
        sortBar.getChildren().add(roleBadge);
        contentPane.getChildren().add(sortBar);

        feedPostsContainer = new VBox(12);
        feedPostsContainer.setPadding(new Insets(8, 0, 0, 0));
        contentPane.getChildren().add(feedPostsContainer);

        contentPane.getChildren().add(new TLLoadingState());
        currentPage = 1;
        currentFeedPosts.clear();
        isLoading = true;

        Task<List<Post>> task = new Task<>() {
            @Override
            protected List<Post> call() {
                return PostService.getInstance().getFeed(currentUser.getId(), currentPage, 20);
            }
        };

        task.setOnSucceeded(e -> {
            contentPane.getChildren().removeIf(n -> n instanceof TLLoadingState);
            List<Post> posts = task.getValue();
            currentFeedPosts = new ArrayList<>(posts != null ? posts : List.of());
            displaySortedPosts(feedPostsContainer, currentFeedPosts, feedSortSelect.getValue(), feedSearchField.getText());
            isLoading = false;
        });
        task.setOnFailed(e -> {
            contentPane.getChildren().removeIf(n -> n instanceof TLLoadingState);
            isLoading = false;
            logger.error("Failed to load feed", task.getException());
        });
        AppThreadPool.execute(task);
    }

    private void applyFeedFilterAndSort() {
        if (feedPostsContainer != null && feedSortSelect != null && feedSearchField != null) {
            displaySortedPosts(feedPostsContainer, currentFeedPosts, feedSortSelect.getValue(), feedSearchField.getText());
        }
    }

    /**
     * Displays posts in the container with optional keyword filter and sort (Plus récent / Plus ancien).
     */
    private void displaySortedPosts(VBox container, List<Post> posts, String sortVal, String keyword) {
        container.getChildren().clear();
        if (posts == null || posts.isEmpty()) {
            Label empty = new Label(I18n.get("post.empty"));
            empty.getStyleClass().add("text-muted");
            container.getChildren().add(empty);
            return;
        }
        List<Post> filtered = keyword != null && !keyword.isBlank()
                ? posts.stream()
                .filter(p -> (p.getContent() != null && p.getContent().toLowerCase().contains(keyword.toLowerCase()))
                        || (p.getAuthorName() != null && p.getAuthorName().toLowerCase().contains(keyword.toLowerCase())))
                .toList()
                : new ArrayList<>(posts);
        if (filtered.isEmpty()) {
            Label noMatch = new Label("Aucun résultat");
            noMatch.getStyleClass().add("text-muted");
            container.getChildren().add(noMatch);
            return;
        }
        boolean ascending = "Plus ancien".equals(sortVal);
        List<Post> sorted = new ArrayList<>(filtered);
        sorted.sort((a, b) -> {
            LocalDateTime da = a.getCreatedDate();
            LocalDateTime db = b.getCreatedDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return ascending ? da.compareTo(db) : db.compareTo(da);
        });
        int delay = 0;
        for (Post post : sorted) {
            TLCard card = createPostCard(post);
            container.getChildren().add(card);
            animateCardEntry(card, delay);
            delay += 80;
        }
    }

    private void loadNextPage() {
        if (currentUser == null || isLoading || !"feed".equals(activeTab) || feedPostsContainer == null) return;
        isLoading = true;
        currentPage++;

        TLLoadingState loadingState = new TLLoadingState();
        feedPostsContainer.getChildren().add(loadingState);

        Task<List<Post>> task = new Task<>() {
            @Override
            protected List<Post> call() {
                return PostService.getInstance().getFeed(currentUser.getId(), currentPage, 20);
            }
        };

        task.setOnSucceeded(e -> {
            feedPostsContainer.getChildren().removeIf(n -> n instanceof TLLoadingState);
            List<Post> posts = task.getValue();
            if (posts != null && !posts.isEmpty()) {
                currentFeedPosts.addAll(posts);
                displaySortedPosts(feedPostsContainer, currentFeedPosts, feedSortSelect != null ? feedSortSelect.getValue() : "Plus récent", feedSearchField != null ? feedSearchField.getText() : null);
            }
            isLoading = false;
        });
        task.setOnFailed(e -> {
            feedPostsContainer.getChildren().removeIf(n -> n instanceof TLLoadingState);
            isLoading = false;
            logger.error("Failed to load next page", task.getException());
        });
        AppThreadPool.execute(task);
    }

    private TLCard createPostCard(Post post) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        
        VBox content = new VBox(12);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        // Author Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String authorName = post.getAuthorName() != null ? post.getAuthorName() : "Unknown";
        StackPane avatarPane = createAvatar(authorName, 40);

        VBox authorInfo = new VBox(2);
        Label author = new Label(post.getAuthorName());
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label date = new Label(formatDate(post.getCreatedDate()));
        date.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
        authorInfo.getChildren().addAll(author, date);
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        header.getChildren().addAll(avatarPane, authorInfo, headerSpacer);
        
        // Content
        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-line-spacing: 4px;");
        VBox.setVgrow(contentLabel, Priority.NEVER);
        
        content.getChildren().addAll(header, contentLabel);
        
        // Image with rounded clip
        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            try {
                Image image = new Image(post.getImageUrl(), 500, 0, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(500);
                imageView.setSmooth(true);
                Rectangle clip = new Rectangle(500, 350);
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                imageView.setClip(clip);
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && image.getHeight() > 0) {
                        double ratio = 500.0 / image.getWidth();
                        double displayHeight = Math.min(image.getHeight() * ratio, 350);
                        clip.setHeight(displayHeight);
                    }
                });
                content.getChildren().add(imageView);
            } catch (Exception e) {
                logger.error("Failed to load image for post " + post.getId(), e);
            }
        }
        
        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));
        
        TLButton likeBtn = new TLButton();
        likeBtn.setText("♥ " + post.getLikesCount());
        likeBtn.setVariant(post.isLikedByCurrentUser() ? TLButton.ButtonVariant.PRIMARY : TLButton.ButtonVariant.GHOST);
        likeBtn.setSize(TLButton.ButtonSize.SM);
        likeBtn.setOnAction(e -> {
            Task<Boolean> likeTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return PostService.getInstance().toggleLike(post.getId(), currentUser.getId());
                }
            };
            likeTask.setOnSucceeded(ev -> {
                Boolean success = likeTask.getValue();
                if (Boolean.TRUE.equals(success)) {
                    boolean wasLiked = post.isLikedByCurrentUser();
                    boolean nowLiked = !wasLiked;
                    int newCount = post.getLikesCount() + (nowLiked ? 1 : -1);
                    post.setLikesCount(newCount);
                    post.setLikedByCurrentUser(nowLiked);
                    Platform.runLater(() -> {
                        likeBtn.setText("♥ " + newCount);
                        likeBtn.setVariant(nowLiked ? TLButton.ButtonVariant.PRIMARY : TLButton.ButtonVariant.GHOST);
                    });
                }
            });
            AppThreadPool.execute(likeTask);
        });
        
        TLButton commentBtn = new TLButton();
        commentBtn.setText(I18n.get("post.comment") + " (" + post.getCommentsCount() + ")");
        commentBtn.setVariant(TLButton.ButtonVariant.GHOST);
        commentBtn.setGraphic(SvgIcons.icon(SvgIcons.MESSAGE_CIRCLE, 16));

        TLButton translateBtn = new TLButton();
        translateBtn.setText("Traduire");
        translateBtn.setVariant(TLButton.ButtonVariant.GHOST);
        translateBtn.setSize(TLButton.ButtonSize.SM);
        translateBtn.setOnAction(ev -> showTranslationMenu(translateBtn, contentLabel, post.getContent()));
        
        TLButton shareBtn = new TLButton();
        shareBtn.setText("Share");
        shareBtn.setVariant(TLButton.ButtonVariant.GHOST);
        shareBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 16));
        shareBtn.setOnAction(e -> {
            String link = "https://skilora.tn/post/" + post.getId();
            javafx.scene.input.ClipboardContent contentCopy = new javafx.scene.input.ClipboardContent();
            contentCopy.putString(link);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(contentCopy);

            TLToast.success(contentPane.getScene(), "Link Copied", "Link copied to clipboard");

            // Temporary visual feedback
            shareBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 16));
            shareBtn.setText("Copied!");
            
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(ev -> {
                shareBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 16));
                shareBtn.setText("Share");
            });
            pause.play();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(likeBtn, commentBtn, translateBtn, shareBtn, spacer);

        if (canEditOrDelete(post.getAuthorId())) {
            TLButton editBtn = new TLButton();
            editBtn.setText(I18n.get("post.edit"));
            editBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
            editBtn.setSize(TLButton.ButtonSize.SM);
            editBtn.setOnAction(ev -> showPostDialog(post));
            TLButton deleteBtn = new TLButton();
            deleteBtn.setText(I18n.get("post.delete"));
            deleteBtn.setVariant(TLButton.ButtonVariant.DANGER);
            deleteBtn.setSize(TLButton.ButtonSize.SM);
            deleteBtn.setOnAction(ev -> deletePost(post));
            actions.getChildren().addAll(editBtn, deleteBtn);
        } else if (currentUser != null && post.getAuthorId() != currentUser.getId()) {
            TLButton reportBtn = new TLButton();
            reportBtn.setGraphic(SvgIcons.icon(SvgIcons.FLAG, 14, "-fx-muted-foreground"));
            reportBtn.setVariant(TLButton.ButtonVariant.GHOST);
            reportBtn.setTooltip(new Tooltip(I18n.get("community.report")));
            reportBtn.setOnAction(e -> handleReportPost(post));
            actions.getChildren().add(reportBtn);
        }
        
        content.getChildren().add(actions);
        
        // Inline Comments Section
        VBox commentsSection = new VBox(12);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);
        commentsSection.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: -fx-border transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        
        commentBtn.setOnAction(e -> {
            boolean isVisible = commentsSection.isVisible();
            commentsSection.setVisible(!isVisible);
            commentsSection.setManaged(!isVisible);
            if (!isVisible && commentsSection.getChildren().isEmpty()) {
                loadInlineComments(post, commentsSection);
            }
        });
        
        content.getChildren().add(commentsSection);
        
        card.setContent(content);
        return card;
    }

    private void loadInlineComments(Post post, VBox container) {
        container.getChildren().clear();
        container.getChildren().add(new TLLoadingState());

        Task<List<PostComment>> loadTask = new Task<>() {
            @Override
            protected List<PostComment> call() {
                return PostService.getInstance().getComments(post.getId());
            }
        };
        loadTask.setOnSucceeded(e -> {
            container.getChildren().clear();
            List<PostComment> comments = loadTask.getValue();
            
            VBox commentsList = new VBox(8);
            if (comments.isEmpty()) {
                Label empty = new Label(I18n.get("post.no_comments"));
                empty.getStyleClass().add("text-muted");
                commentsList.getChildren().add(empty);
            } else {
                for (PostComment comment : comments) {
                    commentsList.getChildren().add(createInlineComment(comment));
                }
            }
            
            // Add comment input
            HBox addBox = new HBox(8);
            addBox.setAlignment(Pos.CENTER_LEFT);
            addBox.setPadding(new Insets(8, 0, 0, 0));
            
            TLTextField commentField = new TLTextField("", I18n.get("post.comment.placeholder"));
            HBox.setHgrow(commentField, Priority.ALWAYS);
            setupMentionDetectionForTextField(commentField);
            
            TLButton addBtn = new TLButton(I18n.get("message.send"));
            addBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            addBtn.setOnAction(ev -> {
                String text = commentField.getText();
                if (text != null && !text.trim().isEmpty()) {
                    String finalText = text.trim();
                    commentField.setText("");
                    addBtn.setDisable(true);
                    PostComment newComment = new PostComment();
                    newComment.setPostId(post.getId());
                    newComment.setAuthorId(currentUser.getId());
                    newComment.setContent(finalText);

                    Task<Integer> addTask = new Task<>() {
                        @Override
                        protected Integer call() {
                            return PostService.getInstance().addComment(newComment);
                        }
                    };
                    addTask.setOnSucceeded(ev2 -> {
                        addBtn.setDisable(false);
                        // Refresh inline comments
                        loadInlineComments(post, container);
                    });
                    AppThreadPool.execute(addTask);
                }
            });
            addBox.getChildren().addAll(commentField, addBtn);
            
            container.getChildren().addAll(commentsList, addBox);
        });
        AppThreadPool.execute(loadTask);
    }

    private VBox createInlineComment(PostComment comment) {
        VBox commentCard = new VBox(4);
        commentCard.setPadding(new Insets(8));
        commentCard.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label author = new Label(comment.getAuthorName());
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label time = new Label(formatDate(comment.getCreatedDate()));
        time.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        header.getChildren().addAll(author, time);

        Label text = new Label(comment.getContent());
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 13px;");

        commentCard.getChildren().addAll(header, text);
        return commentCard;
    }

    private void toggleLike(Post post) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return PostService.getInstance().toggleLike(post.getId(), currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(task.getValue())) {
                boolean wasLiked = post.isLikedByCurrentUser();
                boolean nowLiked = !wasLiked;
                int newCount = post.getLikesCount() + (nowLiked ? 1 : -1);
                post.setLikesCount(newCount);
                post.setLikedByCurrentUser(nowLiked);
                Platform.runLater(() -> displaySortedPosts(feedPostsContainer, currentFeedPosts, feedSortSelect != null ? feedSortSelect.getValue() : "Plus récent", feedSearchField != null ? feedSearchField.getText() : null));
            }
        });
        AppThreadPool.execute(task);
    }

    private void handleReportPost(Post post) {
        if (currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("community.report.title"));
        dialog.setDescription(I18n.get("community.report.reason"));

        TLTextarea reasonField = new TLTextarea();
        reasonField.setPromptText(I18n.get("community.report.placeholder"));
        reasonField.setPrefRowCount(4);

        dialog.setContent(reasonField);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.styleButtons();

        if (contentPane.getScene() != null && contentPane.getScene().getWindow() != null) {
            dialog.initOwner(contentPane.getScene().getWindow());
        }

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String reason = reasonField.getText() != null ? reasonField.getText().trim() : "";
                if (reason.isEmpty()) {
                    reasonField.setError(I18n.get("error.validation.required", I18n.get("community.report.reason")));
                    event.consume();
                } else {
                    reasonField.clearValidation();
                }
            }
        );

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String reason = reasonField.getText() != null ? reasonField.getText().trim() : "";

            Task<Integer> reportTask = new Task<>() {
                @Override
                protected Integer call() {
                    Report report = new Report();
                    report.setSubject("Reported Post #" + post.getId());
                    report.setType("Inappropriate");
                    report.setDescription(reason);
                    report.setReporterId(currentUser.getId());
                    report.setReportedEntityType("POST");
                    report.setReportedEntityId(post.getId());
                    return ReportService.getInstance().create(report);
                }
            };

            reportTask.setOnSucceeded(e -> Platform.runLater(() ->
                TLToast.success(contentPane.getScene(),
                    I18n.get("community.report"), I18n.get("community.report.success"))
            ));

            reportTask.setOnFailed(e -> Platform.runLater(() -> {
                logger.error("Failed to submit report", reportTask.getException());
                TLToast.error(contentPane.getScene(),
                    I18n.get("common.error"), I18n.get("community.report.error"));
            }));

            AppThreadPool.execute(reportTask);
        });
    }

    private void loadConnectionsTab() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;
        contentPane.getChildren().add(new TLLoadingState());
        
        Task<ConnectionsData> task = new Task<>() {
            @Override
            protected ConnectionsData call() {
                ConnectionsData data = new ConnectionsData();
                data.pending = ConnectionService.getInstance().getPendingRequests(currentUser.getId());
                data.accepted = ConnectionService.getInstance().getConnections(currentUser.getId());
                data.suggestions = ConnectionService.getInstance().getSuggestions(currentUser.getId(), 6);
                return data;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            contentPane.getChildren().clear();
            ConnectionsData data = task.getValue();

            // Pending requests
            if (data.pending != null && !data.pending.isEmpty()) {
                Label pendingTitle = new Label(I18n.get("connection.pending"));
                pendingTitle.getStyleClass().add("h4");
                contentPane.getChildren().add(pendingTitle);

                for (Connection req : data.pending) {
                    contentPane.getChildren().add(createPendingRequestCard(req));
                }
                contentPane.getChildren().add(new TLSeparator());
            }

            // Search people + suggestions
            Label peopleTitle = new Label(I18n.get("connection.title"));
            peopleTitle.getStyleClass().add("h4");

            TLTextField search = new TLTextField(I18n.get("connection.search"), I18n.get("connection.search.prompt"));
            VBox resultsBox = new VBox(10);
            resultsBox.setPadding(new Insets(4, 0, 0, 0));

            if (search.getControl() != null) {
                UiUtils.debounce(search.getControl(), 350, () -> {
                    String q = search.getText();
                    if (q == null || q.trim().isEmpty()) {
                        resultsBox.getChildren().setAll(buildSuggestionsBlock(data.suggestions));
                        return;
                    }
                    resultsBox.getChildren().setAll(new TLLoadingState(I18n.get("common.loading")));
                    AppThreadPool.execute(() -> {
                        List<Connection> found = ConnectionService.getInstance().searchPeople(currentUser.getId(), q, 12);
                        Platform.runLater(() -> resultsBox.getChildren().setAll(buildPeopleResults(found)));
                    });
                });
            }

            resultsBox.getChildren().addAll(buildSuggestionsBlock(data.suggestions));
            contentPane.getChildren().addAll(peopleTitle, search, resultsBox, new TLSeparator());

            // Current connections
            int connCount = data.accepted != null ? data.accepted.size() : 0;
            Label connectionsTitle = new Label(I18n.get("connection.count", connCount));
            connectionsTitle.getStyleClass().add("h4");
            contentPane.getChildren().add(connectionsTitle);

            if (data.accepted == null || data.accepted.isEmpty()) {
                Label empty = new Label(I18n.get("connection.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                FlowPane acceptedList = new FlowPane(12, 12);
                for (Connection conn : data.accepted) {
                    acceptedList.getChildren().add(createConnectionCard(conn));
                }
                contentPane.getChildren().add(acceptedList);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load connections", task.getException());
            contentPane.getChildren().clear();
            contentPane.getChildren().add(new TLEmptyState(
                    SvgIcons.USER_PLUS,
                    I18n.get("common.error"),
                    I18n.get("connection.error.load")));
        }));

        AppThreadPool.execute(task);
    }

    private TLCard createPendingRequestCard(Connection req) {
        TLCard card = new TLCard();
        card.setStyle("-fx-padding: 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: -fx-border; -fx-border-width: 1;");
        
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(40, 40);
        avatarPane.setMinSize(40, 40);
        avatarPane.setMaxSize(40, 40);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 20;");
        
        String initials = "";
        if (req.getOtherUserName() != null && !req.getOtherUserName().isEmpty()) {
            String[] parts = req.getOtherUserName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox infoBox = new VBox(4);
        Label name = new Label(req.getOtherUserName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label subtitle = new Label(I18n.get("connection.pending.subtitle", "Pending Request"));
        subtitle.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(name, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton rejectBtn = new TLButton(I18n.get("common.reject"), TLButton.ButtonVariant.OUTLINE);
        rejectBtn.setGraphic(SvgIcons.icon(SvgIcons.X_CIRCLE, 14));
        TLButton acceptBtn = new TLButton(I18n.get("common.accept"), TLButton.ButtonVariant.SUCCESS);
        acceptBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));

        rejectBtn.setOnAction(e -> respondToRequest(req.getId(), false));
        acceptBtn.setOnAction(e -> respondToRequest(req.getId(), true));

        content.getChildren().addAll(avatarPane, infoBox, spacer, rejectBtn, acceptBtn);
        card.setContent(content);
        return card;
    }

    private List<javafx.scene.Node> buildSuggestionsBlock(List<Connection> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            Label empty = new Label(I18n.get("connection.suggestions.empty"));
            empty.getStyleClass().add("text-muted");
            return List.of(empty);
        }

        Label title = new Label(I18n.get("connection.suggestions"));
        title.getStyleClass().add("text-muted");

        FlowPane list = new FlowPane(12, 12);
        for (Connection s : suggestions) {
            list.getChildren().add(createConnectCard(s));
        }
        return List.of(title, list);
    }

    private List<javafx.scene.Node> buildPeopleResults(List<Connection> found) {
        if (found == null || found.isEmpty()) {
            Label empty = new Label(I18n.get("connection.search.empty"));
            empty.getStyleClass().add("text-muted");
            return List.of(empty);
        }
        FlowPane list = new FlowPane(12, 12);
        for (Connection p : found) {
            list.getChildren().add(createConnectCard(p));
        }
        return List.of(list);
    }

    private TLCard createConnectCard(Connection person) {
        TLCard card = new TLCard();
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(280);
        card.setMinWidth(240);
        
        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);

        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(64, 64);
        avatarPane.setMinSize(64, 64);
        avatarPane.setMaxSize(64, 64);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 32;");
        
        String initials = "";
        if (person.getOtherUserName() != null && !person.getOtherUserName().isEmpty()) {
            String[] parts = person.getOtherUserName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px;");
        avatarPane.getChildren().add(avatarLabel);

        Label name = new Label(person.getOtherUserName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        TLButton connectBtn = new TLButton(I18n.get("connection.send_request"), TLButton.ButtonVariant.PRIMARY);
        connectBtn.setGraphic(SvgIcons.icon(SvgIcons.USER_PLUS, 14));
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setOnAction(e -> sendConnectionRequest(person.getUserId2()));

        content.getChildren().addAll(avatarPane, name, spacer, connectBtn);
        card.setContent(content);
        return card;
    }

    private void sendConnectionRequest(int toUserId) {
        if (currentUser == null) return;
        AppThreadPool.execute(() -> {
            int id = ConnectionService.getInstance().sendRequest(currentUser.getId(), toUserId);
            Platform.runLater(() -> {
                if (id > 0) {
                    TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("connection.success.sent"));
                } else {
                    TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("connection.error.failed"));
                }
                loadConnectionsTab();
            });
        });
    }

    private void respondToRequest(int connectionId, boolean accept) {
        AppThreadPool.execute(() -> {
            boolean ok = accept
                    ? ConnectionService.getInstance().acceptRequest(connectionId)
                    : ConnectionService.getInstance().rejectRequest(connectionId);
            Platform.runLater(() -> {
                if (ok) {
                    TLToast.success(contentPane.getScene(), I18n.get("common.success"),
                            accept ? I18n.get("connection.success.accepted") : I18n.get("connection.success.rejected"));
                } else {
                    TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("connection.error.failed"));
                }
                loadConnectionsTab();
            });
        });
    }

    private static class ConnectionsData {
        List<Connection> pending;
        List<Connection> accepted;
        List<Connection> suggestions;
    }

    private TLCard createConnectionCard(Connection conn) {
        TLCard card = new TLCard();
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(280);
        card.setMinWidth(240);
        
        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(64, 64);
        avatarPane.setMinSize(64, 64);
        avatarPane.setMaxSize(64, 64);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 32;");
        
        String initials = "";
        if (conn.getOtherUserName() != null && !conn.getOtherUserName().isEmpty()) {
            String[] parts = conn.getOtherUserName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px;");
        avatarPane.getChildren().add(avatarLabel);

        Label name = new Label(conn.getOtherUserName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        TLButton removeBtn = new TLButton();
        removeBtn.setText(I18n.get("connection.remove"));
        removeBtn.setVariant(TLButton.ButtonVariant.DANGER);
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setOnAction(e -> removeConnection(conn.getId()));
        
        content.getChildren().addAll(avatarPane, name, spacer, removeBtn);
        card.setContent(content);
        return card;
    }

    private void removeConnection(int connectionId) {
        DialogUtils.showConfirmation(
            I18n.get("common.confirm"),
            I18n.get("connection.confirm.remove")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                contentPane.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ConnectionService.getInstance().removeConnection(connectionId);
                    }
                };
                task.setOnSucceeded(e -> {
                    contentPane.setDisable(false);
                    if (task.getValue()) {
                        logger.info("Connection removed successfully");
                        loadConnectionsTab();
                    }
                });
                task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
                AppThreadPool.execute(task);
            }
        });
    }

    private void loadMessagesTab() {
        if (messagePollingTimeline != null) {
            messagePollingTimeline.stop();
        }
        contentPane.getChildren().clear();
        if (currentUser == null) return;

        HBox splitView = new HBox();
        VBox.setVgrow(splitView, Priority.ALWAYS);
        splitView.setStyle("-fx-background-color: -fx-background;");

        // Left side: Conversations list
        VBox leftSide = new VBox();
        leftSide.setPrefWidth(300);
        leftSide.setMinWidth(250);
        leftSide.setMaxWidth(400);
        leftSide.setStyle("-fx-border-color: transparent -fx-border transparent transparent; -fx-border-width: 0 1 0 0;");

        Label headerLabel = new Label(I18n.get("community.tab.messages"));
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 16;");

        // Right side: Active chat (declared early so filter lambda can reference it)
        VBox rightSide = new VBox();
        HBox.setHgrow(rightSide, Priority.ALWAYS);
        rightSide.setAlignment(Pos.CENTER);
        Label emptyState = new Label("Select a conversation");
        emptyState.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 16px;");
        rightSide.getChildren().add(emptyState);

        // Filter bar: Toutes / Non lues + sort
        final String[] messagesFilterRef = new String[]{"all"};
        final String[] messagesSortRef = new String[]{"recent"};
        TLButton filterAllBtn = new TLButton("Toutes");
        filterAllBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        filterAllBtn.setSize(TLButton.ButtonSize.SM);
        TLButton filterUnreadBtn = new TLButton("Non lues");
        filterUnreadBtn.setVariant(TLButton.ButtonVariant.GHOST);
        filterUnreadBtn.setSize(TLButton.ButtonSize.SM);
        TLSelect<String> messagesSortSelect = new TLSelect<>("Tri", "Plus récent", "Plus ancien");
        messagesSortSelect.setValue("Plus récent");
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 16, 8, 16));
        filterBar.getChildren().addAll(filterAllBtn, filterUnreadBtn, messagesSortSelect);

        ScrollPane leftScroll = new ScrollPane();
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(leftScroll, Priority.ALWAYS);
        
        VBox conversationsList = new VBox(0);
        leftScroll.setContent(conversationsList);
        leftSide.getChildren().addAll(headerLabel, filterBar, leftScroll);

        final List<Conversation>[] allConversationsRef = new List[]{null};
        java.util.function.Consumer<java.util.List<Conversation>> applyMessagesFilterAndSort = all -> {
            if (all == null) return;
            List<Conversation> filtered = "unread".equals(messagesFilterRef[0])
                    ? all.stream().filter(c -> c.getUnreadCount() > 0).toList()
                    : new ArrayList<>(all);
            String sort = messagesSortRef[0];
            filtered = new ArrayList<>(filtered);
            filtered.sort((a, b) -> {
                LocalDateTime da = a.getLastMessageDate() != null ? a.getLastMessageDate() : LocalDateTime.MIN;
                LocalDateTime db = b.getLastMessageDate() != null ? b.getLastMessageDate() : LocalDateTime.MIN;
                return "old".equals(sort) ? da.compareTo(db) : db.compareTo(da);
            });
            conversationsList.getChildren().clear();
            for (Conversation conv : filtered) {
                conversationsList.getChildren().add(createConversationCard(conv, rightSide));
            }
        };

        filterAllBtn.setOnAction(e -> {
            messagesFilterRef[0] = "all";
            filterAllBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            filterUnreadBtn.setVariant(TLButton.ButtonVariant.GHOST);
            applyMessagesFilterAndSort.accept(allConversationsRef[0]);
        });
        filterUnreadBtn.setOnAction(e -> {
            messagesFilterRef[0] = "unread";
            filterUnreadBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
            filterAllBtn.setVariant(TLButton.ButtonVariant.GHOST);
            applyMessagesFilterAndSort.accept(allConversationsRef[0]);
        });
        messagesSortSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            messagesSortRef[0] = "Plus ancien".equals(newVal) ? "old" : "recent";
            applyMessagesFilterAndSort.accept(allConversationsRef[0]);
        });

        splitView.getChildren().addAll(leftSide, rightSide);
        contentPane.getChildren().add(splitView);

        conversationsList.getChildren().add(new TLLoadingState());

        Task<List<Conversation>> task = new Task<>() {
            @Override
            protected List<Conversation> call() {
                return MessagingService.getInstance().getConversations(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Conversation> conversations = task.getValue();
            allConversationsRef[0] = conversations != null ? conversations : new ArrayList<>();
            conversationsList.getChildren().clear();
            if (allConversationsRef[0].isEmpty()) {
                Label empty = new Label(I18n.get("message.empty"));
                empty.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-padding: 16;");
                conversationsList.getChildren().add(empty);
            } else {
                applyMessagesFilterAndSort.accept(allConversationsRef[0]);
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load conversations", task.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("error.failed_load_conversations"));
        });
        AppThreadPool.execute(task);
    }

    private javafx.scene.Node createConversationCard(Conversation conv, VBox rightSide) {
        int otherUserId = (conv.getParticipant1() == currentUser.getId()) ? conv.getParticipant2() : conv.getParticipant1();
        boolean unread = conv.getUnreadCount() > 0;
        String baseStyle = "-fx-cursor: hand; -fx-background-color: transparent;";
        String unreadBorder = " -fx-border-color: transparent transparent transparent -fx-primary; -fx-border-width: 0 0 0 3;";
        String cardStyle = baseStyle + (unread ? unreadBorder : "");
        String hoverStyle = "-fx-cursor: hand; -fx-background-color: -fx-muted;" + (unread ? unreadBorder : "");

        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(cardStyle);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(cardStyle));

        // Online indicator + Avatar
        Circle onlineDot = new Circle(6);
        onlineDot.setStyle("-fx-fill: #9ca3af;");
        Task<Boolean> onlineTask = new Task<>() {
            @Override
            protected Boolean call() {
                return OnlineStatusService.getInstance().isUserOnline(otherUserId);
            }
        };
        onlineTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            onlineDot.setStyle(Boolean.TRUE.equals(onlineTask.getValue()) ? "-fx-fill: #22c55e;" : "-fx-fill: #9ca3af;");
        }));
        AppThreadPool.execute(onlineTask);

        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(48, 48);
        avatarPane.setMinSize(48, 48);
        avatarPane.setMaxSize(48, 48);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 24;");

        String initials = "";
        if (conv.getOtherUserName() != null && !conv.getOtherUserName().isEmpty()) {
            String[] parts = conv.getOtherUserName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        avatarPane.getChildren().add(avatarLabel);

        HBox avatarWithDot = new HBox(6);
        avatarWithDot.setAlignment(Pos.CENTER_LEFT);
        avatarWithDot.getChildren().addAll(onlineDot, avatarPane);

        VBox textBox = new VBox(4);
        Label name = new Label(conv.getOtherUserName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label preview = new Label(conv.getLastMessagePreview() != null ? conv.getLastMessagePreview() : "");
        preview.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
        preview.setMaxWidth(180);
        preview.setWrapText(false);

        textBox.getChildren().addAll(name, preview);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        Label timeLabel = new Label(formatConversationTime(conv.getLastMessageDate()));
        timeLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
        rightBox.getChildren().add(timeLabel);
        if (conv.getUnreadCount() > 0) {
            TLBadge unreadBadge = new TLBadge(String.valueOf(conv.getUnreadCount()), TLBadge.Variant.DESTRUCTIVE);
            rightBox.getChildren().add(unreadBadge);
        }

        card.getChildren().addAll(avatarWithDot, textBox, spacer, rightBox);

        card.setOnMouseClicked(e -> openConversation(conv, rightSide));
        return card;
    }

    private void openConversation(Conversation conv, VBox rightSide) {
        if (messagePollingTimeline != null) messagePollingTimeline.stop();
        if (typingPollTimeline != null) typingPollTimeline.stop();
        MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
        rightSide.getChildren().clear();
        rightSide.setAlignment(Pos.TOP_CENTER);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle("-fx-border-color: transparent transparent -fx-border transparent; -fx-border-width: 0 0 1 0; -fx-background-color: -fx-background;");

        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(40, 40);
        avatarPane.setMinSize(40, 40);
        avatarPane.setMaxSize(40, 40);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 20;");
        
        String initials = "";
        if (conv.getOtherUserName() != null && !conv.getOtherUserName().isEmpty()) {
            String[] parts = conv.getOtherUserName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        Label headerLabel = new Label(conv.getOtherUserName());
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // Determine the other user's ID for block operations
        int otherUserId = (conv.getParticipant1() == currentUser.getId())
                ? conv.getParticipant2() : conv.getParticipant1();
        boolean alreadyBlocked = UserBlockService.getInstance().isBlocked(currentUser.getId(), otherUserId);

        TLButton blockBtn = new TLButton(
                alreadyBlocked ? I18n.get("message.unblock") : I18n.get("message.block"),
                TLButton.ButtonVariant.GHOST);
        blockBtn.setGraphic(SvgIcons.icon(
                alreadyBlocked ? SvgIcons.CHECK_CIRCLE : SvgIcons.SHIELD, 14,
                alreadyBlocked ? "-fx-success" : "-fx-destructive"));
        blockBtn.setContentDisplay(ContentDisplay.LEFT);
        blockBtn.setOnAction(blockEvt -> {
            boolean isBlocked = UserBlockService.getInstance().isBlocked(currentUser.getId(), otherUserId);
            if (isBlocked) {
                UserBlockService.getInstance().unblockUser(currentUser.getId(), otherUserId);
                blockBtn.setText(I18n.get("message.block"));
                blockBtn.setGraphic(SvgIcons.icon(SvgIcons.SHIELD, 14, "-fx-destructive"));
            } else {
                UserBlockService.getInstance().blockUser(currentUser.getId(), otherUserId, "Blocked from DM");
                blockBtn.setText(I18n.get("message.unblock"));
                blockBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK_CIRCLE, 14, "-fx-success"));
            }
        });

        header.getChildren().addAll(avatarPane, headerLabel, headerSpacer, blockBtn);

        // Messages Area
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox messagesBox = new VBox(12);
        messagesBox.setPadding(new Insets(16));
        scrollPane.setContent(messagesBox);

        java.util.Map<Integer, HBox> messageNodes = new java.util.HashMap<>();

        Runnable fetchMessages = () -> {
        Task<List<Message>> msgTask = new Task<>() {
            @Override
            protected List<Message> call() {
                MessagingService.getInstance().markAsRead(conv.getId(), currentUser.getId());
                return MessagingService.getInstance().getMessages(conv.getId(), 1, 50);
            }
        };
        msgTask.setOnSucceeded(e -> {
                boolean addedNew = false;
                double vvalue = scrollPane.getVvalue();
                boolean isAtBottom = vvalue >= 0.95 || scrollPane.getVmax() == 0;
                
            for (Message msg : msgTask.getValue()) {
                    if (!messageNodes.containsKey(msg.getId())) {
                        HBox row = createMessageBubble(msg, msg.getSenderId() == currentUser.getId());
                        messageNodes.put(msg.getId(), row);
                messagesBox.getChildren().add(row);
                        addedNew = true;
                    } else {
                        updateMessageBubble(messageNodes.get(msg.getId()), msg, msg.getSenderId() == currentUser.getId());
            }
                }
                if (addedNew && isAtBottom) {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
                }
        });
        AppThreadPool.execute(msgTask);
        };

        fetchMessages.run();

        messagePollingTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> fetchMessages.run()));
        messagePollingTimeline.setCycleCount(Animation.INDEFINITE);
        messagePollingTimeline.play();

        String otherName = conv.getOtherUserName() != null ? conv.getOtherUserName() : "";

        // Typing indicator row (avatar + 3 animated dots + "X est en train d'écrire")
        HBox typingIndicatorRow = new HBox(8);
        typingIndicatorRow.setAlignment(Pos.CENTER_LEFT);
        typingIndicatorRow.setPadding(new Insets(2, 16, 2, 16));
        typingIndicatorRow.setVisible(false);
        typingIndicatorRow.setManaged(false);
        StackPane typingAvatar = createAvatar(otherName, 24);
        HBox typingBubble = new HBox(4);
        typingBubble.setAlignment(Pos.CENTER);
        typingBubble.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 12; -fx-padding: 6 10;");
        Label dot1 = new Label("\u25CF");
        Label dot2 = new Label("\u25CF");
        Label dot3 = new Label("\u25CF");
        dot1.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        dot2.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        dot3.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        typingBubble.getChildren().addAll(dot1, dot2, dot3);
        conversationDotAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dot1.opacityProperty(), 0.3),
                        new KeyValue(dot2.opacityProperty(), 0.3),
                        new KeyValue(dot3.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(300), new KeyValue(dot1.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(dot1.opacityProperty(), 0.3), new KeyValue(dot2.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(900), new KeyValue(dot2.opacityProperty(), 0.3), new KeyValue(dot3.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1200), new KeyValue(dot3.opacityProperty(), 0.3)));
        conversationDotAnimation.setCycleCount(Animation.INDEFINITE);
        Label typingText = new Label(otherName + " " + I18n.get("message.typing"));
        typingText.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        typingIndicatorRow.getChildren().addAll(typingAvatar, typingBubble, typingText);

        typingPollTimeline = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
            AppThreadPool.execute(() -> {
                boolean isTyping = MessagingService.getInstance().isUserTyping(conv.getId(), otherUserId);
                Platform.runLater(() -> {
                    if (isTyping && !typingIndicatorRow.isVisible()) {
                        typingIndicatorRow.setVisible(true);
                        typingIndicatorRow.setManaged(true);
                        conversationDotAnimation.play();
                        scrollPane.setVvalue(1.0);
                    } else if (!isTyping && typingIndicatorRow.isVisible()) {
                        typingIndicatorRow.setVisible(false);
                        typingIndicatorRow.setManaged(false);
                        conversationDotAnimation.stop();
                    }
                });
            });
        }));
        typingPollTimeline.setCycleCount(Animation.INDEFINITE);
        typingPollTimeline.play();

        // Send box
        HBox sendBox = new HBox(12);
        sendBox.setAlignment(Pos.CENTER_LEFT);
        sendBox.setPadding(new Insets(16));
        sendBox.setStyle("-fx-border-color: -fx-border transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-background-color: -fx-background;");

        TLTextField msgField = new TLTextField("", I18n.get("message.placeholder"));
        HBox.setHgrow(msgField, Priority.ALWAYS);

        msgField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                AppThreadPool.execute(() -> MessagingService.getInstance().updateTypingStatus(conv.getId(), currentUser.getId()));
            }
        });

        // Attach button (image/video)
        TLButton attachBtn = new TLButton("\uD83D\uDCCE", TLButton.ButtonVariant.GHOST);
        attachBtn.setSize(TLButton.ButtonSize.SM);
        attachBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        attachBtn.setOnAction(ev -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.get("message.attach.media"));
            String[] mediaPatterns = cloudinaryService.getAllowedMediaExtensionPatterns();
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(I18n.get("message.attach.media"), mediaPatterns));
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(I18n.get("message.attach.images"), cloudinaryService.getAllowedExtensionPatterns()));
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(I18n.get("message.attach.videos"), cloudinaryService.getAllowedVideoExtensionPatterns()));
            File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
            if (file != null) {
                String caption = msgField.getText();
                msgField.setText("");
                attachBtn.setText("\u23F3");
                attachBtn.setDisable(true);
                AppThreadPool.execute(() -> {
                    try {
                        String mediaType = cloudinaryService.detectMediaType(file);
                        String mediaUrl = "VIDEO".equals(mediaType) ? cloudinaryService.uploadVideo(file) : cloudinaryService.uploadImage(file);
                        MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                        int newId = MessagingService.getInstance().sendMessage(conv.getId(), currentUser.getId(),
                                (caption != null && !caption.trim().isEmpty()) ? caption.trim() : null,
                                mediaType, mediaUrl, file.getName());
                        Platform.runLater(() -> {
                            attachBtn.setText("\uD83D\uDCCE");
                            attachBtn.setDisable(false);
                            if (newId > 0) fetchMessages.run();
                            if (newId == -2 && msgField.getScene() != null)
                                TLToast.error(msgField.getScene(), I18n.get("message.block"), I18n.get("message.blocked_error"));
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            attachBtn.setText("\uD83D\uDCCE");
                            attachBtn.setDisable(false);
                            DialogUtils.showError(I18n.get("message.attach.error"), ex.getMessage());
                        });
                    }
                });
            }
        });

        AudioRecorderService audioRecorder = AudioRecorderService.getInstance();
        TLButton micBtn = new TLButton("\uD83C\uDFA4", TLButton.ButtonVariant.GHOST);
        micBtn.setSize(TLButton.ButtonSize.SM);
        micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        HBox recordingIndicator = new HBox(8);
        recordingIndicator.setAlignment(Pos.CENTER_LEFT);
        recordingIndicator.setVisible(false);
        recordingIndicator.setManaged(false);
        Label recordDot = new Label("\uD83D\uDD34");
        recordDot.setStyle("-fx-font-size: 12px;");
        Label recordTimer = new Label("0:00");
        recordTimer.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        Label recordHint = new Label(I18n.get("message.recording"));
        recordHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
        TLButton cancelRecordBtn = new TLButton("\u2716", TLButton.ButtonVariant.GHOST);
        cancelRecordBtn.setSize(TLButton.ButtonSize.SM);
        cancelRecordBtn.setStyle("-fx-text-fill: #e74c3c; -fx-cursor: hand;");
        recordingIndicator.getChildren().addAll(recordDot, recordTimer, recordHint, cancelRecordBtn);
        conversationRecordTimerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            if (audioRecorder.isRecording()) {
                int elapsed = audioRecorder.getElapsedSeconds();
                recordTimer.setText(AudioRecorderService.formatDuration(elapsed));
                if (elapsed >= 300) micBtn.fire();
            }
        }));
        conversationRecordTimerTimeline.setCycleCount(Animation.INDEFINITE);
        cancelRecordBtn.setOnAction(ev -> {
            audioRecorder.cancelRecording();
            conversationRecordTimerTimeline.stop();
            recordingIndicator.setVisible(false);
            recordingIndicator.setManaged(false);
            msgField.setVisible(true);
            msgField.setManaged(true);
            micBtn.setText("\uD83C\uDFA4");
            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        });
        micBtn.setOnAction(ev -> {
            if (!audioRecorder.isRecording()) {
                try {
                    audioRecorder.startRecording();
                    micBtn.setText("\u23F9");
                    micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #e74c3c;");
                    recordTimer.setText("0:00");
                    recordingIndicator.setVisible(true);
                    recordingIndicator.setManaged(true);
                    msgField.setVisible(false);
                    msgField.setManaged(false);
                    conversationRecordTimerTimeline.play();
                } catch (Exception ex) {
                    DialogUtils.showError("Microphone", ex.getMessage());
                }
            } else {
                conversationRecordTimerTimeline.stop();
                recordingIndicator.setVisible(false);
                recordingIndicator.setManaged(false);
                msgField.setVisible(true);
                msgField.setManaged(true);
                micBtn.setText("\u23F3");
                micBtn.setDisable(true);
                File wavFile = audioRecorder.stopRecording();
                if (wavFile == null) {
                    micBtn.setText("\uD83C\uDFA4");
                    micBtn.setDisable(false);
                    micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                    return;
                }
                int durationSec = AudioRecorderService.getWavDurationSeconds(wavFile);
                AppThreadPool.execute(() -> {
                    try {
                        String audioUrl = cloudinaryService.uploadAudio(wavFile);
                        MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                        MessagingService.getInstance().sendMessage(conv.getId(), currentUser.getId(), null, "VOCAL", audioUrl, wavFile.getName(), durationSec);
                        Platform.runLater(() -> {
                            micBtn.setText("\uD83C\uDFA4");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                            fetchMessages.run();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            micBtn.setText("\uD83C\uDFA4");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                            DialogUtils.showError(I18n.get("message.vocal.error"), ex.getMessage());
                        });
                    }
                });
            }
        });

        TLButton emojiBtn = new TLButton("\uD83D\uDE00", TLButton.ButtonVariant.GHOST);
        emojiBtn.setSize(TLButton.ButtonSize.SM);
        emojiBtn.setOnAction(e -> showEmojiPickerForTextField(emojiBtn, msgField));

        TLButton sendBtn = new TLButton(I18n.get("message.send"));
        sendBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
        sendBtn.setGraphic(SvgIcons.icon(SvgIcons.SEND, 16));
        sendBtn.setOnAction(e -> {
            if (audioRecorder.isRecording()) {
                micBtn.fire();
                return;
            }
            String text = msgField.getText();
            if (text != null && !text.trim().isEmpty()) {
                String finalText = text.trim();
                msgField.setText("");
                MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                sendBtn.setDisable(true);
                Task<Integer> sendTask = new Task<>() {
                    @Override
                    protected Integer call() {
                        return MessagingService.getInstance().sendMessage(conv.getId(), currentUser.getId(), finalText);
                    }
                };
                sendTask.setOnSucceeded(ev -> {
                    sendBtn.setDisable(false);
                    int newMsgId = sendTask.getValue();
                    if (newMsgId == -2) {
                        if (msgField.getScene() != null) {
                            TLToast.error(msgField.getScene(), I18n.get("message.block"), I18n.get("message.blocked_error"));
                        }
                        return;
                    }
                    if (newMsgId > 0) {
                        Message newMsg = new Message();
                        newMsg.setId(newMsgId);
                        newMsg.setContent(finalText);
                        newMsg.setSenderId(currentUser.getId());
                        newMsg.setRead(false);
                        HBox row = createMessageBubble(newMsg, true);
                        messageNodes.put(newMsgId, row);
                        messagesBox.getChildren().add(row);
                        Platform.runLater(() -> scrollPane.setVvalue(1.0));
                    }
                });
                AppThreadPool.execute(sendTask);
            }
        });
        sendBox.getChildren().addAll(msgField, recordingIndicator, attachBtn, micBtn, emojiBtn, sendBtn);

        rightSide.getChildren().addAll(header, scrollPane, typingIndicatorRow, sendBox);

        // Clear typing and stop timelines when leaving the conversation view
        rightSide.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (openConversationId >= 0) {
                    MessagingService.getInstance().clearTypingStatus(openConversationId, currentUser.getId());
                    openConversationId = -1;
                }
                if (messagePollingTimeline != null) messagePollingTimeline.stop();
                if (typingPollTimeline != null) typingPollTimeline.stop();
                if (conversationDotAnimation != null) conversationDotAnimation.stop();
                if (conversationRecordTimerTimeline != null) conversationRecordTimerTimeline.stop();
                if (audioRecorder.isRecording()) audioRecorder.cancelRecording();
            }
        });
    }

    private HBox createMessageBubble(Message msg, boolean isMine) {
        HBox row = new HBox();
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        String bubbleStyle = isMine
                ? "-fx-background-color: -fx-primary; -fx-background-radius: 16 16 4 16;"
                : "-fx-background-color: -fx-muted; -fx-background-radius: 16 16 16 4;";
        VBox bubbleBox = new VBox(4);
        bubbleBox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleBox.setMaxWidth(450);
        bubbleBox.setStyle(bubbleStyle + " -fx-padding: 10 14 10 14;");

        if (msg.isImage() && msg.hasMedia()) {
            try {
                ImageView imageView = new ImageView();
                imageView.setFitWidth(280);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                String url = msg.getMediaUrl();
                if (url != null && !url.isBlank()) {
                    if (!url.startsWith("http") && !url.startsWith("file:")) url = "file:" + url;
                    imageView.setImage(new Image(url, true));
                }
                bubbleBox.getChildren().add(imageView);
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    Label captionLabel = new Label(msg.getContent());
                    captionLabel.setWrapText(true);
                    captionLabel.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                            : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                    bubbleBox.getChildren().add(captionLabel);
                }
            } catch (Exception imgEx) {
                Label fallback = new Label("\uD83D\uDCF7 " + (msg.getFileName() != null ? msg.getFileName() : "Image"));
                fallback.setWrapText(true);
                fallback.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 13px;"
                        : "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
                bubbleBox.getChildren().add(fallback);
            }
        } else if (msg.isVideo() && msg.hasMedia()) {
            VBox videoBox = new VBox(4);
            videoBox.setAlignment(Pos.CENTER);
            Label videoIcon = new Label("\uD83C\uDFAC");
            videoIcon.setStyle("-fx-font-size: 36px;");
            Label videoName = new Label(msg.getFileName() != null ? msg.getFileName() : "Vid\u00E9o");
            videoName.setWrapText(true);
            videoName.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px;"
                    : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px;");
            Label playHint = new Label("\u25B6 Cliquer pour ouvrir");
            playHint.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));
            videoBox.getChildren().addAll(videoIcon, videoName, playHint);
            videoBox.setStyle("-fx-cursor: hand; -fx-padding: 12;");
            videoBox.setOnMouseClicked(vidEv -> {
                try {
                    String mediaUrl = msg.getMediaUrl();
                    if (mediaUrl != null) {
                        if (mediaUrl.startsWith("file:")) {
                            Desktop.getDesktop().open(new File(URI.create(mediaUrl)));
                        } else {
                            Desktop.getDesktop().browse(URI.create(mediaUrl));
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Cannot open video: {}", ex.getMessage());
                }
            });
            bubbleBox.getChildren().add(videoBox);
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label captionLabel = new Label(msg.getContent());
                captionLabel.setWrapText(true);
                captionLabel.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                        : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                bubbleBox.getChildren().add(captionLabel);
            }
        } else if (msg.isVocal() && msg.hasMedia()) {
            HBox vocalBox = new HBox(8);
            vocalBox.setAlignment(Pos.CENTER_LEFT);
            Label playPauseIcon = new Label("\u25B6");
            playPauseIcon.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-text-fill: " + (isMine ? "-fx-primary-foreground;" : "-fx-primary;"));
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(150);
            progressBar.setPrefHeight(6);
            int durationSecs = msg.getDuration() > 0 ? msg.getDuration() : 0;
            String durationText = AudioRecorderService.formatDuration(durationSecs);
            Label durationLabel = new Label(durationText);
            durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));
            final MediaPlayer[] playerHolder = { null };
            final boolean[] isPlaying = { false };
            playPauseIcon.setOnMouseClicked(playEv -> {
                if (isPlaying[0] && playerHolder[0] != null) {
                    playerHolder[0].pause();
                    playPauseIcon.setText("\u25B6");
                    isPlaying[0] = false;
                    return;
                }
                if (playerHolder[0] == null) {
                    String mediaUrl = msg.getMediaUrl();
                    if (mediaUrl == null || mediaUrl.isBlank()) return;
                    if (!mediaUrl.startsWith("http") && !mediaUrl.startsWith("file:")) {
                        mediaUrl = mediaUrl.startsWith("/") ? "file:" + mediaUrl : "file:///" + mediaUrl;
                    }
                    try {
                        Media media = new Media(mediaUrl);
                        MediaPlayer player = new MediaPlayer(media);
                        playerHolder[0] = player;
                        player.currentTimeProperty().addListener((obsT, oldT, newT) -> {
                            MediaPlayer p = playerHolder[0];
                            if (p == null) return;
                            javafx.util.Duration total = p.getTotalDuration();
                            if (total == null || total.toMillis() <= 0) return;
                            double progress = Math.min(1.0, newT.toMillis() / total.toMillis());
                            double secs = newT.toSeconds();
                            Platform.runLater(() -> {
                                if (playerHolder[0] == null) return;
                                progressBar.setProgress(progress);
                                durationLabel.setText(AudioRecorderService.formatDuration((int) secs));
                            });
                        });
                        player.setOnEndOfMedia(() -> Platform.runLater(() -> {
                            playPauseIcon.setText("\u25B6");
                            progressBar.setProgress(0);
                            durationLabel.setText(durationText);
                            isPlaying[0] = false;
                            MediaPlayer p = playerHolder[0];
                            if (p != null) {
                                try { p.stop(); } catch (Exception ignored) { }
                                try { p.dispose(); } catch (Exception ignored) { }
                                playerHolder[0] = null;
                            }
                        }));
                        player.setOnError(() -> Platform.runLater(() -> {
                            playPauseIcon.setText("\u25B6");
                            isPlaying[0] = false;
                            MediaPlayer p = playerHolder[0];
                            logger.warn("Audio playback error: {}", p != null ? p.getError() : "player null");
                            if (p != null) {
                                try { p.dispose(); } catch (Exception ignored) { }
                                playerHolder[0] = null;
                            }
                        }));
                        player.play();
                        playPauseIcon.setText("\u23F8");
                        isPlaying[0] = true;
                    } catch (Exception audioEx) {
                        logger.warn("Cannot create audio player for vocal: {}", audioEx.getMessage());
                        return;
                    }
                } else {
                    if (playerHolder[0] != null) {
                        playerHolder[0].play();
                        playPauseIcon.setText("\u23F8");
                        isPlaying[0] = true;
                    }
                }
            });
            vocalBox.getChildren().addAll(playPauseIcon, progressBar, durationLabel);
            bubbleBox.getChildren().add(vocalBox);
        } else {
            String content = msg.getContent() != null ? msg.getContent() : "";
            Label msgLabel = new Label(content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle(isMine ? "-fx-text-fill: white; -fx-font-size: 14px;" : "-fx-text-fill: -fx-foreground; -fx-font-size: 14px;");
            bubbleBox.getChildren().add(msgLabel);
        }

        if (isMine && msg.isRead()) {
            Label readLabel = new Label("Read");
            readLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-muted-foreground;");
            readLabel.setGraphic(SvgIcons.icon(SvgIcons.CHECK_DOUBLE, 12, "-fx-primary"));
            bubbleBox.getChildren().add(readLabel);
        }

        row.getChildren().add(bubbleBox);
        return row;
    }

    private void updateMessageBubble(HBox row, Message msg, boolean isMine) {
        if (!isMine || !msg.isRead()) return;
        VBox bubble = (VBox) row.getChildren().get(0);
        if (bubble.getChildren().isEmpty()) return;
        Node last = bubble.getChildren().get(bubble.getChildren().size() - 1);
        if (last instanceof Label l && "Read".equals(l.getText())) return;
        Label readLabel = new Label("Read");
        readLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-muted-foreground;");
        readLabel.setGraphic(SvgIcons.icon(SvgIcons.CHECK_DOUBLE, 12, "-fx-primary"));
        bubble.getChildren().add(readLabel);
    }

    private void loadEventsTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(new TLLoadingState());

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
            contentPane.getChildren().clear();
            contentPane.getChildren().add(topBar);
            List<Event> events = task.getValue();
            if (events.isEmpty()) {
                Label empty = new Label(I18n.get("event.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                FlowPane grid = new FlowPane(16, 16);
                for (Event event : events) {
                    grid.getChildren().add(createEventCard(event));
                }
                contentPane.getChildren().add(grid);
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
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(320);
        card.setMinWidth(280);
        
        VBox content = new VBox(12);
        
        // Header with icon and title
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(48, 48);
        iconPane.setStyle("-fx-background-color: derive(-fx-primary, 60%); -fx-background-radius: 8;");
        javafx.scene.Node icon = SvgIcons.icon(SvgIcons.CALENDAR, 24, "-fx-primary");
        iconPane.getChildren().add(icon);
        
        VBox titleBox = new VBox(4);
        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        title.setWrapText(true);
        
        Label type = new Label(event.getEventType() != null ? event.getEventType().name() : "EVENT");
        type.setStyle("-fx-text-fill: -fx-primary; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        titleBox.getChildren().addAll(title, type);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        header.getChildren().addAll(iconPane, titleBox);
        
        // Details
        VBox details = new VBox(8);
        
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.getChildren().addAll(
            SvgIcons.icon(SvgIcons.CLOCK, 14, "-fx-muted-foreground"),
            new Label(formatDate(event.getStartDate()))
        );
        dateBox.getChildren().get(1).setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
        
        HBox locBox = new HBox(8);
        locBox.setAlignment(Pos.CENTER_LEFT);
        String locStr = (event.getLocation() != null && !event.getLocation().isEmpty()) ? event.getLocation() : "Online";
        locBox.getChildren().addAll(
            SvgIcons.icon(SvgIcons.MAP_PIN, 14, "-fx-muted-foreground"),
            new Label(locStr)
        );
        locBox.getChildren().get(1).setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
        
        HBox attBox = new HBox(8);
        attBox.setAlignment(Pos.CENTER_LEFT);
        attBox.getChildren().addAll(
            SvgIcons.icon(SvgIcons.USERS, 14, "-fx-muted-foreground"),
            new Label(I18n.get("event.attendees", event.getCurrentAttendees()))
        );
        attBox.getChildren().get(1).setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
        
        details.getChildren().addAll(dateBox, locBox, attBox);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // RSVP / Cancel RSVP button
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        if (currentUser != null) {
            TLButton rsvpBtn = new TLButton();
            rsvpBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rsvpBtn, Priority.ALWAYS);
            
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

            TLButton calendarBtn = new TLButton();
            calendarBtn.setText("Add to Calendar");
            calendarBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
            calendarBtn.setOnAction(ev -> generateIcsFile(event));

            actions.getChildren().addAll(rsvpBtn, calendarBtn);
        }

        content.getChildren().addAll(header, details, spacer, actions);
        card.setContent(content);
        return card;
    }

    private void generateIcsFile(Event event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Event to Calendar");
        fileChooser.setInitialFileName("event.ics");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("iCalendar Files", "*.ics"));
        
        java.io.File file = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
        if (file != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                String dtStart = event.getStartDate() != null ? event.getStartDate().format(formatter) : LocalDateTime.now().format(formatter);
                String dtEnd = event.getEndDate() != null ? event.getEndDate().format(formatter) : (event.getStartDate() != null ? event.getStartDate().plusHours(1).format(formatter) : LocalDateTime.now().plusHours(1).format(formatter));
                
                String icsContent = "BEGIN:VCALENDAR\n" +
                        "VERSION:2.0\n" +
                        "BEGIN:VEVENT\n" +
                        "SUMMARY:" + (event.getTitle() != null ? event.getTitle() : "Event") + "\n" +
                        "DTSTART:" + dtStart + "\n" +
                        "DTEND:" + dtEnd + "\n" +
                        "LOCATION:" + (event.getLocation() != null ? event.getLocation() : "") + "\n" +
                        "DESCRIPTION:" + (event.getDescription() != null ? event.getDescription().replace("\n", "\\n") : "") + "\n" +
                        "END:VEVENT\n" +
                        "END:VCALENDAR";
                
                java.nio.file.Files.writeString(file.toPath(), icsContent);
                TLToast.success(contentPane.getScene(), "Success", "Event saved to calendar");
            } catch (Exception e) {
                logger.error("Failed to save .ics file", e);
                TLToast.error(contentPane.getScene(), "Error", "Failed to save calendar file");
            }
        }
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
        contentPane.getChildren().add(new TLLoadingState());

        Task<List<CommunityGroup>> task = new Task<>() {
            @Override
            protected List<CommunityGroup> call() {
                return GroupService.getInstance().findAll();
            }
        };

        task.setOnSucceeded(e -> {
            contentPane.getChildren().clear();
            List<CommunityGroup> groups = task.getValue();
            if (groups.isEmpty()) {
                Label empty = new Label(I18n.get("group.empty"));
                empty.getStyleClass().add("text-muted");
                contentPane.getChildren().add(empty);
            } else {
                FlowPane grid = new FlowPane(16, 16);
                for (CommunityGroup group : groups) {
                    grid.getChildren().add(createGroupCard(group));
                }
                contentPane.getChildren().add(grid);
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
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(300);
        card.setMinWidth(260);
        
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(64, 64);
        avatarPane.setMinSize(64, 64);
        avatarPane.setMaxSize(64, 64);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 32;");
        
        String initials = "";
        if (group.getName() != null && !group.getName().isEmpty()) {
            String[] parts = group.getName().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px;");
        avatarPane.getChildren().add(avatarLabel);
        
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER);
        
        Label name = new Label(group.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        
        HBox membersBox = new HBox(6);
        membersBox.setAlignment(Pos.CENTER);
        membersBox.getChildren().addAll(
            SvgIcons.icon(SvgIcons.USERS, 14, "-fx-muted-foreground"),
            new Label(I18n.get("group.members", group.getMemberCount()))
        );
        membersBox.getChildren().get(1).setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
        
        infoBox.getChildren().addAll(name, membersBox);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Join / Leave button
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(4, 0, 0, 0));

        if (currentUser != null) {
            TLButton actionBtn = new TLButton();
            actionBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(actionBtn, Priority.ALWAYS);
            
            Task<Boolean> checkTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return GroupService.getInstance().isMember(group.getId(), currentUser.getId());
                }
            };
            checkTask.setOnSucceeded(e -> {
                boolean isMember = checkTask.getValue();
                if (isMember) {
                    actionBtn.setText(I18n.get("group.view", "View Group"));
                    actionBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
                    actionBtn.setOnAction(ev -> showGroupDetails(group));
                } else {
                    actionBtn.setText(I18n.get("group.join"));
                    actionBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
                    actionBtn.setOnAction(ev -> joinGroup(group.getId()));
                }
            });
            AppThreadPool.execute(checkTask);

            actions.getChildren().add(actionBtn);
        }

        content.getChildren().addAll(avatarPane, infoBox, spacer, actions);
        card.setContent(content);
        return card;
    }

    private void joinGroup(int groupId) {
        contentPane.setDisable(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return GroupService.getInstance().join(groupId, currentUser.getId());
            }
        };
        task.setOnSucceeded(e -> {
            contentPane.setDisable(false);
            if (task.getValue()) loadGroupsTab();
        });
        task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
        AppThreadPool.execute(task);
    }

    private void leaveGroup(int groupId) {
        DialogUtils.showConfirmation(
            I18n.get("group.leave.confirm.title"),
            I18n.get("group.leave.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                contentPane.setDisable(true);
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return GroupService.getInstance().leave(groupId, currentUser.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    contentPane.setDisable(false);
                    if (task.getValue()) loadGroupsTab();
                });
                task.setOnFailed(e -> Platform.runLater(() -> contentPane.setDisable(false)));
                AppThreadPool.execute(task);
            }
        });
    }

    private void showGroupDetails(CommunityGroup group) {
        contentPane.getChildren().clear();

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));

        TLButton backBtn = new TLButton();
        backBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 16));
        backBtn.setVariant(TLButton.ButtonVariant.GHOST);
        backBtn.setOnAction(e -> loadGroupsTab());

        VBox titleBox = new VBox(4);
        Label nameLabel = new Label(group.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label descLabel = new Label(group.getDescription() != null ? group.getDescription() : "");
        descLabel.setStyle("-fx-text-fill: -fx-muted-foreground;");
        titleBox.getChildren().addAll(nameLabel, descLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton leaveBtn = new TLButton();
        leaveBtn.setText(I18n.get("group.leave", "Leave Group"));
        leaveBtn.setVariant(TLButton.ButtonVariant.DANGER);
        leaveBtn.setOnAction(e -> leaveGroup(group.getId()));

        header.getChildren().addAll(backBtn, titleBox, spacer, leaveBtn);

        // Tabs for Feed and Members
        TLTabs groupTabs = new TLTabs();
        
        // Feed Tab (Placeholder)
        VBox feedTab = new VBox(16);
        feedTab.setAlignment(Pos.CENTER);
        feedTab.setPadding(new Insets(32));
        Label feedPlaceholder = new Label("Group feed coming soon");
        feedPlaceholder.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 16px;");
        feedTab.getChildren().add(feedPlaceholder);
        
        // Members Tab
        VBox membersTab = new VBox(16);
        membersTab.setPadding(new Insets(16, 0, 0, 0));
        FlowPane membersGrid = new FlowPane(16, 16);
        membersTab.getChildren().add(membersGrid);

        groupTabs.addTab("group_feed", "Feed", feedTab);
        groupTabs.addTab("group_members", "Members", membersTab);

        VBox.setVgrow(groupTabs, Priority.ALWAYS);
        contentPane.getChildren().addAll(header, groupTabs);

        // Fetch members
        Task<List<GroupMember>> fetchMembersTask = new Task<>() {
            @Override
            protected List<GroupMember> call() {
                return GroupService.getInstance().getMembers(group.getId());
            }
        };

        fetchMembersTask.setOnSucceeded(e -> {
            List<GroupMember> members = fetchMembersTask.getValue();
            membersGrid.getChildren().clear();
            for (GroupMember member : members) {
                membersGrid.getChildren().add(createMemberCard(member));
            }
        });

        fetchMembersTask.setOnFailed(e -> {
            logger.error("Failed to load group members", fetchMembersTask.getException());
            TLToast.error(contentPane.getScene(), I18n.get("common.error", "Error"), "Failed to load group members");
        });

        AppThreadPool.execute(fetchMembersTask);
    }

    private TLCard createMemberCard(GroupMember member) {
        TLCard card = new TLCard();
        card.setStyle("-fx-padding: 12; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(200);

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(40, 40);
        avatarPane.setMinSize(40, 40);
        avatarPane.setMaxSize(40, 40);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 20;");

        String initials = "";
        String name = member.getUserName() != null ? member.getUserName() : "";
        if (!name.trim().isEmpty()) {
            String[] parts = name.trim().split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox infoBox = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label roleLabel = new Label(member.getRole() != null ? member.getRole() : "");
        roleLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(nameLabel, roleLabel);

        content.getChildren().addAll(avatarPane, infoBox);
        card.setContent(content);
        return card;
    }

    // ── Blog ─────────────────────────────────────────────────────────

    private void handleNewArticle() {
        if (currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("blog.new"));
        dialog.setDescription(I18n.get("blog.new.description"));

        TLTextField title = new TLTextField(I18n.get("blog.field.title"), I18n.get("blog.title.placeholder"));
        TLTextField summary = new TLTextField(I18n.get("blog.field.summary"), I18n.get("blog.summary.placeholder"));
        TLTextarea content = new TLTextarea(I18n.get("blog.field.content"), I18n.get("blog.content.placeholder"));
        content.setPrefRowCount(8);

        TLTextField category = new TLTextField(I18n.get("blog.field.category"), I18n.get("blog.category.placeholder"));
        TLTextField tags = new TLTextField(I18n.get("blog.field.tags"), I18n.get("blog.tags.placeholder"));

        VBox form = new VBox(12, title, summary, content, category, tags);
        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.styleButtons();

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String t = title.getText() != null ? title.getText().trim() : "";
                String c = content.getText() != null ? content.getText().trim() : "";
                if (t.isEmpty()) {
                    title.setError(I18n.get("error.validation.required", I18n.get("blog.field.title")));
                    valid = false;
                } else { title.clearValidation(); }
                if (c.isEmpty()) {
                    content.setError(I18n.get("error.validation.required", I18n.get("blog.field.content")));
                    valid = false;
                } else { content.clearValidation(); }
                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String t = title.getText() != null ? title.getText().trim() : "";
            String c = content.getText() != null ? content.getText().trim() : "";

            BlogArticle article = new BlogArticle();
            article.setAuthorId(currentUser.getId());
            article.setTitle(t);
            article.setSummary(summary.getText());
            article.setContent(c);
            article.setCategory(category.getText());
            article.setTags(tags.getText());
            article.setPublished(true);

            AppThreadPool.execute(() -> {
                int id = BlogService.getInstance().create(article);
                Platform.runLater(() -> {
                    if (id > 0) {
                        TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("blog.success.created"));
                        loadBlogTab();
                    } else {
                        TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("blog.create_failed"));
                    }
                });
            });
        });
    }

    private void loadBlogTab() {
        contentPane.getChildren().clear();
        if (currentUser == null) return;

        VBox container = new VBox(12);
        TLTextField search = new TLTextField(I18n.get("blog.search"), I18n.get("blog.search.prompt"));
        VBox listBox = new VBox(12);
        listBox.getChildren().add(new TLLoadingState(I18n.get("common.loading")));
        container.getChildren().addAll(search, listBox);
        contentPane.getChildren().add(container);

        Runnable refresh = () -> loadBlogArticles(search.getText(), listBox);
        if (search.getControl() != null) {
            UiUtils.debounce(search.getControl(), 350, refresh);
        }
        refresh.run();
    }

    private void loadBlogArticles(String query, VBox listBox) {
        if (listBox == null) return;
        listBox.getChildren().setAll(new TLLoadingState(I18n.get("common.loading")));

        Task<List<BlogArticle>> task = new Task<>() {
                        @Override
            protected List<BlogArticle> call() {
                String q = query != null ? query.trim() : "";
                if (q.isEmpty()) return BlogService.getInstance().findPublished();
                return BlogService.getInstance().search(q);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            listBox.getChildren().clear();
            List<BlogArticle> articles = task.getValue();
            if (articles == null || articles.isEmpty()) {
                listBox.getChildren().add(new TLEmptyState(
                        SvgIcons.FILE_TEXT,
                        I18n.get("blog.empty"),
                        I18n.get("blog.empty.desc")));
                return;
            }
            FlowPane grid = new FlowPane(16, 16);
            for (BlogArticle a : articles) {
                grid.getChildren().add(createBlogCard(a));
            }
            listBox.getChildren().add(grid);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load blog articles", task.getException());
            listBox.getChildren().setAll(new TLEmptyState(
                    SvgIcons.ALERT_TRIANGLE,
                    I18n.get("common.error"),
                    I18n.get("blog.load_failed")));
        }));

        AppThreadPool.execute(task);
    }

    private TLCard createBlogCard(BlogArticle article) {
        TLCard card = new TLCard();
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");
        card.setPrefWidth(320);

        VBox content = new VBox(12);

        // Category badge
        if (article.getCategory() != null && !article.getCategory().isEmpty()) {
            Label category = new Label(article.getCategory().toUpperCase());
            category.setStyle("-fx-background-color: derive(-fx-primary, 60%); -fx-text-fill: -fx-primary; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            content.getChildren().add(category);
        }

        Label title = new Label(article.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        title.setWrapText(true);

        Label summary = new Label();
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            summary.setText(article.getSummary());
        } else if (article.getContent() != null) {
            String text = article.getContent();
            summary.setText(text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }
        summary.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 14px; -fx-line-spacing: 4px;");
        summary.setWrapText(true);
        summary.setMaxHeight(60);
        
        content.getChildren().addAll(title, summary);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Author info
        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setPadding(new Insets(8, 0, 0, 0));
        meta.setStyle("-fx-border-color: -fx-border transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(32, 32);
        avatarPane.setMinSize(32, 32);
        avatarPane.setMaxSize(32, 32);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 16;");
        
        String initials = "";
        String authorName = article.getAuthorName() != null ? article.getAuthorName() : "Unknown";
        if (!authorName.isEmpty()) {
            String[] parts = authorName.split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        avatarPane.getChildren().add(avatarLabel);
        
        VBox authorInfo = new VBox(2);
        Label author = new Label(authorName);
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label date = new Label(article.getPublishedDate() != null ? formatDate(article.getPublishedDate()) : "");
        date.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
        authorInfo.getChildren().addAll(author, date);
        
        meta.getChildren().addAll(avatarPane, authorInfo);
        
        content.getChildren().addAll(spacer, meta);

        card.setContent(content);
        card.setOnMouseClicked(e -> showArticleView(article));
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-primary; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-padding: 16; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;"));
        
        return card;
    }

    private void showArticleView(BlogArticle article) {
        if (article == null) return;
        contentPane.getChildren().clear();

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));

        // Back button
        TLButton backBtn = new TLButton();
        backBtn.setText("Back to Blog");
        backBtn.setVariant(TLButton.ButtonVariant.SECONDARY);
        backBtn.setOnAction(e -> loadBlogTab());

        // Title
        Label title = new Label(article.getTitle());
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        title.setWrapText(true);

        // Author info
        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(40, 40);
        avatarPane.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 20;");
        
        String initials = "";
        String authorName = article.getAuthorName() != null ? article.getAuthorName() : "Unknown";
        if (!authorName.isEmpty()) {
            String[] parts = authorName.split(" ");
            initials += parts[0].charAt(0);
            if (parts.length > 1) {
                initials += parts[parts.length - 1].charAt(0);
            }
        }
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);
        
        VBox authorInfo = new VBox(2);
        Label author = new Label(authorName);
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label date = new Label(article.getPublishedDate() != null ? formatDate(article.getPublishedDate()) : "");
        date.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
        authorInfo.getChildren().addAll(author, date);
        
        meta.getChildren().addAll(avatarPane, authorInfo);

        // Content
        Label content = new Label(article.getContent() != null ? article.getContent() : "");
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 16px; -fx-line-spacing: 6px;");

        // Comments placeholder
        VBox commentsSection = new VBox(10);
        commentsSection.setPadding(new Insets(20, 0, 0, 0));
        commentsSection.setStyle("-fx-border-color: -fx-border transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label commentsPlaceholder = new Label("Comments will be displayed here.");
        commentsPlaceholder.setStyle("-fx-text-fill: -fx-muted-foreground;");
        commentsSection.getChildren().addAll(commentsTitle, commentsPlaceholder);

        container.getChildren().addAll(backBtn, title, meta, content, commentsSection);
        contentPane.getChildren().add(container);

        AppThreadPool.execute(() -> {
            BlogService.getInstance().incrementViews(article.getId());
        });
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Short time for conversation list: HH:mm today, dd/MM otherwise */
    private String formatConversationTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    // ═══════════════════════════════════════════════════════════
    //  REAL-TIME NOTIFICATIONS — Polling service
    // ═══════════════════════════════════════════════════════════

    /**
     * Start real-time notification polling.
     * Updates tab badges and shows toasts when new messages/invitations arrive.
     */
    private void startRealTimeNotifications() {
        if (communityNotificationService == null) {
            communityNotificationService = new CommunityNotificationService(currentUser.getId(), 6);
            communityNotificationService.setOnUnreadMessagesChanged((oldCount, newCount) -> {
            if (communityTabs != null) {
                // TODO: Add setTabBadge to TLTabs when framework supports it
                logger.debug("Unread messages badge: {}", newCount);
            }
            if (oldCount >= 0 && newCount > oldCount) {
                int diff = newCount - oldCount;
                showToast("\uD83D\uDCE9  " + I18n.get("notification.new_messages", diff));
                if ("messages".equals(activeTab)) {
                    loadMessagesTab();
                }
            }
        });

        communityNotificationService.setOnPendingConnectionsChanged((oldCount, newCount) -> {
            if (communityTabs != null) {
                // TODO: Add setTabBadge to TLTabs when framework supports it
                logger.debug("Pending connections badge: {}", newCount);
            }
            if (oldCount >= 0 && newCount > oldCount) {
                int diff = newCount - oldCount;
                showToast("\uD83D\uDD14  " + I18n.get("notification.new_invitations", diff));
                if ("connections".equals(activeTab)) {
                    loadConnectionsTab();
                }
            }
        });
        }
        communityNotificationService.start();
        logger.info("Real-time notifications started for user {}", currentUser.getId());
    }

    /**
     * Stop real-time notification polling (called when leaving community view).
     */
    public void stopRealTimeNotifications() {
        if (communityNotificationService != null) {
            communityNotificationService.stop();
            communityNotificationService = null;
        }
        OnlineStatusService.getInstance().stopHeartbeat();
    }

    // ═══════════════════════════════════════════════════════════
    //  POST CRUD — Update & Delete
    // ═══════════════════════════════════════════════════════════

    private void updatePost(Post post) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return PostService.getInstance().update(post);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                logger.info("Post updated successfully");
                showToast(I18n.get("post.success.created"));
                loadFeedTab();
            }
        });
        new Thread(task, "UpdatePostThread").start();
    }

    private void deletePost(Post post) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        Task<Boolean> task = new Task<>() {
                            @Override
                            protected Boolean call() {
                                return PostService.getInstance().delete(post.getId());
                            }
                        };
                        task.setOnSucceeded(e -> {
                            if (task.getValue()) {
                                logger.info("Post deleted");
                                loadFeedTab();
                            }
                        });
                        new Thread(task, "DeletePostThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  EVENT CRUD — Delete
    // ═══════════════════════════════════════════════════════════

    private void deleteEvent(Event event) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            EventService.getInstance().delete(event.getId());
                            Platform.runLater(this::loadEventsTab);
                        }, "DeleteEventThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  GROUP CRUD — Dialog & Delete
    // ═══════════════════════════════════════════════════════════

    private void showGroupDialog(CommunityGroup existingGroup) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingGroup != null;
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") + " Group" : I18n.get("group.new"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField nameField = new TLTextField("Name", "Group name");
        TLTextarea descField = new TLTextarea("Description", "Group description");
        descField.getControl().setPrefRowCount(3);
        TLTextField categoryField = new TLTextField("Category", "e.g., Technology, Finance");

        HBox visibilityRow = new HBox(12);
        visibilityRow.setAlignment(Pos.CENTER_LEFT);
        Label visibilityLabel = new Label("\uD83C\uDF10  Public Group");
        visibilityLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
        javafx.scene.control.CheckBox publicToggle = new javafx.scene.control.CheckBox();
        publicToggle.setSelected(true);
        Label visibilityHint = new Label("Public groups are visible and joinable by everyone");
        visibilityHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
        VBox visibilityInfo = new VBox(2, visibilityLabel, visibilityHint);
        visibilityRow.getChildren().addAll(publicToggle, visibilityInfo);

        publicToggle.selectedProperty().addListener((obs, was, isNow) -> {
            visibilityLabel.setText(isNow ? "\uD83C\uDF10  Public Group" : "\uD83D\uDD12  Private Group");
            visibilityHint.setText(isNow
                    ? "Public groups are visible and joinable by everyone"
                    : "Private groups are hidden from search and discovery");
        });

        if (isEdit) {
            nameField.setText(existingGroup.getName());
            descField.setText(existingGroup.getDescription());
            categoryField.setText(existingGroup.getCategory());
            publicToggle.setSelected(existingGroup.isPublic());
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton saveBtn = new TLButton(I18n.get("common.save"), TLButton.ButtonVariant.PRIMARY);
        saveBtn.setOnAction(e -> {
            String n = nameField.getText();
            if (n == null || n.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"), "Name is required");
                return;
            }

            if (isEdit) {
                existingGroup.setName(n.trim());
                existingGroup.setDescription(descField.getText());
                existingGroup.setCategory(categoryField.getText());
                existingGroup.setPublic(publicToggle.isSelected());
                new Thread(() -> {
                    GroupService.getInstance().update(existingGroup);
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.created"));
                        loadGroupsTab();
                    });
                }, "UpdateGroupThread").start();
            } else {
                CommunityGroup group = new CommunityGroup();
                group.setName(n.trim());
                group.setDescription(descField.getText());
                group.setCategory(categoryField.getText());
                group.setPublic(publicToggle.isSelected());
                group.setCreatorId(currentUser.getId());
                new Thread(() -> {
                    GroupService.getInstance().create(group);
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.created"));
                        loadGroupsTab();
                    });
                }, "CreateGroupThread").start();
            }
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(nameField, descField, categoryField, visibilityRow, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void deleteGroup(CommunityGroup group) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            GroupService.getInstance().delete(group.getId());
                            Platform.runLater(this::loadGroupsTab);
                        }, "DeleteGroupThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  BLOG CRUD — Dialog, Save & Delete
    // ═══════════════════════════════════════════════════════════

    private void showBlogDialog(BlogArticle existingArticle) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingArticle != null;
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") + " Article" : I18n.get("blog.new"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField titleFieldBlog = new TLTextField("Title", "Article title");
        TLTextarea contentField = new TLTextarea("Content", "Write your article...");
        contentField.getControl().setPrefRowCount(8);
        TLTextField summaryField = new TLTextField("Summary", "Short description");
        TLTextField categoryField = new TLTextField("Category", "e.g., Technology, Career");
        TLTextField tagsField = new TLTextField("Tags", "Comma-separated tags");

        if (isEdit) {
            titleFieldBlog.setText(existingArticle.getTitle());
            contentField.setText(existingArticle.getContent());
            summaryField.setText(existingArticle.getSummary());
            categoryField.setText(existingArticle.getCategory());
            tagsField.setText(existingArticle.getTags());
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton draftBtn = new TLButton(I18n.get("blog.draft"), TLButton.ButtonVariant.OUTLINE);
        draftBtn.setOnAction(e -> saveBlogArticle(isEdit, existingArticle, titleFieldBlog, contentField, summaryField,
                categoryField, tagsField, false, dialog));

        TLButton publishBtn = new TLButton(I18n.get("blog.publish"), TLButton.ButtonVariant.PRIMARY);
        publishBtn.setOnAction(e -> saveBlogArticle(isEdit, existingArticle, titleFieldBlog, contentField, summaryField,
                categoryField, tagsField, true, dialog));

        buttons.getChildren().addAll(cancelBtn, draftBtn, publishBtn);
        content.getChildren().addAll(titleFieldBlog, summaryField, contentField, categoryField, tagsField, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    private void saveBlogArticle(boolean isEdit, BlogArticle existing, TLTextField titleField,
            TLTextarea contentField, TLTextField summaryField, TLTextField categoryField,
            TLTextField tagsField, boolean publish, TLDialog<?> dialog) {
        String t = titleField.getText();
        if (t == null || t.trim().isEmpty()) {
            DialogUtils.showError(I18n.get("message.error"), "Title is required");
            return;
        }
        String c = contentField.getText();
        if (c == null || c.trim().isEmpty()) {
            DialogUtils.showError(I18n.get("message.error"), "Content is required");
            return;
        }

        BlogArticle article = isEdit ? existing : new BlogArticle();
        article.setTitle(t.trim());
        article.setContent(c.trim());
        article.setSummary(summaryField.getText());
        article.setCategory(categoryField.getText());
        article.setTags(tagsField.getText());
        article.setPublished(publish);

        if (isEdit) {
            new Thread(() -> {
                BlogService.getInstance().update(article);
                Platform.runLater(() -> {
                    showToast(publish ? I18n.get("blog.success.published") : I18n.get("blog.success.created"));
                    loadBlogTab();
                });
            }, "UpdateBlogThread").start();
        } else {
            article.setAuthorId(currentUser.getId());
            new Thread(() -> {
                BlogService.getInstance().create(article);
                Platform.runLater(() -> {
                    showToast(publish ? I18n.get("blog.success.published") : I18n.get("blog.success.created"));
                    loadBlogTab();
                });
            }, "CreateBlogThread").start();
        }
        dialog.close();
    }

    private void deleteBlog(BlogArticle article) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            BlogService.getInstance().delete(article.getId());
                            Platform.runLater(this::loadBlogTab);
                        }, "DeleteBlogThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    //  AVATAR HELPERS — Colored circular avatars with initials
    // ═══════════════════════════════════════════════════════════

    private static final String[] AVATAR_COLORS = {
            "#3b82f6", "#22c55e", "#a855f7", "#f97316",
            "#ec4899", "#06b6d4", "#ef4444", "#6366f1"
    };

    private StackPane createAvatar(String name, int size) {
        return createAvatar(name, (double) size);
    }

    private StackPane createAvatar(String name, double size) {
        String initials = getInitials(name);
        int colorIndex = Math.abs((name != null ? name : "").hashCode()) % AVATAR_COLORS.length;
        String color = AVATAR_COLORS[colorIndex];

        Circle circle = new Circle(size / 2);
        circle.setFill(Color.web(color));
        circle.setStroke(Color.web(color, 0.3));
        circle.setStrokeWidth(2);

        Text text = new Text(initials);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Inter", FontWeight.BOLD, size * 0.38));

        StackPane avatar = new StackPane(circle, text);
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        avatar.setPrefSize(size, size);
        return avatar;
    }

    private StackPane createAvatar(String name) {
        return createAvatar(name, 42);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    // ═══════════════════════════════════════════════════════════
    //  TOAST NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════

    private void showToast(String message) {
        try {
            if (contentPane.getScene() != null) {
                TLToast.success(contentPane.getScene(), "Community", message);
            }
        } catch (Exception e) {
            logger.debug("Toast not shown: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SEARCH — Advanced multi-entity search bar
    // ═══════════════════════════════════════════════════════════

    private HBox buildSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(8, 0, 12, 0));

        TLTextField searchField = new TLTextField("", "\uD83D\uDD0D  Rechercher dans la communaut\u00e9...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        TLSelect<String> dateFilterSelect = new TLSelect<>("P\u00e9riode",
                "Tout", "Aujourd'hui", "Cette semaine", "Ce mois", "Cette ann\u00e9e");
        dateFilterSelect.setValue("Tout");

        TLSelect<String> sortSelect = new TLSelect<>("Tri", "Plus r\u00e9cent", "Plus ancien");
        sortSelect.setValue("Plus r\u00e9cent");

        Runnable executeSearch = () -> {
            String keyword = searchField.getText();
            if (keyword == null || keyword.isBlank()) return;

            String dateVal = dateFilterSelect.getValue();
            SearchService.DateFilter dateFilter = switch (dateVal != null ? dateVal : "Tout") {
                case "Aujourd'hui" -> SearchService.DateFilter.TODAY;
                case "Cette semaine" -> SearchService.DateFilter.THIS_WEEK;
                case "Ce mois" -> SearchService.DateFilter.THIS_MONTH;
                case "Cette ann\u00e9e" -> SearchService.DateFilter.THIS_YEAR;
                default -> SearchService.DateFilter.ALL;
            };

            boolean sortAscending = "Plus ancien".equals(sortSelect.getValue());

            Task<List<SearchService.SearchResult>> searchTask = new Task<>() {
                @Override
                protected List<SearchService.SearchResult> call() {
                    List<SearchService.SearchResult> all = searchService.search(
                            keyword, SearchService.SearchFilter.ALL, currentUser.getId());
                    List<SearchService.SearchResult> filtered = searchService.filterByDate(all, dateFilter);
                    filtered.sort((a, b) -> {
                        if (a.getDate() == null && b.getDate() == null) return 0;
                        if (a.getDate() == null) return 1;
                        if (b.getDate() == null) return -1;
                        return sortAscending ? a.getDate().compareTo(b.getDate())
                                : b.getDate().compareTo(a.getDate());
                    });
                    return filtered;
                }
            };

            searchTask.setOnSucceeded(ev -> {
                List<SearchService.SearchResult> results = searchTask.getValue();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(searchBar);

                if (results.isEmpty()) {
                    Label noResults = new Label("Aucun r\u00e9sultat pour \"" + keyword + "\"");
                    noResults.getStyleClass().add("text-muted");
                    noResults.setStyle("-fx-font-size: 14px; -fx-padding: 20 0;");
                    contentPane.getChildren().add(noResults);
                } else {
                    Label countLabel = new Label("\uD83D\uDD0D  " + results.size() + " r\u00e9sultat(s) pour \"" + keyword + "\"");
                    countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-foreground;");
                    contentPane.getChildren().add(countLabel);

                    int delay = 0;
                    for (SearchService.SearchResult r : results) {
                        TLCard card = createSearchResultCard(r);
                        contentPane.getChildren().add(card);
                        animateCardEntry(card, delay);
                        delay += 60;
                    }
                }

                TLButton backBtn = new TLButton("\u2190  Retour au feed", TLButton.ButtonVariant.GHOST);
                backBtn.setOnAction(ev2 -> loadFeedTab());
                contentPane.getChildren().add(backBtn);
            });

            new Thread(searchTask, "SearchThread").start();
        };

        TLButton searchBtn = new TLButton("\uD83D\uDD0D  Chercher", TLButton.ButtonVariant.PRIMARY);
        searchBtn.setSize(TLButton.ButtonSize.SM);
        searchBtn.setOnAction(e -> executeSearch.run());

        dateFilterSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (searchField.getText() != null && !searchField.getText().isBlank()) {
                executeSearch.run();
            }
        });
        sortSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (searchField.getText() != null && !searchField.getText().isBlank()) {
                executeSearch.run();
            }
        });

        searchBar.getChildren().addAll(searchField, dateFilterSelect, sortSelect, searchBtn);
        return searchBar;
    }

    private TLCard createSearchResultCard(SearchService.SearchResult result) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(6);
        body.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        TLBadge.Variant badgeVariant = switch (result.getType()) {
            case "POST" -> TLBadge.Variant.DEFAULT;
            case "MESSAGE" -> TLBadge.Variant.SECONDARY;
            case "EVENT" -> TLBadge.Variant.SUCCESS;
            case "GROUP" -> TLBadge.Variant.OUTLINE;
            case "BLOG" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
        TLBadge typeBadge = new TLBadge(result.getType(), badgeVariant);

        Label searchTitleLabel = new Label(result.getTitle());
        searchTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        HBox.setHgrow(searchTitleLabel, Priority.ALWAYS);

        Label dateLabel = new Label(formatDate(result.getDate()));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        header.getChildren().addAll(typeBadge, searchTitleLabel, dateLabel);

        Label excerpt = new Label(result.getExcerpt());
        excerpt.setWrapText(true);
        excerpt.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");

        Label authorLabel = new Label("Par " + (result.getAuthor() != null ? result.getAuthor() : "Unknown"));
        authorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        body.getChildren().addAll(header, excerpt, authorLabel);
        card.setContent(body);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  EMOJI PICKER — Grid of frequently used emojis
    // ═══════════════════════════════════════════════════════════

    private static final String[] EMOJI_LIST = {
            "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06", "\uD83D\uDE05", "\uD83E\uDD23", "\uD83D\uDE02",
            "\uD83D\uDE42", "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83E\uDD70", "\uD83D\uDE0D", "\uD83E\uDD29", "\uD83D\uDE18", "\uD83D\uDE17",
            "\uD83D\uDE1C", "\uD83E\uDD2A", "\uD83D\uDE1D", "\uD83E\uDD11", "\uD83E\uDD17", "\uD83E\uDD14", "\uD83E\uDD10", "\uD83D\uDE10",
            "\uD83D\uDE0F", "\uD83D\uDE12", "\uD83D\uDE14", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE24", "\uD83E\uDD2C", "\uD83D\uDE08",
            "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4F", "\uD83D\uDE4C", "\uD83E\uDD1D", "\uD83D\uDCAA", "\u270C\uFE0F", "\uD83E\uDD1E",
            "\u2764\uFE0F", "\uD83E\uDDE1", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99", "\uD83D\uDC9C", "\uD83D\uDDA4", "\uD83D\uDC94",
            "\uD83D\uDD25", "\u2B50", "\uD83C\uDF1F", "\uD83D\uDCAF", "\u2705", "\u274C", "\u26A1", "\uD83C\uDFAF",
            "\uD83D\uDCCC", "\uD83D\uDCA1", "\uD83D\uDCE2", "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83C\uDFC6", "\uD83E\uDD47", "\uD83D\uDE80"
    };

    private void showEmojiPicker(Node anchor, TLTextarea textArea) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        for (int i = 0; i < EMOJI_LIST.length; i++) {
            String emoji = EMOJI_LIST[i];
            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;");
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setStyle(
                    "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 2; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setStyle(
                    "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;"));
            emojiLabel.setOnMouseClicked(e -> {
                String current = textArea.getText() != null ? textArea.getText() : "";
                textArea.setText(current + emoji);
                popup.hide();
            });
            grid.add(emojiLabel, i % 8, i / 8);
        }

        popup.getContent().add(grid);
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    private void showEmojiPickerForTextField(Node anchor, TLTextField textField) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        for (int i = 0; i < EMOJI_LIST.length; i++) {
            String emoji = EMOJI_LIST[i];
            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;");
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setStyle(
                    "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 2; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setStyle(
                    "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;"));
            emojiLabel.setOnMouseClicked(e -> {
                String current = textField.getText() != null ? textField.getText() : "";
                textField.setText(current + emoji);
                popup.hide();
            });
            grid.add(emojiLabel, i % 8, i / 8);
        }

        popup.getContent().add(grid);
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  TRANSLATION MENU — MyMemory API language choice
    // ═══════════════════════════════════════════════════════════

    private void showTranslationMenu(TLButton translateBtn, Label contentLabel, String originalText) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox menu = new VBox(2);
        menu.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);");
        menu.setMinWidth(180);

        Label title = new Label("\uD83C\uDF10  Traduire en :");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -fx-muted-foreground; -fx-padding: 4 8;");
        menu.getChildren().add(title);

        String detectedLang = translationService.detectLanguage(originalText);

        String[][] languages = {
                { "Fran\u00e7ais", "fr" },
                { "English", "en" },
                { "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", "ar" }
        };

        for (String[] lang : languages) {
            String label = lang[0];
            String langCode = lang[1];

            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(8, 12, 8, 12));
            item.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");

            Label itemLabel = new Label(label);
            itemLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-foreground;");

            if (langCode.equals(detectedLang)) {
                TLBadge currentBadge = new TLBadge("d\u00e9tect\u00e9e", TLBadge.Variant.OUTLINE);
                item.getChildren().addAll(itemLabel, currentBadge);
            } else {
                item.getChildren().add(itemLabel);
            }

            item.setOnMouseEntered(ev -> item.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            item.setOnMouseExited(ev -> item.setStyle(
                    "-fx-cursor: hand; -fx-background-radius: 6;"));

            item.setOnMouseClicked(ev -> {
                popup.hide();
                translateBtn.setText("\u23F3  Traduction...");
                translateBtn.setDisable(true);

                new Thread(() -> {
                    try {
                        String sourceLang = detectedLang;
                        String translated = translationService.translate(originalText, sourceLang, langCode);
                        Platform.runLater(() -> {
                            contentLabel.setText("\uD83C\uDF10 [" + langCode.toUpperCase() + "] " + translated);
                            translateBtn.setText("\u21A9  Original");
                            translateBtn.setDisable(false);
                            translateBtn.setOnAction(ev2 -> {
                                contentLabel.setText(originalText);
                                translateBtn.setText("\uD83C\uDF10  Traduire");
                                translateBtn.setOnAction(ev3 -> showTranslationMenu(translateBtn, contentLabel, originalText));
                            });
                            showToast("Traduit : " + sourceLang.toUpperCase() + " \u2192 " + langCode.toUpperCase());
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            translateBtn.setText("\uD83C\uDF10  Traduire");
                            translateBtn.setDisable(false);
                            showToast("Erreur de traduction : " + ex.getMessage());
                        });
                    }
                }, "TranslateThread").start();
            });

            menu.getChildren().add(item);
        }

        if (!contentLabel.getText().equals(originalText)) {
            menu.getChildren().add(new TLSeparator());
            HBox restoreItem = new HBox(8);
            restoreItem.setAlignment(Pos.CENTER_LEFT);
            restoreItem.setPadding(new Insets(8, 12, 8, 12));
            restoreItem.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");
            Label restoreLabel = new Label("\u21A9  Texte original");
            restoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-muted-foreground;");
            restoreItem.getChildren().add(restoreLabel);
            restoreItem.setOnMouseEntered(ev -> restoreItem.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            restoreItem.setOnMouseExited(ev -> restoreItem.setStyle(
                    "-fx-cursor: hand; -fx-background-radius: 6;"));
            restoreItem.setOnMouseClicked(ev -> {
                popup.hide();
                contentLabel.setText(originalText);
                translateBtn.setText("\uD83C\uDF10  Traduire");
                translateBtn.setOnAction(ev2 -> showTranslationMenu(translateBtn, contentLabel, originalText));
            });
            menu.getChildren().add(restoreItem);
        }

        popup.getContent().add(menu);
        var bounds = translateBtn.localToScreen(translateBtn.getBoundsInLocal());
        if (bounds != null) {
            popup.show(translateBtn, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  @MENTION DETECTION — Autocomplete user mentions
    // ═══════════════════════════════════════════════════════════

    private void setupMentionDetection(TLTextarea textArea, TLDialog<?> dialog) {
        TextArea innerControl = textArea.getControl();
        Popup mentionPopup = new Popup();
        mentionPopup.setAutoHide(true);
        VBox mentionList = new VBox(2);
        mentionList.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        mentionList.setMinWidth(220);
        mentionPopup.getContent().add(mentionList);

        innerControl.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (newText == null || newText.isEmpty()) { mentionPopup.hide(); return; }
                int caretPos = innerControl.getCaretPosition();
                if (caretPos <= 0 || caretPos > newText.length()) { mentionPopup.hide(); return; }

                String textBeforeCaret = newText.substring(0, caretPos);
                int atIndex = textBeforeCaret.lastIndexOf('@');
                if (atIndex < 0) { mentionPopup.hide(); return; }

                String query = textBeforeCaret.substring(atIndex + 1);
                if (query.contains(" ") || query.contains("\n") || query.length() < 1) {
                    mentionPopup.hide(); return;
                }

                final String searchQuery = query;
                final int mentionStart = atIndex;
                final int currentCaretPos = caretPos;
                new Thread(() -> {
                    List<MentionService.UserMention> users = mentionService.searchUsers(searchQuery, 5);
                    Platform.runLater(() -> {
                        mentionList.getChildren().clear();
                        if (users.isEmpty()) { mentionPopup.hide(); return; }
                        for (MentionService.UserMention user : users) {
                            HBox item = new HBox(8);
                            item.setAlignment(Pos.CENTER_LEFT);
                            item.setPadding(new Insets(6, 10, 6, 10));
                            item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;");
                            item.setOnMouseEntered(ev -> item.setStyle(
                                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 4;"));
                            item.setOnMouseExited(ev -> item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;"));

                            StackPane userAvatar = createAvatar(user.getFullName(), 24);
                            Label userName = new Label(user.getFullName() + " (@" + user.getHandle() + ")");
                            userName.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
                            item.getChildren().addAll(userAvatar, userName);

                            item.setOnMouseClicked(ev -> {
                                String currentText = innerControl.getText();
                                String before = currentText.substring(0, mentionStart);
                                String after = currentCaretPos < currentText.length()
                                        ? currentText.substring(currentCaretPos) : "";
                                String replacement = "@" + user.getHandle() + " ";
                                innerControl.setText(before + replacement + after);
                                innerControl.positionCaret(before.length() + replacement.length());
                                mentionPopup.hide();
                            });

                            mentionList.getChildren().add(item);
                        }
                        if (!mentionPopup.isShowing()) {
                            var bounds = innerControl.localToScreen(innerControl.getBoundsInLocal());
                            if (bounds != null) {
                                javafx.stage.Window owner = innerControl.getScene() != null
                                        ? innerControl.getScene().getWindow() : null;
                                if (owner != null) {
                                    mentionPopup.show(owner, bounds.getMinX() + 20, bounds.getMinY() + 40);
                                } else {
                                    mentionPopup.show(innerControl, bounds.getMinX() + 20, bounds.getMinY() + 40);
                                }
                            }
                        }
                    });
                }, "MentionSearchThread").start();
            });
        });
    }

    private void setupMentionDetectionForTextField(TLTextField textField) {
        TextField innerControl = textField.getControl();
        Popup mentionPopup = new Popup();
        mentionPopup.setAutoHide(true);
        VBox mentionList = new VBox(2);
        mentionList.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        mentionList.setMinWidth(220);
        mentionPopup.getContent().add(mentionList);

        innerControl.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (newText == null || newText.isEmpty()) { mentionPopup.hide(); return; }
                int caretPos = innerControl.getCaretPosition();
                if (caretPos <= 0 || caretPos > newText.length()) { mentionPopup.hide(); return; }

                String textBeforeCaret = newText.substring(0, caretPos);
                int atIndex = textBeforeCaret.lastIndexOf('@');
                if (atIndex < 0) { mentionPopup.hide(); return; }

                String query = textBeforeCaret.substring(atIndex + 1);
                if (query.contains(" ") || query.length() < 1) { mentionPopup.hide(); return; }

                final String searchQuery = query;
                final int mentionStart = atIndex;
                final int currentCaretPos = caretPos;
                new Thread(() -> {
                    List<MentionService.UserMention> users = mentionService.searchUsers(searchQuery, 5);
                    Platform.runLater(() -> {
                        mentionList.getChildren().clear();
                        if (users.isEmpty()) { mentionPopup.hide(); return; }
                        for (MentionService.UserMention user : users) {
                            HBox item = new HBox(8);
                            item.setAlignment(Pos.CENTER_LEFT);
                            item.setPadding(new Insets(6, 10, 6, 10));
                            item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;");
                            item.setOnMouseEntered(ev -> item.setStyle(
                                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 4;"));
                            item.setOnMouseExited(ev -> item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;"));

                            StackPane userAvatar = createAvatar(user.getFullName(), 24);
                            Label userName = new Label(user.getFullName() + " (@" + user.getHandle() + ")");
                            userName.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
                            item.getChildren().addAll(userAvatar, userName);

                            item.setOnMouseClicked(ev -> {
                                String currentText = innerControl.getText();
                                String before = currentText.substring(0, mentionStart);
                                String after = currentCaretPos < currentText.length()
                                        ? currentText.substring(currentCaretPos) : "";
                                String replacement = "@" + user.getHandle() + " ";
                                innerControl.setText(before + replacement + after);
                                innerControl.positionCaret(before.length() + replacement.length());
                                mentionPopup.hide();
                            });

                            mentionList.getChildren().add(item);
                        }
                        if (!mentionPopup.isShowing()) {
                            var bounds = innerControl.localToScreen(innerControl.getBoundsInLocal());
                            if (bounds != null) {
                                javafx.stage.Window owner = innerControl.getScene() != null
                                        ? innerControl.getScene().getWindow() : null;
                                double popupX = bounds.getMinX();
                                double popupY = bounds.getMinY() - (users.size() * 36 + 16);
                                if (owner != null) {
                                    mentionPopup.show(owner, popupX, popupY);
                                } else {
                                    mentionPopup.show(innerControl, popupX, popupY);
                                }
                            }
                        }
                    });
                }, "MentionSearchThread").start();
            });
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  ANIMATIONS — Smooth card entry transitions
    // ═══════════════════════════════════════════════════════════

    private void animateCardEntry(Node node, int delay) {
        node.setOpacity(0);
        node.setTranslateY(10);

        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(delay));

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setDelay(Duration.millis(delay));

        fade.play();
        slide.play();
    }
}
