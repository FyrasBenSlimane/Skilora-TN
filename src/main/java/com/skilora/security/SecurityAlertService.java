package com.skilora.security;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SecurityAlertService — manages security alerts (camera captures on failed login, etc.).
 * Provides CRUD for the 'security_alerts' table.
 */
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);
    private static volatile SecurityAlertService instance;

    private SecurityAlertService() {}

    public static SecurityAlertService getInstance() {
        if (instance == null) {
            synchronized (SecurityAlertService.class) {
                if (instance == null) {
                    instance = new SecurityAlertService();
                }
            }
        }
        return instance;
    }

    /**
     * Create a new security alert.
     * Returns the generated ID, or -1 on failure.
     */
    public int create(SecurityAlert alert) {
        String sql = """
            INSERT INTO security_alerts (username, alert_type, photo_path, ip_address,
                                         failed_attempts, reviewed, created_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, alert.getUsername());
            stmt.setString(2, alert.getAlertType());
            stmt.setString(3, alert.getPhotoPath());
            stmt.setString(4, alert.getIpAddress());
            stmt.setInt(5, alert.getFailedAttempts());
            stmt.setBoolean(6, false);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Security alert created: id={}, type={}, user={}",
                        id, alert.getAlertType(), alert.getUsername());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Failed to create security alert for user '{}': {}",
                    alert.getUsername(), e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Get all unreviewed security alerts (for admin dashboard).
     */
    public List<SecurityAlert> getUnreviewedAlerts() {
        return getAlerts("WHERE reviewed = FALSE ORDER BY created_at DESC");
    }

    /**
     * Get all security alerts (for admin panel), newest first.
     */
    public List<SecurityAlert> getAllAlerts(int limit) {
        return getAlerts("ORDER BY created_at DESC LIMIT " + Math.max(1, Math.min(limit, 500)));
    }

    /**
     * Get alerts for a specific username.
     */
    public List<SecurityAlert> getAlertsByUsername(String username) {
        List<SecurityAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM security_alerts WHERE username = ? ORDER BY created_at DESC LIMIT 50";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch alerts for username '{}': {}", username, e.getMessage(), e);
        }
        return alerts;
    }

    /**
     * Count unreviewed alerts (for admin badge).
     */
    public int getUnreviewedCount() {
        String sql = "SELECT COUNT(*) FROM security_alerts WHERE reviewed = FALSE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.debug("Could not count unreviewed alerts: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Mark an alert as reviewed by an admin.
     */
    public boolean markReviewed(int alertId, String adminUsername, String notes) {
        String sql = "UPDATE security_alerts SET reviewed = TRUE, reviewed_by = ?, notes = ?, reviewed_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminUsername);
            stmt.setString(2, notes);
            stmt.setInt(3, alertId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to mark alert {} as reviewed: {}", alertId, e.getMessage(), e);
        }
        return false;
    }

    // ── Private helpers ─────────────────────────────────────────────

    private List<SecurityAlert> getAlerts(String whereClause) {
        List<SecurityAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM security_alerts " + whereClause;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch security alerts: {}", e.getMessage(), e);
        }
        return alerts;
    }

    private SecurityAlert mapRow(ResultSet rs) throws SQLException {
        SecurityAlert a = new SecurityAlert();
        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setAlertType(rs.getString("alert_type"));
        a.setPhotoPath(rs.getString("photo_path"));
        a.setIpAddress(rs.getString("ip_address"));
        a.setFailedAttempts(rs.getInt("failed_attempts"));
        a.setReviewed(rs.getBoolean("reviewed"));
        a.setReviewedBy(rs.getString("reviewed_by"));
        a.setNotes(rs.getString("notes"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());

        Timestamp reviewed = rs.getTimestamp("reviewed_at");
        if (reviewed != null) a.setReviewedAt(reviewed.toLocalDateTime());

        return a;
    }
}
