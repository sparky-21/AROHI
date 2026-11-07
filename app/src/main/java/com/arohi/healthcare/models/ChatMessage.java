package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single secure chat message stored in /conversations/{conversationId}/messages/{messageId}.
 */
public class ChatMessage {

    private String messageId;
    private String conversationId;
    private String senderUid;
    private String senderName;
    private String senderRole;
    private String receiverUid;
    private String messageText;
    private long timestamp;
    private boolean read;
    private long readAt;

    public ChatMessage() {
        // Required empty constructor for Firestore
    }

    public ChatMessage(String messageId, String conversationId,
                       String senderUid, String senderName, String senderRole,
                       String receiverUid, String messageText) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.receiverUid = receiverUid;
        this.messageText = messageText;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.readAt = 0L;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("conversationId", conversationId);
        map.put("senderUid", senderUid);
        map.put("senderName", senderName != null ? senderName : "");
        map.put("senderRole", senderRole != null ? senderRole : "");
        map.put("receiverUid", receiverUid != null ? receiverUid : "");
        map.put("messageText", messageText != null ? messageText : "");
        map.put("timestamp", timestamp);
        map.put("read", read);
        map.put("readAt", readAt);
        return map;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public void setReceiverUid(String receiverUid) {
        this.receiverUid = receiverUid;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(long readAt) {
        this.readAt = readAt;
    }
}
