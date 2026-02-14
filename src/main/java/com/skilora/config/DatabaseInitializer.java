package com.skilora.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * DatabaseInitializer
 * Ensures all required tables exist on application startup.
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String SCRIPT_PATH = "skilora.sql";

    public static void initialize() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {

            boolean hasSkills = tableExists(stmt, "skills");
            boolean hasCompanies = tableExists(stmt, "companies");
            boolean hasJobOffers = tableExists(stmt, "job_offers");
            boolean hasApplications = tableExists(stmt, "applications");
            boolean hasPasswordResetTokens = tableExists(stmt, "password_reset_tokens");
            boolean hasLoginAttempts = tableExists(stmt, "login_attempts");

            // Check if job_offers has correct schema (company_id column)
            boolean jobOffersHasCorrectSchema = hasJobOffers && columnExists(stmt, "job_offers", "company_id");

            // If job_offers exists but has wrong schema, drop it
            if (hasJobOffers && !jobOffersHasCorrectSchema) {
                logger.info("job_offers table has incorrect schema. Recreating...");
                stmt.execute("DROP TABLE IF EXISTS job_offers");
                hasJobOffers = false;
            }

            // Create companies + job_offers if missing
            if (!hasCompanies || !hasJobOffers) {
                logger.info("Creating missing core tables...");
                createMissingTables(stmt, hasCompanies, hasJobOffers);
            }

            // Create applications table if missing
            if (!hasApplications) {
                createApplicationsTable(stmt);
            }

            // Create password_reset_tokens table if missing
            if (!hasPasswordResetTokens) {
                createPasswordResetTokensTable(stmt);
            }

            // Create login_attempts table if missing
            if (!hasLoginAttempts) {
                createLoginAttemptsTable(stmt);
            }

            // Create user_preferences table if missing
            if (!tableExists(stmt, "user_preferences")) {
                createUserPreferencesTable(stmt);
            }

            // Add missing columns to users table
            ensureUsersTableColumns(stmt);

            // Create performance indexes on foreign keys
            createIndexes(stmt);

            if (hasSkills && tableExists(stmt, "payslips")) {
                // Validate finance table schemas - fix if columns are wrong
                boolean needsFinanceRebuild = false;

                // Check payslips has correct columns (base_salary, not gross_salary)
                if (!columnExists(stmt, "payslips", "base_salary")) {
                    logger.info("payslips table has outdated schema. Rebuilding...");
                    stmt.execute("DROP TABLE IF EXISTS payslips");
                    needsFinanceRebuild = true;
                }

                // Check contracts table exists (not employment_contracts)
                if (!tableExists(stmt, "contracts")) {
                    logger.info("contracts table missing (may be named employment_contracts). Creating...");
                    stmt.execute("DROP TABLE IF EXISTS employment_contracts");
                    needsFinanceRebuild = true;
                }

                // Check bank_accounts has correct columns (swift, not swift_code)
                if (tableExists(stmt, "bank_accounts") && !columnExists(stmt, "bank_accounts", "swift")) {
                    logger.info("bank_accounts table has outdated schema. Rebuilding...");
                    stmt.execute("DROP TABLE IF EXISTS bank_accounts");
                    needsFinanceRebuild = true;
                }

                if (needsFinanceRebuild) {
                    createFinanceTables(stmt);
                } else {
                    logger.info("Database schema is up-to-date.");
                }
                return;
            }

            // Create Finance Module Tables
            createFinanceTables(stmt);

            // Run full initialization for brand new database
            logger.info("Database missing core tables. Running initialization script...");
            runFullScript(stmt);

        } catch (SQLException e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
        }
    }

    private static boolean tableExists(Statement stmt, String tableName) {
        try {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean columnExists(Statement stmt, String tableName, String columnName) {
        try {
            stmt.executeQuery("SELECT " + columnName + " FROM " + tableName + " LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void createMissingTables(Statement stmt, boolean hasCompanies, boolean hasJobOffers) {
        try {
            if (!hasCompanies) {
                String createCompanies = """
                        CREATE TABLE IF NOT EXISTS companies (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            owner_id INT,
                            name VARCHAR(100) NOT NULL,
                            country VARCHAR(50),
                            industry VARCHAR(100),
                            website VARCHAR(255),
                            logo_url TEXT,
                            is_verified BOOLEAN DEFAULT FALSE,
                            size VARCHAR(50)
                        )
                        """;
                stmt.execute(createCompanies);
                logger.info("Created 'companies' table.");
            }

            String createJobOffers = """
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
                    )
                    """;
            stmt.execute(createJobOffers);
            if (!hasJobOffers) {
                logger.info("Created 'job_offers' table.");
            }

        } catch (SQLException e) {
            logger.error("Error creating core tables: {}", e.getMessage(), e);
        }
    }

    private static void createApplicationsTable(Statement stmt) {
        try {
            String sql = """
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
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'applications' table.");
        } catch (SQLException e) {
            logger.error("Error creating applications table: {}", e.getMessage(), e);
        }
    }

    private static void createPasswordResetTokensTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS password_reset_tokens (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        otp_code VARCHAR(6) NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        expires_at DATETIME NOT NULL,
                        used BOOLEAN DEFAULT FALSE,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'password_reset_tokens' table.");
        } catch (SQLException e) {
            logger.error("Error creating password_reset_tokens table: {}", e.getMessage(), e);
        }
    }

    private static void createLoginAttemptsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS login_attempts (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        success BOOLEAN DEFAULT FALSE,
                        ip_address VARCHAR(45)
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'login_attempts' table.");
        } catch (SQLException e) {
            logger.error("Error creating login_attempts table: {}", e.getMessage(), e);
        }
    }

    private static void createUserPreferencesTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        pref_key VARCHAR(100) NOT NULL,
                        pref_value VARCHAR(500),
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_user_pref (user_id, pref_key),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'user_preferences' table.");
        } catch (SQLException e) {
            logger.error("Error creating user_preferences table: {}", e.getMessage(), e);
        }
    }

    private static void ensureUsersTableColumns(Statement stmt) {
        // Add email column if missing
        if (!columnExists(stmt, "users", "email")) {
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email VARCHAR(100) AFTER username");
                logger.info("Added 'email' column to users table.");
            } catch (SQLException e) {
                logger.debug("Could not add email column: {}", e.getMessage());
            }
        }
        // Add is_verified column if missing
        if (!columnExists(stmt, "users", "is_verified")) {
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE");
                logger.info("Added 'is_verified' column to users table.");
            } catch (SQLException e) {
                logger.debug("Could not add is_verified column: {}", e.getMessage());
            }
        }
        // Add is_active column if missing
        if (!columnExists(stmt, "users", "is_active")) {
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
                logger.info("Added 'is_active' column to users table.");
            } catch (SQLException e) {
                logger.debug("Could not add is_active column: {}", e.getMessage());
            }
        }
    }

    private static void runFullScript(Statement stmt) {
        StringBuilder script = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(SCRIPT_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                script.append(line).append("\n");
            }
        } catch (Exception ex) {
            logger.warn("Could not read {}: {}", SCRIPT_PATH, ex.getMessage());
            return;
        }

        String[] statements = script.toString().split(";");
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty())
                continue;
            try {
                stmt.execute(trimmed);
            } catch (SQLException ex) {
                logger.debug("Init statement skipped: {}", ex.getMessage());
            }
        }

        logger.info("Database initialized successfully from script.");
    }

    /**
     * Create indexes on foreign keys and frequently queried columns
     * for faster JOINs and lookups. Uses CREATE INDEX IF NOT EXISTS
     * equivalent (try/catch for duplicate index errors).
     */
    private static void createIndexes(Statement stmt) {
        String[] indexes = {
                "CREATE INDEX idx_profiles_user_id ON profiles(user_id)",
                "CREATE INDEX idx_skills_profile_id ON skills(profile_id)",
                "CREATE INDEX idx_experiences_profile_id ON experiences(profile_id)",
                "CREATE INDEX idx_job_offers_company_id ON job_offers(company_id)",
                "CREATE INDEX idx_job_offers_status ON job_offers(status)",
                "CREATE INDEX idx_applications_job_offer_id ON applications(job_offer_id)",
                "CREATE INDEX idx_applications_candidate_id ON applications(candidate_profile_id)",
                "CREATE INDEX idx_applications_status ON applications(status)",
                "CREATE INDEX idx_login_attempts_username ON login_attempts(username, attempted_at)",
                "CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id, used)",
                "CREATE INDEX idx_companies_owner ON companies(owner_id)",
                "CREATE INDEX idx_users_email ON users(email)"
        };

        int created = 0;
        for (String sql : indexes) {
            try {
                stmt.execute(sql);
                created++;
            } catch (SQLException e) {
                // Index likely already exists - skip silently
            }
        }
        if (created > 0) {
            logger.info("Created {} database indexes for performance.", created);
        }
    }

    private static void createFinanceTables(Statement stmt) {
        try {
            // COMPANIES (Enhanced for Finance)
            if (!tableExists(stmt, "companies")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS companies (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL,
                            address VARCHAR(255),
                            registration_number VARCHAR(50),
                            tax_id VARCHAR(50),
                            contact_email VARCHAR(100),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'companies' table.");
            }

            // CONTRACTS (table name = contracts, columns match FinanceService queries)
            String contractsSql = """
                    CREATE TABLE IF NOT EXISTS contracts (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        company_id INT NOT NULL,
                        type VARCHAR(50) NOT NULL,
                        position VARCHAR(100),
                        salary DECIMAL(10, 2) NOT NULL,
                        start_date DATE NOT NULL,
                        end_date DATE,
                        status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_contracts_user (user_id),
                        CONSTRAINT fk_contracts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(contractsSql);

            // PAYSLIPS (columns match FinanceService: base_salary, overtime_hours,
            // overtime_total, bonuses, other_deductions)
            String payslipsSql = """
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
                        pdf_url VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_payslips_user (user_id),
                        INDEX idx_payslips_period (month, year),
                        CONSTRAINT fk_payslips_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(payslipsSql);

            // BONUSES
            String bonusesSql = """
                    CREATE TABLE IF NOT EXISTS bonuses (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        amount DECIMAL(10, 2) NOT NULL,
                        reason VARCHAR(255),
                        date_awarded DATE NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_bonuses_user (user_id),
                        CONSTRAINT fk_bonuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(bonusesSql);

            // BANK ACCOUNTS (columns match FinanceService: swift, is_primary, is_verified)
            String bankSql = """
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        bank_name VARCHAR(100) NOT NULL,
                        iban VARCHAR(50),
                        swift VARCHAR(20),
                        currency VARCHAR(3) DEFAULT 'TND',
                        is_primary BOOLEAN DEFAULT FALSE,
                        is_verified BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_bank_user (user_id),
                        CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(bankSql);

            // EXCHANGE RATES
            String ratesSql = """
                    CREATE TABLE IF NOT EXISTS exchange_rates (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        from_currency VARCHAR(3) NOT NULL,
                        to_currency VARCHAR(3) NOT NULL,
                        rate DECIMAL(15, 6) NOT NULL,
                        date DATE NOT NULL,
                        source VARCHAR(100),
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_currency_pair (from_currency, to_currency, date)
                    )
                    """;
            stmt.execute(ratesSql);

            logger.info("Finance module tables initialized.");

        } catch (SQLException e) {
            logger.error("Error creating Finance tables: {}", e.getMessage(), e);
        }
    }
}
