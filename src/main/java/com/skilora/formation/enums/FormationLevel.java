package com.skilora.formation.enums;

/**
 * Formation Level Enum
 * Defines the difficulty levels for formations.
 */
public enum FormationLevel {
    BEGINNER("Débutant", "Beginner"),
    INTERMEDIATE("Intermédiaire", "Intermediate"),
    ADVANCED("Avancé", "Advanced");

    private final String displayNameFr;
    private final String displayNameEn;

    FormationLevel(String displayNameFr, String displayNameEn) {
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
