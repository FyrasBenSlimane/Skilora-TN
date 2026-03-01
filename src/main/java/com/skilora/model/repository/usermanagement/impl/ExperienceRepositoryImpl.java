package com.skilora.model.repository.usermanagement.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.Experience;
import com.skilora.model.repository.usermanagement.ExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExperienceRepositoryImpl implements ExperienceRepository {
    private static final Logger logger = LoggerFactory.getLogger(ExperienceRepositoryImpl.class);

    @Override
    public Optional<Experience> findById(int id) {
        String sql = "SELECT * FROM experiences WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExperience(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding experience by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Experience> findByProfileId(int profileId) {
        List<Experience> experiences = new ArrayList<>();
        String sql = "SELECT * FROM experiences WHERE profile_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, profileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    experiences.add(mapResultSetToExperience(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding experiences by profile_id: {}", profileId, e);
        }
        return experiences;
    }

    @Override
    public void save(Experience experience) {
        String sql = "INSERT INTO experiences (profile_id, company, position, start_date, end_date, description, current_job) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, experience.getProfileId());
            pstmt.setString(2, experience.getCompany());
            pstmt.setString(3, experience.getPosition());
            pstmt.setDate(4, experience.getStartDate() != null ? Date.valueOf(experience.getStartDate()) : null);
            pstmt.setDate(5, experience.getEndDate() != null ? Date.valueOf(experience.getEndDate()) : null);
            pstmt.setString(6, experience.getDescription());
            pstmt.setBoolean(7, experience.isCurrentJob());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        experience.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving experience", e);
        }
    }

    @Override
    public void update(Experience experience) {
        String sql = "UPDATE experiences SET profile_id = ?, company = ?, position = ?, start_date = ?, end_date = ?, description = ?, current_job = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, experience.getProfileId());
            pstmt.setString(2, experience.getCompany());
            pstmt.setString(3, experience.getPosition());
            pstmt.setDate(4, experience.getStartDate() != null ? Date.valueOf(experience.getStartDate()) : null);
            pstmt.setDate(5, experience.getEndDate() != null ? Date.valueOf(experience.getEndDate()) : null);
            pstmt.setString(6, experience.getDescription());
            pstmt.setBoolean(7, experience.isCurrentJob());
            pstmt.setInt(8, experience.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating experience", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM experiences WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting experience", e);
        }
    }

    private Experience mapResultSetToExperience(ResultSet rs) throws SQLException {
        Experience experience = new Experience();
        experience.setId(rs.getInt("id"));
        experience.setProfileId(rs.getInt("profile_id"));
        experience.setCompany(rs.getString("company"));
        experience.setPosition(rs.getString("position"));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) experience.setStartDate(startDate.toLocalDate());
        
        Date endDate = rs.getDate("end_date");
        if (endDate != null) experience.setEndDate(endDate.toLocalDate());
        
        experience.setDescription(rs.getString("description"));
        experience.setCurrentJob(rs.getBoolean("current_job"));
        return experience;
    }
}
