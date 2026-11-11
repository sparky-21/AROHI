package com.arohi.healthcare.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model representing an order placed in the AROHI Smart E-Pharmacy.
 * Tracks lifecycle statuses: Received -> Verified -> Preparing -> Ready -> Collected.
 * Supports linking to a verified digital prescription from Phase 4.
 */
public class PharmacyOrder implements Serializable {

    public static final String STATUS_PLACED = "Placed";
    public static final String STATUS_RECEIVED = "Received";
    public static final String STATUS_VERIFIED = "Verified";
    public static final String STATUS_PREPARING = "Preparing";
    public static final String STATUS_READY = "Ready";
    public static final String STATUS_DISPATCHED = "Dispatched";
    public static final String STATUS_OUT_FOR_DELIVERY = "Out for Delivery";
    public static final String STATUS_DELIVERED = "Delivered";
    public static final String STATUS_COLLECTED = "Collected";
    public static final String STATUS_CANCELLED = "Cancelled";

    public static final String DELIVERY_HOSPITAL_PICKUP = "Hospital Pharmacy Counter Pickup";
    public static final String DELIVERY_WARD_DELIVERY = "Inpatient Ward Bedside Delivery";
    public static final String DELIVERY_HOME = "Home Delivery";

    private String orderId;
    private String hospitalId;
    private String patientUid;
    private String patientArohiId;
    private String patientName;
    private String patientPhone;
    private String prescriptionId; // nullable if only OTC medicines
    private String prescriptionDoctorName; // doctor who issued the prescription
    private List<PharmacyOrderItem> items;
    private double totalAmount;
    private String deliveryType;
    private String deliveryAddress;
    private String status; // Placed/Received -> Verified -> Preparing -> Ready -> Dispatched/Collected -> Out for Delivery -> Delivered
    private String orderDate; // YYYY-MM-DD
    private long createdAtTimestamp;
    private long updatedAtTimestamp;
    private String pharmacistNotes;
    private String verifiedByStaffUid;

    // Phase 14: Delivery Partner & Authorized Telemetry Fields (never fabricated)
    private String deliveryPartnerUid;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;
    private String estimatedDeliveryTime;
    private Double liveLat;
    private Double liveLng;
    private long locationUpdatedAt;
    private boolean locationSharingActive;
    private List<TrackingStatusEvent> trackingHistory;

    public PharmacyOrder() {
        this.hospitalId = "hosp_arohi_central";
        this.items = new ArrayList<>();
        this.status = STATUS_RECEIVED;
        this.deliveryType = DELIVERY_HOSPITAL_PICKUP;
        long now = System.currentTimeMillis();
        this.createdAtTimestamp = now;
        this.updatedAtTimestamp = now;
        this.trackingHistory = new ArrayList<>();
    }

    public PharmacyOrder(String orderId, String patientUid, String patientArohiId,
                         String patientName, String patientPhone, String prescriptionId,
                         String prescriptionDoctorName, List<PharmacyOrderItem> items,
                         double totalAmount, String deliveryType, String deliveryAddress,
                         String orderDate) {
        this.orderId = orderId;
        this.patientUid = patientUid;
        this.patientArohiId = patientArohiId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.prescriptionId = prescriptionId;
        this.prescriptionDoctorName = prescriptionDoctorName;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.deliveryType = deliveryType != null ? deliveryType : DELIVERY_HOSPITAL_PICKUP;
        this.deliveryAddress = deliveryAddress != null ? deliveryAddress : "";
        this.hospitalId = "hosp_arohi_central";
        this.status = STATUS_RECEIVED;
        this.orderDate = orderDate;
        long now = System.currentTimeMillis();
        this.createdAtTimestamp = now;
        this.updatedAtTimestamp = now;
        this.pharmacistNotes = "";
        this.verifiedByStaffUid = "";
        this.trackingHistory = new ArrayList<>();
        recordTrackingEvent(STATUS_PLACED, patientUid, patientName, "PATIENT", "Order placed.");
    }

    public boolean isPrescriptionLinked() {
        return prescriptionId != null && !prescriptionId.trim().isEmpty();
    }

    public boolean isHomeDelivery() {
        return DELIVERY_HOME.equalsIgnoreCase(deliveryType);
    }

    public boolean canBeCancelled() {
        return STATUS_RECEIVED.equalsIgnoreCase(status) || STATUS_PLACED.equalsIgnoreCase(status);
    }

    /**
     * Checks if transitioning from the current status to nextStatus is valid.
     * Supports both pickup (Received/Placed -> Verified -> Preparing -> Ready -> Collected)
     * and home delivery (Placed -> Verified -> Preparing -> Ready -> Dispatched -> Out for Delivery -> Delivered).
     */
    public boolean isValidTransition(String nextStatus) {
        if (nextStatus == null) return false;
        if (STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            return STATUS_RECEIVED.equalsIgnoreCase(status) || STATUS_PLACED.equalsIgnoreCase(status);
        }
        if (STATUS_RECEIVED.equalsIgnoreCase(status) || STATUS_PLACED.equalsIgnoreCase(status)) {
            return STATUS_VERIFIED.equalsIgnoreCase(nextStatus);
        } else if (STATUS_VERIFIED.equalsIgnoreCase(status)) {
            return STATUS_PREPARING.equalsIgnoreCase(nextStatus);
        } else if (STATUS_PREPARING.equalsIgnoreCase(status)) {
            return STATUS_READY.equalsIgnoreCase(nextStatus);
        } else if (STATUS_READY.equalsIgnoreCase(status)) {
            return STATUS_COLLECTED.equalsIgnoreCase(nextStatus)
                    || STATUS_DISPATCHED.equalsIgnoreCase(nextStatus);
        } else if (STATUS_DISPATCHED.equalsIgnoreCase(status)) {
            return STATUS_OUT_FOR_DELIVERY.equalsIgnoreCase(nextStatus);
        } else if (STATUS_OUT_FOR_DELIVERY.equalsIgnoreCase(status)) {
            return STATUS_DELIVERED.equalsIgnoreCase(nextStatus);
        }
        return false;
    }

    public int getStatusStepIndex() {
        if (STATUS_RECEIVED.equalsIgnoreCase(status) || STATUS_PLACED.equalsIgnoreCase(status)) return 0;
        if (STATUS_VERIFIED.equalsIgnoreCase(status)) return 1;
        if (STATUS_PREPARING.equalsIgnoreCase(status)) return 2;
        if (STATUS_READY.equalsIgnoreCase(status)) return 3;
        if (STATUS_COLLECTED.equalsIgnoreCase(status) || STATUS_DISPATCHED.equalsIgnoreCase(status)) return 4;
        if (STATUS_OUT_FOR_DELIVERY.equalsIgnoreCase(status)) return 5;
        if (STATUS_DELIVERED.equalsIgnoreCase(status)) return 6;
        return -1; // Cancelled
    }

    public void recordTrackingEvent(String statusLabel, String actorUid, String actorName,
                                    String actorRole, String note) {
        if (trackingHistory == null) trackingHistory = new ArrayList<>();
        trackingHistory.add(new TrackingStatusEvent(
                statusLabel, System.currentTimeMillis(), actorUid, actorName, actorRole, note
        ));
        this.updatedAtTimestamp = System.currentTimeMillis();
        if (STATUS_DELIVERED.equalsIgnoreCase(statusLabel)
                || STATUS_COLLECTED.equalsIgnoreCase(statusLabel)
                || STATUS_CANCELLED.equalsIgnoreCase(statusLabel)) {
            this.locationSharingActive = false;
        }
    }

    public boolean hasActiveLiveLocation() {
        return locationSharingActive && liveLat != null && liveLng != null
                && !STATUS_DELIVERED.equalsIgnoreCase(status)
                && !STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public List<String> getExpectedStages() {
        if (isHomeDelivery()) {
            return java.util.Arrays.asList(
                    STATUS_PLACED, STATUS_VERIFIED, STATUS_PREPARING,
                    STATUS_READY, STATUS_DISPATCHED, STATUS_OUT_FOR_DELIVERY, STATUS_DELIVERED
            );
        }
        return java.util.Arrays.asList(
                STATUS_PLACED, STATUS_VERIFIED, STATUS_PREPARING, STATUS_READY, STATUS_COLLECTED
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("hospitalId", hospitalId != null ? hospitalId : "hosp_arohi_central");
        map.put("patientUid", patientUid);
        map.put("patientArohiId", patientArohiId);
        map.put("patientName", patientName);
        map.put("patientPhone", patientPhone);
        map.put("prescriptionId", prescriptionId);
        map.put("prescriptionDoctorName", prescriptionDoctorName);
        List<Map<String, Object>> itemsList = new ArrayList<>();
        if (items != null) {
            for (PharmacyOrderItem item : items) {
                itemsList.add(item.toMap());
            }
        }
        map.put("items", itemsList);
        map.put("totalAmount", totalAmount);
        map.put("deliveryType", deliveryType);
        map.put("deliveryAddress", deliveryAddress);
        map.put("status", status);
        map.put("orderDate", orderDate);
        map.put("createdAtTimestamp", createdAtTimestamp);
        map.put("updatedAtTimestamp", updatedAtTimestamp);
        map.put("pharmacistNotes", pharmacistNotes);
        map.put("verifiedByStaffUid", verifiedByStaffUid);
        map.put("deliveryPartnerUid", deliveryPartnerUid != null ? deliveryPartnerUid : "");
        map.put("deliveryPartnerName", deliveryPartnerName != null ? deliveryPartnerName : "");
        map.put("deliveryPartnerPhone", deliveryPartnerPhone != null ? deliveryPartnerPhone : "");
        map.put("estimatedDeliveryTime", estimatedDeliveryTime != null ? estimatedDeliveryTime : "");
        if (liveLat != null) map.put("liveLat", liveLat);
        if (liveLng != null) map.put("liveLng", liveLng);
        map.put("locationUpdatedAt", locationUpdatedAt);
        map.put("locationSharingActive", locationSharingActive);
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
    public static PharmacyOrder fromMap(Map<String, Object> map) {
        if (map == null) return null;
        PharmacyOrder order = new PharmacyOrder();
        order.setOrderId((String) map.get("orderId"));
        if (map.get("hospitalId") instanceof String) {
            order.setHospitalId((String) map.get("hospitalId"));
        }
        order.setPatientUid((String) map.get("patientUid"));
        order.setPatientArohiId((String) map.get("patientArohiId"));
        order.setPatientName((String) map.get("patientName"));
        order.setPatientPhone((String) map.get("patientPhone"));
        order.setPrescriptionId((String) map.get("prescriptionId"));
        order.setPrescriptionDoctorName((String) map.get("prescriptionDoctorName"));

        List<PharmacyOrderItem> items = new ArrayList<>();
        Object rawItems = map.get("items");
        if (rawItems instanceof List) {
            for (Object obj : (List<?>) rawItems) {
                if (obj instanceof Map) {
                    items.add(PharmacyOrderItem.fromMap((Map<String, Object>) obj));
                }
            }
        }
        order.setItems(items);

        if (map.get("totalAmount") instanceof Number) {
            order.setTotalAmount(((Number) map.get("totalAmount")).doubleValue());
        }
        order.setDeliveryType((String) map.get("deliveryType"));
        order.setDeliveryAddress((String) map.get("deliveryAddress"));
        order.setStatus((String) map.get("status"));
        order.setOrderDate((String) map.get("orderDate"));
        if (map.get("createdAtTimestamp") instanceof Number) {
            order.setCreatedAtTimestamp(((Number) map.get("createdAtTimestamp")).longValue());
        }
        order.setPharmacistNotes((String) map.get("pharmacistNotes"));
        order.setVerifiedByStaffUid((String) map.get("verifiedByStaffUid"));
        if (map.get("updatedAtTimestamp") instanceof Number) {
            order.setUpdatedAtTimestamp(((Number) map.get("updatedAtTimestamp")).longValue());
        }
        order.setDeliveryPartnerUid((String) map.get("deliveryPartnerUid"));
        order.setDeliveryPartnerName((String) map.get("deliveryPartnerName"));
        order.setDeliveryPartnerPhone((String) map.get("deliveryPartnerPhone"));
        order.setEstimatedDeliveryTime((String) map.get("estimatedDeliveryTime"));
        if (map.get("liveLat") instanceof Number) order.setLiveLat(((Number) map.get("liveLat")).doubleValue());
        if (map.get("liveLng") instanceof Number) order.setLiveLng(((Number) map.get("liveLng")).doubleValue());
        if (map.get("locationUpdatedAt") instanceof Number) {
            order.setLocationUpdatedAt(((Number) map.get("locationUpdatedAt")).longValue());
        }
        if (map.get("locationSharingActive") instanceof Boolean) {
            order.setLocationSharingActive((Boolean) map.get("locationSharingActive"));
        }
        Object rawHistory = map.get("trackingHistory");
        if (rawHistory instanceof List) {
            List<TrackingStatusEvent> history = new ArrayList<>();
            for (Object obj : (List<?>) rawHistory) {
                if (obj instanceof Map) {
                    TrackingStatusEvent ev = TrackingStatusEvent.fromMap((Map<String, Object>) obj);
                    if (ev != null) history.add(ev);
                }
            }
            order.setTrackingHistory(history);
        }
        return order;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getPatientUid() { return patientUid; }
    public void setPatientUid(String patientUid) { this.patientUid = patientUid; }

    public String getPatientArohiId() { return patientArohiId; }
    public void setPatientArohiId(String patientArohiId) { this.patientArohiId = patientArohiId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getPrescriptionDoctorName() { return prescriptionDoctorName; }
    public void setPrescriptionDoctorName(String prescriptionDoctorName) { this.prescriptionDoctorName = prescriptionDoctorName; }

    public List<PharmacyOrderItem> getItems() { return items; }
    public void setItems(List<PharmacyOrderItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public String getPharmacistNotes() { return pharmacistNotes; }
    public void setPharmacistNotes(String pharmacistNotes) { this.pharmacistNotes = pharmacistNotes; }

    public String getVerifiedByStaffUid() { return verifiedByStaffUid; }
    public void setVerifiedByStaffUid(String verifiedByStaffUid) { this.verifiedByStaffUid = verifiedByStaffUid; }

    public long getUpdatedAtTimestamp() { return updatedAtTimestamp; }
    public void setUpdatedAtTimestamp(long updatedAtTimestamp) { this.updatedAtTimestamp = updatedAtTimestamp; }

    public String getDeliveryPartnerUid() { return deliveryPartnerUid; }
    public void setDeliveryPartnerUid(String deliveryPartnerUid) { this.deliveryPartnerUid = deliveryPartnerUid; }

    public String getDeliveryPartnerName() { return deliveryPartnerName; }
    public void setDeliveryPartnerName(String deliveryPartnerName) { this.deliveryPartnerName = deliveryPartnerName; }

    public String getDeliveryPartnerPhone() { return deliveryPartnerPhone; }
    public void setDeliveryPartnerPhone(String deliveryPartnerPhone) { this.deliveryPartnerPhone = deliveryPartnerPhone; }

    public String getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public Double getLiveLat() { return liveLat; }
    public void setLiveLat(Double liveLat) { this.liveLat = liveLat; }

    public Double getLiveLng() { return liveLng; }
    public void setLiveLng(Double liveLng) { this.liveLng = liveLng; }

    public long getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(long locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public boolean isLocationSharingActive() { return locationSharingActive; }
    public void setLocationSharingActive(boolean locationSharingActive) { this.locationSharingActive = locationSharingActive; }

    public List<TrackingStatusEvent> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<TrackingStatusEvent> trackingHistory) {
        this.trackingHistory = trackingHistory != null ? trackingHistory : new ArrayList<>();
    }
}
