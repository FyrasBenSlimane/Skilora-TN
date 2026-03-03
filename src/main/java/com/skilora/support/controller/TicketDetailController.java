package com.skilora.support.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.skilora.support.entity.SupportTicket;
import com.skilora.support.entity.TicketMessage;
import com.skilora.support.entity.UserFeedback;
import com.skilora.support.service.GeminiAIService;
import com.skilora.support.service.TicketMessageService;
import com.skilora.support.service.UserFeedbackService;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the ticket detail view showing full ticket info,
 * message thread, feedback section, and AI suggestion panel.
 * Ported from branch TicketDetailController with full package refactor.
 */
public class TicketDetailController {

    @FXML private Label subjectLabel;
    @FXML private Label statusBadge;
    @FXML private Label metaLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label dateLabel;
    @FXML private VBox messageContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private VBox feedbackSection;
    @FXML private Label feedbackRatingLabel;
    @FXML private Label feedbackCommentLabel;
    @FXML private HBox feedbackActions;
    @FXML private VBox aiSuggestionBox;
    @FXML private Label aiSuggestionLabel;

    private javafx.scene.image.Image qrCodeImage;
    private SupportTicket ticket;
    private final TicketMessageService messageService = TicketMessageService.getInstance();
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();
    private int currentUserId = 1;
    private boolean isAdminMode = false;

    /** Callback to navigate back — set by parent controller */
    private Runnable onBackAction;

    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        this.currentUserId = isAdmin ? 2 : 1;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
        subjectLabel.setText(ticket.getSubject());
        statusBadge.setText(ticket.getStatus());

        String qrData = "SKILORA Support\nTicket ID: #" + ticket.getId();
        generateQRCode(qrData);

        statusBadge.getStyleClass().add("status-" + ticket.getStatus().toLowerCase().replace("_", "-"));
        metaLabel.setText("Category: " + ticket.getCategory() + " | Priority: " + ticket.getPriority());
        descriptionLabel.setText(ticket.getDescription());

        if (ticket.getCreatedDate() != null) {
            dateLabel.setText("Created: " + ticket.getCreatedDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        loadMessages();
        loadFeedback();
    }

    private void loadFeedback() {
        try {
            UserFeedback feedback = feedbackService.findByTicketId(ticket.getId());
            if (feedback != null) {
                feedbackSection.setVisible(true);
                feedbackSection.setManaged(true);
                feedbackRatingLabel.setText(feedback.getRating() + "/5");
                feedbackCommentLabel.setText(feedback.getComment());
                feedbackActions.setVisible(!isAdminMode);
                feedbackActions.setManaged(!isAdminMode);
            } else {
                feedbackSection.setVisible(false);
                feedbackSection.setManaged(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateQRCode(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            this.qrCodeImage = SwingFXUtils.toFXImage(image, null);
        } catch (WriterException e) {
            System.err.println("QR Code generation failed: " + e.getMessage());
        }
    }

    private void loadMessages() {
        messageContainer.getChildren().clear();
        List<TicketMessage> messages = messageService.findByTicketId(ticket.getId());
        for (TicketMessage msg : messages) {
            addMessageToUI(msg);
        }
        scrollPane.setVvalue(1.0);
    }

    private void addMessageToUI(TicketMessage msg) {
        VBox msgBox = new VBox(5);
        msgBox.setMaxWidth(350);
        msgBox.setPadding(new Insets(10));

        Label content = new Label(msg.getMessage());
        content.setWrapText(true);

        Label date = new Label(msg.getCreatedDate() != null
                ? msg.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                : "");
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        HBox wrapper = new HBox();
        if (msg.getSenderId() == currentUserId) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            msgBox.setStyle("-fx-background-color: #1E40AF; -fx-background-radius: 15 15 0 15;");
            content.setStyle("-fx-text-fill: white;");
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            msgBox.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 15 15 15 0;");
            content.setStyle("-fx-text-fill: #1F2937;");
        }
        msgBox.getChildren().addAll(content, date);

        if (isAdminMode) {
            msgBox.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem editItem = new MenuItem("Edit");
                    MenuItem deleteItem = new MenuItem("Delete");
                    editItem.setOnAction(e -> handleEditMessage(msg));
                    deleteItem.setOnAction(e -> handleDeleteMessage(msg));
                    contextMenu.getItems().addAll(editItem, deleteItem);
                    contextMenu.show(msgBox, event.getScreenX(), event.getScreenY());
                }
            });
            msgBox.setStyle(msgBox.getStyle() + "-fx-cursor: hand;");
        }

        wrapper.getChildren().add(msgBox);
        messageContainer.getChildren().add(wrapper);
    }

    private void handleEditMessage(TicketMessage msg) {
        TextInputDialog dialog = new TextInputDialog(msg.getMessage());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit message content");
        dialog.setContentText("Content:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newContent -> {
            msg.setMessage(newContent);
            if (messageService.updateMessage(msg)) {
                loadMessages();
            }
        });
    }

    private void handleDeleteMessage(TicketMessage msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Message");
        alert.setHeaderText("Delete this message?");
        alert.setContentText("This action cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (messageService.delete(msg.getId())) {
                loadMessages();
            }
        }
    }

    @FXML
    private void handleAISuggestion() {
        new Thread(() -> {
            try {
                List<TicketMessage> messages = messageService.findByTicketId(ticket.getId());
                String convThread = buildConversationThreadForAI(messages, ticket.getUserId());
                String suggestion = GeminiAIService.getInstance().suggestReplyWithContext(
                        ticket.getSubject() != null ? ticket.getSubject() : "",
                        ticket.getDescription() != null ? ticket.getDescription() : "",
                        ticket.getCategory(),
                        convThread);
                Platform.runLater(() -> {
                    if (suggestion != null && !suggestion.isBlank() && !suggestion.startsWith("Error")) {
                        aiSuggestionBox.setVisible(true);
                        aiSuggestionBox.setManaged(true);
                        aiSuggestionLabel.setText(suggestion);
                    } else if (suggestion != null && suggestion.startsWith("Error")) {
                        System.err.println("Gemini Suggestion Error: " + suggestion);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String buildConversationThreadForAI(List<TicketMessage> messages, int ticketUserId) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (TicketMessage m : messages) {
            if (m.isInternal()) continue;
            String who = (ticketUserId > 0 && m.getSenderId() == ticketUserId) ? "User" : "Support";
            String text = m.getMessage() != null ? m.getMessage().trim() : "";
            if (text.isEmpty()) continue;
            sb.append(who).append(": ").append(text).append("\n");
        }
        return sb.toString().trim();
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        TicketMessage msg = new TicketMessage(ticket.getId(), currentUserId, text, false);
        if (messageService.addMessage(msg) > 0) {
            messageInput.clear();
            loadMessages();
        }
    }

    @FXML
    private void handleEditFeedback() {
        try {
            UserFeedback feedback = feedbackService.findByTicketId(ticket.getId());
            if (feedback == null) return;

            TextInputDialog dialog = new TextInputDialog(feedback.getComment());
            dialog.setTitle("Edit Feedback");
            dialog.setHeaderText("Edit your comment");
            dialog.setContentText("Comment:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newComment -> {
                feedback.setComment(newComment);
                feedbackService.update(feedback);
                loadFeedback();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Feedback");
        alert.setHeaderText("Delete your feedback?");
        alert.setContentText("This will remove your rating and comment.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                UserFeedback feedback = feedbackService.findByTicketId(ticket.getId());
                if (feedback != null) {
                    feedbackService.delete(feedback.getId());
                    loadFeedback();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBack() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    @FXML
    private void handleShowQR() {
        try {
            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle("-fx-background-color: #111827; -fx-background-radius: 15; "
                    + "-fx-border-color: #4F46E5; -fx-border-width: 2; -fx-border-radius: 15;");

            Label title = new Label("SKILORA");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #4F46E5;");

            Label subTitle = new Label("Support Ticket QR");
            subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");

            StackPane qrContainer = new StackPane();
            qrContainer.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12;");
            ImageView popupQrView = new ImageView(qrCodeImage);
            popupQrView.setFitWidth(200);
            popupQrView.setFitHeight(200);
            qrContainer.getChildren().add(popupQrView);

            Button closeBtn = new Button("Close");
            closeBtn.getStyleClass().add("btn-secondary");
            closeBtn.setOnAction(e -> ((javafx.stage.Stage) closeBtn.getScene().getWindow()).close());

            root.getChildren().addAll(title, subTitle, qrContainer, closeBtn);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setTitle("Skilora QR Code");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCloseAISuggestion() {
        aiSuggestionBox.setVisible(false);
        aiSuggestionBox.setManaged(false);
    }

    @FXML
    private void handleViewWebPage() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://skilora.tn"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
