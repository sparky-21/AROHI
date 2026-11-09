package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7 Smart Patient Journey Model.
 * Represents a visual 7-stage patient progression across:
 * Consultation -> Diagnosis -> Testing -> Treatment -> Operation -> Recovery -> Follow-up.
 * Automatically updated based on real authorized clinical workflow events.
 */
public class PatientJourney implements Serializable {

    public static final String STAGE_CONSULTATION = "CONSULTATION";
    public static final String STAGE_DIAGNOSIS = "DIAGNOSIS";
    public static final String STAGE_TESTING = "TESTING";
    public static final String STAGE_TREATMENT = "TREATMENT";
    public static final String STAGE_OPERATION = "OPERATION";
    public static final String STAGE_RECOVERY = "RECOVERY";
    public static final String STAGE_FOLLOW_UP = "FOLLOW_UP";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private String journeyId;
    private String patientUid;
    private String patientId;
    private String patientName;
    private String attendingDoctorUid;
    private String attendingDoctorName;
    private String primaryDiagnosis;
    private int currentStageIndex; // 0 to 6
    private boolean active;
    private long createdAt;
    private long updatedAt;
    private List<JourneyMilestone> milestones;

    public PatientJourney() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.active = true;
        this.currentStageIndex = 0;
        this.milestones = new ArrayList<>();
        initializeDefaultMilestones();
    }

    public PatientJourney(String journeyId, String patientUid, String patientId,
                          String patientName, String attendingDoctorUid, String attendingDoctorName) {
        this.journeyId = journeyId;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.patientName = patientName;
        this.attendingDoctorUid = attendingDoctorUid;
        this.attendingDoctorName = attendingDoctorName;
        this.active = true;
        this.currentStageIndex = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.milestones = new ArrayList<>();
        initializeDefaultMilestones();
    }

    public void initializeDefaultMilestones() {
        if (milestones == null) {
            milestones = new ArrayList<>();
        }
        if (!milestones.isEmpty()) return;

        milestones.add(new JourneyMilestone(STAGE_CONSULTATION, "1. Clinical Consultation",
                "Initial doctor evaluation and symptom review.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_DIAGNOSIS, "2. Clinical Diagnosis",
                "Formulation of verified clinical condition & diagnosis.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_TESTING, "3. Diagnostic Laboratory Testing",
                "Diagnostic investigations, pathology, and lab reports.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_TREATMENT, "4. Medical Treatment & Rx",
                "Digital prescription issuance and medical therapeutic plan.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_OPERATION, "5. Surgical Operation (OT)",
                "Pre-op checklist, surgical operation, and OT procedure.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_RECOVERY, "6. Inpatient Recovery & Vitals",
                "Ward bed recovery, nursing observations, and vital monitoring.", STATUS_PENDING));
        milestones.add(new JourneyMilestone(STAGE_FOLLOW_UP, "7. Follow-up & Discharge",
                "Post-operative follow-up review and final discharge summary.", STATUS_PENDING));
    }

    public JourneyMilestone getMilestone(String stageKey) {
        if (milestones == null || stageKey == null) return null;
        for (JourneyMilestone m : milestones) {
            if (stageKey.equalsIgnoreCase(m.getStageKey())) {
                return m;
            }
        }
        return null;
    }

    public void updateMilestone(String stageKey, String status, String performerName,
                                String performerRole, String summary, String referenceId) {
        JourneyMilestone m = getMilestone(stageKey);
        if (m != null) {
            m.setStatus(status);
            m.setPerformerName(performerName);
            m.setPerformerRole(performerRole);
            if (summary != null && !summary.isEmpty()) {
                m.setClinicalSummary(summary);
            }
            if (referenceId != null && !referenceId.isEmpty()) {
                m.setReferenceId(referenceId);
            }
            m.setUpdatedAt(System.currentTimeMillis());
            if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
                m.setCompletedAt(System.currentTimeMillis());
            }
        }
        this.updatedAt = System.currentTimeMillis();
        recalculateCurrentStageIndex();
    }

    private void recalculateCurrentStageIndex() {
        if (milestones == null) return;
        for (int i = 0; i < milestones.size(); i++) {
            JourneyMilestone m = milestones.get(i);
            if (!STATUS_COMPLETED.equalsIgnoreCase(m.getStatus()) &&
                !STATUS_SKIPPED.equalsIgnoreCase(m.getStatus())) {
                this.currentStageIndex = i;
                return;
            }
        }
        this.currentStageIndex = milestones.size() - 1;
    }

    // Getters and Setters
    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getAttendingDoctorUid() { return attendingDoctorUid; }
    public void setAttendingDoctorUid(String attendingDoctorUid) { this.attendingDoctorUid = attendingDoctorUid; }

    public String getAttendingDoctorName() { return attendingDoctorName; }
    public void setAttendingDoctorName(String attendingDoctorName) { this.attendingDoctorName = attendingDoctorName; }

    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public void setPrimaryDiagnosis(String primaryDiagnosis) { this.primaryDiagnosis = primaryDiagnosis; }

    public int getCurrentStageIndex() { return currentStageIndex; }
    public void setCurrentStageIndex(int currentStageIndex) { this.currentStageIndex = currentStageIndex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<JourneyMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<JourneyMilestone> milestones) { this.milestones = milestones; }

    /**
     * Inner Milestone Model for Individual Journey Stages
     */
    public static class JourneyMilestone implements Serializable {
        private String stageKey;
        private String title;
        private String description;
        private String status; // PENDING, IN_PROGRESS, COMPLETED, SKIPPED
        private String performerName;
        private String performerRole;
        private String clinicalSummary;
        private String referenceId;
        private long updatedAt;
        private long completedAt;

        public JourneyMilestone() {
            this.status = STATUS_PENDING;
            this.updatedAt = System.currentTimeMillis();
        }

        public JourneyMilestone(String stageKey, String title, String description, String status) {
            this.stageKey = stageKey;
            this.title = title;
            this.description = description;
            this.status = status;
            this.updatedAt = System.currentTimeMillis();
        }

        public String getStageKey() { return stageKey; }
        public void setStageKey(String stageKey) { this.stageKey = stageKey; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPerformerName() { return performerName; }
        public void setPerformerName(String performerName) { this.performerName = performerName; }

        public String getPerformerRole() { return performerRole; }
        public void setPerformerRole(String performerRole) { this.performerRole = performerRole; }

        public String getClinicalSummary() { return clinicalSummary; }
        public void setClinicalSummary(String clinicalSummary) { this.clinicalSummary = clinicalSummary; }

        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    }
}
