package com.skilora.model.repository.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.TrainingEnrollment;
import com.skilora.model.repository.TrainingEnrollmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrainingEnrollmentRepositoryImpl implements TrainingEnrollmentRepository {

    private static final Logger logger = LoggerFactory.getLogger(TrainingEnrollmentRepositoryImpl.class);

    @Override
    public TrainingEnrollment save(TrainingEnrollment enrollment) {
        if (enrollment.getId() == 0) {
            return insert(enrollment);
        } else {
            update(enrollment);
            return enrollment;
        }
    }

    private TrainingEnrollment insert(TrainingEnrollment enrollment) {
        String sql = "INSERT INTO training_enrollments (user_id, training_id, enrolled_at, last_accessed_at, completed) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, enrollment.getUserId());
            stmt.setInt(2, enrollment.getTrainingId());
            stmt.setTimestamp(3, Timestamp.valueOf(enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt() : now));
            stmt.setTimestamp(4, Timestamp.valueOf(enrollment.getLastAccessedAt() != null ? enrollment.getLastAccessedAt() : now));
            stmt.setBoolean(5, enrollment.isCompleted());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating enrollment failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    enrollment.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating enrollment failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting enrollment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save enrollment: " + e.getMessage(), e);
        }
        
        return enrollment;
    }

    private void update(TrainingEnrollment enrollment) {
        String sql = "UPDATE training_enrollments SET last_accessed_at = ?, completed = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(2, enrollment.isCompleted());
            stmt.setInt(3, enrollment.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating enrollment id: {}", enrollment.getId(), e);
            throw new RuntimeException("Failed to update enrollment: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TrainingEnrollment> findById(int id) {
        String sql = "SELECT * FROM training_enrollments WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollment by id: {}", id, e);
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<TrainingEnrollment> findByUserIdAndTrainingId(int userId, int trainingId) {
        String sql = "SELECT * FROM training_enrollments WHERE user_id = ? AND training_id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollment by user_id and training_id: user_id={}, training_id={}", userId, trainingId, e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<TrainingEnrollment> findByUserId(int userId) {
        List<TrainingEnrollment> enrollments = new ArrayList<>();
        String sql = "SELECT * FROM training_enrollments WHERE user_id = ? ORDER BY enrolled_at DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollments by user_id: {}", userId, e);
        }
        
        return enrollments;
    }

    @Override
    public List<TrainingEnrollment> findByTrainingId(int trainingId) {
        List<TrainingEnrollment> enrollments = new ArrayList<>();
        String sql = "SELECT * FROM training_enrollments WHERE training_id = ? ORDER BY enrolled_at DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, trainingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(mapResultSetToEnrollment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding enrollments by training_id: {}", trainingId, e);
        }
        
        return enrollments;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM training_enrollments WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting enrollment id: {}", id, e);
            throw new RuntimeException("Failed to delete enrollment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserIdAndTrainingId(int userId, int trainingId) {
        String sql = "SELECT COUNT(*) FROM training_enrollments WHERE user_id = ? AND training_id = ?";
        
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
            logger.error("Error checking enrollment existence: user_id={}, training_id={}", userId, trainingId, e);
        }
        
        return false;
    }

    private TrainingEnrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setId(rs.getInt("id"));
        enrollment.setUserId(rs.getInt("user_id"));
        enrollment.setTrainingId(rs.getInt("training_id"));
        
        Timestamp enrolledAt = rs.getTimestamp("enrolled_at");
        if (enrolledAt != null) {
            enrollment.setEnrolledAt(enrolledAt.toLocalDateTime());
        }
        
        Timestamp lastAccessedAt = rs.getTimestamp("last_accessed_at");
        if (lastAccessedAt != null) {
            enrollment.setLastAccessedAt(lastAccessedAt.toLocalDateTime());
        }
        
        enrollment.setCompleted(rs.getBoolean("completed"));
        
        return enrollment;
    }
}
