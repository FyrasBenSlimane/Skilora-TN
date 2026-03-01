package com.skilora.model.repository.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.TrainingRating;
import com.skilora.model.repository.TrainingRatingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class TrainingRatingRepositoryImpl implements TrainingRatingRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrainingRatingRepositoryImpl.class);

    @Override
    public TrainingRating save(TrainingRating rating) {
        if (rating.getId() == 0) {
            return insert(rating);
        } else {
            update(rating);
            return rating;
        }
    }

    private TrainingRating insert(TrainingRating rating) {
        String sql = "INSERT INTO training_ratings (user_id, training_id, is_liked, star_rating, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, rating.getUserId());
            stmt.setInt(2, rating.getTrainingId());
            
            if (rating.getIsLiked() != null) {
                stmt.setBoolean(3, rating.getIsLiked());
            } else {
                stmt.setNull(3, Types.BOOLEAN);
            }
            
            stmt.setInt(4, rating.getStarRating());
            stmt.setTimestamp(5, Timestamp.valueOf(rating.getCreatedAt() != null ? rating.getCreatedAt() : now));
            stmt.setTimestamp(6, Timestamp.valueOf(rating.getUpdatedAt() != null ? rating.getUpdatedAt() : now));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating rating failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rating.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating rating failed, no ID obtained.");
                }
            }
            
            logger.debug("Created new rating: id={}, userId={}, trainingId={}, starRating={}", 
                rating.getId(), rating.getUserId(), rating.getTrainingId(), rating.getStarRating());
            
            return rating;
        } catch (SQLException e) {
            logger.error("Error inserting rating: userId={}, trainingId={}", 
                rating.getUserId(), rating.getTrainingId(), e);
            throw new RuntimeException("Failed to save rating", e);
        }
    }

    private void update(TrainingRating rating) {
        String sql = "UPDATE training_ratings SET is_liked = ?, star_rating = ?, updated_at = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (rating.getIsLiked() != null) {
                stmt.setBoolean(1, rating.getIsLiked());
            } else {
                stmt.setNull(1, Types.BOOLEAN);
            }
            
            stmt.setInt(2, rating.getStarRating());
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, rating.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating rating failed, no rows affected.");
            }
            
            logger.debug("Updated rating: id={}, userId={}, trainingId={}", 
                rating.getId(), rating.getUserId(), rating.getTrainingId());
        } catch (SQLException e) {
            logger.error("Error updating rating: id={}", rating.getId(), e);
            throw new RuntimeException("Failed to update rating", e);
        }
    }

    @Override
    public Optional<TrainingRating> findByUserIdAndTrainingId(int userId, int trainingId) {
        String sql = "SELECT id, user_id, training_id, is_liked, star_rating, created_at, updated_at " +
                     "FROM training_ratings WHERE user_id = ? AND training_id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRating(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding rating: userId={}, trainingId={}", userId, trainingId, e);
        }
        
        return Optional.empty();
    }

    @Override
    public boolean existsByUserIdAndTrainingId(int userId, int trainingId) {
        String sql = "SELECT COUNT(*) FROM training_ratings WHERE user_id = ? AND training_id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking rating existence: userId={}, trainingId={}", userId, trainingId, e);
        }
        
        return false;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM training_ratings WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
            logger.debug("Deleted rating: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting rating: id={}", id, e);
            throw new RuntimeException("Failed to delete rating", e);
        }
    }

    @Override
    public RatingStatistics getStatistics(int trainingId) {
        String sql = "SELECT " +
                     "COUNT(*) as total_ratings, " +
                     "SUM(CASE WHEN is_liked = TRUE THEN 1 ELSE 0 END) as total_likes, " +
                     "SUM(CASE WHEN is_liked = FALSE THEN 1 ELSE 0 END) as total_dislikes, " +
                     "AVG(star_rating) as average_rating " +
                     "FROM training_ratings WHERE training_id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, trainingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long totalRatings = rs.getLong("total_ratings");
                    long totalLikes = rs.getLong("total_likes");
                    long totalDislikes = rs.getLong("total_dislikes");
                    double averageRating = rs.getDouble("average_rating");
                    
                    if (rs.wasNull()) {
                        averageRating = 0.0;
                    }
                    
                    return new RatingStatistics(totalRatings, totalLikes, totalDislikes, averageRating);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting statistics for training: trainingId={}", trainingId, e);
        }
        
        return new RatingStatistics(0, 0, 0, 0.0);
    }

    private TrainingRating mapRowToRating(ResultSet rs) throws SQLException {
        TrainingRating rating = new TrainingRating();
        rating.setId(rs.getInt("id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setTrainingId(rs.getInt("training_id"));
        
        Boolean isLiked = rs.getBoolean("is_liked");
        if (rs.wasNull()) {
            rating.setIsLiked(null);
        } else {
            rating.setIsLiked(isLiked);
        }
        
        rating.setStarRating(rs.getInt("star_rating"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            rating.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            rating.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return rating;
    }
}
