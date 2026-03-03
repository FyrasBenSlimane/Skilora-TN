package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);
    private static volatile CertificateService instance;

    private CertificateService() {}

    public static CertificateService getInstance() {
        if (instance == null) {
            synchronized (CertificateService.class) {
                if (instance == null) {
                    instance = new CertificateService();
                }
            }
        }
        return instance;
    }

    public int issue(Certificate cert) {
        if (cert == null) throw new IllegalArgumentException("Certificate cannot be null");
        if (cert.getEnrollmentId() <= 0) throw new IllegalArgumentException("Valid enrollment ID is required");
        if (cert.getCertificateNumber() == null || cert.getCertificateNumber().isEmpty()) {
            cert.setCertificateNumber(generateCertificateNumber());
        }
        String sql = """
            INSERT INTO certificates (enrollment_id, certificate_number, issued_date,
                qr_code, hash_value, pdf_url)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, cert.getEnrollmentId());
            stmt.setString(2, cert.getCertificateNumber());
            stmt.setTimestamp(3, Timestamp.valueOf(cert.getIssuedDate()));
            stmt.setString(4, cert.getQrCode());
            stmt.setString(5, cert.getHashValue());
            stmt.setString(6, cert.getPdfUrl());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Certificate issued: id={}, number={}", id, cert.getCertificateNumber());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error issuing certificate: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Certificate findById(int id) {
        String sql = "SELECT * FROM certificates WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCertificate(rs);
        } catch (SQLException e) {
            logger.error("Error finding certificate {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public Certificate findByEnrollment(int enrollmentId) {
        String sql = "SELECT * FROM certificates WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCertificate(rs);
        } catch (SQLException e) {
            logger.error("Error finding certificate for enrollment {}: {}", enrollmentId, e.getMessage(), e);
        }
        return null;
    }

    public List<Certificate> findByUser(int userId) {
        List<Certificate> list = new ArrayList<>();
        String sql = """
            SELECT c.* FROM certificates c
            JOIN enrollments e ON c.enrollment_id = e.id
            WHERE e.user_id = ?
            ORDER BY c.issued_date DESC
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapCertificate(rs));
        } catch (SQLException e) {
            logger.error("Error finding certificates for user {}: {}", userId, e.getMessage(), e);
        }
        return list;
    }

    public Certificate verify(String certificateNumber) {
        String sql = "SELECT * FROM certificates WHERE certificate_number = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, certificateNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info("Certificate verified: {}", certificateNumber);
                return mapCertificate(rs);
            }
        } catch (SQLException e) {
            logger.error("Error verifying certificate {}: {}", certificateNumber, e.getMessage(), e);
        }
        return null;
    }

    public boolean existsForEnrollment(int enrollmentId) {
        String sql = "SELECT 1 FROM certificates WHERE enrollment_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, enrollmentId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking certificate existence: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM certificates WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting certificate: {}", e.getMessage(), e);
        }
        return false;
    }

    public String generateCertificateNumber() {
        return "SKL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Create a new certificate for a user's completed formation.
     * Generates a unique certificate number and verification token.
     */
    public Certificate createCertificate(int userId, int formationId) {
        logger.info("Creating certificate for user={}, formation={}", userId, formationId);
        String certNumber = generateCertificateNumber();
        String verificationToken = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        String sql = "INSERT INTO certificates (user_id, formation_id, enrollment_id, certificate_number, " +
                "verification_token, issued_date, completed_at) " +
                "SELECT ?, ?, e.id, ?, ?, ?, ? FROM enrollments e WHERE e.user_id = ? AND e.formation_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            stmt.setString(3, certNumber);
            stmt.setString(4, verificationToken);
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.setInt(7, userId);
            stmt.setInt(8, formationId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    Certificate cert = new Certificate();
                    cert.setId(rs.getInt(1));
                    cert.setUserId(userId);
                    cert.setFormationId(formationId);
                    cert.setCertificateNumber(certNumber);
                    cert.setVerificationToken(verificationToken);
                    cert.setIssuedDate(now);
                    cert.setCompletedAt(now);
                    logger.info("Certificate created: id={}, number={}", cert.getId(), certNumber);
                    return cert;
                }
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            logger.warn("Certificate already exists for user={}, formation={}", userId, formationId);
            return getCertificateByUserAndFormation(userId, formationId);
        } catch (SQLException e) {
            logger.error("Error creating certificate: user={}, formation={}", userId, formationId, e);
        }
        return null;
    }

    /**
     * Get certificate by user ID and formation ID.
     */
    public Certificate getCertificateByUserAndFormation(int userId, int formationId) {
        String sql = "SELECT * FROM certificates WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCertificate(rs);
        } catch (SQLException e) {
            logger.error("Error finding certificate by user and formation: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Check if a certificate exists for a user and formation.
     */
    public boolean certificateExists(int userId, int formationId) {
        String sql = "SELECT 1 FROM certificates WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, formationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking certificate existence: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Update certificate PDF path/URL.
     */
    public void updatePdfPath(int certificateId, String pdfPath) {
        String sql = "UPDATE certificates SET pdf_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pdfPath);
            stmt.setInt(2, certificateId);
            stmt.executeUpdate();
            logger.info("Updated PDF path for certificate id={}", certificateId);
        } catch (SQLException e) {
            logger.error("Error updating certificate PDF path: {}", e.getMessage(), e);
        }
    }

    /**
     * Get certificate by certificate number (e.g., "SKL-ABC123") for public verification.
     */
    public Certificate getCertificateByNumber(String certificateNumber) {
        String sql = "SELECT * FROM certificates WHERE certificate_number = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, certificateNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.info("Certificate found by number: {}", certificateNumber);
                return mapCertificate(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding certificate by number: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get certificate by verification token for public verification.
     */
    public Certificate getCertificateByToken(String token) {
        String sql = "SELECT * FROM certificates WHERE verification_token = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCertificate(rs);
        } catch (SQLException e) {
            logger.error("Error finding certificate by token: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Ensure all certificates for a user have verification tokens.
     */
    public void ensureVerificationTokens(int userId) {
        String sql = "SELECT id FROM certificates c JOIN enrollments e ON c.enrollment_id = e.id " +
                "WHERE e.user_id = ? AND (c.verification_token IS NULL OR c.verification_token = '')";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int certId = rs.getInt("id");
                String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                String updateSql = "UPDATE certificates SET verification_token = ? WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, token);
                    updateStmt.setInt(2, certId);
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Error ensuring verification tokens for user {}: {}", userId, e.getMessage(), e);
        }
    }

    // ── Private helpers ──

    private Certificate mapCertificate(ResultSet rs) throws SQLException {
        Certificate c = new Certificate();
        c.setId(rs.getInt("id"));
        c.setEnrollmentId(rs.getInt("enrollment_id"));
        c.setCertificateNumber(rs.getString("certificate_number"));
        Timestamp issued = rs.getTimestamp("issued_date");
        if (issued != null) c.setIssuedDate(issued.toLocalDateTime());
        c.setQrCode(rs.getString("qr_code"));
        c.setHashValue(rs.getString("hash_value"));
        c.setPdfUrl(rs.getString("pdf_url"));
        // Map additional columns added by schema migration
        try {
            c.setUserId(rs.getInt("user_id"));
            c.setFormationId(rs.getInt("formation_id"));
            String token = rs.getString("verification_token");
            if (token != null) c.setVerificationToken(token);
            Timestamp completedAt = rs.getTimestamp("completed_at");
            if (completedAt != null) c.setCompletedAt(completedAt.toLocalDateTime());
        } catch (SQLException ignored) {
            // Columns may not exist in older schemas
        }
        return c;
    }
}
