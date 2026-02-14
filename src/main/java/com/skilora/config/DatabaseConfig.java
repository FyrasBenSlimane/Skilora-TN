package com.skilora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

    private HikariDataSource dataSource;

    private DatabaseConfig() {
        String url = System.getenv("SKILORA_DB_URL");
        if (url == null || url.isBlank())
            url = DEFAULT_URL;

        String user = System.getenv("SKILORA_DB_USER");
        if (user == null || user.isBlank())
            user = DEFAULT_USER;

        String password = System.getenv("SKILORA_DB_PASSWORD");
        if (password == null)
            password = DEFAULT_PASSWORD;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        // Performance Tuning
        config.setMaximumPoolSize(10); // Standard for desktop apps
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(5000); // 5s timeout
        config.setValidationTimeout(3000); // 3s validation timeout
        config.setConnectionTestQuery("SELECT 1"); // Test query
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        // Additional connection properties for better error handling
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("failOverReadOnly", "false");
        config.addDataSourceProperty("maxReconnects", "3");
        config.addDataSourceProperty("initialTimeout", "2");

        try {
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection. " +
                    "Please ensure MySQL is running and accessible at: " + url + 
                    "\nError: " + e.getMessage(), e);
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
