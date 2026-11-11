package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * High-frequency live GPS telemetry payload stored in Firebase Realtime Database.
 * Powers sub-second marker rendering and bearing rotation for:
 * 1. Emergency Ambulance Live GPS
 * 2. Smart Pharmacy Delivery Rider Live GPS
 */
public class LiveTelemetryPayload implements Serializable {

    private String recordId;
    private String category; // AMBULANCE or PHARMACY
    private String hospitalId;
    private String operatorUid;
    private String operatorName;
    private String operatorPhone;
    private double latitude;
    private double longitude;
    private float bearing;
    private float speed;
    private int etaMinutes;
    private String status;
    private boolean sharingActive;
    private long updatedAt;

    public LiveTelemetryPayload() {
        this.updatedAt = System.currentTimeMillis();
    }

    public LiveTelemetryPayload(String recordId,
                                String category,
                                String hospitalId,
                                String operatorUid,
                                String operatorName,
                                String operatorPhone,
                                double latitude,
                                double longitude,
                                float bearing,
                                float speed,
                                int etaMinutes,
                                String status,
                                boolean sharingActive) {
        this.recordId = recordId;
        this.category = category;
        this.hospitalId = hospitalId;
        this.operatorUid = operatorUid;
        this.operatorName = operatorName;
        this.operatorPhone = operatorPhone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
        this.speed = speed;
        this.etaMinutes = etaMinutes;
        this.status = status;
        this.sharingActive = sharingActive;
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("recordId", recordId);
        map.put("category", category);
        map.put("hospitalId", hospitalId);
        map.put("operatorUid", operatorUid);
        map.put("operatorName", operatorName);
        map.put("operatorPhone", operatorPhone);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("bearing", bearing);
        map.put("speed", speed);
        map.put("etaMinutes", etaMinutes);
        map.put("status", status);
        map.put("sharingActive", sharingActive);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public static LiveTelemetryPayload fromMap(Map<String, Object> map) {
        if (map == null) return null;
        LiveTelemetryPayload p = new LiveTelemetryPayload();
        p.recordId = (String) map.get("recordId");
        p.category = (String) map.get("category");
        p.hospitalId = (String) map.get("hospitalId");
        p.operatorUid = (String) map.get("operatorUid");
        p.operatorName = (String) map.get("operatorName");
        p.operatorPhone = (String) map.get("operatorPhone");
        if (map.get("latitude") instanceof Number) {
            p.latitude = ((Number) map.get("latitude")).doubleValue();
        }
        if (map.get("longitude") instanceof Number) {
            p.longitude = ((Number) map.get("longitude")).doubleValue();
        }
        if (map.get("bearing") instanceof Number) {
            p.bearing = ((Number) map.get("bearing")).floatValue();
        }
        if (map.get("speed") instanceof Number) {
            p.speed = ((Number) map.get("speed")).floatValue();
        }
        if (map.get("etaMinutes") instanceof Number) {
            p.etaMinutes = ((Number) map.get("etaMinutes")).intValue();
        }
        p.status = (String) map.get("status");
        if (map.get("sharingActive") instanceof Boolean) {
            p.sharingActive = (Boolean) map.get("sharingActive");
        }
        if (map.get("updatedAt") instanceof Number) {
            p.updatedAt = ((Number) map.get("updatedAt")).longValue();
        }
        return p;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getOperatorUid() { return operatorUid; }
    public void setOperatorUid(String operatorUid) { this.operatorUid = operatorUid; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getOperatorPhone() { return operatorPhone; }
    public void setOperatorPhone(String operatorPhone) { this.operatorPhone = operatorPhone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getBearing() { return bearing; }
    public void setBearing(float bearing) { this.bearing = bearing; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public int getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSharingActive() { return sharingActive; }
    public void setSharingActive(boolean sharingActive) { this.sharingActive = sharingActive; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
