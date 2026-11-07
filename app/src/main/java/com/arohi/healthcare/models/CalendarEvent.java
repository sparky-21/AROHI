package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 4 Unified Calendar Event model.
 * Synchronizes scheduled activities across Patients and Doctors, including Appointments,
 * Laboratory tests, Operations, Follow-ups, and Medicine reminders.
 * Prepared for Nurse shift/ward rounds and Hospital Admin OT scheduling in Phase 6.
 */
public class CalendarEvent implements Serializable, Comparable<CalendarEvent> {

    public static final String TYPE_APPOINTMENT = "APPOINTMENT";
    public static final String TYPE_LAB_TEST = "LAB_TEST";
    public static final String TYPE_OPERATION = "OPERATION";
    public static final String TYPE_FOLLOW_UP = "FOLLOW_UP";
    public static final String TYPE_MED_REMINDER = "MED_REMINDER";
    public static final String TYPE_NURSE_SHIFT = "NURSE_SHIFT";
    public static final String TYPE_WARD_ROUND = "WARD_ROUND";
    public static final String TYPE_OT_SCHEDULE = "OT_SCHEDULE";

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_RESCHEDULED = "RESCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String eventId;
    private String title;
    private String description;
    private String eventType;          // APPOINTMENT, LAB_TEST, OPERATION, FOLLOW_UP, etc.
    private String eventDate;          // Format: YYYY-MM-DD
    private String eventTime;          // e.g. "10:30 AM"
    private long timestamp;            // Milliseconds epoch for sorting

    // Participants (Supports Patient, Doctor, Nurse & Admin)
    private String patientUid;
    private String patientName;
    private String patientId;

    private String doctorUid;
    private String doctorName;
    private String doctorId;

    private String nurseUid;           // Prepared for Phase 6
    private String nurseName;

    private String departmentOrLocation; // e.g. "Cardiology OPD Room 3", "Main OT-2", "General Ward B"
    private String status;               // SCHEDULED, CONFIRMED, RESCHEDULED, COMPLETED, CANCELLED
    private String referenceId;          // ID of appointment, lab request, prescription, or operation
    private String roleVisibility;       // "PATIENT", "DOCTOR", "NURSE", "ADMIN", "ALL"

    private long createdAt;
    private long updatedAt;

    public CalendarEvent() {
        this.status = STATUS_SCHEDULED;
        this.roleVisibility = "ALL";
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public CalendarEvent(String eventId, String title, String description, String eventType,
                         String eventDate, String eventTime, long timestamp,
                         String patientUid, String patientName,
                         String doctorUid, String doctorName,
                         String departmentOrLocation, String referenceId) {
        this();
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.timestamp = timestamp;
        this.patientUid = patientUid;
        this.patientName = patientName;
        this.doctorUid = doctorUid;
        this.doctorName = doctorName;
        this.departmentOrLocation = departmentOrLocation;
        this.referenceId = referenceId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("title", title);
        map.put("description", description);
        map.put("eventType", eventType);
        map.put("eventDate", eventDate);
        map.put("eventTime", eventTime);
        map.put("timestamp", timestamp);

        map.put("patientUid", patientUid);
        map.put("patientName", patientName);
        map.put("patientId", patientId);

        map.put("doctorUid", doctorUid);
        map.put("doctorName", doctorName);
        map.put("doctorId", doctorId);

        map.put("nurseUid", nurseUid);
        map.put("nurseName", nurseName);

        map.put("departmentOrLocation", departmentOrLocation);
        map.put("status", status);
        map.put("referenceId", referenceId);
        map.put("roleVisibility", roleVisibility);

        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public static CalendarEvent fromMap(Map<String, Object> map) {
        if (map == null) return null;
        CalendarEvent e = new CalendarEvent();
        e.eventId = (String) map.get("eventId");
        e.title = (String) map.get("title");
        e.description = (String) map.get("description");
        e.eventType = (String) map.get("eventType");
        e.eventDate = (String) map.get("eventDate");
        e.eventTime = (String) map.get("eventTime");
        if (map.get("timestamp") instanceof Number) {
            e.timestamp = ((Number) map.get("timestamp")).longValue();
        }

        e.patientUid = (String) map.get("patientUid");
        e.patientName = (String) map.get("patientName");
        e.patientId = (String) map.get("patientId");

        e.doctorUid = (String) map.get("doctorUid");
        e.doctorName = (String) map.get("doctorName");
        e.doctorId = (String) map.get("doctorId");

        e.nurseUid = (String) map.get("nurseUid");
        e.nurseName = (String) map.get("nurseName");

        e.departmentOrLocation = (String) map.get("departmentOrLocation");
        e.status = (String) map.get("status");
        e.referenceId = (String) map.get("referenceId");
        e.roleVisibility = (String) map.get("roleVisibility");

        if (map.get("createdAt") instanceof Number) {
            e.createdAt = ((Number) map.get("createdAt")).longValue();
        }
        if (map.get("updatedAt") instanceof Number) {
            e.updatedAt = ((Number) map.get("updatedAt")).longValue();
        }
        return e;
    }

    @Override
    public int compareTo(CalendarEvent o) {
        if (o == null) return 1;
        return Long.compare(this.timestamp, o.timestamp);
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorUid() { return doctorUid; }
    public void setDoctorUid(String doctorUid) { this.doctorUid = doctorUid; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getNurseUid() { return nurseUid; }
    public void setNurseUid(String nurseUid) { this.nurseUid = nurseUid; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public String getDepartmentOrLocation() { return departmentOrLocation; }
    public void setDepartmentOrLocation(String departmentOrLocation) { this.departmentOrLocation = departmentOrLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getRoleVisibility() { return roleVisibility; }
    public void setRoleVisibility(String roleVisibility) { this.roleVisibility = roleVisibility; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
