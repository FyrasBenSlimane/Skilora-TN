package com.skilora.utils;

import com.skilora.config.DatabaseConfig;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;

public class DBDiagnostic {
    public static void main(String[] args) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("DB_DIAGNOSTIC.txt"))) {
            writer.println("=== SKILORA DB DIAGNOSTIC ===");
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                writer.println("Connection: SUCCESS");
                DatabaseMetaData meta = conn.getMetaData();
                writer.println("DB Product: " + meta.getDatabaseProductName());
                
                writer.println("\n--- Tables ---");
                try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        writer.println("- " + rs.getString("TABLE_NAME"));
                    }
                }
                
                writer.println("\n--- Users ---");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, username, role, is_active FROM users")) {
                    while (rs.next()) {
                        writer.println("User: " + rs.getString("username") + " | Role: " + rs.getString("role") + " | Active: " + rs.getBoolean("is_active"));
                    }
                }
            } catch (Exception e) {
                writer.println("Connection: FAILED");
                writer.println("Error: " + e.getMessage());
                e.printStackTrace(writer);
            }
            writer.println("\n=== END ===");
            System.out.println("Diagnostic written to DB_DIAGNOSTIC.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
