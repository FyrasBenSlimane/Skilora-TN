package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.security.AuditLogService;
import com.skilora.user.entity.RoleUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * Table creation SQL for role_upgrade_requests:
 *
 * CREATE TABLE role_upgrade_requests (
 *     id INT AUTO_INCREMENT PRIMARY KEY,
 *     user_id INT NOT NULL,
 *     requested_role VARCHAR(50) NOT NULL,
 *     current_user_role VARCHAR(50) NOT NULL,
 *     request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 *     justification TEXT,
 *     admin_notes TEXT,
 *     reviewed_by INT NULL,
 *     requested_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     reviewed_date DATETIME NULL,
 *     CONSTRAINT fk_rur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 *     CONSTRAINT fk_rur_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
 * );
 *
 * CREATE INDEX idx_rur_user_status ON role_upgrade_requests(user_id, request_status);
 */
public class RoleUpgradeService {

    private static final Logger logger = LoggerFactory.getLogger(RoleUpgradeService.class);
    private static volatile RoleUpgradeService instance;

    private static final String[] VALID_ROLES = {"ADMIN", "USER", "EMPLOYER", "TRAINER"};

    private RoleUpgradeService() {
    }

    public static RoleUpgradeService getInstance() {
        if (instance == null) {
            synchronized (RoleUpgradeService.class) {
                if (instance == null) {
                    instance = new RoleUpgradeService();
                }
            }
        }
        return instance;
    }

    /**
     * Request a role upgrade. Validates: no existing pending request, requested role is valid.
     *
     * @return generated request ID
     * @throws IllegalArgumentException if validation fails
     */
    public int requestUpgrade(int userId, String requestedRole, String justification) {
        if (hasPendingRequest(userId)) {
            throw new IllegalArgumentException("User already has a pending role upgrade request");
        }
        if (!isValidRole(requestedRole)) {
            throw new IllegalArgumentException("Invalid requested role: " + requestedRole);
        }

        String currentRole = getCurrentRole(userId);
        if (currentRole == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        String sql = "INSERT INTO role_upgrade_requests (user_id, requested_role, current_user_role, request_status, justification, requested_date) " +
                "VALUES (?, ?, ?, 'PENDING', ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, requestedRole);
            stmt.setString(3, currentRole);
            stmt.setString(4, justification != null ? justification : "");
            stmt.setObject(5, LocalDateTime.now());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    AuditLogService.getInstance().log(
                            userId,
                            "user",
                            "role_upgrade_request",
                            "RoleUpgradeRequest",
                            String.valueOf(id),
                            "from=" + currentRole + ", to=" + requestedRole
                    );
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create role upgrade request for user {}", userId, e);
            throw new RuntimeException("Failed to create role upgrade request", e);
        }
        throw new RuntimeException("Failed to create role upgrade request: no generated ID");
    }

    /**
     * Returns all PENDING requests with user full_name (JOIN users).
     */
    public List<RoleUpgradeRequest> findPendingRequests() {
        String sql = """
            SELECT r.*, u.full_name
            FROM role_upgrade_requests r
            INNER JOIN users u ON r.user_id = u.id
            WHERE r.request_status = 'PENDING'
            ORDER BY r.requested_date ASC
            """;
        List<RoleUpgradeRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs, true));
            }
        } catch (SQLException e) {
            logger.error("Failed to find pending role upgrade requests", e);
        }
        return list;
    }

    /**
     * Returns all requests for a user.
     */
    public List<RoleUpgradeRequest> findByUserId(int userId) {
        String sql = "SELECT * FROM role_upgrade_requests WHERE user_id = ? ORDER BY requested_date DESC";
        List<RoleUpgradeRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs, false));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find role upgrade requests for user {}", userId, e);
        }
        return list;
    }

    /**
     * Approve a request: update status, reviewed_by, reviewed_date, admin_notes, and update user's role.
     */
    public boolean approve(int requestId, int adminId, String notes) {
        RoleUpgradeRequest req = findById(requestId);
        if (req == null || !"PENDING".equals(req.getStatus())) {
            return false;
        }
        // Defense-in-depth: admin cannot approve their own role upgrade request
        if (req.getUserId() == adminId) {
            logger.warn("Blocked admin {} from approving their own role upgrade request {}", adminId, requestId);
            return false;
        }

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String updateUser = "UPDATE users SET role = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateUser)) {
                    stmt.setString(1, req.getRequestedRole());
                    stmt.setInt(2, req.getUserId());
                    stmt.executeUpdate();
                }

                String updateRequest = "UPDATE role_upgrade_requests SET request_status = 'APPROVED', reviewed_by = ?, reviewed_date = ?, admin_notes = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateRequest)) {
                    stmt.setInt(1, adminId);
                    stmt.setObject(2, LocalDateTime.now());
                    stmt.setString(3, notes != null ? notes : "");
                    stmt.setInt(4, requestId);
                    stmt.executeUpdate();
                }

                conn.commit();
                logger.info("Role upgrade request {} approved by admin {}", requestId, adminId);
                AuditLogService.getInstance().log(
                        adminId,
                        "user",
                        "role_upgrade_approve",
                        "RoleUpgradeRequest",
                        String.valueOf(requestId),
                        "user_id=" + req.getUserId() + ", from=" + req.getCurrentRole() + ", to=" + req.getRequestedRole()
                );
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Failed to approve role upgrade request {}", requestId, e);
                throw new RuntimeException("Failed to approve request", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to get connection for approve", e);
            return false;
        }
    }

    /**
     * Reject a request: update status, reviewed_by, reviewed_date, admin_notes.
     */
    public boolean reject(int requestId, int adminId, String notes) {
        // Defense-in-depth: admin cannot reject their own role upgrade request
        RoleUpgradeRequest reqCheck = findById(requestId);
        if (reqCheck != null && reqCheck.getUserId() == adminId) {
            logger.warn("Blocked admin {} from rejecting their own role upgrade request {}", adminId, requestId);
            return false;
        }
        String sql = "UPDATE role_upgrade_requests SET request_status = 'REJECTED', reviewed_by = ?, reviewed_date = ?, admin_notes = ? WHERE id = ? AND request_status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adminId);
            stmt.setObject(2, LocalDateTime.now());
            stmt.setString(3, notes != null ? notes : "");
            stmt.setInt(4, requestId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Role upgrade request {} rejected by admin {}", requestId, adminId);
                AuditLogService.getInstance().log(
                        adminId,
                        "user",
                        "role_upgrade_reject",
                        "RoleUpgradeRequest",
                        String.valueOf(requestId),
                        "notes=" + (notes != null ? notes : "")
                );
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to reject role upgrade request {}", requestId, e);
        }
        return false;
    }

    /**
     * Check if user has a pending request.
     */
    public boolean hasPendingRequest(int userId) {
        String sql = "SELECT 1 FROM role_upgrade_requests WHERE user_id = ? AND request_status = 'PENDING' LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check pending request for user {}", userId, e);
        }
        return false;
    }

    private RoleUpgradeRequest findById(int id) {
        String sql = "SELECT * FROM role_upgrade_requests WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs, false);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find role upgrade request by id {}", id, e);
        }
        return null;
    }

    private String getCurrentRole(int userId) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get current role for user {}", userId, e);
        }
        return null;
    }

    private boolean isValidRole(String role) {
        if (role == null || role.isBlank()) return false;
        for (String r : VALID_ROLES) {
            if (r.equals(role)) return true;
        }
        return false;
    }

    private RoleUpgradeRequest mapResultSet(ResultSet rs, boolean hasFullName) throws SQLException {
        RoleUpgradeRequest r = new RoleUpgradeRequest();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setRequestedRole(rs.getString("requested_role"));
        r.setCurrentRole(rs.getString("current_user_role"));
        r.setStatus(rs.getString("request_status"));
        r.setJustification(rs.getString("justification"));
        r.setAdminNotes(rs.getString("admin_notes"));
        int reviewedBy = rs.getInt("reviewed_by");
        r.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        r.setRequestedDate(getLocalDateTime(rs, "requested_date"));
        r.setReviewedDate(getLocalDateTime(rs, "reviewed_date"));
        if (hasFullName) {
            try {
                r.setFullName(rs.getString("full_name"));
            } catch (SQLException ignored) {
            }
        }
        return r;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
