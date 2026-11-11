package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 14 Ambulance Emergency Dispatch & Live Telemetry Request Model.
 * Strict lifecycle:
 * Requested -> Accepted -> Ambulance Assigned -> On the Way -> Arrived -> Patient Picked Up -> Hospital Reached -> Completed.
 * Location sharing authorization is automatically revoked when completed or cancelled.
 * Never generates fake coordinates: telemetry fields stay null until actual GPS updates arrive.
 */
public class AmbulanceRequest implements Serializable {

    public static final String STATUS_REQUESTED = "Requested";
    public static final String STATUS_ACCEPTED = "Accepted";
    public static final String STATUS_AMBULANCE_ASSIGNED = "Ambulance Assigned";
    public static final String STATUS_ON_THE_WAY = "On the Way";
    public static final String STATUS_ARRIVED = "Arrived";
    public static final String STATUS_PATIENT_PICKED_UP = "Patient Picked Up";
    public static final String STATUS_HOSPITAL_REACHED = "Hospital Reached";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancelled";

    public static final List<String> WORKFLOW_STAGES = Arrays.asList(
            STATUS_REQUESTED,
            STATUS_ACCEPTED,
            STATUS_AMBULANCE_ASSIGNED,
            STATUS_ON_THE_WAY,
            STATUS_ARRIVED,
            STATUS_PATIENT_PICKED_UP,
            STATUS_HOSPITAL_REACHED,
            STATUS_COMPLETED
    );

    private String requestId;
    private String hospitalId;
    private String hospitalName;
    private String patientUid;
    private String patientArohiId;
    private String patientName;
    private String patientPhone;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String emergencyNotes;
    private String priorityLevel; // CRITICAL, HIGH, STANDARD

    // Assigned Ambulance & Driver Info (populated by Hospital Authority upon dispatch)
    private String ambulanceId;
    private String vehicleNumber;
    private String vehicleType; // ACLS, ICU, BLS
    private String driverUid;
    private String driverName;
    private String driverPhone;
    private String acceptedByAuthorityUid;

    // Live GPS Telemetry (ONLY populated by authorized telemetry feed, never fabricated)
    private Double currentLat;
    private Double currentLng;
    private long locationUpdatedAt;
    private int etaMinutes; // <= 0 when not computed
    private boolean locationSharingAuthorized;

    private String status;
    private long createdAt;
    private long updatedAt;
    private List<TrackingStatusEvent> trackingHistory;

    public AmbulanceRequest() {
        this.status = STATUS_REQUESTED;
        this.priorityLevel = "HIGH";
        this.etaMinutes = -1;
        this.locationSharingAuthorized = false;
        this.trackingHistory = new ArrayList<>();
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public AmbulanceRequest(String requestId, String hospitalId, String hospitalName,
                            String patientUid, String patientArohiId, String patientName,
                            String patientPhone, String pickupAddress,
                            Double pickupLat, Double pickupLng,
                            String emergencyNotes, String priorityLevel) {
        this();
        this.requestId = requestId;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.patientUid = patientUid;
        this.patientArohiId = patientArohiId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.pickupAddress = pickupAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.emergencyNotes = emergencyNotes;
        this.priorityLevel = priorityLevel != null ? priorityLevel : "HIGH";
        recordTrackingEvent(STATUS_REQUESTED, patientUid, patientName, "PATIENT", "Emergency ambulance requested.");
    }

    public boolean isValidTransition(String nextStatus) {
        if (nextStatus == null) return false;
        if (STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            return STATUS_REQUESTED.equalsIgnoreCase(status)
                    || STATUS_ACCEPTED.equalsIgnoreCase(status)
                    || STATUS_AMBULANCE_ASSIGNED.equalsIgnoreCase(status);
        }
        int curIdx = getStatusStepIndex();
        int nextIdx = WORKFLOW_STAGES.indexOf(nextStatus);
        if (curIdx < 0) return false;
        return nextIdx == curIdx + 1;
    }

    public int getStatusStepIndex() {
        for (int i = 0; i < WORKFLOW_STAGES.size(); i++) {
            if (WORKFLOW_STAGES.get(i).equalsIgnoreCase(status)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isTerminal() {
        return STATUS_COMPLETED.equalsIgnoreCase(status) || STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public boolean hasActiveLiveTelemetry() {
        return locationSharingAuthorized && !isTerminal() && currentLat != null && currentLng != null;
    }

    public void updateStatusWithAudit(String nextStatus, String actorUid, String actorName,
                                      String actorRole, String note) {
        this.status = nextStatus;
        this.updatedAt = System.currentTimeMillis();
        if (isTerminal()) {
            // Strictly terminate live GPS tracking when ambulance mission ends or is cancelled
            this.locationSharingAuthorized = false;
        } else if (STATUS_ON_THE_WAY.equalsIgnoreCase(nextStatus)
                || STATUS_ARRIVED.equalsIgnoreCase(nextStatus)
                || STATUS_PATIENT_PICKED_UP.equalsIgnoreCase(nextStatus)
                || STATUS_HOSPITAL_REACHED.equalsIgnoreCase(nextStatus)) {
            this.locationSharingAuthorized = true;
        }
        recordTrackingEvent(nextStatus, actorUid, actorName, actorRole, note);
    }

    public void recordTrackingEvent(String statusLabel, String actorUid, String actorName,
                                    String actorRole, String note) {
        if (trackingHistory == null) trackingHistory = new ArrayList<>();
        trackingHistory.add(new TrackingStatusEvent(
                statusLabel, System.currentTimeMillis(), actorUid, actorName, actorRole, note
        ));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("hospitalId", hospitalId);
        map.put("hospitalName", hospitalName);
        map.put("patientUid", patientUid);
        map.put("patientArohiId", patientArohiId);
        map.put("patientName", patientName);
        map.put("patientPhone", patientPhone);
        map.put("pickupAddress", pickupAddress);
        if (pickupLat != null) map.put("pickupLat", pickupLat);
        if (pickupLng != null) map.put("pickupLng", pickupLng);
        map.put("emergencyNotes", emergencyNotes != null ? emergencyNotes : "");
        map.put("priorityLevel", priorityLevel);
        map.put("ambulanceId", ambulanceId != null ? ambulanceId : "");
        map.put("vehicleNumber", vehicleNumber != null ? vehicleNumber : "");
        map.put("vehicleType", vehicleType != null ? vehicleType : "");
        map.put("driverUid", driverUid != null ? driverUid : "");
        map.put("driverName", driverName != null ? driverName : "");
        map.put("driverPhone", driverPhone != null ? driverPhone : "");
        map.put("acceptedByAuthorityUid", acceptedByAuthorityUid != null ? acceptedByAuthorityUid : "");
        if (currentLat != null) map.put("currentLat", currentLat);
        if (currentLng != null) map.put("currentLng", currentLng);
        map.put("locationUpdatedAt", locationUpdatedAt);
        map.put("etaMinutes", etaMinutes);
        map.put("locationSharingAuthorized", locationSharingAuthorized);
        map.put("status", status);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        List<Map<String, Object>> historyList = new ArrayList<>();
        if (trackingHistory != null) {
            for (TrackingStatusEvent ev : trackingHistory) {
                historyList.add(ev.toMap());
            }
        }
        map.put("trackingHistory", historyList);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static AmbulanceRequest fromMap(Map<String, Object> map) {
        if (map == null) return null;
        AmbulanceRequest req = new AmbulanceRequest();
        req.setRequestId((String) map.get("requestId"));
        req.setHospitalId((String) map.get("hospitalId"));
        req.setHospitalName((String) map.get("hospitalName"));
        req.setPatientUid((String) map.get("patientUid"));
        req.setPatientArohiId((String) map.get("patientArohiId"));
        req.setPatientName((String) map.get("patientName"));
        req.setPatientPhone((String) map.get("patientPhone"));
        req.setPickupAddress((String) map.get("pickupAddress"));
        if (map.get("pickupLat") instanceof Number) req.setPickupLat(((Number) map.get("pickupLat")).doubleValue());
        if (map.get("pickupLng") instanceof Number) req.setPickupLng(((Number) map.get("pickupLng")).doubleValue());
        req.setEmergencyNotes((String) map.get("emergencyNotes"));
        req.setPriorityLevel((String) map.get("priorityLevel"));
        req.setAmbulanceId((String) map.get("ambulanceId"));
        req.setVehicleNumber((String) map.get("vehicleNumber"));
        req.setVehicleType((String) map.get("vehicleType"));
        req.setDriverUid((String) map.get("driverUid"));
        req.setDriverName((String) map.get("driverName"));
        req.setDriverPhone((String) map.get("driverPhone"));
        req.setAcceptedByAuthorityUid((String) map.get("acceptedByAuthorityUid"));
        if (map.get("currentLat") instanceof Number) req.setCurrentLat(((Number) map.get("currentLat")).doubleValue());
        if (map.get("currentLng") instanceof Number) req.setCurrentLng(((Number) map.get("currentLng")).doubleValue());
        if (map.get("locationUpdatedAt") instanceof Number) req.setLocationUpdatedAt(((Number) map.get("locationUpdatedAt")).longValue());
        if (map.get("etaMinutes") instanceof Number) req.setEtaMinutes(((Number) map.get("etaMinutes")).intValue());
        if (map.get("locationSharingAuthorized") instanceof Boolean) {
            req.setLocationSharingAuthorized((Boolean) map.get("locationSharingAuthorized"));
        }
        req.setStatus((String) map.get("status"));
        if (map.get("createdAt") instanceof Number) req.setCreatedAt(((Number) map.get("createdAt")).longValue());
        if (map.get("updatedAt") instanceof Number) req.setUpdatedAt(((Number) map.get("updatedAt")).longValue());
        List<TrackingStatusEvent> history = new ArrayList<>();
        Object rawHistory = map.get("trackingHistory");
        if (rawHistory instanceof List) {
            for (Object o : (List<?>) rawHistory) {
                if (o instanceof Map) {
                    TrackingStatusEvent ev = TrackingStatusEvent.fromMap((Map<String, Object>) o);
                    if (ev != null) history.add(ev);
                }
            }
        }
        req.setTrackingHistory(history);
        return req;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientArohiId() { return patientArohiId; }
    public void setPatientArohiId(String patientArohiId) { this.patientArohiId = patientArohiId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }

    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }

    public String getEmergencyNotes() { return emergencyNotes; }
    public void setEmergencyNotes(String emergencyNotes) { this.emergencyNotes = emergencyNotes; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(String ambulanceId) { this.ambulanceId = ambulanceId; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getDriverUid() { return driverUid; }
    public void setDriverUid(String driverUid) { this.driverUid = driverUid; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getAcceptedByAuthorityUid() { return acceptedByAuthorityUid; }
    public void setAcceptedByAuthorityUid(String acceptedByAuthorityUid) { this.acceptedByAuthorityUid = acceptedByAuthorityUid; }

    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }

    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }

    public long getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(long locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public int getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }

    public boolean isLocationSharingAuthorized() { return locationSharingAuthorized; }
    public void setLocationSharingAuthorized(boolean locationSharingAuthorized) { this.locationSharingAuthorized = locationSharingAuthorized; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<TrackingStatusEvent> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<TrackingStatusEvent> trackingHistory) { this.trackingHistory = trackingHistory != null ? trackingHistory : new ArrayList<>(); }
}
