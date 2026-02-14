package com.skilora.service.formation;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.LessonProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * LessonProgressService
 * 
 * Handles business logic for lesson progress tracking.
 */
public class LessonProgressService {
    private static final Logger logger = LoggerFactory.getLogger(LessonProgressService.class);
    private static volatile LessonProgressService instance;

    private LessonProgressService() {}

    public static LessonProgressService getInstance() {
        if (instance == null) {
            synchronized (LessonProgressService.class) {
                if (instance == null) instance = new LessonProgressService();
            }
        }
        return instance;
    }

    public void updateProgress(int enrollmentId, int lessonId, int percentage) {
        logger.debug("Updating progress: enrollmentId={}, lessonId={}, percentage={}", enrollmentId, lessonId, percentage);
        String sql = "INSERT INTO lesson_progress (enrollment_id, lesson_id, progress_percentage, last_accessed_at) " +
                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE progress_percentage = ?, last_accessed_at = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, enrollmentId);
            stmt.setInt(2, lessonId);
            stmt.setInt(3, percentage);
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setInt(5, percentage);
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.executeUpdate();
            logger.debug("Progress updated successfully: enrollmentId={}, lessonId={}, percentage={}", 
                enrollmentId, lessonId, percentage);
            
            // Check if training is now complete
            checkAndMarkTrainingComplete(enrollmentId);
        } catch (SQLException e) {
            logger.error("Error updating lesson progress: enrollmentId={}, lessonId={}, percentage={}", 
                enrollmentId, lessonId, percentage, e);
        }
    }

    /**
     * Check if training is complete (100% progress) and automatically mark as completed
     * This marks the training as completed via TrainingEnrollmentService.markCompleted()
     */
    private void checkAndMarkTrainingComplete(int enrollmentId) {
        try {
            // Get enrollment details to find user_id and training_id
            String enrollmentSql = "SELECT user_id, training_id, completed FROM training_enrollments WHERE id = ?";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(enrollmentSql)) {
                stmt.setInt(1, enrollmentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("user_id");
                        int trainingId = rs.getInt("training_id");
                        boolean alreadyCompleted = rs.getBoolean("completed");
                        
                        if (alreadyCompleted) {
                            logger.debug("Training already marked as completed: enrollmentId={}, userId={}, trainingId={}", 
                                enrollmentId, userId, trainingId);
                            return;
                        }
                        
                        // Get total number of lessons for this training from training.lesson_count
                        String trainingSql = "SELECT lesson_count FROM trainings WHERE id = ?";
                        try (PreparedStatement trainingStmt = conn.prepareStatement(trainingSql)) {
                            trainingStmt.setInt(1, trainingId);
                            try (ResultSet trainingRs = trainingStmt.executeQuery()) {
                                if (trainingRs.next()) {
                                    int totalLessons = trainingRs.getInt("lesson_count");
                                    
                                    if (totalLessons == 0) {
                                        logger.debug("Training has no lessons (lesson_count=0), skipping completion check: trainingId={}", trainingId);
                                        return;
                                    }
                                    
                                    // Count completed lessons (progress_percentage >= 100)
                                    int completedLessons = getCompletedLessonsCount(enrollmentId);
                                    logger.debug("Progress check: enrollmentId={}, totalLessons={}, completedLessons={}", 
                                        enrollmentId, totalLessons, completedLessons);
                                    
                                    // If all lessons are completed (100%), mark enrollment as completed
                                    if (completedLessons >= totalLessons) {
                                        logger.info("All lessons completed! Marking training as completed: enrollmentId={}, userId={}, trainingId={}, completedLessons={}/{},", 
                                            enrollmentId, userId, trainingId, completedLessons, totalLessons);
                                        TrainingEnrollmentService enrollmentService = TrainingEnrollmentService.getInstance();
                                        enrollmentService.markCompleted(userId, trainingId);
                                        logger.info("Training marked as completed and certificate generation triggered: userId={}, trainingId={}", 
                                            userId, trainingId);
                                    }
                                } else {
                                    logger.warn("Training not found for completion check: trainingId={}", trainingId);
                                }
                            }
                        }
                    } else {
                        logger.warn("Enrollment not found for completion check: enrollmentId={}", enrollmentId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking training completion: enrollmentId={}, error={}", enrollmentId, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error checking training completion: enrollmentId={}, error={}", 
                enrollmentId, e.getMessage(), e);
        }
    }

    public Map<Integer, Integer> getProgressByEnrollment(int enrollmentId) {
        Map<Integer, Integer> progress = new HashMap<>();
        String sql = "SELECT lesson_id, progress_percentage FROM lesson_progress WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progress.put(rs.getInt("lesson_id"), rs.getInt("progress_percentage"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting progress for enrollment: {}", enrollmentId, e);
        }
        return progress;
    }

    public int calculateOverallProgress(int enrollmentId, int totalLessons) {
        if (totalLessons == 0) return 0;
        Map<Integer, Integer> progress = getProgressByEnrollment(enrollmentId);
        int totalProgress = progress.values().stream().mapToInt(Integer::intValue).sum();
        return totalProgress / totalLessons;
    }

    /**
     * Get the number of completed lessons for an enrollment
     * A lesson is considered completed if progress_percentage >= 100
     */
    public int getCompletedLessonsCount(int enrollmentId) {
        String sql = "SELECT COUNT(*) as completed FROM lesson_progress WHERE enrollment_id = ? AND progress_percentage >= 100";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("completed");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting completed lessons count for enrollment: {}", enrollmentId, e);
        }
        return 0;
    }

    /**
     * Mark a lesson as completed (100% progress)
     */
    public void markLessonCompleted(int enrollmentId, int lessonId) {
        logger.info("Marking lesson as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);
        String sql = "INSERT INTO lesson_progress (enrollment_id, lesson_id, progress_percentage, completed, completed_at, last_accessed_at) " +
                     "VALUES (?, ?, 100, TRUE, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE progress_percentage = 100, completed = TRUE, completed_at = ?, last_accessed_at = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, enrollmentId);
            stmt.setInt(2, lessonId);
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.executeUpdate();
            logger.debug("Lesson marked as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);
            
            // Check if training is now complete
            checkAndMarkTrainingComplete(enrollmentId);
        } catch (SQLException e) {
            logger.error("Error marking lesson as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId, e);
        }
    }
}
