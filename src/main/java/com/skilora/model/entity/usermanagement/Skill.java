package com.skilora.model.entity.usermanagement;

import com.skilora.model.enums.ProficiencyLevel;
import java.util.Objects;

/**
 * Skill Entity
 * Represents a skill associated with a user profile.
 */
public class Skill {
    private int id;
    private int profileId;
    private String skillName;
    private ProficiencyLevel proficiencyLevel;
    private Integer yearsExperience;
    private boolean verified;

    public Skill() {
    }

    public Skill(int profileId, String skillName, ProficiencyLevel proficiencyLevel) {
        this.profileId = profileId;
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return id == skill.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return skillName + " (" + (proficiencyLevel != null ? proficiencyLevel.getDisplayName() : "N/A") + ")";
    }
}
