package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.FormationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationModuleService {

    private static final Logger logger = LoggerFactory.getLogger(FormationModuleService.class);
    private static volatile FormationModuleService instance;

    private FormationModuleService() {}

    public static FormationModuleService getInstance() {
        if (instance == null) {
            synchronized (FormationModuleService.class) {
                if (instance == null) {
                    instance = new FormationModuleService();
                }
            }
        }
        return instance;
    }

    public int create(FormationModule module) {
        if (module == null) throw new IllegalArgumentException("Module cannot be null");
        if (module.getTitle() == null || module.getTitle().isBlank()) throw new IllegalArgumentException("Module title is required");
        if (module.getFormationId() <= 0) throw new IllegalArgumentException("Valid formation ID is required");
        String sql = """
            INSERT INTO formation_modules (formation_id, title, description,
                content_url, duration_minutes, order_index)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, module.getFormationId());
            stmt.setString(2, module.getTitle());
            stmt.setString(3, module.getDescription());
            stmt.setString(4, module.getContentUrl());
            stmt.setInt(5, module.getDurationMinutes());
            stmt.setInt(6, module.getOrderIndex());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Formation module created: id={}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating module: {}", e.getMessage(), e);
        }
        return -1;
    }

    public FormationModule findById(int id) {
        String sql = "SELECT * FROM formation_modules WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapModule(rs);
        } catch (SQLException e) {
            logger.error("Error finding module {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public List<FormationModule> findByFormation(int formationId) {
        List<FormationModule> list = new ArrayList<>();
        String sql = "SELECT * FROM formation_modules WHERE formation_id = ? ORDER BY order_index, id";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapModule(rs));
        } catch (SQLException e) {
            logger.error("Error finding modules for formation {}: {}", formationId, e.getMessage(), e);
        }
        return list;
    }

    public boolean update(FormationModule module) {
        String sql = """
            UPDATE formation_modules SET title = ?, description = ?, content_url = ?,
                duration_minutes = ?, order_index = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, module.getTitle());
            stmt.setString(2, module.getDescription());
            stmt.setString(3, module.getContentUrl());
            stmt.setInt(4, module.getDurationMinutes());
            stmt.setInt(5, module.getOrderIndex());
            stmt.setInt(6, module.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating module: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM formation_modules WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting module: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean reorder(int formationId, List<Integer> moduleIds) {
        String sql = "UPDATE formation_modules SET order_index = ? WHERE id = ? AND formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < moduleIds.size(); i++) {
                    stmt.setInt(1, i);
                    stmt.setInt(2, moduleIds.get(i));
                    stmt.setInt(3, formationId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                logger.info("Reordered {} modules for formation {}", moduleIds.size(), formationId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error reordering modules: {}", e.getMessage(), e);
        }
        return false;
    }

    public int countByFormation(int formationId) {
        String sql = "SELECT COUNT(*) FROM formation_modules WHERE formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error counting modules: {}", e.getMessage(), e);
        }
        return 0;
    }

    public int getTotalDuration(int formationId) {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM formation_modules WHERE formation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error getting total duration: {}", e.getMessage(), e);
        }
        return 0;
    }

    // ── Private helpers ──

    private FormationModule mapModule(ResultSet rs) throws SQLException {
        FormationModule m = new FormationModule();
        m.setId(rs.getInt("id"));
        m.setFormationId(rs.getInt("formation_id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setContentUrl(rs.getString("content_url"));
        m.setDurationMinutes(rs.getInt("duration_minutes"));
        m.setOrderIndex(rs.getInt("order_index"));
        return m;
    }
}
