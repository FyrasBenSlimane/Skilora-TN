-- =============================================
-- UNLOCK USER ACCOUNT - Quick Fix
-- =============================================
-- Use this to unlock the "user" account if it's locked

USE skilora;

-- Clear all failed login attempts for user "user"
DELETE FROM login_attempts WHERE username = 'user' AND success = FALSE;

-- Verify the account is unlocked
SELECT 'Account unlock status:' AS info;
SELECT 
    username,
    COUNT(*) as failed_attempts,
    MAX(attempted_at) as last_failed_attempt
FROM login_attempts 
WHERE username = 'user' AND success = FALSE
  AND attempted_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE)
GROUP BY username;

-- If the query above returns 0 rows, the account is unlocked.
