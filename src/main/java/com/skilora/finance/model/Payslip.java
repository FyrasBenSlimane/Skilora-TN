package com.skilora.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Payslip {
    private Integer id;
    private Integer userId;
    private Integer month;
    private Integer year;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private String currency;
    private String deductionsJson;
    private String bonusesJson;
    private String pdfUrl;
    private LocalDate paymentDate;
    private String status; // DRAFT, SENT, PAID

    // Constructors
    public Payslip() {}

    public Payslip(Integer userId, Integer month, Integer year, BigDecimal grossSalary, String currency) {
        this.userId = userId;
        this.month = month;
        this.year = year;
        this.grossSalary = grossSalary;
        this.currency = currency;
        this.status = "DRAFT";
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDeductionsJson() { return deductionsJson; }
    public void setDeductionsJson(String deductionsJson) { this.deductionsJson = deductionsJson; }

    public String getBonusesJson() { return bonusesJson; }
    public void setBonusesJson(String bonusesJson) { this.bonusesJson = bonusesJson; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Payslip{" +
                "id=" + id +
                ", month=" + month +
                ", year=" + year +
                ", grossSalary=" + grossSalary +
                ", netSalary=" + netSalary +
                ", status='" + status + '\'' +
                '}';
    }
}
