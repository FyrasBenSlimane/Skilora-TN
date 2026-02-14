package com.skilora.finance.model;

public class BankAccount {
    private Integer id;
    private Integer userId;
    private String bankName;
    private String iban;
    private String swiftCode;
    private String currency;
    private Boolean verified;
    private Boolean primaryAccount;

    // Constructors
    public BankAccount() {}

    public BankAccount(Integer userId, String bankName, String iban, String currency) {
        this.userId = userId;
        this.bankName = bankName;
        this.iban = iban;
        this.currency = currency;
        this.verified = false;
        this.primaryAccount = false;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public Boolean getPrimaryAccount() { return primaryAccount; }
    public void setPrimaryAccount(Boolean primaryAccount) { this.primaryAccount = primaryAccount; }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", bankName='" + bankName + '\'' +
                ", iban='" + iban + '\'' +
                ", verified=" + verified +
                ", primaryAccount=" + primaryAccount +
                '}';
    }
}
