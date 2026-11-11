package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * High-priority hospital emergency alert model.
 * Broadcast across staff group chats and Firebase Realtime Database to trigger
 * auditory beep alerts in the Authority Portal.
 */
public class EmergencyHospitalAlert implements Serializable {

    private String alertId;
    private String hospitalId;
    private String senderUid;
    private String senderName;
    private String senderRole;
    private String emergencyType;
    private String message;
    private String wardOrRoom;
    private long timestamp;
    private boolean acknowledged;
    private String acknowledgedByUid;
    private String acknowledgedByName;
    private long acknowledgedAt;

    public EmergencyHospitalAlert() {
        this.timestamp = System.currentTimeMillis();
        this.acknowledged = false;
    }

    public EmergencyHospitalAlert(String alertId,
                                  String hospitalId,
                                  String senderUid,
                                  String senderName,
                                  String senderRole,
                                  String emergencyType,
                                  String message,
                                  String wardOrRoom) {
        this();
        this.alertId = alertId;
        this.hospitalId = hospitalId;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.emergencyType = emergencyType;
        this.message = message;
        this.wardOrRoom = wardOrRoom;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("alertId", alertId);
        map.put("hospitalId", hospitalId);
        map.put("senderUid", senderUid);
        map.put("senderName", senderName);
        map.put("senderRole", senderRole);
        map.put("emergencyType", emergencyType);
        map.put("message", message);
        map.put("wardOrRoom", wardOrRoom);
        map.put("timestamp", timestamp);
        map.put("acknowledged", acknowledged);
        map.put("acknowledgedByUid", acknowledgedByUid != null ? acknowledgedByUid : "");
        map.put("acknowledgedByName", acknowledgedByName != null ? acknowledgedByName : "");
        map.put("acknowledgedAt", acknowledgedAt);
        return map;
    }

    public static EmergencyHospitalAlert fromMap(Map<String, Object> map) {
        if (map == null) return null;
        EmergencyHospitalAlert a = new EmergencyHospitalAlert();
        a.alertId = (String) map.get("alertId");
        a.hospitalId = (String) map.get("hospitalId");
        a.senderUid = (String) map.get("senderUid");
        a.senderName = (String) map.get("senderName");
        a.senderRole = (String) map.get("senderRole");
        a.emergencyType = (String) map.get("emergencyType");
        a.message = (String) map.get("message");
        a.wardOrRoom = (String) map.get("wardOrRoom");
        if (map.get("timestamp") instanceof Number) {
            a.timestamp = ((Number) map.get("timestamp")).longValue();
        }
        if (map.get("acknowledged") instanceof Boolean) {
            a.acknowledged = (Boolean) map.get("acknowledged");
        }
        a.acknowledgedByUid = (String) map.get("acknowledgedByUid");
        a.acknowledgedByName = (String) map.get("acknowledgedByName");
        if (map.get("acknowledgedAt") instanceof Number) {
            a.acknowledgedAt = ((Number) map.get("acknowledgedAt")).longValue();
        }
        return a;
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getWardOrRoom() { return wardOrRoom; }
    public void setWardOrRoom(String wardOrRoom) { this.wardOrRoom = wardOrRoom; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public String getAcknowledgedByUid() { return acknowledgedByUid; }
    public void setAcknowledgedByUid(String acknowledgedByUid) { this.acknowledgedByUid = acknowledgedByUid; }

    public String getAcknowledgedByName() { return acknowledgedByName; }
    public void setAcknowledgedByName(String acknowledgedByName) { this.acknowledgedByName = acknowledgedByName; }

    public long getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(long acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
