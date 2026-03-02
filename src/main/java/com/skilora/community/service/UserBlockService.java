package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserBlockService — DM privacy via user-level blocking.
 *
 * Provides block / unblock / check for the messaging system.
 * Blocked users cannot send messages, and their conversations
 * are hidden from the blocker's list.
 */
public class UserBlockService {

    private static final Logger logger = LoggerFactory.getLogger(UserBlockService.class);
    private static volatile UserBlockService instance;

    private UserBlockService() {}

    public static UserBlockService getInstance() {
        if (instance == null) {
            synchronized (UserBlockService.class) {
                if (instance == null) instance = new UserBlockService();
            }
        }
        return instance;
    }

    /**
     * Block a user. The blocker will no longer receive messages from the blocked user,
     * and conversations with the blocked user will be hidden.
     *
     * @param blockerId  the user performing the block
     * @param blockedId  the user being blocked
     * @param reason     optional reason for the block
     * @return true if block was created successfully
     */
    public boolean blockUser(int blockerId, int blockedId, String reason) {
        if (blockerId == blockedId) return false;

        String sql = "INSERT INTO user_blocks (blocker_id, blocked_id, reason) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE reason = VALUES(reason), created_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            ps.setString(3, reason);
            ps.executeUpdate();
            logger.info("User {} blocked user {} (reason: {})", blockerId, blockedId, reason);
            return true;
        } catch (SQLException e) {
            logger.error("Failed to block user", e);
            return false;
        }
    }

    /**
     * Unblock a user.
     */
    public boolean unblockUser(int blockerId, int blockedId) {
        String sql = "DELETE FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                logger.info("User {} unblocked user {}", blockerId, blockedId);
            }
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to unblock user", e);
            return false;
        }
    }

    /**
     * Check if userId has blocked otherUserId.
     */
    public boolean isBlocked(int blockerId, int blockedId) {
        String sql = "SELECT 1 FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check block status", e);
            return false;
        }
    }

    /**
     * Check if EITHER user has blocked the other (bidirectional check).
     * Used before sending messages.
     */
    public boolean isEitherBlocked(int userId1, int userId2) {
        String sql = "SELECT 1 FROM user_blocks WHERE " +
                     "(blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check mutual block status", e);
            return false;
        }
    }

    /**
     * Get the list of user IDs that a user has blocked.
     */
    public List<Integer> getBlockedUserIds(int blockerId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT blocked_id FROM user_blocks WHERE blocker_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("blocked_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get blocked users", e);
        }
        return ids;
    }

    /**
     * Get count of users blocked by this user.
     */
    public int getBlockedCount(int blockerId) {
        String sql = "SELECT COUNT(*) FROM user_blocks WHERE blocker_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count blocked users", e);
        }
        return 0;
    }
}
