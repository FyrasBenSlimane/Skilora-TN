package com.skilora.usermanagement;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.usermanagement.AuthService;
import com.skilora.service.usermanagement.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Test script to verify the default user account exists and can login.
 */
public class TestUserAccount {
    
    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("TESTING DEFAULT USER ACCOUNT");
        System.out.println("============================================\n");
        
        try {
            // Check if user exists in database
            System.out.println("1. Checking if user 'user' exists in database...");
            Optional<User> userOpt = UserService.getInstance().findByUsername("user");
            
            if (!userOpt.isPresent()) {
                System.out.println("   ❌ User 'user' NOT FOUND in database!");
                System.out.println("   → Creating user account now...");
                
                User newUser = new User();
                newUser.setUsername("user");
                newUser.setPassword("user123");
                newUser.setRole(Role.USER);
                newUser.setEmail("user@skilora.com");
                newUser.setFullName("Default User");
                newUser.setVerified(true);
                newUser.setActive(true);
                
                UserService.getInstance().create(newUser);
                System.out.println("   ✓ User account created successfully!");
                
                // Re-fetch to verify
                userOpt = UserService.getInstance().findByUsername("user");
            }
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("   ✓ User found!");
                System.out.println("   - ID: " + user.getId());
                System.out.println("   - Username: " + user.getUsername());
                System.out.println("   - Email: " + user.getEmail());
                System.out.println("   - Role: " + user.getRole());
                System.out.println("   - Active: " + user.isActive());
                System.out.println("   - Verified: " + user.isVerified());
                
                // Check password hash
                String passwordHash = user.getPassword();
                System.out.println("   - Password hash preview: " + 
                    (passwordHash != null && passwordHash.length() > 30 
                        ? passwordHash.substring(0, 30) + "..." 
                        : passwordHash));
                System.out.println("   - Password type: " + 
                    (passwordHash != null && (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$"))
                        ? "BCrypt ✓" 
                        : "Plaintext (needs hashing)"));
                
                // Test password verification
                System.out.println("\n2. Testing password verification...");
                boolean passwordValid = UserService.verifyPassword("user123", passwordHash);
                System.out.println("   Password 'user123' verification: " + (passwordValid ? "✓ VALID" : "✗ INVALID"));
                
                if (!passwordValid) {
                    System.out.println("   ⚠️  Password verification failed!");
                    System.out.println("   → This might be because:");
                    System.out.println("      - Password is stored as plaintext but verification expects BCrypt");
                    System.out.println("      - Password hash is corrupted");
                    System.out.println("      - Wrong password stored");
                }
                
                // Test login
                System.out.println("\n3. Testing login with AuthService...");
                AuthService authService = AuthService.getInstance();
                
                // Check lockout
                boolean lockedOut = authService.isLockedOut("user");
                if (lockedOut) {
                    System.out.println("   ⚠️  Account is LOCKED!");
                    int minutes = authService.getRemainingLockoutMinutes("user");
                    System.out.println("   → Resetting lockout...");
                    authService.resetLockout("user");
                    System.out.println("   ✓ Lockout reset");
                }
                
                Optional<User> loginResult = authService.login("user", "user123");
                if (loginResult.isPresent()) {
                    System.out.println("   ✓ LOGIN SUCCESSFUL!");
                    System.out.println("   - Logged in as: " + loginResult.get().getUsername());
                } else {
                    System.out.println("   ✗ LOGIN FAILED!");
                    System.out.println("   → Possible reasons:");
                    System.out.println("      - Password verification failed");
                    System.out.println("      - Account is locked");
                    System.out.println("      - User is inactive");
                }
                
            } else {
                System.out.println("   ✗ Could not find or create user account!");
            }
            
            // List all users in database
            System.out.println("\n4. All users in database:");
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT id, username, email, role, is_active, is_verified FROM users");
                 ResultSet rs = stmt.executeQuery()) {
                
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("   " + count + ". " + rs.getString("username") + 
                        " (" + rs.getString("role") + ") - " +
                        "Active: " + rs.getBoolean("is_active") + 
                        ", Verified: " + rs.getBoolean("is_verified"));
                }
                if (count == 0) {
                    System.out.println("   (No users found)");
                }
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n============================================");
        System.out.println("TEST COMPLETE");
        System.out.println("============================================");
    }
}
