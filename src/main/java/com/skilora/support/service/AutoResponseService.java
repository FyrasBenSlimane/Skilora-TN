package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.AutoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoResponseService {

    private static final Logger logger = LoggerFactory.getLogger(AutoResponseService.class);
    private static volatile AutoResponseService instance;

    private AutoResponseService() {}

    public static AutoResponseService getInstance() {
        if (instance == null) {
            synchronized (AutoResponseService.class) {
                if (instance == null) {
                    instance = new AutoResponseService();
                }
            }
        }
        return instance;
    }

    public int create(AutoResponse ar) {
        String sql = """
            INSERT INTO auto_responses (trigger_keyword, response_text, category, language, is_active)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, ar.getTriggerKeyword());
            stmt.setString(2, ar.getResponseText());
            stmt.setString(3, ar.getCategory());
            stmt.setString(4, ar.getLanguage());
            stmt.setBoolean(5, ar.isActive());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create auto response", e);
        }
        return -1;
    }

    public List<AutoResponse> findAll() {
        String sql = "SELECT * FROM auto_responses ORDER BY created_date DESC";
        List<AutoResponse> responses = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                responses.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all auto responses", e);
        }
        return responses;
    }

    public List<AutoResponse> findActive() {
        String sql = "SELECT * FROM auto_responses WHERE is_active = TRUE ORDER BY usage_count DESC";
        List<AutoResponse> responses = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                responses.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find active auto responses", e);
        }
        return responses;
    }

    public boolean update(AutoResponse ar) {
        String sql = """
            UPDATE auto_responses 
            SET trigger_keyword = ?, response_text = ?, category = ?, language = ?, is_active = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ar.getTriggerKeyword());
            stmt.setString(2, ar.getResponseText());
            stmt.setString(3, ar.getCategory());
            stmt.setString(4, ar.getLanguage());
            stmt.setBoolean(5, ar.isActive());
            stmt.setInt(6, ar.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update auto response", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM auto_responses WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete auto response", e);
        }
        return false;
    }

    public boolean toggleActive(int id) {
        String sql = "UPDATE auto_responses SET is_active = NOT is_active WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to toggle auto response", e);
        }
        return false;
    }

    private AutoResponse mapResultSet(ResultSet rs) throws SQLException {
        AutoResponse ar = new AutoResponse();
        ar.setId(rs.getInt("id"));
        ar.setTriggerKeyword(rs.getString("trigger_keyword"));
        ar.setResponseText(rs.getString("response_text"));
        ar.setCategory(rs.getString("category"));
        ar.setLanguage(rs.getString("language"));
        ar.setActive(rs.getBoolean("is_active"));
        ar.setUsageCount(rs.getInt("usage_count"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) ar.setCreatedDate(created.toLocalDateTime());
        
        return ar;
    }
}
