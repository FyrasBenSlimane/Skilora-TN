package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.SavedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SavedJobService {
    private static final Logger logger = LoggerFactory.getLogger(SavedJobService.class);
    private static volatile SavedJobService instance;

    private SavedJobService() {}

    public static SavedJobService getInstance() {
        if (instance == null) {
            synchronized (SavedJobService.class) {
                if (instance == null) {
                    instance = new SavedJobService();
                }
            }
        }
        return instance;
    }

    public boolean isSaved(int userId, String jobUrl) {
        if (userId <= 0 || jobUrl == null || jobUrl.isBlank()) return false;
        String sql = "SELECT 1 FROM saved_jobs WHERE user_id = ? AND job_url = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, jobUrl);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.debug("isSaved failed: {}", e.getMessage());
            return false;
        }
    }

    public int save(SavedJob job) {
        if (job == null) return -1;
        if (job.getUserId() <= 0) throw new IllegalArgumentException("userId required");
        if (job.getJobUrl() == null || job.getJobUrl().isBlank()) throw new IllegalArgumentException("jobUrl required");

        String sql = """
                INSERT INTO saved_jobs (user_id, job_url, job_title, company_name, location, source)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    job_title = VALUES(job_title),
                    company_name = VALUES(company_name),
                    location = VALUES(location),
                    source = VALUES(source)
                """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, job.getUserId());
            stmt.setString(2, job.getJobUrl());
            stmt.setString(3, job.getJobTitle());
            stmt.setString(4, job.getCompanyName());
            stmt.setString(5, job.getLocation());
            stmt.setString(6, job.getSource());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    job.setId(id);
                    return id;
                }
            }

            // duplicate path: fetch id
            String q = "SELECT id FROM saved_jobs WHERE user_id = ? AND job_url = ? LIMIT 1";
            try (PreparedStatement qstmt = conn.prepareStatement(q)) {
                qstmt.setInt(1, job.getUserId());
                qstmt.setString(2, job.getJobUrl());
                try (ResultSet rs = qstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed saving job bookmark: {}", e.getMessage(), e);
        }
        return -1;
    }

    public boolean unsave(int userId, String jobUrl) {
        if (userId <= 0 || jobUrl == null || jobUrl.isBlank()) return false;
        String sql = "DELETE FROM saved_jobs WHERE user_id = ? AND job_url = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, jobUrl);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.debug("unsave failed: {}", e.getMessage());
            return false;
        }
    }

    public List<SavedJob> findByUser(int userId) {
        List<SavedJob> list = new ArrayList<>();
        if (userId <= 0) return list;
        String sql = "SELECT * FROM saved_jobs WHERE user_id = ? ORDER BY saved_at DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed loading saved jobs: {}", e.getMessage(), e);
        }
        return list;
    }

    private SavedJob map(ResultSet rs) throws SQLException {
        SavedJob s = new SavedJob();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setJobUrl(rs.getString("job_url"));
        s.setJobTitle(rs.getString("job_title"));
        s.setCompanyName(rs.getString("company_name"));
        s.setLocation(rs.getString("location"));
        s.setSource(rs.getString("source"));
        Timestamp ts = rs.getTimestamp("saved_at");
        if (ts != null) s.setSavedAt(ts.toLocalDateTime());
        else s.setSavedAt(LocalDateTime.now());
        return s;
    }
}

