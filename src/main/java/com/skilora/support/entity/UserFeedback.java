package com.skilora.support.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserFeedback {
    private int id;
    private int userId;
    private String feedbackType;
    private int rating;
    private String comment;
    private String category;
    private boolean isResolved;
    private LocalDateTime createdDate;
    
    // Transient fields
    private String userName;
    
    public UserFeedback() {}
    
    public UserFeedback(int userId, String feedbackType, int rating, String comment) {
        this.userId = userId;
        this.feedbackType = feedbackType;
        this.rating = rating;
        this.comment = comment;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { isResolved = resolved; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFeedback that = (UserFeedback) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "UserFeedback{" +
                "id=" + id +
                ", userId=" + userId +
                ", feedbackType='" + feedbackType + '\'' +
                ", rating=" + rating +
                ", createdDate=" + createdDate +
                '}';
    }
}
