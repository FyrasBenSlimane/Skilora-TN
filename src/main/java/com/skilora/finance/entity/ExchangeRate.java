package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ExchangeRate Entity
 * Represents currency exchange rates.
 * Maps to the 'exchange_rates' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class ExchangeRate {
    private int id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDate rateDate;
    private String source;
    private LocalDateTime lastUpdated;

    public ExchangeRate() {
        this.source = "BCT";
    }

    public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate rateDate) {
        this();
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeRate that = (ExchangeRate) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "ExchangeRate{" + fromCurrency + " -> " + toCurrency +
                ", rate=" + rate + ", date=" + rateDate + "}";
    }
}
