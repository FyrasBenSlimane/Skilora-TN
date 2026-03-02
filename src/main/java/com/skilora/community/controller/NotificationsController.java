package com.skilora.community.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLDropdownMenu;
import com.skilora.framework.components.TLLoadingState;
import com.skilora.community.entity.Notification;
import com.skilora.user.entity.User;
import com.skilora.community.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

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
    @FXML private TLButton allTab;
    @FXML private TLButton unreadTab;
    @FXML private TLButton mentionsTab;
    
    private User currentUser;
    private List<Notification> notifications = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, UNREAD, MENTION
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        markAllBtn.setText(I18n.get("notif.mark_all_read"));
        markAllBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
        clearBtn.setText(I18n.get("notif.clear_all"));
        clearBtn.setGraphic(SvgIcons.icon(SvgIcons.TRASH, 14));
        if (allTab != null) allTab.setText(I18n.get("notif.filter.all"));
        if (unreadTab != null) unreadTab.setText(I18n.get("notif.filter.unread"));
        if (mentionsTab != null) mentionsTab.setText(I18n.get("notif.filter.mentions"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadNotifications();
    }
    
    private void loadNotifications() {
        if (currentUser == null) return;

        notificationsList.getChildren().clear();
        notificationsList.getChildren().add(new TLLoadingState());

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

        AppThreadPool.execute(task);
    }
    
    private void displayNotifications() {
        notificationsList.getChildren().clear();
        
        List<Notification> display;
        if ("UNREAD".equals(currentFilter)) {
            display = notifications.stream().filter(n -> !n.isRead()).toList();
        } else if ("MENTION".equals(currentFilter)) {
            display = notifications.stream()
                .filter(n -> "MENTION".equalsIgnoreCase(n.getType()) || "MESSAGE".equalsIgnoreCase(n.getType()))
                .toList();
        } else {
            display = notifications;
        }
        
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        statsLabel.setText(I18n.get("notif.unread", unreadCount));

        // Group by date (C-10)
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Map<String, List<Notification>> grouped = new LinkedHashMap<>();
        for (Notification notif : display) {
            String group;
            LocalDate date = notif.getCreatedAt() != null ? notif.getCreatedAt().toLocalDate() : null;
            if (date == null) {
                group = I18n.get("notif.group.earlier");
            } else if (date.equals(today)) {
                group = I18n.get("notif.group.today");
            } else if (date.equals(yesterday)) {
                group = I18n.get("notif.group.yesterday");
            } else {
                group = I18n.get("notif.group.earlier");
            }
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(notif);
        }

        for (Map.Entry<String, List<Notification>> entry : grouped.entrySet()) {
            // Date group header
            Label dateHeader = new Label(entry.getKey());
            dateHeader.getStyleClass().add("notification-date-header");
            notificationsList.getChildren().add(dateHeader);

            for (Notification notif : entry.getValue()) {
                notificationsList.getChildren().add(createNotificationCard(notif));
            }
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
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label();
        icon.setGraphic(SvgIcons.icon(SvgIcons.BELL, 20, "-fx-muted-foreground"));
        icon.getStyleClass().add("text-2xl");
        
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
        time.getStyleClass().addAll("text-2xs", "text-muted");
        
        textBox.getChildren().addAll(titleRow, message, time);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        TLButton actionBtn = new TLButton();
        actionBtn.setGraphic(SvgIcons.icon(SvgIcons.ELLIPSIS, 14, "-fx-muted-foreground"));
        actionBtn.setVariant(TLButton.ButtonVariant.GHOST);
        actionBtn.setTooltip(new Tooltip("More actions"));
        actionBtn.setOnAction(e -> handleNotificationAction(notif, actionBtn));
        
        header.getChildren().addAll(icon, textBox, spacer, actionBtn);
        content.getChildren().add(header);
        
        card.getContent().add(content);
        card.getStyleClass().add("card-interactive");
        card.setOnMouseClicked(e -> {
            if (!notif.isRead()) {
                AppThreadPool.execute(() -> {
                    try {
                        notificationService.markAsRead(notif.getId());
                        notif.setRead(true);
                        Platform.runLater(this::displayNotifications);
                    } catch (Exception ex) {
                        logger.error("Failed to mark notification as read", ex);
                    }
                });
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
    
    private void handleNotificationAction(Notification notif, TLButton anchor) {
        TLDropdownMenu menu = new TLDropdownMenu();

        if (!notif.isRead()) {
            menu.addItem(I18n.get("notif.action.mark_read"), ev -> {
                AppThreadPool.execute(() -> {
                    try {
                        notificationService.markAsRead(notif.getId());
                        notif.setRead(true);
                        Platform.runLater(this::displayNotifications);
                    } catch (Exception ex) {
                        logger.error("Failed to mark notification as read", ex);
                    }
                });
            });
        }

        // Contextual label based on notification type
        String type = notif.getType() != null ? notif.getType().toUpperCase() : "";
        String detailLabel = switch (type) {
            case "APPLICATION" -> I18n.get("notif.action.view_application");
            case "MESSAGE" -> I18n.get("notif.action.view_message");
            case "MATCH" -> I18n.get("notif.action.view_match");
            default -> I18n.get("notif.action.view_details");
        };
        menu.addItem(detailLabel, ev -> {
            com.skilora.utils.DialogUtils.showInfo(
                notif.getTitle(),
                notif.getMessage()
                    + (notif.getReferenceType() != null ? "\n\n" + I18n.get("notif.ref_type") + ": " + notif.getReferenceType() : "")
                    + (notif.getReferenceId() != null ? " #" + notif.getReferenceId() : "")
            );
        });

        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }
    
    @FXML
    private void handleMarkAllRead() {
        if (currentUser == null) return;
        AppThreadPool.execute(() -> {
            try {
                notificationService.markAllAsRead(currentUser.getId());
                notifications.forEach(n -> n.setRead(true));
                Platform.runLater(this::displayNotifications);
            } catch (Exception e) {
                logger.error("Failed to mark all notifications as read", e);
            }
        });
    }
    
    @FXML
    private void handleClearAll() {
        if (currentUser == null) return;
        com.skilora.utils.DialogUtils.showConfirmation(
            I18n.get("notif.clear.confirm.title"),
            I18n.get("notif.clear.confirm.message")
        ).ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                AppThreadPool.execute(() -> {
                    try {
                        notificationService.clearAll(currentUser.getId());
                        notifications.clear();
                        Platform.runLater(this::displayNotifications);
                    } catch (Exception e) {
                        logger.error("Failed to clear all notifications", e);
                    }
                });
            }
        });
    }
    
    @FXML
    private void handleShowAll() {
        currentFilter = "ALL";
        setActiveTab(allTab);
        displayNotifications();
    }
    
    @FXML
    private void handleShowUnread() {
        currentFilter = "UNREAD";
        setActiveTab(unreadTab);
        displayNotifications();
    }
    
    @FXML
    private void handleShowMentions() {
        currentFilter = "MENTION";
        setActiveTab(mentionsTab);
        displayNotifications();
    }

    private void setActiveTab(TLButton active) {
        for (TLButton tab : new TLButton[]{allTab, unreadTab, mentionsTab}) {
            if (tab != null) {
                tab.getStyleClass().remove("tab-button-active");
            }
        }
        if (active != null) {
            active.getStyleClass().add("tab-button-active");
        }
    }
}
