package com.skilora.formation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LessonProgress Entity
 * 
 * Tracks a user's progress through individual lessons/modules within
 * an enrolled formation. Linked to an Enrollment record.
 */
public class LessonProgress {
    private int id;
    private int enrollmentId;
    private int lessonId;
    private boolean completed;
    private double progressPercentage; // 0 to 100
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;

    // Default constructor
    public LessonProgress() {
        this.completed = false;
        this.progressPercentage = 0.0;
        this.startedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public LessonProgress(int enrollmentId, int lessonId) {
        this();
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
    }

    // Full constructor
    public LessonProgress(int id, int enrollmentId, int lessonId, boolean completed,
                          double progressPercentage, LocalDateTime startedAt,
                          LocalDateTime completedAt, LocalDateTime lastAccessedAt) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
        this.completed = completed;
        this.progressPercentage = progressPercentage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.lastAccessedAt = lastAccessedAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public int getLessonId() {
        return lessonId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public void setLessonId(int lessonId) {
        this.lessonId = lessonId;
    }

    /**
     * Sets the completed status. If marked as completed, automatically
     * sets the completedAt timestamp to now. If uncompleted, clears it.
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        } else if (!completed) {
            this.completedAt = null;
        }
    }

    /**
     * Sets the progress percentage, clamped to 0-100 range.
     * If progress reaches 100%, automatically marks lesson as completed.
     */
    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = Math.max(0.0, Math.min(100.0, progressPercentage));
        if (this.progressPercentage >= 100.0 && !this.completed) {
            setCompleted(true);
        }
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    /**
     * Updates the last accessed timestamp to now.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "LessonProgress{" +
                "id=" + id +
                ", enrollmentId=" + enrollmentId +
                ", lessonId=" + lessonId +
                ", completed=" + completed +
                ", progressPercentage=" + progressPercentage +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LessonProgress that = (LessonProgress) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
