package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.BlogArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BlogService - Full CRUD for blog_articles table.
 */
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

    // ── CREATE ──

    public int create(BlogArticle article) {
        String sql = "INSERT INTO blog_articles (author_id, title, content, summary, cover_image_url, " +
                     "category, tags, is_published, published_date, created_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, article.getAuthorId());
            stmt.setString(2, article.getTitle());
            stmt.setString(3, article.getContent());
            stmt.setString(4, article.getSummary());
            stmt.setString(5, article.getCoverImageUrl());
            stmt.setString(6, article.getCategory());
            stmt.setString(7, article.getTags());
            stmt.setBoolean(8, article.isPublished());
            if (article.isPublished()) {
                stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(9, Types.TIMESTAMP);
            }
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Blog article created with id {}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating blog article: {}", e.getMessage(), e);
        }
        return -1;
    }

    // ── READ ──

    public BlogArticle findById(int id) {
        String sql = """
            SELECT b.*, u.full_name as author_name, u.photo_url as author_photo
            FROM blog_articles b
            JOIN users u ON b.author_id = u.id
            WHERE b.id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapArticle(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding blog article: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<BlogArticle> findAll() {
        List<BlogArticle> articles = new ArrayList<>();
        String sql = """
            SELECT b.*, u.full_name as author_name, u.photo_url as author_photo
            FROM blog_articles b
            JOIN users u ON b.author_id = u.id
            ORDER BY b.created_date DESC
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(mapArticle(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all blog articles: {}", e.getMessage(), e);
        }
        return articles;
    }

    public List<BlogArticle> findPublished() {
        List<BlogArticle> articles = new ArrayList<>();
        String sql = """
            SELECT b.*, u.full_name as author_name, u.photo_url as author_photo
            FROM blog_articles b
            JOIN users u ON b.author_id = u.id
            WHERE b.is_published = TRUE
            ORDER BY b.published_date DESC
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(mapArticle(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching published articles: {}", e.getMessage(), e);
        }
        return articles;
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

    public List<BlogArticle> search(String query) {
        List<BlogArticle> articles = new ArrayList<>();
        String sql = """
            SELECT b.*, u.full_name as author_name, u.photo_url as author_photo
            FROM blog_articles b
            JOIN users u ON b.author_id = u.id
            WHERE b.is_published = TRUE AND (b.title LIKE ? OR b.content LIKE ? OR b.tags LIKE ?)
            ORDER BY b.created_date DESC
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                articles.add(mapArticle(rs));
            }
        } catch (SQLException e) {
            logger.error("Error searching blog articles: {}", e.getMessage(), e);
        }
        return articles;
    }

    // ── UPDATE ──

    public boolean update(BlogArticle article) {
        String sql = "UPDATE blog_articles SET title = ?, content = ?, summary = ?, cover_image_url = ?, " +
                     "category = ?, tags = ?, is_published = ?, published_date = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, article.getTitle());
            stmt.setString(2, article.getContent());
            stmt.setString(3, article.getSummary());
            stmt.setString(4, article.getCoverImageUrl());
            stmt.setString(5, article.getCategory());
            stmt.setString(6, article.getTags());
            stmt.setBoolean(7, article.isPublished());
            if (article.isPublished() && article.getPublishedDate() == null) {
                stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            } else if (article.getPublishedDate() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(article.getPublishedDate()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }
            stmt.setInt(9, article.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating blog article: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean incrementViews(int articleId) {
        String sql = "UPDATE blog_articles SET views_count = views_count + 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, articleId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error incrementing views: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── DELETE ──

    public boolean delete(int id) {
        String sql = "DELETE FROM blog_articles WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting blog article: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── MAPPER ──

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
