package com.skilora.formation.entity;

import com.skilora.formation.enums.FormationLevel;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Formation Entity
 * 
 * Represents a training course on the platform.
 * Maps to the 'formations' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Formation {
    private int id;
    private String title;
    private String description;
    private String category;
    private int durationHours;
    private double cost;
    private String currency;
    private String provider;
    private String imageUrl;
    private FormationLevel level;
    private boolean isFree;
    private int createdBy;
    private LocalDateTime createdDate;
    private String status;

    // Default constructor
    public Formation() {
        this.durationHours = 0;
        this.cost = 0.00;
        this.currency = "TND";
        this.level = FormationLevel.BEGINNER;
        this.isFree = true;
        this.status = "ACTIVE";
        this.createdDate = LocalDateTime.now();
    }

    // Constructor with basic fields
    public Formation(String title, String category, int durationHours) {
        this();
        this.title = title;
        this.category = category;
        this.durationHours = durationHours;
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

    public String getCategory() {
        return category;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public double getCost() {
        return cost;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public FormationLevel getLevel() {
        return level;
    }

    public boolean isFree() {
        return isFree;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getStatus() {
        return status;
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLevel(FormationLevel level) {
        this.level = level;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Formation{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", durationHours=" + durationHours +
                ", cost=" + cost +
                ", level=" + level +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formation formation = (Formation) o;
        return id == formation.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
