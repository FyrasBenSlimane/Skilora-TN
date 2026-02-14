-- ============================================================
-- Fix Finance Tables - Drop and Recreate with Correct Schema
-- Run this script ONCE to fix existing databases
-- ============================================================

-- Drop old tables (reverse order to respect FK constraints)
DROP TABLE IF EXISTS payslips;
DROP TABLE IF EXISTS bonuses;
DROP TABLE IF EXISTS contracts;
DROP TABLE IF EXISTS employment_contracts;
DROP TABLE IF EXISTS bank_accounts;

-- Recreate with correct schema matching FinanceService SQL queries

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

-- Verify tables were created
SELECT 'contracts' AS table_name, COUNT(*) AS row_count FROM contracts
UNION ALL
SELECT 'payslips', COUNT(*) FROM payslips
UNION ALL
SELECT 'bank_accounts', COUNT(*) FROM bank_accounts
UNION ALL
SELECT 'bonuses', COUNT(*) FROM bonuses;
