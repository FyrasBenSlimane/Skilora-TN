package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.UserFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(UserFeedbackService.class);
    private static volatile UserFeedbackService instance;

    private UserFeedbackService() {}

    public static UserFeedbackService getInstance() {
        if (instance == null) {
            synchronized (UserFeedbackService.class) {
                if (instance == null) {
                    instance = new UserFeedbackService();
                }
            }
        }
        return instance;
    }

    public int submit(UserFeedback fb) {
        String sql = """
            INSERT INTO user_feedback (user_id, feedback_type, rating, comment, category)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, fb.getUserId());
            stmt.setString(2, fb.getFeedbackType());
            stmt.setInt(3, fb.getRating());
            stmt.setString(4, fb.getComment());
            stmt.setString(5, fb.getCategory());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to submit feedback", e);
        }
        return -1;
    }

    public List<UserFeedback> findByUserId(int userId) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? ORDER BY created_date DESC";
        List<UserFeedback> feedbackList = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    feedbackList.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find feedback for user: {}", userId, e);
        }
        return feedbackList;
    }

    public List<UserFeedback> findAll() {
        String sql = """
            SELECT f.*, u.username as user_name 
            FROM user_feedback f
            LEFT JOIN users u ON f.user_id = u.id
            ORDER BY f.created_date DESC
            """;
        List<UserFeedback> feedbackList = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UserFeedback fb = mapResultSet(rs);
                fb.setUserName(rs.getString("user_name"));
                feedbackList.add(fb);
            }
        } catch (SQLException e) {
            logger.error("Failed to find all feedback", e);
        }
        return feedbackList;
    }

    public List<UserFeedback> findByType(String type) {
        String sql = """
            SELECT f.*, u.username as user_name 
            FROM user_feedback f
            LEFT JOIN users u ON f.user_id = u.id
            WHERE f.feedback_type = ?
            ORDER BY f.created_date DESC
            """;
        List<UserFeedback> feedbackList = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserFeedback fb = mapResultSet(rs);
                    fb.setUserName(rs.getString("user_name"));
                    feedbackList.add(fb);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find feedback by type", e);
        }
        return feedbackList;
    }

    public boolean resolve(int id) {
        String sql = "UPDATE user_feedback SET is_resolved = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to resolve feedback", e);
        }
        return false;
    }

    public boolean update(UserFeedback fb) {
        String sql = """
            UPDATE user_feedback 
            SET feedback_type = ?, rating = ?, comment = ?, category = ?, is_resolved = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, fb.getFeedbackType());
            stmt.setInt(2, fb.getRating());
            stmt.setString(3, fb.getComment());
            stmt.setString(4, fb.getCategory());
            stmt.setBoolean(5, fb.isResolved());
            stmt.setInt(6, fb.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update feedback", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM user_feedback WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete feedback", e);
        }
        return false;
    }

    public double getAverageRating() {
        String sql = "SELECT AVG(rating) as avg_rating FROM user_feedback WHERE rating > 0";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (SQLException e) {
            logger.error("Failed to get average rating", e);
        }
        return 0.0;
    }

    private UserFeedback mapResultSet(ResultSet rs) throws SQLException {
        UserFeedback fb = new UserFeedback();
        fb.setId(rs.getInt("id"));
        fb.setUserId(rs.getInt("user_id"));
        fb.setFeedbackType(rs.getString("feedback_type"));
        fb.setRating(rs.getInt("rating"));
        fb.setComment(rs.getString("comment"));
        fb.setCategory(rs.getString("category"));
        fb.setResolved(rs.getBoolean("is_resolved"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) fb.setCreatedDate(created.toLocalDateTime());
        
        return fb;
    }
}
