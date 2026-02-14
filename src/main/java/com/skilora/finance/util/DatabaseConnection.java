package com.skilora.finance.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton Database Connection Manager
 * Ensures only one database connection instance throughout the application
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    
    // Database configuration
    private static final String URL = "jdbc:mysql://localhost:3306/skilora?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Change this to your MySQL password
    
    // Private constructor (Singleton pattern)
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Database connection established successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            e.printStackTrace();
        }
    }
    
    /**
     * Get the single instance of DatabaseConnection (Singleton)
     */
    public static DatabaseConnection getInstance() {
        if (instance == null || !isConnectionValid()) {
            synchronized (DatabaseConnection.class) {
                if (instance == null || !isConnectionValid()) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the database connection
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * Check if connection is still valid
     */
    private static boolean isConnectionValid() {
        if (instance == null || instance.connection == null) {
            return false;
        }
        try {
            return !instance.connection.isClosed() && instance.connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Close the database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔌 Database connection closed.");
            } catch (SQLException e) {
                System.err.println("❌ Error closing connection!");
                e.printStackTrace();
            }
        }
    }
}
