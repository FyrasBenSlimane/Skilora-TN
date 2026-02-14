-- =============================================
-- SKILORA COMPLETE DATABASE SCHEMA
-- Merges JAVAFX11 (Recruitment) and SkiloraFinance (Finance)
-- =============================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS & AUTH
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(255), -- Used by JAVAFX11
    password_hash VARCHAR(255), -- Used by Finance (Backup)
    role VARCHAR(20) DEFAULT 'USER',
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    full_name VARCHAR(100),
    photo_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. RECRUITMENT MODULE (JAVAFX11) TABLES

CREATE TABLE IF NOT EXISTS profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    photo_url TEXT,
    cv_url TEXT,
    location VARCHAR(255),
    birth_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id)
);

CREATE TABLE IF NOT EXISTS skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    skill_name VARCHAR(100),
    proficiency_level VARCHAR(50),
    years_experience INT,
    verified TINYINT DEFAULT 0,
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experiences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    company VARCHAR(100),
    position VARCHAR(100),
    start_date DATE,
    end_date DATE,
    description TEXT,
    current_job TINYINT DEFAULT 0,
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

-- Merged Companies Table
CREATE TABLE IF NOT EXISTS companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    industry VARCHAR(100),
    website VARCHAR(255),
    logo_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    size VARCHAR(50),
    address VARCHAR(255),
    registration_number VARCHAR(50),
    tax_id VARCHAR(50),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS job_offers (
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
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_offer_id INT NOT NULL,
    candidate_profile_id INT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    applied_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    cover_letter TEXT,
    custom_cv_url TEXT,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS biometric_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    face_encoding BLOB,
    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS login_attempts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    pref_key VARCHAR(100) NOT NULL,
    pref_value VARCHAR(500),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_pref (user_id, pref_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. FINANCE MODULE (SkiloraFinance) TABLES

CREATE TABLE IF NOT EXISTS bank_accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    iban VARCHAR(34) NOT NULL UNIQUE,
    swift VARCHAR(11),
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    is_verified BOOLEAN DEFAULT FALSE,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bank_user_id (user_id),
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payslips (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    base_salary DECIMAL(10, 2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10, 2) DEFAULT 0,
    overtime_total DECIMAL(10, 2) DEFAULT 0,
    bonuses DECIMAL(10, 2) DEFAULT 0,
    other_deductions DECIMAL(10, 2) DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payslip_user_id (user_id),
    INDEX idx_payslip_month_year (month, year),
    CONSTRAINT fk_payslips_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS contracts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    company_id INT,
    position VARCHAR(100),
    salary DECIMAL(10, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    start_date DATE NOT NULL,
    end_date DATE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contract_user_id (user_id),
    CONSTRAINT fk_contracts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bonuses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    reason VARCHAR(255),
    date_awarded DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bonus_user_id (user_id),
    CONSTRAINT fk_bonuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deductions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10,2),
    reason VARCHAR(255),
    date_applied DATE,
    CONSTRAINT fk_deductions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id INT PRIMARY KEY AUTO_INCREMENT,
    currency_pair VARCHAR(10) UNIQUE, -- e.g. 'EUR/TND'
    rate DECIMAL(10,4),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tax_parameters (
    id INT PRIMARY KEY AUTO_INCREMENT,
    country VARCHAR(50) DEFAULT 'Tunisia',
    tax_bracket_min DECIMAL(10,2),
    tax_bracket_max DECIMAL(10,2),
    rate DECIMAL(5,2), -- Percentage
    description VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS financial_reports (
    id INT PRIMARY KEY AUTO_INCREMENT,
    report_type VARCHAR(50),
    period_start DATE,
    period_end DATE,
    content_json TEXT, -- Aggregated data
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    action VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. SEED DATA (Insert only if not exists)

INSERT IGNORE INTO users (id, username, email, role, first_name, last_name, full_name, is_active, password) VALUES 
(1, 'admin', 'admin@skilora.com', 'ADMIN', 'System', 'Admin', 'System Admin', TRUE, '$2a$10$YourHashedPasswordHere'),
(2, 'john.doe', 'john@skilora.com', 'EMPLOYEE', 'John', 'Doe', 'John Doe', TRUE, '$2a$10$YourHashedPasswordHere');

INSERT IGNORE INTO companies (id, name, country, industry, is_verified) VALUES
(1, 'Skilora Tunisia', 'Tunisia', 'Technology', TRUE);

INSERT IGNORE INTO exchange_rates (currency_pair, rate) VALUES
('EUR/TND', 3.4000),
('USD/TND', 3.1500);

SET FOREIGN_KEY_CHECKS = 1;
