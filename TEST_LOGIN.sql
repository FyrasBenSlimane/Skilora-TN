-- ============================================
-- TEST LOGIN - SQL COMMANDS
-- ============================================

-- 1. Check if admin user exists and see password hash
SELECT id, username, email, password, role, status, is_verified, is_active 
FROM users 
WHERE username = 'admin';

-- 2. Check if account is locked
SELECT COUNT(*) as failed_attempts, MAX(attempted_at) as last_attempt
FROM login_attempts 
WHERE username = 'admin' AND success = FALSE 
  AND attempted_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE);

-- 3. Clear lockout for admin (if locked)
DELETE FROM login_attempts WHERE username = 'admin' AND success = FALSE;

-- 4. QUICK FIX: Set admin password to plaintext 'admin123' (for testing)
-- The code supports plaintext passwords for legacy accounts
UPDATE users 
SET password = 'admin123' 
WHERE username = 'admin';

-- 5. Also set other test accounts
UPDATE users 
SET password = 'user123' 
WHERE username = 'user';

UPDATE users 
SET password = 'emp123' 
WHERE username = 'employer';

-- OR if you want to use BCrypt (more secure):
-- Run this Java code to generate the hash, then update:
-- String hash = UserService.hashPassword("admin123");
-- Then use that hash in the UPDATE statement below:
-- UPDATE users SET password = '<generated_hash>' WHERE username = 'admin';

-- 5. Verify all test accounts exist
SELECT username, email, role, 
       CASE 
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
         ELSE 'Plaintext'
       END as password_type
FROM users 
WHERE username IN ('admin', 'user', 'employer');

-- 6. Reset all lockouts
DELETE FROM login_attempts WHERE success = FALSE;

-- ============================================
