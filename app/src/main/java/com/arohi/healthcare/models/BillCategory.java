package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 13 Billing Categories.
 * Covers all healthcare billing types across the AROHI ecosystem.
 */
public enum BillCategory implements Serializable {
    CONSULTATION("CONSULTATION", "Doctor Consultation"),
    APPOINTMENT("APPOINTMENT", "Appointment Booking"),
    PHARMACY("PHARMACY", "Pharmacy Order"),
    LAB_TEST("LAB_TEST", "Laboratory Test"),
    HOSPITAL_SERVICE("HOSPITAL_SERVICE", "Hospital Service Bill"),
    OPERATION("OPERATION", "Surgical & OT Bill");

    private final String key;
    private final String displayName;

    BillCategory(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BillCategory fromKey(String key) {
        if (key == null) return HOSPITAL_SERVICE;
        for (BillCategory c : values()) {
            if (c.key.equalsIgnoreCase(key) || c.name().equalsIgnoreCase(key)) {
                return c;
            }
        }
        return HOSPITAL_SERVICE;
    }
}
