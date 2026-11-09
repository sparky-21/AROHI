package com.arohi.healthcare.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AuditLogEntry;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.models.UserRole;
import com.arohi.healthcare.repositories.AuthCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Phase 8 Centralized Security and Audit Manager.
 * Ensures all clinical actions, administrative approvals, patient record accesses,
 * and security events are logged in an immutable, privacy-safe audit trail.
 */
public class SecurityAuditManager {

    private static final String PREF_AUDIT = "arohi_audit_logs_prefs";
    private static final String KEY_AUDIT_LOGS_JSON = "key_audit_logs_list";

    private SecurityAuditManager() {
        // Static utility
    }

    public static void logEvent(Context context,
                                String actorUid, String actorName, String actorRole, String actorArohiId,
                                String action, String targetUid, String targetType,
                                String targetReferenceId, String details) {
        if (context == null) return;

        String logId = "log_" + UUID.randomUUID().toString().substring(0, 10);
        String safeDetails = sanitizeDetails(details);

        AuditLogEntry entry = new AuditLogEntry(
                logId,
                actorUid != null ? actorUid : "SYSTEM",
                actorName != null ? actorName : "Automated Process",
                actorRole != null ? actorRole : "SYSTEM",
                actorArohiId != null ? actorArohiId : "",
                action,
                targetUid != null ? targetUid : "",
                targetType != null ? targetType : "",
                targetReferenceId != null ? targetReferenceId : "",
                safeDetails
        );

        // 1. Save to local append-only preferences
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_AUDIT, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_AUDIT_LOGS_JSON, null);
            JSONArray arr = (json != null) ? new JSONArray(json) : new JSONArray();
            arr.put(new JSONObject(entry.toMap()));
            prefs.edit().putString(KEY_AUDIT_LOGS_JSON, arr.toString()).apply();
        } catch (Exception ignored) {}

        // 2. Persist to Firestore /auditLogs
        try {
            FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.AUDIT_LOGS)
                    .document(logId)
                    .set(entry.toMap());
        } catch (Exception ignored) {}
    }

    public static void logClinicalAction(Context context, UserAccount actor,
                                         String action, String patientUid,
                                         String referenceId, String summary) {
        if (actor == null) return;
        logEvent(
                context,
                actor.getUid(),
                actor.getFullName(),
                actor.getRole(),
                actor.getArohiId(),
                action,
                patientUid,
                "PATIENT",
                referenceId,
                summary
        );
    }

    public static void logAdminAction(Context context, UserAccount admin,
                                      String action, String targetUid,
                                      String referenceId, String summary) {
        if (admin == null || admin.getUserRoleEnum() != UserRole.ADMIN) return;
        logEvent(
                context,
                admin.getUid(),
                admin.getFullName(),
                admin.getRole(),
                admin.getArohiId(),
                action,
                targetUid,
                "ADMINISTRATION",
                referenceId,
                summary
        );
    }

    public static void loadAuditLogs(Context context, AuthCallback<List<AuditLogEntry>> callback) {
        if (context == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<AuditLogEntry> list = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_AUDIT, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_AUDIT_LOGS_JSON, null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    AuditLogEntry entry = new AuditLogEntry(
                            o.optString("logId"),
                            o.optString("actorUid"),
                            o.optString("actorName"),
                            o.optString("actorRole"),
                            o.optString("actorArohiId"),
                            o.optString("action"),
                            o.optString("targetUid"),
                            o.optString("targetType"),
                            o.optString("targetReferenceId"),
                            o.optString("details")
                    );
                    entry.setTimestamp(o.optLong("timestamp"));
                    entry.setSuccess(o.optBoolean("success", true));
                    list.add(entry);
                }
            }
        } catch (Exception ignored) {}

        // Chronological descending (newest first)
        Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        callback.onSuccess(list);
    }

    private static String sanitizeDetails(String text) {
        if (text == null) return "";
        // Redact potential passwords or credentials
        return text.replaceAll("(?i)(password|secret|token)=[^,;\\s]+", "$1=***REDACTED***");
    }
}
