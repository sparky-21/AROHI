package com.arohi.healthcare.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a one-to-one secure conversation metadata document stored in /conversations/{doctorUid}_{patientUid}.
 * Only accessible when /connections/{doctorUid}_{patientUid} has status == "Accepted".
 */
public class ConversationThread {

    private String conversationId;
    private String connectionId;
    private String doctorUid;
    private String doctorName;
    private String doctorSpecialty;
    private String patientUid;
    private String patientName;
    private String patientId;
    private String connectionStatus;
    private String lastMessagePreview;
    private String lastMessageSenderUid;
    private long lastMessageTimestamp;
    private boolean unreadByDoctor;
    private boolean unreadByPatient;

    public ConversationThread() {
        // Required empty constructor for Firestore
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("conversationId", conversationId);
        map.put("connectionId", connectionId);
        map.put("doctorUid", doctorUid);
        map.put("doctorName", doctorName != null ? doctorName : "");
        map.put("doctorSpecialty", doctorSpecialty != null ? doctorSpecialty : "");
        map.put("patientUid", patientUid);
        map.put("patientName", patientName != null ? patientName : "");
        map.put("patientId", patientId != null ? patientId : "");
        map.put("connectionStatus", connectionStatus != null ? connectionStatus : DoctorPatientConnection.STATUS_ACCEPTED);
        map.put("lastMessagePreview", lastMessagePreview != null ? lastMessagePreview : "");
        map.put("lastMessageSenderUid", lastMessageSenderUid != null ? lastMessageSenderUid : "");
        map.put("lastMessageTimestamp", lastMessageTimestamp);
        map.put("unreadByDoctor", unreadByDoctor);
        map.put("unreadByPatient", unreadByPatient);
        return map;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorSpecialty() {
        return doctorSpecialty;
    }

    public void setDoctorSpecialty(String doctorSpecialty) {
        this.doctorSpecialty = doctorSpecialty;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public String getLastMessageSenderUid() {
        return lastMessageSenderUid;
    }

    public void setLastMessageSenderUid(String lastMessageSenderUid) {
        this.lastMessageSenderUid = lastMessageSenderUid;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public boolean isUnreadByDoctor() {
        return unreadByDoctor;
    }

    public void setUnreadByDoctor(boolean unreadByDoctor) {
        this.unreadByDoctor = unreadByDoctor;
    }

    public boolean isUnreadByPatient() {
        return unreadByPatient;
    }

    public void setUnreadByPatient(boolean unreadByPatient) {
        this.unreadByPatient = unreadByPatient;
    }
}
