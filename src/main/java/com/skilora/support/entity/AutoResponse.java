package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class AutoResponse {
    private int id;
    private String triggerKeyword;
    private String responseText;
    private String category;
    private String language;
    private boolean isActive;
    private int usageCount;
    private LocalDateTime createdDate;
    
    public AutoResponse() {}
    
    public AutoResponse(String triggerKeyword, String responseText, String category, String language) {
        this.triggerKeyword = triggerKeyword;
        this.responseText = responseText;
        this.category = category;
        this.language = language;
        this.isActive = true;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTriggerKeyword() { return triggerKeyword; }
    public void setTriggerKeyword(String triggerKeyword) { this.triggerKeyword = triggerKeyword; }
    
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoResponse that = (AutoResponse) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AutoResponse{" +
                "id=" + id +
                ", triggerKeyword='" + triggerKeyword + '\'' +
                ", category='" + category + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
