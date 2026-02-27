package com.skilora.service.usermanagement;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserService
 * Handles all user-related business logic and data access.
 */
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static volatile UserService instance;

    private UserService() {
    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    // ==================== Password Hashing ====================

    /**
     * Hash a plaintext password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Verify a plaintext password against a hashed password.
     * Also handles legacy plaintext passwords by detecting non-bcrypt strings.
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (storedPassword == null || plainPassword == null) {
            return false;
        }
        // BCrypt hashes start with "$2a$" or "$2b$"
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return BCrypt.checkpw(plainPassword, storedPassword);
        }
        // Legacy plaintext comparison for existing accounts not yet migrated
        return storedPassword.equals(plainPassword);
    }

    // ==================== CRUD Operations ====================

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by username: {}", username, e);
        }
        return Optional.empty();
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by email: {}", email, e);
        }
        return Optional.empty();
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(int id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by id: {}", id, e);
        }
        return Optional.empty();
    }

    /**
     * Get all users
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch all users", e);
        }
        return users;
    }

    /**
     * Create a new user (password will be hashed before storing)
     */
    public void create(User user) {
        String query = "INSERT INTO users (username, email, password, role, full_name, photo_url, is_verified, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashPassword(user.getPassword()));
            stmt.setString(4, user.getRole().name());
            stmt.setString(5, user.getFullName() != null ? user.getFullName() : "");
            stmt.setString(6, user.getPhotoUrl());
            stmt.setBoolean(7, user.isVerified());
            stmt.setBoolean(8, user.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing user.
     * Note: Only re-hashes password if it's not already a bcrypt hash.
     */
    public void update(User user) {
        String query = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, " +
                "full_name = ?, photo_url = ?, is_verified = ?, is_active = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            // Only hash if not already hashed
            String pwd = user.getPassword();
            if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$")) {
                pwd = hashPassword(pwd);
            }
            stmt.setString(3, pwd);
            stmt.setString(4, user.getRole().name());
            stmt.setString(5, user.getFullName() != null ? user.getFullName() : "");
            stmt.setString(6, user.getPhotoUrl());
            stmt.setBoolean(7, user.isVerified());
            stmt.setBoolean(8, user.isActive());
            stmt.setInt(9, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Update only the password for a user (used by password reset).
     */
    public boolean updatePassword(int userId, String newPlainPassword) {
        String query = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, hashPassword(newPlainPassword));
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update password for user id: {}", userId, e);
        }
        return false;
    }

    /**
     * Delete a user by ID
     */
    public void delete(int id) {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete user id: {}", id, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    // ==================== Business Logic ====================

    public List<User> getAllUsers() {
        return findAll();
    }

    public Optional<User> getUserById(int id) {
        return findById(id);
    }

    public void saveUser(User user) {
        if (user.getId() != 0) {
            update(user);
        } else {
            create(user);
        }
    }

    public void deleteUser(int id) {
        delete(id);
    }

    // ==================== Helper Methods ====================

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")),
                rs.getString("full_name"));
        user.setId(rs.getInt("id"));
        try {
            user.setEmail(rs.getString("email"));
        } catch (SQLException e) {
            // Column might not exist yet if migration hasn't run
        }
        try {
            user.setPhotoUrl(rs.getString("photo_url"));
        } catch (SQLException e) {
            // Column might not exist yet if migration hasn't run
        }
        try {
            user.setVerified(rs.getBoolean("is_verified"));
        } catch (SQLException e) {
            // Column might not exist yet if migration hasn't run
        }
        try {
            user.setActive(rs.getBoolean("is_active"));
        } catch (SQLException e) {
            // Column might not exist yet if migration hasn't run
            user.setActive(true);
        }
        return user;
    }
}
