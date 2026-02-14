package com.skilora.model.entity;

import java.time.LocalDate;

/**
 * Bonus Entity
 * Represents bonus payment records for employees.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Bonus {
    private int id;
    private int userId;
    private double amount;
    private String currency;
    private String reason;
    private LocalDate dateAwarded;
    private String userName; // For display purposes

    public Bonus() {
        this.currency = "TND";
    }

    public Bonus(int userId, double amount, String reason, LocalDate dateAwarded) {
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.dateAwarded = dateAwarded;
        this.currency = "TND";
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getDateAwarded() {
        return dateAwarded;
    }

    public void setDateAwarded(LocalDate dateAwarded) {
        this.dateAwarded = dateAwarded;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Bonus{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", reason='" + reason + '\'' +
                ", dateAwarded=" + dateAwarded +
                '}';
    }
}
