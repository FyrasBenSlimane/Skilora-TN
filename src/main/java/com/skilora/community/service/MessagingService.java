package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Conversation;
import com.skilora.community.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    private static volatile MessagingService instance;

    private MessagingService() {}

    public static MessagingService getInstance() {
        if (instance == null) {
            synchronized (MessagingService.class) {
                if (instance == null) {
                    instance = new MessagingService();
                }
            }
        }
        return instance;
    }

    public int getOrCreateConversation(int userId1, int userId2) {
        int p1 = Math.min(userId1, userId2);
        int p2 = Math.max(userId1, userId2);
        
        // Check if exists
        String checkSql = "SELECT id FROM conversations WHERE participant_1 = ? AND participant_2 = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, p1);
            stmt.setInt(2, p2);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error checking conversation: {}", e.getMessage(), e);
        }
        
        // Create new
        String insertSql = "INSERT INTO conversations (participant_1, participant_2, created_date) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, p1);
            stmt.setInt(2, p2);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error creating conversation: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<Conversation> getConversations(int userId) {
        List<Conversation> conversations = new ArrayList<>();
        String sql = """
            SELECT c.*,
                CASE 
                    WHEN c.participant_1 = ? THEN u2.full_name 
                    ELSE u1.full_name 
                END as other_name,
                CASE 
                    WHEN c.participant_1 = ? THEN u2.photo_url 
                    ELSE u1.photo_url 
                END as other_photo,
                (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_date DESC LIMIT 1) as last_msg,
                (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = FALSE) as unread
            FROM conversations c
            JOIN users u1 ON c.participant_1 = u1.id
            JOIN users u2 ON c.participant_2 = u2.id
            WHERE (c.participant_1 = ? OR c.participant_2 = ?)
            AND ((c.participant_1 = ? AND c.is_archived_1 = FALSE) OR (c.participant_2 = ? AND c.is_archived_2 = FALSE))
            ORDER BY c.last_message_date DESC NULLS LAST
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.setInt(5, userId);
            stmt.setInt(6, userId);
            stmt.setInt(7, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                conversations.add(mapConversation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching conversations: {}", e.getMessage(), e);
        }
        return conversations;
    }

    public int sendMessage(int conversationId, int senderId, String content) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                // Update conversation last message date
                String updateSql = "UPDATE conversations SET last_message_date = NOW() WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, conversationId);
                    updateStmt.executeUpdate();
                }
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<Message> getMessages(int conversationId, int page, int pageSize) {
        List<Message> messages = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        
        String sql = """
            SELECT m.*, u.full_name as sender_name
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.conversation_id = ?
            ORDER BY m.created_date ASC
            LIMIT ? OFFSET ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching messages: {}", e.getMessage(), e);
        }
        return messages;
    }

    public boolean markAsRead(int conversationId, int userId) {
        String sql = "UPDATE messages SET is_read = TRUE WHERE conversation_id = ? AND sender_id != ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            logger.error("Error marking as read: {}", e.getMessage(), e);
        }
        return false;
    }

    public int getUnreadCount(int userId) {
        String sql = """
            SELECT COUNT(*) FROM messages m
            JOIN conversations c ON m.conversation_id = c.id
            WHERE (c.participant_1 = ? OR c.participant_2 = ?)
            AND m.sender_id != ?
            AND m.is_read = FALSE
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting unread count: {}", e.getMessage(), e);
        }
        return 0;
    }

    private Conversation mapConversation(ResultSet rs) throws SQLException {
        Conversation conv = new Conversation();
        conv.setId(rs.getInt("id"));
        conv.setParticipant1(rs.getInt("participant_1"));
        conv.setParticipant2(rs.getInt("participant_2"));
        conv.setArchived1(rs.getBoolean("is_archived_1"));
        conv.setArchived2(rs.getBoolean("is_archived_2"));
        
        Timestamp lastMsg = rs.getTimestamp("last_message_date");
        if (lastMsg != null) conv.setLastMessageDate(lastMsg.toLocalDateTime());
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) conv.setCreatedDate(created.toLocalDateTime());
        
        conv.setOtherUserName(rs.getString("other_name"));
        conv.setOtherUserPhoto(rs.getString("other_photo"));
        conv.setLastMessagePreview(rs.getString("last_msg"));
        conv.setUnreadCount(rs.getInt("unread"));
        
        return conv;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setId(rs.getInt("id"));
        msg.setConversationId(rs.getInt("conversation_id"));
        msg.setSenderId(rs.getInt("sender_id"));
        msg.setContent(rs.getString("content"));
        msg.setRead(rs.getBoolean("is_read"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) msg.setCreatedDate(created.toLocalDateTime());
        
        msg.setSenderName(rs.getString("sender_name"));
        
        return msg;
    }
}
