package com.skilora.recruitment.entity;

import java.util.Objects;

/**
 * JobOpportunity Entity
 * 
 * Represents a job opportunity from external sources.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class JobOpportunity {
    private String source;
    private String title;
    private String url;
    /**
     * Optional "apply" URL (e.g., extracted from a Reddit post body).
     * If absent, {@link #url} is used as the primary open link.
     */
    private String applyUrl;
    private String description;
    private String location;
    private String postedDate;
    private String rawId;

    public JobOpportunity() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public void setApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    // Additional fields for database offers (from branch merge)
    private int id;
    private String company;
    private String type;
    private String salaryInfo;
    private java.util.List<String> skills;
    private String status;
    private boolean recommended;
    private int matchPercentage;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSalaryInfo() { return salaryInfo; }
    public void setSalaryInfo(String salaryInfo) { this.salaryInfo = salaryInfo; }

    public java.util.List<String> getSkills() { return skills; }
    public void setSkills(java.util.List<String> skills) { this.skills = skills; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }

    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { this.matchPercentage = matchPercentage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobOpportunity that = (JobOpportunity) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() { return Objects.hash(url); }

    @Override
    public String toString() {
        return "JobOpportunity{title='" + title + "', source='" + source + "', location='" + location + "', url='" + url + "'}";
    }
}
