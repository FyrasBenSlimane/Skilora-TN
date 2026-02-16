package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AuthService
 * Handles authentication-related business logic.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private static volatile AuthService instance;

    private AuthService() {
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    /**
     * Login with username and password.
     * Includes rate limiting: locks account after MAX_FAILED_ATTEMPTS.
     */
    public Optional<User> login(String username, String password) {
        // Check rate limiting
        if (isLockedOut(username)) {
            logger.warn("Login attempt for locked-out user: {}", username);
            return Optional.empty();
        }

        Optional<User> userOpt = UserService.getInstance().findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (UserService.verifyPassword(password, user.getPassword())) {
                recordLoginAttempt(username, true);
                return Optional.of(user);
            }
        }

        recordLoginAttempt(username, false);
        return Optional.empty();
    }

    /**
     * Get user by username (used after biometric verification).
     */
    public Optional<User> getUser(String username) {
        return UserService.getInstance().findByUsername(username);
    }

    /**
     * Register a new user.
     */
    public void register(User user) {
        if (UserService.getInstance().findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        UserService.getInstance().create(user);
    }

    /**
     * Check if a username is currently locked out due to too many failed attempts.
     */
    public boolean isLockedOut(String username) {
        String sql = "SELECT COUNT(*) FROM login_attempts " +
                "WHERE username = ? AND success = FALSE AND attempted_at > ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES)));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= MAX_FAILED_ATTEMPTS;
                }
            }
        } catch (SQLException e) {
            // Table might not exist yet - don't block login
            logger.debug("login_attempts table not available, skipping rate limit check");
        }
        return false;
    }

    /**
     * Record a login attempt for rate limiting.
     */
    private void recordLoginAttempt(String username, boolean success) {
        String sql = "INSERT INTO login_attempts (username, attempted_at, success) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(3, success);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Table might not exist yet - don't break login
            logger.debug("Could not record login attempt, login_attempts table may not exist");
        }

        // Clean up old attempts (older than 24 hours) to prevent table bloat
        if (success) {
            cleanupOldAttempts(username);
        }
    }

    /**
     * Clean up old login attempts for a user after successful login.
     */
    private void cleanupOldAttempts(String username) {
        String sql = "DELETE FROM login_attempts WHERE username = ? AND attempted_at < ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusHours(24)));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Could not cleanup old login attempts");
        }
    }

    /**
     * Get remaining lockout time in minutes (0 if not locked out).
     * Uses a single query to check both lockout status and remaining time.
     */
    public int getRemainingLockoutMinutes(String username) {
        String sql = "SELECT COUNT(*) AS fail_count, MAX(attempted_at) AS last_attempt FROM login_attempts " +
                "WHERE username = ? AND success = FALSE AND attempted_at > ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES)));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int failCount = rs.getInt("fail_count");
                    if (failCount >= MAX_FAILED_ATTEMPTS) {
                        Timestamp lastAttempt = rs.getTimestamp("last_attempt");
                        if (lastAttempt != null) {
                            LocalDateTime unlockTime = lastAttempt.toLocalDateTime().plusMinutes(LOCKOUT_MINUTES);
                            long remaining = java.time.Duration.between(LocalDateTime.now(), unlockTime).toMinutes();
                            return (int) Math.max(0, remaining + 1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.debug("Could not check lockout time");
        }
        return 0;
    }
}
