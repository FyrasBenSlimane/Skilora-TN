package com.skilora.model.entity.recruitment;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Interview Entity
 * 
 * Represents a scheduled interview for an accepted job application.
 * Maps to the 'interviews' table in the database.
 */
public class Interview {

    public enum InterviewType {
        IN_PERSON("En personne"),
        VIDEO("Vidéo"),
        PHONE("Téléphone"),
        ONLINE("En ligne");

        private final String displayName;

        InterviewType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum InterviewStatus {
        SCHEDULED("Planifié"),
        COMPLETED("Terminé"),
        CANCELLED("Annulé"),
        RESCHEDULED("Reprogrammé");

        private final String displayName;

        InterviewStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int id;
    private int applicationId;
    private LocalDateTime interviewDate;
    private String location;
    private InterviewType interviewType;
    private String notes;
    private InterviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields (populated by JOINs, not stored directly)
    private String candidateName;
    private String jobTitle;
    private String companyName;

    public Interview() {
        this.interviewType = InterviewType.IN_PERSON;
        this.status = InterviewStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Interview(int applicationId, LocalDateTime interviewDate) {
        this();
        this.applicationId = applicationId;
        this.interviewDate = interviewDate;
    }

    // Getters
    public int getId() { return id; }
    public int getApplicationId() { return applicationId; }
    public LocalDateTime getInterviewDate() { return interviewDate; }
    public String getLocation() { return location; }
    public InterviewType getInterviewType() { return interviewType; }
    public String getNotes() { return notes; }
    public InterviewStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCandidateName() { return candidateName; }
    public String getJobTitle() { return jobTitle; }
    public String getCompanyName() { return companyName; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    public void setInterviewDate(LocalDateTime interviewDate) { this.interviewDate = interviewDate; }
    public void setLocation(String location) { this.location = location; }
    public void setInterviewType(InterviewType interviewType) { this.interviewType = interviewType; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

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

    @Override
    public String toString() {
        return "Interview{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", interviewDate=" + interviewDate +
                ", location='" + location + '\'' +
                ", interviewType=" + interviewType +
                ", status=" + status +
                '}';
    }
}

