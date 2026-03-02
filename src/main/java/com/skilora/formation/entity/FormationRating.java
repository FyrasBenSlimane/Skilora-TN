package com.skilora.formation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * FormationRating Entity
 * 
 * Represents a user's rating and feedback for a completed formation.
 * Each user can only rate a formation once (enforced by unique constraint).
 */
public class FormationRating {
    private int id;
    private int userId;
    private int formationId;
    private Boolean isLiked; // TRUE = liked, FALSE = disliked, NULL = no like/dislike choice
    private int starRating; // 1 to 5 stars
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public FormationRating() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public FormationRating(int userId, int formationId, int starRating) {
        this();
        this.userId = userId;
        this.formationId = formationId;
        this.starRating = starRating;
    }

    // Full constructor
    public FormationRating(int userId, int formationId, Boolean isLiked, int starRating) {
        this(userId, formationId, starRating);
        this.isLiked = isLiked;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getFormationId() {
        return formationId;
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

    public void setFormationId(int formationId) {
        this.formationId = formationId;
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
        return "FormationRating{" +
                "id=" + id +
                ", userId=" + userId +
                ", formationId=" + formationId +
                ", isLiked=" + isLiked +
                ", starRating=" + starRating +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormationRating that = (FormationRating) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
