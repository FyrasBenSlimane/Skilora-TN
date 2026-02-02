package com.skilora.community.entity;

import java.time.LocalDateTime;

/**
 * Notification Entity — maps to the `notifications` table.
 */
public class Notification {
    private int id;
    private int userId;
    private String type;       // APPLICATION, VIEW, ACCEPTANCE, MESSAGE, MATCH, INFO, SYSTEM
    private String title;
    private String message;
    private String icon;
    private boolean read;
    private String referenceType; // optional: "application", "job_offer", "user", etc.
    private Integer referenceId;  // optional: ID of linked entity
    private LocalDateTime createdAt;

    public Notification() {
        this.type = "INFO";
        this.icon = "🔔";
        this.read = false;
    }

    public Notification(int userId, String type, String title, String message) {
        this();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
