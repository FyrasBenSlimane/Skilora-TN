package com.skilora.recruitment.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class SavedJob {
    private int id;
    private int userId;
    private String jobUrl;
    private String jobTitle;
    private String companyName;
    private String location;
    private String source;
    private LocalDateTime savedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedJob savedJob = (SavedJob) o;
        return id == savedJob.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

