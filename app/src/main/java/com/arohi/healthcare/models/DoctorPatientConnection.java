package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an authorized doctor-patient clinical connection stored in /connections/{doctorUid}_{patientUid}.
 * Created when a verified doctor accepts a patient's ConsultationRequest.
 */
public class DoctorPatientConnection {

    public static final String STATUS_ACCEPTED = "Accepted";

    private String connectionId;
    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorDepartment;

    private String patientUid;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientBloodGroup;

    private String consultationReason;
    private String status;
    private long connectedAt;
    private long updatedAt;

    public DoctorPatientConnection() {
        // Required empty constructor for Firestore
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("connectionId", connectionId);
        map.put("doctorUid", doctorUid);
        map.put("doctorId", doctorId != null ? doctorId : "");
        map.put("doctorName", doctorName != null ? doctorName : "");
        map.put("doctorSpecialty", doctorSpecialty != null ? doctorSpecialty : "");
        map.put("doctorDepartment", doctorDepartment != null ? doctorDepartment : "");
        map.put("patientUid", patientUid);
        map.put("patientId", patientId != null ? patientId : "");
        map.put("patientName", patientName != null ? patientName : "");
        map.put("patientPhone", patientPhone != null ? patientPhone : "");
        map.put("patientBloodGroup", patientBloodGroup != null ? patientBloodGroup : "");
        map.put("consultationReason", consultationReason != null ? consultationReason : "");
        map.put("status", status != null ? status : STATUS_ACCEPTED);
        map.put("connectedAt", connectedAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public boolean isAccepted() {
        return STATUS_ACCEPTED.equalsIgnoreCase(status);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
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

    public String getConsultationReason() {
        return consultationReason;
    }

    public void setConsultationReason(String consultationReason) {
        this.consultationReason = consultationReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(long connectedAt) {
        this.connectedAt = connectedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
