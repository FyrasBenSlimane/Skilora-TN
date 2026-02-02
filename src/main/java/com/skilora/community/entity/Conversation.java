package com.skilora.community.entity;

import java.time.LocalDateTime;

public class Conversation {
    private int id;
    private int participant1;
    private int participant2;
    private LocalDateTime lastMessageDate;
    private boolean isArchived1;
    private boolean isArchived2;
    private LocalDateTime createdDate;
    
    // Transient fields for UI display
    private String otherUserName;
    private String otherUserPhoto;
    private String lastMessagePreview;
    private int unreadCount;
    
    public Conversation() {
        this.isArchived1 = false;
        this.isArchived2 = false;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getParticipant1() { return participant1; }
    public void setParticipant1(int participant1) { this.participant1 = participant1; }
    
    public int getParticipant2() { return participant2; }
    public void setParticipant2(int participant2) { this.participant2 = participant2; }
    
    public LocalDateTime getLastMessageDate() { return lastMessageDate; }
    public void setLastMessageDate(LocalDateTime lastMessageDate) { this.lastMessageDate = lastMessageDate; }
    
    public boolean isArchived1() { return isArchived1; }
    public void setArchived1(boolean archived1) { isArchived1 = archived1; }
    
    public boolean isArchived2() { return isArchived2; }
    public void setArchived2(boolean archived2) { isArchived2 = archived2; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    
    public String getOtherUserPhoto() { return otherUserPhoto; }
    public void setOtherUserPhoto(String otherUserPhoto) { this.otherUserPhoto = otherUserPhoto; }
    
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
    
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
