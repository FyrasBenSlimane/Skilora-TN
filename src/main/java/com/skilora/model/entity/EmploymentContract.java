package com.skilora.model.entity;

import java.time.LocalDate;

/**
 * Employment Contract Entity
 * Represents employment contract information for an employee.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class EmploymentContract {
    private int id;
    private int userId;
    private int companyId;
    private double baseSalary;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String type; // CDI, CDD, Freelance
    private boolean signed;
    private String contractDocumentUrl;
    private String companyName; // For display purposes

    public EmploymentContract() {
        this.currency = "TND";
        this.signed = false;
    }

    public EmploymentContract(int userId, int companyId, double baseSalary, LocalDate startDate, String type) {
        this.userId = userId;
        this.companyId = companyId;
        this.baseSalary = baseSalary;
        this.startDate = startDate;
        this.type = type;
        this.currency = "TND";
        this.signed = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public String getContractDocumentUrl() {
        return contractDocumentUrl;
    }

    public void setContractDocumentUrl(String contractDocumentUrl) {
        this.contractDocumentUrl = contractDocumentUrl;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "EmploymentContract{" +
                "id=" + id +
                ", userId=" + userId +
                ", companyId=" + companyId +
                ", baseSalary=" + baseSalary +
                ", currency='" + currency + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", type='" + type + '\'' +
                ", signed=" + signed +
                '}';
    }
}
