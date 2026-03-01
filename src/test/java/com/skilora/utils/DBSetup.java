package com.skilora.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Utility to ensure the database 'skilora' exists on the server.
 */
public class DBSetup {
    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3306/?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "";

        System.out.println("Checking MySQL Server connection...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Connected to MySQL server Successfully!");
            
            System.out.println("Ensuring database 'skilora' exists...");
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS skilora");
            System.out.println("✅ Database 'skilora' is ready!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to connect or create database: " + e.getMessage());
            System.err.println("Please check if MySQL is running and if the credentials (root / no password) are correct.");
        }
    }
}
