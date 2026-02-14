package com.skilora.model.entity;

/**
 * Bank Account Entity
 * Represents employee banking information for payment processing.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class BankAccount {
    private int id;
    private int userId;
    private String bankName;
    private String iban;
    private String swiftCode;
    private String currency;
    private boolean verified;
    private boolean primaryAccount;
    private String accountHolderName;

    public BankAccount() {
        this.currency = "TND";
        this.verified = false;
        this.primaryAccount = false;
    }

    public BankAccount(int userId, String bankName, String iban, String accountHolderName) {
        this.userId = userId;
        this.bankName = bankName;
        this.iban = iban;
        this.accountHolderName = accountHolderName;
        this.currency = "TND";
        this.verified = false;
        this.primaryAccount = false;
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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isPrimaryAccount() {
        return primaryAccount;
    }

    public void setPrimaryAccount(boolean primaryAccount) {
        this.primaryAccount = primaryAccount;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getIbanMasked() {
        if (iban != null && iban.length() >= 4) {
            return "****" + iban.substring(iban.length() - 4);
        }
        return iban;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", userId=" + userId +
                ", bankName='" + bankName + '\'' +
                ", iban='" + getIbanMasked() + '\'' +
                ", currency='" + currency + '\'' +
                ", verified=" + verified +
                ", primaryAccount=" + primaryAccount +
                '}';
    }
}
