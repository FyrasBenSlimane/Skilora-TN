package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Mentorship;
import com.skilora.formation.enums.MentorshipStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MentorshipService {

    private static final Logger logger = LoggerFactory.getLogger(MentorshipService.class);
    private static volatile MentorshipService instance;

    private MentorshipService() {}

    public static MentorshipService getInstance() {
        if (instance == null) {
            synchronized (MentorshipService.class) {
                if (instance == null) {
                    instance = new MentorshipService();
                }
            }
        }
        return instance;
    }

    /**
     * Find users who can be mentors (all users except the requesting mentee).
     * Returns a list of [id, fullName] pairs.
     */
    public List<int[]> findAvailableMentorIds(int excludeUserId) {
        List<int[]> mentors = new ArrayList<>();
        String sql = "SELECT id, CONCAT(first_name, ' ', last_name) AS full_name FROM users WHERE id != ? AND is_active = 1 ORDER BY first_name, last_name";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, excludeUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentors.add(new int[]{rs.getInt("id")});
            }
        } catch (SQLException e) {
            logger.error("Error finding available mentors: {}", e.getMessage(), e);
        }
        return mentors;
    }

    /**
     * Find users who can be mentors (all active users except the requesting mentee).
     * Returns a Map of userId → fullName.
     */
    public java.util.LinkedHashMap<Integer, String> findAvailableMentors(int excludeUserId) {
        java.util.LinkedHashMap<Integer, String> mentors = new java.util.LinkedHashMap<>();
        String sql = "SELECT id, CONCAT(first_name, ' ', last_name) AS full_name FROM users WHERE id != ? AND is_active = 1 ORDER BY first_name, last_name";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, excludeUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentors.put(rs.getInt("id"), rs.getString("full_name"));
            }
        } catch (SQLException e) {
            logger.error("Error finding available mentors: {}", e.getMessage(), e);
        }
        return mentors;
    }

    public int requestMentorship(int menteeId, int mentorId, String topic, String goals) {
        String sql = "INSERT INTO mentorships (mentor_id, mentee_id, status, topic, goals, created_date) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, mentorId);
            stmt.setInt(2, menteeId);
            stmt.setString(3, MentorshipStatus.PENDING.name());
            stmt.setString(4, topic);
            stmt.setString(5, goals);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Mentorship requested: id {}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error requesting mentorship: {}", e.getMessage(), e);
        }
        return -1;
    }

    public boolean accept(int id) {
        String sql = "UPDATE mentorships SET status = ?, start_date = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, MentorshipStatus.ACTIVE.name());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error accepting mentorship: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean complete(int id, int rating, String feedback) {
        String sql = "UPDATE mentorships SET status = ?, end_date = ?, rating = ?, feedback = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, MentorshipStatus.COMPLETED.name());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, rating);
            stmt.setString(4, feedback);
            stmt.setInt(5, id);
            
            if (stmt.executeUpdate() > 0) {
                // Get mentor_id for achievement
                String getSql = "SELECT mentor_id FROM mentorships WHERE id = ?";
                try (PreparedStatement getStmt = conn.prepareStatement(getSql)) {
                    getStmt.setInt(1, id);
                    ResultSet rs = getStmt.executeQuery();
                    if (rs.next()) {
                        int mentorId = rs.getInt("mentor_id");
                        AchievementService.getInstance().checkAndAward(mentorId);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error completing mentorship: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean cancel(int id) {
        String sql = "UPDATE mentorships SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, MentorshipStatus.CANCELLED.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error cancelling mentorship: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<Mentorship> findByMentor(int mentorId) {
        List<Mentorship> mentorships = new ArrayList<>();
        String sql = """
            SELECT m.*, 
                u1.full_name as mentor_name, u1.photo_url as mentor_photo,
                u2.full_name as mentee_name
            FROM mentorships m
            JOIN users u1 ON m.mentor_id = u1.id
            JOIN users u2 ON m.mentee_id = u2.id
            WHERE m.mentor_id = ?
            ORDER BY m.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mentorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentorships.add(mapMentorship(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching mentor mentorships: {}", e.getMessage(), e);
        }
        return mentorships;
    }

    public List<Mentorship> findByMentee(int menteeId) {
        List<Mentorship> mentorships = new ArrayList<>();
        String sql = """
            SELECT m.*, 
                u1.full_name as mentor_name, u1.photo_url as mentor_photo,
                u2.full_name as mentee_name
            FROM mentorships m
            JOIN users u1 ON m.mentor_id = u1.id
            JOIN users u2 ON m.mentee_id = u2.id
            WHERE m.mentee_id = ?
            ORDER BY m.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menteeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentorships.add(mapMentorship(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching mentee mentorships: {}", e.getMessage(), e);
        }
        return mentorships;
    }

    public List<Mentorship> findPendingForMentor(int mentorId) {
        List<Mentorship> mentorships = new ArrayList<>();
        String sql = """
            SELECT m.*, 
                u1.full_name as mentor_name, u1.photo_url as mentor_photo,
                u2.full_name as mentee_name
            FROM mentorships m
            JOIN users u1 ON m.mentor_id = u1.id
            JOIN users u2 ON m.mentee_id = u2.id
            WHERE m.mentor_id = ? AND m.status = ?
            ORDER BY m.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mentorId);
            stmt.setString(2, MentorshipStatus.PENDING.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mentorships.add(mapMentorship(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching pending mentorships: {}", e.getMessage(), e);
        }
        return mentorships;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM mentorships WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting mentorship: {}", e.getMessage(), e);
        }
        return false;
    }

    private Mentorship mapMentorship(ResultSet rs) throws SQLException {
        Mentorship mentorship = new Mentorship();
        mentorship.setId(rs.getInt("id"));
        mentorship.setMentorId(rs.getInt("mentor_id"));
        mentorship.setMenteeId(rs.getInt("mentee_id"));
        mentorship.setStatus(MentorshipStatus.valueOf(rs.getString("status")));
        mentorship.setTopic(rs.getString("topic"));
        mentorship.setGoals(rs.getString("goals"));
        mentorship.setRating(rs.getInt("rating"));
        mentorship.setFeedback(rs.getString("feedback"));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) mentorship.setStartDate(startDate.toLocalDate());
        
        Date endDate = rs.getDate("end_date");
        if (endDate != null) mentorship.setEndDate(endDate.toLocalDate());
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) mentorship.setCreatedDate(created.toLocalDateTime());
        
        mentorship.setMentorName(rs.getString("mentor_name"));
        mentorship.setMentorPhoto(rs.getString("mentor_photo"));
        mentorship.setMenteeName(rs.getString("mentee_name"));
        
        return mentorship;
    }
}
