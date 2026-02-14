package com.skilora.model.repository.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.enums.TrainingCategory;
import com.skilora.model.enums.TrainingLevel;
import com.skilora.model.repository.TrainingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TrainingRepositoryImpl
 * 
 * JDBC implementation of TrainingRepository interface.
 * Handles all database operations for Training entities.
 */
public class TrainingRepositoryImpl implements TrainingRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrainingRepositoryImpl.class);

    @Override
    public Optional<Training> findById(int id) {
        String sql = "SELECT id, title, description, cost, duration, lesson_count, level, category, " +
                     "created_at, updated_at FROM trainings WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTraining(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding training by id: {}", id, e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Training> findAll() {
        String sql = "SELECT id, title, description, cost, duration, lesson_count, level, category, " +
                     "created_at, updated_at FROM trainings ORDER BY created_at DESC";
        List<Training> trainings = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                trainings.add(mapResultSetToTraining(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all trainings", e);
        }
        
        return trainings;
    }

    @Override
    public Training save(Training training) {
        if (training.getId() == 0) {
            return insert(training);
        } else {
            update(training);
            return training;
        }
    }

    private Training insert(Training training) {
        // Don't include created_at and updated_at - let MySQL use DEFAULT values
        // This avoids issues with timestamp defaults and ensures AUTO_INCREMENT works properly
        String sql = "INSERT INTO trainings (title, description, cost, duration, lesson_count, level, category) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, training.getTitle());
            stmt.setString(2, training.getDescription());
            if (training.getCost() != null) {
                stmt.setDouble(3, training.getCost());
            } else {
                stmt.setNull(3, java.sql.Types.DECIMAL);
            }
            stmt.setInt(4, training.getDuration());
            stmt.setInt(5, training.getLessonCount());
            stmt.setString(6, training.getLevel() != null ? training.getLevel().name() : null);
            stmt.setString(7, training.getCategory() != null ? training.getCategory().name() : null);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating training failed, no rows affected.");
            }
            
            // Get the generated ID from AUTO_INCREMENT
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    training.setId(generatedId);
                    
                    // Set timestamps - MySQL will set these via DEFAULT, but we set them here for consistency
                    LocalDateTime now = LocalDateTime.now();
                    if (training.getCreatedAt() == null) {
                        training.setCreatedAt(now);
                    }
                    if (training.getUpdatedAt() == null) {
                        training.setUpdatedAt(now);
                    }
                } else {
                    throw new SQLException("Creating training failed, no ID obtained. Check that the table has AUTO_INCREMENT enabled on the id column.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting training: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save training: " + e.getMessage(), e);
        }
        
        return training;
    }

    @Override
    public boolean update(Training training) {
        String sql = "UPDATE trainings SET title = ?, description = ?, cost = ?, duration = ?, " +
                     "lesson_count = ?, level = ?, category = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, training.getTitle());
            stmt.setString(2, training.getDescription());
            if (training.getCost() != null) {
                stmt.setDouble(3, training.getCost());
            } else {
                stmt.setNull(3, java.sql.Types.DECIMAL);
            }
            stmt.setInt(4, training.getDuration());
            stmt.setInt(5, training.getLessonCount());
            stmt.setString(6, training.getLevel() != null ? training.getLevel().name() : null);
            stmt.setString(7, training.getCategory() != null ? training.getCategory().name() : null);
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(9, training.getId());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error updating training id: {}", training.getId(), e);
            throw new RuntimeException("Failed to update training: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM trainings WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting training id: {}", id, e);
            throw new RuntimeException("Failed to delete training: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM trainings WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking existence of training id: {}", id, e);
        }
        
        return false;
    }

    @Override
    public List<Training> searchByKeyword(String keyword) {
        String sql = "SELECT id, title, description, cost, duration, lesson_count, level, category, " +
                     "created_at, updated_at FROM trainings " +
                     "WHERE title LIKE ? OR description LIKE ? ORDER BY created_at DESC";
        List<Training> trainings = new ArrayList<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        
        String searchPattern = "%" + keyword.trim() + "%";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainings.add(mapResultSetToTraining(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching trainings by keyword: {}", keyword, e);
        }
        
        return trainings;
    }

    @Override
    public List<Training> findByCategory(TrainingCategory category) {
        String sql = "SELECT id, title, description, cost, duration, lesson_count, level, category, " +
                     "created_at, updated_at FROM trainings WHERE category = ? ORDER BY created_at DESC";
        List<Training> trainings = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainings.add(mapResultSetToTraining(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding trainings by category: {}", category, e);
        }
        
        return trainings;
    }

    @Override
    public List<Training> findByCategoryAndKeyword(TrainingCategory category, String keyword) {
        String sql = "SELECT id, title, description, cost, duration, lesson_count, level, category, " +
                     "created_at, updated_at FROM trainings " +
                     "WHERE category = ? AND (title LIKE ? OR description LIKE ?) " +
                     "ORDER BY created_at DESC";
        List<Training> trainings = new ArrayList<>();
        
        String searchPattern = keyword != null && !keyword.trim().isEmpty() 
            ? "%" + keyword.trim() + "%" : "%";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.name());
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainings.add(mapResultSetToTraining(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding trainings by category and keyword: category={}, keyword={}", 
                        category, keyword, e);
        }
        
        return trainings;
    }

    /**
     * Maps a ResultSet row to a Training entity
     */
    private Training mapResultSetToTraining(ResultSet rs) throws SQLException {
        Training training = new Training();
        training.setId(rs.getInt("id"));
        training.setTitle(rs.getString("title"));
        training.setDescription(rs.getString("description"));
        // Handle nullable cost
        double costValue = rs.getDouble("cost");
        if (rs.wasNull()) {
            training.setCost(null);
        } else {
            training.setCost(costValue);
        }
        training.setDuration(rs.getInt("duration"));
        training.setLessonCount(rs.getInt("lesson_count"));
        
        // Parse level enum
        String levelStr = rs.getString("level");
        if (levelStr != null) {
            try {
                training.setLevel(TrainingLevel.valueOf(levelStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown training level '{}' found in database. Defaulting to BEGINNER.", levelStr);
                training.setLevel(TrainingLevel.BEGINNER);
            }
        }
        
        // Parse category enum
        String categoryStr = rs.getString("category");
        if (categoryStr != null) {
            try {
                training.setCategory(TrainingCategory.valueOf(categoryStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown training category '{}' found in database. Defaulting to DEVELOPMENT.", categoryStr);
                training.setCategory(TrainingCategory.DEVELOPMENT);
            }
        }
        
        // Parse timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            training.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            training.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return training;
    }
}
