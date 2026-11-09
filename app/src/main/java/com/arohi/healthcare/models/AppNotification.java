package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Privacy-safe notification document stored in /notifications/{notificationId}.
 * Never includes sensitive medical text or raw chat messages in notification previews.
 */
public class AppNotification {

    public static final String TYPE_CONSULTATION_REQUEST = "CONSULTATION_REQUEST";
    public static final String TYPE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    public static final String TYPE_REQUEST_DECLINED = "REQUEST_DECLINED";
    public static final String TYPE_NEW_MESSAGE = "NEW_MESSAGE";

    // Phase 4 Clinical Notifications
    public static final String TYPE_APPOINTMENT_REQUEST = "APPOINTMENT_REQUEST";
    public static final String TYPE_APPOINTMENT_UPDATE = "APPOINTMENT_UPDATE";
    public static final String TYPE_APPOINTMENT_RESCHEDULED = "APPOINTMENT_RESCHEDULED";
    public static final String TYPE_PRESCRIPTION_ISSUED = "PRESCRIPTION_ISSUED";
    public static final String TYPE_LAB_TEST_ORDERED = "LAB_TEST_ORDERED";
    public static final String TYPE_LAB_REPORT_READY = "LAB_REPORT_READY";
    public static final String TYPE_LAB_REPORT_REVIEWED = "LAB_REPORT_REVIEWED";

    private String notificationId;
    private String recipientUid;
    private String senderUid;
    private String senderName;
    private String type;
    private String title;
    private String body;
    private String referenceId;
    private boolean read;
    private long createdAt;

    public AppNotification() {
        // Required empty constructor for Firestore
    }

    public AppNotification(String notificationId, String recipientUid, String title, String body, String type) {
        this.notificationId = notificationId;
        this.recipientUid = recipientUid;
        this.title = title;
        this.body = body;
        this.type = type;
        this.senderUid = "";
        this.senderName = "AROHI Healthcare System";
        this.referenceId = "";
        this.read = false;
        this.createdAt = System.currentTimeMillis();
    }

    public AppNotification(String notificationId, String recipientUid,
                           String senderUid, String senderName,
                           String type, String title, String body, String referenceId) {
        this.notificationId = notificationId;
        this.recipientUid = recipientUid;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceId = referenceId;
        this.read = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notificationId);
        map.put("recipientUid", recipientUid);
        map.put("senderUid", senderUid != null ? senderUid : "");
        map.put("senderName", senderName != null ? senderName : "");
        map.put("type", type != null ? type : "");
        map.put("title", title != null ? title : "");
        map.put("body", body != null ? body : "");
        map.put("referenceId", referenceId != null ? referenceId : "");
        map.put("read", read);
        map.put("createdAt", createdAt);
        return map;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getRecipientUid() {
        return recipientUid;
    }

    public void setRecipientUid(String recipientUid) {
        this.recipientUid = recipientUid;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
