package com.arohi.healthcare.repositories;

import android.content.Context;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.DashboardMetrics;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.HospitalAnnouncement;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

        int localUnread = 0;
        for (AppNotification n : sessionManager.getNotificationsForRecipientLocal(patientUid)) {
            if (!n.isRead()) {
                localUnread++;
            }
        }
        metrics.setUnreadNotificationsCount(localUnread);
        applyLocalAnnouncementsToMetrics(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(metrics);
            return;
        }

        final int fallbackDocs = localAcceptedDocs;
        final int fallbackUnread = localUnread;

        db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(appointmentsSnap -> {
                    metrics.setUpcomingAppointmentsCount(appointmentsSnap.size());

                    db.collection(FirestoreCollections.PRESCRIPTIONS)
                            .whereEqualTo("patientUid", patientUid)
                            .get()
                            .addOnSuccessListener(prescriptionsSnap -> {
                                metrics.setRecentPrescriptionsCount(prescriptionsSnap.size());
                                metrics.setActiveMedicationsCount(prescriptionsSnap.size());

                                db.collection(FirestoreCollections.LABORATORY_REPORTS)
                                        .whereEqualTo("patientUid", patientUid)
                                        .get()
                                        .addOnSuccessListener(reportsSnap -> {
                                            metrics.setPendingReportsCount(reportsSnap.size());

                                            db.collection(FirestoreCollections.CONNECTIONS)
                                                    .whereEqualTo("patientUid", patientUid)
                                                    .get()
                                                    .addOnSuccessListener(connSnap -> {
                                                        int acceptedCount = 0;
                                                        for (DocumentSnapshot d : connSnap.getDocuments()) {
                                                            if ("Accepted".equalsIgnoreCase(d.getString("status"))) {
                                                                acceptedCount++;
                                                            }
                                                        }
                                                        metrics.setConnectedDoctorsCount(Math.max(acceptedCount, fallbackDocs));

                                                        db.collection(FirestoreCollections.NOTIFICATIONS)
                                                                .whereEqualTo("recipientUid", patientUid)
                                                                .get()
                                                                .addOnSuccessListener(notifSnap -> {
                                                                    int unread = 0;
                                                                    for (DocumentSnapshot nd : notifSnap.getDocuments()) {
                                                                        Boolean r = nd.getBoolean("read");
                                                                        if (r == null || !r) {
                                                                            unread++;
                                                                        }
                                                                    }
                                                                    metrics.setUnreadNotificationsCount(Math.max(unread, fallbackUnread));
                                                                    loadLatestAnnouncementIntoMetrics(db, metrics, callback);
                                                                })
                                                                .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                                                    })
                                                    .addOnFailureListener(e -> callback.onSuccess(metrics));
                                        })
                                        .addOnFailureListener(e -> callback.onSuccess(metrics));
                            })
                            .addOnFailureListener(e -> callback.onSuccess(metrics));
                })
                .addOnFailureListener(e -> callback.onSuccess(metrics));
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
        applyLocalAnnouncementsToMetrics(metrics);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(metrics);
            return;
        }

        final int fallbackPending = localPendingReqs;
        final int fallbackAccepted = localAcceptedPats;

        db.collection(FirestoreCollections.APPOINTMENTS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(apptSnap -> {
                    metrics.setTodayAppointmentsCount(apptSnap.size());

                    db.collection(FirestoreCollections.DOCTOR_REQUESTS)
                            .whereEqualTo("doctorUid", doctorUid)
                            .get()
                            .addOnSuccessListener(reqSnap -> {
                                int pendingReqs = 0;
                                for (DocumentSnapshot d : reqSnap.getDocuments()) {
                                    if ("Pending".equalsIgnoreCase(d.getString("status"))) {
                                        pendingReqs++;
                                    }
                                }
                                metrics.setPendingRequestsCount(Math.max(pendingReqs, fallbackPending));

                                db.collection(FirestoreCollections.CONNECTIONS)
                                        .whereEqualTo("doctorUid", doctorUid)
                                        .get()
                                        .addOnSuccessListener(connSnap -> {
                                            int acceptedCount = 0;
                                            for (DocumentSnapshot cd : connSnap.getDocuments()) {
                                                if ("Accepted".equalsIgnoreCase(cd.getString("status"))) {
                                                    acceptedCount++;
                                                }
                                            }
                                            metrics.setAcceptedPatientsCount(Math.max(acceptedCount, fallbackAccepted));

                                            db.collection(FirestoreCollections.OPERATIONS)
                                                    .whereEqualTo("doctorUid", doctorUid)
                                                    .get()
                                                    .addOnSuccessListener(opSnap -> {
                                                        metrics.setUpcomingOperationsCount(opSnap.size());
                                                        loadLatestAnnouncementIntoMetrics(db, metrics, callback);
                                                    })
                                                    .addOnFailureListener(e -> callback.onSuccess(metrics));
                                        })
                                        .addOnFailureListener(e -> callback.onSuccess(metrics));
                            })
                            .addOnFailureListener(e -> callback.onSuccess(metrics));
                })
                .addOnFailureListener(e -> callback.onSuccess(metrics));
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

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(metrics);
            return;
        }

        db.collection(FirestoreCollections.NURSES).document(nurseUid).get()
                .addOnSuccessListener(nurseDoc -> {
                    if (nurseDoc.exists()) {
                        NurseProfile profile = nurseDoc.toObject(NurseProfile.class);
                        if (profile != null) {
                            sessionManager.saveNurseProfileLocal(profile);
                            applyNurseProfileToMetrics(profile, metrics);
                        }
                    }

                    db.collection(FirestoreCollections.NURSE_ASSIGNMENTS)
                            .whereEqualTo("nurseUid", nurseUid)
                            .get()
                            .addOnSuccessListener(assignSnap -> {
                                int total = assignSnap.size();
                                int completed = 0;
                                for (DocumentSnapshot doc : assignSnap.getDocuments()) {
                                    String status = doc.getString("status");
                                    if ("COMPLETED".equalsIgnoreCase(status)) {
                                        completed++;
                                    }
                                }
                                metrics.setAssignedPatientsCount(total);
                                metrics.setTodayTasksCount(total);
                                metrics.setCompletedTasksCount(completed);
                                loadLatestAnnouncementIntoMetrics(db, metrics, callback);
                            })
                            .addOnFailureListener(e -> callback.onSuccess(metrics));
                })
                .addOnFailureListener(e -> callback.onSuccess(metrics));
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

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(metrics);
            return;
        }

        db.collection(FirestoreCollections.USERS).get()
                .addOnSuccessListener(usersSnap -> {
                    List<UserAccount> mergedUsers = mergeFirestoreAndLocalUsers(usersSnap.getDocuments());
                    populateUserCountsIntoMetrics(mergedUsers, metrics);

                    db.collection(FirestoreCollections.HOSPITAL_DEPARTMENTS).get()
                            .addOnSuccessListener(deptSnap -> {
                                metrics.setDepartmentsCount(deptSnap.size());
                                db.collection(FirestoreCollections.WARDS).get()
                                        .addOnSuccessListener(wardSnap -> {
                                            metrics.setWardsCount(wardSnap.size());
                                            db.collection(FirestoreCollections.BEDS).get()
                                                    .addOnSuccessListener(bedSnap -> {
                                                        metrics.setTotalBedsCount(bedSnap.size());
                                                        db.collection(FirestoreCollections.OPERATION_ROOMS).get()
                                                                .addOnSuccessListener(otSnap -> {
                                                                    metrics.setOtRoomsCount(otSnap.size());
                                                                    db.collection(FirestoreCollections.OPERATIONS).get()
                                                                            .addOnSuccessListener(opSnap -> {
                                                                                metrics.setScheduledSurgeriesCount(opSnap.size());
                                                                                loadLatestAnnouncementIntoMetrics(db, metrics, callback);
                                                                            })
                                                                            .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                                                                })
                                                                .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                                                    })
                                                    .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                                        })
                                        .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                            })
                            .addOnFailureListener(e -> loadLatestAnnouncementIntoMetrics(db, metrics, callback));
                })
                .addOnFailureListener(e -> callback.onSuccess(metrics));
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
        Map<String, UserAccount> byUid = new LinkedHashMap<>();
        for (UserAccount localAcc : sessionManager.getAllRegisteredAccounts()) {
            if (localAcc.getUid() != null) {
                byUid.put(localAcc.getUid(), localAcc);
            }
        }
        if (firestoreDocs != null) {
            for (DocumentSnapshot doc : firestoreDocs) {
                UserAccount remote = doc.toObject(UserAccount.class);
                if (remote != null && remote.getUid() != null) {
                    UserAccount existingLocal = byUid.get(remote.getUid());
                    if (existingLocal != null && existingLocal.getUpdatedAt() > remote.getUpdatedAt()) {
                        remote.setVerificationStatus(existingLocal.getVerificationStatus());
                    }
                    byUid.put(remote.getUid(), remote);
                    sessionManager.upsertAccountInRegistry(remote);
                }
            }
        }
        return new ArrayList<>(byUid.values());
    }

    /**
     * Loads all accounts with verificationStatus == PENDING from /users and local registry for Admin review.
     */
    public void loadPendingVerifications(AuthCallback<List<UserAccount>> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(filterPendingAccounts(sessionManager.getAllRegisteredAccounts()));
            return;
        }

        db.collection(FirestoreCollections.USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserAccount> merged = mergeFirestoreAndLocalUsers(querySnapshot.getDocuments());
                    callback.onSuccess(filterPendingAccounts(merged));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterPendingAccounts(sessionManager.getAllRegisteredAccounts())));
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
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(sessionManager.getAllRegisteredAccounts());
            return;
        }

        db.collection(FirestoreCollections.USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserAccount> merged = mergeFirestoreAndLocalUsers(querySnapshot.getDocuments());
                    callback.onSuccess(merged);
                })
                .addOnFailureListener(e -> callback.onSuccess(sessionManager.getAllRegisteredAccounts()));
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
        targetUser.setVerificationStatus(newStatus.getKey());
        targetUser.setUpdatedAt(System.currentTimeMillis());
        sessionManager.updateAccountVerificationInRegistry(uid, newStatus);

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

        UserRole role = targetUser.getUserRoleEnum();
        String roleCollection;
        switch (role) {
            case DOCTOR:
                roleCollection = FirestoreCollections.DOCTORS;
                break;
            case NURSE:
                roleCollection = FirestoreCollections.NURSES;
                break;
            case ADMIN:
                roleCollection = FirestoreCollections.ADMINISTRATORS;
                break;
            case PATIENT:
            default:
                roleCollection = FirestoreCollections.PATIENTS;
                break;
        }

        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("verificationStatus", newStatus.getKey());
        statusUpdate.put("updatedAt", System.currentTimeMillis());

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.USERS).document(uid), statusUpdate, SetOptions.merge());
        batch.set(db.collection(roleCollection).document(uid), statusUpdate, SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), decisionNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
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
