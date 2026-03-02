package com.skilora.support.controller;

import com.skilora.support.entity.SupportTicket;
import com.skilora.support.service.GeminiAIService;
import com.skilora.support.service.SupportTicketService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the ticket creation/edit modal.
 * Supports AI-powered auto-categorization and text correction via GeminiAIService.
 * Ported from branch TicketModalController with full package refactor.
 */
public class TicketModalController implements Initializable {

    @FXML private Label modalTitle;
    @FXML private TextField subjectField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> priorityBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private final SupportTicketService ticketService = SupportTicketService.getInstance();
    private SupportTicket existingTicket;
    private int currentUserId = 1;

    /** Callback to refresh parent after save */
    private Runnable onSaved;
    /** Callback to close/navigate away */
    private Runnable onClose;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryBox.getItems().addAll("TECHNICAL", "PAYMENT", "ACCOUNT", "FORMATION", "RECRUITMENT", "OTHER");
        priorityBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT");
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Pre-fills the form with existing ticket data for editing.
     */
    public void setTicketData(SupportTicket ticket) {
        this.existingTicket = ticket;
        modalTitle.setText("Edit Ticket #" + ticket.getId());
        subjectField.setText(ticket.getSubject());
        categoryBox.setValue(ticket.getCategory());
        priorityBox.setValue(ticket.getPriority());
        descriptionArea.setText(ticket.getDescription());
    }

    /**
     * Uses Gemini AI to auto-categorize the ticket based on subject and description.
     */
    @FXML
    public void handleAICategorize() {
        String subject = subjectField.getText();
        String description = descriptionArea.getText();

        if ((subject == null || subject.isEmpty()) && (description == null || description.isEmpty())) {
            errorLabel.setText("Please fill in at least the subject or description.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        new Thread(() -> {
            String prediction = GeminiAIService.getInstance().predictCategory(
                    subject != null ? subject : "", description != null ? description : "");
            Platform.runLater(() -> {
                if (prediction != null && !prediction.startsWith("ERROR")) {
                    categoryBox.setValue(prediction);
                }
            });
        }).start();
    }

    /**
     * Uses Gemini AI to auto-correct spelling/grammar in description.
     */
    @FXML
    public void handleAICorrect() {
        String description = descriptionArea.getText();
        if (description == null || description.isEmpty()) return;

        new Thread(() -> {
            String corrected = GeminiAIService.getInstance().correctText(description);
            Platform.runLater(() -> {
                if (corrected != null && !corrected.startsWith("Error")) {
                    descriptionArea.setText(corrected);
                }
            });
        }).start();
    }

    @FXML
    public void handleSave() {
        if (subjectField.getText().isEmpty() || categoryBox.getValue() == null
                || priorityBox.getValue() == null || descriptionArea.getText().isEmpty()) {
            errorLabel.setText("Please fill in all required fields.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        try {
            if (existingTicket == null) {
                SupportTicket newTicket = new SupportTicket(
                        currentUserId,
                        categoryBox.getValue(),
                        priorityBox.getValue(),
                        "OPEN",
                        subjectField.getText(),
                        descriptionArea.getText());
                ticketService.create(newTicket);
            } else {
                existingTicket.setSubject(subjectField.getText());
                existingTicket.setCategory(categoryBox.getValue());
                existingTicket.setPriority(priorityBox.getValue());
                existingTicket.setDescription(descriptionArea.getText());
                ticketService.update(existingTicket);
            }

            if (onSaved != null) onSaved.run();
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        if (onClose != null) onClose.run();
    }
}
