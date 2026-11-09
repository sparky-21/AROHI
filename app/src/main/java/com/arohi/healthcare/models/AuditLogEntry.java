package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 8 Immutable Audit Log Entry Model.
 * Represents an audited security or clinical operation across the AROHI Healthcare Ecosystem.
 * Written to /auditLogs and stored in immutable local audit logs.
 */
public class AuditLogEntry implements Serializable {

    // Common Security & Clinical Actions
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_ISSUE_PRESCRIPTION = "ISSUE_PRESCRIPTION";
    public static final String ACTION_SCHEDULE_OPERATION = "SCHEDULE_OPERATION";
    public static final String ACTION_UPDATE_OPERATION_STATUS = "UPDATE_OPERATION_STATUS";
    public static final String ACTION_ASSIGN_BED = "ASSIGN_BED";
    public static final String ACTION_DISCHARGE_BED = "DISCHARGE_BED";
    public static final String ACTION_RECORD_VITALS = "RECORD_VITALS";
    public static final String ACTION_ORDER_LAB = "ORDER_LAB";
    public static final String ACTION_REVIEW_LAB = "REVIEW_LAB";
    public static final String ACTION_VERIFY_STAFF = "VERIFY_STAFF";
    public static final String ACTION_REJECT_STAFF = "REJECT_STAFF";
    public static final String ACTION_MODERATE_POST = "MODERATE_POST";
    public static final String ACTION_ACCESS_PATIENT_RECORD = "ACCESS_PATIENT_RECORD";
    public static final String ACTION_ADVANCE_JOURNEY = "ADVANCE_JOURNEY";

    private String logId;
    private String actorUid;
    private String actorName;
    private String actorRole;
    private String actorArohiId;
    private String action;
    private String targetUid;
    private String targetType;
    private String targetReferenceId;
    private String details;
    private long timestamp;
    private boolean success;

    public AuditLogEntry() {
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }

    public AuditLogEntry(String logId, String actorUid, String actorName, String actorRole,
                         String actorArohiId, String action, String targetUid,
                         String targetType, String targetReferenceId, String details) {
        this.logId = logId;
        this.actorUid = actorUid;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.actorArohiId = actorArohiId;
        this.action = action;
        this.targetUid = targetUid;
        this.targetType = targetType;
        this.targetReferenceId = targetReferenceId;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("logId", logId != null ? logId : "");
        map.put("actorUid", actorUid != null ? actorUid : "");
        map.put("actorName", actorName != null ? actorName : "");
        map.put("actorRole", actorRole != null ? actorRole : "");
        map.put("actorArohiId", actorArohiId != null ? actorArohiId : "");
        map.put("action", action != null ? action : "");
        map.put("targetUid", targetUid != null ? targetUid : "");
        map.put("targetType", targetType != null ? targetType : "");
        map.put("targetReferenceId", targetReferenceId != null ? targetReferenceId : "");
        map.put("details", details != null ? details : "");
        map.put("timestamp", timestamp);
        map.put("success", success);
        return map;
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getActorUid() { return actorUid; }
    public void setActorUid(String actorUid) { this.actorUid = actorUid; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getActorArohiId() { return actorArohiId; }
    public void setActorArohiId(String actorArohiId) { this.actorArohiId = actorArohiId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetUid() { return targetUid; }
    public void setTargetUid(String targetUid) { this.targetUid = targetUid; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetReferenceId() { return targetReferenceId; }
    public void setTargetReferenceId(String targetReferenceId) { this.targetReferenceId = targetReferenceId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
