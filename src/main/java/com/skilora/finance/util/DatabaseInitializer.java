package com.skilora.finance.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the database by executing the SQL script from resources.
 * This ensures all tables and seed data exist on startup.
 */
public class DatabaseInitializer {

    public static void initialize() {
        // Check if database is available
        if (!isConnected()) {
            System.out.println("⏭️  Skipping database initialization (offline mode)");
            return;
        }

        System.out.println("🔄 Initializing Database...");

        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            System.err.println("⚠️  Cannot initialize database: No connection available.");
            return;
        }

        try (Statement stmt = conn.createStatement();
                InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/skilora_database.sql")) {

            if (inputStream == null) {
                System.err.println("❌ Database script 'skilora_database.sql' not found in resources!");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder sqlStatement = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("//") || line.startsWith("/*")) {
                        continue;
                    }

                    sqlStatement.append(line);

                    // If statement ends with semicolon, execute it
                    if (line.endsWith(";")) {
                        String finalSql = sqlStatement.toString();
                        try {
                            stmt.execute(finalSql);
                            // System.out.println("Executed: " + (finalSql.length() > 50 ?
                            // finalSql.substring(0, 50) + "..." : finalSql));
                        } catch (Exception e) {
                            // Don't fail completely on individual statement errors (e.g. table already
                            // exists)
                            // But log them if they aren't "table already exists"
                            String msg = e.getMessage().toLowerCase();
                            if (!msg.contains("already exists") && !msg.contains("duplicate column")) {
                                System.err.println("⚠️ SQL Warning: " + e.getMessage());
                            }
                        }
                        sqlStatement.setLength(0); // Reset buffer
                    } else {
                        sqlStatement.append(" "); // Add space for multi-line statements
                    }
                }
            }

            System.out.println("✅ Database initialization completed successfully.");

        } catch (Exception e) {
            System.err.println("⚠️  Database initialization issue: " + e.getMessage());
        }
    }

    public static boolean isConnected() {
        try {
            return DatabaseConnection.getInstance().getConnection() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getConnectionStatus() {
        if (isConnected()) {
            return "✅ DATABASE CONNECTED";
        } else {
            return "⚠️  OFFLINE MODE (Database unavailable)";
        }
    }
}
