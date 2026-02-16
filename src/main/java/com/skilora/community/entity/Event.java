package com.skilora.community.entity;

import com.skilora.community.enums.EventType;
import com.skilora.community.enums.EventStatus;
import java.time.LocalDateTime;

public class Event {
    private int id;
    private int organizerId;
    private String title;
    private String description;
    private EventType eventType;
    private String location;
    private boolean isOnline;
    private String onlineLink;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int maxAttendees;
    private int currentAttendees;
    private String imageUrl;
    private EventStatus status;
    private LocalDateTime createdDate;
    
    // Transient fields for UI display
    private String organizerName;
    private boolean isAttending;
    
    public Event() {
        this.eventType = EventType.MEETUP;
        this.status = EventStatus.UPCOMING;
        this.isOnline = false;
        this.maxAttendees = 0;
        this.currentAttendees = 0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getOrganizerId() { return organizerId; }
    public void setOrganizerId(int organizerId) { this.organizerId = organizerId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public String getOnlineLink() { return onlineLink; }
    public void setOnlineLink(String onlineLink) { this.onlineLink = onlineLink; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public int getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(int maxAttendees) { this.maxAttendees = maxAttendees; }
    
    public int getCurrentAttendees() { return currentAttendees; }
    public void setCurrentAttendees(int currentAttendees) { this.currentAttendees = currentAttendees; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    
    public boolean isAttending() { return isAttending; }
    public void setAttending(boolean attending) { isAttending = attending; }
}
