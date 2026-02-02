package com.skilora.community.entity;

import com.skilora.community.enums.ConnectionStatus;
import java.time.LocalDateTime;

public class Connection {
    private int id;
    private int userId1;
    private int userId2;
    private ConnectionStatus status;
    private String connectionType;
    private LocalDateTime createdDate;
    private LocalDateTime lastInteraction;
    private int strengthScore;
    
    // Transient fields for UI display
    private String otherUserName;
    private String otherUserPhoto;
    
    public Connection() {
        this.status = ConnectionStatus.PENDING;
        this.connectionType = "PROFESSIONAL";
        this.strengthScore = 0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId1() { return userId1; }
    public void setUserId1(int userId1) { this.userId1 = userId1; }
    
    public int getUserId2() { return userId2; }
    public void setUserId2(int userId2) { this.userId2 = userId2; }
    
    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }
    
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
    
    public int getStrengthScore() { return strengthScore; }
    public void setStrengthScore(int strengthScore) { this.strengthScore = strengthScore; }
    
    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    
    public String getOtherUserPhoto() { return otherUserPhoto; }
    public void setOtherUserPhoto(String otherUserPhoto) { this.otherUserPhoto = otherUserPhoto; }
}
