package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;
import com.skilora.recruitment.entity.MatchingScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ApplicationService
 * 
 * Handles all CRUD operations for job applications.
 * Maps to the 'applications' table in the database.
 */
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private static ApplicationService instance;

    private ApplicationService() {}

    public static synchronized ApplicationService getInstance() {
        if (instance == null) {
            instance = new ApplicationService();
        }
        return instance;
    }

    /**
     * Submit a new application for a job offer.
     *
     * @return the generated application ID, or -1 on failure
     */
    public int apply(int jobOfferId, int candidateProfileId, String coverLetter) throws SQLException {
        return apply(jobOfferId, candidateProfileId, coverLetter, null);
    }

    /**
     * Submit a new application for a job offer with optional custom CV URL.
     *
     * @return the generated application ID, or -1 on failure
     */
    public int apply(int jobOfferId, int candidateProfileId, String coverLetter, String cvUrl) throws SQLException {
        // Link application to an employer-owned offer when possible (same title), so it appears in employer inbox
        jobOfferId = JobService.getInstance().resolveEmployerJobOfferId(jobOfferId);
        if (jobOfferId <= 0) return -1;

        // Calculate match scores
        int matchPercentage = 0;
        int candidateScore = 0;
        try {
            var jobOffer = JobService.getInstance().getById(jobOfferId);
            if (jobOffer != null && jobOffer.isPresent()) {
                MatchingScore score = MatchingService.getInstance().calculateMatch(candidateProfileId, jobOffer.get());
                if (score != null) {
                    matchPercentage = (int) score.getTotalScore();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to calculate match scores for profile {} and job {}: {}", 
                candidateProfileId, jobOfferId, e.getMessage());
        }

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if already applied (within same transaction to prevent TOCTOU race)
                String checkSql = "SELECT 1 FROM applications WHERE job_offer_id = ? AND candidate_profile_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, jobOfferId);
                    checkStmt.setInt(2, candidateProfileId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return -1; // Already applied
                        }
                    }
                }

                String sql = "INSERT INTO applications (job_offer_id, candidate_profile_id, status, applied_date, cover_letter, custom_cv_url, match_percentage, candidate_score) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, jobOfferId);
                    stmt.setInt(2, candidateProfileId);
                    stmt.setString(3, Status.PENDING.name());
                    stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(5, coverLetter);
                    stmt.setString(6, cvUrl);
                    stmt.setInt(7, matchPercentage);
                    stmt.setInt(8, candidateScore);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        try (ResultSet keys = stmt.getGeneratedKeys()) {
                            if (keys.next()) {
                                int id = keys.getInt(1);
                                conn.commit();
                                return id;
                            }
                        }
                    }
                }
                conn.rollback();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return -1;
    }

    /**
     * Check if a candidate has already applied to a specific job.
     */
    public boolean hasApplied(int jobOfferId, int candidateProfileId) throws SQLException {
        String sql = "SELECT 1 FROM applications WHERE job_offer_id = ? AND candidate_profile_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobOfferId);
            stmt.setInt(2, candidateProfileId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Get all applications for a specific candidate profile (Job Seeker view).
     * Includes job title and company name via JOINs.
     */
    public List<Application> getApplicationsByProfile(int profileId) throws SQLException {
        String sql = "SELECT a.*, jo.title AS job_title, jo.location AS job_location, " +
                "c.name AS company_name " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "WHERE a.candidate_profile_id = ? " +
                "ORDER BY a.applied_date DESC";

        List<Application> apps = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setJobLocation(rs.getString("job_location"));
                    app.setCompanyName(rs.getString("company_name"));
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    /**
     * Get all applications for a specific job offer (Employer view).
     * Includes candidate name via JOINs.
     */
    public List<Application> getApplicationsByJobOffer(int jobOfferId) throws SQLException {
        String sql = "SELECT a.*, jo.title AS job_title, " +
                "CONCAT(p.first_name, ' ', p.last_name) AS candidate_name " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "JOIN profiles p ON a.candidate_profile_id = p.id " +
                "WHERE a.job_offer_id = ? " +
                "ORDER BY a.applied_date DESC";

        List<Application> apps = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobOfferId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setCandidateName(rs.getString("candidate_name"));
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    /**
     * Get all applications for job offers belonging to a specific company (Employer inbox).
     * Uses application -> job_offer -> company_id so retrieval does not depend on companies.owner_id.
     * Use this with the company id from JobService.getOrCreateEmployerCompanyId(ownerUserId, ...).
     */
    public List<Application> getApplicationsByCompanyId(int companyId) throws SQLException {
        if (companyId <= 0) return new ArrayList<>();
        String sql = "SELECT a.*, jo.title AS job_title, jo.location AS job_location, " +
                "COALESCE(NULLIF(TRIM(CONCAT(IFNULL(p.first_name,''), ' ', IFNULL(p.last_name,''))), ''), CONCAT('Candidat #', a.candidate_profile_id)) AS candidate_name, " +
                "c.name AS company_name " +
                "FROM applications a " +
                "INNER JOIN job_offers jo ON a.job_offer_id = jo.id AND jo.company_id = ? " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "ORDER BY a.applied_date DESC";

        List<Application> apps = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setJobLocation(rs.getString("job_location"));
                    String candidateName = rs.getString("candidate_name");
                    app.setCandidateName(candidateName != null && !candidateName.isBlank() ? candidateName : "Candidat #" + app.getCandidateProfileId());
                    app.setCompanyName(rs.getString("company_name"));
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    /**
     * Get all applications for all jobs owned by a specific company (Employer inbox).
     * Filters by companies.owner_id; prefer getApplicationsByCompanyId with resolved company id for robustness.
     */
    public List<Application> getApplicationsByCompanyOwner(int ownerUserId) throws SQLException {
        String sql = "SELECT a.*, jo.title AS job_title, jo.location AS job_location, " +
                "COALESCE(NULLIF(TRIM(CONCAT(IFNULL(p.first_name,''), ' ', IFNULL(p.last_name,''))), ''), CONCAT('Candidat #', a.candidate_profile_id)) AS candidate_name, " +
                "c.name AS company_name " +
                "FROM applications a " +
                "INNER JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "INNER JOIN companies c ON jo.company_id = c.id AND c.owner_id = ? " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "ORDER BY a.applied_date DESC";

        List<Application> apps = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setJobLocation(rs.getString("job_location"));
                    String candidateName = rs.getString("candidate_name");
                    app.setCandidateName(candidateName != null && !candidateName.isBlank() ? candidateName : "Candidat #" + app.getCandidateProfileId());
                    app.setCompanyName(rs.getString("company_name"));
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    /**
     * Update application status.
     */
    public boolean updateStatus(int applicationId, Status newStatus) throws SQLException {
        String sql = "UPDATE applications SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, applicationId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Get a single application by ID with full details.
     * Convenience wrapper around getById().
     */
    public Application getApplicationById(int applicationId) throws SQLException {
        Optional<Application> opt = getById(applicationId);
        return opt.orElse(null);
    }

    /**
     * Get a single application by ID.
     */
    public Optional<Application> getById(int id) throws SQLException {
        String sql = "SELECT a.*, jo.title AS job_title, " +
                "CONCAT(p.first_name, ' ', p.last_name) AS candidate_name " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "JOIN profiles p ON a.candidate_profile_id = p.id " +
                "WHERE a.id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setCandidateName(rs.getString("candidate_name"));
                    return Optional.of(app);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Delete an application.
     */
    public boolean delete(int applicationId) throws SQLException {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Count applications by status for a candidate profile.
     */
    public int countByProfileAndStatus(int profileId, Status status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE candidate_profile_id = ? AND status = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ==================== Helper ====================

    private Application mapResultSet(ResultSet rs) throws SQLException {
        Application app = new Application();
        app.setId(rs.getInt("id"));
        app.setJobOfferId(rs.getInt("job_offer_id"));
        app.setCandidateProfileId(rs.getInt("candidate_profile_id"));
        app.setCoverLetter(rs.getString("cover_letter"));
        app.setCustomCvUrl(rs.getString("custom_cv_url"));
        
        // Read AI match scores
        app.setMatchPercentage((int) rs.getDouble("match_percentage"));
        app.setCandidateScore(rs.getInt("candidate_score"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                app.setStatus(Status.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                app.setStatus(Status.PENDING);
            }
        }

        Timestamp ts = rs.getTimestamp("applied_date");
        if (ts != null) {
            app.setAppliedDate(ts.toLocalDateTime());
        }

        return app;
    }
}
