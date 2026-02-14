package com.skilora.model.enums;

/**
 * Job Status Enum
 * 
 * Represents the current status of a job offer.
 * Used in JobOffer entity for tracking job availability.
 */
public enum JobStatus {
    ACTIVE("Active"),
    OPEN("Open"),
    CLOSED("Closed"),
    PENDING("Pending"),
    DRAFT("Draft");

    private final String displayName;

    JobStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
