package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EscrowAccount Entity
 * Represents an escrow hold tied to an employment contract.
 * Maps to the 'escrow_accounts' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class EscrowAccount {
    private int id;
    private int contractId;
    private int adminId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime releasedDate;
    private String releaseNotes;

    // Transient fields (from JOINs)
    private String userName;
    private String employerName;

    public EscrowAccount() {
        this.currency = "TND";
        this.status = "HOLDING";
    }

    public EscrowAccount(int contractId, BigDecimal amount) {
        this();
        this.contractId = contractId;
        this.amount = amount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getReleasedDate() { return releasedDate; }
    public void setReleasedDate(LocalDateTime releasedDate) { this.releasedDate = releasedDate; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EscrowAccount that = (EscrowAccount) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "EscrowAccount{id=" + id + ", contractId=" + contractId +
                ", amount=" + amount + ", currency='" + currency +
                "', status='" + status + "'}";
    }
}
