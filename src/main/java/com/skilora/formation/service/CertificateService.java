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
        return c;
    }
}
