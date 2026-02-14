package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TrainingLesson Entity
 * 
 * Represents a lesson within a training course.
 */
public class TrainingLesson {
    private int id;
    private int trainingId;
    private String title;
    private String description;
    private String content; // HTML or markdown content
    private int orderIndex; // Order of lesson in the training
    private int durationMinutes; // Estimated duration
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TrainingLesson() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TrainingLesson(int trainingId, String title, String description, int orderIndex) {
        this();
        this.trainingId = trainingId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
    }

    // Getters
    public int getId() { return id; }
    public int getTrainingId() { return trainingId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getContent() { return content; }
    public int getOrderIndex() { return orderIndex; }
    public int getDurationMinutes() { return durationMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTrainingId(int trainingId) { this.trainingId = trainingId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setContent(String content) { this.content = content; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFormattedDuration() {
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        return minutes > 0 ? hours + "h " + minutes + "min" : hours + "h";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingLesson that = (TrainingLesson) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
