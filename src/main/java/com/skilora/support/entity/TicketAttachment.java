package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class TicketAttachment {
    private int id;
    private int ticketId;
    private Integer messageId;
    private String fileName;
    private String mimeType;
    private String filePath;
    private Long fileSize;
    private LocalDateTime createdDate;

    public TicketAttachment() {}

    public TicketAttachment(int ticketId, Integer messageId, String fileName, String mimeType, String filePath, Long fileSize) {
        this.ticketId = ticketId;
        this.messageId = messageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketAttachment that = (TicketAttachment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

