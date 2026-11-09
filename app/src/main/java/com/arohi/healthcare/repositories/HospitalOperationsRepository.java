package com.arohi.healthcare.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.arohi.healthcare.models.Bed;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.DutySchedule;
import com.arohi.healthcare.models.HospitalDepartment;
import com.arohi.healthcare.models.HospitalService;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.NursingTask;
import com.arohi.healthcare.models.OTRoom;
import com.arohi.healthcare.models.Operation;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VitalObservation;
import com.arohi.healthcare.models.Ward;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Phase 6 Central Repository for Hospital Operations, Operation Theatres (OT),
 * Wards & Beds, Nurse Care Execution, and Synchronized Calendars.
 */
public class HospitalOperationsRepository {

    private static final String PREF_OPS = "arohi_hospital_operations_prefs";
    private static final String KEY_OT_ROOMS_JSON = "key_ot_rooms_json";
    private static final String KEY_OPERATIONS_JSON = "key_operations_json";
    private static final String KEY_WARDS_JSON = "key_wards_json";
    private static final String KEY_BEDS_JSON = "key_beds_json";
    private static final String KEY_NURSE_TASKS_JSON = "key_nurse_tasks_json";
    private static final String KEY_VITALS_JSON = "key_vitals_json";
    private static final String KEY_DUTY_SCHEDULES_JSON = "key_duty_schedules_json";
    private static final String KEY_SERVICES_JSON = "key_services_json";
    private static final String KEY_DEPTS_JSON = "key_depts_json";

    private final Context context;
    private final SharedPreferences prefs;
    private final SessionManager sessionManager;
    private final FirebaseFirestore firestore;

    public HospitalOperationsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_OPS, Context.MODE_PRIVATE);
        this.sessionManager = new SessionManager(context);
        this.firestore = FirebaseFirestore.getInstance();
        ensureSeedData();
    }

    // =========================================================================
    // 1. OPERATION THEATRE (OT) MANAGEMENT & CONFLICT CHECK
    // =========================================================================

    public void loadOTRooms(AuthCallback<List<OTRoom>> callback) {
        firestore.collection("otRooms").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<OTRoom> list = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            OTRoom room = doc.toObject(OTRoom.class);
                            if (room != null) {
                                room.setRoomId(doc.getId());
                                list.add(room);
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = getLocalOTRooms();
                    } else {
                        saveLocalOTRooms(list);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onSuccess(getLocalOTRooms()));
    }

    public void updateOTRoomStatus(@NonNull String roomId, @NonNull String status, AuthCallback<Void> callback) {
        List<OTRoom> rooms = getLocalOTRooms();
        for (OTRoom r : rooms) {
            if (r.getRoomId().equals(roomId)) {
                r.setStatus(status);
                break;
            }
        }
        saveLocalOTRooms(rooms);

        firestore.collection("otRooms").document(roomId)
                .update("status", status, "available", OTRoom.STATUS_AVAILABLE.equalsIgnoreCase(status))
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onSuccess(null);
                });
    }

    public void loadOperations(AuthCallback<List<Operation>> callback) {
        firestore.collection("operations").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Operation> list = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Operation op = doc.toObject(Operation.class);
                            if (op != null) {
                                op.setOperationId(doc.getId());
                                list.add(op);
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = getLocalOperations();
                    } else {
                        saveLocalOperations(list);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onSuccess(getLocalOperations()));
    }

    /**
     * Schedules an authorized operation with full 3-way conflict detection:
     * - OT Room conflict
     * - Doctor schedule conflict
     * - Nurse schedule conflict
     */
    public void scheduleOperation(@NonNull Operation op, AuthCallback<Operation> callback) {
        loadOperations(new AuthCallback<List<Operation>>() {
            @Override
            public void onSuccess(List<Operation> existingOps) {
                // 1. Conflict Check: OT Room
                for (Operation other : existingOps) {
                    if (Operation.STATUS_CANCELLED.equalsIgnoreCase(other.getStatus())) continue;
                    if (other.getOperationDate() != null && other.getOperationDate().equals(op.getOperationDate())) {
                        if (other.getOtRoomId() != null && other.getOtRoomId().equals(op.getOtRoomId())) {
                            if (isTimeConflict(other.getOperationTime(), op.getOperationTime())) {
                                callback.onError("OT Room Conflict: " + op.getOtRoomName()
                                        + " is already booked for " + other.getProcedureName()
                                        + " on " + op.getOperationDate() + " at " + other.getOperationTime());
                                return;
                            }
                        }
                    }
                }

                // 2. Conflict Check: Doctor
                for (Operation other : existingOps) {
                    if (Operation.STATUS_CANCELLED.equalsIgnoreCase(other.getStatus())) continue;
                    if (other.getOperationDate() != null && other.getOperationDate().equals(op.getOperationDate())) {
                        if (other.getDoctorUid() != null && other.getDoctorUid().equals(op.getDoctorUid())) {
                            if (isTimeConflict(other.getOperationTime(), op.getOperationTime())) {
                                callback.onError("Doctor Conflict: Dr. " + op.getDoctorName()
                                        + " is already performing " + other.getProcedureName()
                                        + " at " + other.getOperationTime());
                                return;
                            }
                        }
                    }
                }

                // 3. Conflict Check: Nurse
                if (op.getNurseUid() != null && !op.getNurseUid().isEmpty()) {
                    for (Operation other : existingOps) {
                        if (Operation.STATUS_CANCELLED.equalsIgnoreCase(other.getStatus())) continue;
                        if (other.getOperationDate() != null && other.getOperationDate().equals(op.getOperationDate())) {
                            if (op.getNurseUid().equals(other.getNurseUid())) {
                                if (isTimeConflict(other.getOperationTime(), op.getOperationTime())) {
                                    callback.onError("Nurse Conflict: Assigned Nurse " + op.getNurseName()
                                            + " is assisting another surgery at " + other.getOperationTime());
                                    return;
                                }
                            }
                        }
                    }
                }

                // Generate ID if empty
                if (op.getOperationId() == null || op.getOperationId().isEmpty()) {
                    op.setOperationId("op_" + System.currentTimeMillis());
                }

                // Save locally
                existingOps.add(0, op);
                saveLocalOperations(existingOps);

                // Synchronize Calendar Events for Patient, Doctor, Nurse & Admin
                syncOperationToCalendar(op);

                // Phase 7: Advance Patient Journey Milestone (Operation)
                new EcosystemIntegrationRepository(context).advanceJourneyMilestone(
                        op.getPatientUid(),
                        com.arohi.healthcare.models.PatientJourney.STAGE_OPERATION,
                        com.arohi.healthcare.models.PatientJourney.STATUS_IN_PROGRESS,
                        op.getDoctorName(),
                        "SURGEON",
                        "Scheduled: " + op.getProcedureName() + " in " + op.getOtRoomName(),
                        op.getOperationId(),
                        null
                );

                // Sync to Firestore
                firestore.collection("operations").document(op.getOperationId())
                        .set(op.toMap())
                        .addOnSuccessListener(v -> callback.onSuccess(op))
                        .addOnFailureListener(e -> callback.onSuccess(op));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void updateOperationStatus(@NonNull String operationId, @NonNull String newStatus,
                                      String postOpNotes, String followUpDate,
                                      AuthCallback<Void> callback) {
        List<Operation> list = getLocalOperations();
        Operation matched = null;
        for (Operation o : list) {
            if (o.getOperationId().equals(operationId)) {
                o.setStatus(newStatus);
                if (postOpNotes != null && !postOpNotes.isEmpty()) {
                    o.setPostOpNotes(postOpNotes);
                }
                if (followUpDate != null && !followUpDate.isEmpty()) {
                    o.setFollowUpDate(followUpDate);
                }
                o.setUpdatedAt(System.currentTimeMillis());
                matched = o;
                break;
            }
        }
        saveLocalOperations(list);

        if (matched != null) {
            syncOperationToCalendar(matched);

            // Phase 7: Automatically update Journey Milestones on operation status changes
            EcosystemIntegrationRepository ecoRepo = new EcosystemIntegrationRepository(context);
            if (Operation.STATUS_COMPLETED.equalsIgnoreCase(newStatus)) {
                ecoRepo.advanceJourneyMilestone(
                        matched.getPatientUid(),
                        com.arohi.healthcare.models.PatientJourney.STAGE_OPERATION,
                        com.arohi.healthcare.models.PatientJourney.STATUS_COMPLETED,
                        matched.getDoctorName(),
                        "SURGEON",
                        "Completed: " + matched.getProcedureName() + (matched.getPostOpNotes() != null ? " • " + matched.getPostOpNotes() : ""),
                        matched.getOperationId(),
                        null
                );
                ecoRepo.advanceJourneyMilestone(
                        matched.getPatientUid(),
                        com.arohi.healthcare.models.PatientJourney.STAGE_RECOVERY,
                        com.arohi.healthcare.models.PatientJourney.STATUS_IN_PROGRESS,
                        matched.getDoctorName(),
                        "SURGEON",
                        "Post-operative recovery & ward care initiated",
                        matched.getOperationId(),
                        null
                );
            }
            if (matched.getFollowUpDate() != null && !matched.getFollowUpDate().isEmpty()) {
                ecoRepo.advanceJourneyMilestone(
                        matched.getPatientUid(),
                        com.arohi.healthcare.models.PatientJourney.STAGE_FOLLOW_UP,
                        com.arohi.healthcare.models.PatientJourney.STATUS_IN_PROGRESS,
                        matched.getDoctorName(),
                        "SURGEON",
                        "Post-op follow-up scheduled for: " + matched.getFollowUpDate(),
                        matched.getOperationId(),
                        null
                );
            }
        }

        firestore.collection("operations").document(operationId)
                .update("status", newStatus,
                        "postOpNotes", postOpNotes != null ? postOpNotes : "",
                        "followUpDate", followUpDate != null ? followUpDate : "",
                        "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onSuccess(null);
                });
    }

    private boolean isTimeConflict(String time1, String time2) {
        if (time1 == null || time2 == null) return false;
        String t1 = time1.replaceAll("\\s+", "").toLowerCase();
        String t2 = time2.replaceAll("\\s+", "").toLowerCase();
        return t1.equals(t2);
    }

    private void syncOperationToCalendar(Operation op) {
        CalendarEvent event = new CalendarEvent();
        event.setEventId("cal_op_" + op.getOperationId());
        event.setTitle("Surgery: " + op.getProcedureName() + " (" + op.getOtRoomName() + ")");
        event.setDescription("Surgeon: Dr. " + op.getDoctorName() + " • Nurse: " + op.getNurseName()
                + " • Status: " + op.getStatus());
        event.setEventType(CalendarEvent.TYPE_OPERATION);
        event.setEventDate(op.getOperationDate());
        event.setEventTime(op.getOperationTime());
        event.setDepartmentOrLocation(op.getOtRoomName());
        event.setPatientUid(op.getPatientUid());
        event.setPatientName(op.getPatientName());
        event.setPatientId(op.getPatientId());
        event.setDoctorUid(op.getDoctorUid());
        event.setDoctorName(op.getDoctorName());
        event.setNurseUid(op.getNurseUid());
        event.setNurseName(op.getNurseName());
        event.setReferenceId(op.getOperationId());
        event.setStatus(op.getStatus());
        event.setRoleVisibility("ALL");

        sessionManager.saveCalendarEventLocal(event);
        firestore.collection("calendarEvents").document(event.getEventId())
                .set(event.toMap())
                .addOnFailureListener(e -> {});
    }

    // =========================================================================
    // 2. WARD AND BED MANAGEMENT
    // =========================================================================

    public void loadWards(AuthCallback<List<Ward>> callback) {
        firestore.collection("wards").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Ward> list = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Ward w = doc.toObject(Ward.class);
                            if (w != null) {
                                w.setWardId(doc.getId());
                                list.add(w);
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = getLocalWards();
                    } else {
                        saveLocalWards(list);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onSuccess(getLocalWards()));
    }

    public void loadBeds(AuthCallback<List<Bed>> callback) {
        firestore.collection("beds").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Bed> list = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Bed b = doc.toObject(Bed.class);
                            if (b != null) {
                                b.setBedId(doc.getId());
                                list.add(b);
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = getLocalBeds();
                    } else {
                        saveLocalBeds(list);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onSuccess(getLocalBeds()));
    }

    public void assignPatientToBed(@NonNull String bedId,
                                   @NonNull String patientUid, @NonNull String patientId, @NonNull String patientName,
                                   @NonNull String doctorUid, @NonNull String doctorName,
                                   @NonNull String nurseUid, @NonNull String nurseName,
                                   String clinicalNotes,
                                   AuthCallback<Bed> callback) {
        List<Bed> beds = getLocalBeds();
        Bed target = null;
        for (Bed b : beds) {
            if (b.getBedId().equals(bedId)) {
                target = b;
                break;
            }
        }

        if (target == null) {
            callback.onError("Selected bed was not found in registry.");
            return;
        }

        // Conflict check: prevent assigning occupied or unavailable beds
        if (!target.isAvailableForAdmission()) {
            callback.onError("Cannot assign patient: " + target.getBedNumber() + " is currently " + target.getStatus());
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        target.setStatus(Bed.STATUS_OCCUPIED);
        target.setPatientUid(patientUid);
        target.setPatientId(patientId);
        target.setPatientName(patientName);
        target.setAssignedDoctorUid(doctorUid);
        target.setAssignedDoctorName(doctorName);
        target.setAssignedNurseUid(nurseUid);
        target.setAssignedNurseName(nurseName);
        target.setAdmissionDate(today);
        target.setClinicalNotes(clinicalNotes != null ? clinicalNotes : "Admitted for Inpatient Care");
        target.setUpdatedAt(System.currentTimeMillis());

        saveLocalBeds(beds);
        updateWardBedCounts(target.getWardId());

        // Create default nursing care tasks for admitted patient
        createAdmissionNursingTasks(target);

        // Phase 7: Advance Patient Journey Milestone (Recovery)
        new EcosystemIntegrationRepository(context).advanceJourneyMilestone(
                patientUid,
                com.arohi.healthcare.models.PatientJourney.STAGE_RECOVERY,
                com.arohi.healthcare.models.PatientJourney.STATUS_IN_PROGRESS,
                doctorName,
                "DOCTOR",
                "Admitted to " + target.getWardName() + " (Bed " + target.getBedNumber() + ") • Nurse: " + nurseName,
                target.getBedId(),
                null
        );

        final Bed assignedBed = target;
        firestore.collection("beds").document(assignedBed.getBedId())
                .set(assignedBed.toMap())
                .addOnSuccessListener(v -> callback.onSuccess(assignedBed))
                .addOnFailureListener(e -> callback.onSuccess(assignedBed));
    }

    public void dischargePatientFromBed(@NonNull String bedId, AuthCallback<Void> callback) {
        List<Bed> beds = getLocalBeds();
        Bed target = null;
        for (Bed b : beds) {
            if (b.getBedId().equals(bedId)) {
                target = b;
                break;
            }
        }

        if (target == null) {
            callback.onError("Bed not found.");
            return;
        }

        String wardId = target.getWardId();
        target.setStatus(Bed.STATUS_CLEANING);
        target.setPatientUid("");
        target.setPatientId("");
        target.setPatientName("");
        target.setAssignedDoctorUid("");
        target.setAssignedDoctorName("");
        target.setAssignedNurseUid("");
        target.setAssignedNurseName("");
        target.setAdmissionDate("");
        target.setClinicalNotes("Discharged. Sanitization in progress.");
        target.setUpdatedAt(System.currentTimeMillis());

        saveLocalBeds(beds);
        updateWardBedCounts(wardId);

        firestore.collection("beds").document(target.getBedId())
                .set(target.toMap())
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    public void updateBedStatus(@NonNull String bedId, @NonNull String newStatus, AuthCallback<Void> callback) {
        List<Bed> beds = getLocalBeds();
        Bed target = null;
        for (Bed b : beds) {
            if (b.getBedId().equals(bedId)) {
                b.setStatus(newStatus);
                b.setUpdatedAt(System.currentTimeMillis());
                target = b;
                break;
            }
        }
        saveLocalBeds(beds);
        if (target != null) {
            updateWardBedCounts(target.getWardId());
            firestore.collection("beds").document(bedId)
                    .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                    .addOnFailureListener(e -> { if (callback != null) callback.onSuccess(null); });
        } else if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private void updateWardBedCounts(String wardId) {
        if (wardId == null) return;
        List<Bed> beds = getLocalBeds();
        int total = 0;
        int occupied = 0;
        for (Bed b : beds) {
            if (wardId.equals(b.getWardId())) {
                total++;
                if (Bed.STATUS_OCCUPIED.equalsIgnoreCase(b.getStatus())) {
                    occupied++;
                }
            }
        }
        List<Ward> wards = getLocalWards();
        for (Ward w : wards) {
            if (wardId.equals(w.getWardId())) {
                w.setTotalBeds(Math.max(w.getTotalBeds(), total));
                w.setOccupiedBeds(occupied);
                if (occupied >= w.getTotalBeds()) {
                    w.setStatus(Ward.STATUS_FULL);
                } else {
                    w.setStatus(Ward.STATUS_ACTIVE);
                }
                break;
            }
        }
        saveLocalWards(wards);
    }

    // =========================================================================
    // 3. NURSE CARE & TASK MANAGEMENT (WITH AUTHORIZED PATIENT RESTRICTION)
    // =========================================================================

    public void loadTasksForNurse(String nurseUid, AuthCallback<List<NursingTask>> callback) {
        firestore.collection("nursingTasks").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<NursingTask> list = new ArrayList<>();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            NursingTask t = doc.toObject(NursingTask.class);
                            if (t != null) {
                                t.setTaskId(doc.getId());
                                if (nurseUid == null || nurseUid.isEmpty()
                                        || nurseUid.equals(t.getNurseUid())
                                        || "ALL".equalsIgnoreCase(t.getNurseUid())) {
                                    list.add(t);
                                }
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        list = getLocalNurseTasks(nurseUid);
                    } else {
                        saveLocalNurseTasks(list);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onSuccess(getLocalNurseTasks(nurseUid)));
    }

    public void completeNursingTask(@NonNull String taskId, String nurseNotes, AuthCallback<Void> callback) {
        List<NursingTask> tasks = getLocalNurseTasks(null);
        for (NursingTask t : tasks) {
            if (t.getTaskId().equals(taskId)) {
                t.setStatus(NursingTask.STATUS_COMPLETED);
                t.setCompletedAt(System.currentTimeMillis());
                t.setNurseCompletionNotes(nurseNotes != null ? nurseNotes : "Administered as prescribed.");
                break;
            }
        }
        saveLocalNurseTasks(tasks);

        firestore.collection("nursingTasks").document(taskId)
                .update("status", NursingTask.STATUS_COMPLETED,
                        "completedAt", System.currentTimeMillis(),
                        "nurseCompletionNotes", nurseNotes != null ? nurseNotes : "Completed.")
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    public void recordPatientVitals(@NonNull VitalObservation vital, AuthCallback<VitalObservation> callback) {
        if (vital.getVitalId() == null || vital.getVitalId().isEmpty()) {
            vital.setVitalId("vit_" + System.currentTimeMillis());
        }
        List<VitalObservation> list = getLocalVitals();
        list.add(0, vital);
        saveLocalVitals(list);

        // Also create a completed nursing task for audit
        NursingTask vitTask = new NursingTask(
                "task_vit_" + System.currentTimeMillis(),
                NursingTask.TYPE_VITALS,
                "Bedside Vitals Recorded: " + vital.getBloodPressure() + " • HR " + vital.getHeartRateBpm() + " bpm",
                "Temp: " + vital.getTemperatureF() + "°F, SpO2: " + vital.getSpO2Percent() + "%, RR: " + vital.getRespiratoryRate() + "/min",
                "Normal",
                vital.getPatientUid(), vital.getPatientId(), vital.getPatientName(),
                vital.getWardName(), vital.getBedNumber(),
                vital.getNurseUid(), vital.getNurseName(),
                vital.getRecordedDate(), vital.getRecordedTime(),
                "Attending Physician", "Regular telemetry & vitals monitoring"
        );
        vitTask.setStatus(NursingTask.STATUS_COMPLETED);
        vitTask.setCompletedAt(System.currentTimeMillis());
        vitTask.setNurseCompletionNotes(vital.getClinicalNotes());
        List<NursingTask> tasks = getLocalNurseTasks(null);
        tasks.add(0, vitTask);
        saveLocalNurseTasks(tasks);

        // Phase 7: Advance Patient Journey Milestone (Recovery)
        new EcosystemIntegrationRepository(context).advanceJourneyMilestone(
                vital.getPatientUid(),
                com.arohi.healthcare.models.PatientJourney.STAGE_RECOVERY,
                com.arohi.healthcare.models.PatientJourney.STATUS_IN_PROGRESS,
                vital.getNurseName(),
                "NURSE",
                "Vitals: BP " + vital.getBloodPressure() + " • SpO2 " + vital.getSpO2Percent() + "% • HR " + vital.getHeartRateBpm() + " bpm",
                vital.getVitalId(),
                null
        );

        firestore.collection("vitals").document(vital.getVitalId())
                .set(vital.toMap())
                .addOnSuccessListener(v -> callback.onSuccess(vital))
                .addOnFailureListener(e -> callback.onSuccess(vital));
    }

    public void loadVitalsForPatient(String patientUid, AuthCallback<List<VitalObservation>> callback) {
        List<VitalObservation> filtered = new ArrayList<>();
        for (VitalObservation v : getLocalVitals()) {
            if (patientUid == null || patientUid.isEmpty() || patientUid.equals(v.getPatientUid())) {
                filtered.add(v);
            }
        }
        callback.onSuccess(filtered);
    }

    /**
     * Enforces Nurse Security Requirement:
     * "Nurses must only access authorized patient information."
     * Returns only patients admitted to the nurse's assigned ward or assigned beds.
     */
    public void loadAuthorizedPatientsForNurse(String nurseUid, String nurseWardName,
                                              AuthCallback<List<Bed>> callback) {
        loadBeds(new AuthCallback<List<Bed>>() {
            @Override
            public void onSuccess(List<Bed> allBeds) {
                List<Bed> authorizedBeds = new ArrayList<>();
                for (Bed b : allBeds) {
                    if (Bed.STATUS_OCCUPIED.equalsIgnoreCase(b.getStatus())) {
                        boolean isAssignedNurse = (nurseUid != null && nurseUid.equals(b.getAssignedNurseUid()));
                        boolean isInAssignedWard = (nurseWardName != null && !nurseWardName.isEmpty()
                                && nurseWardName.equalsIgnoreCase(b.getWardName()));
                        // Authorized if explicitly assigned nurse OR assigned to this ward
                        if (isAssignedNurse || isInAssignedWard || nurseUid == null) {
                            authorizedBeds.add(b);
                        }
                    }
                }
                callback.onSuccess(authorizedBeds);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // =========================================================================
    // 4. DUTY SCHEDULES & SHIFT ROSTER
    // =========================================================================

    public void loadDutySchedulesForNurse(String nurseUid, AuthCallback<List<DutySchedule>> callback) {
        List<DutySchedule> list = getLocalDutySchedules();
        List<DutySchedule> filtered = new ArrayList<>();
        for (DutySchedule d : list) {
            if (nurseUid == null || nurseUid.isEmpty() || nurseUid.equals(d.getNurseUid())) {
                filtered.add(d);
            }
        }
        callback.onSuccess(filtered);
    }

    // =========================================================================
    // 5. HOSPITAL SERVICES & DEPARTMENTS
    // =========================================================================

    public void loadHospitalServices(AuthCallback<List<HospitalService>> callback) {
        callback.onSuccess(getLocalServices());
    }

    public void toggleServiceStatus(@NonNull String serviceId, boolean active, AuthCallback<Void> callback) {
        List<HospitalService> list = getLocalServices();
        for (HospitalService s : list) {
            if (s.getServiceId().equals(serviceId)) {
                s.setActive(active);
                break;
            }
        }
        saveLocalServices(list);
        if (callback != null) callback.onSuccess(null);
    }

    public void loadDepartments(AuthCallback<List<HospitalDepartment>> callback) {
        callback.onSuccess(getLocalDepartments());
    }

    // =========================================================================
    // LOCAL SEED & STORAGE IMPLEMENTATION
    // =========================================================================

    private void ensureSeedData() {
        if (!prefs.contains(KEY_OT_ROOMS_JSON)) {
            List<OTRoom> otList = new ArrayList<>();
            otList.add(new OTRoom("ot_01", "OT Suite 1 - Cardiac Surgery", "Floor 3, South Wing", "Cardiothoracic, Vascular", OTRoom.STATUS_AVAILABLE, "Laminar flow, Heart-lung bypass console, St. Jude Telemetry"));
            otList.add(new OTRoom("ot_02", "OT Suite 2 - General & Laparoscopic", "Floor 3, South Wing", "General Surgery, Gastro, Urology", OTRoom.STATUS_AVAILABLE, "Karl Storz 4K Laparoscopy Tower, Harmonic scalpel"));
            otList.add(new OTRoom("ot_03", "OT Suite 3 - Orthopedic & Trauma", "Floor 3, South Wing", "Orthopedic, Joint Replacement, Spine", OTRoom.STATUS_AVAILABLE, "C-Arm Fluoroscopy, Stryker Ortho Power Drills"));
            otList.add(new OTRoom("ot_04", "OT Suite 4 - Neuro & Micro-Surgery", "Floor 3, South Wing", "Neurosurgery, Plastic Surgery", OTRoom.STATUS_AVAILABLE, "Zeiss Surgical Microscope, Medtronic StealthStation"));
            saveLocalOTRooms(otList);
        }

        if (!prefs.contains(KEY_WARDS_JSON)) {
            List<Ward> wardList = new ArrayList<>();
            wardList.add(new Ward("ward_a", "Ward A - Male Surgical", "General Surgery", "Floor 2, East Wing", 12, 4, "nurse_demo_01", "Sister Fatima (RN)", Ward.STATUS_ACTIVE));
            wardList.add(new Ward("ward_b", "Ward B - Female Medical", "Internal Medicine", "Floor 2, East Wing", 12, 3, "nurse_demo_02", "Sister Nasreen (RN)", Ward.STATUS_ACTIVE));
            wardList.add(new Ward("ward_icu", "ICU / CCU Critical Care", "Critical Care", "Floor 3, North Wing", 8, 3, "nurse_demo_01", "Sister Fatima (RN)", Ward.STATUS_ACTIVE));
            wardList.add(new Ward("ward_peds", "Pediatric Inpatient Ward", "Pediatrics", "Floor 1, West Wing", 10, 2, "nurse_demo_02", "Sister Nasreen (RN)", Ward.STATUS_ACTIVE));
            saveLocalWards(wardList);
        }

        if (!prefs.contains(KEY_BEDS_JSON)) {
            List<Bed> bedList = new ArrayList<>();
            // Ward A beds
            bedList.add(new Bed("bed_wa_101", "Bed 101", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_OCCUPIED));
            bedList.get(0).setPatientName("Karim Ahmed");
            bedList.get(0).setPatientId("AROHI-PAT-KRM1");
            bedList.get(0).setAssignedDoctorName("Dr. Raisa");
            bedList.get(0).setAssignedNurseName("Sister Fatima");
            bedList.get(0).setAdmissionDate("2026-11-06");
            bedList.get(0).setClinicalNotes("Post-op Day 2 Laparoscopic Appendectomy. Vitals stable.");

            bedList.add(new Bed("bed_wa_102", "Bed 102", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_OCCUPIED));
            bedList.get(1).setPatientName("Tanvir Hossain");
            bedList.get(1).setPatientId("AROHI-PAT-TNV2");
            bedList.get(1).setAssignedDoctorName("Dr. Raisa");
            bedList.get(1).setAssignedNurseName("Sister Fatima");
            bedList.get(1).setAdmissionDate("2026-11-07");
            bedList.get(1).setClinicalNotes("Pre-op Inguinal Hernia repair scheduled. NPO fasting.");

            bedList.add(new Bed("bed_wa_103", "Bed 103", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_AVAILABLE));
            bedList.add(new Bed("bed_wa_104", "Bed 104", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_AVAILABLE));
            bedList.add(new Bed("bed_wa_105", "Bed 105", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_CLEANING));
            bedList.add(new Bed("bed_wa_106", "Bed 106", "ward_a", "Ward A - Male Surgical", "General", Bed.STATUS_MAINTENANCE));

            // Ward B beds
            bedList.add(new Bed("bed_wb_201", "Bed 201", "ward_b", "Ward B - Female Medical", "General", Bed.STATUS_OCCUPIED));
            bedList.get(6).setPatientName("Salma Begum");
            bedList.get(6).setPatientId("AROHI-PAT-SLM3");
            bedList.get(6).setAssignedDoctorName("Dr. Raisa");
            bedList.get(6).setAssignedNurseName("Sister Nasreen");
            bedList.get(6).setAdmissionDate("2026-11-05");
            bedList.get(6).setClinicalNotes("Severe enteric fever with electrolyte imbalance. IV fluids running.");

            bedList.add(new Bed("bed_wb_202", "Bed 202", "ward_b", "Ward B - Female Medical", "General", Bed.STATUS_AVAILABLE));
            bedList.add(new Bed("bed_wb_203", "Bed 203", "ward_b", "Ward B - Female Medical", "General", Bed.STATUS_AVAILABLE));

            // ICU beds
            bedList.add(new Bed("bed_icu_01", "ICU Bed 1", "ward_icu", "ICU / CCU Critical Care", "ICU", Bed.STATUS_OCCUPIED));
            bedList.get(9).setPatientName("Rafiqul Islam");
            bedList.get(9).setPatientId("AROHI-PAT-RFQ4");
            bedList.get(9).setAssignedDoctorName("Dr. Raisa");
            bedList.get(9).setAssignedNurseName("Sister Fatima");
            bedList.get(9).setAdmissionDate("2026-11-08");
            bedList.get(9).setClinicalNotes("Acute coronary syndrome post-PCI. Continuous SpO2/ECG telemetry.");

            bedList.add(new Bed("bed_icu_02", "ICU Bed 2", "ward_icu", "ICU / CCU Critical Care", "ICU", Bed.STATUS_AVAILABLE));
            saveLocalBeds(bedList);
        }

        if (!prefs.contains(KEY_OPERATIONS_JSON)) {
            List<Operation> opList = new ArrayList<>();
            Operation op1 = new Operation(
                    "op_demo_01", "Laparoscopic Cholecystectomy", "General Surgery",
                    "ot_02", "OT Suite 2 - General & Laparoscopic",
                    "2026-11-09", "10:00 AM", 90,
                    "pat_demo_01", "AROHI-PAT-KRM1", "Karim Ahmed",
                    "doc_demo_01", "AROHI-DOC-01", "Dr. Raisa", "General Surgery",
                    "nurse_demo_01", "Sister Fatima (RN)"
            );
            op1.setNpoConfirmed(true);
            op1.setAnesthesiaCleared(true);
            op1.setConsentSigned(true);
            op1.setBloodArranged(true);
            op1.setPreOpAntibioticsGiven(true);
            op1.setStatus(Operation.STATUS_SCHEDULED);
            op1.setFollowUpDate("2026-11-16");
            opList.add(op1);
            saveLocalOperations(opList);
        }

        if (!prefs.contains(KEY_NURSE_TASKS_JSON)) {
            List<NursingTask> taskList = new ArrayList<>();
            taskList.add(new NursingTask(
                    "task_01", NursingTask.TYPE_MEDICATION, "Ceftriaxone 1g IV Infusion",
                    "Slow IV infusion in 100ml Normal Saline over 30 mins", "High",
                    "pat_demo_01", "AROHI-PAT-KRM1", "Karim Ahmed",
                    "Ward A - Male Surgical", "Bed 101",
                    "nurse_demo_01", "Sister Fatima (RN)",
                    "2026-11-08", "10:00 AM", "Dr. Raisa", "Monitor for any hypersensitivity reaction"
            ));
            taskList.add(new NursingTask(
                    "task_02", NursingTask.TYPE_VITALS, "Post-Op Hourly Vitals Check",
                    "Record BP, Heart Rate, SpO2, and surgical site check", "Urgent",
                    "pat_demo_01", "AROHI-PAT-KRM1", "Karim Ahmed",
                    "Ward A - Male Surgical", "Bed 101",
                    "nurse_demo_01", "Sister Fatima (RN)",
                    "2026-11-08", "12:00 PM", "Dr. Raisa", "Alert if systolic BP drops below 90 mmHg"
            ));
            taskList.add(new NursingTask(
                    "task_03", NursingTask.TYPE_DRESSING, "Aseptic Wound Dressing",
                    "Clean appendectomy port sites with Betadine and apply sterile dry dressing", "Normal",
                    "pat_demo_01", "AROHI-PAT-KRM1", "Karim Ahmed",
                    "Ward A - Male Surgical", "Bed 101",
                    "nurse_demo_01", "Sister Fatima (RN)",
                    "2026-11-08", "04:00 PM", "Dr. Raisa", "Inspect for any erythema or exudate"
            ));
            saveLocalNurseTasks(taskList);
        }

        if (!prefs.contains(KEY_VITALS_JSON)) {
            List<VitalObservation> vitList = new ArrayList<>();
            vitList.add(new VitalObservation(
                    "vit_01", "pat_demo_01", "AROHI-PAT-KRM1", "Karim Ahmed",
                    "nurse_demo_01", "Sister Fatima (RN)", "Ward A - Male Surgical", "Bed 101",
                    "122/78 mmHg", 74, 98.4, 99, 18,
                    "2026-11-08", "08:30 AM", "Patient resting comfortably. Pain score 2/10."
            ));
            saveLocalVitals(vitList);
        }

        if (!prefs.contains(KEY_DUTY_SCHEDULES_JSON)) {
            List<DutySchedule> dutyList = new ArrayList<>();
            dutyList.add(new DutySchedule(
                    "duty_01", "nurse_demo_01", "AROHI-NRS-01", "Sister Fatima (RN)",
                    "ward_a", "Ward A - Male Surgical", DutySchedule.SHIFT_MORNING, "2026-11-08",
                    true, "OT Suite 2 - General & Laparoscopic", "Laparoscopic Cholecystectomy (10:00 AM)"
            ));
            dutyList.add(new DutySchedule(
                    "duty_02", "nurse_demo_01", "AROHI-NRS-01", "Sister Fatima (RN)",
                    "ward_a", "Ward A - Male Surgical", DutySchedule.SHIFT_MORNING, "2026-11-09",
                    false, "", ""
            ));
            dutyList.add(new DutySchedule(
                    "duty_03", "nurse_demo_02", "AROHI-NRS-02", "Sister Nasreen (RN)",
                    "ward_b", "Ward B - Female Medical", DutySchedule.SHIFT_EVENING, "2026-11-08",
                    false, "", ""
            ));
            saveLocalDutySchedules(dutyList);
        }

        if (!prefs.contains(KEY_SERVICES_JSON)) {
            List<HospitalService> sList = new ArrayList<>();
            sList.add(new HospitalService("srv_01", "Emergency & Trauma Resuscitation", "Emergency", true, "24 Hours / 7 Days", "Dr. Raisa", "Ext: 101", "Equipped for acute stroke, polytrauma, and cardiac arrest"));
            sList.add(new HospitalService("srv_02", "Intensive Care Unit (ICU & CCU)", "Critical Care", true, "24 Hours / 7 Days", "Dr. Raisa", "Ext: 401", "Advanced invasive mechanical ventilation & central monitoring"));
            sList.add(new HospitalService("srv_03", "Ambulance Fleet & Mobile Care", "Transport", true, "24 Hours / 7 Days", "Dispatch Desk", "Hotline: 999", "3 Advanced Cardiac Life Support (ACLS) ambulances"));
            sList.add(new HospitalService("srv_04", "Clinical Diagnostic Pathology", "Pathology", true, "06:30 AM - 10:00 PM", "Chief Pathologist", "Ext: 205", "Automated biochemistry, hematology, and rapid PCR testing"));
            sList.add(new HospitalService("srv_05", "24/7 Blood Bank & Transfusion", "Transfusion Medicine", true, "24 Hours / 7 Days", "Blood Bank Officer", "Ext: 206", "All blood groups, packed RBCs, FFP, and platelets"));
            sList.add(new HospitalService("srv_06", "Hemodialysis Care Unit", "Nephrology", true, "07:00 AM - 11:00 PM", "Head of Nephrology", "Ext: 308", "8 Fresenius high-efficiency dialysis stations"));
            saveLocalServices(sList);
        }

        if (!prefs.contains(KEY_DEPTS_JSON)) {
            List<HospitalDepartment> dList = new ArrayList<>();
            dList.add(new HospitalDepartment("dept_01", "Cardiology & Vascular Center", "Dr. Raisa", "Floor 1, West Wing", 18, true, "Comprehensive non-invasive & interventional cardiovascular medicine"));
            dList.add(new HospitalDepartment("dept_02", "General, Laparoscopic & GI Surgery", "Dr. Raisa", "Floor 2 & 3", 14, true, "Minimally invasive keyhole procedures and elective abdominal surgeries"));
            dList.add(new HospitalDepartment("dept_03", "Orthopedics & Joint Reconstruction", "Dr. Kamal", "Floor 1 & 3", 12, true, "Trauma fracture fixation, arthroscopy, and total joint replacement"));
            dList.add(new HospitalDepartment("dept_04", "Critical Care & Anesthesiology", "Dr. Raisa", "Floor 3, North Wing", 22, true, "State of the art critical life support and surgical anesthesia"));
            dList.add(new HospitalDepartment("dept_05", "Internal Medicine & Diabetology", "Dr. Raisa", "Floor 1 & 2", 16, true, "Chronic disease management, infectious diseases, and diagnostics"));
            saveLocalDepartments(dList);
        }
    }

    private void createAdmissionNursingTasks(Bed bed) {
        List<NursingTask> tasks = getLocalNurseTasks(null);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        tasks.add(0, new NursingTask(
                "task_adm_" + System.currentTimeMillis() + "_1",
                NursingTask.TYPE_VITALS,
                "Initial Inpatient Bedside Vitals Baseline",
                "Record baseline BP, Pulse, Temp, SpO2, and weight for " + bed.getPatientName(),
                "High",
                bed.getPatientUid(), bed.getPatientId(), bed.getPatientName(),
                bed.getWardName(), bed.getBedNumber(),
                bed.getAssignedNurseUid(), bed.getAssignedNurseName(),
                today, "02:00 PM",
                bed.getAssignedDoctorName(), "Establish comprehensive baseline vitals for clinical chart"
        ));

        tasks.add(0, new NursingTask(
                "task_adm_" + System.currentTimeMillis() + "_2",
                NursingTask.TYPE_DOCTOR_INSTRUCTION,
                "Admitting Doctor Clinical Care Orders",
                bed.getClinicalNotes(),
                "Normal",
                bed.getPatientUid(), bed.getPatientId(), bed.getPatientName(),
                bed.getWardName(), bed.getBedNumber(),
                bed.getAssignedNurseUid(), bed.getAssignedNurseName(),
                today, "04:00 PM",
                bed.getAssignedDoctorName(), "Verify allergy wristband and ensure IV cannula patency"
        ));

        saveLocalNurseTasks(tasks);
    }

    // =========================================================================
    // JSON HELPERS
    // =========================================================================

    public List<OTRoom> getLocalOTRooms() {
        List<OTRoom> list = new ArrayList<>();
        String json = prefs.getString(KEY_OT_ROOMS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    OTRoom r = new OTRoom(
                            o.optString("roomId"), o.optString("roomName"), o.optString("floor"),
                            o.optString("specialties"), o.optString("status"), o.optString("equipmentNotes")
                    );
                    r.setAvailable(o.optBoolean("available", true));
                    r.setCurrentOperationId(o.optString("currentOperationId"));
                    list.add(r);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalOTRooms(List<OTRoom> list) {
        JSONArray arr = new JSONArray();
        for (OTRoom r : list) {
            arr.put(new JSONObject(r.toMap()));
        }
        prefs.edit().putString(KEY_OT_ROOMS_JSON, arr.toString()).apply();
    }

    public List<Operation> getLocalOperations() {
        List<Operation> list = new ArrayList<>();
        String json = prefs.getString(KEY_OPERATIONS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Operation op = new Operation(
                            o.optString("operationId"), o.optString("procedureName"), o.optString("department"),
                            o.optString("otRoomId"), o.optString("otRoomName"), o.optString("operationDate"),
                            o.optString("operationTime"), o.optInt("estimatedDurationMinutes", 90),
                            o.optString("patientUid"), o.optString("patientId"), o.optString("patientName"),
                            o.optString("doctorUid"), o.optString("doctorId"), o.optString("doctorName"),
                            o.optString("doctorSpecialty"), o.optString("nurseUid"), o.optString("nurseName")
                    );
                    op.setStatus(o.optString("status", Operation.STATUS_SCHEDULED));
                    op.setNpoConfirmed(o.optBoolean("npoConfirmed"));
                    op.setAnesthesiaCleared(o.optBoolean("anesthesiaCleared"));
                    op.setConsentSigned(o.optBoolean("consentSigned"));
                    op.setBloodArranged(o.optBoolean("bloodArranged"));
                    op.setPreOpAntibioticsGiven(o.optBoolean("preOpAntibioticsGiven"));
                    op.setPostOpNotes(o.optString("postOpNotes"));
                    op.setFollowUpDate(o.optString("followUpDate"));
                    op.setSurgicalNotes(o.optString("surgicalNotes"));
                    list.add(op);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalOperations(List<Operation> list) {
        JSONArray arr = new JSONArray();
        for (Operation o : list) {
            arr.put(new JSONObject(o.toMap()));
        }
        prefs.edit().putString(KEY_OPERATIONS_JSON, arr.toString()).apply();
    }

    public List<Ward> getLocalWards() {
        List<Ward> list = new ArrayList<>();
        String json = prefs.getString(KEY_WARDS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Ward w = new Ward(
                            o.optString("wardId"), o.optString("wardName"), o.optString("department"),
                            o.optString("floor"), o.optInt("totalBeds", 10), o.optInt("occupiedBeds", 0),
                            o.optString("nurseSupervisorUid"), o.optString("nurseSupervisorName"),
                            o.optString("status", Ward.STATUS_ACTIVE)
                    );
                    list.add(w);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalWards(List<Ward> list) {
        JSONArray arr = new JSONArray();
        for (Ward w : list) {
            arr.put(new JSONObject(w.toMap()));
        }
        prefs.edit().putString(KEY_WARDS_JSON, arr.toString()).apply();
    }

    public List<Bed> getLocalBeds() {
        List<Bed> list = new ArrayList<>();
        String json = prefs.getString(KEY_BEDS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Bed b = new Bed(
                            o.optString("bedId"), o.optString("bedNumber"), o.optString("wardId"),
                            o.optString("wardName"), o.optString("bedType", "General"),
                            o.optString("status", Bed.STATUS_AVAILABLE)
                    );
                    b.setPatientUid(o.optString("patientUid"));
                    b.setPatientId(o.optString("patientId"));
                    b.setPatientName(o.optString("patientName"));
                    b.setAdmissionDate(o.optString("admissionDate"));
                    b.setAssignedDoctorUid(o.optString("assignedDoctorUid"));
                    b.setAssignedDoctorName(o.optString("assignedDoctorName"));
                    b.setAssignedNurseUid(o.optString("assignedNurseUid"));
                    b.setAssignedNurseName(o.optString("assignedNurseName"));
                    b.setClinicalNotes(o.optString("clinicalNotes"));
                    list.add(b);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalBeds(List<Bed> list) {
        JSONArray arr = new JSONArray();
        for (Bed b : list) {
            arr.put(new JSONObject(b.toMap()));
        }
        prefs.edit().putString(KEY_BEDS_JSON, arr.toString()).apply();
    }

    public List<NursingTask> getLocalNurseTasks(String nurseUid) {
        List<NursingTask> list = new ArrayList<>();
        String json = prefs.getString(KEY_NURSE_TASKS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    NursingTask t = new NursingTask(
                            o.optString("taskId"), o.optString("taskType"), o.optString("title"),
                            o.optString("description"), o.optString("priority", "Normal"),
                            o.optString("patientUid"), o.optString("patientId"), o.optString("patientName"),
                            o.optString("wardName"), o.optString("bedNumber"),
                            o.optString("nurseUid"), o.optString("nurseName"),
                            o.optString("scheduledDate"), o.optString("scheduledTime"),
                            o.optString("doctorName"), o.optString("doctorInstruction")
                    );
                    t.setStatus(o.optString("status", NursingTask.STATUS_PENDING));
                    t.setCompletedAt(o.optLong("completedAt", 0));
                    t.setNurseCompletionNotes(o.optString("nurseCompletionNotes"));

                    if (nurseUid == null || nurseUid.isEmpty()
                            || nurseUid.equals(t.getNurseUid())
                            || "ALL".equalsIgnoreCase(t.getNurseUid())) {
                        list.add(t);
                    }
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalNurseTasks(List<NursingTask> list) {
        JSONArray arr = new JSONArray();
        for (NursingTask t : list) {
            arr.put(new JSONObject(t.toMap()));
        }
        prefs.edit().putString(KEY_NURSE_TASKS_JSON, arr.toString()).apply();
    }

    public List<VitalObservation> getLocalVitals() {
        List<VitalObservation> list = new ArrayList<>();
        String json = prefs.getString(KEY_VITALS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    VitalObservation v = new VitalObservation(
                            o.optString("vitalId"), o.optString("patientUid"), o.optString("patientId"),
                            o.optString("patientName"), o.optString("nurseUid"), o.optString("nurseName"),
                            o.optString("wardName"), o.optString("bedNumber"),
                            o.optString("bloodPressure", "120/80 mmHg"), o.optInt("heartRateBpm", 75),
                            o.optDouble("temperatureF", 98.6), o.optInt("spO2Percent", 98),
                            o.optInt("respiratoryRate", 18), o.optString("recordedDate"),
                            o.optString("recordedTime"), o.optString("clinicalNotes")
                    );
                    v.setRecordedTimestamp(o.optLong("recordedTimestamp", System.currentTimeMillis()));
                    list.add(v);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalVitals(List<VitalObservation> list) {
        JSONArray arr = new JSONArray();
        for (VitalObservation v : list) {
            arr.put(new JSONObject(v.toMap()));
        }
        prefs.edit().putString(KEY_VITALS_JSON, arr.toString()).apply();
    }

    public List<DutySchedule> getLocalDutySchedules() {
        List<DutySchedule> list = new ArrayList<>();
        String json = prefs.getString(KEY_DUTY_SCHEDULES_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    DutySchedule d = new DutySchedule(
                            o.optString("scheduleId"), o.optString("nurseUid"), o.optString("nurseId"),
                            o.optString("nurseName"), o.optString("wardId"), o.optString("wardName"),
                            o.optString("shiftType"), o.optString("shiftDate"),
                            o.optBoolean("otAssistance"), o.optString("otRoomName"), o.optString("otOperationName")
                    );
                    d.setStatus(o.optString("status", DutySchedule.STATUS_SCHEDULED));
                    d.setNotes(o.optString("notes"));
                    list.add(d);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalDutySchedules(List<DutySchedule> list) {
        JSONArray arr = new JSONArray();
        for (DutySchedule d : list) {
            arr.put(new JSONObject(d.toMap()));
        }
        prefs.edit().putString(KEY_DUTY_SCHEDULES_JSON, arr.toString()).apply();
    }

    public List<HospitalService> getLocalServices() {
        List<HospitalService> list = new ArrayList<>();
        String json = prefs.getString(KEY_SERVICES_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    HospitalService s = new HospitalService(
                            o.optString("serviceId"), o.optString("serviceName"), o.optString("department"),
                            o.optBoolean("active", true), o.optString("operatingHours"),
                            o.optString("leadDoctorOrHead"), o.optString("contactExtension"),
                            o.optString("description")
                    );
                    list.add(s);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalServices(List<HospitalService> list) {
        JSONArray arr = new JSONArray();
        for (HospitalService s : list) {
            arr.put(new JSONObject(s.toMap()));
        }
        prefs.edit().putString(KEY_SERVICES_JSON, arr.toString()).apply();
    }

    public List<HospitalDepartment> getLocalDepartments() {
        List<HospitalDepartment> list = new ArrayList<>();
        String json = prefs.getString(KEY_DEPTS_JSON, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    HospitalDepartment d = new HospitalDepartment(
                            o.optString("deptId"), o.optString("deptName"), o.optString("headOfDepartment"),
                            o.optString("location"), o.optInt("totalStaff", 10),
                            o.optBoolean("active", true), o.optString("description")
                    );
                    list.add(d);
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void saveLocalDepartments(List<HospitalDepartment> list) {
        JSONArray arr = new JSONArray();
        for (HospitalDepartment d : list) {
            arr.put(new JSONObject(d.toMap()));
        }
        prefs.edit().putString(KEY_DEPTS_JSON, arr.toString()).apply();
    }
}
