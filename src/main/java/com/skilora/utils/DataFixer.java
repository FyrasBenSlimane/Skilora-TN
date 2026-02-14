package com.skilora.utils;

import com.skilora.config.DatabaseConfig;
import com.skilora.service.recruitment.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class to fix data inconsistencies in the database
 */
public class DataFixer {
    private static final Logger logger = LoggerFactory.getLogger(DataFixer.class);
    
    /**
     * Fixes applications that are linked to the wrong company.
     * Moves job offers from Company ID = 1 (or NULL) to the employer's company.
     * This method is more aggressive and will fix ALL job offers with applications
     * that are not linked to any employer's company.
     * 
     * @param employerUserId The user ID of the employer
     * @return Number of job offers fixed
     */
    public static int fixApplicationsForEmployer(int employerUserId) {
        try {
            // Get or create employer's company ID
            int employerCompanyId = JobService.getInstance().getOrCreateEmployerCompanyId(employerUserId, "Entreprise");
            if (employerCompanyId <= 0) {
                logger.error("DataFixer: Failed to get or create company for employer user ID: {}. Cannot fix data.", employerUserId);
                return 0;
            }
            logger.info("DataFixer: Employer user ID {} is associated with company ID {}", employerUserId, employerCompanyId);
            
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                int fixedCount = 0;
                
                // Strategy 1: Fix job offers with applications that are linked to invalid companies (1, NULL, 0)
                String fixInvalidCompaniesSql = """
                    UPDATE job_offers jo
                    SET jo.company_id = ?
                    WHERE (jo.company_id = 1 OR jo.company_id IS NULL OR jo.company_id = 0)
                    AND EXISTS (SELECT 1 FROM applications a WHERE a.job_offer_id = jo.id)
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(fixInvalidCompaniesSql)) {
                    stmt.setInt(1, employerCompanyId);
                    fixedCount = stmt.executeUpdate();
                    if (fixedCount > 0) {
                        logger.info("DataFixer: Fixed {} job offers with invalid company IDs", fixedCount);
                    }
                }
                
                // Strategy 2: Fix job offers that are linked to companies without an employer owner
                String fixOrphanedCompaniesSql = """
                    UPDATE job_offers jo
                    LEFT JOIN companies c ON jo.company_id = c.id
                    LEFT JOIN users u ON c.owner_id = u.id
                    SET jo.company_id = ?
                    WHERE jo.company_id IS NOT NULL
                    AND jo.company_id != ?
                    AND (c.owner_id IS NULL OR u.role != 'EMPLOYER')
                    AND EXISTS (SELECT 1 FROM applications a WHERE a.job_offer_id = jo.id)
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(fixOrphanedCompaniesSql)) {
                    stmt.setInt(1, employerCompanyId);
                    stmt.setInt(2, employerCompanyId);
                    int fixed = stmt.executeUpdate();
                    if (fixed > 0) {
                        logger.info("DataFixer: Fixed {} job offers linked to companies without employer owners", fixed);
                        fixedCount += fixed;
                    }
                }
                
                // Strategy 3: For ALL job offers with applications, ensure they're linked to an employer's company
                // This is the most aggressive fix - it will link any job offer with applications to this employer's company
                // if it's not already linked to a valid employer company
                String fixAllApplicationsSql = """
                    UPDATE job_offers jo
                    SET jo.company_id = ?
                    WHERE EXISTS (SELECT 1 FROM applications a WHERE a.job_offer_id = jo.id)
                    AND NOT EXISTS (
                        SELECT 1 FROM companies c2
                        INNER JOIN users u2 ON c2.owner_id = u2.id
                        WHERE c2.id = jo.company_id
                        AND u2.role = 'EMPLOYER'
                    )
                    AND jo.company_id != ?
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(fixAllApplicationsSql)) {
                    stmt.setInt(1, employerCompanyId);
                    stmt.setInt(2, employerCompanyId);
                    int fixed = stmt.executeUpdate();
                    if (fixed > 0) {
                        logger.info("DataFixer: Fixed {} additional job offers to ensure all applications are visible", fixed);
                        fixedCount += fixed;
                    }
                }
                
                logger.info("DataFixer: Total job offers fixed: {}", fixedCount);
                return fixedCount;
            }
            
        } catch (SQLException e) {
            logger.error("DataFixer: Error fixing applications for employer user ID: {}", employerUserId, e);
            return 0;
        } catch (Exception e) {
            logger.error("DataFixer: Unexpected error during data fix for employer user ID: {}", employerUserId, e);
            return 0;
        }
    }
    
    /**
     * Verifies that all applications for an employer are correctly linked.
     * 
     * @param employerUserId The user ID of the employer
     * @return Number of applications found for the employer
     */
    public static int verifyApplicationsForEmployer(int employerUserId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            String sql = "SELECT COUNT(*) as total " +
                    "FROM applications a " +
                    "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                    "JOIN companies c ON jo.company_id = c.id " +
                    "WHERE c.owner_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employerUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("total");
                        logger.info("Found {} applications for employer user ID: {}", count, employerUserId);
                        return count;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error verifying applications for employer user ID: {}", employerUserId, e);
        }
        return 0;
    }
}

