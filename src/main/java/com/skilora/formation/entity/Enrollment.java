package com.skilora.formation.entity;

import com.skilora.formation.enums.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Enrollment Entity
 * 
 * Represents a user's enrollment in a formation.
 * Maps to the 'enrollments' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Enrollment {
    private int id;
    private int formationId;
    private int userId;
    private EnrollmentStatus status;
    private double progress;
    private LocalDateTime enrolledDate;
    private LocalDateTime completedDate;

    // Transient fields from JOINs
    private String formationTitle;
    private String userName;

    // Default constructor
    public Enrollment() {
        this.status = EnrollmentStatus.IN_PROGRESS;
        this.progress = 0.00;
        this.enrolledDate = LocalDateTime.now();
    }

    // Constructor with basic fields
    public Enrollment(int formationId, int userId) {
        this();
        this.formationId = formationId;
        this.userId = userId;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getFormationId() {
        return formationId;
    }

    public int getUserId() {
        return userId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public double getProgress() {
        return progress;
    }

    public LocalDateTime getEnrolledDate() {
        return enrolledDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public String getFormationTitle() {
        return formationTitle;
    }

    public String getUserName() {
        return userName;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setEnrolledDate(LocalDateTime enrolledDate) {
        this.enrolledDate = enrolledDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public void setFormationTitle(String formationTitle) {
        this.formationTitle = formationTitle;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "id=" + id +
                ", formationId=" + formationId +
                ", userId=" + userId +
                ", status=" + status +
                ", progress=" + progress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enrollment that = (Enrollment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
