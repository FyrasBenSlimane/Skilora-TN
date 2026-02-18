package com.skilora.support.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.FAQArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FAQService {

    private static final Logger logger = LoggerFactory.getLogger(FAQService.class);
    private static volatile FAQService instance;

    private FAQService() {}

    public static FAQService getInstance() {
        if (instance == null) {
            synchronized (FAQService.class) {
                if (instance == null) {
                    instance = new FAQService();
                }
            }
        }
        return instance;
    }

    public int create(FAQArticle article) {
        String sql = """
            INSERT INTO faq_articles (category, question, answer, language, is_published)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, article.getCategory());
            stmt.setString(2, article.getQuestion());
            stmt.setString(3, article.getAnswer());
            stmt.setString(4, article.getLanguage());
            stmt.setBoolean(5, article.isPublished());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to create FAQ article", e);
        }
        return -1;
    }

    public FAQArticle findById(int id) {
        String sql = "SELECT * FROM faq_articles WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find FAQ article by id: {}", id, e);
        }
        return null;
    }

    public List<FAQArticle> findAll() {
        String sql = "SELECT * FROM faq_articles WHERE is_published = TRUE ORDER BY created_date DESC";
        List<FAQArticle> articles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                articles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all FAQ articles", e);
        }
        return articles;
    }

    /**
     * Returns ALL FAQ articles including unpublished drafts.
     * Use this for admin views where drafts need to be visible.
     */
    public List<FAQArticle> findAllIncludingDrafts() {
        String sql = "SELECT * FROM faq_articles ORDER BY created_date DESC";
        List<FAQArticle> articles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                articles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all FAQ articles (including drafts)", e);
        }
        return articles;
    }

    public List<FAQArticle> findByCategory(String category) {
        String sql = "SELECT * FROM faq_articles WHERE category = ? AND is_published = TRUE ORDER BY created_date DESC";
        List<FAQArticle> articles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find FAQ articles by category", e);
        }
        return articles;
    }

    public List<FAQArticle> search(String query) {
        String sql = """
            SELECT * FROM faq_articles 
            WHERE is_published = TRUE 
            AND (question LIKE ? OR answer LIKE ?)
            ORDER BY created_date DESC
            """;
        List<FAQArticle> articles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    articles.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to search FAQ articles", e);
        }
        return articles;
    }

    public boolean update(FAQArticle article) {
        String sql = """
            UPDATE faq_articles 
            SET category = ?, question = ?, answer = ?, language = ?, is_published = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, article.getCategory());
            stmt.setString(2, article.getQuestion());
            stmt.setString(3, article.getAnswer());
            stmt.setString(4, article.getLanguage());
            stmt.setBoolean(5, article.isPublished());
            stmt.setInt(6, article.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update FAQ article", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM faq_articles WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete FAQ article", e);
        }
        return false;
    }

    private FAQArticle mapResultSet(ResultSet rs) throws SQLException {
        FAQArticle article = new FAQArticle();
        article.setId(rs.getInt("id"));
        article.setCategory(rs.getString("category"));
        article.setQuestion(rs.getString("question"));
        article.setAnswer(rs.getString("answer"));
        article.setLanguage(rs.getString("language"));
        article.setHelpfulCount(rs.getInt("helpful_count"));
        article.setViewCount(rs.getInt("view_count"));
        article.setPublished(rs.getBoolean("is_published"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) article.setCreatedDate(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_date");
        if (updated != null) article.setUpdatedDate(updated.toLocalDateTime());
        
        return article;
    }
}
