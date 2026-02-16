package com.skilora.formation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Certificate Entity
 * 
 * Represents a certificate issued upon completion of a formation.
 * Maps to the 'certificates' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Certificate {
    private int id;
    private int enrollmentId;
    private String certificateNumber;
    private LocalDateTime issuedDate;
    private String qrCode;
    private String hashValue;
    private String pdfUrl;

    // Default constructor
    public Certificate() {
        this.issuedDate = LocalDateTime.now();
    }

    // Constructor with basic fields
    public Certificate(int enrollmentId, String certificateNumber) {
        this();
        this.enrollmentId = enrollmentId;
        this.certificateNumber = certificateNumber;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getHashValue() {
        return hashValue;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "id=" + id +
                ", enrollmentId=" + enrollmentId +
                ", certificateNumber='" + certificateNumber + '\'' +
                ", issuedDate=" + issuedDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
