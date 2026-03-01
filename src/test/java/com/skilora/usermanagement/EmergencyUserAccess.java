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
 * EMERGENCY FIX FOR 'USER' ACCOUNT
 * Run this to reset the 'user' account and clear any lockouts.
 */
public class EmergencyUserAccess {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("EMERGENCY AUTH RECOVERY: 'USER' ACCOUNT");
        System.out.println("===========================================\n");
        
        String username = "user";
        String password = "user123";
        
        try {
            UserService userService = UserService.getInstance();
            AuthService authService = AuthService.getInstance();
            
            // 1. Reset Lockout
            System.out.println("[Step 1] Clearing lockouts for '" + username + "'...");
            authService.resetLockout(username);
            System.out.println("  ✓ Lockouts cleared.\n");
            
            // 2. Ensure User Exists and Set Password
            System.out.println("[Step 2] Checking user account in database...");
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                System.out.println("  User '" + username + "' found. Updating password and status...");
                User user = userOpt.get();
                user.setPassword(password); // Will be handled by update()
                user.setActive(true);
                user.setVerified(true);
                userService.update(user);
            } else {
                System.out.println("  User '" + username + "' NOT found. Creating new account...");
                User user = new User(username, password, Role.USER, "Fixed User");
                user.setEmail("user@skilora.com");
                user.setActive(true);
                user.setVerified(true);
                userService.create(user);
            }
            System.out.println("  ✓ User account synchronized.\n");
            
            // 3. Final Verification
            System.out.println("[Step 3] Verifying credentials...");
            Optional<User> loginTest = authService.login(username, password);
            
            if (loginTest.isPresent()) {
                System.out.println("\n  ⭐⭐⭐ SUCCESS! ⭐⭐⭐");
                System.out.println("  You can now log in with:");
                System.out.println("  Username: " + username);
                System.out.println("  Password: " + password);
            } else {
                System.out.println("\n  ❌ Verification failed.");
                System.out.println("  Trying manual SQL override to plaintext...");
                
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password = ?, is_active = 1, is_verified = 1 WHERE username = ?")) {
                    stmt.setString(1, password);
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                    System.out.println("  ✓ Manual override applied (Plaintext).");
                    System.out.println("  You can now log in with: " + username + " / " + password);
                }
            }
            
            System.out.println("\n===========================================");
            System.out.println("RECOVERY COMPLETE");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("\n❌ CRITICAL ERROR during recovery: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
