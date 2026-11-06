package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Doctor professional credential & profile document stored in /doctors/{uid}.
 * Initialized with verificationStatus == PENDING until verified by Hospital Authority.
 */
public class DoctorProfile {
    private String uid;
    private String doctorId;
    private String fullName;
    private String email;
    private String phone;
    private String medicalLicenseNumber;
    private String specialty;
    private String qualifications;
    private String department;
    private int experienceYears;
    private double consultationFee;
    private String verificationStatus;
    private String address;
    private String bioNotes;
    private long createdAt;

    public DoctorProfile() {
        // Required empty constructor for Firestore
    }

    public DoctorProfile(String uid, String doctorId, String fullName, String email, String phone,
                         String medicalLicenseNumber, String specialty, String qualifications,
                         String department, int experienceYears, double consultationFee) {
        this.uid = uid;
        this.doctorId = doctorId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.medicalLicenseNumber = medicalLicenseNumber;
        this.specialty = specialty;
        this.qualifications = qualifications;
        this.department = department;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.verificationStatus = VerificationStatus.PENDING.getKey();
        this.address = "";
        this.bioNotes = "";
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("doctorId", doctorId);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("medicalLicenseNumber", medicalLicenseNumber);
        map.put("specialty", specialty);
        map.put("qualifications", qualifications);
        map.put("department", department);
        map.put("experienceYears", experienceYears);
        map.put("consultationFee", consultationFee);
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

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
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

    public String getMedicalLicenseNumber() {
        return medicalLicenseNumber;
    }

    public void setMedicalLicenseNumber(String medicalLicenseNumber) {
        this.medicalLicenseNumber = medicalLicenseNumber;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
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
}
