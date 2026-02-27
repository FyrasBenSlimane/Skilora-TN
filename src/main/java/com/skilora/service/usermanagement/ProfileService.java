package com.skilora.service.usermanagement;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.usermanagement.Experience;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.model.entity.usermanagement.Skill;
import com.skilora.model.enums.ProficiencyLevel;

import com.skilora.service.PythonApiService;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

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
                "cv_url, location, birth_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
                "photo_url = ?, cv_url = ?, location = ?, birth_date = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, profile.getFirstName());
            stmt.setString(2, profile.getLastName());
            stmt.setString(3, profile.getPhone());
            stmt.setString(4, profile.getPhotoUrl());
            stmt.setString(5, profile.getCvUrl());
            stmt.setString(6, profile.getLocation());
            stmt.setDate(7, profile.getBirthDate() != null ? Date.valueOf(profile.getBirthDate()) : null);
            stmt.setInt(8, profile.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a profile by ID (also deletes associated skills and experiences).
     */
    public boolean deleteProfile(int id) throws SQLException {
        // Delete associated skills and experiences first
        deleteSkillsByProfileId(id);
        deleteExperiencesByProfileId(id);

        String sql = "DELETE FROM profiles WHERE id = ?";
        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all profiles.
     */
    public List<Profile> findAllProfiles() throws SQLException {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles ORDER BY id DESC";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                profiles.add(mapResultSetToProfile(rs));
            }
        }
        return profiles;
    }

    /**
     * Analyze a CV PDF and update the profile with detected skills.
     */
    public CompletableFuture<JsonObject> analyzeCvAndEnrich(int profileId, File cvFile) {
        return PythonApiService.getInstance().analyzeCv(cvFile)
                .thenApply(analysis -> {
                    if (analysis != null) {
                        try {
                            List<String> skills = new ArrayList<>();
                            analysis.get("skills_detected").getAsJsonArray().forEach(e -> skills.add(e.getAsString()));

                            if (!skills.isEmpty()) {
                                saveSkills(profileId, skills);
                                logger.info("Enriched profile ID {} with detected skills from CV", profileId);
                            }
                        } catch (Exception e) {
                            logger.error("Error enrichment profile with CV data", e);
                        }
                    }
                    return analysis;
                });
    }

    // ==================== Skill CRUD Operations ====================

    /**
     * Creates a new skill.
     */
    public int createSkill(Skill skill) throws SQLException {
        String sql = "INSERT INTO skills (profile_id, skill_name, proficiency_level, " +
                "years_experience, verified) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, skill.getProfileId());
            stmt.setString(2, skill.getSkillName());
            stmt.setString(3, skill.getProficiencyLevel() != null ? skill.getProficiencyLevel().name()
                    : ProficiencyLevel.BEGINNER.name());
            stmt.setInt(4, skill.getYearsExperience());
            stmt.setBoolean(5, skill.isVerified());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating skill failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    skill.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating skill failed, no ID obtained.");
                }
            }
        }
    }

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

    /**
     * Deletes all skills for a profile.
     */
    public int deleteSkillsByProfileId(int profileId) throws SQLException {
        String sql = "DELETE FROM skills WHERE profile_id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            return stmt.executeUpdate();
        }
    }

    /**
     * Saves a list of skills for a profile (replaces existing).
     */
    public void saveSkills(int profileId, List<String> skillNames) throws SQLException {
        deleteSkillsByProfileId(profileId);

        for (String name : skillNames) {
            if (name == null || name.trim().isEmpty())
                continue;
            Skill skill = new Skill(profileId, name.trim());
            createSkill(skill);
        }
    }

    // ==================== Experience CRUD Operations ====================

    /**
     * Creates a new experience.
     */
    public int createExperience(Experience experience) throws SQLException {
        String sql = "INSERT INTO experiences (profile_id, company, position, start_date, " +
                "end_date, description, current_job) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, experience.getProfileId());
            stmt.setString(2, experience.getCompany());
            stmt.setString(3, experience.getPosition());
            stmt.setDate(4, experience.getStartDate() != null ? Date.valueOf(experience.getStartDate()) : null);
            stmt.setDate(5, experience.getEndDate() != null ? Date.valueOf(experience.getEndDate()) : null);
            stmt.setString(6, experience.getDescription());
            stmt.setBoolean(7, experience.isCurrentJob());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating experience failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    experience.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating experience failed, no ID obtained.");
                }
            }
        }
    }

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

    /**
     * Deletes all experiences for a profile.
     */
    public int deleteExperiencesByProfileId(int profileId) throws SQLException {
        String sql = "DELETE FROM experiences WHERE profile_id = ?";

        try (Connection connection = DatabaseConfig.getInstance().getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            return stmt.executeUpdate();
        }
    }

    // ==================== Business Logic ====================

    /**
     * Gets a profile with all its details (skills and experiences).
     */
    public Map<String, Object> getProfileWithDetails(int profileId) throws SQLException {
        Profile profile = findProfileById(profileId);
        if (profile == null) {
            return null;
        }

        List<Skill> skills = findSkillsByProfileId(profileId);
        List<Experience> experiences = findExperiencesByProfileId(profileId);

        Map<String, Object> details = new HashMap<>();
        details.put("profile", profile);
        details.put("skills", skills);
        details.put("experiences", experiences);
        details.put("completionPercentage", calculateProfileCompletion(profile, skills, experiences));

        return details;
    }

    /**
     * Calculates profile completion percentage.
     */
    public int calculateProfileCompletion(Profile profile, List<Skill> skills, List<Experience> experiences) {
        int totalFields = 12;
        int completedFields = 0;

        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty())
            completedFields++;
        if (profile.getLastName() != null && !profile.getLastName().isEmpty())
            completedFields++;
        if (profile.getPhone() != null && !profile.getPhone().isEmpty())
            completedFields++;
        if (profile.getLocation() != null && !profile.getLocation().isEmpty())
            completedFields++;
        if (profile.getBirthDate() != null)
            completedFields++;
        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty())
            completedFields++;

        if (profile.getCvUrl() != null && !profile.getCvUrl().isEmpty())
            completedFields += 2;

        if (skills != null && !skills.isEmpty()) {
            completedFields++;
            if (skills.size() >= 3)
                completedFields++;
        }

        if (experiences != null && !experiences.isEmpty()) {
            completedFields++;
            if (experiences.size() >= 1)
                completedFields++;
        }

        return (completedFields * 100) / totalFields;
    }

    // ==================== Validation ====================

    private void validateProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }

        if (profile.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        if (profile.getFirstName() == null || profile.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (profile.getLastName() == null || profile.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
            if (!profile.getPhone().matches("^[0-9+\\-\\s()]+$")) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
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
