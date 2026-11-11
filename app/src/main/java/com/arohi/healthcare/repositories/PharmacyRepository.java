package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.Medicine;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.models.PharmacyOrderItem;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.services.TrackingNotificationDispatcher;
import com.arohi.healthcare.utils.SessionManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Repository for AROHI Smart E-Pharmacy.
 * Manages medicine catalogue, inventory stock, prescription-based orders,
 * and order status lifecycle (Received -> Verified -> Preparing -> Ready -> Collected).
 * Follows the cache-first / instant render architecture pattern.
 */
public class PharmacyRepository {

    private final Context appContext;
    private final SessionManager sessionManager;
    private final FirebaseFirestore firestore;
    private final com.arohi.healthcare.services.LiveTrackingRealtimeManager realtimeManager;

    public PharmacyRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.appContext);
        this.firestore = FirebaseFirestore.getInstance();
        this.realtimeManager = new com.arohi.healthcare.services.LiveTrackingRealtimeManager(this.appContext);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    // =========================================================================
    // 1. MEDICINE CATALOGUE & SEARCH
    // =========================================================================

    public void loadMedicines(String categoryFilter, String searchQuery,
                              @NonNull AuthCallback<List<Medicine>> callback) {
        // Step 1: Instant Cache-First Delivery
        List<Medicine> localList = sessionManager.getAllMedicinesLocal();
        List<Medicine> filteredLocal = filterMedicines(localList, categoryFilter, searchQuery);
        callback.onSuccess(filteredLocal);

        // Step 2: Background Firestore Synchronization
        firestore.collection(FirestoreCollections.PHARMACY_PRODUCTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.getData() != null) {
                                Medicine remote = Medicine.fromMap(doc.getData());
                                if (remote != null && remote.getMedicineId() != null) {
                                    sessionManager.saveMedicineLocal(remote);
                                }
                            }
                        }
                    } else if (localList.isEmpty()) {
                        // First run cloud seed
                        seedRemoteMedicines(localList);
                    }
                    List<Medicine> updated = filterMedicines(sessionManager.getAllMedicinesLocal(), categoryFilter, searchQuery);
                    callback.onSuccess(updated);
                })
                .addOnFailureListener(e -> {
                    // Fallback to local cache already emitted
                });
    }

    private List<Medicine> filterMedicines(List<Medicine> list, String category, String query) {
        List<Medicine> result = new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        boolean filterCategory = category != null && !category.equalsIgnoreCase("ALL") && !category.trim().isEmpty();

        for (Medicine m : list) {
            if (filterCategory && !category.equalsIgnoreCase(m.getCategory())) {
                continue;
            }
            if (!normalizedQuery.isEmpty()) {
                String name = m.getName() != null ? m.getName().toLowerCase(Locale.ROOT) : "";
                String generic = m.getGenericName() != null ? m.getGenericName().toLowerCase(Locale.ROOT) : "";
                String cat = m.getCategory() != null ? m.getCategory().toLowerCase(Locale.ROOT) : "";
                if (!name.contains(normalizedQuery) && !generic.contains(normalizedQuery) && !cat.contains(normalizedQuery)) {
                    continue;
                }
            }
            result.add(m);
        }
        return result;
    }

    private void seedRemoteMedicines(List<Medicine> medicines) {
        for (Medicine m : medicines) {
            firestore.collection(FirestoreCollections.PHARMACY_PRODUCTS)
                    .document(m.getMedicineId())
                    .set(m.toMap());
        }
    }

    public void addOrUpdateMedicine(@NonNull Medicine medicine, @NonNull AuthCallback<Void> callback) {
        if (medicine.getMedicineId() == null || medicine.getMedicineId().isEmpty()) {
            medicine.setMedicineId("med_" + System.currentTimeMillis());
        }
        // Save local
        sessionManager.saveMedicineLocal(medicine);

        // Sync Firestore
        firestore.collection(FirestoreCollections.PHARMACY_PRODUCTS)
                .document(medicine.getMedicineId())
                .set(medicine.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Failed to sync medicine to cloud: " + e.getMessage()));
    }

    public void updateStock(@NonNull String medicineId, int newStock, @NonNull AuthCallback<Void> callback) {
        sessionManager.updateMedicineStockLocal(medicineId, newStock);
        firestore.collection(FirestoreCollections.PHARMACY_PRODUCTS)
                .document(medicineId)
                .update("stockQuantity", Math.max(0, newStock))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError("Failed to update stock: " + e.getMessage()));
    }

    // =========================================================================
    // 2. PHARMACY ORDERS & PRESCRIPTION VALIDATION
    // =========================================================================

    public void loadPatientOrders(@NonNull String patientUid,
                                  @NonNull AuthCallback<List<PharmacyOrder>> callback) {
        // Step 1: Instant Cache-First Delivery
        List<PharmacyOrder> localOrders = sessionManager.getPatientPharmacyOrdersLocal(patientUid);
        callback.onSuccess(localOrders);

        // Step 2: Background Firestore Synchronization
        firestore.collection(FirestoreCollections.PHARMACY_ORDERS)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.getData() != null) {
                                PharmacyOrder remote = PharmacyOrder.fromMap(doc.getData());
                                if (remote != null) {
                                    sessionManager.savePharmacyOrderLocal(remote);
                                }
                            }
                        }
                    }
                    callback.onSuccess(sessionManager.getPatientPharmacyOrdersLocal(patientUid));
                })
                .addOnFailureListener(e -> {
                    // Fallback to local cache already emitted
                });
    }

    public void loadAllOrders(@NonNull AuthCallback<List<PharmacyOrder>> callback) {
        // Step 1: Instant Cache-First Delivery
        List<PharmacyOrder> localOrders = sessionManager.getAllPharmacyOrdersLocal();
        callback.onSuccess(localOrders);

        // Step 2: Background Firestore Synchronization
        firestore.collection(FirestoreCollections.PHARMACY_ORDERS)
                .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.getData() != null) {
                                PharmacyOrder remote = PharmacyOrder.fromMap(doc.getData());
                                if (remote != null) {
                                    sessionManager.savePharmacyOrderLocal(remote);
                                }
                            }
                        }
                    }
                    callback.onSuccess(sessionManager.getAllPharmacyOrdersLocal());
                })
                .addOnFailureListener(e -> {
                    // Fallback to local cache
                });
    }

    public void createOrder(@NonNull PharmacyOrder order, @NonNull AuthCallback<PharmacyOrder> callback) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            callback.onError("Order must contain at least one medicine item.");
            return;
        }

        // Validate Prescription Requirements: If any medicine requires prescription, verify attached Rx
        boolean requiresPrescription = false;
        for (PharmacyOrderItem item : order.getItems()) {
            Medicine m = sessionManager.getMedicineLocal(item.getMedicineId());
            if (m != null && m.isRequiresPrescription()) {
                requiresPrescription = true;
                break;
            }
        }

        if (requiresPrescription && !order.isPrescriptionLinked()) {
            callback.onError("This order contains prescription-only medication. A valid digital prescription must be attached.");
            return;
        }

        // Generate Order ID if needed
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId("ord_" + System.currentTimeMillis());
        }
        if (order.getHospitalId() == null || order.getHospitalId().trim().isEmpty()) {
            order.setHospitalId(sessionManager.getSelectedHospitalId());
        }
        order.setStatus(PharmacyOrder.STATUS_RECEIVED);
        order.recordTrackingEvent(PharmacyOrder.STATUS_PLACED, order.getPatientUid(),
                order.getPatientName(), "PATIENT", "Order placed (" + order.getDeliveryType() + ")");

        // Deduct Inventory Stock
        for (PharmacyOrderItem item : order.getItems()) {
            Medicine m = sessionManager.getMedicineLocal(item.getMedicineId());
            if (m != null) {
                int newStock = Math.max(0, m.getStockQuantity() - item.getQuantity());
                m.setStockQuantity(newStock);
                sessionManager.saveMedicineLocal(m);
                firestore.collection(FirestoreCollections.PHARMACY_PRODUCTS)
                        .document(m.getMedicineId())
                        .update("stockQuantity", newStock);
            }
        }

        // Save order locally
        sessionManager.savePharmacyOrderLocal(order);

        // Save order to Firestore
        firestore.collection(FirestoreCollections.PHARMACY_ORDERS)
                .document(order.getOrderId())
                .set(order.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(order))
                .addOnFailureListener(e -> {
                    // Local order is already saved; inform of offline sync
                    callback.onSuccess(order);
                });
    }

    public void updateOrderStatus(@NonNull String orderId,
                                  @NonNull String newStatus,
                                  String staffNotes,
                                  String staffUid,
                                  @NonNull AuthCallback<Void> callback) {
        updateDeliveryDispatchDetails(orderId, newStatus, null, null, null, null, null, staffNotes, staffUid, callback);
    }

    /**
     * Phase 14 Pharmacy Delivery Tracking update supporting both pickup and home delivery,
     * delivery partner assignment, real GPS telemetry (never fabricated), and patient notifications.
     */
    public void updateDeliveryDispatchDetails(@NonNull String orderId,
                                              @NonNull String newStatus,
                                              String partnerName,
                                              String partnerPhone,
                                              String estimatedDeliveryTime,
                                              Double liveLat,
                                              Double liveLng,
                                              String staffNotes,
                                              String staffUid,
                                              @NonNull AuthCallback<Void> callback) {
        PharmacyOrder order = sessionManager.getPharmacyOrderLocal(orderId);
        if (order == null) {
            callback.onError("Order not found: " + orderId);
            return;
        }

        if (!order.isValidTransition(newStatus) && !newStatus.equalsIgnoreCase(order.getStatus())) {
            callback.onError("Invalid status transition from " + order.getStatus() + " to " + newStatus
                    + ". Valid workflow is Placed/Received -> Verified -> Preparing -> Ready -> Dispatched/Collected -> Out for Delivery -> Delivered.");
            return;
        }

        order.setStatus(newStatus);
        if (staffNotes != null && !staffNotes.trim().isEmpty()) order.setPharmacistNotes(staffNotes.trim());
        if (staffUid != null && !staffUid.trim().isEmpty()) order.setVerifiedByStaffUid(staffUid);
        if (partnerName != null && !partnerName.trim().isEmpty()) order.setDeliveryPartnerName(partnerName.trim());
        if (partnerPhone != null && !partnerPhone.trim().isEmpty()) order.setDeliveryPartnerPhone(partnerPhone.trim());
        if (estimatedDeliveryTime != null && !estimatedDeliveryTime.trim().isEmpty()) {
            order.setEstimatedDeliveryTime(estimatedDeliveryTime.trim());
        }
        if (liveLat != null && liveLng != null
                && !PharmacyOrder.STATUS_DELIVERED.equalsIgnoreCase(newStatus)
                && !PharmacyOrder.STATUS_CANCELLED.equalsIgnoreCase(newStatus)) {
            order.setLiveLat(liveLat);
            order.setLiveLng(liveLng);
            order.setLocationUpdatedAt(System.currentTimeMillis());
            order.setLocationSharingActive(true);

            // Push to Firebase Realtime Database
            com.arohi.healthcare.models.LiveTelemetryPayload payload = new com.arohi.healthcare.models.LiveTelemetryPayload(
                    orderId,
                    "PHARMACY",
                    order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central",
                    order.getDeliveryPartnerUid(),
                    order.getDeliveryPartnerName(),
                    order.getDeliveryPartnerPhone(),
                    liveLat,
                    liveLng,
                    0f,
                    0f,
                    15,
                    newStatus,
                    true
            );
            realtimeManager.publishPharmacyTelemetry(
                    order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central",
                    orderId,
                    payload,
                    null
            );
        } else if (PharmacyOrder.STATUS_DELIVERED.equalsIgnoreCase(newStatus)
                || PharmacyOrder.STATUS_CANCELLED.equalsIgnoreCase(newStatus)) {
            order.setLocationSharingActive(false);
            realtimeManager.stopPharmacyTracking(
                    order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central",
                    orderId
            );
        }
        order.recordTrackingEvent(newStatus, staffUid,
                partnerName != null && !partnerName.isEmpty() ? partnerName : "Pharmacy Staff",
                "PHARMACY",
                staffNotes != null && !staffNotes.isEmpty() ? staffNotes : "Order status updated to " + newStatus);

        sessionManager.savePharmacyOrderLocal(order);

        if (order.getPatientUid() != null && !order.getPatientUid().isEmpty()) {
            TrackingNotificationDispatcher.dispatchTrackingUpdate(
                    appContext,
                    order.getPatientUid(),
                    staffUid,
                    "AROHI Pharmacy",
                    TrackingCategory.PHARMACY,
                    orderId,
                    "Pharmacy Order: " + newStatus,
                    "Your order #" + (orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId)
                            + " is now " + newStatus + "."
            );
        }

        // Sync Firestore
        firestore.collection(FirestoreCollections.PHARMACY_ORDERS)
                .document(orderId)
                .set(order.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onSuccess(null));
    }

    /**
     * Ingestion point for delivery rider telemetry directly to Realtime Database and local cache.
     */
    public void publishAuthorizedRiderTelemetry(@NonNull String orderId,
                                                @NonNull String riderUid,
                                                double latitude,
                                                double longitude,
                                                float bearing,
                                                float speed,
                                                int etaMinutes,
                                                @NonNull AuthCallback<Void> callback) {
        PharmacyOrder order = sessionManager.getPharmacyOrderLocal(orderId);
        if (order == null) {
            callback.onError("Pharmacy order not found: " + orderId);
            return;
        }
        if (PharmacyOrder.STATUS_DELIVERED.equalsIgnoreCase(order.getStatus())
                || PharmacyOrder.STATUS_CANCELLED.equalsIgnoreCase(order.getStatus())) {
            order.setLocationSharingActive(false);
            callback.onError("Delivery already finished; rider telemetry rejected.");
            return;
        }
        order.setLiveLat(latitude);
        order.setLiveLng(longitude);
        order.setLocationUpdatedAt(System.currentTimeMillis());
        order.setLocationSharingActive(true);
        sessionManager.savePharmacyOrderLocal(order);

        com.arohi.healthcare.models.LiveTelemetryPayload payload = new com.arohi.healthcare.models.LiveTelemetryPayload(
                orderId,
                "PHARMACY",
                order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central",
                riderUid,
                order.getDeliveryPartnerName(),
                order.getDeliveryPartnerPhone(),
                latitude,
                longitude,
                bearing,
                speed,
                etaMinutes,
                order.getStatus(),
                true
        );
        realtimeManager.publishPharmacyTelemetry(
                order.getHospitalId() != null ? order.getHospitalId() : "hosp_arohi_central",
                orderId,
                payload,
                callback
        );
    }
}
