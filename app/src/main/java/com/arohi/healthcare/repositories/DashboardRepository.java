package com.arohi.healthcare.repositories;

import android.content.Context;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.HospitalAnnouncement;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository powering role-based dashboards (Patient, Doctor, Nurse, Admin),
 * Admin verification approval workflows, and Hospital Announcements in Cloud Firestore.
 */
public class DashboardRepository {

    private final Context appContext;
    private final SessionManager sessionManager;

    public DashboardRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    // =========================================================================
    // 1. PATIENT DASHBOARD METRICS
    // =========================================================================
    public void loadPatientDashboardMetrics(String patientUid, AuthCallback<DashboardMetrics> callback) {
        DashboardMetrics metrics = new DashboardMetrics();
        if (patientUid == null) {
            callback.onSuccess(metrics);
            return;
        }

        // Pre-populate from local hybrid registry so metrics are accurate even if Firestore is offline
        int localAcceptedDocs = 0;
        for (DoctorPatientConnection c : sessionManager.getAllConnectionsLocal()) {
            if (patientUid.equals(c.getPatientUid()) && c.isAccepted()) {
                localAcceptedDocs++;
            }
        }
        metrics.setConnectedDoctorsCount(localAcceptedDocs);

        int localAppts = 0;
        for (Appointment a : sessionManager.getAllAppointmentsLocal()) {
            if (patientUid.equals(a.getPatientUid()) && a.isActive()) {
                localAppts++;
            }
        }
        metrics.setUpcomingAppointmentsCount(localAppts);

        int localRx = 0;
        for (DigitalPrescription rx : sessionManager.getAllPrescriptionsLocal()) {
            if (patientUid.equals(rx.getPatientUid())) {
                localRx++;
            }
        }
        metrics.setRecentPrescriptionsCount(localRx);
        metrics.setActiveMedicationsCount(localRx);

        int localReports = 0;
        for (LaboratoryRequest lab : sessionManager.getAllLabRequestsLocal()) {
            if (patientUid.equals(lab.getPatientUid())) {
                localReports++;
            }
        }
        metrics.setPendingReportsCount(localReports);

        int localUnread = 0;
        for (AppNotification n : sessionManager.getNotificationsForRecipientLocal(patientUid)) {
            if (!n.isRead()) {
                localUnread++;
            }
        }
        metrics.setUnreadNotificationsCount(localUnread);
        applyLocalAnnouncementsToMetrics(metrics);

        // INSTANT RENDER (0 ms): Serve cached local state immediately to the UI
        callback.onSuccess(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        final int fallbackDocs = localAcceptedDocs;
        final int fallbackAppts = localAppts;
        final int fallbackRx = localRx;
        final int fallbackReports = localReports;
        final int fallbackUnread = localUnread;

        // PARALLEL EXECUTION: Launch all Firestore queries concurrently
        Task<QuerySnapshot> apptTask = db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("patientUid", patientUid).get();
        Task<QuerySnapshot> rxTask = db.collection(FirestoreCollections.PRESCRIPTIONS)
                .whereEqualTo("patientUid", patientUid).get();
        Task<QuerySnapshot> labTask = db.collection(FirestoreCollections.LABORATORY_REQUESTS)
                .whereEqualTo("patientUid", patientUid).get();
        Task<QuerySnapshot> connTask = db.collection(FirestoreCollections.CONNECTIONS)
                .whereEqualTo("patientUid", patientUid).get();
        Task<QuerySnapshot> notifTask = db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("recipientUid", patientUid).get();
        Task<QuerySnapshot> postTask = db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get();

        Tasks.whenAllComplete(apptTask, rxTask, labTask, connTask, notifTask, postTask)
                .addOnSuccessListener(tasks -> {
                    if (apptTask.isSuccessful() && apptTask.getResult() != null) {
                        metrics.setUpcomingAppointmentsCount(Math.max(apptTask.getResult().size(), fallbackAppts));
                    }
                    if (rxTask.isSuccessful() && rxTask.getResult() != null) {
                        metrics.setRecentPrescriptionsCount(Math.max(rxTask.getResult().size(), fallbackRx));
                        metrics.setActiveMedicationsCount(Math.max(rxTask.getResult().size(), fallbackRx));
                    }
                    if (labTask.isSuccessful() && labTask.getResult() != null) {
                        metrics.setPendingReportsCount(Math.max(labTask.getResult().size(), fallbackReports));
                    }
                    if (connTask.isSuccessful() && connTask.getResult() != null) {
                        int acceptedCount = 0;
                        for (DocumentSnapshot d : connTask.getResult().getDocuments()) {
                            if ("Accepted".equalsIgnoreCase(d.getString("status"))) {
                                acceptedCount++;
                            }
                        }
                        metrics.setConnectedDoctorsCount(Math.max(acceptedCount, fallbackDocs));
                    }
                    if (notifTask.isSuccessful() && notifTask.getResult() != null) {
                        int unread = 0;
                        for (DocumentSnapshot nd : notifTask.getResult().getDocuments()) {
                            Boolean r = nd.getBoolean("read");
                            if (r == null || !r) {
                                unread++;
                            }
                        }
                        metrics.setUnreadNotificationsCount(Math.max(unread, fallbackUnread));
                    }
                    if (postTask.isSuccessful() && postTask.getResult() != null && !postTask.getResult().isEmpty()) {
                        DocumentSnapshot latest = postTask.getResult().getDocuments().get(0);
                        metrics.setLatestAnnouncementTitle(latest.getString("title"));
                        metrics.setLatestAnnouncementBody(latest.getString("content"));
                    }
                    callback.onSuccess(metrics);
                })
                .addOnFailureListener(e -> {
                    // Local state already rendered
                });
    }

    // =========================================================================
    // 2. DOCTOR DASHBOARD METRICS
    // =========================================================================
    public void loadDoctorDashboardMetrics(String doctorUid, AuthCallback<DashboardMetrics> callback) {
        DashboardMetrics metrics = new DashboardMetrics();
        if (doctorUid == null) {
            callback.onSuccess(metrics);
            return;
        }

        int localPendingReqs = 0;
        for (ConsultationRequest r : sessionManager.getAllConsultationRequestsLocal()) {
            if (doctorUid.equals(r.getDoctorUid()) && r.isPending()) {
                localPendingReqs++;
            }
        }
        metrics.setPendingRequestsCount(localPendingReqs);

        int localAcceptedPats = 0;
        for (DoctorPatientConnection c : sessionManager.getAllConnectionsLocal()) {
            if (doctorUid.equals(c.getDoctorUid()) && c.isAccepted()) {
                localAcceptedPats++;
            }
        }
        metrics.setAcceptedPatientsCount(localAcceptedPats);

        int localTodayAppts = 0;
        for (Appointment a : sessionManager.getAllAppointmentsLocal()) {
            if (doctorUid.equals(a.getDoctorUid()) && a.isActive()) {
                localTodayAppts++;
            }
        }
        metrics.setTodayAppointmentsCount(localTodayAppts);

        int localPendingReports = 0;
        for (LaboratoryRequest lab : sessionManager.getAllLabRequestsLocal()) {
            if (doctorUid.equals(lab.getDoctorUid()) && lab.isReportReady() && !lab.isReviewed()) {
                localPendingReports++;
            }
        }
        metrics.setPendingReportReviewsCount(localPendingReports);

        int localFollowUps = 0;
        for (DigitalPrescription rx : sessionManager.getAllPrescriptionsLocal()) {
            if (doctorUid.equals(rx.getDoctorUid()) && rx.getFollowUpDate() != null && !rx.getFollowUpDate().isEmpty()) {
                localFollowUps++;
            }
        }
        metrics.setFollowUpRemindersCount(localFollowUps);

        applyLocalAnnouncementsToMetrics(metrics);

        // INSTANT RENDER (0 ms): Serve cached local state immediately
        callback.onSuccess(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        final int fallbackPending = localPendingReqs;
        final int fallbackAccepted = localAcceptedPats;
        final int fallbackTodayAppts = localTodayAppts;

        Task<QuerySnapshot> apptTask = db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("doctorUid", doctorUid).get();
        Task<QuerySnapshot> reqTask = db.collection(FirestoreCollections.DOCTOR_REQUESTS)
                .whereEqualTo("doctorUid", doctorUid).get();
        Task<QuerySnapshot> connTask = db.collection(FirestoreCollections.CONNECTIONS)
                .whereEqualTo("doctorUid", doctorUid).get();
        Task<QuerySnapshot> opTask = db.collection(FirestoreCollections.OPERATIONS)
                .whereEqualTo("doctorUid", doctorUid).get();
        Task<QuerySnapshot> postTask = db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get();

        Tasks.whenAllComplete(apptTask, reqTask, connTask, opTask, postTask)
                .addOnSuccessListener(tasks -> {
                    if (apptTask.isSuccessful() && apptTask.getResult() != null) {
                        metrics.setTodayAppointmentsCount(Math.max(apptTask.getResult().size(), fallbackTodayAppts));
                    }
                    if (reqTask.isSuccessful() && reqTask.getResult() != null) {
                        int pendingReqs = 0;
                        for (DocumentSnapshot d : reqTask.getResult().getDocuments()) {
                            if ("Pending".equalsIgnoreCase(d.getString("status"))) {
                                pendingReqs++;
                            }
                        }
                        metrics.setPendingRequestsCount(Math.max(pendingReqs, fallbackPending));
                    }
                    if (connTask.isSuccessful() && connTask.getResult() != null) {
                        int acceptedCount = 0;
                        for (DocumentSnapshot cd : connTask.getResult().getDocuments()) {
                            if ("Accepted".equalsIgnoreCase(cd.getString("status"))) {
                                acceptedCount++;
                            }
                        }
                        metrics.setAcceptedPatientsCount(Math.max(acceptedCount, fallbackAccepted));
                    }
                    if (opTask.isSuccessful() && opTask.getResult() != null) {
                        metrics.setUpcomingOperationsCount(opTask.getResult().size());
                    }
                    if (postTask.isSuccessful() && postTask.getResult() != null && !postTask.getResult().isEmpty()) {
                        DocumentSnapshot latest = postTask.getResult().getDocuments().get(0);
                        metrics.setLatestAnnouncementTitle(latest.getString("title"));
                        metrics.setLatestAnnouncementBody(latest.getString("content"));
                    }
                    callback.onSuccess(metrics);
                })
                .addOnFailureListener(e -> {
                    // Local state already rendered
                });
    }

    // =========================================================================
    // 3. NURSE DASHBOARD METRICS
    // =========================================================================
    public void loadNurseDashboardMetrics(String nurseUid, AuthCallback<DashboardMetrics> callback) {
        DashboardMetrics metrics = new DashboardMetrics();
        if (nurseUid == null) {
            callback.onSuccess(metrics);
            return;
        }

        NurseProfile localNurse = sessionManager.getNurseProfileLocal(nurseUid);
        if (localNurse != null) {
            applyNurseProfileToMetrics(localNurse, metrics);
        }
        applyLocalAnnouncementsToMetrics(metrics);

        // INSTANT RENDER (0 ms): Serve cached local state immediately
        callback.onSuccess(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        Task<DocumentSnapshot> nurseDocTask = db.collection(FirestoreCollections.NURSES).document(nurseUid).get();
        Task<QuerySnapshot> assignTask = db.collection(FirestoreCollections.NURSE_ASSIGNMENTS)
                .whereEqualTo("nurseUid", nurseUid).get();
        Task<QuerySnapshot> postTask = db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get();

        Tasks.whenAllComplete(nurseDocTask, assignTask, postTask)
                .addOnSuccessListener(tasks -> {
                    if (nurseDocTask.isSuccessful() && nurseDocTask.getResult() != null && nurseDocTask.getResult().exists()) {
                        NurseProfile profile = nurseDocTask.getResult().toObject(NurseProfile.class);
                        if (profile != null) {
                            sessionManager.saveNurseProfileLocal(profile);
                            applyNurseProfileToMetrics(profile, metrics);
                        }
                    }
                    if (assignTask.isSuccessful() && assignTask.getResult() != null) {
                        int total = assignTask.getResult().size();
                        int completed = 0;
                        for (DocumentSnapshot doc : assignTask.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if ("COMPLETED".equalsIgnoreCase(status)) {
                                completed++;
                            }
                        }
                        metrics.setAssignedPatientsCount(total);
                        metrics.setTodayTasksCount(total);
                        metrics.setCompletedTasksCount(completed);
                    }
                    if (postTask.isSuccessful() && postTask.getResult() != null && !postTask.getResult().isEmpty()) {
                        DocumentSnapshot latest = postTask.getResult().getDocuments().get(0);
                        metrics.setLatestAnnouncementTitle(latest.getString("title"));
                        metrics.setLatestAnnouncementBody(latest.getString("content"));
                    }
                    callback.onSuccess(metrics);
                })
                .addOnFailureListener(e -> {
                    // Local state already rendered
                });
    }

    private void applyNurseProfileToMetrics(NurseProfile profile, DashboardMetrics metrics) {
        String ward = profile.getAssignedWardId();
        if (ward == null || ward.isEmpty() || "UNASSIGNED".equalsIgnoreCase(ward)) {
            ward = profile.getPreferredWard() != null && !profile.getPreferredWard().isEmpty()
                    ? profile.getPreferredWard() + " (Preferred)"
                    : "UNASSIGNED";
        }
        metrics.setAssignedWardName(ward);
        metrics.setDutyShift(profile.getShiftPreference() != null && !profile.getShiftPreference().isEmpty()
                ? profile.getShiftPreference() + " Shift"
                : "Morning Shift");
    }

    // =========================================================================
    // 4. ADMIN DASHBOARD OPERATIONAL METRICS & VERIFICATION WORKFLOW
    // =========================================================================
    public void loadAdminDashboardMetrics(AuthCallback<DashboardMetrics> callback) {
        DashboardMetrics metrics = new DashboardMetrics();
        populateUserCountsIntoMetrics(sessionManager.getAllRegisteredAccounts(), metrics);
        applyLocalAnnouncementsToMetrics(metrics);

        // INSTANT RENDER (0 ms): Serve cached local state immediately
        callback.onSuccess(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        Task<QuerySnapshot> usersTask = db.collection(FirestoreCollections.USERS).get();
        Task<QuerySnapshot> deptTask = db.collection(FirestoreCollections.HOSPITAL_DEPARTMENTS).get();
        Task<QuerySnapshot> wardTask = db.collection(FirestoreCollections.WARDS).get();
        Task<QuerySnapshot> bedTask = db.collection(FirestoreCollections.BEDS).get();
        Task<QuerySnapshot> otTask = db.collection(FirestoreCollections.OPERATION_ROOMS).get();
        Task<QuerySnapshot> opTask = db.collection(FirestoreCollections.OPERATIONS).get();
        Task<QuerySnapshot> postTask = db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get();

        Tasks.whenAllComplete(usersTask, deptTask, wardTask, bedTask, otTask, opTask, postTask)
                .addOnSuccessListener(tasks -> {
                    if (usersTask.isSuccessful() && usersTask.getResult() != null) {
                        List<UserAccount> mergedUsers = mergeFirestoreAndLocalUsers(usersTask.getResult().getDocuments());
                        populateUserCountsIntoMetrics(mergedUsers, metrics);
                    }
                    if (deptTask.isSuccessful() && deptTask.getResult() != null) {
                        metrics.setDepartmentsCount(deptTask.getResult().size());
                    }
                    if (wardTask.isSuccessful() && wardTask.getResult() != null) {
                        metrics.setWardsCount(wardTask.getResult().size());
                    }
                    if (bedTask.isSuccessful() && bedTask.getResult() != null) {
                        metrics.setTotalBedsCount(bedTask.getResult().size());
                    }
                    if (otTask.isSuccessful() && otTask.getResult() != null) {
                        metrics.setOtRoomsCount(otTask.getResult().size());
                    }
                    if (opTask.isSuccessful() && opTask.getResult() != null) {
                        metrics.setScheduledSurgeriesCount(opTask.getResult().size());
                    }
                    if (postTask.isSuccessful() && postTask.getResult() != null && !postTask.getResult().isEmpty()) {
                        DocumentSnapshot latest = postTask.getResult().getDocuments().get(0);
                        metrics.setLatestAnnouncementTitle(latest.getString("title"));
                        metrics.setLatestAnnouncementBody(latest.getString("content"));
                    }
                    callback.onSuccess(metrics);
                })
                .addOnFailureListener(e -> {
                    // Local state already rendered
                });
    }

    private void populateUserCountsIntoMetrics(List<UserAccount> accounts, DashboardMetrics metrics) {
        int totalPatients = 0;
        int verifiedPatients = 0;
        int totalDoctors = 0;
        int verifiedDoctors = 0;
        int totalNurses = 0;
        int verifiedNurses = 0;
        int pendingTotal = 0;

        for (UserAccount acc : accounts) {
            String role = acc.getRole();
            String status = acc.getVerificationStatus();
            boolean isPending = VerificationStatus.PENDING.getKey().equalsIgnoreCase(status);
            boolean isVerified = VerificationStatus.VERIFIED.getKey().equalsIgnoreCase(status);

            if (isPending) {
                pendingTotal++;
            }
            if (UserRole.PATIENT.getKey().equalsIgnoreCase(role)) {
                totalPatients++;
                if (isVerified) verifiedPatients++;
            } else if (UserRole.DOCTOR.getKey().equalsIgnoreCase(role)) {
                totalDoctors++;
                if (isVerified) verifiedDoctors++;
            } else if (UserRole.NURSE.getKey().equalsIgnoreCase(role)) {
                totalNurses++;
                if (isVerified) verifiedNurses++;
            }
        }

        metrics.setTotalPatientsCount(totalPatients);
        metrics.setVerifiedPatientsCount(verifiedPatients);
        metrics.setTotalDoctorsCount(totalDoctors);
        metrics.setVerifiedDoctorsCount(verifiedDoctors);
        metrics.setTotalNursesCount(totalNurses);
        metrics.setVerifiedNursesCount(verifiedNurses);
        metrics.setPendingVerificationsCount(pendingTotal);
    }

    private List<UserAccount> mergeFirestoreAndLocalUsers(List<DocumentSnapshot> firestoreDocs) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        Map<String, UserAccount> byUid = new LinkedHashMap<>();
        Map<String, UserAccount> remoteByUid = new HashMap<>();

        if (firestoreDocs != null) {
            for (DocumentSnapshot doc : firestoreDocs) {
                UserAccount remote = doc.toObject(UserAccount.class);
                if (remote != null && remote.getUid() != null) {
                    remoteByUid.put(remote.getUid(), remote);
                }
            }
        }

        for (UserAccount localAcc : sessionManager.getAllRegisteredAccounts()) {
            if (localAcc.getUid() != null) {
                byUid.put(localAcc.getUid(), localAcc);
                if (db != null && !remoteByUid.containsKey(localAcc.getUid())) {
                    // Push previously local-only account & its role profile to Cloud Firestore for global multi-phone visibility
                    db.collection(FirestoreCollections.USERS).document(localAcc.getUid())
                            .set(localAcc.toMap(), SetOptions.merge());
                    pushLocalRoleProfileToCloud(db, localAcc);
                }
            }
        }

        for (UserAccount remote : remoteByUid.values()) {
            UserAccount existingLocal = byUid.get(remote.getUid());
            boolean shouldUpdateRemoteInFirestore = false;
            if (existingLocal != null) {
                // If local account has an Admin decision (VERIFIED or REJECTED) while remote is still PENDING,
                // NEVER let stale PENDING overwrite the Admin's explicit verification!
                if (!VerificationStatus.PENDING.getKey().equalsIgnoreCase(existingLocal.getVerificationStatus())
                        && VerificationStatus.PENDING.getKey().equalsIgnoreCase(remote.getVerificationStatus())) {
                    remote.setVerificationStatus(existingLocal.getVerificationStatus());
                    shouldUpdateRemoteInFirestore = true;
                } else if (existingLocal.getUpdatedAt() > remote.getUpdatedAt()) {
                    remote.setVerificationStatus(existingLocal.getVerificationStatus());
                    shouldUpdateRemoteInFirestore = true;
                }
                if (shouldUpdateRemoteInFirestore && db != null) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("verificationStatus", remote.getVerificationStatus());
                    update.put("updatedAt", System.currentTimeMillis());
                    db.collection(FirestoreCollections.USERS).document(remote.getUid())
                            .set(update, SetOptions.merge());
                    String col = getRoleCollection(remote.getUserRoleEnum());
                    if (col != null) {
                        db.collection(col).document(remote.getUid())
                                .set(update, SetOptions.merge());
                    }
                }
            }
            byUid.put(remote.getUid(), remote);
            sessionManager.upsertAccountInRegistry(remote);
        }
        return new ArrayList<>(byUid.values());
    }

    private String getRoleCollection(UserRole role) {
        if (role == null) return FirestoreCollections.PATIENTS;
        switch (role) {
            case DOCTOR:
                return FirestoreCollections.DOCTORS;
            case NURSE:
                return FirestoreCollections.NURSES;
            case ADMIN:
                return FirestoreCollections.ADMINISTRATORS;
            case PATIENT:
            default:
                return FirestoreCollections.PATIENTS;
        }
    }

    private void pushLocalRoleProfileToCloud(FirebaseFirestore db, UserAccount acc) {
        if (db == null || acc == null || acc.getUid() == null) return;
        String uid = acc.getUid();
        com.arohi.healthcare.models.PatientProfile pat = sessionManager.getPatientProfileLocal(uid);
        if (pat != null) {
            db.collection(FirestoreCollections.PATIENTS).document(uid).set(pat.toMap(), SetOptions.merge());
        }
        com.arohi.healthcare.models.DoctorProfile doc = sessionManager.getDoctorProfileLocal(uid);
        if (doc != null) {
            db.collection(FirestoreCollections.DOCTORS).document(uid).set(doc.toMap(), SetOptions.merge());
        }
        NurseProfile nrs = sessionManager.getNurseProfileLocal(uid);
        if (nrs != null) {
            db.collection(FirestoreCollections.NURSES).document(uid).set(nrs.toMap(), SetOptions.merge());
        }
        com.arohi.healthcare.models.AdminProfile adm = sessionManager.getAdminProfileLocal(uid);
        if (adm != null) {
            db.collection(FirestoreCollections.ADMINISTRATORS).document(uid).set(adm.toMap(), SetOptions.merge());
        }
    }

    /**
     * Loads all accounts with verificationStatus == PENDING from /users and local registry for Admin review.
     */
    public void loadPendingVerifications(AuthCallback<List<UserAccount>> callback) {
        // INSTANT RENDER (0 ms): Serve cached local state immediately
        callback.onSuccess(filterPendingAccounts(sessionManager.getAllRegisteredAccounts()));

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserAccount> merged = mergeFirestoreAndLocalUsers(querySnapshot.getDocuments());
                    callback.onSuccess(filterPendingAccounts(merged));
                });
    }

    private List<UserAccount> filterPendingAccounts(List<UserAccount> accounts) {
        List<UserAccount> pendingList = new ArrayList<>();
        for (UserAccount acc : accounts) {
            if (acc.getVerificationStatusEnum() == VerificationStatus.PENDING) {
                pendingList.add(acc);
            }
        }
        return pendingList;
    }

    /**
     * Loads all registered user & staff accounts from /users and local registry for the Admin Staff & Accounts tab.
     */
    public void loadAllUsersAndStaff(AuthCallback<List<UserAccount>> callback) {
        // INSTANT RENDER (0 ms): Serve cached local state immediately
        callback.onSuccess(sessionManager.getAllRegisteredAccounts());

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            return;
        }

        db.collection(FirestoreCollections.USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserAccount> merged = mergeFirestoreAndLocalUsers(querySnapshot.getDocuments());
                    callback.onSuccess(merged);
                });
    }

    /**
     * Updates a user's verificationStatus (e.g., PENDING -> VERIFIED or REJECTED) across both
     * local SessionManager registry, /users/{uid}, and their role-specific profile collection.
     */
    public void updateUserVerificationStatus(UserAccount targetUser,
                                             VerificationStatus newStatus,
                                             AuthCallback<Void> callback) {
        if (targetUser == null || targetUser.getUid() == null) {
            callback.onError("Target user unavailable.");
            return;
        }

        String uid = targetUser.getUid();
        long now = System.currentTimeMillis();
        targetUser.setVerificationStatus(newStatus.getKey());
        targetUser.setUpdatedAt(now);
        sessionManager.updateAccountVerificationInRegistry(targetUser, newStatus);

        UserAccount currentAdmin = sessionManager.getCurrentSessionAccount();
        String adminUid = currentAdmin != null ? currentAdmin.getUid() : "ADMIN";
        String adminName = currentAdmin != null ? currentAdmin.getFullName() : "Hospital Administration";

        String notifId = "notif_ver_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification decisionNotif = new AppNotification(
                notifId,
                uid,
                adminUid,
                adminName,
                "ACCOUNT_VERIFICATION_UPDATE",
                newStatus == VerificationStatus.VERIFIED
                        ? "Account Approved & Verified"
                        : "Account Verification Declined",
                newStatus == VerificationStatus.VERIFIED
                        ? "Hospital Administration verified your " + targetUser.getRole() + " credentials (" + targetUser.getArohiId() + "). Full portal features are now unlocked."
                        : "Hospital Administration declined your credential verification. Please review your profile details.",
                uid
        );
        sessionManager.saveNotificationLocal(decisionNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        String roleCollection = getRoleCollection(targetUser.getUserRoleEnum());

        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("verificationStatus", newStatus.getKey());
        statusUpdate.put("updatedAt", now);

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.USERS).document(uid), statusUpdate, SetOptions.merge());
        batch.set(db.collection(roleCollection).document(uid), statusUpdate, SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), decisionNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    // Direct set fallback in case batch was rejected by a collection rule
                    db.collection(FirestoreCollections.USERS).document(uid)
                            .set(statusUpdate, SetOptions.merge());
                    db.collection(roleCollection).document(uid)
                            .set(statusUpdate, SetOptions.merge());
                    callback.onSuccess(null);
                });
    }

    // =========================================================================
    // 5. HOSPITAL ANNOUNCEMENTS (/communityPosts)
    // =========================================================================
    public void publishHospitalAnnouncement(String title, String content, String category,
                                            AuthCallback<HospitalAnnouncement> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("No active authenticated user.");
            return;
        }

        String postId = "post_" + UUID.randomUUID().toString().substring(0, 8);
        HospitalAnnouncement announcement = new HospitalAnnouncement(
                postId, title.trim(), content.trim(), category,
                current.getUid(), current.getFullName(), current.getRole()
        );
        sessionManager.saveAnnouncementLocal(announcement);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(announcement);
            return;
        }

        db.collection(FirestoreCollections.COMMUNITY_POSTS).document(postId)
                .set(announcement.toMap())
                .addOnSuccessListener(unused -> callback.onSuccess(announcement))
                .addOnFailureListener(e -> callback.onSuccess(announcement));
    }

    public void loadRecentAnnouncements(AuthCallback<List<HospitalAnnouncement>> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(sessionManager.getAnnouncementsLocal());
            return;
        }

        db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        HospitalAnnouncement item = doc.toObject(HospitalAnnouncement.class);
                        if (item != null) {
                            sessionManager.saveAnnouncementLocal(item);
                        }
                    }
                    callback.onSuccess(sessionManager.getAnnouncementsLocal());
                })
                .addOnFailureListener(e -> callback.onSuccess(sessionManager.getAnnouncementsLocal()));
    }

    private void applyLocalAnnouncementsToMetrics(DashboardMetrics metrics) {
        List<HospitalAnnouncement> localList = sessionManager.getAnnouncementsLocal();
        if (!localList.isEmpty()) {
            metrics.setAnnouncementsCount(localList.size());
            HospitalAnnouncement latest = localList.get(0);
            metrics.setLatestAnnouncementTitle(latest.getTitle());
            metrics.setLatestAnnouncementBody(latest.getContent() + " — " + latest.getAuthorName());
        }
    }

    private void loadLatestAnnouncementIntoMetrics(FirebaseFirestore db,
                                                   DashboardMetrics metrics,
                                                   AuthCallback<DashboardMetrics> callback) {
        db.collection(FirestoreCollections.COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        HospitalAnnouncement item = doc.toObject(HospitalAnnouncement.class);
                        if (item != null) {
                            sessionManager.saveAnnouncementLocal(item);
                        }
                    }
                    applyLocalAnnouncementsToMetrics(metrics);
                    callback.onSuccess(metrics);
                })
                .addOnFailureListener(e -> {
                    applyLocalAnnouncementsToMetrics(metrics);
                    callback.onSuccess(metrics);
                });
    }
}
