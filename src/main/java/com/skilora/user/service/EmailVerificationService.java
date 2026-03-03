package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Service for email verification during registration.
 * Manages OTP generation, storage, verification, and email verification status.
 */
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private static final int MAX_VERIFICATION_ATTEMPTS = 3;
    private final SecureRandom secureRandom = new SecureRandom();

    private static volatile EmailVerificationService instance;

    private EmailVerificationService() {}

    public static EmailVerificationService getInstance() {
        if (instance == null) {
            synchronized (EmailVerificationService.class) {
                if (instance == null) {
                    instance = new EmailVerificationService();
                }
            }
        }
        return instance;
    }

    /**
     * Generate a 6-digit OTP code.
     */
    public String generateOTP() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Store OTP for email verification.
     * Creates a pending verification record for a new email.
     */
    public void storeVerificationOTP(String email, String otp) {
        // Invalidate any existing unused OTPs for this email
        String invalidateSql = "UPDATE email_verifications SET used = TRUE, expired = TRUE WHERE email = ? AND used = FALSE";
        String insertSql = "INSERT INTO email_verifications (email, otp_code, expires_at, attempts) VALUES (?, ?, ?, 0)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(invalidateSql)) {
                stmt.setString(1, email);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, email);
                stmt.setString(2, otp);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusSeconds(OTP_EXPIRY_SECONDS)));
                stmt.executeUpdate();
            }
            logger.info("Stored verification OTP for email: {}", email);
        } catch (SQLException e) {
            logger.error("Failed to store verification OTP for email: {}", email, e);
            throw new RuntimeException("Failed to store verification OTP", e);
        }
    }

    /**
     * Verify OTP for email verification.
     * Returns true if valid and increments attempt counter.
     */
    public boolean verifyOTP(String email, String otp) {
        String selectSql = "SELECT id, attempts, expires_at FROM email_verifications " +
                "WHERE email = ? AND otp_code = ? AND used = FALSE AND expired = FALSE";
        String updateSql = "UPDATE email_verifications SET attempts = attempts + 1 WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            int verificationId = -1;
            int attempts = 0;
            Timestamp expiresAt = null;

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, email);
                stmt.setString(2, otp);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        verificationId = rs.getInt("id");
                        attempts = rs.getInt("attempts");
                        expiresAt = rs.getTimestamp("expires_at");

                        // Check if expired
                        if (expiresAt != null && expiresAt.before(Timestamp.valueOf(LocalDateTime.now()))) {
                            markExpired(email);
                            logger.warn("OTP expired for email: {}", email);
                            return false;
                        }

                        // Check max attempts
                        if (attempts >= MAX_VERIFICATION_ATTEMPTS - 1) {
                            markExpired(email);
                            logger.warn("Max attempts reached for email: {}", email);
                            return false;
                        }
                    } else {
                        // Increment attempts for wrong OTP
                        incrementAttemptsForWrongOTP(email);
                        return false;
                    }
                }
            }

            // Increment attempts
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, verificationId);
                stmt.executeUpdate();
            }

            // Mark as verified
            markVerified(email, otp);
            logger.info("Email verified successfully: {}", email);
            return true;

        } catch (SQLException e) {
            logger.error("Failed to verify OTP for email: {}", email, e);
        }
        return false;
    }

    /**
     * Check if email is verified.
     */
    public boolean isEmailVerified(String email) {
        String sql = "SELECT 1 FROM email_verifications WHERE email = ? AND verified = TRUE " +
                "AND verified_at > DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check email verification status: {}", email, e);
        }
        return false;
    }

    /**
     * Check if verification is locked (max attempts reached).
     */
    public boolean isVerificationLocked(String email) {
        String sql = "SELECT attempts, expired FROM email_verifications " +
                "WHERE email = ? AND used = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int attempts = rs.getInt("attempts");
                    boolean expired = rs.getBoolean("expired");
                    return attempts >= MAX_VERIFICATION_ATTEMPTS || expired;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check verification lock status: {}", email, e);
        }
        return false;
    }

    /**
     * Get remaining attempts for email verification.
     */
    public int getRemainingAttempts(String email) {
        String sql = "SELECT attempts FROM email_verifications " +
                "WHERE email = ? AND used = FALSE AND expired = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int attempts = rs.getInt("attempts");
                    return Math.max(0, MAX_VERIFICATION_ATTEMPTS - attempts);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get remaining attempts: {}", email, e);
        }
        return 0;
    }

    /**
     * Mark OTP as verified.
     */
    private void markVerified(String email, String otp) {
        String sql = "UPDATE email_verifications SET used = TRUE, verified = TRUE, verified_at = NOW() " +
                "WHERE email = ? AND otp_code = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, otp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark email as verified: {}", email, e);
        }
    }

    /**
     * Mark verification as expired.
     */
    private void markExpired(String email) {
        String sql = "UPDATE email_verifications SET expired = TRUE WHERE email = ? AND used = FALSE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark verification as expired: {}", email, e);
        }
    }

    /**
     * Increment attempts for wrong OTP.
     */
    private void incrementAttemptsForWrongOTP(String email) {
        String sql = "UPDATE email_verifications SET attempts = attempts + 1 " +
                "WHERE email = ? AND used = FALSE AND expired = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to increment attempts: {}", email, e);
        }
    }

    /**
     * Clean up old verifications (older than 24 hours).
     */
    public void cleanupOldVerifications() {
        String sql = "DELETE FROM email_verifications WHERE created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int deleted = stmt.executeUpdate();
            logger.debug("Cleaned up {} old email verifications", deleted);
        } catch (SQLException e) {
            logger.error("Failed to cleanup old verifications", e);
        }
    }

    /**
     * Get OTP expiry seconds.
     */
    public int getExpirySeconds() {
        return OTP_EXPIRY_SECONDS;
    }

    /**
     * Get max attempts.
     */
    public int getMaxAttempts() {
        return MAX_VERIFICATION_ATTEMPTS;
    }
}
