package com.skilora;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple utility to verify database connection and schema.
 */
public class DatabaseVerifier {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseVerifier.class);

    public static void main(String[] args) {
        logger.info("Starting Database Verification...");
        
        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            
            if (dbConfig.isConnected()) {
                try (Connection conn = dbConfig.getConnection();
                     Statement stmt = conn.createStatement()) {
                    
                    // 1. Basic Connection Test
                    logger.info("Connection test (SELECT 1)...");
                    try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        if (rs.next()) {
                            logger.info("Basic connectivity: OK");
                        }
                    }
                    
                    // 2. Schema Check
                    logger.info("Checking key tables...");
                    String[] tables = {"users", "profiles", "companies", "job_offers", "trainings"};
                    for (String table : tables) {
                        try (ResultSet rs = conn.getMetaData().getTables(null, null, table, null)) {
                            if (rs.next()) {
                                logger.info("Table '{}': EXISTS", table);
                            } else {
                                logger.warn("Table '{}': MISSING", table);
                            }
                        }
                    }
                    
                    // 3. User Count
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                        if (rs.next()) {
                            logger.info("Total users in database: {}", rs.getInt(1));
                        }
                    }
                    
                    logger.info("Database verification completed successfully!");
                }
            } else {
                logger.error("Database connection could not be established.");
            }
        } catch (Exception e) {
            logger.error("Database verification failed with error: {}", e.getMessage(), e);
        } finally {
            DatabaseConfig.getInstance().closeConnection();
        }
    }
}
