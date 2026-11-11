package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 13 Payment Gateway Providers.
 * Configurable provider architecture supporting bKash, Nagad, Card (SSLCommerz/Stripe), and Hospital Cash Counter.
 */
public enum PaymentGatewayType implements Serializable {
    BKASH("BKASH", "bKash", "bKash Online Payment"),
    NAGAD("NAGAD", "Nagad", "Nagad Direct Gateway"),
    CARD_SSLCOMMERZ("CARD_SSLCOMMERZ", "Card / SSLCommerz", "Debit/Credit Card (Visa, MasterCard)"),
    CASH_COUNTER("CASH_COUNTER", "Hospital Counter", "Pay at Hospital Billing Counter");

    private final String key;
    private final String shortName;
    private final String description;

    PaymentGatewayType(String key, String shortName, String description) {
        this.key = key;
        this.shortName = shortName;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentGatewayType fromKey(String key) {
        if (key == null) return BKASH;
        for (PaymentGatewayType g : values()) {
            if (g.key.equalsIgnoreCase(key) || g.name().equalsIgnoreCase(key)) {
                return g;
            }
        }
        return BKASH;
    }
}
