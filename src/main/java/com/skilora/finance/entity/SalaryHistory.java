package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SalaryHistory Entity
 * Records salary changes for employment contracts.
 * Maps to the 'salary_history' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class SalaryHistory {
    private int id;
    private int contractId;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private String reason;
    private LocalDate effectiveDate;
    private LocalDateTime createdDate;

    public SalaryHistory() {}

    public SalaryHistory(int contractId, BigDecimal oldSalary, BigDecimal newSalary,
                        LocalDate effectiveDate, String reason) {
        this.contractId = contractId;
        this.oldSalary = oldSalary;
        this.newSalary = newSalary;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public BigDecimal getOldSalary() { return oldSalary; }
    public void setOldSalary(BigDecimal oldSalary) { this.oldSalary = oldSalary; }

    public BigDecimal getNewSalary() { return newSalary; }
    public void setNewSalary(BigDecimal newSalary) { this.newSalary = newSalary; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalaryHistory that = (SalaryHistory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "SalaryHistory{id=" + id + ", oldSalary=" + oldSalary +
                ", newSalary=" + newSalary + ", effectiveDate=" + effectiveDate + "}";
    }
}
