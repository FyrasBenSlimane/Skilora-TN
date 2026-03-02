package com.skilora.support.controller;

import com.skilora.support.entity.SupportTicket;
import com.skilora.support.service.SupportTicketService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Controller for rendering a single ticket card in the user dashboard.
 * Ported from branch TicketCardController with full package refactor.
 */
public class TicketCardController {

    @FXML private VBox cardRoot;
    @FXML private Label priorityBadge;
    @FXML private Label statusBadge;
    @FXML private Label subjectLabel;
    @FXML private Label categoryLabel;
    @FXML private Label dateLabel;
    @FXML private javafx.scene.control.Button rateButton;

    private SupportTicket ticket;

    /** Callback to refresh parent ticket list */
    private Runnable onRefresh;
    /** Callback to navigate to a view with given root node */
    private java.util.function.Consumer<Parent> onNavigate;

    public void setData(SupportTicket ticket, Runnable onRefresh, java.util.function.Consumer<Parent> onNavigate) {
        this.ticket = ticket;
        this.onRefresh = onRefresh;
        this.onNavigate = onNavigate;

        subjectLabel.setText(ticket.getSubject());
        categoryLabel.setText("Category: " + ticket.getCategory());
        if (ticket.getCreatedDate() != null) {
            dateLabel.setText("Created: " + ticket.getCreatedDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        priorityBadge.setText(ticket.getPriority());
        priorityBadge.getStyleClass().add("priority-" + ticket.getPriority().toLowerCase());

        statusBadge.setText(ticket.getStatus());
        statusBadge.getStyleClass().add("status-" + ticket.getStatus().toLowerCase().replace("_", "-"));

        // Show rate button only for RESOLVED tickets
        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus()) || "RESOLU".equalsIgnoreCase(ticket.getStatus())) {
            rateButton.setVisible(true);
            rateButton.setManaged(true);
        }
    }

    @FXML
    private void handleRate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/support/FeedbackModalView.fxml"));
            Parent root = loader.load();
            FeedbackModalController controller = loader.getController();
            controller.setTicketId(ticket.getId());
            if (onNavigate != null) onNavigate.accept(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/support/TicketModalView.fxml"));
            Parent root = loader.load();
            TicketModalController controller = loader.getController();
            controller.setTicketData(ticket);
            if (onNavigate != null) onNavigate.accept(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Ticket");
        alert.setHeaderText("Are you sure you want to delete this ticket?");
        alert.setContentText("This action cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                SupportTicketService.getInstance().delete(ticket.getId());
                if (onRefresh != null) onRefresh.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/support/TicketDetailView.fxml"));
            Parent root = loader.load();
            TicketDetailController controller = loader.getController();
            controller.setAdminMode(false);
            controller.setTicket(ticket);
            if (onNavigate != null) onNavigate.accept(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
