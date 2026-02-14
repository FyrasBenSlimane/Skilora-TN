package com.skilora.service.recruitment;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.recruitment.Interview.InterviewStatus;
import com.skilora.model.entity.recruitment.Interview.InterviewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * InterviewService
 * 
 * Handles all interview-related business logic and data access.
 * Manages scheduling, updating, and retrieving interviews.
 */
public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);
    private static volatile InterviewService instance;

    private InterviewService() {}

    public static synchronized InterviewService getInstance() {
        if (instance == null) {
            instance = new InterviewService();
        }
        return instance;
    }

    /**
     * Schedule a new interview for an accepted application.
     * 
     * @param applicationId The application ID (must be ACCEPTED status)
     * @param interviewDate The scheduled date and time
     * @param location Interview location
     * @param interviewType Type of interview (IN_PERSON, VIDEO, etc.)
     * @param notes Additional notes
     * @return The created interview ID, or -1 on failure
     */
    public int scheduleInterview(int applicationId, LocalDateTime interviewDate, String location, 
                                 InterviewType interviewType, String notes) throws SQLException {
        // Verify application exists and is ACCEPTED
        if (!isApplicationAccepted(applicationId)) {
            throw new SQLException("Cannot schedule interview: Application is not in ACCEPTED status");
        }

        // Check if interview already exists for this application
        if (hasInterview(applicationId)) {
            throw new SQLException("Interview already scheduled for this application");
        }

        String sql = "INSERT INTO interviews (application_id, interview_date, location, interview_type, notes, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, applicationId);
            stmt.setTimestamp(2, Timestamp.valueOf(interviewDate));
            stmt.setString(3, location);
            stmt.setString(4, interviewType != null ? interviewType.name() : InterviewType.IN_PERSON.name());
            stmt.setString(5, notes);
            stmt.setString(6, InterviewStatus.SCHEDULED.name());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int interviewId = keys.getInt(1);
                        logger.info("Scheduled interview ID {} for application ID {}", interviewId, applicationId);
                        return interviewId;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Update an existing interview.
     */
    public boolean updateInterview(int interviewId, LocalDateTime interviewDate, String location,
                                   InterviewType interviewType, String notes, InterviewStatus status) throws SQLException {
        String sql = "UPDATE interviews SET interview_date = ?, location = ?, interview_type = ?, notes = ?, status = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(interviewDate));
            stmt.setString(2, location);
            stmt.setString(3, interviewType != null ? interviewType.name() : InterviewType.IN_PERSON.name());
            stmt.setString(4, notes);
            stmt.setString(5, status != null ? status.name() : InterviewStatus.SCHEDULED.name());
            stmt.setInt(6, interviewId);

            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Get interview by ID.
     */
    public Optional<Interview> getInterviewById(int interviewId) throws SQLException {
        String sql = """
                SELECT i.*, 
                       a.candidate_profile_id,
                       p.first_name, p.last_name,
                       jo.title as job_title,
                       c.name as company_name
                FROM interviews i
                JOIN applications a ON i.application_id = a.id
                JOIN profiles p ON a.candidate_profile_id = p.id
                JOIN job_offers jo ON a.job_offer_id = jo.id
                LEFT JOIN companies c ON jo.company_id = c.id
                WHERE i.id = ?
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, interviewId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInterview(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get interview by application ID.
     */
    public Optional<Interview> getInterviewByApplicationId(int applicationId) throws SQLException {
        String sql = """
                SELECT i.*, 
                       a.candidate_profile_id,
                       p.first_name, p.last_name,
                       jo.title as job_title,
                       c.name as company_name
                FROM interviews i
                JOIN applications a ON i.application_id = a.id
                JOIN profiles p ON a.candidate_profile_id = p.id
                JOIN job_offers jo ON a.job_offer_id = jo.id
                LEFT JOIN companies c ON jo.company_id = c.id
                WHERE i.application_id = ?
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, applicationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInterview(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get all interviews for an employer (all accepted applications from their companies).
     */
    public List<Interview> getInterviewsForEmployer(int employerUserId) throws SQLException {
        String sql = """
                SELECT i.*, 
                       a.candidate_profile_id,
                       p.first_name, p.last_name,
                       jo.title as job_title,
                       c.name as company_name
                FROM interviews i
                JOIN applications a ON i.application_id = a.id
                JOIN job_offers jo ON a.job_offer_id = jo.id
                JOIN companies c ON jo.company_id = c.id
                JOIN profiles p ON a.candidate_profile_id = p.id
                WHERE c.owner_id = ? AND a.status = 'ACCEPTED'
                ORDER BY i.interview_date ASC
                """;

        List<Interview> interviews = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, employerUserId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interviews.add(mapResultSetToInterview(rs));
                }
            }
        }
        return interviews;
    }

    /**
     * Get all interviews for a candidate (all their accepted applications with interviews).
     */
    public List<Interview> getInterviewsForCandidate(int candidateProfileId) throws SQLException {
        String sql = """
                SELECT i.*, 
                       a.candidate_profile_id,
                       p.first_name, p.last_name,
                       jo.title as job_title,
                       c.name as company_name
                FROM interviews i
                JOIN applications a ON i.application_id = a.id
                JOIN profiles p ON a.candidate_profile_id = p.id
                JOIN job_offers jo ON a.job_offer_id = jo.id
                LEFT JOIN companies c ON jo.company_id = c.id
                WHERE a.candidate_profile_id = ? AND a.status = 'ACCEPTED'
                ORDER BY i.interview_date ASC
                """;

        List<Interview> interviews = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, candidateProfileId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interviews.add(mapResultSetToInterview(rs));
                }
            }
        }
        return interviews;
    }

    /**
     * Delete an interview.
     */
    public boolean deleteInterview(int interviewId) throws SQLException {
        String sql = "DELETE FROM interviews WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, interviewId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Check if application is in ACCEPTED status.
     */
    private boolean isApplicationAccepted(int applicationId) throws SQLException {
        String sql = "SELECT status FROM applications WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, applicationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "ACCEPTED".equals(rs.getString("status"));
                }
            }
        }
        return false;
    }

    /**
     * Check if interview already exists for an application.
     */
    private boolean hasInterview(int applicationId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM interviews WHERE application_id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, applicationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Map ResultSet to Interview entity.
     */
    private Interview mapResultSetToInterview(ResultSet rs) throws SQLException {
        Interview interview = new Interview();
        interview.setId(rs.getInt("id"));
        interview.setApplicationId(rs.getInt("application_id"));
        
        Timestamp dateTs = rs.getTimestamp("interview_date");
        if (dateTs != null) {
            interview.setInterviewDate(dateTs.toLocalDateTime());
        }
        
        interview.setLocation(rs.getString("location"));
        
        String typeStr = rs.getString("interview_type");
        if (typeStr != null) {
            try {
                interview.setInterviewType(InterviewType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                interview.setInterviewType(InterviewType.IN_PERSON);
            }
        }
        
        interview.setNotes(rs.getString("notes"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                interview.setStatus(InterviewStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                interview.setStatus(InterviewStatus.SCHEDULED);
            }
        }
        
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            interview.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        if (updatedAtTs != null) {
            interview.setUpdatedAt(updatedAtTs.toLocalDateTime());
        }
        
        // Transient fields from JOINs
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        if (firstName != null || lastName != null) {
            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            interview.setCandidateName(fullName);
        }
        interview.setJobTitle(rs.getString("job_title"));
        interview.setCompanyName(rs.getString("company_name"));
        
        return interview;
    }
}

