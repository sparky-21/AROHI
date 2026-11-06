package com.arohi.healthcare.models;

/**
 * Defines the four core user roles in the AROHI healthcare ecosystem.
 */
public enum UserRole {
    PATIENT("PATIENT", "Patient & Family"),
    DOCTOR("DOCTOR", "Doctor / Specialist"),
    NURSE("NURSE", "Nursing Professional"),
    ADMIN("ADMIN", "Hospital Authority / Admin");

    private final String key;
    private final String displayName;

    UserRole(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromKey(String key) {
        if (key == null) {
            return PATIENT;
        }
        for (UserRole role : values()) {
            if (role.key.equalsIgnoreCase(key.trim())) {
                return role;
            }
        }
        return PATIENT;
    }
}
