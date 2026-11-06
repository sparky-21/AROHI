package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Core authentication identity document stored in /users/{uid}.
 * Separates authentication & authorization state from role-specific clinical data.
 */
public class UserAccount {
    private String uid;
    private String arohiId;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String verificationStatus;
    private boolean emailVerified;
    private boolean profileCompleted;
    private String address;
    private String bioNotes;
    private long createdAt;
    private long updatedAt;

    public UserAccount() {
        // Required empty constructor for Firestore serialization
    }

    public UserAccount(String uid, String arohiId, String email, String fullName, String phone,
                       UserRole role, VerificationStatus verificationStatus,
                       boolean emailVerified, boolean profileCompleted) {
        this.uid = uid;
        this.arohiId = arohiId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role.getKey();
        this.verificationStatus = verificationStatus.getKey();
        this.emailVerified = emailVerified;
        this.profileCompleted = profileCompleted;
        this.address = "";
        this.bioNotes = "";
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("arohiId", arohiId);
        map.put("email", email);
        map.put("fullName", fullName);
        map.put("phone", phone);
        map.put("role", role);
        map.put("verificationStatus", verificationStatus);
        map.put("emailVerified", emailVerified);
        map.put("profileCompleted", profileCompleted);
        map.put("address", address != null ? address : "");
        map.put("bioNotes", bioNotes != null ? bioNotes : "");
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public UserRole getUserRoleEnum() {
        return UserRole.fromKey(role);
    }

    public VerificationStatus getVerificationStatusEnum() {
        return VerificationStatus.fromKey(verificationStatus);
    }

    public boolean isVerifiedProfessionalOrPatient() {
        return VerificationStatus.VERIFIED.getKey().equalsIgnoreCase(verificationStatus);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getArohiId() {
        return arohiId;
    }

    public void setArohiId(String arohiId) {
        this.arohiId = arohiId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBioNotes() {
        return bioNotes;
    }

    public void setBioNotes(String bioNotes) {
        this.bioNotes = bioNotes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
