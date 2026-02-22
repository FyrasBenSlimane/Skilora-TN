package com.skilora.finance.model;

public class EmployeeSummaryRow {
    private int userId;
    private String fullName;
    private String position;
    private double currentSalary;
    private double totalBonuses;
    private double lastNetPay;
    private String bankStatus;

    public EmployeeSummaryRow(int userId, String fullName, String position, double currentSalary,
            double totalBonuses, double lastNetPay, String bankStatus) {
        this.userId = userId;
        this.fullName = fullName;
        this.position = position;
        this.currentSalary = currentSalary;
        this.totalBonuses = totalBonuses;
        this.lastNetPay = lastNetPay;
        this.bankStatus = bankStatus;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getCurrentSalary() {
        return currentSalary;
    }

    public void setCurrentSalary(double currentSalary) {
        this.currentSalary = currentSalary;
    }

    public double getTotalBonuses() {
        return totalBonuses;
    }

    public void setTotalBonuses(double totalBonuses) {
        this.totalBonuses = totalBonuses;
    }

    public double getLastNetPay() {
        return lastNetPay;
    }

    public void setLastNetPay(double lastNetPay) {
        this.lastNetPay = lastNetPay;
    }

    public String getBankStatus() {
        return bankStatus;
    }

    public void setBankStatus(String bankStatus) {
        this.bankStatus = bankStatus;
    }
}
