package com.skilora.utils;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DataFixer - Utility for verifying and repairing recruitment data linkage.
 *
 * Ensures that applications are correctly linked to employer-owned job offers
 * via the companies → job_offers → applications chain.
 */
public final class DataFixer {

    private static final Logger logger = LoggerFactory.getLogger(DataFixer.class);

    private DataFixer() {}

    /**
     * Count applications linked to job offers owned by the employer's company.
     *
     * @param employerUserId the employer user ID
     * @return number of applications found, 0 if none
     */
    public static int verifyApplicationsForEmployer(int employerUserId) {
        String sql = "SELECT COUNT(*) AS cnt " +
                "FROM applications a " +
                "INNER JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "INNER JOIN companies c ON jo.company_id = c.id " +
                "WHERE c.owner_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        } catch (Exception e) {
            logger.error("verifyApplicationsForEmployer failed for user {}", employerUserId, e);
        }
        return 0;
    }

    /**
     * Attempt to fix orphaned applications by linking them to the employer's
     * job offers when a title match exists.
     *
     * This handles the case where applications were created against a
     * "feed" job_offer row that is not associated with a company, while
     * the employer has a matching offer (same title) under their company.
     *
     * @param employerUserId the employer user ID
     * @return number of applications re-linked
     */
    public static int fixApplicationsForEmployer(int employerUserId) {
        int fixed = 0;
        String sql = "UPDATE applications a " +
                "INNER JOIN job_offers feed_jo ON a.job_offer_id = feed_jo.id AND feed_jo.company_id IS NULL " +
                "INNER JOIN job_offers emp_jo ON LOWER(TRIM(emp_jo.title)) = LOWER(TRIM(feed_jo.title)) " +
                "INNER JOIN companies c ON emp_jo.company_id = c.id AND c.owner_id = ? " +
                "SET a.job_offer_id = emp_jo.id " +
                "WHERE a.job_offer_id != emp_jo.id";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerUserId);
            fixed = stmt.executeUpdate();
            if (fixed > 0) {
                logger.info("DataFixer: re-linked {} application(s) to employer {} offers by title match", fixed, employerUserId);
            }
        } catch (Exception e) {
            logger.warn("fixApplicationsForEmployer failed for user {}. This is non-fatal.", employerUserId, e);
        }
        return fixed;
    }
}
