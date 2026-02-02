package com.skilora.user.enums;

/**
 * Proficiency Level Enum
 * 
 * Represents skill proficiency levels from beginner to expert.
 * Used in Skill entity for skill assessment.
 */
public enum ProficiencyLevel {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 2),
    ADVANCED("Advanced", 3),
    EXPERT("Expert", 4);

    private final String displayName;
    private final int level;

    ProficiencyLevel(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public static ProficiencyLevel fromLevel(int level) {
        for (ProficiencyLevel prof : values()) {
            if (prof.level == level) {
                return prof;
            }
        }
        return BEGINNER;
    }
}
