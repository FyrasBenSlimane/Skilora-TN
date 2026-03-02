package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.PortfolioItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PortfolioService {

    private static volatile PortfolioService instance;

    private PortfolioService() {}

    public static PortfolioService getInstance() {
        if (instance == null) {
            synchronized (PortfolioService.class) {
                if (instance == null) instance = new PortfolioService();
            }
        }
        return instance;
    }

    public List<PortfolioItem> findByUserId(int userId) throws SQLException {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT * FROM portfolio_items WHERE user_id = ? ORDER BY created_date DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        }
        return items;
    }

    public void create(PortfolioItem item) throws SQLException {
        String sql = "INSERT INTO portfolio_items (user_id, title, description, image_url, project_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getUserId());
            stmt.setString(2, item.getTitle());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getImageUrl());
            stmt.setString(5, item.getProjectUrl());
            
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM portfolio_items WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private PortfolioItem mapResultSetToItem(ResultSet rs) throws SQLException {
        PortfolioItem item = new PortfolioItem();
        item.setId(rs.getInt("id"));
        item.setUserId(rs.getInt("user_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setImageUrl(rs.getString("image_url"));
        item.setProjectUrl(rs.getString("project_url"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) item.setCreatedAt(ts.toLocalDateTime());
        return item;
    }
}
