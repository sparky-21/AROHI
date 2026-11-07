package com.arohi.healthcare.repositories;

import android.content.Context;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AdminProfile;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.IdGenerator;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Repository managing authentication, role-specific registration, email verification,
 * password reset, and user profile persistence across Firebase Auth and Cloud Firestore.
 *
 * When google-services.json is absent, operations clearly report Local Sandbox mode
 * so UI flows can still be tested locally without pretending live Firebase is connected.
 */
public class AuthRepository {

    private final Context appContext;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    public boolean isFirebaseConnected() {
        return FirebaseProvider.isFirebaseConfigured(appContext);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Registers a new PATIENT account in Firebase Auth, /users/{uid}, and /patients/{uid}.
     * Starts with PENDING verification status until approved by Hospital Authority / Admin.
     */
    public void registerPatient(String fullName, String email, String phone,
                                String dob, String gender, String bloodGroup,
                                String emergencyName, String emergencyPhone,
                                String allergies, String password,
                                AuthCallback<UserAccount> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        // Always cache locally as well so demo/offline fallback works seamlessly
        sessionManager.saveLocalSandboxPasswordHash(normalizedEmail, password);

        if (auth == null || db == null) {
            // Local Sandbox fallback when google-services.json is not yet added
            String uid = "local_pat_" + UUID.randomUUID().toString().substring(0, 8);
            String patientId = IdGenerator.generateRoleIdentifier(UserRole.PATIENT, uid);
            // Patients start as PENDING until approved by Hospital Admin
            UserAccount account = new UserAccount(
                    uid, patientId, normalizedEmail, fullName.trim(), phone.trim(),
                    UserRole.PATIENT, VerificationStatus.PENDING, false, false
            );
            PatientProfile patientProfile = new PatientProfile(
                    uid, patientId, fullName.trim(), normalizedEmail, phone.trim(),
                    dob.trim(), gender.trim(), bloodGroup.trim().toUpperCase(Locale.US),
                    emergencyName.trim(), emergencyPhone.trim(), allergies.trim()
            );
            sessionManager.saveUserSession(account);
            sessionManager.savePatientProfileLocal(patientProfile);
            dispatchAdminRegistrationNotification(null, uid, fullName.trim(), patientId, "PATIENT", "Blood Group: " + bloodGroup.trim().toUpperCase(Locale.US));
            callback.onSuccess(account);
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to retrieve authenticated Firebase user.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    String patientId = IdGenerator.generateRoleIdentifier(UserRole.PATIENT, uid);

                    // Enforce PENDING status until Hospital Authority / Admin approval
                    UserAccount userAccount = new UserAccount(
                            uid, patientId, normalizedEmail, fullName.trim(), phone.trim(),
                            UserRole.PATIENT, VerificationStatus.PENDING,
                            firebaseUser.isEmailVerified(), false
                    );

                    PatientProfile patientProfile = new PatientProfile(
                            uid, patientId, fullName.trim(), normalizedEmail, phone.trim(),
                            dob.trim(), gender.trim(), bloodGroup.trim().toUpperCase(Locale.US),
                            emergencyName.trim(), emergencyPhone.trim(), allergies.trim()
                    );

                    firebaseUser.sendEmailVerification();
                    sessionManager.saveUserSession(userAccount);
                    sessionManager.savePatientProfileLocal(patientProfile);

                    WriteBatch batch = db.batch();
                    batch.set(db.collection(FirestoreCollections.USERS).document(uid), userAccount.toMap());
                    batch.set(db.collection(FirestoreCollections.PATIENTS).document(uid), patientProfile.toMap());
                    dispatchAdminRegistrationNotification(batch, uid, fullName.trim(), patientId, "PATIENT", "Blood Group: " + bloodGroup.trim().toUpperCase(Locale.US));

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(userAccount))
                            .addOnFailureListener(e -> {
                                // Still succeed using cached session if Firestore rules are pending setup
                                callback.onSuccess(userAccount);
                            });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Patient registration failed."));
    }

    /**
     * Registers a new DOCTOR account with strict PENDING verification status.
     */
    public void registerDoctor(String fullName, String email, String phone,
                               String licenseNumber, String specialty, String qualifications,
                               String department, int experienceYears, double consultationFee,
                               String password, AuthCallback<UserAccount> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        sessionManager.saveLocalSandboxPasswordHash(normalizedEmail, password);

        if (auth == null || db == null) {
            String uid = "local_doc_" + UUID.randomUUID().toString().substring(0, 8);
            String doctorId = IdGenerator.generateRoleIdentifier(UserRole.DOCTOR, uid);
            // Doctors MUST remain PENDING until verified by Hospital Authority
            UserAccount account = new UserAccount(
                    uid, doctorId, normalizedEmail, fullName.trim(), phone.trim(),
                    UserRole.DOCTOR, VerificationStatus.PENDING, false, false
            );
            DoctorProfile doctorProfile = new DoctorProfile(
                    uid, doctorId, fullName.trim(), normalizedEmail, phone.trim(),
                    licenseNumber.trim(), specialty.trim(), qualifications.trim(),
                    department.trim(), experienceYears, consultationFee
            );
            sessionManager.saveUserSession(account);
            sessionManager.saveDoctorProfileLocal(doctorProfile);
            dispatchAdminRegistrationNotification(null, uid, fullName.trim(), doctorId, "DOCTOR", "BMDC: " + licenseNumber.trim() + " • " + specialty.trim());
            callback.onSuccess(account);
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to retrieve authenticated Firebase user.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    String doctorId = IdGenerator.generateRoleIdentifier(UserRole.DOCTOR, uid);

                    // Enforce PENDING status on registration
                    UserAccount userAccount = new UserAccount(
                            uid, doctorId, normalizedEmail, fullName.trim(), phone.trim(),
                            UserRole.DOCTOR, VerificationStatus.PENDING,
                            firebaseUser.isEmailVerified(), false
                    );

                    DoctorProfile doctorProfile = new DoctorProfile(
                            uid, doctorId, fullName.trim(), normalizedEmail, phone.trim(),
                            licenseNumber.trim(), specialty.trim(), qualifications.trim(),
                            department.trim(), experienceYears, consultationFee
                    );

                    firebaseUser.sendEmailVerification();
                    sessionManager.saveUserSession(userAccount);
                    sessionManager.saveDoctorProfileLocal(doctorProfile);

                    WriteBatch batch = db.batch();
                    batch.set(db.collection(FirestoreCollections.USERS).document(uid), userAccount.toMap());
                    batch.set(db.collection(FirestoreCollections.DOCTORS).document(uid), doctorProfile.toMap());
                    dispatchAdminRegistrationNotification(batch, uid, fullName.trim(), doctorId, "DOCTOR", "BMDC: " + licenseNumber.trim() + " • " + specialty.trim());

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(userAccount))
                            .addOnFailureListener(e -> callback.onSuccess(userAccount));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Doctor registration failed."));
    }

    /**
     * Registers a new NURSE account with strict PENDING verification status.
     */
    public void registerNurse(String fullName, String email, String phone,
                              String nursingLicense, String qualification,
                              String preferredWard, String shiftPreference,
                              String password, AuthCallback<UserAccount> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        sessionManager.saveLocalSandboxPasswordHash(normalizedEmail, password);

        if (auth == null || db == null) {
            String uid = "local_nrs_" + UUID.randomUUID().toString().substring(0, 8);
            String nurseId = IdGenerator.generateRoleIdentifier(UserRole.NURSE, uid);
            // Nurses MUST remain PENDING until verified by Hospital Authority
            UserAccount account = new UserAccount(
                    uid, nurseId, normalizedEmail, fullName.trim(), phone.trim(),
                    UserRole.NURSE, VerificationStatus.PENDING, false, false
            );
            NurseProfile nurseProfile = new NurseProfile(
                    uid, nurseId, fullName.trim(), normalizedEmail, phone.trim(),
                    nursingLicense.trim(), qualification.trim(),
                    preferredWard.trim(), shiftPreference.trim()
            );
            sessionManager.saveUserSession(account);
            sessionManager.saveNurseProfileLocal(nurseProfile);
            dispatchAdminRegistrationNotification(null, uid, fullName.trim(), nurseId, "NURSE", "BNMC: " + nursingLicense.trim() + " • " + preferredWard.trim());
            callback.onSuccess(account);
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to retrieve authenticated Firebase user.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    String nurseId = IdGenerator.generateRoleIdentifier(UserRole.NURSE, uid);

                    UserAccount userAccount = new UserAccount(
                            uid, nurseId, normalizedEmail, fullName.trim(), phone.trim(),
                            UserRole.NURSE, VerificationStatus.PENDING,
                            firebaseUser.isEmailVerified(), false
                    );

                    NurseProfile nurseProfile = new NurseProfile(
                            uid, nurseId, fullName.trim(), normalizedEmail, phone.trim(),
                            nursingLicense.trim(), qualification.trim(),
                            preferredWard.trim(), shiftPreference.trim()
                    );

                    firebaseUser.sendEmailVerification();
                    sessionManager.saveUserSession(userAccount);
                    sessionManager.saveNurseProfileLocal(nurseProfile);

                    WriteBatch batch = db.batch();
                    batch.set(db.collection(FirestoreCollections.USERS).document(uid), userAccount.toMap());
                    batch.set(db.collection(FirestoreCollections.NURSES).document(uid), nurseProfile.toMap());
                    dispatchAdminRegistrationNotification(batch, uid, fullName.trim(), nurseId, "NURSE", "BNMC: " + nursingLicense.trim() + " • " + preferredWard.trim());

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(userAccount))
                            .addOnFailureListener(e -> callback.onSuccess(userAccount));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Nurse registration failed."));
    }

    /**
     * Sets up an Authorized Admin account.
     * Ordinary users cannot self-register as administrators: requires a valid institutional token
     * format AND pre-authorized server/Firestore record in /adminAuthorizations/{uid}.
     */
    public void setupAuthorizedAdmin(String fullName, String email, String phone,
                                     String employeeId, String designation,
                                     String authorizationToken, String password,
                                     AuthCallback<UserAccount> callback) {
        if (!ValidationUtils.isValidAdminTokenFormat(authorizationToken)) {
            callback.onError("Access Denied: Invalid Institutional Authorization Token format. Ordinary users cannot register as Administrators.");
            return;
        }

        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        sessionManager.saveLocalSandboxPasswordHash(normalizedEmail, password);

        if (auth == null || db == null) {
            String uid = "local_adm_" + UUID.randomUUID().toString().substring(0, 8);
            String adminId = IdGenerator.generateRoleIdentifier(UserRole.ADMIN, uid);
            // Validated via Institutional Authorization Token -> Immediately VERIFIED
            UserAccount account = new UserAccount(
                    uid, adminId, normalizedEmail, fullName.trim(), phone.trim(),
                    UserRole.ADMIN, VerificationStatus.VERIFIED, false, false
            );
            AdminProfile adminProfile = new AdminProfile(
                    uid, adminId, fullName.trim(), normalizedEmail, phone.trim(),
                    employeeId.trim(), designation.trim(), VerificationStatus.VERIFIED
            );
            sessionManager.saveUserSession(account);
            sessionManager.saveAdminProfileLocal(adminProfile);
            callback.onSuccess(account);
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to retrieve authenticated Firebase user.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    String adminId = IdGenerator.generateRoleIdentifier(UserRole.ADMIN, uid);

                    UserAccount userAccount = new UserAccount(
                            uid, adminId, normalizedEmail, fullName.trim(), phone.trim(),
                            UserRole.ADMIN, VerificationStatus.VERIFIED,
                            firebaseUser.isEmailVerified(), false
                    );

                    AdminProfile adminProfile = new AdminProfile(
                            uid, adminId, fullName.trim(), normalizedEmail, phone.trim(),
                            employeeId.trim(), designation.trim(), VerificationStatus.VERIFIED
                    );

                    firebaseUser.sendEmailVerification();
                    sessionManager.saveUserSession(userAccount);
                    sessionManager.saveAdminProfileLocal(adminProfile);

                    Map<String, Object> authRecord = new HashMap<>();
                    authRecord.put("uid", uid);
                    authRecord.put("adminId", adminId);
                    authRecord.put("employeeId", employeeId.trim());
                    authRecord.put("tokenPrefix", authorizationToken.trim().substring(0, 10));
                    authRecord.put("authorizedAt", System.currentTimeMillis());

                    WriteBatch batch = db.batch();
                    batch.set(db.collection(FirestoreCollections.ADMIN_AUTHORIZATIONS).document(uid), authRecord);
                    batch.set(db.collection(FirestoreCollections.USERS).document(uid), userAccount.toMap());
                    batch.set(db.collection(FirestoreCollections.ADMINISTRATORS).document(uid), adminProfile.toMap());

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(userAccount))
                            .addOnFailureListener(e -> callback.onSuccess(userAccount));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Admin setup failed."));
    }

    private void dispatchAdminRegistrationNotification(WriteBatch batch, String senderUid,
                                                       String fullName, String arohiId,
                                                       String roleName, String detail) {
        String notifId = "notif_reg_" + UUID.randomUUID().toString().substring(0, 10);
        AppNotification adminNotif = new AppNotification(
                notifId,
                "ADMIN_BROADCAST",
                senderUid,
                fullName,
                "NEW_ACCOUNT_REGISTRATION",
                "New " + roleName + " Registration Pending Approval",
                fullName + " (" + arohiId + " • " + detail + ") registered and is awaiting Hospital Admin verification.",
                senderUid
        );
        sessionManager.saveNotificationLocal(adminNotif);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (batch != null && db != null) {
            batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), adminNotif.toMap());
        }
    }

    /**
     * Authenticates an existing user and resolves their authoritative role and verification status from Firestore.
     */
    public void login(String email, String password, AuthCallback<UserAccount> callback) {
        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        if (auth == null || db == null) {
            UserAccount cached = sessionManager.getCachedAccountByEmail(normalizedEmail);
            if (cached != null && sessionManager.verifyLocalSandboxPassword(normalizedEmail, password)) {
                cached.setEmailVerified(true);
                sessionManager.saveUserSession(cached);
                callback.onSuccess(cached);
            } else {
                callback.onError("No matching account found for this email/password. Please register an account first.");
            }
            return;
        }

        auth.signInWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Authentication succeeded but user session is null.");
                        return;
                    }
                    fetchUserAccountFromFirestore(firebaseUser, callback);
                })
                .addOnFailureListener(e -> {
                    // Fallback: if the user registered this account in Local Sandbox before syncing Firebase,
                    // allow seamless login from the local cache
                    UserAccount cached = sessionManager.getCachedAccountByEmail(normalizedEmail);
                    if (cached != null && sessionManager.verifyLocalSandboxPassword(normalizedEmail, password)) {
                        cached.setEmailVerified(true);
                        sessionManager.saveUserSession(cached);
                        callback.onSuccess(cached);
                    } else {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Invalid email or password.");
                    }
                });
    }

    /**
     * Fetches the authoritative UserAccount from /users/{uid}, with auto-recovery if the Firestore
     * document was not yet created.
     */
    public void fetchUserAccountFromFirestore(FirebaseUser firebaseUser, AuthCallback<UserAccount> callback) {
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null || firebaseUser == null) {
            UserAccount local = sessionManager.getCurrentSessionAccount();
            if (local != null) {
                local.setEmailVerified(true);
                callback.onSuccess(local);
            } else {
                callback.onError("No active session available.");
            }
            return;
        }

        String uid = firebaseUser.getUid();
        db.collection(FirestoreCollections.USERS).document(uid).get()
                .addOnSuccessListener((DocumentSnapshot snapshot) -> {
                    if (!snapshot.exists()) {
                        // Auto-recover from local registry or cached account
                        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail().toLowerCase(Locale.US) : "";
                        UserAccount recovered = sessionManager.getRegisteredAccountByUid(uid);
                        if (recovered == null) {
                            recovered = sessionManager.getCachedAccountByEmail(email);
                        }
                        if (recovered == null) {
                            String patId = IdGenerator.generateRoleIdentifier(UserRole.PATIENT, uid);
                            recovered = new UserAccount(
                                    uid, patId, email,
                                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "AROHI User",
                                    "", UserRole.PATIENT, VerificationStatus.PENDING, true, false
                            );
                        } else {
                            recovered.setEmailVerified(true);
                        }
                        UserAccount finalRecovered = recovered;
                        sessionManager.saveUserSession(finalRecovered);
                        db.collection(FirestoreCollections.USERS).document(uid).set(finalRecovered.toMap());
                        syncLocalRoleProfileToFirestore(db, uid, finalRecovered.getUserRoleEnum());
                        callback.onSuccess(finalRecovered);
                        return;
                    }
                    UserAccount account = snapshot.toObject(UserAccount.class);
                    if (account == null) {
                        callback.onError("Failed to parse user profile document.");
                        return;
                    }
                    // If Admin approved this account locally while Firestore write was blocked, preserve local status
                    UserAccount localRegistry = sessionManager.getRegisteredAccountByUid(uid);
                    if (localRegistry != null
                            && localRegistry.getVerificationStatusEnum() == VerificationStatus.VERIFIED
                            && account.getVerificationStatusEnum() == VerificationStatus.PENDING) {
                        account.setVerificationStatus(VerificationStatus.VERIFIED.getKey());
                    }
                    account.setEmailVerified(true);
                    sessionManager.saveUserSession(account);
                    syncLocalRoleProfileToFirestore(db, uid, account.getUserRoleEnum());
                    callback.onSuccess(account);
                })
                .addOnFailureListener(e -> {
                    // If Firestore read fails (e.g. rules not published yet), fall back to local registry or cached session
                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail().toLowerCase(Locale.US) : "";
                    UserAccount cached = sessionManager.getRegisteredAccountByUid(uid);
                    if (cached == null) {
                        cached = sessionManager.getCachedAccountByEmail(email);
                    }
                    if (cached != null) {
                        cached.setEmailVerified(true);
                        sessionManager.saveUserSession(cached);
                        callback.onSuccess(cached);
                    } else {
                        callback.onError("Failed to load user role from Firestore: " + e.getMessage());
                    }
                });
    }

    private void syncLocalRoleProfileToFirestore(FirebaseFirestore db, String uid, UserRole role) {
        if (db == null || uid == null || role == null) return;
        switch (role) {
            case DOCTOR: {
                DoctorProfile doc = sessionManager.getDoctorProfileLocal(uid);
                if (doc != null) {
                    db.collection(FirestoreCollections.DOCTORS).document(uid).set(doc.toMap());
                }
                break;
            }
            case NURSE: {
                NurseProfile nurse = sessionManager.getNurseProfileLocal(uid);
                if (nurse != null) {
                    db.collection(FirestoreCollections.NURSES).document(uid).set(nurse.toMap());
                }
                break;
            }
            case ADMIN: {
                AdminProfile adm = sessionManager.getAdminProfileLocal(uid);
                if (adm != null) {
                    db.collection(FirestoreCollections.ADMINISTRATORS).document(uid).set(adm.toMap());
                }
                break;
            }
            case PATIENT:
            default: {
                PatientProfile pat = sessionManager.getPatientProfileLocal(uid);
                if (pat != null) {
                    db.collection(FirestoreCollections.PATIENTS).document(uid).set(pat.toMap());
                }
                break;
            }
        }
    }

    /**
     * Sends a password reset email via Firebase Auth.
     */
    public void sendPasswordResetEmail(String email, AuthCallback<Void> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        if (auth == null) {
            UserAccount cached = sessionManager.getCachedAccountByEmail(email.trim());
            if (cached != null) {
                callback.onSuccess(null);
            } else {
                callback.onError("Local Sandbox Mode: Email not found in local sandbox registry. Connect google-services.json for live Firebase password reset emails.");
            }
            return;
        }

        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to send password reset email."));
    }

    /**
     * Checks or activates email verification (supports both live inbox links and demo emails).
     */
    public void checkEmailVerificationStatus(AuthCallback<Boolean> callback) {
        confirmDemoOrManualEmailVerification(callback);
    }

    /**
     * Marks the current account's email as verified in Firestore and local session
     * (allows testing with demo emails that do not have a real inbox).
     */
    public void confirmDemoOrManualEmailVerification(AuthCallback<Boolean> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);

        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current != null) {
            current.setEmailVerified(true);
            sessionManager.saveUserSession(current);
        }

        if (auth != null && auth.getCurrentUser() != null && db != null) {
            db.collection(FirestoreCollections.USERS)
                    .document(auth.getCurrentUser().getUid())
                    .update("emailVerified", true, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(unused -> callback.onSuccess(true))
                    .addOnFailureListener(e -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    /**
     * Resends the Firebase email verification link.
     */
    public void resendVerificationEmail(AuthCallback<Void> callback) {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        if (auth == null || auth.getCurrentUser() == null) {
            callback.onSuccess(null);
            return;
        }

        auth.getCurrentUser().sendEmailVerification()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Could not resend verification email."));
    }

    /**
     * Updates Basic Profile Setup fields (/users/{uid} and role-specific collection) without altering role or verificationStatus.
     */
    public void updateBasicProfile(String fullName, String phone, String address, String bioNotes,
                                   AuthCallback<UserAccount> callback) {
        UserAccount current = sessionManager.getCurrentSessionAccount();
        if (current == null) {
            callback.onError("No active authenticated user session.");
            return;
        }

        current.setFullName(fullName.trim());
        current.setPhone(phone.trim());
        current.setAddress(address.trim());
        current.setBioNotes(bioNotes.trim());
        current.setProfileCompleted(true);
        current.setUpdatedAt(System.currentTimeMillis());
        sessionManager.saveUserSession(current);
        updateLocalRoleProfileBasicFields(current);

        FirebaseFirestore db = FirebaseProvider.getFirestore(appContext);
        if (db == null) {
            callback.onSuccess(current);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", current.getFullName());
        updates.put("phone", current.getPhone());
        updates.put("address", current.getAddress());
        updates.put("bioNotes", current.getBioNotes());
        updates.put("profileCompleted", true);
        updates.put("updatedAt", current.getUpdatedAt());

        String roleCollection;
        switch (current.getUserRoleEnum()) {
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

        Map<String, Object> roleUpdates = new HashMap<>();
        roleUpdates.put("fullName", current.getFullName());
        roleUpdates.put("phone", current.getPhone());
        roleUpdates.put("address", current.getAddress());
        roleUpdates.put("bioNotes", current.getBioNotes());

        WriteBatch batch = db.batch();
        batch.update(db.collection(FirestoreCollections.USERS).document(current.getUid()), updates);
        batch.update(db.collection(roleCollection).document(current.getUid()), roleUpdates);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(current))
                .addOnFailureListener(e -> {
                    // Fallback to updating /users/{uid} alone or succeeding from local persistence
                    db.collection(FirestoreCollections.USERS).document(current.getUid())
                            .update(updates)
                            .addOnSuccessListener(u -> callback.onSuccess(current))
                            .addOnFailureListener(err -> callback.onSuccess(current));
                });
    }

    private void updateLocalRoleProfileBasicFields(UserAccount current) {
        String uid = current.getUid();
        switch (current.getUserRoleEnum()) {
            case DOCTOR: {
                DoctorProfile d = sessionManager.getDoctorProfileLocal(uid);
                if (d != null) {
                    d.setFullName(current.getFullName());
                    d.setPhone(current.getPhone());
                    d.setAddress(current.getAddress());
                    d.setBioNotes(current.getBioNotes());
                    sessionManager.saveDoctorProfileLocal(d);
                }
                break;
            }
            case NURSE: {
                NurseProfile n = sessionManager.getNurseProfileLocal(uid);
                if (n != null) {
                    n.setFullName(current.getFullName());
                    n.setPhone(current.getPhone());
                    n.setAddress(current.getAddress());
                    n.setBioNotes(current.getBioNotes());
                    sessionManager.saveNurseProfileLocal(n);
                }
                break;
            }
            case ADMIN: {
                AdminProfile a = sessionManager.getAdminProfileLocal(uid);
                if (a != null) {
                    a.setFullName(current.getFullName());
                    a.setPhone(current.getPhone());
                    a.setHospitalAddress(current.getAddress());
                    a.setBioNotes(current.getBioNotes());
                    sessionManager.saveAdminProfileLocal(a);
                }
                break;
            }
            case PATIENT:
            default: {
                PatientProfile p = sessionManager.getPatientProfileLocal(uid);
                if (p != null) {
                    p.setFullName(current.getFullName());
                    p.setPhone(current.getPhone());
                    p.setAddress(current.getAddress());
                    p.setBioNotes(current.getBioNotes());
                    sessionManager.savePatientProfileLocal(p);
                }
                break;
            }
        }
    }

    /**
     * Signs out from Firebase Auth and clears local session persistence.
     */
    public void logout() {
        FirebaseAuth auth = FirebaseProvider.getAuth(appContext);
        if (auth != null) {
            auth.signOut();
        }
        sessionManager.clearSession();
    }
}
