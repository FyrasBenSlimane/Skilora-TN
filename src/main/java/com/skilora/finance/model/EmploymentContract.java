package com.skilora.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmploymentContract {
    private Integer id;
    private Integer userId;
    private Integer employerId;
    private BigDecimal salaryBase;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String contractType; // CDI, CDD, FREELANCE
    private String status;
    private String pdfUrl;
    private Boolean signed;
    private LocalDate signatureDate;

    // Constructors
    public EmploymentContract() {}

    public EmploymentContract(Integer userId, Integer employerId, BigDecimal salaryBase, 
                            String currency, LocalDate startDate, String contractType) {
        this.userId = userId;
        this.employerId = employerId;
        this.salaryBase = salaryBase;
        this.currency = currency;
        this.startDate = startDate;
        this.contractType = contractType;
        this.status = "DRAFT";
        this.signed = false;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getEmployerId() { return employerId; }
    public void setEmployerId(Integer employerId) { this.employerId = employerId; }

    public BigDecimal getSalaryBase() { return salaryBase; }
    public void setSalaryBase(BigDecimal salaryBase) { this.salaryBase = salaryBase; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public Boolean getSigned() { return signed; }
    public void setSigned(Boolean signed) { this.signed = signed; }

    public LocalDate getSignatureDate() { return signatureDate; }
    public void setSignatureDate(LocalDate signatureDate) { this.signatureDate = signatureDate; }

    @Override
    public String toString() {
        return "EmploymentContract{" +
                "id=" + id +
                ", userId=" + userId +
                ", salaryBase=" + salaryBase +
                ", currency='" + currency + '\'' +
                ", contractType='" + contractType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
