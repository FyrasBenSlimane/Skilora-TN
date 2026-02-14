-- =============================================
-- SKILORA FINANCE MODULE - DATABASE SCHEMA v3.1
-- Date: 2026-02-11
-- =============================================

USE skilora_db;

-- =============================================
-- TABLE: employees (Employés)
-- =============================================
CREATE TABLE IF NOT EXISTS employees (
    id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(20),
    position VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_first_name CHECK (first_name REGEXP '^[A-Za-zÀ-ÿ ]+$'),
    CONSTRAINT chk_last_name CHECK (last_name REGEXP '^[A-Za-zÀ-ÿ ]+$'),
    CONSTRAINT chk_email CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$')
);

-- =============================================
-- TABLE: contracts (Contrats)
-- =============================================
CREATE TABLE IF NOT EXISTS contracts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    company_id INT NOT NULL,
    type ENUM('PERMANENT', 'TEMPORARY', 'INTERNSHIP', 'FREELANCE') DEFAULT 'PERMANENT',
    position VARCHAR(150) NOT NULL,
    salary DECIMAL(10, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status ENUM('ACTIVE', 'ENDED', 'SUSPENDED', 'TERMINATED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_salary CHECK (salary >= 0),
    CONSTRAINT chk_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- =============================================
-- TABLE: bank_accounts (Comptes Bancaires)
-- =============================================
CREATE TABLE IF NOT EXISTS bank_accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    bank_name VARCHAR(150) NOT NULL,
    iban VARCHAR(34) NOT NULL,
    swift VARCHAR(11),
    currency VARCHAR(10) DEFAULT 'TND',
    is_primary BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_iban CHECK (LENGTH(iban) BETWEEN 15 AND 34),
    UNIQUE KEY unique_primary_per_user (user_id, is_primary)
);

-- =============================================
-- TABLE: bonuses (Primes)
-- =============================================
CREATE TABLE IF NOT EXISTS bonuses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(255),
    date_awarded DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_bonus_amount CHECK (amount > 0)
);

-- =============================================
-- TABLE: payslips (Bulletins de Paie) - CRÉATIF
-- =============================================
CREATE TABLE IF NOT EXISTS payslips (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    
    -- Revenus (CRÉATIF!)
    base_salary DECIMAL(10, 2) NOT NULL,
    overtime_hours DECIMAL(5, 2) DEFAULT 0,
    overtime_rate DECIMAL(10, 2) DEFAULT 0,
    overtime_total DECIMAL(10, 2) GENERATED ALWAYS AS (overtime_hours * overtime_rate) STORED,
    bonuses DECIMAL(10, 2) DEFAULT 0,
    gross_salary DECIMAL(10, 2) GENERATED ALWAYS AS (base_salary + (overtime_hours * overtime_rate) + bonuses) STORED,
    
    -- Déductions (CRÉATIF!)
    cnss_deduction DECIMAL(10, 2) GENERATED ALWAYS AS (gross_salary * 0.0918) STORED,
    irpp_tax DECIMAL(10, 2) GENERATED ALWAYS AS ((gross_salary - (gross_salary * 0.0918)) * 0.26) STORED,
    other_deductions DECIMAL(10, 2) DEFAULT 0,
    total_deductions DECIMAL(10, 2) GENERATED ALWAYS AS ((gross_salary * 0.0918) + ((gross_salary - (gross_salary * 0.0918)) * 0.26) + other_deductions) STORED,
    
    -- Net (CRÉATIF!)
    net_salary DECIMAL(10, 2) GENERATED ALWAYS AS (gross_salary - (gross_salary * 0.0918) - ((gross_salary - (gross_salary * 0.0918)) * 0.26) - other_deductions) STORED,
    
    status ENUM('DRAFT', 'PENDING', 'APPROVED', 'PAID') DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_year CHECK (year BETWEEN 2020 AND 2050),
    CONSTRAINT chk_base_salary CHECK (base_salary >= 0),
    UNIQUE KEY unique_payslip_per_month (user_id, month, year)
);

-- =============================================
-- INDEXES pour performance
-- =============================================
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_contracts_user ON contracts(user_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id);
CREATE INDEX idx_bonuses_user ON bonuses(user_id);
CREATE INDEX idx_payslips_user ON payslips(user_id);
CREATE INDEX idx_payslips_period ON payslips(year, month);

-- =============================================
-- DONNÉES D'EXEMPLE
-- =============================================

-- Employés
INSERT INTO employees (first_name, last_name, email, phone, position) VALUES
('Ahmed', 'Ben Ali', 'ahmed@skilora.tn', '+216 20123456', 'Developer'),
('Fatima', 'Mansouri', 'fatima@skilora.tn', '+216 20234567', 'Manager'),
('Mohamed', 'Trabelsi', 'mohamed@skilora.tn', '+216 20345678', 'Designer'),
('Leila', 'Kacem', 'leila@skilora.tn', '+216 20456789', 'Accountant'),
('Youssef', 'Hamdi', 'youssef@skilora.tn', '+216 20567890', 'HR Specialist')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Contrats
INSERT INTO contracts (user_id, company_id, type, position, salary, start_date, status) VALUES
(1, 1, 'PERMANENT', 'Senior Developer', 4500.00, '2023-01-15', 'ACTIVE'),
(2, 1, 'PERMANENT', 'Department Manager', 5500.00, '2022-06-01', 'ACTIVE'),
(3, 1, 'TEMPORARY', 'UI/UX Designer', 3200.00, '2024-01-01', 'ACTIVE'),
(4, 1, 'PERMANENT', 'Senior Accountant', 3800.00, '2023-03-15', 'ACTIVE'),
(5, 1, 'PERMANENT', 'HR Manager', 4200.00, '2023-05-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Comptes bancaires
INSERT INTO bank_accounts (user_id, bank_name, iban, swift, currency, is_primary, is_verified) VALUES
(1, 'Banque de Tunisie', 'TN5914207207100707129648', 'BTUISNTTXXX', 'TND', TRUE, TRUE),
(2, 'ATB', 'TN5903006013820000356752', 'ATBKTNTTXXX', 'TND', TRUE, TRUE),
(3, 'BIAT', 'TN5908903020000000356456', 'BIATTNTTXXX', 'TND', TRUE, FALSE),
(4, 'STB', 'TN5905603111522356789012', 'STBKTNTTXXX', 'TND', TRUE, TRUE),
(5, 'UIB', 'TN5902501234567890356789', 'UIBKTNTTXXX', 'TND', TRUE, TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Primes
INSERT INTO bonuses (user_id, amount, reason, date_awarded) VALUES
(1, 500.00, 'Excellent performance Q4 2024', '2024-12-31'),
(2, 800.00, 'Leadership award', '2024-12-31'),
(1, 300.00, 'Project completion bonus', '2024-11-15'),
(4, 400.00, 'Year-end bonus', '2024-12-31')
ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP;

-- Bulletins de paie
INSERT INTO payslips (user_id, month, year, base_salary, overtime_hours, overtime_rate, bonuses, other_deductions, status) VALUES
(1, 1, 2025, 4500.00, 10, 25.00, 0, 0, 'PAID'),
(2, 1, 2025, 5500.00, 0, 0, 0, 0, 'PAID'),
(3, 1, 2025, 3200.00, 5, 20.00, 0, 0, 'APPROVED'),
(1, 2, 2025, 4500.00, 8, 25.00, 300.00, 0, 'PENDING')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- =============================================
-- VUES UTILES
-- =============================================

-- Vue: Salaires nets moyens par position
CREATE OR REPLACE VIEW avg_salaries_by_position AS
SELECT 
    e.position,
    AVG(p.net_salary) as avg_net_salary,
    COUNT(DISTINCT e.id) as employee_count
FROM employees e
JOIN payslips p ON e.id = p.user_id
WHERE p.status = 'PAID'
GROUP BY e.position;

-- Vue: Rapport financier par employé
CREATE OR REPLACE VIEW employee_financial_summary AS
SELECT 
    e.id,
    CONCAT(e.first_name, ' ', e.last_name) as full_name,
    e.email,
    c.salary as contract_salary,
    COALESCE(SUM(b.amount), 0) as total_bonuses,
    COUNT(DISTINCT p.id) as payslips_count,
    AVG(p.net_salary) as avg_net_salary
FROM employees e
LEFT JOIN contracts c ON e.id = c.user_id AND c.status = 'ACTIVE'
LEFT JOIN bonuses b ON e.id = b.user_id
LEFT JOIN payslips p ON e.id = p.user_id AND p.status = 'PAID'
GROUP BY e.id, e.first_name, e.last_name, e.email, c.salary;

-- =============================================
-- PROCÉDURE: Calculer automatiquement les taxes
-- =============================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS calculate_payslip_taxes(
    IN p_base_salary DECIMAL(10,2),
    IN p_overtime_hours DECIMAL(5,2),
    IN p_overtime_rate DECIMAL(10,2),
    IN p_bonuses DECIMAL(10,2),
    OUT p_gross DECIMAL(10,2),
    OUT p_cnss DECIMAL(10,2),
    OUT p_irpp DECIMAL(10,2),
    OUT p_net DECIMAL(10,2)
)
BEGIN
    DECLARE v_overtime_total DECIMAL(10,2);
    
    SET v_overtime_total = p_overtime_hours * p_overtime_rate;
    SET p_gross = p_base_salary + v_overtime_total + p_bonuses;
    SET p_cnss = p_gross * 0.0918;
    SET p_irpp = (p_gross - p_cnss) * 0.26;
    SET p_net = p_gross - p_cnss - p_irpp;
END//
DELIMITER ;

-- =============================================
-- FIN DU SCRIPT
-- =============================================
SELECT 'Database schema created successfully!' as status;
