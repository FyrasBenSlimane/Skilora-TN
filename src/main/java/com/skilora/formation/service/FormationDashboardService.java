package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FormationDashboardService
 * 
 * Combined service providing both admin and user-specific dashboard statistics.
 * Merges DashboardService + UserDashboardService from the branch.
 * Uses direct SQL via DatabaseConfig.
 */
public class FormationDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(FormationDashboardService.class);
    private static volatile FormationDashboardService instance;

    private FormationDashboardService() {}

    public static FormationDashboardService getInstance() {
        if (instance == null) {
            synchronized (FormationDashboardService.class) {
                if (instance == null) {
                    instance = new FormationDashboardService();
                }
            }
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    // Admin Dashboard Statistics
    // ═══════════════════════════════════════════════════════════

    /**
     * Get total number of formations.
     */
    public int getTotalFormations() {
        String sql = "SELECT COUNT(*) as total FROM formations";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            logger.error("Error fetching total formations count", e);
        }
        return 0;
    }

    /**
     * Get number of processed formations (those with at least one module/lesson).
     */
    public int getProcessedFormations() {
        String sql = "SELECT COUNT(DISTINCT f.id) as processed FROM formations f " +
                "WHERE EXISTS (SELECT 1 FROM formation_modules fm WHERE fm.formation_id = f.id)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("processed");
        } catch (SQLException e) {
            logger.error("Error fetching processed formations count", e);
        }
        return 0;
    }

    /**
     * Get total number of certificates generated.
     */
    public int getCertificatesGenerated() {
        String sql = "SELECT COUNT(*) as total FROM certificates";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            logger.error("Error fetching certificates count", e);
        }
        return 0;
    }

    /**
     * Get all admin dashboard statistics in one call.
     */
    public DashboardStats getDashboardStats() {
        return new DashboardStats(
                getTotalFormations(),
                getProcessedFormations(),
                getCertificatesGenerated()
        );
    }

    // ═══════════════════════════════════════════════════════════
    // User-Specific Dashboard Statistics
    // ═══════════════════════════════════════════════════════════

    /**
     * Get total number of available formations.
     */
    public int getTotalAvailableFormations() {
        return getTotalFormations(); // Same query, reuse
    }

    /**
     * Get number of formations the user is enrolled in.
     */
    public int getEnrolledFormations(int userId) {
        String sql = "SELECT COUNT(*) as total FROM enrollments WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching enrolled formations count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Get number of formations the user has completed.
     */
    public int getCompletedFormations(int userId) {
        String sql = "SELECT COUNT(*) as total FROM enrollments WHERE user_id = ? AND (status = 'COMPLETED' OR completed = TRUE)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching completed formations count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Get number of certificates earned by the user.
     */
    public int getCertificatesEarned(int userId) {
        String sql = "SELECT COUNT(*) as total FROM certificates c " +
                "JOIN enrollments e ON c.enrollment_id = e.id " +
                "WHERE e.user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching certificates count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Calculate progress percentage (completed / enrolled).
     */
    public int getProgressPercentage(int userId) {
        int enrolled = getEnrolledFormations(userId);
        if (enrolled == 0) return 0;
        int completed = getCompletedFormations(userId);
        return (int) Math.round((completed * 100.0) / enrolled);
    }

    /**
     * Get all user dashboard statistics in one call.
     */
    public UserDashboardStats getUserDashboardStats(int userId) {
        return new UserDashboardStats(
                getTotalAvailableFormations(),
                getEnrolledFormations(userId),
                getCompletedFormations(userId),
                getCertificatesEarned(userId),
                getProgressPercentage(userId)
        );
    }

    // ═══════════════════════════════════════════════════════════
    // Inner Data Classes
    // ═══════════════════════════════════════════════════════════

    /**
     * Admin dashboard statistics.
     */
    public static class DashboardStats {
        private final int totalFormations;
        private final int processedFormations;
        private final int certificatesGenerated;

        public DashboardStats(int totalFormations, int processedFormations, int certificatesGenerated) {
            this.totalFormations = totalFormations;
            this.processedFormations = processedFormations;
            this.certificatesGenerated = certificatesGenerated;
        }

        public int getTotalFormations() { return totalFormations; }
        public int getProcessedFormations() { return processedFormations; }
        public int getCertificatesGenerated() { return certificatesGenerated; }
    }

    /**
     * User-specific dashboard statistics.
     */
    public static class UserDashboardStats {
        private final int totalAvailableFormations;
        private final int enrolledFormations;
        private final int completedFormations;
        private final int certificatesEarned;
        private final int progressPercentage;

        public UserDashboardStats(int totalAvailableFormations, int enrolledFormations,
                                  int completedFormations, int certificatesEarned, int progressPercentage) {
            this.totalAvailableFormations = totalAvailableFormations;
            this.enrolledFormations = enrolledFormations;
            this.completedFormations = completedFormations;
            this.certificatesEarned = certificatesEarned;
            this.progressPercentage = progressPercentage;
        }

        public int getTotalAvailableFormations() { return totalAvailableFormations; }
        public int getEnrolledFormations() { return enrolledFormations; }
        public int getCompletedFormations() { return completedFormations; }
        public int getCertificatesEarned() { return certificatesEarned; }
        public int getProgressPercentage() { return progressPercentage; }
    }
}
