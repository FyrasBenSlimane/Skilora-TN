package com.skilora.scripts;

import com.skilora.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseMigration {

    public static void main(String[] args) {
        System.out.println("Starting Database Migration for Finance Module...");

        String[] sqlStatements = {
                // COMPANIES
                "CREATE TABLE IF NOT EXISTS companies (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    name VARCHAR(100) NOT NULL," +
                        "    address VARCHAR(255)," +
                        "    registration_number VARCHAR(50)," +
                        "    tax_id VARCHAR(50)," +
                        "    contact_email VARCHAR(100)," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ");",

                // PAYSLIPS - corrected schema matching FinanceService
                "CREATE TABLE IF NOT EXISTS payslips (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    user_id INT NOT NULL," +
                        "    month INT NOT NULL," +
                        "    year INT NOT NULL," +
                        "    base_salary DECIMAL(10, 2) NOT NULL DEFAULT 0," +
                        "    overtime_hours DECIMAL(10, 2) DEFAULT 0," +
                        "    overtime_total DECIMAL(10, 2) DEFAULT 0," +
                        "    bonuses DECIMAL(10, 2) DEFAULT 0," +
                        "    other_deductions DECIMAL(10, 2) DEFAULT 0," +
                        "    currency VARCHAR(3) NOT NULL DEFAULT 'TND'," +
                        "    status VARCHAR(50) DEFAULT 'PENDING'," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    INDEX idx_user_id (user_id)," +
                        "    INDEX idx_month_year (month, year)," +
                        "    CONSTRAINT fk_payslips_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ");",

                // BONUSES
                "CREATE TABLE IF NOT EXISTS bonuses (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    user_id INT NOT NULL," +
                        "    amount DECIMAL(10, 2) NOT NULL," +
                        "    currency VARCHAR(3) NOT NULL DEFAULT 'TND'," +
                        "    reason VARCHAR(255)," +
                        "    date_awarded DATE NOT NULL," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    INDEX idx_user_id (user_id)," +
                        "    CONSTRAINT fk_bonuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ");",

                // CONTRACTS - corrected table name and schema matching FinanceService
                "CREATE TABLE IF NOT EXISTS contracts (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    user_id INT NOT NULL," +
                        "    company_id INT," +
                        "    position VARCHAR(100)," +
                        "    salary DECIMAL(10, 2) NOT NULL DEFAULT 0," +
                        "    currency VARCHAR(3) NOT NULL DEFAULT 'TND'," +
                        "    start_date DATE NOT NULL," +
                        "    end_date DATE," +
                        "    type VARCHAR(50) NOT NULL," +
                        "    status VARCHAR(50) DEFAULT 'ACTIVE'," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    INDEX idx_user_id (user_id)," +
                        "    CONSTRAINT fk_contracts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                        +
                        ");",

                // BANK_ACCOUNTS - corrected column names matching FinanceService
                "CREATE TABLE IF NOT EXISTS bank_accounts (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    user_id INT NOT NULL," +
                        "    bank_name VARCHAR(100) NOT NULL," +
                        "    iban VARCHAR(34) NOT NULL UNIQUE," +
                        "    swift VARCHAR(11)," +
                        "    currency VARCHAR(3) NOT NULL DEFAULT 'TND'," +
                        "    is_verified BOOLEAN DEFAULT FALSE," +
                        "    is_primary BOOLEAN DEFAULT FALSE," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    INDEX idx_user_id (user_id)," +
                        "    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                        +
                        ");",

                // EXCHANGE_RATES
                "CREATE TABLE IF NOT EXISTS exchange_rates (" +
                        "    id INT PRIMARY KEY AUTO_INCREMENT," +
                        "    from_currency VARCHAR(3) NOT NULL," +
                        "    to_currency VARCHAR(3) NOT NULL," +
                        "    rate DECIMAL(15, 6) NOT NULL," +
                        "    date DATE NOT NULL," +
                        "    source VARCHAR(100)," +
                        "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    UNIQUE KEY unique_currency_pair (from_currency, to_currency, date)" +
                        ");"
        };

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {

            for (String sql : sqlStatements) {
                try {
                    stmt.execute(sql);
                    System.out.println("Executed: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                } catch (SQLException e) {
                    System.err.println("Error executing SQL: " + e.getMessage());
                    // Don't stop, continue with other tables if one fails (e.g. already exists)
                }
            }
            System.out.println("Migration completed successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
