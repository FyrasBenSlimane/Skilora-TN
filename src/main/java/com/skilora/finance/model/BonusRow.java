package com.skilora.finance.model;

public class BonusRow {
    private int id, userId;
    private String employeeName, reason, dateAwarded;
    private double amount;

    public BonusRow(int id, int userId, String employeeName, double amount, String reason, String dateAwarded) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.amount = amount;
        this.reason = reason;
        this.dateAwarded = dateAwarded;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDateAwarded() {
        return dateAwarded;
    }
}
