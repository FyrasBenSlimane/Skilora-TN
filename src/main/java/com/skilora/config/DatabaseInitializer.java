package com.skilora.config;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
            // Older DBs may have applications without custom_cv_url (INSERT then fails)
            ensureApplicationsTableColumns(stmt);
            // Legacy schemas: id NOT NULL without AUTO_INCREMENT → "Field 'id' doesn't have a default value"
            ensureLegacyIdsAutoIncrement(conn);

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

            // Create interview_candidates table if missing (for accepted candidates eligible for interviews)
            if (!tableExists(stmt, "interview_candidates")) {
                createInterviewCandidatesTable(stmt);
            }
            
            // Create interviews table if missing (for scheduled interviews)
            if (!tableExists(stmt, "interviews")) {
                createInterviewsTable(stmt);
            }
            // Legacy DBs: table existed before interview_type / notes / status columns were added
            ensureInterviewsTableColumns(stmt);

            // Migrate existing data: populate interview_candidates with ACCEPTED applications
            migrateInterviewCandidates(stmt);

            // Add missing columns to users table
            ensureUsersTableColumns(stmt);

            // Create performance indexes on foreign keys
            createIndexes(stmt);

            // Create AI recruitment tables and constraints
            createRecruitmentAiTables(stmt);
            ensureRecruitmentConstraints(stmt);
            backfillJobOfferSkills(stmt);

            // Seed data for demo/testing scenarios
            seedRecruitmentSampleData(stmt);

                // Ensure test users exist with correct roles
                ensureTestUsers(stmt);

                // Fix all application relationships to ensure employers can see all applications
                fixAllApplicationRelationships(stmt);

                // Tables may be created above; re-run id fixes before early exit
                ensureLegacyIdsAutoIncrement(conn);

                if (hasSkills) {
                    logger.info("Database schema is up-to-date.");
                    return;
                }

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

    /**
     * Align legacy {@code applications} tables with code that INSERTs {@code custom_cv_url}.
     */
    private static void ensureApplicationsTableColumns(Statement stmt) {
        if (!tableExists(stmt, "applications")) {
            return;
        }
        try {
            if (!columnExists(stmt, "applications", "custom_cv_url")) {
                stmt.execute("ALTER TABLE applications ADD COLUMN custom_cv_url TEXT NULL");
                logger.info("Added column applications.custom_cv_url (migration).");
            }
        } catch (SQLException e) {
            logger.warn("Could not add applications.custom_cv_url: {}", e.getMessage());
        }
    }

    /**
     * Ensures {@code id} is AUTO_INCREMENT on core tables (fixes MySQL
     * "Field 'id' doesn't have a default value" on INSERT without id).
     * Uses a fresh {@link Statement} per query to avoid driver/statement state issues,
     * {@code SHOW CREATE TABLE} for detection (more reliable than INFORMATION_SCHEMA.EXTRA),
     * and retries with {@code ADD PRIMARY KEY (id)} when MODIFY alone fails.
     */
    private static void ensureLegacyIdsAutoIncrement(Connection conn) {
        String[] tables = {
                "applications", "profiles", "skills", "experiences",
                "job_offer_skills", "interview_candidates", "interviews",
                "companies", "job_offers"
        };
        for (String t : tables) {
            ensurePrimaryKeyAutoIncrement(conn, t);
        }
    }

    private static boolean tableExists(Connection conn, String tableName) {
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("SELECT 1 FROM `" + tableName + "` LIMIT 1")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean columnExistsOnConnection(Connection conn, String tableName, String columnName) {
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT `" + columnName + "` FROM `" + tableName + "` LIMIT 1")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean ddlLineForIdHasAutoIncrement(String createTableDdl) {
        if (createTableDdl == null) {
            return false;
        }
        for (String raw : createTableDdl.split("\n")) {
            String line = raw.trim().replace("\r", "").toLowerCase().replace("`", "");
            if (line.startsWith("id ") || line.startsWith("id\t")) {
                return line.contains("auto_increment");
            }
        }
        return false;
    }

    private static String readIdDataType(Connection conn, String tableName) {
        String dataType = "INT";
        String schema;
        try {
            schema = conn.getCatalog();
        } catch (SQLException e) {
            return dataType;
        }
        if (schema == null || schema.isBlank()) {
            return dataType;
        }
        String escSchema = schema.replace("'", "''");
        String safeTable = tableName.replace("'", "");
        String sql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '"
                + escSchema + "' AND TABLE_NAME = '" + safeTable + "' AND COLUMN_NAME = 'id'";
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                String dt = rs.getString(1);
                if (dt != null && dt.equalsIgnoreCase("bigint")) {
                    dataType = "BIGINT";
                }
            }
        } catch (SQLException ignored) {
        }
        return dataType;
    }

    /**
     * Next AUTO_INCREMENT value: {@code MAX(id)+1}, minimum 1. Avoids #1062 when enabling AUTO_INCREMENT
     * if the engine would otherwise start resequencing from 1 while id=1 already exists.
     */
    private static long readNextAutoIncrementValue(Connection conn, String tableName) {
        String safe = tableName.replace("`", "").replace("'", "");
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM `" + safe + "`")) {
            if (rs.next()) {
                return Math.max(1L, rs.getLong(1));
            }
        } catch (SQLException e) {
            logger.debug("readNextAutoIncrementValue {}: {}", tableName, e.getMessage());
        }
        return 1L;
    }

    private static boolean hasDuplicatePrimaryKeyOnId(Connection conn, String tableName) throws SQLException {
        String safe = tableName.replace("`", "").replace("'", "");
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT id FROM `" + safe + "` GROUP BY id HAVING COUNT(*) > 1 LIMIT 1")) {
            return rs.next();
        }
    }

    /**
     * Rows with {@code id <= 0} break {@code ALTER … AUTO_INCREMENT} (#1062 duplicate '1'): MySQL may try
     * to assign 1 while a row already has id 1. Renumber those rows to unused positive ids and update
     * {@code interview_candidates} / {@code interviews}.
     */
    private static void fixApplicationsNonPositivePrimaryKeys(Connection conn) {
        try (Statement enable = conn.createStatement()) {
            enable.execute("SET FOREIGN_KEY_CHECKS = 0");
        } catch (SQLException e) {
            logger.warn("SET FOREIGN_KEY_CHECKS=0 before applications id fix: {}", e.getMessage());
        }
        try {
            while (true) {
                int badId;
                boolean haveNonPositive;
                try (Statement s = conn.createStatement();
                        ResultSet rs = s.executeQuery("SELECT MIN(id) AS m FROM applications WHERE id <= 0")) {
                    haveNonPositive = rs.next() && rs.getObject("m") != null;
                    badId = haveNonPositive ? rs.getInt("m") : 0;
                }
                if (!haveNonPositive) {
                    break;
                }
                long maxPositive;
                try (Statement s = conn.createStatement();
                        ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(id), 0) FROM applications WHERE id > 0")) {
                    rs.next();
                    maxPositive = rs.getLong(1);
                }
                long newId = maxPositive + 1;
                if (newId < 1) {
                    newId = 1;
                }
                while (applicationIdExists(conn, newId)) {
                    newId++;
                }
                if (tableExists(conn, "interview_candidates")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE interview_candidates SET application_id = ? WHERE application_id = ?")) {
                        ps.setLong(1, newId);
                        ps.setInt(2, badId);
                        ps.executeUpdate();
                    }
                }
                if (tableExists(conn, "interviews")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE interviews SET application_id = ? WHERE application_id = ?")) {
                        ps.setLong(1, newId);
                        ps.setInt(2, badId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE applications SET id = ? WHERE id = ?")) {
                    ps.setLong(1, newId);
                    ps.setInt(2, badId);
                    int n = ps.executeUpdate();
                    if (n == 0) {
                        break;
                    }
                }
                logger.info("Renumbered applications.id from {} to {} (non-positive PK before AUTO_INCREMENT)", badId,
                        newId);
            }
        } catch (SQLException e) {
            logger.warn("fixApplicationsNonPositivePrimaryKeys: {}", e.getMessage());
        }
    }

    private static boolean applicationIdExists(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM applications WHERE id = ? LIMIT 1")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void ensurePrimaryKeyAutoIncrement(Connection conn, String tableName) {
        if (!tableExists(conn, tableName) || !columnExistsOnConnection(conn, tableName, "id")) {
            return;
        }
        String ddl;
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
            if (!rs.next()) {
                return;
            }
            ddl = rs.getString(2);
        } catch (SQLException e) {
            logger.debug("SHOW CREATE TABLE {} failed: {}", tableName, e.getMessage());
            return;
        }
        if (ddlLineForIdHasAutoIncrement(ddl)) {
            return;
        }
        String dataType = readIdDataType(conn, tableName);

        if ("applications".equals(tableName)) {
            try {
                if (hasDuplicatePrimaryKeyOnId(conn, "applications")) {
                    logger.error("applications.id has duplicate values; fix data manually, then restart.");
                    return;
                }
            } catch (SQLException e) {
                logger.warn("Duplicate PK check failed: {}", e.getMessage());
            }
            fixApplicationsNonPositivePrimaryKeys(conn);
        }

        // phpMyAdmin often uses a new connection per query, so SET FOREIGN_KEY_CHECKS does not
        // "stick" to the next ALTER. Dropping every FK that references applications.id avoids #1834.
        int droppedRefsToApplications = 0;
        if ("applications".equals(tableName)) {
            try {
                droppedRefsToApplications = dropForeignKeysReferencingTable(conn, "applications");
            } catch (SQLException e) {
                logger.warn("Could not drop FKs referencing applications: {}", e.getMessage());
            }
        }

        long nextAutoInc = readNextAutoIncrementValue(conn, tableName);
        String autoIncClause = ", AUTO_INCREMENT = " + nextAutoInc;

        try (Statement fk = conn.createStatement()) {
            fk.execute("SET FOREIGN_KEY_CHECKS = 0");
        } catch (SQLException e) {
            logger.warn("Could not SET FOREIGN_KEY_CHECKS=0 for {}: {}", tableName, e.getMessage());
        }
        try {
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `id` " + dataType + " NOT NULL AUTO_INCREMENT"
                        + autoIncClause);
                logger.info("Migration: {}.id is now AUTO_INCREMENT (next={})", tableName, nextAutoInc);
                return;
            } catch (SQLException e) {
                logger.warn("ALTER MODIFY id failed for {}: {}", tableName, e.getMessage());
            }
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + tableName + "` ADD PRIMARY KEY (`id`)");
                logger.info("Migration: added PRIMARY KEY on {}.id", tableName);
            } catch (SQLException e) {
                logger.debug("ADD PRIMARY KEY skipped for {}: {}", tableName, e.getMessage());
            }
            nextAutoInc = readNextAutoIncrementValue(conn, tableName);
            autoIncClause = ", AUTO_INCREMENT = " + nextAutoInc;
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `id` " + dataType + " NOT NULL AUTO_INCREMENT"
                        + autoIncClause);
                logger.info("Migration: {}.id AUTO_INCREMENT (after PK step, next={})", tableName, nextAutoInc);
            } catch (SQLException e) {
                logger.error("Could not fix {}.id AUTO_INCREMENT: {}", tableName, e.getMessage());
            }
        } finally {
            try (Statement s = conn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (SQLException e) {
                logger.error("Failed to SET FOREIGN_KEY_CHECKS=1 after migrating {}: {}", tableName, e.getMessage());
            }
            if ("applications".equals(tableName) && droppedRefsToApplications > 0) {
                restoreApplicationsIncomingForeignKeys(conn);
            }
        }
    }

    /**
     * Drops every foreign key on other tables that reference {@code referencedTable}.id
     * (needed before ALTER … MODIFY on MySQL #1834 when session FK checks do not persist).
     *
     * @return number of constraints dropped
     */
    private static int dropForeignKeysReferencingTable(Connection conn, String referencedTable) throws SQLException {
        String schema = conn.getCatalog();
        if (schema == null || schema.isBlank()) {
            return 0;
        }
        String escSchema = schema.replace("'", "''");
        String escRef = referencedTable.replace("'", "''");
        String q = "SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME "
                + "FROM information_schema.KEY_COLUMN_USAGE kcu "
                + "WHERE kcu.TABLE_SCHEMA = '" + escSchema + "' "
                + "AND kcu.REFERENCED_TABLE_NAME = '" + escRef + "' "
                + "AND kcu.REFERENCED_COLUMN_NAME = 'id'";
        List<String[]> pairs = new ArrayList<>();
        try (Statement s = conn.createStatement();
                ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                pairs.add(new String[] { rs.getString(1), rs.getString(2) });
            }
        }
        int n = 0;
        for (String[] p : pairs) {
            String tbl = p[0].replace("`", "");
            String cst = p[1].replace("`", "");
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + tbl + "` DROP FOREIGN KEY `" + cst + "`");
                n++;
                logger.info("Dropped FK {} on table {} (referenced {})", cst, tbl, referencedTable);
            } catch (SQLException e) {
                logger.warn("Could not drop FK {} on {}: {}", cst, tbl, e.getMessage());
            }
        }
        return n;
    }

    /**
     * Recreates FKs from {@code interview_candidates} and {@code interviews} to {@code applications(id)}
     * (same ON DELETE CASCADE as in {@link #createInterviewCandidatesTable} / {@link #createInterviewsTable}).
     */
    private static void restoreApplicationsIncomingForeignKeys(Connection conn) {
        try (Statement s = conn.createStatement()) {
            if (tableExists(conn, "interview_candidates")) {
                try {
                    s.execute("ALTER TABLE interview_candidates "
                            + "ADD CONSTRAINT fk_interview_candidates_application "
                            + "FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE");
                    logger.info("Recreated FK interview_candidates.application_id -> applications.id");
                } catch (SQLException e) {
                    logger.warn("Could not recreate interview_candidates -> applications FK: {}", e.getMessage());
                }
            }
            if (tableExists(conn, "interviews")) {
                try {
                    s.execute("ALTER TABLE interviews "
                            + "ADD CONSTRAINT fk_interviews_application "
                            + "FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE");
                    logger.info("Recreated FK interviews.application_id -> applications.id");
                } catch (SQLException e) {
                    logger.warn("Could not recreate interviews -> applications FK: {}", e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.warn("restoreApplicationsIncomingForeignKeys: {}", e.getMessage());
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

    /**
     * Creates the interview_candidates table.
     * This table stores all accepted candidates who are eligible for interviews (before scheduling).
     * Represents the interview pool before planning.
     */
    private static void createInterviewCandidatesTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS interview_candidates (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        application_id INT NOT NULL,
                        candidate_profile_id INT NOT NULL,
                        job_offer_id INT NOT NULL,
                        company_id INT,
                        status VARCHAR(30) DEFAULT 'ELIGIBLE',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
                        FOREIGN KEY (candidate_profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
                        FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE CASCADE,
                        UNIQUE KEY uq_application_candidate (application_id)
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'interview_candidates' table.");
        } catch (SQLException e) {
            logger.error("Error creating interview_candidates table: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates the interviews table.
     * This table stores only planned/scheduled interviews with date, time, and details.
     * Represents the final interview planning stage.
     */
    private static void createInterviewsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS interviews (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        application_id INT NOT NULL,
                        interview_date DATETIME NOT NULL,
                        location VARCHAR(255),
                        interview_type VARCHAR(50) DEFAULT 'IN_PERSON',
                        notes TEXT,
                        status VARCHAR(30) DEFAULT 'SCHEDULED',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
                        UNIQUE KEY uq_application_interview (application_id)
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'interviews' table.");
        } catch (SQLException e) {
            logger.error("Error creating interviews table: {}", e.getMessage(), e);
        }
    }

    /**
     * Adds columns expected by {@link com.skilora.service.recruitment.InterviewService} when an old
     * {@code interviews} table exists without them ({@code CREATE TABLE IF NOT EXISTS} does not alter it).
     */
    private static void ensureInterviewsTableColumns(Statement stmt) {
        if (!tableExists(stmt, "interviews")) {
            return;
        }
        try {
            // Legacy: column was named scheduled_date; code uses interview_date. INSERT omitted scheduled_date → MySQL error.
            if (columnExists(stmt, "interviews", "scheduled_date")) {
                if (!columnExists(stmt, "interviews", "interview_date")) {
                    stmt.execute("ALTER TABLE interviews CHANGE COLUMN scheduled_date interview_date DATETIME NOT NULL");
                    logger.info("Renamed interviews.scheduled_date -> interview_date (migration).");
                } else {
                    stmt.execute("ALTER TABLE interviews MODIFY COLUMN scheduled_date DATETIME NULL");
                    logger.info("Legacy interviews.scheduled_date set to NULLABLE (application uses interview_date).");
                }
            }
            if (!columnExists(stmt, "interviews", "interview_date")) {
                stmt.execute(
                        "ALTER TABLE interviews ADD COLUMN interview_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
                logger.info("Added column interviews.interview_date (migration).");
            }
            if (!columnExists(stmt, "interviews", "interview_type")) {
                stmt.execute("ALTER TABLE interviews ADD COLUMN interview_type VARCHAR(50) DEFAULT 'IN_PERSON'");
                logger.info("Added column interviews.interview_type (migration).");
            }
            if (!columnExists(stmt, "interviews", "notes")) {
                stmt.execute("ALTER TABLE interviews ADD COLUMN notes TEXT NULL");
                logger.info("Added column interviews.notes (migration).");
            }
            if (!columnExists(stmt, "interviews", "status")) {
                stmt.execute("ALTER TABLE interviews ADD COLUMN status VARCHAR(30) DEFAULT 'SCHEDULED'");
                logger.info("Added column interviews.status (migration).");
            }
            if (!columnExists(stmt, "interviews", "location")) {
                stmt.execute("ALTER TABLE interviews ADD COLUMN location VARCHAR(255) NULL");
                logger.info("Added column interviews.location (migration).");
            }
            if (!columnExists(stmt, "interviews", "created_at")) {
                stmt.execute("ALTER TABLE interviews ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP");
                logger.info("Added column interviews.created_at (migration).");
            }
            if (!columnExists(stmt, "interviews", "updated_at")) {
                stmt.execute(
                        "ALTER TABLE interviews ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                logger.info("Added column interviews.updated_at (migration).");
            }
        } catch (SQLException e) {
            logger.warn("Could not migrate interviews table columns: {}", e.getMessage());
        }
    }

    /**
     * Migrates existing ACCEPTED applications to interview_candidates table.
     * This ensures that all existing accepted applications are available for interview scheduling.
     */
    private static void migrateInterviewCandidates(Statement stmt) {
        try {
            // Check if interview_candidates table exists
            if (!tableExists(stmt, "interview_candidates")) {
                logger.warn("interview_candidates table does not exist, skipping migration");
                return;
            }

            // Insert all ACCEPTED applications that don't have an interview scheduled yet
            // and are not already in interview_candidates
            String migrationSql = """
                INSERT INTO interview_candidates (application_id, candidate_profile_id, job_offer_id, company_id, status)
                SELECT 
                    a.id AS application_id,
                    a.candidate_profile_id,
                    a.job_offer_id,
                    jo.company_id,
                    'ELIGIBLE' AS status
                FROM applications a
                INNER JOIN job_offers jo ON a.job_offer_id = jo.id
                WHERE a.status = 'ACCEPTED'
                AND NOT EXISTS (
                    SELECT 1 FROM interviews i WHERE i.application_id = a.id
                )
                AND NOT EXISTS (
                    SELECT 1 FROM interview_candidates ic WHERE ic.application_id = a.id
                )
                """;
            
            int rowsInserted = stmt.executeUpdate(migrationSql);
            if (rowsInserted > 0) {
                logger.info("Migrated {} ACCEPTED applications to interview_candidates table", rowsInserted);
            } else {
                logger.debug("No new applications to migrate to interview_candidates");
            }
        } catch (SQLException e) {
            logger.error("Error migrating interview candidates: {}", e.getMessage(), e);
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
            "CREATE INDEX idx_users_email ON users(email)",
            "CREATE INDEX idx_applications_profile_job ON applications(candidate_profile_id, job_offer_id)"
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

    private static void createRecruitmentAiTables(Statement stmt) {
        try {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS job_offer_skills (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        job_offer_id INT NOT NULL,
                        skill_name VARCHAR(120) NOT NULL,
                        skill_weight INT DEFAULT 1,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_job_skill (job_offer_id, skill_name),
                        FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS candidate_match_scores (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        profile_id INT NOT NULL,
                        job_offer_id INT NOT NULL,
                        match_percentage INT NOT NULL,
                        algorithm_source VARCHAR(40) DEFAULT 'LOCAL',
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_profile_job_match (profile_id, job_offer_id),
                        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
                        FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS candidate_scores (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        profile_id INT NOT NULL,
                        score INT NOT NULL,
                        years_experience INT DEFAULT 0,
                        skills_count INT DEFAULT 0,
                        certifications_count INT DEFAULT 0,
                        score_source VARCHAR(40) DEFAULT 'LOCAL',
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_profile_score (profile_id),
                        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS cv_ai_analysis (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        profile_id INT NOT NULL,
                        cv_file_name VARCHAR(255),
                        detected_skills_json TEXT,
                        summary TEXT,
                        years_of_experience INT DEFAULT 0,
                        analyzed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS candidate_certifications (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        profile_id INT NOT NULL,
                        certification_name VARCHAR(255) NOT NULL,
                        issuer VARCHAR(255),
                        obtained_date DATE,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
                    )
                    """);
            logger.info("Recruitment AI tables verified.");
        } catch (SQLException e) {
            logger.error("Error creating recruitment AI tables: {}", e.getMessage(), e);
        }
    }

    private static void ensureRecruitmentConstraints(Statement stmt) {
        try {
            stmt.execute("""
                    ALTER TABLE applications
                    ADD CONSTRAINT uq_applications_candidate_job UNIQUE (candidate_profile_id, job_offer_id)
                    """);
            logger.info("Added unique constraint for applications(candidate_profile_id, job_offer_id).");
        } catch (SQLException ignored) {
            // Likely already exists.
        }
    }

    private static void backfillJobOfferSkills(Statement stmt) {
        try {
            String sql = """
                    INSERT IGNORE INTO job_offer_skills (job_offer_id, skill_name, skill_weight)
                    SELECT jo.id, TRIM(jt.skill_token) AS skill_name, 1
                    FROM job_offers jo
                    JOIN JSON_TABLE(
                        CONCAT('["', REPLACE(REPLACE(REPLACE(COALESCE(jo.requirements, ''), '\\\\r', ''), '\\\\n', ','), ',', '","'), '"]'),
                        "$[*]" COLUMNS(skill_token VARCHAR(120) PATH "$")
                    ) AS jt
                    WHERE TRIM(jt.skill_token) <> ''
                    """;
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // JSON_TABLE can fail on older MySQL; skip silently.
            logger.debug("Job offer skills backfill skipped: {}", e.getMessage());
        }
    }

    private static void seedRecruitmentSampleData(Statement stmt) {
        try {
            // Minimal deterministic sample data for ranking/scoring flows.
            stmt.executeUpdate("""
                    INSERT INTO candidate_certifications (profile_id, certification_name, issuer, obtained_date)
                    SELECT p.id, 'AWS Cloud Practitioner', 'Amazon', CURDATE()
                    FROM profiles p
                    WHERE NOT EXISTS (
                        SELECT 1 FROM candidate_certifications cc WHERE cc.profile_id = p.id
                    )
                    LIMIT 2
                    """);
        } catch (SQLException e) {
            logger.debug("Sample data seed skipped: {}", e.getMessage());
        }
    }

    /**
     * Ensure test users exist with correct roles.
     * This method creates or updates test users to have the correct roles.
     */
    private static void ensureTestUsers(Statement stmt) {
        try {
            if (!tableExists(stmt, "users")) {
                logger.warn("users table is missing; skipping test user seed");
                return;
            }

            // Hash for password "emp123" using BCrypt
            String emp123Hash = BCrypt.hashpw("emp123", BCrypt.gensalt(12));
            // Hash for password "yosr123" using BCrypt
            String yosr123Hash = BCrypt.hashpw("yosr123", BCrypt.gensalt(12));
            // Hash for password "admin123" using BCrypt (demo administrator)
            String admin123Hash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));

            int updated;
            int created;

            // Fix admin user role if the row exists under another role
            String fixAdmin = "UPDATE users SET role = 'ADMIN' WHERE username = 'admin' AND role != 'ADMIN'";
            updated = stmt.executeUpdate(fixAdmin);
            if (updated > 0) {
                logger.info("Fixed {} user(s) named admin to role ADMIN", updated);
            }

            // Create admin user if it doesn't exist
            String createAdmin = String.format(
                    "INSERT INTO users (username, email, password, role, full_name, is_verified, is_active) "
                            + "SELECT 'admin', 'admin@skilora.com', '%s', 'ADMIN', 'Administrator', TRUE, TRUE "
                            + "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')",
                    admin123Hash.replace("'", "''"));
            created = stmt.executeUpdate(createAdmin);
            if (created > 0) {
                logger.info("Created admin test user (username: admin, password: admin123)");
            }

            // Existing row with username admin but wrong hash (or legacy password) would make login fail;
            // keep demo admin aligned with admin123 on every startup.
            String syncAdminDemo = String.format(
                    "UPDATE users SET password = '%s', role = 'ADMIN', email = 'admin@skilora.com', "
                            + "is_verified = TRUE, is_active = TRUE WHERE username = 'admin'",
                    admin123Hash.replace("'", "''"));
            int syncedAdmin = stmt.executeUpdate(syncAdminDemo);
            if (syncedAdmin > 0) {
                logger.info("Demo admin password and role synchronized (login: admin / admin123)");
            }

            // Fix employer user role if it exists (check for any variation of employer username)
            String fixEmployer = "UPDATE users SET role = 'EMPLOYER' WHERE (username = 'employer' OR username LIKE 'employer%') AND role != 'EMPLOYER'";
            updated = stmt.executeUpdate(fixEmployer);
            if (updated > 0) {
                logger.info("Fixed {} employer user(s) role to EMPLOYER", updated);
            }

            // Create employer user if it doesn't exist
            String createEmployer = String.format(
                "INSERT INTO users (username, email, password, role, full_name, is_verified, is_active) " +
                "SELECT 'employer', 'employer@skilora.com', '%s', 'EMPLOYER', 'Employer User', TRUE, TRUE " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'employer')",
                emp123Hash.replace("'", "''") // Escape single quotes in SQL
            );
            created = stmt.executeUpdate(createEmployer);
            if (created > 0) {
                logger.info("Created employer test user (username: employer, password: emp123)");
            }

            // Keep demo employer password aligned with emp123 (same issue as admin if row pre-existed).
            String syncEmployerDemo = String.format(
                    "UPDATE users SET password = '%s', role = 'EMPLOYER', email = 'employer@skilora.com', "
                            + "is_verified = TRUE, is_active = TRUE WHERE username = 'employer'",
                    emp123Hash.replace("'", "''"));
            int syncedEmployer = stmt.executeUpdate(syncEmployerDemo);
            if (syncedEmployer > 0) {
                logger.info("Demo employer password and role synchronized (login: employer / emp123)");
            }

            // Fix trainer user role if it exists
            String fixTrainer = "UPDATE users SET role = 'TRAINER' WHERE (username = 'trainer' OR username LIKE 'trainer%') AND role != 'TRAINER'";
            updated = stmt.executeUpdate(fixTrainer);
            if (updated > 0) {
                logger.info("Fixed {} trainer user(s) role to TRAINER", updated);
            }

            // Create yosr user if it doesn't exist
            String createYosr = String.format(
                "INSERT INTO users (username, email, password, role, full_name, is_verified, is_active) " +
                "SELECT 'yosr', 'yosr@skilora.com', '%s', 'USER', 'Yosr User', TRUE, TRUE " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'yosr')",
                yosr123Hash.replace("'", "''") // Escape single quotes in SQL
            );
            created = stmt.executeUpdate(createYosr);
            if (created > 0) {
                logger.info("Created yosr test user (username: yosr, password: yosr123)");
            }

            // Create candidate test users with proper BCrypt password hashes
            ensureCandidateTestUsers(stmt);
            // Create profiles for candidate test users
            ensureCandidateTestProfiles(stmt);

        } catch (SQLException e) {
            logger.warn("Could not ensure test users: {}", e.getMessage());
        }
    }

    /**
     * Ensure candidate test users exist with proper BCrypt password hashes.
     * Password for all candidates: "candidate123"
     */
    private static void ensureCandidateTestUsers(Statement stmt) {
        try {
            // Hash for password "candidate123" using BCrypt
            String candidate123Hash = BCrypt.hashpw("candidate123", BCrypt.gensalt(12));
            
            // Candidate test users data
            Object[][] candidates = {
                {200, "ahmed_dev", "ahmed.dev@email.com", "Ahmed Ben Ali"},
                {201, "sarah_frontend", "sarah.frontend@email.com", "Sarah Trabelsi"},
                {202, "mohamed_fullstack", "mohamed.fullstack@email.com", "Mohamed Khelifi"},
                {203, "fatma_java", "fatma.java@email.com", "Fatma Mansouri"},
                {204, "youssef_python", "youssef.python@email.com", "Youssef Chaabani"},
                {205, "mariem_devops", "mariem.devops@email.com", "Mariem Bouazizi"},
                {206, "karim_react", "karim.react@email.com", "Karim Sassi"},
                {207, "leila_ui", "leila.ui@email.com", "Leila Hammami"},
                {208, "hassan_backend", "hassan.backend@email.com", "Hassan Mezghani"},
                {209, "nour_mobile", "nour.mobile@email.com", "Nour Ben Ammar"}
            };
            
            int created = 0;
            for (Object[] candidate : candidates) {
                int userId = (Integer) candidate[0];
                String username = (String) candidate[1];
                String email = (String) candidate[2];
                String fullName = (String) candidate[3];
                
                String createCandidate = String.format(
                    "INSERT INTO users (id, username, email, password, role, full_name, is_verified, is_active) " +
                    "SELECT %d, '%s', '%s', '%s', 'CANDIDATE', '%s', TRUE, TRUE " +
                    "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = '%s' OR id = %d)",
                    userId, username, email, candidate123Hash.replace("'", "''"), fullName, username, userId
                );
                
                int rows = stmt.executeUpdate(createCandidate);
                if (rows > 0) {
                    created++;
                    logger.info("Created candidate test user: {} (password: candidate123)", username);
                }
            }
            
            if (created > 0) {
                logger.info("Created {} candidate test user(s). All use password: candidate123", created);
            }
            
        } catch (SQLException e) {
            logger.warn("Could not ensure candidate test users: {}", e.getMessage());
        }
    }

    /**
     * Ensure candidate test profiles exist for the candidate test users.
     */
    private static void ensureCandidateTestProfiles(Statement stmt) {
        try {
            // Profile data matching the user IDs
            Object[][] profiles = {
                {200, 200, "Ahmed", "Ben Ali", "+216 98 123 456", "Tunis, Tunisia", "1995-03-15"},
                {201, 201, "Sarah", "Trabelsi", "+216 22 234 567", "Sousse, Tunisia", "1997-07-22"},
                {202, 202, "Mohamed", "Khelifi", "+216 55 345 678", "Sfax, Tunisia", "1993-11-30"},
                {203, 203, "Fatma", "Mansouri", "+216 71 456 789", "Tunis, Tunisia", "1996-04-10"},
                {204, 204, "Youssef", "Chaabani", "+216 98 567 890", "Monastir, Tunisia", "1994-09-25"},
                {205, 205, "Mariem", "Bouazizi", "+216 20 678 901", "Tunis, Tunisia", "1998-01-18"},
                {206, 206, "Karim", "Sassi", "+216 22 789 012", "Sousse, Tunisia", "1995-06-05"},
                {207, 207, "Leila", "Hammami", "+216 55 890 123", "Sfax, Tunisia", "1997-12-20"},
                {208, 208, "Hassan", "Mezghani", "+216 71 901 234", "Tunis, Tunisia", "1992-08-14"},
                {209, 209, "Nour", "Ben Ammar", "+216 98 012 345", "Ariana, Tunisia", "1999-02-28"}
            };
            
            int created = 0;
            for (Object[] profile : profiles) {
                int profileId = (Integer) profile[0];
                int userId = (Integer) profile[1];
                String firstName = (String) profile[2];
                String lastName = (String) profile[3];
                String phone = (String) profile[4];
                String location = (String) profile[5];
                String birthDate = (String) profile[6];
                
                String createProfile = String.format(
                    "INSERT INTO profiles (id, user_id, first_name, last_name, phone, location, birth_date) " +
                    "SELECT %d, %d, '%s', '%s', '%s', '%s', '%s' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM profiles WHERE user_id = %d OR id = %d) " +
                    "AND EXISTS (SELECT 1 FROM users WHERE id = %d)",
                    profileId, userId, firstName, lastName, phone, location, birthDate, userId, profileId, userId
                );
                
                int rows = stmt.executeUpdate(createProfile);
                if (rows > 0) {
                    created++;
                    logger.debug("Created profile for candidate user ID: {}", userId);
                }
            }
            
            if (created > 0) {
                logger.info("Created {} candidate test profile(s)", created);
            }
            
        } catch (SQLException e) {
            logger.warn("Could not ensure candidate test profiles: {}", e.getMessage());
        }
    }

    /**
     * Fixes all application relationships to ensure employers can see all applications.
     * This method:
     * 1. Ensures all employers have a company
     * 2. Ensures all job offers are linked to the correct company
     * 3. Fixes any orphaned applications
     */
    private static void fixAllApplicationRelationships(Statement stmt) {
        try {
            logger.info("Fixing application relationships to ensure all applications are visible to employers...");
            
            // Step 1: Ensure all employers have a company
            String ensureCompaniesSql = """
                INSERT INTO companies (owner_id, name, country)
                SELECT DISTINCT u.id, COALESCE(u.full_name, u.username, 'Entreprise'), 'Tunisia'
                FROM users u
                WHERE u.role = 'EMPLOYER'
                AND NOT EXISTS (
                    SELECT 1 FROM companies c WHERE c.owner_id = u.id
                )
                """;
            int companiesCreated = stmt.executeUpdate(ensureCompaniesSql);
            if (companiesCreated > 0) {
                logger.info("Created {} companies for employers", companiesCreated);
            }
            
            // Step 2: For each application, ensure its job offer is linked to an employer's company
            // If a job offer has applications but is linked to company_id = 1, NULL, or 0,
            // link it to the first available employer's company
            String fixJobOffersSql = """
                UPDATE job_offers jo
                INNER JOIN applications a ON a.job_offer_id = jo.id
                INNER JOIN (
                    SELECT c.id AS company_id
                    FROM companies c
                    INNER JOIN users u ON c.owner_id = u.id
                    WHERE u.role = 'EMPLOYER'
                    ORDER BY c.id
                    LIMIT 1
                ) AS employer_company ON 1=1
                SET jo.company_id = employer_company.company_id
                WHERE (jo.company_id = 1 OR jo.company_id IS NULL OR jo.company_id = 0)
                OR NOT EXISTS (
                    SELECT 1 FROM companies c2 
                    WHERE c2.id = jo.company_id 
                    AND c2.owner_id IN (SELECT id FROM users WHERE role = 'EMPLOYER')
                )
                """;
            
            int fixedOffers = stmt.executeUpdate(fixJobOffersSql);
            if (fixedOffers > 0) {
                logger.info("Fixed {} job offers by linking them to employer companies", fixedOffers);
            }
            
            // Step 3: Verify all applications are now visible to employers
            String verifySql = """
                SELECT 
                    COUNT(DISTINCT a.id) as total_apps,
                    COUNT(DISTINCT CASE 
                        WHEN c.owner_id IN (SELECT id FROM users WHERE role = 'EMPLOYER') 
                        THEN a.id 
                    END) as visible_apps
                FROM applications a
                INNER JOIN job_offers jo ON a.job_offer_id = jo.id
                LEFT JOIN companies c ON jo.company_id = c.id
                """;
            
            try (var rs = stmt.executeQuery(verifySql)) {
                if (rs.next()) {
                    int totalApps = rs.getInt("total_apps");
                    int visibleApps = rs.getInt("visible_apps");
                    logger.info("Application visibility check: {}/{} applications are visible to employers", visibleApps, totalApps);
                    if (visibleApps < totalApps) {
                        logger.warn("Some applications are still not visible to employers. Manual review may be needed.");
                    } else {
                        logger.info("All applications are now visible to employers.");
                    }
                }
            }
            
            logger.info("Application relationships fix completed.");
            
        } catch (SQLException e) {
            logger.error("Error fixing application relationships: {}", e.getMessage(), e);
        }
    }
}
