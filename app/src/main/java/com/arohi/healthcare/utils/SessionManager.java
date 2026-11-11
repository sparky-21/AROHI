package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

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
import com.arohi.healthcare.models.EmergencyCardProfile;
import com.arohi.healthcare.models.FamilyMember;
import com.arohi.healthcare.models.HospitalLocation;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.models.HospitalMembership;
import com.arohi.healthcare.models.SuperAdminAuthorization;
import com.arohi.healthcare.models.Medicine;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.PharmacyOrderItem;
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
    private static final String KEY_MEDICINES_REGISTRY_JSON = "registry_medicines_json";
    private static final String KEY_PHARMACY_ORDERS_REGISTRY_JSON = "registry_pharmacy_orders_json";
    private static final String KEY_EMERGENCY_PROFILES_REGISTRY_JSON = "registry_emergency_profiles_json";
    private static final String KEY_FAMILY_MEMBERS_REGISTRY_JSON = "registry_family_members_json";
    public static final String KEY_SELECTED_HOSPITAL_ID = "selected_hospital_id";
    public static final String KEY_SELECTED_HOSPITAL_CODE = "selected_hospital_code";
    public static final String KEY_SELECTED_HOSPITAL_NAME = "selected_hospital_name";
    public static final String KEY_HOSPITALS_REGISTRY_JSON = "registry_hospitals_json";
    public static final String KEY_MEMBERSHIPS_REGISTRY_JSON = "registry_memberships_json";
    public static final String KEY_SUPER_ADMINS_REGISTRY_JSON = "registry_super_admins_json";

    public static final String DEFAULT_HOSPITAL_ID = "hosp_arohi_central";
    public static final String DEFAULT_HOSPITAL_CODE = "AROHI-HQ";
    public static final String DEFAULT_HOSPITAL_NAME = "AROHI Central Research & General Hospital";

    // Phase 10: Appearance, Language & Settings Keys
    public static final String KEY_THEME_MODE = "app_theme_mode";
    public static final String KEY_APP_LANGUAGE = "app_language";

    public static final int THEME_MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // -1
    public static final int THEME_MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;             // 1
    public static final int THEME_MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;             // 2

    public static final String KEY_NOTIF_APPOINTMENTS = "pref_notif_appointments";
    public static final String KEY_NOTIF_MESSAGES = "pref_notif_messages";
    public static final String KEY_NOTIF_PRESCRIPTIONS = "pref_notif_prescriptions";
    public static final String KEY_NOTIF_LAB_REPORTS = "pref_notif_lab_reports";
    public static final String KEY_NOTIF_ANNOUNCEMENTS = "pref_notif_announcements";
    public static final String KEY_NOTIF_EMERGENCY = "pref_notif_emergency";

    public static final String KEY_PRIVACY_SHARE_EMERGENCY = "pref_privacy_share_emergency";
    public static final String KEY_PRIVACY_PUBLIC_COMMUNITY = "pref_privacy_public_community";
    public static final String KEY_PRIVACY_RECORD_SHARING = "pref_privacy_record_sharing";

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
            obj.put("hospitalId", nonNull(account.getHospitalId()));
            obj.put("hospitalCode", nonNull(account.getHospitalCode()));
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
        acc.setHospitalId(o.optString("hospitalId", DEFAULT_HOSPITAL_ID));
        acc.setHospitalCode(o.optString("hospitalCode", DEFAULT_HOSPITAL_CODE));
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

    
    // =========================================================================
    // 11. PHASE 5: SMART E-PHARMACY LOCAL PERSISTENCE & CATALOGUE SEED
    // =========================================================================

    public void saveMedicineLocal(Medicine m) {
        if (m == null || m.getMedicineId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_MEDICINES_REGISTRY_JSON);
            JSONObject o = new JSONObject();
            o.put("medicineId", m.getMedicineId());
            o.put("name", nonNull(m.getName()));
            o.put("genericName", nonNull(m.getGenericName()));
            o.put("category", nonNull(m.getCategory()));
            o.put("strength", nonNull(m.getStrength()));
            o.put("dosageForm", nonNull(m.getDosageForm()));
            o.put("unitPrice", m.getUnitPrice());
            o.put("stockQuantity", m.getStockQuantity());
            o.put("requiresPrescription", m.isRequiresPrescription());
            o.put("manufacturer", nonNull(m.getManufacturer()));
            o.put("description", nonNull(m.getDescription()));
            o.put("sideEffects", nonNull(m.getSideEffects()));
            o.put("dosageInstructions", nonNull(m.getDosageInstructions()));
            o.put("createdAtTimestamp", m.getCreatedAtTimestamp());

            root.put(m.getMedicineId(), o);
            prefs.edit().putString(KEY_MEDICINES_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public Medicine getMedicineLocal(String medicineId) {
        if (medicineId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_MEDICINES_REGISTRY_JSON);
            if (!root.has(medicineId)) return null;
            return parseMedicine(root.getJSONObject(medicineId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<Medicine> getAllMedicinesLocal() {
        List<Medicine> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_MEDICINES_REGISTRY_JSON);
            if (root.length() == 0) {
                seedDefaultPharmacyCatalogue();
                root = getJsonObject(KEY_MEDICINES_REGISTRY_JSON);
            }
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Medicine m = parseMedicine(root.getJSONObject(k));
                if (m != null) list.add(m);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public void updateMedicineStockLocal(String medicineId, int newStock) {
        Medicine m = getMedicineLocal(medicineId);
        if (m != null) {
            m.setStockQuantity(Math.max(0, newStock));
            saveMedicineLocal(m);
        }
    }

    private void seedDefaultPharmacyCatalogue() {
        List<Medicine> defaults = new ArrayList<>();
        defaults.add(new Medicine("med_001", "Paracetamol 500mg", "Paracetamol", Medicine.CATEGORY_ANALGESICS,
                "500 mg", "Tablet", 12.0, 250, false, "Square Pharmaceuticals",
                "Fast fever reduction and mild-to-moderate pain relief.", "Safe when used as directed.", "Take 1-2 tablets after food every 6 hours."));
        defaults.add(new Medicine("med_002", "Amoxicillin 500mg", "Amoxicillin Trihydrate", Medicine.CATEGORY_ANTIBIOTICS,
                "500 mg", "Capsule", 45.0, 120, true, "Beximco Pharma",
                "Broad-spectrum antibiotic for bacterial respiratory & ear infections.", "Mild nausea, rash in penicillin-allergic patients.", "Take 1 capsule 3 times daily for 7 days."));
        defaults.add(new Medicine("med_003", "Atorvastatin 20mg", "Atorvastatin Calcium", Medicine.CATEGORY_CARDIOVASCULAR,
                "20 mg", "Tablet", 35.0, 80, true, "Incepta Pharmaceuticals",
                "Lowers LDL cholesterol and reduces cardiovascular risk.", "Muscle fatigue, mild headache.", "Take 1 tablet daily at bedtime."));
        defaults.add(new Medicine("med_004", "Omeprazole 20mg", "Omeprazole", Medicine.CATEGORY_GASTROINTESTINAL,
                "20 mg", "Capsule", 18.0, 180, false, "Renata Limited",
                "Proton-pump inhibitor for gastric acidity, reflux, and heartburn.", "Headache, mild stomach discomfort.", "Take 1 capsule 30 minutes before breakfast."));
        defaults.add(new Medicine("med_005", "Cetirizine 10mg", "Cetirizine Hydrochloride", Medicine.CATEGORY_RESPIRATORY,
                "10 mg", "Tablet", 10.0, 300, false, "Square Pharmaceuticals",
                "Antihistamine for allergic rhinitis, sneezing, hives, and itchy skin.", "Mild drowsiness in some individuals.", "Take 1 tablet daily in the evening."));
        defaults.add(new Medicine("med_006", "Metformin 500mg", "Metformin Hydrochloride", Medicine.CATEGORY_DIABETES,
                "500 mg", "Tablet", 22.0, 160, true, "Eskayef Pharmaceuticals",
                "First-line oral antidiabetic therapy for glycemic control in Type 2 Diabetes.", "Gastrointestinal upset, metallic taste.", "Take 1 tablet with or after meals twice daily."));
        defaults.add(new Medicine("med_007", "Salbutamol Inhaler 100mcg", "Salbutamol Sulfate", Medicine.CATEGORY_RESPIRATORY,
                "100 mcg/dose", "Inhaler", 220.0, 45, true, "Beximco Pharma",
                "Fast-acting bronchodilator for rapid relief of asthma and bronchospasm.", "Transient fine tremor, palpitations.", "Inhale 1-2 puffs as required for acute shortness of breath."));
        defaults.add(new Medicine("med_008", "Azithromycin 500mg", "Azithromycin", Medicine.CATEGORY_ANTIBIOTICS,
                "500 mg", "Tablet", 95.0, 60, true, "Incepta Pharmaceuticals",
                "Macrolide antibiotic for throat, chest, and soft tissue infections.", "Abdominal cramps, loose stools.", "Take 1 tablet daily 1 hour before or 2 hours after meals for 3-5 days."));
        defaults.add(new Medicine("med_009", "Amlodipine 5mg", "Amlodipine Besylate", Medicine.CATEGORY_CARDIOVASCULAR,
                "5 mg", "Tablet", 25.0, 95, true, "Square Pharmaceuticals",
                "Calcium channel blocker for hypertension and chronic stable angina.", "Peripheral ankle swelling, flushing.", "Take 1 tablet daily with or without food."));
        defaults.add(new Medicine("med_010", "Calcium 500mg + Vitamin D3", "Calcium Carbonate + Cholecalciferol", Medicine.CATEGORY_VITAMINS,
                "500 mg + 200 IU", "Tablet", 15.0, 200, false, "Healthcare Pharma",
                "Strengthens bone density, supports calcium homeostasis.", "Constipation if taken with insufficient water.", "Take 1 tablet twice daily with food."));
        defaults.add(new Medicine("med_011", "Hydrocortisone Cream 1%", "Hydrocortisone", Medicine.CATEGORY_DERMATOLOGY,
                "1% w/w", "Tube", 60.0, 50, false, "Renata Limited",
                "Topical corticosteroid for inflammatory dermatitis, eczema, and rashes.", "Skin thinning if overused.", "Apply thinly to affected area 1-2 times daily."));

        for (Medicine m : defaults) {
            saveMedicineLocal(m);
        }
    }

    private Medicine parseMedicine(JSONObject o) {
        Medicine m = new Medicine();
        m.setMedicineId(o.optString("medicineId", ""));
        m.setName(o.optString("name", ""));
        m.setGenericName(o.optString("genericName", ""));
        m.setCategory(o.optString("category", ""));
        m.setStrength(o.optString("strength", ""));
        m.setDosageForm(o.optString("dosageForm", "Tablet"));
        m.setUnitPrice(o.optDouble("unitPrice", 0.0));
        m.setStockQuantity(o.optInt("stockQuantity", 0));
        m.setRequiresPrescription(o.optBoolean("requiresPrescription", false));
        m.setManufacturer(o.optString("manufacturer", ""));
        m.setDescription(o.optString("description", ""));
        m.setSideEffects(o.optString("sideEffects", ""));
        m.setDosageInstructions(o.optString("dosageInstructions", ""));
        m.setCreatedAtTimestamp(o.optLong("createdAtTimestamp", System.currentTimeMillis()));
        return m;
    }

    // =========================================================================
    // 12. PHASE 5: PHARMACY ORDERS LOCAL PERSISTENCE
    // =========================================================================

    public void savePharmacyOrderLocal(PharmacyOrder o) {
        if (o == null || o.getOrderId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_PHARMACY_ORDERS_REGISTRY_JSON);
            JSONObject jo = new JSONObject();
            jo.put("orderId", o.getOrderId());
            jo.put("patientUid", nonNull(o.getPatientUid()));
            jo.put("patientArohiId", nonNull(o.getPatientArohiId()));
            jo.put("patientName", nonNull(o.getPatientName()));
            jo.put("patientPhone", nonNull(o.getPatientPhone()));
            jo.put("prescriptionId", nonNull(o.getPrescriptionId()));
            jo.put("prescriptionDoctorName", nonNull(o.getPrescriptionDoctorName()));

            JSONArray itemsArr = new JSONArray();
            if (o.getItems() != null) {
                for (PharmacyOrderItem item : o.getItems()) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("medicineId", nonNull(item.getMedicineId()));
                    itemObj.put("medicineName", nonNull(item.getMedicineName()));
                    itemObj.put("strength", nonNull(item.getStrength()));
                    itemObj.put("quantity", item.getQuantity());
                    itemObj.put("unitPrice", item.getUnitPrice());
                    itemObj.put("subtotal", item.getSubtotal());
                    itemsArr.put(itemObj);
                }
            }
            jo.put("items", itemsArr);
            jo.put("totalAmount", o.getTotalAmount());
            jo.put("deliveryType", nonNull(o.getDeliveryType()));
            jo.put("deliveryAddress", nonNull(o.getDeliveryAddress()));
            jo.put("status", nonNull(o.getStatus()));
            jo.put("orderDate", nonNull(o.getOrderDate()));
            jo.put("createdAtTimestamp", o.getCreatedAtTimestamp());
            jo.put("pharmacistNotes", nonNull(o.getPharmacistNotes()));
            jo.put("verifiedByStaffUid", nonNull(o.getVerifiedByStaffUid()));

            root.put(o.getOrderId(), jo);
            prefs.edit().putString(KEY_PHARMACY_ORDERS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public PharmacyOrder getPharmacyOrderLocal(String orderId) {
        if (orderId == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_PHARMACY_ORDERS_REGISTRY_JSON);
            if (!root.has(orderId)) return null;
            return parsePharmacyOrder(root.getJSONObject(orderId));
        } catch (JSONException e) {
            return null;
        }
    }

    public List<PharmacyOrder> getAllPharmacyOrdersLocal() {
        List<PharmacyOrder> list = new ArrayList<>();
        try {
            JSONObject root = getJsonObject(KEY_PHARMACY_ORDERS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                PharmacyOrder o = parsePharmacyOrder(root.getJSONObject(k));
                if (o != null) list.add(o);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public List<PharmacyOrder> getPatientPharmacyOrdersLocal(String patientUid) {
        List<PharmacyOrder> list = new ArrayList<>();
        if (patientUid == null) return list;
        for (PharmacyOrder o : getAllPharmacyOrdersLocal()) {
            if (patientUid.equals(o.getPatientUid())) {
                list.add(o);
            }
        }
        return list;
    }

    public void updatePharmacyOrderStatusLocal(String orderId, String newStatus, String notes, String staffUid) {
        PharmacyOrder o = getPharmacyOrderLocal(orderId);
        if (o != null) {
            o.setStatus(newStatus);
            if (notes != null) o.setPharmacistNotes(notes);
            if (staffUid != null) o.setVerifiedByStaffUid(staffUid);
            savePharmacyOrderLocal(o);
        }
    }

    private PharmacyOrder parsePharmacyOrder(JSONObject jo) {
        PharmacyOrder o = new PharmacyOrder();
        o.setOrderId(jo.optString("orderId", ""));
        o.setPatientUid(jo.optString("patientUid", ""));
        o.setPatientArohiId(jo.optString("patientArohiId", ""));
        o.setPatientName(jo.optString("patientName", ""));
        o.setPatientPhone(jo.optString("patientPhone", ""));
        o.setPrescriptionId(jo.optString("prescriptionId", ""));
        o.setPrescriptionDoctorName(jo.optString("prescriptionDoctorName", ""));

        List<PharmacyOrderItem> items = new ArrayList<>();
        JSONArray arr = jo.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject itemObj = arr.optJSONObject(i);
                if (itemObj != null) {
                    PharmacyOrderItem item = new PharmacyOrderItem(
                            itemObj.optString("medicineId", ""),
                            itemObj.optString("medicineName", ""),
                            itemObj.optString("strength", ""),
                            itemObj.optInt("quantity", 1),
                            itemObj.optDouble("unitPrice", 0.0)
                    );
                    items.add(item);
                }
            }
        }
        o.setItems(items);
        o.setTotalAmount(jo.optDouble("totalAmount", 0.0));
        o.setDeliveryType(jo.optString("deliveryType", PharmacyOrder.DELIVERY_HOSPITAL_PICKUP));
        o.setDeliveryAddress(jo.optString("deliveryAddress", ""));
        o.setStatus(jo.optString("status", PharmacyOrder.STATUS_RECEIVED));
        o.setOrderDate(jo.optString("orderDate", ""));
        o.setCreatedAtTimestamp(jo.optLong("createdAtTimestamp", System.currentTimeMillis()));
        o.setPharmacistNotes(jo.optString("pharmacistNotes", ""));
        o.setVerifiedByStaffUid(jo.optString("verifiedByStaffUid", ""));
        return o;
    }

    // =========================================================================
    // 13. PHASE 5: EMERGENCY HEALTH CARD LOCAL PERSISTENCE
    // =========================================================================

    public void saveEmergencyCardLocal(EmergencyCardProfile p) {
        if (p == null || p.getPatientUid() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_EMERGENCY_PROFILES_REGISTRY_JSON);
            JSONObject jo = new JSONObject();
            jo.put("patientUid", p.getPatientUid());
            jo.put("arohiId", nonNull(p.getArohiId()));
            jo.put("fullName", nonNull(p.getFullName()));
            jo.put("dateOfBirth", nonNull(p.getDateOfBirth()));
            jo.put("gender", nonNull(p.getGender()));
            jo.put("bloodGroup", nonNull(p.getBloodGroup()));
            jo.put("primaryPhone", nonNull(p.getPrimaryPhone()));
            jo.put("knownAllergies", nonNull(p.getKnownAllergies()));
            jo.put("chronicConditions", nonNull(p.getChronicConditions()));
            jo.put("currentMedications", nonNull(p.getCurrentMedications()));
            jo.put("emergencyContactName", nonNull(p.getEmergencyContactName()));
            jo.put("emergencyContactPhone", nonNull(p.getEmergencyContactPhone()));
            jo.put("emergencyContactRelation", nonNull(p.getEmergencyContactRelation()));
            jo.put("organDonor", p.isOrganDonor());
            jo.put("insuranceProvider", nonNull(p.getInsuranceProvider()));
            jo.put("insurancePolicyNumber", nonNull(p.getInsurancePolicyNumber()));

            jo.put("showBloodGroup", p.isShowBloodGroup());
            jo.put("showAllergies", p.isShowAllergies());
            jo.put("showChronicConditions", p.isShowChronicConditions());
            jo.put("showCurrentMedications", p.isShowCurrentMedications());
            jo.put("showEmergencyContact", p.isShowEmergencyContact());
            jo.put("showOrganDonorStatus", p.isShowOrganDonorStatus());
            jo.put("lastUpdatedAt", p.getLastUpdatedAt());

            root.put(p.getPatientUid(), jo);
            prefs.edit().putString(KEY_EMERGENCY_PROFILES_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public EmergencyCardProfile getEmergencyCardLocal(String patientUid) {
        if (patientUid == null) return null;
        try {
            JSONObject root = getJsonObject(KEY_EMERGENCY_PROFILES_REGISTRY_JSON);
            if (!root.has(patientUid)) return null;
            JSONObject jo = root.getJSONObject(patientUid);
            EmergencyCardProfile p = new EmergencyCardProfile();
            p.setPatientUid(jo.optString("patientUid", patientUid));
            p.setArohiId(jo.optString("arohiId", ""));
            p.setFullName(jo.optString("fullName", ""));
            p.setDateOfBirth(jo.optString("dateOfBirth", ""));
            p.setGender(jo.optString("gender", ""));
            p.setBloodGroup(jo.optString("bloodGroup", ""));
            p.setPrimaryPhone(jo.optString("primaryPhone", ""));
            p.setKnownAllergies(jo.optString("knownAllergies", ""));
            p.setChronicConditions(jo.optString("chronicConditions", ""));
            p.setCurrentMedications(jo.optString("currentMedications", ""));
            p.setEmergencyContactName(jo.optString("emergencyContactName", ""));
            p.setEmergencyContactPhone(jo.optString("emergencyContactPhone", ""));
            p.setEmergencyContactRelation(jo.optString("emergencyContactRelation", ""));
            p.setOrganDonor(jo.optBoolean("organDonor", false));
            p.setInsuranceProvider(jo.optString("insuranceProvider", ""));
            p.setInsurancePolicyNumber(jo.optString("insurancePolicyNumber", ""));

            p.setShowBloodGroup(jo.optBoolean("showBloodGroup", true));
            p.setShowAllergies(jo.optBoolean("showAllergies", true));
            p.setShowChronicConditions(jo.optBoolean("showChronicConditions", true));
            p.setShowCurrentMedications(jo.optBoolean("showCurrentMedications", true));
            p.setShowEmergencyContact(jo.optBoolean("showEmergencyContact", true));
            p.setShowOrganDonorStatus(jo.optBoolean("showOrganDonorStatus", true));
            p.setLastUpdatedAt(jo.optLong("lastUpdatedAt", System.currentTimeMillis()));
            return p;
        } catch (JSONException e) {
            return null;
        }
    }

    // =========================================================================
    // 14. PHASE 5: FAMILY MEMBERS LOCAL PERSISTENCE
    // =========================================================================

    public void saveFamilyMemberLocal(FamilyMember f) {
        if (f == null || f.getMemberId() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_FAMILY_MEMBERS_REGISTRY_JSON);
            JSONObject jo = new JSONObject();
            jo.put("memberId", f.getMemberId());
            jo.put("primaryPatientUid", nonNull(f.getPrimaryPatientUid()));
            jo.put("fullName", nonNull(f.getFullName()));
            jo.put("relationship", nonNull(f.getRelationship()));
            jo.put("dateOfBirth", nonNull(f.getDateOfBirth()));
            jo.put("gender", nonNull(f.getGender()));
            jo.put("bloodGroup", nonNull(f.getBloodGroup()));
            jo.put("knownAllergies", nonNull(f.getKnownAllergies()));
            jo.put("emergencyPhone", nonNull(f.getEmergencyPhone()));
            jo.put("hasExplicitAuthorization", f.isHasExplicitAuthorization());
            jo.put("authorizationNotes", nonNull(f.getAuthorizationNotes()));
            jo.put("linkedPatientArohiId", nonNull(f.getLinkedPatientArohiId()));
            jo.put("createdAtTimestamp", f.getCreatedAtTimestamp());

            root.put(f.getMemberId(), jo);
            prefs.edit().putString(KEY_FAMILY_MEMBERS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public void deleteFamilyMemberLocal(String memberId) {
        if (memberId == null) return;
        try {
            JSONObject root = getJsonObject(KEY_FAMILY_MEMBERS_REGISTRY_JSON);
            if (root.has(memberId)) {
                root.remove(memberId);
                prefs.edit().putString(KEY_FAMILY_MEMBERS_REGISTRY_JSON, root.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public List<FamilyMember> getFamilyMembersForPatientLocal(String primaryPatientUid) {
        List<FamilyMember> list = new ArrayList<>();
        if (primaryPatientUid == null) return list;
        try {
            JSONObject root = getJsonObject(KEY_FAMILY_MEMBERS_REGISTRY_JSON);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject jo = root.getJSONObject(k);
                if (primaryPatientUid.equals(jo.optString("primaryPatientUid", ""))) {
                    FamilyMember f = new FamilyMember();
                    f.setMemberId(jo.optString("memberId", ""));
                    f.setPrimaryPatientUid(jo.optString("primaryPatientUid", ""));
                    f.setFullName(jo.optString("fullName", ""));
                    f.setRelationship(jo.optString("relationship", FamilyMember.RELATION_OTHER));
                    f.setDateOfBirth(jo.optString("dateOfBirth", ""));
                    f.setGender(jo.optString("gender", ""));
                    f.setBloodGroup(jo.optString("bloodGroup", ""));
                    f.setKnownAllergies(jo.optString("knownAllergies", ""));
                    f.setEmergencyPhone(jo.optString("emergencyPhone", ""));
                    f.setHasExplicitAuthorization(jo.optBoolean("hasExplicitAuthorization", true));
                    f.setAuthorizationNotes(jo.optString("authorizationNotes", ""));
                    f.setLinkedPatientArohiId(jo.optString("linkedPatientArohiId", ""));
                    f.setCreatedAtTimestamp(jo.optLong("createdAtTimestamp", System.currentTimeMillis()));
                    list.add(f);
                }
            }
        } catch (JSONException ignored) {}
        return list;
    }

    // =========================================================================
    // 15. PHASE 5: HOSPITAL MAP LOCATIONS DIRECTORY
    // =========================================================================

    public List<HospitalLocation> getAllHospitalLocationsLocal() {
        List<HospitalLocation> list = new ArrayList<>();
        // Ground Floor
        list.add(new HospitalLocation("loc_g01", "Emergency & Trauma Centre", HospitalLocation.TYPE_EMERGENCY,
                HospitalLocation.FLOOR_GROUND, "West Wing", "Room G-01",
                "24/7 emergency resuscitation, acute trauma response, and ambulance triage bay.",
                "24 Hours / 7 Days", "Ext: 101, 102",
                "Enter hospital Main Entrance -> Immediate turn left -> Follow red floor markers to Emergency Bay.",
                0, 0));
        list.add(new HospitalLocation("loc_g02", "Main Reception & Central Information", HospitalLocation.TYPE_RECEPTION,
                HospitalLocation.FLOOR_GROUND, "Central Atrium", "Main Lobby",
                "Patient registration, admission desk, consultation token counter, and hospital directory.",
                "24 Hours / 7 Days", "Ext: 100",
                "Located directly facing the Main Entrance Glass Doors in the central glass atrium.",
                1, 1));
        list.add(new HospitalLocation("loc_g03", "AROHI Smart E-Pharmacy (Central Dispensary)", HospitalLocation.TYPE_PHARMACY,
                HospitalLocation.FLOOR_GROUND, "East Wing", "Room G-12",
                "Full-service outpatient & emergency dispensary, prescription fulfillment, and home delivery dispatch.",
                "24 Hours / 7 Days", "Ext: 105",
                "From Main Reception -> Walk east past Elevator Bank A -> Room G-12 on the right.",
                0, 2));
        list.add(new HospitalLocation("loc_g04", "Radiology & Diagnostic Imaging", HospitalLocation.TYPE_RADIOLOGY,
                HospitalLocation.FLOOR_GROUND, "North Wing", "Room G-05",
                "Digital X-Ray, 128-Slice CT Scan, 3.0 Tesla MRI, and Ultrasound imaging suites.",
                "7:00 AM - 11:00 PM (Emergency 24/7)", "Ext: 108",
                "From Main Reception -> Head straight north -> Take corridor past Cafe to Radiology Wing.",
                2, 1));

        // Floor 1
        list.add(new HospitalLocation("loc_f1_01", "Clinical Pathology & Diagnostic Laboratory", HospitalLocation.TYPE_LABORATORY,
                HospitalLocation.FLOOR_1, "East Wing", "Room 108",
                "Automated biochemistry, hematology, immunology, and rapid sample collection center.",
                "6:30 AM - 10:00 PM", "Ext: 205",
                "Take Elevator A or Stairs to Floor 1 -> Turn right into East Wing -> Follow blue signs to Room 108.",
                0, 2));
        list.add(new HospitalLocation("loc_f1_02", "Specialist Outpatient Clinics (OPD)", HospitalLocation.TYPE_RECEPTION,
                HospitalLocation.FLOOR_1, "West Wing", "Suites 101-106",
                "Cardiology, Neurology, Pediatrics, General Medicine, and Orthopedic consultation chambers.",
                "8:00 AM - 9:00 PM", "Ext: 201",
                "Take Elevator A to Floor 1 -> Turn left -> Doctor consultation waiting lounge.",
                0, 0));

        // Floor 2
        list.add(new HospitalLocation("loc_f2_01", "Inpatient Medical Ward (Ward A & B)", HospitalLocation.TYPE_WARD,
                HospitalLocation.FLOOR_2, "East Wing", "Beds 201-224",
                "Male and Female general medical inpatient care with central nurses' station.",
                "Visiting Hours: 4:00 PM - 7:00 PM", "Ext: 301",
                "Take Elevator B to Floor 2 -> Enter East Wing double doors -> Report to Ward A Nurses' Desk.",
                0, 2));
        list.add(new HospitalLocation("loc_f2_02", "Surgical Inpatient Ward (Ward C & D)", HospitalLocation.TYPE_WARD,
                HospitalLocation.FLOOR_2, "West Wing", "Beds 225-248",
                "Pre-operative and post-operative surgical recovery ward with telemetry monitoring.",
                "Visiting Hours: 4:00 PM - 7:00 PM", "Ext: 302",
                "Take Elevator B to Floor 2 -> Enter West Wing -> Report to Ward C Nurses' Desk.",
                0, 0));

        // Floor 3
        list.add(new HospitalLocation("loc_f3_01", "Intensive Care Unit (ICU & CCU)", HospitalLocation.TYPE_ICU,
                HospitalLocation.FLOOR_3, "North Wing", "ICU Complex 301",
                "State-of-the-art critical care unit with dedicated ventilators, hemodynamic monitors, and 1:1 nurse ratio.",
                "Restricted Access (Authorized Staff & Scheduled Family Briefing)", "Ext: 401",
                "Take Elevator B to Floor 3 -> Follow sterile corridor north -> Intercom check-in at ICU Entrance.",
                2, 1));
        list.add(new HospitalLocation("loc_f3_02", "Operation Theatre Complex (OT 1 to 4)", HospitalLocation.TYPE_OT,
                HospitalLocation.FLOOR_3, "South Wing", "OT Suites 310-314",
                "Laminar airflow surgical suites for general, orthopedic, cardiac, and laparoscopic surgeries.",
                "Sterile Surgical Zone (Staff Only)", "Ext: 405",
                "Take Elevator B to Floor 3 -> Proceed south to Restricted Scrub Area.",
                0, 1));

        // Basement
        list.add(new HospitalLocation("loc_b01", "Central Sterile Supply & Biomedical Engineering", HospitalLocation.TYPE_RECEPTION,
                HospitalLocation.FLOOR_BASEMENT, "Central", "Suite B-02",
                "Surgical instrument sterilization, medical equipment maintenance, and facility engineering.",
                "24 Hours / 7 Days", "Ext: 005",
                "Take Elevator A or Service Lift to Basement level.",
                1, 1));

        return list;
    }

    // =========================================================================
    // PHASE 9: MULTI-TENANT HOSPITAL NETWORK & SUPER ADMIN ARCHITECTURE
    // =========================================================================

    public void setSelectedHospital(String hospitalId, String hospitalCode, String hospitalName) {
        prefs.edit()
                .putString(KEY_SELECTED_HOSPITAL_ID, hospitalId)
                .putString(KEY_SELECTED_HOSPITAL_CODE, hospitalCode != null ? hospitalCode.toUpperCase().trim() : "")
                .putString(KEY_SELECTED_HOSPITAL_NAME, hospitalName)
                .apply();
    }

    public String getSelectedHospitalId() {
        return prefs.getString(KEY_SELECTED_HOSPITAL_ID, DEFAULT_HOSPITAL_ID);
    }

    public String getSelectedHospitalCode() {
        return prefs.getString(KEY_SELECTED_HOSPITAL_CODE, DEFAULT_HOSPITAL_CODE);
    }

    public String getSelectedHospitalName() {
        return prefs.getString(KEY_SELECTED_HOSPITAL_NAME, DEFAULT_HOSPITAL_NAME);
    }

    public boolean hasSelectedHospital() {
        return prefs.contains(KEY_SELECTED_HOSPITAL_ID);
    }

    public void clearSelectedHospital() {
        prefs.edit()
                .remove(KEY_SELECTED_HOSPITAL_ID)
                .remove(KEY_SELECTED_HOSPITAL_CODE)
                .remove(KEY_SELECTED_HOSPITAL_NAME)
                .apply();
    }

    public List<Hospital> getRegisteredHospitals() {
        List<Hospital> hospitals = new ArrayList<>();
        String json = prefs.getString(KEY_HOSPITALS_REGISTRY_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONObject root = new JSONObject(json);
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    Hospital h = Hospital.fromJson(root.getJSONObject(k));
                    if (h != null) {
                        hospitals.add(h);
                    }
                }
            } catch (JSONException ignored) {
            }
        }

        if (hospitals.isEmpty()) {
            hospitals = seedDefaultVerifiedHospitals();
            for (Hospital h : hospitals) {
                saveHospital(h);
            }
        } else {
            // Update coordinates for any legacy cached hospital without lat/lng
            List<Hospital> defaults = seedDefaultVerifiedHospitals();
            for (Hospital cached : hospitals) {
                if (cached.getLatitude() == 0.0 && cached.getLongitude() == 0.0) {
                    for (Hospital d : defaults) {
                        if (d.getHospitalId().equalsIgnoreCase(cached.getHospitalId()) ||
                                d.getHospitalCode().equalsIgnoreCase(cached.getHospitalCode())) {
                            cached.setLatitude(d.getLatitude());
                            cached.setLongitude(d.getLongitude());
                            saveHospital(cached);
                            break;
                        }
                    }
                }
            }
        }
        return hospitals;
    }

    public void saveHospital(Hospital hospital) {
        if (hospital == null || hospital.getHospitalId() == null || hospital.getHospitalId().trim().isEmpty()) {
            return;
        }
        try {
            JSONObject root = getJsonObject(KEY_HOSPITALS_REGISTRY_JSON);
            root.put(hospital.getHospitalId(), hospital.toJson());
            prefs.edit().putString(KEY_HOSPITALS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public Hospital findHospitalById(String hospitalId) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) return null;
        for (Hospital h : getRegisteredHospitals()) {
            if (hospitalId.equalsIgnoreCase(h.getHospitalId())) {
                return h;
            }
        }
        return null;
    }

    public Hospital findHospitalByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String normalized = code.trim().toUpperCase();
        for (Hospital h : getRegisteredHospitals()) {
            if (h.getHospitalCode() != null && normalized.equalsIgnoreCase(h.getHospitalCode())) {
                return h;
            }
        }
        return null;
    }

    public List<Hospital> searchHospitals(String query, String categoryFilter) {
        List<Hospital> all = getRegisteredHospitals();
        List<Hospital> results = new ArrayList<>();
        String q = query != null ? query.trim().toLowerCase() : "";

        for (Hospital h : all) {
            if ("VERIFIED".equalsIgnoreCase(categoryFilter) && !h.isVerified()) {
                continue;
            }
            if ("EMERGENCY".equalsIgnoreCase(categoryFilter) && (h.getEmergencyHotline() == null || h.getEmergencyHotline().isEmpty())) {
                continue;
            }
            if ("DHAKA".equalsIgnoreCase(categoryFilter) && (h.getCity() == null || !h.getCity().toLowerCase().contains("dhaka"))) {
                continue;
            }
            if ("CHITTAGONG".equalsIgnoreCase(categoryFilter) && (h.getCity() == null || !h.getCity().toLowerCase().contains("chittagong"))) {
                continue;
            }

            if (q.isEmpty()) {
                results.add(h);
                continue;
            }

            boolean matchesName = h.getHospitalName() != null && h.getHospitalName().toLowerCase().contains(q);
            boolean matchesCode = h.getHospitalCode() != null && h.getHospitalCode().toLowerCase().contains(q);
            boolean matchesCity = h.getCity() != null && h.getCity().toLowerCase().contains(q);
            boolean matchesAddress = h.getAddress() != null && h.getAddress().toLowerCase().contains(q);
            boolean matchesDept = false;
            if (h.getSupportedDepartments() != null) {
                for (String d : h.getSupportedDepartments()) {
                    if (d.toLowerCase().contains(q)) {
                        matchesDept = true;
                        break;
                    }
                }
            }

            if (matchesName || matchesCode || matchesCity || matchesAddress || matchesDept) {
                results.add(h);
            }
        }
        return results;
    }

    private List<Hospital> seedDefaultVerifiedHospitals() {
        List<Hospital> list = new ArrayList<>();

        // 1. Central Flagship Hospital
        List<String> arohiDepts = new ArrayList<>();
        arohiDepts.add("Cardiology");
        arohiDepts.add("Neurology");
        arohiDepts.add("Emergency & Trauma");
        arohiDepts.add("General Medicine");
        arohiDepts.add("Pediatrics");
        arohiDepts.add("Orthopedics");
        arohiDepts.add("Oncology");
        arohiDepts.add("ICU / Critical Care");
        arohiDepts.add("Diagnostic Imaging");
        list.add(new Hospital(
                DEFAULT_HOSPITAL_ID,
                DEFAULT_HOSPITAL_CODE,
                DEFAULT_HOSPITAL_NAME,
                "Plot 12, Health Sciences Avenue, Dhaka-1212",
                "Dhaka",
                "+880 1800-AROHI (27644)",
                "central@arohi.health",
                "+880 1800-27644",
                Hospital.STATUS_ACTIVE,
                500,
                arohiDepts,
                true,
                "Premier tertiary multi-specialty research and teaching healthcare facility within the AROHI Global Network.",
                23.7937,
                90.4043
        ));

        // 2. Square Hospital Dhaka
        List<String> squareDepts = new ArrayList<>();
        squareDepts.add("Cardiology");
        squareDepts.add("Gastroenterology");
        squareDepts.add("Critical Care");
        squareDepts.add("General Surgery");
        squareDepts.add("Emergency Medicine");
        list.add(new Hospital(
                "hosp_square_dhk",
                "SQUARE-01",
                "Square Hospital Dhaka",
                "18/F, West Panthapath, Dhaka-1205",
                "Dhaka",
                "+880 2 8159457",
                "info@squarehospital.com",
                "+880 1713-377775",
                Hospital.STATUS_ACTIVE,
                400,
                squareDepts,
                true,
                "Leading JCI-accredited tertiary care hospital delivering world-class comprehensive clinical solutions.",
                23.7531,
                90.3816
        ));

        // 3. Apollo Imperial Hospital Chittagong
        List<String> apolloDepts = new ArrayList<>();
        apolloDepts.add("Emergency Medicine");
        apolloDepts.add("Cardiology");
        apolloDepts.add("Pediatrics");
        apolloDepts.add("Oncology");
        apolloDepts.add("Nephrology");
        list.add(new Hospital(
                "hosp_apollo_ctg",
                "APOLLO-CTG",
                "Apollo Imperial Hospital Chittagong",
                "Zakir Hossain Road, Pahartali, Chittagong",
                "Chittagong",
                "+880 31 658855",
                "info@apolloimperial.com",
                "+880 31 658850",
                Hospital.STATUS_ACTIVE,
                375,
                apolloDepts,
                true,
                "State-of-the-art multi-disciplinary hospital serving the greater Chittagong coastal division.",
                22.3601,
                91.8016
        ));

        // 4. United Hospital Gulshan
        List<String> unitedDepts = new ArrayList<>();
        unitedDepts.add("Cardiology");
        unitedDepts.add("Nephrology");
        unitedDepts.add("General Surgery");
        unitedDepts.add("Diagnostic Imaging");
        unitedDepts.add("Pathology");
        list.add(new Hospital(
                "hosp_united_dhk",
                "UNITED-01",
                "United Hospital Gulshan",
                "Plot 15, Road 71, Gulshan-2, Dhaka-1212",
                "Dhaka",
                "+880 2 8836000",
                "info@uhlbd.com",
                "+880 1914-001234",
                Hospital.STATUS_ACTIVE,
                450,
                unitedDepts,
                true,
                "Modern multi-specialty healthcare institution recognized for cardiology, renal transplants, and advanced diagnostics.",
                23.7978,
                90.4184
        ));

        // 5. Evercare Hospital Dhaka
        List<String> evercareDepts = new ArrayList<>();
        evercareDepts.add("Cardiology & Cardiothoracic");
        evercareDepts.add("Oncology");
        evercareDepts.add("Orthopedics & Joint Replacement");
        evercareDepts.add("Emergency & Trauma");
        evercareDepts.add("Pediatric ICU");
        list.add(new Hospital(
                "hosp_evercare_dhk",
                "EVERCARE-01",
                "Evercare Hospital Dhaka",
                "Plot 81, Block E, Bashundhara R/A, Dhaka-1229",
                "Dhaka",
                "+880 2 8431661",
                "info.dhaka@evercarebd.com",
                "+880 1713-047461",
                Hospital.STATUS_ACTIVE,
                425,
                evercareDepts,
                true,
                "Internationally accredited super-specialty tertiary care hospital with advanced clinical centers of excellence.",
                23.8103,
                90.4312
        ));

        return list;
    }

    // =========================================================================
    // HOSPITAL MEMBERSHIPS REGISTRY
    // =========================================================================

    public List<HospitalMembership> getUserMemberships(String userId) {
        List<HospitalMembership> list = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) return list;

        String json = prefs.getString(KEY_MEMBERSHIPS_REGISTRY_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONObject root = new JSONObject(json);
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    HospitalMembership m = HospitalMembership.fromJson(root.getJSONObject(k));
                    if (m != null && userId.equalsIgnoreCase(m.getUserId())) {
                        list.add(m);
                    }
                }
            } catch (JSONException ignored) {
            }
        }

        if (list.isEmpty()) {
            UserAccount acc = getRegisteredAccountByUid(userId);
            String role = (acc != null && acc.getRole() != null) ? acc.getRole() : "PATIENT";
            HospitalMembership defaultMem = new HospitalMembership(
                    "mem_" + userId + "_" + DEFAULT_HOSPITAL_ID,
                    userId,
                    DEFAULT_HOSPITAL_ID,
                    DEFAULT_HOSPITAL_CODE,
                    role,
                    HospitalMembership.STATUS_ACTIVE,
                    "General"
            );
            saveHospitalMembership(defaultMem);
            list.add(defaultMem);
        }

        return list;
    }

    public void saveHospitalMembership(HospitalMembership membership) {
        if (membership == null || membership.getMembershipId() == null || membership.getMembershipId().trim().isEmpty()) {
            return;
        }
        try {
            JSONObject root = getJsonObject(KEY_MEMBERSHIPS_REGISTRY_JSON);
            root.put(membership.getMembershipId(), membership.toJson());
            prefs.edit().putString(KEY_MEMBERSHIPS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public HospitalMembership getActiveMembership(String userId, String hospitalId) {
        if (userId == null || hospitalId == null) return null;
        for (HospitalMembership m : getUserMemberships(userId)) {
            if (hospitalId.equalsIgnoreCase(m.getHospitalId()) && m.isActive()) {
                return m;
            }
        }
        return null;
    }

    // =========================================================================
    // SUPER ADMIN AUTHORIZATION REGISTRY
    // =========================================================================

    public boolean isSuperAdmin(String uid) {
        if (uid == null || uid.trim().isEmpty()) return false;
        if ("superadmin_root".equalsIgnoreCase(uid) || "superadmin@arohi.health".equalsIgnoreCase(uid)) {
            return true;
        }
        String json = prefs.getString(KEY_SUPER_ADMINS_REGISTRY_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONObject root = new JSONObject(json);
                if (root.has(uid)) {
                    SuperAdminAuthorization sa = SuperAdminAuthorization.fromJson(root.getJSONObject(uid));
                    return sa != null && sa.isActive();
                }
            } catch (JSONException ignored) {
            }
        }
        return false;
    }

    public void saveSuperAdmin(SuperAdminAuthorization admin) {
        if (admin == null || admin.getSuperAdminUid() == null) return;
        try {
            JSONObject root = getJsonObject(KEY_SUPER_ADMINS_REGISTRY_JSON);
            root.put(admin.getSuperAdminUid(), admin.toJson());
            prefs.edit().putString(KEY_SUPER_ADMINS_REGISTRY_JSON, root.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<SuperAdminAuthorization> getAllSuperAdmins() {
        List<SuperAdminAuthorization> list = new ArrayList<>();
        String json = prefs.getString(KEY_SUPER_ADMINS_REGISTRY_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONObject root = new JSONObject(json);
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    SuperAdminAuthorization sa = SuperAdminAuthorization.fromJson(root.getJSONObject(k));
                    if (sa != null) {
                        list.add(sa);
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        return list;
    }

    // =========================================================================
    // BACKWARD COMPATIBILITY & DATA MIGRATION
    // =========================================================================

    public void migrateLegacyDataToHospital(String targetHospitalId, String targetHospitalCode) {
        String hId = targetHospitalId != null ? targetHospitalId : DEFAULT_HOSPITAL_ID;
        String hCode = targetHospitalCode != null ? targetHospitalCode : DEFAULT_HOSPITAL_CODE;

        for (UserAccount acc : getAllRegisteredAccounts()) {
            if (acc.getHospitalId() == null || acc.getHospitalId().trim().isEmpty() || "hosp_arohi_central".equals(acc.getHospitalId())) {
                acc.setHospitalId(hId);
                acc.setHospitalCode(hCode);
                upsertAccountInRegistry(acc);
            }
        }
    }

    // =========================================================================
    // PHASE 10: SETTINGS, DARK THEME, LOCALIZATION & PRIVACY
    // =========================================================================

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM);
    }

    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).commit();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    public void applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode());
    }

    public String getAppLanguage() {
        return prefs.getString(KEY_APP_LANGUAGE, LocaleHelper.LANGUAGE_ENGLISH);
    }

    public void setAppLanguage(String languageCode) {
        prefs.edit().putString(KEY_APP_LANGUAGE, languageCode).commit();
    }

    public void applySavedLocale(Context context) {
        LocaleHelper.setLocale(context, getAppLanguage());
    }

    // Notification Preferences
    public boolean isNotificationEnabled(String categoryKey) {
        return prefs.getBoolean(categoryKey, true);
    }

    public void setNotificationEnabled(String categoryKey, boolean enabled) {
        prefs.edit().putBoolean(categoryKey, enabled).apply();
    }

    public boolean isAppointmentsNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_APPOINTMENTS); }
    public boolean isMessagesNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_MESSAGES); }
    public boolean isPrescriptionsNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_PRESCRIPTIONS); }
    public boolean isLabReportsNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_LAB_REPORTS); }
    public boolean isAnnouncementsNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_ANNOUNCEMENTS); }
    public boolean isEmergencyNotifEnabled() { return isNotificationEnabled(KEY_NOTIF_EMERGENCY); }

    // Privacy Preferences
    public boolean isPrivacyOptionEnabled(String key) {
        return prefs.getBoolean(key, true);
    }

    public void setPrivacyOptionEnabled(String key, boolean enabled) {
        prefs.edit().putBoolean(key, enabled).apply();
    }

    public boolean isEmergencyProfileSharingEnabled() { return isPrivacyOptionEnabled(KEY_PRIVACY_SHARE_EMERGENCY); }
    public boolean isPublicCommunityFeedEnabled() { return isPrivacyOptionEnabled(KEY_PRIVACY_PUBLIC_COMMUNITY); }
    public boolean isMedicalRecordSharingEnabled() { return isPrivacyOptionEnabled(KEY_PRIVACY_RECORD_SHARING); }

    // Account Management
    public boolean changeLocalPassword(String email, String oldPassword, String newPassword) {
        if (!verifyLocalSandboxPassword(email, oldPassword)) {
            return false;
        }
        saveLocalSandboxPasswordHash(email, newPassword);
        return true;
    }

    public void requestAccountDeletion(String uid, String reason) {
        UserAccount account = getRegisteredAccountByUid(uid);
        if (account != null) {
            account.setVerificationStatus("DELETION_REQUESTED");
            account.setBioNotes("DELETION REQUESTED: " + (reason != null ? reason : "User requested account removal"));
            account.setUpdatedAt(System.currentTimeMillis());
            upsertAccountInRegistry(account);
        }
    }

    private String normalizeEmailKey(String email) {
        return email.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
