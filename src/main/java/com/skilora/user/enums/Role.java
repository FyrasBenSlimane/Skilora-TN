package com.skilora.user.enums;

/**
 * User Role Enum
 * Defines the available roles in the system.
 */
public enum Role {
    ADMIN("Administrator"),
    USER("Job Seeker"),
    EMPLOYER("Employer"),
    TRAINER("Trainer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
