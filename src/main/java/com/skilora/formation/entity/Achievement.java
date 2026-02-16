package com.skilora.formation.entity;

import com.skilora.formation.enums.BadgeRarity;
import java.time.LocalDateTime;

public class Achievement {
    private int id;
    private int userId;
    private String badgeType;
    private String title;
    private String description;
    private String iconUrl;
    private LocalDateTime earnedDate;
    private BadgeRarity rarity;
    private int points;
    
    public Achievement() {
        this.rarity = BadgeRarity.COMMON;
        this.points = 10;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    
    public LocalDateTime getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDateTime earnedDate) { this.earnedDate = earnedDate; }
    
    public BadgeRarity getRarity() { return rarity; }
    public void setRarity(BadgeRarity rarity) { this.rarity = rarity; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
