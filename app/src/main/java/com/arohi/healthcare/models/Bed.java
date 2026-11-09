package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Inpatient Bed model.
 * Statuses: Available, Occupied, Cleaning, Maintenance.
 * Prevents assigning occupied or unavailable beds.
 */
public class Bed implements Serializable {

    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_OCCUPIED = "Occupied";
    public static final String STATUS_CLEANING = "Cleaning";
    public static final String STATUS_MAINTENANCE = "Maintenance";

    private String bedId;
    private String bedNumber;
    private String wardId;
    private String wardName;
    private String bedType; // General, Semi-Private, ICU, Recovery
    private String status;  // Available, Occupied, Cleaning, Maintenance

    // Assigned Patient
    private String patientUid;
    private String patientId;
    private String patientName;
    private String admissionDate; // YYYY-MM-DD

    // Assigned Care Team
    private String assignedDoctorUid;
    private String assignedDoctorName;
    private String assignedNurseUid;
    private String assignedNurseName;

    private String clinicalNotes;
    private long updatedAt;

    public Bed() {
        this.status = STATUS_AVAILABLE;
        this.bedType = "General";
        this.updatedAt = System.currentTimeMillis();
    }

    public Bed(String bedId, String bedNumber, String wardId, String wardName, String bedType, String status) {
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.wardId = wardId;
        this.wardName = wardName;
        this.bedType = bedType;
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("bedId", bedId);
        map.put("bedNumber", bedNumber);
        map.put("wardId", wardId);
        map.put("wardName", wardName);
        map.put("bedType", bedType);
        map.put("status", status);
        map.put("patientUid", patientUid != null ? patientUid : "");
        map.put("patientId", patientId != null ? patientId : "");
        map.put("patientName", patientName != null ? patientName : "");
        map.put("admissionDate", admissionDate != null ? admissionDate : "");
        map.put("assignedDoctorUid", assignedDoctorUid != null ? assignedDoctorUid : "");
        map.put("assignedDoctorName", assignedDoctorName != null ? assignedDoctorName : "");
        map.put("assignedNurseUid", assignedNurseUid != null ? assignedNurseUid : "");
        map.put("assignedNurseName", assignedNurseName != null ? assignedNurseName : "");
        map.put("clinicalNotes", clinicalNotes != null ? clinicalNotes : "");
        map.put("updatedAt", updatedAt);
        return map;
    }

    public boolean isAvailableForAdmission() {
        return STATUS_AVAILABLE.equalsIgnoreCase(status);
    }

    public String getBedId() { return bedId; }
    public void setBedId(String bedId) { this.bedId = bedId; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }

    public String getAssignedDoctorUid() { return assignedDoctorUid; }
    public void setAssignedDoctorUid(String assignedDoctorUid) { this.assignedDoctorUid = assignedDoctorUid; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public String getAssignedNurseUid() { return assignedNurseUid; }
    public void setAssignedNurseUid(String assignedNurseUid) { this.assignedNurseUid = assignedNurseUid; }

    public String getAssignedNurseName() { return assignedNurseName; }
    public void setAssignedNurseName(String assignedNurseName) { this.assignedNurseName = assignedNurseName; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
