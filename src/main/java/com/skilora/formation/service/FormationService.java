package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.enums.FormationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FormationService
 * 
 * Handles all CRUD operations for formations (training courses).
 * Maps to the 'formations' table in the database.
 */
public class FormationService {

    private static final Logger logger = LoggerFactory.getLogger(FormationService.class);
    private static volatile FormationService instance;

    private FormationService() {}

    public static FormationService getInstance() {
        if (instance == null) {
            synchronized (FormationService.class) {
                if (instance == null) {
                    instance = new FormationService();
                }
            }
        }
        return instance;
    }

    /**
     * Create a new formation.
     *
     * @return the generated formation ID, or -1 on failure
     */
    public int createFormation(Formation formation) {
        String sql = "INSERT INTO formations (title, description, category, duration_hours, cost, currency, " +
                "provider, image_url, level, is_free, created_by, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, formation.getTitle());
            stmt.setString(2, formation.getDescription());
            stmt.setString(3, formation.getCategory());
            stmt.setInt(4, formation.getDurationHours());
            stmt.setDouble(5, formation.getCost());
            stmt.setString(6, formation.getCurrency());
            stmt.setString(7, formation.getProvider());
            stmt.setString(8, formation.getImageUrl());
            stmt.setString(9, formation.getLevel() != null ? formation.getLevel().name() : "BEGINNER");
            stmt.setBoolean(10, formation.isFree());
            stmt.setObject(11, formation.getCreatedBy() > 0 ? formation.getCreatedBy() : null);
            stmt.setString(12, formation.getStatus());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        formation.setId(id);
                        logger.info("Formation created successfully with ID: {}", id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating formation: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Find a formation by ID.
     */
    public Formation findById(int id) {
        String sql = "SELECT * FROM formations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding formation by ID {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Find all formations.
     */
    public List<Formation> findAll() {
        String sql = "SELECT * FROM formations ORDER BY created_date DESC";
        List<Formation> formations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                formations.add(mapResultSet(rs));
            }
            logger.debug("Retrieved {} formations", formations.size());
        } catch (SQLException e) {
            logger.error("Error finding all formations: {}", e.getMessage(), e);
        }
        return formations;
    }

    /**
     * Find formations by category.
     */
    public List<Formation> findByCategory(String category) {
        String sql = "SELECT * FROM formations WHERE category = ? ORDER BY created_date DESC";
        List<Formation> formations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    formations.add(mapResultSet(rs));
                }
            }
            logger.debug("Retrieved {} formations for category: {}", formations.size(), category);
        } catch (SQLException e) {
            logger.error("Error finding formations by category {}: {}", category, e.getMessage(), e);
        }
        return formations;
    }

    /**
     * Find formations by status.
     */
    public List<Formation> findByStatus(String status) {
        String sql = "SELECT * FROM formations WHERE status = ? ORDER BY created_date DESC";
        List<Formation> formations = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    formations.add(mapResultSet(rs));
                }
            }
            logger.debug("Retrieved {} formations with status: {}", formations.size(), status);
        } catch (SQLException e) {
            logger.error("Error finding formations by status {}: {}", status, e.getMessage(), e);
        }
        return formations;
    }

    /**
     * Search formations by title, description, or provider.
     */
    public List<Formation> search(String query) {
        String sql = "SELECT * FROM formations WHERE title LIKE ? OR description LIKE ? OR provider LIKE ? " +
                "ORDER BY created_date DESC";
        List<Formation> formations = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    formations.add(mapResultSet(rs));
                }
            }
            logger.debug("Search for '{}' returned {} formations", query, formations.size());
        } catch (SQLException e) {
            logger.error("Error searching formations: {}", e.getMessage(), e);
        }
        return formations;
    }

    /**
     * Update a formation.
     */
    public boolean update(Formation formation) {
        String sql = "UPDATE formations SET title = ?, description = ?, category = ?, duration_hours = ?, " +
                "cost = ?, currency = ?, provider = ?, image_url = ?, level = ?, is_free = ?, status = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, formation.getTitle());
            stmt.setString(2, formation.getDescription());
            stmt.setString(3, formation.getCategory());
            stmt.setInt(4, formation.getDurationHours());
            stmt.setDouble(5, formation.getCost());
            stmt.setString(6, formation.getCurrency());
            stmt.setString(7, formation.getProvider());
            stmt.setString(8, formation.getImageUrl());
            stmt.setString(9, formation.getLevel() != null ? formation.getLevel().name() : "BEGINNER");
            stmt.setBoolean(10, formation.isFree());
            stmt.setString(11, formation.getStatus());
            stmt.setInt(12, formation.getId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Formation ID {} updated successfully", formation.getId());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating formation ID {}: {}", formation.getId(), e.getMessage(), e);
        }
        return false;
    }

    /**
     * Delete a formation by ID.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM formations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Formation ID {} deleted successfully", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error deleting formation ID {}: {}", id, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Count all formations.
     */
    public long countAll() {
        String sql = "SELECT COUNT(*) FROM formations";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting formations: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Count formations by category.
     */
    public long countByCategory(String category) {
        String sql = "SELECT COUNT(*) FROM formations WHERE category = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting formations by category {}: {}", category, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Map ResultSet to Formation entity.
     */
    private Formation mapResultSet(ResultSet rs) throws SQLException {
        Formation formation = new Formation();
        formation.setId(rs.getInt("id"));
        formation.setTitle(rs.getString("title"));
        formation.setDescription(rs.getString("description"));
        formation.setCategory(rs.getString("category"));
        formation.setDurationHours(rs.getInt("duration_hours"));
        formation.setCost(rs.getDouble("cost"));
        formation.setCurrency(rs.getString("currency"));
        formation.setProvider(rs.getString("provider"));
        formation.setImageUrl(rs.getString("image_url"));
        
        String levelStr = rs.getString("level");
        if (levelStr != null) {
            try {
                formation.setLevel(FormationLevel.valueOf(levelStr));
            } catch (IllegalArgumentException e) {
                formation.setLevel(FormationLevel.BEGINNER);
            }
        }
        
        formation.setFree(rs.getBoolean("is_free"));
        formation.setCreatedBy(rs.getInt("created_by"));
        
        Timestamp createdTs = rs.getTimestamp("created_date");
        if (createdTs != null) {
            formation.setCreatedDate(createdTs.toLocalDateTime());
        }
        
        formation.setStatus(rs.getString("status"));
        return formation;
    }
}
