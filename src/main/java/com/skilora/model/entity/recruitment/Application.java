package com.skilora.model.entity.recruitment;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Application Entity
 * 
 * Represents a job application submitted by a candidate.
 * Maps to the 'applications' table in the database.
 */
public class Application {

    public enum Status {
        PENDING("En attente"),
        REVIEWING("En cours"),
        INTERVIEW("Entretien"),
        OFFER("Offre"),
        REJECTED("Refusé"),
        ACCEPTED("Accepté");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int id;
    private int jobOfferId;
    private int candidateProfileId;
    private Status status;
    private LocalDateTime appliedDate;
    private String coverLetter;
    private String customCvUrl;

    // Transient fields (populated by JOINs, not stored directly)
    private String jobTitle;
    private String companyName;
    private String candidateName;
    private String jobLocation;
    private int matchPercentage;
    private int candidateScore;

    public Application() {
        this.status = Status.PENDING;
        this.appliedDate = LocalDateTime.now();
    }

    public Application(int jobOfferId, int candidateProfileId) {
        this();
        this.jobOfferId = jobOfferId;
        this.candidateProfileId = candidateProfileId;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getJobOfferId() {
        return jobOfferId;
    }

    public int getCandidateProfileId() {
        return candidateProfileId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public String getCustomCvUrl() {
        return customCvUrl;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public void setCandidateProfileId(int candidateProfileId) {
        this.candidateProfileId = candidateProfileId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public void setCustomCvUrl(String customCvUrl) {
        this.customCvUrl = customCvUrl;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public int getCandidateScore() {
        return candidateScore;
    }

    public void setCandidateScore(int candidateScore) {
        this.candidateScore = candidateScore;
    }

    @Override
    public String toString() {
        return "Application{id=" + id + ", jobOfferId=" + jobOfferId +
                ", candidateProfileId=" + candidateProfileId +
                ", status=" + status + ", appliedDate=" + appliedDate + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Application that = (Application) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
