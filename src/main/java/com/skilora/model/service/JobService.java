package com.skilora.model.service;

import com.google.gson.Gson;
import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.JobOffer;
import com.skilora.model.entity.JobOpportunity;
import com.skilora.model.enums.JobStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JobService
 * 
 * Handles all job-related business logic and data access.
 * This follows the MVC pattern where Service = Model layer (data + logic).
 * Merges JobOfferDAO and JobDAO functionality.
 * 
 * Note: No JavaFX imports allowed in this class.
 * Uses callbacks for async operations - the Controller handles
 * Platform.runLater().
 */
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private static final String FEED_PATH = "data/job_feed.json";

    // In-Memory Cache (Thread Safe)
    private static final List<JobOpportunity> CACHE = new CopyOnWriteArrayList<>();
    private static boolean isCacheWarm = false;

    private static volatile JobService instance;

    private JobService() {
        // Warm cache on init if empty
        if (!isCacheWarm) {
            reloadCacheFromJson();
        }
    }

    /**
     * Get singleton instance
     */
    public static JobService getInstance() {
        if (instance == null) {
            synchronized (JobService.class) {
                if (instance == null) {
                    instance = new JobService();
                }
            }
        }
        return instance;
    }

    // ==================== JobOffer CRUD Operations ====================

    /**
     * Creates a new job offer in the database.
     */
    public int createJobOffer(JobOffer jobOffer) throws SQLException {
        String sql = "INSERT INTO job_offers (company_id, title, description, requirements, location, " +
                "min_salary, max_salary, currency, work_type, status, posted_date, deadline) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, jobOffer.getEmployerId());
            stmt.setString(2, jobOffer.getTitle());
            stmt.setString(3, jobOffer.getDescription());
            stmt.setString(4, joinSkills(jobOffer.getRequiredSkills()));
            stmt.setString(5, jobOffer.getLocation());
            stmt.setDouble(6, jobOffer.getSalaryMin());
            stmt.setDouble(7, jobOffer.getSalaryMax());
            stmt.setString(8, jobOffer.getCurrency() != null ? jobOffer.getCurrency() : "TND");
            stmt.setString(9, jobOffer.getWorkType() != null ? jobOffer.getWorkType() : "ONSITE");
            stmt.setString(10, jobOffer.getStatus() != null ? jobOffer.getStatus().name() : JobStatus.OPEN.name());
            stmt.setDate(11, jobOffer.getPostedDate() != null ? Date.valueOf(jobOffer.getPostedDate())
                    : Date.valueOf(LocalDate.now()));
            stmt.setDate(12, jobOffer.getDeadline() != null ? Date.valueOf(jobOffer.getDeadline()) : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating job offer failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    jobOffer.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating job offer failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a job offer by its ID.
     */
    public JobOffer findJobOfferById(int id) throws SQLException {
        String sql = "SELECT * FROM job_offers WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToJobOffer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates an existing job offer.
     */
    public boolean updateJobOffer(JobOffer jobOffer) throws SQLException {
        String sql = "UPDATE job_offers SET title = ?, description = ?, requirements = ?, location = ?, " +
                "min_salary = ?, max_salary = ?, currency = ?, work_type = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, jobOffer.getTitle());
            stmt.setString(2, jobOffer.getDescription());
            stmt.setString(3, joinSkills(jobOffer.getRequiredSkills()));
            stmt.setString(4, jobOffer.getLocation());
            stmt.setDouble(5, jobOffer.getSalaryMin());
            stmt.setDouble(6, jobOffer.getSalaryMax());
            stmt.setString(7, jobOffer.getCurrency() != null ? jobOffer.getCurrency() : "TND");
            stmt.setString(8, jobOffer.getWorkType() != null ? jobOffer.getWorkType() : "ONSITE");
            stmt.setString(9, jobOffer.getStatus().name());
            stmt.setInt(10, jobOffer.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a job offer by ID.
     */
    public boolean deleteJobOffer(int id) throws SQLException {
        String sql = "DELETE FROM job_offers WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all job offers.
     */
    public List<JobOffer> findAllJobOffers() throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT * FROM job_offers ORDER BY posted_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }
        return jobOffers;
    }

    /**
     * Finds job offers by status.
     */
    public List<JobOffer> findJobOffersByStatus(JobStatus status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT * FROM job_offers WHERE status = ? ORDER BY posted_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobOffers.add(mapResultSetToJobOffer(rs));
                }
            }
        }
        return jobOffers;
    }

    /**
     * Searches job offers by keywords in title and description.
     */
    public List<JobOffer> searchJobOffersByKeywords(String keywords) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT * FROM job_offers WHERE " +
                "(LOWER(title) LIKE ? OR LOWER(description) LIKE ?) AND status = ? " +
                "ORDER BY posted_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + keywords.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, JobStatus.ACTIVE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobOffers.add(mapResultSetToJobOffer(rs));
                }
            }
        }
        return jobOffers;
    }

    /**
     * Finds all job offers for companies owned by a specific user.
     * Used by Employer "Mes Offres" and Admin views.
     */
    public List<JobOffer> findJobOffersByCompanyOwner(int ownerUserId) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT jo.* FROM job_offers jo " +
                "JOIN companies c ON jo.company_id = c.id " +
                "WHERE c.owner_id = ? ORDER BY jo.posted_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobOffers.add(mapResultSetToJobOffer(rs));
                }
            }
        }
        return jobOffers;
    }

    /**
     * Finds all job offers with company name (for admin overview).
     * Returns a map with "offer" and "companyName" keys.
     */
    public List<JobOffer> findAllJobOffersWithCompany() throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT jo.*, c.name AS company_name FROM job_offers jo " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "ORDER BY jo.posted_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JobOffer offer = mapResultSetToJobOffer(rs);
                // Store company name in description prefix for admin display
                String companyName = rs.getString("company_name");
                if (companyName != null) {
                    offer.setCompanyName(companyName);
                }
                jobOffers.add(offer);
            }
        }
        return jobOffers;
    }

    /**
     * Counts applications for a specific job offer.
     */
    public int countApplicationsForJob(int jobOfferId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE job_offer_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobOfferId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // ==================== Job Feed Operations ====================

    /**
     * Returns jobs from the high-speed in-memory cache.
     */
    public List<JobOpportunity> getJobsFromCache() {
        if (CACHE.isEmpty()) {
            reloadCacheFromJson();
        }
        return new ArrayList<>(CACHE);
    }

    /**
     * Reloads job feed from JSON file.
     */
    public void reloadCacheFromJson() {
        try {
            if (!Files.exists(Paths.get(FEED_PATH)))
                return;

            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(FEED_PATH));
            JobFeedResponse response = gson.fromJson(reader, JobFeedResponse.class);
            reader.close();

            if (response != null && response.jobs != null) {
                CACHE.clear();
                CACHE.addAll(response.jobs);
                isCacheWarm = true;

                // Async Persistence (Don't block)
                new Thread(() -> persistJobs(response.jobs)).start();
            }
        } catch (Exception e) {
            logger.error("Failed to reload job cache from JSON", e);
        }
    }

    /**
     * Refreshes the job feed (runs crawler and reloads cache).
     * Note: The callback handling of Platform.runLater() is done by Controller.
     */
    public void refreshFeed(Runnable onComplete) {
        Thread thread = new Thread(() -> {
            try {
                // 1. Run Crawler
                ProcessBuilder pb = new ProcessBuilder("python", "python/job_feed_crawler.py");
                pb.inheritIO();
                Process p = pb.start();
                p.waitFor();

                // 2. Reload Cache & Persist
                reloadCacheFromJson();

            } catch (Exception e) {
                logger.error("Failed to refresh job feed", e);
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ==================== External Job Persistence ====================

    /**
     * Gets or creates a system company for external job links.
     */
    public int getOrCreateSystemCompanyId() {
        return getOrCreateCompanyByName("Skilora Feed", "Aggregator", "Global", null);
    }

    /**
     * Gets or creates a company for an employer user.
     */
    public int getOrCreateEmployerCompanyId(int ownerUserId, String companyName) {
        // First check if user already owns a company
        String checkSql = "SELECT id FROM companies WHERE owner_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check existing company for owner user ID: {}", ownerUserId, e);
        }

        // Create new company for this employer
        String createSql = "INSERT INTO companies (owner_id, name, country) VALUES (?, ?, 'Tunisia')";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(createSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ownerUserId);
            stmt.setString(2, companyName != null ? companyName : "Entreprise");
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to create company for owner user ID: {}", ownerUserId, e);
        }
        return -1;
    }

    private int getOrCreateCompanyByName(String name, String industry, String country, Integer ownerId) {
        String checkSql = "SELECT id FROM companies WHERE name = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }

            String createSql = "INSERT INTO companies (name, industry, country, owner_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement cs = conn.prepareStatement(createSql, Statement.RETURN_GENERATED_KEYS)) {
                cs.setString(1, name);
                cs.setString(2, industry);
                cs.setString(3, country);
                if (ownerId != null) cs.setInt(4, ownerId);
                else cs.setNull(4, Types.INTEGER);
                cs.executeUpdate();
                try (ResultSet ids = cs.getGeneratedKeys()) {
                    if (ids.next()) return ids.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get or create company by name: {}", name, e);
        }
        return -1;
    }

    /**
     * Saves external job to database.
     */
    public void saveExternalJob(JobOpportunity job, int systemCompanyId) {
        if (systemCompanyId == -1)
            return;

        String checkSql = "SELECT id FROM job_offers WHERE title = ? AND company_id = ? AND description LIKE ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, job.getTitle());
            checkStmt.setInt(2, systemCompanyId);
            String descSig = (job.getDescription() != null && job.getDescription().length() > 50)
                    ? job.getDescription().substring(0, 50) + "%"
                    : (job.getDescription() + "%");
            checkStmt.setString(3, descSig);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next())
                    return; // Already exists
            }

            // Insert
            String insertSql = "INSERT INTO job_offers " +
                    "(company_id, title, description, location, posted_date, status, work_type) " +
                    "VALUES (?, ?, ?, ?, ?, 'OPEN', ?)";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, systemCompanyId);
                insertStmt.setString(2, job.getTitle());
                insertStmt.setString(3, job.getDescription());
                insertStmt.setString(4, job.getLocation());
                insertStmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

                String type = "ONSITE";
                String titleLower = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                String locationLower = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
                if (titleLower.contains("remote") || locationLower.contains("remote")) {
                    type = "REMOTE";
                }
                insertStmt.setString(6, type);

                insertStmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.error("Failed to save job: {}", job.getTitle(), e);
        }
    }

    private void persistJobs(List<JobOpportunity> jobs) {
        int sysId = getOrCreateSystemCompanyId();
        if (sysId == -1)
            return;

        for (JobOpportunity job : jobs) {
            saveExternalJob(job, sysId);
        }
    }

    // ==================== Helper Methods ====================

    private JobOffer mapResultSetToJobOffer(ResultSet rs) throws SQLException {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(rs.getInt("id"));
        jobOffer.setEmployerId(rs.getInt("company_id"));
        jobOffer.setTitle(rs.getString("title"));
        jobOffer.setDescription(rs.getString("description"));
        jobOffer.setLocation(rs.getString("location"));
        jobOffer.setSalaryMin(rs.getDouble("min_salary"));
        jobOffer.setSalaryMax(rs.getDouble("max_salary"));
        jobOffer.setCurrency(rs.getString("currency"));
        jobOffer.setWorkType(rs.getString("work_type"));
        jobOffer.setRequiredSkills(splitSkills(rs.getString("requirements")));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                jobOffer.setStatus(JobStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                jobOffer.setStatus(JobStatus.DRAFT);
            }
        }

        Date postedDate = rs.getDate("posted_date");
        if (postedDate != null) {
            jobOffer.setPostedDate(postedDate.toLocalDate());
        }

        Date deadline = rs.getDate("deadline");
        if (deadline != null) {
            jobOffer.setDeadline(deadline.toLocalDate());
        }

        return jobOffer;
    }

    private String joinSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        return String.join(",", skills);
    }

    private List<String> splitSkills(String skillsStr) {
        if (skillsStr == null || skillsStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(skillsStr.split(",")));
    }

    // Helper class for JSON structure
    private static class JobFeedResponse {
        List<JobOpportunity> jobs;
    }
}
