-- =============================================
-- CREATE DEFAULT USER ACCOUNT
-- =============================================
-- This script creates the default user account if it doesn't exist
-- Username: user
-- Password: user123 (will be hashed by the application)

USE skilora;

-- Check if user exists
SELECT 'Checking if user exists...' AS status;
SELECT id, username, email, role, is_active, is_verified,
       CASE 
         WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt'
         ELSE 'Plaintext'
       END as password_type
FROM users 
WHERE username = 'user';

-- If user doesn't exist, you need to run the Java application
-- which will automatically create it with BCrypt hashed password.
-- 
-- OR manually create with plaintext (application supports both):
-- INSERT INTO users (username, email, password, role, is_verified, is_active)
-- VALUES ('user', 'user@skilora.com', 'user123', 'USER', TRUE, TRUE);
--
-- Note: The application will hash the password automatically when using UserService.create()
-- For manual SQL insertion, you can use plaintext and the app will verify it,
-- but it's better to let the app create it with BCrypt.

-- To verify the user can login, check:
-- 1. User exists: SELECT * FROM users WHERE username = 'user';
-- 2. User is active: is_active = TRUE
-- 3. Password is set: password IS NOT NULL
-- 4. No lockout: Check login_attempts table

-- Clear any lockout for user
DELETE FROM login_attempts WHERE username = 'user' AND success = FALSE;
