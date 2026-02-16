package com.skilora.user.entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Experience Entity
 * 
 * Represents work experience associated with a profile.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Experience {

    private int id;
    private int profileId;
    private String company;
    private String position;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private boolean currentJob;

    // Constructors
    public Experience() {
    }

    public Experience(int profileId, String company, String position, LocalDate startDate) {
        this.profileId = profileId;
        this.company = company;
        this.position = position;
        this.startDate = startDate;
        this.currentJob = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(boolean currentJob) {
        this.currentJob = currentJob;
        if (currentJob) {
            this.endDate = null;
        }
    }

    // Utility methods
    public int getDurationInMonths() {
        LocalDate end = currentJob ? LocalDate.now() : endDate;
        if (end == null || startDate == null)
            return 0;
        return (int) Period.between(startDate, end).toTotalMonths();
    }

    @Override
    public String toString() {
        return "Experience{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", company='" + company + '\'' +
                ", position='" + position + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", currentJob=" + currentJob +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Experience that = (Experience) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
