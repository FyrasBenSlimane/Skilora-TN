package com.skilora.framework.components;

import com.skilora.formation.entity.Formation;
import com.skilora.formation.service.FormationAIService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Chatbot widget for the Formation module (floating button + chat window).
 * Matches Gestion_FormCertif branch behavior: visible as overlay on the formations page.
 */
public class FormationChatbotWidget extends StackPane {

    private final List<Formation> formations;
    private final FormationAIService aiService;
    private Button chatButton;
    private VBox chatWindow;
    private ScrollPane chatScrollPane;
    private VBox chatMessages;
    private TextField messageInput;
    private Button sendButton;
    private VBox suggestionsBox;
    private boolean isOpen = false;

    public FormationChatbotWidget(List<Formation> formations) {
        this.formations = formations != null ? new ArrayList<>(formations) : new ArrayList<>();
        this.aiService = FormationAIService.getInstance();

        getStyleClass().add("chatbot-widget");
        setAlignment(Pos.BOTTOM_RIGHT);
        setVisible(true);
        setManaged(true);
        setMouseTransparent(false);
        setPadding(new Insets(0, 24, 24, 0));

        createChatButton();
        createChatWindow();
        getChildren().add(chatButton);
        setPickOnBounds(false);
    }

    private void createChatButton() {
        chatButton = new Button("\uD83D\uDCAC");
        chatButton.getStyleClass().add("chatbot-button");
        chatButton.setMinSize(64, 64);
        chatButton.setMaxSize(64, 64);
        chatButton.setPrefSize(64, 64);
        chatButton.setStyle(
            "-fx-background-color: radial-gradient(center 50% 50%, radius 100%, #52525b 0%, #3f3f46 50%, #27272a 100%); " +
            "-fx-text-fill: white; -fx-font-size: 28px; -fx-cursor: hand; -fx-background-radius: 50%; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 4); " +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1px; -fx-border-radius: 50%;"
        );
        chatButton.setOnAction(e -> toggleChatWindow());
    }

    private void createChatWindow() {
        chatWindow = new VBox(0);
        chatWindow.getStyleClass().add("chatbot-window");
        chatWindow.setStyle(
            "-fx-background-color: #18181b; -fx-background-radius: 6px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 25, 0, 0, 10); " +
            "-fx-pref-width: 420px; -fx-pref-height: 550px; -fx-max-width: 420px; -fx-max-height: 550px; " +
            "-fx-border-color: #27272a; -fx-border-width: 1px; -fx-border-radius: 6px;"
        );
        chatWindow.setVisible(false);
        chatWindow.setManaged(false);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #27272a; -fx-background-radius: 6px 6px 0 0; -fx-padding: 16px 20px;");
        header.setMinHeight(Region.USE_PREF_SIZE);
        Label title = new Label("Assistant Formations");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 19px; -fx-font-weight: 600;");
        title.setMinHeight(Region.USE_PREF_SIZE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button newConvBtn = new Button("Nouvelle conversation");
        newConvBtn.setMinHeight(Region.USE_PREF_SIZE);
        newConvBtn.setPrefHeight(40);
        newConvBtn.setStyle(
            "-fx-background-color: #3f3f46; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand; " +
            "-fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-content-display: center; " +
            "-fx-alignment: center; -fx-min-width: 120;"
        );
        newConvBtn.setOnAction(e -> resetChat());
        Button closeBtn = new Button("\u2715");
        closeBtn.setMinSize(36, 36);
        closeBtn.setPrefSize(36, 36);
        closeBtn.setStyle(
            "-fx-background-color: #3f3f46; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; " +
            "-fx-padding: 0; -fx-background-radius: 50%; -fx-content-display: center; -fx-alignment: center;"
        );
        closeBtn.setOnAction(e -> toggleChatWindow());
        header.getChildren().addAll(title, spacer, newConvBtn, closeBtn);

        chatMessages = new VBox(14);
        chatMessages.setStyle("-fx-padding: 20px;");
        chatMessages.setFillWidth(true);

        chatScrollPane = new ScrollPane(chatMessages);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setFitToHeight(true);
        chatScrollPane.setStyle("-fx-background: #09090b; -fx-background-color: transparent; -fx-control-inner-background: #09090b; -fx-border-width: 0;");
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        chatMessages.heightProperty().addListener((o, a, b) -> Platform.runLater(() -> chatScrollPane.setVvalue(1.0)));

        suggestionsBox = new VBox(8);
        suggestionsBox.setStyle("-fx-padding: 16px;");
        Label sugLabel = new Label("Questions suggérées :");
        sugLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 15px; -fx-text-fill: white;");
        sugLabel.setMinHeight(Region.USE_PREF_SIZE);
        VBox suggestionsList = new VBox(6);
        suggestionsList.setFillWidth(true);
        int count = Math.min(formations.size(), 5);
        for (int i = 0; i < count; i++) {
            Formation f = formations.get(i);
            Button btn = new Button("\uD83D\uDCA1 " + (f.getTitle() != null ? f.getTitle() : "Formation"));
            styleSuggestionButton(btn);
            String question = "Parlez-moi de la formation " + (f.getTitle() != null ? f.getTitle() : "");
            btn.setOnAction(e -> { hideSuggestions(); sendMessage(question); });
            suggestionsList.getChildren().add(btn);
        }
        for (String q : new String[]{"Quelles formations sont disponibles ?", "Quelles formations sont gratuites ?", "Quelles formations pour débutants ?"}) {
            Button btn = new Button("\u2753 " + q);
            styleSuggestionButton(btn);
            btn.setOnAction(e -> { hideSuggestions(); sendMessage(q); });
            suggestionsList.getChildren().add(btn);
        }
        suggestionsBox.getChildren().addAll(sugLabel, suggestionsList);

        VBox inputContainer = new VBox(4);
        inputContainer.setStyle("-fx-padding: 16px 18px; -fx-background-color: #18181b; -fx-border-color: #27272a; -fx-border-width: 1px 0 0 0;");
        HBox inputArea = new HBox(8);
        inputArea.setAlignment(Pos.CENTER);
        messageInput = new TextField();
        messageInput.setPromptText("Tapez votre question...");
        messageInput.setMinHeight(Region.USE_PREF_SIZE);
        messageInput.setPrefHeight(44);
        messageInput.setStyle("-fx-background-color: #27272a; -fx-background-radius: 6px; -fx-padding: 8px 12px; -fx-text-fill: white; -fx-font-size: 14px;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        messageInput.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) sendMessage(); });
        sendButton = new Button("\u27A4");
        sendButton.setMinSize(44, 44);
        sendButton.setPrefSize(44, 44);
        sendButton.setStyle(
            "-fx-background-color: #3f3f46; -fx-text-fill: white; -fx-background-radius: 6px; " +
            "-fx-padding: 0; -fx-cursor: hand; -fx-font-size: 18px; -fx-content-display: center; -fx-alignment: center;"
        );
        sendButton.setOnAction(e -> sendMessage());
        inputArea.getChildren().addAll(messageInput, sendButton);
        inputContainer.getChildren().add(inputArea);

        chatWindow.getChildren().addAll(header, chatScrollPane, suggestionsBox, inputContainer);
    }

    private void styleSuggestionButton(Button btn) {
        btn.setMinHeight(Region.USE_PREF_SIZE);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
            "-fx-background-color: #27272a; -fx-text-fill: white; -fx-background-radius: 6px; " +
            "-fx-padding: 10px 14px; -fx-cursor: hand; -fx-alignment: center-left; " +
            "-fx-font-size: 13px; -fx-content-display: left; -fx-graphic-text-gap: 8; " +
            "-fx-wrap-text: true;"
        );
    }

    private void hideSuggestions() {
        suggestionsBox.setVisible(false);
        suggestionsBox.setManaged(false);
    }

    private void toggleChatWindow() {
        isOpen = !isOpen;
        if (isOpen) {
            getChildren().add(chatWindow);
            chatWindow.setVisible(true);
            chatWindow.setManaged(true);
            if (chatMessages.getChildren().isEmpty()) {
                addWelcomeMessage();
            }
            Platform.runLater(() -> { chatScrollPane.setVvalue(1.0); messageInput.requestFocus(); });
        } else {
            chatWindow.setVisible(false);
            chatWindow.setManaged(false);
            getChildren().remove(chatWindow);
        }
    }

    private void addWelcomeMessage() {
        Label welcome = new Label("Bonjour ! Je suis l'assistant Formations. Posez-moi une question sur les formations disponibles.");
        welcome.setWrapText(true);
        welcome.setStyle("-fx-text-fill: #EEEEEE; -fx-font-size: 14px; -fx-padding: 12px;");
        chatMessages.getChildren().add(welcome);
    }

    private void resetChat() {
        chatMessages.getChildren().clear();
        addWelcomeMessage();
        suggestionsBox.setVisible(true);
        suggestionsBox.setManaged(true);
        messageInput.clear();
    }

    private void sendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;
        sendMessage(text.trim());
        messageInput.clear();
    }

    private void sendMessage(String message) {
        hideSuggestions();
        addMessage(message, true);
        messageInput.setDisable(true);
        sendButton.setDisable(true);

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return aiService.answer(message, formations);
            }
        };
        task.setOnSucceeded(e -> {
            String reply = task.getValue();
            if (reply != null && !reply.isEmpty()) addMessage(reply, false);
            messageInput.setDisable(false);
            sendButton.setDisable(false);
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        });
        task.setOnFailed(e -> {
            addMessage("Désolé, une erreur s'est produite. Réessayez.", false);
            messageInput.setDisable(false);
            sendButton.setDisable(false);
        });
        new Thread(task).start();
    }

    private void addMessage(String text, boolean isUser) {
        HBox row = new HBox(8);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(300);
        bubble.setStyle(
            "-fx-background-color: " + (isUser ? "#3f3f46" : "#27272a") + "; " +
            "-fx-text-fill: white; -fx-padding: 14px 18px; -fx-background-radius: 6px; " +
            "-fx-font-size: 14px; -fx-border-color: #3f3f46; -fx-border-width: 1px; -fx-border-radius: 6px;"
        );

        if (isUser) row.getChildren().add(bubble);
        else row.getChildren().add(0, bubble);

        chatMessages.getChildren().add(row);

        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), row);
        tt.setFromY(10);
        tt.setToY(0);
        tt.play();
    }
}
