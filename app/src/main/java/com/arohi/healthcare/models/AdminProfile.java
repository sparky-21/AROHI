package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Authorized Hospital Administrator profile stored in /administrators/{uid}.
 * Ordinary users cannot self-register as administrators without a valid institutional authorization record.
 */
public class AdminProfile {
    private String uid;
    private String adminId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeId;
    private String designation;
    private String hospitalName;
    private String hospitalAddress;
    private String emergencyHotline;
    private String bioNotes;
    private String verificationStatus;
    private long createdAt;

    public AdminProfile() {
        // Required empty constructor for Firestore
    }

    public AdminProfile(String uid, String adminId, String fullName, String email, String phone,
                        String employeeId, String designation, VerificationStatus verificationStatus) {
        this.uid = uid;
        this.adminId = adminId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.employeeId = employeeId;
        this.designation = designation;
        this.hospitalName = "AROHI Central Specialized Hospital";
        this.hospitalAddress = "Dhaka Medical Hub, Bangladesh";
        this.emergencyHotline = "+880-9612-AROHI";
        this.bioNotes = "";
        this.verificationStatus = verificationStatus.getKey();
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("adminId", adminId);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("employeeId", employeeId);
        map.put("designation", designation);
        map.put("hospitalName", hospitalName != null ? hospitalName : "AROHI Central Specialized Hospital");
        map.put("hospitalAddress", hospitalAddress != null ? hospitalAddress : "");
        map.put("emergencyHotline", emergencyHotline != null ? emergencyHotline : "");
        map.put("bioNotes", bioNotes != null ? bioNotes : "");
        map.put("verificationStatus", verificationStatus);
        map.put("createdAt", createdAt);
        return map;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
    }

    public String getEmergencyHotline() {
        return emergencyHotline;
    }

    public void setEmergencyHotline(String emergencyHotline) {
        this.emergencyHotline = emergencyHotline;
    }

    public String getBioNotes() {
        return bioNotes;
    }

    public void setBioNotes(String bioNotes) {
        this.bioNotes = bioNotes;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getArohiId() {
        return adminId;
    }

    public void setUpdatedAt(long updatedAt) {
        // Optional timestamp setter for profile updates
    }
}
