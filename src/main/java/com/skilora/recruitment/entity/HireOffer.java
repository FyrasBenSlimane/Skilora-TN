package com.skilora.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * HireOffer Entity
 * 
 * Represents a formal employment offer made to a candidate after interview.
 * Maps to the 'hire_offers' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class HireOffer {
    private int id;
    private int applicationId;     // FK to applications.id
    private double salaryOffered;
    private String currency;       // "TND", "EUR", "USD"
    private LocalDate startDate;
    private String contractType;   // "CDI", "CDD", "FREELANCE", "STAGE"
    private String benefits;       // JSON or comma-separated
    private String status;         // "PENDING", "ACCEPTED", "REJECTED", "EXPIRED"
    private LocalDateTime createdDate;
    private LocalDateTime respondedDate;

    // Transient fields
    private String candidateName;
    private String jobTitle;
    private String companyName;

    // Default constructor
    public HireOffer() {
        this.currency = "TND";
        this.contractType = "CDI";
        this.status = "PENDING";
        this.createdDate = LocalDateTime.now();
    }

    // Constructor with basic fields
    public HireOffer(int applicationId, double salaryOffered, String contractType) {
        this();
        this.applicationId = applicationId;
        this.salaryOffered = salaryOffered;
        this.contractType = contractType;
    }

    // Constructor with all fields except id
    public HireOffer(int applicationId, double salaryOffered, String currency,
                     LocalDate startDate, String contractType, String benefits) {
        this.applicationId = applicationId;
        this.salaryOffered = salaryOffered;
        this.currency = currency;
        this.startDate = startDate;
        this.contractType = contractType;
        this.benefits = benefits;
        this.status = "PENDING";
        this.createdDate = LocalDateTime.now();
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public double getSalaryOffered() {
        return salaryOffered;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getContractType() {
        return contractType;
    }

    public String getBenefits() {
        return benefits;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getRespondedDate() {
        return respondedDate;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public void setSalaryOffered(double salaryOffered) {
        this.salaryOffered = salaryOffered;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setRespondedDate(LocalDateTime respondedDate) {
        this.respondedDate = respondedDate;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    // Utility methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public String getFormattedSalary() {
        return String.format("%.2f %s", salaryOffered, currency);
    }

    @Override
    public String toString() {
        return "HireOffer{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", salaryOffered=" + salaryOffered +
                ", currency='" + currency + '\'' +
                ", contractType='" + contractType + '\'' +
                ", status='" + status + '\'' +
                ", candidateName='" + candidateName + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HireOffer hireOffer = (HireOffer) o;
        return id == hireOffer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
