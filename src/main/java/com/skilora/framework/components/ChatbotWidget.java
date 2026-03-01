package com.skilora.framework.components;

import com.skilora.model.entity.formation.Training;
import com.skilora.service.ai.AIService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chatbot widget with floating button and chat window
 */
public class ChatbotWidget extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotWidget.class);
    
    private final AIService aiService;
    private final List<Training> formations;
    
    private Button chatButton;
    private VBox chatWindow;
    private ScrollPane chatScrollPane;
    private VBox chatMessages;
    private TextField messageInput;
    private Button sendButton;
    private VBox suggestionsBox;
    private Button newConversationButton;
    private boolean isOpen = false;
    private boolean isFirstOpen = true;
    private ImageView botAvatar;
    private Circle userAvatar;
    private Timeline typingAnimation;
    private ScaleTransition idlePulseAnimation;
    private VBox welcomeMessageBox;
    private String sessionId;
    private Label characterCountLabel;
    private static final int MAX_INPUT_LENGTH = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public ChatbotWidget(List<Training> formations) {
        this.formations = formations != null ? formations : new ArrayList<>();
        this.aiService = AIService.getInstance();
        
        // Initialize session ID for conversation memory
        this.sessionId = aiService.getSessionId();
        
        logger.info("ChatbotWidget initialized with {} formations, AI service configured: {}", 
                   this.formations.size(), aiService.isConfigured());
        
        getStyleClass().add("chatbot-widget");
        setAlignment(Pos.BOTTOM_RIGHT);
        
        // Ensure widget is visible and can receive mouse events
        setVisible(true);
        setManaged(true);
        setMouseTransparent(false);
        
        createChatButton();
        createChatWindow();
        
        // Ensure button is visible and clickable
        chatButton.setVisible(true);
        chatButton.setManaged(true);
        chatButton.setDisable(false);
        
        getChildren().add(chatButton);
        
        // Allow clicks on the button, but let clicks pass through empty areas
        setPickOnBounds(false);
        
        // Ensure widget itself is properly sized and positioned
        // Add premium spacing from screen edges (24px bottom and right)
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        
        // Premium spacing: ensure button has proper margin from edges
        setPadding(new Insets(0, 24, 24, 0));
    }
    
    private void createChatButton() {
        // Create premium circular floating button
        chatButton = new Button("💬");
        chatButton.getStyleClass().add("chatbot-button");
        
        // Perfect circular shape with premium dimensions
        chatButton.setMinSize(64, 64);
        chatButton.setMaxSize(64, 64);
        chatButton.setPrefSize(64, 64);
        
        // Premium styling: subtle radial gradient, soft glow, layered shadows
        chatButton.setStyle(
            "-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #52525b 0%, #3f3f46 50%, #27272a 100%); " +
            "-fx-text-fill: #fafafa; " +
            "-fx-font-size: 28px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 0; " +
            "-fx-background-radius: 50%; " +
            "-fx-border-radius: 50%; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center; " +
            "-fx-content-display: center; " +
            // Layered premium shadows for depth
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 4), " +
            "           dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2), " +
            "           innershadow(gaussian, rgba(255,255,255,0.05), 8, 0, 0, 0); " +
            // Subtle border for definition
            "-fx-border-color: rgba(255,255,255,0.08); " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 50%;"
        );
        
        chatButton.setOnAction(e -> toggleChatWindow());
        
        // Premium hover effect with smooth scale transition
        ScaleTransition hoverScale = new ScaleTransition(Duration.millis(200), chatButton);
        hoverScale.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        
        TranslateTransition hoverLift = new TranslateTransition(Duration.millis(200), chatButton);
        hoverLift.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        
        chatButton.setOnMouseEntered(e -> {
            // Stop idle pulse when hovering
            if (idlePulseAnimation != null) {
                idlePulseAnimation.stop();
            }
            
            // Enhanced hover style with brighter gradient and stronger glow
            chatButton.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #71717a 0%, #52525b 50%, #3f3f46 100%); " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-size: 28px; " +
                "-fx-font-weight: 500; " +
                "-fx-padding: 0; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                // Enhanced shadows on hover (lift effect)
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 6), " +
                "           dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 3), " +
                "           innershadow(gaussian, rgba(255,255,255,0.1), 10, 0, 0, 0); " +
                "-fx-border-color: rgba(255,255,255,0.12); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 50%;"
            );
            
            // Smooth scale up and lift
            hoverScale.setToX(1.1);
            hoverScale.setToY(1.1);
            hoverScale.play();
            
            hoverLift.setToY(-2);
            hoverLift.play();
        });
        
        chatButton.setOnMouseExited(e -> {
            // Restore default style
            chatButton.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #52525b 0%, #3f3f46 50%, #27272a 100%); " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 28px; " +
                "-fx-font-weight: 500; " +
                "-fx-padding: 0; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 4), " +
                "           dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2), " +
                "           innershadow(gaussian, rgba(255,255,255,0.05), 8, 0, 0, 0); " +
                "-fx-border-color: rgba(255,255,255,0.08); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 50%;"
            );
            
            // Smooth scale down and return
            hoverScale.setToX(1.0);
            hoverScale.setToY(1.0);
            hoverScale.play();
            
            hoverLift.setToY(0);
            hoverLift.play();
            
            // Restart idle pulse after hover ends
            if (idlePulseAnimation != null && !isOpen) {
                idlePulseAnimation.play();
            }
        });
        
        // Create subtle idle pulse animation (only when chat is closed)
        createIdlePulseAnimation();
    }
    
    private void createIdlePulseAnimation() {
        if (chatButton == null || isOpen) {
            return;
        }
        
        // Stop any existing pulse animation
        if (idlePulseAnimation != null) {
            idlePulseAnimation.stop();
        }
        
        // Create subtle pulse animation (scale 1.0 to 1.03 and back)
        idlePulseAnimation = new ScaleTransition(Duration.millis(2000), chatButton);
        idlePulseAnimation.setFromX(1.0);
        idlePulseAnimation.setFromY(1.0);
        idlePulseAnimation.setToX(1.03);
        idlePulseAnimation.setToY(1.03);
        idlePulseAnimation.setInterpolator(Interpolator.EASE_BOTH);
        idlePulseAnimation.setAutoReverse(true);
        idlePulseAnimation.setCycleCount(Timeline.INDEFINITE);
        
        // Start pulse animation after a delay (only when chat is closed)
        PauseTransition delay = new PauseTransition(Duration.millis(3000));
        delay.setOnFinished(e -> {
            if (!isOpen && chatButton != null && idlePulseAnimation != null) {
                idlePulseAnimation.play();
            }
        });
        delay.play();
    }
    
    private void createChatWindow() {
        chatWindow = new VBox(0);
        chatWindow.getStyleClass().add("chatbot-window");
        chatWindow.setStyle(
            "-fx-background-color: #18181b; " +
            "-fx-background-radius: 6px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 25, 0, 0, 10); " +
            "-fx-pref-width: 420px; " +
            "-fx-pref-height: 650px; " +
            "-fx-max-width: 420px; " +
            "-fx-max-height: 650px; " +
            "-fx-border-color: #27272a; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px;"
        );
        chatWindow.setVisible(false);
        chatWindow.setManaged(false);
        
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: #27272a; " +
            "-fx-background-radius: 6px 6px 0 0; " +
            "-fx-padding: 18px 20px; " +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4); " +
            "-fx-border-color: #3f3f46; " +
            "-fx-border-width: 0 0 1px 0;"
        );
        
        // Bot avatar in header
        botAvatar = createBotAvatar();
        
        Label title = new Label("Assistant Formations");
        title.setStyle(
            "-fx-text-fill: #fafafa; " +
            "-fx-font-size: 19px; " +
            "-fx-font-weight: 600; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
        );
        title.setMaxWidth(Double.MAX_VALUE);
        title.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(title, Priority.NEVER);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // New conversation button
        newConversationButton = new Button("🔄 Nouvelle conversation");
        newConversationButton.setStyle(
            "-fx-background-color: #3f3f46; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-font-size: 12.5px; " +
            "-fx-font-weight: 500; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 8px 14px; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: #52525b; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px;"
        );
        newConversationButton.setOnMouseEntered(e -> {
            newConversationButton.setStyle(
                "-fx-background-color: #52525b; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 12.5px; " +
                "-fx-font-weight: 500; " +
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8px 14px; " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: #71717a; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-scale-x: 1.05; " +
                "-fx-scale-y: 1.05;"
            );
        });
        newConversationButton.setOnMouseExited(e -> {
            newConversationButton.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 12.5px; " +
                "-fx-font-weight: 500; " +
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8px 14px; " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: #52525b; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        newConversationButton.setOnAction(e -> resetChat());
        
        Button closeButton = new Button("✕");
        closeButton.setStyle(
            "-fx-background-color: #3f3f46; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 6px 10px; " +
            "-fx-background-radius: 50px; " +
            "-fx-min-width: 32px; " +
            "-fx-min-height: 32px; " +
            "-fx-max-width: 32px; " +
            "-fx-max-height: 32px; " +
            "-fx-border-color: #52525b; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 50px;"
        );
        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle(
                "-fx-background-color: #52525b; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 6px 10px; " +
                "-fx-background-radius: 50px; " +
                "-fx-min-width: 32px; " +
                "-fx-min-height: 32px; " +
                "-fx-max-width: 32px; " +
                "-fx-max-height: 32px; " +
                "-fx-border-color: #71717a; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 50px; " +
                "-fx-scale-x: 1.1; " +
                "-fx-scale-y: 1.1;"
            );
        });
        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 6px 10px; " +
                "-fx-background-radius: 50px; " +
                "-fx-min-width: 32px; " +
                "-fx-min-height: 32px; " +
                "-fx-max-width: 32px; " +
                "-fx-max-height: 32px; " +
                "-fx-border-color: #52525b; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 50px; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        closeButton.setOnAction(e -> toggleChatWindow());
        
        header.getChildren().addAll(botAvatar, title, spacer, newConversationButton, closeButton);
        
        // Chat messages area
        chatMessages = new VBox(14);
        chatMessages.setStyle("-fx-padding: 20px;");
        chatMessages.setFillWidth(true);
        
        chatScrollPane = new ScrollPane(chatMessages);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setFitToHeight(true);
        chatScrollPane.setStyle(
            "-fx-background: transparent; " +
            "-fx-background-color: #09090b; " +
            "-fx-border-width: 0;"
        );
        
        // Custom scrollbar styling for dark theme
        Platform.runLater(() -> {
            Node scrollBar = chatScrollPane.lookup(".scroll-bar:vertical");
            if (scrollBar != null) {
                scrollBar.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-pref-width: 8px;"
                );
            }
            Node thumb = chatScrollPane.lookup(".scroll-bar:vertical .thumb");
            if (thumb != null) {
                thumb.setStyle(
                    "-fx-background-color: #3f3f46; " +
                    "-fx-background-radius: 4px; " +
                    "-fx-background-insets: 2px;"
                );
            }
        });
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setPannable(false);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        
        // Auto-scroll to bottom when content changes
        chatMessages.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                chatScrollPane.setVvalue(1.0);
            });
        });
        
        // Create welcome message box (shown on first open)
        createWelcomeMessage();
        
        // Suggestions box (shown after welcome message is dismissed)
        suggestionsBox = new VBox(8);
        suggestionsBox.setStyle("-fx-padding: 16px;");
        suggestionsBox.setVisible(false);
        suggestionsBox.setManaged(false);
        
        Label suggestionsLabel = new Label("Questions suggérées :");
        suggestionsLabel.setStyle(
            "-fx-font-weight: 600; " +
            "-fx-font-size: 15px; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-padding: 0 0 8px 0;"
        );
        
        VBox suggestionsList = new VBox(6);
        suggestionsList.setSpacing(6);
        
        // Add formation name buttons as suggestions
        if (formations != null && !formations.isEmpty()) {
            int count = Math.min(formations.size(), 5); // Show max 5 suggestions
            for (int i = 0; i < count; i++) {
                Training training = formations.get(i);
                Button suggestionBtn = new Button("💡 " + training.getTitle());
                suggestionBtn.setStyle(
                    "-fx-background-color: #27272a; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 12px 18px; " +
                    "-fx-cursor: hand; " +
                    "-fx-alignment: center-left; " +
                    "-fx-max-width: infinity; " +
                    "-fx-font-size: 13.5px; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-border-color: #3f3f46; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px;"
                );
                suggestionBtn.setOnMouseEntered(e -> {
                    suggestionBtn.setStyle(
                        "-fx-background-color: #3f3f46; " +
                        "-fx-text-fill: #fafafa; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-padding: 12px 18px; " +
                        "-fx-cursor: hand; " +
                        "-fx-alignment: center-left; " +
                        "-fx-max-width: infinity; " +
                        "-fx-font-size: 13.5px; " +
                        "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                        "-fx-border-color: #52525b; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-scale-x: 1.02; " +
                        "-fx-scale-y: 1.02;");
                });
                suggestionBtn.setOnMouseExited(e -> {
                    suggestionBtn.setStyle(
                        "-fx-background-color: #27272a; " +
                        "-fx-text-fill: #fafafa; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-padding: 12px 18px; " +
                        "-fx-cursor: hand; " +
                        "-fx-alignment: center-left; " +
                        "-fx-max-width: infinity; " +
                        "-fx-font-size: 13.5px; " +
                        "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                        "-fx-border-color: #3f3f46; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-scale-x: 1.0; " +
                        "-fx-scale-y: 1.0;");
                });
                
                String question = "Parlez-moi de la formation " + training.getTitle();
                suggestionBtn.setOnAction(e -> {
                    suggestionsBox.setVisible(false);
                    suggestionsBox.setManaged(false);
                    sendMessage(question);
                });
                suggestionsList.getChildren().add(suggestionBtn);
            }
        }
        
        // Add common questions
        String[] commonQuestions = {
            "Quelles formations sont disponibles ?",
            "Quelles formations sont gratuites ?",
            "Quelles formations pour débutants ?"
        };
        
        for (String question : commonQuestions) {
            Button suggestionBtn = new Button("❓ " + question);
            suggestionBtn.setStyle(
                "-fx-background-color: #27272a; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 10px 16px; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center-left; " +
                "-fx-max-width: infinity; " +
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-border-color: #3f3f46; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px;"
            );
            suggestionBtn.setOnMouseEntered(e -> {
                suggestionBtn.setStyle(
                    "-fx-background-color: #3f3f46; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 10px 16px; " +
                    "-fx-cursor: hand; " +
                    "-fx-alignment: center-left; " +
                    "-fx-max-width: infinity; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-border-color: #52525b; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px;");
            });
            suggestionBtn.setOnMouseExited(e -> {
                suggestionBtn.setStyle(
                    "-fx-background-color: #27272a; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 10px 16px; " +
                    "-fx-cursor: hand; " +
                    "-fx-alignment: center-left; " +
                    "-fx-max-width: infinity; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-border-color: #3f3f46; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px;");
            });
            suggestionBtn.setOnAction(e -> {
                suggestionsBox.setVisible(false);
                suggestionsBox.setManaged(false);
                sendMessage(question);
            });
            suggestionsList.getChildren().add(suggestionBtn);
        }
        
        suggestionsBox.getChildren().addAll(suggestionsLabel, suggestionsList);
        
        // Input area
        VBox inputContainer = new VBox(4);
        inputContainer.setStyle(
            "-fx-padding: 16px 18px; " +
            "-fx-background-color: #18181b; " +
            "-fx-background-radius: 0 0 6px 6px; " +
            "-fx-border-color: #27272a; " +
            "-fx-border-width: 1px 0 0 0;"
        );
        
        HBox inputArea = new HBox(8);
        inputArea.setAlignment(Pos.CENTER);
        
        messageInput = new TextField();
        messageInput.setPromptText("Tapez votre question...");
        messageInput.getStyleClass().add("chatbot-input");
        messageInput.setStyle(
            "-fx-background-color: #27272a; " +
            "-fx-background-radius: 6px; " +
            "-fx-padding: 12px 18px; " +
            "-fx-border-width: 1px; " +
            "-fx-border-color: #3f3f46; " +
            "-fx-border-radius: 6px; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-font-size: 14.5px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
        );
        
        // Focus effect
        messageInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                messageInput.setStyle(
                    "-fx-background-color: #27272a; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 12px 18px; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-color: #52525b; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-font-size: 14.5px; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
                );
            } else {
                messageInput.setStyle(
                    "-fx-background-color: #27272a; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 12px 18px; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-color: #3f3f46; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-font-size: 14.5px; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
                );
            }
        });
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        // Set placeholder text color using CSS
        try {
            String cssUrl = getClass().getResource("/com/skilora/ui/styles/chatbot-input.css").toExternalForm();
            messageInput.getStylesheets().add(cssUrl);
        } catch (Exception e) {
            logger.warn("Could not load chatbot-input.css, using inline styles only", e);
        }
        
        // Character counter
        characterCountLabel = new Label("0 / " + MAX_INPUT_LENGTH);
        characterCountLabel.setStyle(
            "-fx-text-fill: #a1a1aa; " +
            "-fx-font-size: 11px; " +
            "-fx-padding: 0 8px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
        );
        
        // Add text change listener to update counter and enforce limit
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > MAX_INPUT_LENGTH) {
                messageInput.setText(oldVal);
            } else {
                int length = newVal != null ? newVal.length() : 0;
                characterCountLabel.setText(length + " / " + MAX_INPUT_LENGTH);
                
                // Change color when approaching limit
                if (length > MAX_INPUT_LENGTH * 0.9) {
                    characterCountLabel.setStyle(
                        "-fx-text-fill: #ef4444; " +
                        "-fx-font-size: 11px; " +
                        "-fx-padding: 0 8px; " +
                        "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
                    );
                } else if (length > MAX_INPUT_LENGTH * 0.75) {
                    characterCountLabel.setStyle(
                        "-fx-text-fill: #f59e0b; " +
                        "-fx-font-size: 11px; " +
                        "-fx-padding: 0 8px; " +
                        "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
                    );
                } else {
                    characterCountLabel.setStyle(
                        "-fx-text-fill: #a1a1aa; " +
                        "-fx-font-size: 11px; " +
                        "-fx-padding: 0 8px; " +
                        "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
                    );
                }
            }
        });
        
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
        
        sendButton = new Button("➤");
        sendButton.setStyle(
            "-fx-background-color: #3f3f46; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-background-radius: 6px; " +
            "-fx-padding: 12px 18px; " +
            "-fx-cursor: hand; " +
            "-fx-font-size: 18px; " +
            "-fx-border-color: #52525b; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px;"
        );
        sendButton.setOnAction(e -> sendMessage());
        
        sendButton.setOnMouseEntered(e -> {
            sendButton.setStyle(
                "-fx-background-color: #52525b; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 12px 18px; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 18px; " +
                "-fx-border-color: #71717a; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-scale-x: 1.05; " +
                "-fx-scale-y: 1.05;");
        });
        sendButton.setOnMouseExited(e -> {
            sendButton.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 12px 18px; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 18px; " +
                "-fx-border-color: #52525b; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;");
        });
        
        inputArea.getChildren().addAll(messageInput, sendButton);
        
        // Character counter row
        HBox counterRow = new HBox();
        counterRow.setAlignment(Pos.CENTER_RIGHT);
        counterRow.setPadding(new Insets(0, 16, 0, 0));
        counterRow.getChildren().add(characterCountLabel);
        
        inputContainer.getChildren().addAll(inputArea, counterRow);
        
        chatWindow.getChildren().addAll(header, chatScrollPane, inputContainer);
    }
    
    private void toggleChatWindow() {
        isOpen = !isOpen;
        
        if (isOpen) {
            getChildren().add(chatWindow);
            chatWindow.setVisible(true);
            chatWindow.setManaged(true);
            
            // Smooth animation: slide up, fade in, and scale
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(400), chatWindow);
            slideUp.setFromY(150);
            slideUp.setToY(0);
            slideUp.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), chatWindow);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), chatWindow);
            scaleIn.setFromX(0.9);
            scaleIn.setFromY(0.9);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);
            scaleIn.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            
            // Combine animations for smooth effect
            ParallelTransition openAnimation = new ParallelTransition(slideUp, fadeIn, scaleIn);
            openAnimation.play();
            
            // Show welcome message on first open
            if (isFirstOpen) {
                Platform.runLater(() -> {
                    showWelcomeMessage();
                    isFirstOpen = false;
                });
            }
            
            // Scroll to bottom and focus input
            Platform.runLater(() -> {
                chatScrollPane.setVvalue(1.0);
                messageInput.requestFocus();
            });
        } else {
            // Smooth animation: slide down, fade out, and scale
            TranslateTransition slideDown = new TranslateTransition(Duration.millis(350), chatWindow);
            slideDown.setFromY(0);
            slideDown.setToY(150);
            slideDown.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(350), chatWindow);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setInterpolator(Interpolator.EASE_IN);
            
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(350), chatWindow);
            scaleOut.setFromX(1.0);
            scaleOut.setFromY(1.0);
            scaleOut.setToX(0.9);
            scaleOut.setToY(0.9);
            scaleOut.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            
            // Combine animations
            ParallelTransition closeAnimation = new ParallelTransition(slideDown, fadeOut, scaleOut);
            closeAnimation.setOnFinished(e -> {
                chatWindow.setVisible(false);
                chatWindow.setManaged(false);
                getChildren().remove(chatWindow);
            });
            closeAnimation.play();
        }
    }
    
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Check character limit
        if (message.length() > MAX_INPUT_LENGTH) {
            message = message.substring(0, MAX_INPUT_LENGTH);
        }
        
        messageInput.clear();
        sendMessage(message);
    }
    
    private void sendMessage(String message) {
        // Hide welcome message if visible
        if (welcomeMessageBox != null && welcomeMessageBox.isVisible()) {
            welcomeMessageBox.setVisible(false);
            welcomeMessageBox.setManaged(false);
        }
        
        // Hide suggestions if visible
        if (suggestionsBox.isVisible()) {
            suggestionsBox.setVisible(false);
            suggestionsBox.setManaged(false);
        }
        
        // Add user message
        addMessage(message, true);
        
        // Disable input while processing
        messageInput.setDisable(true);
        sendButton.setDisable(true);
        
        // Show typing indicator
        VBox typingIndicator = addTypingIndicator();
        
        // Get AI response asynchronously with session memory
        logger.info("Sending message to AI: '{}' (session: {})", message, sessionId);
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Checking if AI service is configured...");
                if (!aiService.isConfigured()) {
                    logger.warn("AI service is not configured");
                    return "SERVICE_UNAVAILABLE";
                }
                logger.debug("AI service is configured, calling sendMessage...");
                logger.info("Sending message with {} formations to AI service", formations != null ? formations.size() : 0);
                if (formations != null && !formations.isEmpty()) {
                    logger.info("First formation: {} (ID: {})", formations.get(0).getTitle(), formations.get(0).getId());
                }
                String response = aiService.sendMessage(message, formations, sessionId);
                logger.info("AI service returned response (length: {})", response != null ? response.length() : 0);
                return response;
            } catch (Exception e) {
                logger.error("Exception in CompletableFuture when getting AI response", e);
                return "SERVICE_ERROR";
            }
        });
        
        future.thenAccept(response -> {
            Platform.runLater(() -> {
                logger.info("Processing AI response in JavaFX thread...");
                
                // Stop and remove typing indicator
                if (typingAnimation != null) {
                    typingAnimation.stop();
                }
                chatMessages.getChildren().remove(typingIndicator);
                
                // Check if service is unavailable or error occurred
                if (response == null) {
                    logger.error("Response is null");
                    String errorMessage = "Désolé, le service d'assistance est temporairement indisponible. " +
                                        "Veuillez réessayer dans quelques instants. " +
                                        "Si le problème persiste, n'hésitez pas à contacter notre support.";
                    addMessage(errorMessage, false);
                } else if ("SERVICE_UNAVAILABLE".equals(response)) {
                    logger.warn("Service unavailable");
                    String errorMessage = "Désolé, le service d'assistance est temporairement indisponible. " +
                                        "Veuillez réessayer dans quelques instants. " +
                                        "Si le problème persiste, n'hésitez pas à contacter notre support.";
                    addMessage(errorMessage, false);
                } else if ("SERVICE_ERROR".equals(response)) {
                    logger.error("Service error occurred");
                    String errorMessage = "Désolé, le service d'assistance est temporairement indisponible. " +
                                        "Veuillez réessayer dans quelques instants. " +
                                        "Si le problème persiste, n'hésitez pas à contacter notre support.";
                    addMessage(errorMessage, false);
                } else if (response.contains("n'est pas configuré") || response.contains("n'est pas configuré")) {
                    logger.warn("Service not configured (detected in response)");
                    String errorMessage = "Désolé, le service d'assistance est temporairement indisponible. " +
                                        "Veuillez réessayer dans quelques instants. " +
                                        "Si le problème persiste, n'hésitez pas à contacter notre support.";
                    addMessage(errorMessage, false);
                } else {
                    // Add AI response
                    logger.info("Adding AI response to chat (length: {})", response.length());
                    logger.info("AI response content: {}", response);
                    logger.info("AI response first 100 chars: {}", response.length() > 100 ? response.substring(0, 100) + "..." : response);
                    addMessage(response, false);
                }
                
                // Re-enable input
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                messageInput.requestFocus();
                
                // Scroll to bottom
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            });
        }).exceptionally(throwable -> {
            logger.error("==========================================");
            logger.error("EXCEPTION in CompletableFuture - Chatbot Error");
            logger.error("==========================================");
            logger.error("Exception type: {}", throwable != null ? throwable.getClass().getName() : "null");
            logger.error("Exception message: {}", throwable != null ? throwable.getMessage() : "null");
            if (throwable != null && throwable.getCause() != null) {
                logger.error("Cause: {}", throwable.getCause().getMessage());
            }
            logger.error("Full stack trace:", throwable);
            logger.error("==========================================");
            
            Platform.runLater(() -> {
                // Stop and remove typing indicator
                if (typingAnimation != null) {
                    typingAnimation.stop();
                }
                chatMessages.getChildren().remove(typingIndicator);
                
                String errorMessage = "Désolé, une erreur s'est produite lors de la communication avec le service. " +
                                    "Veuillez réessayer.";
                addMessage(errorMessage, false);
                
                // Re-enable input
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                messageInput.requestFocus();
            });
            return null;
        });
    }
    
    private void addMessage(String text, boolean isUser) {
        VBox messageContainer = new VBox(4);
        messageContainer.setMaxWidth(Double.MAX_VALUE);
        messageContainer.setPadding(new Insets(4, 0, 4, 0));
        
        HBox messageBox = new HBox(8);
        messageBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setMaxWidth(Double.MAX_VALUE);
        
        // Create avatar
        Node avatar = isUser ? createUserAvatar() : (Node)createBotAvatar();
        
        // Message bubble
        VBox messageBubble = new VBox(4);
        messageBubble.setMaxWidth(300);
        
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setPrefWidth(300);
        // Ensure UTF-8 characters are properly displayed
        messageLabel.setText(text); // Explicitly set text to ensure encoding
        messageLabel.setStyle(
            "-fx-background-color: " + (isUser ? "linear-gradient(to right, #3f3f46, #27272a)" : "#27272a") + "; " +
            "-fx-text-fill: " + (isUser ? "#fafafa" : "#fafafa") + "; " +
            "-fx-padding: 14px 18px; " +
            "-fx-background-radius: 6px; " +
            "-fx-font-size: 14.5px; " +
            "-fx-line-spacing: 5px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            (isUser ? "" : "-fx-border-color: #3f3f46; -fx-border-width: 1px; -fx-border-radius: 6px; ") +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 2);"
        );
        
        // Timestamp and rating buttons row (only for bot messages)
        HBox timestampRow = new HBox(8);
        timestampRow.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        timestampRow.setMaxWidth(280);
        
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        Label timestampLabel = new Label(timestamp);
        timestampLabel.setStyle(
            "-fx-text-fill: #a1a1aa; " +
            "-fx-font-size: 11.5px; " +
            "-fx-padding: 4px 10px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
        );
        
        // Add rating buttons only for bot messages
        if (!isUser) {
            HBox ratingButtons = createRatingButtons(messageContainer);
            timestampRow.getChildren().addAll(timestampLabel, ratingButtons);
            HBox.setHgrow(ratingButtons, Priority.ALWAYS);
        } else {
            timestampRow.getChildren().add(timestampLabel);
        }
        
        messageBubble.getChildren().add(messageLabel);
        messageBubble.getChildren().add(timestampRow);
        
        // Add avatar and message bubble in correct order
        if (isUser) {
            messageBox.getChildren().addAll(messageBubble, avatar);
        } else {
            messageBox.getChildren().addAll(avatar, messageBubble);
        }
        
        messageContainer.getChildren().add(messageBox);
        chatMessages.getChildren().add(messageContainer);
        
        // Animation: fade in and slide up
        messageContainer.setOpacity(0);
        messageContainer.setTranslateY(10);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), messageContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), messageContainer);
        slideUp.setFromY(10);
        slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);
        
        ParallelTransition messageAnimation = new ParallelTransition(fadeIn, slideUp);
        messageAnimation.play();
        
        // Scroll to bottom
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
            chatScrollPane.layout();
        });
    }
    
    private VBox addTypingIndicator() {
        VBox typingContainer = new VBox(4);
        typingContainer.setMaxWidth(Double.MAX_VALUE);
        typingContainer.setPadding(new Insets(4, 0, 4, 0));
        
        HBox typingBox = new HBox(8);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        
        // Bot avatar
        ImageView avatar = createBotAvatar();
        
        // Typing bubble with animated dots
        HBox typingBubble = new HBox(6);
        typingBubble.setAlignment(Pos.CENTER_LEFT);
        typingBubble.setStyle(
            "-fx-background-color: #27272a; " +
            "-fx-background-radius: 6px; " +
            "-fx-padding: 14px 18px; " +
            "-fx-border-color: #3f3f46; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px; " +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 2);"
        );
        
        // Create animated dots
        Circle dot1 = new Circle(5, Color.web("#fafafa"));
        Circle dot2 = new Circle(5, Color.web("#fafafa"));
        Circle dot3 = new Circle(5, Color.web("#fafafa"));
        
        typingBubble.getChildren().addAll(dot1, dot2, dot3);
        
        // Animate dots
        typingAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                dot1.setOpacity(0.3);
                dot2.setOpacity(0.3);
                dot3.setOpacity(0.3);
            }),
            new KeyFrame(Duration.millis(400), e -> dot1.setOpacity(1.0)),
            new KeyFrame(Duration.millis(600), e -> {
                dot1.setOpacity(0.3);
                dot2.setOpacity(1.0);
            }),
            new KeyFrame(Duration.millis(800), e -> {
                dot2.setOpacity(0.3);
                dot3.setOpacity(1.0);
            }),
            new KeyFrame(Duration.millis(1000), e -> dot3.setOpacity(0.3))
        );
        typingAnimation.setCycleCount(Timeline.INDEFINITE);
        typingAnimation.play();
        
        typingBox.getChildren().addAll(avatar, typingBubble);
        typingContainer.getChildren().add(typingBox);
        chatMessages.getChildren().add(typingContainer);
        
        // Scroll to bottom
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
            chatScrollPane.layout();
        });
        
        return typingContainer;
    }
    
    private ImageView createBotAvatar() {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);
        avatar.setPreserveRatio(true);
        
        // Try to load Skilora logo
        String[] logoPaths = {
            "/com/skilora/assets/images/skilora-logo.png",
            "/com/skilora/assets/logo.png",
            "/assets/skilora-logo.png",
            "/skilora-logo.png"
        };
        
        Image logoImage = null;
        for (String path : logoPaths) {
            InputStream logoStream = getClass().getResourceAsStream(path);
            if (logoStream != null) {
                try {
                    logoImage = new Image(logoStream);
                    break;
                } catch (Exception e) {
                    logger.debug("Could not load logo from {}", path);
                }
            }
        }
        
        if (logoImage != null) {
            avatar.setImage(logoImage);
        } else {
            // Fallback: create a colored circle with "S" for Skilora
            Circle fallbackAvatar = new Circle(16);
            fallbackAvatar.setFill(Color.web("#667eea"));
            // We'll use a StackPane with Circle and Label for the fallback
            // For now, create a simple colored circle
            avatar.setImage(createFallbackBotAvatarImage());
        }
        
        // Make it circular
        Circle clip = new Circle(16);
        clip.setCenterX(16);
        clip.setCenterY(16);
        avatar.setClip(clip);
        
        return avatar;
    }
    
    private Image createFallbackBotAvatarImage() {
        // Create a simple image programmatically
        Canvas canvas = new Canvas(32, 32);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw circle background
        gc.setFill(Color.web("#667eea"));
        gc.fillOval(0, 0, 32, 32);
        
        // Draw "S" text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText("S", 10, 22);
        
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        return canvas.snapshot(params, null);
    }
    
    private StackPane createUserAvatar() {
        StackPane avatarContainer = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web("#667eea"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2.5);
        circle.setEffect(new javafx.scene.effect.DropShadow(3, Color.web("rgba(0,0,0,0.2)")));
        
        Label userLabel = new Label("U");
        userLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 15px; " +
            "-fx-font-family: 'Segoe UI', 'System', sans-serif;"
        );
        
        avatarContainer.getChildren().addAll(circle, userLabel);
        return avatarContainer;
    }
    
    private void resetChat() {
        // Stop typing animation if running
        if (typingAnimation != null) {
            typingAnimation.stop();
        }
        
        // Clear session memory
        aiService.clearSession(sessionId);
        
        // Create new session ID for new conversation
        sessionId = aiService.getSessionId();
        
        // Clear all messages
        chatMessages.getChildren().clear();
        
        // Show welcome message again
        isFirstOpen = true;
        showWelcomeMessage();
        
        // Scroll to top
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(0.0);
        });
    }
    
    private void createWelcomeMessage() {
        welcomeMessageBox = new VBox(12);
        welcomeMessageBox.setMaxWidth(Double.MAX_VALUE);
        welcomeMessageBox.setPadding(new Insets(4, 0, 4, 0));
        welcomeMessageBox.setFillWidth(true);
        welcomeMessageBox.setVisible(false);
        welcomeMessageBox.setManaged(false);
        
        HBox messageBox = new HBox(8);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setMaxWidth(Double.MAX_VALUE);
        
        // Bot avatar
        ImageView avatar = createBotAvatar();
        
        // Welcome message bubble
        VBox messageBubble = new VBox(8);
        messageBubble.setMaxWidth(280);
        
        Label welcomeLabel = new Label("Bonjour! Je suis votre assistant Skilora Tunisia. Comment puis-je vous aider?");
        welcomeLabel.setWrapText(true);
        welcomeLabel.setMaxWidth(280);
        welcomeLabel.setPrefWidth(280);
        welcomeLabel.setStyle(
            "-fx-background-color: #27272a; " +
            "-fx-text-fill: #fafafa; " +
            "-fx-padding: 14px 18px; " +
            "-fx-background-radius: 6px; " +
            "-fx-font-size: 14.5px; " +
            "-fx-line-spacing: 5px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-border-color: #3f3f46; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px; " +
            "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 2);"
        );
        
        // Timestamp
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        Label timestampLabel = new Label(timestamp);
        timestampLabel.setAlignment(Pos.CENTER_LEFT);
        timestampLabel.setMaxWidth(280);
        timestampLabel.setStyle(
            "-fx-text-fill: #a1a1aa; " +
            "-fx-font-size: 11px; " +
            "-fx-padding: 2px 8px; " +
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif;"
        );
        
        messageBubble.getChildren().addAll(welcomeLabel, timestampLabel);
        messageBox.getChildren().addAll(avatar, messageBubble);
        
        // Quick reply buttons
        VBox quickRepliesBox = new VBox(8);
        quickRepliesBox.setPadding(new Insets(8, 0, 0, 40)); // Indent to align with message
        quickRepliesBox.setMaxWidth(320);
        quickRepliesBox.setMinWidth(280);
        
        String[] quickReplies = {
            "Voir les formations disponibles",
            "Formations pour débutants",
            "Formations en développement",
            "Obtenir un certificat"
        };
        
        for (String reply : quickReplies) {
            Button quickReplyBtn = new Button(reply);
            quickReplyBtn.setMaxWidth(320);
            quickReplyBtn.setMinWidth(280);
            quickReplyBtn.setPrefWidth(280);
            quickReplyBtn.setAlignment(Pos.CENTER_LEFT);
            quickReplyBtn.setWrapText(false);
            quickReplyBtn.setStyle(
                "-fx-background-color: #27272a; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 10px 16px; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 13px; " +
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-border-color: #3f3f46; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px;"
            );
            
            quickReplyBtn.setOnMouseEntered(e -> {
                quickReplyBtn.setStyle(
                    "-fx-background-color: #3f3f46; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 10px 16px; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-border-color: #52525b; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px;"
                );
            });
            
            quickReplyBtn.setOnMouseExited(e -> {
                quickReplyBtn.setStyle(
                    "-fx-background-color: #27272a; " +
                    "-fx-text-fill: #fafafa; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-padding: 10px 16px; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-border-color: #3f3f46; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px;"
                );
            });
            
            quickReplyBtn.setOnAction(e -> {
                // Hide welcome message
                welcomeMessageBox.setVisible(false);
                welcomeMessageBox.setManaged(false);
                // Send the quick reply as a message
                sendMessage(reply);
            });
            
            quickRepliesBox.getChildren().add(quickReplyBtn);
        }
        
        welcomeMessageBox.getChildren().addAll(messageBox, quickRepliesBox);
    }
    
    private void showWelcomeMessage() {
        if (welcomeMessageBox == null) {
            createWelcomeMessage();
        }
        
        // Add welcome message if not already in chat
        if (!chatMessages.getChildren().contains(welcomeMessageBox)) {
            chatMessages.getChildren().add(0, welcomeMessageBox);
        }
        
        welcomeMessageBox.setVisible(true);
        welcomeMessageBox.setManaged(true);
        
        // Reset animation
        welcomeMessageBox.setOpacity(0);
        welcomeMessageBox.setTranslateY(10);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), welcomeMessageBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), welcomeMessageBox);
        slideUp.setFromY(10);
        slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);
        
        ParallelTransition welcomeAnimation = new ParallelTransition(fadeIn, slideUp);
        welcomeAnimation.play();
    }
    
    private HBox createRatingButtons(VBox messageContainer) {
        HBox ratingBox = new HBox(4);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        
        Button thumbsUp = new Button("👍");
        thumbsUp.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #a1a1aa; " +
            "-fx-font-size: 17px; " +
            "-fx-padding: 6px 10px; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 6px; " +
            "-fx-min-width: 36px; " +
            "-fx-min-height: 36px;"
        );
        
        Button thumbsDown = new Button("👎");
        thumbsDown.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #a1a1aa; " +
            "-fx-font-size: 17px; " +
            "-fx-padding: 6px 10px; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 6px; " +
            "-fx-min-width: 36px; " +
            "-fx-min-height: 36px;"
        );
        
        // Thumbs up hover and click
        thumbsUp.setOnMouseEntered(e -> {
            thumbsUp.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px; " +
                "-fx-scale-x: 1.15; " +
                "-fx-scale-y: 1.15;"
            );
        });
        thumbsUp.setOnMouseExited(e -> {
            thumbsUp.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #a1a1aa; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        thumbsUp.setOnAction(e -> {
            thumbsUp.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px;"
            );
            thumbsUp.setDisable(true);
            thumbsDown.setDisable(true);
            logger.debug("User rated message: thumbs up");
        });
        
        // Thumbs down hover and click
        thumbsDown.setOnMouseEntered(e -> {
            thumbsDown.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #ef4444; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px; " +
                "-fx-scale-x: 1.15; " +
                "-fx-scale-y: 1.15;"
            );
        });
        thumbsDown.setOnMouseExited(e -> {
            thumbsDown.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #a1a1aa; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        thumbsDown.setOnAction(e -> {
            thumbsDown.setStyle(
                "-fx-background-color: #3f3f46; " +
                "-fx-text-fill: #ef4444; " +
                "-fx-font-size: 17px; " +
                "-fx-padding: 6px 10px; " +
                "-fx-cursor: hand; " +
                "-fx-background-radius: 6px; " +
                "-fx-min-width: 36px; " +
                "-fx-min-height: 36px;"
            );
            thumbsUp.setDisable(true);
            thumbsDown.setDisable(true);
            logger.debug("User rated message: thumbs down");
        });
        
        ratingBox.getChildren().addAll(thumbsUp, thumbsDown);
        return ratingBox;
    }
}
