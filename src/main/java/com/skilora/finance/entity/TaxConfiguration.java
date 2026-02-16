package com.skilora.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TaxConfiguration Entity
 * Represents tax brackets and rates for a given country.
 * Maps to the 'tax_configurations' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class TaxConfiguration {
    private int id;
    private String country;
    private String taxType;
    private BigDecimal rate;
    private BigDecimal minBracket;
    private BigDecimal maxBracket;
    private String description;
    private LocalDate effectiveDate;
    private boolean isActive;

    public TaxConfiguration() {
        this.isActive = true;
        this.minBracket = BigDecimal.ZERO;
    }

    public TaxConfiguration(String country, String taxType, BigDecimal rate, LocalDate effectiveDate) {
        this();
        this.country = country;
        this.taxType = taxType;
        this.rate = rate;
        this.effectiveDate = effectiveDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getMinBracket() { return minBracket; }
    public void setMinBracket(BigDecimal minBracket) { this.minBracket = minBracket; }

    public BigDecimal getMaxBracket() { return maxBracket; }
    public void setMaxBracket(BigDecimal maxBracket) { this.maxBracket = maxBracket; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxConfiguration that = (TaxConfiguration) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "TaxConfiguration{country='" + country + "', taxType='" + taxType +
                "', rate=" + rate + ", bracket=" + minBracket + "-" + maxBracket + "}";
    }
}
