package com.skilora.model.repository.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.TrainingLesson;
import com.skilora.model.repository.TrainingLessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrainingLessonRepositoryImpl implements TrainingLessonRepository {
    private static final Logger logger = LoggerFactory.getLogger(TrainingLessonRepositoryImpl.class);

    @Override
    public TrainingLesson save(TrainingLesson lesson) {
        if (lesson.getId() == 0) {
            return insert(lesson);
        } else {
            update(lesson);
            return lesson;
        }
    }

    private TrainingLesson insert(TrainingLesson lesson) {
        String sql = "INSERT INTO training_lessons (training_id, title, description, content, order_index, duration_minutes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, lesson.getTrainingId());
            stmt.setString(2, lesson.getTitle());
            stmt.setString(3, lesson.getDescription());
            stmt.setString(4, lesson.getContent());
            stmt.setInt(5, lesson.getOrderIndex());
            stmt.setInt(6, lesson.getDurationMinutes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) lesson.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Error inserting lesson", e);
            throw new RuntimeException("Failed to save lesson: " + e.getMessage(), e);
        }
        return lesson;
    }

    private void update(TrainingLesson lesson) {
        String sql = "UPDATE training_lessons SET title = ?, description = ?, content = ?, order_index = ?, duration_minutes = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lesson.getTitle());
            stmt.setString(2, lesson.getDescription());
            stmt.setString(3, lesson.getContent());
            stmt.setInt(4, lesson.getOrderIndex());
            stmt.setInt(5, lesson.getDurationMinutes());
            stmt.setInt(6, lesson.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating lesson", e);
            throw new RuntimeException("Failed to update lesson: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TrainingLesson> findById(int id) {
        String sql = "SELECT * FROM training_lessons WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding lesson by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<TrainingLesson> findByTrainingId(int trainingId) {
        List<TrainingLesson> lessons = new ArrayList<>();
        String sql = "SELECT * FROM training_lessons WHERE training_id = ? ORDER BY order_index ASC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, trainingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lessons.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding lessons by training_id: {}", trainingId, e);
        }
        return lessons;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM training_lessons WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting lesson id: {}", id, e);
            throw new RuntimeException("Failed to delete lesson: " + e.getMessage(), e);
        }
    }

    private TrainingLesson mapResultSet(ResultSet rs) throws SQLException {
        TrainingLesson lesson = new TrainingLesson();
        lesson.setId(rs.getInt("id"));
        lesson.setTrainingId(rs.getInt("training_id"));
        lesson.setTitle(rs.getString("title"));
        lesson.setDescription(rs.getString("description"));
        lesson.setContent(rs.getString("content"));
        lesson.setOrderIndex(rs.getInt("order_index"));
        lesson.setDurationMinutes(rs.getInt("duration_minutes"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) lesson.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) lesson.setUpdatedAt(updatedAt.toLocalDateTime());
        return lesson;
    }
}
