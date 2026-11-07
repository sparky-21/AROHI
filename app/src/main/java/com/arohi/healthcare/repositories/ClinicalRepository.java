package com.arohi.healthcare.repositories;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.MedicalRecordEntry;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.PrescriptionMedicine;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 4 Core Clinical Repository managing:
 * 1. Appointment booking, scheduling conflict detection, confirmation, rescheduling, and cancellation.
 * 2. Unified synchronized medical calendar for patients and doctors.
 * 3. Digital prescription issuance, dosage schedules, and history.
 * 4. Diagnostic laboratory requests, status progression, file upload, and doctor review.
 * 5. Aggregated patient medical record timeline.
 * 6. Privacy-safe notifications across clinical workflows.
 */
public class ClinicalRepository {

    private final Context appContext;
    private final SessionManager sessionManager;

    public ClinicalRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    // =========================================================================
    // 1. APPOINTMENT MANAGEMENT & SCHEDULING CONFLICT DETECTION
    // =========================================================================
    public void requestAppointment(@NonNull Appointment appt,
                                   @NonNull AuthCallback<Appointment> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("Authentication Error: You must be logged in to book an appointment.");
            return;
        }

        if (appt.getAppointmentDate() == null || appt.getAppointmentDate().trim().isEmpty()
                || appt.getAppointmentTime() == null || appt.getAppointmentTime().trim().isEmpty()) {
            callback.onError("Please select both a valid appointment date and time slot.");
            return;
        }

        final String date = appt.getAppointmentDate().trim();
        final String time = appt.getAppointmentTime().trim();
        final String docUid = appt.getDoctorUid();
        final String patUid = appt.getPatientUid();

        // 1. Check local appointment registry for scheduling conflicts
        for (Appointment existing : sessionManager.getAllAppointmentsLocal()) {
            if (existing.isActive()) {
                if (date.equals(existing.getAppointmentDate()) && time.equalsIgnoreCase(existing.getAppointmentTime())) {
                    if (docUid != null && docUid.equals(existing.getDoctorUid())) {
                        callback.onError("Scheduling Conflict: Dr. " + existing.getDoctorName()
                                + " is already booked for " + time + " on " + date + ". Please choose another slot.");
                        return;
                    }
                    if (patUid != null && patUid.equals(existing.getPatientUid())) {
                        callback.onError("Scheduling Conflict: You already have an active appointment scheduled at "
                                + time + " on " + date + ".");
                        return;
                    }
                }
                // Duplicate check
                if (docUid != null && docUid.equals(existing.getDoctorUid())
                        && patUid != null && patUid.equals(existing.getPatientUid())
                        && date.equals(existing.getAppointmentDate())) {
                    callback.onError("Duplicate Appointment: You already have a scheduled consultation with Dr. "
                            + existing.getDoctorName() + " on " + date + ".");
                    return;
                }
            }
        }

        // Initialize appointment metadata
        String apptId = "appt_" + UUID.randomUUID().toString().substring(0, 10);
        long now = System.currentTimeMillis();
        appt.setAppointmentId(apptId);
        appt.setStatus(Appointment.STATUS_PENDING);
        appt.setCreatedAt(now);
        appt.setUpdatedAt(now);

        // Synchronized Calendar Event
        String eventId = "evt_appt_" + apptId;
        CalendarEvent event = new CalendarEvent(
                eventId,
                "Consultation: " + appt.getDoctorName(),
                "Reason: " + appt.getReason() + " (" + appt.getConsultationType() + ")",
                CalendarEvent.TYPE_APPOINTMENT,
                date,
                time,
                now,
                patUid,
                appt.getPatientName(),
                docUid,
                appt.getDoctorName(),
                appt.getConsultationType(),
                apptId
        );
        event.setStatus(CalendarEvent.STATUS_SCHEDULED);

        // Privacy-Safe Notification to Doctor
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification docNotif = new AppNotification(
                notifId,
                docUid,
                patUid,
                appt.getPatientName(),
                AppNotification.TYPE_APPOINTMENT_REQUEST,
                "New Appointment Request",
                appt.getPatientName() + " requested a " + appt.getConsultationType() + " on " + date + " at " + time + ".",
                apptId
        );

        sessionManager.saveAppointmentLocal(appt);
        sessionManager.saveCalendarEventLocal(event);
        sessionManager.saveNotificationLocal(docNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(appt);
            return;
        }

        // Check remote Firestore for conflict before committing
        db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("doctorUid", docUid)
                .whereEqualTo("appointmentDate", date)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Appointment remote = doc.toObject(Appointment.class);
                        if (remote != null && remote.isActive()) {
                            if (time.equalsIgnoreCase(remote.getAppointmentTime())) {
                                callback.onError("Remote Conflict: Dr. " + appt.getDoctorName()
                                        + " was just booked for " + time + " on " + date + ". Please select another slot.");
                                return;
                            }
                        }
                    }

                    WriteBatch batch = db.batch();
                    batch.set(db.collection(FirestoreCollections.APPOINTMENTS).document(apptId), appt.toMap());
                    batch.set(db.collection(FirestoreCollections.CALENDAR_EVENTS).document(eventId), event.toMap());
                    batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), docNotif.toMap());

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(appt))
                            .addOnFailureListener(e -> callback.onSuccess(appt));
                })
                .addOnFailureListener(e -> callback.onSuccess(appt));
    }

    public void loadPatientAppointments(String patientUid, AuthCallback<List<Appointment>> callback) {
        if (patientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local appointments immediately
        callback.onSuccess(filterAppointmentsByPatient(patientUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Appointment> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (a != null) remote.add(a);
                    }
                    callback.onSuccess(filterAppointmentsByPatient(patientUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterAppointmentsByPatient(patientUid, null)));
    }

    private List<Appointment> filterAppointmentsByPatient(String patientUid, List<Appointment> remote) {
        Map<String, Appointment> map = new LinkedHashMap<>();
        for (Appointment local : sessionManager.getAllAppointmentsLocal()) {
            if (patientUid.equals(local.getPatientUid()) && local.getAppointmentId() != null) {
                map.put(local.getAppointmentId(), local);
            }
        }
        if (remote != null) {
            for (Appointment r : remote) {
                if (r.getAppointmentId() != null) {
                    map.put(r.getAppointmentId(), r);
                    sessionManager.saveAppointmentLocal(r);
                }
            }
        }
        List<Appointment> result = new ArrayList<>(map.values());
        Collections.sort(result, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return result;
    }

    public void loadDoctorAppointments(String doctorUid, AuthCallback<List<Appointment>> callback) {
        if (doctorUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local appointments immediately
        callback.onSuccess(filterAppointmentsByDoctor(doctorUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Appointment> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (a != null) remote.add(a);
                    }
                    callback.onSuccess(filterAppointmentsByDoctor(doctorUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterAppointmentsByDoctor(doctorUid, null)));
    }

    private List<Appointment> filterAppointmentsByDoctor(String doctorUid, List<Appointment> remote) {
        Map<String, Appointment> map = new LinkedHashMap<>();
        for (Appointment local : sessionManager.getAllAppointmentsLocal()) {
            if (doctorUid.equals(local.getDoctorUid()) && local.getAppointmentId() != null) {
                map.put(local.getAppointmentId(), local);
            }
        }
        if (remote != null) {
            for (Appointment r : remote) {
                if (r.getAppointmentId() != null) {
                    map.put(r.getAppointmentId(), r);
                    sessionManager.saveAppointmentLocal(r);
                }
            }
        }
        List<Appointment> result = new ArrayList<>(map.values());
        Collections.sort(result, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return result;
    }

    public void updateAppointmentStatus(@NonNull Appointment appointment,
                                        @NonNull String newStatus,
                                        String doctorNotes,
                                        String cancellationReason,
                                        AuthCallback<Void> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("Authentication error.");
            return;
        }

        long now = System.currentTimeMillis();
        appointment.setStatus(newStatus);
        if (doctorNotes != null) appointment.setDoctorNotes(doctorNotes.trim());
        if (cancellationReason != null) appointment.setCancellationReason(cancellationReason.trim());
        appointment.setUpdatedAt(now);
        sessionManager.saveAppointmentLocal(appointment);

        // Update corresponding CalendarEvent
        String eventId = "evt_appt_" + appointment.getAppointmentId();
        CalendarEvent event = sessionManager.getCalendarEventLocal(eventId);
        if (event != null) {
            if (Appointment.STATUS_CONFIRMED.equalsIgnoreCase(newStatus)) {
                event.setStatus(CalendarEvent.STATUS_CONFIRMED);
            } else if (Appointment.STATUS_CANCELLED.equalsIgnoreCase(newStatus)) {
                event.setStatus(CalendarEvent.STATUS_CANCELLED);
            } else if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(newStatus)) {
                event.setStatus(CalendarEvent.STATUS_COMPLETED);
            }
            event.setUpdatedAt(now);
            sessionManager.saveCalendarEventLocal(event);
        }

        // Notification to Counterparty
        boolean isDoctorSender = UserRole.DOCTOR == current.getUserRoleEnum();
        String recipientUid = isDoctorSender ? appointment.getPatientUid() : appointment.getDoctorUid();
        String notifTitle = "Appointment " + newStatus;
        String notifBody = isDoctorSender
                ? "Dr. " + current.getFullName() + " marked your appointment on " + appointment.getAppointmentDate() + " as " + newStatus + "."
                : current.getFullName() + " updated consultation appointment status to " + newStatus + ".";

        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification notif = new AppNotification(
                notifId,
                recipientUid,
                current.getUid(),
                current.getFullName(),
                AppNotification.TYPE_APPOINTMENT_UPDATE,
                notifTitle,
                notifBody,
                appointment.getAppointmentId()
        );
        sessionManager.saveNotificationLocal(notif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.APPOINTMENTS).document(appointment.getAppointmentId()),
                appointment.toMap(), SetOptions.merge());
        if (event != null) {
            batch.set(db.collection(FirestoreCollections.CALENDAR_EVENTS).document(eventId),
                    event.toMap(), SetOptions.merge());
        }
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), notif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    public void rescheduleAppointment(@NonNull Appointment appointment,
                                      @NonNull String newDate,
                                      @NonNull String newTime,
                                      String reason,
                                      AuthCallback<Void> callback) {
        if (newDate.trim().isEmpty() || newTime.trim().isEmpty()) {
            callback.onError("Please select both a new date and time slot.");
            return;
        }

        // Conflict check for new slot
        for (Appointment existing : sessionManager.getAllAppointmentsLocal()) {
            if (existing.isActive() && !existing.getAppointmentId().equals(appointment.getAppointmentId())) {
                if (newDate.equals(existing.getAppointmentDate()) && newTime.equalsIgnoreCase(existing.getAppointmentTime())) {
                    if (appointment.getDoctorUid() != null && appointment.getDoctorUid().equals(existing.getDoctorUid())) {
                        callback.onError("Doctor Conflict: Slot " + newTime + " on " + newDate + " is already booked.");
                        return;
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        appointment.setAppointmentDate(newDate.trim());
        appointment.setAppointmentTime(newTime.trim());
        appointment.setStatus(Appointment.STATUS_RESCHEDULED);
        if (reason != null && !reason.trim().isEmpty()) {
            appointment.setPatientNotes(appointment.getPatientNotes() + " | Reschedule Reason: " + reason.trim());
        }
        appointment.setUpdatedAt(now);
        sessionManager.saveAppointmentLocal(appointment);

        // Update CalendarEvent
        String eventId = "evt_appt_" + appointment.getAppointmentId();
        CalendarEvent event = sessionManager.getCalendarEventLocal(eventId);
        if (event != null) {
            event.setEventDate(newDate.trim());
            event.setEventTime(newTime.trim());
            event.setStatus(CalendarEvent.STATUS_RESCHEDULED);
            event.setUpdatedAt(now);
            sessionManager.saveCalendarEventLocal(event);
        }

        // Dispatch notification
        UserAccount current = sessionManager.getCurrentSessionAccount();
        String senderName = current != null ? current.getFullName() : "User";
        String recipientUid = (current != null && current.getUid().equals(appointment.getPatientUid()))
                ? appointment.getDoctorUid()
                : appointment.getPatientUid();

        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification notif = new AppNotification(
                notifId,
                recipientUid,
                current != null ? current.getUid() : "",
                senderName,
                AppNotification.TYPE_APPOINTMENT_RESCHEDULED,
                "Appointment Rescheduled",
                "Consultation rescheduled to " + newDate.trim() + " at " + newTime.trim() + ".",
                appointment.getAppointmentId()
        );
        sessionManager.saveNotificationLocal(notif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.APPOINTMENTS).document(appointment.getAppointmentId()),
                appointment.toMap(), SetOptions.merge());
        if (event != null) {
            batch.set(db.collection(FirestoreCollections.CALENDAR_EVENTS).document(eventId),
                    event.toMap(), SetOptions.merge());
        }
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), notif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    // =========================================================================
    // 2. UNIFIED SYNCHRONIZED CALENDAR
    // =========================================================================
    public void loadUnifiedCalendarEvents(String userUid,
                                          UserRole role,
                                          String filterDate,
                                          AuthCallback<List<CalendarEvent>> callback) {
        if (userUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local calendar events immediately
        callback.onSuccess(buildMergedCalendarEvents(userUid, role, filterDate, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        String queryField = (role == UserRole.DOCTOR) ? "doctorUid" : "patientUid";
        db.collection(FirestoreCollections.CALENDAR_EVENTS)
                .whereEqualTo(queryField, userUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<CalendarEvent> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        CalendarEvent ev = doc.toObject(CalendarEvent.class);
                        if (ev != null) remote.add(ev);
                    }
                    callback.onSuccess(buildMergedCalendarEvents(userUid, role, filterDate, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(buildMergedCalendarEvents(userUid, role, filterDate, null)));
    }

    private List<CalendarEvent> buildMergedCalendarEvents(String userUid,
                                                          UserRole role,
                                                          String filterDate,
                                                          List<CalendarEvent> remote) {
        Map<String, CalendarEvent> map = new LinkedHashMap<>();

        // 1. From local calendar registry
        for (CalendarEvent local : sessionManager.getAllCalendarEventsLocal()) {
            boolean matchesUser = (role == UserRole.DOCTOR)
                    ? userUid.equals(local.getDoctorUid())
                    : userUid.equals(local.getPatientUid());
            if (matchesUser && local.getEventId() != null) {
                map.put(local.getEventId(), local);
            }
        }

        // 2. Remote Firestore events
        if (remote != null) {
            for (CalendarEvent r : remote) {
                if (r.getEventId() != null) {
                    map.put(r.getEventId(), r);
                    sessionManager.saveCalendarEventLocal(r);
                }
            }
        }

        // 3. Synthesize events from active Appointments if not already in calendar
        List<Appointment> appts = (role == UserRole.DOCTOR)
                ? sessionManager.getAllAppointmentsLocal()
                : sessionManager.getAllAppointmentsLocal();
        for (Appointment a : appts) {
            boolean matches = (role == UserRole.DOCTOR)
                    ? userUid.equals(a.getDoctorUid())
                    : userUid.equals(a.getPatientUid());
            if (matches && a.isActive()) {
                String eventId = "evt_appt_" + a.getAppointmentId();
                if (!map.containsKey(eventId)) {
                    CalendarEvent ev = new CalendarEvent(
                            eventId,
                            "Appointment: " + (role == UserRole.DOCTOR ? a.getPatientName() : a.getDoctorName()),
                            a.getConsultationType() + " • " + a.getReason(),
                            CalendarEvent.TYPE_APPOINTMENT,
                            a.getAppointmentDate(),
                            a.getAppointmentTime(),
                            a.getScheduledTimestamp() > 0 ? a.getScheduledTimestamp() : a.getCreatedAt(),
                            a.getPatientUid(),
                            a.getPatientName(),
                            a.getDoctorUid(),
                            a.getDoctorName(),
                            a.getConsultationType(),
                            a.getAppointmentId()
                    );
                    map.put(eventId, ev);
                }
            }
        }

        // 4. Synthesize follow-up events from active Prescriptions
        for (DigitalPrescription rx : sessionManager.getAllPrescriptionsLocal()) {
            boolean matches = (role == UserRole.DOCTOR)
                    ? userUid.equals(rx.getDoctorUid())
                    : userUid.equals(rx.getPatientUid());
            if (matches && rx.getFollowUpDate() != null && !rx.getFollowUpDate().trim().isEmpty()) {
                String eventId = "evt_followup_" + rx.getPrescriptionId();
                if (!map.containsKey(eventId)) {
                    CalendarEvent ev = new CalendarEvent(
                            eventId,
                            "Follow-up Consultation: " + rx.getPatientName(),
                            "Diagnosis: " + rx.getDiagnosis(),
                            CalendarEvent.TYPE_FOLLOW_UP,
                            rx.getFollowUpDate().trim(),
                            "10:00 AM",
                            rx.getCreatedAt(),
                            rx.getPatientUid(),
                            rx.getPatientName(),
                            rx.getDoctorUid(),
                            rx.getDoctorName(),
                            "Clinical Follow-up",
                            rx.getPrescriptionId()
                    );
                    map.put(eventId, ev);
                }
            }
        }

        // 5. Synthesize Lab Test Events
        for (LaboratoryRequest lab : sessionManager.getAllLabRequestsLocal()) {
            boolean matches = (role == UserRole.DOCTOR)
                    ? userUid.equals(lab.getDoctorUid())
                    : userUid.equals(lab.getPatientUid());
            if (matches) {
                String eventId = "evt_lab_" + lab.getRequestId();
                if (!map.containsKey(eventId)) {
                    CalendarEvent ev = new CalendarEvent(
                            eventId,
                            "Lab Test: " + lab.getTestName(),
                            "Status: " + lab.getStatus() + " (" + lab.getUrgency() + ")",
                            CalendarEvent.TYPE_LAB_TEST,
                            DateFormat.format("yyyy-MM-dd", new Date(lab.getCreatedAt())).toString(),
                            "09:00 AM",
                            lab.getCreatedAt(),
                            lab.getPatientUid(),
                            lab.getPatientName(),
                            lab.getDoctorUid(),
                            lab.getDoctorName(),
                            lab.getTestCategory(),
                            lab.getRequestId()
                    );
                    map.put(eventId, ev);
                }
            }
        }

        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent e : map.values()) {
            if (filterDate == null || filterDate.trim().isEmpty() || filterDate.equals(e.getEventDate())) {
                result.add(e);
            }
        }
        Collections.sort(result);
        return result;
    }

    // =========================================================================
    // 3. DIGITAL PRESCRIPTIONS
    // =========================================================================
    public void createPrescription(@NonNull DigitalPrescription rx,
                                   @NonNull AuthCallback<DigitalPrescription> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.DOCTOR) {
            callback.onError("Security Error: Only authorized Doctors can issue digital prescriptions.");
            return;
        }
        if (current.getVerificationStatusEnum() != VerificationStatus.VERIFIED) {
            callback.onError("Verification Gate: Your BMDC credentials must be VERIFIED before issuing digital prescriptions.");
            return;
        }

        if (rx.getDiagnosis() == null || rx.getDiagnosis().trim().isEmpty()) {
            callback.onError("Clinical diagnosis is required to issue a prescription.");
            return;
        }

        String rxId = "rx_" + UUID.randomUUID().toString().substring(0, 10);
        long now = System.currentTimeMillis();
        rx.setPrescriptionId(rxId);
        rx.setDoctorUid(current.getUid());
        rx.setDoctorName(current.getFullName());
        rx.setCreatedAt(now);
        rx.setUpdatedAt(now);

        sessionManager.savePrescriptionLocal(rx);

        // Schedule follow-up calendar event if set
        CalendarEvent followUpEvent = null;
        if (rx.getFollowUpDate() != null && !rx.getFollowUpDate().trim().isEmpty()) {
            followUpEvent = new CalendarEvent(
                    "evt_followup_" + rxId,
                    "Follow-up: " + rx.getPatientName(),
                    "Diagnosis: " + rx.getDiagnosis(),
                    CalendarEvent.TYPE_FOLLOW_UP,
                    rx.getFollowUpDate().trim(),
                    "10:00 AM",
                    now,
                    rx.getPatientUid(),
                    rx.getPatientName(),
                    current.getUid(),
                    current.getFullName(),
                    "OPD Clinic",
                    rxId
            );
            sessionManager.saveCalendarEventLocal(followUpEvent);
        }

        // Notification to Patient
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification patientNotif = new AppNotification(
                notifId,
                rx.getPatientUid(),
                current.getUid(),
                current.getFullName(),
                AppNotification.TYPE_PRESCRIPTION_ISSUED,
                "New Digital Prescription Issued",
                "Dr. " + current.getFullName() + " issued an authorized prescription. Tap to view and export PDF.",
                rxId
        );
        sessionManager.saveNotificationLocal(patientNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(rx);
            return;
        }

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.PRESCRIPTIONS).document(rxId), rx.toMap());
        if (followUpEvent != null) {
            batch.set(db.collection(FirestoreCollections.CALENDAR_EVENTS).document(followUpEvent.getEventId()),
                    followUpEvent.toMap());
        }
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), patientNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(rx))
                .addOnFailureListener(e -> callback.onSuccess(rx));
    }

    public void loadPatientPrescriptions(String patientUid, AuthCallback<List<DigitalPrescription>> callback) {
        if (patientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local prescriptions immediately
        callback.onSuccess(filterPrescriptionsByPatient(patientUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.PRESCRIPTIONS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DigitalPrescription> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        DigitalPrescription p = DigitalPrescription.fromMap(doc.getData());
                        if (p != null) remote.add(p);
                    }
                    callback.onSuccess(filterPrescriptionsByPatient(patientUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterPrescriptionsByPatient(patientUid, null)));
    }

    private List<DigitalPrescription> filterPrescriptionsByPatient(String patientUid, List<DigitalPrescription> remote) {
        Map<String, DigitalPrescription> map = new LinkedHashMap<>();
        for (DigitalPrescription local : sessionManager.getAllPrescriptionsLocal()) {
            if (patientUid.equals(local.getPatientUid()) && local.getPrescriptionId() != null) {
                map.put(local.getPrescriptionId(), local);
            }
        }
        if (remote != null) {
            for (DigitalPrescription r : remote) {
                if (r.getPrescriptionId() != null) {
                    map.put(r.getPrescriptionId(), r);
                    sessionManager.savePrescriptionLocal(r);
                }
            }
        }
        List<DigitalPrescription> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    public void loadDoctorPrescriptions(String doctorUid, AuthCallback<List<DigitalPrescription>> callback) {
        if (doctorUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local prescriptions immediately
        callback.onSuccess(filterPrescriptionsByDoctor(doctorUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.PRESCRIPTIONS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DigitalPrescription> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        DigitalPrescription p = DigitalPrescription.fromMap(doc.getData());
                        if (p != null) remote.add(p);
                    }
                    callback.onSuccess(filterPrescriptionsByDoctor(doctorUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterPrescriptionsByDoctor(doctorUid, null)));
    }

    private List<DigitalPrescription> filterPrescriptionsByDoctor(String doctorUid, List<DigitalPrescription> remote) {
        Map<String, DigitalPrescription> map = new LinkedHashMap<>();
        for (DigitalPrescription local : sessionManager.getAllPrescriptionsLocal()) {
            if (doctorUid.equals(local.getDoctorUid()) && local.getPrescriptionId() != null) {
                map.put(local.getPrescriptionId(), local);
            }
        }
        if (remote != null) {
            for (DigitalPrescription r : remote) {
                if (r.getPrescriptionId() != null) {
                    map.put(r.getPrescriptionId(), r);
                    sessionManager.savePrescriptionLocal(r);
                }
            }
        }
        List<DigitalPrescription> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    // =========================================================================
    // 4. LABORATORY MANAGEMENT & REPORT UPLOAD / REVIEW
    // =========================================================================
    public void createLaboratoryRequest(@NonNull LaboratoryRequest request,
                                        @NonNull AuthCallback<LaboratoryRequest> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.DOCTOR) {
            callback.onError("Security Error: Only authorized Doctors can order laboratory tests.");
            return;
        }
        if (current.getVerificationStatusEnum() != VerificationStatus.VERIFIED) {
            callback.onError("Verification Gate: Your BMDC credentials must be VERIFIED before ordering lab tests.");
            return;
        }

        if (request.getTestName() == null || request.getTestName().trim().isEmpty()) {
            callback.onError("Diagnostic test name is required.");
            return;
        }

        String reqId = "lab_" + UUID.randomUUID().toString().substring(0, 10);
        long now = System.currentTimeMillis();
        request.setRequestId(reqId);
        request.setDoctorUid(current.getUid());
        request.setDoctorName(current.getFullName());
        request.setStatus(LaboratoryRequest.STATUS_REQUESTED);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        sessionManager.saveLabRequestLocal(request);

        // Calendar Event
        CalendarEvent labEvent = new CalendarEvent(
                "evt_lab_" + reqId,
                "Diagnostic Test: " + request.getTestName(),
                "Category: " + request.getTestCategory() + " • Urgency: " + request.getUrgency(),
                CalendarEvent.TYPE_LAB_TEST,
                DateFormat.format("yyyy-MM-dd", new Date(now)).toString(),
                "09:00 AM",
                now,
                request.getPatientUid(),
                request.getPatientName(),
                current.getUid(),
                current.getFullName(),
                "Diagnostic Laboratory",
                reqId
        );
        sessionManager.saveCalendarEventLocal(labEvent);

        // Notification to Patient
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification patientNotif = new AppNotification(
                notifId,
                request.getPatientUid(),
                current.getUid(),
                current.getFullName(),
                AppNotification.TYPE_LAB_TEST_ORDERED,
                "Diagnostic Test Requested",
                "Dr. " + current.getFullName() + " requested " + request.getTestName() + " (" + request.getUrgency() + ").",
                reqId
        );
        sessionManager.saveNotificationLocal(patientNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(request);
            return;
        }

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.LABORATORY_REQUESTS).document(reqId), request.toMap());
        batch.set(db.collection(FirestoreCollections.CALENDAR_EVENTS).document(labEvent.getEventId()), labEvent.toMap());
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), patientNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(request))
                .addOnFailureListener(e -> callback.onSuccess(request));
    }

    public void loadPatientLabRequests(String patientUid, AuthCallback<List<LaboratoryRequest>> callback) {
        if (patientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local lab requests immediately
        callback.onSuccess(filterLabRequestsByPatient(patientUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.LABORATORY_REQUESTS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<LaboratoryRequest> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        LaboratoryRequest r = LaboratoryRequest.fromMap(doc.getData());
                        if (r != null) remote.add(r);
                    }
                    callback.onSuccess(filterLabRequestsByPatient(patientUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterLabRequestsByPatient(patientUid, null)));
    }

    private List<LaboratoryRequest> filterLabRequestsByPatient(String patientUid, List<LaboratoryRequest> remote) {
        Map<String, LaboratoryRequest> map = new LinkedHashMap<>();
        for (LaboratoryRequest local : sessionManager.getAllLabRequestsLocal()) {
            if (patientUid.equals(local.getPatientUid()) && local.getRequestId() != null) {
                map.put(local.getRequestId(), local);
            }
        }
        if (remote != null) {
            for (LaboratoryRequest r : remote) {
                if (r.getRequestId() != null) {
                    map.put(r.getRequestId(), r);
                    sessionManager.saveLabRequestLocal(r);
                }
            }
        }
        List<LaboratoryRequest> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    public void loadDoctorLabRequests(String doctorUid, AuthCallback<List<LaboratoryRequest>> callback) {
        if (doctorUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // INSTANT RENDER (0 ms): Serve cached local lab requests immediately
        callback.onSuccess(filterLabRequestsByDoctor(doctorUid, null));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.LABORATORY_REQUESTS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<LaboratoryRequest> remote = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        LaboratoryRequest r = LaboratoryRequest.fromMap(doc.getData());
                        if (r != null) remote.add(r);
                    }
                    callback.onSuccess(filterLabRequestsByDoctor(doctorUid, remote));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterLabRequestsByDoctor(doctorUid, null)));
    }

    private List<LaboratoryRequest> filterLabRequestsByDoctor(String doctorUid, List<LaboratoryRequest> remote) {
        Map<String, LaboratoryRequest> map = new LinkedHashMap<>();
        for (LaboratoryRequest local : sessionManager.getAllLabRequestsLocal()) {
            if (doctorUid.equals(local.getDoctorUid()) && local.getRequestId() != null) {
                map.put(local.getRequestId(), local);
            }
        }
        if (remote != null) {
            for (LaboratoryRequest r : remote) {
                if (r.getRequestId() != null) {
                    map.put(r.getRequestId(), r);
                    sessionManager.saveLabRequestLocal(r);
                }
            }
        }
        List<LaboratoryRequest> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    public void uploadLaboratoryReport(@NonNull LaboratoryRequest labRequest,
                                       byte[] fileBytes,
                                       String fileName,
                                       String fileType,
                                       String findingsSummary,
                                       AuthCallback<LaboratoryRequest> callback) {
        long now = System.currentTimeMillis();
        labRequest.setStatus(LaboratoryRequest.STATUS_REPORT_READY);
        labRequest.setReportFileName(fileName);
        labRequest.setReportFileType(fileType != null ? fileType : "PDF");
        labRequest.setReportFindingsSummary(findingsSummary != null ? findingsSummary.trim() : "Findings uploaded.");
        labRequest.setReportUploadedAt(now);
        labRequest.setUpdatedAt(now);

        // Save file locally as resilient fallback
        try {
            File labDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (labDir == null) labDir = appContext.getFilesDir();
            File localReportFile = new File(labDir, "LabReport_" + labRequest.getRequestId() + "_" + fileName);
            if (fileBytes != null && fileBytes.length > 0) {
                FileOutputStream fos = new FileOutputStream(localReportFile);
                fos.write(fileBytes);
                fos.flush();
                fos.close();
                labRequest.setReportFileUrl(localReportFile.getAbsolutePath());
            } else {
                labRequest.setReportFileUrl(localReportFile.getAbsolutePath());
            }
        } catch (Exception ignored) {
        }

        sessionManager.saveLabRequestLocal(labRequest);

        // Privacy-safe notification to Doctor
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification docNotif = new AppNotification(
                notifId,
                labRequest.getDoctorUid(),
                labRequest.getPatientUid(),
                labRequest.getPatientName(),
                AppNotification.TYPE_LAB_REPORT_READY,
                "Diagnostic Report Ready",
                "Laboratory findings for " + labRequest.getTestName() + " (" + labRequest.getPatientName() + ") are now uploaded and awaiting your review.",
                labRequest.getRequestId()
        );
        sessionManager.saveNotificationLocal(docNotif);

        FirebaseStorage storage = FirebaseProvider.getStorage(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        if (storage != null && fileBytes != null && fileBytes.length > 0) {
            String path = "laboratory_reports/" + labRequest.getPatientUid() + "/" + labRequest.getRequestId() + "_" + fileName;
            StorageReference ref = storage.getReference().child(path);
            ref.putBytes(fileBytes)
                    .addOnSuccessListener(taskSnap -> ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                labRequest.setReportFileUrl(uri.toString());
                                sessionManager.saveLabRequestLocal(labRequest);
                                if (db != null) {
                                    WriteBatch batch = db.batch();
                                    batch.set(db.collection(FirestoreCollections.LABORATORY_REQUESTS).document(labRequest.getRequestId()),
                                            labRequest.toMap(), SetOptions.merge());
                                    batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), docNotif.toMap());
                                    batch.commit().addOnCompleteListener(t -> callback.onSuccess(labRequest));
                                } else {
                                    callback.onSuccess(labRequest);
                                }
                            })
                            .addOnFailureListener(e -> callback.onSuccess(labRequest)))
                    .addOnFailureListener(e -> callback.onSuccess(labRequest));
        } else {
            if (db != null) {
                WriteBatch batch = db.batch();
                batch.set(db.collection(FirestoreCollections.LABORATORY_REQUESTS).document(labRequest.getRequestId()),
                        labRequest.toMap(), SetOptions.merge());
                batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), docNotif.toMap());
                batch.commit().addOnCompleteListener(t -> callback.onSuccess(labRequest));
            } else {
                callback.onSuccess(labRequest);
            }
        }
    }

    public void reviewLaboratoryReport(@NonNull LaboratoryRequest labRequest,
                                       @NonNull String doctorReviewNotes,
                                       AuthCallback<Void> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.DOCTOR) {
            callback.onError("Security Error: Only Doctors can review diagnostic laboratory reports.");
            return;
        }

        long now = System.currentTimeMillis();
        labRequest.setStatus(LaboratoryRequest.STATUS_REVIEWED);
        labRequest.setDoctorReviewNotes(doctorReviewNotes.trim());
        labRequest.setDoctorReviewedAt(now);
        labRequest.setUpdatedAt(now);
        sessionManager.saveLabRequestLocal(labRequest);

        // Notification to Patient
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification patientNotif = new AppNotification(
                notifId,
                labRequest.getPatientUid(),
                current.getUid(),
                current.getFullName(),
                AppNotification.TYPE_LAB_REPORT_REVIEWED,
                "Laboratory Report Reviewed",
                "Dr. " + current.getFullName() + " has reviewed your " + labRequest.getTestName() + " report and added clinical review findings.",
                labRequest.getRequestId()
        );
        sessionManager.saveNotificationLocal(patientNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.LABORATORY_REQUESTS).document(labRequest.getRequestId()),
                labRequest.toMap(), SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), patientNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    // =========================================================================
    // 5. UNIFIED PATIENT MEDICAL RECORD & CHRONOLOGICAL HISTORY
    // =========================================================================
    public void loadPatientMedicalRecord(String patientUid, AuthCallback<List<MedicalRecordEntry>> callback) {
        if (patientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<MedicalRecordEntry> entries = new ArrayList<>();

        // 1. Add Prescriptions
        for (DigitalPrescription rx : sessionManager.getAllPrescriptionsLocal()) {
            if (patientUid.equals(rx.getPatientUid())) {
                StringBuilder medsSummary = new StringBuilder();
                if (rx.getMedicines() != null) {
                    for (PrescriptionMedicine m : rx.getMedicines()) {
                        if (medsSummary.length() > 0) medsSummary.append(", ");
                        medsSummary.append(m.getMedicineName());
                    }
                }
                MedicalRecordEntry entry = new MedicalRecordEntry(
                        rx.getPrescriptionId(),
                        rx.getPatientUid(),
                        rx.getPatientId(),
                        MedicalRecordEntry.CATEGORY_PRESCRIPTION,
                        "Prescription: " + rx.getDiagnosis(),
                        "Prescribed by Dr. " + rx.getDoctorName() + " (" + rx.getDoctorSpecialty() + ")",
                        "Medications: " + (medsSummary.length() > 0 ? medsSummary.toString() : "Prescribed medications")
                                + (rx.getFollowUpDate() != null ? " • Follow-up: " + rx.getFollowUpDate() : ""),
                        DateFormat.format("yyyy-MM-dd", new Date(rx.getCreatedAt())).toString(),
                        rx.getCreatedAt(),
                        rx.getDoctorName(),
                        "DOCTOR",
                        "Active Prescription",
                        rx.getPrescriptionId()
                );
                entry.setRawObject(rx);
                entries.add(entry);
            }
        }

        // 2. Add Laboratory Requests & Reports
        for (LaboratoryRequest lab : sessionManager.getAllLabRequestsLocal()) {
            if (patientUid.equals(lab.getPatientUid())) {
                String statusLabel = lab.isReviewed() ? "Reviewed by Doctor"
                        : (lab.isReportReady() ? "Report Ready" : "Pending Diagnostic");
                String summary = "Status: " + lab.getStatus() + " (" + lab.getUrgency() + ")";
                if (lab.getReportFindingsSummary() != null && !lab.getReportFindingsSummary().isEmpty()) {
                    summary += " • Findings: " + lab.getReportFindingsSummary();
                }
                if (lab.getDoctorReviewNotes() != null && !lab.getDoctorReviewNotes().isEmpty()) {
                    summary += " • Doctor Note: " + lab.getDoctorReviewNotes();
                }

                MedicalRecordEntry entry = new MedicalRecordEntry(
                        lab.getRequestId(),
                        lab.getPatientUid(),
                        lab.getPatientId(),
                        MedicalRecordEntry.CATEGORY_LAB_REPORT,
                        "Lab Test: " + lab.getTestName(),
                        "Ordered by Dr. " + lab.getDoctorName() + " • " + lab.getTestCategory(),
                        summary,
                        DateFormat.format("yyyy-MM-dd", new Date(lab.getCreatedAt())).toString(),
                        lab.getCreatedAt(),
                        lab.getDoctorName(),
                        "LABORATORY",
                        statusLabel,
                        lab.getRequestId()
                );
                entry.setRawObject(lab);
                entries.add(entry);
            }
        }

        // 3. Add Completed or Confirmed Appointments
        for (Appointment a : sessionManager.getAllAppointmentsLocal()) {
            if (patientUid.equals(a.getPatientUid())) {
                MedicalRecordEntry entry = new MedicalRecordEntry(
                        a.getAppointmentId(),
                        a.getPatientUid(),
                        a.getPatientId(),
                        MedicalRecordEntry.CATEGORY_APPOINTMENT,
                        "Consultation: " + a.getDoctorName(),
                        a.getConsultationType() + " • " + a.getDoctorSpecialty(),
                        "Date: " + a.getAppointmentDate() + " at " + a.getAppointmentTime() + " • Reason: " + a.getReason(),
                        a.getAppointmentDate(),
                        a.getCreatedAt(),
                        a.getDoctorName(),
                        "DOCTOR",
                        a.getStatus(),
                        a.getAppointmentId()
                );
                entry.setRawObject(a);
                entries.add(entry);
            }
        }

        // Sort in chronological order (most recent first)
        Collections.sort(entries);
        callback.onSuccess(entries);
    }
}
