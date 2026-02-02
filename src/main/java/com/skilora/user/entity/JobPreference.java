package com.skilora.user.entity;

import com.skilora.recruitment.enums.WorkType;
import java.util.Objects;

/**
 * JobPreference Entity
 * 
 * Represents job preferences for a profile.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class JobPreference {
    private int id;
    private int profileId;
    private String desiredPosition;
    private double minSalary;
    private double maxSalary;
    private WorkType workType;
    private String locationPreference;
    private boolean remoteWork;

    // Default constructor
    public JobPreference() {
        this.workType = WorkType.FULL_TIME;
        this.remoteWork = false;
    }

    // Constructor with profileId
    public JobPreference(int profileId) {
        this();
        this.profileId = profileId;
    }

    // Constructor with essential fields
    public JobPreference(int profileId, String desiredPosition, WorkType workType) {
        this();
        this.profileId = profileId;
        this.desiredPosition = desiredPosition;
        this.workType = workType;
    }

    // Constructor with all fields except id
    public JobPreference(int profileId, String desiredPosition, double minSalary, double maxSalary,
            WorkType workType, String locationPreference, boolean remoteWork) {
        this.profileId = profileId;
        this.desiredPosition = desiredPosition;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.workType = workType;
        this.locationPreference = locationPreference;
        this.remoteWork = remoteWork;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getProfileId() {
        return profileId;
    }

    public String getDesiredPosition() {
        return desiredPosition;
    }

    public double getMinSalary() {
        return minSalary;
    }

    public double getMaxSalary() {
        return maxSalary;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public String getLocationPreference() {
        return locationPreference;
    }

    public boolean isRemoteWork() {
        return remoteWork;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public void setDesiredPosition(String desiredPosition) {
        this.desiredPosition = desiredPosition;
    }

    public void setMinSalary(double minSalary) {
        this.minSalary = minSalary;
    }

    public void setMaxSalary(double maxSalary) {
        this.maxSalary = maxSalary;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public void setLocationPreference(String locationPreference) {
        this.locationPreference = locationPreference;
    }

    public void setRemoteWork(boolean remoteWork) {
        this.remoteWork = remoteWork;
    }

    // Utility method
    public String getExpectedSalaryRange() {
        if (minSalary > 0 && maxSalary > 0) {
            return String.format("%.0f - %.0f", minSalary, maxSalary);
        } else if (minSalary > 0) {
            return String.format("From %.0f", minSalary);
        } else if (maxSalary > 0) {
            return String.format("Up to %.0f", maxSalary);
        }
        return "Negotiable";
    }

    @Override
    public String toString() {
        return "JobPreference{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", desiredPosition='" + desiredPosition + '\'' +
                ", salaryRange=" + getExpectedSalaryRange() +
                ", workType=" + workType +
                ", locationPreference='" + locationPreference + '\'' +
                ", remoteWork=" + remoteWork +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JobPreference that = (JobPreference) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
