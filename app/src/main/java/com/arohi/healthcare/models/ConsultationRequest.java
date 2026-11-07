package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a patient-to-doctor consultation request stored in /doctorRequests/{doctorUid}_{patientUid}.
 * Valid states: Pending, Accepted, Declined, Cancelled.
 */
public class ConsultationRequest {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ACCEPTED = "Accepted";
    public static final String STATUS_DECLINED = "Declined";
    public static final String STATUS_CANCELLED = "Cancelled";

    private String requestId;
    private String patientUid;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientBloodGroup;
    private String patientGender;
    private String patientDob;
    private String patientAllergies;

    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorDepartment;
    private double consultationFee;

    private String consultationReason;
    private String patientNote;
    private String doctorResponseNote;
    private String status;
    private long createdAt;
    private long updatedAt;

    public ConsultationRequest() {
        // Required empty constructor for Firestore serialization
    }

    public static String buildPairId(String doctorUid, String patientUid) {
        return doctorUid + "_" + patientUid;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId != null ? patientId : "");
        map.put("patientName", patientName != null ? patientName : "");
        map.put("patientPhone", patientPhone != null ? patientPhone : "");
        map.put("patientBloodGroup", patientBloodGroup != null ? patientBloodGroup : "");
        map.put("patientGender", patientGender != null ? patientGender : "");
        map.put("patientDob", patientDob != null ? patientDob : "");
        map.put("patientAllergies", patientAllergies != null ? patientAllergies : "None");
        map.put("doctorUid", doctorUid);
        map.put("doctorId", doctorId != null ? doctorId : "");
        map.put("doctorName", doctorName != null ? doctorName : "");
        map.put("doctorSpecialty", doctorSpecialty != null ? doctorSpecialty : "");
        map.put("doctorDepartment", doctorDepartment != null ? doctorDepartment : "");
        map.put("consultationFee", consultationFee);
        map.put("consultationReason", consultationReason != null ? consultationReason : "");
        map.put("patientNote", patientNote != null ? patientNote : "");
        map.put("doctorResponseNote", doctorResponseNote != null ? doctorResponseNote : "");
        map.put("status", status != null ? status : STATUS_PENDING);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return STATUS_ACCEPTED.equalsIgnoreCase(status);
    }

    public boolean isDeclined() {
        return STATUS_DECLINED.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientBloodGroup() {
        return patientBloodGroup;
    }

    public void setPatientBloodGroup(String patientBloodGroup) {
        this.patientBloodGroup = patientBloodGroup;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientDob() {
        return patientDob;
    }

    public void setPatientDob(String patientDob) {
        this.patientDob = patientDob;
    }

    public String getPatientAllergies() {
        return patientAllergies;
    }

    public void setPatientAllergies(String patientAllergies) {
        this.patientAllergies = patientAllergies;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorSpecialty() {
        return doctorSpecialty;
    }

    public void setDoctorSpecialty(String doctorSpecialty) {
        this.doctorSpecialty = doctorSpecialty;
    }

    public String getDoctorDepartment() {
        return doctorDepartment;
    }

    public void setDoctorDepartment(String doctorDepartment) {
        this.doctorDepartment = doctorDepartment;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getConsultationReason() {
        return consultationReason;
    }

    public void setConsultationReason(String consultationReason) {
        this.consultationReason = consultationReason;
    }

    public String getPatientNote() {
        return patientNote;
    }

    public void setPatientNote(String patientNote) {
        this.patientNote = patientNote;
    }

    public String getDoctorResponseNote() {
        return doctorResponseNote;
    }

    public void setDoctorResponseNote(String doctorResponseNote) {
        this.doctorResponseNote = doctorResponseNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
