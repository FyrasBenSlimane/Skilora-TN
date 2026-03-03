package com.skilora.community.entity;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int conversationId;
    private int senderId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdDate;
    
    // Champs média (image/vidéo/vocal)
    private String messageType;  // TEXT, IMAGE, VIDEO, VOCAL
    private String mediaUrl;     // URL du fichier média (Cloudinary ou local)
    private String fileName;     // Nom original du fichier
    private int duration;        // Durée en secondes (pour les messages vocaux)
    
    // Transient fields for UI display
    private String senderName;
    
    public Message() {
        this.isRead = false;
        this.messageType = "TEXT";
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    // Helpers
    public boolean isImage() { return "IMAGE".equals(messageType); }
    public boolean isVideo() { return "VIDEO".equals(messageType); }
    public boolean isVocal() { return "VOCAL".equals(messageType); }
    public boolean isText() { return "TEXT".equals(messageType) || messageType == null; }
    public boolean hasMedia() { return mediaUrl != null && !mediaUrl.isBlank(); }
}
