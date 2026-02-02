package com.skilora.community.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.community.entity.Notification;
import com.skilora.user.entity.User;
import com.skilora.community.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.skilora.utils.I18n;

/**
 * NotificationsController - Display and manage user notifications from DB
 */
public class NotificationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsController.class);
    private final NotificationService notificationService = NotificationService.getInstance();

    @FXML private Label statsLabel;
    @FXML private TLButton markAllBtn;
    @FXML private TLButton clearBtn;
    @FXML private HBox tabsBox;
    @FXML private VBox notificationsList;
    
    private User currentUser;
    private List<Notification> notifications = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, UNREAD
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data loads after setCurrentUser
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadNotifications();
    }
    
    private void loadNotifications() {
        if (currentUser == null) return;

        Task<List<Notification>> task = new Task<>() {
            @Override
            protected List<Notification> call() {
                return notificationService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            notifications = task.getValue();
            displayNotifications();
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load notifications", task.getException());
            statsLabel.setText(I18n.get("common.error"));
        });

        new Thread(task, "NotifLoadThread") {{ setDaemon(true); }}.start();
    }
    
    private void displayNotifications() {
        notificationsList.getChildren().clear();
        
        List<Notification> display;
        if ("UNREAD".equals(currentFilter)) {
            display = notifications.stream().filter(n -> !n.isRead()).toList();
        } else {
            display = notifications;
        }
        
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        statsLabel.setText(I18n.get("notif.unread", unreadCount));
        
        for (Notification notif : display) {
            notificationsList.getChildren().add(createNotificationCard(notif));
        }
        
        if (display.isEmpty()) {
            Label emptyLabel = new Label(I18n.get("notif.no_notifications"));
            emptyLabel.getStyleClass().add("text-muted");
            notificationsList.getChildren().add(emptyLabel);
        }
    }
    
    private TLCard createNotificationCard(Notification notif) {
        TLCard card = new TLCard();
        
        if (!notif.isRead()) {
            card.getStyleClass().add("notification-unread");
        }
        
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label(notif.getIcon() != null ? notif.getIcon() : "🔔");
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
        
        Label time = new Label(formatTime(notif.getCreatedAt()));
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
                new Thread(() -> {
                    notificationService.markAsRead(notif.getId());
                    notif.setRead(true);
                    Platform.runLater(this::displayNotifications);
                }, "MarkReadThread").start();
            }
        });
        
        return card;
    }
    
    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        long minutesAgo = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
        
        if (minutesAgo < 1) return I18n.get("notif.time.now");
        if (minutesAgo < 60) return I18n.get("notif.time.minutes", minutesAgo);
        
        long hoursAgo = minutesAgo / 60;
        if (hoursAgo < 24) return I18n.get("notif.time.hours", hoursAgo);
        
        long daysAgo = hoursAgo / 24;
        if (daysAgo < 7) return I18n.get("notif.time.days", daysAgo);
        
        return time.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
    
    private void handleNotificationAction(Notification notif) {
        logger.debug("Notification action: {}", notif.getTitle());
    }
    
    @FXML
    private void handleMarkAllRead() {
        if (currentUser == null) return;
        new Thread(() -> {
            notificationService.markAllAsRead(currentUser.getId());
            notifications.forEach(n -> n.setRead(true));
            Platform.runLater(this::displayNotifications);
        }, "MarkAllReadThread").start();
    }
    
    @FXML
    private void handleClearAll() {
        if (currentUser == null) return;
        new Thread(() -> {
            notificationService.clearAll(currentUser.getId());
            notifications.clear();
            Platform.runLater(this::displayNotifications);
        }, "ClearAllThread").start();
    }
    
    @FXML
    private void handleShowAll() {
        currentFilter = "ALL";
        displayNotifications();
    }
    
    @FXML
    private void handleShowUnread() {
        currentFilter = "UNREAD";
        displayNotifications();
    }
    
    @FXML
    private void handleShowMentions() {
        // Filter by type = MENTION if needed
        displayNotifications();
    }
}
