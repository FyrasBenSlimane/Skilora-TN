package com.skilora.community.entity;

import java.time.LocalDateTime;

public class PostComment {
    private int id;
    private int postId;
    private int authorId;
    private String content;
    private LocalDateTime createdDate;
    
    // Transient fields for UI display
    private String authorName;
    private String authorPhoto;
    
    public PostComment() {}
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getAuthorPhoto() { return authorPhoto; }
    public void setAuthorPhoto(String authorPhoto) { this.authorPhoto = authorPhoto; }
}
