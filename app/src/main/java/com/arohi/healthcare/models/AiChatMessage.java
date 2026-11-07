package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a chat message exchange in the AROHI AI Healthcare Assistant.
 */
public class AiChatMessage implements Serializable {

    public static final String SENDER_USER = "USER";
    public static final String SENDER_AI = "AI";

    public static final String DISCLAIMER_TEXT =
            "Disclaimer: AROHI AI is an educational assistant and cannot diagnose diseases, prescribe medication, or adjust dosages. Always consult your verified doctor.";

    private String messageId;
    private String sender; // USER or AI
    private String text;
    private long timestamp;
    private boolean isDemoMode;
    private String disclaimer;
    private String topicCategory; // e.g., EDUCATION, NAVIGATION, TERMINOLOGY, APPOINTMENTS, RECORDS, GENERAL

    public AiChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public AiChatMessage(String messageId, String sender, String text, boolean isDemoMode, String topicCategory) {
        this.messageId = messageId;
        this.sender = sender;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.isDemoMode = isDemoMode;
        this.topicCategory = topicCategory;
        if (SENDER_AI.equalsIgnoreCase(sender)) {
            this.disclaimer = DISCLAIMER_TEXT;
        } else {
            this.disclaimer = "";
        }
    }

    public boolean isAi() {
        return SENDER_AI.equalsIgnoreCase(sender);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("sender", sender);
        map.put("text", text);
        map.put("timestamp", timestamp);
        map.put("isDemoMode", isDemoMode);
        map.put("disclaimer", disclaimer);
        map.put("topicCategory", topicCategory);
        return map;
    }

    public static AiChatMessage fromMap(Map<String, Object> map) {
        if (map == null) return null;
        AiChatMessage msg = new AiChatMessage();
        msg.setMessageId((String) map.get("messageId"));
        msg.setSender((String) map.get("sender"));
        msg.setText((String) map.get("text"));
        if (map.get("timestamp") instanceof Number) {
            msg.setTimestamp(((Number) map.get("timestamp")).longValue());
        }
        if (map.get("isDemoMode") instanceof Boolean) {
            msg.setDemoMode((Boolean) map.get("isDemoMode"));
        }
        msg.setDisclaimer((String) map.get("disclaimer"));
        msg.setTopicCategory((String) map.get("topicCategory"));
        return msg;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isDemoMode() { return isDemoMode; }
    public void setDemoMode(boolean demoMode) { isDemoMode = demoMode; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public String getTopicCategory() { return topicCategory; }
    public void setTopicCategory(String topicCategory) { this.topicCategory = topicCategory; }
}
