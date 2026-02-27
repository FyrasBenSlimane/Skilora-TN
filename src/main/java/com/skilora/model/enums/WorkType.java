package com.skilora.model.enums;

public enum WorkType {
    FULL_TIME("Temps plein"),
    PART_TIME("Temps partiel"),
    REMOTE("Télétravail"),
    INTERNSHIP("Stage");

    private final String displayName;

    WorkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
