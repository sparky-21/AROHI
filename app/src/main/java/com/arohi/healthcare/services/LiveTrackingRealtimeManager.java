package com.arohi.healthcare.services;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.models.LiveTelemetryPayload;
import com.arohi.healthcare.repositories.AuthCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

/**
 * Service managing Firebase Realtime Database live telemetry streaming.
 * Provides low-latency, real-time sync for:
 * 1. Ambulance Dispatch Live GPS (/liveTracking/hospitals/{hospitalId}/ambulances/{requestId})
 * 2. Pharmacy Delivery Partner Live GPS (/liveTracking/hospitals/{hospitalId}/pharmacy/{orderId})
 */
public class LiveTrackingRealtimeManager {

    private static final String ROOT_PATH = "liveTracking";
    private static final String HOSPITALS_NODE = "hospitals";
    private static final String AMBULANCES_NODE = "ambulances";
    private static final String PHARMACY_NODE = "pharmacy";

    private final Context context;
    private final FirebaseDatabase realtimeDatabase;

    public interface TelemetryObserver {
        void onTelemetryReceived(@NonNull LiveTelemetryPayload payload);
        void onError(String message);
    }

    public LiveTrackingRealtimeManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.realtimeDatabase = FirebaseProvider.getDatabase(this.context);
    }

    public boolean isAvailable() {
        return realtimeDatabase != null;
    }

    @Nullable
    public DatabaseReference getAmbulanceRef(@NonNull String hospitalId, @NonNull String requestId) {
        if (realtimeDatabase == null) return null;
        return realtimeDatabase.getReference(ROOT_PATH)
                .child(HOSPITALS_NODE)
                .child(hospitalId)
                .child(AMBULANCES_NODE)
                .child(requestId);
    }

    @Nullable
    public DatabaseReference getPharmacyRef(@NonNull String hospitalId, @NonNull String orderId) {
        if (realtimeDatabase == null) return null;
        return realtimeDatabase.getReference(ROOT_PATH)
                .child(HOSPITALS_NODE)
                .child(hospitalId)
                .child(PHARMACY_NODE)
                .child(orderId);
    }

    public void publishAmbulanceTelemetry(@NonNull String hospitalId,
                                          @NonNull String requestId,
                                          @NonNull LiveTelemetryPayload payload,
                                          @Nullable AuthCallback<Void> callback) {
        DatabaseReference ref = getAmbulanceRef(hospitalId, requestId);
        if (ref == null) {
            if (callback != null) callback.onError("Firebase Realtime Database offline or not configured.");
            return;
        }
        ref.setValue(payload.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void publishPharmacyTelemetry(@NonNull String hospitalId,
                                         @NonNull String orderId,
                                         @NonNull LiveTelemetryPayload payload,
                                         @Nullable AuthCallback<Void> callback) {
        DatabaseReference ref = getPharmacyRef(hospitalId, orderId);
        if (ref == null) {
            if (callback != null) callback.onError("Firebase Realtime Database offline or not configured.");
            return;
        }
        ref.setValue(payload.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void stopAmbulanceTracking(@NonNull String hospitalId, @NonNull String requestId) {
        DatabaseReference ref = getAmbulanceRef(hospitalId, requestId);
        if (ref != null) {
            ref.child("sharingActive").setValue(false);
        }
    }

    public void stopPharmacyTracking(@NonNull String hospitalId, @NonNull String orderId) {
        DatabaseReference ref = getPharmacyRef(hospitalId, orderId);
        if (ref != null) {
            ref.child("sharingActive").setValue(false);
        }
    }

    @Nullable
    public ValueEventListener observeAmbulanceTelemetry(@NonNull String hospitalId,
                                                        @NonNull String requestId,
                                                        @NonNull TelemetryObserver observer) {
        DatabaseReference ref = getAmbulanceRef(hospitalId, requestId);
        if (ref == null) return null;

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    try {
                        Object val = snapshot.getValue();
                        if (val instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) val;
                            LiveTelemetryPayload payload = LiveTelemetryPayload.fromMap(map);
                            if (payload != null) {
                                observer.onTelemetryReceived(payload);
                            }
                        }
                    } catch (Exception e) {
                        observer.onError("Failed to parse telemetry: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                observer.onError(error.getMessage());
            }
        };

        ref.addValueEventListener(listener);
        return listener;
    }

    @Nullable
    public ValueEventListener observePharmacyTelemetry(@NonNull String hospitalId,
                                                       @NonNull String orderId,
                                                       @NonNull TelemetryObserver observer) {
        DatabaseReference ref = getPharmacyRef(hospitalId, orderId);
        if (ref == null) return null;

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    try {
                        Object val = snapshot.getValue();
                        if (val instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) val;
                            LiveTelemetryPayload payload = LiveTelemetryPayload.fromMap(map);
                            if (payload != null) {
                                observer.onTelemetryReceived(payload);
                            }
                        }
                    } catch (Exception e) {
                        observer.onError("Failed to parse telemetry: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                observer.onError(error.getMessage());
            }
        };

        ref.addValueEventListener(listener);
        return listener;
    }

    public void removeListener(@NonNull DatabaseReference ref, @NonNull ValueEventListener listener) {
        ref.removeEventListener(listener);
    }
}
