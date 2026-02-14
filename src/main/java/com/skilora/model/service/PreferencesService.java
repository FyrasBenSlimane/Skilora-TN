package com.skilora.model.service;

import com.skilora.config.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * PreferencesService - Persists per-user preferences in the database.
 *
 * Stores key-value pairs per user, enabling settings like dark mode,
 * language, animations, and notifications to survive across sessions.
 */
public class PreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesService.class);
    private static PreferencesService instance;

    // Default preference keys
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_ANIMATIONS = "animations";
    public static final String KEY_NOTIFICATIONS = "notifications";
    public static final String KEY_SOUND_NOTIFICATIONS = "sound_notifications";

    private PreferencesService() {}

    public static synchronized PreferencesService getInstance() {
        if (instance == null) {
            instance = new PreferencesService();
        }
        return instance;
    }

    /**
     * Ensures the user_preferences table exists.
     */
    public void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_preferences (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                pref_key VARCHAR(100) NOT NULL,
                pref_value VARCHAR(500),
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uq_user_pref (user_id, pref_key),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("user_preferences table ensured.");
        } catch (SQLException e) {
            logger.error("Failed to create user_preferences table: {}", e.getMessage());
        }
    }

    /**
     * Get a single preference value for a user.
     */
    public Optional<String> get(int userId, String key) {
        String sql = "SELECT pref_value FROM user_preferences WHERE user_id = ? AND pref_key = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, key);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("pref_value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get preference {}:{}: {}", userId, key, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Get all preferences for a user as a Map.
     */
    public Map<String, String> getAll(int userId) {
        Map<String, String> prefs = new LinkedHashMap<>();
        String sql = "SELECT pref_key, pref_value FROM user_preferences WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prefs.put(rs.getString("pref_key"), rs.getString("pref_value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get preferences for user {}: {}", userId, e.getMessage());
        }
        return prefs;
    }

    /**
     * Set a single preference (upsert).
     */
    public void set(int userId, String key, String value) {
        String sql = """
            INSERT INTO user_preferences (user_id, pref_key, pref_value)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value)
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, key);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set preference {}:{}={}: {}", userId, key, value, e.getMessage());
        }
    }

    /**
     * Save multiple preferences at once (batch upsert).
     */
    public void saveAll(int userId, Map<String, String> prefs) {
        String sql = """
            INSERT INTO user_preferences (user_id, pref_key, pref_value)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value)
        """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, String> entry : prefs.entrySet()) {
                ps.setInt(1, userId);
                ps.setString(2, entry.getKey());
                ps.setString(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            logger.info("Saved {} preferences for user {}", prefs.size(), userId);

        } catch (SQLException e) {
            logger.error("Failed to save preferences for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get boolean preference with default.
     */
    public boolean getBoolean(int userId, String key, boolean defaultValue) {
        return get(userId, key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /**
     * Get string preference with default.
     */
    public String getString(int userId, String key, String defaultValue) {
        return get(userId, key).orElse(defaultValue);
    }

    /**
     * Delete all preferences for a user.
     */
    public void deleteAll(int userId) {
        String sql = "DELETE FROM user_preferences WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete preferences for user {}: {}", userId, e.getMessage());
        }
    }
}
