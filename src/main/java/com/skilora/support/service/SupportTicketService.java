package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.SupportTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);
    private static volatile SupportTicketService instance;

    private SupportTicketService() {}

    public static SupportTicketService getInstance() {
        if (instance == null) {
            synchronized (SupportTicketService.class) {
                if (instance == null) {
                    instance = new SupportTicketService();
                }
            }
        }
        return instance;
    }

    public int create(SupportTicket ticket) {
        String sql = """
            INSERT INTO support_tickets (user_id, category, priority, status, subject, description, assigned_to)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, ticket.getUserId());
            stmt.setString(2, ticket.getCategory());
            stmt.setString(3, ticket.getPriority());
            stmt.setString(4, ticket.getStatus());
            stmt.setString(5, ticket.getSubject());
            stmt.setString(6, ticket.getDescription());
            if (ticket.getAssignedTo() != null) {
                stmt.setInt(7, ticket.getAssignedTo());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create support ticket", e);
        }
        return -1;
    }

    public SupportTicket findById(int id) {
        String sql = """
            SELECT t.*, 
                   u1.full_name as user_name, 
                   u2.full_name as assigned_to_name
            FROM support_tickets t
            LEFT JOIN users u1 ON t.user_id = u1.id
            LEFT JOIN users u2 ON t.assigned_to = u2.id
            WHERE t.id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find ticket by id: {}", id, e);
        }
        return null;
    }

    public List<SupportTicket> findByUserId(int userId) {
        String sql = """
            SELECT t.*, 
                   u1.full_name as user_name, 
                   u2.full_name as assigned_to_name
            FROM support_tickets t
            LEFT JOIN users u1 ON t.user_id = u1.id
            LEFT JOIN users u2 ON t.assigned_to = u2.id
            WHERE t.user_id = ?
            ORDER BY t.created_date DESC
            """;
        return executeQuery(sql, userId);
    }

    public List<SupportTicket> findAll() {
        String sql = """
            SELECT t.*, 
                   u1.full_name as user_name, 
                   u2.full_name as assigned_to_name
            FROM support_tickets t
            LEFT JOIN users u1 ON t.user_id = u1.id
            LEFT JOIN users u2 ON t.assigned_to = u2.id
            ORDER BY t.created_date DESC
            """;
        List<SupportTicket> tickets = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tickets.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all tickets", e);
        }
        return tickets;
    }

    public List<SupportTicket> findByStatus(String status) {
        String sql = """
            SELECT t.*, 
                   u1.full_name as user_name, 
                   u2.full_name as assigned_to_name
            FROM support_tickets t
            LEFT JOIN users u1 ON t.user_id = u1.id
            LEFT JOIN users u2 ON t.assigned_to = u2.id
            WHERE t.status = ?
            ORDER BY t.created_date DESC
            """;
        return executeQuery(sql, status);
    }

    public List<SupportTicket> findByPriority(String priority) {
        String sql = """
            SELECT t.*, 
                   u1.full_name as user_name, 
                   u2.full_name as assigned_to_name
            FROM support_tickets t
            LEFT JOIN users u1 ON t.user_id = u1.id
            LEFT JOIN users u2 ON t.assigned_to = u2.id
            WHERE t.priority = ?
            ORDER BY t.created_date DESC
            """;
        return executeQuery(sql, priority);
    }

    public boolean update(SupportTicket ticket) {
        String sql = """
            UPDATE support_tickets 
            SET category = ?, priority = ?, status = ?, subject = ?, 
                description = ?, assigned_to = ?, resolved_date = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ticket.getCategory());
            stmt.setString(2, ticket.getPriority());
            stmt.setString(3, ticket.getStatus());
            stmt.setString(4, ticket.getSubject());
            stmt.setString(5, ticket.getDescription());
            if (ticket.getAssignedTo() != null) {
                stmt.setInt(6, ticket.getAssignedTo());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            if (ticket.getResolvedDate() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(ticket.getResolvedDate()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            stmt.setInt(8, ticket.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update ticket", e);
        }
        return false;
    }

    public boolean updateStatus(int id, String status) {
        String sql = """
            UPDATE support_tickets 
            SET status = ?, resolved_date = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            if ("RESOLVED".equals(status)) {
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            stmt.setInt(3, id);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update ticket status", e);
        }
        return false;
    }

    public boolean assign(int ticketId, int adminId) {
        String sql = "UPDATE support_tickets SET assigned_to = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, adminId);
            stmt.setInt(2, ticketId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to assign ticket", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM support_tickets WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete ticket", e);
        }
        return false;
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM support_tickets WHERE status = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count tickets by status", e);
        }
        return 0;
    }

    public long countOpen() {
        String sql = "SELECT COUNT(*) FROM support_tickets WHERE status IN ('OPEN', 'IN_PROGRESS')";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count open tickets", e);
        }
        return 0;
    }

    private List<SupportTicket> executeQuery(String sql, Object param) {
        List<SupportTicket> tickets = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (param instanceof Integer) {
                stmt.setInt(1, (Integer) param);
            } else if (param instanceof String) {
                stmt.setString(1, (String) param);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to execute query", e);
        }
        return tickets;
    }

    private SupportTicket mapResultSet(ResultSet rs) throws SQLException {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(rs.getInt("id"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setCategory(rs.getString("category"));
        ticket.setPriority(rs.getString("priority"));
        ticket.setStatus(rs.getString("status"));
        ticket.setSubject(rs.getString("subject"));
        ticket.setDescription(rs.getString("description"));
        
        int assignedTo = rs.getInt("assigned_to");
        if (!rs.wasNull()) {
            ticket.setAssignedTo(assignedTo);
        }
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) ticket.setCreatedDate(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_date");
        if (updated != null) ticket.setUpdatedDate(updated.toLocalDateTime());
        
        Timestamp resolved = rs.getTimestamp("resolved_date");
        if (resolved != null) ticket.setResolvedDate(resolved.toLocalDateTime());
        
        // Transient fields
        ticket.setUserName(rs.getString("user_name"));
        ticket.setAssignedToName(rs.getString("assigned_to_name"));
        
        return ticket;
    }
}
