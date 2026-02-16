package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Achievement;
import com.skilora.formation.entity.Mentorship;
import com.skilora.formation.enums.BadgeRarity;
import com.skilora.formation.enums.MentorshipStatus;
import com.skilora.community.entity.CommunityGroup;
import com.skilora.community.service.ConnectionService;
import com.skilora.community.service.PostService;
import com.skilora.community.service.BlogService;
import com.skilora.community.service.EventService;
import com.skilora.community.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);
    private static volatile AchievementService instance;

    private AchievementService() {}

    public static AchievementService getInstance() {
        if (instance == null) {
            synchronized (AchievementService.class) {
                if (instance == null) {
                    instance = new AchievementService();
                }
            }
        }
        return instance;
    }

    public int award(Achievement achievement) {
        // Check if already has this badge
        if (hasAchievement(achievement.getUserId(), achievement.getBadgeType())) {
            return -1;
        }
        
        String sql = "INSERT INTO achievements (user_id, badge_type, title, description, icon_url, rarity, points, earned_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, achievement.getUserId());
            stmt.setString(2, achievement.getBadgeType());
            stmt.setString(3, achievement.getTitle());
            stmt.setString(4, achievement.getDescription());
            stmt.setString(5, achievement.getIconUrl());
            stmt.setString(6, achievement.getRarity().name());
            stmt.setInt(7, achievement.getPoints());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Achievement awarded: {} to user {}", achievement.getBadgeType(), achievement.getUserId());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error awarding achievement: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<Achievement> findByUserId(int userId) {
        List<Achievement> achievements = new ArrayList<>();
        String sql = "SELECT * FROM achievements WHERE user_id = ? ORDER BY earned_date DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                achievements.add(mapAchievement(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching achievements: {}", e.getMessage(), e);
        }
        return achievements;
    }

    public int getTotalPoints(int userId) {
        String sql = "SELECT SUM(points) as total FROM achievements WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.error("Error getting total points: {}", e.getMessage(), e);
        }
        return 0;
    }

    public boolean hasAchievement(int userId, String badgeType) {
        String sql = "SELECT 1 FROM achievements WHERE user_id = ? AND badge_type = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, badgeType);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking achievement: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Auto-award badges based on user activity
     */
    public void checkAndAward(int userId) {
        try {
            // FIRST_CONNECTION - when connection count reaches 1
            if (!hasAchievement(userId, "FIRST_CONNECTION")) {
                int connectionCount = ConnectionService.getInstance().getConnectionCount(userId);
                if (connectionCount >= 1) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("FIRST_CONNECTION");
                    ach.setTitle("Premier Contact");
                    ach.setDescription("Vous avez établi votre première connexion!");
                    ach.setRarity(BadgeRarity.COMMON);
                    ach.setPoints(10);
                    award(ach);
                }
            }

            // NETWORKER - when connection count reaches 10
            if (!hasAchievement(userId, "NETWORKER")) {
                int connectionCount = ConnectionService.getInstance().getConnectionCount(userId);
                if (connectionCount >= 10) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("NETWORKER");
                    ach.setTitle("Networker");
                    ach.setDescription("Vous avez 10 connexions professionnelles!");
                    ach.setRarity(BadgeRarity.UNCOMMON);
                    ach.setPoints(25);
                    award(ach);
                }
            }

            // SUPER_NETWORKER - when connections reach 50
            if (!hasAchievement(userId, "SUPER_NETWORKER")) {
                int connectionCount = ConnectionService.getInstance().getConnectionCount(userId);
                if (connectionCount >= 50) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("SUPER_NETWORKER");
                    ach.setTitle("Super Networker");
                    ach.setDescription("Vous avez 50 connexions! Impressionnant!");
                    ach.setRarity(BadgeRarity.RARE);
                    ach.setPoints(50);
                    award(ach);
                }
            }

            // FIRST_POST - when user creates first post
            if (!hasAchievement(userId, "FIRST_POST")) {
                List<?> posts = PostService.getInstance().getByAuthor(userId);
                if (!posts.isEmpty()) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("FIRST_POST");
                    ach.setTitle("Première Publication");
                    ach.setDescription("Vous avez partagé votre premier post!");
                    ach.setRarity(BadgeRarity.COMMON);
                    ach.setPoints(10);
                    award(ach);
                }
            }

            // BLOGGER - when user publishes first blog article
            if (!hasAchievement(userId, "BLOGGER")) {
                List<?> articles = BlogService.getInstance().findByAuthor(userId);
                if (!articles.isEmpty()) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("BLOGGER");
                    ach.setTitle("Blogueur");
                    ach.setDescription("Vous avez publié votre premier article!");
                    ach.setRarity(BadgeRarity.UNCOMMON);
                    ach.setPoints(20);
                    award(ach);
                }
            }

            // MENTOR - when user completes first mentorship
            if (!hasAchievement(userId, "MENTOR")) {
                List<?> mentorships = MentorshipService.getInstance().findByMentor(userId);
                boolean hasCompleted = mentorships.stream()
                    .anyMatch(m -> ((Mentorship) m).getStatus() 
                        == MentorshipStatus.COMPLETED);
                if (hasCompleted) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("MENTOR");
                    ach.setTitle("Mentor");
                    ach.setDescription("Vous avez complété votre premier mentorat!");
                    ach.setRarity(BadgeRarity.RARE);
                    ach.setPoints(40);
                    award(ach);
                }
            }

            // EVENT_ORGANIZER - when user creates first event
            if (!hasAchievement(userId, "EVENT_ORGANIZER")) {
                List<?> events = EventService.getInstance().findByOrganizer(userId);
                if (!events.isEmpty()) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("EVENT_ORGANIZER");
                    ach.setTitle("Organisateur");
                    ach.setDescription("Vous avez créé votre premier événement!");
                    ach.setRarity(BadgeRarity.UNCOMMON);
                    ach.setPoints(30);
                    award(ach);
                }
            }

            // COMMUNITY_BUILDER - when user creates a group
            if (!hasAchievement(userId, "COMMUNITY_BUILDER")) {
                List<?> groups = GroupService.getInstance().findByMember(userId);
                boolean isCreator = groups.stream()
                    .anyMatch(g -> ((CommunityGroup) g).getCreatorId() == userId);
                if (isCreator) {
                    Achievement ach = new Achievement();
                    ach.setUserId(userId);
                    ach.setBadgeType("COMMUNITY_BUILDER");
                    ach.setTitle("Bâtisseur de Communauté");
                    ach.setDescription("Vous avez créé un groupe communautaire!");
                    ach.setRarity(BadgeRarity.RARE);
                    ach.setPoints(50);
                    award(ach);
                }
            }

        } catch (Exception e) {
            logger.error("Error checking/awarding achievements: {}", e.getMessage(), e);
        }
    }

    private Achievement mapAchievement(ResultSet rs) throws SQLException {
        Achievement ach = new Achievement();
        ach.setId(rs.getInt("id"));
        ach.setUserId(rs.getInt("user_id"));
        ach.setBadgeType(rs.getString("badge_type"));
        ach.setTitle(rs.getString("title"));
        ach.setDescription(rs.getString("description"));
        ach.setIconUrl(rs.getString("icon_url"));
        ach.setRarity(BadgeRarity.valueOf(rs.getString("rarity")));
        ach.setPoints(rs.getInt("points"));
        
        Timestamp earned = rs.getTimestamp("earned_date");
        if (earned != null) ach.setEarnedDate(earned.toLocalDateTime());
        
        return ach;
    }
}
