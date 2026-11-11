package com.arohi.healthcare.models;

import androidx.annotation.NonNull;

/**
 * Unified tracking service categories supported in AROHI Phase 14.
 */
public enum TrackingCategory {
    PHARMACY("PHARMACY", "Pharmacy Delivery", "Rx"),
    APPOINTMENT("APPOINTMENT", "Appointments", "Appt"),
    OPERATION("OPERATION", "Operations (OT)", "OT"),
    PAYMENT("PAYMENT", "Payments & Bills", "Pay"),
    AMBULANCE("AMBULANCE", "Ambulance Live", "Amb");

    private final String key;
    private final String displayName;
    private final String shortTag;

    TrackingCategory(String key, String displayName, String shortTag) {
        this.key = key;
        this.displayName = displayName;
        this.shortTag = shortTag;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortTag() {
        return shortTag;
    }

    @NonNull
    public static TrackingCategory fromKey(String key) {
        if (key == null) return PHARMACY;
        for (TrackingCategory c : values()) {
            if (c.key.equalsIgnoreCase(key.trim())) {
                return c;
            }
        }
        return PHARMACY;
    }
}
