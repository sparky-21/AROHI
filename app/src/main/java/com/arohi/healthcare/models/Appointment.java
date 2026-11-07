package com.arohi.healthcare.models;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 4 Appointment model representing scheduled consultations between Patients and Doctors.
 * Supports duplicate prevention and scheduling conflict detection.
 */
public class Appointment {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_RESCHEDULED = "Rescheduled";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_COMPLETED = "Completed";

    public static final String TYPE_IN_PERSON = "In-Person Consultation";
    public static final String TYPE_VIDEO_CALL = "Video Call Consultation";
    public static final String TYPE_FOLLOW_UP = "Follow-up Consultation";

    private String appointmentId;
    private String patientUid;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientBloodGroup;

    private String doctorUid;
    private String doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private double consultationFee;

    private String appointmentDate; // Format: YYYY-MM-DD
    private String appointmentTime; // e.g. "10:00 AM"
    private String consultationType; // IN_PERSON, VIDEO_CALL, FOLLOW_UP
    private String reason;
    private String patientNotes;
    private String doctorNotes;
    private String cancellationReason;
    private String status;
    private long scheduledTimestamp;
    private long createdAt;
    private long updatedAt;

    public Appointment() {
        this.status = STATUS_PENDING;
        this.consultationType = TYPE_IN_PERSON;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Appointment(String appointmentId,
                       String patientUid, String patientId, String patientName, String patientPhone,
                       String doctorUid, String doctorId, String doctorName, String doctorSpecialty,
                       double consultationFee,
                       String appointmentDate, String appointmentTime,
                       String consultationType, String reason, String patientNotes) {
        this();
        this.appointmentId = appointmentId;
        this.patientUid = patientUid;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.doctorUid = doctorUid;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorSpecialty = doctorSpecialty;
        this.consultationFee = consultationFee;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.consultationType = consultationType != null ? consultationType : TYPE_IN_PERSON;
        this.reason = reason;
        this.patientNotes = patientNotes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
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
        map.put("consultationFee", consultationFee);
        map.put("appointmentDate", appointmentDate);
        map.put("appointmentTime", appointmentTime);
        map.put("consultationType", consultationType);
        map.put("reason", reason);
        map.put("patientNotes", patientNotes);
        map.put("doctorNotes", doctorNotes);
        map.put("cancellationReason", cancellationReason);
        map.put("status", status);
        map.put("scheduledTimestamp", scheduledTimestamp);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equalsIgnoreCase(status);
    }

    public boolean isRescheduled() {
        return STATUS_RESCHEDULED.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    public boolean isActive() {
        return !isCancelled() && !isCompleted();
    }

    // Getters and Setters
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

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getConsultationType() { return consultationType; }
    public void setConsultationType(String consultationType) { this.consultationType = consultationType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPatientNotes() { return patientNotes; }
    public void setPatientNotes(String patientNotes) { this.patientNotes = patientNotes; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getScheduledTimestamp() { return scheduledTimestamp; }
    public void setScheduledTimestamp(long scheduledTimestamp) { this.scheduledTimestamp = scheduledTimestamp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
