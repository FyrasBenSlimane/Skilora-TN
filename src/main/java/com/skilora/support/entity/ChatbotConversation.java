package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChatbotConversation {
    private int id;
    private int userId;
    private LocalDateTime startedDate;
    private LocalDateTime endedDate;
    private String status;
    private Integer escalatedToTicketId;
    
    public ChatbotConversation() {}
    
    public ChatbotConversation(int userId, String status) {
        this.userId = userId;
        this.status = status;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public LocalDateTime getStartedDate() { return startedDate; }
    public void setStartedDate(LocalDateTime startedDate) { this.startedDate = startedDate; }
    
    public LocalDateTime getEndedDate() { return endedDate; }
    public void setEndedDate(LocalDateTime endedDate) { this.endedDate = endedDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getEscalatedToTicketId() { return escalatedToTicketId; }
    public void setEscalatedToTicketId(Integer escalatedToTicketId) { 
        this.escalatedToTicketId = escalatedToTicketId; 
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatbotConversation that = (ChatbotConversation) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ChatbotConversation{" +
                "id=" + id +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", startedDate=" + startedDate +
                '}';
    }
}
