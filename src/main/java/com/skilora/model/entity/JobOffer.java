package com.skilora.model.entity;

import com.skilora.model.enums.JobStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JobOffer Entity
 * 
 * Represents a job offer posted by an employer.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class JobOffer {
    private int id;
    private int employerId; // Maps to company_id in DB
    private String title;
    private String description;
    private String location;
    private double salaryMin;
    private double salaryMax;
    private String currency;
    private String workType;
    private List<String> requiredSkills;
    private JobStatus status;
    private LocalDate postedDate;
    private LocalDate deadline;

    // Transient field (populated by JOINs, not stored directly)
    private String companyName;

    // Default constructor
    public JobOffer() {
        this.requiredSkills = new ArrayList<>();
        this.status = JobStatus.DRAFT;
        this.postedDate = LocalDate.now();
    }

    // Constructor with basic fields
    public JobOffer(int employerId, String title, String location) {
        this();
        this.employerId = employerId;
        this.title = title;
        this.location = location;
    }

    // Constructor with all fields except id
    public JobOffer(int employerId, String title, String description, String location,
            double salaryMin, double salaryMax, List<String> requiredSkills, JobStatus status) {
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
        this.status = status;
        this.postedDate = LocalDate.now();
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getEmployerId() {
        return employerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public double getSalaryMin() {
        return salaryMin;
    }

    public double getSalaryMax() {
        return salaryMax;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public JobStatus getStatus() {
        return status;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSalaryMin(double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public void setSalaryMax(double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    // Utility methods
    public String getSalaryRange() {
        if (salaryMin > 0 && salaryMax > 0) {
            return String.format("%.0f - %.0f", salaryMin, salaryMax);
        } else if (salaryMin > 0) {
            return String.format("From %.0f", salaryMin);
        } else if (salaryMax > 0) {
            return String.format("Up to %.0f", salaryMax);
        }
        return "Negotiable";
    }

    public boolean isActive() {
        return status == JobStatus.ACTIVE;
    }

    public void addRequiredSkill(String skill) {
        if (requiredSkills == null) {
            requiredSkills = new ArrayList<>();
        }
        if (!requiredSkills.contains(skill)) {
            requiredSkills.add(skill);
        }
    }

    @Override
    public String toString() {
        return "JobOffer{" +
                "id=" + id +
                ", employerId=" + employerId +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", salaryRange=" + getSalaryRange() +
                ", status=" + status +
                ", requiredSkills=" + requiredSkills +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JobOffer jobOffer = (JobOffer) o;
        return id == jobOffer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
