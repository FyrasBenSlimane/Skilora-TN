package tn.esprit.skylora.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.sql.SQLException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonType;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import tn.esprit.skylora.entities.Feedback;
import tn.esprit.skylora.entities.MessageTicket;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.MessageTicketService;
import tn.esprit.skylora.services.ServiceFeedback;
import tn.esprit.skylora.utils.MyConnection;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TicketDetailController {

    @FXML
    private Label subjectLabel;
    @FXML
    private Label statusBadge;
    @FXML
    private Label metaLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label dateLabel;
    private javafx.scene.image.Image qrCodeImage;
    @FXML
    private VBox messageContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendButton;
    @FXML
    private VBox feedbackSection;
    @FXML
    private Label feedbackRatingLabel;
    @FXML
    private Label feedbackCommentLabel;
    @FXML
    private HBox feedbackActions;
    @FXML
    private VBox aiSuggestionBox;
    @FXML
    private Label aiSuggestionLabel;

    private Ticket ticket;
    private MessageTicketService messageService = new MessageTicketService(MyConnection.getInstance().getConnection());
    private ServiceFeedback serviceFeedback = new ServiceFeedback();
    private int currentUserId = 1; // Simulation: utilisateur connecté
    private boolean isAdminMode = false;

    // Active ou désactive le mode administrateur (permet de modérer les messages)
    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        this.currentUserId = isAdmin ? 2 : 1; // Scenario: 1=User, 2=Admin
    }

    // Affiche les informations du ticket et charge la conversation
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
        subjectLabel.setText(ticket.getSubject());
        statusBadge.setText(ticket.getStatut());
        // Générer le QR Code avec un message personnalisé
        String qrData = "🚀 SKYLORA WEB is coming soon!\n" +
                "Scannez pour découvrir le futur du support.\n" +
                "Ticket ID: #" + ticket.getId();
        generateQRCode(qrData);
        statusBadge.getStyleClass().add("status-" + ticket.getStatut().toLowerCase().replace("_", "-"));
        metaLabel.setText("Catégorie: " + ticket.getCategorie() + " | Priorité: " + ticket.getPriorite());
        descriptionLabel.setText(ticket.getDescription());

        if (ticket.getDateCreation() != null) {
            dateLabel
                    .setText("Créé le : " + ticket.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        loadMessages();
        loadFeedback();
    }

    private void loadFeedback() {
        try {
            Feedback feedback = serviceFeedback.getFeedbackByTicketId(ticket.getId());
            if (feedback != null) {
                feedbackSection.setVisible(true);
                feedbackSection.setManaged(true);
                feedbackRatingLabel.setText(feedback.getRating() + "/5");
                feedbackCommentLabel.setText(feedback.getComment());

                // Only creator can edit/delete. Admin can only see.
                // In this demo, currentUserId is 1 for user and 2 for admin.
                // Assume feedback owner is currentUserId 1.
                feedbackActions.setVisible(!isAdminMode);
                feedbackActions.setManaged(!isAdminMode);
            } else {
                feedbackSection.setVisible(false);
                feedbackSection.setManaged(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Génère un QR code avec les informations spécifiées
    private void generateQRCode(String content) {
        try {
            // Generate QR using ZXing
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            // Convert to BufferedImage then JavaFX Image
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

    // Charge tous les messages de ce ticket depuis la base de données
    private void loadMessages() {
        messageContainer.getChildren().clear();
        List<MessageTicket> messages = messageService.getMessagesByTicketId(ticket.getId());
        for (MessageTicket msg : messages) {
            addMessageToUI(msg);
        }
        scrollPane.setVvalue(1.0);
    }

    // Crée une bulle de message (bleue pour moi, grise pour l'autre) et l'ajoute à
    // l'écran
    private void addMessageToUI(MessageTicket msg) {
        VBox msgBox = new VBox(5);
        msgBox.setMaxWidth(350);
        msgBox.setPadding(new Insets(10));

        Label content = new Label(msg.getContenu());
        content.setWrapText(true);

        Label date = new Label(msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        HBox wrapper = new HBox();
        if (msg.getUtilisateurId() == currentUserId) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            msgBox.setStyle("-fx-background-color: #1E40AF; -fx-background-radius: 15 15 0 15;");
            content.setStyle("-fx-text-fill: white;");
            msgBox.getChildren().addAll(content, date);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
            msgBox.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 15 15 15 0;");
            content.setStyle("-fx-text-fill: #1F2937;");
            msgBox.getChildren().addAll(content, date);
        }

        if (isAdminMode) {
            msgBox.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem editItem = new MenuItem("Modifier");
                    MenuItem deleteItem = new MenuItem("Supprimer");

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

    // Permet de modifier un message existant (clic droit admin)
    private void handleEditMessage(MessageTicket msg) {
        TextInputDialog dialog = new TextInputDialog(msg.getContenu());
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("Modifier le contenu du message");
        dialog.setContentText("Contenu:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newContent -> {
            msg.setContenu(newContent);
            if (messageService.modifierMessage(msg)) {
                loadMessages();
            }
        });
    }

    // Supprime un message (clic droit admin)
    private void handleDeleteMessage(MessageTicket msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le message");
        alert.setHeaderText("Supprimer ce message ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (messageService.supprimerMessage(msg.getId())) {
                loadMessages();
            }
        }
    }
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
////////btn suggestion ai fl message
    @FXML
    private void handleAISuggestion() {
        // Remove restriction to allow users to see suggestions too, or just fix it for
        // everyone
        new Thread(() -> {
            try {
                String suggestion = tn.esprit.skylora.utils.GeminiService.suggestReply(ticket.getSubject(),
                        ticket.getDescription());
                javafx.application.Platform.runLater(() -> {
                    if (suggestion != null && !suggestion.startsWith("Error")) {
                        aiSuggestionBox.setVisible(true);
                        aiSuggestionBox.setManaged(true);
                        aiSuggestionLabel.setText(suggestion);
                    } else {
                        System.err.println("Gemini Suggestion Error or Empty: " + suggestion);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    // Envoie le message écrit dans la zone de texte
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty())
            return;

        MessageTicket msg = new MessageTicket(ticket.getId(), currentUserId, text);
        if (messageService.ajouterMessage(msg)) {
            messageInput.clear();
            loadMessages();
        }
    }

    @FXML
    private void handleEditFeedback() {
        try {
            Feedback feedback = serviceFeedback.getFeedbackByTicketId(ticket.getId());
            if (feedback == null)
                return;

            TextInputDialog dialog = new TextInputDialog(feedback.getComment());
            dialog.setTitle("Modifier Feedback");
            dialog.setHeaderText("Modifier votre commentaire");
            dialog.setContentText("Commentaire:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newComment -> {
                try {
                    feedback.setComment(newComment);
                    serviceFeedback.modifier(feedback);
                    loadFeedback();
                    String qrData = "🚀 SKYLORA WEB is coming soon!\n" +
                            "Scannez pour découvrir le futur du support.\n" +
                            "Ticket ID: #" + ticket.getId();
                    generateQRCode(qrData); // Update QR as it contains info
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer Feedback");
        alert.setHeaderText("Voulez-vous supprimer votre feedback ?");
        alert.setContentText("Cette action supprimera votre note et votre commentaire.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                Feedback feedback = serviceFeedback.getFeedbackByTicketId(ticket.getId());
                if (feedback != null) {
                    serviceFeedback.supprimer(feedback.getId());
                    loadFeedback();
                    String qrData = "🚀 SKYLORA WEB is coming soon!\n" +
                            "Scannez pour découvrir le futur du support.\n" +
                            "Ticket ID: #" + ticket.getId();
                    generateQRCode(qrData);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBack() {
        MainShellController.getInstance().loadView(isAdminMode ? "/tn/esprit/skylora/gui/AdminDashboard.fxml"
                : "/tn/esprit/skylora/gui/UserDashboard.fxml");
    }

    @FXML
    private void handleViewWebPage() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://skylora.tn"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowQR() {
        try {
            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle(
                    "-fx-background-color: #111827; -fx-background-radius: 15; -fx-border-color: #4F46E5; -fx-border-width: 2; -fx-border-radius: 15;");

            Label title = new Label("SKYLORA WEB");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #4F46E5;");

            Label subTitle = new Label("is coming soon");
            subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");

            StackPane qrContainer = new StackPane();
            qrContainer.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12;");

            ImageView popupQrView = new ImageView(qrCodeImage);
            popupQrView.setFitWidth(200);
            popupQrView.setFitHeight(200);
            qrContainer.getChildren().add(popupQrView);

            Button closeBtn = new Button("Fermer");
            closeBtn.getStyleClass().add("btn-secondary");
            closeBtn.setOnAction(e -> ((javafx.stage.Stage) closeBtn.getScene().getWindow()).close());

            root.getChildren().addAll(title, subTitle, qrContainer, closeBtn);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setTitle("Skylora Web QR Code");
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
}
