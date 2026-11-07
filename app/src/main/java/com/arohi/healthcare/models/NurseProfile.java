package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Nurse professional credential & profile document stored in /nurses/{uid}.
 * Initialized with verificationStatus == PENDING until verified by Hospital Authority.
 */
public class NurseProfile {
    private String uid;
    private String nurseId;
    private String fullName;
    private String email;
    private String phone;
    private String nursingLicenseNumber;
    private String qualification;
    private String preferredWard;
    private String shiftPreference;
    private String assignedWardId;
    private String verificationStatus;
    private String address;
    private String bioNotes;
    private long createdAt;

    public NurseProfile() {
        // Required empty constructor for Firestore
    }

    public NurseProfile(String uid, String nurseId, String fullName, String email, String phone,
                        String nursingLicenseNumber, String qualification,
                        String preferredWard, String shiftPreference) {
        this.uid = uid;
        this.nurseId = nurseId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.nursingLicenseNumber = nursingLicenseNumber;
        this.qualification = qualification;
        this.preferredWard = preferredWard;
        this.shiftPreference = shiftPreference;
        this.assignedWardId = "UNASSIGNED";
        this.verificationStatus = VerificationStatus.PENDING.getKey();
        this.address = "";
        this.bioNotes = "";
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("nurseId", nurseId);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("nursingLicenseNumber", nursingLicenseNumber);
        map.put("qualification", qualification);
        map.put("preferredWard", preferredWard);
        map.put("shiftPreference", shiftPreference);
        map.put("assignedWardId", assignedWardId);
        map.put("verificationStatus", verificationStatus);
        map.put("address", address != null ? address : "");
        map.put("bioNotes", bioNotes != null ? bioNotes : "");
        map.put("createdAt", createdAt);
        return map;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNurseId() {
        return nurseId;
    }

    public void setNurseId(String nurseId) {
        this.nurseId = nurseId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNursingLicenseNumber() {
        return nursingLicenseNumber;
    }

    public void setNursingLicenseNumber(String nursingLicenseNumber) {
        this.nursingLicenseNumber = nursingLicenseNumber;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getPreferredWard() {
        return preferredWard;
    }

    public void setPreferredWard(String preferredWard) {
        this.preferredWard = preferredWard;
    }

    public String getShiftPreference() {
        return shiftPreference;
    }

    public void setShiftPreference(String shiftPreference) {
        this.shiftPreference = shiftPreference;
    }

    public String getAssignedWardId() {
        return assignedWardId;
    }

    public void setAssignedWardId(String assignedWardId) {
        this.assignedWardId = assignedWardId;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
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

    public String getArohiId() {
        return nurseId;
    }

    public void setUpdatedAt(long updatedAt) {
        // Optional timestamp setter for profile updates
    }
}
