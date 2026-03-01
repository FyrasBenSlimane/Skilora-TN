package com.skilora.usermanagement;

import com.skilora.service.usermanagement.UserService;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Test BCrypt password hashing and verification
 * This verifies that the password system correctly uses BCrypt
 */
public class TestBCryptPassword {
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("BCRYPT PASSWORD TEST");
        System.out.println("===========================================\n");
        
        String testPassword = "admin123";
        
        // Test 1: Generate BCrypt hash
        System.out.println("TEST 1: Generate BCrypt Hash");
        System.out.println("-------------------------------------------");
        String hash1 = UserService.hashPassword(testPassword);
        System.out.println("Password: " + testPassword);
        System.out.println("BCrypt Hash: " + hash1);
        System.out.println("Hash starts with $2a$ or $2b$: " + 
            (hash1.startsWith("$2a$") || hash1.startsWith("$2b$")));
        System.out.println();
        
        // Test 2: Verify password against hash
        System.out.println("TEST 2: Verify Password Against Hash");
        System.out.println("-------------------------------------------");
        boolean isValid1 = UserService.verifyPassword(testPassword, hash1);
        System.out.println("Password: " + testPassword);
        System.out.println("Hash: " + hash1);
        System.out.println("Verification Result: " + (isValid1 ? "✅ VALID" : "❌ INVALID"));
        System.out.println();
        
        // Test 3: Verify wrong password fails
        System.out.println("TEST 3: Wrong Password Should Fail");
        System.out.println("-------------------------------------------");
        boolean isValid2 = UserService.verifyPassword("wrongpassword", hash1);
        System.out.println("Password: wrongpassword");
        System.out.println("Hash: " + hash1);
        System.out.println("Verification Result: " + (isValid2 ? "❌ SHOULD FAIL (BUG!)" : "✅ CORRECTLY REJECTED"));
        System.out.println();
        
        // Test 4: Test with plaintext password (legacy support)
        System.out.println("TEST 4: Plaintext Password (Legacy Support)");
        System.out.println("-------------------------------------------");
        String plaintextPassword = "admin123";
        boolean isValid3 = UserService.verifyPassword(plaintextPassword, plaintextPassword);
        System.out.println("Password: " + plaintextPassword);
        System.out.println("Stored (plaintext): " + plaintextPassword);
        System.out.println("Verification Result: " + (isValid3 ? "✅ VALID (Legacy mode)" : "❌ INVALID"));
        System.out.println();
        
        // Test 5: Test with existing hash from database
        System.out.println("TEST 5: Test with Database Hash");
        System.out.println("-------------------------------------------");
        // This is the hash from your database (from the screenshot)
        String dbHash = "$2a$10SjViMQKYD9dPZG77zXqRf8O7mO3c5YgE5YJ0rXzSh4Zz...";
        System.out.println("Database Hash (truncated): " + dbHash);
        System.out.println("Note: This hash is truncated in the display.");
        System.out.println("To test with full hash, you need to query the database.");
        System.out.println();
        
        // Test 6: Generate multiple hashes (should be different each time)
        System.out.println("TEST 6: BCrypt Generates Unique Hashes");
        System.out.println("-------------------------------------------");
        String hash2 = UserService.hashPassword(testPassword);
        String hash3 = UserService.hashPassword(testPassword);
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hash 3: " + hash3);
        System.out.println("All hashes are different: " + 
            (!hash1.equals(hash2) && !hash2.equals(hash3) && !hash1.equals(hash3)));
        System.out.println("All verify correctly: " + 
            (UserService.verifyPassword(testPassword, hash1) &&
             UserService.verifyPassword(testPassword, hash2) &&
             UserService.verifyPassword(testPassword, hash3)));
        System.out.println();
        
        // Test 7: Direct BCrypt library test
        System.out.println("TEST 7: Direct BCrypt Library Test");
        System.out.println("-------------------------------------------");
        String directHash = BCrypt.hashpw(testPassword, BCrypt.gensalt(12));
        boolean directVerify = BCrypt.checkpw(testPassword, directHash);
        System.out.println("Direct BCrypt Hash: " + directHash);
        System.out.println("Direct BCrypt Verify: " + (directVerify ? "✅ VALID" : "❌ INVALID"));
        System.out.println("UserService can verify direct hash: " + 
            (UserService.verifyPassword(testPassword, directHash) ? "✅ YES" : "❌ NO"));
        System.out.println();
        
        System.out.println("===========================================");
        System.out.println("TEST SUMMARY");
        System.out.println("===========================================");
        System.out.println("✅ BCrypt hashing: " + (hash1.startsWith("$2a$") || hash1.startsWith("$2b$")));
        System.out.println("✅ BCrypt verification: " + isValid1);
        System.out.println("✅ Wrong password rejection: " + !isValid2);
        System.out.println("✅ Plaintext legacy support: " + isValid3);
        System.out.println("✅ Unique hash generation: " + (!hash1.equals(hash2)));
        System.out.println("✅ Direct BCrypt compatibility: " + 
            UserService.verifyPassword(testPassword, directHash));
        System.out.println("===========================================");
    }
}
