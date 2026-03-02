package com.skilora.security;

import java.time.LocalDateTime;

/**
 * SecurityAlert Entity — stores security events such as
 * failed login camera captures, suspicious activity, etc.
 * Maps to the 'security_alerts' table.
 */
public class SecurityAlert {
    private int id;
    private String username;
    private String alertType;          // FAILED_LOGIN_CAPTURE, LOCKOUT, BRUTE_FORCE
    private String photoPath;          // file path to captured image (nullable)
    private String ipAddress;          // client IP if detectable
    private int failedAttempts;
    private boolean reviewed;
    private String reviewedBy;         // admin username who reviewed
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public SecurityAlert() {
        this.alertType = "FAILED_LOGIN_CAPTURE";
        this.reviewed = false;
    }

    public SecurityAlert(String username, String alertType, int failedAttempts) {
        this();
        this.username = username;
        this.alertType = alertType;
        this.failedAttempts = failedAttempts;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    @Override
    public String toString() {
        return "SecurityAlert{id=" + id + ", username='" + username +
                "', type='" + alertType + "', attempts=" + failedAttempts +
                ", reviewed=" + reviewed + "}";
    }
}
