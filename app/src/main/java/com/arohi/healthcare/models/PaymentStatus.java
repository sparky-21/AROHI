package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 13 Payment State Machine.
 * States: PENDING, PROCESSING, SUCCESSFUL, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED.
 */
public enum PaymentStatus implements Serializable {
    PENDING("PENDING", "Pending"),
    PROCESSING("PROCESSING", "Processing"),
    SUCCESSFUL("SUCCESSFUL", "Successful / Paid"),
    FAILED("FAILED", "Failed"),
    CANCELLED("CANCELLED", "Cancelled"),
    REFUNDED("REFUNDED", "Refunded"),
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED", "Partially Refunded");

    private final String key;
    private final String label;

    PaymentStatus(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return this == SUCCESSFUL
                || this == FAILED
                || this == CANCELLED
                || this == REFUNDED
                || this == PARTIALLY_REFUNDED;
    }

    public static PaymentStatus fromKey(String key) {
        if (key == null) return PENDING;
        for (PaymentStatus s : values()) {
            if (s.key.equalsIgnoreCase(key) || s.name().equalsIgnoreCase(key)) {
                return s;
            }
        }
        return PENDING;
    }
}
