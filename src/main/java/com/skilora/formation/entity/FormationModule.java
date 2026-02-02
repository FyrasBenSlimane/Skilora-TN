package com.skilora.formation.entity;

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
    private int durationMinutes;
    private int orderIndex;

    // Default constructor
    public FormationModule() {
        this.durationMinutes = 0;
        this.orderIndex = 0;
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
