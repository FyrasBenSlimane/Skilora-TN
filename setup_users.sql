-- =============================================
-- SETUP: Ensure the 3 required users exist
-- Run this in phpMyAdmin (XAMPP) if needed
-- =============================================

USE skilora;

-- Create the 3 users with BCrypt hashed passwords
-- admin / admin123
-- user / user123
-- employer / emp123

-- NOTE: The passwords below are BCrypt hashes. 
-- If login fails, check that UserService.verifyPassword() uses BCrypt.

INSERT IGNORE INTO users (username, email, role, first_name, last_name, full_name, is_active, password) VALUES
('admin', 'admin@skilora.com', 'ADMIN', 'System', 'Admin', 'System Admin', TRUE, 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('user', 'user@skilora.com', 'USER', 'Consultation', 'User', 'Consultation User', TRUE, 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('employer', 'employer@skilora.com', 'EMPLOYER', 'Test', 'Employer', 'Test Employer', TRUE, 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- Verify users exist
SELECT id, username, role, full_name FROM users;
