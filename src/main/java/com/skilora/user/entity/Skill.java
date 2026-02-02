package com.skilora.user.entity;

import com.skilora.user.enums.ProficiencyLevel;
import java.util.Objects;

/**
 * Skill Entity
 * 
 * Represents a skill associated with a profile.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Skill {

    private int id;
    private int profileId;
    private String skillName;
    private ProficiencyLevel proficiencyLevel;
    private int yearsExperience;
    private boolean verified;

    // Constructors
    public Skill() {
    }

    public Skill(int profileId, String skillName) {
        this.profileId = profileId;
        this.skillName = skillName;
        this.proficiencyLevel = ProficiencyLevel.BEGINNER;
        this.yearsExperience = 0;
        this.verified = false;
    }

    public Skill(int profileId, String skillName, ProficiencyLevel proficiencyLevel, int yearsExperience) {
        this.profileId = profileId;
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsExperience = yearsExperience;
        this.verified = false;
    }

    // Getters and Setters
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

    public int getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(int yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", skillName='" + skillName + '\'' +
                ", proficiencyLevel=" + proficiencyLevel +
                ", yearsExperience=" + yearsExperience +
                ", verified=" + verified +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Skill skill = (Skill) o;
        return id == skill.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
