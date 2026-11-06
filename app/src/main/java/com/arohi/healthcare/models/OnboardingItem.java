package com.arohi.healthcare.models;

/**
 * Immutable data model representing an Onboarding screen slide.
 */
public class OnboardingItem {
    private final int iconResId;
    private final String badge;
    private final String title;
    private final String description;

    public OnboardingItem(int iconResId, String badge, String title, String description) {
        this.iconResId = iconResId;
        this.badge = badge;
        this.title = title;
        this.description = description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getBadge() {
        return badge;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
