package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.TicketAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketAttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(TicketAttachmentService.class);
    private static volatile TicketAttachmentService instance;

    private TicketAttachmentService() {}

    public static TicketAttachmentService getInstance() {
        if (instance == null) {
            synchronized (TicketAttachmentService.class) {
                if (instance == null) instance = new TicketAttachmentService();
            }
        }
        return instance;
    }

    public int create(TicketAttachment att) {
        String sql = """
            INSERT INTO ticket_attachments (ticket_id, message_id, file_name, mime_type, file_path, file_size)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, att.getTicketId());
            if (att.getMessageId() != null) stmt.setInt(2, att.getMessageId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setString(3, att.getFileName());
            stmt.setString(4, att.getMimeType());
            stmt.setString(5, att.getFilePath());
            if (att.getFileSize() != null) stmt.setLong(6, att.getFileSize());
            else stmt.setNull(6, Types.BIGINT);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to create ticket attachment", e);
        }
        return -1;
    }

    public List<TicketAttachment> findByTicketId(int ticketId) {
        String sql = "SELECT * FROM ticket_attachments WHERE ticket_id = ? ORDER BY created_date ASC";
        List<TicketAttachment> atts = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) atts.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to load attachments for ticket {}", ticketId, e);
        }
        return atts;
    }

    public List<TicketAttachment> findByMessageId(int messageId) {
        String sql = "SELECT * FROM ticket_attachments WHERE message_id = ? ORDER BY created_date ASC";
        List<TicketAttachment> atts = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) atts.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to load attachments for message {}", messageId, e);
        }
        return atts;
    }

    private TicketAttachment mapResultSet(ResultSet rs) throws SQLException {
        TicketAttachment att = new TicketAttachment();
        att.setId(rs.getInt("id"));
        att.setTicketId(rs.getInt("ticket_id"));
        int mid = rs.getInt("message_id");
        if (!rs.wasNull()) att.setMessageId(mid);
        att.setFileName(rs.getString("file_name"));
        att.setMimeType(rs.getString("mime_type"));
        att.setFilePath(rs.getString("file_path"));

        long size = rs.getLong("file_size");
        if (!rs.wasNull()) att.setFileSize(size);

        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) att.setCreatedDate(created.toLocalDateTime());
        return att;
    }
}

