package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService - CRUD for the notifications table.
 */
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    /**
     * Create a notification record.
     */
    public int create(Notification notif) {
        String sql = "INSERT INTO notifications (user_id, type, title, message, icon, is_read, reference_type, reference_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notif.getUserId());
            ps.setString(2, notif.getType());
            ps.setString(3, notif.getTitle());
            ps.setString(4, notif.getMessage());
            ps.setString(5, notif.getIcon());
            ps.setBoolean(6, notif.isRead());
            ps.setString(7, notif.getReferenceType());
            if (notif.getReferenceId() != null) {
                ps.setInt(8, notif.getReferenceId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to create notification", e);
        }
        return -1;
    }

    /**
     * Get all notifications for a user, newest first.
     */
    public List<Notification> findByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find notifications for user {}", userId, e);
        }
        return list;
    }

    /**
     * Get unread notifications for a user.
     */
    public List<Notification> findUnreadByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find unread notifications for user {}", userId, e);
        }
        return list;
    }

    /**
     * Mark a single notification as read.
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to mark notification {} as read", notificationId, e);
        }
        return false;
    }

    /**
     * Mark all notifications as read for a user.
     */
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            logger.error("Failed to mark all as read for user {}", userId, e);
        }
        return false;
    }

    /**
     * Delete all notifications for a user (clear all).
     */
    public boolean clearAll(int userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            logger.error("Failed to clear notifications for user {}", userId, e);
        }
        return false;
    }

    /**
     * Delete a single notification.
     */
    public boolean delete(int notificationId) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete notification {}", notificationId, e);
        }
        return false;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setType(rs.getString("type"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setIcon(rs.getString("icon"));
        n.setRead(rs.getBoolean("is_read"));
        n.setReferenceType(rs.getString("reference_type"));
        int refId = rs.getInt("reference_id");
        n.setReferenceId(rs.wasNull() ? null : refId);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }
}
