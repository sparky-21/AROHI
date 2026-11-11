package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.arohi.healthcare.R;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.databinding.ActivityLiveTrackingMapBinding;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.repositories.AmbulanceTrackingRepository;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

/**
 * Phase 14 Live Telemetry Map Screen powered by Free OpenStreetMap (OSMDroid — zero API key).
 * Supports:
 * 1. Ambulance Dispatch Live GPS Tracking
 * 2. Pharmacy Home Delivery Partner Live GPS Tracking
 *
 * Strictly enforces:
 * - Zero fake live coordinates: only renders moving partner/ambulance markers when authorized telemetry exists.
 * - Automatically detaches telemetry updates and clears active marker when session ends.
 */
public class LiveTrackingMapActivity extends BaseActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_AMBULANCE = "AMBULANCE";
    public static final String MODE_PHARMACY = "PHARMACY";
    public static final String EXTRA_RECORD_ID = "extra_record_id";
    public static final String EXTRA_HOSPITAL_ID = "extra_hospital_id";

    private static final double DEFAULT_DHAKA_LAT = 23.7937;
    private static final double DEFAULT_DHAKA_LNG = 90.4043;

    private ActivityLiveTrackingMapBinding binding;
    private SessionManager sessionManager;
    private AmbulanceTrackingRepository ambulanceRepo;
    private com.arohi.healthcare.services.LiveTrackingRealtimeManager rtdbManager;
    private Marker activeVehicleMarker;
    private Marker destinationMarker;
    private ListenerRegistration realtimeListener;
    private com.google.firebase.database.ValueEventListener rtdbListener;
    private com.google.firebase.database.DatabaseReference activeRtdbRef;

    private String mode;
    private String recordId;
    private String hospitalId;
    private String partnerPhone = "";

    public static Intent newAmbulanceMapIntent(@NonNull Context context, @NonNull String hospitalId, @NonNull String requestId) {
        Intent i = new Intent(context, LiveTrackingMapActivity.class);
        i.putExtra(EXTRA_MODE, MODE_AMBULANCE);
        i.putExtra(EXTRA_HOSPITAL_ID, hospitalId);
        i.putExtra(EXTRA_RECORD_ID, requestId);
        return i;
    }

    public static Intent newPharmacyMapIntent(@NonNull Context context, @NonNull String hospitalId, @NonNull String orderId) {
        Intent i = new Intent(context, LiveTrackingMapActivity.class);
        i.putExtra(EXTRA_MODE, MODE_PHARMACY);
        i.putExtra(EXTRA_HOSPITAL_ID, hospitalId);
        i.putExtra(EXTRA_RECORD_ID, orderId);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveTrackingMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        ambulanceRepo = new AmbulanceTrackingRepository(this);
        rtdbManager = new com.arohi.healthcare.services.LiveTrackingRealtimeManager(this);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        hospitalId = getIntent().getStringExtra(EXTRA_HOSPITAL_ID);
        if (hospitalId == null || hospitalId.isEmpty()) {
            hospitalId = sessionManager.getSelectedHospitalId();
        }

        if (recordId == null || hospitalId == null) {
            Toast.makeText(this, "Tracking reference unavailable.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnLiveMapBack.setOnClickListener(v -> finish());
        binding.btnCallLivePartner.setOnClickListener(v -> {
            if (partnerPhone == null || partnerPhone.trim().isEmpty()) {
                Toast.makeText(this, "Driver/Partner contact phone not assigned yet.", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + partnerPhone.trim())));
            }
        });

        initOpenStreetMap();
        bindInitialRecordAndListen();
    }

    private void initOpenStreetMap() {
        binding.osmMapLiveTracking.setTileSource(TileSourceFactory.MAPNIK);
        binding.osmMapLiveTracking.setMultiTouchControls(true);
        binding.osmMapLiveTracking.getController().setZoom(13.5);
        binding.osmMapLiveTracking.getController().setCenter(new GeoPoint(DEFAULT_DHAKA_LAT, DEFAULT_DHAKA_LNG));
    }

    private void bindInitialRecordAndListen() {
        bindInitialRecord();
        if (MODE_AMBULANCE.equalsIgnoreCase(mode)) {
            // 1. Primary low-latency Firebase Realtime Database telemetry listener
            activeRtdbRef = rtdbManager.getAmbulanceRef(hospitalId, recordId);
            rtdbListener = rtdbManager.observeAmbulanceTelemetry(hospitalId, recordId,
                    new com.arohi.healthcare.services.LiveTrackingRealtimeManager.TelemetryObserver() {
                        @Override
                        public void onTelemetryReceived(@NonNull com.arohi.healthcare.models.LiveTelemetryPayload payload) {
                            runOnUiThread(() -> applyRealtimeTelemetry(payload));
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });

            // 2. Fallback Firestore snapshot listener for metadata sync
            realtimeListener = ambulanceRepo.observeAmbulanceRequestRealtime(hospitalId, recordId,
                    new AuthCallback<AmbulanceRequest>() {
                        @Override
                        public void onSuccess(AmbulanceRequest updated) {
                            renderAmbulance(updated);
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });
        } else if (MODE_PHARMACY.equalsIgnoreCase(mode)) {
            // 1. Primary low-latency Firebase Realtime Database telemetry listener
            activeRtdbRef = rtdbManager.getPharmacyRef(hospitalId, recordId);
            rtdbListener = rtdbManager.observePharmacyTelemetry(hospitalId, recordId,
                    new com.arohi.healthcare.services.LiveTrackingRealtimeManager.TelemetryObserver() {
                        @Override
                        public void onTelemetryReceived(@NonNull com.arohi.healthcare.models.LiveTelemetryPayload payload) {
                            runOnUiThread(() -> applyRealtimeTelemetry(payload));
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });

            // 2. Fallback Firestore snapshot listener for order changes
            FirebaseFirestore db = FirebaseProvider.getFirestore(this);
            if (db != null) {
                realtimeListener = db.collection(FirestoreCollections.PHARMACY_ORDERS)
                        .document(recordId)
                        .addSnapshotListener((snapshot, e) -> {
                            if (snapshot != null && snapshot.exists()) {
                                PharmacyOrder updated = PharmacyOrder.fromMap(snapshot.getData());
                                if (updated != null) {
                                    sessionManager.savePharmacyOrderLocal(updated);
                                    renderPharmacyOrder(updated);
                                }
                            }
                        });
            }
        }
    }

    private void applyRealtimeTelemetry(@NonNull com.arohi.healthcare.models.LiveTelemetryPayload payload) {
        if (!payload.isSharingActive()) {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_session_ended);
            clearVehicleMarker();
            return;
        }

        binding.bannerTelemetryNotice.setVisibility(View.GONE);
        if (payload.getEtaMinutes() > 0) {
            binding.tvLiveMapEtaPill.setVisibility(View.VISIBLE);
            binding.tvLiveMapEtaPill.setText("ETA ~" + payload.getEtaMinutes() + " min");
        }
        if (payload.getStatus() != null && !payload.getStatus().isEmpty()) {
            binding.tvLiveMapStatus.setText("Status: " + payload.getStatus());
        }

        int tint = MODE_AMBULANCE.equalsIgnoreCase(mode)
                ? Color.parseColor("#C94B4B")
                : ContextCompat.getColor(this, R.color.primary_moss_green);
        String label = (payload.getOperatorName() != null && !payload.getOperatorName().isEmpty())
                ? payload.getOperatorName()
                : (MODE_AMBULANCE.equalsIgnoreCase(mode) ? "Ambulance" : "Delivery Rider");

        updateVehicleMarker(payload.getLatitude(), payload.getLongitude(),
                label,
                "Live GPS • " + (payload.getSpeed() > 0 ? String.format(java.util.Locale.ROOT, "%.1f km/h", payload.getSpeed()) : "Active"),
                tint);
    }

    private void bindInitialRecord() {
        if (MODE_AMBULANCE.equalsIgnoreCase(mode)) {
            AmbulanceRequest req = ambulanceRepo.getRequestById(hospitalId, recordId);
            if (req != null) renderAmbulance(req);
        } else {
            PharmacyOrder order = sessionManager.getPharmacyOrderLocal(recordId);
            if (order != null) renderPharmacyOrder(order);
        }
    }

    private void renderAmbulance(@NonNull AmbulanceRequest req) {
        binding.tvLiveMapTitle.setText("Ambulance Live Tracking • " + req.getPriorityLevel());
        binding.tvLiveMapStatus.setText("Status: " + req.getStatus());

        if (req.getEtaMinutes() > 0 && !req.isTerminal()) {
            binding.tvLiveMapEtaPill.setVisibility(View.VISIBLE);
            binding.tvLiveMapEtaPill.setText("ETA ~" + req.getEtaMinutes() + " min");
        } else {
            binding.tvLiveMapEtaPill.setVisibility(View.GONE);
        }

        if (req.getDriverName() != null && !req.getDriverName().isEmpty()) {
            binding.tvLivePartnerName.setText("Driver: " + req.getDriverName()
                    + " (" + (req.getVehicleNumber() != null ? req.getVehicleNumber() : "Ambulance") + ")");
        } else {
            binding.tvLivePartnerName.setText("Awaiting Ambulance & Driver Assignment");
        }
        binding.tvLivePartnerDetails.setText("Hospital: " + req.getHospitalName()
                + "\nPickup: " + req.getPickupAddress());
        this.partnerPhone = req.getDriverPhone() != null ? req.getDriverPhone() : "";

        if (req.isTerminal()) {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_session_ended);
            clearVehicleMarker();
        } else if (req.hasActiveLiveTelemetry()) {
            binding.bannerTelemetryNotice.setVisibility(View.GONE);
            updateVehicleMarker(req.getCurrentLat(), req.getCurrentLng(),
                    "Ambulance " + req.getVehicleNumber(),
                    "Status: " + req.getStatus(),
                    Color.parseColor("#C94B4B"));
        } else {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_no_telemetry);
            clearVehicleMarker();
            if (req.getPickupLat() != null && req.getPickupLng() != null) {
                GeoPoint pickup = new GeoPoint(req.getPickupLat(), req.getPickupLng());
                if (destinationMarker != null) {
                    binding.osmMapLiveTracking.getOverlays().remove(destinationMarker);
                }
                destinationMarker = createTintedMarker(pickup, "Pickup Location", req.getPickupAddress(),
                        ContextCompat.getColor(this, R.color.primary_moss_green));
                binding.osmMapLiveTracking.getOverlays().add(destinationMarker);
                binding.osmMapLiveTracking.getController().animateTo(pickup);
                binding.osmMapLiveTracking.getController().setZoom(14.5);
                binding.osmMapLiveTracking.invalidate();
            }
        }
    }

    private void renderPharmacyOrder(@NonNull PharmacyOrder order) {
        binding.tvLiveMapTitle.setText("Pharmacy Delivery #" + order.getOrderId());
        binding.tvLiveMapStatus.setText("Status: " + order.getStatus());

        if (order.getEstimatedDeliveryTime() != null && !order.getEstimatedDeliveryTime().isEmpty()) {
            binding.tvLiveMapEtaPill.setVisibility(View.VISIBLE);
            binding.tvLiveMapEtaPill.setText("ETA: " + order.getEstimatedDeliveryTime());
        } else {
            binding.tvLiveMapEtaPill.setVisibility(View.GONE);
        }

        if (order.getDeliveryPartnerName() != null && !order.getDeliveryPartnerName().isEmpty()) {
            binding.tvLivePartnerName.setText("Delivery Partner: " + order.getDeliveryPartnerName());
        } else {
            binding.tvLivePartnerName.setText("Delivery Partner Not Assigned Yet");
        }
        binding.tvLivePartnerDetails.setText("Type: " + order.getDeliveryType()
                + "\nAddress: " + (order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "Hospital Counter"));
        this.partnerPhone = order.getDeliveryPartnerPhone() != null ? order.getDeliveryPartnerPhone() : "";

        boolean isDone = PharmacyOrder.STATUS_DELIVERED.equalsIgnoreCase(order.getStatus())
                || PharmacyOrder.STATUS_COLLECTED.equalsIgnoreCase(order.getStatus())
                || PharmacyOrder.STATUS_CANCELLED.equalsIgnoreCase(order.getStatus());

        if (isDone) {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_session_ended);
            clearVehicleMarker();
        } else if (order.hasActiveLiveLocation()) {
            binding.bannerTelemetryNotice.setVisibility(View.GONE);
            updateVehicleMarker(order.getLiveLat(), order.getLiveLng(),
                    "Delivery Partner: " + order.getDeliveryPartnerName(),
                    "Order #" + order.getOrderId() + " (" + order.getStatus() + ")",
                    ContextCompat.getColor(this, R.color.primary_moss_green));
        } else {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_no_telemetry);
            clearVehicleMarker();
        }
    }

    private void updateVehicleMarker(Double lat, Double lng, String title, String snippet, int tintColor) {
        if (lat == null || lng == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        if (activeVehicleMarker == null) {
            activeVehicleMarker = createTintedMarker(pos, title, snippet, tintColor);
            binding.osmMapLiveTracking.getOverlays().add(activeVehicleMarker);
            binding.osmMapLiveTracking.getController().animateTo(pos);
            binding.osmMapLiveTracking.getController().setZoom(15.5);
        } else {
            activeVehicleMarker.setPosition(pos);
            activeVehicleMarker.setTitle(title);
            activeVehicleMarker.setSnippet(snippet);
            binding.osmMapLiveTracking.getController().animateTo(pos);
        }
        binding.osmMapLiveTracking.invalidate();
    }

    private Marker createTintedMarker(GeoPoint pos, String title, String snippet, int tintColor) {
        Marker m = new Marker(binding.osmMapLiveTracking);
        m.setPosition(pos);
        m.setTitle(title);
        m.setSnippet(snippet);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);
        if (icon != null) {
            Drawable tinted = icon.mutate();
            tinted.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            m.setIcon(tinted);
        }
        return m;
    }

    private void clearVehicleMarker() {
        if (activeVehicleMarker != null) {
            binding.osmMapLiveTracking.getOverlays().remove(activeVehicleMarker);
            activeVehicleMarker = null;
            binding.osmMapLiveTracking.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.osmMapLiveTracking.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.osmMapLiveTracking.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeRtdbRef != null && rtdbListener != null) {
            activeRtdbRef.removeEventListener(rtdbListener);
            rtdbListener = null;
        }
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }
    }
}
