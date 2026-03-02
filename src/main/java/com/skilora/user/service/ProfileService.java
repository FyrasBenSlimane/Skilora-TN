package com.skilora.user.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.enums.ProficiencyLevel;
import com.skilora.utils.I18n;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProfileService
 * 
 * Handles all profile-related business logic and data access.
 * This follows the MVC pattern where Service = Model layer (data + logic).
 * Merges ProfileDAO, SkillDAO, and ExperienceDAO functionality.
 * 
 * Note: No JavaFX imports allowed in this class.
 */
public class ProfileService {

    private static volatile ProfileService instance;

    private ProfileService() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static ProfileService getInstance() {
        if (instance == null) {
            synchronized (ProfileService.class) {
                if (instance == null) {
                    instance = new ProfileService();
                }
            }
        }
        return instance;
    }

    // ==================== Profile CRUD Operations ====================

    /**
     * Creates a new profile in the database.
     */
    public int createProfile(Profile profile) throws SQLException {
        validateProfile(profile);

        String sql = "INSERT INTO profiles (user_id, first_name, last_name, phone, photo_url, " +
                "cv_url, location, birth_date, headline, bio, website) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, profile.getUserId());
            stmt.setString(2, profile.getFirstName());
            stmt.setString(3, profile.getLastName());
            stmt.setString(4, profile.getPhone());
            stmt.setString(5, profile.getPhotoUrl());
            stmt.setString(6, profile.getCvUrl());
            stmt.setString(7, profile.getLocation());
            stmt.setDate(8, profile.getBirthDate() != null ? Date.valueOf(profile.getBirthDate()) : null);
            stmt.setString(9, profile.getHeadline());
            stmt.setString(10, profile.getBio());
            stmt.setString(11, profile.getWebsite());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating profile failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    profile.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating profile failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a profile by its ID.
     */
    public Profile findProfileById(int id) throws SQLException {
        String sql = "SELECT * FROM profiles WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a profile by user ID.
     */
    public Profile findProfileByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM profiles WHERE user_id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates an existing profile.
     */
    public boolean updateProfile(Profile profile) throws SQLException {
        validateProfile(profile);

        String sql = "UPDATE profiles SET first_name = ?, last_name = ?, phone = ?, " +
                "photo_url = ?, cv_url = ?, location = ?, birth_date = ?, " +
                "headline = ?, bio = ?, website = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, profile.getFirstName());
            stmt.setString(2, profile.getLastName());
            stmt.setString(3, profile.getPhone());
            stmt.setString(4, profile.getPhotoUrl());
            stmt.setString(5, profile.getCvUrl());
            stmt.setString(6, profile.getLocation());
            stmt.setDate(7, profile.getBirthDate() != null ? Date.valueOf(profile.getBirthDate()) : null);
            stmt.setString(8, profile.getHeadline());
            stmt.setString(9, profile.getBio());
            stmt.setString(10, profile.getWebsite());
            stmt.setInt(11, profile.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Finds all profiles.
     */
    public List<Profile> findAllProfiles() throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles ORDER BY id DESC";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                profiles.add(mapResultSetToProfile(rs));
            }
        }
        return profiles;
    }

    /**
     * Deletes a profile by ID (skills/experiences expected to cascade in schema).
     */
    public boolean deleteProfile(int profileId) throws SQLException {
        String sql = "DELETE FROM profiles WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Returns a profile with its skills and experiences.
     *
     * Map keys: profile, skills, experiences, completionPercentage
     */
    public Map<String, Object> getProfileWithDetails(int profileId) throws SQLException {
        Profile profile = findProfileById(profileId);
        List<Skill> skills = findSkillsByProfileId(profileId);
        List<Experience> experiences = findExperiencesByProfileId(profileId);
        int completion = calculateProfileCompletion(profile, skills, experiences);

        Map<String, Object> result = new HashMap<>();
        result.put("profile", profile);
        result.put("skills", skills);
        result.put("experiences", experiences);
        result.put("completionPercentage", completion);
        return result;
    }

    /**
     * Calculates profile completion percentage (0..100).
     * Weights are tuned for tests and UI expectations.
     */
    public int calculateProfileCompletion(Profile profile, List<?> skills, List<?> experiences) {
        if (profile == null) return 0;

        int total = 0;

        boolean hasName = profile.getFirstName() != null && !profile.getFirstName().trim().isEmpty()
                && profile.getLastName() != null && !profile.getLastName().trim().isEmpty();
        if (hasName) total += 20;

        if (profile.getPhone() != null && !profile.getPhone().trim().isEmpty()) total += 10;
        if (profile.getLocation() != null && !profile.getLocation().trim().isEmpty()) total += 10;
        if (profile.getBirthDate() != null) total += 10;
        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().trim().isEmpty()) total += 10;
        if (profile.getCvUrl() != null && !profile.getCvUrl().trim().isEmpty()) total += 10;

        int skillCount = skills != null ? skills.size() : 0;
        if (skillCount >= 3) total += 20;
        else if (skillCount >= 1) total += 10;

        int expCount = experiences != null ? experiences.size() : 0;
        if (expCount >= 1) total += 10;

        return Math.min(100, Math.max(0, total));
    }

    // ==================== Skill CRUD Operations ====================

    /**
     * Finds all skills for a profile.
     */
    public List<Skill> findSkillsByProfileId(int profileId) throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT * FROM skills WHERE profile_id = ? ORDER BY verified DESC, years_experience DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapResultSetToSkill(rs));
                }
            }
        }
        return skills;
    }

    public int createSkill(Skill skill) throws SQLException {
        return addOrUpdateSkill(skill);
    }

    /**
     * Replaces all skills for a profile using plain skill names.
     */
    public void saveSkills(int profileId, List<String> newSkills) throws SQLException {
        deleteSkillsByProfileId(profileId);
        if (newSkills == null) return;
        for (String name : newSkills) {
            if (name == null || name.trim().isEmpty()) continue;
            Skill s = new Skill(profileId, name.trim(), ProficiencyLevel.BEGINNER, 0);
            addOrUpdateSkill(s);
        }
    }

    public int deleteSkillsByProfileId(int profileId) throws SQLException {
        String sql = "DELETE FROM skills WHERE profile_id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            return stmt.executeUpdate();
        }
    }

    public int addOrUpdateSkill(Skill skill) throws SQLException {
        validateSkill(skill);
        String sql = """
                INSERT INTO skills (profile_id, skill_name, proficiency_level, years_experience, verified)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    proficiency_level = VALUES(proficiency_level),
                    years_experience = VALUES(years_experience),
                    verified = VALUES(verified)
                """;
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, skill.getProfileId());
            stmt.setString(2, skill.getSkillName());
            stmt.setString(3, skill.getProficiencyLevel() != null ? skill.getProficiencyLevel().name() : ProficiencyLevel.BEGINNER.name());
            stmt.setInt(4, Math.max(0, skill.getYearsExperience()));
            stmt.setBoolean(5, skill.isVerified());

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        skill.setId(id);
                        return id;
                    }
                }
                // Duplicate-key update path: fetch the existing id
                String q = "SELECT id FROM skills WHERE profile_id = ? AND skill_name = ? LIMIT 1";
                try (PreparedStatement qstmt = connection.prepareStatement(q)) {
                    qstmt.setInt(1, skill.getProfileId());
                    qstmt.setString(2, skill.getSkillName());
                    try (ResultSet rs = qstmt.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt("id");
                            skill.setId(id);
                            return id;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public boolean deleteSkill(int skillId) throws SQLException {
        String sql = "DELETE FROM skills WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, skillId);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==================== Experience CRUD Operations ====================

    /**
     * Finds all experiences for a profile.
     */
    public List<Experience> findExperiencesByProfileId(int profileId) throws SQLException {
        List<Experience> experiences = new ArrayList<>();
        String sql = "SELECT * FROM experiences WHERE profile_id = ? " +
                "ORDER BY current_job DESC, start_date DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    experiences.add(mapResultSetToExperience(rs));
                }
            }
        }
        return experiences;
    }

    public int createExperience(Experience exp) throws SQLException {
        return addExperience(exp);
    }

    public int deleteExperiencesByProfileId(int profileId) throws SQLException {
        String sql = "DELETE FROM experiences WHERE profile_id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            return stmt.executeUpdate();
        }
    }

    public int addExperience(Experience exp) throws SQLException {
        validateExperience(exp);
        String sql = """
                INSERT INTO experiences (profile_id, company, position, start_date, end_date, description, current_job)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, exp.getProfileId());
            stmt.setString(2, exp.getCompany());
            stmt.setString(3, exp.getPosition());
            stmt.setDate(4, exp.getStartDate() != null ? Date.valueOf(exp.getStartDate()) : null);
            stmt.setDate(5, exp.getEndDate() != null ? Date.valueOf(exp.getEndDate()) : null);
            stmt.setString(6, exp.getDescription());
            stmt.setBoolean(7, exp.isCurrentJob());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        exp.setId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public boolean updateExperience(Experience exp) throws SQLException {
        validateExperience(exp);
        if (exp.getId() <= 0) return false;
        String sql = """
                UPDATE experiences
                SET company = ?, position = ?, start_date = ?, end_date = ?, description = ?, current_job = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, exp.getCompany());
            stmt.setString(2, exp.getPosition());
            stmt.setDate(3, exp.getStartDate() != null ? Date.valueOf(exp.getStartDate()) : null);
            stmt.setDate(4, exp.getEndDate() != null ? Date.valueOf(exp.getEndDate()) : null);
            stmt.setString(5, exp.getDescription());
            stmt.setBoolean(6, exp.isCurrentJob());
            stmt.setInt(7, exp.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteExperience(int experienceId) throws SQLException {
        String sql = "DELETE FROM experiences WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, experienceId);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==================== Business Logic ====================

    // ==================== Validation ====================

    private void validateProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException(I18n.get("validation.profile.null"));
        }

        if (profile.getUserId() <= 0) {
            throw new IllegalArgumentException(I18n.get("validation.profile.invalid_user_id"));
        }

        if (profile.getFirstName() == null || profile.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException(I18n.get("validation.profile.first_name_required"));
        }

        if (profile.getLastName() == null || profile.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException(I18n.get("validation.profile.last_name_required"));
        }

        if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
            String phoneError = com.skilora.utils.Validators.validatePhone(profile.getPhone());
            if (phoneError != null) {
                throw new IllegalArgumentException(phoneError);
            }
        }
    }

    private void validateSkill(Skill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is null");
        }
        if (skill.getProfileId() <= 0) {
            throw new IllegalArgumentException("Invalid profile ID for skill");
        }
        if (skill.getSkillName() == null || skill.getSkillName().trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name is required");
        }
        skill.setSkillName(skill.getSkillName().trim());
    }

    private void validateExperience(Experience exp) {
        if (exp == null) {
            throw new IllegalArgumentException("Experience is null");
        }
        if (exp.getProfileId() <= 0) {
            throw new IllegalArgumentException("Invalid profile ID for experience");
        }
        if (exp.getCompany() == null || exp.getCompany().trim().isEmpty()) {
            throw new IllegalArgumentException("Company is required");
        }
        if (exp.getPosition() == null || exp.getPosition().trim().isEmpty()) {
            throw new IllegalArgumentException("Position is required");
        }
        if (exp.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        exp.setCompany(exp.getCompany().trim());
        exp.setPosition(exp.getPosition().trim());
        if (exp.isCurrentJob()) {
            exp.setEndDate(null);
        }
    }

    // ==================== Mapping Methods ====================

    private Profile mapResultSetToProfile(ResultSet rs) throws SQLException {
        Profile profile = new Profile();
        profile.setId(rs.getInt("id"));
        profile.setUserId(rs.getInt("user_id"));
        profile.setFirstName(rs.getString("first_name"));
        profile.setLastName(rs.getString("last_name"));
        profile.setPhone(rs.getString("phone"));
        profile.setPhotoUrl(rs.getString("photo_url"));
        profile.setCvUrl(rs.getString("cv_url"));
        profile.setLocation(rs.getString("location"));

        Date birthDate = rs.getDate("birth_date");
        if (birthDate != null) {
            profile.setBirthDate(birthDate.toLocalDate());
        }

        profile.setHeadline(rs.getString("headline"));
        profile.setBio(rs.getString("bio"));
        profile.setWebsite(rs.getString("website"));

        return profile;
    }

    private Skill mapResultSetToSkill(ResultSet rs) throws SQLException {
        Skill skill = new Skill();
        skill.setId(rs.getInt("id"));
        skill.setProfileId(rs.getInt("profile_id"));
        skill.setSkillName(rs.getString("skill_name"));

        String proficiencyStr = rs.getString("proficiency_level");
        if (proficiencyStr != null) {
            try {
                skill.setProficiencyLevel(ProficiencyLevel.valueOf(proficiencyStr));
            } catch (IllegalArgumentException e) {
                skill.setProficiencyLevel(ProficiencyLevel.BEGINNER);
            }
        }

        skill.setYearsExperience(rs.getInt("years_experience"));
        skill.setVerified(rs.getBoolean("verified"));

        return skill;
    }

    private Experience mapResultSetToExperience(ResultSet rs) throws SQLException {
        Experience experience = new Experience();
        experience.setId(rs.getInt("id"));
        experience.setProfileId(rs.getInt("profile_id"));
        experience.setCompany(rs.getString("company"));
        experience.setPosition(rs.getString("position"));

        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            experience.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("end_date");
        if (endDate != null) {
            experience.setEndDate(endDate.toLocalDate());
        }

        experience.setDescription(rs.getString("description"));
        experience.setCurrentJob(rs.getBoolean("current_job"));

        return experience;
    }
}
