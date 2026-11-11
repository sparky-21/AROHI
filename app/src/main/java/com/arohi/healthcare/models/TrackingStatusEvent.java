package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable chronological tracking milestone event recorded across Pharmacy,
 * Appointment, Operation, Payment, and Ambulance workflows.
 */
public class TrackingStatusEvent implements Serializable {

    private String status;
    private long timestamp;
    private String updatedByUid;
    private String updatedByName;
    private String updatedByRole;
    private String notes;
    private Double latitude;
    private Double longitude;

    public TrackingStatusEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public TrackingStatusEvent(String status, long timestamp, String updatedByUid,
                               String updatedByName, String updatedByRole, String notes) {
        this.status = status;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.updatedByUid = updatedByUid != null ? updatedByUid : "";
        this.updatedByName = updatedByName != null ? updatedByName : "AROHI System";
        this.updatedByRole = updatedByRole != null ? updatedByRole : "SYSTEM";
        this.notes = notes != null ? notes : "";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("timestamp", timestamp);
        map.put("updatedByUid", updatedByUid);
        map.put("updatedByName", updatedByName);
        map.put("updatedByRole", updatedByRole);
        map.put("notes", notes != null ? notes : "");
        if (latitude != null && longitude != null) {
            map.put("latitude", latitude);
            map.put("longitude", longitude);
        }
        return map;
    }

    public static TrackingStatusEvent fromMap(Map<String, Object> map) {
        if (map == null) return null;
        TrackingStatusEvent event = new TrackingStatusEvent();
        event.setStatus((String) map.get("status"));
        if (map.get("timestamp") instanceof Number) {
            event.setTimestamp(((Number) map.get("timestamp")).longValue());
        }
        event.setUpdatedByUid((String) map.get("updatedByUid"));
        event.setUpdatedByName((String) map.get("updatedByName"));
        event.setUpdatedByRole((String) map.get("updatedByRole"));
        event.setNotes((String) map.get("notes"));
        if (map.get("latitude") instanceof Number) {
            event.setLatitude(((Number) map.get("latitude")).doubleValue());
        }
        if (map.get("longitude") instanceof Number) {
            event.setLongitude(((Number) map.get("longitude")).doubleValue());
        }
        return event;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUpdatedByUid() { return updatedByUid; }
    public void setUpdatedByUid(String updatedByUid) { this.updatedByUid = updatedByUid; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public String getUpdatedByRole() { return updatedByRole; }
    public void setUpdatedByRole(String updatedByRole) { this.updatedByRole = updatedByRole; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
