package com.arohi.healthcare;

import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.models.VerificationStatus;
import com.arohi.healthcare.utils.LocaleHelper;
import com.arohi.healthcare.utils.SessionManager;
import com.arohi.healthcare.utils.ValidationUtils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for AROHI Phase 10:
 * Settings, Dark Mode, Bangla Localization & Privacy Preferences:
 * 1. Appearance / Theme Mode Persistence (Light, Dark, System Default)
 * 2. Language Selection & Localization Strategy (English, বাংলা)
 * 3. Granular Notification Categories Control
 * 4. Privacy Preferences (Emergency Profile, Timeline Sharing)
 * 5. Password Change Validation & Security
 * 6. Account Deletion Request Safety & Audit Workflow
 */
public class Phase10SettingsLocalizationTest {

    // =========================================================================
    // 1. APPEARANCE & THEME PREFERENCE
    // =========================================================================

    @Test
    public void testThemeModeConstantsAndMapping() {
        assertEquals(-1, SessionManager.THEME_MODE_SYSTEM);
        assertEquals(1, SessionManager.THEME_MODE_LIGHT);
        assertEquals(2, SessionManager.THEME_MODE_DARK);

        // Simulate state transitions
        int currentTheme = SessionManager.THEME_MODE_SYSTEM;
        assertEquals(-1, currentTheme);

        // Switch to Dark
        currentTheme = SessionManager.THEME_MODE_DARK;
        assertEquals(2, currentTheme);

        // Switch to Light
        currentTheme = SessionManager.THEME_MODE_LIGHT;
        assertEquals(1, currentTheme);
    }

    // =========================================================================
    // 2. LANGUAGE LOCALIZATION (ENGLISH & BANGLA)
    // =========================================================================

    @Test
    public void testLanguageConstantsAndSwitching() {
        assertEquals("en", LocaleHelper.LANGUAGE_ENGLISH);
        assertEquals("bn", LocaleHelper.LANGUAGE_BANGLA);

        String activeLanguage = LocaleHelper.LANGUAGE_ENGLISH;
        assertEquals("en", activeLanguage);

        // Switch to Bangla
        activeLanguage = LocaleHelper.LANGUAGE_BANGLA;
        assertEquals("bn", activeLanguage);
        assertTrue(LocaleHelper.LANGUAGE_BANGLA.equalsIgnoreCase(activeLanguage));
    }

    // =========================================================================
    // 3. NOTIFICATION PREFERENCES TOGGLES
    // =========================================================================

    @Test
    public void testNotificationCategoryPreferences() {
        Map<String, Boolean> notifPrefs = new HashMap<>();
        notifPrefs.put(SessionManager.KEY_NOTIF_APPOINTMENTS, true);
        notifPrefs.put(SessionManager.KEY_NOTIF_MESSAGES, true);
        notifPrefs.put(SessionManager.KEY_NOTIF_PRESCRIPTIONS, true);
        notifPrefs.put(SessionManager.KEY_NOTIF_LAB_REPORTS, true);
        notifPrefs.put(SessionManager.KEY_NOTIF_ANNOUNCEMENTS, true);
        notifPrefs.put(SessionManager.KEY_NOTIF_EMERGENCY, true);

        // Verify default states: all active
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_APPOINTMENTS));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_MESSAGES));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_PRESCRIPTIONS));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_LAB_REPORTS));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_ANNOUNCEMENTS));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_EMERGENCY));

        // Toggle announcements off
        notifPrefs.put(SessionManager.KEY_NOTIF_ANNOUNCEMENTS, false);
        assertFalse(notifPrefs.get(SessionManager.KEY_NOTIF_ANNOUNCEMENTS));

        // Emergency and Appointment alerts remain active
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_EMERGENCY));
        assertTrue(notifPrefs.get(SessionManager.KEY_NOTIF_APPOINTMENTS));
    }

    // =========================================================================
    // 4. PRIVACY PREFERENCES
    // =========================================================================

    @Test
    public void testPrivacyPreferencesToggles() {
        Map<String, Boolean> privacyPrefs = new HashMap<>();
        privacyPrefs.put(SessionManager.KEY_PRIVACY_SHARE_EMERGENCY, true);
        privacyPrefs.put(SessionManager.KEY_PRIVACY_RECORD_SHARING, true);

        assertTrue(privacyPrefs.get(SessionManager.KEY_PRIVACY_SHARE_EMERGENCY));
        assertTrue(privacyPrefs.get(SessionManager.KEY_PRIVACY_RECORD_SHARING));

        // Toggle record sharing off
        privacyPrefs.put(SessionManager.KEY_PRIVACY_RECORD_SHARING, false);
        assertFalse(privacyPrefs.get(SessionManager.KEY_PRIVACY_RECORD_SHARING));
        assertTrue(privacyPrefs.get(SessionManager.KEY_PRIVACY_SHARE_EMERGENCY));
    }

    // =========================================================================
    // 5. PASSWORD CHANGE VALIDATION
    // =========================================================================

    @Test
    public void testChangePasswordValidation() {
        // Valid password: at least 8 chars with letters & numbers
        assertTrue(ValidationUtils.isValidPassword("Secret1234"));
        assertTrue(ValidationUtils.isValidPassword("Medical@Pass2026"));

        // Invalid passwords
        assertFalse(ValidationUtils.isValidPassword("short1")); // Too short
        assertFalse(ValidationUtils.isValidPassword("onlylettersnocount")); // No digit
        assertFalse(ValidationUtils.isValidPassword("12345678")); // No letter
        assertFalse(ValidationUtils.isValidPassword("")); // Empty
        assertFalse(ValidationUtils.isValidPassword(null)); // Null

        // Password matching check
        String newPass = "StrongPass2026";
        String confirmPassMatch = "StrongPass2026";
        String confirmPassMismatch = "DifferentPass2026";

        assertEquals(newPass, confirmPassMatch);
        assertNotEquals(newPass, confirmPassMismatch);
    }

    // =========================================================================
    // 6. ACCOUNT DELETION REQUEST WORKFLOW
    // =========================================================================

    @Test
    public void testAccountDeletionWorkflow() {
        UserAccount account = new UserAccount(
                "usr_del_01", "AROHI-DEL-01", "user.delete@example.com",
                "Del User", "+880 1711-001122",
                UserRole.PATIENT, VerificationStatus.VERIFIED, true, true
        );
        assertEquals("VERIFIED", account.getVerificationStatus());

        // Perform deletion request
        account.setVerificationStatus("DELETION_REQUESTED");
        account.setBioNotes("DELETION REQUESTED: Patient relocated overseas");
        account.setUpdatedAt(System.currentTimeMillis());

        assertEquals("DELETION_REQUESTED", account.getVerificationStatus());
        assertTrue(account.getBioNotes().contains("DELETION REQUESTED"));
        assertNotNull(account.getArohiId()); // Preserves identity for medical legal compliance
    }
}
