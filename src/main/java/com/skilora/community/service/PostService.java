package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Post;
import com.skilora.community.entity.PostComment;
import com.skilora.community.enums.PostType;
import com.skilora.formation.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    private static volatile PostService instance;

    private PostService() {}

    public static PostService getInstance() {
        if (instance == null) {
            synchronized (PostService.class) {
                if (instance == null) {
                    instance = new PostService();
                }
            }
        }
        return instance;
    }

    public int create(Post post) {
        String sql = "INSERT INTO posts (author_id, content, image_url, post_type, is_published, created_date) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, post.getAuthorId());
            stmt.setString(2, post.getContent());
            stmt.setString(3, post.getImageUrl());
            stmt.setString(4, post.getPostType().name());
            stmt.setBoolean(5, post.isPublished());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Post created with id {}", id);
                
                // Award achievement for first post
                AchievementService.getInstance().checkAndAward(post.getAuthorId());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating post: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Post findById(int id) {
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapPost(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding post: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<Post> getFeed(int userId, int page, int pageSize) {
        List<Post> feed = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.is_published = TRUE
            AND (p.author_id = ? OR p.author_id IN (
                SELECT user_id_2 FROM connections WHERE user_id_1 = ? AND status = 'ACCEPTED'
                UNION
                SELECT user_id_1 FROM connections WHERE user_id_2 = ? AND status = 'ACCEPTED'
            ))
            ORDER BY p.created_date DESC
            LIMIT ? OFFSET ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Post post = mapPost(rs);
                post.setLikedByCurrentUser(isLikedBy(post.getId(), userId));
                feed.add(post);
            }
        } catch (SQLException e) {
            logger.error("Error fetching feed: {}", e.getMessage(), e);
        }
        return feed;
    }

    public List<Post> getByAuthor(int authorId) {
        List<Post> posts = new ArrayList<>();
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.author_id = ?
            ORDER BY p.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching author posts: {}", e.getMessage(), e);
        }
        return posts;
    }

    public boolean update(Post post) {
        String sql = "UPDATE posts SET content = ?, image_url = ?, post_type = ?, updated_date = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, post.getContent());
            stmt.setString(2, post.getImageUrl());
            stmt.setString(3, post.getPostType().name());
            stmt.setInt(4, post.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating post: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting post: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean toggleLike(int postId, int userId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Check if already liked
            String checkSql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setInt(1, postId);
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // Unlike
                    String deleteSql = "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, postId);
                        deleteStmt.setInt(2, userId);
                        deleteStmt.executeUpdate();
                    }
                    // Decrement count
                    updateLikeCount(conn, postId, -1);
                } else {
                    // Like
                    String insertSql = "INSERT INTO post_likes (post_id, user_id, created_date) VALUES (?, ?, NOW())";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, postId);
                        insertStmt.setInt(2, userId);
                        insertStmt.executeUpdate();
                    }
                    // Increment count
                    updateLikeCount(conn, postId, 1);
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error toggling like: {}", e.getMessage(), e);
        }
        return false;
    }

    private void updateLikeCount(Connection conn, int postId, int delta) throws SQLException {
        String sql = "UPDATE posts SET likes_count = likes_count + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, postId);
            stmt.executeUpdate();
        }
    }

    public boolean isLikedBy(int postId, int userId) {
        String sql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking like: {}", e.getMessage(), e);
        }
        return false;
    }

    public int addComment(PostComment comment) {
        String sql = "INSERT INTO post_comments (post_id, author_id, content, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getPostId());
            stmt.setInt(2, comment.getAuthorId());
            stmt.setString(3, comment.getContent());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                // Update comment count
                String updateSql = "UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, comment.getPostId());
                    updateStmt.executeUpdate();
                }
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error adding comment: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<PostComment> getComments(int postId) {
        List<PostComment> comments = new ArrayList<>();
        String sql = """
            SELECT c.*, u.full_name as author_name, u.photo_url as author_photo
            FROM post_comments c
            JOIN users u ON c.author_id = u.id
            WHERE c.post_id = ?
            ORDER BY c.created_date ASC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                comments.add(mapComment(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching comments: {}", e.getMessage(), e);
        }
        return comments;
    }

    private Post mapPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("id"));
        post.setAuthorId(rs.getInt("author_id"));
        post.setContent(rs.getString("content"));
        post.setImageUrl(rs.getString("image_url"));
        post.setPostType(PostType.valueOf(rs.getString("post_type")));
        post.setLikesCount(rs.getInt("likes_count"));
        post.setCommentsCount(rs.getInt("comments_count"));
        post.setSharesCount(rs.getInt("shares_count"));
        post.setPublished(rs.getBoolean("is_published"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) post.setCreatedDate(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_date");
        if (updated != null) post.setUpdatedDate(updated.toLocalDateTime());
        
        post.setAuthorName(rs.getString("author_name"));
        post.setAuthorPhoto(rs.getString("author_photo"));
        
        return post;
    }

    private PostComment mapComment(ResultSet rs) throws SQLException {
        PostComment comment = new PostComment();
        comment.setId(rs.getInt("id"));
        comment.setPostId(rs.getInt("post_id"));
        comment.setAuthorId(rs.getInt("author_id"));
        comment.setContent(rs.getString("content"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) comment.setCreatedDate(created.toLocalDateTime());
        
        comment.setAuthorName(rs.getString("author_name"));
        comment.setAuthorPhoto(rs.getString("author_photo"));
        
        return comment;
    }
}
