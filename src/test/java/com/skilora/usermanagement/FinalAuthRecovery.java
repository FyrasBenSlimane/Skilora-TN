package com.skilora.usermanagement;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.usermanagement.AuthService;
import com.skilora.service.usermanagement.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;

/**
 * FINAL AUTH RECOVERY - Resets both Admin and User accounts
 * Clears lockouts and forces plaintext passwords which are always supported.
 */
public class FinalAuthRecovery {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("FINAL AUTH RECOVERY: ADMIN & USER");
        System.out.println("===========================================\n");
        
        String[][] accounts = {
            {"admin", "admin123", Role.ADMIN},
            {"user", "user123", Role.USER}
        };
        
        try {
            UserService userService = UserService.getInstance();
            AuthService authService = AuthService.getInstance();
            
            for (String[] acc : accounts) {
                String username = acc[0];
                String password = acc[1];
                Role role = (Role) acc[2];
                
                System.out.println("--- Recovering account: " + username + " ---");
                
                // 1. Clear Lockout
                authService.resetLockout(username);
                System.out.println("  ✓ Lockouts cleared.");
                
                // 2. Direct SQL Plaintext Reset (The most reliable way)
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO users (username, email, password, role, is_active, is_verified) " +
                         "VALUES (?, ?, ?, ?, 1, 1) " +
                         "ON DUPLICATE KEY UPDATE password = ?, is_active = 1, is_verified = 1")) {
                    
                    String email = username + "@skilora.com";
                    stmt.setString(1, username);
                    stmt.setString(2, email);
                    stmt.setString(3, password);
                    stmt.setString(4, role.name());
                    stmt.setString(5, password);
                    
                    stmt.executeUpdate();
                    System.out.println("  ✓ Database forced to Plaintext password: " + password);
                }
                
                // 3. Verification
                Optional<User> loginTest = authService.login(username, password);
                if (loginTest.isPresent()) {
                    System.out.println("  ✅ VERIFIED: " + username + " is ready!");
                } else {
                    System.out.println("  ❌ WARNING: Verification failed for " + username);
                }
                System.out.println();
            }
            
            System.out.println("===========================================");
            System.out.println("RECOVERY COMPLETE!");
            System.out.println("Use these credentials now:");
            System.out.println("Admin: admin / admin123");
            System.out.println("User:  user / user123");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("\n❌ CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
