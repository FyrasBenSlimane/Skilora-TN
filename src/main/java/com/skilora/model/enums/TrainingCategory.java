package com.skilora.model.enums;

/**
 * Training Category Enum
 * 
 * Represents the category/domain of a training course.
 */
public enum TrainingCategory {
    DEVELOPMENT("Développement", "Development"),
    DESIGN("Design", "Design"),
    MARKETING("Marketing", "Marketing"),
    DATA_SCIENCE("Data Science", "Data Science"),
    LANGUAGES("Langues", "Languages"),
    SOFT_SKILLS("Soft Skills", "Soft Skills");

    private final String displayNameFr;
    private final String displayNameEn;

    TrainingCategory(String displayNameFr, String displayNameEn) {
        this.displayNameFr = displayNameFr;
        this.displayNameEn = displayNameEn;
    }

    public String getDisplayNameFr() {
        return displayNameFr;
    }

    public String getDisplayNameEn() {
        return displayNameEn;
    }

    public String getDisplayName() {
        // Default to French for now
        return displayNameFr;
    }
}
