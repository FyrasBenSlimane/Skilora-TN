package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class TicketMessage {
    private int id;
    private int ticketId;
    private int senderId;
    private String message;
    private boolean isInternal;
    private LocalDateTime createdDate;
    
    // Transient fields
    private String senderName;
    private String senderRole;
    
    public TicketMessage() {}
    
    public TicketMessage(int ticketId, int senderId, String message, boolean isInternal) {
        this.ticketId = ticketId;
        this.senderId = senderId;
        this.message = message;
        this.isInternal = isInternal;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketMessage that = (TicketMessage) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "TicketMessage{" +
                "id=" + id +
                ", ticketId=" + ticketId +
                ", senderId=" + senderId +
                ", isInternal=" + isInternal +
                ", createdDate=" + createdDate +
                '}';
    }
}
