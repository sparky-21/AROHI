package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.arohi.healthcare.models.AdminProfile;
import com.arohi.healthcare.models.AppNotification;
import com.arohi.healthcare.models.Appointment;
import com.arohi.healthcare.models.CalendarEvent;
import com.arohi.healthcare.models.ChatMessage;
import com.arohi.healthcare.models.ConsultationRequest;
import com.arohi.healthcare.models.ConversationThread;
import com.arohi.healthcare.models.DigitalPrescription;
import com.arohi.healthcare.models.DoctorPatientConnection;
import com.arohi.healthcare.models.DoctorProfile;
import com.arohi.healthcare.models.HospitalAnnouncement;
import com.arohi.healthcare.models.LaboratoryRequest;
import com.arohi.healthcare.models.NurseProfile;
import com.arohi.healthcare.models.PatientProfile;
import com.arohi.healthcare.models.PrescriptionMedicine;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages persistent session state, onboarding completion, and a full local/hybrid
 * registry of UserAccounts, Role Profiles (Patient, Doctor, Nurse, Admin),
 * ConsultationRequests, DoctorPatientConnections, ConversationThreads, ChatMessages,
 * Notifications, and HospitalAnnouncements so user data is never lost or replaced by
 * fake fallback defaults even when Firestore rules are not yet deployed.
 */
public class SessionManager {

    private static final String PREF_NAME = "arohi_healthcare_session_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_UID = "user_uid";
    private static final String KEY_AROHI_ID = "user_arohi_id";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_FULL_NAME = "user_full_name";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_VERIFICATION_STATUS = "user_verification_status";
    private static final String KEY_EMAIL_VERIFIED = "user_email_verified";
    private static final String KEY_PROFILE_COMPLETED = "user_profile_completed";
    private static final String KEY_ADDRESS = "user_address";
    private static final String KEY_BIO_NOTES = "user_bio_notes";

    private static final String KEY_ACCOUNTS_REGISTRY_JSON = "registry_accounts_json";
    private static final String KEY_PATIENTS_REGISTRY_JSON = "registry_patients_json";
    private static final String KEY_DOCTORS_REGISTRY_JSON = "registry_doctors_json";
    private static final String KEY_NURSES_REGISTRY_JSON = "registry_nurses_json";
    private static final String KEY_ADMINS_REGISTRY_JSON = "registry_admins_json";
    private static final String KEY_REQUESTS_REGISTRY_JSON = "registry_requests_json";
    private static final String KEY_CONNECTIONS_REGISTRY_JSON = "registry_connections_json";
    private static final String KEY_CONVERSATIONS_REGISTRY_JSON = "registry_conversations_json";
    private static final String KEY_MESSAGES_PREFIX = "registry_messages_";
    private static final String KEY_NOTIFICATIONS_REGISTRY_JSON = "registry_notifications_json";
    private static final String KEY_ANNOUNCEMENTS_REGISTRY_JSON = "registry_announcements_json";
    private static final String KEY_APPOINTMENTS_REGISTRY_JSON = "registry_appointments_json";
    private static final String KEY_PRESCRIPTIONS_REGISTRY_JSON = "registry_prescriptions_json";
    private static final String KEY_LAB_REQUESTS_REGISTRY_JSON = "registry_lab_requests_json";
    private static final String KEY_CALENDAR_EVENTS_REGISTRY_JSON = "registry_calendar_events_json";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public void saveUserSession(UserAccount account) {
        if (account == null) {
            return;
        }
        // Preserve any Admin-updated verificationStatus already stored in the local registry
        UserAccount existingInRegistry = getRegisteredAccountByUid(account.getUid());
        if (existingInRegistry != null && existingInRegistry.getVerificationStatus() != null) {
            // If registry has VERIFIED or REJECTED and incoming account was stale PENDING, keep registry status
            if (!VerificationStatus.PENDING.getKey().equalsIgnoreCase(existingInRegistry.getVerificationStatus())
                    && VerificationStatus.PENDING.getKey().equalsIgnoreCase(account.getVerificationStatus())) {
                account.setVerificationStatus(existingInRegistry.getVerificationStatus());
            }
        }

        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_UID, account.getUid())
                .putString(KEY_AROHI_ID, account.getArohiId())
                .putString(KEY_EMAIL, account.getEmail())
                .putString(KEY_FULL_NAME, account.getFullName())
                .putString(KEY_PHONE, account.getPhone())
                .putString(KEY_ROLE, account.getRole())
                .putString(KEY_VERIFICATION_STATUS, account.getVerificationStatus())
                .putBoolean(KEY_EMAIL_VERIFIED, account.isEmailVerified())
                .putBoolean(KEY_PROFILE_COMPLETED, account.isProfileCompleted())
                .putString(KEY_ADDRESS, account.getAddress() != null ? account.getAddress() : "")
                .putString(KEY_BIO_NOTES, account.getBioNotes() != null ? account.getBioNotes() : "")
                .apply();

        if (account.getEmail() != null) {
            String emailKey = normalizeEmailKey(account.getEmail());
            prefs.edit()
                    .putString("acct_" + emailKey + "_uid", account.getUid())
                    .putString("acct_" + emailKey + "_arohi_id", account.getArohiId())
                    .putString("acct_" + emailKey + "_name", account.getFullName())
                    .putString("acct_" + emailKey + "_phone", account.getPhone())
                    .putString("acct_" + emailKey + "_role", account.getRole())
                    .putString("acct_" + emailKey + "_status", account.getVerificationStatus())
                    .putBoolean("acct_" + emailKey + "_email_verified", account.isEmailVerified())
                    .putBoolean("acct_" + emailKey + "_profile_completed", account.isProfileCompleted())
                    .putString("acct_" + emailKey + "_address", account.getAddress() != null ? account.getAddress() : "")
                    .putString("acct_" + emailKey + "_bio", account.getBioNotes() != null ? account.getBioNotes() : "")
                    .apply();
        }

        upsertAccountInRegistry(account);
    }

    public void saveLocalSandboxPasswordHash(String email, String password) {
        if (email == null || password == null) {
            return;
        }
        String emailKey = normalizeEmailKey(email);
        prefs.edit().putString("acct_" + emailKey + "_pwd_hash", Integer.toHexString(password.hashCode())).apply();
    }

    public boolean verifyLocalSandboxPassword(String email, String password) {
        if (email == null || password == null) {
            return false;
        }
        String emailKey = normalizeEmailKey(email);
        String storedHash = prefs.getString("acct_" + emailKey + "_pwd_hash", null);
        return storedHash != null && storedHash.equals(Integer.toHexString(password.hashCode()));
    }

    public UserAccount getCachedAccountByEmail(String email) {
        if (email == null) {
            return null;
        }
        // First check the full accounts registry
        for (UserAccount acc : getAllRegisteredAccounts()) {
            if (acc.getEmail() != null && acc.getEmail().equalsIgnoreCase(email.trim())) {
                return acc;
            }
        }
        String emailKey = normalizeEmailKey(email);
        String uid = prefs.getString("acct_" + emailKey + "_uid", null);
        if (uid == null) {
            return null;
        }
        UserRole role = UserRole.fromKey(prefs.getString("acct_" + emailKey + "_role", "PATIENT"));
        VerificationStatus status = VerificationStatus.fromKey(prefs.getString("acct_" + emailKey + "_status", "PENDING"));
        UserAccount account = new UserAccount(
                uid,
                prefs.getString("acct_" + emailKey + "_arohi_id", ""),
                email.trim(),
                prefs.getString("acct_" + emailKey + "_name", ""),
                prefs.getString("acct_" + emailKey + "_phone", ""),
                role,
                status,
                prefs.getBoolean("acct_" + emailKey + "_email_verified", false),
                prefs.getBoolean("acct_" + emailKey + "_profile_completed", false)
        );
        account.setAddress(prefs.getString("acct_" + emailKey + "_address", ""));
        account.setBioNotes(prefs.getString("acct_" + emailKey + "_bio", ""));
        return account;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && prefs.getString(KEY_UID, null) != null;
    }

    public UserAccount getCurrentSessionAccount() {
        if (!isLoggedIn()) {
            return null;
        }
        String uid = prefs.getString(KEY_UID, "");
        UserAccount fromRegistry = getRegisteredAccountByUid(uid);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        UserRole role = UserRole.fromKey(prefs.getString(KEY_ROLE, UserRole.PATIENT.getKey()));
        VerificationStatus status = VerificationStatus.fromKey(
                prefs.getString(KEY_VERIFICATION_STATUS, VerificationStatus.PENDING.getKey())
        );
        UserAccount account = new UserAccount(
                uid,
                prefs.getString(KEY_AROHI_ID, ""),
                prefs.getString(KEY_EMAIL, ""),
                prefs.getString(KEY_FULL_NAME, ""),
                prefs.getString(KEY_PHONE, ""),
                role,
                status,
                prefs.getBoolean(KEY_EMAIL_VERIFIED, false),
                prefs.getBoolean(KEY_PROFILE_COMPLETED, false)
        );
        account.setAddress(prefs.getString(KEY_ADDRESS, ""));
        account.setBioNotes(prefs.getString(KEY_BIO_NOTES, ""));
        return account;
    }

    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_UID)
                .remove(KEY_AROHI_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_FULL_NAME)
                .remove(KEY_PHONE)
                .remove(KEY_ROLE)
                .remove(KEY_VERIFICATION_STATUS)
                .remove(KEY_EMAIL_VERIFIED)
                .remove(KEY_PROFILE_COMPLETED)
                .remove(KEY_ADDRESS)
                .remove(KEY_BIO_NOTES)
                .apply();
    }

    // =========================================================================
    // 1. LOCAL & HYBRID ACCOUNTS REGISTRY (/users)
    // =========================================================================
    public void upsertAccountInRegistry(UserAccount account) {
        if (account == null || account.getUid() == null || account.getUid().isEmpty()) {
            return;
        }
        try {
            JSONObject root = getJsonObject(KEY_ACCOUNTS_REGISTRY_JSON);
            JSONObject obj = new JSONObject();
            obj.put("uid", account.getUid());
            obj.put("arohiId", nonNull(account.getArohiId()));
            obj.put("email", nonNull(account.getEmail()));
            obj.put("fullName", nonNull(account.getFullName()));
            obj.put("phone", nonNull(account.getPhone()));
            obj.put("role", nonNull(account.getRole()));
            obj.put("verificationStatus", nonNull(account.getVerificationStatus()));
            obj.put("emailVerified", account.isEmailVerified());
            obj.put("profileCompleted", account.isProfileCompleted());
            obj.put("address", nonNull(account.getAddress()));
            obj.put("bioNotes", nonNull(account.getBioNotes()));
            obj.put("createdAt", account.getCreatedAt());
            obj.put("updatedAt", account.getUpdatedAt());
            root.put(account.getUid(), obj);
            prefs.edit().putString(KEY_ACCOUNTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public UserAccount getRegisteredAccountByUid(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            JSONObject root = getJsonObject(KEY_ACCOUNTS_REGISTRY_JSON);
            if (!root.has(uid)) return null;
            return parseUserAccount(root.getJSONObject(uid));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<UserAccount> getAllRegisteredAccounts() {
        List<UserAccount> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_ACCOUNTS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                UserAccount acc = parseUserAccount(root.getJSONObject(k));
                if (acc != null) {
                    list.add(acc);
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void updateAccountVerificationInRegistry(UserAccount account, VerificationStatus newStatus) {
        if (account == null || account.getUid() == null || newStatus == null) return;
        account.setVerificationStatus(newStatus.getKey());
        account.setUpdatedAt(System.currentTimeMillis());
        upsertAccountInRegistry(account);
        updateAccountVerificationInRegistry(account.getUid(), newStatus);
    }

    public void updateAccountVerificationInRegistry(String uid, VerificationStatus newStatus) {
        if (uid == null || newStatus == null) return;
        UserAccount acc = getRegisteredAccountByUid(uid);
        if (acc != null) {
            acc.setVerificationStatus(newStatus.getKey());
            acc.setUpdatedAt(System.currentTimeMillis());
            upsertAccountInRegistry(acc);

            if (acc.getEmail() != null) {
                String emailKey = normalizeEmailKey(acc.getEmail());
                prefs.edit().putString("acct_" + emailKey + "_status", newStatus.getKey()).apply();
            }
            if (uid.equals(prefs.getString(KEY_UID, ""))) {
                prefs.edit().putString(KEY_VERIFICATION_STATUS, newStatus.getKey()).apply();
            }
        }
        PatientProfile pat = getPatientProfileLocal(uid);
        if (pat != null) {
            pat.setVerificationStatus(newStatus.getKey());
            savePatientProfileLocal(pat);
        }
        DoctorProfile doc = getDoctorProfileLocal(uid);
        if (doc != null) {
            doc.setVerificationStatus(newStatus.getKey());
            saveDoctorProfileLocal(doc);
        }
        NurseProfile nrs = getNurseProfileLocal(uid);
        if (nrs != null) {
            nrs.setVerificationStatus(newStatus.getKey());
            saveNurseProfileLocal(nrs);
        }
    }

    private UserAccount parseUserAccount(JSONObject o) throws JSONException {
        UserAccount acc = new UserAccount();
        acc.setUid(o.optString("uid", ""));
        acc.setArohiId(o.optString("arohiId", ""));
        acc.setEmail(o.optString("email", ""));
        acc.setFullName(o.optString("fullName", ""));
        acc.setPhone(o.optString("phone", ""));
        acc.setRole(o.optString("role", "PATIENT"));
        acc.setVerificationStatus(o.optString("verificationStatus", "PENDING"));
        acc.setEmailVerified(o.optBoolean("emailVerified", false));
        acc.setProfileCompleted(o.optBoolean("profileCompleted", false));
        acc.setAddress(o.optString("address", ""));
        acc.setBioNotes(o.optString("bioNotes", ""));
        acc.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        acc.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return acc;
    }

    // =========================================================================
    // 2. PATIENT PROFILE LOCAL PERSISTENCE (/patients)
    // =========================================================================
    public void savePatientProfileLocal(PatientProfile p) {
        if (p == null || p.getUid() == null || p.getUid().isEmpty()) return;
        try {
            JSONObject root = getJsonObject(KEY_PATIENTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("uid", p.getUid());
            o.put("patientId", nonNull(p.getPatientId()));
            o.put("fullName", nonNull(p.getFullName()));
            o.put("email", nonNull(p.getEmail()));
            o.put("phone", nonNull(p.getPhone()));
            o.put("dateOfBirth", nonNull(p.getDateOfBirth()));
            o.put("gender", nonNull(p.getGender()));
            o.put("bloodGroup", nonNull(p.getBloodGroup()));
            o.put("emergencyContactName", nonNull(p.getEmergencyContactName()));
            o.put("emergencyContactPhone", nonNull(p.getEmergencyContactPhone()));
            o.put("allergies", nonNull(p.getAllergies()));
            o.put("chronicConditions", nonNull(p.getChronicConditions()));
            o.put("verificationStatus", nonNull(p.getVerificationStatus()));
            o.put("address", nonNull(p.getAddress()));
            o.put("bioNotes", nonNull(p.getBioNotes()));
            o.put("createdAt", p.getCreatedAt());
            root.put(p.getUid(), o);
            prefs.edit().putString(KEY_PATIENTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public PatientProfile getPatientProfileLocal(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            JSONObject root = getJsonObject(KEY_PATIENTS_REGISTRY_JSON);
            if (!root.has(uid)) return null;
            JSONObject o = root.getJSONObject(uid);
            PatientProfile p = new PatientProfile(
                    o.optString("uid", uid),
                    o.optString("patientId", ""),
                    o.optString("fullName", ""),
                    o.optString("email", ""),
                    o.optString("phone", ""),
                    o.optString("dateOfBirth", ""),
                    o.optString("gender", ""),
                    o.optString("bloodGroup", ""),
                    o.optString("emergencyContactName", ""),
                    o.optString("emergencyContactPhone", ""),
                    o.optString("allergies", "None")
            );
            p.setChronicConditions(o.optString("chronicConditions", "None"));
            p.setVerificationStatus(o.optString("verificationStatus", "PENDING"));
            p.setAddress(o.optString("address", ""));
            p.setBioNotes(o.optString("bioNotes", ""));
            p.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
            return p;
        } catch (JSONException e) {
            return null;
        }
    }

    // =========================================================================
    // 3. DOCTOR PROFILE LOCAL PERSISTENCE (/doctors)
    // =========================================================================
    public void saveDoctorProfileLocal(DoctorProfile d) {
        if (d == null || d.getUid() == null || d.getUid().isEmpty()) return;
        try {
            JSONObject root = getJsonObject(KEY_DOCTORS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("uid", d.getUid());
            o.put("doctorId", nonNull(d.getDoctorId()));
            o.put("fullName", nonNull(d.getFullName()));
            o.put("email", nonNull(d.getEmail()));
            o.put("phone", nonNull(d.getPhone()));
            o.put("medicalLicenseNumber", nonNull(d.getMedicalLicenseNumber()));
            o.put("specialty", nonNull(d.getSpecialty()));
            o.put("qualifications", nonNull(d.getQualifications()));
            o.put("department", nonNull(d.getDepartment()));
            o.put("experienceYears", d.getExperienceYears());
            o.put("consultationFee", d.getConsultationFee());
            o.put("availabilitySchedule", nonNull(d.getAvailabilitySchedule()));
            o.put("verificationStatus", nonNull(d.getVerificationStatus()));
            o.put("address", nonNull(d.getAddress()));
            o.put("bioNotes", nonNull(d.getBioNotes()));
            o.put("createdAt", d.getCreatedAt());
            root.put(d.getUid(), o);
            prefs.edit().putString(KEY_DOCTORS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public DoctorProfile getDoctorProfileLocal(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            JSONObject root = getJsonObject(KEY_DOCTORS_REGISTRY_JSON);
            if (!root.has(uid)) return null;
            return parseDoctorProfile(root.getJSONObject(uid));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<DoctorProfile> getAllDoctorProfilesLocal() {
        List<DoctorProfile> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_DOCTORS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                DoctorProfile d = parseDoctorProfile(root.getJSONObject(k));
                if (d != null) {
                    list.add(d);
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private DoctorProfile parseDoctorProfile(JSONObject o) {
        DoctorProfile d = new DoctorProfile(
                o.optString("uid", ""),
                o.optString("doctorId", ""),
                o.optString("fullName", ""),
                o.optString("email", ""),
                o.optString("phone", ""),
                o.optString("medicalLicenseNumber", ""),
                o.optString("specialty", "General Medicine"),
                o.optString("qualifications", "MBBS"),
                o.optString("department", "Medicine"),
                o.optInt("experienceYears", 1),
                o.optDouble("consultationFee", 500.0)
        );
        d.setAvailabilitySchedule(o.optString("availabilitySchedule", "Sun - Thu, 5:00 PM - 9:00 PM"));
        d.setVerificationStatus(o.optString("verificationStatus", "PENDING"));
        d.setAddress(o.optString("address", ""));
        d.setBioNotes(o.optString("bioNotes", ""));
        d.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        return d;
    }

    // =========================================================================
    // 4. NURSE PROFILE LOCAL PERSISTENCE (/nurses)
    // =========================================================================
    public void saveNurseProfileLocal(NurseProfile n) {
        if (n == null || n.getUid() == null || n.getUid().isEmpty()) return;
        try {
            JSONObject root = getJsonObject(KEY_NURSES_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("uid", n.getUid());
            o.put("nurseId", nonNull(n.getNurseId()));
            o.put("fullName", nonNull(n.getFullName()));
            o.put("email", nonNull(n.getEmail()));
            o.put("phone", nonNull(n.getPhone()));
            o.put("nursingLicenseNumber", nonNull(n.getNursingLicenseNumber()));
            o.put("qualification", nonNull(n.getQualification()));
            o.put("preferredWard", nonNull(n.getPreferredWard()));
            o.put("shiftPreference", nonNull(n.getShiftPreference()));
            o.put("assignedWardId", nonNull(n.getAssignedWardId()));
            o.put("verificationStatus", nonNull(n.getVerificationStatus()));
            o.put("address", nonNull(n.getAddress()));
            o.put("bioNotes", nonNull(n.getBioNotes()));
            o.put("createdAt", n.getCreatedAt());
            root.put(n.getUid(), o);
            prefs.edit().putString(KEY_NURSES_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public NurseProfile getNurseProfileLocal(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            JSONObject root = getJsonObject(KEY_NURSES_REGISTRY_JSON);
            if (!root.has(uid)) return null;
            JSONObject o = root.getJSONObject(uid);
            NurseProfile n = new NurseProfile(
                    o.optString("uid", uid),
                    o.optString("nurseId", ""),
                    o.optString("fullName", ""),
                    o.optString("email", ""),
                    o.optString("phone", ""),
                    o.optString("nursingLicenseNumber", ""),
                    o.optString("qualification", ""),
                    o.optString("preferredWard", ""),
                    o.optString("shiftPreference", "")
            );
            n.setAssignedWardId(o.optString("assignedWardId", "UNASSIGNED"));
            n.setVerificationStatus(o.optString("verificationStatus", "PENDING"));
            n.setAddress(o.optString("address", ""));
            n.setBioNotes(o.optString("bioNotes", ""));
            n.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
            return n;
        } catch (JSONException e) {
            return null;
        }
    }

    // =========================================================================
    // 5. ADMIN PROFILE LOCAL PERSISTENCE (/administrators)
    // =========================================================================
    public void saveAdminProfileLocal(AdminProfile a) {
        if (a == null || a.getUid() == null || a.getUid().isEmpty()) return;
        try {
            JSONObject root = getJsonObject(KEY_ADMINS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("uid", a.getUid());
            o.put("adminId", nonNull(a.getAdminId()));
            o.put("fullName", nonNull(a.getFullName()));
            o.put("email", nonNull(a.getEmail()));
            o.put("phone", nonNull(a.getPhone()));
            o.put("employeeId", nonNull(a.getEmployeeId()));
            o.put("designation", nonNull(a.getDesignation()));
            o.put("hospitalName", nonNull(a.getHospitalName()));
            o.put("hospitalAddress", nonNull(a.getHospitalAddress()));
            o.put("emergencyHotline", nonNull(a.getEmergencyHotline()));
            o.put("bioNotes", nonNull(a.getBioNotes()));
            o.put("verificationStatus", nonNull(a.getVerificationStatus()));
            o.put("createdAt", a.getCreatedAt());
            root.put(a.getUid(), o);
            prefs.edit().putString(KEY_ADMINS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public AdminProfile getAdminProfileLocal(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        try {
            JSONObject root = getJsonObject(KEY_ADMINS_REGISTRY_JSON);
            if (!root.has(uid)) return null;
            JSONObject o = root.getJSONObject(uid);
            AdminProfile a = new AdminProfile(
                    o.optString("uid", uid),
                    o.optString("adminId", ""),
                    o.optString("fullName", ""),
                    o.optString("email", ""),
                    o.optString("phone", ""),
                    o.optString("employeeId", ""),
                    o.optString("designation", ""),
                    VerificationStatus.fromKey(o.optString("verificationStatus", "VERIFIED"))
            );
            a.setHospitalName(o.optString("hospitalName", "AROHI Central Specialized Hospital"));
            a.setHospitalAddress(o.optString("hospitalAddress", "Dhaka Medical Hub, Bangladesh"));
            a.setEmergencyHotline(o.optString("emergencyHotline", "10666"));
            a.setBioNotes(o.optString("bioNotes", ""));
            a.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
            return a;
        } catch (JSONException e) {
            return null;
        }
    }

    // =========================================================================
    // 6. CONSULTATION REQUESTS, CONNECTIONS, CONVERSATIONS, MESSAGES & NOTIFS
    // =========================================================================
    public void saveConsultationRequestLocal(ConsultationRequest r) {
        if (r == null || r.getRequestId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_REQUESTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("requestId", r.getRequestId());
            o.put("patientUid", nonNull(r.getPatientUid()));
            o.put("patientId", nonNull(r.getPatientId()));
            o.put("patientName", nonNull(r.getPatientName()));
            o.put("patientPhone", nonNull(r.getPatientPhone()));
            o.put("patientBloodGroup", nonNull(r.getPatientBloodGroup()));
            o.put("patientGender", nonNull(r.getPatientGender()));
            o.put("patientDob", nonNull(r.getPatientDob()));
            o.put("patientAllergies", nonNull(r.getPatientAllergies()));
            o.put("doctorUid", nonNull(r.getDoctorUid()));
            o.put("doctorId", nonNull(r.getDoctorId()));
            o.put("doctorName", nonNull(r.getDoctorName()));
            o.put("doctorSpecialty", nonNull(r.getDoctorSpecialty()));
            o.put("doctorDepartment", nonNull(r.getDoctorDepartment()));
            o.put("consultationFee", r.getConsultationFee());
            o.put("consultationReason", nonNull(r.getConsultationReason()));
            o.put("patientNote", nonNull(r.getPatientNote()));
            o.put("doctorResponseNote", nonNull(r.getDoctorResponseNote()));
            o.put("status", nonNull(r.getStatus()));
            o.put("createdAt", r.getCreatedAt());
            o.put("updatedAt", r.getUpdatedAt());
            root.put(r.getRequestId(), o);
            prefs.edit().putString(KEY_REQUESTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public ConsultationRequest getConsultationRequestLocal(String requestId) {
        if (requestId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_REQUESTS_REGISTRY_JSON);
            if (!root.has(requestId)) return null;
            return parseConsultationRequest(root.getJSONObject(requestId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<ConsultationRequest> getAllConsultationRequestsLocal() {
        List<ConsultationRequest> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_REQUESTS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                ConsultationRequest r = parseConsultationRequest(root.getJSONObject(k));
                if (r != null) {
                    list.add(r);
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private ConsultationRequest parseConsultationRequest(JSONObject o) {
        ConsultationRequest r = new ConsultationRequest();
        r.setRequestId(o.optString("requestId", ""));
        r.setPatientUid(o.optString("patientUid", ""));
        r.setPatientId(o.optString("patientId", ""));
        r.setPatientName(o.optString("patientName", ""));
        r.setPatientPhone(o.optString("patientPhone", ""));
        r.setPatientBloodGroup(o.optString("patientBloodGroup", ""));
        r.setPatientGender(o.optString("patientGender", ""));
        r.setPatientDob(o.optString("patientDob", ""));
        r.setPatientAllergies(o.optString("patientAllergies", "None"));
        r.setDoctorUid(o.optString("doctorUid", ""));
        r.setDoctorId(o.optString("doctorId", ""));
        r.setDoctorName(o.optString("doctorName", ""));
        r.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        r.setDoctorDepartment(o.optString("doctorDepartment", ""));
        r.setConsultationFee(o.optDouble("consultationFee", 0.0));
        r.setConsultationReason(o.optString("consultationReason", ""));
        r.setPatientNote(o.optString("patientNote", ""));
        r.setDoctorResponseNote(o.optString("doctorResponseNote", ""));
        r.setStatus(o.optString("status", ConsultationRequest.STATUS_PENDING));
        r.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        r.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return r;
    }

    public void saveConnectionLocal(DoctorPatientConnection c) {
        if (c == null || c.getConnectionId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_CONNECTIONS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("connectionId", c.getConnectionId());
            o.put("doctorUid", nonNull(c.getDoctorUid()));
            o.put("doctorId", nonNull(c.getDoctorId()));
            o.put("doctorName", nonNull(c.getDoctorName()));
            o.put("doctorSpecialty", nonNull(c.getDoctorSpecialty()));
            o.put("doctorDepartment", nonNull(c.getDoctorDepartment()));
            o.put("patientUid", nonNull(c.getPatientUid()));
            o.put("patientId", nonNull(c.getPatientId()));
            o.put("patientName", nonNull(c.getPatientName()));
            o.put("patientPhone", nonNull(c.getPatientPhone()));
            o.put("patientBloodGroup", nonNull(c.getPatientBloodGroup()));
            o.put("consultationReason", nonNull(c.getConsultationReason()));
            o.put("status", nonNull(c.getStatus()));
            o.put("connectedAt", c.getConnectedAt());
            o.put("updatedAt", c.getUpdatedAt());
            root.put(c.getConnectionId(), o);
            prefs.edit().putString(KEY_CONNECTIONS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public DoctorPatientConnection getConnectionLocal(String connectionId) {
        if (connectionId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_CONNECTIONS_REGISTRY_JSON);
            if (!root.has(connectionId)) return null;
            return parseConnection(root.getJSONObject(connectionId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<DoctorPatientConnection> getAllConnectionsLocal() {
        List<DoctorPatientConnection> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_CONNECTIONS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                DoctorPatientConnection c = parseConnection(root.getJSONObject(k));
                if (c != null) list.add(c);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private DoctorPatientConnection parseConnection(JSONObject o) {
        DoctorPatientConnection c = new DoctorPatientConnection();
        c.setConnectionId(o.optString("connectionId", ""));
        c.setDoctorUid(o.optString("doctorUid", ""));
        c.setDoctorId(o.optString("doctorId", ""));
        c.setDoctorName(o.optString("doctorName", ""));
        c.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        c.setDoctorDepartment(o.optString("doctorDepartment", ""));
        c.setPatientUid(o.optString("patientUid", ""));
        c.setPatientId(o.optString("patientId", ""));
        c.setPatientName(o.optString("patientName", ""));
        c.setPatientPhone(o.optString("patientPhone", ""));
        c.setPatientBloodGroup(o.optString("patientBloodGroup", ""));
        c.setConsultationReason(o.optString("consultationReason", ""));
        c.setStatus(o.optString("status", DoctorPatientConnection.STATUS_ACCEPTED));
        c.setConnectedAt(o.optLong("connectedAt", System.currentTimeMillis()));
        c.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return c;
    }

    public void saveConversationLocal(ConversationThread t) {
        if (t == null || t.getConversationId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_CONVERSATIONS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("conversationId", t.getConversationId());
            o.put("connectionId", nonNull(t.getConnectionId()));
            o.put("doctorUid", nonNull(t.getDoctorUid()));
            o.put("doctorName", nonNull(t.getDoctorName()));
            o.put("doctorSpecialty", nonNull(t.getDoctorSpecialty()));
            o.put("patientUid", nonNull(t.getPatientUid()));
            o.put("patientName", nonNull(t.getPatientName()));
            o.put("patientId", nonNull(t.getPatientId()));
            o.put("connectionStatus", nonNull(t.getConnectionStatus()));
            o.put("lastMessagePreview", nonNull(t.getLastMessagePreview()));
            o.put("lastMessageSenderUid", nonNull(t.getLastMessageSenderUid()));
            o.put("lastMessageTimestamp", t.getLastMessageTimestamp());
            o.put("unreadByDoctor", t.isUnreadByDoctor());
            o.put("unreadByPatient", t.isUnreadByPatient());
            root.put(t.getConversationId(), o);
            prefs.edit().putString(KEY_CONVERSATIONS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public ConversationThread getConversationLocal(String conversationId) {
        if (conversationId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_CONVERSATIONS_REGISTRY_JSON);
            if (!root.has(conversationId)) return null;
            return parseConversation(root.getJSONObject(conversationId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<ConversationThread> getAllConversationsLocal() {
        List<ConversationThread> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_CONVERSATIONS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                ConversationThread t = parseConversation(root.getJSONObject(k));
                if (t != null) list.add(t);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private ConversationThread parseConversation(JSONObject o) {
        ConversationThread t = new ConversationThread();
        t.setConversationId(o.optString("conversationId", ""));
        t.setConnectionId(o.optString("connectionId", ""));
        t.setDoctorUid(o.optString("doctorUid", ""));
        t.setDoctorName(o.optString("doctorName", ""));
        t.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        t.setPatientUid(o.optString("patientUid", ""));
        t.setPatientName(o.optString("patientName", ""));
        t.setPatientId(o.optString("patientId", ""));
        t.setConnectionStatus(o.optString("connectionStatus", DoctorPatientConnection.STATUS_ACCEPTED));
        t.setLastMessagePreview(o.optString("lastMessagePreview", ""));
        t.setLastMessageSenderUid(o.optString("lastMessageSenderUid", ""));
        t.setLastMessageTimestamp(o.optLong("lastMessageTimestamp", System.currentTimeMillis()));
        t.setUnreadByDoctor(o.optBoolean("unreadByDoctor", false));
        t.setUnreadByPatient(o.optBoolean("unreadByPatient", false));
        return t;
    }

    public void appendMessageLocal(String conversationId, ChatMessage m) {
        if (m == null) return;
        if (conversationId != null && (m.getConversationId() == null || m.getConversationId().isEmpty())) {
            m.setConversationId(conversationId);
        }
        appendMessageLocal(m);
    }

    public void appendMessageLocal(ChatMessage m) {
        if (m == null || m.getConversationId() == null) return;
        try {
            String key = KEY_MESSAGES_PREFIX + m.getConversationId();
            JSONArray arr = getJsonArray(key);
            JSONObject o = new JSONObject();
            o.put("messageId", nonNull(m.getMessageId()));
            o.put("conversationId", nonNull(m.getConversationId()));
            o.put("senderUid", nonNull(m.getSenderUid()));
            o.put("senderName", nonNull(m.getSenderName()));
            o.put("senderRole", nonNull(m.getSenderRole()));
            o.put("receiverUid", nonNull(m.getReceiverUid()));
            o.put("messageText", nonNull(m.getMessageText()));
            o.put("timestamp", m.getTimestamp());
            o.put("read", m.isRead());
            o.put("readAt", m.getReadAt());

            boolean updated = false;
            String msgId = nonNull(m.getMessageId());
            if (!msgId.isEmpty()) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject existing = arr.getJSONObject(i);
                    if (msgId.equals(existing.optString("messageId", ""))) {
                        arr.put(i, o);
                        updated = true;
                        break;
                    }
                }
            }
            if (!updated) {
                arr.put(o);
            }
            prefs.edit().putString(key, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<ChatMessage> getMessagesLocal(String conversationId) {
        return getMessagesLocal(conversationId, null);
    }

    public List<ChatMessage> getMessagesLocal(String conversationId, String currentViewerUid) {
        List<ChatMessage> list = new ArrayList<>();
        if (conversationId == null) return list;
        try {
            String key = KEY_MESSAGES_PREFIX + conversationId;
            JSONArray arr = getJsonArray(key);
            boolean modified = false;
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ChatMessage m = new ChatMessage();
                m.setMessageId(o.optString("messageId", ""));
                m.setConversationId(o.optString("conversationId", conversationId));
                m.setSenderUid(o.optString("senderUid", ""));
                m.setSenderName(o.optString("senderName", ""));
                m.setSenderRole(o.optString("senderRole", ""));
                m.setReceiverUid(o.optString("receiverUid", ""));
                m.setMessageText(o.optString("messageText", ""));
                m.setTimestamp(o.optLong("timestamp", now));
                boolean isRead = o.optBoolean("read", false);
                if (!isRead && currentViewerUid != null && !currentViewerUid.equals(m.getSenderUid())) {
                    isRead = true;
                    o.put("read", true);
                    o.put("readAt", now);
                    modified = true;
                }
                m.setRead(isRead);
                m.setReadAt(o.optLong("readAt", 0L));
                list.add(m);
            }
            if (modified) {
                prefs.edit().putString(key, arr.toString()).apply();
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void saveNotificationLocal(AppNotification n) {
        if (n == null || n.getNotificationId() == null) return;
        try {
            JSONArray arr = getJsonArray(KEY_NOTIFICATIONS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("notificationId", n.getNotificationId());
            o.put("recipientUid", nonNull(n.getRecipientUid()));
            o.put("senderUid", nonNull(n.getSenderUid()));
            o.put("senderName", nonNull(n.getSenderName()));
            o.put("type", nonNull(n.getType()));
            o.put("title", nonNull(n.getTitle()));
            o.put("body", nonNull(n.getBody()));
            o.put("referenceId", nonNull(n.getReferenceId()));
            o.put("read", n.isRead());
            o.put("createdAt", n.getCreatedAt());
            arr.put(o);
            prefs.edit().putString(KEY_NOTIFICATIONS_REGISTRY_JSON, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<AppNotification> getNotificationsForRecipientLocal(String recipientUid) {
        List<AppNotification> list = new ArrayList<>();
        if (recipientUid == null) return list;
        try {
            JSONArray arr = getJsonArray(KEY_NOTIFICATIONS_REGISTRY_JSON);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String rec = o.optString("recipientUid", "");
                if (recipientUid.equals(rec) || "ADMIN_BROADCAST".equals(rec)) {
                    AppNotification n = new AppNotification();
                    n.setNotificationId(o.optString("notificationId", ""));
                    n.setRecipientUid(rec);
                    n.setSenderUid(o.optString("senderUid", ""));
                    n.setSenderName(o.optString("senderName", ""));
                    n.setType(o.optString("type", ""));
                    n.setTitle(o.optString("title", ""));
                    n.setBody(o.optString("body", ""));
                    n.setReferenceId(o.optString("referenceId", ""));
                    n.setRead(o.optBoolean("read", false));
                    n.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
                    list.add(n);
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void markNotificationsReadLocal(String recipientUid) {
        if (recipientUid == null) return;
        try {
            JSONArray arr = getJsonArray(KEY_NOTIFICATIONS_REGISTRY_JSON);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (recipientUid.equals(o.optString("recipientUid", ""))) {
                    o.put("read", true);
                }
            }
            prefs.edit().putString(KEY_NOTIFICATIONS_REGISTRY_JSON, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public void saveAnnouncementLocal(HospitalAnnouncement a) {
        if (a == null || a.getId() == null) return;
        try {
            JSONArray arr = getJsonArray(KEY_ANNOUNCEMENTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("postId", a.getId());
            o.put("title", nonNull(a.getTitle()));
            o.put("content", nonNull(a.getContent()));
            o.put("category", nonNull(a.getCategory()));
            o.put("authorUid", nonNull(a.getAuthorUid()));
            o.put("authorName", nonNull(a.getAuthorName()));
            o.put("authorRole", nonNull(a.getAuthorRole()));
            o.put("createdAt", a.getCreatedAt());

            boolean exists = false;
            for (int i = 0; i < arr.length(); i++) {
                if (a.getId().equals(arr.getJSONObject(i).optString("postId", ""))) {
                    arr.put(i, o);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                arr.put(o);
            }
            prefs.edit().putString(KEY_ANNOUNCEMENTS_REGISTRY_JSON, arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<HospitalAnnouncement> getAnnouncementsLocal() {
        List<HospitalAnnouncement> list = new ArrayList<>();
        try {
            JSONArray arr = getJsonArray(KEY_ANNOUNCEMENTS_REGISTRY_JSON);
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                HospitalAnnouncement a = new HospitalAnnouncement(
                        o.optString("postId", ""),
                        o.optString("title", ""),
                        o.optString("content", ""),
                        o.optString("category", "HOSPITAL_NOTICE"),
                        o.optString("authorUid", ""),
                        o.optString("authorName", "Hospital Authority"),
                        o.optString("authorRole", "ADMIN")
                );
                a.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
                list.add(a);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    // =========================================================================
    // 7. PHASE 4: APPOINTMENTS LOCAL PERSISTENCE
    // =========================================================================
    public void saveAppointmentLocal(Appointment a) {
        if (a == null || a.getAppointmentId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_APPOINTMENTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("appointmentId", a.getAppointmentId());
            o.put("patientUid", nonNull(a.getPatientUid()));
            o.put("patientId", nonNull(a.getPatientId()));
            o.put("patientName", nonNull(a.getPatientName()));
            o.put("patientPhone", nonNull(a.getPatientPhone()));
            o.put("patientBloodGroup", nonNull(a.getPatientBloodGroup()));
            o.put("doctorUid", nonNull(a.getDoctorUid()));
            o.put("doctorId", nonNull(a.getDoctorId()));
            o.put("doctorName", nonNull(a.getDoctorName()));
            o.put("doctorSpecialty", nonNull(a.getDoctorSpecialty()));
            o.put("consultationFee", a.getConsultationFee());
            o.put("appointmentDate", nonNull(a.getAppointmentDate()));
            o.put("appointmentTime", nonNull(a.getAppointmentTime()));
            o.put("consultationType", nonNull(a.getConsultationType()));
            o.put("reason", nonNull(a.getReason()));
            o.put("patientNotes", nonNull(a.getPatientNotes()));
            o.put("doctorNotes", nonNull(a.getDoctorNotes()));
            o.put("cancellationReason", nonNull(a.getCancellationReason()));
            o.put("status", nonNull(a.getStatus()));
            o.put("scheduledTimestamp", a.getScheduledTimestamp());
            o.put("createdAt", a.getCreatedAt());
            o.put("updatedAt", a.getUpdatedAt());
            root.put(a.getAppointmentId(), o);
            prefs.edit().putString(KEY_APPOINTMENTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public Appointment getAppointmentLocal(String appointmentId) {
        if (appointmentId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_APPOINTMENTS_REGISTRY_JSON);
            if (!root.has(appointmentId)) return null;
            return parseAppointment(root.getJSONObject(appointmentId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<Appointment> getAllAppointmentsLocal() {
        List<Appointment> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_APPOINTMENTS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Appointment a = parseAppointment(root.getJSONObject(k));
                if (a != null) list.add(a);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private Appointment parseAppointment(JSONObject o) {
        Appointment a = new Appointment();
        a.setAppointmentId(o.optString("appointmentId", ""));
        a.setPatientUid(o.optString("patientUid", ""));
        a.setPatientId(o.optString("patientId", ""));
        a.setPatientName(o.optString("patientName", ""));
        a.setPatientPhone(o.optString("patientPhone", ""));
        a.setPatientBloodGroup(o.optString("patientBloodGroup", ""));
        a.setDoctorUid(o.optString("doctorUid", ""));
        a.setDoctorId(o.optString("doctorId", ""));
        a.setDoctorName(o.optString("doctorName", ""));
        a.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        a.setConsultationFee(o.optDouble("consultationFee", 0.0));
        a.setAppointmentDate(o.optString("appointmentDate", ""));
        a.setAppointmentTime(o.optString("appointmentTime", ""));
        a.setConsultationType(o.optString("consultationType", Appointment.TYPE_IN_PERSON));
        a.setReason(o.optString("reason", ""));
        a.setPatientNotes(o.optString("patientNotes", ""));
        a.setDoctorNotes(o.optString("doctorNotes", ""));
        a.setCancellationReason(o.optString("cancellationReason", ""));
        a.setStatus(o.optString("status", Appointment.STATUS_PENDING));
        a.setScheduledTimestamp(o.optLong("scheduledTimestamp", 0));
        a.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        a.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return a;
    }

    // =========================================================================
    // 8. PHASE 4: DIGITAL PRESCRIPTIONS LOCAL PERSISTENCE
    // =========================================================================
    public void savePrescriptionLocal(DigitalPrescription p) {
        if (p == null || p.getPrescriptionId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_PRESCRIPTIONS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("prescriptionId", p.getPrescriptionId());
            o.put("appointmentId", nonNull(p.getAppointmentId()));
            o.put("patientUid", nonNull(p.getPatientUid()));
            o.put("patientId", nonNull(p.getPatientId()));
            o.put("patientName", nonNull(p.getPatientName()));
            o.put("patientAgeOrDob", nonNull(p.getPatientAgeOrDob()));
            o.put("patientGender", nonNull(p.getPatientGender()));
            o.put("patientBloodGroup", nonNull(p.getPatientBloodGroup()));
            o.put("doctorUid", nonNull(p.getDoctorUid()));
            o.put("doctorId", nonNull(p.getDoctorId()));
            o.put("doctorName", nonNull(p.getDoctorName()));
            o.put("doctorSpecialty", nonNull(p.getDoctorSpecialty()));
            o.put("doctorLicense", nonNull(p.getDoctorLicense()));
            o.put("doctorDepartment", nonNull(p.getDoctorDepartment()));
            o.put("diagnosis", nonNull(p.getDiagnosis()));
            o.put("clinicalObservations", nonNull(p.getClinicalObservations()));
            o.put("adviceInstructions", nonNull(p.getAdviceInstructions()));
            o.put("followUpDate", nonNull(p.getFollowUpDate()));
            o.put("createdAt", p.getCreatedAt());
            o.put("updatedAt", p.getUpdatedAt());

            JSONArray medsArr = new JSONArray();
            if (p.getMedicines() != null) {
                for (PrescriptionMedicine m : p.getMedicines()) {
                    if (m == null) continue;
                    JSONObject mo = new JSONObject();
                    mo.put("medicineName", nonNull(m.getMedicineName()));
                    mo.put("dosage", nonNull(m.getDosage()));
                    mo.put("frequency", nonNull(m.getFrequency()));
                    mo.put("duration", nonNull(m.getDuration()));
                    mo.put("mealInstruction", nonNull(m.getMealInstruction()));
                    mo.put("instructions", nonNull(m.getInstructions()));
                    medsArr.put(mo);
                }
            }
            o.put("medicines", medsArr);

            root.put(p.getPrescriptionId(), o);
            prefs.edit().putString(KEY_PRESCRIPTIONS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public DigitalPrescription getPrescriptionLocal(String prescriptionId) {
        if (prescriptionId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_PRESCRIPTIONS_REGISTRY_JSON);
            if (!root.has(prescriptionId)) return null;
            return parsePrescription(root.getJSONObject(prescriptionId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<DigitalPrescription> getAllPrescriptionsLocal() {
        List<DigitalPrescription> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_PRESCRIPTIONS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                DigitalPrescription p = parsePrescription(root.getJSONObject(k));
                if (p != null) list.add(p);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private DigitalPrescription parsePrescription(JSONObject o) {
        DigitalPrescription p = new DigitalPrescription();
        p.setPrescriptionId(o.optString("prescriptionId", ""));
        p.setAppointmentId(o.optString("appointmentId", ""));
        p.setPatientUid(o.optString("patientUid", ""));
        p.setPatientId(o.optString("patientId", ""));
        p.setPatientName(o.optString("patientName", ""));
        p.setPatientAgeOrDob(o.optString("patientAgeOrDob", ""));
        p.setPatientGender(o.optString("patientGender", ""));
        p.setPatientBloodGroup(o.optString("patientBloodGroup", ""));
        p.setDoctorUid(o.optString("doctorUid", ""));
        p.setDoctorId(o.optString("doctorId", ""));
        p.setDoctorName(o.optString("doctorName", ""));
        p.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        p.setDoctorLicense(o.optString("doctorLicense", ""));
        p.setDoctorDepartment(o.optString("doctorDepartment", ""));
        p.setDiagnosis(o.optString("diagnosis", ""));
        p.setClinicalObservations(o.optString("clinicalObservations", ""));
        p.setAdviceInstructions(o.optString("adviceInstructions", ""));
        p.setFollowUpDate(o.optString("followUpDate", ""));
        p.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        p.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));

        JSONArray medsArr = o.optJSONArray("medicines");
        List<PrescriptionMedicine> meds = new ArrayList<>();
        if (medsArr != null) {
            for (int i = 0; i < medsArr.length(); i++) {
                JSONObject mo = medsArr.optJSONObject(i);
                if (mo != null) {
                    PrescriptionMedicine m = new PrescriptionMedicine(
                            mo.optString("medicineName", ""),
                            mo.optString("dosage", ""),
                            mo.optString("frequency", ""),
                            mo.optString("duration", ""),
                            mo.optString("mealInstruction", "After Meal"),
                            mo.optString("instructions", "")
                    );
                    meds.add(m);
                }
            }
        }
        p.setMedicines(meds);
        return p;
    }

    // =========================================================================
    // 9. PHASE 4: LABORATORY REQUESTS LOCAL PERSISTENCE
    // =========================================================================
    public void saveLabRequestLocal(LaboratoryRequest r) {
        if (r == null || r.getRequestId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_LAB_REQUESTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("requestId", r.getRequestId());
            o.put("appointmentId", nonNull(r.getAppointmentId()));
            o.put("patientUid", nonNull(r.getPatientUid()));
            o.put("patientId", nonNull(r.getPatientId()));
            o.put("patientName", nonNull(r.getPatientName()));
            o.put("patientPhone", nonNull(r.getPatientPhone()));
            o.put("patientBloodGroup", nonNull(r.getPatientBloodGroup()));
            o.put("doctorUid", nonNull(r.getDoctorUid()));
            o.put("doctorId", nonNull(r.getDoctorId()));
            o.put("doctorName", nonNull(r.getDoctorName()));
            o.put("doctorSpecialty", nonNull(r.getDoctorSpecialty()));
            o.put("testName", nonNull(r.getTestName()));
            o.put("testCategory", nonNull(r.getTestCategory()));
            o.put("clinicalIndication", nonNull(r.getClinicalIndication()));
            o.put("urgency", nonNull(r.getUrgency()));
            o.put("status", nonNull(r.getStatus()));
            o.put("reportFileUrl", nonNull(r.getReportFileUrl()));
            o.put("reportFileName", nonNull(r.getReportFileName()));
            o.put("reportFileType", nonNull(r.getReportFileType()));
            o.put("reportFindingsSummary", nonNull(r.getReportFindingsSummary()));
            o.put("reportUploadedAt", r.getReportUploadedAt());
            o.put("doctorReviewNotes", nonNull(r.getDoctorReviewNotes()));
            o.put("doctorReviewedAt", r.getDoctorReviewedAt());
            o.put("createdAt", r.getCreatedAt());
            o.put("updatedAt", r.getUpdatedAt());
            root.put(r.getRequestId(), o);
            prefs.edit().putString(KEY_LAB_REQUESTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public LaboratoryRequest getLabRequestLocal(String requestId) {
        if (requestId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_LAB_REQUESTS_REGISTRY_JSON);
            if (!root.has(requestId)) return null;
            return parseLabRequest(root.getJSONObject(requestId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<LaboratoryRequest> getAllLabRequestsLocal() {
        List<LaboratoryRequest> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_LAB_REQUESTS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                LaboratoryRequest r = parseLabRequest(root.getJSONObject(k));
                if (r != null) list.add(r);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private LaboratoryRequest parseLabRequest(JSONObject o) {
        LaboratoryRequest r = new LaboratoryRequest();
        r.setRequestId(o.optString("requestId", ""));
        r.setAppointmentId(o.optString("appointmentId", ""));
        r.setPatientUid(o.optString("patientUid", ""));
        r.setPatientId(o.optString("patientId", ""));
        r.setPatientName(o.optString("patientName", ""));
        r.setPatientPhone(o.optString("patientPhone", ""));
        r.setPatientBloodGroup(o.optString("patientBloodGroup", ""));
        r.setDoctorUid(o.optString("doctorUid", ""));
        r.setDoctorId(o.optString("doctorId", ""));
        r.setDoctorName(o.optString("doctorName", ""));
        r.setDoctorSpecialty(o.optString("doctorSpecialty", ""));
        r.setTestName(o.optString("testName", ""));
        r.setTestCategory(o.optString("testCategory", ""));
        r.setClinicalIndication(o.optString("clinicalIndication", ""));
        r.setUrgency(o.optString("urgency", LaboratoryRequest.URGENCY_ROUTINE));
        r.setStatus(o.optString("status", LaboratoryRequest.STATUS_REQUESTED));
        r.setReportFileUrl(o.optString("reportFileUrl", ""));
        r.setReportFileName(o.optString("reportFileName", ""));
        r.setReportFileType(o.optString("reportFileType", "PDF"));
        r.setReportFindingsSummary(o.optString("reportFindingsSummary", ""));
        r.setReportUploadedAt(o.optLong("reportUploadedAt", 0));
        r.setDoctorReviewNotes(o.optString("doctorReviewNotes", ""));
        r.setDoctorReviewedAt(o.optLong("doctorReviewedAt", 0));
        r.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        r.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return r;
    }

    // =========================================================================
    // 10. PHASE 4: UNIFIED CALENDAR EVENTS LOCAL PERSISTENCE
    // =========================================================================
    public void saveCalendarEventLocal(CalendarEvent e) {
        if (e == null || e.getEventId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_CALENDAR_EVENTS_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("eventId", e.getEventId());
            o.put("title", nonNull(e.getTitle()));
            o.put("description", nonNull(e.getDescription()));
            o.put("eventType", nonNull(e.getEventType()));
            o.put("eventDate", nonNull(e.getEventDate()));
            o.put("eventTime", nonNull(e.getEventTime()));
            o.put("timestamp", e.getTimestamp());
            o.put("patientUid", nonNull(e.getPatientUid()));
            o.put("patientName", nonNull(e.getPatientName()));
            o.put("patientId", nonNull(e.getPatientId()));
            o.put("doctorUid", nonNull(e.getDoctorUid()));
            o.put("doctorName", nonNull(e.getDoctorName()));
            o.put("doctorId", nonNull(e.getDoctorId()));
            o.put("nurseUid", nonNull(e.getNurseUid()));
            o.put("nurseName", nonNull(e.getNurseName()));
            o.put("departmentOrLocation", nonNull(e.getDepartmentOrLocation()));
            o.put("status", nonNull(e.getStatus()));
            o.put("referenceId", nonNull(e.getReferenceId()));
            o.put("roleVisibility", nonNull(e.getRoleVisibility()));
            o.put("createdAt", e.getCreatedAt());
            o.put("updatedAt", e.getUpdatedAt());
            root.put(e.getEventId(), o);
            prefs.edit().putString(KEY_CALENDAR_EVENTS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public CalendarEvent getCalendarEventLocal(String eventId) {
        if (eventId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_CALENDAR_EVENTS_REGISTRY_JSON);
            if (!root.has(eventId)) return null;
            return parseCalendarEvent(root.getJSONObject(eventId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<CalendarEvent> getAllCalendarEventsLocal() {
        List<CalendarEvent> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_CALENDAR_EVENTS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                CalendarEvent e = parseCalendarEvent(root.getJSONObject(k));
                if (e != null) list.add(e);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private CalendarEvent parseCalendarEvent(JSONObject o) {
        CalendarEvent e = new CalendarEvent();
        e.setEventId(o.optString("eventId", ""));
        e.setTitle(o.optString("title", ""));
        e.setDescription(o.optString("description", ""));
        e.setEventType(o.optString("eventType", CalendarEvent.TYPE_APPOINTMENT));
        e.setEventDate(o.optString("eventDate", ""));
        e.setEventTime(o.optString("eventTime", ""));
        e.setTimestamp(o.optLong("timestamp", System.currentTimeMillis()));
        e.setPatientUid(o.optString("patientUid", ""));
        e.setPatientName(o.optString("patientName", ""));
        e.setPatientId(o.optString("patientId", ""));
        e.setDoctorUid(o.optString("doctorUid", ""));
        e.setDoctorName(o.optString("doctorName", ""));
        e.setDoctorId(o.optString("doctorId", ""));
        e.setNurseUid(o.optString("nurseUid", ""));
        e.setNurseName(o.optString("nurseName", ""));
        e.setDepartmentOrLocation(o.optString("departmentOrLocation", ""));
        e.setStatus(o.optString("status", CalendarEvent.STATUS_SCHEDULED));
        e.setReferenceId(o.optString("referenceId", ""));
        e.setRoleVisibility(o.optString("roleVisibility", "ALL"));
        e.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        e.setUpdatedAt(o.optLong("updatedAt", System.currentTimeMillis()));
        return e;
    }

    private JSONObject getJsonObject(String key) {
        String raw = prefs.getString(key, null);
        if (raw == null || raw.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private JSONArray getJsonArray(String key) {
        String raw = prefs.getString(key, null);
        if (raw == null || raw.isEmpty()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private String nonNull(String s) {
        return s != null ? s : "";
    }

    private String normalizeEmailKey(String email) {
        return email.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
