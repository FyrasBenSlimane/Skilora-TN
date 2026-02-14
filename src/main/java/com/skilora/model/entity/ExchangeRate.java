package com.skilora.model.entity;

import java.time.LocalDate;

/**
 * Exchange Rate Entity
 * Represents currency exchange rates for multi-currency support.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class ExchangeRate {
    private int id;
    private String fromCurrency;
    private String toCurrency;
    private double rate;
    private LocalDate date;
    private String source; // ECB, CBT, etc
    private long timestamp;

    public ExchangeRate() {
        this.date = LocalDate.now();
    }

    public ExchangeRate(String fromCurrency, String toCurrency, double rate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.date = LocalDate.now();
        this.timestamp = System.currentTimeMillis();
    }

    public ExchangeRate(String fromCurrency, String toCurrency, double rate, LocalDate date, String source) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.date = date;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double convertAmount(double amount) {
        return amount * rate;
    }

    @Override
    public String toString() {
        return "ExchangeRate{" +
                "id=" + id +
                ", fromCurrency='" + fromCurrency + '\'' +
                ", toCurrency='" + toCurrency + '\'' +
                ", rate=" + rate +
                ", date=" + date +
                ", source='" + source + '\'' +
                '}';
    }
}
