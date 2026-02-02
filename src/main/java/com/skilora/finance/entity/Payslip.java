package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payslip Entity
 * Represents a monthly payslip for an employee.
 * Maps to the 'payslips' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Payslip {
    private int id;
    private int contractId;
    private int userId;
    private int periodMonth;
    private int periodYear;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal cnssEmployee;
    private BigDecimal cnssEmployer;
    private BigDecimal irpp;
    private BigDecimal otherDeductions;
    private BigDecimal bonuses;
    private String currency;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String pdfUrl;
    private LocalDateTime createdDate;

    // Transient fields
    private String userName;
    private String periodLabel;

    public Payslip() {
        this.currency = "TND";
        this.paymentStatus = "PENDING";
        this.cnssEmployee = BigDecimal.ZERO;
        this.cnssEmployer = BigDecimal.ZERO;
        this.irpp = BigDecimal.ZERO;
        this.otherDeductions = BigDecimal.ZERO;
        this.bonuses = BigDecimal.ZERO;
    }

    public Payslip(int contractId, int userId, int periodMonth, int periodYear, BigDecimal grossSalary) {
        this();
        this.contractId = contractId;
        this.userId = userId;
        this.periodMonth = periodMonth;
        this.periodYear = periodYear;
        this.grossSalary = grossSalary;
        this.netSalary = grossSalary;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public BigDecimal getCnssEmployee() { return cnssEmployee; }
    public void setCnssEmployee(BigDecimal cnssEmployee) { this.cnssEmployee = cnssEmployee; }

    public BigDecimal getCnssEmployer() { return cnssEmployer; }
    public void setCnssEmployer(BigDecimal cnssEmployer) { this.cnssEmployer = cnssEmployer; }

    public BigDecimal getIrpp() { return irpp; }
    public void setIrpp(BigDecimal irpp) { this.irpp = irpp; }

    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { this.otherDeductions = otherDeductions; }

    public BigDecimal getBonuses() { return bonuses; }
    public void setBonuses(BigDecimal bonuses) { this.bonuses = bonuses; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payslip that = (Payslip) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "Payslip{id=" + id + ", period=" + periodMonth + "/" + periodYear +
                ", gross=" + grossSalary + ", net=" + netSalary +
                ", status='" + paymentStatus + "'}";
    }
}
