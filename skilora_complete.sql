-- =====================================================
-- SKILORA TUNISIA - DATABASE SCHEMA COMPLETE
-- Finance & HRM Management System
-- Version: 3.0.0
-- Updated: February 9, 2026
-- =====================================================

-- 1. DATABASE CREATION
DROP DATABASE IF EXISTS skilora;
CREATE DATABASE skilora CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE skilora;

-- =====================================================
-- SECTION 1: CORE AUTHENTICATION & USER TABLES
-- =====================================================

-- Users (Authentication & Core Identity)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    photo_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Profiles (Extended User Details)
CREATE TABLE profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    photo_url TEXT,
    cv_url TEXT,
    location VARCHAR(255),
    birth_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id),
    INDEX idx_user_id (user_id)
);

-- Biometric Data
CREATE TABLE biometric_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    face_encoding BLOB,
    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);

-- =====================================================
-- SECTION 2: SECURITY & AUTHENTICATION TABLES
-- =====================================================

-- Password Reset Tokens
CREATE TABLE password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_otp (otp_code)
);

-- Login Attempts (Rate Limiting)
CREATE TABLE login_attempts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    INDEX idx_login_attempts_username (username, attempted_at)
);

-- =====================================================
-- SECTION 3: PROFESSIONAL PROFILE TABLES
-- =====================================================

-- Skills
CREATE TABLE skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    skill_name VARCHAR(100),
    proficiency_level VARCHAR(50),
    years_experience INT,
    verified TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
    INDEX idx_profile_id (profile_id)
);

-- Experiences
CREATE TABLE experiences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    company VARCHAR(100),
    position VARCHAR(100),
    start_date DATE,
    end_date DATE,
    description TEXT,
    current_job TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
    INDEX idx_profile_id (profile_id)
);

-- =====================================================
-- SECTION 4: COMPANY & JOB OFFER TABLES
-- =====================================================

-- Companies
CREATE TABLE companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    industry VARCHAR(100),
    website VARCHAR(255),
    logo_url TEXT,
    is_verified TINYINT DEFAULT 0,
    size VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_owner_id (owner_id),
    INDEX idx_name (name)
);

-- Job Offers
CREATE TABLE job_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    requirements TEXT,
    min_salary DECIMAL(10,2),
    max_salary DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'EUR',
    location VARCHAR(100),
    work_type VARCHAR(50),
    posted_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    deadline DATE,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_company_id (company_id),
    INDEX idx_status (status),
    INDEX idx_posted_date (posted_date)
);

-- Applications
CREATE TABLE applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_offer_id INT NOT NULL,
    candidate_profile_id INT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    applied_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    cover_letter TEXT,
    custom_cv_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
    INDEX idx_job_offer_id (job_offer_id),
    INDEX idx_candidate_id (candidate_profile_id),
    INDEX idx_status (status)
);

-- =====================================================
-- SECTION 5: FINANCE & REMUNERATION MANAGEMENT TABLES
-- =====================================================

-- Employment Contracts
CREATE TABLE employment_contracts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    company_id INT NOT NULL,
    base_salary DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    start_date DATE NOT NULL,
    end_date DATE,
    type VARCHAR(50) NOT NULL COMMENT 'CDI, CDD, Freelance',
    is_signed BOOLEAN DEFAULT FALSE,
    contract_document_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_company_id (company_id),
    INDEX idx_is_signed (is_signed)
);

-- Bank Accounts
CREATE TABLE bank_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    iban VARCHAR(34) NOT NULL UNIQUE,
    swift_code VARCHAR(11),
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    is_verified BOOLEAN DEFAULT FALSE,
    is_primary_account BOOLEAN DEFAULT FALSE,
    account_holder_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_iban (iban)
);

-- Payslips
CREATE TABLE payslips (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    gross_salary DECIMAL(10, 2) NOT NULL,
    net_salary DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    deductions_amount VARCHAR(500) COMMENT 'JSON format for deductions breakdown',
    bonuses_amount VARCHAR(500) COMMENT 'JSON format for bonuses breakdown',
    pdf_url VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, GENERATED, PAID',
    generated_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_month_year (month, year),
    UNIQUE KEY unique_payslip (user_id, month, year)
);

-- Bonuses
CREATE TABLE bonuses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    reason VARCHAR(255),
    date_awarded DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_date_awarded (date_awarded)
);

-- Exchange Rates
CREATE TABLE exchange_rates (
    id INT PRIMARY KEY AUTO_INCREMENT,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(15, 6) NOT NULL,
    date DATE NOT NULL,
    source VARCHAR(100) COMMENT 'API source like ECB, CBT, etc',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_currency_pair (from_currency, to_currency, date),
    INDEX idx_currencies (from_currency, to_currency),
    INDEX idx_date (date)
);

-- =====================================================
-- SECTION 6: SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Sample Users
INSERT INTO users (username, email, password, role, full_name, is_verified, is_active) VALUES
('admin', 'admin@skilora.tn', 'hashed_password_here', 'ADMIN', 'Administrator', TRUE, TRUE),
('john_doe', 'john.doe@example.com', 'hashed_password_here', 'USER', 'John Doe', TRUE, TRUE),
('jane_smith', 'jane.smith@example.com', 'hashed_password_here', 'USER', 'Jane Smith', TRUE, TRUE),
('company_recruiter', 'recruiter@company.com', 'hashed_password_here', 'RECRUITER', 'Company Recruiter', TRUE, TRUE);

-- Sample Companies
INSERT INTO companies (owner_id, name, country, industry, website, is_verified, size) VALUES
(4, 'Tech Solutions Inc', 'Tunisia', 'Technology', 'https://techsolutions.tn', 1, 'LARGE'),
(4, 'Digital Agency Pro', 'Tunisia', 'Digital Marketing', 'https://digitalagency.tn', 1, 'MEDIUM');

-- Sample Exchange Rates
INSERT INTO exchange_rates (from_currency, to_currency, rate, date, source, last_updated)
VALUES 
('TND', 'EUR', 0.31, CURDATE(), 'Central Bank of Tunisia', NOW()),
('TND', 'USD', 0.32, CURDATE(), 'Central Bank of Tunisia', NOW()),
('EUR', 'USD', 1.05, CURDATE(), 'Central Bank of Tunisia', NOW()),
('TND', 'GBP', 0.26, CURDATE(), 'Central Bank of Tunisia', NOW());

-- =====================================================
-- SECTION 7: VIEWS (Optional - for reporting)
-- =====================================================

-- View: Employee Financial Summary
CREATE VIEW employee_financial_summary AS
SELECT 
    u.id,
    u.username,
    u.full_name,
    ec.base_salary,
    ec.currency,
    COALESCE(SUM(b.amount), 0) as total_bonuses,
    COUNT(DISTINCT p.id) as payslip_count
FROM users u
LEFT JOIN employment_contracts ec ON u.id = ec.user_id
LEFT JOIN bonuses b ON u.id = b.user_id
LEFT JOIN payslips p ON u.id = p.user_id
GROUP BY u.id, u.username, u.full_name, ec.base_salary, ec.currency;

-- =====================================================
-- SECTION 8: DOCUMENTATION & METADATA
-- =====================================================

/*
DATABASE INFORMATION:
======================
Database Name: skilora
Purpose: Skilora Tunisia - Finance & HRM Management System
Version: 3.0.0
Last Updated: February 9, 2026
Character Set: UTF8MB4
Collation: utf8mb4_unicode_ci

TABLE SUMMARY:
==============
AUTHENTICATION & USER MANAGEMENT:
- users: Core user accounts and authentication
- profiles: Extended user profile information
- biometric_data: Biometric authentication data
- password_reset_tokens: OTP and reset token management
- login_attempts: Login attempt tracking for security

PROFESSIONAL & RECRUITMENT:
- companies: Company information
- job_offers: Job postings
- applications: Job applications
- skills: User skill profiles
- experiences: Work experience records

FINANCE & REMUNERATION:
- employment_contracts: Employment contract terms
- bank_accounts: Employee banking details
- payslips: Monthly salary records
- bonuses: Bonus payment records
- exchange_rates: Currency conversion rates

KEY FEATURES:
=============
✓ Comprehensive user authentication system
✓ Multi-currency support (primary: TND)
✓ Audit trails with created_at and updated_at timestamps
✓ Foreign key relationships for data integrity
✓ JSON storage for complex financial data
✓ Performance indexes on frequently queried columns
✓ Unique constraints for critical data (IBAN, username)
✓ Reporting views for financial analysis

SECURITY CONSIDERATIONS:
========================
✓ Passwords should be hashed with BCrypt
✓ IBAN should be encrypted in production
✓ Rate limiting via login_attempts table
✓ OTP tokens with expiration
✓ User verification status tracking

CONNECTION DETAILS (Default):
=============================
Host: localhost
Port: 3306
Database: skilora
User: root
Password: (set as needed)
SSL: disabled (enable in production)
Server Timezone: UTC
Charset: utf8mb4

DATABASE DRIVER:
================
MySQL 8.0+ with JDBC connector (mysql-connector-java)

USAGE EXAMPLES:
===============
-- Create a new user
INSERT INTO users (username, email, password, role, full_name)
VALUES ('newuser', 'newuser@example.com', 'hashed_pass', 'USER', 'New User');

-- Add bank account
INSERT INTO bank_accounts (user_id, bank_name, iban, swift_code, currency)
VALUES (1, 'Banque de Tunisie', 'TN59090020000123456789', 'BKBKTNTX', 'TND');

-- Record payslip
INSERT INTO payslips (user_id, month, year, gross_salary, net_salary, currency, status)
VALUES (1, 2, 2026, 2000.00, 1700.00, 'TND', 'GENERATED');

-- Add bonus
INSERT INTO bonuses (user_id, amount, currency, reason, date_awarded)
VALUES (1, 500.00, 'TND', 'Performance Bonus', CURDATE());

*/

-- =====================================================
-- END OF COMPLETE DATABASE SCHEMA
-- =====================================================
