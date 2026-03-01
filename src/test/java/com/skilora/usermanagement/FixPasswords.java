package com.skilora.usermanagement;

import com.skilora.config.DatabaseConfig;
import com.skilora.service.usermanagement.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Fix passwords in database - sets them to plaintext for testing
 * OR generates BCrypt hashes and updates them
 */
public class FixPasswords {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("FIX PASSWORDS IN DATABASE");
        System.out.println("===========================================\n");
        
        boolean useBCrypt = false; // Set to true to use BCrypt, false for plaintext
        
        String[][] accounts = {
            {"admin", "admin123"},
            {"employer", "emp123"}
        };
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            for (String[] account : accounts) {
                String username = account[0];
                String password = account[1];
                
                String passwordToStore;
                if (useBCrypt) {
                    passwordToStore = UserService.hashPassword(password);
                    System.out.println("Username: " + username);
                    System.out.println("Password: " + password);
                    System.out.println("BCrypt Hash: " + passwordToStore);
                } else {
                    passwordToStore = password; // Plaintext
                    System.out.println("Username: " + username);
                    System.out.println("Setting password to plaintext: " + password);
                }
                
                String sql = "UPDATE users SET password = ? WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, passwordToStore);
                    stmt.setString(2, username);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        System.out.println("  ✅ Password updated successfully!");
                    } else {
                        System.out.println("  ⚠️  User not found: " + username);
                    }
                }
                System.out.println();
            }
            
            System.out.println("===========================================");
            System.out.println("PASSWORDS FIXED!");
            System.out.println("Now run TestLogin.java again to verify");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("Error fixing passwords: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
