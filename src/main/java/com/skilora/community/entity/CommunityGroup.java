package com.skilora.community.entity;

import java.time.LocalDateTime;

public class CommunityGroup {
    private int id;
    private String name;
    private String description;
    private String category;
    private String coverImageUrl;
    private int creatorId;
    private int memberCount;
    private boolean isPublic;
    private LocalDateTime createdDate;
    
    // Transient fields for UI display
    private String creatorName;
    private boolean isMember;
    
    public CommunityGroup() {
        this.isPublic = true;
        this.memberCount = 1;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    
    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    
    public boolean isMember() { return isMember; }
    public void setMember(boolean member) { isMember = member; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityGroup that = (CommunityGroup) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "CommunityGroup{id=" + id + ", name='" + name + "', category='" + category + "', memberCount=" + memberCount + "}";
    }
}
