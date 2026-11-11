package com.arohi.healthcare.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arohi.healthcare.R;
import com.arohi.healthcare.database.FirestoreCollections;
import com.arohi.healthcare.databinding.ActivityLiveTrackingMapBinding;
import com.arohi.healthcare.models.AmbulanceRequest;
import com.arohi.healthcare.models.PharmacyOrder;
import com.arohi.healthcare.repositories.AmbulanceTrackingRepository;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.services.FirebaseProvider;
import com.arohi.healthcare.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Phase 14 Shared Google Maps Live Telemetry Screen.
 * Supports:
 * 1. Ambulance Dispatch Live GPS Tracking
 * 2. Pharmacy Home Delivery Partner Live GPS Tracking
 *
 * Strictly enforces:
 * - Zero fake live coordinates: only renders moving partner/ambulance markers when authorized telemetry exists.
 * - Automatically detaches telemetry updates and clears active marker when session ends.
 */
public class LiveTrackingMapActivity extends BaseActivity implements OnMapReadyCallback {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_AMBULANCE = "AMBULANCE";
    public static final String MODE_PHARMACY = "PHARMACY";
    public static final String EXTRA_RECORD_ID = "extra_record_id";
    public static final String EXTRA_HOSPITAL_ID = "extra_hospital_id";

    private ActivityLiveTrackingMapBinding binding;
    private SessionManager sessionManager;
    private AmbulanceTrackingRepository ambulanceRepo;
    private GoogleMap googleMap;
    private Marker activeVehicleMarker;
    private Marker destinationMarker;
    private ListenerRegistration realtimeListener;

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

        setupMapOrFallback();
        bindInitialRecordAndListen();
    }

    private void setupMapOrFallback() {
        if (!hasValidMapsApiKey()) {
            binding.layoutMapConfigFallback.setVisibility(View.VISIBLE);
            return;
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.liveTrackingMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private boolean hasValidMapsApiKey() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) return false;
            String key = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            return key != null && !key.trim().isEmpty() && !"null".equalsIgnoreCase(key.trim());
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        bindInitialRecord();
    }

    private void bindInitialRecordAndListen() {
        bindInitialRecord();
        if (MODE_AMBULANCE.equalsIgnoreCase(mode)) {
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
            updateMapMarker(req.getCurrentLat(), req.getCurrentLng(),
                    "Ambulance " + req.getVehicleNumber(),
                    "Status: " + req.getStatus(),
                    BitmapDescriptorFactory.HUE_RED);
        } else {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_no_telemetry);
            clearVehicleMarker();
            if (req.getPickupLat() != null && req.getPickupLng() != null && googleMap != null) {
                LatLng pickup = new LatLng(req.getPickupLat(), req.getPickupLng());
                if (destinationMarker != null) destinationMarker.remove();
                destinationMarker = googleMap.addMarker(new MarkerOptions()
                        .position(pickup)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 14f));
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
            updateMapMarker(order.getLiveLat(), order.getLiveLng(),
                    "Delivery Partner: " + order.getDeliveryPartnerName(),
                    "Order #" + order.getOrderId() + " (" + order.getStatus() + ")",
                    BitmapDescriptorFactory.HUE_AZURE);
        } else {
            binding.bannerTelemetryNotice.setVisibility(View.VISIBLE);
            binding.tvTelemetryNoticeText.setText(R.string.tracking_map_no_telemetry);
            clearVehicleMarker();
        }
    }

    private void updateMapMarker(Double lat, Double lng, String title, String snippet, float hue) {
        if (googleMap == null || lat == null || lng == null) return;
        LatLng pos = new LatLng(lat, lng);
        if (activeVehicleMarker == null) {
            activeVehicleMarker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        } else {
            activeVehicleMarker.setPosition(pos);
            activeVehicleMarker.setTitle(title);
            activeVehicleMarker.setSnippet(snippet);
        }
    }

    private void clearVehicleMarker() {
        if (activeVehicleMarker != null) {
            activeVehicleMarker.remove();
            activeVehicleMarker = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }
    }
}
