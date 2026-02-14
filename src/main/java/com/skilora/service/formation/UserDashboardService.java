package com.skilora.service.formation;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserDashboardService
 * 
 * Provides user-specific statistics for the user dashboard.
 * Fetches real-time data from the database filtered by user ID.
 */
public class UserDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(UserDashboardService.class);
    private static volatile UserDashboardService instance;

    private UserDashboardService() {
    }

    public static UserDashboardService getInstance() {
        if (instance == null) {
            synchronized (UserDashboardService.class) {
                if (instance == null) {
                    instance = new UserDashboardService();
                }
            }
        }
        return instance;
    }

    /**
     * Get total number of available formations.
     * 
     * @return total count, or 0 if error or no data
     */
    public int getTotalAvailableFormations() {
        String sql = "SELECT COUNT(*) as total FROM trainings";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching total available formations count", e);
        }
        return 0;
    }

    /**
     * Get number of formations the user is enrolled in.
     * 
     * @param userId the user ID
     * @return enrolled count, or 0 if error or no data
     */
    public int getEnrolledFormations(int userId) {
        String sql = "SELECT COUNT(*) as total FROM training_enrollments WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching enrolled formations count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Get number of formations the user has completed.
     * 
     * @param userId the user ID
     * @return completed count, or 0 if error or no data
     */
    public int getCompletedFormations(int userId) {
        String sql = "SELECT COUNT(*) as total FROM training_enrollments WHERE user_id = ? AND completed = TRUE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching completed formations count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Get number of certificates earned by the user.
     * 
     * @param userId the user ID
     * @return certificate count, or 0 if error or no data
     */
    public int getCertificatesEarned(int userId) {
        String sql = "SELECT COUNT(*) as total FROM certificates WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching certificates count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Calculate progress percentage (completed / enrolled).
     * 
     * @param userId the user ID
     * @return progress percentage (0-100), or 0 if no enrollments
     */
    public int getProgressPercentage(int userId) {
        int enrolled = getEnrolledFormations(userId);
        if (enrolled == 0) {
            return 0;
        }
        int completed = getCompletedFormations(userId);
        return (int) Math.round((completed * 100.0) / enrolled);
    }

    /**
     * Get all user dashboard statistics in one call.
     * 
     * @param userId the user ID
     * @return UserDashboardStats object with all statistics
     */
    public UserDashboardStats getUserDashboardStats(int userId) {
        int totalAvailable = getTotalAvailableFormations();
        int enrolled = getEnrolledFormations(userId);
        int completed = getCompletedFormations(userId);
        int certificates = getCertificatesEarned(userId);
        int progress = getProgressPercentage(userId);

        return new UserDashboardStats(
            totalAvailable,
            enrolled,
            completed,
            certificates,
            progress
        );
    }

    /**
     * Data class for user dashboard statistics.
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

        public int getTotalAvailableFormations() {
            return totalAvailableFormations;
        }

        public int getEnrolledFormations() {
            return enrolledFormations;
        }

        public int getCompletedFormations() {
            return completedFormations;
        }

        public int getCertificatesEarned() {
            return certificatesEarned;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }
    }
}
