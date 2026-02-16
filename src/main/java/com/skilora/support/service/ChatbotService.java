package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.ChatbotConversation;
import com.skilora.support.entity.ChatbotMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    private static volatile ChatbotService instance;

    private ChatbotService() {}

    public static ChatbotService getInstance() {
        if (instance == null) {
            synchronized (ChatbotService.class) {
                if (instance == null) {
                    instance = new ChatbotService();
                }
            }
        }
        return instance;
    }

    public int startConversation(int userId) {
        String sql = "INSERT INTO chatbot_conversations (user_id, status) VALUES (?, 'ACTIVE')";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to start conversation", e);
        }
        return -1;
    }

    public ChatbotConversation findById(int id) {
        String sql = "SELECT * FROM chatbot_conversations WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapConversationResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find conversation by id: {}", id, e);
        }
        return null;
    }

    public List<ChatbotConversation> findByUserId(int userId) {
        String sql = "SELECT * FROM chatbot_conversations WHERE user_id = ? ORDER BY started_date DESC";
        List<ChatbotConversation> conversations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapConversationResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find conversations for user: {}", userId, e);
        }
        return conversations;
    }

    public boolean endConversation(int id) {
        String sql = "UPDATE chatbot_conversations SET status = 'ENDED', ended_date = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to end conversation", e);
        }
        return false;
    }

    public int addMessage(ChatbotMessage msg) {
        String sql = """
            INSERT INTO chatbot_messages (conversation_id, sender, message, intent, confidence)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, msg.getConversationId());
            stmt.setString(2, msg.getSender());
            stmt.setString(3, msg.getMessage());
            stmt.setString(4, msg.getIntent());
            stmt.setDouble(5, msg.getConfidence());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to add chatbot message", e);
        }
        return -1;
    }

    public List<ChatbotMessage> getMessages(int conversationId) {
        String sql = "SELECT * FROM chatbot_messages WHERE conversation_id = ? ORDER BY created_date ASC";
        List<ChatbotMessage> messages = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessageResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get messages for conversation: {}", conversationId, e);
        }
        return messages;
    }

    public boolean escalateToTicket(int conversationId, int ticketId) {
        String sql = "UPDATE chatbot_conversations SET escalated_to_ticket_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ticketId);
            stmt.setInt(2, conversationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to escalate conversation to ticket", e);
        }
        return false;
    }

    public String getAutoResponse(String userMessage) {
        String sql = """
            SELECT id, response_text FROM auto_responses 
            WHERE is_active = TRUE AND ? LIKE CONCAT('%', trigger_keyword, '%')
            ORDER BY LENGTH(trigger_keyword) DESC
            LIMIT 1
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userMessage.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int responseId = rs.getInt("id");
                    String response = rs.getString("response_text");
                    
                    // Increment usage count
                    incrementUsageCount(conn, responseId);
                    
                    return response;
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get auto response", e);
        }
        return null;
    }

    private void incrementUsageCount(Connection conn, int responseId) {
        String sql = "UPDATE auto_responses SET usage_count = usage_count + 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, responseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to increment usage count", e);
        }
    }

    private ChatbotConversation mapConversationResultSet(ResultSet rs) throws SQLException {
        ChatbotConversation conv = new ChatbotConversation();
        conv.setId(rs.getInt("id"));
        conv.setUserId(rs.getInt("user_id"));
        conv.setStatus(rs.getString("status"));
        
        Timestamp started = rs.getTimestamp("started_date");
        if (started != null) conv.setStartedDate(started.toLocalDateTime());
        
        Timestamp ended = rs.getTimestamp("ended_date");
        if (ended != null) conv.setEndedDate(ended.toLocalDateTime());
        
        int escalatedId = rs.getInt("escalated_to_ticket_id");
        if (!rs.wasNull()) {
            conv.setEscalatedToTicketId(escalatedId);
        }
        
        return conv;
    }

    private ChatbotMessage mapMessageResultSet(ResultSet rs) throws SQLException {
        ChatbotMessage msg = new ChatbotMessage();
        msg.setId(rs.getInt("id"));
        msg.setConversationId(rs.getInt("conversation_id"));
        msg.setSender(rs.getString("sender"));
        msg.setMessage(rs.getString("message"));
        msg.setIntent(rs.getString("intent"));
        msg.setConfidence(rs.getDouble("confidence"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) msg.setCreatedDate(created.toLocalDateTime());
        
        return msg;
    }
}
