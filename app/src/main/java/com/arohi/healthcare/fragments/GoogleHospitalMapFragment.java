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
import java.util.Locale;
import java.util.Map;

/**
 * Fragment rendering Google Maps styled indoor hospital navigation.
 * Features optimal shortest path indoor routing, vector floor plan rendering,
 * Google Maps pins, and interactive turn-by-turn live navigation simulation.
 */
public class GoogleHospitalMapFragment extends Fragment {

    private FragmentGoogleHospitalMapBinding binding;
    private HospitalRoutePlanner routePlanner;
    private final List<HospitalNode> allNodeList = new ArrayList<>();
    private final List<String> nodeNames = new ArrayList<>();

    private String currentFloor = "Ground Floor";
    private int selectedStartIndex = 1; // Default to Reception
    private int selectedDestIndex = 17; // Default to ICU

    private RouteResult currentRouteResult;
    private boolean isNavigating = false;
    private int currentNavStep = 0;

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
                if (selectedStartIndex != position) {
                    selectedStartIndex = position;
                    if (isNavigating) stopLiveNavigation();
                    calculateAndDisplayRoute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerDestinationLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedDestIndex != position) {
                    selectedDestIndex = position;
                    if (isNavigating) stopLiveNavigation();
                    calculateAndDisplayRoute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Tap on vector map to pick destination
        binding.googleHospitalMapView.setOnNodeClickListener(node -> {
            if (isNavigating) {
                stopLiveNavigation();
            }
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
            if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.btnSwitchToStandardMap.setOnClickListener(v -> {
            if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
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

        // Toggle turn-by-turn steps expandable list in overview mode
        binding.btnToggleStepDetails.setOnClickListener(v -> {
            boolean isVisible = binding.layoutTurnByTurnSteps.getVisibility() == View.VISIBLE;
            binding.layoutTurnByTurnSteps.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.btnToggleStepDetails.setText(isVisible ? "Steps" : "Hide");
        });

        // Overview Mode: Start Navigation
        binding.btnStartNavigation.setOnClickListener(v -> startLiveNavigation());

        // Active Navigation Banner: Close button
        binding.btnNavCloseBanner.setOnClickListener(v -> stopLiveNavigation());

        // Active Navigation: Exit button
        binding.btnNavExit.setOnClickListener(v -> stopLiveNavigation());

        // Active Navigation: Next Step button
        binding.btnNavNextStep.setOnClickListener(v -> advanceNavStep());
    }

    private void calculateAndDisplayRoute() {
        if (allNodeList.isEmpty() || selectedStartIndex < 0 || selectedStartIndex >= allNodeList.size()
                || selectedDestIndex < 0 || selectedDestIndex >= allNodeList.size()) {
            return;
        }

        HospitalNode start = allNodeList.get(selectedStartIndex);
        HospitalNode dest = allNodeList.get(selectedDestIndex);

        // Optimal shortest path calculation
        currentRouteResult = routePlanner.findShortestPathDijkstra(start.getId(), dest.getId());

        if (currentRouteResult != null) {
            binding.tvGoogleRouteTimeEstimate.setText(
                    currentRouteResult.getTimeFormatted() + " (" + currentRouteResult.getDistanceFormatted() + ")");
            binding.tvGoogleRouteAlgorithmBanner.setText(
                    "Fastest walking route • " + currentRouteResult.getPathNodes().size() + " waypoints");

            // Build turn-by-turn steps summary
            StringBuilder sb = new StringBuilder();
            List<String> steps = currentRouteResult.getTurnByTurnSteps();
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            binding.tvTurnByTurnInstructions.setText(sb.toString().trim());

            // Update Map View
            binding.googleHospitalMapView.setRoute(currentRouteResult, start, dest);

            // Automatically switch view to destination floor if starting floor is different
            if (!dest.getFloor().equalsIgnoreCase(currentFloor)) {
                currentFloor = dest.getFloor();
                syncFloorChipSelection(currentFloor);
                binding.googleHospitalMapView.setCurrentFloor(currentFloor);
            }
        }
    }

    private void startLiveNavigation() {
        if (currentRouteResult == null || currentRouteResult.getPathNodes().isEmpty()) {
            UiStateHelper.showWarning(binding.getRoot(), "Please select a valid route first.");
            return;
        }

        isNavigating = true;
        currentNavStep = 0;

        // Toggle UI Visibility for active navigation
        binding.layoutRoutePlannerInputs.setVisibility(View.GONE);
        binding.cardActiveNavBanner.setVisibility(View.VISIBLE);
        binding.btnStartNavigation.setVisibility(View.GONE);
        binding.layoutTurnByTurnSteps.setVisibility(View.GONE);
        binding.btnToggleStepDetails.setVisibility(View.GONE);
        binding.layoutActiveNavControls.setVisibility(View.VISIBLE);

        // Start View live navigation mode
        binding.googleHospitalMapView.startNavigation();

        // Update step UI
        updateNavStepUi();

        HospitalNode dest = allNodeList.get(selectedDestIndex);
        UiStateHelper.showSuccess(binding.getRoot(),
                "Navigation started to " + dest.getName() + " (" + dest.getFloor() + ")");
    }

    private void updateNavStepUi() {
        if (currentRouteResult == null) return;

        List<HospitalNode> pathNodes = currentRouteResult.getPathNodes();
        List<String> instructions = currentRouteResult.getTurnByTurnSteps();
        List<Double> stepDists = currentRouteResult.getStepDistances();
        int totalNodes = pathNodes.size();

        if (currentNavStep >= totalNodes) {
            completeNavigation();
            return;
        }

        // Update Map view active step
        binding.googleHospitalMapView.setNavigationStep(currentNavStep);

        // Floor Sync: auto-switch floor tab when step moves across floors
        HospitalNode currentNode = pathNodes.get(currentNavStep);
        if (!currentNode.getFloor().equalsIgnoreCase(currentFloor)) {
            currentFloor = currentNode.getFloor();
            syncFloorChipSelection(currentFloor);
            binding.googleHospitalMapView.setCurrentFloor(currentFloor);
        }

        // Check if reached destination
        boolean isArrival = (currentNavStep == totalNodes - 1);

        if (isArrival) {
            HospitalNode destNode = pathNodes.get(totalNodes - 1);
            binding.tvNavManeuverIcon.setText("🎉");
            binding.tvNavStepDistance.setText("Arrived");
            binding.tvNavCurrentInstruction.setText("You have arrived at " + destNode.getName());
            binding.tvNavNextPrompt.setText("Floor: " + destNode.getFloor() + " • Navigation complete");

            binding.tvNavStepProgress.setText("Arrival (" + totalNodes + "/" + totalNodes + ")");
            binding.tvNavRemainingStats.setText("0m remaining");
            binding.navProgressBar.setProgress(100);
            binding.btnNavNextStep.setText("Finish 🎉");
        } else {
            String instruction = (currentNavStep < instructions.size())
                    ? instructions.get(currentNavStep)
                    : "Proceed along indoor path to destination";

            double distMeters = (currentNavStep < stepDists.size())
                    ? stepDists.get(currentNavStep)
                    : 20.0;

            // Determine maneuver icon from instruction context
            String icon = "⬆";
            String lower = instruction.toLowerCase(Locale.ROOT);
            if (lower.contains("elevator")) {
                icon = "🛗";
            } else if (lower.contains("stairs")) {
                icon = "🪜";
            } else if (lower.contains("left")) {
                icon = "↰";
            } else if (lower.contains("right")) {
                icon = "↱";
            } else {
                icon = "⬆";
            }

            binding.tvNavManeuverIcon.setText(icon);
            binding.tvNavStepDistance.setText(String.format(Locale.ROOT, "In %.0f meters", distMeters));
            binding.tvNavCurrentInstruction.setText(instruction);

            // Next step preview
            if (currentNavStep + 1 < instructions.size()) {
                binding.tvNavNextPrompt.setText("Then: " + instructions.get(currentNavStep + 1));
            } else {
                HospitalNode dest = pathNodes.get(totalNodes - 1);
                binding.tvNavNextPrompt.setText("Destination: " + dest.getName() + " (" + dest.getFloor() + ")");
            }

            // Bottom card progress indicators
            int stepNumber = currentNavStep + 1;
            binding.tvNavStepProgress.setText("Step " + stepNumber + " of " + (totalNodes - 1));

            // Remaining distance and time calculation
            double remainingDist = 0;
            for (int k = currentNavStep; k < stepDists.size(); k++) {
                remainingDist += stepDists.get(k);
            }
            int remainingMins = Math.max(1, (int) Math.ceil((remainingDist / 1.2) / 60.0));
            binding.tvNavRemainingStats.setText(String.format(Locale.ROOT, "%.0fm • %d min", remainingDist, remainingMins));

            int totalLegs = Math.max(1, totalNodes - 1);
            int progressPercent = (int) Math.round(((double) currentNavStep / totalLegs) * 100);
            binding.navProgressBar.setProgress(progressPercent);

            binding.btnNavNextStep.setText(currentNavStep + 1 == totalNodes - 1 ? "Arrive ➔" : "Next Step ➔");
        }
    }

    private void advanceNavStep() {
        if (currentRouteResult == null) return;
        int totalNodes = currentRouteResult.getPathNodes().size();
        if (currentNavStep >= totalNodes - 1) {
            completeNavigation();
            return;
        }
        currentNavStep++;
        updateNavStepUi();
    }

    private void completeNavigation() {
        HospitalNode dest = allNodeList.get(selectedDestIndex);
        UiStateHelper.showSuccess(binding.getRoot(), "🎉 You have reached " + dest.getName() + "!");
        stopLiveNavigation();
    }

    private void stopLiveNavigation() {
        isNavigating = false;
        currentNavStep = 0;

        binding.layoutRoutePlannerInputs.setVisibility(View.VISIBLE);
        binding.cardActiveNavBanner.setVisibility(View.GONE);
        binding.btnStartNavigation.setVisibility(View.VISIBLE);
        binding.btnToggleStepDetails.setVisibility(View.VISIBLE);
        binding.layoutActiveNavControls.setVisibility(View.GONE);

        binding.googleHospitalMapView.stopNavigation();
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