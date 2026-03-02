package com.skilora.support.controller;

import com.skilora.support.entity.UserFeedback;
import com.skilora.support.service.GeminiAIService;
import com.skilora.support.service.UserFeedbackService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.Rating;

/**
 * Controller for the feedback submission modal.
 * Allows users to rate a resolved ticket (1-5 stars) and leave a comment.
 * Includes bad-word filtering and AI sentiment analysis via GeminiAIService.
 * Ported from branch FeedbackModalController with full package refactor.
 */
public class FeedbackModalController {

    @FXML private Rating ratingField;
    @FXML private TextArea commentArea;

    private int ticketId;
    private final UserFeedbackService feedbackService = UserFeedbackService.getInstance();

    /** Callback to close/navigate away */
    private Runnable onClose;

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void handleSubmit() {
        int rating = (int) ratingField.getRating();
        String comment = commentArea.getText() != null ? commentArea.getText() : "";
        String commentLower = comment.toLowerCase();

        // Basic profanity check (placeholder list)
        String[] badWords = {"bad1", "bad2", "bad3", "bad4", "bad5", "bad6"};
        for (String word : badWords) {
            if (commentLower.contains(word)) {
                applyPunishment();
                return;
            }
        }

        if (rating == 0) {
            Notifications.create()
                    .title("Rating Required")
                    .text("Please select a rating before submitting.")
                    .showWarning();
            return;
        }

        try {
            // Sentiment Analysis via AI
            String sentiment = "";
            try {
                sentiment = GeminiAIService.getInstance().analyzeSentiment(comment);
            } catch (Exception e) {
                sentiment = "N/A";
            }

            UserFeedback fb = new UserFeedback();
            fb.setTicketId(ticketId);
            fb.setRating(rating);
            fb.setComment(comment);
            fb.setFeedbackType("TICKET_FEEDBACK");
            feedbackService.submit(fb);

            Notifications.create()
                    .title("Thank you! (Sentiment: " + sentiment + ")")
                    .text("Your feedback has been recorded successfully.")
                    .showInformation();

            closeModal();
        } catch (Exception e) {
            e.printStackTrace();
            Notifications.create()
                    .title("Error")
                    .text("Could not save feedback. Please try again.")
                    .showError();
        }
    }

    private int strikeCount = 0;

    private void applyPunishment() {
        strikeCount++;
        if (strikeCount == 1) {
            Notifications.create()
                    .title("Warning!")
                    .text("Inappropriate language detected. Please be respectful.")
                    .hideAfter(javafx.util.Duration.seconds(10))
                    .showWarning();
        } else {
            Notifications.create()
                    .title("Warning!")
                    .text("Repeated inappropriate language. Your comment will not be submitted.")
                    .hideAfter(javafx.util.Duration.seconds(15))
                    .showError();
        }
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        if (onClose != null) onClose.run();
    }
}
