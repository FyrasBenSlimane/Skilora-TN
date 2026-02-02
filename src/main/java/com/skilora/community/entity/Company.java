package com.skilora.community.entity;

import java.util.Objects;

/**
 * Company Entity
 * 
 * Represents an employer company on the platform.
 * Maps to the 'companies' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Company {
    private int id;
    private int ownerId;           // FK to users.id
    private String name;
    private String country;
    private String industry;
    private String website;
    private String logoUrl;
    private boolean verified;
    private String size;           // e.g., "1-10", "11-50", "51-200", "201-500", "500+"

    // Default constructor
    public Company() {
        this.verified = false;
    }

    // Constructor with basic fields
    public Company(int ownerId, String name, String country) {
        this();
        this.ownerId = ownerId;
        this.name = name;
        this.country = country;
    }

    // Constructor with all fields except id
    public Company(int ownerId, String name, String country, String industry, 
                   String website, String logoUrl, boolean verified, String size) {
        this.ownerId = ownerId;
        this.name = name;
        this.country = country;
        this.industry = industry;
        this.website = website;
        this.logoUrl = logoUrl;
        this.verified = verified;
        this.size = size;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getIndustry() {
        return industry;
    }

    public String getWebsite() {
        return website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getSize() {
        return size;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", industry='" + industry + '\'' +
                ", verified=" + verified +
                ", size='" + size + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return id == company.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
