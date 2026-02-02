package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class SupportTicket {
    private int id;
    private int userId;
    private String category;
    private String priority;
    private String status;
    private String subject;
    private String description;
    private Integer assignedTo;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime resolvedDate;
    
    // Transient fields
    private String userName;
    private String assignedToName;
    
    public SupportTicket() {}
    
    public SupportTicket(int userId, String category, String priority, String status, 
                        String subject, String description) {
        this.userId = userId;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.subject = subject;
        this.description = description;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    
    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportTicket that = (SupportTicket) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "SupportTicket{" +
                "id=" + id +
                ", userId=" + userId +
                ", category='" + category + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                ", subject='" + subject + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
