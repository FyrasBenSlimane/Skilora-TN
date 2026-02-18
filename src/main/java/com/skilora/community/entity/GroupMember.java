package com.skilora.community.entity;

import java.time.LocalDateTime;

public class GroupMember {
    private int id;
    private int groupId;
    private int userId;
    private String role; // ADMIN, MODERATOR, MEMBER
    private LocalDateTime joinedDate;
    
    // Transient fields for UI display
    private String userName;
    private String userPhoto;
    
    public GroupMember() {
        this.role = "MEMBER";
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public LocalDateTime getJoinedDate() { return joinedDate; }
    public void setJoinedDate(LocalDateTime joinedDate) { this.joinedDate = joinedDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserPhoto() { return userPhoto; }
    public void setUserPhoto(String userPhoto) { this.userPhoto = userPhoto; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMember that = (GroupMember) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "GroupMember{id=" + id + ", groupId=" + groupId + ", userId=" + userId + ", role='" + role + "'}";
    }
}
