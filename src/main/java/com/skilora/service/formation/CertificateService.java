package com.skilora.service.formation;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.formation.Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CertificateService
 * 
 * Service for managing certificates.
 */
public class CertificateService {
    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);
    private static CertificateService instance;

    private CertificateService() {}

    public static synchronized CertificateService getInstance() {
        if (instance == null) {
            instance = new CertificateService();
        }
        return instance;
    }

    /**
     * Create a new certificate for a user's completed training
     */
    public Certificate createCertificate(int userId, int trainingId) {
        logger.info("Creating certificate for user={}, training={}", userId, trainingId);
        String certNumber = "SKL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String verificationToken = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        
        String sql = "INSERT INTO certificates (user_id, training_id, certificate_number, verification_token, issued_at, completed_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            stmt.setString(3, certNumber);
            stmt.setString(4, verificationToken);
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Certificate INSERT executed, rows affected: {}", rowsAffected);
            
            if (rowsAffected == 0) {
                logger.error("Certificate INSERT failed - no rows affected for user={}, training={}", userId, trainingId);
                return null;
            }
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int certId = rs.getInt(1);
                    Certificate cert = new Certificate();
                    cert.setId(certId);
                    cert.setUserId(userId);
                    cert.setTrainingId(trainingId);
                    cert.setCertificateNumber(certNumber);
                    cert.setVerificationToken(verificationToken);
                    cert.setIssuedAt(now);
                    cert.setCompletedAt(now);
                    logger.info("Certificate created successfully: id={}, number={}, user={}, training={}", 
                        certId, certNumber, userId, trainingId);
                    return cert;
                } else {
                    logger.error("Certificate INSERT succeeded but no generated key returned. Retrieving by user_id and training_id.");
                    return getCertificateByUserAndTraining(userId, trainingId);
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warn("Certificate already exists for user={}, training={}. Retrieving existing certificate.", userId, trainingId);
            return getCertificateByUserAndTraining(userId, trainingId);
        } catch (SQLException e) {
            logger.error("Error creating certificate for user={}, training={}: {}", userId, trainingId, e.getMessage(), e);
            throw new RuntimeException("Failed to create certificate: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get certificate by user ID and training ID
     */
    public Certificate getCertificateByUserAndTraining(int userId, int trainingId) {
        String sql = "SELECT * FROM certificates WHERE user_id = ? AND training_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCertificate(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving certificate by user and training: user={}, training={}, error={}", 
                userId, trainingId, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all certificates for a user
     * Automatically generates certificates for completed trainings that don't have one yet
     */
    public List<Certificate> getUserCertificates(int userId) {
        logger.info("Getting certificates for user={}", userId);
        
        // First, ensure all completed trainings have certificates
        ensureCertificatesForCompletedTrainings(userId);
        
        // Ensure all certificates have verification tokens
        ensureVerificationTokens(userId);
        
        List<Certificate> certificates = new ArrayList<>();
        String sql = "SELECT c.*, t.title as training_title " +
                    "FROM certificates c " +
                    "LEFT JOIN trainings t ON c.training_id = t.id " +
                    "WHERE c.user_id = ? " +
                    "ORDER BY c.issued_at DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    certificates.add(mapResultSetToCertificate(rs));
                }
            }
            logger.info("Found {} certificates for user={}", certificates.size(), userId);
        } catch (SQLException e) {
            logger.error("Error getting user certificates for user={}: {}", userId, e.getMessage(), e);
        }
        return certificates;
    }
    
    /**
     * Ensure certificates exist for all completed trainings that don't have certificates yet
     */
    private void ensureCertificatesForCompletedTrainings(int userId) {
        logger.info("Ensuring certificates for completed trainings: user={}", userId);
        try {
            // Get all completed enrollments without certificates
            String sql = "SELECT te.training_id, t.title " +
                        "FROM training_enrollments te " +
                        "LEFT JOIN trainings t ON te.training_id = t.id " +
                        "WHERE te.user_id = ? " +
                        "AND te.completed = TRUE " +
                        "AND NOT EXISTS (" +
                        "    SELECT 1 FROM certificates c " +
                        "    WHERE c.user_id = te.user_id " +
                        "    AND c.training_id = te.training_id" +
                        ")";
            
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    int successCount = 0;
                    int failCount = 0;
                    while (rs.next()) {
                        int trainingId = rs.getInt("training_id");
                        String trainingTitle = rs.getString("title");
                        count++;
                        logger.info("Found completed training without certificate: user={}, trainingId={}, title={}", 
                            userId, trainingId, trainingTitle);
                        try {
                            // Double-check that certificate doesn't exist (race condition protection)
                            if (!certificateExists(userId, trainingId)) {
                                Certificate cert = createCertificate(userId, trainingId);
                                if (cert != null && cert.getId() > 0) {
                                    successCount++;
                                    logger.info("Successfully auto-generated certificate: id={}, number={}, user={}, training={}", 
                                        cert.getId(), cert.getCertificateNumber(), userId, trainingId);
                                } else {
                                    failCount++;
                                    logger.error("Failed to auto-generate certificate (returned null or invalid): user={}, training={}", 
                                        userId, trainingId);
                                }
                            } else {
                                logger.debug("Certificate already exists (race condition): user={}, training={}", 
                                    userId, trainingId);
                            }
                        } catch (Exception e) {
                            failCount++;
                            logger.error("Error auto-generating certificate for user {} and training {}: {}", 
                                userId, trainingId, e.getMessage(), e);
                        }
                    }
                    if (count == 0) {
                        logger.debug("No completed trainings without certificates found for user={}", userId);
                    } else {
                        logger.info("Processed {} completed trainings without certificates for user={}: {} succeeded, {} failed", 
                            count, userId, successCount, failCount);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error ensuring certificates for completed trainings: user={}, error={}", 
                userId, e.getMessage(), e);
        }
    }
    
    /**
     * Check if a certificate exists for a user and training
     */
    public boolean certificateExists(int userId, int trainingId) {
        String sql = "SELECT COUNT(*) FROM certificates WHERE user_id = ? AND training_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, trainingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking certificate existence for user={}, training={}: {}", 
                userId, trainingId, e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Update certificate PDF path
     */
    public void updatePdfPath(int certificateId, String pdfPath) {
        String sql = "UPDATE certificates SET pdf_path = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pdfPath);
            stmt.setInt(2, certificateId);
            stmt.executeUpdate();
            logger.info("Updated PDF path for certificate id={}: {}", certificateId, pdfPath);
        } catch (SQLException e) {
            logger.error("Error updating certificate PDF path: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update certificate PDF path: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ensure all certificates for a user have verification tokens
     */
    private void ensureVerificationTokens(int userId) {
        logger.debug("Ensuring verification tokens for user={}", userId);
        try {
            String sql = "SELECT id, verification_token FROM certificates WHERE user_id = ? AND (verification_token IS NULL OR verification_token = '')";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        int certId = rs.getInt("id");
                        String verificationToken = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                        String updateSql = "UPDATE certificates SET verification_token = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, verificationToken);
                            updateStmt.setInt(2, certId);
                            updateStmt.executeUpdate();
                            count++;
                            logger.debug("Generated verification token for certificate id={}", certId);
                        } catch (SQLException e) {
                            logger.warn("Could not update verification token for certificate id={}: {}", certId, e.getMessage());
                        }
                    }
                    if (count > 0) {
                        logger.info("Generated verification tokens for {} certificates for user={}", count, userId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.debug("Error ensuring verification tokens for user={}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Map ResultSet to Certificate entity
     */
    private Certificate mapResultSetToCertificate(ResultSet rs) throws SQLException {
        Certificate cert = new Certificate();
        cert.setId(rs.getInt("id"));
        cert.setUserId(rs.getInt("user_id"));
        cert.setTrainingId(rs.getInt("training_id"));
        cert.setCertificateNumber(rs.getString("certificate_number"));
        
        try {
            String verificationToken = rs.getString("verification_token");
            if (verificationToken != null) {
                cert.setVerificationToken(verificationToken);
            }
        } catch (SQLException e) {
            // Column might not exist yet if migration hasn't run
            logger.debug("verification_token column not found, skipping");
        }
        
        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) {
            cert.setIssuedAt(issuedAt.toLocalDateTime());
        }
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            cert.setCompletedAt(completedAt.toLocalDateTime());
        } else {
            cert.setCompletedAt(cert.getIssuedAt());
        }
        
        String pdfPath = rs.getString("pdf_path");
        if (pdfPath != null) {
            cert.setPdfPath(pdfPath);
        }
        
        return cert;
    }
}
