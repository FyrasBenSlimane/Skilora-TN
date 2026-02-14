-- ============================================
-- FIX PASSWORDS - Run this after generating hashes
-- ============================================

-- Step 1: First, run GenerateBCryptHash.java to get the correct hashes
-- Step 2: Then update the passwords below with the generated hashes

-- For now, set to plaintext (code supports this for testing):
UPDATE users SET password = 'admin123' WHERE username = 'admin';
UPDATE users SET password = 'emp123' WHERE username = 'employer';

-- After running GenerateBCryptHash.java, you'll get BCrypt hashes like:
-- UPDATE users SET password = '$2a$12$...' WHERE username = 'admin';

-- ============================================
-- VERIFY PASSWORDS ARE SET
-- ============================================
SELECT username, 
       LEFT(password, 30) as password_preview,
       CASE 
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
         ELSE 'Plaintext'
       END as password_type
FROM users 
WHERE username IN ('admin', 'employer');

-- ============================================
