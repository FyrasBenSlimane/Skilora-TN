package com.skilora.usermanagement;

import com.skilora.service.usermanagement.UserService;

/**
 * Simple utility to generate BCrypt hash for a password
 * Use this to generate hashes for database updates
 */
public class GenerateBCryptHash {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("BCRYPT HASH GENERATOR");
        System.out.println("===========================================\n");
        
        // Generate hashes for test accounts
        String[] passwords = {"admin123", "user123", "emp123"};
        String[] usernames = {"admin", "user", "employer"};
        
        for (int i = 0; i < passwords.length; i++) {
            String password = passwords[i];
            String username = usernames[i];
            String hash = UserService.hashPassword(password);
            
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
            System.out.println("BCrypt Hash: " + hash);
            System.out.println("SQL Update Command:");
            System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = '" + username + "';");
            System.out.println();
        }
        
        System.out.println("===========================================");
        System.out.println("Copy the SQL commands above and run them in your database");
        System.out.println("===========================================");
    }
}
