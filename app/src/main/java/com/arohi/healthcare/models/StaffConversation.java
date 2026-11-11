package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 12 Staff Conversation Model.
 * Represents either a Hospital Group Chat ("All Doctors", "All Nurses")
 * or an Individual One-to-One Staff Conversation between Hospital Authority and a Doctor or Nurse.
 * Multi-tenant isolation is strictly enforced via hospitalId.
 */
public class StaffConversation implements Serializable {

    public static final String TYPE_GROUP_DOCTORS = "GROUP_DOCTORS";
    public static final String TYPE_GROUP_NURSES = "GROUP_NURSES";
    public static final String TYPE_INDIVIDUAL_DOCTOR = "INDIVIDUAL_DOCTOR";
    public static final String TYPE_INDIVIDUAL_NURSE = "INDIVIDUAL_NURSE";

    private String conversationId;
    private String hospitalId;
    private String hospitalCode;
    private String conversationType; // GROUP_DOCTORS, GROUP_NURSES, INDIVIDUAL_DOCTOR, INDIVIDUAL_NURSE
    private String title;
    private String authorityUid;
    private String authorityName;

    // For individual conversations
    private String participantUid;
    private String participantName;
    private String participantRole; // DOCTOR, NURSE
    private String participantArohiId;
    private String participantSpecialty;

    // For group conversations
    private List<String> memberUids;
    private int memberCount;

    // Last message summary
    private String lastMessageText;
    private String lastMessageSenderName;
    private long lastMessageTimestamp;
    private int unreadCount;
    private boolean active;

    public StaffConversation() {
        this.memberUids = new ArrayList<>();
        this.active = true;
        this.lastMessageTimestamp = System.currentTimeMillis();
    }

    /**
     * Factory for Group Conversations ("All Doctors" or "All Nurses").
     */
    public static StaffConversation createGroup(String hospitalId, String hospitalCode, String groupType, String title) {
        StaffConversation c = new StaffConversation();
        c.setConversationId("grp_" + hospitalId + "_" + groupType.toLowerCase());
        c.setHospitalId(hospitalId);
        c.setHospitalCode(hospitalCode);
        c.setConversationType(groupType);
        c.setTitle(title);
        c.setActive(true);
        c.setLastMessageText("Hospital group initialized for verified clinical staff.");
        c.setLastMessageSenderName("Hospital Authority");
        c.setLastMessageTimestamp(System.currentTimeMillis());
        return c;
    }

    /**
     * Factory for Individual Authority <-> Staff Member Conversations.
     */
    public static StaffConversation createIndividual(String hospitalId, String hospitalCode,
                                                     String authorityUid, String authorityName,
                                                     String staffUid, String staffName,
                                                     String staffRole, String staffArohiId,
                                                     String staffSpecialty) {
        StaffConversation c = new StaffConversation();
        c.setConversationId("conv_" + hospitalId + "_" + staffUid);
        c.setHospitalId(hospitalId);
        c.setHospitalCode(hospitalCode);
        c.setConversationType(UserRole.DOCTOR.name().equalsIgnoreCase(staffRole)
                ? TYPE_INDIVIDUAL_DOCTOR : TYPE_INDIVIDUAL_NURSE);
        c.setTitle(staffName);
        c.setAuthorityUid(authorityUid);
        c.setAuthorityName(authorityName);
        c.setParticipantUid(staffUid);
        c.setParticipantName(staffName);
        c.setParticipantRole(staffRole);
        c.setParticipantArohiId(staffArohiId);
        c.setParticipantSpecialty(staffSpecialty);
        c.setActive(true);
        c.setLastMessageText("Direct staff communication channel opened.");
        c.setLastMessageSenderName(authorityName);
        c.setLastMessageTimestamp(System.currentTimeMillis());
        return c;
    }

    public boolean isGroup() {
        return TYPE_GROUP_DOCTORS.equals(conversationType) || TYPE_GROUP_NURSES.equals(conversationType);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("conversationId", conversationId);
        map.put("hospitalId", hospitalId);
        map.put("hospitalCode", hospitalCode != null ? hospitalCode : "");
        map.put("conversationType", conversationType);
        map.put("title", title);
        map.put("authorityUid", authorityUid != null ? authorityUid : "");
        map.put("authorityName", authorityName != null ? authorityName : "");
        map.put("participantUid", participantUid != null ? participantUid : "");
        map.put("participantName", participantName != null ? participantName : "");
        map.put("participantRole", participantRole != null ? participantRole : "");
        map.put("participantArohiId", participantArohiId != null ? participantArohiId : "");
        map.put("participantSpecialty", participantSpecialty != null ? participantSpecialty : "");
        map.put("memberUids", memberUids != null ? memberUids : new ArrayList<>());
        map.put("memberCount", memberCount);
        map.put("lastMessageText", lastMessageText != null ? lastMessageText : "");
        map.put("lastMessageSenderName", lastMessageSenderName != null ? lastMessageSenderName : "");
        map.put("lastMessageTimestamp", lastMessageTimestamp);
        map.put("unreadCount", unreadCount);
        map.put("active", active);
        return map;
    }

    // Getters and Setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalCode() { return hospitalCode; }
    public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }

    public String getConversationType() { return conversationType; }
    public void setConversationType(String conversationType) { this.conversationType = conversationType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthorityUid() { return authorityUid; }
    public void setAuthorityUid(String authorityUid) { this.authorityUid = authorityUid; }

    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }

    public String getParticipantUid() { return participantUid; }
    public void setParticipantUid(String participantUid) { this.participantUid = participantUid; }

    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }

    public String getParticipantRole() { return participantRole; }
    public void setParticipantRole(String participantRole) { this.participantRole = participantRole; }

    public String getParticipantArohiId() { return participantArohiId; }
    public void setParticipantArohiId(String participantArohiId) { this.participantArohiId = participantArohiId; }

    public String getParticipantSpecialty() { return participantSpecialty; }
    public void setParticipantSpecialty(String participantSpecialty) { this.participantSpecialty = participantSpecialty; }

    public List<String> getMemberUids() { return memberUids; }
    public void setMemberUids(List<String> memberUids) { this.memberUids = memberUids; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public String getLastMessageSenderName() { return lastMessageSenderName; }
    public void setLastMessageSenderName(String lastMessageSenderName) { this.lastMessageSenderName = lastMessageSenderName; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
