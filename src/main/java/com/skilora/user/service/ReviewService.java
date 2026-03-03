package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.Review;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {
    private static volatile ReviewService instance;

    private ReviewService() {}

    public static ReviewService getInstance() {
        if (instance == null) {
            synchronized (ReviewService.class) {
                if (instance == null) instance = new ReviewService();
            }
        }
        return instance;
    }

    public void create(Review review) throws SQLException {
        // Schema (DatabaseInitializer): reviews(reviewer_id, reviewed_user_id, rating, comment, is_anonymous, created_date)
        String sql = "INSERT INTO reviews (reviewer_id, reviewed_user_id, rating, comment, created_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, review.getReviewerId());
            stmt.setInt(2, review.getTargetUserId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getComment());
            stmt.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    review.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Review> findByTargetUserId(int targetUserId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        // Schema uses reviewed_user_id + created_date; map reviewed_user_id -> targetUserId in entity
        String sql = "SELECT r.*, u.full_name, u.photo_url " +
                     "FROM reviews r JOIN users u ON r.reviewer_id = u.id " +
                     "WHERE r.reviewed_user_id = ? ORDER BY r.created_date DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, targetUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        return reviews;
    }

    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        // job_id column is not present in current schema; keep jobId at default 0
        r.setReviewerId(rs.getInt("reviewer_id"));
        r.setTargetUserId(rs.getInt("reviewed_user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        Timestamp ts = rs.getTimestamp("created_date");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());

        r.setReviewerName(rs.getString("full_name"));
        r.setReviewerPhotoUrl(rs.getString("photo_url"));
        return r;
    }
}
