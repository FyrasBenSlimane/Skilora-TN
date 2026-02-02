package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.TicketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketMessageService {

    private static final Logger logger = LoggerFactory.getLogger(TicketMessageService.class);
    private static volatile TicketMessageService instance;

    private TicketMessageService() {}

    public static TicketMessageService getInstance() {
        if (instance == null) {
            synchronized (TicketMessageService.class) {
                if (instance == null) {
                    instance = new TicketMessageService();
                }
            }
        }
        return instance;
    }

    public int addMessage(TicketMessage msg) {
        String sql = """
            INSERT INTO ticket_messages (ticket_id, sender_id, message, is_internal)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, msg.getTicketId());
            stmt.setInt(2, msg.getSenderId());
            stmt.setString(3, msg.getMessage());
            stmt.setBoolean(4, msg.isInternal());
            
            stmt.executeUpdate();
            
            // Update ticket's updated_date
            updateTicketTimestamp(conn, msg.getTicketId());
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to add ticket message", e);
        }
        return -1;
    }

    public List<TicketMessage> findByTicketId(int ticketId) {
        String sql = """
            SELECT tm.*, u.full_name as sender_name, u.role as sender_role
            FROM ticket_messages tm
            LEFT JOIN users u ON tm.sender_id = u.id
            WHERE tm.ticket_id = ?
            ORDER BY tm.created_date ASC
            """;
        List<TicketMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find messages for ticket: {}", ticketId, e);
        }
        return messages;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM ticket_messages WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete message", e);
        }
        return false;
    }

    private void updateTicketTimestamp(Connection conn, int ticketId) {
        String sql = "UPDATE support_tickets SET updated_date = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, ticketId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to update ticket timestamp", e);
        }
    }

    private TicketMessage mapResultSet(ResultSet rs) throws SQLException {
        TicketMessage msg = new TicketMessage();
        msg.setId(rs.getInt("id"));
        msg.setTicketId(rs.getInt("ticket_id"));
        msg.setSenderId(rs.getInt("sender_id"));
        msg.setMessage(rs.getString("message"));
        msg.setInternal(rs.getBoolean("is_internal"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) msg.setCreatedDate(created.toLocalDateTime());
        
        // Transient fields
        msg.setSenderName(rs.getString("sender_name"));
        msg.setSenderRole(rs.getString("sender_role"));
        
        return msg;
    }
}
