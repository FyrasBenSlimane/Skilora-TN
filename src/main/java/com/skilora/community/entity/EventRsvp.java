package com.skilora.community.entity;

import java.time.LocalDateTime;

public class EventRsvp {
    private int id;
    private int eventId;
    private int userId;
    private String status; // GOING, MAYBE, NOT_GOING
    private LocalDateTime rsvpDate;
    
    public EventRsvp() {
        this.status = "GOING";
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getRsvpDate() { return rsvpDate; }
    public void setRsvpDate(LocalDateTime rsvpDate) { this.rsvpDate = rsvpDate; }
}
