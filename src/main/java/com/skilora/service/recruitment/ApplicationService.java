package com.skilora.service.recruitment;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Application.Status;
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
    private final RecruitmentIntelligenceService intelligenceService = RecruitmentIntelligenceService.getInstance();

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

    public int apply(int jobOfferId, int candidateProfileId, String coverLetter, String cvUrl) throws SQLException {
        // Check if already applied
        if (hasApplied(jobOfferId, candidateProfileId)) {
            return -1; // Already applied
        }

        // Verify that the job offer exists and is linked to a valid company
        // This ensures the application will be visible to the employer
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            String verifyJobOfferSql = """
                SELECT jo.id, jo.company_id, c.owner_id
                FROM job_offers jo
                LEFT JOIN companies c ON jo.company_id = c.id
                WHERE jo.id = ?
                """;
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifyJobOfferSql)) {
                verifyStmt.setInt(1, jobOfferId);
                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (!rs.next()) {
                        logger.error("Job offer ID {} does not exist. Cannot create application.", jobOfferId);
                        return -1;
                    }
                    
                    int companyId = rs.getInt("company_id");
                    Integer ownerId = rs.getObject("owner_id") != null ? rs.getInt("owner_id") : null;
                    
                    // If job offer is not linked to a valid company (company_id = 1, NULL, or 0),
                    // or the company doesn't have an owner, try to fix it
                    if (companyId <= 0 || companyId == 1 || ownerId == null) {
                        logger.warn("Job offer ID {} is linked to invalid company (company_id: {}, owner_id: {}). Attempting to fix...", 
                            jobOfferId, companyId, ownerId);
                        
                        // Try to find an employer's company to link this job offer to
                        String findEmployerCompanySql = """
                            SELECT c.id
                            FROM companies c
                            INNER JOIN users u ON c.owner_id = u.id
                            WHERE u.role = 'EMPLOYER'
                            ORDER BY c.id
                            LIMIT 1
                            """;
                        try (PreparedStatement findStmt = conn.prepareStatement(findEmployerCompanySql);
                             ResultSet companyRs = findStmt.executeQuery()) {
                            if (companyRs.next()) {
                                int employerCompanyId = companyRs.getInt("id");
                                String fixJobOfferSql = "UPDATE job_offers SET company_id = ? WHERE id = ?";
                                try (PreparedStatement fixStmt = conn.prepareStatement(fixJobOfferSql)) {
                                    fixStmt.setInt(1, employerCompanyId);
                                    fixStmt.setInt(2, jobOfferId);
                                    int fixed = fixStmt.executeUpdate();
                                    if (fixed > 0) {
                                        logger.info("Fixed job offer ID {} by linking it to company ID {}", jobOfferId, employerCompanyId);
                                        companyId = employerCompanyId;
                                    }
                                }
                            } else {
                                // No employer company exists, create one
                                logger.warn("No employer company found. Creating default company...");
                                // This will be handled by the system, but we'll proceed with the application
                            }
                        }
                    }
                }
            }
            
            // Now create the application
            String sql = "INSERT INTO applications (job_offer_id, candidate_profile_id, status, applied_date, cover_letter, custom_cv_url) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, jobOfferId);
                stmt.setInt(2, candidateProfileId);
                stmt.setString(3, Status.PENDING.name());
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(5, coverLetter);
                stmt.setString(6, cvUrl);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int applicationId = keys.getInt(1);
                            logger.info("Created application ID {} for job offer ID {} (candidate profile ID: {})", 
                                applicationId, jobOfferId, candidateProfileId);
                            intelligenceService.calculateCompatibility(candidateProfileId, jobOfferId);
                            intelligenceService.scoreCandidate(candidateProfileId);
                            return applicationId;
                        }
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
                "c.name AS company_name, cms.match_percentage, cs.score AS candidate_score " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN candidate_match_scores cms ON cms.profile_id = a.candidate_profile_id AND cms.job_offer_id = a.job_offer_id " +
                "LEFT JOIN candidate_scores cs ON cs.profile_id = a.candidate_profile_id " +
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
                    app.setMatchPercentage(rs.getInt("match_percentage"));
                    app.setCandidateScore(rs.getInt("candidate_score"));
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
                "CONCAT(p.first_name, ' ', p.last_name) AS candidate_name, cms.match_percentage, cs.score AS candidate_score " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "JOIN profiles p ON a.candidate_profile_id = p.id " +
                "LEFT JOIN candidate_match_scores cms ON cms.profile_id = a.candidate_profile_id AND cms.job_offer_id = a.job_offer_id " +
                "LEFT JOIN candidate_scores cs ON cs.profile_id = a.candidate_profile_id " +
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
                    app.setMatchPercentage(rs.getInt("match_percentage"));
                    app.setCandidateScore(rs.getInt("candidate_score"));
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    /**
     * Get all applications for all jobs owned by a specific company (Employer inbox).
     * Includes candidate name and job title via JOINs.
     * This method automatically fixes data relationships if needed.
     */
    public List<Application> getApplicationsByCompanyOwner(int ownerUserId) throws SQLException {
        logger.info("Getting applications for company owner user ID: {}", ownerUserId);
        
        // First, ensure the employer has a company
        JobService jobService = JobService.getInstance();
        int companyId = jobService.getOrCreateEmployerCompanyId(ownerUserId, "Entreprise");
        if (companyId <= 0) {
            logger.warn("No company found or created for employer user ID: {}. Returning empty list.", ownerUserId);
            return new ArrayList<>();
        }
        logger.info("Using company ID: {} for employer user ID: {}", companyId, ownerUserId);
        
        // Try to fix any orphaned applications before querying
        try {
            com.skilora.utils.DataFixer.fixApplicationsForEmployer(ownerUserId);
        } catch (Exception e) {
            logger.warn("Could not fix applications before query: {}", e.getMessage());
        }
        
        // Get ALL applications for job offers owned by this user's company
        // Use LEFT JOINs to ensure we don't miss any applications
        // Check both direct company ownership and company_id match
        String sql = "SELECT DISTINCT a.*, " +
                "COALESCE(jo.title, CONCAT('Offre #', a.job_offer_id)) AS job_title, " +
                "jo.location AS job_location, " +
                "CASE " +
                "  WHEN p.first_name IS NOT NULL AND p.last_name IS NOT NULL THEN CONCAT(p.first_name, ' ', p.last_name) " +
                "  WHEN p.first_name IS NOT NULL THEN p.first_name " +
                "  ELSE CONCAT('Candidat #', a.candidate_profile_id) " +
                "END AS candidate_name, " +
                "COALESCE(c.name, 'Entreprise') AS company_name, cms.match_percentage, cs.score AS candidate_score " +
                "FROM applications a " +
                "LEFT JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "LEFT JOIN candidate_match_scores cms ON cms.profile_id = a.candidate_profile_id AND cms.job_offer_id = a.job_offer_id " +
                "LEFT JOIN candidate_scores cs ON cs.profile_id = a.candidate_profile_id " +
                "WHERE (c.owner_id = ? OR jo.company_id = ?) " +
                "ORDER BY a.applied_date DESC";

        List<Application> apps = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ownerUserId);
            stmt.setInt(2, companyId);
            logger.info("Executing query for owner user ID: {} (company ID: {}) - retrieving ALL applications", ownerUserId, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setJobLocation(rs.getString("job_location"));
                    String candidateName = rs.getString("candidate_name");
                    if (candidateName == null || candidateName.trim().isEmpty()) {
                        candidateName = "Candidat #" + app.getCandidateProfileId();
                    }
                    app.setCandidateName(candidateName);
                    app.setCompanyName(rs.getString("company_name"));
                    app.setMatchPercentage(rs.getInt("match_percentage"));
                    app.setCandidateScore(rs.getInt("candidate_score"));
                    apps.add(app);
                    count++;
                    logger.debug("Found application ID: {} for job offer ID: {} (title: {})", 
                        app.getId(), app.getJobOfferId(), app.getJobTitle());
                }
                logger.info("Total applications found: {} for owner user ID: {} (company ID: {})", count, ownerUserId, companyId);
                
                // If no applications found, log diagnostic info
                if (count == 0) {
                    logDiagnosticInfo(conn, ownerUserId, companyId);
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error while fetching applications for owner user ID: {}", ownerUserId, e);
            e.printStackTrace();
            throw e;
        }
        return apps;
    }
    
    /**
     * Log diagnostic information when no applications are found.
     */
    private void logDiagnosticInfo(Connection conn, int ownerUserId, int companyId) {
        try {
            // Check total applications
            try (java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as total FROM applications");
                 java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("DIAGNOSTIC: Total applications in database: {}", rs.getInt("total"));
                }
            }
            
            // Check job offers for this company
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM job_offers WHERE company_id = ?")) {
                stmt.setInt(1, companyId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        logger.info("DIAGNOSTIC: Job offers for company ID {}: {}", companyId, rs.getInt("count"));
                    }
                }
            }
            
            // Check applications for job offers of this company
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM applications a " +
                    "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                    "WHERE jo.company_id = ?")) {
                stmt.setInt(1, companyId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        logger.info("DIAGNOSTIC: Applications for job offers of company ID {}: {}", companyId, rs.getInt("count"));
                    }
                }
            }
            
            // Check if there are applications linked to wrong company
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                    "SELECT a.id, a.job_offer_id, jo.company_id, c.owner_id " +
                    "FROM applications a " +
                    "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                    "LEFT JOIN companies c ON jo.company_id = c.id " +
                    "WHERE c.owner_id != ? OR c.owner_id IS NULL " +
                    "LIMIT 10")) {
                stmt.setInt(1, ownerUserId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        logger.warn("DIAGNOSTIC: Found application ID {} linked to job offer ID {} with company ID {} (owner: {})", 
                            rs.getInt("a.id"), rs.getInt("a.job_offer_id"), 
                            rs.getObject("jo.company_id"), rs.getObject("c.owner_id"));
                    }
                    if (count > 0) {
                        logger.warn("DIAGNOSTIC: Found {} applications linked to wrong company. These need to be fixed.");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error in diagnostic logging", e);
        }
    }

    /**
     * Update application status.
     * When status is ACCEPTED, automatically add to interview_candidates table.
     */
    public boolean updateStatus(int applicationId, Status newStatus) throws SQLException {
        Connection conn = DatabaseConfig.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            
            // Update application status
        String sql = "UPDATE applications SET status = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, applicationId);
                int rows = stmt.executeUpdate();
                
                if (rows > 0 && newStatus == Status.ACCEPTED) {
                    // Add to interview_candidates table when application is accepted
                    addToInterviewCandidates(conn, applicationId);
                } else if (rows > 0 && newStatus != Status.ACCEPTED) {
                    // Remove from interview_candidates if status changes from ACCEPTED
                    removeFromInterviewCandidates(conn, applicationId);
                }
                
                conn.commit();
                if (rows > 0) {
                    sendStatusChangeNotification(applicationId, newStatus);
                }
                return rows > 0;
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * Add application to interview_candidates table when accepted.
     */
    private void addToInterviewCandidates(Connection conn, int applicationId) throws SQLException {
        // Get application details
        String selectSql = "SELECT candidate_profile_id, job_offer_id FROM applications WHERE id = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, applicationId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    int candidateProfileId = rs.getInt("candidate_profile_id");
                    int jobOfferId = rs.getInt("job_offer_id");
                    
                    // Get company_id from job_offer
                    int companyId = 0;
                    String companySql = "SELECT company_id FROM job_offers WHERE id = ?";
                    try (PreparedStatement companyStmt = conn.prepareStatement(companySql)) {
                        companyStmt.setInt(1, jobOfferId);
                        try (ResultSet companyRs = companyStmt.executeQuery()) {
                            if (companyRs.next()) {
                                companyId = companyRs.getInt("company_id");
                            }
                        }
                    }
                    
                    // Insert into interview_candidates if not already exists
                    String insertSql = """
                        INSERT INTO interview_candidates (application_id, candidate_profile_id, job_offer_id, company_id, status)
                        VALUES (?, ?, ?, ?, 'ELIGIBLE')
                        ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP
                        """;
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, applicationId);
                        insertStmt.setInt(2, candidateProfileId);
                        insertStmt.setInt(3, jobOfferId);
                        insertStmt.setInt(4, companyId);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }
    
    /**
     * Remove application from interview_candidates table when status changes from ACCEPTED.
     */
    private void removeFromInterviewCandidates(Connection conn, int applicationId) throws SQLException {
        String sql = "DELETE FROM interview_candidates WHERE application_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.executeUpdate();
        }
    }

    /**
     * Get a single application by ID with full details.
     */
    public Application getApplicationById(int applicationId) throws SQLException {
        Optional<Application> opt = getById(applicationId);
        if (opt.isPresent()) {
            Application app = opt.get();
            // Load additional details if needed
            return app;
        }
        return null;
    }

    /**
     * Get a single application by ID (legacy method).
     */
    public Optional<Application> getById(int id) throws SQLException {
        String sql = "SELECT a.*, " +
                "jo.title AS job_title, " +
                "jo.location AS job_location, " +
                "CASE " +
                "  WHEN p.first_name IS NOT NULL AND p.last_name IS NOT NULL THEN CONCAT(p.first_name, ' ', p.last_name) " +
                "  WHEN p.first_name IS NOT NULL THEN p.first_name " +
                "  ELSE CONCAT('Candidat #', a.candidate_profile_id) " +
                "END AS candidate_name, " +
                "COALESCE(c.name, 'Entreprise') AS company_name, cms.match_percentage, cs.score AS candidate_score " +
                "FROM applications a " +
                "INNER JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "LEFT JOIN candidate_match_scores cms ON cms.profile_id = a.candidate_profile_id AND cms.job_offer_id = a.job_offer_id " +
                "LEFT JOIN candidate_scores cs ON cs.profile_id = a.candidate_profile_id " +
                "WHERE a.id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Application app = mapResultSet(rs);
                    app.setJobTitle(rs.getString("job_title"));
                    app.setJobLocation(rs.getString("job_location"));
                    String candidateName = rs.getString("candidate_name");
                    if (candidateName == null || candidateName.trim().isEmpty()) {
                        candidateName = "Candidat #" + app.getCandidateProfileId();
                    }
                    app.setCandidateName(candidateName);
                    app.setCompanyName(rs.getString("company_name"));
                    app.setMatchPercentage(rs.getInt("match_percentage"));
                    app.setCandidateScore(rs.getInt("candidate_score"));
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

    private void sendStatusChangeNotification(int applicationId, Status newStatus) {
        // Email/SMS notifications removed - only interview scheduling sends SMS
        // Status changes are handled in the UI only
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
