package com.skilora.community.service;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * CommunityNotificationService — Real-time poller for community notifications.
 *
 * Polls the database every few seconds for:
 *   - New unread messages
 *   - New pending connection requests (invitations)
 *
 * Fires callbacks on the JavaFX Application Thread when counts change,
 * enabling live badge updates and toast notifications.
 *
 * Usage:
 *   var notifier = new CommunityNotificationService(userId, 5);
 *   notifier.setOnUnreadMessagesChanged((oldCount, newCount) -> ...);
 *   notifier.setOnPendingConnectionsChanged((oldCount, newCount) -> ...);
 *   notifier.start();
 *   // later: notifier.stop();
 */
public class CommunityNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(CommunityNotificationService.class);

    /** Default poll interval in seconds. */
    private static final int DEFAULT_INTERVAL_SECONDS = 8;

    private final int userId;
    private final Timeline timeline;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Last known counts
    private int lastUnreadMessages = -1;
    private int lastPendingConnections = -1;

    // Callbacks (fired on FX thread)
    private BiConsumer<Integer, Integer> onUnreadMessagesChanged;
    private BiConsumer<Integer, Integer> onPendingConnectionsChanged;
    private List<Runnable> onTickListeners = new ArrayList<>();

    /**
     * Create a notification poller for the given user.
     *
     * @param userId            the current user's ID
     * @param intervalSeconds   seconds between each poll (minimum 3)
     */
    public CommunityNotificationService(int userId, int intervalSeconds) {
        this.userId = userId;
        int interval = Math.max(3, intervalSeconds);

        this.timeline = new Timeline(new KeyFrame(
                Duration.seconds(interval),
                e -> pollNotifications()
        ));
        this.timeline.setCycleCount(Animation.INDEFINITE);
    }

    public CommunityNotificationService(int userId) {
        this(userId, DEFAULT_INTERVAL_SECONDS);
    }

    // ── Callbacks ──

    /**
     * Called when unread message count changes. Args: (oldCount, newCount).
     */
    public void setOnUnreadMessagesChanged(BiConsumer<Integer, Integer> handler) {
        this.onUnreadMessagesChanged = handler;
    }

    /**
     * Called when pending connection request count changes. Args: (oldCount, newCount).
     */
    public void setOnPendingConnectionsChanged(BiConsumer<Integer, Integer> handler) {
        this.onPendingConnectionsChanged = handler;
    }

    /**
     * Register a listener called on every poll tick (for badge refresh even if count unchanged).
     */
    public void addOnTickListener(Runnable listener) {
        this.onTickListeners.add(listener);
    }

    // ── Lifecycle ──

    /**
     * Start polling. Also does an immediate first poll.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("CommunityNotificationService started for user {}", userId);
            // Immediate first poll
            pollNotifications();
            timeline.play();
        }
    }

    /**
     * Stop polling and release resources.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            timeline.stop();
            logger.info("CommunityNotificationService stopped for user {}", userId);
        }
    }

    /**
     * Check if the poller is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Force an immediate poll (useful after sending a message or accepting a request).
     */
    public void pollNow() {
        pollNotifications();
    }

    /**
     * Get the last known unread message count.
     */
    public int getLastUnreadMessages() {
        return Math.max(0, lastUnreadMessages);
    }

    /**
     * Get the last known pending connection count.
     */
    public int getLastPendingConnections() {
        return Math.max(0, lastPendingConnections);
    }

    // ── Core polling logic ──

    private void pollNotifications() {
        // Run DB queries on a background thread to avoid blocking FX thread
        Thread pollThread = new Thread(() -> {
            try {
                int unread = MessagingService.getInstance().getUnreadCount(userId);
                int pending = ConnectionService.getInstance().getPendingCount(userId);

                Platform.runLater(() -> {
                    // Messages
                    if (lastUnreadMessages != unread) {
                        int old = lastUnreadMessages;
                        lastUnreadMessages = unread;
                        if (onUnreadMessagesChanged != null) {
                            onUnreadMessagesChanged.accept(old, unread);
                        }
                    }

                    // Connections
                    if (lastPendingConnections != pending) {
                        int old = lastPendingConnections;
                        lastPendingConnections = pending;
                        if (onPendingConnectionsChanged != null) {
                            onPendingConnectionsChanged.accept(old, pending);
                        }
                    }

                    // Tick listeners
                    for (Runnable listener : onTickListeners) {
                        try {
                            listener.run();
                        } catch (Exception ex) {
                            logger.debug("Tick listener error: {}", ex.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                logger.debug("Poll error: {}", e.getMessage());
            }
        }, "CommunityNotification-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
    }
}
