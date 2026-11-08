package com.arohi.healthcare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arohi.healthcare.R;
import com.arohi.healthcare.databinding.FragmentGoogleHospitalMapBinding;
import com.arohi.healthcare.utils.HospitalRoutePlanner;
import com.arohi.healthcare.utils.HospitalRoutePlanner.HospitalNode;
import com.arohi.healthcare.utils.HospitalRoutePlanner.RouteResult;
import com.arohi.healthcare.utils.UiStateHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment rendering Google Maps styled indoor hospital navigation.
 * Features Dijkstra's & Bellman-Ford optimal shortest path calculation,
 * vector floor plan rendering, Google pins, and turn-by-turn pedestrian routing.
 */
public class GoogleHospitalMapFragment extends Fragment {

    private FragmentGoogleHospitalMapBinding binding;
    private HospitalRoutePlanner routePlanner;
    private final List<HospitalNode> allNodeList = new ArrayList<>();
    private final List<String> nodeNames = new ArrayList<>();

    private String currentFloor = "Ground Floor";
    private boolean useDijkstra = true;
    private int selectedStartIndex = 1; // RECEPTION
    private int selectedDestIndex = 17; // ICU

    public static GoogleHospitalMapFragment newInstance() {
        return new GoogleHospitalMapFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGoogleHospitalMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        routePlanner = new HospitalRoutePlanner();
        binding.googleHospitalMapView.setRoutePlanner(routePlanner);

        setupNodes();
        setupSpinners();
        setupControls();
        calculateAndDisplayRoute();
    }

    private void setupNodes() {
        allNodeList.clear();
        nodeNames.clear();

        Map<String, HospitalNode> map = routePlanner.getAllNodes();
        for (HospitalNode node : map.values()) {
            allNodeList.add(node);
            nodeNames.add(node.getName() + " (" + node.getFloor() + ")");
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, nodeNames);

        binding.spinnerStartLocation.setAdapter(adapter);
        binding.spinnerDestinationLocation.setAdapter(adapter);

        // Find Reception and ICU default indices
        for (int i = 0; i < allNodeList.size(); i++) {
            if ("RECEPTION".equals(allNodeList.get(i).getId())) selectedStartIndex = i;
            if ("ICU".equals(allNodeList.get(i).getId())) selectedDestIndex = i;
        }

        binding.spinnerStartLocation.setSelection(selectedStartIndex);
        binding.spinnerDestinationLocation.setSelection(selectedDestIndex);

        binding.spinnerStartLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStartIndex = position;
                calculateAndDisplayRoute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerDestinationLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDestIndex = position;
                calculateAndDisplayRoute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Tap on vector map to pick destination
        binding.googleHospitalMapView.setOnNodeClickListener(node -> {
            for (int i = 0; i < allNodeList.size(); i++) {
                if (allNodeList.get(i).getId().equals(node.getId())) {
                    binding.spinnerDestinationLocation.setSelection(i);
                    UiStateHelper.showSuccess(binding.getRoot(), "Destination set to " + node.getName());
                    break;
                }
            }
        });
    }

    private void setupControls() {
        binding.btnGoogleMapBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.btnSwitchToStandardMap.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Algorithm Switch
        binding.chipGroupAlgorithm.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            useDijkstra = (id == R.id.chipDijkstra);
            calculateAndDisplayRoute();
        });

        // Floor selector chips
        binding.chipGroupGoogleFloors.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipGFloorGround) currentFloor = "Ground Floor";
            else if (id == R.id.chipGFloor1) currentFloor = "Floor 1";
            else if (id == R.id.chipGFloor2) currentFloor = "Floor 2";
            else if (id == R.id.chipGFloor3) currentFloor = "Floor 3";
            else if (id == R.id.chipGFloorB) currentFloor = "Basement";

            binding.googleHospitalMapView.setCurrentFloor(currentFloor);
        });

        // Toggle turn-by-turn steps
        binding.btnToggleStepDetails.setOnClickListener(v -> {
            boolean isVisible = binding.layoutTurnByTurnSteps.getVisibility() == View.VISIBLE;
            binding.layoutTurnByTurnSteps.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.btnToggleStepDetails.setText(isVisible ? "Steps" : "Hide");
        });

        // Start Navigation
        binding.btnStartNavigation.setOnClickListener(v -> {
            HospitalNode dest = allNodeList.get(selectedDestIndex);
            UiStateHelper.showSuccess(binding.getRoot(),
                    "Navigation active: Follow Google Blue route on Floor " + dest.getFloor() + " to " + dest.getName());
        });
    }

    private void calculateAndDisplayRoute() {
        if (allNodeList.isEmpty() || selectedStartIndex >= allNodeList.size() || selectedDestIndex >= allNodeList.size()) {
            return;
        }

        HospitalNode start = allNodeList.get(selectedStartIndex);
        HospitalNode dest = allNodeList.get(selectedDestIndex);

        RouteResult result;
        if (useDijkstra) {
            result = routePlanner.findShortestPathDijkstra(start.getId(), dest.getId());
        } else {
            result = routePlanner.findShortestPathBellmanFord(start.getId(), dest.getId());
        }

        if (result != null) {
            binding.tvGoogleRouteTimeEstimate.setText(result.getTimeFormatted() + " (" + result.getDistanceFormatted() + ")");
            binding.tvGoogleRouteAlgorithmBanner.setText(result.getAlgorithmUsed());

            // Build turn-by-turn steps
            StringBuilder sb = new StringBuilder();
            List<String> steps = result.getTurnByTurnSteps();
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            binding.tvTurnByTurnInstructions.setText(sb.toString().trim());

            // Update View
            binding.googleHospitalMapView.setRoute(result, start, dest);

            // Automatically switch view to destination floor
            if (!dest.getFloor().equalsIgnoreCase(currentFloor)) {
                currentFloor = dest.getFloor();
                syncFloorChipSelection(currentFloor);
                binding.googleHospitalMapView.setCurrentFloor(currentFloor);
            }
        }
    }

    private void syncFloorChipSelection(String floor) {
        if ("Ground Floor".equalsIgnoreCase(floor)) binding.chipGFloorGround.setChecked(true);
        else if ("Floor 1".equalsIgnoreCase(floor)) binding.chipGFloor1.setChecked(true);
        else if ("Floor 2".equalsIgnoreCase(floor)) binding.chipGFloor2.setChecked(true);
        else if ("Floor 3".equalsIgnoreCase(floor)) binding.chipGFloor3.setChecked(true);
        else if ("Basement".equalsIgnoreCase(floor)) binding.chipGFloorB.setChecked(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
