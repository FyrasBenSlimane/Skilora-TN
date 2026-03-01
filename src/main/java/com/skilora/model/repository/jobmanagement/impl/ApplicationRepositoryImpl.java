package com.skilora.model.repository.jobmanagement.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.jobmanagement.Application;
import com.skilora.model.repository.jobmanagement.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicationRepositoryImpl implements ApplicationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationRepositoryImpl.class);

    @Override
    public Optional<Application> findById(int id) {
        String sql = "SELECT * FROM applications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding application by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Application> findAll() {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications ORDER BY applied_date DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                applications.add(mapResultSetToApplication(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all applications", e);
        }
        return applications;
    }

    @Override
    public List<Application> findByJobOfferId(int jobOfferId) {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE job_offer_id = ? ORDER BY applied_date DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, jobOfferId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapResultSetToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding applications by job_offer_id: {}", jobOfferId, e);
        }
        return applications;
    }

    @Override
    public List<Application> findByCandidateProfileId(int profileId) {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE candidate_profile_id = ? ORDER BY applied_date DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, profileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapResultSetToApplication(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding applications by profile_id: {}", profileId, e);
        }
        return applications;
    }

    @Override
    public void save(Application application) {
        String sql = "INSERT INTO applications (job_offer_id, candidate_profile_id, status, cover_letter, custom_cv_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, application.getJobOfferId());
            pstmt.setInt(2, application.getCandidateProfileId());
            pstmt.setString(3, application.getStatus());
            pstmt.setString(4, application.getCoverLetter());
            pstmt.setString(5, application.getCustomCvUrl());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        application.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving application", e);
        }
    }

    @Override
    public void update(Application application) {
        String sql = "UPDATE applications SET job_offer_id = ?, candidate_profile_id = ?, status = ?, cover_letter = ?, custom_cv_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, application.getJobOfferId());
            pstmt.setInt(2, application.getCandidateProfileId());
            pstmt.setString(3, application.getStatus());
            pstmt.setString(4, application.getCoverLetter());
            pstmt.setString(5, application.getCustomCvUrl());
            pstmt.setInt(6, application.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating application", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting application", e);
        }
    }

    private Application mapResultSetToApplication(ResultSet rs) throws SQLException {
        Application application = new Application();
        application.setId(rs.getInt("id"));
        application.setJobOfferId(rs.getInt("job_offer_id"));
        application.setCandidateProfileId(rs.getInt("candidate_profile_id"));
        application.setStatus(rs.getString("status"));
        
        Timestamp appliedDateTs = rs.getTimestamp("applied_date");
        if (appliedDateTs != null) {
            application.setAppliedDate(appliedDateTs.toLocalDateTime());
        }
        
        application.setCoverLetter(rs.getString("cover_letter"));
        application.setCustomCvUrl(rs.getString("custom_cv_url"));
        return application;
    }
}
