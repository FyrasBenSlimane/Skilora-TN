package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;

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
        // Check if already applied
        if (hasApplied(jobOfferId, candidateProfileId)) {
            return -1; // Already applied
        }

        String sql = "INSERT INTO applications (job_offer_id, candidate_profile_id, status, applied_date, cover_letter) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, jobOfferId);
            stmt.setInt(2, candidateProfileId);
            stmt.setString(3, Status.PENDING.name());
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(5, coverLetter);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
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
     * Get all applications for all jobs owned by a specific company (Employer inbox).
     * Includes candidate name and job title via JOINs.
     */
    public List<Application> getApplicationsByCompanyOwner(int ownerUserId) throws SQLException {
        String sql = "SELECT a.*, jo.title AS job_title, jo.location AS job_location, " +
                "CONCAT(p.first_name, ' ', p.last_name) AS candidate_name, " +
                "c.name AS company_name " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "JOIN companies c ON jo.company_id = c.id " +
                "JOIN profiles p ON a.candidate_profile_id = p.id " +
                "WHERE c.owner_id = ? " +
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
                    app.setCandidateName(rs.getString("candidate_name"));
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
