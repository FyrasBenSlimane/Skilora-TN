package com.skilora.model.entity;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Payslip Entity
 * Represents employee payslip records with salary calculations.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Payslip {
    private int id;
    private int userId;
    private int month;
    private int year;
    private double grossSalary;
    private double netSalary;
    private String currency;
    private String deductionsAmount; // JSON format
    private String bonusesAmount;    // JSON format
    private String pdfUrl;
    private String status; // PENDING, GENERATED, PAID
    private LocalDate generatedDate;
    private String userName; // For display purposes

    public Payslip() {
        this.currency = "TND";
        this.status = "PENDING";
    }

    public Payslip(int userId, int month, int year, double grossSalary, double netSalary) {
        this.userId = userId;
        this.month = month;
        this.year = year;
        this.grossSalary = grossSalary;
        this.netSalary = netSalary;
        this.currency = "TND";
        this.status = "PENDING";
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

    public double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(double netSalary) {
        this.netSalary = netSalary;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDeductionsAmount() {
        return deductionsAmount;
    }

    public void setDeductionsAmount(String deductionsAmount) {
        this.deductionsAmount = deductionsAmount;
    }

    public String getBonusesAmount() {
        return bonusesAmount;
    }

    public void setBonusesAmount(String bonusesAmount) {
        this.bonusesAmount = bonusesAmount;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getDeductions() {
        return grossSalary - netSalary;
    }

    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }

    @Override
    public String toString() {
        return "Payslip{" +
                "id=" + id +
                ", userId=" + userId +
                ", month=" + month +
                ", year=" + year +
                ", grossSalary=" + grossSalary +
                ", netSalary=" + netSalary +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
