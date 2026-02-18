package com.skilora.formation.entity;

import com.skilora.formation.enums.MentorshipStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Mentorship {
    private int id;
    private int mentorId;
    private int menteeId;
    private MentorshipStatus status;
    private String topic;
    private String goals;
    private LocalDate startDate;
    private LocalDate endDate;
    private int rating;
    private String feedback;
    private LocalDateTime createdDate;
    
    // Transient fields for UI display
    private String mentorName;
    private String menteeName;
    private String mentorPhoto;
    
    public Mentorship() {
        this.status = MentorshipStatus.PENDING;
        this.rating = 0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMentorId() { return mentorId; }
    public void setMentorId(int mentorId) { this.mentorId = mentorId; }
    
    public int getMenteeId() { return menteeId; }
    public void setMenteeId(int menteeId) { this.menteeId = menteeId; }
    
    public MentorshipStatus getStatus() { return status; }
    public void setStatus(MentorshipStatus status) { this.status = status; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }
    
    public String getMenteeName() { return menteeName; }
    public void setMenteeName(String menteeName) { this.menteeName = menteeName; }
    
    public String getMentorPhoto() { return mentorPhoto; }
    public void setMentorPhoto(String mentorPhoto) { this.mentorPhoto = mentorPhoto; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mentorship that = (Mentorship) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "Mentorship{id=" + id + ", mentorId=" + mentorId + ", menteeId=" + menteeId + ", status=" + status + ", topic='" + topic + "'}";
    }
}
