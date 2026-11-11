package com.arohi.healthcare.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.TrackingCategory;
import com.arohi.healthcare.models.UserAccount;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.services.TrackingNotificationDispatcher;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.UUID;

/**
 * Phase 14 Ambulance Dispatch & Authorized Live Telemetry Repository.
 * Lifecycle:
 * Requested -> Accepted -> Ambulance Assigned -> On the Way -> Arrived -> Patient Picked Up -> Hospital Reached -> Completed.
 *
 * SECURITY & ANTI-FAKE POLICY:
 * - Never fabricates live GPS coordinates.
 * - Live location sharing requires explicit authorized driver/dispatch activation and automatically
 *   terminates when the request reaches Completed or Cancelled.
 * - Real-time Firestore snapshot listener receives actual coordinates pushed by a verified driver app / GPS feed.
 */
public class AmbulanceTrackingRepository {

    private final Context appContext;
    private final UnifiedTrackingRepository trackingRepository;
    private final FirebaseFirestore firestore;
    private final com.arohi.healthcare.services.LiveTrackingRealtimeManager realtimeManager;

    public AmbulanceTrackingRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.trackingRepository = new UnifiedTrackingRepository(this.appContext);
        this.firestore = FirebaseProvider.getFirestore(this.appContext);
        this.realtimeManager = new com.arohi.healthcare.services.LiveTrackingRealtimeManager(this.appContext);
    }

    public void createAmbulanceRequest(@NonNull String hospitalId,
                                       @NonNull String hospitalName,
                                       @NonNull UserAccount patient,
                                       @NonNull String pickupAddress,
                                       @Nullable Double pickupLat,
                                       @Nullable Double pickupLng,
                                       @NonNull String priorityLevel,
                                       String emergencyNotes,
                                       @NonNull AuthCallback<AmbulanceRequest> callback) {
        if (pickupAddress.trim().isEmpty()) {
            callback.onError("Pickup address is required for ambulance dispatch.");
            return;
        }

        String requestId = "amb_" + UUID.randomUUID().toString().substring(0, 8);
        AmbulanceRequest req = new AmbulanceRequest(
                requestId,
                hospitalId,
                hospitalName,
                patient.getUid(),
                patient.getArohiId(),
                patient.getFullName(),
                patient.getPhone(),
                pickupAddress.trim(),
                pickupLat,
                pickupLng,
                emergencyNotes,
                priorityLevel
        );

        trackingRepository.saveAmbulanceRequest(req, new AuthCallback<AmbulanceRequest>() {
            @Override
            public void onSuccess(AmbulanceRequest saved) {
                TrackingNotificationDispatcher.dispatchTrackingUpdate(
                        appContext,
                        patient.getUid(),
                        patient.getUid(),
                        hospitalName,
                        TrackingCategory.AMBULANCE,
                        saved.getRequestId(),
                        "Ambulance Request Submitted",
                        "Emergency dispatch request sent to " + hospitalName + " (" + priorityLevel + ")."
                );
                callback.onSuccess(saved);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    @Nullable
    public AmbulanceRequest getRequestById(@NonNull String hospitalId, @NonNull String requestId) {
        for (AmbulanceRequest r : trackingRepository.getLocalAmbulanceRequests(hospitalId)) {
            if (requestId.equals(r.getRequestId())) {
                return r;
            }
        }
        return null;
    }

    @Nullable
    public AmbulanceRequest getLatestActiveForPatient(@NonNull String hospitalId, @NonNull String patientUid) {
        for (AmbulanceRequest r : trackingRepository.getLocalAmbulanceRequests(hospitalId)) {
            if (patientUid.equals(r.getPatientUid()) && !r.isTerminal()) {
                return r;
            }
        }
        return null;
    }

    /**
     * Hospital Authority accepts an emergency request and assigns ambulance vehicle & driver.
     */
    public void acceptAndAssignAmbulance(@NonNull String hospitalId,
                                         @NonNull String requestId,
                                         @NonNull UserAccount authority,
                                         @NonNull String vehicleNumber,
                                         @NonNull String vehicleType,
                                         @NonNull String driverUid,
                                         @NonNull String driverName,
                                         @NonNull String driverPhone,
                                         @NonNull AuthCallback<AmbulanceRequest> callback) {
        AmbulanceRequest req = getRequestById(hospitalId, requestId);
        if (req == null) {
            callback.onError("Ambulance request not found.");
            return;
        }
        if (req.isTerminal()) {
            callback.onError("Cannot assign ambulance to a closed/completed request.");
            return;
        }

        req.setAcceptedByAuthorityUid(authority.getUid());
        req.setAmbulanceId("AMB_" + vehicleNumber.replaceAll("\\s+", "_"));
        req.setVehicleNumber(vehicleNumber.trim());
        req.setVehicleType(vehicleType.trim());
        req.setDriverUid(driverUid.trim());
        req.setDriverName(driverName.trim());
        req.setDriverPhone(driverPhone.trim());

        if (AmbulanceRequest.STATUS_REQUESTED.equalsIgnoreCase(req.getStatus())) {
            req.updateStatusWithAudit(AmbulanceRequest.STATUS_ACCEPTED, authority.getUid(),
                    authority.getFullName(), "ADMIN", "Accepted by Hospital Authority");
        }
        req.updateStatusWithAudit(AmbulanceRequest.STATUS_AMBULANCE_ASSIGNED, authority.getUid(),
                authority.getFullName(), "ADMIN",
                "Assigned " + vehicleType + " (" + vehicleNumber + ") • Driver: " + driverName);

        trackingRepository.saveAmbulanceRequest(req, new AuthCallback<AmbulanceRequest>() {
            @Override
            public void onSuccess(AmbulanceRequest saved) {
                TrackingNotificationDispatcher.dispatchTrackingUpdate(
                        appContext,
                        saved.getPatientUid(),
                        authority.getUid(),
                        authority.getFullName(),
                        TrackingCategory.AMBULANCE,
                        saved.getRequestId(),
                        "Ambulance Assigned: " + vehicleNumber,
                        "Driver " + driverName + " (" + driverPhone + ") assigned to your emergency pickup."
                );
                callback.onSuccess(saved);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void advanceAmbulanceStatus(@NonNull String hospitalId,
                                       @NonNull String requestId,
                                       @NonNull String nextStatus,
                                       @NonNull UserAccount actor,
                                       String note,
                                       int etaMinutes,
                                       @NonNull AuthCallback<AmbulanceRequest> callback) {
        AmbulanceRequest req = getRequestById(hospitalId, requestId);
        if (req == null) {
            callback.onError("Ambulance request not found.");
            return;
        }
        if (!req.isValidTransition(nextStatus)) {
            callback.onError("Invalid ambulance transition from " + req.getStatus() + " to " + nextStatus);
            return;
        }

        if (etaMinutes > 0 && !AmbulanceRequest.STATUS_COMPLETED.equalsIgnoreCase(nextStatus)
                && !AmbulanceRequest.STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            req.setEtaMinutes(etaMinutes);
        } else if (AmbulanceRequest.STATUS_COMPLETED.equalsIgnoreCase(nextStatus)
                || AmbulanceRequest.STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            req.setEtaMinutes(-1);
        }

        req.updateStatusWithAudit(nextStatus, actor.getUid(), actor.getFullName(),
                actor.getUserRoleEnum().getKey(),
                note != null && !note.trim().isEmpty() ? note.trim() : "Status updated to " + nextStatus);

        trackingRepository.saveAmbulanceRequest(req, new AuthCallback<AmbulanceRequest>() {
            @Override
            public void onSuccess(AmbulanceRequest saved) {
                if (saved.isTerminal()) {
                    realtimeManager.stopAmbulanceTracking(hospitalId, saved.getRequestId());
                }
                TrackingNotificationDispatcher.dispatchTrackingUpdate(
                        appContext,
                        saved.getPatientUid(),
                        actor.getUid(),
                        actor.getFullName(),
                        TrackingCategory.AMBULANCE,
                        saved.getRequestId(),
                        "Ambulance Status: " + nextStatus,
                        saved.isTerminal()
                                ? "Ambulance dispatch ended (" + nextStatus + "). Live GPS tracking stopped."
                                : "Ambulance update: " + nextStatus
                                + (saved.getEtaMinutes() > 0 ? " (ETA ~" + saved.getEtaMinutes() + " min)" : "")
                );
                callback.onSuccess(saved);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Driver App / GPS Telemetry Ingestion Entry Point.
     * Strictly verifies driver identity, hospital scoping, and active non-terminal session before accepting GPS.
     */
    public void publishAuthorizedDriverTelemetry(@NonNull String hospitalId,
                                                 @NonNull String requestId,
                                                 @NonNull String driverUid,
                                                 double latitude,
                                                 double longitude,
                                                 int etaMinutes,
                                                 @NonNull AuthCallback<AmbulanceRequest> callback) {
        AmbulanceRequest req = getRequestById(hospitalId, requestId);
        if (req == null) {
            callback.onError("Ambulance dispatch session not found.");
            return;
        }
        if (req.isTerminal()) {
            req.setLocationSharingAuthorized(false);
            callback.onError("Tracking session already ended; GPS telemetry rejected.");
            return;
        }
        if (req.getDriverUid() == null || !req.getDriverUid().equals(driverUid)) {
            callback.onError("Unauthorized GPS source: driver UID does not match assigned ambulance.");
            return;
        }

        req.setLocationSharingAuthorized(true);
        req.setCurrentLat(latitude);
        req.setCurrentLng(longitude);
        req.setLocationUpdatedAt(System.currentTimeMillis());
        if (etaMinutes > 0) req.setEtaMinutes(etaMinutes);
        req.setUpdatedAt(System.currentTimeMillis());

        // Publish live telemetry directly to Firebase Realtime Database
        com.arohi.healthcare.models.LiveTelemetryPayload payload = new com.arohi.healthcare.models.LiveTelemetryPayload(
                requestId,
                "AMBULANCE",
                hospitalId,
                driverUid,
                req.getDriverName(),
                req.getDriverPhone(),
                latitude,
                longitude,
                req.getBearing(),
                req.getSpeedKmh(),
                req.getEtaMinutes(),
                req.getStatus(),
                true
        );
        realtimeManager.publishAmbulanceTelemetry(hospitalId, requestId, payload, null);

        trackingRepository.saveAmbulanceRequest(req, callback);
    }

    @Nullable
    public ListenerRegistration observeAmbulanceRequestRealtime(@NonNull String hospitalId,
                                                                @NonNull String requestId,
                                                                @NonNull AuthCallback<AmbulanceRequest> listener) {
        if (firestore == null) return null;
        return firestore.collection(FirestoreCollections.HOSPITALS)
                .document(hospitalId)
                .collection(FirestoreCollections.AMBULANCE_REQUESTS)
                .document(requestId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        AmbulanceRequest updated = AmbulanceRequest.fromMap(snapshot.getData());
                        if (updated != null) {
                            trackingRepository.saveAmbulanceRequest(updated, null);
                            listener.onSuccess(updated);
                        }
                    }
                });
    }
}
