package com.arohi.healthcare.models;

import java.io.Serializable;

/**
 * Phase 4 Unified Medical Record Timeline Entry.
 * Aggregates consultations, digital prescriptions, diagnostic lab tests, reports,
 * and appointments into a single chronological patient medical health history.
 */
public class MedicalRecordEntry implements Serializable, Comparable<MedicalRecordEntry> {

    public static final String CATEGORY_CONSULTATION = "CONSULTATION";
    public static final String CATEGORY_PRESCRIPTION = "PRESCRIPTION";
    public static final String CATEGORY_LAB_REPORT = "LAB_REPORT";
    public static final String CATEGORY_APPOINTMENT = "APPOINTMENT";

    private String entryId;
    private String patientUid;
    private String patientId;

    private String category;           // CONSULTATION, PRESCRIPTION, LAB_REPORT, APPOINTMENT
    private String title;              // e.g. "Prescription: Acute Bronchitis" or "Lab: CBC Report"
    private String subtitle;           // e.g. "Issued by Dr. Specialist"
    private String summary;            // Clinical findings, key medications or test results
    private String dateString;         // e.g. "2026-11-08" or formatted date
    private long timestamp;            // Milliseconds epoch for chronological sorting

    private String providerName;       // Doctor or Laboratory specialist
    private String providerRole;       // "DOCTOR", "LAB", "HOSPITAL"
    private String status;             // e.g. "Active", "Verified", "Report Ready"
    private String referenceId;        // ID linking to prescriptionId, labRequestId, or appointmentId
    private Object rawObject;          // Original DigitalPrescription, LaboratoryRequest, or Appointment

    public MedicalRecordEntry() {
        this.timestamp = System.currentTimeMillis();
    }

    public MedicalRecordEntry(String entryId, String patientUid, String patientId,
                              String category, String title, String subtitle, String summary,
                              String dateString, long timestamp,
                              String providerName, String providerRole, String status,
                              String referenceId) {
        this.entryId = entryId;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.summary = summary;
        this.dateString = dateString;
        this.timestamp = timestamp;
        this.providerName = providerName;
        this.providerRole = providerRole;
        this.status = status;
        this.referenceId = referenceId;
    }

    @Override
    public int compareTo(MedicalRecordEntry o) {
        if (o == null) return -1;
        // Chronological descending (most recent first)
        return Long.compare(o.timestamp, this.timestamp);
    }

    // Getters and Setters
    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDateString() { return dateString; }
    public void setDateString(String dateString) { this.dateString = dateString; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderRole() { return providerRole; }
    public void setProviderRole(String providerRole) { this.providerRole = providerRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Object getRawObject() { return rawObject; }
    public void setRawObject(Object rawObject) { this.rawObject = rawObject; }
}
