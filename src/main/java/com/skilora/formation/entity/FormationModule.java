package com.skilora.formation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * FormationModule Entity
 * 
 * Represents a module/chapter within a formation.
 * Maps to the 'formation_modules' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class FormationModule {
    private int id;
    private int formationId;
    private String title;
    private String description;
    private String contentUrl;
    private String content; // HTML content for the lesson
    private int durationMinutes;
    private int orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public FormationModule() {
        this.durationMinutes = 0;
        this.orderIndex = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with basic fields
    public FormationModule(int formationId, String title, int orderIndex) {
        this();
        this.formationId = formationId;
        this.title = title;
        this.orderIndex = orderIndex;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getFormationId() {
        return formationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getContent() {
        return content;
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

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns a human-readable formatted duration string.
     */
    public String getFormattedDuration() {
        if (durationMinutes <= 0) return "N/A";
        if (durationMinutes < 60) return durationMinutes + " min";
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        return mins > 0 ? hours + "h" + String.format("%02d", mins) : hours + "h";
    }

    @Override
    public String toString() {
        return "FormationModule{" +
                "id=" + id +
                ", formationId=" + formationId +
                ", title='" + title + '\'' +
                ", orderIndex=" + orderIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormationModule that = (FormationModule) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
