package com.arohi.healthcare.repositories;

import android.content.Context;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AdminProfile;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for reading and updating role-specific profiles (/patients, /doctors, /nurses, /administrators)
 * while keeping /users/{uid} and local SessionManager synchronized.
 */
public class ProfileRepository {

    private final Context appContext;
    private final SessionManager sessionManager;

    public ProfileRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Refreshes the current user's authoritative UserAccount (including latest verificationStatus) from Firestore
     * and local account registry.
     */
    public void refreshCurrentAccount(AuthCallback<UserAccount> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("No active session.");
            return;
        }
        UserAccount localReg = sessionManager.getRegisteredAccountByUid(current.getUid());
        if (localReg != null && localReg.getVerificationStatus() != null) {
            current.setVerificationStatus(localReg.getVerificationStatus());
            sessionManager.saveUserSession(current);
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(current);
            return;
        }

        db.collection(FirestoreCollections.USERS).document(current.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserAccount refreshed = snapshot.toObject(UserAccount.class);
                        if (refreshed != null) {
                            if (localReg != null
                                    && localReg.getVerificationStatusEnum() == VerificationStatus.VERIFIED
                                    && refreshed.getVerificationStatusEnum() == VerificationStatus.PENDING) {
                                refreshed.setVerificationStatus(VerificationStatus.VERIFIED.getKey());
                            }
                            refreshed.setEmailVerified(true);
                            sessionManager.saveUserSession(refreshed);
                            callback.onSuccess(refreshed);
                            return;
                        }
                    }
                    callback.onSuccess(current);
                })
                .addOnFailureListener(e -> callback.onSuccess(current));
    }

    // =========================================================================
    // 1. PATIENT PROFILE
    // =========================================================================
    public void loadPatientProfile(String uid, AuthCallback<PatientProfile> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        UserAccount session = sessionManager.getCurrentSessionAccount();
        PatientProfile localCached = uid != null ? sessionManager.getPatientProfileLocal(uid) : null;

        if (db == null || uid == null) {
            callback.onSuccess(localCached != null ? localCached : buildFallbackPatientProfile(session));
            return;
        }

        db.collection(FirestoreCollections.PATIENTS).document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        PatientProfile profile = snapshot.toObject(PatientProfile.class);
                        if (profile != null) {
                            sessionManager.savePatientProfileLocal(profile);
                            callback.onSuccess(profile);
                            return;
                        }
                    }
                    if (localCached != null) {
                        db.collection(FirestoreCollections.PATIENTS).document(uid).set(localCached.toMap(), SetOptions.merge());
                        callback.onSuccess(localCached);
                    } else {
                        callback.onSuccess(buildFallbackPatientProfile(session));
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess(
                        localCached != null ? localCached : buildFallbackPatientProfile(session)));
    }

    public void savePatientProfile(PatientProfile profile, AuthCallback<PatientProfile> callback) {
        sessionManager.savePatientProfileLocal(profile);
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null && current.getUid() != null && current.getUid().equals(profile.getUid())) {
            current.setFullName(profile.getFullName());
            current.setPhone(profile.getPhone());
            current.setAddress(profile.getAddress());
            current.setBioNotes(profile.getBioNotes());
            current.setProfileCompleted(true);
            current.setUpdatedAt(System.currentTimeMillis());
            sessionManager.saveUserSession(current);
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null || profile.getUid() == null) {
            callback.onSuccess(profile);
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", profile.getFullName());
        userUpdates.put("phone", profile.getPhone());
        userUpdates.put("address", profile.getAddress() != null ? profile.getAddress() : "");
        userUpdates.put("bioNotes", profile.getBioNotes() != null ? profile.getBioNotes() : "");
        userUpdates.put("profileCompleted", true);
        userUpdates.put("updatedAt", System.currentTimeMillis());

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.PATIENTS).document(profile.getUid()), profile.toMap(), SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.USERS).document(profile.getUid()), userUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(e -> callback.onSuccess(profile));
    }

    private PatientProfile buildFallbackPatientProfile(UserAccount session) {
        if (session == null) {
            return new PatientProfile("", "AROHI-PAT-0000", "Patient", "", "", "Not Specified", "Not Specified", "Not Set", "Not Set", "", "None");
        }
        PatientProfile p = new PatientProfile(
                session.getUid(), session.getArohiId(), session.getFullName(),
                session.getEmail(), session.getPhone(),
                "Not Specified", "Not Specified", "Not Set",
                "Not Set", session.getPhone(), "None"
        );
        p.setVerificationStatus(session.getVerificationStatus());
        p.setAddress(session.getAddress());
        p.setBioNotes(session.getBioNotes());
        return p;
    }

    // =========================================================================
    // 2. DOCTOR PROFILE
    // =========================================================================
    public void loadDoctorProfile(String uid, AuthCallback<DoctorProfile> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        UserAccount session = sessionManager.getCurrentSessionAccount();
        DoctorProfile localCached = uid != null ? sessionManager.getDoctorProfileLocal(uid) : null;

        if (db == null || uid == null) {
            callback.onSuccess(localCached != null ? localCached : buildFallbackDoctorProfile(session));
            return;
        }

        db.collection(FirestoreCollections.DOCTORS).document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        DoctorProfile profile = snapshot.toObject(DoctorProfile.class);
                        if (profile != null) {
                            sessionManager.saveDoctorProfileLocal(profile);
                            callback.onSuccess(profile);
                            return;
                        }
                    }
                    if (localCached != null) {
                        db.collection(FirestoreCollections.DOCTORS).document(uid).set(localCached.toMap(), SetOptions.merge());
                        callback.onSuccess(localCached);
                    } else {
                        callback.onSuccess(buildFallbackDoctorProfile(session));
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess(
                        localCached != null ? localCached : buildFallbackDoctorProfile(session)));
    }

    public void saveDoctorProfile(DoctorProfile profile, AuthCallback<DoctorProfile> callback) {
        sessionManager.saveDoctorProfileLocal(profile);
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null && current.getUid() != null && current.getUid().equals(profile.getUid())) {
            current.setFullName(profile.getFullName());
            current.setPhone(profile.getPhone());
            current.setAddress(profile.getAddress());
            current.setBioNotes(profile.getBioNotes());
            current.setProfileCompleted(true);
            current.setUpdatedAt(System.currentTimeMillis());
            sessionManager.saveUserSession(current);
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null || profile.getUid() == null) {
            callback.onSuccess(profile);
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", profile.getFullName());
        userUpdates.put("phone", profile.getPhone());
        userUpdates.put("address", profile.getAddress() != null ? profile.getAddress() : "");
        userUpdates.put("bioNotes", profile.getBioNotes() != null ? profile.getBioNotes() : "");
        userUpdates.put("profileCompleted", true);
        userUpdates.put("updatedAt", System.currentTimeMillis());

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.DOCTORS).document(profile.getUid()), profile.toMap(), SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.USERS).document(profile.getUid()), userUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(e -> callback.onSuccess(profile));
    }

    private DoctorProfile buildFallbackDoctorProfile(UserAccount session) {
        if (session == null) {
            return new DoctorProfile("", "AROHI-DOC-0000", "Doctor", "", "", "BMDC-PENDING", "General Medicine", "MBBS", "Medicine", 1, 500.0);
        }
        DoctorProfile d = new DoctorProfile(
                session.getUid(), session.getArohiId(), session.getFullName(),
                session.getEmail(), session.getPhone(),
                "BMDC-REGISTERED", "General Medicine", "MBBS",
                "Medicine", 1, 500.0
        );
        d.setVerificationStatus(session.getVerificationStatus());
        d.setAddress(session.getAddress());
        d.setBioNotes(session.getBioNotes());
        return d;
    }

    // =========================================================================
    // 3. NURSE PROFILE
    // =========================================================================
    public void loadNurseProfile(String uid, AuthCallback<NurseProfile> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        UserAccount session = sessionManager.getCurrentSessionAccount();
        NurseProfile localCached = uid != null ? sessionManager.getNurseProfileLocal(uid) : null;

        if (db == null || uid == null) {
            callback.onSuccess(localCached != null ? localCached : buildFallbackNurseProfile(session));
            return;
        }

        db.collection(FirestoreCollections.NURSES).document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        NurseProfile profile = snapshot.toObject(NurseProfile.class);
                        if (profile != null) {
                            sessionManager.saveNurseProfileLocal(profile);
                            callback.onSuccess(profile);
                            return;
                        }
                    }
                    if (localCached != null) {
                        db.collection(FirestoreCollections.NURSES).document(uid).set(localCached.toMap(), SetOptions.merge());
                        callback.onSuccess(localCached);
                    } else {
                        callback.onSuccess(buildFallbackNurseProfile(session));
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess(
                        localCached != null ? localCached : buildFallbackNurseProfile(session)));
    }

    public void saveNurseProfile(NurseProfile profile, AuthCallback<NurseProfile> callback) {
        sessionManager.saveNurseProfileLocal(profile);
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null && current.getUid() != null && current.getUid().equals(profile.getUid())) {
            current.setFullName(profile.getFullName());
            current.setPhone(profile.getPhone());
            current.setAddress(profile.getAddress());
            current.setBioNotes(profile.getBioNotes());
            current.setProfileCompleted(true);
            current.setUpdatedAt(System.currentTimeMillis());
            sessionManager.saveUserSession(current);
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null || profile.getUid() == null) {
            callback.onSuccess(profile);
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", profile.getFullName());
        userUpdates.put("phone", profile.getPhone());
        userUpdates.put("address", profile.getAddress() != null ? profile.getAddress() : "");
        userUpdates.put("bioNotes", profile.getBioNotes() != null ? profile.getBioNotes() : "");
        userUpdates.put("profileCompleted", true);
        userUpdates.put("updatedAt", System.currentTimeMillis());

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.NURSES).document(profile.getUid()), profile.toMap(), SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.USERS).document(profile.getUid()), userUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(e -> callback.onSuccess(profile));
    }

    private NurseProfile buildFallbackNurseProfile(UserAccount session) {
        if (session == null) {
            return new NurseProfile("", "AROHI-NRS-0000", "Nurse", "", "", "BNMC-PENDING", "BSc in Nursing", "General Ward", "Morning");
        }
        NurseProfile n = new NurseProfile(
                session.getUid(), session.getArohiId(), session.getFullName(),
                session.getEmail(), session.getPhone(),
                "BNMC-REGISTERED", "BSc in Nursing",
                "General Medicine Ward", "Morning"
        );
        n.setVerificationStatus(session.getVerificationStatus());
        n.setAddress(session.getAddress());
        n.setBioNotes(session.getBioNotes());
        return n;
    }

    // =========================================================================
    // 4. ADMIN PROFILE & HOSPITAL CONFIGURATION
    // =========================================================================
    public void loadAdminProfile(String uid, AuthCallback<AdminProfile> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        UserAccount session = sessionManager.getCurrentSessionAccount();
        AdminProfile localCached = uid != null ? sessionManager.getAdminProfileLocal(uid) : null;

        if (db == null || uid == null) {
            callback.onSuccess(localCached != null ? localCached : buildFallbackAdminProfile(session));
            return;
        }

        db.collection(FirestoreCollections.ADMINISTRATORS).document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        AdminProfile profile = snapshot.toObject(AdminProfile.class);
                        if (profile != null) {
                            sessionManager.saveAdminProfileLocal(profile);
                            callback.onSuccess(profile);
                            return;
                        }
                    }
                    if (localCached != null) {
                        db.collection(FirestoreCollections.ADMINISTRATORS).document(uid).set(localCached.toMap(), SetOptions.merge());
                        callback.onSuccess(localCached);
                    } else {
                        callback.onSuccess(buildFallbackAdminProfile(session));
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess(
                        localCached != null ? localCached : buildFallbackAdminProfile(session)));
    }

    public void saveAdminProfile(AdminProfile profile, AuthCallback<AdminProfile> callback) {
        sessionManager.saveAdminProfileLocal(profile);
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null && current.getUid() != null && current.getUid().equals(profile.getUid())) {
            current.setFullName(profile.getFullName());
            current.setPhone(profile.getPhone());
            current.setAddress(profile.getHospitalAddress());
            current.setBioNotes(profile.getBioNotes());
            current.setProfileCompleted(true);
            current.setUpdatedAt(System.currentTimeMillis());
            sessionManager.saveUserSession(current);
        }

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null || profile.getUid() == null) {
            callback.onSuccess(profile);
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", profile.getFullName());
        userUpdates.put("phone", profile.getPhone());
        userUpdates.put("address", profile.getHospitalAddress() != null ? profile.getHospitalAddress() : "");
        userUpdates.put("bioNotes", profile.getBioNotes() != null ? profile.getBioNotes() : "");
        userUpdates.put("profileCompleted", true);
        userUpdates.put("updatedAt", System.currentTimeMillis());

        WriteBatch batch = db.batch();
        batch.set(db.collection(FirestoreCollections.ADMINISTRATORS).document(profile.getUid()), profile.toMap(), SetOptions.merge());
        batch.set(db.collection(FirestoreCollections.USERS).document(profile.getUid()), userUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(e -> callback.onSuccess(profile));
    }

    private AdminProfile buildFallbackAdminProfile(UserAccount session) {
        if (session == null) {
            return new AdminProfile("", "AROHI-ADM-0000", "Administrator", "", "", "HA-EMP-001", "Hospital Authority", VerificationStatus.VERIFIED);
        }
        return new AdminProfile(
                session.getUid(), session.getArohiId(), session.getFullName(),
                session.getEmail(), session.getPhone(),
                "HA-EMP-1001", "Chief Hospital Administrator",
                VerificationStatus.VERIFIED
        );
    }
}
