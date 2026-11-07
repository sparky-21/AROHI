package com.arohi.healthcare.models;

/**
 * Represents the account and credential verification state for AROHI users.
 * Doctor and Nurse registrations always start as PENDING until verified by Hospital Authority.
 */
public enum VerificationStatus {
    PENDING("PENDING", "Pending Hospital Authority Verification"),
    VERIFIED("VERIFIED", "Verified & Authorized"),
    REJECTED("REJECTED", "Credential Verification Declined"),
    SUSPENDED("SUSPENDED", "Account Suspended");

    private final String key;
    private final String description;

    VerificationStatus(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayLabel() {
        return description;
    }

    public static VerificationStatus fromKey(String key) {
        if (key == null) {
            return PENDING;
        }
        for (VerificationStatus status : values()) {
            if (status.key.equalsIgnoreCase(key.trim())) {
                return status;
            }
        }
        return PENDING;
    }
}
