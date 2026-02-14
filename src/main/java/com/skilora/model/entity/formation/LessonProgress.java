package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LessonProgress Entity
 * 
 * Tracks a user's progress through individual lessons.
 */
public class LessonProgress {
    private int id;
    private int enrollmentId;
    private int lessonId;
    private boolean completed;
    private int progressPercentage; // 0-100
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;

    public LessonProgress() {
        this.startedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.completed = false;
        this.progressPercentage = 0;
    }

    public LessonProgress(int enrollmentId, int lessonId) {
        this();
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
    }

    // Getters
    public int getId() { return id; }
    public int getEnrollmentId() { return enrollmentId; }
    public int getLessonId() { return lessonId; }
    public boolean isCompleted() { return completed; }
    public int getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
    public void setCompleted(boolean completed) { 
        this.completed = completed;
        if (completed && completedAt == null) {
            this.completedAt = LocalDateTime.now();
            this.progressPercentage = 100;
        }
    }
    public void setProgressPercentage(int progressPercentage) { 
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
        if (this.progressPercentage == 100) {
            this.completed = true;
            if (this.completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
        }
    }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

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
