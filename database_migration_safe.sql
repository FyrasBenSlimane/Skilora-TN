-- =============================================
-- SKILORA FINANCE MODULE - MIGRATION v3.1
-- Date: 2026-02-11
-- UTILISE LES TABLES EXISTANTES
-- =============================================

USE skilora_db;

-- =============================================
-- AJOUTER COLONNES MANQUANTES À user_info
-- (Si elles n'existent pas déjà)
-- =============================================

-- Vérifier et ajouter colonne 'position' si elle n'existe pas
SET @dbname = 'skilora_db';
SET @tablename = 'user_info';
SET @columnname = 'position';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD ", @columnname, " VARCHAR(100) DEFAULT NULL;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =============================================
-- CRÉER SEULEMENT LES TABLES FINANCE QUI N'EXISTENT PAS
-- =============================================

-- Table: contracts (Contrats) - SEULEMENT SI ELLE N'EXISTE PAS
CREATE TABLE IF NOT EXISTS contracts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    company_id INT NOT NULL DEFAULT 1,
    type ENUM('PERMANENT', 'TEMPORARY', 'INTERNSHIP', 'FREELANCE') DEFAULT 'PERMANENT',
    position VARCHAR(150) NOT NULL,
    salary DECIMAL(10, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status ENUM('ACTIVE', 'ENDED', 'SUSPENDED', 'TERMINATED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_salary CHECK (salary >= 0),
    CONSTRAINT chk_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Table: bank_accounts (Comptes Bancaires) - SEULEMENT SI ELLE N'EXISTE PAS
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
    
    CONSTRAINT chk_iban CHECK (LENGTH(iban) BETWEEN 15 AND 34)
);

-- Table: bonuses (Primes) - SEULEMENT SI ELLE N'EXISTE PAS
CREATE TABLE IF NOT EXISTS bonuses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(255),
    date_awarded DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_bonus_amount CHECK (amount > 0)
);

-- Table: payslips (Bulletins de Paie) - SEULEMENT SI ELLE N'EXISTE PAS
CREATE TABLE IF NOT EXISTS payslips (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    
    -- Revenus
    base_salary DECIMAL(10, 2) NOT NULL,
    overtime_hours DECIMAL(5, 2) DEFAULT 0,
    overtime_rate DECIMAL(10, 2) DEFAULT 0,
    overtime_total DECIMAL(10, 2) DEFAULT 0,
    bonuses DECIMAL(10, 2) DEFAULT 0,
    gross_salary DECIMAL(10, 2) DEFAULT 0,
    
    -- Déductions
    cnss_deduction DECIMAL(10, 2) DEFAULT 0,
    irpp_tax DECIMAL(10, 2) DEFAULT 0,
    other_deductions DECIMAL(10, 2) DEFAULT 0,
    total_deductions DECIMAL(10, 2) DEFAULT 0,
    
    -- Net
    net_salary DECIMAL(10, 2) DEFAULT 0,
    
    status ENUM('DRAFT', 'PENDING', 'APPROVED', 'PAID') DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_year CHECK (year BETWEEN 2020 AND 2050),
    CONSTRAINT chk_base_salary CHECK (base_salary >= 0),
    UNIQUE KEY unique_payslip_per_month (user_id, month, year)
);

-- =============================================
-- CRÉER DES TRIGGERS POUR CALCULS AUTOMATIQUES
-- (Puisque les colonnes calculées ne fonctionnent pas partout)
-- =============================================

DELIMITER //

-- Trigger BEFORE INSERT pour calculer automatiquement
DROP TRIGGER IF EXISTS payslip_calculate_before_insert//
CREATE TRIGGER payslip_calculate_before_insert
BEFORE INSERT ON payslips
FOR EACH ROW
BEGIN
    SET NEW.overtime_total = NEW.overtime_hours * NEW.overtime_rate;
    SET NEW.gross_salary = NEW.base_salary + NEW.overtime_total + NEW.bonuses;
    SET NEW.cnss_deduction = NEW.gross_salary * 0.0918;
    SET NEW.irpp_tax = (NEW.gross_salary - NEW.cnss_deduction) * 0.26;
    SET NEW.total_deductions = NEW.cnss_deduction + NEW.irpp_tax + NEW.other_deductions;
    SET NEW.net_salary = NEW.gross_salary - NEW.total_deductions;
END//

-- Trigger BEFORE UPDATE pour recalculer automatiquement
DROP TRIGGER IF EXISTS payslip_calculate_before_update//
CREATE TRIGGER payslip_calculate_before_update
BEFORE UPDATE ON payslips
FOR EACH ROW
BEGIN
    SET NEW.overtime_total = NEW.overtime_hours * NEW.overtime_rate;
    SET NEW.gross_salary = NEW.base_salary + NEW.overtime_total + NEW.bonuses;
    SET NEW.cnss_deduction = NEW.gross_salary * 0.0918;
    SET NEW.irpp_tax = (NEW.gross_salary - NEW.cnss_deduction) * 0.26;
    SET NEW.total_deductions = NEW.cnss_deduction + NEW.irpp_tax + NEW.other_deductions;
    SET NEW.net_salary = NEW.gross_salary - NEW.total_deductions;
END//

DELIMITER ;

-- =============================================
-- INDEXES pour performance (seulement si n'existent pas)
-- =============================================

-- Créer les indexes de manière sécurisée
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE table_schema = 'skilora_db' AND table_name = 'contracts' AND index_name = 'idx_contracts_user') > 0,
    "SELECT 1",
    "CREATE INDEX idx_contracts_user ON contracts(user_id)"
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE table_schema = 'skilora_db' AND table_name = 'bank_accounts' AND index_name = 'idx_bank_accounts_user') > 0,
    "SELECT 1",
    "CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id)"
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE table_schema = 'skilora_db' AND table_name = 'bonuses' AND index_name = 'idx_bonuses_user') > 0,
    "SELECT 1",
    "CREATE INDEX idx_bonuses_user ON bonuses(user_id)"
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE table_schema = 'skilora_db' AND table_name = 'payslips' AND index_name = 'idx_payslips_user') > 0,
    "SELECT 1",
    "CREATE INDEX idx_payslips_user ON payslips(user_id)"
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- FIN DU SCRIPT
-- =============================================
SELECT 'Finance module migration completed successfully!' as status;
SELECT 'IMPORTANT: Ce script N\'A PAS créé de nouvelles tables employees.' as note;
SELECT 'Il utilise la table user_info existante.' as info;
