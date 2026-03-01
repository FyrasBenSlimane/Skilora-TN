package com.skilora.model.repository.usermanagement.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.Skill;
import com.skilora.model.enums.ProficiencyLevel;
import com.skilora.model.repository.usermanagement.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkillRepositoryImpl implements SkillRepository {
    private static final Logger logger = LoggerFactory.getLogger(SkillRepositoryImpl.class);

    @Override
    public Optional<Skill> findById(int id) {
        String sql = "SELECT * FROM skills WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding skill by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Skill> findByProfileId(int profileId) {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT * FROM skills WHERE profile_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, profileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapResultSetToSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding skills by profile_id: {}", profileId, e);
        }
        return skills;
    }

    @Override
    public void save(Skill skill) {
        String sql = "INSERT INTO skills (profile_id, skill_name, proficiency_level, years_experience, verified) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, skill.getProfileId());
            pstmt.setString(2, skill.getSkillName());
            pstmt.setString(3, skill.getProficiencyLevel() != null ? skill.getProficiencyLevel().name() : null);
            if (skill.getYearsExperience() != null) pstmt.setInt(4, skill.getYearsExperience()); else pstmt.setNull(4, Types.INTEGER);
            pstmt.setBoolean(5, skill.isVerified());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        skill.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving skill", e);
        }
    }

    @Override
    public void update(Skill skill) {
        String sql = "UPDATE skills SET profile_id = ?, skill_name = ?, proficiency_level = ?, years_experience = ?, verified = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, skill.getProfileId());
            pstmt.setString(2, skill.getSkillName());
            pstmt.setString(3, skill.getProficiencyLevel() != null ? skill.getProficiencyLevel().name() : null);
            if (skill.getYearsExperience() != null) pstmt.setInt(4, skill.getYearsExperience()); else pstmt.setNull(4, Types.INTEGER);
            pstmt.setBoolean(5, skill.isVerified());
            pstmt.setInt(6, skill.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating skill", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM skills WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting skill", e);
        }
    }

    private Skill mapResultSetToSkill(ResultSet rs) throws SQLException {
        Skill skill = new Skill();
        skill.setId(rs.getInt("id"));
        skill.setProfileId(rs.getInt("profile_id"));
        skill.setSkillName(rs.getString("skill_name"));
        
        String profStr = rs.getString("proficiency_level");
        if (profStr != null) {
            try {
                skill.setProficiencyLevel(ProficiencyLevel.valueOf(profStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown proficiency level: {}", profStr);
            }
        }
        
        int years = rs.getInt("years_experience");
        skill.setYearsExperience(rs.wasNull() ? null : years);
        skill.setVerified(rs.getBoolean("verified"));
        return skill;
    }
}
