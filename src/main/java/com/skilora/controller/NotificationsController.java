package com.skilora.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.skilora.utils.I18n;

/**
 * NotificationsController - Display and manage user notifications
 */
public class NotificationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsController.class);

    @FXML private Label statsLabel;
    @FXML private TLButton markAllBtn;
    @FXML private TLButton clearBtn;
    @FXML private HBox tabsBox;
    @FXML private VBox notificationsList;
    
    private List<NotificationItem> notifications = new ArrayList<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
        displayNotifications();
    }
    
    private void loadNotifications() {
        // Sample notifications
        notifications.add(new NotificationItem(
            "💼",
            I18n.get("notif.new_application"),
            I18n.get("notif.new_application.desc"),
            LocalDateTime.now().minusMinutes(5),
            false,
            NotificationType.APPLICATION
        ));
        
        notifications.add(new NotificationItem(
            "👁️",
            I18n.get("notif.profile_viewed"),
            I18n.get("notif.profile_viewed.desc"),
            LocalDateTime.now().minusHours(2),
            false,
            NotificationType.VIEW
        ));
        
        notifications.add(new NotificationItem(
            "✅",
            I18n.get("notif.application_accepted"),
            I18n.get("notif.application_accepted.desc"),
            LocalDateTime.now().minusHours(5),
            true,
            NotificationType.ACCEPTANCE
        ));
        
        notifications.add(new NotificationItem(
            "📧",
            I18n.get("notif.new_message"),
            I18n.get("notif.new_message.desc"),
            LocalDateTime.now().minusDays(1),
            true,
            NotificationType.MESSAGE
        ));
        
        notifications.add(new NotificationItem(
            "🎉",
            I18n.get("notif.new_match"),
            I18n.get("notif.new_match.desc"),
            LocalDateTime.now().minusDays(2),
            true,
            NotificationType.MATCH
        ));
    }
    
    private void displayNotifications() {
        notificationsList.getChildren().clear();
        
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        statsLabel.setText(I18n.get("notif.unread", unreadCount));
        
        for (NotificationItem notif : notifications) {
            notificationsList.getChildren().add(createNotificationCard(notif));
        }
        
        if (notifications.isEmpty()) {
            Label emptyLabel = new Label(I18n.get("notif.no_notifications"));
            emptyLabel.getStyleClass().add("text-muted");
            notificationsList.getChildren().add(emptyLabel);
        }
    }
    
    private TLCard createNotificationCard(NotificationItem notif) {
        TLCard card = new TLCard();
        
        if (!notif.isRead()) {
            card.getStyleClass().add("notification-unread");
        }
        
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label(notif.getIcon());
        icon.setStyle("-fx-font-size: 24px;");
        
        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(notif.getTitle());
        title.getStyleClass().add("h4");
        titleRow.getChildren().add(title);
        
        if (!notif.isRead()) {
            TLBadge unreadBadge = new TLBadge(I18n.get("notif.new_badge"), TLBadge.Variant.DEFAULT);
            titleRow.getChildren().add(unreadBadge);
        }
        
        Label message = new Label(notif.getMessage());
        message.getStyleClass().add("text-muted");
        message.setWrapText(true);
        
        Label time = new Label(formatTime(notif.getTimestamp()));
        time.setStyle("-fx-font-size: 11px;");
        time.getStyleClass().add("text-muted");
        
        textBox.getChildren().addAll(titleRow, message, time);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        TLButton actionBtn = new TLButton("⋮");
        actionBtn.setVariant(TLButton.ButtonVariant.GHOST);
        actionBtn.setOnAction(e -> handleNotificationAction(notif));
        
        header.getChildren().addAll(icon, textBox, spacer, actionBtn);
        content.getChildren().add(header);
        
        card.getContent().add(content);
        card.setOnMouseClicked(e -> {
            if (!notif.isRead()) {
                notif.setRead(true);
                displayNotifications();
            }
        });
        
        return card;
    }
    
    private String formatTime(LocalDateTime time) {
        long minutesAgo = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        
        if (minutesAgo < 1) return I18n.get("notif.time.now");
        if (minutesAgo < 60) return I18n.get("notif.time.minutes", minutesAgo);
        
        long hoursAgo = minutesAgo / 60;
        if (hoursAgo < 24) return I18n.get("notif.time.hours", hoursAgo);
        
        long daysAgo = hoursAgo / 24;
        if (daysAgo < 7) return I18n.get("notif.time.days", daysAgo);
        
        return time.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
    
    private void handleNotificationAction(NotificationItem notif) {
        logger.debug("Notification action: {}", notif.getTitle());
        // TODO: Show dropdown menu with actions (Mark read/unread, Delete, etc.)
    }
    
    @FXML
    private void handleMarkAllRead() {
        notifications.forEach(n -> n.setRead(true));
        displayNotifications();
    }
    
    @FXML
    private void handleClearAll() {
        notifications.clear();
        displayNotifications();
    }
    
    @FXML
    private void handleShowAll() {
        displayNotifications();
    }
    
    @FXML
    private void handleShowUnread() {
        notificationsList.getChildren().clear();
        notifications.stream()
            .filter(n -> !n.isRead())
            .forEach(n -> notificationsList.getChildren().add(createNotificationCard(n)));
    }
    
    @FXML
    private void handleShowMentions() {
        // TODO: Filter by mentions
        displayNotifications();
    }
    
    // Inner classes
    private static class NotificationItem {
        private String icon;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private boolean read;
        
        public NotificationItem(String icon, String title, String message, LocalDateTime timestamp, boolean read, NotificationType type) {
            this.icon = icon;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }
        
        public String getIcon() { return icon; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }
    
    private enum NotificationType {
        APPLICATION, VIEW, ACCEPTANCE, MESSAGE, MATCH
    }
}
