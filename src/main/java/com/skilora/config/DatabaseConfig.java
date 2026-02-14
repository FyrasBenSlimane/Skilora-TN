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
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/skilora?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private HikariDataSource dataSource;
    private boolean isConnected = false;

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
        config.setConnectionTimeout(3000); // 3s fast fail
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            this.dataSource = new HikariDataSource(config);
            this.isConnected = true;
            System.out.println("✅ Database connected successfully");
        } catch (Exception e) {
            this.isConnected = false;
            System.err.println("\n⚠️  WARNING: Database connection failed (OFFLINE MODE ENABLED)");
            System.err.println("   URL: " + url);
            System.err.println("   Error: " + e.getMessage().split("\n")[0]);
            System.err.println("");
            System.err.println("   ℹ️  To fix:");
            System.err.println("   1. Start MySQL server (XAMPP or MySQL service)");
            System.err.println("   2. Create database: CREATE DATABASE skilora;");
            System.err.println("   3. Restart the application");
            System.err.println("");
            System.err.println("   The app will run in OFFLINE mode (limited functionality)\n");
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected || dataSource == null) {
            return null;
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public static boolean isDatabaseAvailable() {
        try {
            DatabaseConfig config = getInstance();
            return config.isConnected && config.dataSource != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getConnectionStatus() {
        if (isDatabaseAvailable()) {
            return "✅ DATABASE CONNECTED";
        } else {
            return "⚠️  OFFLINE MODE (Database unavailable)";
        }
    }

    public String getUrl() {
        return dataSource != null ? dataSource.getJdbcUrl() : "N/A";
    }

    public String getUser() {
        return dataSource != null ? dataSource.getUsername() : "N/A";
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
