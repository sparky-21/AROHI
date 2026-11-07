package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.PatientProfile;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 3 Repository handling:
 * - Patient discovery of Registered & Verified Doctors (search & specialty filtering)
 * - Consultation request submission with duplicate prevention and ownership validation
 * - Patient cancellation of eligible (Pending) requests
 * - Doctor incoming request inspection, acceptance, and rejection
 * - Atomic creation of DoctorPatientConnection + ConversationThread + Privacy-safe Notifications
 * - Authorized inspection of connected Patient profiles
 */
public class DoctorDiscoveryAndRequestRepository {

    private final Context appContext;
    private final SessionManager sessionManager;

    public DoctorDiscoveryAndRequestRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    // =========================================================================
    // 1. DOCTOR DISCOVERY (MERGING FIRESTORE & LOCAL REGISTRY)
    // =========================================================================
    public void loadVerifiedDoctors(AuthCallback<List<DoctorProfile>> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(buildMergedDoctorList(null, null));
            return;
        }

        db.collection(FirestoreCollections.DOCTORS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DoctorProfile> remoteList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        DoctorProfile profile = doc.toObject(DoctorProfile.class);
                        if (profile != null && profile.getUid() != null) {
                            sessionManager.saveDoctorProfileLocal(profile);
                            remoteList.add(profile);
                        }
                    }
                    // Also fetch DOCTOR accounts from /users so global cross-phone doctor registrations and verification updates are always synced
                    db.collection(FirestoreCollections.USERS)
                            .whereEqualTo("role", UserRole.DOCTOR.getKey())
                            .get()
                            .addOnSuccessListener(userSnap -> {
                                for (DocumentSnapshot uDoc : userSnap.getDocuments()) {
                                    UserAccount uAcc = uDoc.toObject(UserAccount.class);
                                    if (uAcc != null && uAcc.getUid() != null) {
                                        sessionManager.upsertAccountInRegistry(uAcc);
                                    }
                                }
                                callback.onSuccess(buildMergedDoctorList(remoteList, db));
                            })
                            .addOnFailureListener(e -> callback.onSuccess(buildMergedDoctorList(remoteList, db)));
                })
                .addOnFailureListener(e -> callback.onSuccess(buildMergedDoctorList(null, db)));
    }

    private List<DoctorProfile> buildMergedDoctorList(List<DoctorProfile> remoteDoctors, FirebaseFirestore db) {
        Map<String, DoctorProfile> byUid = new LinkedHashMap<>();
        Map<String, DoctorProfile> remoteByUid = new HashMap<>();

        if (remoteDoctors != null) {
            for (DoctorProfile remote : remoteDoctors) {
                if (remote.getUid() != null) {
                    remoteByUid.put(remote.getUid(), remote);
                }
            }
        }

        // 1. Include all locally saved DoctorProfiles & push any local-only DoctorProfiles to Cloud Firestore
        for (DoctorProfile localDoc : sessionManager.getAllDoctorProfilesLocal()) {
            if (localDoc.getUid() != null) {
                UserAccount regAcc = sessionManager.getRegisteredAccountByUid(localDoc.getUid());
                if (regAcc != null && regAcc.getVerificationStatus() != null) {
                    localDoc.setVerificationStatus(regAcc.getVerificationStatus());
                }
                if (!VerificationStatus.REJECTED.getKey().equalsIgnoreCase(localDoc.getVerificationStatus())) {
                    byUid.put(localDoc.getUid(), localDoc);
                    if (db != null && !remoteByUid.containsKey(localDoc.getUid())) {
                        db.collection(FirestoreCollections.DOCTORS).document(localDoc.getUid())
                                .set(localDoc.toMap(), SetOptions.merge());
                    }
                }
            }
        }

        // 2. Merge remote Firestore DoctorProfiles
        if (remoteDoctors != null) {
            for (DoctorProfile remote : remoteDoctors) {
                if (remote.getUid() != null) {
                    UserAccount regAcc = sessionManager.getRegisteredAccountByUid(remote.getUid());
                    if (regAcc != null
                            && regAcc.getVerificationStatusEnum() == VerificationStatus.VERIFIED) {
                        remote.setVerificationStatus(VerificationStatus.VERIFIED.getKey());
                    }
                    if (!VerificationStatus.REJECTED.getKey().equalsIgnoreCase(remote.getVerificationStatus())) {
                        byUid.put(remote.getUid(), remote);
                    } else {
                        byUid.remove(remote.getUid());
                    }
                }
            }
        }

        // 3. Ensure any registered DOCTOR account in UserAccount registry also has a discoverable profile
        for (UserAccount acc : sessionManager.getAllRegisteredAccounts()) {
            if (acc.getUserRoleEnum() == UserRole.DOCTOR
                    && acc.getVerificationStatusEnum() != VerificationStatus.REJECTED
                    && acc.getUid() != null
                    && !byUid.containsKey(acc.getUid())) {
                DoctorProfile synthesized = new DoctorProfile(
                        acc.getUid(),
                        acc.getArohiId(),
                        acc.getFullName(),
                        acc.getEmail(),
                        acc.getPhone(),
                        "BMDC-REGISTERED",
                        "General Medicine",
                        "MBBS",
                        "Medicine",
                        1,
                        500.0
                );
                synthesized.setVerificationStatus(acc.getVerificationStatus());
                sessionManager.saveDoctorProfileLocal(synthesized);
                if (db != null) {
                    db.collection(FirestoreCollections.DOCTORS).document(acc.getUid())
                            .set(synthesized.toMap(), SetOptions.merge());
                }
                byUid.put(acc.getUid(), synthesized);
            }
        }

        List<DoctorProfile> result = new ArrayList<>(byUid.values());
        Collections.sort(result, (a, b) -> {
            // Verified doctors first, then alphabetical
            boolean v1 = VerificationStatus.VERIFIED.getKey().equalsIgnoreCase(a.getVerificationStatus());
            boolean v2 = VerificationStatus.VERIFIED.getKey().equalsIgnoreCase(b.getVerificationStatus());
            if (v1 != v2) {
                return v1 ? -1 : 1;
            }
            String n1 = a.getFullName() != null ? a.getFullName() : "";
            String n2 = b.getFullName() != null ? b.getFullName() : "";
            return n1.compareToIgnoreCase(n2);
        });
        return result;
    }

    // =========================================================================
    // 2. PATIENT CONSULTATION REQUEST SUBMISSION & DUPLICATE PREVENTION
    // =========================================================================
    public void sendConsultationRequest(@NonNull DoctorProfile doctor,
                                        @NonNull PatientProfile patient,
                                        @NonNull String consultationReason,
                                        @NonNull String patientNote,
                                        AuthCallback<ConsultationRequest> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.PATIENT) {
            callback.onError("Only authenticated Patient accounts can send consultation requests.");
            return;
        }
        if (!current.getUid().equals(patient.getUid())) {
            callback.onError("Security Error: Request patient UID does not match authenticated user.");
            return;
        }
        if (doctor.getUid() == null || doctor.getUid().isEmpty()) {
            callback.onError("Invalid Doctor profile selected.");
            return;
        }

        String pairId = ConsultationRequest.buildPairId(doctor.getUid(), current.getUid());

        // Check local registry first for duplicate prevention
        ConsultationRequest localExisting = sessionManager.getConsultationRequestLocal(pairId);
        if (localExisting != null) {
            if (localExisting.isPending()) {
                callback.onError("Duplicate Request Prevented: You already have a Pending consultation request with "
                        + doctor.getFullName() + ".");
                return;
            }
            if (localExisting.isAccepted()) {
                callback.onError("Already Connected: " + doctor.getFullName()
                        + " has already accepted your request. You can message them directly.");
                return;
            }
        }

        long now = System.currentTimeMillis();
        ConsultationRequest request = new ConsultationRequest();
        request.setRequestId(pairId);
        request.setPatientUid(current.getUid());
        request.setPatientId(patient.getArohiId() != null ? patient.getArohiId() : current.getArohiId());
        request.setPatientName(patient.getFullName() != null ? patient.getFullName() : current.getFullName());
        request.setPatientPhone(patient.getPhone() != null ? patient.getPhone() : current.getPhone());
        request.setPatientBloodGroup(patient.getBloodGroup());
        request.setPatientGender(patient.getGender());
        request.setPatientDob(patient.getDateOfBirth());
        request.setPatientAllergies(patient.getKnownAllergies());

        request.setDoctorUid(doctor.getUid());
        request.setDoctorId(doctor.getArohiId());
        request.setDoctorName(doctor.getFullName());
        request.setDoctorSpecialty(doctor.getSpecialty());
        request.setDoctorDepartment(doctor.getDepartment());
        request.setConsultationFee(doctor.getConsultationFeeBdt());

        request.setConsultationReason(consultationReason.trim());
        request.setPatientNote(patientNote.trim());
        request.setDoctorResponseNote("");
        request.setStatus(ConsultationRequest.STATUS_PENDING);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        // Privacy-safe notification for the doctor (does not leak sensitive patient notes)
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification doctorNotif = new AppNotification(
                notifId,
                doctor.getUid(),
                current.getUid(),
                request.getPatientName(),
                AppNotification.TYPE_CONSULTATION_REQUEST,
                "New Patient Consultation Request",
                "A patient submitted a consultation request (" + consultationReason.trim() + "). Open AROHI Doctor Portal to review.",
                pairId
        );

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            sessionManager.saveConsultationRequestLocal(request);
            sessionManager.saveNotificationLocal(doctorNotif);
            callback.onSuccess(request);
            return;
        }

        DocumentReference reqRef = db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(pairId);
        reqRef.get().addOnSuccessListener(existingSnap -> {
            if (existingSnap.exists()) {
                ConsultationRequest existing = existingSnap.toObject(ConsultationRequest.class);
                if (existing != null) {
                    sessionManager.saveConsultationRequestLocal(existing);
                    if (existing.isPending()) {
                        callback.onError("Duplicate Request Prevented: You already have a Pending consultation request with "
                                + doctor.getFullName() + ".");
                        return;
                    }
                    if (existing.isAccepted()) {
                        callback.onError("Already Connected: " + doctor.getFullName()
                                + " has already accepted your request. You can message them directly.");
                        return;
                    }
                }
            }

            sessionManager.saveConsultationRequestLocal(request);
            sessionManager.saveNotificationLocal(doctorNotif);

            WriteBatch batch = db.batch();
            batch.set(reqRef, request.toMap());
            batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), doctorNotif.toMap());

            batch.commit()
                    .addOnSuccessListener(unused -> callback.onSuccess(request))
                    .addOnFailureListener(e -> callback.onSuccess(request));
        }).addOnFailureListener(e -> {
            sessionManager.saveConsultationRequestLocal(request);
            sessionManager.saveNotificationLocal(doctorNotif);
            callback.onSuccess(request);
        });
    }

    // =========================================================================
    // 3. PATIENT REQUEST LIST & CANCELLATION OF ELIGIBLE REQUESTS
    // =========================================================================
    public void loadPatientRequests(String patientUid, AuthCallback<List<ConsultationRequest>> callback) {
        if (patientUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(filterRequestsByPatient(patientUid, null));
            return;
        }

        db.collection(FirestoreCollections.DOCTOR_REQUESTS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ConsultationRequest> remoteList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ConsultationRequest req = doc.toObject(ConsultationRequest.class);
                        if (req != null) {
                            remoteList.add(req);
                        }
                    }
                    callback.onSuccess(filterRequestsByPatient(patientUid, remoteList));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterRequestsByPatient(patientUid, null)));
    }

    private List<ConsultationRequest> filterRequestsByPatient(String patientUid, List<ConsultationRequest> remoteList) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        Map<String, ConsultationRequest> byId = new LinkedHashMap<>();
        Map<String, ConsultationRequest> remoteById = new HashMap<>();
        if (remoteList != null) {
            for (ConsultationRequest remote : remoteList) {
                if (remote.getRequestId() != null) {
                    remoteById.put(remote.getRequestId(), remote);
                }
            }
        }
        for (ConsultationRequest local : sessionManager.getAllConsultationRequestsLocal()) {
            if (patientUid.equals(local.getPatientUid()) && local.getRequestId() != null) {
                byId.put(local.getRequestId(), local);
                if (db != null && !remoteById.containsKey(local.getRequestId())) {
                    db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(local.getRequestId())
                            .set(local.toMap(), SetOptions.merge());
                }
            }
        }
        for (ConsultationRequest remote : remoteById.values()) {
            ConsultationRequest existingLocal = byId.get(remote.getRequestId());
            if (existingLocal == null || remote.getUpdatedAt() >= existingLocal.getUpdatedAt()) {
                byId.put(remote.getRequestId(), remote);
                sessionManager.saveConsultationRequestLocal(remote);
            } else if (db != null) {
                db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(existingLocal.getRequestId())
                        .set(existingLocal.toMap(), SetOptions.merge());
            }
        }
        List<ConsultationRequest> list = new ArrayList<>(byId.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return list;
    }

    public void cancelConsultationRequest(@NonNull ConsultationRequest request, AuthCallback<Void> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || !current.getUid().equals(request.getPatientUid())) {
            callback.onError("Security Error: You can only cancel your own consultation requests.");
            return;
        }
        if (!request.isPending()) {
            callback.onError("Only Pending requests are eligible to be cancelled (current status: " + request.getStatus() + ").");
            return;
        }

        long now = System.currentTimeMillis();
        request.setStatus(ConsultationRequest.STATUS_CANCELLED);
        request.setUpdatedAt(now);
        sessionManager.saveConsultationRequestLocal(request);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", ConsultationRequest.STATUS_CANCELLED);
        updates.put("updatedAt", now);

        db.collection(FirestoreCollections.DOCTOR_REQUESTS)
                .document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    // =========================================================================
    // 4. DOCTOR INCOMING REQUESTS, ACCEPT/DECLINE & CONNECTION CREATION
    // =========================================================================
    public void loadDoctorRequests(String doctorUid, AuthCallback<List<ConsultationRequest>> callback) {
        if (doctorUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(filterRequestsByDoctor(doctorUid, null));
            return;
        }

        db.collection(FirestoreCollections.DOCTOR_REQUESTS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ConsultationRequest> remoteList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ConsultationRequest req = doc.toObject(ConsultationRequest.class);
                        if (req != null) {
                            remoteList.add(req);
                        }
                    }
                    callback.onSuccess(filterRequestsByDoctor(doctorUid, remoteList));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterRequestsByDoctor(doctorUid, null)));
    }

    private List<ConsultationRequest> filterRequestsByDoctor(String doctorUid, List<ConsultationRequest> remoteList) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        Map<String, ConsultationRequest> byId = new LinkedHashMap<>();
        Map<String, ConsultationRequest> remoteById = new HashMap<>();
        if (remoteList != null) {
            for (ConsultationRequest remote : remoteList) {
                if (remote.getRequestId() != null) {
                    remoteById.put(remote.getRequestId(), remote);
                }
            }
        }
        for (ConsultationRequest local : sessionManager.getAllConsultationRequestsLocal()) {
            if (doctorUid.equals(local.getDoctorUid()) && local.getRequestId() != null) {
                byId.put(local.getRequestId(), local);
                if (db != null && !remoteById.containsKey(local.getRequestId())) {
                    db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(local.getRequestId())
                            .set(local.toMap(), SetOptions.merge());
                }
            }
        }
        for (ConsultationRequest remote : remoteById.values()) {
            ConsultationRequest existingLocal = byId.get(remote.getRequestId());
            if (existingLocal == null || remote.getUpdatedAt() >= existingLocal.getUpdatedAt()) {
                byId.put(remote.getRequestId(), remote);
                sessionManager.saveConsultationRequestLocal(remote);
            } else if (db != null) {
                db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(existingLocal.getRequestId())
                        .set(existingLocal.toMap(), SetOptions.merge());
            }
        }
        List<ConsultationRequest> list = new ArrayList<>(byId.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return list;
    }

    public void respondToConsultationRequest(@NonNull ConsultationRequest request,
                                             boolean accept,
                                             @NonNull String doctorResponseNote,
                                             AuthCallback<Void> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null || current.getUserRoleEnum() != UserRole.DOCTOR) {
            callback.onError("Security Error: Only authenticated Doctors can respond to consultation requests.");
            return;
        }
        // Check latest verification status from local registry as well
        UserAccount latestReg = sessionManager.getRegisteredAccountByUid(current.getUid());
        if (latestReg != null && latestReg.getVerificationStatusEnum() == VerificationStatus.VERIFIED) {
            current.setVerificationStatus(VerificationStatus.VERIFIED.getKey());
            sessionManager.saveUserSession(current);
        }
        if (current.getVerificationStatusEnum() != VerificationStatus.VERIFIED) {
            callback.onError("Verification Gate: Your BMDC credentials must be VERIFIED by Hospital Administration before accepting or declining patient requests.");
            return;
        }
        if (!current.getUid().equals(request.getDoctorUid())) {
            callback.onError("Security Error: You can only respond to requests addressed to your Doctor profile.");
            return;
        }
        if (!request.isPending()) {
            callback.onError("This request is no longer Pending (current status: " + request.getStatus() + ").");
            return;
        }

        String pairId = request.getRequestId();
        long now = System.currentTimeMillis();
        String newStatus = accept ? ConsultationRequest.STATUS_ACCEPTED : ConsultationRequest.STATUS_DECLINED;

        request.setStatus(newStatus);
        request.setDoctorResponseNote(doctorResponseNote.trim());
        request.setUpdatedAt(now);
        sessionManager.saveConsultationRequestLocal(request);

        DoctorPatientConnection connection = null;
        ConversationThread conversation = null;

        if (accept) {
            // 1. Create active DoctorPatientConnection in /connections/{pairId}
            connection = new DoctorPatientConnection();
            connection.setConnectionId(pairId);
            connection.setDoctorUid(request.getDoctorUid());
            connection.setDoctorId(request.getDoctorId());
            connection.setDoctorName(request.getDoctorName());
            connection.setDoctorSpecialty(request.getDoctorSpecialty());
            connection.setDoctorDepartment(request.getDoctorDepartment());
            connection.setPatientUid(request.getPatientUid());
            connection.setPatientId(request.getPatientId());
            connection.setPatientName(request.getPatientName());
            connection.setPatientPhone(request.getPatientPhone());
            connection.setPatientBloodGroup(request.getPatientBloodGroup());
            connection.setConsultationReason(request.getConsultationReason());
            connection.setStatus(DoctorPatientConnection.STATUS_ACCEPTED);
            connection.setConnectedAt(now);
            connection.setUpdatedAt(now);
            sessionManager.saveConnectionLocal(connection);

            // 2. Provision secure ConversationThread in /conversations/{pairId}
            conversation = new ConversationThread();
            conversation.setConversationId(pairId);
            conversation.setConnectionId(pairId);
            conversation.setDoctorUid(request.getDoctorUid());
            conversation.setDoctorName(request.getDoctorName());
            conversation.setDoctorSpecialty(request.getDoctorSpecialty());
            conversation.setPatientUid(request.getPatientUid());
            conversation.setPatientName(request.getPatientName());
            conversation.setPatientId(request.getPatientId());
            conversation.setConnectionStatus(DoctorPatientConnection.STATUS_ACCEPTED);
            conversation.setLastMessagePreview("Consultation request accepted. Secure messaging is now unlocked.");
            conversation.setLastMessageSenderUid(current.getUid());
            conversation.setLastMessageTimestamp(now);
            conversation.setUnreadByDoctor(false);
            conversation.setUnreadByPatient(true);
            sessionManager.saveConversationLocal(conversation);
        }

        // 3. Dispatch privacy-safe decision notification to the patient
        String notifId = "notif_" + UUID.randomUUID().toString().substring(0, 10);
        String notifTitle = accept ? "Consultation Request Accepted" : "Consultation Request Declined";
        String notifBody = accept
                ? request.getDoctorName() + " accepted your consultation request. Secure messaging is now available."
                : request.getDoctorName() + " was unable to accept your consultation request at this time.";
        AppNotification patientNotif = new AppNotification(
                notifId,
                request.getPatientUid(),
                current.getUid(),
                request.getDoctorName(),
                accept ? AppNotification.TYPE_REQUEST_ACCEPTED : AppNotification.TYPE_REQUEST_DECLINED,
                notifTitle,
                notifBody,
                pairId
        );
        sessionManager.saveNotificationLocal(patientNotif);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(null);
            return;
        }

        Map<String, Object> reqUpdates = new HashMap<>();
        reqUpdates.put("status", newStatus);
        reqUpdates.put("doctorResponseNote", doctorResponseNote.trim());
        reqUpdates.put("updatedAt", now);

        WriteBatch batch = db.batch();
        DocumentReference reqRef = db.collection(FirestoreCollections.DOCTOR_REQUESTS).document(pairId);
        batch.set(reqRef, reqUpdates, SetOptions.merge());

        if (accept && connection != null && conversation != null) {
            DocumentReference connRef = db.collection(FirestoreCollections.CONNECTIONS).document(pairId);
            batch.set(connRef, connection.toMap(), SetOptions.merge());

            DocumentReference convRef = db.collection(FirestoreCollections.CONVERSATIONS).document(pairId);
            batch.set(convRef, conversation.toMap(), SetOptions.merge());
        }

        batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), patientNotif.toMap());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    // =========================================================================
    // 5. ACCEPTED CONNECTIONS & CONNECTED PATIENT PROFILE INSPECTION
    // =========================================================================
    public void loadDoctorAcceptedConnections(String doctorUid, AuthCallback<List<DoctorPatientConnection>> callback) {
        if (doctorUid == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(filterConnectionsByDoctor(doctorUid, null));
            return;
        }

        db.collection(FirestoreCollections.CONNECTIONS)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DoctorPatientConnection> remoteList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        DoctorPatientConnection conn = doc.toObject(DoctorPatientConnection.class);
                        if (conn != null && conn.isAccepted()) {
                            remoteList.add(conn);
                        }
                    }
                    callback.onSuccess(filterConnectionsByDoctor(doctorUid, remoteList));
                })
                .addOnFailureListener(e -> callback.onSuccess(filterConnectionsByDoctor(doctorUid, null)));
    }

    private List<DoctorPatientConnection> filterConnectionsByDoctor(String doctorUid, List<DoctorPatientConnection> remoteList) {
        Map<String, DoctorPatientConnection> byId = new LinkedHashMap<>();
        for (DoctorPatientConnection local : sessionManager.getAllConnectionsLocal()) {
            if (doctorUid.equals(local.getDoctorUid()) && local.isAccepted() && local.getConnectionId() != null) {
                byId.put(local.getConnectionId(), local);
            }
        }
        if (remoteList != null) {
            for (DoctorPatientConnection remote : remoteList) {
                if (remote.getConnectionId() != null && remote.isAccepted()) {
                    byId.put(remote.getConnectionId(), remote);
                    sessionManager.saveConnectionLocal(remote);
                }
            }
        }
        List<DoctorPatientConnection> list = new ArrayList<>(byId.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return list;
    }

    public void loadConnectedPatientProfile(String doctorUid,
                                            String patientUid,
                                            AuthCallback<PatientProfile> callback) {
        if (doctorUid == null || patientUid == null) {
            callback.onError("Invalid connection parameters.");
            return;
        }

        String pairId = ConsultationRequest.buildPairId(doctorUid, patientUid);
        DoctorPatientConnection localConn = sessionManager.getConnectionLocal(pairId);
        PatientProfile localPatient = sessionManager.getPatientProfileLocal(patientUid);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            if (localConn != null && localConn.isAccepted() && localPatient != null) {
                callback.onSuccess(localPatient);
            } else {
                callback.onError("Access Denied: No active connection exists with this patient.");
            }
            return;
        }

        db.collection(FirestoreCollections.CONNECTIONS).document(pairId).get()
                .addOnSuccessListener(connSnap -> {
                    DoctorPatientConnection conn = connSnap.exists() ? connSnap.toObject(DoctorPatientConnection.class) : localConn;
                    if (conn == null || !conn.isAccepted()) {
                        callback.onError("Access Denied: Patient connection is not in Accepted state.");
                        return;
                    }

                    db.collection(FirestoreCollections.PATIENTS).document(patientUid).get()
                            .addOnSuccessListener(patSnap -> {
                                if (patSnap.exists()) {
                                    PatientProfile profile = patSnap.toObject(PatientProfile.class);
                                    if (profile != null) {
                                        sessionManager.savePatientProfileLocal(profile);
                                        callback.onSuccess(profile);
                                        return;
                                    }
                                }
                                if (localPatient != null) {
                                    callback.onSuccess(localPatient);
                                } else {
                                    callback.onError("Patient profile document not found.");
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (localPatient != null) {
                                    callback.onSuccess(localPatient);
                                } else {
                                    callback.onError("Could not read patient profile: " + e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (localConn != null && localConn.isAccepted() && localPatient != null) {
                        callback.onSuccess(localPatient);
                    } else {
                        callback.onError("Connection check failed: " + e.getMessage());
                    }
                });
    }
}
