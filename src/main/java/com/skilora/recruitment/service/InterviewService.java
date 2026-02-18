package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.enums.InterviewStatus;
import com.skilora.utils.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);
    private static volatile InterviewService instance;

    private InterviewService() {}

    public static InterviewService getInstance() {
        if (instance == null) {
            synchronized (InterviewService.class) {
                if (instance == null) {
                    instance = new InterviewService();
                }
            }
        }
        return instance;
    }

    public int create(Interview interview) {
        if (interview == null) throw new IllegalArgumentException("Interview cannot be null");
        if (interview.getScheduledDate() == null) throw new IllegalArgumentException("Scheduled date is required");
        if (interview.getApplicationId() <= 0) throw new IllegalArgumentException("Valid application ID is required");
        String sql = """
            INSERT INTO interviews (application_id, scheduled_date, duration_minutes, type,
                location, video_link, notes, status, timezone)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, interview.getApplicationId());
            stmt.setTimestamp(2, Timestamp.valueOf(interview.getScheduledDate()));
            stmt.setInt(3, interview.getDurationMinutes());
            stmt.setString(4, interview.getType());
            stmt.setString(5, interview.getLocation());
            stmt.setString(6, interview.getVideoLink());
            stmt.setString(7, interview.getNotes());
            stmt.setString(8, interview.getStatus());
            stmt.setString(9, interview.getTimezone());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Interview created: id={}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating interview: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Interview findById(int id) {
        String sql = """
            SELECT i.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM interviews i
            JOIN applications a ON i.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE i.id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapInterview(rs);
        } catch (SQLException e) {
            logger.error("Error finding interview {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public List<Interview> findByApplication(int applicationId) {
        return findByQuery("WHERE i.application_id = ?", applicationId);
    }

    public List<Interview> findUpcomingForUser(int userId) {
        String sql = """
            SELECT i.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM interviews i
            JOIN applications a ON i.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE (a.candidate_profile_id = ? OR j.employer_id = ?)
              AND i.status = '" + InterviewStatus.SCHEDULED.name() + "'
              AND i.scheduled_date > NOW()
            ORDER BY i.scheduled_date ASC
            """;
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapInterview(rs));
        } catch (SQLException e) {
            logger.error("Error finding upcoming interviews for user {}: {}", userId, e.getMessage(), e);
        }
        return list;
    }

    public List<Interview> findByEmployer(int employerId) {
        String sql = """
            SELECT i.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM interviews i
            JOIN applications a ON i.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE j.employer_id = ?
            ORDER BY i.scheduled_date DESC
            """;
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapInterview(rs));
        } catch (SQLException e) {
            logger.error("Error finding employer interviews: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE interviews SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating interview status: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean complete(int id, String feedback, int rating) {
        String sql = "UPDATE interviews SET status = '" + InterviewStatus.COMPLETED.name() + "', feedback = ?, rating = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feedback);
            stmt.setInt(2, rating);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error completing interview: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean cancel(int id) {
        return updateStatus(id, InterviewStatus.CANCELLED.name());
    }

    public boolean reschedule(int id, java.time.LocalDateTime newDate) {
        String sql = "UPDATE interviews SET scheduled_date = ?, status = '" + InterviewStatus.SCHEDULED.name() + "' WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(newDate));
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error rescheduling interview: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM interviews WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting interview: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── Private helpers ──

    private List<Interview> findByQuery(String whereClause, int param) {
        String sql = """
            SELECT i.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM interviews i
            JOIN applications a ON i.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            """ + whereClause + " ORDER BY i.scheduled_date DESC";
        List<Interview> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, param);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapInterview(rs));
        } catch (SQLException e) {
            logger.error("Error querying interviews: {}", e.getMessage(), e);
        }
        return list;
    }

    private Interview mapInterview(ResultSet rs) throws SQLException {
        Interview i = new Interview();
        i.setId(rs.getInt("id"));
        i.setApplicationId(rs.getInt("application_id"));
        Timestamp scheduled = rs.getTimestamp("scheduled_date");
        if (scheduled != null) i.setScheduledDate(scheduled.toLocalDateTime());
        i.setDurationMinutes(rs.getInt("duration_minutes"));
        i.setType(rs.getString("type"));
        i.setLocation(rs.getString("location"));
        i.setVideoLink(rs.getString("video_link"));
        i.setNotes(rs.getString("notes"));
        i.setStatus(rs.getString("status"));
        i.setFeedback(rs.getString("feedback"));
        i.setRating(rs.getInt("rating"));
        i.setTimezone(rs.getString("timezone"));
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) i.setCreatedDate(created.toLocalDateTime());
        i.setCandidateName(ResultSetUtils.getOptionalString(rs, "candidate_name"));
        i.setJobTitle(ResultSetUtils.getOptionalString(rs, "job_title"));
        i.setCompanyName(ResultSetUtils.getOptionalString(rs, "company_name"));
        return i;
    }
}
