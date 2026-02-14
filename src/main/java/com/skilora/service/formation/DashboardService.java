package com.skilora.service.formation;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DashboardService
 * 
 * Provides statistics for the admin dashboard.
 * Fetches real-time data from the database.
 */
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static volatile DashboardService instance;

    private DashboardService() {
    }

    public static DashboardService getInstance() {
        if (instance == null) {
            synchronized (DashboardService.class) {
                if (instance == null) {
                    instance = new DashboardService();
                }
            }
        }
        return instance;
    }

    /**
     * Get total number of formations (all trainings).
     * 
     * @return total count, or 0 if error or no data
     */
    public int getTotalFormations() {
        String sql = "SELECT COUNT(*) as total FROM trainings";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching total formations count", e);
        }
        return 0;
    }

    /**
     * Get number of processed formations.
     * Since Training entity doesn't have a status field, we consider
     * formations that have lessons as "processed" (they've been set up and are ready).
     * 
     * @return processed count, or 0 if error or no data
     */
    public int getProcessedFormations() {
        // Count formations that have at least one lesson (meaning they're complete/processed)
        String sql = "SELECT COUNT(DISTINCT t.id) as processed " +
                     "FROM trainings t " +
                     "WHERE EXISTS (SELECT 1 FROM training_lessons tl WHERE tl.training_id = t.id)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("processed");
            }
        } catch (SQLException e) {
            logger.error("Error fetching processed formations count", e);
            // Fallback: if query fails, return 0
        }
        return 0;
    }

    /**
     * Get number of certificates generated and ready.
     * 
     * @return certificate count, or 0 if error or no data
     */
    public int getCertificatesGenerated() {
        String sql = "SELECT COUNT(*) as total FROM certificates";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error fetching certificates count", e);
        }
        return 0;
    }

    /**
     * Get all dashboard statistics in one call.
     * 
     * @return DashboardStats object with all statistics
     */
    public DashboardStats getDashboardStats() {
        return new DashboardStats(
            getTotalFormations(),
            getProcessedFormations(),
            getCertificatesGenerated()
        );
    }

    /**
     * Data class for dashboard statistics.
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

        public int getTotalFormations() {
            return totalFormations;
        }

        public int getProcessedFormations() {
            return processedFormations;
        }

        public int getCertificatesGenerated() {
            return certificatesGenerated;
        }
    }
}
