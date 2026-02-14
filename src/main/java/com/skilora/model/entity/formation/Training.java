package com.skilora.model.entity.formation;

import com.skilora.model.enums.TrainingCategory;
import com.skilora.model.enums.TrainingLevel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Training Entity
 * 
 * Represents a training course/formation in the system.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Training {
    private int id;
    private String title;
    private String description;
    private Double cost; // Nullable - optional field
    private int duration; // Duration in hours
    private int lessonCount; // Number of lessons in the training
    private TrainingLevel level;
    private TrainingCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Training() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public Training(String title, String description, Double cost, int duration, 
                    int lessonCount, TrainingLevel level, TrainingCategory category) {
        this();
        this.title = title;
        this.description = description;
        this.cost = cost;
        this.duration = duration;
        this.lessonCount = lessonCount;
        this.level = level;
        this.category = category;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Double getCost() {
        return cost;
    }

    public int getDuration() {
        return duration;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public TrainingLevel getLevel() {
        return level;
    }

    public TrainingCategory getCategory() {
        return category;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = lessonCount;
    }

    public void setLevel(TrainingLevel level) {
        this.level = level;
    }

    public void setCategory(TrainingCategory category) {
        this.category = category;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean isFree() {
        return cost == null || cost == 0.0;
    }

    public String getFormattedCost() {
        if (isFree()) {
            return "Gratuit";
        }
        return String.format("%.2f TND", cost);
    }

    public String getFormattedDuration() {
        return duration + " heures";
    }

    public String getFormattedLessonCount() {
        if (lessonCount == 0) {
            return "Aucune leçon";
        } else if (lessonCount == 1) {
            return "1 leçon";
        } else {
            return lessonCount + " leçons";
        }
    }

    @Override
    public String toString() {
        return "Training{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category=" + (category != null ? category.getDisplayName() : "null") +
                ", level=" + (level != null ? level.getDisplayName() : "null") +
                ", cost=" + cost +
                ", duration=" + duration +
                ", lessonCount=" + lessonCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Training training = (Training) o;
        return id == training.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
