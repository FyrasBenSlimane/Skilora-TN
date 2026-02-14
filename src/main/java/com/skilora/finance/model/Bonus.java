package com.skilora.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Bonus {
    private Integer id;
    private Integer userId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private LocalDate dateAwarded;
    private Integer approvedBy;
    private String status; // PENDING, APPROVED, REJECTED

    // Constructors
    public Bonus() {}

    public Bonus(Integer userId, String type, BigDecimal amount, String currency, String reason) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.dateAwarded = LocalDate.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getDateAwarded() { return dateAwarded; }
    public void setDateAwarded(LocalDate dateAwarded) { this.dateAwarded = dateAwarded; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Bonus{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
