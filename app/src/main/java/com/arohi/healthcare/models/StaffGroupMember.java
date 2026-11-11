package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 12 Staff Group Member Model.
 * Represents a member of "All Doctors" or "All Nurses" group in a specific hospital.
 * Tracks staff verification, specialty, and active authorization status.
 */
public class StaffGroupMember implements Serializable {

    private String userId;
    private String fullName;
    private String arohiId;
    private String role; // DOCTOR, NURSE, ADMIN
    private String specialty;
    private String hospitalId;
    private boolean verified;
    private String status; // ACTIVE, SUSPENDED, TERMINATED
    private long joinedAt;

    public StaffGroupMember() {
        this.status = HospitalMembership.STATUS_ACTIVE;
        this.joinedAt = System.currentTimeMillis();
    }

    public StaffGroupMember(String userId, String fullName, String arohiId,
                            String role, String specialty, String hospitalId,
                            boolean verified, String status) {
        this.userId = userId;
        this.fullName = fullName;
        this.arohiId = arohiId;
        this.role = role;
        this.specialty = specialty;
        this.hospitalId = hospitalId;
        this.verified = verified;
        this.status = status != null ? status : HospitalMembership.STATUS_ACTIVE;
        this.joinedAt = System.currentTimeMillis();
    }

    public boolean isEligibleForChat() {
        return verified && HospitalMembership.STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getArohiId() { return arohiId; }
    public void setArohiId(String arohiId) { this.arohiId = arohiId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
}
