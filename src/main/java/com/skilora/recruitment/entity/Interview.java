package com.skilora.recruitment.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Interview Entity
 * 
 * Represents a scheduled interview for a job application.
 * Maps to the 'interviews' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Interview {
    private int id;
    private int applicationId;     // FK to applications.id
    private LocalDateTime scheduledDate;
    private int durationMinutes;   // default 60
    private String type;           // "VIDEO", "IN_PERSON", "PHONE"
    private String location;       // physical address or video link
    private String videoLink;      // Zoom/Jitsi/Teams link
    private String notes;          // interviewer notes
    private String status;         // "SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW"
    private String feedback;       // post-interview feedback
    private int rating;            // 1-5 score
    private String timezone;       // e.g., "Africa/Tunis", "Europe/Paris"
    private LocalDateTime createdDate;

    // Transient fields (from JOINs)
    private String candidateName;
    private String jobTitle;
    private String companyName;

    // Default constructor
    public Interview() {
        this.durationMinutes = 60;
        this.type = "VIDEO";
        this.status = "SCHEDULED";
        this.rating = 0;
        this.timezone = "Africa/Tunis";
        this.createdDate = LocalDateTime.now();
    }

    // Constructor with basic fields
    public Interview(int applicationId, LocalDateTime scheduledDate, String type) {
        this();
        this.applicationId = applicationId;
        this.scheduledDate = scheduledDate;
        this.type = type;
    }

    // Constructor with all fields except id
    public Interview(int applicationId, LocalDateTime scheduledDate, int durationMinutes,
                     String type, String location, String videoLink, String notes, String timezone) {
        this.applicationId = applicationId;
        this.scheduledDate = scheduledDate;
        this.durationMinutes = durationMinutes;
        this.type = type;
        this.location = location;
        this.videoLink = videoLink;
        this.notes = notes;
        this.status = "SCHEDULED";
        this.rating = 0;
        this.timezone = timezone;
        this.createdDate = LocalDateTime.now();
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public String getFeedback() {
        return feedback;
    }

    public int getRating() {
        return rating;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    // Utility methods
    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean isUpcoming() {
        return isScheduled() && scheduledDate != null && scheduledDate.isAfter(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "Interview{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", scheduledDate=" + scheduledDate +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", candidateName='" + candidateName + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interview interview = (Interview) o;
        return id == interview.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
