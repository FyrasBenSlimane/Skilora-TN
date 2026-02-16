package com.skilora.finance.entity;

import java.time.LocalDateTime;

/**
 * BankAccount Entity
 * Represents a user's bank account for salary payments.
 * Maps to the 'bank_accounts' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class BankAccount {
    private int id;
    private int userId;
    private String bankName;
    private String accountHolder;
    private String iban;
    private String swiftBic;
    private String rib;
    private String currency;
    private boolean isPrimary;
    private LocalDateTime createdDate;

    public BankAccount() {
        this.currency = "TND";
        this.isPrimary = false;
    }

    public BankAccount(int userId, String bankName, String accountHolder) {
        this();
        this.userId = userId;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getSwiftBic() { return swiftBic; }
    public void setSwiftBic(String swiftBic) { this.swiftBic = swiftBic; }

    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "BankAccount{id=" + id + ", bankName='" + bankName +
                "', accountHolder='" + accountHolder + "', isPrimary=" + isPrimary + "}";
    }
}
