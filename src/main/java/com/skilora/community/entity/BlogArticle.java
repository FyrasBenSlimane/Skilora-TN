package com.skilora.community.entity;

import java.time.LocalDateTime;

public class BlogArticle {
    private int id;
    private int authorId;
    private String title;
    private String content;
    private String summary;
    private String coverImageUrl;
    private String category;
    private String tags;
    private int viewsCount;
    private int likesCount;
    private boolean isPublished;
    private LocalDateTime publishedDate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Transient fields for UI display
    private String authorName;
    private String authorPhoto;
    
    public BlogArticle() {
        this.isPublished = false;
        this.viewsCount = 0;
        this.likesCount = 0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }
    
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }
    
    public LocalDateTime getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDateTime publishedDate) { this.publishedDate = publishedDate; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getAuthorPhoto() { return authorPhoto; }
    public void setAuthorPhoto(String authorPhoto) { this.authorPhoto = authorPhoto; }
}
