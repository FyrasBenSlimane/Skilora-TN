package com.skilora.recruitment.entity;

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
}
