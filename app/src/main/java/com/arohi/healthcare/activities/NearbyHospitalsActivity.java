package com.arohi.healthcare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.NearbyHospitalAdapter;
import com.arohi.healthcare.databinding.ActivityNearbyHospitalsBinding;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalNetworkRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Phase 11 — Global Hospital Discovery & Nearby Hospital Finder using Google Maps SDK.
 * Features foreground location permission handling, live distance calculations,
 * dual synchronized Map/List views, manual area search, Google Maps directions navigation,
 * and a graceful demo mode fallback when Maps API credentials are not configured.
 */
public class NearbyHospitalsActivity extends BaseActivity implements OnMapReadyCallback,
        NearbyHospitalAdapter.OnNearbyHospitalClickListener {

    private ActivityNearbyHospitalsBinding binding;
    private HospitalNetworkRepository repository;
    private FusedLocationProviderClient fusedLocationClient;

    private GoogleMap googleMap;
    private NearbyHospitalAdapter listAdapter;

    private final List<Hospital> allHospitals = new ArrayList<>();
    private final List<Hospital> displayedHospitals = new ArrayList<>();
    private final List<Marker> mapMarkers = new ArrayList<>();

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private boolean isListViewMode = false;
    private Hospital selectedHospital = null;

    // Default center: Dhaka central health corridor
    private static final double DEFAULT_DHAKA_LAT = 23.7937;
    private static final double DEFAULT_DHAKA_LNG = 90.4043;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    binding.cardLocationPermissionWarning.setVisibility(View.GONE);
                    fetchUserLocation();
                } else {
                    handleLocationPermissionDenied();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNearbyHospitalsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new HospitalNetworkRepository(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupSearchAndFilters();
        initGoogleMap();
        checkAndRequestLocationPermission();
        loadHospitalData();
    }

    private void initViews() {
        binding.toolbarNearby.setNavigationOnClickListener(v -> finish());

        // Toggle View Mode (Map vs List)
        binding.btnToggleViewMode.setOnClickListener(v -> toggleViewMode());

        // Refresh Location
        binding.btnRefreshLocation.setOnClickListener(v -> {
            checkAndRequestLocationPermission();
        });

        // Location Permission Warning Card Actions
        binding.btnRetryLocationPermission.setOnClickListener(v -> {
            requestLocationPermissionWithRationale();
        });

        // Demo Mode Setup Guide Dialog
        binding.btnDemoSetupGuide.setOnClickListener(v -> showMapsConfigDialog());

        // Setup Recycler View for List View
        listAdapter = new NearbyHospitalAdapter(this);
        binding.rvNearbyHospitals.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNearbyHospitals.setAdapter(listAdapter);
    }

    private void setupSearchAndFilters() {
        binding.etSearchArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHospitals(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initGoogleMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNearbyFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Check if API key is provided in manifest
        checkMapsApiKeyConfigured();
    }

    private void checkMapsApiKeyConfigured() {
        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA).metaData;
            String apiKey = metaData != null ? metaData.getString("com.google.android.geo.API_KEY") : "";
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("YOUR_KEY")) {
                binding.layoutDemoModeBanner.setVisibility(View.VISIBLE);
            } else {
                binding.layoutDemoModeBanner.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            binding.layoutDemoModeBanner.setVisibility(View.VISIBLE);
        }
    }

    private void checkAndRequestLocationPermission() {
        boolean hasFine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasFine || hasCoarse) {
            binding.cardLocationPermissionWarning.setVisibility(View.GONE);
            fetchUserLocation();
        } else {
            requestLocationPermissionWithRationale();
        }
    }

    private void requestLocationPermissionWithRationale() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_location_rationale_title)
                    .setMessage(R.string.permission_location_rationale_message)
                    .setPositiveButton(R.string.btn_grant_location, (dialog, which) -> {
                        locationPermissionLauncher.launch(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        });
                    })
                    .setNegativeButton(R.string.btn_manual_search, (dialog, which) -> {
                        handleLocationPermissionDenied();
                    })
                    .show();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void handleLocationPermissionDenied() {
        binding.cardLocationPermissionWarning.setVisibility(View.VISIBLE);
        binding.tvNearbyStatusSummary.setText(getString(R.string.permission_location_denied_title)
                + " — Showing default region hospitals");
        // Center around default region (Dhaka)
        updateHospitalDistancesAndSort();
    }

    private void fetchUserLocation() {
        binding.progressBarLocating.setVisibility(View.VISIBLE);
        binding.tvNearbyStatusSummary.setText(R.string.locating_user);

        try {
            boolean hasFine = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasFine && !hasCoarse) {
                binding.progressBarLocating.setVisibility(View.GONE);
                return;
            }

            if (googleMap != null) {
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (SecurityException ignored) {}
            }

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        binding.progressBarLocating.setVisibility(View.GONE);
                        if (location != null) {
                            userLatitude = location.getLatitude();
                            userLongitude = location.getLongitude();
                            onUserLocationAcquired();
                        } else {
                            // Fallback to last known location
                            fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                                if (lastLoc != null) {
                                    userLatitude = lastLoc.getLatitude();
                                    userLongitude = lastLoc.getLongitude();
                                    onUserLocationAcquired();
                                } else {
                                    binding.tvNearbyStatusSummary.setText("Using regional hospital coordinates");
                                    updateHospitalDistancesAndSort();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        binding.progressBarLocating.setVisibility(View.GONE);
                        binding.tvNearbyStatusSummary.setText("Using regional hospital coordinates");
                        updateHospitalDistancesAndSort();
                    });
        } catch (SecurityException e) {
            binding.progressBarLocating.setVisibility(View.GONE);
            handleLocationPermissionDenied();
        }
    }

    private void onUserLocationAcquired() {
        binding.tvNearbyStatusSummary.setText(String.format(Locale.getDefault(),
                "Your Location: %.4f, %.4f • Sorted by proximity", userLatitude, userLongitude));

        if (googleMap != null) {
            LatLng userLatLng = new LatLng(userLatitude, userLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(12.5f)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        updateHospitalDistancesAndSort();
    }

    private void loadHospitalData() {
        repository.getHospitals(new AuthCallback<List<Hospital>>() {
            @Override
            public void onSuccess(List<Hospital> hospitals) {
                allHospitals.clear();
                if (hospitals != null) {
                    allHospitals.addAll(hospitals);
                }
                updateHospitalDistancesAndSort();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(NearbyHospitalsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterHospitals(String query) {
        String cleanQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        displayedHospitals.clear();

        for (Hospital h : allHospitals) {
            if (cleanQuery.isEmpty()) {
                displayedHospitals.add(h);
                continue;
            }

            boolean matchesName = h.getHospitalName() != null && h.getHospitalName().toLowerCase().contains(cleanQuery);
            boolean matchesCity = h.getCity() != null && h.getCity().toLowerCase().contains(cleanQuery);
            boolean matchesAddress = h.getAddress() != null && h.getAddress().toLowerCase().contains(cleanQuery);
            boolean matchesCode = h.getHospitalCode() != null && h.getHospitalCode().toLowerCase().contains(cleanQuery);
            boolean matchesDept = false;

            if (h.getSupportedDepartments() != null) {
                for (String dept : h.getSupportedDepartments()) {
                    if (dept.toLowerCase().contains(cleanQuery)) {
                        matchesDept = true;
                        break;
                    }
                }
            }

            if (matchesName || matchesCity || matchesAddress || matchesCode || matchesDept) {
                displayedHospitals.add(h);
            }
        }

        renderFilteredResults();
    }

    private void updateHospitalDistancesAndSort() {
        if (allHospitals.isEmpty()) return;

        // If user coordinates acquired, sort strictly by distance
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            Collections.sort(allHospitals, (h1, h2) -> {
                double d1 = h1.calculateDistanceKm(userLatitude, userLongitude);
                double d2 = h2.calculateDistanceKm(userLatitude, userLongitude);
                if (d1 < 0) return 1;
                if (d2 < 0) return -1;
                return Double.compare(d1, d2);
            });
        }

        displayedHospitals.clear();
        displayedHospitals.addAll(allHospitals);
        renderFilteredResults();
    }

    private void renderFilteredResults() {
        if (displayedHospitals.isEmpty()) {
            binding.layoutEmptyResults.setVisibility(View.VISIBLE);
            binding.rvNearbyHospitals.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyResults.setVisibility(View.GONE);
            if (isListViewMode) {
                binding.rvNearbyHospitals.setVisibility(View.VISIBLE);
            }
        }

        // Update list adapter
        listAdapter.updateData(displayedHospitals, userLatitude, userLongitude);

        // Update Map Markers
        renderMapMarkers();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Center initially on Dhaka health hub
        LatLng initialCenter = new LatLng(DEFAULT_DHAKA_LAT, DEFAULT_DHAKA_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialCenter, 11f));

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Hospital) {
                onHospitalClick((Hospital) tag);
            }
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> {
            binding.cardSelectedHospitalInfo.setVisibility(View.GONE);
        });

        // If location was already acquired before map completed loading
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(userLatitude, userLongitude), 12.5f));
        }

        renderMapMarkers();
    }

    private void renderMapMarkers() {
        if (googleMap == null) return;

        googleMap.clear();
        mapMarkers.clear();

        for (Hospital h : displayedHospitals) {
            if (h.getLatitude() == 0.0 && h.getLongitude() == 0.0) continue;

            LatLng pos = new LatLng(h.getLatitude(), h.getLongitude());

            float markerColor = h.isVerified()
                    ? BitmapDescriptorFactory.HUE_AZURE
                    : BitmapDescriptorFactory.HUE_RED;

            String snippet = h.isVerified() ? "AROHI Verified Hospital" : "Accredited Facility";
            if (userLatitude != 0.0 && userLongitude != 0.0) {
                double km = h.calculateDistanceKm(userLatitude, userLongitude);
                if (km >= 0) snippet += String.format(Locale.getDefault(), " • %.1f km", km);
            }

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(h.getHospitalName())
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                marker.setTag(h);
                mapMarkers.add(marker);
            }
        }
    }

    private void toggleViewMode() {
        isListViewMode = !isListViewMode;

        if (isListViewMode) {
            binding.rvNearbyHospitals.setVisibility(View.VISIBLE);
            binding.btnToggleViewMode.setText(R.string.view_map_mode);
            binding.btnToggleViewMode.setIconResource(R.drawable.ic_view_map);
            binding.cardSelectedHospitalInfo.setVisibility(View.GONE);
        } else {
            binding.rvNearbyHospitals.setVisibility(View.GONE);
            binding.btnToggleViewMode.setText(R.string.view_list_mode);
            binding.btnToggleViewMode.setIconResource(R.drawable.ic_view_list);
            if (selectedHospital != null) {
                binding.cardSelectedHospitalInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    // =========================================================================
    // HOSPITAL SELECTION & ACTIONS
    // =========================================================================

    @Override
    public void onHospitalClick(@NonNull Hospital hospital) {
        this.selectedHospital = hospital;

        // Populate bottom detail card
        binding.tvCardHospitalName.setText(hospital.getHospitalName());
        binding.tvCardHospitalAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : hospital.getCity());

        // Distance
        if (userLatitude != 0.0 && userLongitude != 0.0 && hospital.getLatitude() != 0.0) {
            double km = hospital.calculateDistanceKm(userLatitude, userLongitude);
            if (km >= 0) {
                binding.tvCardDistanceBadge.setText(String.format(Locale.getDefault(), "%.1f km away", km));
                binding.tvCardDistanceBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvCardDistanceBadge.setVisibility(View.GONE);
            }
        } else {
            binding.tvCardDistanceBadge.setVisibility(View.GONE);
        }

        // Verification & Beds
        if (hospital.isVerified()) {
            binding.tvCardVerificationBadge.setText("✓ " + getString(R.string.label_verified_arohi));
            binding.tvCardVerificationBadge.setTextColor(ContextCompat.getColor(this, R.color.status_verified));
        } else {
            binding.tvCardVerificationBadge.setText(getString(R.string.label_external_facility));
            binding.tvCardVerificationBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
        }

        binding.tvCardBeds.setText(getString(R.string.label_operational_beds, hospital.getTotalBeds()));

        // Departments
        if (hospital.getSupportedDepartments() != null && !hospital.getSupportedDepartments().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String dept : hospital.getSupportedDepartments()) {
                if (count > 0) sb.append(" • ");
                sb.append(dept);
                count++;
                if (count >= 5) break;
            }
            binding.tvCardDepartments.setText(sb.toString());
            binding.tvCardDepartments.setVisibility(View.VISIBLE);
        } else {
            binding.tvCardDepartments.setVisibility(View.GONE);
        }

        // Actions
        binding.btnCardDirections.setOnClickListener(v -> onDirectionsClick(hospital));

        binding.btnCardCallHotline.setOnClickListener(v -> {
            if (hospital.getEmergencyHotline() != null && !hospital.getEmergencyHotline().trim().isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + hospital.getEmergencyHotline().replaceAll("[^0-9+]", "")));
                startActivity(dialIntent);
            } else {
                Toast.makeText(this, "Emergency hotline not listed for this facility.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCardSelectHospital.setOnClickListener(v -> onSelectHospitalClick(hospital));

        binding.cardSelectedHospitalInfo.setVisibility(View.VISIBLE);

        // Center map on clicked hospital
        if (googleMap != null && hospital.getLatitude() != 0.0 && hospital.getLongitude() != 0.0) {
            if (isListViewMode) {
                toggleViewMode(); // Switch back to map to show position
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(hospital.getLatitude(), hospital.getLongitude()), 14.5f));
        }
    }

    @Override
    public void onDirectionsClick(@NonNull Hospital hospital) {
        if (hospital.getLatitude() == 0.0 && hospital.getLongitude() == 0.0) {
            Toast.makeText(this, "GPS coordinates unavailable for directions.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try standard Google Maps Navigation Intent
        String uriString = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f&mode=d",
                hospital.getLatitude(), hospital.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser Google Maps directions
            String webUri = String.format(Locale.ENGLISH,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                    hospital.getLatitude(), hospital.getLongitude());
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            startActivity(webIntent);
        }
    }

    @Override
    public void onSelectHospitalClick(@NonNull Hospital hospital) {
        repository.selectHospital(hospital);
        Toast.makeText(this, "Selected: " + hospital.getHospitalName() + " (" + hospital.getHospitalCode() + ")",
                Toast.LENGTH_SHORT).show();

        // Proceed to Authentication / Role selection flow
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        startActivity(intent);
    }

    private void showMapsConfigDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_maps_dialog_title)
                .setMessage(R.string.demo_maps_dialog_body)
                .setPositiveButton("OK", null)
                .show();
    }
}
