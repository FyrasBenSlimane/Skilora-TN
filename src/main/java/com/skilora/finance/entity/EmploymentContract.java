package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EmploymentContract Entity
 * Represents an employment contract between an employee and an employer.
 * Maps to the 'employment_contracts' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class EmploymentContract {
    private int id;
    private int userId;
    private Integer employerId;
    private Integer jobOfferId;
    private BigDecimal salaryBase;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String contractType;
    private String status;
    private String pdfUrl;
    private boolean isSigned;
    private LocalDateTime signedDate;
    private LocalDateTime createdDate;

    // Transient fields (not in DB)
    private String userName;
    private String employerName;
    private String jobTitle;

    public EmploymentContract() {
        this.currency = "TND";
        this.contractType = "CDI";
        this.status = "DRAFT";
        this.isSigned = false;
    }

    public EmploymentContract(int userId, BigDecimal salaryBase, LocalDate startDate) {
        this();
        this.userId = userId;
        this.salaryBase = salaryBase;
        this.startDate = startDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getEmployerId() { return employerId; }
    public void setEmployerId(Integer employerId) { this.employerId = employerId; }

    public Integer getJobOfferId() { return jobOfferId; }
    public void setJobOfferId(Integer jobOfferId) { this.jobOfferId = jobOfferId; }

    public BigDecimal getSalaryBase() { return salaryBase; }
    public void setSalaryBase(BigDecimal salaryBase) { this.salaryBase = salaryBase; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public boolean isSigned() { return isSigned; }
    public void setSigned(boolean signed) { isSigned = signed; }

    public LocalDateTime getSignedDate() { return signedDate; }
    public void setSignedDate(LocalDateTime signedDate) { this.signedDate = signedDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmploymentContract that = (EmploymentContract) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "EmploymentContract{id=" + id + ", userId=" + userId +
                ", salaryBase=" + salaryBase + ", currency='" + currency +
                "', status='" + status + "'}";
    }
}
