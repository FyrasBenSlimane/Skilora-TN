package com.skilora.community.entity;

import java.time.LocalDateTime;

/**
 * Report entity — maps to the `reports` table.
 */
public class Report {
    private int id;
    private String subject;
    private String type;        // Fraud, Spam, Inappropriate, etc.
    private String description;
    private int reporterId;
    private String reporterName; // transient, joined from users
    private String reportedEntityType;
    private Integer reportedEntityId;
    private String status;      // PENDING, INVESTIGATING, RESOLVED, DISMISSED
    private Integer resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Report() {
        this.status = "PENDING";
    }

    public Report(String subject, String type, String description, int reporterId) {
        this();
        this.subject = subject;
        this.type = type;
        this.description = description;
        this.reporterId = reporterId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReportedEntityType() { return reportedEntityType; }
    public void setReportedEntityType(String reportedEntityType) { this.reportedEntityType = reportedEntityType; }

    public Integer getReportedEntityId() { return reportedEntityId; }
    public void setReportedEntityId(Integer reportedEntityId) { this.reportedEntityId = reportedEntityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Integer resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report that = (Report) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "Report{id=" + id + ", subject='" + subject + "', type='" + type + "', status='" + status + "'}";
    }
}
