package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.CommunityGroup;
import com.skilora.community.entity.GroupMember;
import com.skilora.community.entity.GroupMessage;
import com.skilora.formation.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private static volatile GroupService instance;
    private final java.util.Map<String, Long> groupTypingStatus = new java.util.concurrent.ConcurrentHashMap<>();

    private GroupService() {
    }

    public static GroupService getInstance() {
        if (instance == null) {
            synchronized (GroupService.class) {
                if (instance == null) {
                    instance = new GroupService();
                }
            }
        }
        return instance;
    }

    public int create(CommunityGroup group) {
        String sql = "INSERT INTO community_groups (name, description, category, cover_image_url, creator_id, is_public, created_date) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setString(3, group.getCategory());
            stmt.setString(4, group.getCoverImageUrl());
            stmt.setInt(5, group.getCreatorId());
            stmt.setBoolean(6, group.isPublic());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Group created with id {}", id);

                // Add creator as ADMIN member
                String memberSql = "INSERT INTO group_members (group_id, user_id, role, joined_date) VALUES (?, ?, 'ADMIN', NOW())";
                try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                    memberStmt.setInt(1, id);
                    memberStmt.setInt(2, group.getCreatorId());
                    memberStmt.executeUpdate();
                }

                // Award achievement
                AchievementService.getInstance().checkAndAward(group.getCreatorId());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating group: {}", e.getMessage(), e);
        }
        return -1;
    }

    public CommunityGroup findById(int id) {
        String sql = """
                SELECT g.*, u.full_name as creator_name
                FROM community_groups g
                JOIN users u ON g.creator_id = u.id
                WHERE g.id = ?
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapGroup(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding group: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<CommunityGroup> findAll() {
        List<CommunityGroup> groups = new ArrayList<>();
        String sql = """
                SELECT g.*, u.full_name as creator_name
                FROM community_groups g
                JOIN users u ON g.creator_id = u.id
                WHERE g.is_public = TRUE
                ORDER BY g.member_count DESC, g.created_date DESC
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(mapGroup(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all groups: {}", e.getMessage(), e);
        }
        return groups;
    }

    public List<CommunityGroup> findByMember(int userId) {
        List<CommunityGroup> groups = new ArrayList<>();
        String sql = """
                SELECT g.*, u.full_name as creator_name
                FROM community_groups g
                JOIN users u ON g.creator_id = u.id
                JOIN group_members gm ON g.id = gm.group_id
                WHERE gm.user_id = ?
                ORDER BY gm.joined_date DESC
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                CommunityGroup group = mapGroup(rs);
                group.setMember(true);
                groups.add(group);
            }
        } catch (SQLException e) {
            logger.error("Error fetching member groups: {}", e.getMessage(), e);
        }
        return groups;
    }

    public List<CommunityGroup> search(String query) {
        List<CommunityGroup> groups = new ArrayList<>();
        String sql = """
                SELECT g.*, u.full_name as creator_name
                FROM community_groups g
                JOIN users u ON g.creator_id = u.id
                WHERE g.is_public = TRUE AND (g.name LIKE ? OR g.description LIKE ?)
                ORDER BY g.member_count DESC
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(mapGroup(rs));
            }
        } catch (SQLException e) {
            logger.error("Error searching groups: {}", e.getMessage(), e);
        }
        return groups;
    }

    public boolean update(CommunityGroup group) {
        String sql = "UPDATE community_groups SET name = ?, description = ?, category = ?, cover_image_url = ?, is_public = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setString(3, group.getCategory());
            stmt.setString(4, group.getCoverImageUrl());
            stmt.setBoolean(5, group.isPublic());
            stmt.setInt(6, group.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating group: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM community_groups WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting group: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean join(int groupId, int userId) {
        String sql = "INSERT INTO group_members (group_id, user_id, role, joined_date) VALUES (?, ?, 'MEMBER', NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);

            if (stmt.executeUpdate() > 0) {
                // Increment member count
                String updateSql = "UPDATE community_groups SET member_count = member_count + 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, groupId);
                    updateStmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error joining group: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean leave(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);

            if (stmt.executeUpdate() > 0) {
                // Decrement member count
                String updateSql = "UPDATE community_groups SET member_count = member_count - 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, groupId);
                    updateStmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error leaving group: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking membership: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<GroupMember> getMembers(int groupId) {
        List<GroupMember> members = new ArrayList<>();
        String sql = """
                SELECT gm.*, u.full_name as user_name, u.photo_url as user_photo
                FROM group_members gm
                JOIN users u ON gm.user_id = u.id
                WHERE gm.group_id = ?
                ORDER BY gm.role, gm.joined_date DESC
                """;

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching members: {}", e.getMessage(), e);
        }
        return members;
    }

    public List<GroupMessage> getMessages(int groupId) {
        List<GroupMessage> messages = new ArrayList<>();
        String sql = """
                SELECT gm.*, u.full_name as sender_name
                FROM group_messages gm
                JOIN users u ON gm.sender_id = u.id
                WHERE gm.group_id = ?
                ORDER BY gm.created_date ASC
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GroupMessage msg = new GroupMessage();
                msg.setId(rs.getInt("id"));
                msg.setGroupId(rs.getInt("group_id"));
                msg.setSenderId(rs.getInt("sender_id"));
                msg.setContent(rs.getString("content"));
                msg.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
                msg.setSenderName(rs.getString("sender_name"));
                msg.setMessageType(rs.getString("message_type"));
                msg.setMediaUrl(rs.getString("media_url"));
                msg.setFileName(rs.getString("file_name"));
                msg.setDuration(rs.getInt("duration"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            logger.error("Error fetching group messages: {}", e.getMessage(), e);
        }
        return messages;
    }

    public void addMessage(GroupMessage msg) {
        String sql = "INSERT INTO group_messages (group_id, sender_id, content, message_type, media_url, file_name, duration) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, msg.getGroupId());
            stmt.setInt(2, msg.getSenderId());
            stmt.setString(3, msg.getContent());
            stmt.setString(4, msg.getMessageType());
            stmt.setString(5, msg.getMediaUrl());
            stmt.setString(6, msg.getFileName());
            stmt.setInt(7, msg.getDuration());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                msg.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Error adding group message: {}", e.getMessage(), e);
        }
    }

    public void updateTypingStatus(int groupId, int userId) {
        String key = groupId + ":" + userId;
        groupTypingStatus.put(key, System.currentTimeMillis());
    }

    public void clearTypingStatus(int groupId, int userId) {
        String key = groupId + ":" + userId;
        groupTypingStatus.remove(key);
    }

    public List<String> getTypingUsers(int groupId, int excludeUserId) {
        List<String> typingUsers = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (java.util.Map.Entry<String, Long> entry : groupTypingStatus.entrySet()) {
            if (now - entry.getValue() < 3000) { // 3 seconds timeout
                String[] parts = entry.getKey().split(":");
                int gId = Integer.parseInt(parts[0]);
                int uId = Integer.parseInt(parts[1]);
                if (gId == groupId && uId != excludeUserId) {
                    typingUsers.add(getUserFullName(uId));
                }
            } else {
                groupTypingStatus.remove(entry.getKey());
            }
        }
        return typingUsers;
    }

    private String getUserFullName(int userId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT full_name FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getString(1);
        } catch (SQLException e) {
        }
        return "Un utilisateur";
    }

    private CommunityGroup mapGroup(ResultSet rs) throws SQLException {
        CommunityGroup group = new CommunityGroup();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCategory(rs.getString("category"));
        group.setCoverImageUrl(rs.getString("cover_image_url"));
        group.setCreatorId(rs.getInt("creator_id"));
        group.setMemberCount(rs.getInt("member_count"));
        group.setPublic(rs.getBoolean("is_public"));

        Timestamp created = rs.getTimestamp("created_date");
        if (created != null)
            group.setCreatedDate(created.toLocalDateTime());

        group.setCreatorName(rs.getString("creator_name"));

        return group;
    }

    private GroupMember mapMember(ResultSet rs) throws SQLException {
        GroupMember member = new GroupMember();
        member.setId(rs.getInt("id"));
        member.setGroupId(rs.getInt("group_id"));
        member.setUserId(rs.getInt("user_id"));
        member.setRole(rs.getString("role"));

        Timestamp joined = rs.getTimestamp("joined_date");
        if (joined != null)
            member.setJoinedDate(joined.toLocalDateTime());

        member.setUserName(rs.getString("user_name"));
        member.setUserPhoto(rs.getString("user_photo"));

        return member;
    }

    /**
     * Marks all messages in a group as read by the given user.
     * Uses INSERT IGNORE to avoid duplicates.
     */
    public void markMessagesAsRead(int groupId, int userId) {
        String sql = """
                INSERT IGNORE INTO group_message_reads (message_id, user_id)
                SELECT gm.id, ?
                FROM group_messages gm
                WHERE gm.group_id = ? AND gm.sender_id != ?
                AND gm.id NOT IN (
                    SELECT gmr.message_id FROM group_message_reads gmr WHERE gmr.user_id = ?
                )
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, groupId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marking group messages as read: {}", e.getMessage(), e);
        }
    }

    /**
     * Returns the number of distinct users who have read a specific message.
     */
    public int getReadByCount(int messageId) {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM group_message_reads WHERE message_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting read count for message {}: {}", messageId, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Returns a map of messageId -> readByCount for all messages in a group sent by a specific user.
     */
    public java.util.Map<Integer, Integer> getReadCountsForUserMessages(int groupId, int senderId) {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        String sql = """
                SELECT gm.id, COUNT(DISTINCT gmr.user_id) as read_count
                FROM group_messages gm
                LEFT JOIN group_message_reads gmr ON gm.id = gmr.message_id
                WHERE gm.group_id = ? AND gm.sender_id = ?
                GROUP BY gm.id
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, senderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                counts.put(rs.getInt("id"), rs.getInt("read_count"));
            }
        } catch (SQLException e) {
            logger.error("Error getting read counts: {}", e.getMessage(), e);
        }
        return counts;
    }

    // ═══════════════════════════════════════════════════════════
    //  REACTIONS — Réactions emoji sur les messages de groupe
    // ═══════════════════════════════════════════════════════════

    /**
     * Toggle a reaction on a group message. If the user already reacted with the same emoji, remove it.
     */
    public void toggleReaction(int messageId, int userId, String emoji) {
        String checkSql = "SELECT id FROM group_message_reactions WHERE message_id = ? AND user_id = ? AND emoji = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, messageId);
            checkStmt.setInt(2, userId);
            checkStmt.setString(3, emoji);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                try (PreparedStatement delStmt = conn.prepareStatement(
                        "DELETE FROM group_message_reactions WHERE id = ?")) {
                    delStmt.setInt(1, rs.getInt("id"));
                    delStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insStmt = conn.prepareStatement(
                        "INSERT INTO group_message_reactions (message_id, user_id, emoji) VALUES (?, ?, ?)")) {
                    insStmt.setInt(1, messageId);
                    insStmt.setInt(2, userId);
                    insStmt.setString(3, emoji);
                    insStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Error toggling group reaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all reactions for messages in a group.
     * Returns a map of messageId -> map of emoji -> count.
     */
    public java.util.Map<Integer, java.util.Map<String, Integer>> getReactionsForGroup(int groupId) {
        java.util.Map<Integer, java.util.Map<String, Integer>> result = new java.util.HashMap<>();
        String sql = """
                SELECT gmr.message_id, gmr.emoji, COUNT(*) as cnt
                FROM group_message_reactions gmr
                JOIN group_messages gm ON gmr.message_id = gm.id
                WHERE gm.group_id = ?
                GROUP BY gmr.message_id, gmr.emoji
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int msgId = rs.getInt("message_id");
                String emoji = rs.getString("emoji");
                int count = rs.getInt("cnt");
                result.computeIfAbsent(msgId, k -> new java.util.LinkedHashMap<>()).put(emoji, count);
            }
        } catch (SQLException e) {
            logger.error("Error fetching group reactions: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * Get the emojis the current user has reacted with for messages in a group.
     */
    public java.util.Map<Integer, java.util.Set<String>> getUserReactionsForGroup(int groupId, int userId) {
        java.util.Map<Integer, java.util.Set<String>> result = new java.util.HashMap<>();
        String sql = """
                SELECT gmr.message_id, gmr.emoji
                FROM group_message_reactions gmr
                JOIN group_messages gm ON gmr.message_id = gm.id
                WHERE gm.group_id = ? AND gmr.user_id = ?
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int msgId = rs.getInt("message_id");
                String emoji = rs.getString("emoji");
                result.computeIfAbsent(msgId, k -> new java.util.HashSet<>()).add(emoji);
            }
        } catch (SQLException e) {
            logger.error("Error fetching user group reactions: {}", e.getMessage(), e);
        }
        return result;
    }
}
