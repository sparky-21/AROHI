package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arohi.healthcare.R;
import com.arohi.healthcare.adapters.HospitalLocationAdapter;
import com.arohi.healthcare.databinding.FragmentHospitalMapBinding;
import com.arohi.healthcare.models.HospitalLocation;
import com.arohi.healthcare.repositories.AuthCallback;
import com.arohi.healthcare.repositories.HospitalMapRepository;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.List;

/**
 * Fragment hosting the Interactive Hospital Map & Indoor Navigation Simulation.
 * Supports department search, floor selection, floor plan schematic visualization,
 * location details, and step-by-step route directions.
 */
public class HospitalMapFragment extends Fragment {

    private FragmentHospitalMapBinding binding;
    private HospitalMapRepository mapRepository;
    private HospitalLocationAdapter locationAdapter;

    private String currentFloor = HospitalLocation.FLOOR_GROUND;
    private String currentSearchQuery = "";
    private HospitalLocation selectedLocation = null;

    public static HospitalMapFragment newInstance() {
        return new HospitalMapFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHospitalMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapRepository = new HospitalMapRepository(requireContext());
        setupRecyclerView();
        setupListeners();
        loadLocationsForFloor(HospitalLocation.FLOOR_GROUND);
    }

    private void setupRecyclerView() {
        binding.recyclerHospitalLocations.setLayoutManager(new LinearLayoutManager(requireContext()));
        locationAdapter = new HospitalLocationAdapter(new HospitalLocationAdapter.OnLocationSelectListener() {
            @Override
            public void onLocationSelected(@NonNull HospitalLocation location) {
                displaySelectedLocation(location);
            }
        });
        binding.recyclerHospitalLocations.setAdapter(locationAdapter);
    }

    private void setupListeners() {
        binding.btnMapBack.setOnClickListener(v -> {
            if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.btnOpenGoogleMapStyle.setOnClickListener(v -> {
            if (getActivity() instanceof com.arohi.healthcare.activities.RolePortalActivity) {
                ((com.arohi.healthcare.activities.RolePortalActivity) getActivity()).openSubFragment(
                        GoogleHospitalMapFragment.newInstance(), "google_hospital_map");
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, GoogleHospitalMapFragment.newInstance())
                        .addToBackStack("google_hospital_map")
                        .commit();
            }
        });

        // Floor Chips
        binding.chipGroupFloors.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFloorGround) currentFloor = HospitalLocation.FLOOR_GROUND;
            else if (id == R.id.chipFloor1) currentFloor = HospitalLocation.FLOOR_1;
            else if (id == R.id.chipFloor2) currentFloor = HospitalLocation.FLOOR_2;
            else if (id == R.id.chipFloor3) currentFloor = HospitalLocation.FLOOR_3;
            else if (id == R.id.chipFloorBasement) currentFloor = HospitalLocation.FLOOR_BASEMENT;

            updateFloorPlanSchematicTiles(currentFloor);
            loadLocationsForFloor(currentFloor);
        });

        // Search Department
        binding.etSearchDepartment.setOnEditorActionListener((v, actionId, event) -> {
            currentSearchQuery = binding.etSearchDepartment.getText() != null
                    ? binding.etSearchDepartment.getText().toString().trim() : "";
            loadLocationsForFloor(currentFloor);
            return true;
        });

        // Interactive Floor Plan Tiles
        binding.tileWestWing.setOnClickListener(v -> selectTileDepartment("West Wing"));
        binding.tileCentralCorridor.setOnClickListener(v -> selectTileDepartment("Central"));
        binding.tileEastWing.setOnClickListener(v -> selectTileDepartment("East Wing"));
        binding.tileNorthWing.setOnClickListener(v -> selectTileDepartment("North Wing"));
        binding.tileElevators.setOnClickListener(v -> {
            UiStateHelper.showSuccess(binding.getRoot(),
                    "Elevator Bank: Serves Basement, Ground, Floor 1, Floor 2, and Floor 3 with stretcher-accessible elevators.");
        });
    }

    private void selectTileDepartment(String wingKeyword) {
        mapRepository.loadLocations(currentFloor, "", new AuthCallback<List<HospitalLocation>>() {
            @Override
            public void onSuccess(List<HospitalLocation> result) {
                for (HospitalLocation loc : result) {
                    if (loc.getWing() != null && loc.getWing().contains(wingKeyword)) {
                        displaySelectedLocation(loc);
                        break;
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void loadLocationsForFloor(String floor) {
        mapRepository.loadLocations(floor, currentSearchQuery, new AuthCallback<List<HospitalLocation>>() {
            @Override
            public void onSuccess(List<HospitalLocation> result) {
                if (binding == null) return;
                locationAdapter.submitList(result);
                if (!result.isEmpty()) {
                    if (selectedLocation == null || !floor.equals(selectedLocation.getFloor())) {
                        displaySelectedLocation(result.get(0));
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (binding != null) UiStateHelper.showError(binding.getRoot(), message);
            }
        });
    }

    private void displaySelectedLocation(@NonNull HospitalLocation loc) {
        this.selectedLocation = loc;
        if (binding == null) return;

        locationAdapter.setSelectedLocationId(loc.getLocationId());

        binding.tvDetailLocationName.setText(loc.getName());
        binding.tvDetailFloor.setText(loc.getFloor());
        binding.tvDetailWingAndRoom.setText(loc.getWing() + " • " + loc.getRoomNumber());
        binding.tvDetailDirections.setText(loc.getStepByStepDirections());
        binding.tvDetailHours.setText("Hours: " + loc.getWorkingHours());
        binding.tvDetailExtension.setText(loc.getContactExtension());
    }

    private void updateFloorPlanSchematicTiles(String floor) {
        if (binding == null) return;
        binding.tvFloorPlanTitle.setText(floor + " Schematic Layout");

        if (HospitalLocation.FLOOR_GROUND.equals(floor)) {
            binding.tileWestWing.setText("West Wing\nEmergency G-01");
            binding.tileCentralCorridor.setText("Central Atrium\nReception & Lobby");
            binding.tileEastWing.setText("East Wing\nPharmacy G-12");
            binding.tileNorthWing.setText("North Wing\nRadiology G-05");
        } else if (HospitalLocation.FLOOR_1.equals(floor)) {
            binding.tileWestWing.setText("West Wing\nOutpatient OPD");
            binding.tileCentralCorridor.setText("Central Waiting\nInformation Desk");
            binding.tileEastWing.setText("East Wing\nPathology Lab 108");
            binding.tileNorthWing.setText("North Wing\nCardiology OPD");
        } else if (HospitalLocation.FLOOR_2.equals(floor)) {
            binding.tileWestWing.setText("West Wing\nSurgical Ward C & D");
            binding.tileCentralCorridor.setText("Nurses' Station\nFloor 2 Duty Desk");
            binding.tileEastWing.setText("East Wing\nMedical Ward A & B");
            binding.tileNorthWing.setText("North Wing\nDoctor Chambers");
        } else if (HospitalLocation.FLOOR_3.equals(floor)) {
            binding.tileWestWing.setText("West Wing\nOT Waiting Area");
            binding.tileCentralCorridor.setText("Sterile Corridor\nRestricted Access");
            binding.tileEastWing.setText("South Wing\nOperation Theatres (OT)");
            binding.tileNorthWing.setText("North Wing\nICU & CCU Complex");
        } else {
            binding.tileWestWing.setText("West Wing\nFacility Mgmt");
            binding.tileCentralCorridor.setText("Basement Center\nCSSD & Bio-Med");
            binding.tileEastWing.setText("East Wing\nMorgue & Utility");
            binding.tileNorthWing.setText("North Wing\nMedical Records Archive");
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null && isAdded()) {
            loadLocationsForFloor(currentFloor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && isAdded()) {
            loadLocationsForFloor(currentFloor);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
