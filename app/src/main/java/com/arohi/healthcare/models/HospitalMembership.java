package com.arohi.healthcare.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Institutional membership entity linking a global UserAccount to a specific Hospital tenant.
 * Enables authorized multi-hospital membership with distinct, hospital-specific roles.
 */
public class HospitalMembership {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVOKED = "REVOKED";

    private String membershipId;
    private String userId;
    private String hospitalId;
    private String hospitalCode;
    private String role; // PATIENT, DOCTOR, NURSE, ADMIN
    private String status;
    private String department;
    private long joinedAt;
    private long updatedAt;

    public HospitalMembership() {
        this.status = STATUS_ACTIVE;
        long now = System.currentTimeMillis();
        this.joinedAt = now;
        this.updatedAt = now;
    }

    public HospitalMembership(String membershipId, String userId, String hospitalId,
                              String hospitalCode, String role, String status, String department) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.hospitalId = hospitalId;
        this.hospitalCode = hospitalCode != null ? hospitalCode.toUpperCase().trim() : "";
        this.role = role != null ? role.toUpperCase().trim() : "PATIENT";
        this.status = status != null ? status : STATUS_ACTIVE;
        this.department = department != null ? department : "";
        long now = System.currentTimeMillis();
        this.joinedAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("membershipId", membershipId);
        map.put("userId", userId);
        map.put("hospitalId", hospitalId);
        map.put("hospitalCode", hospitalCode);
        map.put("role", role);
        map.put("status", status);
        map.put("department", department);
        map.put("joinedAt", joinedAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("membershipId", membershipId);
            json.put("userId", userId);
            json.put("hospitalId", hospitalId);
            json.put("hospitalCode", hospitalCode);
            json.put("role", role);
            json.put("status", status);
            json.put("department", department);
            json.put("joinedAt", joinedAt);
            json.put("updatedAt", updatedAt);
        } catch (JSONException e) {
            // Ignored
        }
        return json;
    }

    public static HospitalMembership fromJson(JSONObject json) {
        if (json == null) return null;
        try {
            HospitalMembership m = new HospitalMembership(
                    json.optString("membershipId"),
                    json.optString("userId"),
                    json.optString("hospitalId"),
                    json.optString("hospitalCode"),
                    json.optString("role", "PATIENT"),
                    json.optString("status", STATUS_ACTIVE),
                    json.optString("department")
            );
            m.setJoinedAt(json.optLong("joinedAt", System.currentTimeMillis()));
            m.setUpdatedAt(json.optLong("updatedAt", System.currentTimeMillis()));
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    // Getters and Setters
    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode != null ? hospitalCode.toUpperCase().trim() : "";
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase().trim() : "PATIENT";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
