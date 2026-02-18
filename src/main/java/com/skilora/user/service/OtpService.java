package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Service for OTP (One-Time Password) operations: generate, store, verify, and consume.
 * Backs the forgot-password flow with database-persisted tokens.
 */
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_SECONDS = 120;
    private final SecureRandom secureRandom = new SecureRandom();

    private static volatile OtpService instance;

    private OtpService() {}

    public static OtpService getInstance() {
        if (instance == null) {
            synchronized (OtpService.class) {
                if (instance == null) {
                    instance = new OtpService();
                }
            }
        }
        return instance;
    }

    /** Returns the OTP expiry duration in seconds. */
    public int getExpirySeconds() {
        return OTP_EXPIRY_SECONDS;
    }

    /** Generate a 6-digit OTP code. */
    public String generate() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Invalidates all unused OTPs for the user, then stores a new one.
     */
    public void store(int userId, String otp) {
        String invalidateSql = "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND used = FALSE";
        String insertSql = "INSERT INTO password_reset_tokens (user_id, otp_code, expires_at) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(invalidateSql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, otp);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SECONDS)));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to store OTP for user id: {}", userId, e);
            throw new RuntimeException("Failed to store OTP", e);
        }
    }

    /**
     * Verify that a valid, unused, non-expired OTP exists for the user.
     */
    public boolean verify(int userId, String otp) {
        String sql = "SELECT 1 FROM password_reset_tokens " +
                "WHERE user_id = ? AND otp_code = ? AND used = FALSE AND expires_at > NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, otp);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to verify OTP for user id: {}", userId, e);
        }
        return false;
    }

    /**
     * Mark a specific OTP as used so it cannot be reused.
     */
    public void markUsed(int userId, String otp) {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND otp_code = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, otp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark OTP as used for user id: {}", userId, e);
        }
    }
}
