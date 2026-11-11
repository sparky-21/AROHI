package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Operation Theatre (OT) Surgical Operation model.
 * Tracks patient, attending surgeon (doctor), scrub/assisting nurse, OT room,
 * date/time, pre-operation checklist, lifecycle status, post-operative notes, and follow-up date.
 */
public class Operation implements Serializable {

    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_PREPARATION = "Preparation";
    public static final String STATUS_SHIFTED_TO_OT = "Patient Shifted to OT";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_RECOVERY = "Recovery";
    public static final String STATUS_FOLLOW_UP = "Follow-up";
    public static final String STATUS_CANCELLED = "Cancelled";

    private String operationId;
    private String hospitalId;
    private String procedureName;
    private String department;
    private String otRoomId;
    private String otRoomName;
    private String operationDate; // YYYY-MM-DD
    private String operationTime; // e.g. "09:00 AM"
    private int estimatedDurationMinutes;

    // Patient Info
    private String patientUid;
    private String patientId;
    private String patientName;

    // Doctor Info
    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;

    // Nurse Info
    private String nurseUid;
    private String nurseName;

    // Pre-op checklist
    private boolean npoConfirmed;
    private boolean anesthesiaCleared;
    private boolean consentSigned;
    private boolean bloodArranged;
    private boolean preOpAntibioticsGiven;

    // Status & Notes
    private String status; // Scheduled -> Preparation -> In Progress -> Completed -> Follow-up
    private String postOpNotes;
    private String followUpDate; // YYYY-MM-DD
    private String surgicalNotes;

    private long createdAt;
    private long updatedAt;
    private java.util.List<String> authorizedFamilyUids;
    private java.util.List<TrackingStatusEvent> trackingHistory;

    public Operation() {
        this.hospitalId = "hosp_arohi_central";
        this.status = STATUS_SCHEDULED;
        this.estimatedDurationMinutes = 90;
        this.authorizedFamilyUids = new java.util.ArrayList<>();
        this.trackingHistory = new java.util.ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Operation(String operationId, String procedureName, String department,
                     String otRoomId, String otRoomName, String operationDate, String operationTime,
                     int estimatedDurationMinutes, String patientUid, String patientId, String patientName,
                     String doctorUid, String doctorId, String doctorName, String doctorSpecialty,
                     String nurseUid, String nurseName) {
        this.operationId = operationId;
        this.procedureName = procedureName;
        this.department = department;
        this.otRoomId = otRoomId;
        this.otRoomName = otRoomName;
        this.operationDate = operationDate;
        this.operationTime = operationTime;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorUid = doctorUid;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorSpecialty = doctorSpecialty;
        this.nurseUid = nurseUid;
        this.nurseName = nurseName;
        this.hospitalId = "hosp_arohi_central";
        this.status = STATUS_SCHEDULED;
        this.authorizedFamilyUids = new java.util.ArrayList<>();
        this.trackingHistory = new java.util.ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        recordTrackingEvent(STATUS_SCHEDULED, doctorUid, doctorName, "DOCTOR", "Operation scheduled in " + otRoomName);
    }

    public int getStatusStepIndex() {
        if (STATUS_SCHEDULED.equalsIgnoreCase(status)) return 0;
        if (STATUS_PREPARATION.equalsIgnoreCase(status)) return 1;
        if (STATUS_SHIFTED_TO_OT.equalsIgnoreCase(status)) return 2;
        if (STATUS_IN_PROGRESS.equalsIgnoreCase(status)) return 3;
        if (STATUS_COMPLETED.equalsIgnoreCase(status)) return 4;
        if (STATUS_RECOVERY.equalsIgnoreCase(status)) return 5;
        if (STATUS_FOLLOW_UP.equalsIgnoreCase(status)) return 6;
        return -1;
    }

    public boolean isValidTransition(String nextStatus) {
        if (nextStatus == null) return false;
        if (STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            return STATUS_SCHEDULED.equalsIgnoreCase(status) || STATUS_PREPARATION.equalsIgnoreCase(status);
        }
        if (STATUS_SCHEDULED.equalsIgnoreCase(status)) return STATUS_PREPARATION.equalsIgnoreCase(nextStatus);
        if (STATUS_PREPARATION.equalsIgnoreCase(status)) {
            return STATUS_SHIFTED_TO_OT.equalsIgnoreCase(nextStatus)
                    || STATUS_IN_PROGRESS.equalsIgnoreCase(nextStatus);
        }
        if (STATUS_SHIFTED_TO_OT.equalsIgnoreCase(status)) return STATUS_IN_PROGRESS.equalsIgnoreCase(nextStatus);
        if (STATUS_IN_PROGRESS.equalsIgnoreCase(status)) return STATUS_COMPLETED.equalsIgnoreCase(nextStatus);
        if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return STATUS_RECOVERY.equalsIgnoreCase(nextStatus)
                    || STATUS_FOLLOW_UP.equalsIgnoreCase(nextStatus);
        }
        if (STATUS_RECOVERY.equalsIgnoreCase(status)) return STATUS_FOLLOW_UP.equalsIgnoreCase(nextStatus);
        return false;
    }

    public void recordTrackingEvent(String statusLabel, String actorUid, String actorName,
                                    String actorRole, String note) {
        if (trackingHistory == null) trackingHistory = new java.util.ArrayList<>();
        trackingHistory.add(new TrackingStatusEvent(
                statusLabel, System.currentTimeMillis(), actorUid, actorName, actorRole, note
        ));
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isFamilyViewerAuthorized(String viewerUid) {
        if (viewerUid == null || viewerUid.trim().isEmpty()) return false;
        if (viewerUid.equals(patientUid)) return true;
        return authorizedFamilyUids != null && authorizedFamilyUids.contains(viewerUid);
    }

    /**
     * Privacy-safe summary exposed to explicitly authorized family members:
     * exposes stage, OT room, surgeon, and schedule without sensitive surgical/post-op notes.
     */
    public String toFamilyPermittedSummary() {
        return "Status: " + status
                + " • Room: " + (otRoomName != null ? otRoomName : "OT")
                + " • Lead Surgeon: " + (doctorName != null ? doctorName : "Assigned Specialist")
                + " • Schedule: " + (operationDate != null ? operationDate : "")
                + (operationTime != null ? " " + operationTime : "");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("operationId", operationId);
        map.put("hospitalId", hospitalId != null ? hospitalId : "hosp_arohi_central");
        map.put("procedureName", procedureName);
        map.put("department", department);
        map.put("otRoomId", otRoomId);
        map.put("otRoomName", otRoomName);
        map.put("operationDate", operationDate);
        map.put("operationTime", operationTime);
        map.put("estimatedDurationMinutes", estimatedDurationMinutes);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId);
        map.put("patientName", patientName);
        map.put("doctorUid", doctorUid);
        map.put("doctorId", doctorId);
        map.put("doctorName", doctorName);
        map.put("doctorSpecialty", doctorSpecialty);
        map.put("nurseUid", nurseUid);
        map.put("nurseName", nurseName);
        map.put("npoConfirmed", npoConfirmed);
        map.put("anesthesiaCleared", anesthesiaCleared);
        map.put("consentSigned", consentSigned);
        map.put("bloodArranged", bloodArranged);
        map.put("preOpAntibioticsGiven", preOpAntibioticsGiven);
        map.put("status", status);
        map.put("postOpNotes", postOpNotes != null ? postOpNotes : "");
        map.put("followUpDate", followUpDate != null ? followUpDate : "");
        map.put("surgicalNotes", surgicalNotes != null ? surgicalNotes : "");
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("authorizedFamilyUids", authorizedFamilyUids != null ? authorizedFamilyUids : new java.util.ArrayList<>());
        java.util.List<Map<String, Object>> historyList = new java.util.ArrayList<>();
        if (trackingHistory != null) {
            for (TrackingStatusEvent ev : trackingHistory) {
                historyList.add(ev.toMap());
            }
        }
        map.put("trackingHistory", historyList);
        return map;
    }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOtRoomId() { return otRoomId; }
    public void setOtRoomId(String otRoomId) { this.otRoomId = otRoomId; }

    public String getOtRoomName() { return otRoomName; }
    public void setOtRoomName(String otRoomName) { this.otRoomName = otRoomName; }

    public String getOperationDate() { return operationDate; }
    public void setOperationDate(String operationDate) { this.operationDate = operationDate; }

    public String getOperationTime() { return operationTime; }
    public void setOperationTime(String operationTime) { this.operationTime = operationTime; }

    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getNurseUid() { return nurseUid; }
    public void setNurseUid(String nurseUid) { this.nurseUid = nurseUid; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public boolean isNpoConfirmed() { return npoConfirmed; }
    public void setNpoConfirmed(boolean npoConfirmed) { this.npoConfirmed = npoConfirmed; }

    public boolean isAnesthesiaCleared() { return anesthesiaCleared; }
    public void setAnesthesiaCleared(boolean anesthesiaCleared) { this.anesthesiaCleared = anesthesiaCleared; }

    public boolean isConsentSigned() { return consentSigned; }
    public void setConsentSigned(boolean consentSigned) { this.consentSigned = consentSigned; }

    public boolean isBloodArranged() { return bloodArranged; }
    public void setBloodArranged(boolean bloodArranged) { this.bloodArranged = bloodArranged; }

    public boolean isPreOpAntibioticsGiven() { return preOpAntibioticsGiven; }
    public void setPreOpAntibioticsGiven(boolean preOpAntibioticsGiven) { this.preOpAntibioticsGiven = preOpAntibioticsGiven; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPostOpNotes() { return postOpNotes; }
    public void setPostOpNotes(String postOpNotes) { this.postOpNotes = postOpNotes; }

    public String getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(String followUpDate) { this.followUpDate = followUpDate; }

    public String getSurgicalNotes() { return surgicalNotes; }
    public void setSurgicalNotes(String surgicalNotes) { this.surgicalNotes = surgicalNotes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public java.util.List<String> getAuthorizedFamilyUids() { return authorizedFamilyUids; }
    public void setAuthorizedFamilyUids(java.util.List<String> authorizedFamilyUids) {
        this.authorizedFamilyUids = authorizedFamilyUids != null ? authorizedFamilyUids : new java.util.ArrayList<>();
    }

    public java.util.List<TrackingStatusEvent> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(java.util.List<TrackingStatusEvent> trackingHistory) {
        this.trackingHistory = trackingHistory != null ? trackingHistory : new java.util.ArrayList<>();
    }
}
