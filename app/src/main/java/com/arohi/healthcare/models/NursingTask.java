package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6 Nursing Task model.
 * Covers Medication schedules, Vital observations, Procedures, Doctor instructions, and Task completion.
 */
public class NursingTask implements Serializable {

    public static final String TYPE_MEDICATION = "Medication";
    public static final String TYPE_VITALS = "Vitals Check";
    public static final String TYPE_PROCEDURE = "Procedure";
    public static final String TYPE_DOCTOR_INSTRUCTION = "Doctor Instruction";
    public static final String TYPE_DRESSING = "Wound Dressing";
    public static final String TYPE_IV_FLUID = "IV Fluid";

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_MISSED = "Missed";

    private String taskId;
    private String taskType; // Medication, Vitals Check, Procedure, Doctor Instruction
    private String title;
    private String description;
    private String priority; // High, Normal, Urgent

    // Patient Info
    private String patientUid;
    private String patientId;
    private String patientName;
    private String wardName;
    private String bedNumber;

    // Nurse Info
    private String nurseUid;
    private String nurseName;

    // Schedule
    private String scheduledDate; // YYYY-MM-DD
    private String scheduledTime; // e.g. "10:00 AM"

    // Doctor info & instructions
    private String doctorUid;
    private String doctorName;
    private String doctorInstruction;

    // Completion
    private String status; // Pending, Completed
    private long completedAt;
    private String nurseCompletionNotes;

    public NursingTask() {
        this.status = STATUS_PENDING;
        this.priority = "Normal";
    }

    public NursingTask(String taskId, String taskType, String title, String description,
                       String priority, String patientUid, String patientId, String patientName,
                       String wardName, String bedNumber, String nurseUid, String nurseName,
                       String scheduledDate, String scheduledTime, String doctorName,
                       String doctorInstruction) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.patientName = patientName;
        this.wardName = wardName;
        this.bedNumber = bedNumber;
        this.nurseUid = nurseUid;
        this.nurseName = nurseName;
        this.scheduledDate = scheduledDate;
        this.scheduledTime = scheduledTime;
        this.doctorName = doctorName;
        this.doctorInstruction = doctorInstruction;
        this.status = STATUS_PENDING;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("taskType", taskType);
        map.put("title", title);
        map.put("description", description);
        map.put("priority", priority);
        map.put("patientUid", patientUid);
        map.put("patientId", patientId);
        map.put("patientName", patientName);
        map.put("wardName", wardName);
        map.put("bedNumber", bedNumber);
        map.put("nurseUid", nurseUid);
        map.put("nurseName", nurseName);
        map.put("scheduledDate", scheduledDate);
        map.put("scheduledTime", scheduledTime);
        map.put("doctorUid", doctorUid != null ? doctorUid : "");
        map.put("doctorName", doctorName != null ? doctorName : "");
        map.put("doctorInstruction", doctorInstruction != null ? doctorInstruction : "");
        map.put("status", status);
        map.put("completedAt", completedAt);
        map.put("nurseCompletionNotes", nurseCompletionNotes != null ? nurseCompletionNotes : "");
        return map;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public String getNurseUid() { return nurseUid; }
    public void setNurseUid(String nurseUid) { this.nurseUid = nurseUid; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorInstruction() { return doctorInstruction; }
    public void setDoctorInstruction(String doctorInstruction) { this.doctorInstruction = doctorInstruction; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public String getNurseCompletionNotes() { return nurseCompletionNotes; }
    public void setNurseCompletionNotes(String nurseCompletionNotes) { this.nurseCompletionNotes = nurseCompletionNotes; }
}
