package com.skilora.model.enums;

/**
 * Work Type Enum
 * 
 * Represents different types of employment/work arrangements.
 * Used in JobPreference entity for desired work type.
 */
public enum WorkType {
    FULL_TIME("Full-Time"),
    PART_TIME("Part-Time"),
    CONTRACT("Contract"),
    FREELANCE("Freelance"),
    INTERNSHIP("Internship"),
    TEMPORARY("Temporary");

    private final String displayName;

    WorkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
