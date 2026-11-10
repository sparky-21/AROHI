package com.arohi.healthcare.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Platform-level Super Administrator authorization document stored in /superAdmins/{uid}.
 * Never exposed via public registration. Manages network hospital onboarding, verification,
 * activation, and hospital code allocation.
 */
public class SuperAdminAuthorization {

    private String superAdminUid;
    private String email;
    private String fullName;
    private int level;
    private boolean active;
    private String notes;
    private long createdAt;

    public SuperAdminAuthorization() {
        this.active = true;
        this.level = 1;
        this.createdAt = System.currentTimeMillis();
    }

    public SuperAdminAuthorization(String superAdminUid, String email, String fullName, int level, boolean active, String notes) {
        this.superAdminUid = superAdminUid;
        this.email = email;
        this.fullName = fullName;
        this.level = level;
        this.active = active;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("superAdminUid", superAdminUid);
        map.put("email", email);
        map.put("fullName", fullName);
        map.put("level", level);
        map.put("active", active);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        return map;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("superAdminUid", superAdminUid);
            json.put("email", email);
            json.put("fullName", fullName);
            json.put("level", level);
            json.put("active", active);
            json.put("notes", notes);
            json.put("createdAt", createdAt);
        } catch (JSONException e) {
            // Ignored
        }
        return json;
    }

    public static SuperAdminAuthorization fromJson(JSONObject json) {
        if (json == null) return null;
        try {
            SuperAdminAuthorization auth = new SuperAdminAuthorization(
                    json.optString("superAdminUid"),
                    json.optString("email"),
                    json.optString("fullName"),
                    json.optInt("level", 1),
                    json.optBoolean("active", true),
                    json.optString("notes")
            );
            auth.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
            return auth;
        } catch (Exception e) {
            return null;
        }
    }

    // Getters and Setters
    public String getSuperAdminUid() {
        return superAdminUid;
    }

    public void setSuperAdminUid(String superAdminUid) {
        this.superAdminUid = superAdminUid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
