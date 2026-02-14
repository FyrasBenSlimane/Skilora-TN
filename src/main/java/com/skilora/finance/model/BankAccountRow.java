package com.skilora.finance.model;

public class BankAccountRow {
    private int id, userId;
    private String employeeName, bankName, iban, swift, currency;
    private boolean isPrimary, isVerified;

    public BankAccountRow(int id, int userId, String employeeName, String bankName, String iban,
            String swift, String currency, boolean isPrimary, boolean isVerified) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.bankName = bankName;
        this.iban = iban;
        this.swift = swift;
        this.currency = currency;
        this.isPrimary = isPrimary;
        this.isVerified = isVerified;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
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

    public String getSwift() {
        return swift;
    }

    public void setSwift(String swift) {
        this.swift = swift;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }
}
