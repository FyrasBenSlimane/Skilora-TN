package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * UserActivityService - Tracks user activity, login history, and session stats.
 * Provides comprehensive activity monitoring for the User module.
 */
public class UserActivityService {

    private static final Logger logger = LoggerFactory.getLogger(UserActivityService.class);
    private static UserActivityService instance;

    private UserActivityService() {
        ensureTable();
    }

    public static synchronized UserActivityService getInstance() {
        if (instance == null) {
            instance = new UserActivityService();
        }
        return instance;
    }

    /**
     * Ensure the user_activity_logs table exists.
     */
    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_activity_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                activity_type VARCHAR(50) NOT NULL,
                description VARCHAR(500),
                ip_address VARCHAR(45),
                user_agent VARCHAR(500),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_user_activity (user_id, created_at),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("user_activity_logs table ensured.");
        } catch (SQLException e) {
            logger.error("Failed to create user_activity_logs table: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Activity Logging
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Log a user activity.
     */
    public void logActivity(int userId, String activityType, String description) {
        String sql = "INSERT INTO user_activity_logs (user_id, activity_type, description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, activityType);
            ps.setString(3, description);
            ps.executeUpdate();
            logger.debug("Activity logged: {} - {} for user {}", activityType, description, userId);
        } catch (SQLException e) {
            logger.error("Failed to log activity for user {}: {}", userId, e.getMessage());
        }
    }

    // Activity type constants
    public static final String ACTIVITY_LOGIN = "LOGIN";
    public static final String ACTIVITY_LOGOUT = "LOGOUT";
    public static final String ACTIVITY_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTIVITY_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTIVITY_2FA_ENABLED = "2FA_ENABLED";
    public static final String ACTIVITY_2FA_DISABLED = "2FA_DISABLED";
    public static final String ACTIVITY_SETTINGS_CHANGE = "SETTINGS_CHANGE";

    // ══════════════════════════════════════════════════════════════════════
    // Login History
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Get login history for a user from login_attempts table.
     */
    public List<LoginRecord> getLoginHistory(int userId, int limit) {
        List<LoginRecord> records = new ArrayList<>();
        
        // Get username first
        String username = null;
        String userSql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(userSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get username for user {}", userId, e);
            return records;
        }

        if (username == null) return records;

        String sql = "SELECT attempted_at, success FROM login_attempts WHERE username = ? ORDER BY attempted_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime time = rs.getTimestamp("attempted_at").toLocalDateTime();
                    boolean success = rs.getBoolean("success");
                    records.add(new LoginRecord(time, success));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get login history for user {}: {}", userId, e.getMessage());
        }

        return records;
    }

    /**
     * Get the last successful login time for a user.
     */
    public Optional<LocalDateTime> getLastLogin(int userId) {
        String username = null;
        String userSql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(userSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get username for user {}", userId, e);
            return Optional.empty();
        }

        if (username == null) return Optional.empty();

        String sql = "SELECT attempted_at FROM login_attempts WHERE username = ? AND success = TRUE ORDER BY attempted_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getTimestamp("attempted_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get last login for user {}: {}", userId, e.getMessage());
        }

        return Optional.empty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Activity Stats
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Get comprehensive activity stats for a user.
     */
    public UserActivityStats getActivityStats(int userId) {
        UserActivityStats stats = new UserActivityStats();

        String username = null;
        String userSql = "SELECT username, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(userSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        stats.setAccountCreatedAt(createdAt.toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get user info for {}", userId, e);
        }

        if (username == null) return stats;

        // Login stats from login_attempts
        String loginStatsSql = """
            SELECT 
                COUNT(*) as total_attempts,
                SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) as successful_logins,
                SUM(CASE WHEN success = FALSE THEN 1 ELSE 0 END) as failed_logins,
                MAX(CASE WHEN success = TRUE THEN attempted_at END) as last_login,
                MIN(attempted_at) as first_login
            FROM login_attempts WHERE username = ?
        """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(loginStatsSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalLoginAttempts(rs.getInt("total_attempts"));
                    stats.setSuccessfulLogins(rs.getInt("successful_logins"));
                    stats.setFailedLogins(rs.getInt("failed_logins"));
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    if (lastLogin != null) {
                        stats.setLastLoginAt(lastLogin.toLocalDateTime());
                    }
                    Timestamp firstLogin = rs.getTimestamp("first_login");
                    if (firstLogin != null) {
                        stats.setFirstLoginAt(firstLogin.toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get login stats for user {}: {}", userId, e.getMessage());
        }

        // Activity logs count
        String activitySql = "SELECT activity_type, COUNT(*) as count FROM user_activity_logs WHERE user_id = ? GROUP BY activity_type";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(activitySql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> activityCounts = new HashMap<>();
                while (rs.next()) {
                    activityCounts.put(rs.getString("activity_type"), rs.getInt("count"));
                }
                stats.setActivityCounts(activityCounts);
            }
        } catch (SQLException e) {
            logger.error("Failed to get activity counts for user {}: {}", userId, e.getMessage());
        }

        return stats;
    }

    /**
     * Get recent activity logs for a user.
     */
    public List<ActivityLog> getRecentActivity(int userId, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT activity_type, description, created_at FROM user_activity_logs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                        rs.getString("activity_type"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent activity for user {}: {}", userId, e.getMessage());
        }

        return logs;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner Classes
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Login record containing timestamp and success status.
     */
    public static class LoginRecord {
        private final LocalDateTime time;
        private final boolean success;

        public LoginRecord(LocalDateTime time, boolean success) {
            this.time = time;
            this.success = success;
        }

        public LocalDateTime getTime() { return time; }
        public boolean isSuccess() { return success; }
        
        public String getFormattedTime() {
            return time.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
    }

    /**
     * Activity log entry.
     */
    public static class ActivityLog {
        private final String type;
        private final String description;
        private final LocalDateTime time;

        public ActivityLog(String type, String description, LocalDateTime time) {
            this.type = type;
            this.description = description;
            this.time = time;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public LocalDateTime getTime() { return time; }
        
        public String getFormattedTime() {
            return time.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
    }

    /**
     * Comprehensive user activity stats.
     */
    public static class UserActivityStats {
        private LocalDateTime accountCreatedAt;
        private LocalDateTime firstLoginAt;
        private LocalDateTime lastLoginAt;
        private int totalLoginAttempts;
        private int successfulLogins;
        private int failedLogins;
        private Map<String, Integer> activityCounts = new HashMap<>();

        // Getters and setters
        public LocalDateTime getAccountCreatedAt() { return accountCreatedAt; }
        public void setAccountCreatedAt(LocalDateTime accountCreatedAt) { this.accountCreatedAt = accountCreatedAt; }

        public LocalDateTime getFirstLoginAt() { return firstLoginAt; }
        public void setFirstLoginAt(LocalDateTime firstLoginAt) { this.firstLoginAt = firstLoginAt; }

        public LocalDateTime getLastLoginAt() { return lastLoginAt; }
        public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

        public int getTotalLoginAttempts() { return totalLoginAttempts; }
        public void setTotalLoginAttempts(int totalLoginAttempts) { this.totalLoginAttempts = totalLoginAttempts; }

        public int getSuccessfulLogins() { return successfulLogins; }
        public void setSuccessfulLogins(int successfulLogins) { this.successfulLogins = successfulLogins; }

        public int getFailedLogins() { return failedLogins; }
        public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }

        public Map<String, Integer> getActivityCounts() { return activityCounts; }
        public void setActivityCounts(Map<String, Integer> activityCounts) { this.activityCounts = activityCounts; }

        public String getFormattedAccountCreatedAt() {
            return accountCreatedAt != null 
                ? accountCreatedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                : "Unknown";
        }

        public String getFormattedLastLoginAt() {
            return lastLoginAt != null 
                ? lastLoginAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                : "Never";
        }

        public double getLoginSuccessRate() {
            if (totalLoginAttempts == 0) return 100.0;
            return (successfulLogins * 100.0) / totalLoginAttempts;
        }
    }
}
