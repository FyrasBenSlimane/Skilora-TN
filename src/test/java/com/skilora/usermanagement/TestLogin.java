package com.skilora.usermanagement;

import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.usermanagement.AuthService;
import com.skilora.service.usermanagement.UserService;

import java.util.Optional;

/**
 * Simple test script to verify login functionality
 * Run this from your IDE or command line to test login
 */
public class TestLogin {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("LOGIN TEST SCRIPT");
        System.out.println("===========================================\n");
        
        // Test credentials
        String[] usernames = {"admin", "user", "employer"};
        String[] passwords = {"admin123", "user123", "emp123"};
        
        AuthService authService = AuthService.getInstance();
        
        for (int i = 0; i < usernames.length; i++) {
            String username = usernames[i];
            String password = passwords[i];
            
            System.out.println("Testing: " + username + " / " + password);
            
            // Check if locked out
            boolean lockedOut = authService.isLockedOut(username);
            if (lockedOut) {
                int minutes = authService.getRemainingLockoutMinutes(username);
                System.out.println("  ❌ Account is LOCKED for " + minutes + " minutes");
                System.out.println("  → Resetting lockout...");
                authService.resetLockout(username);
                System.out.println("  ✓ Lockout reset");
            }
            
            // Check if user exists
            Optional<User> userOpt = UserService.getInstance().findByUsername(username);
            if (!userOpt.isPresent()) {
                System.out.println("  ❌ User not found in database");
                System.out.println();
                continue;
            }
            
            User user = userOpt.get();
            System.out.println("  ✓ User found: " + user.getUsername());
            System.out.println("  ✓ Role: " + user.getRole());
            System.out.println("  ✓ Email: " + user.getEmail());
            System.out.println("  ✓ Password hash type: " + 
                (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") 
                    ? "BCrypt" : "Plaintext"));
            
            // Test password verification
            boolean passwordValid = UserService.verifyPassword(password, user.getPassword());
            System.out.println("  ✓ Password verification: " + (passwordValid ? "VALID" : "INVALID"));
            
            // Test login
            Optional<User> loginResult = authService.login(username, password);
            if (loginResult.isPresent()) {
                System.out.println("  ✅ LOGIN SUCCESS!");
                System.out.println("  → Logged in as: " + loginResult.get().getUsername());
            } else {
                System.out.println("  ❌ LOGIN FAILED");
                System.out.println("  → Check password or account status");
            }
            
            System.out.println();
        }
        
        System.out.println("===========================================");
        System.out.println("TEST COMPLETE");
        System.out.println("===========================================");
    }
}
