package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.CommunityGroup;
import com.skilora.community.entity.GroupMember;
import com.skilora.formation.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private static volatile GroupService instance;

    private GroupService() {}

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
        String sql = "INSERT INTO community_groups (name, description, category, cover_image_url, creator_id, is_public, created_date) " +
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
        if (created != null) group.setCreatedDate(created.toLocalDateTime());
        
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
        if (joined != null) member.setJoinedDate(joined.toLocalDateTime());
        
        member.setUserName(rs.getString("user_name"));
        member.setUserPhoto(rs.getString("user_photo"));
        
        return member;
    }
}
