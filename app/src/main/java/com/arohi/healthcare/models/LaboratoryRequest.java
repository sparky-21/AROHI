package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 4 Laboratory Request & Diagnostic Report model.
 * Handles doctor test requests, processing statuses, laboratory report uploads,
 * Firebase Storage integration, and doctor review comments.
 */
public class LaboratoryRequest implements Serializable {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_SAMPLE_COLLECTED = "SAMPLE_COLLECTED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_REPORT_READY = "REPORT_READY";
    public static final String STATUS_REVIEWED = "REVIEWED_BY_DOCTOR";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String URGENCY_ROUTINE = "Routine";
    public static final String URGENCY_URGENT = "Urgent";
    public static final String URGENCY_EMERGENCY = "Emergency";

    private String requestId;
    private String appointmentId; // Optional link

    // Patient Demographics
    private String patientUid;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientBloodGroup;

    // Issuing Doctor
    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;

    // Test Information
    private String testName;            // e.g. "Complete Blood Count (CBC)", "Serum Creatinine"
    private String testCategory;        // e.g. "Hematology", "Biochemistry", "Radiology"
    private String clinicalIndication;  // Reason for ordering test
    private String urgency;             // Routine, Urgent, Emergency
    private String status;              // REQUESTED -> SAMPLE_COLLECTED -> PROCESSING -> REPORT_READY -> REVIEWED_BY_DOCTOR

    // Report Upload Information
    private String reportFileUrl;       // Firebase Storage download URL or local file path
    private String reportFileName;
    private String reportFileType;      // "PDF", "IMAGE"
    private String reportFindingsSummary;
    private long reportUploadedAt;

    // Doctor Clinical Review
    private String doctorReviewNotes;
    private long doctorReviewedAt;

    private long createdAt;
    private long updatedAt;

    public LaboratoryRequest() {
        this.status = STATUS_REQUESTED;
        this.urgency = URGENCY_ROUTINE;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("appointmentId", appointmentId);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId);
        map.put("patientName", patientName);
        map.put("patientPhone", patientPhone);
        map.put("patientBloodGroup", patientBloodGroup);

        map.put("doctorUid", doctorUid);
        map.put("doctorId", doctorId);
        map.put("doctorName", doctorName);
        map.put("doctorSpecialty", doctorSpecialty);

        map.put("testName", testName);
        map.put("testCategory", testCategory);
        map.put("clinicalIndication", clinicalIndication);
        map.put("urgency", urgency);
        map.put("status", status);

        map.put("reportFileUrl", reportFileUrl);
        map.put("reportFileName", reportFileName);
        map.put("reportFileType", reportFileType);
        map.put("reportFindingsSummary", reportFindingsSummary);
        map.put("reportUploadedAt", reportUploadedAt);

        map.put("doctorReviewNotes", doctorReviewNotes);
        map.put("doctorReviewedAt", doctorReviewedAt);

        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public static LaboratoryRequest fromMap(Map<String, Object> map) {
        if (map == null) return null;
        LaboratoryRequest r = new LaboratoryRequest();
        r.requestId = (String) map.get("requestId");
        r.appointmentId = (String) map.get("appointmentId");
        r.patientUid = (String) map.get("patientUid");
        r.patientId = (String) map.get("patientId");
        r.patientName = (String) map.get("patientName");
        r.patientPhone = (String) map.get("patientPhone");
        r.patientBloodGroup = (String) map.get("patientBloodGroup");

        r.doctorUid = (String) map.get("doctorUid");
        r.doctorId = (String) map.get("doctorId");
        r.doctorName = (String) map.get("doctorName");
        r.doctorSpecialty = (String) map.get("doctorSpecialty");

        r.testName = (String) map.get("testName");
        r.testCategory = (String) map.get("testCategory");
        r.clinicalIndication = (String) map.get("clinicalIndication");
        r.urgency = (String) map.get("urgency");
        r.status = (String) map.get("status");

        r.reportFileUrl = (String) map.get("reportFileUrl");
        r.reportFileName = (String) map.get("reportFileName");
        r.reportFileType = (String) map.get("reportFileType");
        r.reportFindingsSummary = (String) map.get("reportFindingsSummary");

        if (map.get("reportUploadedAt") instanceof Number) {
            r.reportUploadedAt = ((Number) map.get("reportUploadedAt")).longValue();
        }
        r.doctorReviewNotes = (String) map.get("doctorReviewNotes");
        if (map.get("doctorReviewedAt") instanceof Number) {
            r.doctorReviewedAt = ((Number) map.get("doctorReviewedAt")).longValue();
        }
        if (map.get("createdAt") instanceof Number) {
            r.createdAt = ((Number) map.get("createdAt")).longValue();
        }
        if (map.get("updatedAt") instanceof Number) {
            r.updatedAt = ((Number) map.get("updatedAt")).longValue();
        }
        return r;
    }

    public boolean isReportReady() {
        return STATUS_REPORT_READY.equalsIgnoreCase(status)
                || STATUS_REVIEWED.equalsIgnoreCase(status);
    }

    public boolean isReviewed() {
        return STATUS_REVIEWED.equalsIgnoreCase(status);
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPatientBloodGroup() { return patientBloodGroup; }
    public void setPatientBloodGroup(String patientBloodGroup) { this.patientBloodGroup = patientBloodGroup; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getTestCategory() { return testCategory; }
    public void setTestCategory(String testCategory) { this.testCategory = testCategory; }

    public String getClinicalIndication() { return clinicalIndication; }
    public void setClinicalIndication(String clinicalIndication) { this.clinicalIndication = clinicalIndication; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReportFileUrl() { return reportFileUrl; }
    public void setReportFileUrl(String reportFileUrl) { this.reportFileUrl = reportFileUrl; }

    public String getReportFileName() { return reportFileName; }
    public void setReportFileName(String reportFileName) { this.reportFileName = reportFileName; }

    public String getReportFileType() { return reportFileType; }
    public void setReportFileType(String reportFileType) { this.reportFileType = reportFileType; }

    public String getReportFindingsSummary() { return reportFindingsSummary; }
    public void setReportFindingsSummary(String reportFindingsSummary) { this.reportFindingsSummary = reportFindingsSummary; }

    public long getReportUploadedAt() { return reportUploadedAt; }
    public void setReportUploadedAt(long reportUploadedAt) { this.reportUploadedAt = reportUploadedAt; }

    public String getDoctorReviewNotes() { return doctorReviewNotes; }
    public void setDoctorReviewNotes(String doctorReviewNotes) { this.doctorReviewNotes = doctorReviewNotes; }

    public long getDoctorReviewedAt() { return doctorReviewedAt; }
    public void setDoctorReviewedAt(long doctorReviewedAt) { this.doctorReviewedAt = doctorReviewedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
