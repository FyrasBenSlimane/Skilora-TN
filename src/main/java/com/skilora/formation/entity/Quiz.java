package com.skilora.formation.entity;

import java.util.Objects;

/**
 * Quiz Entity
 * 
 * Represents a quiz for a formation or module.
 * Maps to the 'quizzes' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Quiz {
    private int id;
    private int formationId;
    private Integer moduleId;
    private String title;
    private String description;
    private int passScore;
    private int maxAttempts;
    private int timeLimitMinutes;

    // Default constructor
    public Quiz() {
        this.passScore = 70;
        this.maxAttempts = 3;
        this.timeLimitMinutes = 30;
    }

    // Constructor with basic fields
    public Quiz(int formationId, String title) {
        this();
        this.formationId = formationId;
        this.title = title;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getFormationId() {
        return formationId;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPassScore() {
        return passScore;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPassScore(int passScore) {
        this.passScore = passScore;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setTimeLimitMinutes(int timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", formationId=" + formationId +
                ", title='" + title + '\'' +
                ", passScore=" + passScore +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quiz quiz = (Quiz) o;
        return id == quiz.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
