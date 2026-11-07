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

    public static final String STATUS_RECEIVED = "Received";
    public static final String STATUS_VERIFIED = "Verified";
    public static final String STATUS_PREPARING = "Preparing";
    public static final String STATUS_READY = "Ready";
    public static final String STATUS_COLLECTED = "Collected";
    public static final String STATUS_CANCELLED = "Cancelled";

    public static final String DELIVERY_HOSPITAL_PICKUP = "Hospital Pharmacy Counter Pickup";
    public static final String DELIVERY_WARD_DELIVERY = "Inpatient Ward Bedside Delivery";
    public static final String DELIVERY_HOME = "Home Delivery";

    private String orderId;
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
    private String status; // Received, Verified, Preparing, Ready, Collected, Cancelled
    private String orderDate; // YYYY-MM-DD
    private long createdAtTimestamp;
    private String pharmacistNotes;
    private String verifiedByStaffUid;

    public PharmacyOrder() {
        this.items = new ArrayList<>();
        this.status = STATUS_RECEIVED;
        this.deliveryType = DELIVERY_HOSPITAL_PICKUP;
        this.createdAtTimestamp = System.currentTimeMillis();
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
        this.status = STATUS_RECEIVED;
        this.orderDate = orderDate;
        this.createdAtTimestamp = System.currentTimeMillis();
        this.pharmacistNotes = "";
        this.verifiedByStaffUid = "";
    }

    public boolean isPrescriptionLinked() {
        return prescriptionId != null && !prescriptionId.trim().isEmpty();
    }

    public boolean canBeCancelled() {
        return STATUS_RECEIVED.equalsIgnoreCase(status);
    }

    /**
     * Checks if transitioning from the current status to nextStatus is valid according to the
     * strictly defined workflow: Received -> Verified -> Preparing -> Ready -> Collected.
     */
    public boolean isValidTransition(String nextStatus) {
        if (nextStatus == null) return false;
        if (STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            return STATUS_RECEIVED.equalsIgnoreCase(status);
        }
        if (STATUS_RECEIVED.equalsIgnoreCase(status)) {
            return STATUS_VERIFIED.equalsIgnoreCase(nextStatus);
        } else if (STATUS_VERIFIED.equalsIgnoreCase(status)) {
            return STATUS_PREPARING.equalsIgnoreCase(nextStatus);
        } else if (STATUS_PREPARING.equalsIgnoreCase(status)) {
            return STATUS_READY.equalsIgnoreCase(nextStatus);
        } else if (STATUS_READY.equalsIgnoreCase(status)) {
            return STATUS_COLLECTED.equalsIgnoreCase(nextStatus);
        }
        return false;
    }

    public int getStatusStepIndex() {
        if (STATUS_RECEIVED.equalsIgnoreCase(status)) return 0;
        if (STATUS_VERIFIED.equalsIgnoreCase(status)) return 1;
        if (STATUS_PREPARING.equalsIgnoreCase(status)) return 2;
        if (STATUS_READY.equalsIgnoreCase(status)) return 3;
        if (STATUS_COLLECTED.equalsIgnoreCase(status)) return 4;
        return -1; // Cancelled
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
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
        map.put("pharmacistNotes", pharmacistNotes);
        map.put("verifiedByStaffUid", verifiedByStaffUid);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static PharmacyOrder fromMap(Map<String, Object> map) {
        if (map == null) return null;
        PharmacyOrder order = new PharmacyOrder();
        order.setOrderId((String) map.get("orderId"));
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
        return order;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

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
}
