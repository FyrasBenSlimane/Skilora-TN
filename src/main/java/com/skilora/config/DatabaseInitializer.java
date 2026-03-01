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
            boolean hasInterviews = tableExists(stmt, "interviews");
            boolean hasHireOffers = tableExists(stmt, "hire_offers");
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

            // Create interviews table if missing
            if (!hasInterviews) {
                createInterviewsTable(stmt);
            }

            // Create hire_offers table if missing
            if (!hasHireOffers) {
                createHireOffersTable(stmt);
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

            // Create community module tables if missing
            createCommunityTables(stmt);

            // Create formation & certification module tables if missing
            createFormationTables(stmt);

            // Create support module tables if missing
            createSupportTables(stmt);

            // Create finance module tables if missing
            createFinanceTables(stmt);

            // Add missing columns to users table
            ensureUsersTableColumns(stmt);

            // Create performance indexes on foreign keys
            createIndexes(stmt);

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

    private static void createInterviewsTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS interviews (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        application_id INT NOT NULL,
                        scheduled_date DATETIME NOT NULL,
                        duration_minutes INT DEFAULT 60,
                        type VARCHAR(20) DEFAULT 'VIDEO',
                        location VARCHAR(255),
                        video_link TEXT,
                        notes TEXT,
                        status VARCHAR(20) DEFAULT 'SCHEDULED',
                        feedback TEXT,
                        rating INT DEFAULT 0,
                        timezone VARCHAR(50) DEFAULT 'Africa/Tunis',
                        created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'interviews' table.");
        } catch (SQLException e) {
            logger.error("Error creating interviews table: {}", e.getMessage(), e);
        }
    }

    private static void createHireOffersTable(Statement stmt) {
        try {
            String sql = """
                    CREATE TABLE IF NOT EXISTS hire_offers (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        application_id INT NOT NULL,
                        salary_offered DECIMAL(12,2) NOT NULL,
                        currency VARCHAR(10) DEFAULT 'TND',
                        start_date DATE,
                        contract_type VARCHAR(20) DEFAULT 'CDI',
                        benefits TEXT,
                        status VARCHAR(20) DEFAULT 'PENDING',
                        created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                        responded_date DATETIME,
                        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
                    )
                    """;
            stmt.execute(sql);
            logger.info("Created 'hire_offers' table.");
        } catch (SQLException e) {
            logger.error("Error creating hire_offers table: {}", e.getMessage(), e);
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

    private static void createCommunityTables(Statement stmt) {
        // Professional Connections
        if (!tableExists(stmt, "connections")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS connections (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id_1 INT NOT NULL,
                            user_id_2 INT NOT NULL,
                            status VARCHAR(20) DEFAULT 'PENDING',
                            connection_type VARCHAR(30) DEFAULT 'PROFESSIONAL',
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            last_interaction DATETIME,
                            strength_score INT DEFAULT 0,
                            UNIQUE KEY uq_connection (user_id_1, user_id_2),
                            FOREIGN KEY (user_id_1) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id_2) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'connections' table.");
            } catch (SQLException e) {
                logger.error("Error creating connections table: {}", e.getMessage(), e);
            }
        }

        // Social Posts
        if (!tableExists(stmt, "posts")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS posts (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            author_id INT NOT NULL,
                            content TEXT NOT NULL,
                            image_url TEXT,
                            post_type VARCHAR(30) DEFAULT 'STATUS',
                            likes_count INT DEFAULT 0,
                            comments_count INT DEFAULT 0,
                            shares_count INT DEFAULT 0,
                            is_published BOOLEAN DEFAULT TRUE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'posts' table.");
            } catch (SQLException e) {
                logger.error("Error creating posts table: {}", e.getMessage(), e);
            }
        }

        // Post Comments
        if (!tableExists(stmt, "post_comments")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS post_comments (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            post_id INT NOT NULL,
                            author_id INT NOT NULL,
                            content TEXT NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                            FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'post_comments' table.");
            } catch (SQLException e) {
                logger.error("Error creating post_comments table: {}", e.getMessage(), e);
            }
        }

        // Post Likes
        if (!tableExists(stmt, "post_likes")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS post_likes (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            post_id INT NOT NULL,
                            user_id INT NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_post_like (post_id, user_id),
                            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'post_likes' table.");
            } catch (SQLException e) {
                logger.error("Error creating post_likes table: {}", e.getMessage(), e);
            }
        }

        // Conversations
        if (!tableExists(stmt, "conversations")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS conversations (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            participant_1 INT NOT NULL,
                            participant_2 INT NOT NULL,
                            last_message_date DATETIME,
                            is_archived_1 BOOLEAN DEFAULT FALSE,
                            is_archived_2 BOOLEAN DEFAULT FALSE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_conversation (participant_1, participant_2),
                            FOREIGN KEY (participant_1) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (participant_2) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'conversations' table.");
            } catch (SQLException e) {
                logger.error("Error creating conversations table: {}", e.getMessage(), e);
            }
        }

        // Messages
        if (!tableExists(stmt, "messages")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            conversation_id INT NOT NULL,
                            sender_id INT NOT NULL,
                            content TEXT,
                            message_type VARCHAR(10) DEFAULT 'TEXT',
                            media_url TEXT,
                            file_name VARCHAR(255),
                            duration INT DEFAULT 0,
                            is_read BOOLEAN DEFAULT FALSE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'messages' table.");
            } catch (SQLException e) {
                logger.error("Error creating messages table: {}", e.getMessage(), e);
            }
        }

        // Migration : ajouter les colonnes média si elles n'existent pas (pour les BDD
        // existantes)
        try {
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS message_type VARCHAR(10) DEFAULT 'TEXT'");
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS media_url TEXT");
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_name VARCHAR(255)");
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS duration INT DEFAULT 0");
            stmt.execute("ALTER TABLE messages MODIFY COLUMN content TEXT");
        } catch (SQLException e) {
            // Ignorer si les colonnes existent déjà
            logger.debug("Media columns migration: {}", e.getMessage());
        }

        // Typing Status (indicateur "en train d'écrire")
        if (!tableExists(stmt, "typing_status")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS typing_status (
                            user_id INT NOT NULL,
                            conversation_id INT NOT NULL,
                            last_typed DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, conversation_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'typing_status' table.");
            } catch (SQLException e) {
                logger.error("Error creating typing_status table: {}", e.getMessage(), e);
            }
        }

        // User Online Status (heartbeat présence en ligne)
        if (!tableExists(stmt, "user_online_status")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS user_online_status (
                            user_id INT NOT NULL PRIMARY KEY,
                            last_seen DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'user_online_status' table.");
            } catch (SQLException e) {
                logger.error("Error creating user_online_status table: {}", e.getMessage(), e);
            }
        }

        // Events
        if (!tableExists(stmt, "events")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS events (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            organizer_id INT NOT NULL,
                            title VARCHAR(200) NOT NULL,
                            description TEXT,
                            event_type VARCHAR(30) DEFAULT 'MEETUP',
                            location VARCHAR(255),
                            is_online BOOLEAN DEFAULT FALSE,
                            online_link TEXT,
                            start_date DATETIME NOT NULL,
                            end_date DATETIME,
                            max_attendees INT DEFAULT 0,
                            current_attendees INT DEFAULT 0,
                            image_url TEXT,
                            status VARCHAR(20) DEFAULT 'UPCOMING',
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'events' table.");
            } catch (SQLException e) {
                logger.error("Error creating events table: {}", e.getMessage(), e);
            }
        }

        // Event RSVPs
        if (!tableExists(stmt, "event_rsvps")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS event_rsvps (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            event_id INT NOT NULL,
                            user_id INT NOT NULL,
                            status VARCHAR(20) DEFAULT 'GOING',
                            rsvp_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_rsvp (event_id, user_id),
                            FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'event_rsvps' table.");
            } catch (SQLException e) {
                logger.error("Error creating event_rsvps table: {}", e.getMessage(), e);
            }
        }

        // Mentorships
        if (!tableExists(stmt, "mentorships")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS mentorships (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            mentor_id INT NOT NULL,
                            mentee_id INT NOT NULL,
                            status VARCHAR(20) DEFAULT 'PENDING',
                            topic VARCHAR(200),
                            goals TEXT,
                            start_date DATE,
                            end_date DATE,
                            rating INT DEFAULT 0,
                            feedback TEXT,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_mentorship (mentor_id, mentee_id),
                            FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (mentee_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'mentorships' table.");
            } catch (SQLException e) {
                logger.error("Error creating mentorships table: {}", e.getMessage(), e);
            }
        }

        // Blog Articles
        if (!tableExists(stmt, "blog_articles")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS blog_articles (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            author_id INT NOT NULL,
                            title VARCHAR(300) NOT NULL,
                            content TEXT NOT NULL,
                            summary TEXT,
                            cover_image_url TEXT,
                            category VARCHAR(50),
                            tags VARCHAR(500),
                            views_count INT DEFAULT 0,
                            likes_count INT DEFAULT 0,
                            is_published BOOLEAN DEFAULT FALSE,
                            published_date DATETIME,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'blog_articles' table.");
            } catch (SQLException e) {
                logger.error("Error creating blog_articles table: {}", e.getMessage(), e);
            }
        }

        // Community Groups
        if (!tableExists(stmt, "community_groups")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS community_groups (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(150) NOT NULL,
                            description TEXT,
                            category VARCHAR(50),
                            cover_image_url TEXT,
                            creator_id INT NOT NULL,
                            member_count INT DEFAULT 1,
                            is_public BOOLEAN DEFAULT TRUE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'community_groups' table.");
            } catch (SQLException e) {
                logger.error("Error creating community_groups table: {}", e.getMessage(), e);
            }
        }

        // Group Members
        if (!tableExists(stmt, "group_members")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS group_members (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            group_id INT NOT NULL,
                            user_id INT NOT NULL,
                            role VARCHAR(20) DEFAULT 'MEMBER',
                            joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_group_member (group_id, user_id),
                            FOREIGN KEY (group_id) REFERENCES community_groups(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'group_members' table.");
            } catch (SQLException e) {
                logger.error("Error creating group_members table: {}", e.getMessage(), e);
            }
        }

        // Group Messages
        if (!tableExists(stmt, "group_messages")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS group_messages (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            group_id INT NOT NULL,
                            sender_id INT NOT NULL,
                            content TEXT NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (group_id) REFERENCES community_groups(id) ON DELETE CASCADE,
                            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'group_messages' table.");
            } catch (SQLException e) {
                logger.error("Error creating group_messages table: {}", e.getMessage(), e);
            }
        }

        try {
            stmt.execute("ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS message_type VARCHAR(10) DEFAULT 'TEXT'");
            stmt.execute("ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS media_url TEXT");
            stmt.execute("ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS file_name VARCHAR(255)");
            stmt.execute("ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS duration INT DEFAULT 0");
        } catch (SQLException e) {
            logger.debug("Media columns migration for group_messages: {}", e.getMessage());
        }

        // Group Message Reads (tracks which users have seen which group messages)
        if (!tableExists(stmt, "group_message_reads")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS group_message_reads (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            message_id INT NOT NULL,
                            user_id INT NOT NULL,
                            read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_read (message_id, user_id),
                            FOREIGN KEY (message_id) REFERENCES group_messages(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'group_message_reads' table.");
            } catch (SQLException e) {
                logger.error("Error creating group_message_reads table: {}", e.getMessage(), e);
            }
        }

        // Message Reactions (private messages)
        if (!tableExists(stmt, "message_reactions")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS message_reactions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            message_id INT NOT NULL,
                            user_id INT NOT NULL,
                            emoji VARCHAR(10) NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_reaction (message_id, user_id, emoji),
                            FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'message_reactions' table.");
            } catch (SQLException e) {
                logger.error("Error creating message_reactions table: {}", e.getMessage(), e);
            }
        }

        // Group Message Reactions
        if (!tableExists(stmt, "group_message_reactions")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS group_message_reactions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            message_id INT NOT NULL,
                            user_id INT NOT NULL,
                            emoji VARCHAR(10) NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_reaction (message_id, user_id, emoji),
                            FOREIGN KEY (message_id) REFERENCES group_messages(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'group_message_reactions' table.");
            } catch (SQLException e) {
                logger.error("Error creating group_message_reactions table: {}", e.getMessage(), e);
            }
        }

        // Achievements
        if (!tableExists(stmt, "achievements")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS achievements (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            badge_type VARCHAR(50) NOT NULL,
                            title VARCHAR(100) NOT NULL,
                            description VARCHAR(255),
                            icon_url TEXT,
                            earned_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            rarity VARCHAR(20) DEFAULT 'COMMON',
                            points INT DEFAULT 10,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'achievements' table.");
            } catch (SQLException e) {
                logger.error("Error creating achievements table: {}", e.getMessage(), e);
            }
        }
    }

    private static void createSupportTables(Statement stmt) {
        // Support Tickets
        if (!tableExists(stmt, "support_tickets")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS support_tickets (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            category VARCHAR(50) NOT NULL,
                            priority VARCHAR(20) DEFAULT 'MEDIUM',
                            status VARCHAR(20) DEFAULT 'OPEN',
                            subject VARCHAR(255) NOT NULL,
                            description TEXT,
                            assigned_to INT,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            resolved_date DATETIME,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
                        )
                        """);
                logger.info("Created 'support_tickets' table.");
            } catch (SQLException e) {
                logger.error("Error creating support_tickets table: {}", e.getMessage(), e);
            }
        }

        // Ticket Messages
        if (!tableExists(stmt, "ticket_messages")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ticket_messages (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            ticket_id INT NOT NULL,
                            sender_id INT NOT NULL,
                            message TEXT NOT NULL,
                            is_internal BOOLEAN DEFAULT FALSE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
                            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'ticket_messages' table.");
            } catch (SQLException e) {
                logger.error("Error creating ticket_messages table: {}", e.getMessage(), e);
            }
        }

        // FAQ Articles
        if (!tableExists(stmt, "faq_articles")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS faq_articles (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            category VARCHAR(50) NOT NULL,
                            question TEXT NOT NULL,
                            answer TEXT NOT NULL,
                            language VARCHAR(5) DEFAULT 'fr',
                            helpful_count INT DEFAULT 0,
                            view_count INT DEFAULT 0,
                            is_published BOOLEAN DEFAULT TRUE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """);
                logger.info("Created 'faq_articles' table.");
            } catch (SQLException e) {
                logger.error("Error creating faq_articles table: {}", e.getMessage(), e);
            }
        }

        // Chatbot Conversations
        if (!tableExists(stmt, "chatbot_conversations")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS chatbot_conversations (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            started_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            ended_date DATETIME,
                            status VARCHAR(20) DEFAULT 'ACTIVE',
                            escalated_to_ticket_id INT,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (escalated_to_ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL
                        )
                        """);
                logger.info("Created 'chatbot_conversations' table.");
            } catch (SQLException e) {
                logger.error("Error creating chatbot_conversations table: {}", e.getMessage(), e);
            }
        }

        // Chatbot Messages
        if (!tableExists(stmt, "chatbot_messages")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS chatbot_messages (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            conversation_id INT NOT NULL,
                            sender VARCHAR(10) NOT NULL,
                            message TEXT NOT NULL,
                            intent VARCHAR(100),
                            confidence DECIMAL(5,4),
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (conversation_id) REFERENCES chatbot_conversations(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'chatbot_messages' table.");
            } catch (SQLException e) {
                logger.error("Error creating chatbot_messages table: {}", e.getMessage(), e);
            }
        }

        // Auto Responses
        if (!tableExists(stmt, "auto_responses")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS auto_responses (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            trigger_keyword VARCHAR(100) NOT NULL,
                            response_text TEXT NOT NULL,
                            category VARCHAR(50),
                            language VARCHAR(5) DEFAULT 'fr',
                            is_active BOOLEAN DEFAULT TRUE,
                            usage_count INT DEFAULT 0,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                logger.info("Created 'auto_responses' table.");

                // Insert seed data for auto_responses
                insertAutoResponsesSeedData(stmt);
            } catch (SQLException e) {
                logger.error("Error creating auto_responses table: {}", e.getMessage(), e);
            }
        }

        // User Feedback
        if (!tableExists(stmt, "user_feedback")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS user_feedback (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            feedback_type VARCHAR(30) NOT NULL,
                            rating INT DEFAULT 0,
                            comment TEXT,
                            category VARCHAR(50),
                            is_resolved BOOLEAN DEFAULT FALSE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'user_feedback' table.");
            } catch (SQLException e) {
                logger.error("Error creating user_feedback table: {}", e.getMessage(), e);
            }
        }

        // Insert FAQ seed data
        insertFAQSeedData(stmt);
    }

    private static void insertFAQSeedData(Statement stmt) {
        try {
            // Check if FAQ articles already exist
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM faq_articles");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(
                        """
                                INSERT INTO faq_articles (category, question, answer, language) VALUES
                                ('account', 'Comment créer un compte ?', 'Cliquez sur "S''inscrire" sur la page de connexion et remplissez le formulaire avec vos informations personnelles.', 'fr'),
                                ('account', 'Comment réinitialiser mon mot de passe ?', 'Cliquez sur "Mot de passe oublié" sur la page de connexion et suivez les instructions envoyées par email.', 'fr'),
                                ('job', 'Comment postuler à une offre ?', 'Allez dans la section "Offres d''emploi", trouvez l''offre qui vous intéresse et cliquez sur "Postuler".', 'fr'),
                                ('formation', 'Les formations sont-elles gratuites ?', 'Certaines formations sont gratuites, d''autres sont payantes. Consultez la description de chaque formation pour plus de détails.', 'fr'),
                                ('technical', 'Comment contacter le support ?', 'Vous pouvez créer un ticket de support depuis cette page ou utiliser l''assistant virtuel pour obtenir une aide immédiate.', 'fr')
                                """);
                logger.info("Inserted FAQ seed data.");
            }
        } catch (SQLException e) {
            logger.debug("Could not insert FAQ seed data: {}", e.getMessage());
        }
    }

    private static void insertAutoResponsesSeedData(Statement stmt) {
        try {
            // Check if auto responses already exist
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM auto_responses");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(
                        """
                                INSERT INTO auto_responses (trigger_keyword, response_text, category, language) VALUES
                                ('mot de passe', 'Pour réinitialiser votre mot de passe, allez dans Paramètres > Sécurité ou cliquez sur "Mot de passe oublié" sur la page de connexion.', 'account', 'fr'),
                                ('inscription', 'Pour vous inscrire, cliquez sur le bouton "S''inscrire" sur la page de connexion et remplissez le formulaire.', 'account', 'fr'),
                                ('formation', 'Nous proposons des formations gratuites et payantes. Consultez la section Formations pour voir le catalogue complet.', 'formation', 'fr'),
                                ('emploi', 'Pour trouver un emploi, consultez notre fil d''actualités emploi ou la section Offres d''emploi.', 'job', 'fr'),
                                ('contact', 'Vous pouvez nous contacter via ce chat ou en créant un ticket de support. Notre équipe vous répondra dans les 24 heures.', 'general', 'fr')
                                """);
                logger.info("Inserted auto_responses seed data.");
            }
        } catch (SQLException e) {
            logger.debug("Could not insert auto_responses seed data: {}", e.getMessage());
        }
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
                "CREATE INDEX idx_interviews_application ON interviews(application_id)",
                "CREATE INDEX idx_interviews_status ON interviews(status)",
                "CREATE INDEX idx_interviews_date ON interviews(scheduled_date)",
                "CREATE INDEX idx_hire_offers_application ON hire_offers(application_id)",
                "CREATE INDEX idx_hire_offers_status ON hire_offers(status)",
                "CREATE INDEX idx_login_attempts_username ON login_attempts(username, attempted_at)",
                "CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id, used)",
                "CREATE INDEX idx_companies_owner ON companies(owner_id)",
                "CREATE INDEX idx_users_email ON users(email)",
                // Community module indexes
                "CREATE INDEX idx_connections_user1 ON connections(user_id_1)",
                "CREATE INDEX idx_connections_user2 ON connections(user_id_2)",
                "CREATE INDEX idx_connections_status ON connections(status)",
                "CREATE INDEX idx_posts_author ON posts(author_id)",
                "CREATE INDEX idx_posts_date ON posts(created_date)",
                "CREATE INDEX idx_post_comments_post ON post_comments(post_id)",
                "CREATE INDEX idx_post_likes_post ON post_likes(post_id)",
                "CREATE INDEX idx_conversations_p1 ON conversations(participant_1)",
                "CREATE INDEX idx_conversations_p2 ON conversations(participant_2)",
                "CREATE INDEX idx_messages_conv ON messages(conversation_id)",
                "CREATE INDEX idx_messages_sender ON messages(sender_id)",
                "CREATE INDEX idx_events_organizer ON events(organizer_id)",
                "CREATE INDEX idx_events_date ON events(start_date)",
                "CREATE INDEX idx_event_rsvps_event ON event_rsvps(event_id)",
                "CREATE INDEX idx_mentorships_mentor ON mentorships(mentor_id)",
                "CREATE INDEX idx_mentorships_mentee ON mentorships(mentee_id)",
                "CREATE INDEX idx_blog_author ON blog_articles(author_id)",
                "CREATE INDEX idx_blog_published ON blog_articles(is_published, published_date)",
                "CREATE INDEX idx_group_members_group ON group_members(group_id)",
                "CREATE INDEX idx_group_members_user ON group_members(user_id)",
                "CREATE INDEX idx_achievements_user ON achievements(user_id)",
                // Finance module indexes
                "CREATE INDEX idx_contracts_user ON employment_contracts(user_id)",
                "CREATE INDEX idx_contracts_employer ON employment_contracts(employer_id)",
                "CREATE INDEX idx_contracts_status ON employment_contracts(status)",
                "CREATE INDEX idx_payslips_contract ON payslips(contract_id)",
                "CREATE INDEX idx_payslips_user ON payslips(user_id)",
                "CREATE INDEX idx_payslips_period ON payslips(period_year, period_month)",
                "CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id)",
                "CREATE INDEX idx_salary_history_contract ON salary_history(contract_id)",
                "CREATE INDEX idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency)",
                "CREATE INDEX idx_transactions_payslip ON payment_transactions(payslip_id)",
                "CREATE INDEX idx_tax_config_country ON tax_configurations(country, tax_type)",
                // Support module indexes
                "CREATE INDEX idx_tickets_user ON support_tickets(user_id)",
                "CREATE INDEX idx_tickets_status ON support_tickets(status)",
                "CREATE INDEX idx_tickets_priority ON support_tickets(priority)",
                "CREATE INDEX idx_tickets_assigned ON support_tickets(assigned_to)",
                "CREATE INDEX idx_ticket_messages_ticket ON ticket_messages(ticket_id)",
                "CREATE INDEX idx_chatbot_conv_user ON chatbot_conversations(user_id)",
                "CREATE INDEX idx_chatbot_msg_conv ON chatbot_messages(conversation_id)",
                "CREATE INDEX idx_faq_category ON faq_articles(category)",
                "CREATE INDEX idx_feedback_user ON user_feedback(user_id)"
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

    /**
     * Create Finance & Remuneration module tables
     */
    private static void createFinanceTables(Statement stmt) {
        if (!tableExists(stmt, "employment_contracts")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS employment_contracts (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            employer_id INT,
                            job_offer_id INT,
                            salary_base DECIMAL(12,2) NOT NULL,
                            currency VARCHAR(10) DEFAULT 'TND',
                            start_date DATE NOT NULL,
                            end_date DATE,
                            contract_type VARCHAR(20) DEFAULT 'CDI',
                            status VARCHAR(20) DEFAULT 'DRAFT',
                            pdf_url TEXT,
                            is_signed BOOLEAN DEFAULT FALSE,
                            signed_date DATETIME,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (employer_id) REFERENCES users(id) ON DELETE SET NULL,
                            FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE SET NULL
                        )
                        """);
                logger.info("Created 'employment_contracts' table.");
            } catch (SQLException e) {
                logger.error("Error creating employment_contracts table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "payslips")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS payslips (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            contract_id INT NOT NULL,
                            user_id INT NOT NULL,
                            period_month INT NOT NULL,
                            period_year INT NOT NULL,
                            gross_salary DECIMAL(12,2) NOT NULL,
                            net_salary DECIMAL(12,2) NOT NULL,
                            cnss_employee DECIMAL(10,2) DEFAULT 0.00,
                            cnss_employer DECIMAL(10,2) DEFAULT 0.00,
                            irpp DECIMAL(10,2) DEFAULT 0.00,
                            other_deductions DECIMAL(10,2) DEFAULT 0.00,
                            bonuses DECIMAL(10,2) DEFAULT 0.00,
                            currency VARCHAR(10) DEFAULT 'TND',
                            payment_status VARCHAR(20) DEFAULT 'PENDING',
                            payment_date DATETIME,
                            pdf_url TEXT,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_payslip_period (contract_id, period_month, period_year),
                            FOREIGN KEY (contract_id) REFERENCES employment_contracts(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'payslips' table.");
            } catch (SQLException e) {
                logger.error("Error creating payslips table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "bank_accounts")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS bank_accounts (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            bank_name VARCHAR(100) NOT NULL,
                            account_holder VARCHAR(150) NOT NULL,
                            iban VARCHAR(34),
                            swift_bic VARCHAR(11),
                            rib VARCHAR(24),
                            currency VARCHAR(10) DEFAULT 'TND',
                            is_primary BOOLEAN DEFAULT FALSE,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'bank_accounts' table.");
            } catch (SQLException e) {
                logger.error("Error creating bank_accounts table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "salary_history")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS salary_history (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            contract_id INT NOT NULL,
                            old_salary DECIMAL(12,2),
                            new_salary DECIMAL(12,2) NOT NULL,
                            reason VARCHAR(255),
                            effective_date DATE NOT NULL,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (contract_id) REFERENCES employment_contracts(id) ON DELETE CASCADE
                        )
                        """);
                logger.info("Created 'salary_history' table.");
            } catch (SQLException e) {
                logger.error("Error creating salary_history table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "exchange_rates")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS exchange_rates (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            from_currency VARCHAR(10) NOT NULL,
                            to_currency VARCHAR(10) NOT NULL,
                            rate DECIMAL(12,6) NOT NULL,
                            rate_date DATE NOT NULL,
                            source VARCHAR(50) DEFAULT 'BCT',
                            last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_rate (from_currency, to_currency, rate_date)
                        )
                        """);
                logger.info("Created 'exchange_rates' table.");
                insertExchangeRateSeedData(stmt);
            } catch (SQLException e) {
                logger.error("Error creating exchange_rates table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "payment_transactions")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS payment_transactions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            payslip_id INT,
                            from_account_id INT,
                            to_account_id INT,
                            amount DECIMAL(12,2) NOT NULL,
                            currency VARCHAR(10) DEFAULT 'TND',
                            transaction_type VARCHAR(30) DEFAULT 'SALARY',
                            status VARCHAR(20) DEFAULT 'PENDING',
                            reference VARCHAR(50),
                            transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            notes TEXT,
                            FOREIGN KEY (payslip_id) REFERENCES payslips(id) ON DELETE SET NULL,
                            FOREIGN KEY (from_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL,
                            FOREIGN KEY (to_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL
                        )
                        """);
                logger.info("Created 'payment_transactions' table.");
            } catch (SQLException e) {
                logger.error("Error creating payment_transactions table: {}", e.getMessage(), e);
            }
        }

        if (!tableExists(stmt, "tax_configurations")) {
            try {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS tax_configurations (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            country VARCHAR(50) NOT NULL,
                            tax_type VARCHAR(50) NOT NULL,
                            rate DECIMAL(8,4) NOT NULL,
                            min_bracket DECIMAL(12,2) DEFAULT 0.00,
                            max_bracket DECIMAL(12,2),
                            description VARCHAR(255),
                            effective_date DATE NOT NULL,
                            is_active BOOLEAN DEFAULT TRUE
                        )
                        """);
                logger.info("Created 'tax_configurations' table.");
                insertTaxConfigSeedData(stmt);
            } catch (SQLException e) {
                logger.error("Error creating tax_configurations table: {}", e.getMessage(), e);
            }
        }
    }

    private static void insertExchangeRateSeedData(Statement stmt) {
        try {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM exchange_rates");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                        INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) VALUES
                        ('EUR', 'TND', 3.4000, CURDATE(), 'BCT'),
                        ('USD', 'TND', 3.1500, CURDATE(), 'BCT'),
                        ('GBP', 'TND', 3.9500, CURDATE(), 'BCT'),
                        ('TND', 'EUR', 0.2941, CURDATE(), 'BCT'),
                        ('TND', 'USD', 0.3175, CURDATE(), 'BCT')
                        """);
                logger.info("Inserted exchange rate seed data.");
            }
        } catch (SQLException e) {
            logger.debug("Could not insert exchange rate seed data: {}", e.getMessage());
        }
    }

    private static void insertTaxConfigSeedData(Statement stmt) {
        try {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM tax_configurations");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(
                        """
                                INSERT INTO tax_configurations (country, tax_type, rate, min_bracket, max_bracket, description, effective_date, is_active) VALUES
                                ('Tunisia', 'IRPP', 0.0000, 0.00, 5000.00, 'Tranche exoneree', '2025-01-01', TRUE),
                                ('Tunisia', 'IRPP', 0.2600, 5000.01, 20000.00, 'Tranche 26%', '2025-01-01', TRUE),
                                ('Tunisia', 'IRPP', 0.2800, 20000.01, 30000.00, 'Tranche 28%', '2025-01-01', TRUE),
                                ('Tunisia', 'IRPP', 0.3200, 30000.01, 50000.00, 'Tranche 32%', '2025-01-01', TRUE),
                                ('Tunisia', 'IRPP', 0.3500, 50000.01, NULL, 'Tranche 35%', '2025-01-01', TRUE),
                                ('Tunisia', 'CNSS_EMPLOYEE', 0.0918, 0.00, NULL, 'CNSS part salariale 9.18%', '2025-01-01', TRUE),
                                ('Tunisia', 'CNSS_EMPLOYER', 0.1657, 0.00, NULL, 'CNSS part patronale 16.57%', '2025-01-01', TRUE)
                                """);
                logger.info("Inserted tax configuration seed data.");
            }
        } catch (SQLException e) {
            logger.debug("Could not insert tax config seed data: {}", e.getMessage());
        }
    }

    /**
     * Create Formation & Certification module tables
     */
    private static void createFormationTables(Statement stmt) {
        try {
            // Create formations table
            if (!tableExists(stmt, "formations")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS formations (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            title VARCHAR(200) NOT NULL,
                            description TEXT,
                            category VARCHAR(50) NOT NULL,
                            duration_hours INT DEFAULT 0,
                            cost DECIMAL(10,2) DEFAULT 0.00,
                            currency VARCHAR(10) DEFAULT 'TND',
                            provider VARCHAR(100),
                            image_url TEXT,
                            level VARCHAR(30) DEFAULT 'BEGINNER',
                            is_free BOOLEAN DEFAULT TRUE,
                            created_by INT,
                            created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            status VARCHAR(20) DEFAULT 'ACTIVE',
                            FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'formations' table.");
            }

            // Create formation_modules table
            if (!tableExists(stmt, "formation_modules")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS formation_modules (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            formation_id INT NOT NULL,
                            title VARCHAR(200) NOT NULL,
                            description TEXT,
                            content_url TEXT,
                            duration_minutes INT DEFAULT 0,
                            order_index INT DEFAULT 0,
                            FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'formation_modules' table.");
            }

            // Create enrollments table
            if (!tableExists(stmt, "enrollments")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS enrollments (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            formation_id INT NOT NULL,
                            user_id INT NOT NULL,
                            status VARCHAR(30) DEFAULT 'IN_PROGRESS',
                            progress DECIMAL(5,2) DEFAULT 0.00,
                            enrolled_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            completed_date DATETIME,
                            UNIQUE KEY uq_enrollment (formation_id, user_id),
                            FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'enrollments' table.");
            }

            // Create quizzes table
            if (!tableExists(stmt, "quizzes")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS quizzes (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            formation_id INT NOT NULL,
                            module_id INT,
                            title VARCHAR(200) NOT NULL,
                            description TEXT,
                            pass_score INT DEFAULT 70,
                            max_attempts INT DEFAULT 3,
                            time_limit_minutes INT DEFAULT 30,
                            FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE,
                            FOREIGN KEY (module_id) REFERENCES formation_modules(id) ON DELETE SET NULL
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'quizzes' table.");
            }

            // Create quiz_questions table
            if (!tableExists(stmt, "quiz_questions")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS quiz_questions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            quiz_id INT NOT NULL,
                            question_text TEXT NOT NULL,
                            option_a VARCHAR(500),
                            option_b VARCHAR(500),
                            option_c VARCHAR(500),
                            option_d VARCHAR(500),
                            correct_option CHAR(1) NOT NULL,
                            points INT DEFAULT 1,
                            order_index INT DEFAULT 0,
                            FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'quiz_questions' table.");
            }

            // Create quiz_results table
            if (!tableExists(stmt, "quiz_results")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS quiz_results (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            quiz_id INT NOT NULL,
                            user_id INT NOT NULL,
                            score INT DEFAULT 0,
                            max_score INT DEFAULT 0,
                            passed BOOLEAN DEFAULT FALSE,
                            attempt_number INT DEFAULT 1,
                            taken_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            time_spent_seconds INT DEFAULT 0,
                            FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'quiz_results' table.");
            }

            // Create certificates table
            if (!tableExists(stmt, "certificates")) {
                String sql = """
                        CREATE TABLE IF NOT EXISTS certificates (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            enrollment_id INT NOT NULL UNIQUE,
                            certificate_number VARCHAR(50) NOT NULL UNIQUE,
                            issued_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                            qr_code TEXT,
                            hash_value VARCHAR(128),
                            pdf_url TEXT,
                            FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE
                        )
                        """;
                stmt.execute(sql);
                logger.info("Created 'certificates' table.");
            }

            // Create indexes for formation tables
            createFormationIndexes(stmt);

        } catch (SQLException e) {
            logger.error("Error creating formation tables: {}", e.getMessage(), e);
        }
    }

    /**
     * Create indexes for formation module tables
     */
    private static void createFormationIndexes(Statement stmt) {
        String[] indexes = {
                "CREATE INDEX idx_enrollments_user ON enrollments(user_id)",
                "CREATE INDEX idx_enrollments_formation ON enrollments(formation_id)",
                "CREATE INDEX idx_quiz_results_user ON quiz_results(user_id)",
                "CREATE INDEX idx_quiz_results_quiz ON quiz_results(quiz_id)",
                "CREATE INDEX idx_formations_category ON formations(category)",
                "CREATE INDEX idx_formations_status ON formations(status)"
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
            logger.info("Created {} formation module indexes.", created);
        }
    }
}
