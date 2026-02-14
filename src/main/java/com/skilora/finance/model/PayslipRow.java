package com.skilora.finance.model;

public class PayslipRow {
    private int id, userId, month, year;
    private String employeeName, currency, status;
    private double baseSalary, overtime, overtimeTotal, bonuses, gross, cnss, irpp, otherDeductions, totalDeductions,
            net;

    public PayslipRow(int id, int userId, String employeeName, int month, int year, double baseSalary,
            double overtime, double overtimeTotal, double bonuses, String currency, String status) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.month = month;
        this.year = year;
        this.baseSalary = baseSalary;
        this.overtime = overtime;
        this.overtimeTotal = overtimeTotal;
        this.bonuses = bonuses;
        this.currency = currency;
        this.status = status;
        calculateTotals();
    }

    public void calculateTotals() {
        this.gross = baseSalary + overtimeTotal + bonuses;
        this.cnss = gross * 0.0918; // 9.18%
        this.irpp = (gross - cnss) * 0.26; // 26%
        this.totalDeductions = cnss + irpp + otherDeductions;
        this.net = gross - totalDeductions;
    }

    // Getters and Setters
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

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPeriod() {
        return month + "/" + year;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
        calculateTotals();
    }

    public double getOvertime() {
        return overtime;
    }

    public double getOvertimeTotal() {
        return overtimeTotal;
    }

    public void setOvertimeTotal(double overtimeTotal) {
        this.overtimeTotal = overtimeTotal;
        calculateTotals();
    }

    public double getBonuses() {
        return bonuses;
    }

    public void setBonuses(double bonuses) {
        this.bonuses = bonuses;
        calculateTotals();
    }

    public double getGross() {
        return gross;
    }

    public double getCnss() {
        return cnss;
    }

    public double getIrpp() {
        return irpp;
    }

    public double getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(double otherDeductions) {
        this.otherDeductions = otherDeductions;
        calculateTotals();
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNet() {
        return net;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
