package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentTransaction Entity
 * Represents a financial transaction (salary payment, bonus, etc.).
 * Maps to the 'payment_transactions' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class PaymentTransaction {
    private int id;
    private Integer payslipId;
    private Integer fromAccountId;
    private Integer toAccountId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private String reference;
    private LocalDateTime transactionDate;
    private String notes;

    public PaymentTransaction() {
        this.currency = "TND";
        this.transactionType = "SALARY";
        this.status = "PENDING";
    }

    public PaymentTransaction(Integer payslipId, BigDecimal amount, Integer toAccountId) {
        this();
        this.payslipId = payslipId;
        this.amount = amount;
        this.toAccountId = toAccountId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getPayslipId() { return payslipId; }
    public void setPayslipId(Integer payslipId) { this.payslipId = payslipId; }

    public Integer getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

    public Integer getToAccountId() { return toAccountId; }
    public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "PaymentTransaction{id=" + id + ", amount=" + amount +
                ", currency='" + currency + "', status='" + status + "'}";
    }
}
