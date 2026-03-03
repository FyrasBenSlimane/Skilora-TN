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
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if exists
                String checkSql = "SELECT id FROM conversations WHERE participant_1 = ? AND participant_2 = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setInt(1, p1);
                    stmt.setInt(2, p2);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        conn.commit();
                        return rs.getInt("id");
                    }
                }
                
                // Create new (within same transaction)
                String insertSql = "INSERT INTO conversations (participant_1, participant_2, created_date) VALUES (?, ?, NOW())";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, p1);
                    stmt.setInt(2, p2);
                    stmt.executeUpdate();
                    
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        conn.commit();
                        return id;
                    }
                }
                conn.rollback();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error in getOrCreateConversation: {}", e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error getting connection: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<Conversation> getConversations(int userId) {
        List<Conversation> conversations = new ArrayList<>();
        // Get blocked user IDs to filter from conversation list
        List<Integer> blockedIds = UserBlockService.getInstance().getBlockedUserIds(userId);

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
                CASE
                    WHEN c.participant_1 = ? THEN c.participant_2
                    ELSE c.participant_1
                END as other_id,
                (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_date DESC LIMIT 1) as last_msg,
                (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = FALSE) as unread
            FROM conversations c
            JOIN users u1 ON c.participant_1 = u1.id
            JOIN users u2 ON c.participant_2 = u2.id
            WHERE (c.participant_1 = ? OR c.participant_2 = ?)
            AND ((c.participant_1 = ? AND c.is_archived_1 = FALSE) OR (c.participant_2 = ? AND c.is_archived_2 = FALSE))
            ORDER BY c.last_message_date IS NULL, c.last_message_date DESC
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
            stmt.setInt(8, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int otherId = rs.getInt("other_id");
                // Skip conversations with blocked users
                if (blockedIds.contains(otherId)) continue;
                conversations.add(mapConversation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching conversations: {}", e.getMessage(), e);
        }
        return conversations;
    }

    public int sendMessage(int conversationId, int senderId, String content) {
        // Privacy check: determine the other participant and check block status
        int otherId = getOtherParticipant(conversationId, senderId);
        if (otherId > 0 && UserBlockService.getInstance().isEitherBlocked(senderId, otherId)) {
            logger.warn("Message blocked: user {} or {} has a block in place", senderId, otherId);
            return -2; // Special code: blocked
        }

        String sql = "INSERT INTO messages (conversation_id, sender_id, content, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                    conn.commit();
                    return id;
                }
                conn.rollback();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Send a message with media (image, video). Delegates to the full overload with duration=0.
     */
    public int sendMessage(int conversationId, int senderId, String content, String messageType, String mediaUrl, String fileName) {
        return sendMessage(conversationId, senderId, content, messageType, mediaUrl, fileName, 0);
    }

    /**
     * Send a message with media and duration (for vocal messages).
     */
    public int sendMessage(int conversationId, int senderId, String content, String messageType, String mediaUrl, String fileName, int duration) {
        // Privacy check
        int otherId = getOtherParticipant(conversationId, senderId);
        if (otherId > 0 && UserBlockService.getInstance().isEitherBlocked(senderId, otherId)) {
            logger.warn("Message blocked: user {} or {} has a block in place", senderId, otherId);
            return -2;
        }

        String sql = "INSERT INTO messages (conversation_id, sender_id, content, message_type, media_url, file_name, duration, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, conversationId);
                stmt.setInt(2, senderId);
                stmt.setString(3, content != null ? content : "");
                stmt.setString(4, messageType != null ? messageType : "TEXT");
                stmt.setString(5, mediaUrl);
                stmt.setString(6, fileName);
                stmt.setInt(7, duration);
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    String updateSql = "UPDATE conversations SET last_message_date = NOW() WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, conversationId);
                        updateStmt.executeUpdate();
                    }
                    conn.commit();
                    return id;
                }
                conn.rollback();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error sending media message: {}", e.getMessage(), e);
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

    /**
     * Update the content of an existing message. Only the sender can update.
     */
    public boolean updateMessage(int messageId, int senderId, String newContent) {
        String sql = "UPDATE messages SET content = ? WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newContent);
            stmt.setInt(2, messageId);
            stmt.setInt(3, senderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating message: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Delete a message. Only the sender can delete.
     */
    public boolean deleteMessage(int messageId, int senderId) {
        String sql = "DELETE FROM messages WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.setInt(2, senderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting message: {}", e.getMessage(), e);
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

    /**
     * Get the other participant in a conversation. Returns their user ID or -1.
     */
    public int getOtherParticipant(int conversationId, int currentUserId) {
        String sql = "SELECT participant_1, participant_2 FROM conversations WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int p1 = rs.getInt("participant_1");
                    int p2 = rs.getInt("participant_2");
                    return (p1 == currentUserId) ? p2 : p1;
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting other participant: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Check if the current user can message the other user (not blocked).
     */
    public boolean canMessage(int userId, int otherUserId) {
        return !UserBlockService.getInstance().isEitherBlocked(userId, otherUserId);
    }

    // ═══════════════════════════════════════════════════════════
    //  TYPING STATUS — "Typing..." indicator
    // ═══════════════════════════════════════════════════════════

    /**
     * Update the typing status of a user in a conversation.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE to create or update the last typed timestamp.
     */
    public void updateTypingStatus(int conversationId, int userId) {
        String sql = "INSERT INTO typing_status (user_id, conversation_id, last_typed) VALUES (?, ?, NOW()) "
                   + "ON DUPLICATE KEY UPDATE last_typed = NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, conversationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Error updating typing status: {}", e.getMessage());
        }
    }

    /**
     * Check if another user is currently typing in a conversation.
     * A user is considered "typing" if their last keystroke was less than 4 seconds ago.
     */
    public boolean isUserTyping(int conversationId, int otherUserId) {
        String sql = "SELECT COUNT(*) FROM typing_status WHERE user_id = ? AND conversation_id = ? "
                   + "AND TIMESTAMPDIFF(SECOND, last_typed, NOW()) < 4";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, otherUserId);
            stmt.setInt(2, conversationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.debug("Error checking typing status: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Clear the typing status when a user sends a message or leaves the conversation.
     */
    public void clearTypingStatus(int conversationId, int userId) {
        String sql = "DELETE FROM typing_status WHERE user_id = ? AND conversation_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, conversationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Error clearing typing status: {}", e.getMessage());
        }
    }

    /**
     * Get the read status (is_read) of all messages sent by a user in a conversation.
     * Used for real-time "Seen" polling.
     *
     * @return Map (messageId -> isRead) for each message sent by senderId
     */
    public java.util.Map<Integer, Boolean> getReadStatusForMyMessages(int conversationId, int senderId) {
        java.util.Map<Integer, Boolean> statusMap = new java.util.HashMap<>();
        String sql = "SELECT id, is_read FROM messages WHERE conversation_id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, senderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                statusMap.put(rs.getInt("id"), rs.getBoolean("is_read"));
            }
        } catch (SQLException e) {
            logger.debug("Error fetching read status: {}", e.getMessage());
        }
        return statusMap;
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
        
        // Media fields
        msg.setMessageType(rs.getString("message_type"));
        msg.setMediaUrl(rs.getString("media_url"));
        msg.setFileName(rs.getString("file_name"));
        msg.setDuration(rs.getInt("duration"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) msg.setCreatedDate(created.toLocalDateTime());
        
        msg.setSenderName(rs.getString("sender_name"));
        
        return msg;
    }

    // ═══════════════════════════════════════════════════════════
    //  REACTIONS — Emoji reactions on private messages
    // ═══════════════════════════════════════════════════════════

    /**
     * Toggle a reaction on a private message. If the user already reacted with the same emoji, remove it.
     */
    public void toggleReaction(int messageId, int userId, String emoji) {
        String checkSql = "SELECT id FROM message_reactions WHERE message_id = ? AND user_id = ? AND emoji = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, messageId);
            checkStmt.setInt(2, userId);
            checkStmt.setString(3, emoji);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                try (PreparedStatement delStmt = conn.prepareStatement(
                        "DELETE FROM message_reactions WHERE id = ?")) {
                    delStmt.setInt(1, rs.getInt("id"));
                    delStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insStmt = conn.prepareStatement(
                        "INSERT INTO message_reactions (message_id, user_id, emoji) VALUES (?, ?, ?)")) {
                    insStmt.setInt(1, messageId);
                    insStmt.setInt(2, userId);
                    insStmt.setString(3, emoji);
                    insStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Error toggling reaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all reactions for messages in a conversation.
     * Returns a map of messageId -> map of emoji -> count.
     */
    public java.util.Map<Integer, java.util.Map<String, Integer>> getReactionsForConversation(int conversationId) {
        java.util.Map<Integer, java.util.Map<String, Integer>> result = new java.util.HashMap<>();
        String sql = """
                SELECT mr.message_id, mr.emoji, COUNT(*) as cnt
                FROM message_reactions mr
                JOIN messages m ON mr.message_id = m.id
                WHERE m.conversation_id = ?
                GROUP BY mr.message_id, mr.emoji
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int msgId = rs.getInt("message_id");
                String emojiVal = rs.getString("emoji");
                int count = rs.getInt("cnt");
                result.computeIfAbsent(msgId, k -> new java.util.LinkedHashMap<>()).put(emojiVal, count);
            }
        } catch (SQLException e) {
            logger.error("Error fetching reactions: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * Get the emojis the current user has reacted with for messages in a conversation.
     * Returns a map of messageId -> set of emojis.
     */
    public java.util.Map<Integer, java.util.Set<String>> getUserReactionsForConversation(int conversationId, int userId) {
        java.util.Map<Integer, java.util.Set<String>> result = new java.util.HashMap<>();
        String sql = """
                SELECT mr.message_id, mr.emoji
                FROM message_reactions mr
                JOIN messages m ON mr.message_id = m.id
                WHERE m.conversation_id = ? AND mr.user_id = ?
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int msgId = rs.getInt("message_id");
                String emojiVal = rs.getString("emoji");
                result.computeIfAbsent(msgId, k -> new java.util.HashSet<>()).add(emojiVal);
            }
        } catch (SQLException e) {
            logger.error("Error fetching user reactions: {}", e.getMessage(), e);
        }
        return result;
    }
}
