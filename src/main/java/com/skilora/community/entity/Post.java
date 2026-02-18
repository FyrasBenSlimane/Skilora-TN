package com.skilora.community.entity;

import com.skilora.community.enums.PostType;
import java.time.LocalDateTime;

public class Post {
    private int id;
    private int authorId;
    private String content;
    private String imageUrl;
    private PostType postType;
    private int likesCount;
    private int commentsCount;
    private int sharesCount;
    private boolean isPublished;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // Transient fields for UI display
    private String authorName;
    private String authorPhoto;
    private boolean isLikedByCurrentUser;
    
    public Post() {
        this.postType = PostType.STATUS;
        this.isPublished = true;
        this.likesCount = 0;
        this.commentsCount = 0;
        this.sharesCount = 0;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public PostType getPostType() { return postType; }
    public void setPostType(PostType postType) { this.postType = postType; }
    
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    
    public int getSharesCount() { return sharesCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }
    
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getAuthorPhoto() { return authorPhoto; }
    public void setAuthorPhoto(String authorPhoto) { this.authorPhoto = authorPhoto; }
    
    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { isLikedByCurrentUser = likedByCurrentUser; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post that = (Post) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }

    @Override
    public String toString() {
        return "Post{id=" + id + ", authorId=" + authorId + ", postType=" + postType + ", likesCount=" + likesCount + "}";
    }
}
