package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * LessonProgressService
 * 
 * Handles business logic for lesson progress tracking within formations.
 * Uses direct SQL via DatabaseConfig.
 */
public class LessonProgressService {

    private static final Logger logger = LoggerFactory.getLogger(LessonProgressService.class);
    private static volatile LessonProgressService instance;

    private LessonProgressService() {}

    public static LessonProgressService getInstance() {
        if (instance == null) {
            synchronized (LessonProgressService.class) {
                if (instance == null) {
                    instance = new LessonProgressService();
                }
            }
        }
        return instance;
    }

    /**
     * Resolve the user_id for a given enrollment.
     */
    private int resolveUserId(int enrollmentId) {
        String sql = "SELECT user_id FROM enrollments WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Error resolving user_id for enrollmentId={}", enrollmentId, e);
        }
        return -1;
    }

    /**
     * Update progress for a specific lesson in an enrollment.
     * Uses UPSERT to insert or update in one call.
     * After updating, checks if the entire formation is now complete.
     */
    public void updateProgress(int enrollmentId, int lessonId, int percentage) {
        logger.debug("Updating progress: enrollmentId={}, lessonId={}, percentage={}", enrollmentId, lessonId, percentage);
        int userId = resolveUserId(enrollmentId);
        if (userId < 0) {
            logger.error("Cannot resolve user_id for enrollmentId={}, skipping progress update", enrollmentId);
            return;
        }
        String sql = "INSERT INTO lesson_progress (enrollment_id, module_id, user_id, progress_percentage, last_accessed) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE progress_percentage = ?, last_accessed = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, enrollmentId);
            stmt.setInt(2, lessonId);
            stmt.setInt(3, userId);
            stmt.setInt(4, percentage);
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setInt(6, percentage);
            stmt.setTimestamp(7, Timestamp.valueOf(now));
            stmt.executeUpdate();
            logger.debug("Progress updated successfully: enrollmentId={}, lessonId={}, percentage={}",
                    enrollmentId, lessonId, percentage);

            // Check if formation is now complete
            checkAndMarkFormationComplete(enrollmentId);
        } catch (SQLException e) {
            logger.error("Error updating lesson progress: enrollmentId={}, lessonId={}, percentage={}",
                    enrollmentId, lessonId, percentage, e);
        }
    }

    /**
     * Check if formation is complete (all lessons at 100%) and automatically
     * mark the enrollment as completed, triggering certificate generation.
     */
    private void checkAndMarkFormationComplete(int enrollmentId) {
        try {
            // Get enrollment details to find user_id and formation_id
            String enrollmentSql = "SELECT user_id, formation_id, completed FROM enrollments WHERE id = ?";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(enrollmentSql)) {
                stmt.setInt(1, enrollmentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("user_id");
                        int formationId = rs.getInt("formation_id");
                        boolean alreadyCompleted = rs.getBoolean("completed");

                        if (alreadyCompleted) {
                            logger.debug("Formation already completed: enrollmentId={}", enrollmentId);
                            return;
                        }

                        // Get total number of lessons for this formation
                        String formationSql = "SELECT lesson_count FROM formations WHERE id = ?";
                        try (PreparedStatement fStmt = conn.prepareStatement(formationSql)) {
                            fStmt.setInt(1, formationId);
                            try (ResultSet fRs = fStmt.executeQuery()) {
                                if (fRs.next()) {
                                    int totalLessons = fRs.getInt("lesson_count");
                                    if (totalLessons == 0) {
                                        logger.debug("Formation has no lessons, skipping completion check: formationId={}", formationId);
                                        return;
                                    }

                                    int completedLessons = getCompletedLessonsCount(enrollmentId);
                                    logger.debug("Progress check: enrollmentId={}, totalLessons={}, completedLessons={}",
                                            enrollmentId, totalLessons, completedLessons);

                                    if (completedLessons >= totalLessons) {
                                        logger.info("All lessons completed! Marking formation as completed: enrollmentId={}, userId={}, formationId={}",
                                                enrollmentId, userId, formationId);
                                        EnrollmentService.getInstance().markCompleted(userId, formationId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking formation completion: enrollmentId={}", enrollmentId, e);
        } catch (Exception e) {
            logger.error("Unexpected error checking formation completion: enrollmentId={}", enrollmentId, e);
        }
    }

    /**
     * Get progress map for all lessons in an enrollment.
     * Key = lessonId, Value = progress percentage.
     */
    public Map<Integer, Integer> getProgressByEnrollment(int enrollmentId) {
        Map<Integer, Integer> progress = new HashMap<>();
        String sql = "SELECT module_id, progress_percentage FROM lesson_progress WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progress.put(rs.getInt("module_id"), rs.getInt("progress_percentage"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting progress for enrollment: {}", enrollmentId, e);
        }
        return progress;
    }

    /**
     * Calculate overall progress percentage for an enrollment
     * as the average of individual lesson progresses.
     */
    public int calculateOverallProgress(int enrollmentId, int totalLessons) {
        if (totalLessons == 0) return 0;
        Map<Integer, Integer> progress = getProgressByEnrollment(enrollmentId);
        int totalProgress = progress.values().stream().mapToInt(Integer::intValue).sum();
        return totalProgress / totalLessons;
    }

    /**
     * Get the number of completed lessons for an enrollment.
     * A lesson is considered completed if progress_percentage >= 100.
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
     * Mark a lesson as completed (100% progress).
     * After marking, checks if the entire formation is complete.
     */
    public void markLessonCompleted(int enrollmentId, int lessonId) {
        logger.info("Marking lesson as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);
        int userId = resolveUserId(enrollmentId);
        if (userId < 0) {
            logger.error("Cannot resolve user_id for enrollmentId={}, skipping mark completed", enrollmentId);
            return;
        }
        String sql = "INSERT INTO lesson_progress (enrollment_id, module_id, user_id, progress_percentage, completed, completed_date, last_accessed) " +
                "VALUES (?, ?, ?, 100, TRUE, ?, ?) " +
                "ON DUPLICATE KEY UPDATE progress_percentage = 100, completed = TRUE, completed_date = ?, last_accessed = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, enrollmentId);
            stmt.setInt(2, lessonId);
            stmt.setInt(3, userId);
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.setTimestamp(7, Timestamp.valueOf(now));
            stmt.executeUpdate();
            logger.debug("Lesson marked as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);

            checkAndMarkFormationComplete(enrollmentId);
        } catch (SQLException e) {
            logger.error("Error marking lesson as completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId, e);
        }
    }
}
