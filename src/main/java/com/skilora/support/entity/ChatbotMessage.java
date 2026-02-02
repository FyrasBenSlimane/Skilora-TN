package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChatbotMessage {
    private int id;
    private int conversationId;
    private String sender;
    private String message;
    private String intent;
    private double confidence;
    private LocalDateTime createdDate;
    
    public ChatbotMessage() {}
    
    public ChatbotMessage(int conversationId, String sender, String message) {
        this.conversationId = conversationId;
        this.sender = sender;
        this.message = message;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatbotMessage that = (ChatbotMessage) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ChatbotMessage{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", sender='" + sender + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
