package com.skilora.model.entity.usermanagement;

import com.skilora.model.enums.ProficiencyLevel;

public class Skill {
    private int id;
    private int profileId;
    private String skillName;
    private ProficiencyLevel proficiencyLevel;
    private int yearsExperience;
    private boolean verified;

    public Skill() {}

    public Skill(int profileId, String skillName) {
        this.profileId = profileId;
        this.skillName = skillName;
        this.proficiencyLevel = ProficiencyLevel.BEGINNER;
        this.verified = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public ProficiencyLevel getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }
    public int getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
