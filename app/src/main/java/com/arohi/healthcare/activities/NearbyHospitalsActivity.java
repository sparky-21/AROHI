package com.arohi.healthcare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.NearbyHospitalAdapter;
import com.arohi.healthcare.databinding.ActivityNearbyHospitalsBinding;
import com.arohi.healthcare.models.Hospital;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalNetworkRepository;
import com.arohi.healthcare.services.OverpassHospitalService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Phase 11 — Global Hospital Discovery & Nearby Hospital Finder using Free OpenStreetMap (OSMDroid)
 * and OpenStreetMap Overpass API for zero-cost, zero-API-key external hospital lookup.
 * Retains Google Maps app deep-linking for external turn-by-turn navigation.
 */
public class NearbyHospitalsActivity extends BaseActivity implements NearbyHospitalAdapter.OnNearbyHospitalClickListener {

    private ActivityNearbyHospitalsBinding binding;
    private HospitalNetworkRepository repository;
    private FusedLocationProviderClient fusedLocationClient;

    private NearbyHospitalAdapter listAdapter;
    private MyLocationNewOverlay myLocationOverlay;

    private final List<Hospital> allHospitals = new ArrayList<>();
    private final List<Hospital> displayedHospitals = new ArrayList<>();
    private final List<Marker> mapMarkers = new ArrayList<>();

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private boolean isListViewMode = false;
    private boolean overpassLoadedForLocation = false;
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
                    enableMyLocationOverlay();
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
        initOpenStreetMap();
        checkAndRequestLocationPermission();
        loadHospitalData();
    }

    private void initViews() {
        binding.toolbarNearby.setNavigationOnClickListener(v -> finish());
        binding.layoutDemoModeBanner.setVisibility(View.GONE);

        binding.btnToggleViewMode.setOnClickListener(v -> toggleViewMode());
        binding.btnRefreshLocation.setOnClickListener(v -> checkAndRequestLocationPermission());

        binding.btnOpenGoogleMapsApp.setOnClickListener(v -> {
            String query = binding.etSearchArea.getText() != null ? binding.etSearchArea.getText().toString() : "";
            openInGoogleMapsApp(query);
        });

        binding.btnDemoOpenGoogleMaps.setOnClickListener(v -> {
            String query = binding.etSearchArea.getText() != null ? binding.etSearchArea.getText().toString() : "";
            openInGoogleMapsApp(query);
        });

        binding.btnRetryLocationPermission.setOnClickListener(v -> requestLocationPermissionWithRationale());

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

    private void initOpenStreetMap() {
        binding.osmMapNearby.setTileSource(TileSourceFactory.MAPNIK);
        binding.osmMapNearby.setMultiTouchControls(true);
        binding.osmMapNearby.getController().setZoom(12.5);
        binding.osmMapNearby.getController().setCenter(new GeoPoint(DEFAULT_DHAKA_LAT, DEFAULT_DHAKA_LNG));

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                binding.cardSelectedHospitalInfo.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        binding.osmMapNearby.getOverlays().add(eventsOverlay);
    }

    private void enableMyLocationOverlay() {
        boolean hasFine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) return;

        if (myLocationOverlay == null) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), binding.osmMapNearby);
            myLocationOverlay.enableMyLocation();
            binding.osmMapNearby.getOverlays().add(myLocationOverlay);
        }
    }

    private void checkAndRequestLocationPermission() {
        boolean hasFine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (hasFine || hasCoarse) {
            binding.cardLocationPermissionWarning.setVisibility(View.GONE);
            enableMyLocationOverlay();
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
                    .setPositiveButton(R.string.btn_grant_location, (dialog, which) ->
                            locationPermissionLauncher.launch(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            }))
                    .setNegativeButton(R.string.btn_manual_search, (dialog, which) -> handleLocationPermissionDenied())
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
                + " — Showing OpenStreetMap regional hospitals");
        updateHospitalDistancesAndSort();
        fetchExternalOsmHospitalsOnce(DEFAULT_DHAKA_LAT, DEFAULT_DHAKA_LNG);
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

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        binding.progressBarLocating.setVisibility(View.GONE);
                        if (location != null) {
                            userLatitude = location.getLatitude();
                            userLongitude = location.getLongitude();
                            onUserLocationAcquired();
                        } else {
                            fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                                if (lastLoc != null) {
                                    userLatitude = lastLoc.getLatitude();
                                    userLongitude = lastLoc.getLongitude();
                                    onUserLocationAcquired();
                                } else {
                                    binding.tvNearbyStatusSummary.setText("Using regional hospital coordinates (OpenStreetMap)");
                                    updateHospitalDistancesAndSort();
                                    fetchExternalOsmHospitalsOnce(DEFAULT_DHAKA_LAT, DEFAULT_DHAKA_LNG);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        binding.progressBarLocating.setVisibility(View.GONE);
                        binding.tvNearbyStatusSummary.setText("Using regional hospital coordinates (OpenStreetMap)");
                        updateHospitalDistancesAndSort();
                    });
        } catch (SecurityException e) {
            binding.progressBarLocating.setVisibility(View.GONE);
            handleLocationPermissionDenied();
        }
    }

    private void onUserLocationAcquired() {
        binding.tvNearbyStatusSummary.setText(String.format(Locale.getDefault(),
                "Your Location: %.4f, %.4f • Sorted by proximity (OSM)", userLatitude, userLongitude));

        binding.osmMapNearby.getController().animateTo(new GeoPoint(userLatitude, userLongitude));
        binding.osmMapNearby.getController().setZoom(13.0);

        updateHospitalDistancesAndSort();
        fetchExternalOsmHospitalsOnce(userLatitude, userLongitude);
    }

    private void fetchExternalOsmHospitalsOnce(double lat, double lng) {
        if (overpassLoadedForLocation) return;
        overpassLoadedForLocation = true;

        OverpassHospitalService.fetchNearbyExternalHospitals(lat, lng, 8000, externalList -> {
            if (externalList == null || externalList.isEmpty() || isFinishing()) return;
            runOnUiThread(() -> {
                for (Hospital ext : externalList) {
                    boolean duplicate = false;
                    for (Hospital existing : allHospitals) {
                        if (existing.getHospitalName() != null && ext.getHospitalName() != null
                                && existing.getHospitalName().equalsIgnoreCase(ext.getHospitalName())) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        allHospitals.add(ext);
                    }
                }
                updateHospitalDistancesAndSort();
            });
        });
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

        for (Hospital h : allHospitals) {
            HospitalNetworkRepository.ensureHospitalCoordinates(h);
        }

        double refLat = (userLatitude != 0.0) ? userLatitude : DEFAULT_DHAKA_LAT;
        double refLng = (userLongitude != 0.0) ? userLongitude : DEFAULT_DHAKA_LNG;

        Collections.sort(allHospitals, (h1, h2) -> {
            double d1 = h1.calculateDistanceKm(refLat, refLng);
            double d2 = h2.calculateDistanceKm(refLat, refLng);
            if (d1 < 0) return 1;
            if (d2 < 0) return -1;
            return Double.compare(d1, d2);
        });

        String currentQuery = binding.etSearchArea.getText() != null ? binding.etSearchArea.getText().toString() : "";
        filterHospitals(currentQuery);

        if (selectedHospital == null && !displayedHospitals.isEmpty()) {
            onHospitalClick(displayedHospitals.get(0));
        }
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

        listAdapter.updateData(displayedHospitals, userLatitude, userLongitude);
        renderMapMarkers();
    }

    private void renderMapMarkers() {
        for (Marker old : mapMarkers) {
            binding.osmMapNearby.getOverlays().remove(old);
        }
        mapMarkers.clear();

        double refLat = (userLatitude != 0.0) ? userLatitude : DEFAULT_DHAKA_LAT;
        double refLng = (userLongitude != 0.0) ? userLongitude : DEFAULT_DHAKA_LNG;

        for (Hospital h : displayedHospitals) {
            HospitalNetworkRepository.ensureHospitalCoordinates(h);
            if (h.getLatitude() == 0.0 && h.getLongitude() == 0.0) continue;

            Marker marker = new Marker(binding.osmMapNearby);
            marker.setPosition(new GeoPoint(h.getLatitude(), h.getLongitude()));
            marker.setTitle(h.getHospitalName());
            double km = h.calculateDistanceKm(refLat, refLng);
            String snippet = h.isVerified() ? "AROHI Verified Hospital" : "External OSM Facility";
            if (km >= 0) snippet += String.format(Locale.getDefault(), " • %.1f km", km);
            marker.setSnippet(snippet);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);
            if (icon != null) {
                Drawable tinted = icon.mutate();
                int tint = h.isVerified()
                        ? ContextCompat.getColor(this, R.color.primary_moss_green)
                        : Color.parseColor("#C94B4B");
                tinted.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
                marker.setIcon(tinted);
            }

            marker.setOnMarkerClickListener((m, mapView) -> {
                onHospitalClick(h);
                m.showInfoWindow();
                return true;
            });

            binding.osmMapNearby.getOverlays().add(marker);
            mapMarkers.add(marker);
        }
        binding.osmMapNearby.invalidate();
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

    @Override
    public void onHospitalClick(@NonNull Hospital hospital) {
        this.selectedHospital = hospital;

        binding.tvCardHospitalName.setText(hospital.getHospitalName());
        binding.tvCardHospitalAddress.setText(hospital.getAddress() != null ? hospital.getAddress() : hospital.getCity());

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

        if (hospital.isVerified()) {
            binding.tvCardVerificationBadge.setText("✓ " + getString(R.string.label_verified_arohi));
            binding.tvCardVerificationBadge.setTextColor(ContextCompat.getColor(this, R.color.status_verified));
            binding.btnCardSelectHospital.setVisibility(View.VISIBLE);
        } else {
            binding.tvCardVerificationBadge.setText(getString(R.string.label_external_facility));
            binding.tvCardVerificationBadge.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
            binding.btnCardSelectHospital.setVisibility(View.GONE);
        }

        binding.tvCardBeds.setText(hospital.getTotalBeds() > 0
                ? getString(R.string.label_operational_beds, hospital.getTotalBeds())
                : "Public Directory Entry");

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

        if (hospital.getLatitude() != 0.0 && hospital.getLongitude() != 0.0) {
            if (isListViewMode) {
                toggleViewMode();
            }
            binding.osmMapNearby.getController().animateTo(
                    new GeoPoint(hospital.getLatitude(), hospital.getLongitude()));
            binding.osmMapNearby.getController().setZoom(15.0);
        }
    }

    @Override
    public void onDirectionsClick(@NonNull Hospital hospital) {
        if (hospital.getLatitude() == 0.0 && hospital.getLongitude() == 0.0) {
            Toast.makeText(this, "GPS coordinates unavailable for directions.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uriString = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f&mode=d",
                hospital.getLatitude(), hospital.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            String webUri = String.format(Locale.ENGLISH,
                    "https://www.openstreetmap.org/directions?to=%f,%f",
                    hospital.getLatitude(), hospital.getLongitude());
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
        }
    }

    @Override
    public void onSelectHospitalClick(@NonNull Hospital hospital) {
        repository.selectHospital(hospital);
        Toast.makeText(this, "Selected: " + hospital.getHospitalName() + " (" + hospital.getHospitalCode() + ")",
                Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, RoleSelectionActivity.class));
    }

    public void openInGoogleMapsApp(String query) {
        Uri uri;
        double lat = (userLatitude != 0.0) ? userLatitude : DEFAULT_DHAKA_LAT;
        double lng = (userLongitude != 0.0) ? userLongitude : DEFAULT_DHAKA_LNG;

        if (selectedHospital != null && selectedHospital.getLatitude() != 0.0) {
            String label = Uri.encode(selectedHospital.getHospitalName());
            uri = Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)",
                    selectedHospital.getLatitude(), selectedHospital.getLongitude(),
                    selectedHospital.getLatitude(), selectedHospital.getLongitude(), label));
        } else if (query != null && !query.trim().isEmpty()) {
            uri = Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f?q=%s",
                    lat, lng, Uri.encode(query + " hospital")));
        } else {
            uri = Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f?q=%s",
                    lat, lng, Uri.encode("hospitals near me")));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            String webUrl = String.format(Locale.ENGLISH,
                    "https://www.openstreetmap.org/#map=14/%f/%f", lat, lng);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.osmMapNearby.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.osmMapNearby.onPause();
    }
}
