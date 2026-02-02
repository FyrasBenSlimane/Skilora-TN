package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.BlogArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlogService {

    private static final Logger logger = LoggerFactory.getLogger(BlogService.class);
    private static volatile BlogService instance;

    private BlogService() {}

    public static BlogService getInstance() {
        if (instance == null) {
            synchronized (BlogService.class) {
                if (instance == null) {
                    instance = new BlogService();
                }
            }
        }
        return instance;
    }

    public List<BlogArticle> findByAuthor(int authorId) {
        List<BlogArticle> articles = new ArrayList<>();
        String sql = """
            SELECT b.*, u.full_name as author_name, u.photo_url as author_photo
            FROM blog_articles b
            JOIN users u ON b.author_id = u.id
            WHERE b.author_id = ?
            ORDER BY b.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(mapArticle(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching author articles: {}", e.getMessage(), e);
        }
        return articles;
    }

    private BlogArticle mapArticle(ResultSet rs) throws SQLException {
        BlogArticle article = new BlogArticle();
        article.setId(rs.getInt("id"));
        article.setAuthorId(rs.getInt("author_id"));
        article.setTitle(rs.getString("title"));
        article.setContent(rs.getString("content"));
        article.setSummary(rs.getString("summary"));
        article.setCoverImageUrl(rs.getString("cover_image_url"));
        article.setCategory(rs.getString("category"));
        article.setTags(rs.getString("tags"));
        article.setViewsCount(rs.getInt("views_count"));
        article.setLikesCount(rs.getInt("likes_count"));
        article.setPublished(rs.getBoolean("is_published"));
        
        Timestamp published = rs.getTimestamp("published_date");
        if (published != null) article.setPublishedDate(published.toLocalDateTime());
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) article.setCreatedDate(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_date");
        if (updated != null) article.setUpdatedDate(updated.toLocalDateTime());
        
        article.setAuthorName(rs.getString("author_name"));
        article.setAuthorPhoto(rs.getString("author_photo"));
        
        return article;
    }
}
