package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Enrollment;
import com.skilora.formation.enums.EnrollmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EnrollmentService
 * 
 * Handles all CRUD operations and business logic for enrollments.
 * Maps to the 'enrollments' table in the database.
 */
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);
    private static volatile EnrollmentService instance;

    private EnrollmentService() {}

    public static EnrollmentService getInstance() {
        if (instance == null) {
            synchronized (EnrollmentService.class) {
                if (instance == null) {
                    instance = new EnrollmentService();
                }
            }
        }
        return instance;
    }

    /**
     * Enroll a user in a formation.
     *
     * @return the generated enrollment ID, or -1 if already enrolled or on failure
     */
    public int enroll(int formationId, int userId) {
        if (isEnrolled(formationId, userId)) {
            logger.warn("User {} is already enrolled in formation {}", userId, formationId);
            return -1;
        }

        String sql = "INSERT INTO enrollments (formation_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, formationId);
            stmt.setInt(2, userId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        logger.info("User {} enrolled in formation {} with enrollment ID: {}", userId, formationId, id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error enrolling user {} in formation {}: {}", userId, formationId, e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Check if a user is enrolled in a formation.
     */
    public boolean isEnrolled(int formationId, int userId) {
        String sql = "SELECT 1 FROM enrollments WHERE formation_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, formationId);
            stmt.setInt(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking enrollment status: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Find enrollments by user ID.
     */
    public List<Enrollment> findByUserId(int userId) {
        String sql = "SELECT e.*, f.title as formation_title, u.full_name as user_name " +
                "FROM enrollments e " +
                "JOIN formations f ON e.formation_id = f.id " +
                "JOIN users u ON e.user_id = u.id " +
                "WHERE e.user_id = ? ORDER BY e.enrolled_date DESC";
        List<Enrollment> enrollments = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSet(rs));
                }
            }
            logger.debug("Retrieved {} enrollments for user ID: {}", enrollments.size(), userId);
        } catch (SQLException e) {
            logger.error("Error finding enrollments by user ID {}: {}", userId, e.getMessage(), e);
        }
        return enrollments;
    }

    /**
     * Update enrollment status.
     */
    public boolean updateStatus(int enrollmentId, EnrollmentStatus status) {
        String sql = "UPDATE enrollments SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, enrollmentId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Enrollment ID {} status updated to {}", enrollmentId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating enrollment status: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Find enrollment by ID.
     */
    public Enrollment findById(int id) {
        String sql = "SELECT e.*, f.title as formation_title, u.full_name as user_name " +
                "FROM enrollments e JOIN formations f ON e.formation_id = f.id " +
                "JOIN users u ON e.user_id = u.id WHERE e.id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollment {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Find all enrollments for a formation.
     */
    public List<Enrollment> findByFormation(int formationId) {
        String sql = "SELECT e.*, f.title as formation_title, u.full_name as user_name " +
                "FROM enrollments e JOIN formations f ON e.formation_id = f.id " +
                "JOIN users u ON e.user_id = u.id WHERE e.formation_id = ? ORDER BY e.enrolled_date DESC";
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) enrollments.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollments for formation {}: {}", formationId, e.getMessage(), e);
        }
        return enrollments;
    }

    /**
     * Update enrollment progress (0-100).
     */
    public boolean updateProgress(int enrollmentId, double progress) {
        progress = Math.max(0, Math.min(100, progress));
        String sql = "UPDATE enrollments SET progress = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, progress);
            stmt.setInt(2, enrollmentId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) logger.info("Enrollment {} progress updated to {}%", enrollmentId, progress);
            return updated;
        } catch (SQLException e) {
            logger.error("Error updating progress: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Complete an enrollment: set status=COMPLETED, progress=100, completed_date=NOW.
     */
    public boolean completeEnrollment(int enrollmentId) {
        String sql = "UPDATE enrollments SET status = ?, progress = 100.00, completed_date = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, EnrollmentStatus.COMPLETED.name());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, enrollmentId);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) logger.info("Enrollment {} completed", enrollmentId);
            return updated;
        } catch (SQLException e) {
            logger.error("Error completing enrollment: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Count enrollments by formation.
     */
    public long countByFormation(int formationId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE formation_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, formationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting enrollments: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Check if a formation is completed by a user.
     */
    public boolean isFormationCompleted(int userId, int formationId) {
        String sql = "SELECT 1 FROM enrollments WHERE user_id = ? AND formation_id = ? AND (status = 'COMPLETED' OR completed = TRUE) LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking formation completion: userId={}, formationId={}", userId, formationId, e);
        }
        return false;
    }

    /**
     * Mark formation as completed for a user and automatically generate certificate.
     */
    public void markCompleted(int userId, int formationId) {
        logger.info("Marking formation as completed: user={}, formation={}", userId, formationId);
        String sql = "UPDATE enrollments SET status = 'COMPLETED', progress = 100.00, completed = TRUE, " +
                "completed_date = ? WHERE user_id = ? AND formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            stmt.setInt(3, formationId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Formation marked as completed: user={}, formation={}", userId, formationId);
                // Automatically generate certificate if it doesn't exist
                try {
                    CertificateService certService = CertificateService.getInstance();
                    if (!certService.certificateExists(userId, formationId)) {
                        logger.info("Auto-generating certificate: user={}, formation={}", userId, formationId);
                        certService.createCertificate(userId, formationId);
                    }
                } catch (Exception e) {
                    logger.error("Error auto-generating certificate: user={}, formation={}", userId, formationId, e);
                }
            }
        } catch (SQLException e) {
            logger.error("Error marking formation completed: user={}, formation={}", userId, formationId, e);
        }
    }

    /**
     * Update the last accessed timestamp for an enrollment.
     */
    public void updateLastAccessed(int userId, int formationId) {
        String sql = "UPDATE enrollments SET last_accessed_at = ? WHERE user_id = ? AND formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            stmt.setInt(3, formationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last accessed: userId={}, formationId={}", userId, formationId, e);
        }
    }

    /**
     * Map ResultSet to Enrollment entity.
     */
    private Enrollment mapResultSet(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(rs.getInt("id"));
        enrollment.setFormationId(rs.getInt("formation_id"));
        enrollment.setUserId(rs.getInt("user_id"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                enrollment.setStatus(EnrollmentStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            }
        }
        
        enrollment.setProgress(rs.getDouble("progress"));
        
        Timestamp enrolledTs = rs.getTimestamp("enrolled_date");
        if (enrolledTs != null) {
            enrollment.setEnrolledDate(enrolledTs.toLocalDateTime());
        }
        
        Timestamp completedTs = rs.getTimestamp("completed_date");
        if (completedTs != null) {
            enrollment.setCompletedDate(completedTs.toLocalDateTime());
        }
        
        // Transient fields from JOINs
        enrollment.setFormationTitle(rs.getString("formation_title"));
        enrollment.setUserName(rs.getString("user_name"));
        
        return enrollment;
    }
}
