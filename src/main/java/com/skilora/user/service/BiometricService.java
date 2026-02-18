package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.utils.I18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
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
    private static final String PYTHON_SCRIPT_PATH = "python/face_recognition_service.py";
    private static final String PYTHON_CMD = findPythonCommand();

    private BiometricService() {
    }

    /**
     * Detect the correct python3 binary path.
     * On macOS, 'python' doesn't exist — only 'python3'.
     */
    private static String findPythonCommand() {
        // Try common absolute paths first (most reliable for ProcessBuilder)
        String[] candidates = {
            "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3",
            "/usr/local/bin/python3",
            "/opt/homebrew/bin/python3",
            "/usr/bin/python3",
            "python3",
            "python"
        };
        for (String cmd : candidates) {
            try {
                Process p = new ProcessBuilder(cmd, "--version")
                        .redirectErrorStream(true).start();
                int rc = p.waitFor();
                if (rc == 0) {
                    LoggerFactory.getLogger(BiometricService.class)
                            .info("Using Python command: {}", cmd);
                    return cmd;
                }
            } catch (Exception e) {
                // Expected: command not found, skip to next candidate
                LoggerFactory.getLogger(BiometricService.class)
                        .debug("Python candidate '{}' not available: {}", cmd, e.getMessage());
            }
        }
        return "python3"; // fallback
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
        try {
            // Convert image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Build Process
            ProcessBuilder pb;
            if (username != null) {
                pb = new ProcessBuilder(PYTHON_CMD, PYTHON_SCRIPT_PATH, command, username);
            } else {
                pb = new ProcessBuilder(PYTHON_CMD, PYTHON_SCRIPT_PATH, command);
            }

            pb.directory(new File(".")); // Run in project root
            Process process = pb.start();

            // Send Image Data to Stdin
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(base64Image);
                writer.flush();
            }

            // Read Output from Stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // Read Errors
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    logger.error("[Python Error]: " + line);
                }
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.error("Python script timed out after 30 seconds");
                return null;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Python script exited with code: " + exitCode);
                return null;
            }

            String jsonStr = output.toString().trim();
            if (jsonStr.isEmpty())
                return null;

            return new JSONObject(jsonStr);

        } catch (Exception e) {
            logger.error("Python service execution failed", e);
            return null;
        }
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
     * Register a face for a user using the Python service.
     *
     * @param username the user to register
     * @param image    the face image to register
     * @return JSONObject with success/duplicate/message fields, or null on error
     */
    public JSONObject registerFaceResult(String username, BufferedImage image) {
        try {
            JSONObject result = runPythonService("register", username, image);
            if (result == null) {
                return new JSONObject().put("success", false).put("message", I18n.get("biometric.error.service_unavailable"));
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
     * Resolve a username to its user_id in the users table.
     */
    private int resolveUserId(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error resolving user ID for username: " + username, e);
        }
        return -1;
    }
}
