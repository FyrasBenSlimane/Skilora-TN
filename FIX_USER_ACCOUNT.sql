-- =============================================
-- FIX USER ACCOUNT - Manual SQL Fix
-- =============================================
-- Use this if the automatic seeding didn't work

USE skilora;

-- 1. Check current state
SELECT 'Current user state:' AS info;
SELECT id, username, email, role, is_active, is_verified,
       LEFT(password, 30) as password_preview,
       CASE 
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
         ELSE 'Plaintext'
       END as password_type
FROM users 
WHERE username = 'user';

-- 2. Delete existing user if it has wrong password (optional - be careful!)
-- DELETE FROM users WHERE username = 'user';

-- 3. Create user with plaintext password (application will verify it)
-- The application supports plaintext passwords for legacy accounts
INSERT INTO users (username, email, password, role, is_verified, is_active)
VALUES ('user', 'user@skilora.com', 'user123', 'USER', TRUE, TRUE)
ON DUPLICATE KEY UPDATE 
    password = 'user123',
    email = 'user@skilora.com',
    role = 'USER',
    is_verified = TRUE,
    is_active = TRUE;

-- 4. Clear any lockout
DELETE FROM login_attempts WHERE username = 'user' AND success = FALSE;

-- 5. Verify
SELECT 'After fix:' AS info;
SELECT id, username, email, role, is_active, is_verified,
       CASE 
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
         ELSE 'Plaintext'
       END as password_type
FROM users 
WHERE username = 'user';
