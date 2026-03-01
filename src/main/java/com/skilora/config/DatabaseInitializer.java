package com.skilora.config;

import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.usermanagement.AuthService;
import com.skilora.service.usermanagement.UserService;
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
import java.util.Optional;

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

            // Create trainings table if missing
            if (!tableExists(stmt, "trainings")) {
                createTrainingsTable(stmt);
            } else {
                // Ensure trainings table has all required columns
                ensureTrainingsTableColumns(stmt);
            }

            // Create training_enrollments table if missing
            if (!tableExists(stmt, "training_enrollments")) {
                createTrainingEnrollmentsTable(stmt);
            }

            // Create training_lessons table if missing
            if (!tableExists(stmt, "training_lessons")) {
                createTrainingLessonsTable(stmt);
            }
            
            // Create training_ratings table if missing
            if (!tableExists(stmt, "training_ratings")) {
                createTrainingRatingsTable(stmt);
            }

            // Create training_materials table if missing
            if (!tableExists(stmt, "training_materials")) {
                createTrainingMaterialsTable(stmt);
            }

            // Create lesson_progress table if missing
            if (!tableExists(stmt, "lesson_progress")) {
                createLessonProgressTable(stmt);
            }

            // Create certificates table if missing
            if (!tableExists(stmt, "certificates")) {
                createCertificatesTable(stmt);
            } else {
                // Ensure certificates table has correct structure (AUTO_INCREMENT on id)
                ensureCertificatesTableColumns(stmt);
            }


            // Add missing columns to users table
            ensureUsersTableColumns(stmt);

            // Create performance indexes on foreign keys
            createIndexes(stmt);
            
            // Seed default users if they don't exist
            seedDefaultUsers(conn);

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

    private static void createTrainingsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS trainings (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        cost DECIMAL(10, 2) NULL DEFAULT NULL,
                        duration INT NOT NULL COMMENT 'Duration in hours',
                        level VARCHAR(20) NOT NULL COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
                        category VARCHAR(50) NOT NULL COMMENT 'DEVELOPMENT, DESIGN, MARKETING, DATA_SCIENCE, LANGUAGES, SOFT_SKILLS, MANAGEMENT',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_category (category),
                        INDEX idx_level (level),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'trainings' table.");
        } catch (SQLException e) {
            logger.error("Error creating trainings table: {}", e.getMessage(), e);
        }
    }

    private static void ensureTrainingsTableColumns(Statement stmt) {
        // Ensure id column has AUTO_INCREMENT
        try {
            // Check if id column has AUTO_INCREMENT by querying information_schema
            String checkAutoIncrement = "SELECT COLUMN_TYPE, EXTRA FROM information_schema.COLUMNS " +
                                       "WHERE TABLE_SCHEMA = DATABASE() " +
                                       "AND TABLE_NAME = 'trainings' " +
                                       "AND COLUMN_NAME = 'id'";
            try (var rs = stmt.executeQuery(checkAutoIncrement)) {
                if (rs.next()) {
                    String columnType = rs.getString("COLUMN_TYPE");
                    String extra = rs.getString("EXTRA");
                    boolean hasAutoIncrement = (columnType != null && columnType.toUpperCase().contains("AUTO_INCREMENT")) ||
                                             (extra != null && extra.toUpperCase().contains("AUTO_INCREMENT"));
                    
                    if (!hasAutoIncrement) {
                        // Column exists but doesn't have AUTO_INCREMENT, try to add it
                        logger.warn("trainings table id column does not have AUTO_INCREMENT. Attempting automatic fix...");
                        try {
                            // Modify column to add AUTO_INCREMENT without redefining PRIMARY KEY
                            stmt.execute("ALTER TABLE trainings MODIFY COLUMN id INT AUTO_INCREMENT");
                            
                            // Verify the fix was successful
                            try (var verifyRs = stmt.executeQuery(checkAutoIncrement)) {
                                if (verifyRs.next()) {
                                    String newColumnType = verifyRs.getString("COLUMN_TYPE");
                                    String newExtra = verifyRs.getString("EXTRA");
                                    boolean nowHasAutoIncrement = (newColumnType != null && newColumnType.toUpperCase().contains("AUTO_INCREMENT")) ||
                                                                 (newExtra != null && newExtra.toUpperCase().contains("AUTO_INCREMENT"));
                                    if (nowHasAutoIncrement) {
                                        logger.info("✓ Successfully fixed AUTO_INCREMENT on trainings.id column.");
                                    } else {
                                        logger.warn("⚠ AUTO_INCREMENT fix attempted but verification failed. Manual intervention may be required.");
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            logger.error("✗ Could not set AUTO_INCREMENT on trainings.id: {}. " +
                                       "You may need to manually fix the table structure. " +
                                       "Run: ALTER TABLE trainings MODIFY COLUMN id INT AUTO_INCREMENT;", e.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.debug("Could not check AUTO_INCREMENT for trainings.id: {}", e.getMessage());
        }
        
        // Ensure cost column is nullable
        try {
            String checkCostNullable = "SELECT IS_NULLABLE FROM information_schema.COLUMNS " +
                                      "WHERE TABLE_SCHEMA = DATABASE() " +
                                      "AND TABLE_NAME = 'trainings' " +
                                      "AND COLUMN_NAME = 'cost'";
            try (var rs = stmt.executeQuery(checkCostNullable)) {
                if (rs.next()) {
                    String isNullable = rs.getString("IS_NULLABLE");
                    if (isNullable != null && !isNullable.equalsIgnoreCase("YES")) {
                        logger.info("Making cost column nullable in trainings table...");
                        stmt.execute("ALTER TABLE trainings MODIFY COLUMN cost DECIMAL(10, 2) NULL DEFAULT NULL");
                        logger.info("Cost column is now nullable.");
                    }
                }
            }
        } catch (SQLException e) {
            logger.debug("Could not check/modify cost column nullability: {}", e.getMessage());
        }
        
        // Check and add duration column if missing
        if (!columnExists(stmt, "trainings", "duration")) {
            try {
                stmt.execute("ALTER TABLE trainings ADD COLUMN duration INT NOT NULL DEFAULT 0 COMMENT 'Duration in hours' AFTER cost");
                logger.info("Added 'duration' column to trainings table.");
            } catch (SQLException e) {
                logger.debug("Could not add duration column: {}", e.getMessage());
            }
        }
        // Check and add other required columns if missing
        if (!columnExists(stmt, "trainings", "level")) {
            try {
                stmt.execute("ALTER TABLE trainings ADD COLUMN level VARCHAR(20) NOT NULL DEFAULT 'BEGINNER' COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED' AFTER duration");
                logger.info("Added 'level' column to trainings table.");
            } catch (SQLException e) {
                logger.debug("Could not add level column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "trainings", "category")) {
            try {
                stmt.execute("ALTER TABLE trainings ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'DEVELOPMENT' COMMENT 'DEVELOPMENT, DESIGN, MARKETING, DATA_SCIENCE, LANGUAGES, SOFT_SKILLS, MANAGEMENT' AFTER level");
                logger.info("Added 'category' column to trainings table.");
            } catch (SQLException e) {
                logger.debug("Could not add category column: {}", e.getMessage());
            }
        }
        // Check and add lesson_count column if missing
        if (!columnExists(stmt, "trainings", "lesson_count")) {
            try {
                stmt.execute("ALTER TABLE trainings ADD COLUMN lesson_count INT NOT NULL DEFAULT 0 COMMENT 'Number of lessons in the training' AFTER duration");
                logger.info("Added 'lesson_count' column to trainings table.");
            } catch (SQLException e) {
                logger.debug("Could not add lesson_count column: {}", e.getMessage());
            }
        }

        // Check and add director_signature column if missing
        if (!columnExists(stmt, "trainings", "director_signature")) {
            try {
                stmt.execute("ALTER TABLE trainings ADD COLUMN director_signature LONGTEXT NULL COMMENT 'Base64 PNG of director signature' AFTER lesson_count");
                logger.info("Added 'director_signature' column to trainings table.");
            } catch (SQLException e) {
                logger.debug("Could not add director_signature column: {}", e.getMessage());
            }
        }
    }

    private static void createTrainingEnrollmentsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS training_enrollments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        training_id INT NOT NULL,
                        enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        completed BOOLEAN DEFAULT FALSE,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE,
                        UNIQUE KEY unique_user_training (user_id, training_id),
                        INDEX idx_user_id (user_id),
                        INDEX idx_training_id (training_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'training_enrollments' table.");
        } catch (SQLException e) {
            logger.error("Error creating training_enrollments table: {}", e.getMessage(), e);
        }
    }

    private static void createTrainingLessonsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS training_lessons (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        training_id INT NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        content LONGTEXT,
                        order_index INT NOT NULL DEFAULT 0,
                        duration_minutes INT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE,
                        INDEX idx_training_id (training_id),
                        INDEX idx_order_index (order_index)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'training_lessons' table.");
        } catch (SQLException e) {
            logger.error("Error creating training_lessons table: {}", e.getMessage(), e);
        }
    }

    private static void createTrainingMaterialsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS training_materials (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        training_id INT NOT NULL,
                        lesson_id INT NULL,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        file_url VARCHAR(500) NOT NULL,
                        file_type VARCHAR(50),
                        file_size_bytes BIGINT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE,
                        FOREIGN KEY (lesson_id) REFERENCES training_lessons(id) ON DELETE CASCADE,
                        INDEX idx_training_id (training_id),
                        INDEX idx_lesson_id (lesson_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'training_materials' table.");
        } catch (SQLException e) {
            logger.error("Error creating training_materials table: {}", e.getMessage(), e);
        }
    }

    private static void createLessonProgressTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS lesson_progress (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        enrollment_id INT NOT NULL,
                        lesson_id INT NOT NULL,
                        completed BOOLEAN DEFAULT FALSE,
                        progress_percentage INT DEFAULT 0,
                        started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        completed_at TIMESTAMP NULL,
                        last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (enrollment_id) REFERENCES training_enrollments(id) ON DELETE CASCADE,
                        FOREIGN KEY (lesson_id) REFERENCES training_lessons(id) ON DELETE CASCADE,
                        UNIQUE KEY unique_enrollment_lesson (enrollment_id, lesson_id),
                        INDEX idx_enrollment_id (enrollment_id),
                        INDEX idx_lesson_id (lesson_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'lesson_progress' table.");
        } catch (SQLException e) {
            logger.error("Error creating lesson_progress table: {}", e.getMessage(), e);
        }
    }

    private static void createTrainingRatingsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS training_ratings (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        training_id INT NOT NULL,
                        is_liked TINYINT(1) DEFAULT NULL,
                        star_rating INT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_user_training_rating (user_id, training_id),
                        INDEX idx_user_id (user_id),
                        INDEX idx_training_id (training_id),
                        INDEX idx_star_rating (star_rating),
                        INDEX idx_is_liked (is_liked),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'training_ratings' table.");
        } catch (SQLException e) {
            logger.error("Error creating training_ratings table: {}", e.getMessage(), e);
        }
    }

    private static void createCertificatesTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS certificates (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        training_id INT NOT NULL,
                        certificate_number VARCHAR(100) NOT NULL UNIQUE,
                        verification_token VARCHAR(100) NOT NULL UNIQUE,
                        issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        pdf_path VARCHAR(500),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE,
                        UNIQUE KEY unique_user_training_cert (user_id, training_id),
                        INDEX idx_user_id (user_id),
                        INDEX idx_training_id (training_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
            stmt.execute(sql);
            logger.info("Created 'certificates' table.");
        } catch (SQLException e) {
            logger.error("Error creating certificates table: {}", e.getMessage(), e);
        }
    }

    private static void ensureCertificatesTableColumns(Statement stmt) {
        // Ensure id column has AUTO_INCREMENT
        // Check if id column has AUTO_INCREMENT and is PRIMARY KEY
        String checkAutoIncrement = "SELECT COLUMN_TYPE, COLUMN_KEY, EXTRA FROM information_schema.COLUMNS " +
                                   "WHERE TABLE_SCHEMA = DATABASE() " +
                                   "AND TABLE_NAME = 'certificates' " +
                                   "AND COLUMN_NAME = 'id'";
        try (var rs = stmt.executeQuery(checkAutoIncrement)) {
            if (rs.next()) {
                String columnType = rs.getString("COLUMN_TYPE");
                String columnKey = rs.getString("COLUMN_KEY");
                String extra = rs.getString("EXTRA");
                boolean hasAutoIncrement = (columnType != null && columnType.toUpperCase().contains("AUTO_INCREMENT")) ||
                                         (extra != null && extra.toUpperCase().contains("AUTO_INCREMENT"));
                boolean isPrimaryKey = "PRI".equals(columnKey);
                
                if (!hasAutoIncrement || !isPrimaryKey) {
                    // Try to fix with ALTER TABLE first (preserves data)
                    logger.warn("certificates table id column is not properly configured (AUTO_INCREMENT: {}, PRIMARY KEY: {}). " +
                               "Attempting to fix without data loss...", hasAutoIncrement, isPrimaryKey);
                    
                    boolean fixed = false;
                    
                    // First, try to add AUTO_INCREMENT if missing
                    if (!hasAutoIncrement && isPrimaryKey) {
                        try {
                            stmt.execute("ALTER TABLE certificates MODIFY COLUMN id INT AUTO_INCREMENT");
                            logger.info("✓ Added AUTO_INCREMENT to certificates.id column.");
                            fixed = true;
                        } catch (SQLException e) {
                            logger.debug("Could not add AUTO_INCREMENT via ALTER TABLE: {}", e.getMessage());
                        }
                    }
                    
                    // If still not fixed, try to fix PRIMARY KEY if missing
                    if (!isPrimaryKey) {
                        try {
                            // First ensure AUTO_INCREMENT
                            if (!hasAutoIncrement) {
                                stmt.execute("ALTER TABLE certificates MODIFY COLUMN id INT AUTO_INCREMENT");
                            }
                            // Then ensure PRIMARY KEY
                            stmt.execute("ALTER TABLE certificates MODIFY COLUMN id INT AUTO_INCREMENT PRIMARY KEY");
                            logger.info("✓ Fixed PRIMARY KEY and AUTO_INCREMENT on certificates.id column.");
                            fixed = true;
                        } catch (SQLException e) {
                            logger.debug("Could not fix PRIMARY KEY via ALTER TABLE: {}", e.getMessage());
                        }
                    }
                    
                    // Verify the fix
                    if (fixed) {
                        try (var verifyRs = stmt.executeQuery(checkAutoIncrement)) {
                            if (verifyRs.next()) {
                                String newColumnType = verifyRs.getString("COLUMN_TYPE");
                                String newColumnKey = verifyRs.getString("COLUMN_KEY");
                                String newExtra = verifyRs.getString("EXTRA");
                                boolean nowHasAutoIncrement = (newColumnType != null && newColumnType.toUpperCase().contains("AUTO_INCREMENT")) ||
                                                             (newExtra != null && newExtra.toUpperCase().contains("AUTO_INCREMENT"));
                                boolean nowIsPrimaryKey = "PRI".equals(newColumnKey);
                                
                                if (nowHasAutoIncrement && nowIsPrimaryKey) {
                                    logger.info("✓ Successfully fixed certificates.id column (AUTO_INCREMENT PRIMARY KEY).");
                                    return; // Success, exit early
                                }
                            }
                        }
                    }
                    
                    // If ALTER TABLE failed, only then drop and recreate (last resort)
                    logger.warn("⚠ ALTER TABLE fix failed. Dropping and recreating certificates table (data will be lost)...");
                    try {
                        // Drop foreign key constraints first if they exist
                        try {
                            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                            stmt.execute("DROP TABLE IF EXISTS certificates");
                            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                            logger.info("Dropped certificates table (with foreign key checks disabled).");
                        } catch (SQLException e) {
                            // If foreign key disable fails, try without it
                            stmt.execute("DROP TABLE IF EXISTS certificates");
                            logger.info("Dropped certificates table.");
                        }
                        // Recreate the table with correct structure
                        createCertificatesTable(stmt);
                        logger.info("Recreated certificates table with correct structure (id as AUTO_INCREMENT PRIMARY KEY).");
                        return; // Exit early since we recreated the table
                    } catch (SQLException e) {
                        logger.error("✗ Could not recreate certificates table: {}. " +
                                   "You may need to manually fix the table structure.", e.getMessage());
                        // Don't throw - try to continue, but log the error
                    }
                } else {
                    logger.debug("certificates table id column is properly configured (AUTO_INCREMENT PRIMARY KEY).");
                }
            } else {
                // Table exists but id column doesn't exist - recreate table
                logger.warn("certificates table exists but id column is missing. Recreating table...");
                try {
                    try {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                        stmt.execute("DROP TABLE IF EXISTS certificates");
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                    } catch (SQLException e) {
                        stmt.execute("DROP TABLE IF EXISTS certificates");
                    }
                    createCertificatesTable(stmt);
                    logger.info("Recreated certificates table.");
                    return;
                } catch (SQLException e) {
                    logger.error("Could not recreate certificates table: {}", e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking certificates table structure: {}. Attempting to recreate table...", e.getMessage());
            try {
                try {
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                    stmt.execute("DROP TABLE IF EXISTS certificates");
                    stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                } catch (SQLException e2) {
                    stmt.execute("DROP TABLE IF EXISTS certificates");
                }
                createCertificatesTable(stmt);
                logger.info("Recreated certificates table after error.");
                return;
            } catch (SQLException e2) {
                logger.error("Could not recreate certificates table: {}", e2.getMessage());
            }
        }
        
        // Ensure all required columns exist
        if (!columnExists(stmt, "certificates", "user_id")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN user_id INT NOT NULL AFTER id");
                logger.info("Added 'user_id' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add user_id column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "training_id")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN training_id INT NOT NULL AFTER user_id");
                logger.info("Added 'training_id' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add training_id column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "certificate_number")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN certificate_number VARCHAR(100) NOT NULL UNIQUE AFTER training_id");
                logger.info("Added 'certificate_number' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add certificate_number column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "issued_at")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER certificate_number");
                logger.info("Added 'issued_at' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add issued_at column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "completed_at")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER issued_at");
                logger.info("Added 'completed_at' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add completed_at column: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "verification_token")) {
            try {
                // Add as nullable first to avoid issues with existing rows
                stmt.execute("ALTER TABLE certificates ADD COLUMN verification_token VARCHAR(100) UNIQUE AFTER certificate_number");
                logger.info("Added 'verification_token' column to certificates table.");
                // Populate verification tokens for existing certificates
                try {
                    String updateSql = "UPDATE certificates SET verification_token = CONCAT('SKL-', UPPER(REPLACE(UUID(), '-', ''))) WHERE verification_token IS NULL";
                    int updated = stmt.executeUpdate(updateSql);
                    if (updated > 0) {
                        logger.info("Generated verification tokens for {} existing certificates", updated);
                    }
                    // Now make it NOT NULL
                    stmt.execute("ALTER TABLE certificates MODIFY COLUMN verification_token VARCHAR(100) NOT NULL UNIQUE");
                    logger.info("Made 'verification_token' column NOT NULL");
                } catch (SQLException e) {
                    logger.debug("Could not populate verification tokens: {}", e.getMessage());
                }
            } catch (SQLException e) {
                logger.debug("Could not add verification_token column: {}", e.getMessage());
            }
        } else {
            // Ensure existing certificates without tokens get them
            try {
                String updateSql = "UPDATE certificates SET verification_token = CONCAT('SKL-', UPPER(REPLACE(UUID(), '-', ''))) WHERE verification_token IS NULL OR verification_token = ''";
                int updated = stmt.executeUpdate(updateSql);
                if (updated > 0) {
                    logger.info("Generated verification tokens for {} existing certificates without tokens", updated);
                }
            } catch (SQLException e) {
                logger.debug("Could not backfill verification tokens: {}", e.getMessage());
            }
        }
        if (!columnExists(stmt, "certificates", "pdf_path")) {
            try {
                stmt.execute("ALTER TABLE certificates ADD COLUMN pdf_path VARCHAR(500) AFTER completed_at");
                logger.info("Added 'pdf_path' column to certificates table.");
            } catch (SQLException e) {
                logger.debug("Could not add pdf_path column: {}", e.getMessage());
            }
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
        // Add photo_url column if missing
        if (!columnExists(stmt, "users", "photo_url")) {
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN photo_url TEXT");
                logger.info("Added 'photo_url' column to users table.");
            } catch (SQLException e) {
                logger.debug("Could not add photo_url column: {}", e.getMessage());
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
     * Seed default users in the database if they don't exist.
     * Creates default accounts with properly hashed passwords.
     */
    private static void seedDefaultUsers(Connection conn) {
        try {
            // Check if users table exists first
            try (Statement stmt = conn.createStatement()) {
                if (!tableExists(stmt, "users")) {
                    logger.debug("Users table does not exist yet. Skipping default user seeding.");
                    return;
                }
            }
            
            UserService userService = UserService.getInstance();
            AuthService authService = AuthService.getInstance();
            
            // Ensure 'user' account exists with correct password
            Optional<User> existingUser = userService.findByUsername("user");
            if (!existingUser.isPresent()) {
                User defaultUser = new User("user", "user123", Role.USER, "Default User");
                defaultUser.setEmail("user@skilora.com");
                defaultUser.setVerified(true);
                defaultUser.setActive(true);
                userService.create(defaultUser);
                logger.info("Created default 'user' account.");
            } else {
                User user = existingUser.get();
                user.setPassword("user123");
                user.setActive(true);
                user.setVerified(true);
                userService.update(user);
                logger.info("Synchronized 'user' account password.");
            }

            // Ensure 'admin' account exists with correct password
            Optional<User> existingAdmin = userService.findByUsername("admin");
            if (!existingAdmin.isPresent()) {
                User defaultAdmin = new User("admin", "admin123", Role.ADMIN, "Administrator");
                defaultAdmin.setEmail("admin@skilora.com");
                defaultAdmin.setVerified(true);
                defaultAdmin.setActive(true);
                userService.create(defaultAdmin);
                logger.info("Created default 'admin' account.");
            } else {
                User admin = existingAdmin.get();
                admin.setPassword("admin123");
                admin.setActive(true);
                admin.setVerified(true);
                userService.update(admin);
                logger.info("Synchronized 'admin' account password.");
            }
            
            // Always reset lockout for default user account on startup
            try {
                authService.resetLockout("user");
                logger.debug("Reset lockout for default user account 'user'");
            } catch (Exception e) {
                logger.debug("Could not reset lockout for default user (this is normal if login_attempts table doesn't exist): {}", e.getMessage());
            }
            
        } catch (SQLException e) {
            logger.error("Error seeding default users: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if a user with the given username exists in the database.
     */
    private static boolean userExists(Connection conn, String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.debug("Error checking if user exists: {}", e.getMessage());
            return false;
        }
        return false;
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
}
