package com.skilora.model.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.BiometricData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.json.JSONObject;

/**
 * BiometricService
 * 
 * Manages biometric authentication data and verification.
 * Interacts with Python Face Recognition Service.
 * This follows the MVC pattern where Service = Model layer (data + logic).
 * 
 * Note: No JavaFX imports allowed in this class.
 */
public class BiometricService {

    private static final Logger logger = LoggerFactory.getLogger(BiometricService.class);

    private static BiometricService instance;

    private BiometricService() {
    }

    public static synchronized BiometricService getInstance() {
        if (instance == null) {
            instance = new BiometricService();
        }
        return instance;
    }

    /**
     * Verify face against stored encodings using Python service.
     */
    /**
     * Verify face and return full result including confidence score.
     */
    public JSONObject verifyFaceResult(String username, BufferedImage image) {
        try {
            JSONObject result = runPythonService("verify", username, image);
            return result != null ? result : new JSONObject().put("success", false).put("verified", false);
        } catch (Exception e) {
            logger.error("Face verification failed", e);
            return new JSONObject().put("success", false).put("verified", false);
        }
    }

    /**
     * Boolean convenience wrapper.
     */
    public boolean verifyFace(String username, BufferedImage image) {
        JSONObject result = verifyFaceResult(username, image);
        return result.optBoolean("verified", false);
    }

    /**
     * Identify user from face image using Python service.
     * 
     * @return Optional username if found
     */
    public Optional<String> identifyUser(BufferedImage image) {
        try {
            JSONObject result = runPythonService("verify", null, image);
            if (result != null && result.has("success") && result.getBoolean("success")) {
                if (result.optBoolean("verified", false) && result.has("username")) {
                    return Optional.ofNullable(result.getString("username"));
                }
            }
        } catch (Exception e) {
            logger.error("User identification failed", e);
        }
        return Optional.empty();
    }

    private JSONObject runPythonService(String command, String username, BufferedImage image) {
        logger.warn("Biometric service is disabled. Python script has been removed.");
        return null;
    }

    /**
     * Check if user has biometric data registered.
     */
    public boolean hasBiometricData(String username) {
        String query = "SELECT 1 FROM biometric_data bd JOIN users u ON bd.user_id = u.id WHERE u.username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return true;
            }
        } catch (SQLException e) {
            logger.error("Error checking biometric data for user: " + username, e);
        }
        return false;
    }

    /**
     * Register biometric data for a user.
     */
    public void registerBiometric(String username, String faceEncodingJson) {
        // Resolve user_id from username
        int userId = resolveUserId(username);
        if (userId == -1) {
            logger.error("Cannot register biometric: user '{}' not found.", username);
            return;
        }

        // Check if entry already exists for this user
        String checkQuery = "SELECT id FROM biometric_data WHERE user_id = ?";
        String insertQuery = "INSERT INTO biometric_data (user_id, face_encoding, last_login) VALUES (?, ?, NOW())";
        String updateQuery = "UPDATE biometric_data SET face_encoding = ?, last_login = NOW() WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            boolean exists = false;
            try (PreparedStatement check = conn.prepareStatement(checkQuery)) {
                check.setInt(1, userId);
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setBytes(1, faceEncodingJson.getBytes(StandardCharsets.UTF_8));
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setInt(1, userId);
                    stmt.setBytes(2, faceEncodingJson.getBytes(StandardCharsets.UTF_8));
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Error registering biometric for user: " + username, e);
        }
    }

    /**
     * Get biometric data for a user.
     */
    public Optional<BiometricData> getBiometricData(String username) {
        String query = "SELECT bd.face_encoding, bd.last_login FROM biometric_data bd JOIN users u ON bd.user_id = u.id WHERE u.username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] encodingBytes = rs.getBytes("face_encoding");
                    String encodingStr = encodingBytes != null ? new String(encodingBytes, StandardCharsets.UTF_8)
                            : "{}";
                    BiometricData data = new BiometricData(username, encodingStr);
                    java.sql.Timestamp ts = rs.getTimestamp("last_login");
                    if (ts != null)
                        data.setRegisteredAt(ts.getTime());
                    return Optional.of(data);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting biometric data for user: " + username, e);
        }
        return Optional.empty();
    }

    /**
     * Remove biometric data for a user.
     */
    public void removeBiometric(String username) {
        String query = "DELETE FROM biometric_data WHERE user_id = (SELECT id FROM users WHERE username = ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing biometric for user: " + username, e);
        }
    }

    /**
     * Resolve a username to its user_id in the users table.
     */
    private int resolveUserId(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error resolving user ID for username: " + username, e);
        }
        return -1;
    }

    /**
     * Register a face for a user using the Python service.
     * Captures the image, sends to Python for encoding, and stores in DB.
     * The Python service will reject duplicates (same face already linked to
     * another account).
     *
     * @param username the user to register
     * @param image    the face image to register
     * @return JSONObject with success/duplicate/message fields, or null on error
     */
    public JSONObject registerFaceResult(String username, BufferedImage image) {
        try {
            JSONObject result = runPythonService("register", username, image);
            if (result == null) {
                return new JSONObject().put("success", false).put("message", "Python service unavailable");
            }
            if (result.has("success") && result.getBoolean("success")) {
                // Success — save encoding to DB too
                if (result.has("encoding")) {
                    registerBiometric(username, result.get("encoding").toString());
                } else {
                    registerBiometric(username, "{}");
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Face registration failed", e);
            return new JSONObject().put("success", false).put("message", e.getMessage());
        }
    }

    /**
     * Simplified boolean wrapper for backward compatibility.
     */
    public boolean registerFace(String username, BufferedImage image) {
        JSONObject result = registerFaceResult(username, image);
        return result != null && result.optBoolean("success", false);
    }

    /**
     * Check if the given face already belongs to a different registered account.
     *
     * @param image           the face image to check
     * @param excludeUsername username to exclude from duplicate check (e.g. current
     *                        user re-registering)
     * @return JSONObject with duplicate/matched_username/message fields
     */
    public JSONObject checkDuplicateFace(BufferedImage image, String excludeUsername) {
        try {
            JSONObject result = runPythonService("check_duplicate", excludeUsername, image);
            if (result == null) {
                return new JSONObject().put("success", false).put("duplicate", false)
                        .put("message", "Python service unavailable");
            }
            return result;
        } catch (Exception e) {
            logger.error("Duplicate face check failed", e);
            return new JSONObject().put("success", false).put("duplicate", false)
                    .put("message", e.getMessage());
        }
    }

    /**
     * Check if biometric authentication is enabled for user.
     */
    public boolean isBiometricEnabled(String username) {
        return hasBiometricData(username);
    }
}
