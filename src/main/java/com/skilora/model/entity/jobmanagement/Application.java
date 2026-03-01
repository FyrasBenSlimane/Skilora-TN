package com.skilora.model.entity.jobmanagement;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Application Entity
 * Represents a job application by a candidate.
 */
public class Application {
    private int id;
    private int jobOfferId;
    private int candidateProfileId;
    private String status = "PENDING";
    private LocalDateTime appliedDate;
    private String coverLetter;
    private String customCvUrl;

    public Application() {
        this.appliedDate = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobOfferId() {
        return jobOfferId;
    }

    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public int getCandidateProfileId() {
        return candidateProfileId;
    }

    public void setCandidateProfileId(int candidateProfileId) {
        this.candidateProfileId = candidateProfileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCustomCvUrl() {
        return customCvUrl;
    }

    public void setCustomCvUrl(String customCvUrl) {
        this.customCvUrl = customCvUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
