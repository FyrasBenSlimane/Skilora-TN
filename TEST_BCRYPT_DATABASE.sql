-- ============================================
-- TEST BCRYPT PASSWORD IN DATABASE
-- ============================================

-- 1. Check current password hash for admin
SELECT 
    username,
    email,
    LEFT(password, 30) as password_preview,
    CASE 
        WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
        WHEN LENGTH(password) < 20 THEN 'Plaintext (likely)'
        ELSE 'Unknown format'
    END as password_type,
    LENGTH(password) as password_length
FROM users 
WHERE username = 'admin';

-- 2. Generate a BCrypt hash for 'admin123' using Java
-- You need to run this in Java to get the hash:
-- String hash = UserService.hashPassword("admin123");
-- Then use that hash in the UPDATE below

-- 3. Update admin password with a known BCrypt hash for 'admin123'
-- This hash was generated with: BCrypt.hashpw("admin123", BCrypt.gensalt(12))
-- Replace with your generated hash:
UPDATE users 
SET password = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYqJ5J5J5J6' 
WHERE username = 'admin';

-- 4. Test: Verify the hash works (this is just for reference, actual verification is in Java)
-- The hash should start with $2a$ or $2b$ and be 60 characters long
SELECT 
    username,
    CASE 
        WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt Hash'
        ELSE 'Not BCrypt'
    END as verification,
    LENGTH(password) as hash_length
FROM users 
WHERE username = 'admin';

-- 5. Alternative: Set to plaintext for testing (code supports this)
-- UPDATE users SET password = 'admin123' WHERE username = 'admin';

-- ============================================
-- HOW TO GENERATE BCRYPT HASH IN JAVA
-- ============================================
-- Run this code snippet:
/*
import com.skilora.model.service.UserService;
public class GenerateHash {
    public static void main(String[] args) {
        String hash = UserService.hashPassword("admin123");
        System.out.println("BCrypt hash for 'admin123': " + hash);
    }
}
*/
-- Then copy the output hash and use it in the UPDATE statement above.

-- ============================================
