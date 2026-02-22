package com.skilora.model.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static volatile SearchService instance;

    private SearchService() {
    }

    public static SearchService getInstance() {
        if (instance == null) {
            synchronized (SearchService.class) {
                if (instance == null) {
                    instance = new SearchService();
                }
            }
        }
        return instance;
    }

    public List<SearchResult> searchAll(String query) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty())
            return results;

        String searchTerm = "%" + query.trim().toLowerCase() + "%";

        // Search Users
        searchUsers(searchTerm, results);
        // Search Job Offers
        searchJobOffers(searchTerm, results);
        // Search Profiles
        searchProfiles(searchTerm, results);

        return results;
    }

    private void searchUsers(String searchTerm, List<SearchResult> results) {
        String query = "SELECT id, username, full_name, role FROM users WHERE LOWER(username) LIKE ? OR LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            stmt.setString(3, searchTerm);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            "User",
                            rs.getInt("id"),
                            rs.getString("username"),
                            "Name: " + rs.getString("full_name") + " | Role: " + rs.getString("role"),
                            "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching users", e);
        }
    }

    private void searchJobOffers(String searchTerm, List<SearchResult> results) {
        String query = "SELECT id, title, location, company_id FROM job_offers WHERE LOWER(title) LIKE ? OR LOWER(location) LIKE ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            "Job Offer",
                            rs.getInt("id"),
                            rs.getString("title"),
                            "Location: " + rs.getString("location"),
                            "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching job offers", e);
        }
    }

    private void searchProfiles(String searchTerm, List<SearchResult> results) {
        String query = "SELECT id, first_name, last_name, location FROM profiles WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, searchTerm);
            stmt.setString(2, searchTerm);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            "Profile",
                            rs.getInt("id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            "Location: " + rs.getString("location"),
                            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching profiles", e);
        }
    }
}
