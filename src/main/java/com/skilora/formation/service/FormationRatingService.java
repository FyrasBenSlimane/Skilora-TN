package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.FormationRating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * FormationRatingService
 * 
 * Handles business logic for formation ratings.
 * Ensures users can only rate completed formations and only once per formation.
 * Uses direct SQL via DatabaseConfig (no repository pattern).
 */
public class FormationRatingService {

    private static final Logger logger = LoggerFactory.getLogger(FormationRatingService.class);
    private static volatile FormationRatingService instance;

    private FormationRatingService() {}

    public static FormationRatingService getInstance() {
        if (instance == null) {
            synchronized (FormationRatingService.class) {
                if (instance == null) {
                    instance = new FormationRatingService();
                }
            }
        }
        return instance;
    }

    /**
     * Submit or update a rating for a formation.
     * Validates that:
     * 1. The user has completed the formation
     * 2. The star rating is between 1 and 5
     * 3. If updating, the user already has a rating
     */
    public FormationRating submitRating(int userId, int formationId, Boolean isLiked, int starRating) {
        logger.info("Submitting rating: userId={}, formationId={}, isLiked={}, starRating={}",
                userId, formationId, isLiked, starRating);

        if (starRating < 1 || starRating > 5) {
            throw new IllegalArgumentException("Star rating must be between 1 and 5");
        }

        // Check if formation is completed
        boolean isCompleted = EnrollmentService.getInstance().isFormationCompleted(userId, formationId);
        if (!isCompleted) {
            throw new IllegalStateException("Cannot rate a formation that has not been completed");
        }

        Optional<FormationRating> existing = getUserRating(userId, formationId);

        if (existing.isPresent()) {
            // Update existing rating
            FormationRating rating = existing.get();
            rating.setIsLiked(isLiked);
            rating.setStarRating(starRating);
            rating.setUpdatedAt(LocalDateTime.now());
            updateRating(rating);
            logger.info("Updated existing rating: id={}", rating.getId());
            return rating;
        } else {
            // Create new rating
            FormationRating rating = new FormationRating(userId, formationId, isLiked, starRating);
            int id = insertRating(rating);
            rating.setId(id);
            logger.info("Created new rating: id={}", id);
            return rating;
        }
    }

    /**
     * Get the user's rating for a formation, if it exists.
     */
    public Optional<FormationRating> getUserRating(int userId, int formationId) {
        String sql = "SELECT * FROM formation_ratings WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting user rating: userId={}, formationId={}", userId, formationId, e);
        }
        return Optional.empty();
    }

    /**
     * Check if user has already rated this formation.
     */
    public boolean hasUserRated(int userId, int formationId) {
        String sql = "SELECT 1 FROM formation_ratings WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking if user rated: userId={}, formationId={}", userId, formationId, e);
        }
        return false;
    }

    /**
     * Get aggregated statistics for a formation.
     */
    public RatingStatistics getStatistics(int formationId) {
        String sql = "SELECT COUNT(*) as total_ratings, " +
                "COALESCE(AVG(star_rating), 0) as avg_rating, " +
                "SUM(CASE WHEN is_like = TRUE THEN 1 ELSE 0 END) as like_count, " +
                "SUM(CASE WHEN is_like = FALSE THEN 1 ELSE 0 END) as dislike_count " +
                "FROM formation_ratings WHERE formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RatingStatistics(
                            rs.getInt("total_ratings"),
                            rs.getDouble("avg_rating"),
                            rs.getInt("like_count"),
                            rs.getInt("dislike_count")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting rating statistics: formationId={}", formationId, e);
        }
        return new RatingStatistics(0, 0.0, 0, 0);
    }

    /**
     * Check if user can rate this formation (must be completed and not already rated).
     */
    public boolean canUserRate(int userId, int formationId) {
        boolean isCompleted = EnrollmentService.getInstance().isFormationCompleted(userId, formationId);
        boolean alreadyRated = hasUserRated(userId, formationId);
        return isCompleted && !alreadyRated;
    }

    /**
     * Check if user can update their rating (must be completed and already rated).
     */
    public boolean canUserUpdateRating(int userId, int formationId) {
        boolean isCompleted = EnrollmentService.getInstance().isFormationCompleted(userId, formationId);
        boolean alreadyRated = hasUserRated(userId, formationId);
        return isCompleted && alreadyRated;
    }

    // ── Private helpers ──

    private int insertRating(FormationRating rating) {
        String sql = "INSERT INTO formation_ratings (user_id, formation_id, is_like, star_rating, created_date, updated_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, rating.getUserId());
            stmt.setInt(2, rating.getFormationId());
            if (rating.getIsLiked() != null) {
                stmt.setBoolean(3, rating.getIsLiked());
            } else {
                stmt.setNull(3, Types.BOOLEAN);
            }
            stmt.setInt(4, rating.getStarRating());
            stmt.setTimestamp(5, Timestamp.valueOf(rating.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(rating.getUpdatedAt()));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error inserting rating: {}", e.getMessage(), e);
        }
        return -1;
    }

    private void updateRating(FormationRating rating) {
        String sql = "UPDATE formation_ratings SET is_like = ?, star_rating = ?, updated_date = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (rating.getIsLiked() != null) {
                stmt.setBoolean(1, rating.getIsLiked());
            } else {
                stmt.setNull(1, Types.BOOLEAN);
            }
            stmt.setInt(2, rating.getStarRating());
            stmt.setTimestamp(3, Timestamp.valueOf(rating.getUpdatedAt()));
            stmt.setInt(4, rating.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating rating: {}", e.getMessage(), e);
        }
    }

    private FormationRating mapResultSet(ResultSet rs) throws SQLException {
        FormationRating rating = new FormationRating();
        rating.setId(rs.getInt("id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setFormationId(rs.getInt("formation_id"));
        boolean isLiked = rs.getBoolean("is_like");
        if (!rs.wasNull()) {
            rating.setIsLiked(isLiked);
        }
        rating.setStarRating(rs.getInt("star_rating"));
        Timestamp createdAt = rs.getTimestamp("created_date");
        if (createdAt != null) rating.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_date");
        if (updatedAt != null) rating.setUpdatedAt(updatedAt.toLocalDateTime());
        return rating;
    }

    // ── Inner class ──

    /**
     * Aggregated rating statistics for a formation.
     */
    public static class RatingStatistics {
        private final int totalRatings;
        private final double averageRating;
        private final int likeCount;
        private final int dislikeCount;

        public RatingStatistics(int totalRatings, double averageRating, int likeCount, int dislikeCount) {
            this.totalRatings = totalRatings;
            this.averageRating = averageRating;
            this.likeCount = likeCount;
            this.dislikeCount = dislikeCount;
        }

        public int getTotalRatings() { return totalRatings; }
        public double getAverageRating() { return averageRating; }
        public int getLikeCount() { return likeCount; }
        public int getDislikeCount() { return dislikeCount; }
    }
}
