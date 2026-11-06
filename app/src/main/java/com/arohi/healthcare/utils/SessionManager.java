package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;

/**
 * Manages persistent session state, onboarding completion, and local account state caching.
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

        // Also cache by email key so local sandbox login can restore exact role & verification status
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
        String emailKey = normalizeEmailKey(email);
        String uid = prefs.getString("acct_" + emailKey + "_uid", null);
        if (uid == null) {
            return null;
        }
        UserRole role = UserRole.fromKey(prefs.getString("acct_" + emailKey + "_role", "PATIENT"));
        VerificationStatus status = VerificationStatus.fromKey(prefs.getString("acct_" + emailKey + "_status", "VERIFIED"));
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
        UserRole role = UserRole.fromKey(prefs.getString(KEY_ROLE, UserRole.PATIENT.getKey()));
        VerificationStatus status = VerificationStatus.fromKey(
                prefs.getString(KEY_VERIFICATION_STATUS, VerificationStatus.PENDING.getKey())
        );
        UserAccount account = new UserAccount(
                prefs.getString(KEY_UID, ""),
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

    private String normalizeEmailKey(String email) {
        return email.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
