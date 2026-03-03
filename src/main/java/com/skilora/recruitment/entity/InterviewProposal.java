package com.skilora.recruitment.entity;

import java.time.LocalDateTime;

/**
 * InterviewProposal — Represents a candidate's proposed time slot for an interview.
 * Enables two-way interview scheduling: candidates can suggest times,
 * employers can accept/reject proposals.
 */
public class InterviewProposal {

    private int id;
    private int applicationId;
    private int proposedBy;
    private LocalDateTime proposedDate;
    private int durationMinutes = 60;
    private String type = "VIDEO"; // VIDEO, IN_PERSON, PHONE
    private String message;
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED, EXPIRED
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;

    // Transient display fields
    private String candidateName;
    private String jobTitle;

    public InterviewProposal() {}

    public InterviewProposal(int applicationId, int proposedBy, LocalDateTime proposedDate) {
        this.applicationId = applicationId;
        this.proposedBy   = proposedBy;
        this.proposedDate = proposedDate;
    }

    // --- Getters & Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getProposedBy() { return proposedBy; }
    public void setProposedBy(int proposedBy) { this.proposedBy = proposedBy; }

    public LocalDateTime getProposedDate() { return proposedDate; }
    public void setProposedDate(LocalDateTime proposedDate) { this.proposedDate = proposedDate; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    @Override
    public String toString() {
        return "InterviewProposal{" +
               "id=" + id +
               ", applicationId=" + applicationId +
               ", proposedDate=" + proposedDate +
               ", status='" + status + '\'' +
               '}';
    }
}
