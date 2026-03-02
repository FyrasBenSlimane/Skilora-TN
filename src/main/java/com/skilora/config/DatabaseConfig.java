package com.skilora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Configuration
 * 
 * Singleton pattern for database connection management.
 * Uses HikariCP connection pooling for high performance.
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/skilora";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private HikariDataSource dataSource;

    private DatabaseConfig() {
        String url = EnvConfig.get("SKILORA_DB_URL", DEFAULT_URL);
        String user = EnvConfig.get("SKILORA_DB_USER", DEFAULT_USER);
        String password = EnvConfig.get("SKILORA_DB_PASSWORD", DEFAULT_PASSWORD);

        // Increase MySQL max_allowed_packet for Base64-encoded profile photos
        // Must be done BEFORE creating the pool so all connections inherit the new value
        try (java.sql.Connection bootstrapConn = java.sql.DriverManager.getConnection(url, user, password);
             java.sql.Statement bootstrapStmt = bootstrapConn.createStatement()) {
            bootstrapStmt.execute("SET GLOBAL max_allowed_packet = 16777216"); // 16 MB
        } catch (java.sql.SQLException ignored) {
            // Non-fatal: may lack SUPER privilege in non-root environments
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        // Performance Tuning
        config.setMaximumPoolSize(10); // Standard for desktop apps
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(3000); // 3s fast fail
        // Don't crash app startup if DB is temporarily down (lazy acquire connections).
        config.setInitializationFailTimeout(-1);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            this.dataSource = new HikariDataSource(config);
        } catch (RuntimeException e) {
            // Hikari can throw PoolInitializationException (runtime) on fail-fast configurations.
            // With initializationFailTimeout=-1 this should be rare, but keep the app resilient.
            logger.error("Failed to initialize DB pool (url={} user={}): {}", url, user, e.getMessage(), e);
            throw e;
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getUrl() {
        return dataSource.getJdbcUrl();
    }

    public String getUser() {
        return dataSource.getUsername();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
