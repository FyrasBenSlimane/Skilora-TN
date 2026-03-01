package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TrainingRating Entity
 * 
 * Represents a user's rating and feedback for a completed training.
 * Each user can only rate a training once (enforced by unique constraint).
 */
public class TrainingRating {
    private int id;
    private int userId;
    private int trainingId;
    private Boolean isLiked; // TRUE = liked, FALSE = disliked, NULL = no like/dislike choice
    private int starRating; // 1 to 5 stars
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public TrainingRating() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public TrainingRating(int userId, int trainingId, int starRating) {
        this();
        this.userId = userId;
        this.trainingId = trainingId;
        this.starRating = starRating;
    }

    // Full constructor
    public TrainingRating(int userId, int trainingId, Boolean isLiked, int starRating) {
        this(userId, trainingId, starRating);
        this.isLiked = isLiked;
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

    public Boolean getIsLiked() {
        return isLiked;
    }

    public int getStarRating() {
        return starRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public void setStarRating(int starRating) {
        if (starRating < 1 || starRating > 5) {
            throw new IllegalArgumentException("Star rating must be between 1 and 5");
        }
        this.starRating = starRating;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TrainingRating{" +
                "id=" + id +
                ", userId=" + userId +
                ", trainingId=" + trainingId +
                ", isLiked=" + isLiked +
                ", starRating=" + starRating +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingRating that = (TrainingRating) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
