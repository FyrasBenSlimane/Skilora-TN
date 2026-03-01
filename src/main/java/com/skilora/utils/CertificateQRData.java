package com.skilora.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating QR code data from certificate information.
 * 
 * Instead of encoding a URL, this encodes the certificate data directly
 * in JSON format, making it readable by any QR scanner without network access.
 */
public class CertificateQRData {
    
    private static final Logger logger = LoggerFactory.getLogger(CertificateQRData.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Data structure for certificate information in QR code
     */
    public static class CertificateData {
        public String type = "CERTIFICATE";
        public String platform = "Skilora Tunisia";
        public String certificateNumber;
        public String holderName;
        public String trainingTitle;
        public String completionDate;
        public String issuedDate;
        public String verificationToken;
        
        public CertificateData() {}
    }
    
    /**
     * Generates JSON data string from certificate information.
     * This data can be directly encoded in a QR code and read by any scanner.
     * 
     * @param cert Certificate entity
     * @param training Training entity
     * @param user User entity (certificate holder)
     * @return JSON string containing certificate data
     */
    public static String generateQRData(Certificate cert, Training training, User user) {
        if (cert == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (training == null) {
            throw new IllegalArgumentException("Training cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        CertificateData data = new CertificateData();
        
        // Certificate number
        data.certificateNumber = cert.getCertificateNumber() != null 
                ? cert.getCertificateNumber() 
                : "N/A";
        
        // Holder name (full name or username)
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            data.holderName = user.getFullName();
        } else {
            data.holderName = user.getUsername();
        }
        
        // Training title
        data.trainingTitle = training.getTitle() != null 
                ? training.getTitle() 
                : "Unknown Training";
        
        // Completion date
        if (cert.getCompletedAt() != null) {
            data.completionDate = cert.getCompletedAt().format(DATE_FORMAT);
        } else if (cert.getIssuedAt() != null) {
            data.completionDate = cert.getIssuedAt().format(DATE_FORMAT);
        } else {
            data.completionDate = "N/A";
        }
        
        // Issued date
        if (cert.getIssuedAt() != null) {
            data.issuedDate = cert.getIssuedAt().format(DATE_FORMAT);
        } else {
            data.issuedDate = "N/A";
        }
        
        // Verification token (for online verification if needed)
        data.verificationToken = cert.getVerificationToken() != null 
                ? cert.getVerificationToken() 
                : "N/A";
        
        // Convert to JSON
        String jsonData = gson.toJson(data);
        
        logger.info("Generated QR code data for certificate {} ({}): {} characters", 
                cert.getId(), data.certificateNumber, jsonData.length());
        logger.debug("QR code data content: {}", jsonData);
        
        return jsonData;
    }
    
    /**
     * Generates a compact text format for QR code (alternative to JSON).
     * This format is more readable when scanned with basic QR scanners.
     * 
     * @param cert Certificate entity
     * @param training Training entity
     * @param user User entity
     * @return Compact text string
     */
    public static String generateCompactQRData(Certificate cert, Training training, User user) {
        if (cert == null || training == null || user == null) {
            throw new IllegalArgumentException("Certificate, Training, and User cannot be null");
        }
        
        String certNumber = cert.getCertificateNumber() != null ? cert.getCertificateNumber() : "N/A";
        String holderName = (user.getFullName() != null && !user.getFullName().trim().isEmpty()) 
                ? user.getFullName() 
                : user.getUsername();
        String trainingTitle = training.getTitle() != null ? training.getTitle() : "Unknown Training";
        
        String dateStr = "N/A";
        if (cert.getCompletedAt() != null) {
            dateStr = cert.getCompletedAt().format(DATE_FORMAT);
        } else if (cert.getIssuedAt() != null) {
            dateStr = cert.getIssuedAt().format(DATE_FORMAT);
        }
        
        // Format: Platform | Certificate Number | Holder Name | Training Title | Date
        String compactData = String.format(
            "SKILORA CERTIFICATE\n" +
            "Certificat: %s\n" +
            "Titulaire: %s\n" +
            "Formation: %s\n" +
            "Date: %s\n" +
            "Plateforme: Skilora Tunisia",
            certNumber, holderName, trainingTitle, dateStr
        );
        
        logger.info("Generated compact QR code data for certificate {}: {} characters", 
                cert.getId(), compactData.length());
        
        return compactData;
    }
}
