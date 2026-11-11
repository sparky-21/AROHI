package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 12 Staff Communication Message Model.
 * Represents a single message exchanged within the Hospital Authority Communication Hub
 * (/hospitals/{hospitalId}/conversations/{conversationId}/messages/{messageId}).
 * Supports text, timestamps, read receipts, attachments with file validation, and moderation.
 */
public class StaffMessage implements Serializable {

    public static final String ATTACHMENT_NONE = "NONE";
    public static final String ATTACHMENT_IMAGE = "IMAGE";
    public static final String ATTACHMENT_PDF = "PDF";

    public static final String MODERATION_NORMAL = "NORMAL";
    public static final String MODERATION_FLAGGED = "FLAGGED";
    public static final String MODERATION_REMOVED = "REMOVED";

    private String messageId;
    private String conversationId;
    private String hospitalId;
    private String senderUid;
    private String senderName;
    private String senderRole; // ADMIN, DOCTOR, NURSE
    private String senderArohiId;
    private String text;
    private long timestamp;
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentName;
    private long attachmentSizeBytes;
    private List<String> readByUids;
    private String moderationStatus;

    public StaffMessage() {
        this.timestamp = System.currentTimeMillis();
        this.attachmentType = ATTACHMENT_NONE;
        this.readByUids = new ArrayList<>();
        this.moderationStatus = MODERATION_NORMAL;
    }

    public StaffMessage(String messageId, String conversationId, String hospitalId,
                        String senderUid, String senderName, String senderRole,
                        String senderArohiId, String text) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.hospitalId = hospitalId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.senderArohiId = senderArohiId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.attachmentType = ATTACHMENT_NONE;
        this.attachmentUrl = null;
        this.attachmentName = null;
        this.attachmentSizeBytes = 0;
        this.readByUids = new ArrayList<>();
        this.readByUids.add(senderUid); // Sender has automatically read it
        this.moderationStatus = MODERATION_NORMAL;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("conversationId", conversationId);
        map.put("hospitalId", hospitalId);
        map.put("senderUid", senderUid);
        map.put("senderName", senderName != null ? senderName : "");
        map.put("senderRole", senderRole != null ? senderRole : "");
        map.put("senderArohiId", senderArohiId != null ? senderArohiId : "");
        map.put("text", text != null ? text : "");
        map.put("timestamp", timestamp);
        map.put("attachmentUrl", attachmentUrl);
        map.put("attachmentType", attachmentType != null ? attachmentType : ATTACHMENT_NONE);
        map.put("attachmentName", attachmentName);
        map.put("attachmentSizeBytes", attachmentSizeBytes);
        map.put("readByUids", readByUids != null ? readByUids : new ArrayList<>());
        map.put("moderationStatus", moderationStatus != null ? moderationStatus : MODERATION_NORMAL);
        return map;
    }

    public boolean isReadBy(String uid) {
        return readByUids != null && uid != null && readByUids.contains(uid);
    }

    public void markReadBy(String uid) {
        if (uid == null) return;
        if (readByUids == null) readByUids = new ArrayList<>();
        if (!readByUids.contains(uid)) {
            readByUids.add(uid);
        }
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getSenderArohiId() { return senderArohiId; }
    public void setSenderArohiId(String senderArohiId) { this.senderArohiId = senderArohiId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public long getAttachmentSizeBytes() { return attachmentSizeBytes; }
    public void setAttachmentSizeBytes(long attachmentSizeBytes) { this.attachmentSizeBytes = attachmentSizeBytes; }

    public List<String> getReadByUids() { return readByUids; }
    public void setReadByUids(List<String> readByUids) { this.readByUids = readByUids; }

    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
}
