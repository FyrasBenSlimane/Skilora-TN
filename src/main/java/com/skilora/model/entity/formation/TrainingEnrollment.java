package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TrainingEnrollment Entity
 * 
 * Represents a user's enrollment in a training course.
 */
public class TrainingEnrollment {
    private int id;
    private int userId;
    private int trainingId;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastAccessedAt;
    private boolean completed;

    // Default constructor
    public TrainingEnrollment() {
        this.enrolledAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.completed = false;
    }

    // Constructor with required fields
    public TrainingEnrollment(int userId, int trainingId) {
        this();
        this.userId = userId;
        this.trainingId = trainingId;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getTrainingId() {
        return trainingId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setTrainingId(int trainingId) {
        this.trainingId = trainingId;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "TrainingEnrollment{" +
                "id=" + id +
                ", userId=" + userId +
                ", trainingId=" + trainingId +
                ", enrolledAt=" + enrolledAt +
                ", completed=" + completed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingEnrollment that = (TrainingEnrollment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
