package com.skilora.model.enums;

/**
 * Training Level Enum
 * 
 * Represents the difficulty level of a training course.
 */
public enum TrainingLevel {
    BEGINNER("Débutant", "Beginner"),
    INTERMEDIATE("Intermédiaire", "Intermediate"),
    ADVANCED("Avancé", "Advanced");

    private final String displayNameFr;
    private final String displayNameEn;

    TrainingLevel(String displayNameFr, String displayNameEn) {
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
