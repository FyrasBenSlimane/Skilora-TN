package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for Two-Factor Authentication (2FA) management.
 * Handles 2FA enablement/disablement and OTP verification during login.
 */
public class TwoFactorAuthService {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthService.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int ACCOUNT_LOCKOUT_MINUTES = 30;
    private final SecureRandom secureRandom = new SecureRandom();

    private static volatile TwoFactorAuthService instance;

    private TwoFactorAuthService() {}

    public static TwoFactorAuthService getInstance() {
        if (instance == null) {
            synchronized (TwoFactorAuthService.class) {
                if (instance == null) {
                    instance = new TwoFactorAuthService();
                }
            }
        }
        return instance;
    }

    /**
     * Check if 2FA is enabled for a user.
     */
    public boolean is2FAEnabled(int userId) {
        String sql = "SELECT two_factor_enabled FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("two_factor_enabled");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check 2FA status for user: {}", userId, e);
        }
        return false;
    }

    /**
     * Enable 2FA for a user.
     */
    public void enable2FA(int userId) {
        String sql = "UPDATE users SET two_factor_enabled = TRUE, two_factor_enabled_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            logger.info("2FA enabled for user: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to enable 2FA for user: {}", userId, e);
            throw new RuntimeException("Failed to enable 2FA", e);
        }
    }

    /**
     * Disable 2FA for a user.
     */
    public void disable2FA(int userId) {
        String sql = "UPDATE users SET two_factor_enabled = FALSE, two_factor_enabled_at = NULL WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            // Also clean up any pending 2FA verifications
            cleanup2FAVerifications(userId);
            logger.info("2FA disabled for user: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to disable 2FA for user: {}", userId, e);
            throw new RuntimeException("Failed to disable 2FA", e);
        }
    }

    /**
     * Generate a 6-digit 2FA OTP code.
     */
    public String generateOTP() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Store 2FA OTP for a user during login.
     */
    public void store2FAOTP(int userId, String otp) {
        // Invalidate any existing unused 2FA OTPs
        String invalidateSql = "UPDATE two_factor_verifications SET used = TRUE WHERE user_id = ? AND used = FALSE";
        String insertSql = "INSERT INTO two_factor_verifications (user_id, otp_code, expires_at, attempts) VALUES (?, ?, ?, 0)";

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
            logger.info("Stored 2FA OTP for user: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to store 2FA OTP for user: {}", userId, e);
            throw new RuntimeException("Failed to store 2FA OTP", e);
        }
    }

    /**
     * Verify 2FA OTP during login.
     * Returns true if valid, false otherwise.
     * Locks account after MAX_OTP_ATTEMPTS failed attempts.
     */
    public boolean verify2FAOTP(int userId, String otp) {
        // First check if account is locked
        if (isAccountLocked(userId)) {
            logger.warn("Account locked for user: {}", userId);
            return false;
        }

        String selectSql = "SELECT id, attempts, expires_at FROM two_factor_verifications " +
                "WHERE user_id = ? AND otp_code = ? AND used = FALSE";
        String updateSql = "UPDATE two_factor_verifications SET attempts = attempts + 1 WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            int verificationId = -1;
            int attempts = 0;
            Timestamp expiresAt = null;

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, otp);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        verificationId = rs.getInt("id");
                        attempts = rs.getInt("attempts");
                        expiresAt = rs.getTimestamp("expires_at");

                        // Check if expired
                        if (expiresAt != null && expiresAt.before(Timestamp.valueOf(LocalDateTime.now()))) {
                            mark2FAExpired(userId);
                            increment2FAAttempts(userId);
                            logger.warn("2FA OTP expired for user: {}", userId);
                            return false;
                        }

                        // Check max attempts
                        if (attempts >= MAX_OTP_ATTEMPTS - 1) {
                            mark2FAExpired(userId);
                            lockAccount(userId);
                            logger.warn("Max 2FA attempts reached, account locked for user: {}", userId);
                            return false;
                        }
                    } else {
                        // Wrong OTP - increment attempts
                        increment2FAAttempts(userId);
                        checkAndLockIfNeeded(userId);
                        return false;
                    }
                }
            }

            // Increment attempts
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, verificationId);
                stmt.executeUpdate();
            }

            // Mark as used (successful verification)
            mark2FAUsed(userId, otp);
            clear2FAAttempts(userId);
            logger.info("2FA verified successfully for user: {}", userId);
            return true;

        } catch (SQLException e) {
            logger.error("Failed to verify 2FA OTP for user: {}", userId, e);
        }
        return false;
    }

    /**
     * Check if account is locked due to failed 2FA attempts.
     */
    public boolean isAccountLocked(int userId) {
        String sql = "SELECT two_factor_locked_until FROM users WHERE id = ? AND two_factor_locked_until > NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Failed to check account lock status for user: {}", userId, e);
        }
        return false;
    }

    /**
     * Get remaining lockout minutes for a locked account.
     */
    public int getRemainingLockoutMinutes(int userId) {
        String sql = "SELECT TIMESTAMPDIFF(MINUTE, NOW(), two_factor_locked_until) as remaining " +
                "FROM users WHERE id = ? AND two_factor_locked_until > NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("remaining"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get lockout time for user: {}", userId, e);
        }
        return 0;
    }

    /**
     * Get remaining OTP attempts.
     */
    public int getRemainingAttempts(int userId) {
        String sql = "SELECT attempts FROM two_factor_verifications " +
                "WHERE user_id = ? AND used = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int attempts = rs.getInt("attempts");
                    return Math.max(0, MAX_OTP_ATTEMPTS - attempts);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get remaining attempts for user: {}", userId, e);
        }
        return MAX_OTP_ATTEMPTS;
    }

    /**
     * Lock account after failed 2FA attempts.
     */
    private void lockAccount(int userId) {
        String sql = "UPDATE users SET two_factor_locked_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ACCOUNT_LOCKOUT_MINUTES);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            logger.warn("Account locked for {} minutes for user: {}", ACCOUNT_LOCKOUT_MINUTES, userId);
        } catch (SQLException e) {
            logger.error("Failed to lock account for user: {}", userId, e);
        }
    }

    /**
     * Check if account needs to be locked and lock if needed.
     */
    private void checkAndLockIfNeeded(int userId) {
        String sql = "SELECT attempts FROM two_factor_verifications " +
                "WHERE user_id = ? AND used = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int attempts = rs.getInt("attempts");
                    if (attempts >= MAX_OTP_ATTEMPTS) {
                        lockAccount(userId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check lock needed for user: {}", userId, e);
        }
    }

    /**
     * Increment 2FA attempts counter.
     */
    private void increment2FAAttempts(int userId) {
        String sql = "UPDATE two_factor_verifications SET attempts = attempts + 1 " +
                "WHERE user_id = ? AND used = FALSE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to increment 2FA attempts for user: {}", userId, e);
        }
    }

    /**
     * Clear 2FA attempts after successful verification.
     */
    private void clear2FAAttempts(int userId) {
        String sql = "UPDATE two_factor_verifications SET attempts = 0 " +
                "WHERE user_id = ? AND used = TRUE ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to clear 2FA attempts for user: {}", userId, e);
        }
    }

    /**
     * Mark 2FA OTP as used.
     */
    private void mark2FAUsed(int userId, String otp) {
        String sql = "UPDATE two_factor_verifications SET used = TRUE WHERE user_id = ? AND otp_code = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, otp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark 2FA OTP as used for user: {}", userId, e);
        }
    }

    /**
     * Mark 2FA OTP as expired.
     */
    private void mark2FAExpired(int userId) {
        String sql = "UPDATE two_factor_verifications SET used = TRUE WHERE user_id = ? AND used = FALSE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark 2FA OTP as expired for user: {}", userId, e);
        }
    }

    /**
     * Clean up old 2FA verifications.
     */
    private void cleanup2FAVerifications(int userId) {
        String sql = "DELETE FROM two_factor_verifications WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to cleanup 2FA verifications for user: {}", userId, e);
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
        return MAX_OTP_ATTEMPTS;
    }

    /**
     * Get lockout minutes.
     */
    public int getLockoutMinutes() {
        return ACCOUNT_LOCKOUT_MINUTES;
    }
}
