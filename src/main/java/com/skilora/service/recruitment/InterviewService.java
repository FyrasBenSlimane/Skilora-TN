package com.skilora.service.recruitment;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Application.Status;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.recruitment.Interview.InterviewStatus;
import com.skilora.model.entity.recruitment.Interview.InterviewType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for interview scheduling and queries.
 * Uses tables: interviews, applications, job_offers, companies, profiles.
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
     * Applications in INTERVIEW or ACCEPTED status for jobs owned by this employer (eligible to schedule).
     */
    public List<Application> getEligibleInterviewCandidatesForEmployer(int employerUserId) throws SQLException {
        String sql =
                "SELECT a.id, a.job_offer_id, a.candidate_profile_id, a.status, a.applied_date, a.cover_letter, a.custom_cv_url, " +
                "jo.title AS job_title, c.name AS company_name, CONCAT(p.first_name, ' ', p.last_name) AS candidate_name, jo.location AS job_location " +
                "FROM applications a " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "WHERE (c.owner_id = ? OR (c.owner_id IS NULL AND ? = 1)) " +
                "AND a.status IN ('INTERVIEW','ACCEPTED') " +
                "ORDER BY a.applied_date DESC";
        List<Application> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerUserId);
            stmt.setInt(2, employerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapApplication(rs));
                }
            }
        }
        return list;
    }

    /**
     * All scheduled interviews for jobs owned by this employer.
     */
    public List<Interview> getInterviewsForEmployer(int employerUserId) throws SQLException {
        String sql =
                "SELECT i.id, i.application_id, i.interview_date, i.location, i.interview_type, i.notes, i.status, i.created_at, i.updated_at, " +
                "CONCAT(p.first_name, ' ', p.last_name) AS candidate_name, jo.title AS job_title, c.name AS company_name " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.id " +
                "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                "LEFT JOIN companies c ON jo.company_id = c.id " +
                "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                "WHERE c.owner_id = ? OR (c.owner_id IS NULL AND ? = 1) " +
                "ORDER BY i.interview_date ASC";
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerUserId);
            stmt.setInt(2, employerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
        }
        return list;
    }

    /** Insert or update an interview. */
    public void saveOrUpdate(Interview interview) throws SQLException {
        if (interview.getApplicationId() <= 0) return;
        Optional<Interview> existing = getInterviewByApplicationId(interview.getApplicationId());
        if (existing.isPresent()) {
            String sql = "UPDATE interviews SET interview_date=?, location=?, interview_type=?, notes=?, status=?, updated_at=NOW() WHERE application_id=?";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, interview.getInterviewDate() != null ? Timestamp.valueOf(interview.getInterviewDate()) : null);
                stmt.setString(2, interview.getLocation());
                stmt.setString(3, interview.getInterviewType() != null ? interview.getInterviewType().name() : InterviewType.IN_PERSON.name());
                stmt.setString(4, interview.getNotes());
                stmt.setString(5, interview.getStatus() != null ? interview.getStatus().name() : InterviewStatus.SCHEDULED.name());
                stmt.setInt(6, interview.getApplicationId());
                stmt.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO interviews (application_id, interview_date, location, interview_type, notes, status) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, interview.getApplicationId());
                stmt.setObject(2, interview.getInterviewDate() != null ? Timestamp.valueOf(interview.getInterviewDate()) : null);
                stmt.setString(3, interview.getLocation());
                stmt.setString(4, interview.getInterviewType() != null ? interview.getInterviewType().name() : InterviewType.IN_PERSON.name());
                stmt.setString(5, interview.getNotes());
                stmt.setString(6, interview.getStatus() != null ? interview.getStatus().name() : InterviewStatus.SCHEDULED.name());
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Returns all SCHEDULED interviews for a specific candidate profile,
     * ordered by interview date ascending (soonest first).
     * Includes jobTitle and companyName via JOIN.
     */
    public List<Interview> getUpcomingInterviewsForCandidate(int profileId) throws SQLException {
        String sql =
            "SELECT i.id, i.application_id, i.interview_date, i.location, i.interview_type, " +
            "       i.notes, i.status, i.created_at, i.updated_at, " +
            "       jo.title AS job_title, c.name AS company_name, " +
            "       CONCAT(p.first_name, ' ', p.last_name) AS candidate_name " +
            "FROM interviews i " +
            "JOIN applications a ON i.application_id = a.id " +
            "JOIN job_offers jo ON a.job_offer_id = jo.id " +
            "LEFT JOIN companies c ON jo.company_id = c.id " +
            "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
            "WHERE a.candidate_profile_id = ? " +
            "  AND i.status = 'SCHEDULED' " +
            "ORDER BY i.interview_date ASC";
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
        }
        return list;
    }

    /**
     * Returns the next N upcoming SCHEDULED interviews for a specific employer,
     * ordered by interview date ascending (soonest first).
     */
    public List<Interview> getUpcomingInterviewsForEmployer(int employerUserId, int limit) throws SQLException {
        String sql =
            "SELECT i.id, i.application_id, i.interview_date, i.location, i.interview_type, " +
            "       i.notes, i.status, i.created_at, i.updated_at, " +
            "       CONCAT(p.first_name, ' ', p.last_name) AS candidate_name, " +
            "       jo.title AS job_title, c.name AS company_name " +
            "FROM interviews i " +
            "JOIN applications a ON i.application_id = a.id " +
            "JOIN job_offers jo ON a.job_offer_id = jo.id " +
            "LEFT JOIN companies c ON jo.company_id = c.id " +
            "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
            "WHERE (c.owner_id = ? OR (c.owner_id IS NULL AND ? = 1)) " +
            "  AND i.status = 'SCHEDULED' " +
            "  AND i.interview_date >= NOW() " +
            "ORDER BY i.interview_date ASC " +
            "LIMIT ?";
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerUserId);
            stmt.setInt(2, employerUserId);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
        }
        return list;
    }

    public Optional<Interview> getInterviewByApplicationId(int applicationId) throws SQLException {
        String sql = "SELECT id, application_id, interview_date, location, interview_type, notes, status, created_at, updated_at FROM interviews WHERE application_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapInterview(rs));
                }
            }
        }
        return Optional.empty();
    }

    private Application mapApplication(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setId(rs.getInt("id"));
        a.setJobOfferId(rs.getInt("job_offer_id"));
        a.setCandidateProfileId(rs.getInt("candidate_profile_id"));
        String st = rs.getString("status");
        if (st != null) {
            try {
                a.setStatus(Status.valueOf(st));
            } catch (Exception e) {
                a.setStatus(Status.PENDING);
            }
        }
        Timestamp ts = rs.getTimestamp("applied_date");
        if (ts != null) a.setAppliedDate(ts.toLocalDateTime());
        a.setCoverLetter(rs.getString("cover_letter"));
        a.setCustomCvUrl(rs.getString("custom_cv_url"));
        a.setJobTitle(rs.getString("job_title"));
        a.setCompanyName(rs.getString("company_name"));
        a.setCandidateName(rs.getString("candidate_name"));
        a.setJobLocation(rs.getString("job_location"));
        return a;
    }

    private Interview mapInterview(ResultSet rs) throws SQLException {
        Interview i = new Interview();
        i.setId(rs.getInt("id"));
        i.setApplicationId(rs.getInt("application_id"));
        Timestamp ts = rs.getTimestamp("interview_date");
        if (ts != null) i.setInterviewDate(ts.toLocalDateTime());
        i.setLocation(rs.getString("location"));
        String type = rs.getString("interview_type");
        if (type != null) {
            try {
                i.setInterviewType(InterviewType.valueOf(type));
            } catch (Exception e) {
                i.setInterviewType(InterviewType.IN_PERSON);
            }
        }
        i.setNotes(rs.getString("notes"));
        String st = rs.getString("status");
        if (st != null) {
            try {
                i.setStatus(InterviewStatus.valueOf(st));
            } catch (Exception e) {
                i.setStatus(InterviewStatus.SCHEDULED);
            }
        }
        ts = rs.getTimestamp("created_at");
        if (ts != null) i.setCreatedAt(ts.toLocalDateTime());
        ts = rs.getTimestamp("updated_at");
        if (ts != null) i.setUpdatedAt(ts.toLocalDateTime());
        try {
            i.setCandidateName(rs.getString("candidate_name"));
            i.setJobTitle(rs.getString("job_title"));
            i.setCompanyName(rs.getString("company_name"));
        } catch (SQLException ignored) {}
        return i;
    }
}
