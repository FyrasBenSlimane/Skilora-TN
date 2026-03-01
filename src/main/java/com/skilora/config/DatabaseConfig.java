package com.skilora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Database Configuration
 * 
 * Singleton pattern for database connection management.
 * Uses HikariCP connection pooling for high performance.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;
    
    private static final String PROPERTIES_FILE = "database.properties";
    private HikariDataSource dataSource;

    private DatabaseConfig() {
        Properties props = loadProperties();
        
        // Configuration Priority: 
        // 1. Environment Variables
        // 2. properties file
        // 3. Fallback defaults
        
        String url = System.getenv("SKILORA_DB_URL");
        if (url == null || url.isBlank()) {
            url = props.getProperty("db.url", "jdbc:mysql://127.0.0.1:3306/skilora");
        }

        String user = System.getenv("SKILORA_DB_USER");
        if (user == null || user.isBlank()) {
            user = props.getProperty("db.user", "root");
        }

        String password = System.getenv("SKILORA_DB_PASSWORD");
        if (password == null) {
            password = props.getProperty("db.password", "");
        }

        logger.info("Initializing Database Connection to: {}", url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        
        // Connection Pool Settings
        config.setInitializationFailTimeout(5000); // 5s timeout on startup
        config.setValidationTimeout(3000);
        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        
        // MySQL Performance Tuning
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        try {
            this.dataSource = new HikariDataSource(config);
            testConnection();
        } catch (Exception e) {
            logger.error("CRITICAL: Could not initialize database pool: {}", e.getMessage());
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            } else {
                logger.warn("Database properties file '{}' not found in classpath. Using defaults/env.", PROPERTIES_FILE);
            }
        } catch (Exception e) {
            logger.error("Error loading database properties: {}", e.getMessage());
        }
        return props;
    }

    private void testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next() && rs.getInt(1) == 1) {
                logger.info("Successfully verified database connection.");
            }
        } catch (SQLException e) {
            logger.error("Database connection verification failed: {}", e.getMessage());
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Get a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
