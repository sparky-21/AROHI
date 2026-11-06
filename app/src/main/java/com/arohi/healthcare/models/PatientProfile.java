package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Patient profile stored in /patients/{uid}.
 */
public class PatientProfile {
    private String uid;
    private String patientId;
    private String fullName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String allergies;
    private String verificationStatus;
    private String address;
    private String bioNotes;
    private long createdAt;

    public PatientProfile() {
        // Required empty constructor for Firestore
    }

    public PatientProfile(String uid, String patientId, String fullName, String email, String phone,
                          String dateOfBirth, String gender, String bloodGroup,
                          String emergencyContactName, String emergencyContactPhone, String allergies) {
        this.uid = uid;
        this.patientId = patientId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.allergies = allergies != null ? allergies : "";
        this.verificationStatus = VerificationStatus.PENDING.getKey();
        this.address = "";
        this.bioNotes = "";
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("patientId", patientId);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("dateOfBirth", dateOfBirth);
        map.put("gender", gender);
        map.put("bloodGroup", bloodGroup);
        map.put("emergencyContactName", emergencyContactName);
        map.put("emergencyContactPhone", emergencyContactPhone);
        map.put("allergies", allergies);
        map.put("verificationStatus", verificationStatus);
        map.put("address", address != null ? address : "");
        map.put("bioNotes", bioNotes != null ? bioNotes : "");
        map.put("createdAt", createdAt);
        return map;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
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
}
