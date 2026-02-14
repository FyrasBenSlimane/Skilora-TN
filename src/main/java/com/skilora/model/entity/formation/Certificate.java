package com.skilora.model.entity.formation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Certificate Entity
 * 
 * Represents a certificate earned by a user after completing a training.
 */
public class Certificate {
    private int id;
    private int userId;
    private int trainingId;
    private String certificateNumber; // Unique certificate ID
    private String verificationToken; // Unique token for online verification
    private LocalDateTime issuedAt;
    private LocalDateTime completedAt; // When training was completed
    private String pdfPath; // Path to generated PDF file

    public Certificate() {
        this.issuedAt = LocalDateTime.now();
    }

    public Certificate(int userId, int trainingId, String certificateNumber) {
        this();
        this.userId = userId;
        this.trainingId = trainingId;
        this.certificateNumber = certificateNumber;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getTrainingId() { return trainingId; }
    public String getCertificateNumber() { return certificateNumber; }
    public String getVerificationToken() { return verificationToken; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getPdfPath() { return pdfPath; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTrainingId(int trainingId) { this.trainingId = trainingId; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

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
