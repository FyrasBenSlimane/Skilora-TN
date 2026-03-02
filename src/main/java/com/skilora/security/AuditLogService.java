package com.skilora.security;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * AuditLogService
 *
 * Best-effort audit logging for sensitive actions.
 * Never throws to callers.
 */
public class AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private static volatile AuditLogService instance;

    private AuditLogService() {}

    public static AuditLogService getInstance() {
        if (instance == null) {
            synchronized (AuditLogService.class) {
                if (instance == null) {
                    instance = new AuditLogService();
                }
            }
        }
        return instance;
    }

    public void log(Integer actorUserId, String module, String action, String entityType, String entityId, String details) {
        if (module == null || module.isBlank() || action == null || action.isBlank()) {
            return;
        }
        String sql = "INSERT INTO audit_logs (actor_user_id, module, action, entity_type, entity_id, details) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (actorUserId != null) stmt.setInt(1, actorUserId);
            else stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setString(2, module);
            stmt.setString(3, action);
            stmt.setString(4, entityType);
            stmt.setString(5, entityId);
            stmt.setString(6, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Audit log insert failed: {}", e.getMessage());
        }
    }
}

