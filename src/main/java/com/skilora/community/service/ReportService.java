package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportService - CRUD for the reports table.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static ReportService instance;

    private ReportService() {}

    public static synchronized ReportService getInstance() {
        if (instance == null) instance = new ReportService();
        return instance;
    }

    public int create(Report report) {
        String sql = "INSERT INTO reports (subject, type, description, reporter_id, reported_entity_type, reported_entity_id, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, report.getSubject());
            ps.setString(2, report.getType());
            ps.setString(3, report.getDescription());
            ps.setInt(4, report.getReporterId());
            ps.setString(5, report.getReportedEntityType());
            if (report.getReportedEntityId() != null) {
                ps.setInt(6, report.getReportedEntityId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, report.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to create report", e);
        }
        return -1;
    }

    public List<Report> findAll() {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT r.*, u.full_name AS reporter_name FROM reports r "
                   + "LEFT JOIN users u ON r.reporter_id = u.id "
                   + "ORDER BY r.created_at DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all reports", e);
        }
        return list;
    }

    public List<Report> findByStatus(String status) {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT r.*, u.full_name AS reporter_name FROM reports r "
                   + "LEFT JOIN users u ON r.reporter_id = u.id "
                   + "WHERE r.status = ? ORDER BY r.created_at DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find reports by status {}", status, e);
        }
        return list;
    }

    public boolean updateStatus(int reportId, String status, Integer resolvedByUserId) {
        String sql = "UPDATE reports SET status = ?, resolved_by = ?, resolved_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (resolvedByUserId != null) {
                ps.setInt(2, resolvedByUserId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if ("RESOLVED".equals(status) || "DISMISSED".equals(status)) {
                ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setInt(4, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update report {} status to {}", reportId, status, e);
        }
        return false;
    }

    public boolean delete(int reportId) {
        String sql = "DELETE FROM reports WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete report {}", reportId, e);
        }
        return false;
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getInt("id"));
        r.setSubject(rs.getString("subject"));
        r.setType(rs.getString("type"));
        r.setDescription(rs.getString("description"));
        r.setReporterId(rs.getInt("reporter_id"));
        r.setReporterName(rs.getString("reporter_name"));
        r.setReportedEntityType(rs.getString("reported_entity_type"));
        int entityId = rs.getInt("reported_entity_id");
        r.setReportedEntityId(rs.wasNull() ? null : entityId);
        r.setStatus(rs.getString("status"));
        int resolvedBy = rs.getInt("resolved_by");
        r.setResolvedBy(rs.wasNull() ? null : resolvedBy);
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) r.setResolvedAt(resolvedAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());
        return r;
    }
}
